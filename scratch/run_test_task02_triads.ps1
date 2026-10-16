$ErrorActionPreference = "Stop"
$javafxLib = "C:\Users\arunp\.m2\repository\org\openjfx"
$jars = @(
    "$javafxLib\javafx-base\17.0.16\javafx-base-17.0.16-win.jar",
    "$javafxLib\javafx-graphics\17.0.16\javafx-graphics-17.0.16-win.jar",
    "$javafxLib\javafx-controls\17.0.16\javafx-controls-17.0.16-win.jar"
)
$cp = ($jars -join ";") + ";target/classes"

Write-Host "Compiling TestGlobalAndLocalTriads.java..."
& "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\javac.exe" -cp $cp -d target/classes scratch/TestGlobalAndLocalTriads.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Test compilation failed"
    exit 1
}

Write-Host "Executing TestGlobalAndLocalTriads..."
& "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\java.exe" -cp $cp scratch.TestGlobalAndLocalTriads

if ($LASTEXITCODE -ne 0) {
    Write-Error "Test execution failed"
    exit $LASTEXITCODE
}
