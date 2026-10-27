# Engineering Report: Milestone 8.8 — Controller Semantics and Clearance Audit

## Executive Summary

Milestone 8, Phase 8.8 conducts a rigorous **Controller Semantics and Clearance Audit** across the Astra CAM pipeline (`cam.model`, `cam.post`, `cam.post.verifier`, `cam.toolpath`, and `cam.plan`).

Before expanding toolpath generation into 3D strategies, Phase 8.8 closes critical distinctions between purely passing software geometric tests and proving that reconstructed trajectories faithfully reflect physical CNC controller execution. Phase 8.8 establishes the exact controller dialect and coordinate transformation chain, enforces analytical line-segment boundary crossing checks for continuous swept paths, performs 3D AABB slab intersection checks against fixture envelopes, rejects invalid fixture definitions fail-closed, and provides explicit disclosure of unmodeled machine envelopes.

### Key Metrics
* **New Assertions**: **51 / 51 passed (0 failures)**.
* **Cumulative Direct CAM Assertions**: **1,146 / 1,146 passed across 17 suites (100% pass rate)**.
* **Production Java Files**: **310 files** — strictly **0 files exceeding 200 physical lines** (longest CAM production file: `NcProgramVerifier.java` at 195 lines; `NcMotionVerifier.java` at 177 lines; `IndependentGeometricOracle.java` at 163 lines; `NcTrajectoryReconstructor.java` at 139 lines; `StockDimensions.java` at 98 lines; `NcFindingCode.java` at 62 lines).
* **Headless Decoupling**: Scanned `src/main/java/cam` for `javafx` imports — **0 occurrences**.
* **Build Integrity**: `pom.xml` SHA-256 hash `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309` (**strictly preserved**). Clean JDK 25 compilation.
* **Preservation**: `src/main/CPP/` untouched and untracked.

---

## 1. Controller Dialect & Coordinate Support Boundaries

### 1.1 Supported Dialect & Controller Profile
Astra CAM targets a conservative subset of **ISO 6983 / Fanuc 0i / 31i** standard linear CNC milling syntax. To prevent ambiguity, the postprocessor verifier strictly rejects unsupported or unmodeled syntax fail-closed:

| NC Domain | Supported Subset | Rejected / Flagged Syntax | Enforcement Mechanism |
| :--- | :--- | :--- | :--- |
| **Units** | `G21` (Metric mm) or `G20` (Inch), matched to `ControllerProfile.unitMode()` | Unit mode mismatch or unmodeled unit switches (`UNIT_MODE_MISMATCH`) | `NcProgramVerifier` cross-checks active unit mode against post profile |
| **Distance Modes** | `G90` (Absolute) and `G91` (Incremental) | Unknown coordinate modes, mixed modal conflicts | `NcTrajectoryReconstructor` & `NcBlockParser` |
| **Work Offsets (WCS)** | `G54`, `G55`, `G56`, `G57`, `G58`, `G59` | Programmed motion before active WCS selection (`COORDINATE_SYSTEM_UNRESOLVED`) | Reconstructor flags motion as `CoordinateFrame.UNRESOLVED` and halts |
| **Machine Offsets** | `G53` (Non-modal machine coordinate reference) | `G53` with cutting feeds or invalid machine retract targets (`RETRACT_TARGET_INVALID`) | `NcProgramVerifier` & `NcInterpreterState` |
| **Tool Length Comp** | `G43 H{toolNumber}` (Height compensation activation) and `G49` (Cancel) | Mismatched `H` register (`TOOL_OFFSET_MISMATCH`) or cutting without `G43` (`TOOL_LENGTH_COMP_MISSING`) | `NcMotionVerifier` verifies active tool vs `H` register |
| **Cutter Radius Comp** | Programmed centerline geometry only | `G41` (Left) / `G42` (Right) / `G40` (Cancel) rejected (`UNSUPPORTED_NC_COMMAND`) | Fail-closed preflight rejection |
| **Tool Changes** | `T{n} M06` preceded by safe retract (`G91 G28 Z0.` or `G53 Z0.`) | Wild unretracted tool changes (`TOOL_CHANGE_WITHOUT_RETRACT`) | `NcProgramVerifier` preflight checks |
| **Canned Cycles** | `G81` (Drill), `G82` (Counterbore), `G83` (Peck) with `G80` cancel | Unsupported cycles (`G84` tap, `G76` bore, etc. flagged as `UNSUPPORTED_NC_COMMAND`) | `NcMotionVerifier` validates known cycle envelope |
| **Subprograms & Macro**| None permitted in base contract | `M98`/`M99` subprograms, `G65` macro calls, `G68`/`G69` coordinate rotations | Explicitly rejected with `UNSUPPORTED_NC_COMMAND` |

### 1.2 Coordinate Semantics & Transformation Chain
Reconstructed trajectory points are assigned explicit coordinate frames via the `CoordinateFrame` enumeration:
```
Programmed Block Coordinates (X, Y, Z)
   ↓ (G54 - G59 Active WCS Translation)
Workpiece Coordinate System (WCS) Tool-Tip Positions [CoordinateFrame.WORKPIECE_WCS]
   ↓ (Active Tool Length Offset H)
Compensated Spindle Gauge-Line / Tool-Tip Position
   ↓ (G53 Non-modal Override)
Machine Coordinate System (MCS) [CoordinateFrame.MACHINE_G53]
```
If an NC program attempts tool motion without establishing an active WCS (`G54`-`G59`), `NcTrajectoryReconstructor` flags the motion as `CoordinateFrame.UNRESOLVED` and preflight verification fails closed with `COORDINATE_SYSTEM_UNRESOLVED`.

---

## 2. Continuous Trajectory Geometry & Analytical Edge Crossing

### 2.1 The Multi-Lobe Concave Void Problem
In prior milestones, trajectory verification sampled endpoints ($T_0, T_1$) and the midpoint ($T_{\text{mid}}$) against part boundaries using Jordan curve ray casting. For concave geometries (such as U-shaped or W-shaped pocket boundaries), a straight line between two valid points inside the cavity can traverse outside into retained material across the concave void, while its midpoint happens to lie inside an opposite lobe or cavity island.

### 2.2 Analytical Line-Segment Intersection Guarantee
To guarantee physical safety, [`IndependentGeometricOracle.java`](file:///e:/MultiPhysics/src/main/java/cam/toolpath/IndependentGeometricOracle.java) now performs an analytical edge-by-edge intersection test:
$$\text{segmentsIntersect}(T_0, T_1, B_i, B_{i+1})$$
against every edge of the outer boundary and every edge of all nested islands.

* If any edge intersection is detected with non-endpoint collinearity or interior crossing, the motion is strictly rejected:
  `Trajectory segment crosses boundary edge into retained material`.
* Continuous swept cutter clearance is enforced along the entire segment via closed-form segment-to-segment distance:
  $$\min_{t \in [0, 1], s \in [0, 1]} \| (1-t) T_0 + t T_1 - ((1-s) B_i + s B_{i+1}) \| \ge R_{\text{tool}} + \text{allowance} - \epsilon$$

---

## 3. Fixture Verification & Tool Envelope Disclosures

### 3.1 3D AABB Slab Intersection Collision Detection
Workholding fixture bounding boxes are verified via the 3D Cyrus-Beck / Kay-Kajiya slab algorithm in [`StockDimensions.intersectsSegment`](file:///e:/MultiPhysics/src/main/java/cam/model/StockDimensions.java):
$$t_{\min} = \max_{i \in \{x, y, z\}} \min\left(\frac{B_{i,\min} - P_{1,i}}{d_i}, \frac{B_{i,\max} - P_{1,i}}{d_i}\right)$$
$$t_{\max} = \min_{i \in \{x, y, z\}} \max\left(\frac{B_{i,\min} - P_{1,i}}{d_i}, \frac{B_{i,\max} - P_{1,i}}{d_i}\right)$$
An intersection occurs if $\max(0, t_{\min}) \le \min(1, t_{\max})$.

* Both rapid positioning moves (`G00`) and cutting motions (`G01`, `G81`-`G83`) are checked against `setup.fixtureBounds()`.
* Intersecting moves are rejected with `NcFindingCode.FIXTURE_COLLISION_DETECTED` (Severity: `ERROR`).
* Malformed or inverted bounds ($X_{\min} \ge X_{\max}$ or $V \le 0$) are rejected with `NcFindingCode.INVALID_FIXTURE_BOUNDS` (Severity: `ERROR`).

### 3.2 Explicit Disclosures for Unmodeled Envelopes
To avoid misleading users into believing a clean preflight report guarantees tool holder or machine clearance:
1. `TOOL_HOLDER_CLEARANCE_UNVERIFIED`: Tool holder and spindle geometry is not modeled; tool holder clearance is unverified.
2. `MACHINE_ENVELOPE_UNVERIFIED`: Machine travel limits, table bounds, and enclosure envelope are not modeled; kinematics are unverified.
3. `FIXTURE_CLEARANCE_UNVERIFIED`: Fixture bounds missing or unconfigured in setup; fixture safety unverified.

---

## 4. Milestone 8.8 Verification Test Matrix

The Milestone 8.8 test suite (`TestMilestone8_8_ControllerSemanticsAudit.java`) executed 51 direct assertions across 5 core categories:

| Section | Audit Focus | Assertions | Status |
| :--- | :--- | :---: | :---: |
| **1. Controller Boundaries & Unit Modes** | Unit mode mismatch (`G20`/`G21`), unsupported syntax rejection (`M98`, `G68`, `G41`), modal command conflicts | 8 | **PASS** |
| **2. Coordinate Semantics & WCS/G53/G43** | WCS resolution requirement (`G54`), G53 machine retract non-modal safety, tool length offset matching (`G43 H1` vs `T1`) | 9 | **PASS** |
| **3. Continuous Trajectory Geometry** | U-shape and W-shape void crossings, winding invariance (CCW vs CW), narrow channel capsule clearances | 14 | **PASS** |
| **4. Fixture Bounds & Unmodeled Notices** | 3D AABB rapid & cutting collision detection, invalid bounds rejection, holder & machine envelope disclosures | 11 | **PASS** |
| **5. Fail-Closed Export & Determinism** | Preflight collision fails operation plan export, 30-run bit-identical postprocessing and verification determinism | 9 | **PASS** |
| **Total** | | **51** | **PASS** |

---

## 5. Cumulative CAM Regression Battery Status

All 17 regression test suites across Astra CAM executed with zero failures:

```
[Suite 1]  Milestone 2 CAM Foundation .................  73 / 73  PASS (0 failed)
[Suite 2]  Milestone 3 Deep Hole Drilling .............  56 / 56  PASS (0 failed)
[Suite 3]  Milestone 4 Prismatic Roughing .............  85 / 85  PASS (0 failed)
[Suite 4]  Milestone 6 NC Postprocessor ...............  72 / 72  PASS (0 failed)
[Suite 5]  Milestone 6.1 NC Hardening .................  52 / 52  PASS (0 failed)
[Suite 6]  Milestone 6.2 NC Verification ..............  57 / 57  PASS (0 failed)
[Suite 7]  Milestone 6.3 NC Adversarial Audit .........  65 / 65  PASS (0 failed)
[Suite 8]  Milestone 6.4 NC Contract Review ...........  68 / 68  PASS (0 failed)
[Suite 9]  Milestone 7 CAM Plan & Sequencing ..........  85 / 85  PASS (0 failed)
[Suite 10] Milestone 8.1 2D Contour Geometry ..........  82 / 82  PASS (0 failed)
[Suite 11] Milestone 8.2 Lead-In/Lead-Out .............  80 / 80  PASS (0 failed)
[Suite 12] Milestone 8.3 2D Contour Roughing ..........  78 / 78  PASS (0 failed)
[Suite 13] Milestone 8.4 Contour Boundary Oracle ......  85 / 85  PASS (0 failed)
[Suite 14] Milestone 8.5 NC Contract Audit ............  39 / 39  PASS (0 failed)
[Suite 15] Milestone 8.6 Contract Closure ............. 104 / 104 PASS (0 failed)
[Suite 16] Milestone 8.7 Trajectory Verification ......  74 / 74  PASS (0 failed)
[Suite 17] Milestone 8.8 Controller Semantics Audit ...  51 / 51  PASS (0 failed)
================================================================================
Cumulative CAM Assertions: 1,146 / 1,146 Passed (17 / 17 Suites, 0 Failures)
```

---

## 6. Physical Safety Boundary Disclaimer

> [!WARNING]
> **Physical CNC Safety Boundary**
> Software verification and geometric trajectory checks within Astra CAM are algorithmic validations of the emitted G-code model. They **do not replace**:
> 1. Controller-specific simulation or spindle dry-run verification.
> 2. Physical machine work coordinate zeroing and dial-indicator touch-off.
> 3. Tool length and diameter presetting in the physical machine controller registers.
> 4. Physical workholding, vise jaw, toe clamp, and fixture clearance inspection.
> 5. Operator-supervised first-article dry run with feed hold override enabled.
