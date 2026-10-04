$ErrorActionPreference = "Stop"

$JAVAC = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot\bin\javac.exe"
$JAVAFX_LIB = "C:\Users\arunp\.m2\repository\org\openjfx"
$jars = Get-ChildItem -Path $JAVAFX_LIB -Filter "*.jar" -Recurse | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -ExpandProperty FullName
$mp = ($jars -join ";")

if (!(Test-Path "target/classes")) {
    New-Item -ItemType Directory -Path "target/classes" -Force | Out-Null
}

$sources = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$sourcesFile = "scratch/sources.txt"
$sources | Out-File -FilePath $sourcesFile -Encoding ascii

Write-Host "Compiling $($sources.Count) production sources..."
& $JAVAC -J-Xms64m -J-Xmx512m --module-path $mp --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing -d target/classes "@$sourcesFile"

if ($LASTEXITCODE -ne 0) {
    Write-Error "Production compilation failed"
    exit 1
}
Write-Host "Production compilation successful!"
