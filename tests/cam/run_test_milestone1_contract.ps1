# run_test_milestone1_contract.ps1
# Runs Milestone 1 Geometry & Topology Contract Verification Harness
$ErrorActionPreference = "Stop"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT_ROOT = Split-Path -Parent (Split-Path -Parent $SCRIPT_DIR)
$JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
$JAVAC = Join-Path $JAVA_HOME "bin\javac.exe"
$JAVA  = Join-Path $JAVA_HOME "bin\java.exe"

Write-Host "=========================================================="
Write-Host "Milestone 1 Contract Test Execution"
Write-Host "Java Runtime: $JAVA_HOME"
Write-Host "=========================================================="

$JAVAFX_LIB = "C:\Users\arunp\.m2\repository\org\openjfx"
$jars = Get-ChildItem -Path $JAVAFX_LIB -Filter "*.jar" -Recurse | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -ExpandProperty FullName
$mp = ($jars -join ";")
$BUILD_DIR = Join-Path $PROJECT_ROOT "target\scratch_test_classes"
if (-not (Test-Path $BUILD_DIR)) { New-Item -ItemType Directory -Path $BUILD_DIR -Force | Out-Null }

$SRC_DIR = Join-Path $PROJECT_ROOT "src\main\java"
$TEST_SRC = Join-Path $SCRIPT_DIR "TestMilestone1ContractHarness.java"

Write-Host "Compiling Milestone 1 test harness and dependencies..."
& $JAVAC --module-path $mp --add-modules javafx.controls,javafx.fxml,javafx.graphics -d $BUILD_DIR -sourcepath "$SRC_DIR;$PROJECT_ROOT" `
    (Join-Path $SRC_DIR "backend\native_bridge\astra_native_bridge_ui_main.java") `
    (Join-Path $SRC_DIR "backend\native_bridge\astra_geometry_native_bridge_ui_main.java") `
    (Join-Path $SRC_DIR "ui\workspace\drafting\features\feature_type_ui_main.java") `
    (Join-Path $SRC_DIR "ui\workspace\drafting\features\feature_state_ui_main.java") `
    (Join-Path $SRC_DIR "ui\workspace\drafting\features\dependency_type_ui_main.java") `
    (Join-Path $SRC_DIR "ui\workspace\drafting\features\feature_dependency_ui_main.java") `
    (Join-Path $SRC_DIR "ui\workspace\drafting\features\feature_node_ui_main.java") `
    (Join-Path $SRC_DIR "ui\workspace\drafting\features\entity_lineage_contract_ui_main.java") `
    (Join-Path $SRC_DIR "ui\workspace\drafting\features\feature_dependency_graph_ui_main.java") `
    $TEST_SRC

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation of Milestone 1 contract test harness failed."
    exit 1
}

Write-Host "Executing Milestone 1 Contract Test Harness..."
& $JAVA --enable-native-access=ALL-UNNAMED --module-path $mp --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "$BUILD_DIR;$PROJECT_ROOT" tests.cam.TestMilestone1ContractHarness

if ($LASTEXITCODE -ne 0) {
    Write-Error "Milestone 1 contract test harness reported failures."
    exit 1
}

Write-Host "Milestone 1 Contract Tests PASSED successfully!"
