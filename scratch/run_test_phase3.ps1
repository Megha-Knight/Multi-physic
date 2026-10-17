$ErrorActionPreference = "Stop"

$JAVAC = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\javac.exe"
$JAVA  = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\java.exe"

$JFX_PATH = "C:\Users\arunp\.m2\repository\org\openjfx"
$CP = "target/classes;" +
      "$JFX_PATH/javafx-base/17.0.16/javafx-base-17.0.16-win.jar;" +
      "$JFX_PATH/javafx-graphics/17.0.16/javafx-graphics-17.0.16-win.jar;" +
      "$JFX_PATH/javafx-controls/17.0.16/javafx-controls-17.0.16-win.jar"

Write-Host "Compiling TestPhase3ActualCadBehavior.java..."
& $JAVAC -cp $CP -d target/classes scratch/TestPhase3ActualCadBehavior.java
if ($LASTEXITCODE -ne 0) {
    Write-Error "Test compilation failed"
    exit 1
}

Write-Host "Running TestPhase3ActualCadBehavior..."
& $JAVA -cp $CP scratch.TestPhase3ActualCadBehavior
if ($LASTEXITCODE -ne 0) {
    Write-Error "Phase 3 Test execution failed"
    exit 1
}
