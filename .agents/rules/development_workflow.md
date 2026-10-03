# MultiPhysics Autonomous Development Workflow

This document codifies the active paired engineering workflow for the MultiPhysics project, combining the **Ralph Loop**, **Get Shit Done (GSD)**, **Roo Code**, and **Antigravity Code Review**.

---

## 1. Architect & Plan (Roo Code / GSD Mode)
- **High-Velocity Scoping**: Identify root cause or feature requirements immediately without conversational overhead.
- **Strict Modularity**: Plan all class and helper responsibilities so that every single `.java` file in `src/main/java` stays strictly under **200 physical lines**.
- **No External Dependencies**: Keep the build pure Java 25 + JavaFX without adding external third-party maven libraries unless explicitly requested.

---

## 2. Autonomous Execution Loop (Ralph Loop)
- **Implement**: Make surgical, localized changes to target files.
- **Compile**: Always recompile using the low-memory compilation script:
  `powershell -ExecutionPolicy Bypass -File scratch/compile_sources.ps1`
- **Verify**: Run the project's regression test suite:
  `powershell -ExecutionPolicy Bypass -File scratch/run_all_tests.ps1`
  and feature-specific test scripts (e.g. `run_test_cuboid.ps1`).
- **Non-Blocking Test Standard**: All test harnesses initializing JavaFX MUST wrap execution in `try-finally { System.exit(exitCode); }` to terminate background threads and prevent command hangs. Always ensure clean compilation before executing tests (see `test_execution_safety.md`).
- **Autonomous Self-Healing**: If any compilation error or test failure occurs, inspect the exact error, fix the implementation autonomously, and rerun tests until all assertions are green.

---

## 3. Pre-Commit Review (Antigravity Code Review)
Before finalizing any code change:
- **Line Count Audit**: Run `scratch/line_audit.ps1` to confirm all 69+ `.java` files remain $\le 200$ physical lines.
- **Geometry & Topology Checks**:
  - Degenerate dimensions checked ($< 0.2\text{ mm}$).
  - Face normal directions and right-handed UV coordinates verified.
  - Annular face triangulation and cavity CSG void trimming verified for intersecting features.
- **Zero-Regression Invariant**: Confirm existing persistence formats (`.astra`, `.nd`), 3D exports (`.obj`, `.stl`, `.stp`, `.nc`), and shape manipulators are unaffected.

---

## 4. Delivery & Sync (GSD)
- Commit and push immediately after every verified code change to `origin/main`.
- Provide a concise summary of changes and verification evidence to the user.
