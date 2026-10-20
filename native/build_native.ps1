# build_native.ps1
# Compiles Astra Native Backend using existing LLVM/Clang toolchain
$ErrorActionPreference = "Stop"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$LLVM_BIN = "E:\LLVM\bin"
$CLANG_CXX = Join-Path $LLVM_BIN "clang++.exe"

if (-not (Test-Path $CLANG_CXX)) {
    Write-Error "Clang++ not found at $CLANG_CXX. Please verify LLVM installation."
    exit 1
}

Write-Host "=========================================================="
Write-Host "Astra Native Backend Build System"
Write-Host "Using Compiler: $CLANG_CXX"
& $CLANG_CXX --version
Write-Host "=========================================================="

$BUILD_DIR = Join-Path $SCRIPT_DIR "build"
$BIN_DIR   = Join-Path $BUILD_DIR "bin"
$OBJ_DIR   = Join-Path $BUILD_DIR "obj"

if (-not (Test-Path $BIN_DIR)) { New-Item -ItemType Directory -Path $BIN_DIR -Force | Out-Null }
if (-not (Test-Path $OBJ_DIR)) { New-Item -ItemType Directory -Path $OBJ_DIR -Force | Out-Null }

$INC_DIR  = Join-Path $SCRIPT_DIR "include"
$SRC_DIR  = Join-Path $SCRIPT_DIR "src"
$TEST_DIR = Join-Path $SCRIPT_DIR "tests"

$DLL_OUT  = Join-Path $BIN_DIR "astra_native.dll"
$LIB_OUT  = Join-Path $BIN_DIR "libastra_native.a"
$TEST_OUT = Join-Path $BIN_DIR "test_native_main.exe"

# 1. Compile DLL
Write-Host "Building shared library: $DLL_OUT..."
$SRC_FILE = Join-Path $SRC_DIR "astra_native.cpp"
& $CLANG_CXX --target=x86_64-w64-windows-gnu -std=c++17 -O2 -shared -DASTRA_BUILD_DLL `
    "-I$INC_DIR" $SRC_FILE -o $DLL_OUT "-Wl,--out-implib,$LIB_OUT"

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to compile astra_native.dll"
    exit 1
}
Write-Host "Build SUCCESS: $DLL_OUT created."

# 2. Compile Standalone Test
Write-Host "Building test executable: $TEST_OUT..."
$TEST_SRC = Join-Path $TEST_DIR "test_native_main.cpp"
& $CLANG_CXX --target=x86_64-w64-windows-gnu -std=c++17 -O2 `
    "-I$INC_DIR" $TEST_SRC "-L$BIN_DIR" -lastra_native -o $TEST_OUT

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to compile test_native_main.exe"
    exit 1
}
Write-Host "Build SUCCESS: $TEST_OUT created."

# 3. Execute Standalone Test
Write-Host "`nRunning standalone native tests..."
$env:PATH = "$BIN_DIR;$env:PATH"
& $TEST_OUT

if ($LASTEXITCODE -ne 0) {
    Write-Error "Native tests failed with exit code $LASTEXITCODE"
    exit 1
}

# 4. Copy to target classes for Java runtime convenience
$TARGET_NATIVE = "E:\MultiPhysics\target\classes\native"
if (-not (Test-Path $TARGET_NATIVE)) { New-Item -ItemType Directory -Path $TARGET_NATIVE -Force | Out-Null }
Copy-Item -Path $DLL_OUT -Destination (Join-Path $TARGET_NATIVE "astra_native.dll") -Force
Write-Host "Deployed astra_native.dll to $TARGET_NATIVE"

Write-Host "`nAll native build and test steps completed successfully!"
