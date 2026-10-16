$ErrorActionPreference = "Stop"

$JAVAC = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\javac.exe"
$JAVA  = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\java.exe"

$JFX_PATH = "C:\Users\arunp\.m2\repository\org\openjfx\javafx-controls\17.0.16;C:\Users\arunp\.m2\repository\org\openjfx\javafx-graphics\17.0.16;C:\Users\arunp\.m2\repository\org\openjfx\javafx-base\17.0.16"

Write-Host "Compiling TestTask4TreeConnectors.java..."
& $JAVAC -encoding UTF-8 `
    --module-path $JFX_PATH `
    --add-modules javafx.controls,javafx.graphics,javafx.base `
    -cp "target/classes;." `
    -d scratch/bin `
    scratch/TestTask4TreeConnectors.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation of TestTask4TreeConnectors.java failed."
    exit 1
}

Write-Host "Running TestTask4TreeConnectors..."
& $JAVA `
    --module-path $JFX_PATH `
    --add-modules javafx.controls,javafx.graphics,javafx.base `
    -cp "target/classes;scratch/bin" `
    scratch.TestTask4TreeConnectors

if ($LASTEXITCODE -ne 0) {
    Write-Error "TestTask4TreeConnectors failed!"
    exit 1
}

Write-Host "TestTask4 completed successfully!" -ForegroundColor Green
