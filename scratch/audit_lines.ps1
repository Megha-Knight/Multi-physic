$files = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse
$over = 0
foreach ($f in $files) {
    $lines = (Get-Content $f.FullName).Count
    if ($lines -gt 200) {
        Write-Host "VIOLATION: $($f.FullName) has $lines lines"
        $over++
    }
}
if ($over -eq 0) {
    Write-Host "ALL $($files.Count) FILES COMPLY (<= 200 lines)!"
} else {
    Write-Error "$over files exceed 200 lines!"
    exit 1
}
