$ErrorActionPreference = "Stop"

$JAVAC = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\javac.exe"
$JAVA  = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\java.exe"

$JFX_PATH = "C:\Users\arunp\.m2\repository\org\openjfx\javafx-controls\17.0.16;C:\Users\arunp\.m2\repository\org\openjfx\javafx-graphics\17.0.16;C:\Users\arunp\.m2\repository\org\openjfx\javafx-base\17.0.16"

Write-Host "Compiling TestTask5bEdgePick.java..."
& $JAVAC -encoding UTF-8 `
    --module-path $JFX_PATH `
    --add-modules javafx.controls,javafx.graphics,javafx.base `
    -cp "target/classes;." `
    -d scratch/bin `
    scratch/TestTask5bEdgePick.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation of TestTask5bEdgePick.java failed."
    exit 1
}

Write-Host "Running TestTask5bEdgePick..."
& $JAVA `
    --module-path $JFX_PATH `
    --add-modules javafx.controls,javafx.graphics,javafx.base `
    -cp "target/classes;scratch/bin" `
    scratch.TestTask5bEdgePick

if ($LASTEXITCODE -ne 0) {
    Write-Error "TestTask5bEdgePick failed!"
    exit 1
}

Write-Host "TestTask5b completed successfully!" -ForegroundColor Green
