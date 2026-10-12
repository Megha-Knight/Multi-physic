$ErrorActionPreference = "Stop"
$javafxLib = "C:\Users\arunp\.m2\repository\org\openjfx"
$jars = @(
    "$javafxLib\javafx-base\17.0.16\javafx-base-17.0.16-win.jar",
    "$javafxLib\javafx-graphics\17.0.16\javafx-graphics-17.0.16-win.jar",
    "$javafxLib\javafx-controls\17.0.16\javafx-controls-17.0.16-win.jar"
)
$cp = ($jars -join ";") + ";target/classes"

Write-Host "Compiling TestTask3ColorModel.java..."
javac -cp $cp -d target/classes scratch/TestTask3ColorModel.java
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "Running TestTask3ColorModel..."
java -cp $cp TestTask3ColorModel
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "TestTask3 completed successfully!"
