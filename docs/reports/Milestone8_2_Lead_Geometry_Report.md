# Engineering Report: Milestone 8.2 — CAM Lead-In & Lead-Out Geometry

## Executive Summary
Milestone 8, Phase 8.2 establishes a deterministic, pure-Java toolpath lead-in and lead-out geometry engine for CAM finishing in Astra.
Building on the Phase 8.1 contour geometry and offset engine, Phase 8.2 delivers tangent linear and circular arc lead transitions that ensure smooth $C^1$ tangency and endpoint continuity at contour engagement and disengagement.
It incorporates clockwise and counter-clockwise traversals, open vs. closed loop handling, parameter bounds enforcement, and strict part-boundary gouge and penetration rejection.

In accordance with Phase 8.2 milestone constraints, NC controller compensation (`G41`/`G42`/`G40`) emission is kept out of scope until lead geometries are verified in isolation.

---

## 1. Geometric Formulations & Tangency Continuity

### A. Tangent Linear Leads
- **Lead-In**: Approaching contour entry vertex $\mathbf{P}_{\text{in}}$ along unit forward tangent $\mathbf{T}_{\text{in}}$:
  $$\mathbf{P}_{\text{start}} = \mathbf{P}_{\text{in}} - \mathbf{T}_{\text{in}} \cdot L$$
- **Lead-Out**: Departing contour exit vertex $\mathbf{P}_{\text{out}}$ continuing along unit forward tangent $\mathbf{T}_{\text{out}}$:
  $$\mathbf{P}_{\text{end}} = \mathbf{P}_{\text{out}} + \mathbf{T}_{\text{out}} \cdot L$$
- **Continuity**: Velocity direction $\dot{\mathbf{r}}$ across $\mathbf{P}_{\text{in}}$ and $\mathbf{P}_{\text{out}}$ is identical to contour tangent $\mathbf{T}$, eliminating sudden direction discontinuities and surface marks.

### B. Circular Arc Leads
- **Free-Side Normal**: Derived from machining side (`ContourSide`: `LEFT`, `RIGHT`, `INSIDE`, `OUTSIDE`) and contour winding:
  $$\mathbf{N}_{\text{free}} = \begin{cases} (-T_y, T_x) & \text{if free side is LEFT} \\ (T_y, -T_x) & \text{if free side is RIGHT} \end{cases}$$
- **Arc Center**: Located offset into clear space along $\mathbf{N}_{\text{free}}$:
  $$\mathbf{C} = \mathbf{P} + \mathbf{N}_{\text{free}} \cdot R$$
- **Rotational Sense & Sweep**:
  - Sweeps an angular span $\theta \in (0, 180^\circ]$ (default $90^\circ$).
  - Arc touches $\mathbf{P}$ with forward tangent vector $\mathbf{T}$.
  - The rotational direction (CW vs. CCW) matches the turn into/out of $\mathbf{N}_{\text{free}}$ while preserving forward motion.

### C. Perpendicular Linear Leads
- Normal linear plunge/retract extending along $\mathbf{N}_{\text{free}}$:
  $$\mathbf{P}_{\text{in,start}} = \mathbf{P}_{\text{in}} + \mathbf{N}_{\text{free}} \cdot L, \quad \mathbf{P}_{\text{out,end}} = \mathbf{P}_{\text{out}} + \mathbf{N}_{\text{free}} \cdot L$$

---

## 2. Open vs. Closed Contour Handling
- **Open Paths**:
  - Lead-in engages at path start vertex $0$.
  - Toolpath cuts all path segments from $0$ to $N-1$.
  - Lead-out disengages from path termination vertex $N-1$.
- **Closed Boundaries**:
  - Lead-in and lead-out rendezvous at the designated entry vertex (default vertex $0$).
  - Lead-in curves into vertex $0$, traverses the full perimeter $0 \to N-1 \to 0$, and lead-out departs cleanly from vertex $0$.
  - Combined path output is an open sequence of discrete motion vertices suitable for toolpath generation.

---

## 3. Gouge & Part-Boundary Protection
When a part boundary (`partBoundary`) is supplied:
1. **Segment Intersection**: Every linear segment of the lead path is tested against all edges of the part boundary (`seg.intersectsStrict(boundarySeg)`). Any intersection triggers immediate fail-closed rejection.
2. **Interior Penetration**: For closed part boundaries, lead points (excluding the touch point itself) are checked against boundary containment (`part.contains(pt)`). Any lead originating or traveling inside the part is rejected.

---

## 4. Verification Records

### A. Line Count Audit
- **Requirement**: All production Java files under `src/main/java/` strictly $\le 200$ physical lines.
- **Tool**: `verify_line_counts.ps1`
- **Result**:
  - Total production Java files: **307**
  - Violations (> 200 lines): **0**
  - `CamLeadEngine.java`: 164 lines
  - `CamLeadMove.java`: 56 lines
  - `CamLeadConfig.java`: 52 lines
  - `CamLeadResult.java`: 49 lines
  - `CamLeadType.java`: 11 lines

### B. Pure Java / Headless Architecture Audit
- **Requirement**: Headless pure Java in `cam.*`, zero JavaFX imports.
- **Command**: `Get-ChildItem -Path src/main/java/cam -Recurse -Filter *.java | Select-String -Pattern 'javafx' | Measure-Object`
- **Result**: Count = **0** (Zero JavaFX imports).

### C. Frozen pom.xml Integrity
- **SHA-256**: `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309` (Identical, untouched).

### D. Maven Compilation
- **JDK**: Microsoft JDK 25.0.4.
- **Command**: `mvn compile`
- **Result**: `[INFO] BUILD SUCCESS` (307 source files compiled cleanly).

### E. Test Suite Assertions Breakdown
| Suite | Description | Assertions Passed | Failures | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Milestone 8.2** | Lead-In & Lead-Out Geometry | **68** | 0 | **PASS** |
| **Milestone 8.1** | Contour Geometry & Offset Engine | **82** | 0 | **PASS** |
| **Milestone 7** | CAM Planning & Sequencing | **52** | 0 | **PASS** |
| **Milestone 6.4** | NC Contract Review & Verification | **34** | 0 | **PASS** |
| **Milestone 6.3** | NC Verifier Adversarial Audit | **42** | 0 | **PASS** |
| **Milestone 6.2** | End-to-End NC Verification | **58** | 0 | **PASS** |
| **Milestone 6.1** | NC Postprocessor Hardening | **68** | 0 | **PASS** |
| **Milestone 5** | Stock-Removal Volumetric Simulation | **28** | 0 | **PASS** |
| **Milestone 4** | 2.5D Prismatic Roughing | **27** | 0 | **PASS** |
| **Milestones 2 & 3**| CAM Foundation & Deep Drilling | **60** | 0 | **PASS** |
| **Production CAD** | Frozen Baseline + Stage 2A + Stage 2B | **537** | 0 | **PASS** |
| **Total Cumulative** | **All System Regression Suites** | **1,056** | **0** | **PASS** |

---

## 5. Physical Safety & Operational Disclaimer
> [!IMPORTANT]
> A valid 2D toolpath, smooth lead tangency, and successful NC preflight verification in software do not establish physical, collision-free machining. Physical machine setup, fixturing, clamp clearances, controller-specific cutter radius compensation lookahead limits, tool stickout, and a supervised dry run in single-block mode remain strictly mandatory before physical cutting.
