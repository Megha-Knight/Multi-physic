# Milestone 9.1: Bounded 3D Waterline (Z-Level) Finishing Report
**Constant-Z Contour Slicing, Closed-Loop Assembly, Cutter Compensation, and NC Trajectory Verification**

- **Repository**: `E:\MultiPhysics`
- **Branch**: `main`
- **Baseline Checkpoint**: `bbb5d2e` (`feat(milestone-9.0.2): close 3d geometry verification and cutter contract`)
- **Suggested Commit**: `feat(milestone-9.1): add bounded 3d waterline finishing`
- **Status**: **PASS (Software Verification within Declared Domain; Physical CNC Safety Non-Certified)**

---

## 1. Executive Summary & Objective

Milestone 9.1 extends Astra CAM with a bounded, deterministic **3D Waterline (Z-Level) Finishing** engine. While parallel planar finishing (Milestones 9.0–9.0.2) excels on shallow and horizontal surfaces, flat-endmill machining on steep and vertical faces leaves substantial scallop cusps ($h_{\text{scallop}} = w \tan\theta$). Waterline finishing resolves this geometric gap by generating horizontal slicing contours at constant Z levels, delivering optimal surface finish and controlled chip load on vertical and steep surfaces.

### Core Architectural Additions:
1. **Analytical Triangle-Plane Slicing**:
   [`CamTriangle3D.intersectZPlane`](file:///E:/MultiPhysics/src/main/java/cam/geometry/CamTriangle3D.java) computes exact intersection segments between arbitrary 3D triangular facets and horizontal slicing planes $Z = z_0$. Purely horizontal facets ($N_x \approx 0, N_y \approx 0$) are cleanly filtered, apex tangent contacts ($< 2$ distinct points) produce no spurious zero-length segments, and coplanar edges are detected without floating-point division by zero.
2. **Topological Contour Assembly & Collinear Simplification**:
   [`CamWaterlineSlicer`](file:///E:/MultiPhysics/src/main/java/cam/geometry/CamWaterlineSlicer.java) aggregates raw intersection segments across all mesh triangles, deduplicates coincident segments within tolerance $\epsilon = 10^{-5}\,\text{mm}$, chains segments into closed loops and open boundary polylines, and simplifies intermediate collinear vertices created by facet diagonals. Bounded execution limits prevent unbounded loops on pathological meshes ($\le 50,000$ segments, $\le 5,000$ contours).
3. **Cutter-Radius Compensation & Feasibility**:
   [`WaterlineFinishingOperation`](file:///E:/MultiPhysics/src/main/java/cam/operation/WaterlineFinishingOperation.java) integrates with [`CamContourOffsetEngine`](file:///E:/MultiPhysics/src/main/java/cam/geometry/CamContourOffsetEngine.java) to apply radial tool offset. Supports flat endmills with outward boundary offset (`ContourSide.OUTSIDE`), pocket cavity finishing (`ContourSide.INSIDE`), and uncompensated centerline tracing. Features narrower than the tool diameter fail closed or are safely omitted if offset collapses. Ball-endmill 3D surface contact compensation is deferred to a future milestone.
4. **Motion Planning & Trajectory Verification**:
   Slices are generated monotonically top-down ($Z_{\text{top}} \to Z_{\text{bottom}}$). Contour traversal incorporates safe retracts to the configured clearance plane between levels and islands, vertical local clearance plunges ($+2.0\,\text{mm}$), feed moves along contour vertices, and closed-loop returns. Trajectories postprocess cleanly to ISO 6983 G-code and verify through [`NcProgramVerifier`](file:///E:/MultiPhysics/src/main/java/cam/post/verifier/NcProgramVerifier.java) and [`NcTrajectoryReconstructor`](file:///E:/MultiPhysics/src/main/java/cam/post/verifier/NcTrajectoryReconstructor.java).

All 24 test suites in the complete CAM regression battery were freshly compiled and executed on JDK 25. **All 24 suites passed with 0 errors and 0 failures, executing exactly 1,370 direct assertions.**

---

## 2. Mathematical Formulation & Slicing Algorithm

### 2.1 Triangle-Plane Intersection
Let triangle $T = (\mathbf{a}, \mathbf{b}, \mathbf{c})$ have vertices $\mathbf{p}_i = (x_i, y_i, z_i)$.
For a slicing plane $Z = z_0$:
1. If $z_0 < \min(z_a, z_b, z_c) - \epsilon$ or $z_0 > \max(z_a, z_b, z_c) + \epsilon$, the triangle does not intersect $Z = z_0$.
2. For each oriented edge $\mathbf{e} = (\mathbf{p}_1, \mathbf{p}_2)$:
   - If $|z_2 - z_1| < \epsilon$ and $|z_1 - z_0| < \epsilon$: the entire edge lies on the plane; both endpoints $\mathbf{p}_1, \mathbf{p}_2$ are added.
   - If $(z_1 \le z_0 \le z_2)$ or $(z_2 \le z_0 \le z_1)$ with $|z_2 - z_1| \ge \epsilon$:
     $$t = \text{clamp}\left(\frac{z_0 - z_1}{z_2 - z_1}, 0.0, 1.0\right), \quad \mathbf{p}(t) = \mathbf{p}_1 + t(\mathbf{p}_2 - \mathbf{p}_1)$$
3. Duplicate points within $\epsilon = 10^{-6}\,\text{mm}$ are filtered out:
   - If $|unique| < 2$: single-vertex touch or no intersection (empty).
   - If $|unique| == 2$ and $\|\mathbf{p}_A - \mathbf{p}_B\| \ge 10^{-6}\,\text{mm}$: valid intersection segment $S = [\mathbf{p}_A, \mathbf{p}_B]$.

### 2.2 Contour Assembly & Collinear Simplification
Raw segments from all intersecting triangles are collected into an unvisited pool:
1. **Deduplication**: Reverse-oriented identical segments (e.g. from adjacent counter-facing triangles) are merged.
2. **Greedy Chaining**: Endpoints are matched within connectivity tolerance $\epsilon_{\text{conn}} = 10^{-5}\,\text{mm}$. If the tip connects back to the chain start, it is marked as a closed loop; otherwise it forms an open boundary chain.
3. **Collinear Simplification**: For any 3 consecutive vertices $\mathbf{p}_1, \mathbf{p}_2, \mathbf{p}_3$, if the 2D cross product $|(x_2 - x_1)(y_3 - y_2) - (y_2 - y_1)(x_3 - x_2)| < 10^{-5}$ and $\|\mathbf{p}_1 - \mathbf{p}_2\| + \|\mathbf{p}_2 - \mathbf{p}_3\| \approx \|\mathbf{p}_1 - \mathbf{p}_3\|$, vertex $\mathbf{p}_2$ is redundant and removed. This converts faceted triangulated planar faces into minimal polygonal perimeters (e.g. 8-vertex square prism slices simplify to 4 corner vertices).

---

## 3. Independent Verification Oracle Comparison

To guarantee independent validation without circular dependencies, [`IndependentSlicingOracle`](file:///E:/MultiPhysics_Supporting_Files/scratch/TestMilestone9_1_WaterlineFinishing.java) evaluates triangle-plane intersections directly from raw floating-point vertex arrays using unshared parametric math:

- **Inclined Ramp Slicing ($Z = 0.5 X$)**:
  - Slicing at $Z = 4.0\,\text{mm}$ across $Y \in [0, 15]$ produced an open contour of total length $15.0000\,\text{mm}$ with $X \equiv 8.0000\,\text{mm}$.
  - Independent oracle total segment length: $15.0000\,\text{mm}$ ($\Delta = 0.0\,\text{mm}$).
- **Faceted Cylinder Mesh ($R = 10.0\,\text{mm}$, 8 sides)**:
  - Slicing at $Z = 5.0\,\text{mm}$ produced a closed loop with exactly 8 vertices.
  - Every vertex was verified at Euclidean distance $\sqrt{x^2 + y^2} = 10.0000\,\text{mm} \pm 10^{-4}\,\text{mm}$.
- **Prism Cross-Section**:
  - $10 \times 10\,\text{mm}$ square prism produced closed loop with area $100.0000\,\text{mm}^2$ and perimeter $40.0000\,\text{mm}$.

---

## 4. Test Matrix Coverage (24 Scenarios)

The dedicated test suite [`TestMilestone9_1_WaterlineFinishing.java`](file:///E:/MultiPhysics_Supporting_Files/scratch/TestMilestone9_1_WaterlineFinishing.java) evaluates all 24 required technical scenarios:

| # | Test Scenario | Verified Behavior | Direct Assertions | Result |
| :---: | :--- | :--- | :---: | :---: |
| 1 | Horizontal Plane | Produces empty waterline contour (no vertical wall); plane above/below empty | 3 | **PASS** |
| 2 | Vertical Wall | Slices prism into closed loop, 4 corner vertices, area = $100\,\text{mm}^2$ | 4 | **PASS** |
| 3 | Inclined Plane & Oracle | Slices ramp at Z=4, matches independent parametric oracle length ($15\,\text{mm}$) | 4 | **PASS** |
| 4 | Stepped Surface | Multi-level steps produce independent contours; upper area < lower area | 3 | **PASS** |
| 5 | Cylindrical Faceted Mesh | 8-sided faceted cylinder yields closed loop; all vertices on $R=10\,\text{mm}$ | 4 | **PASS** |
| 6 | Closed Contour Loop | Box slice confirms closed loop with exact perimeter $2(15+20) = 70\,\text{mm}$ | 3 | **PASS** |
| 7 | Open Boundary Chain | Single triangle produces open segment without erroneous loop closure | 2 | **PASS** |
| 8 | Disconnected Islands | Two separate pillars sliced simultaneously produce 2 distinct closed contours | 2 | **PASS** |
| 9 | Hole / Internal Boundary | Hollow block produces 2 closed boundaries (outer wall and inner hole) | 1 | **PASS** |
| 10 | Shared Apex Vertex | Slicing through apex vertex produces 0 spurious zero-length segments | 1 | **PASS** |
| 11 | Edge on Slicing Plane | Horizontal edge coincident with Z plane detected as valid 10mm segment | 2 | **PASS** |
| 12 | Tangent Triangle Apex | Slicing slightly above apex or below base returns empty list | 2 | **PASS** |
| 13 | Degenerate Triangles | Collinear zero-area triangles rejected fail-closed at construction | 1 | **PASS** |
| 14 | Duplicate Segments | Overlapping coincident facets deduplicated to single contour | 1 | **PASS** |
| 15 | Branching Robustness | Slicer processes manifold and non-branching edges cleanly | 1 | **PASS** |
| 16 | Invalid Z Bounds | Rejects topZ < bottomZ, stepdown $\le 0$, and out-of-domain bounds | 3 | **PASS** |
| 17 | Non-Finite Coordinates | Rejects NaN or infinite coordinates fail-closed | 1 | **PASS** |
| 18 | Very Steep Slopes | Slices $80^\circ$ slope ($\tan 80^\circ \approx 5.67$) cleanly at multiple levels | 2 | **PASS** |
| 19 | Narrow Features | Outside offset on $4\times 4\,\text{mm}$ pillar with $R=5\,\text{mm}$ tool succeeds | 1 | **PASS** |
| 20 | Safe Linking & Retracts | Retracts between levels verified; all horizontal rapids at clearance plane | 2 | **PASS** |
| 21 | Fixture Safety Gate | Program approved by verifier; injected fixture collision rejected fail-closed | 2 | **PASS** |
| 22 | NC Trajectory Reconstruct | G-code parsed by `NcTrajectoryReconstructor`; all coordinates strictly finite | 3 | **PASS** |
| 23 | 30-Run Determinism | 30 repeated postprocessing runs produce 100% bit-identical G-code | 1 | **PASS** |
| 24 | Bounded Execution | Level limit enforcement rejects runaway stepdown ($0.0001\,\text{mm}$) | 1 | **PASS** |
| **TOTAL** | **Milestone 9.1 Suite** | **Comprehensive Waterline Finishing Verification** | **50** | **PASS** |

---

## 5. Complete CAM Regression Battery Results (24 Suites)

All 24 test suites executed cleanly under JDK 25:

| # | Suite Script | Functionality | Exit | Direct Assertions | Status |
| :---: | :--- | :--- | :---: | :---: | :---: |
| 1 | `run_test_cam_foundation.ps1` | CAM Core Models, Points, Vectors | 0 | 60 | **PASS** |
| 2 | `run_test_multi_hole.ps1` | Multi-Hole Operation & Sorting | 0 | 12 | **PASS** |
| 3 | `run_test_milestone4_roughing.ps1` | Prismatic Roughing & Toolpath Generation | 0 | 27 | **PASS** |
| 4 | `run_test_milestone5_simulation.ps1` | Material Removal Simulation & Voxels | 0 | 28 | **PASS** |
| 5 | `run_test_milestone6_postprocessor.ps1` | ISO 6983 Postprocessor & Dialect | 0 | 68 | **PASS** |
| 6 | `run_test_milestone62_verification.ps1` | Preflight Verification & State Reconstruction | 0 | 58 | **PASS** |
| 7 | `run_test_milestone63_adversarial_audit.ps1` | Adversarial Syntax & Boundary Invariants | 0 | 42 | **PASS** |
| 8 | `run_test_milestone64_contract_review.ps1` | G53/G28 Semantics & Contract Guarantees | 0 | 34 | **PASS** |
| 9 | `run_test_milestone7_planning.ps1` | Operation Sequencing & Tool Selection | 0 | 52 | **PASS** |
| 10 | `run_test_milestone8_geometry.ps1` | 2D Polygon Offsetting & Bounding Checks | 0 | 82 | **PASS** |
| 11 | `run_test_milestone8_2_leads.ps1` | Lead-In / Lead-Out Tangent Geometry | 0 | 68 | **PASS** |
| 12 | `run_test_milestone8_3_integration.ps1` | Integrated Workflow Operations | 0 | 42 | **PASS** |
| 13 | `run_test_milestone8_4_adversarial.ps1` | Adversarial Geometry & Degenerate Polygons | 0 | 72 | **PASS** |
| 14 | `run_test_milestone8_5_nc_audit.ps1` | NC Postprocessor Dialect Audit | 0 | 148 | **PASS** |
| 15 | `run_test_milestone8_6_contract_closure.ps1` | Verification Contract & Error Codes | 0 | 175 | **PASS** |
| 16 | `run_test_milestone8_7_trajectory_verification.ps1`| Independent Trajectory Oracle Validation | 0 | 74 | **PASS** |
| 17 | `run_test_milestone8_8_semantics_audit.ps1` | Fanuc Dialect & Coordinate Frame Audit | 0 | 51 | **PASS** |
| 18 | `run_test_milestone8_9_swept_envelope_audit.ps1` | Swept Fixture Envelope Clearance | 0 | 53 | **PASS** |
| 19 | `run_test_milestone8_10_collision_audit.ps1`| Euclidean Fixture Geometry & Motion Audit | 0 | 44 | **PASS** |
| 20 | `run_test_milestone8_11_contract_closure.ps1`| Canned Cycles & Swept Cylinder Audit | 0 | 27 | **PASS** |
| 21 | `run_test_milestone9_0_planar_finishing.ps1`| Bounded 3D Parallel Planar Finishing | 0 | 35 | **PASS** |
| 22 | `run_test_milestone9_0_1_geometry_audit.ps1`| M9.0.1 Geometry, Chord & Cutter Audit | 0 | 32 | **PASS** |
| 23 | `run_test_milestone9_0_2_verification_closure.ps1`| M9.0.2 Independent Oracle & Closure Audit | 0 | 36 | **PASS** |
| 24 | `run_test_milestone9_1_waterline_finishing.ps1`| **M9.1 Bounded 3D Waterline Finishing** | 0 | **50** | **PASS** |
| **TOTAL** | **24 Regression Suites** | **Complete Astra CAM Regression Battery** | **0** | **1,370** | **PASS** |

---

## 6. Architectural Invariant Compliance

1. **Frozen POM Hash**:
   - `pom.xml` SHA-256: `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309` (strictly verified unchanged).
2. **Production Line Limits**:
   - All 315 production Java files strictly contain $\le 200$ physical lines:
     - [`CamTriangle3D.java`](file:///E:/MultiPhysics/src/main/java/cam/geometry/CamTriangle3D.java): 157 lines.
     - [`CamWaterlineSlicer.java`](file:///E:/MultiPhysics/src/main/java/cam/geometry/CamWaterlineSlicer.java): 185 lines.
     - [`WaterlineFinishingOperation.java`](file:///E:/MultiPhysics/src/main/java/cam/operation/WaterlineFinishingOperation.java): 176 lines.
     - [`CamOperationType.java`](file:///E:/MultiPhysics/src/main/java/cam/operation/CamOperationType.java): 26 lines.
3. **Headless CAM Architecture**:
   - Zero `javafx.*` imports in `src/main/java/cam/**`.
4. **Preservation of Untracked Native Assets**:
   - `src/main/CPP/` remains intact and untracked.

---

## 7. Supported Geometry Domain & Known Limitations

1. **Supported Geometry**:
   - Vertical walls, cylindrical/revolved surfaces, prisms, steep-wall ramps, stepped surfaces, hollow boxes/pockets, disconnected islands.
   - Closed boundary loops and open boundary chains.
2. **Cutter Support**:
   - Fully supports flat endmills with 2D horizontal radial offset.
   - Ball-endmill 3D surface contact compensation (where contact occurs at $(u, v, w) = \mathbf{p} + R \mathbf{n}$) is mathematically deferred to a dedicated 3D ball-endmill milestone.
3. **Safety Disclaimer**:
   - Software verification confirms algorithm adherence to geometric contracts within the modeled domain. Real CNC machining requires physical machine setup, tool touch-off, work coordinate zeroing, and operator supervision.
