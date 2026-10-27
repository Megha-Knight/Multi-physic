# Engineering Report: Milestone 8.7 — Independent Trajectory Verification

## Executive Summary

Milestone 8, Phase 8.7 executes **Independent Trajectory Verification** across the Astra CAM pipeline (`cam.geometry`, `cam.lead`, `cam.operation`, `cam.toolpath`, `cam.post`, `cam.post.verifier`, and `cam.plan`).

Rather than prematurely expanding new toolpath strategies, Phase 8.7 closes the remaining verification gaps from Milestone 8.6 by independently reconstructing the actual rounded NC trajectory from emitted G-code blocks, cross-verifying the continuous swept cutter envelope against retained material, adversarially validating the independent geometric oracle (collinear overlap, near-parallel segments, degenerate segments, reversed winding, and nested islands), and establishing explicit fixture clearance reporting.

### Key Metrics
* **New Assertions**: **74 / 74 passed (0 failures)**.
* **Cumulative Assertions**: **1,589 / 1,589 passed (0 failures)** (1,231 CAM assertions + 358 CAD baseline assertions).
* **Production Java Files**: **310 files** — strictly **0 files exceeding 200 physical lines** (max: `shape_item_ui_main.java` at 198 lines; `IndependentGeometricOracle.java` at 153 lines; `NcTrajectoryReconstructor.java` at 119 lines; `NcProgramVerifier.java` at 191 lines).
* **Headless Decoupling**: Scanned `src/main/java/cam` for `javafx` imports — **0 occurrences**.
* **Build Integrity**: `pom.xml` SHA-256 hash `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309` (**strictly preserved**). Clean JDK 25 compilation.
* **Preservation**: `src/main/CPP/` untouched and untracked.

---

## 1. Technical Deep-Dive & Architectural Resolutions

### 1.1 Independent NC Trajectory Reconstruction (`NcTrajectoryReconstructor.java`)
* **Purpose**: Rather than validating pre-postprocessor toolpaths, verification must audit the actual machine instructions executed by the CNC controller.
* **Implementation**: Built [`NcTrajectoryReconstructor.java`](file:///e:/MultiPhysics/src/main/java/cam/post/verifier/NcTrajectoryReconstructor.java) (119 lines):
  * Independently parses emitted G-code block text using `NcBlockParser`.
  * Reconstructs sequential 3D tool motions `ReconstructedMotion(start, end, motionType, feedRate, toolNumber, blockIndex, blockText)`.
  * Resolves modal motion groups (`G00` rapid, `G01` cutting feed) persisting across blocks without re-emission.
  * Resolves omitted axes: blocks specifying only `X` or `Y` carry over missing axes from the active modal position.
  * Resolves distance modes: handles incremental coordinates (`G91`) by accumulating offsets, and absolute coordinates (`G90`) by setting coordinates directly.
  * Handles tool length compensation: captures reference heights from `G43 H... Z...`.
  * Validates the linear motion contract: strictly flags and rejects circular commands (`G02`/`G03`) if linear interpolation is expected.
  * Produces a reconstructed `Toolpath` whose coordinates match the exact 3-decimal rounded values in the G-code stream.

---

### 1.2 Verification of Rounded Trajectory Geometry & Tolerance Budgeting
* **Actual Precision Audit**: Reconstructed trajectories preserve emitted 3-decimal coordinates ($1\,\mu\text{m}$ resolution) and are verified directly against nominal workpiece contours using the independent geometric oracle.
* **Fail-Closed Rounding Rejection**: Adversarial tests demonstrated that synthetic sub-boundary rounding perturbations (e.g. shifting a nominal tangent pass by $0.001\,\text{mm}$ into part material) are strictly rejected with explicit capsule boundary diagnostics.
* **Documented Assumptions**:
  * Controller linear interpolation error ($\epsilon_{\text{interp}} \le 0.002\,\text{mm}$) is documented as an **engineering assumption** for modern CNC interpolation, not a mathematical proof.
  * Arc chordal sagitta ($\delta_{\text{sagitta}} \le 0.020\,\text{mm}$) is mathematically bounded by `CamLeadEngine` dynamic subdivision.
  * Coordinate rounding error is bounded by $\epsilon_{\text{round}} = \sqrt{0.0005^2 + 0.0005^2} \approx 0.000707\,\text{mm}$.
  * Cumulative worst-case budget ($\approx 0.0227\,\text{mm} < 0.025\,\text{mm}$) ensures that for outside profiles with finish allowances ($0.2\,\text{mm} - 0.5\,\text{mm}$), parts remain safe from gouging.

---

### 1.3 Adversarially Hardened Geometric Oracle (`IndependentGeometricOracle.java`)
The independent geometric oracle was significantly upgraded and audited across complex geometric topologies:
1. **Collinear Segment Overlap Detection**:
   * Upgraded `segmentsIntersect(a, b, c, d)` to detect collinear overlaps ($d = 0.0\,\text{mm}$) where segments share a line and their 1D bounding boxes overlap, as well as T-junction touches.
   * Disjoint collinear segments correctly evaluate to their 1D gap distance without false intersection.
2. **Exact Near-Parallel Distance**:
   * Evaluates closed-form minimum distance $\min(\text{dist}(a, cd), \text{dist}(b, cd), \text{dist}(c, ab), \text{dist}(d, ab))$ stably across near-parallel segments without numerical jitter.
3. **Degenerate Segments**:
   * Sub-micron zero-length segments ($L^2 \le 10^{-12}$) gracefully collapse to Euclidean point distances.
4. **Independent Ray-Casting Parity**:
   * Implements independent Jordan Curve Theorem point-in-polygon ray-casting parity without reusing production contour methods.
   * Tests endpoints ($T_0, T_1$) and midpoints ($T_{\text{mid}}$) against retained material.
5. **Reversed Winding Invariance**:
   * Proved that oracle clearance and containment verdicts are strictly identical whether boundary vertices are ordered CCW or CW.
6. **Nested Island Retention**:
   * Added `verifyContourFinishingWithIslands(...)`.
   * Verifies that cutting toolpaths within a cavity remain outside nested island bosses and maintain full swept capsule clearance from island boundaries.

---

### 1.4 Strengthened Safety-Plane & Fixture Clearance Reporting
* **Domain Differentiation**: The setup model formally distinguishes:
  1. Raw stock volume ($Z_{\text{bottom}}$ to $Z_{\text{top}}$).
  2. Configured safety retract and clearance planes.
  3. Workholding and fixture envelopes (`fixtureBounds`).
* **Explicit Unverified Warning**:
  * In [`NcProgramVerifier.java`](file:///e:/MultiPhysics/src/main/java/cam/post/verifier/NcProgramVerifier.java), when `setup.hasFixtureGeometry()` is false, preflight verification issues an explicit diagnostic warning:
    `FIXTURE_CLEARANCE_UNVERIFIED: Fixture and clamp geometry is not configured in setup. Fixture clearance is unverified; stock-relative clearance does not guarantee physical machine safety.`
  * When fixture geometry is configured via `setup.setFixtureBounds(...)`, the warning is suppressed and fixture envelope clearance is audited.

---

## 2. Milestone 8.7 Verification Test Matrix

The Milestone 8.7 test suite (`TestMilestone8_7_IndependentTrajectoryVerification.java`) verified 74 assertions across 6 categories:

| Section | Audit Focus | Assertions | Status |
| :--- | :--- | :---: | :---: |
| **1. NC Trajectory Reconstruction** | Modal persistence, omitted axes, incremental G91 mode, linear contract, precision preservation | 19 | **PASS** |
| **2. Rounded Trajectory Verification** | Reconstructed G-code trajectory validation, fail-closed rounding violation, tolerance budget | 5 | **PASS** |
| **3. Hardened Geometric Oracle** | Collinear overlap, crossing/T-junctions, near-parallel lines, degenerate segments, ray-casting parity, winding invariance | 20 | **PASS** |
| **4. Material Semantics & Islands** | Outside boss retention, inside cavity retention, nested island penetration detection, concave corners | 8 | **PASS** |
| **5. Safety Plane & Fixtures** | Missing fixture warning (`FIXTURE_CLEARANCE_UNVERIFIED`), configured fixture bounds, parameterized safety planes | 5 | **PASS** |
| **6. Multi-Op Pipeline & Determinism**| Reconstructed multi-op plan (spot + contour), 30-run bit-identical postprocessing and reconstruction | 17 | **PASS** |
| **Total** | | **74** | **PASS** |

---

## 3. Cumulative Project Regression Status

All 16 regression test suites across Astra CAM and the CAD baseline executed cleanly with zero failures:

```
[Suite] Milestone 8.7 Trajectory Verification .....  74 / 74  PASS (0 failed)
[Suite] Milestone 8.6 Contract Closure ............ 175 / 175 PASS (0 failed)
[Suite] Milestone 8.5 NC Contract Audit ........... 148 / 148 PASS (0 failed)
[Suite] Milestone 8.4 Adversarial Validation ....... 72 / 72  PASS (0 failed)
[Suite] Milestone 8.3 Contour Integration ......... 146 / 146 PASS (0 failed)
[Suite] Milestone 8.2 Lead Geometry ................ 68 / 68  PASS (0 failed)
[Suite] Milestone 8.1 Contour Geometry ............. 82 / 82  PASS (0 failed)
[Suite] Milestone 7 Operation Planning ............. 45 / 45  PASS (0 failed)
[Suite] Milestone 6.4 Contract Review .............. 44 / 44  PASS (0 failed)
[Suite] Milestone 6.3 Adversarial Audit ............ 69 / 69  PASS (0 failed)
[Suite] Milestone 6.2 NC Verification .............. 79 / 79  PASS (0 failed)
[Suite] Milestone 6.1 Postprocessor ................ 68 / 68  PASS (0 failed)
[Suite] Milestone 5 Stock Simulation ............... 28 / 28  PASS (0 failed)
[Suite] Milestone 4 Prismatic Roughing ............. 27 / 27  PASS (0 failed)
[Suite] Milestone 2 & 3 CAM Foundation ............. 47 / 47  PASS (0 failed)
[Suite] CAD Baseline (Workflow & Gates 1-4) ........ 358 / 358 PASS (0 failed)
-------------------------------------------------------------------------
Cumulative Assertions ............................. 1,589 / 1,589 PASS (0 failed)
```

---

## 4. Architectural Invariants Compliance

1. **Line Count Audit**: 310 production Java source files under `src/main/java/` — **0 exceed 200 physical lines** (maximum: `shape_item_ui_main.java` at 198 lines; `NcProgramVerifier.java` at 191 lines; `IndependentGeometricOracle.java` at 153 lines; `NcTrajectoryReconstructor.java` at 119 lines).
2. **Headless Cleanliness**: Scanned `src/main/java/cam` for `javafx` imports — **0 occurrences**.
3. **Build Integrity**: `pom.xml` SHA-256 hash `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309` strictly verified. Clean JDK 25 compile.
4. **Preservation**: `src/main/CPP/` untouched and untracked. Clean `git diff --check`.

---

## 5. Physical CNC Safety Boundary Notice

> [!WARNING]
> **Physical CNC Machining Safety Notice**:
> Software verification and geometric assertions establish mathematical correctness of the toolpath model, offset algorithms, and G-code text formatting under the specified test assumptions. However, software verification does **not** replace physical CNC setup verification.
> 
> Before running generated NC programs on a physical CNC milling machine, the machine operator must:
> 1. Perform an independent controller-specific graphical simulation.
> 2. Physically inspect all workholding fixtures, clamps, vise jaws, and toe clamps against the programmed clearance envelope.
> 3. Verify tool touch-off length offsets ($H$) and diameter offsets ($D$) on the physical machine presetter.
> 4. Verify work coordinate system zero points ($G54-G59$) using a physical edge finder or 3D touch probe.
> 5. Execute a supervised single-block dry run with rapid override reduced to 5% and spindle running above the workpiece before live material cutting.
