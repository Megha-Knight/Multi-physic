# Engineering Report: Milestone 8.1 — CAM Contour Geometry & Offset Engine

## Executive Summary
Milestone 8, Phase 8.1 establishes an isolated, deterministic, pure-Java 2D planar contour geometry and offset engine for Astra CAM.
This phase strictly focuses on robust 2D geometry representations, explicit boundary vs. open-path semantics, orientation-independent machining intent resolution, corner miter joins with bounded limits and beveled fallbacks, collapse/self-intersection detection, and adversarial error gating.

In accordance with Phase 8.1 scope boundaries, lead-in/lead-out moves, controller cutter compensation (`G41`/`G42`), postprocessing integration, and finishing operation orchestration are deliberately excluded from this phase and reserved for Phase 8.2+.

---

## 1. Geometry Conventions & Units
- **Coordinate System**: Planar 2D Cartesian coordinates $(X, Y)$ in standard machine units (millimeters, $\text{mm}$).
- **Floating-Point Tolerance**: Absolute numerical tolerance $\epsilon = 1.0 \times 10^{-6}\text{ mm}$.
- **Open Paths**: Ordered sequences of $N \ge 2$ finite 2D vertices (`CamPoint2D`). Open paths do not enclose area; signed area evaluates to $0.0$.
- **Closed Boundaries**: Ordered sequences of $N \ge 3$ finite 2D vertices forming closed planar polygons (implicit closing segment connecting vertex $N-1$ to vertex $0$).
- **Immutability & Defensive Copying**: All contour instances (`CamContour2D`) defensively clone input collections via `List.copyOf()`, preventing post-construction mutation of geometry.
- **Signed Area & Winding**:
  $$\text{Signed Area} = \frac{1}{2} \sum_{i=0}^{N-1} (x_i y_{i+1} - x_{i+1} y_i)$$
  - Counter-Clockwise (CCW): $\text{Signed Area} > 0$
  - Clockwise (CW): $\text{Signed Area} < 0$

---

## 2. Offset Direction & Machining-Side Semantics
A critical design requirement of Phase 8.1 is that **winding alone must not determine whether a contour is an inside or outside boundary**.
- Winding describes travel orientation.
- The intended machining side (`ContourSide`: `LEFT`, `RIGHT`, `INSIDE`, `OUTSIDE`) is an explicit input.

### Direction Resolution Matrix
| Input Side | Contour Type | Contour Winding | Resolved Travel Side | Physical Meaning |
| :--- | :--- | :--- | :--- | :--- |
| `LEFT` | Open / Closed | Any | `LEFT` | Offset normal to left of motion direction: $(-dy, dx) / L$ |
| `RIGHT` | Open / Closed | Any | `RIGHT` | Offset normal to right of motion direction: $(dy, -dx) / L$ |
| `INSIDE` | Closed | CCW ($\text{Area} > 0$) | `LEFT` | Inward shrinkage towards polygon interior |
| `INSIDE` | Closed | CW ($\text{Area} < 0$) | `RIGHT` | Inward shrinkage towards polygon interior |
| `OUTSIDE` | Closed | CCW ($\text{Area} > 0$) | `RIGHT` | Outward expansion away from interior |
| `OUTSIDE` | Closed | CW ($\text{Area} < 0$) | `LEFT` | Outward expansion away from interior |
| `INSIDE` / `OUTSIDE` | Open | N/A | **REJECTED** | Fails closed with descriptive `IllegalArgumentException` |

---

## 3. Corner Join & Miter-Limit Policy
- **Convex Offsets**: When adjacent shifted edge segments diverge, the engine computes the linear intersection of their infinite carrier lines (the miter vertex).
- **Miter Distance Evaluation**: The distance $d_{\text{miter}} = \|\mathbf{p}_{\text{miter}} - \mathbf{v}_{\text{corner}}\|$ is computed.
- **Miter Limit Enforcement**:
  - The default miter limit is $\mu = 2.0$.
  - If $d_{\text{miter}} \le \mu \cdot d + 10^{-5}$, the sharp miter vertex is admitted.
  - If $d_{\text{miter}} > \mu \cdot d$, the engine falls back to a safe **beveled join**, inserting the endpoints of both shifted edges, and records a structured warning in `CamContourOffsetResult.getWarnings()`.

---

## 4. Degeneracy, Self-Intersection & Collapse Handling
The engine enforces strict fail-closed gating on input and output geometries:
1. **Input Validation**:
   - Rejects non-finite ($NaN$, $\pm\infty$) and null vertex coordinates.
   - Rejects consecutive duplicate vertices and edges with length $\le 10^{-6}\text{ mm}$.
   - Rejects collinear zero-area closed contours.
   - Rejects self-intersecting input contours (strict cross-product orientation test).
2. **Offset Collapse & Inversion Detection**:
   - **Winding Inversion**: If an offset loop's winding flips (e.g. CCW to CW), the loop has crossed over itself and is rejected (`"Inverted loop: resulting contour winding flipped"`).
   - **Area Shrinkage/Expansion Violations**: Inside offsets must strictly reduce enclosed area ($\text{Area}_{\text{offset}} < \text{Area}_{\text{original}} - 10^{-4}$); outside offsets must strictly expand area.
   - **Boundary Containment**: All vertices of an inside offset must remain within the original boundary (`contour.contains(pt)`).
   - **Opposing Boundary Collapse**: For inside offsets, each offset vertex must satisfy $\text{distToBoundary}(\mathbf{p}) \ge d - 10^{-4}\text{ mm}$. If any vertex is closer to the opposing boundary than the offset distance, the feature has collapsed and is rejected.

---

## 5. Known Limitations & Scope Boundaries
- **Planar 2D Only**: Operates strictly in the $(X, Y)$ plane. 3D surface finish and tool orientation are out of scope.
- **No Physical Gouge/Collision Claim**: Detects 2D geometric feature collapse and self-intersections within planar boundaries, but does not claim complete 3D tool holder, shank, or fixture collision safety.
- **Single-Loop Offset**: Offset of concave loops that decompose into multiple disconnected sub-loops currently fails closed; full multi-loop polygon clipping/Boolean union is reserved for subsequent phases.
- **No NC/G-code Emission in Phase 8.1**: Lead-in/out arcs and `G41`/`G42` compensation blocks are intentionally deferred to Phase 8.2+.

---

## 6. Verification Records

### A. Line Count Audit
- **Requirement**: All production Java files under `src/main/java/` must remain $\le 200$ physical lines.
- **Tool**: `verify_line_counts.ps1`
- **Observed**:
  - Total production Java files: **302**
  - Violations (> 200 lines): **0**
  - `CamContour2D.java`: 144 lines
  - `CamContourOffsetEngine.java`: 137 lines
  - `CamContourOffsetResult.java`: 59 lines
  - `ContourSide.java`: 37 lines

### B. Pure Java / Headless Architecture Audit
- **Requirement**: Headless pure Java in `cam.*`, zero JavaFX imports.
- **Command**: `Get-ChildItem -Path src/main/java/cam -Recurse -Filter *.java | Select-String -Pattern 'javafx' | Measure-Object`
- **Result**: Count = **0** (Zero JavaFX imports).

### C. Frozen pom.xml Integrity
- **Requirement**: Frozen SHA-256 hash `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309`.
- **Observed**: `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309` (Identical, untouched).

### D. Maven Compilation
- **JDK**: Microsoft JDK 25.0.4 (`C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot`).
- **Command**: `mvn compile`
- **Result**: `[INFO] BUILD SUCCESS` (302 source files compiled cleanly).

### E. Test Suite Results
| Suite | Description | Assertions Passed | Failures | Status |
| :--- | :--- | :--- | :--- | :--- |
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
| **Total Cumulative** | **All System Regression Suites** | **988** | **0** | **PASS** |

---

## 7. Git Manifest
- **Staged Files**:
  - `src/main/java/cam/geometry/CamContour2D.java`
  - `src/main/java/cam/geometry/CamContourOffsetEngine.java`
  - `src/main/java/cam/geometry/CamContourOffsetResult.java`
  - `src/main/java/cam/geometry/ContourSide.java`
  - `docs/reports/Milestone8_1_Contour_Geometry_Report.md`
- **Unmodified / Preserved Untracked Files**:
  - `src/main/CPP/*` (native C++ sources untouched and preserved)
  - `pom.xml` (frozen SHA-256 verified)
