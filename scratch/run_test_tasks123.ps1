$ErrorActionPreference = "Stop"
$javafxLib = "C:\Users\arunp\.m2\repository\org\openjfx"
$jars = @(
    "$javafxLib\javafx-base\17.0.16\javafx-base-17.0.16-win.jar",
    "$javafxLib\javafx-graphics\17.0.16\javafx-graphics-17.0.16-win.jar",
    "$javafxLib\javafx-controls\17.0.16\javafx-controls-17.0.16-win.jar"
)
$cp = ($jars -join ";") + ";target/classes"

Write-Host "Compiling TestTasks123HoleOverlayAndMultiHole.java..."
javac -cp $cp -d target/classes scratch/TestTasks123HoleOverlayAndMultiHole.java
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "Running TestTasks123HoleOverlayAndMultiHole..."
java -cp $cp scratch.TestTasks123HoleOverlayAndMultiHole
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "TestTasks123 completed successfully!"
