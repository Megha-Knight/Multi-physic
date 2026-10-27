# Engineering Report: Milestone 8.5 — NC Preflight and Contour-Finishing Contract Audit

## Executive Summary

Milestone 8, Phase 8.5 executes an exhaustive contract audit and preflight verification across the entire 2.5D contour-finishing pipeline in Astra CAM (`CamContourOffsetEngine`, `CamLeadEngine`, `ContourFinishingOperation`, `ToolpathValidator`, `Iso6983Postprocessor`, `NcProgramVerifier`, and `CamPlanExecutor`).

Rather than prematurely expanding new machining features, Phase 8.5 rigorously hardens and validates the mathematical and physical boundary between the newly integrated geometry/lead engine and the NC postprocessor/verifier. This audit resolves all three review risks raised in the Milestone 8.4 review:
1. **Mathematical Swept-Cutter Footprint Validation**: Replaced vertex/centerline containment checks with continuous envelope-to-boundary distance verification ($d \ge R_{\text{tool}} + \text{allowance} - 10^{-3}\,\text{mm}$), resolving corner lead-transition gouges via automated flat-edge midpoint entry.
2. **Stock-Envelope & Fixture Clearance Contract**: Established rigorous formal differentiation between tool-center trajectory, cutter swept footprint, nominal part boundary, raw stock prism, and workholding fixture avoidance.
3. **Independent NC Semantics & Modal State Preflight**: Audited independent parsing, modal group tracking, motion sequencing, safe vertical-before-horizontal retracts, units, coordinate systems, spindle/feed preconditions, tool length offsets, and strict compensation policy (zero `G41`/`G42`/`G40`).

All **148 / 148** Milestone 8.5 assertions passed with zero regressions. Cumulative project assertions now stand at **1,340 / 1,340 passed (0 failures)** (803 CAM assertions + 537 CAD baseline assertions).

---

## 1. Deep Dive: Investigation of Reviewer Issues

### Issue 1: Continuous Swept-Cutter Footprint Validation
* **Mathematical Gap Identified**: `ToolpathValidator.validateContourFinishing(...)` previously evaluated `partBoundary.contains(pt)` on centerline samples. For outside profiling, a center point outside the boundary could still have its cutter envelope penetrate into nominal part material if $d < R_{\text{tool}}$. Furthermore, in closed inside pockets, placing arc leads at sharp corner vertices swung the cutter disc into adjacent pocket walls.
* **Continuous Footprint Implementation**: `ToolpathValidator` now mathematically evaluates:
  $$\text{dist} = \min_{s \in \text{boundary}} \text{dist}(s, \mathbf{p}(t))$$
  $$\text{reqClearance} = R_{\text{tool}} + \text{finishStockAllowance}$$
  $$\text{Violates if: } \text{dist} < \text{reqClearance} - 10^{-3}\,\text{mm}$$
* **Tangential Contact vs. Penetration**:
  * Exact tangential contact ($d = R_{\text{tool}}$) is mathematically approved.
  * Sub-boundary penetration ($d < R_{\text{tool}} - 10^{-3}\,\text{mm}$) is rejected fail-closed with explicit diagnostic reporting.
  * Stock allowance encroachment ($R_{\text{tool}} \le d < R_{\text{tool}} + \text{allowance} - 10^{-3}$) is strictly rejected.
* **Lead-to-Contour Corner Gouge Resolution**: In `ContourFinishingOperation.java`, closed contours with lead moves now execute `resolveEffContour(...)`. Rather than placing lead transitions on sharp corner vertices ($V_0$), the operation automatically places the entry/exit point on the midpoint of the longest straight segment ($M_0$). At $M_0$, incoming and outgoing tangents are collinear ($180^\circ$ flat), providing maximum clearance from adjacent corner walls and completely eliminating transition gouges.
* **Scale Invariance**: Tested across micro ($500\,\mu\text{m}$ pocket with $100\,\mu\text{m}$ cutter) and macro ($1.5\,\text{m}$ boss with $50\,\text{mm}$ cutter) scales, confirming numerical stability and robust bounds.

---

### Issue 2: Stock-Envelope and Workholding Boundary Contract

The audit establishes formal contractual boundaries between five distinct geometric domains:

| Domain | Representation | Verification Mechanism | Invariant |
| :--- | :--- | :--- | :--- |
| **Tool-Center Trajectory** | Axis path $\mathbf{p}(t) = (X_c, Y_c, Z_c)$ | `ContourFinishingOperation.validate(setup)` | Must remain within raw stock prism $[X_{\min}, X_{\max}] \times [Y_{\min}, Y_{\max}]$ |
| **Cutter Swept Footprint** | Cylinder volume $\mathcal{V} = \bigcup_t \mathcal{B}(\mathbf{p}(t), R_{\text{tool}})$ | `ToolpathValidator.validateContourFinishing(...)` | Envelope boundary distance $\ge R_{\text{tool}} + \text{allowance}$ |
| **Nominal Part Boundary** | Workpiece contour $\mathcal{C}$ | `CamContourOffsetEngine` & `ToolpathValidator` | Zero gouging; stock allowance preserved across all cuts |
| **Raw Stock Envelope** | Stock prism $\mathcal{S}$ | `setup.getStock()` & `NcMotionVerifier` | Trajectory coordinates bounded within $[Z_{\text{bottom}}, Z_{\text{top}}]$ |
| **Fixture Clearance** | Clamps, vises, jaws | `SafetyPlane.getClearanceZ()` ($25.0\,\text{mm}$) | Rapid traverses between levels/features occur strictly at clearance plane |

*A simple bounding-box check cannot guarantee fixture avoidance; live CNC machining strictly requires workholding clearance plane routing ($Z = 25.0\,\text{mm}$) and supervised single-block dry runs.*

---

### Issue 3: Independent NC Semantics & Modal State Preflight

Astra CAM maintains complete headless decoupling between postprocessing emission (`Iso6983Postprocessor`) and preflight verification (`NcProgramVerifier`). The verifier independently parses raw G-code text and tracks modal interpreter state:

1. **Units & Distance Mode**: Enforces `G21` metric mode and `G90` absolute positioning before any motion block.
2. **Plane & Work Offset**: Verifies `G17` XY plane selection and `G54` work coordinate system before cutting.
3. **Tool Selection & Length Offset**: Confirms `T... M06` tool change followed by `G43 H...` tool length compensation. Validates that `G43` is emitted at safe height above stock top ($Z \ge Z_{\text{stock\_top}}$) and that register $H$ matches tool $T$.
4. **Compensation Policy**: Strictly validates zero controller-side cutter radius compensation (`G41`, `G42`, `G40`). Astra CAM uses software centerline toolpath offsetting; emitting controller cutter compensation would result in catastrophic double-compensation.
5. **Spindle & Feed Preconditions**: Enforces positive spindle speed (`M03 S...`) and positive feed rate (`F...`) on all cutting blocks (`G01`).
6. **Safe Motion Sequencing**: Audits vertical-before-horizontal retract sequencing. When retracting between depth levels or transitioning operations, toolpaths retract vertically to `approachZ` and then vertically to `clearanceZ` ($25.0\,\text{mm}$) before any horizontal rapid traverse ($G00$).
7. **Compensation Cancellation & Termination**: Enforces `G49` cancellation before `M30` program end termination.

---

## 2. Geometry-to-NC Consistency & Rounding Safety

* **Coordinate Preservation**: Every linear cutting segment in the validated toolpath corresponds 1-to-1 with a posted `G01` block in the NC stream. Endpoint coordinates match within $0.001\,\text{mm}$.
* **Rounding Conservatism**:
  * ISO 6983 postprocessing formats coordinates to 3 decimal places ($1\,\mu\text{m}$ resolution).
  * The maximum 2D Euclidean rounding error is:
    $$\epsilon_{\text{round}} = \sqrt{(0.0005)^2 + (0.0005)^2} \approx 0.000707\,\text{mm} < 0.0010\,\text{mm}$$
  * Because the software clearance tolerance margin is $1.0\,\mu\text{m}$ ($10^{-3}\,\text{mm}$), coordinate rounding can never invalidate clearances or cause part gouging.

---

## 3. Adversarial Regression Test Matrix

The Milestone 8.5 adversarial suite (`TestMilestone8_5_NcContractAudit.java`) exercises 148 assertions across 7 audit sections:

| Audit Section | Test Description | Expected Behavior | Status |
| :--- | :--- | :--- | :--- |
| **1a. Tangential vs Penetration** | Tangential contact ($d = R$) vs sub-boundary penetration ($d = R - 0.005$) | Tangent passes; sub-boundary rejected with clearance diagnostic | **PASS** |
| **1a. Allowance Enforcement** | Toolpath encroaching within finish stock margin | Rejected fail-closed | **PASS** |
| **1b. Concave & Transitions** | L-boss with concave reflex corner and circular arc leads | Footprint clears boundary and lead transitions | **PASS** |
| **1c. Winding Invariance** | CCW vs CW outside and inside finishing passes | Identical cut distances, valid swept footprints | **PASS** |
| **1d. Scale Tolerance** | $500\,\mu\text{m}$ micro-pocket and $1.5\,\text{m}$ macro-part | Stable numerical validation at micro and macro scales | **PASS** |
| **2a. Stock Envelope Contract** | Generous stock containing toolpath trajectory | Bounded trajectory coordinates pass validation | **PASS** |
| **2b. Stock Envelope Violation** | Constrained stock smaller than offset trajectory | Rejected in `op.validate(setup)` with envelope diagnostic | **PASS** |
| **3a. Independent NC Semantics** | G21, G90, G54, T1 M06, G43 H1, G49, M30, 0 G41/G42/G40 | Independent verifier APPROVES program | **PASS** |
| **3b. Retract Sequencing** | Multi-level finishing rapid repositioning | All horizontal rapids occur strictly at $Z = 25.0\,\text{mm}$ | **PASS** |
| **4a. Geo-to-NC Consistency** | Segment count and endpoint coordinates | 100% coordinate consistency within $0.001\,\text{mm}$ | **PASS** |
| **4b. Rounding Conservatism** | 3-decimal rounding bound ($0.0007\,\text{mm} < 0.0010\,\text{mm}$) | Rounding cannot cause gouging or clearance loss | **PASS** |
| **5a. Adversarial Missing G49** | Program terminating without `G49` | Flagged `TOOL_LENGTH_NOT_CANCELLED`; unapproved | **PASS** |
| **5b. Adversarial Rapid Crash** | Rapid traverse below stock bottom ($Z = -35.0\,\text{mm}$) | Flagged `SAFETY_CLEARANCE_VIOLATION`; unapproved | **PASS** |
| **5c. Adversarial No Spindle** | Cutting motion (`G01`) without active spindle | Flagged `SPINDLE_NOT_STARTED`; unapproved | **PASS** |
| **5d. Adversarial Unsafe G43** | Tool length comp activated below stock top ($Z = -2.0\,\text{mm}$) | Flagged `UNSAFE_COMPENSATION_POSITION`; unapproved | **PASS** |
| **5e. Adversarial Offset Mismatch** | Tool length register mismatch (`H2` for `T1`) | Flagged `TOOL_OFFSET_MISMATCH`; unapproved | **PASS** |
| **6a. Geometry Failure Gating** | Collapsing narrow channel toolpath in plan | Plan fails validation; `isExportEligible() == false` | **PASS** |
| **6a. Export Gate Enforcement** | Calling `exportNc()` on unapproved result | Strictly throws `IllegalStateException` | **PASS** |
| **6b. Preflight Failure Gating** | Plan failing NC preflight verification | `isExportEligible() == false`; export blocked | **PASS** |
| **7a. Multi-Op Plan Audit** | Full plan (Spot Drill + Pocket Rough + Contour Finish) | Execution `COMPLETED`, preflight `APPROVED` | **PASS** |
| **7b. Determinism Audit** | 30 repeated postprocessor executions | Bit-identical G-code output across all runs | **PASS** |

---

## 4. Failure Gating & Export Pipeline

In Astra CAM, export eligibility is strictly gated by immutable verification contracts:
```java
this.exportEligible = this.status.isSuccess() && ncProgram != null
    && verificationReport != null && verificationReport.isApproved();
```
If an operation experiences geometric collapse, part boundary gouging, stock envelope escape, or preflight rule violations, `isExportEligible()` returns `false`. Any call to `exportNc(destination, overwrite)` immediately throws an `IllegalStateException`, guaranteeing that unverified or unsafe G-code cannot be exported to machine media.

---

## 5. Architectural & Repository Constraints Compliance

* **JDK 25 Clean Build**: Maven clean compile succeeds with release flag `25` (`[INFO] BUILD SUCCESS`).
* **Source Line Limit**: All 308 production Java source files under `src/main/java/` strictly comply with the $\le 200$ physical line limit (longest file is `shape_item_ui_main.java` at 198 lines; `ContourFinishingOperation.java` is 198 lines; `ToolpathValidator.java` is 164 lines).
* **Headless Architecture**: Zero JavaFX imports or UI dependencies exist in `cam.*` (`Get-ChildItem src/main/java/cam | Select-String 'javafx'` returns 0).
* **Frozen `pom.xml` Hash**: SHA-256 hash verified unchanged:
  `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309`.
* **Clean Working Tree**: Only production files and documentation modified; `src/main/CPP/` preserved and untracked.

---

## 6. Physical CNC Machining Safety Boundary

> [!CAUTION]
> **MANDATORY CNC SAFETY BOUNDARY**
> Successful completion of unit tests, swept-cutter geometric validation, and independent NC preflight verification demonstrates software correctness against internal specification models. It does **NOT** prove physical machining safety on a live CNC machine tool.
>
> Prior to cutting live material on a physical CNC machine, the following steps remain mandatory:
> 1. Controller-specific 3D kinematic simulation including toolholders, spindle head, clamps, vises, and table travel limits.
> 2. Physical verification of workpiece stock dimensions, clamping positions, and work coordinate system (WCS / G54) probe zero points.
> 3. Optical or touch-probe measurement of actual tool lengths ($H$) and diameter offsets on the CNC machine tool.
> 4. Supervised dry run above workpiece level and single-block live execution with feed override controls enabled.
