$ErrorActionPreference = "Stop"

$JAVAC = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\javac.exe"
$JAVA  = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\java.exe"
$JAVAFX_LIB = "C:\Users\arunp\.m2\repository\org\openjfx"

# Gather javafx modules
$jars = Get-ChildItem -Path $JAVAFX_LIB -Filter "*.jar" -Recurse | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -ExpandProperty FullName
$mp = ($jars -join ";")

$CLASSPATH = "target/classes;scratch;$mp"

Write-Host "Compiling TestStage7.java..."
& $JAVAC -J-Xms32m -J-Xmx256m --module-path $mp --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp $CLASSPATH -d scratch scratch/TestStage7.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation of TestStage7 failed"
    exit 1
}

Write-Host "Running TestStage7..."
& $JAVA -Xms32m -Xmx512m -ea --module-path $mp --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp $CLASSPATH TestStage7

if ($LASTEXITCODE -ne 0) {
    Write-Error "TestStage7 failed with exit code $LASTEXITCODE"
    exit 1
}

Write-Host "TestStage7 completed successfully!"
