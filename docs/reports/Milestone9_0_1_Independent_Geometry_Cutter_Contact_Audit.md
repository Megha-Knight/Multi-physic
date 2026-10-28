# Milestone 9.0.1: Independent Geometry & Cutter-Contact Audit Report
**Flat-Endmill Disk Contact, Multi-Sheet Topology Constraints, Chord Intersections, and Clearance Guarantees**

- **Repository**: `E:\MultiPhysics`
- **Branch**: `main`
- **Baseline Checkpoint**: `a913568` (`feat(milestone-9.0): add bounded 3d planar finishing`)
- **Suggested Commit**: `fix(cam): audit planar finishing geometry and cutter contact`
- **Status**: **PASS (Software Verification within Declared Geometric Domain; Physical CNC Safety Non-Certified)**

---

## 1. Executive Summary & Audit Context

Milestone 9.0.1 conducts a comprehensive, test-driven geometry and cutter-contact audit of Astra CAM's 3D parallel planar finishing engine. Following expert review of the initial M9.0 release, five fundamental mathematical and topological aspects were audited and resolved:
1. **Mathematical Completeness of Cutter-Disk Supremum**: Simply checking the center point, the unconstrained steepest ascent point, and triangle vertices was insufficient for cases where a triangle edge cuts across the cutter disk like a chord without containing any vertex or the unconstrained extremum. M9.0.1 derives a closed-form mathematical proof and implements exact circle-edge chord intersections in [`CamTriangulatedSurface.queryCutterDiskHeight`](file:///E:/MultiPhysics/src/main/java/cam/geometry/CamTriangulatedSurface.java), guaranteeing that every possible boundary extremum over the compact convex intersection $\mathcal{D}(x, y, R) \cap T_i$ is evaluated.
2. **Piecewise-Planar Surfaces & Changing Slopes**: Tested concave creases (V-grooves) and convex ridges. Verified that when a flat endmill passes over a concave valley, the cutter disk bridges across the valley at the exact tangential contact height ($Z = 2.0\,\text{mm}$ in test) without gouging the valley bottom ($Z = 0.0\,\text{mm}$).
3. **Boundary Behavior**: Defined explicit contracts for cutter disks extending across surface boundaries. Unmodeled exterior space is treated as free space/stock; the cutter height evaluates the supremum over the existing surface intersection $\mathcal{D}(x, y, R) \cap \text{Surface}$. Toolpath passes outside the surface domain retract to clearance height.
4. **Residual Stock & Machining Error Analysis**: Formulated the relationship between cutter radius $R$, surface slope $\theta$, and residual material. On an inclined plane with slope $\tan(\theta) = S$, tangential contact at the uphill perimeter eliminates gouging ($Z_{\text{tool}} \ge Z_{\text{surface}}$), but leaves an uncut scallop of $\Delta Z = R \tan(\theta)$ directly beneath the tool center. This confirms why flat endmills on steep slopes leave planar cusp lines and motivates Milestone 9.1 Waterline (Z-level) finishing.
5. **Reconstructed NC Trajectory Envelope Safety**: Verified that the actual posted G-code blocks—when reconstructed via `NcTrajectoryReconstructor`—contain strictly finite 3D coordinates and obey all vertical stock boundaries and fixture clearances.

All 22 regression suites in the complete CAM battery were freshly compiled and executed on JDK 25. **All 22 suites passed with 0 errors and 0 failures, executing exactly 1,284 direct assertions.**

---

## 2. Mathematical Proof of Cutter-Disk Supremum

Let the flat-bottom cylindrical cutter have radius $R > 0$ and spindle axis oriented parallel to the machine $Z$-axis.
When the cutter center is positioned at $(x, y)$, the bottom face forms the horizontal circular disk:
$$\mathcal{D}(x, y, R) = \{ (u, v) \in \mathbb{R}^2 : (u - x)^2 + (v - y)^2 \le R^2 \}$$
at vertical level $Z_{\text{tool}}$.

Let an inclined surface facet $T_i$ be defined by plane $Ax + By + Cz + D = 0$ with upward unit normal $\mathbf{n} = (n_x, n_y, n_z)$ ($n_z > 0$).
The surface height on $T_i$ is an affine linear function:
$$Z(u, v) = \alpha u + \beta v + \gamma, \quad \text{where } \alpha = -\frac{A}{C}, \beta = -\frac{B}{C}, \gamma = -\frac{D}{C}$$

### Theorem (Exhaustive Candidate Set for Linear Function on Disk-Polygon Intersection):
Because $T_i$ is a compact, convex polygon (triangle) and $\mathcal{D}(x, y, R)$ is a compact, convex disk, their intersection $\Omega_i = T_i \cap \mathcal{D}(x, y, R)$ is compact and convex.
By the maximum principle for linear functions on compact convex sets, the global supremum $\sup_{(u, v) \in \Omega_i} Z(u, v)$ must occur on the boundary $\partial \Omega_i$.

The boundary $\partial \Omega_i$ consists of:
1. **Circular boundary arcs** $\partial \mathcal{D} \cap T_i$:
   - The unconstrained maximum on the circle $\partial \mathcal{D}$ occurs at the unique point of steepest ascent:
     $$\mathbf{p}_{\text{ascent}} = (x, y) + R \cdot \frac{(\alpha, \beta)}{\sqrt{\alpha^2 + \beta^2}}$$
   - If $\mathbf{p}_{\text{ascent}} \in T_i$, this point achieves the maximum and is evaluated.
   - If $\mathbf{p}_{\text{ascent}} \notin T_i$, the maximum along the arc $\partial \mathcal{D} \cap T_i$ must occur at the arc's endpoints.
   - The endpoints of the arc $\partial \mathcal{D} \cap T_i$ are precisely the intersection points of the triangle edges with the circle $\partial \mathcal{D}$.
2. **Straight polygonal edges** $\partial T_i \cap \mathcal{D}$:
   - Along any straight line segment, an affine linear function is strictly monotonic or constant, achieving its maximum at the segment endpoints.
   - The endpoints of an edge segment $\partial T_i \cap \mathcal{D}$ are either:
     a) A vertex of $T_i$ that lies inside $\mathcal{D}$.
     b) An intersection point between the edge of $T_i$ and the circle $\partial \mathcal{D}$.

Therefore, evaluating:
$$\mathcal{C} = \{ (x, y) \cap T_i \} \cup \{ \mathbf{p}_{\text{ascent}} \cap T_i \} \cup \{ \mathbf{v} \in \text{Vertices}(T_i) \cap \mathcal{D} \} \cup \{ \text{Edges}(T_i) \cap \partial \mathcal{D} \}$$
provides a **mathematically provable, closed-form, exhaustive candidate set** for the exact supremum over $\mathcal{D}(x, y, R) \cap T_i$.

---

## 3. Residual Stock Analysis on Sloped Facets

In 3-axis machining with a flat-bottom cylindrical endmill of radius $R$:
1. **Gouge Prevention**: Setting $Z_{\text{tool}} = Z_{\text{surface}}(x, y) + R \tan(\theta)$ guarantees that $\forall (u, v) \in \mathcal{D}(x, y, R), Z_{\text{tool}} \ge Z_{\text{surface}}(u, v)$, completely preventing cutter gouges below the surface.
2. **Residual Material**: Because the cutter bottom is flat and horizontal while the surface slopes at angle $\theta$, contact occurs at a single point or edge. Directly under the tool center $(x, y)$, the vertical distance from the cutter bottom to the surface is:
   $$\text{Residual Stock } \Delta Z = R \tan(\theta) = R \sqrt{s_x^2 + s_y^2}$$
   - On a horizontal face ($\theta = 0$): $\Delta Z = 0.0\,\text{mm}$ (perfect finish).
   - On a 10° slope with $R = 5\,\text{mm}$: $\Delta Z = 5 \times \tan(10^\circ) \approx 0.88\,\text{mm}$.
   - On steep walls: residual cusp height grows proportionally with slope.
   This formalizes why flat endmills on planar passes are limited to horizontal and shallow slopes, and establishes the explicit requirement for **Milestone 9.1: Bounded 3D Waterline (Z-Level) Finishing** and ball-endmill support for steep walls.

---

## 4. Complete CAM Regression Battery Results (22 Suites)

All 22 suites were executed cleanly under JDK 25:

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
| 20 | `run_test_milestone8_11_contract_closure.ps1`| M8.11 Canned Cycles & Swept Cylinder Audit | 0 | 27 | **PASS** |
| 21 | `run_test_milestone9_0_planar_finishing.ps1`| M9.0 Bounded 3D Parallel Planar Finishing | 0 | 35 | **PASS** |
| 22 | `run_test_milestone9_0_1_geometry_audit.ps1`| **M9.0.1 Geometry, Chord & Cutter Audit** | 0 | **32** | **PASS** |
| **TOTAL** | **22 Regression Suites** | **Complete Astra CAM Regression Battery** | **0** | **1,284** | **PASS** |

- **Total Direct Assertions**: **1,284** (dynamically verified from test runner stdout).
- **Failures / Errors**: **0**.

---

## 5. Repository Invariants & Physical Safety Boundaries

1. **`pom.xml` Hash**: `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309` (Exact match).
2. **Physical Line Limit**: All 313 production Java files remain $\le 200$ physical lines ([`CamTriangulatedSurface.java`](file:///E:/MultiPhysics/src/main/java/cam/geometry/CamTriangulatedSurface.java): 167 lines, [`ParallelPlanarFinishingOperation.java`](file:///E:/MultiPhysics/src/main/java/cam/operation/ParallelPlanarFinishingOperation.java): 160 lines).
3. **Headless CAM Architecture**: Zero JavaFX imports under `src/main/java/cam/`.
4. **Preservation**: Untracked native files under `src/main/CPP/` remain untouched.
5. **Physical CNC Safety Non-Certification**: Software verification confirms mathematical gouge elimination and collision clearance for the modeled cylindrical cutter envelope (radius $R$, length $L$) against modeled stock and fixtures. It does not certify unmodeled tool holders, spindle housings, clamps, or physical machine kinematics.
