# Astra / MultiPhysics Native C++ Backend

## 1. Overview

This directory contains the native C++ backend for the **Astra / MultiPhysics** CAD application.
It provides high-performance numerical routines, computational geometry primitives, and future physics solvers, integrated with the JavaFX frontend using modern Java 25 Foreign Function & Memory (FFM) API.

---

## 2. Directory Structure

```
native/
├── CMakeLists.txt                  # Standard CMake build definition
├── README.md                       # Build and usage documentation
├── ARCHITECTURE_DECISION.md        # Architecture decision record (ADR)
├── build_native.ps1                # Direct build script using LLVM/Clang
├── run_native_integration_test.ps1 # End-to-end native + Java FFM test runner
├── include/
│   └── astra_native.h              # Public C-ABI interface definition
├── src/
│   └── astra_native.cpp            # Native implementation
├── tests/
│   └── test_native_main.cpp        # Standalone native C++ test executable
└── build/                          # Generated intermediate & output binaries (gitignored)
    ├── bin/
    │   ├── astra_native.dll        # 64-bit Windows dynamic library
    │   ├── libastra_native.a       # Import library
    │   └── test_native_main.exe    # Standalone test runner
    └── obj/                        # Intermediate object files
```

---

## 3. Toolchain & Environment Requirements

- **Primary Compiler**: Project-local LLVM/Clang at `src/main/cpp/bin/clang++.exe`
  - Version: **21.1.6**
  - Toolchain Location: `src/main/cpp/` (385 files, 657.36 MB, see [TOOLCHAIN_MANIFEST.md](file:///e:/MultiPhysics/src/main/cpp/TOOLCHAIN_MANIFEST.md))
- **C++ Standard**: C++17
- **Target Architecture**: `x86_64-w64-windows-gnu` / Windows Universal C Runtime (UCRT)
- **External Fallback**: `E:\LLVM\bin\clang++.exe` (100% untouched and preserved)
- **Java Runtime**: JDK 25 LTS (`C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot`)
- **Interop Mechanism**: Java Foreign Function & Memory (FFM) API (`java.lang.foreign`)

The build script `native/build_native.ps1` prioritizes `src/main/cpp/bin/clang++.exe` by default.

---

## 4. Build Instructions

### Quick Build (PowerShell):
From the repository root or `native/` folder:
```powershell
powershell -ExecutionPolicy Bypass -File native/build_native.ps1
```

This compiles:
1. `native/build/bin/astra_native.dll`
2. `native/build/bin/test_native_main.exe`
3. Runs the standalone native test suite.
4. Copies `astra_native.dll` to `target/classes/native/astra_native.dll` for runtime access.

### CMake Build (Alternative):
```powershell
mkdir native/build_cmake
cd native/build_cmake
cmake -G Ninja -DCMAKE_CXX_COMPILER="E:/LLVM/bin/clang++.exe" ..
ninja
```

---

## 5. Running Tests

### Standalone Native Test:
```powershell
& "native/build/bin/test_native_main.exe"
```

### Full Java-to-Native Integration Test:
```powershell
powershell -ExecutionPolicy Bypass -File native/run_native_integration_test.ps1
```

---

## 6. Exported C ABI Functions

| Function | Signature | Description |
| :--- | :--- | :--- |
| `astra_get_version` | `uint32_t astra_get_version(void)` | Returns packed semantic version (`0x000100`). |
| `astra_get_version_string` | `const char* astra_get_version_string(void)` | Returns human-readable version string. |
| `astra_vector_magnitude` | `int32_t astra_vector_magnitude(double x, double y, double z, double* out_mag)` | Computes Euclidean norm $\sqrt{x^2+y^2+z^2}$. |
| `astra_ray_triangle_intersect`| `int32_t astra_ray_triangle_intersect(...)` | Möller–Trumbore 3D ray-triangle intersection. |
| `astra_transform_point_4x4` | `int32_t astra_transform_point_4x4(const double* pt, const double* mat, double* out)` | Affine 3D point transformation. |
| `astra_get_last_error` | `int32_t astra_get_last_error(char* buffer, int32_t len)` | Thread-local error string retrieval. |

---

## 7. Java Bridge Usage

```java
import backend.native_bridge.astra_native_bridge_ui_main;

astra_native_bridge_ui_main bridge = astra_native_bridge_ui_main.getInstance();
if (bridge.isAvailable()) {
    double mag = bridge.vectorMagnitude(3.0, 4.0, 12.0); // Returns 13.0
    System.out.println("Native magnitude: " + mag);
}
```
