$ErrorActionPreference = "Stop"

$JAVAC = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\javac.exe"
$JAVA  = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\java.exe"
$JAVAFX_LIB = "C:\Users\arunp\.m2\repository\org\openjfx"

# Gather javafx modules
$jars = Get-ChildItem -Path $JAVAFX_LIB -Filter "*.jar" -Recurse | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -ExpandProperty FullName
$mp = ($jars -join ";")

$CLASSPATH = "target/classes;scratch;$mp"

Write-Host "Compiling TestDynamicRibbonAndTopology.java..."
& $JAVAC -J-Xms32m -J-Xmx256m --module-path $mp --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp $CLASSPATH -d scratch scratch/TestDynamicRibbonAndTopology.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation of TestDynamicRibbonAndTopology failed"
    exit 1
}

Write-Host "Running TestDynamicRibbonAndTopology..."
& $JAVA -Xms32m -Xmx512m -ea --module-path $mp --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp $CLASSPATH TestDynamicRibbonAndTopology

if ($LASTEXITCODE -ne 0) {
    Write-Error "TestDynamicRibbonAndTopology failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
} else {
    Write-Host "TestDynamicRibbonAndTopology completed successfully!"
}
