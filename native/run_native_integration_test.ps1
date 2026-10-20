# run_native_integration_test.ps1
# Builds native backend and executes Java 25 FFM integration test
$ErrorActionPreference = "Stop"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT_ROOT = Split-Path -Parent $SCRIPT_DIR
$BUILD_NATIVE = Join-Path $SCRIPT_DIR "build_native.ps1"

Write-Host "=========================================================="
Write-Host "Step 1: Building Astra Native Backend (C++)"
Write-Host "=========================================================="
& powershell -ExecutionPolicy Bypass -File $BUILD_NATIVE

if ($LASTEXITCODE -ne 0) {
    Write-Error "Native build failed"
    exit 1
}

Write-Host "`n=========================================================="
Write-Host "Step 2: Executing Java 25 FFM End-to-End Integration Test"
Write-Host "=========================================================="
$JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
$JAVAC = Join-Path $JAVA_HOME "bin\javac.exe"
$JAVA  = Join-Path $JAVA_HOME "bin\java.exe"

$TEST_JAVA = "E:\MultiPhysics_Supporting_Files\scratch\TestJavaNativeIntegration.java"
$CP = "E:\MultiPhysics\target\classes;E:\MultiPhysics_Supporting_Files\scratch"

Write-Host "Compiling $TEST_JAVA..."
& $JAVAC -cp $CP $TEST_JAVA
if ($LASTEXITCODE -ne 0) {
    Write-Error "Test compilation failed"
    exit 1
}

Write-Host "Running TestJavaNativeIntegration..."
& $JAVA -ea --enable-native-access=ALL-UNNAMED -cp $CP TestJavaNativeIntegration

if ($LASTEXITCODE -ne 0) {
    Write-Error "Java integration test failed with exit code $LASTEXITCODE"
    exit 1
}

Write-Host "`nAll Java-to-Native Integration Tests Passed Successfully!"
