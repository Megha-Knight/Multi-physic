$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Users\arunp\.jdks\openjdk-25"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$jfxPath = "C:\Users\arunp\.m2\repository\org\openjfx"
$jfxModules = "$jfxPath\javafx-base\17.0.16\javafx-base-17.0.16-win.jar;$jfxPath\javafx-graphics\17.0.16\javafx-graphics-17.0.16-win.jar;$jfxPath\javafx-controls\17.0.16\javafx-controls-17.0.16-win.jar"
$cp = "target\classes;$jfxModules"

Write-Host "Compiling TestStage3.java..." -ForegroundColor Cyan
javac --module-path $jfxModules --add-modules javafx.controls,javafx.graphics,javafx.base -cp $cp -d target\classes scratch\TestStage3.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to compile TestStage3.java"
    exit 1
}

Write-Host "Running TestStage3..." -ForegroundColor Cyan
java --module-path $jfxModules --add-modules javafx.controls,javafx.graphics,javafx.base -cp $cp TestStage3

if ($LASTEXITCODE -ne 0) {
    Write-Error "TestStage3 failed with exit code $LASTEXITCODE"
    exit 1
}

Write-Host "TestStage3 completed successfully!" -ForegroundColor Green
