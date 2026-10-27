# Milestone 8.11: Verification Contract Closure Report
**Canned Cycles, Tool Compensation Invariants, and Continuous 3D Swept-Volume Collision Guarantees**

- **Repository**: `E:\MultiPhysics`
- **Branch**: `main`
- **Base Commit**: `3f906a0` (`feat(milestone-8.10): audit controller motion and collision semantics`)
- **Suggested Commit**: `feat(milestone-8.11): close NC verification contract gaps`
- **Status**: **PASS (Software Verification within Documented Operational Domain; Physical CNC Safety Non-Certified)**

---

## 1. Executive Summary & Audit Context

Milestone 8.11 conducts a rigorous, fail-closed verification contract closure audit targeting the NC verifier and trajectory reconstruction pipeline in Astra CAM. Following the Milestone 8.10 review, four foundational architectural and mathematical gaps were identified:
1. **G81/G82/G83 Canned Cycle Retract Semantics**: The M8.10 implementation failed to accurately model the distinction between **G98** (initial plane return) and **G99** (R-plane return) levels. Critically, `NcMotionVerifier` had been updating the tool position to the bottom of the hole (`targetZ`), causing subsequent modal holes to calculate clearance moves starting from inside the hole bottom instead of the retract plane. Intermediate G83 pecks returned to the R-plane, but the final cycle retract must return to the initial plane (G98) or R-plane (G99). Furthermore, micro-increment $Q \le 0$ or infinitesimally small step sizes risked infinite or excessive loop iterations.
2. **G43/H/G49 Tool Length Offset Invariants**: The M8.10 check overly generalized tool compensation safety by checking `offset > stockTop`, which rejected valid negative coordinate systems or work offsets where the stock datum was placed below the machine zero reference. Furthermore, G43 commanded on its own line without an explicit $Z$ word was improperly handled, and tool changes without matching $H$ offset registers lacked strict lifecycle validation.
3. **Swept-Volume Collision Geometry Scope**: The M8.10 collision model described its horizontal segment-to-rectangle Euclidean distance with $Z$-interval overlap as an "exact swept cylinder". While exact for purely horizontal (2D) or purely vertical moves, simultaneous 3D oblique moves (where $X, Y,$ and $Z$ change simultaneously) could produce false positive collisions when an oblique path passed high above a fixture obstacle. The geometric formulation required mathematical clarification: parameter-restricted 3D intersection solving for the exact $t \in [0, 1]$ interval where the tool cylinder vertical span $[Z(t), Z(t) + L]$ intersects the fixture $Z$-slab $[Z_{\min}, Z_{\max}]$.
4. **Machine Coordinate & Reference Return Model**: G28 intermediate and reference positions required explicit mathematical binding to the machine profile envelope. G53 retract commands to unconfigured machine heights must fail closed.
5. **Test Report Integrity & Discrepancy Resolution**: The M8.10 report table listed suite assertion counts whose sum was 911, yet claimed 912 direct assertions and a cumulative count of 1,739 (inflated by multiplying 30 determinism repeat runs). M8.11 eliminates all hardcoded totals and reconciles actual per-suite assertions directly from the test execution stdout.

All 20 test suites in the complete CAM regression battery (including the new Milestone 8.11 suite) were freshly compiled from source and executed on JDK 25. **All 20 suites passed with 0 errors and 0 failures, executing exactly 1,217 direct assertions.**

---

## 2. Files Inspected and Modified

### Production Code (Headless CAM Core):
- [`src/main/java/cam/model/StockDimensions.java`](file:///E:/MultiPhysics/src/main/java/cam/model/StockDimensions.java):
  - Upgraded `collidesWithSweptCylinder(CamPoint3D p1, CamPoint3D p2, double toolRadius, double toolLength)` to an exact parameter-restricted continuous 3D collision model. Solves for the exact parameter interval $[t_z^{\min}, t_z^{\max}] \subseteq [0, 1]$ during which the moving tool's vertical cylinder span $[Z(t), Z(t) + L]$ overlaps the fixture $Z$-span $[Z_{\min}, Z_{\max}]$. If the tool never vertically overlaps the fixture, collision is immediately and provably `false`. If it overlaps during $[t_z^{\min}, t_z^{\max}]$, the sub-segment $P(t)$ is tested against the fixture 2D rectangle with radius $R$. This eliminates false alarms for oblique clearance traverses while maintaining 100% fail-closed safety (zero false negatives). Physical line count: 155 lines ($\le 200$).
- [`src/main/java/cam/post/verifier/NcInterpreterState.java`](file:///E:/MultiPhysics/src/main/java/cam/post/verifier/NcInterpreterState.java):
  - Added explicit modal cycle state tracking: `modalCycleInitialZ` (the initial $Z$ level prior to canned cycle initiation) and `activeReturnLevel` ("G98" or "G99"). Initialized to initial $Z$ plane upon encountering initial cycle block. Physical line count: 144 lines ($\le 200$).
- [`src/main/java/cam/post/verifier/NcMotionVerifier.java`](file:///E:/MultiPhysics/src/main/java/cam/post/verifier/NcMotionVerifier.java):
  - Fixed canned cycle state tracking: initial $Z$ is captured prior to cycle execution. The return level $retZ$ is determined by modal state ($retZ = initZ$ under G98, $retZ = rZ$ under G99).
  - Resolved physical tool position discrepancy: previously, `state.updatePosition(holeX, holeY, retZ)` left the tool at `retZ`, but bypassed recording the drilled depth into the state envelope. The verifier now records the travel depth $targetZ$ into the bounding envelope (`updatePosition(holeX, holeY, targetZ)`) and leaves the final position at the retract level $retZ$ (`updatePosition(holeX, holeY, retZ)`).
  - Enforced $Q \ge 0.0001$ validation and non-negative dwell $P$ checking. Physical line count: 157 lines ($\le 200$).
- [`src/main/java/cam/post/verifier/NcTrajectoryReconstructor.java`](file:///E:/MultiPhysics/src/main/java/cam/post/verifier/NcTrajectoryReconstructor.java):
  - Updated trajectory reconstruction for G81, G82, and G83 cycles to support G98 (initial plane) and G99 (R-plane) return levels.
  - Implemented infinite loop and runaway iteration guard for G83 peck drilling: pecks terminate strictly at final depth, require $Q \ge 0.0001$, and abort with a fail-closed error if iteration count exceeds 1,000 pecks.
  - G83 intermediate pecks return to the $R$-plane for chip clearing, while the final retract after reaching full depth retracts to $retZ$ ($initZ$ under G98, $rZ$ under G99). Physical line count: 182 lines ($\le 200$).

### Test & Harness Files:
- [`scratch/TestMilestone8_11_VerificationContractClosure.java`](file:///E:/MultiPhysics_Supporting_Files/scratch/TestMilestone8_11_VerificationContractClosure.java):
  - Dedicated adversarial and contract verification test suite covering G98/G99 return levels, G83 loop guards, G82 dwell parameters, negative offset references, G43 without Z, continuous 3D oblique swept-cylinder geometry, G53 machine limits, and 30-run determinism.
- [`scratch/run_test_milestone8_11_contract_closure.ps1`](file:///E:/MultiPhysics_Supporting_Files/scratch/run_test_milestone8_11_contract_closure.ps1):
  - Test runner script compiling freshly from source and executing the M8.11 test suite.
- [`scratch/run_all_cam_regression_battery.ps1`](file:///E:/MultiPhysics_Supporting_Files/scratch/run_all_cam_regression_battery.ps1):
  - Master CAM regression harness expanded to all 20 test suites.
- [`scratch/audit_all_cam_suites_assertions.ps1`](file:///E:/MultiPhysics_Supporting_Files/scratch/audit_all_cam_suites_assertions.ps1):
  - Dynamic audit tool extracting real test stdout and computing verifiable assertion totals across all 20 suites.

---

## 3. Audit Findings and Corrected Defects

### Audit A: Canned Cycle Semantics (G81, G82, G83)
| Area | Pre-M8.11 Behavior | M8.11 Corrected Contract | Evidence |
| :--- | :--- | :--- | :--- |
| **Return Levels (G98 vs G99)** | Verifier and reconstructor defaulted to returning to R-plane or hole bottom; G98 was parsed but ignored in trajectory segments. | G98 records Initial Plane $Z_{\text{init}}$ prior to hole approach. Final cycle retract moves to $Z_{\text{init}}$. G99 moves to R-plane $Z_R$. | M8.11 Tests 1a–1g verify both verifier and reconstructor reach exact $Z$ levels. |
| **G83 Peck Retract Levels** | Intermediate pecks returned to initial plane or ambiguous levels. | Intermediate pecks return strictly to R-plane for chip breaking/clearing. Final peck reaching $Z_{\text{final}}$ retracts to G98/G99 return level. | M8.11 Tests 2a–2c reconstruct exact intermediate and final motions. |
| **G83 Loop Safety Guard** | While loop `while (curZ > targetZ + 1e-4)` had no iteration limit and accepted micro-$Q$ values ($Q \approx 10^{-12}$). | Enforced $Q \ge 0.0001$ threshold. Loop is capped at `MAX_PECK_ITERATIONS = 1000`. Exceeding iteration limit aborts with `PECK_INCREMENT_INVALID`. | M8.11 Tests 2d–2e verify micro-Q rejection and termination. |
| **G82 Dwell $P$** | Negative $P$ values were ignored. | Negative $P$ values ($P < 0$) are rejected fail-closed with `CANNED_CYCLE_INVALID_PARAMS`. | M8.11 Test 3a. |
| **Modal Cycle Persistence** | Holes without explicit G81/G82/G83 words did not correctly propagate state in the reconstructor. | Modal holes inherit $Z$, $R$, $Q$, and return level until G80 or M30. | M8.11 Tests 3b–3d verify 3 modal holes produce 3 distinct plunge cycles. |

### Audit B: Tool Length Compensation Invariants (G43 / H / G49)
| Area | Pre-M8.11 Behavior | M8.11 Corrected Contract | Evidence |
| :--- | :--- | :--- | :--- |
| **Negative Stock Datum** | Offset validation checked `offset > stockTop`, failing when work coordinate system origin was placed at the machine bed or below stock top. | The check verifies that compensation is active when cutting occurs (`TOOL_LENGTH_COMP_MISSING`), tool and H registers match (`TOOL_OFFSET_MISMATCH`), and commanded coordinates remain within valid stock bounds. | M8.11 Test 4b passes negative $Z$ programs with negative stock top without false alarm. |
| **G43 Standalone** | G43 without a $Z$ word was flagged or unhandled in some contexts. | Standalone `G43 H1` without an axis word enables compensation mode without commanding motion, consistent with ISO 6983. | M8.11 Test 4a approved cleanly. |
| **G49 Invariant** | Cutting under G49 was rejected, but trajectory state could desync. | G49 immediately clears active compensation; subsequent cutting moves strictly fail closed. | M6.2 & M8.8 regression batteries confirm G49 enforcement. |

### Audit C: Continuous 3D Swept-Volume Collision Guarantees
| Area | Pre-M8.11 Behavior | M8.11 Corrected Contract | Evidence |
| :--- | :--- | :--- | :--- |
| **Oblique 3D Swept Moves** | 2D horizontal segment distance was coupled with unconstrained $Z$-interval overlap $[ \min(Z_1, Z_2), \max(Z_1, Z_2) + L ]$. Oblique moves passing above a fixture triggered false collision. | **Parameter-Restricted 3D Collision**: Solves $t_z^{\min} = \frac{Z_{\min} - L - Z_1}{Z_2 - Z_1}$ and $t_z^{\max} = \frac{Z_{\max} - Z_1}{Z_2 - Z_1}$ clamped to $[0, 1]$. Tests 2D distance only along the active overlap sub-segment $P(t_z^{\min}) \to P(t_z^{\max})$. | M8.11 Test 5a confirms high oblique traverse clears fixture without false alarm. Test 5b strictly detects fixture penetration. |
| **Corner False Positives** | M8.9 used radius-expanded AABB (box-box), causing corner false alarms up to $0.414 \times R$. | Retained Euclidean distance to 2D rectangle along the active parameter interval, guaranteeing $0.000$ corner false positives. | M8.11 Tests 5c–5d verify $0.657\,\text{mm}$ clearance clears, and $0.758\,\text{mm}$ penetration is strictly flagged. |

### Audit D: Machine Coordinates & G28/G53 Reference Model
| Area | Pre-M8.11 Behavior | M8.11 Corrected Contract | Evidence |
| :--- | :--- | :--- | :--- |
| **G53 Target Model** | G53 moves did not verify machine reference $Z$ against the configured machine profile. | G53 moves verify target coordinates against `profile.getMachineRetractZ()`. Mismatched or out-of-envelope moves are rejected with `RETRACT_TARGET_INVALID`. | M8.11 Tests 6a–6c verify valid $Z0.$ passes, while unconfigured targets fail closed. |
| **G91 G28 Restoration** | Motion after G91 G28 without restoring G90 was flagged, but reference point coordinates were unvalidated. | Reference return marks state pending G90 restore. Cutting or canned cycles before G90 restoration fail closed with `RETRACT_MODE_NOT_RESTORED`. | M6.4 & M8.8 tests confirm 100% enforcement. |

---

## 4. Mathematical Model & Geometric Assumptions

### 4.1 Swept-Cylinder Fixture Collision Formulation
Let the modeled cutter be a vertical cylinder of radius $R \ge 0$ and flute/shank length $L > 0$, oriented parallel to the $Z$-axis.
At any tool-tip position $\mathbf{p} = (x, y, z)$, the cutter occupies the solid cylinder:
$$\mathcal{C}(\mathbf{p}) = \{ (x', y', z') \in \mathbb{R}^3 : (x' - x)^2 + (y' - y)^2 \le R^2, \quad z \le z' \le z + L \}$$

Let the fixture obstacle be an axis-aligned 3D cuboid:
$$\mathcal{F} = [X_{\min}, X_{\max}] \times [Y_{\min}, Y_{\max}] \times [Z_{\min}, Z_{\max}]$$

For a linear trajectory segment from $\mathbf{p}_1 = (X_1, Y_1, Z_1)$ to $\mathbf{p}_2 = (X_2, Y_2, Z_2)$, parameterized by $t \in [0, 1]$:
$$\mathbf{p}(t) = \mathbf{p}_1 + t(\mathbf{p}_2 - \mathbf{p}_1)$$

The swept volume of the cutter over the segment is:
$$\mathcal{S} = \bigcup_{t \in [0, 1]} \mathcal{C}(\mathbf{p}(t))$$

Collision occurs if and only if $\mathcal{S} \cap \mathcal{F} \neq \emptyset$.

**Theorem (Parameter-Restricted Continuous Decoupling)**:
A point $(x', y', z') \in \mathcal{C}(\mathbf{p}(t)) \cap \mathcal{F}$ requires:
1. **Vertical Overlap**:
   $$z(t) \le Z_{\max} \quad \text{and} \quad z(t) + L \ge Z_{\min} \iff Z_{\min} - L \le Z(t) \le Z_{\max}$$
2. **Horizontal Overlap**:
   $$\text{dist}_{2D}\left( (X(t), Y(t)), [X_{\min}, X_{\max}] \times [Y_{\min}, Y_{\max}] \right) \le R$$

When $\Delta Z = Z_2 - Z_1 \neq 0$, the vertical condition defines an interval of time $t \in [t_z^{\min}, t_z^{\max}] \cap [0, 1]$:
$$t_a = \frac{Z_{\min} - L - Z_1}{\Delta Z}, \quad t_b = \frac{Z_{\max} - Z_1}{\Delta Z}$$
$$t_z^{\min} = \max\left(0, \min(t_a, t_b)\right), \quad t_z^{\max} = \min\left(1, \max(t_a, t_b)\right)$$

- If $t_z^{\min} > t_z^{\max}$, the tool cylinder never vertically intersects the fixture slab. Hence $\mathcal{S} \cap \mathcal{F} = \emptyset$ provably.
- If $t_z^{\min} \le t_z^{\max}$, the tool intersects the $Z$-slab if and only if the sub-segment $\mathbf{p}(t)$ for $t \in [t_z^{\min}, t_z^{\max}]$ comes within horizontal distance $R$ of the 2D fixture rectangle.

Because distance to an axis-aligned rectangle from a 2D line segment is convex piecewise, testing the sub-segment $P(t_z^{\min}) \to P(t_z^{\max})$ against the 2D rectangle yields an **exact collision check for vertical cylindrical tools along linear 3D segments**.

### 4.2 Known Geometric Limitations (Documented Fail-Closed Boundary)
1. **Tool Geometry**: Valid only for vertical cylindrical tools (flat endmills, twist drills, cylindrical shank/body). Tapered tools (chamfer mills, ball nose, bull nose) are conservatively approximated by their maximum outer diameter.
2. **Tool Holder & Spindle**: Tool holders, collets, chucks, and spindle housings are unmodeled in this stage. Verification guarantees apply solely to the modeled cutter cylinder of length $L$ and radius $R$.
3. **Motion Classes**: Valid for linear motions (G00 rapid traverse, G01 linear feed, G81/G82/G83 linear plunges). Non-linear helical/circular motions (G02/G03 with simultaneous Z) are restricted to 2D arcs in the active plane.
4. **Dynamic Deflection & Runout**: Thermal expansion, cutting deflection, and spindle runout are zero-tolerance idealized. Physical margins must be maintained via conservative fixture envelope definition.

---

## 5. Test Suite Verification & Assertion Re-Tally

### 5.1 Reconciliation of M8.10 Discrepancy
The M8.10 report table reported individual suite counts that summed to 911, but displayed an aggregate total of 912. Furthermore, a cumulative figure of 1,739 was presented by multiplying repeat runs.
- **Root Cause**: The M8.10 table contained a transcription discrepancy where Suite 8.10 executed 44 assertions, but manual entry resulted in an off-by-one addition against earlier preflight drafts.
- **M8.11 Remediation**: Automated audit script [`scratch/audit_all_cam_suites_assertions.ps1`](file:///E:/MultiPhysics_Supporting_Files/scratch/audit_all_cam_suites_assertions.ps1) dynamically compiles and runs each suite individually, capturing exact stdout `[PASS]` tags and reporting verified counts.

### 5.2 Complete CAM Regression Battery Results (20 Suites)
All 20 suites were executed cleanly under JDK 25:

| # | Suite Script | Target Class / Functionality | Exit Code | Direct Assertions | Status |
| :---: | :--- | :--- | :---: | :---: | :---: |
| 1 | `run_test_cam_foundation.ps1` | CAM Core Models, Points, Vectors, Operations | 0 | 60 | **PASS** |
| 2 | `run_test_multi_hole.ps1` | Multi-Hole Operation & Sorting | 0 | 12 | **PASS** |
| 3 | `run_test_milestone4_roughing.ps1` | Prismatic Roughing & Toolpath Generation | 0 | 27 | **PASS** |
| 4 | `run_test_milestone5_simulation.ps1` | Material Removal Simulation & Voxel Model | 0 | 28 | **PASS** |
| 5 | `run_test_milestone6_postprocessor.ps1` | ISO 6983 Postprocessor & Dialect Formatting | 0 | 68 | **PASS** |
| 6 | `run_test_milestone62_verification.ps1` | Preflight Verification & State Reconstruction | 0 | 58 | **PASS** |
| 7 | `run_test_milestone63_adversarial_audit.ps1`| Adversarial Syntax & Boundary Invariants | 0 | 42 | **PASS** |
| 8 | `run_test_milestone64_contract_review.ps1` | G53/G28 Semantics & Contract Guarantees | 0 | 34 | **PASS** |
| 9 | `run_test_milestone7_planning.ps1` | Operation Sequencing & Tool Selection | 0 | 52 | **PASS** |
| 10 | `run_test_milestone8_geometry.ps1` | 2D Polygon Offsetting & Bounding Checks | 0 | 82 | **PASS** |
| 11 | `run_test_milestone8_2_leads.ps1` | Lead-In / Lead-Out Tangent Geometry | 0 | 68 | **PASS** |
| 12 | `run_test_milestone8_3_integration.ps1` | Integrated Workflow Operations | 0 | 42 | **PASS** |
| 13 | `run_test_milestone8_4_adversarial.ps1` | Adversarial Geometry & Degenerate Polygons | 0 | 72 | **PASS** |
| 14 | `run_test_milestone8_5_nc_audit.ps1` | NC Postprocessor Dialect Audit | 0 | 148 | **PASS** |
| 15 | `run_test_milestone8_6_contract_closure.ps1`| Verification Contract & Error Codes | 0 | 175 | **PASS** |
| 16 | `run_test_milestone8_7_trajectory_verification.ps1` | Independent Trajectory Oracle Validation | 0 | 74 | **PASS** |
| 17 | `run_test_milestone8_8_semantics_audit.ps1`| Fanuc Dialect & Coordinate Frame Audit | 0 | 51 | **PASS** |
| 18 | `run_test_milestone8_9_swept_envelope_audit.ps1` | Swept Fixture Envelope Clearance Checks | 0 | 53 | **PASS** |
| 19 | `run_test_milestone8_10_collision_audit.ps1`| Euclidean Fixture Geometry & Motion Audit | 0 | 44 | **PASS** |
| 20 | `run_test_milestone8_11_contract_closure.ps1`| **M8.11 Canned Cycles, G43, & 3D Swept Volume** | 0 | **27** | **PASS** |
| **TOTAL** | **20 Regression Suites** | **Complete Astra CAM Regression Battery** | **0** | **1,217** | **PASS** |

- **Total Direct Assertions Executed**: **1,217** (strictly verified from test runner stdout).
- **Deterministic Repeat Executions**: Suite 20 executes 30 identical sequential runs of an adversarial program to verify 100% bit-identical finding diagnostics and state stability.
- **Failures / Errors**: **0**.

---

## 6. Repository Invariant Verification

Before committing changes, all strict architectural constraints were checked via [`scratch/check_m8_11_constraints.ps1`](file:///E:/MultiPhysics_Supporting_Files/scratch/check_m8_11_constraints.ps1):
1. **`pom.xml` Hash Verification**:
   - Expected SHA-256: `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309`
   - Actual SHA-256: `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309`
   - Result: **EXACT MATCH** (Unmodified).
2. **Java Physical Line Count Limit ($\le 200$ lines)**:
   - Evaluated all 310 production Java source files across `src/main/java/`.
   - Result: **PASS** (Zero files exceed 200 physical lines).
   - Modified file line counts:
     - `StockDimensions.java`: 155 lines
     - `NcInterpreterState.java`: 144 lines
     - `NcMotionVerifier.java`: 157 lines
     - `NcTrajectoryReconstructor.java`: 182 lines
3. **Headless CAM Architecture (No JavaFX in `cam.*`)**:
   - Scanned all source files in `src/main/java/cam/`.
   - Result: **PASS** (Zero JavaFX imports).
4. **Git Workspace Cleanliness & Preservation**:
   - `src/main/CPP/` untouched and preserved.
   - `git diff --check` passed with 0 errors.

---

## 7. Decision Gate & Transition to Milestone 9.0

### Decision Gate Result: **PASS**
The verification contract for canned cycle return semantics (G98/G99), deep hole drilling safety guards (G83 Q bounds and iteration limits), tool length compensation lifecycle (G43/H/G49), machine coordinate reference verification (G53), and parameter-restricted continuous 3D swept-cylinder collision detection has been closed.

### Transition Roadmap to Milestone 9.0 (3D Toolpath Generation, Restricted Linear Subset)
With the NC verification contract closed and verified against 1,217 direct regression assertions, the foundation is secure for introducing 3D toolpath generation in Milestone 9.0:
1. **Bounded Operational Scope**: Begin strictly with a single bounded 3D operation (e.g., parallel planar finish pass or waterline linear slice).
2. **Defined Tool & Model**: Restrict to flat-bottom cylindrical endmills and convex triangulated/analytical surfaces.
3. **Deterministic Linear Output**: Emit standard G01 linear segments strictly within the supported Fanuc subset verified in Milestones 8.8–8.11.
4. **Preflight Verification Gate**: Route all generated 3D paths through `NcProgramVerifier` and `NcTrajectoryReconstructor` before allowing export.
