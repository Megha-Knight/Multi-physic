$ErrorActionPreference = "Stop"
$javafxLib = "C:\Users\arunp\.m2\repository\org\openjfx"
$jars = @(
    "$javafxLib\javafx-base\17.0.16\javafx-base-17.0.16-win.jar",
    "$javafxLib\javafx-graphics\17.0.16\javafx-graphics-17.0.16-win.jar",
    "$javafxLib\javafx-controls\17.0.16\javafx-controls-17.0.16-win.jar",
    "$javafxLib\javafx-swing\25\javafx-swing-25-win.jar"
)
$cp = ($jars -join ";") + ";target/classes"

Write-Host "Compiling TestTask06StabilityAndStress.java..."
& "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\javac.exe" -cp $cp -d target/classes scratch/TestTask06StabilityAndStress.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Test compilation failed"
    exit 1
}

Write-Host "Executing TestTask06StabilityAndStress..."
& "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\java.exe" -cp $cp scratch.TestTask06StabilityAndStress

if ($LASTEXITCODE -ne 0) {
    Write-Error "Test execution failed"
    exit $LASTEXITCODE
}
