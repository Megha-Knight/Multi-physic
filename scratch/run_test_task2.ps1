$ErrorActionPreference = "Stop"
$javafxLib = "C:\Users\arunp\.m2\repository\org\openjfx"
$jars = @(
    "$javafxLib\javafx-base\17.0.16\javafx-base-17.0.16-win.jar",
    "$javafxLib\javafx-graphics\17.0.16\javafx-graphics-17.0.16-win.jar",
    "$javafxLib\javafx-controls\17.0.16\javafx-controls-17.0.16-win.jar"
)
$cp = ($jars -join ";") + ";target/classes"
Write-Host "Compiling TestTask2ExtrudeMagnet.java..."
javac -cp $cp -d target/classes scratch/TestTask2ExtrudeMagnet.java
if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Host "Running TestTask2ExtrudeMagnet..."
java -cp $cp scratch.TestTask2ExtrudeMagnet
if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Host "TestTask2 completed successfully!"
