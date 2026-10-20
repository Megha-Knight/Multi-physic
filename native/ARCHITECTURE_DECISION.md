# Architecture Decision Record (ADR): Astra Native C++ Backend

**Status:** Accepted  
**Date:** 2026-10-20  
**Context:** MultiPhysics (Astra) CAD / Multiphysics Simulation Platform  

---

## 1. Context & Problem Statement

Astra / MultiPhysics requires a high-performance native computation backend for computational geometry, mesh operations, numerical calculations, and future multiphysics solvers. The existing application is built on Java 25 and JavaFX.

An LLVM installation exists at `E:\LLVM`. We need to establish a stable, maintainable, and testable foundation connecting Java to native C++ using this existing toolchain without:
1. Rebuilding or modifying the JavaFX frontend.
2. Rearranging or modifying the `E:\LLVM` installation folder.
3. Guessing or introducing unnecessary compiler/runtime dependencies.

---

## 2. Toolchain Inspection & Ground Truth

Inspection of `E:\LLVM` revealed:
- **Executables (`E:\LLVM\bin`):** `clang++.exe`, `clang.exe`, `lld-link.exe`, `llvm-ar.exe`.
- **Clang Version:** `21.1.6` (targeting `x86_64-pc-windows-msvc` by default).
- **Installed Headers (`E:\LLVM\include`):** Contains `clang-c` (C API for Clang) and `llvm-c/lto.h`, `llvm-c/Remarks.h`.
- **CRITICAL FINDING on LLVM C++ Headers:** `E:\LLVM\include` does **NOT** contain full LLVM C++ API headers (such as `llvm/IR/Module.h`, `llvm/IR/IRBuilder.h`).
- **Standard Library / CRT:** When configured with `--target=x86_64-w64-windows-gnu`, Clang 21.1.6 automatically leverages the installed Windows Universal C Runtime (UCRT) and `libstdc++` from WinLibs MinGW-w64, producing standalone 64-bit Windows dynamic libraries (`.dll`).

---

## 3. Evaluation of Alternatives

### Alternative A: Native C++ Shared Library with Standard C ABI (Selected)
- **Concept:** Write modular C++17 components in `native/src/` with a public C ABI header (`native/include/astra_native.h`). Compile using `E:\LLVM\bin\clang++.exe` into `astra_native.dll`.
- **Pros:**
  - Standard, human-readable, debuggable C++ code.
  - Can use modern C++17/20 abstractions, STL, and numerical libraries (e.g. Eigen).
  - Clean C ABI guarantees binary stability and isolates Java from C++ compiler layout differences.
  - Standalone native testing without needing a JVM.
  - Zero LLVM C++ API dependencies required.
- **Cons:**
  - Requires compiling the shared library for the target architecture.
- **Verdict: ACCEPTED.**

### Alternative B: Direct LLVM Intermediate Representation (IR) Generation
- **Concept:** Generate LLVM IR directly from Java and JIT-compile it using LLVM execution engines.
- **Evaluation:**
  - `E:\LLVM\include` does not contain the LLVM C++ headers or execution engine libraries.
  - Generating raw IR text or bytecodes introduces heavy coupling to specific LLVM IR opcodes and versions.
  - Unnecessary for static numerical algorithms and CAD operations.
- **Verdict: REJECTED.**

### Alternative C: Java Interop Mechanism — FFM vs JNI vs IPC

#### Option C.1: Java Foreign Function & Memory API (FFM - `java.lang.foreign`) (Selected)
- **Evaluation:**
  - Standardized in Java 22, core in Java 25 (`org.pass.astra` runs on Java 25 LTS).
  - Eliminates all JNI boilerplate: zero `jni.h` dependency, zero `JNIEnv*` parameter passing, zero C-side JNI wrapper functions.
  - C ABI functions are invoked directly via `MethodHandle` and `Linker.nativeLinker()`.
  - Type-safe off-heap memory management via `Arena` and `MemorySegment`.
  - Native C++ codebase remains completely pure and independent of Java.
- **Verdict: ACCEPTED as primary interop mechanism.**

#### Option C.2: Java Native Interface (JNI)
- **Evaluation:**
  - Requires `<jni.h>` from the JDK, complex name mangling (`Java_backend_native_bridge_...`), and manual reference management.
  - Pollutes native C++ codebase with JVM-specific signatures.
- **Verdict: REJECTED** in favor of FFM on Java 25.

#### Option C.3: Separate-Process IPC (Sockets / Named Pipes / stdio)
- **Evaluation:**
  - Adds IPC serialization and synchronization latency to fine-grained geometric and numerical queries.
- **Verdict: REJECTED** for in-process high-frequency computation.

---

## 4. Architectural Rules & Invariants

1. **Strict C ABI Boundary:**
   - All exported functions use `extern "C" __declspec(dllexport)`.
   - Fixed-width types (`int32_t`, `uint32_t`, `double`).
   - No C++ classes, standard library containers (`std::vector`, `std::string`), or compiler-specific layouts cross the DLL boundary.
2. **Explicit Memory Ownership:**
   - Caller allocates and owns all input/output memory buffers.
   - Java uses `Arena.ofConfined()` to allocate off-heap memory that is deterministically freed upon exiting the try-with-resources block.
3. **Deterministic Error Handling:**
   - Native functions return integer status codes (`ASTRA_OK = 0`, negative for errors).
   - Thread-local error messages retrieved via `astra_get_last_error()`.
   - Java bridge catches errors and throws descriptive exceptions without crashing the JVM.
4. **Decoupling from Presentation Layer:**
   - The native backend and Java bridge (`backend.native_bridge.astra_native_bridge_ui_main`) have zero JavaFX dependencies.
   - Long-running native operations must be dispatched to background workers, never blocking the JavaFX UI Application thread.

---

## 5. Next Stage Roadmap

1. **Eigen Matrix Integration:** Introduce Eigen headers for high-performance sparse/dense linear algebra and finite-element solvers.
2. **CAD Geometry Acceleration:** Offload B-Rep tessellation, mesh slicing, and CSG Boolean operations to the native backend.
3. **Physics Solvers:** Implement native finite element analysis (FEA) and computational fluid dynamics (CFD) pipelines.
