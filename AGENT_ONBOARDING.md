# Agent Onboarding Guide — Astra / MultiPhysics
**Quickstart Protocol and Engineering Rules for AI Coding Assistants**

Welcome to the Astra / MultiPhysics repository! This document provides essential instructions for any newly assigned AI agent or human software engineer resuming development on this codebase.

---

## 1. First Steps: Orientation & Baseline Verification

Before making any modifications:
1. **Read the Primary Handoff Report**:
   Inspect [`docs/reports/ASTRA_MULTIPHYSICS_PROJECT_HANDOFF.md`](file:///E:/MultiPhysics/docs/reports/ASTRA_MULTIPHYSICS_PROJECT_HANDOFF.md). This is the authoritative overview of architecture, milestone history, and current status.
2. **Inspect Current Git State**:
   Run `git status` and `git log -3 --oneline`. Ensure your working branch is `main` and tree is clean (with untracked `src/main/CPP/` preserved).
3. **Verify the Frozen POM Hash**:
   ```powershell
   Get-FileHash -Path pom.xml -Algorithm SHA256
   ```
   The hash MUST strictly equal:
   `DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309`
4. **Run the Master CAM Regression Battery**:
   ```powershell
   powershell -ExecutionPolicy Bypass -File E:\MultiPhysics_Supporting_Files\scratch\run_all_cam_regression_battery.ps1
   ```
   Confirm all 24 suites pass with 0 failures and 1,370 direct assertions before touching code.

---

## 2. Non-Negotiable Project Constraints

| Rule | Description | Enforcement Command |
| :--- | :--- | :--- |
| **1. Frozen `pom.xml`** | Never edit `pom.xml`. Dependencies are locked. | `Get-FileHash -Path pom.xml -Algorithm SHA256` |
| **2. 200-Line Limit** | Every production file under `src/main/java/` must contain $\le 200$ physical lines. | `Get-ChildItem -Path src/main/java -Recurse -Filter *.java \| Where-Object { (Get-Content $_.FullName).Length -gt 200 }` |
| **3. Headless CAM** | The `cam/**` package must remain 100% pure headless Java without any `javafx.*` imports. | `Get-ChildItem -Path src/main/java/cam -Recurse -Filter *.java \| Select-String "javafx"` |
| **4. Untracked C++ Assets** | `src/main/CPP/` contains prebuilt native toolchain binaries. Never delete, modify, stage, or commit it. | Do not touch `src/main/CPP/`. Excluded from handoff ZIP. |
| **5. Non-Destructive Git** | Never run `git reset --hard`, `git clean -fd`, or destructive commands. | Use standard `git add`, `git commit`, `git push`. |
| **6. Verified Pushes** | Never claim a git push succeeded without verifying its return code and output. | Check `git status` and `git log -1` after pushing. |

---

## 3. Standard Development Workflow

When assigned a task:
1. **Scope the Work**: Select a single, bounded objective from the roadmap in the handoff report. Avoid broad, unsolicited refactorings.
2. **Implement Production Code**:
   - Write clean, modular, immutable Java code.
   - If a class approaches 180 lines, extract helper logic or strategies into separate delegate classes to protect the 200-line limit.
3. **Write Independent Test Oracles**:
   - Place integration and audit suites in `E:\MultiPhysics_Supporting_Files\scratch\`.
   - Never validate an algorithm solely against helper routines from its own production class. Implement an independent geometric or numerical oracle (e.g. discrete sampling or first-principles math).
4. **Compile & Test**:
   ```powershell
   # Compile CAM classes
   javac -d target/classes (Get-ChildItem -Path src/main/java/cam -Recurse -Filter *.java | ForEach-Object { $_.FullName })
   # Run your new test suite
   powershell -ExecutionPolicy Bypass -File E:\MultiPhysics_Supporting_Files\scratch\your_test_runner.ps1
   # Run full regression battery
   powershell -ExecutionPolicy Bypass -File E:\MultiPhysics_Supporting_Files\scratch\run_all_cam_regression_battery.ps1
   ```
5. **Verify All Invariants**:
   - Recheck POM hash.
   - Recheck 200-line limits across all production Java files.
   - Run `git diff --check` to ensure no whitespace errors.
6. **Produce Engineering Documentation**:
   - Write a formal milestone report in `docs/reports/` documenting mathematical principles, test matrix results, assertion counts, and safety limitations.
7. **Commit & Push**:
   - Stage only production code, tests, and documentation files.
   - Use conventional commit messages: `feat(milestone-X): ...` or `fix(cam): ...`.
   - Push to `origin main` and verify with `git status`.

---

## 4. Note on the Native Component (`native/` and `src/main/CPP/`)

- The active project contains high-performance C++ raycasting code under `native/`.
- The `src/main/CPP/` directory contains an external local toolchain/runtime environment that is intentionally untracked and maintained separately.
- Handoff ZIP archives generated for agents intentionally **exclude** `src/main/CPP/` to keep archive size reasonable and portable.
- Astra CAM's core toolpath generation, operation planning, postprocessing, and verification are 100% pure Java and run completely independently of native C++ binaries.
