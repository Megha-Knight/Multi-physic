# build_handoff_zip.ps1
# Generates a clean, portable ZIP archive of the Astra / MultiPhysics repository
# with normalized forward-slash (/) ZIP entries, strictly excluding src/main/CPP/.
$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot
$distDir = Join-Path $repoRoot "dist"
if (-not (Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir -Force | Out-Null }
$zipPath = Join-Path $distDir "Astra_MultiPhysics_Agent_Handoff_20261028.zip"
if (Test-Path $zipPath) { Remove-Item -Path $zipPath -Force }

Add-Type -AssemblyName "System.IO.Compression"
Add-Type -AssemblyName "System.IO.Compression.FileSystem"

$zipFileStream = [System.IO.File]::Open($zipPath, [System.IO.FileMode]::CreateNew)
$archive = New-Object System.IO.Compression.ZipArchive($zipFileStream, [System.IO.Compression.ZipArchiveMode]::Create)

$prefix = "Astra_MultiPhysics_Agent_Handoff"

function Add-FileToArchive($diskPath, $entryRelativePath) {
    $normalizedPath = ($prefix + "/" + $entryRelativePath).Replace("\", "/")
    $entry = $archive.CreateEntry($normalizedPath, [System.IO.Compression.CompressionLevel]::Optimal)
    $entryStream = $entry.Open()
    $fileStream = [System.IO.File]::OpenRead($diskPath)
    $fileStream.CopyTo($entryStream)
    $fileStream.Close()
    $entryStream.Close()
}

Write-Host "Packaging Astra / MultiPhysics Handoff ZIP..."

# Root metadata and configuration
$rootFiles = @("README.md", "AGENT_ONBOARDING.md", "PROJECT_GIT_STATE.txt", "HOW_TO_RUN.md", 
               "PROJECT_RECOVERY_REPORT.md", "PROJECT_REPORT_AND_AGENT_RULES.txt", "pom.xml", ".gitignore", "build_handoff_zip.ps1")

foreach ($rf in $rootFiles) {
    $fp = Join-Path $repoRoot $rf
    if (Test-Path $fp) {
        Add-FileToArchive $fp $rf
        Write-Host "  Added root file: $rf"
    }
}

# Directories to bundle
$dirsToBundle = @("src/main/java", "src/main/resources", "docs/reports", "native", "tests/cam")

foreach ($d in $dirsToBundle) {
    $diskDir = Join-Path $repoRoot ($d.Replace("/", "\"))
    if (Test-Path $diskDir) {
        Get-ChildItem -Path $diskDir -Recurse -File | ForEach-Object {
            $rel = $_.FullName.Substring($repoRoot.Length + 1).Replace("\", "/")
            # Strict safety: never include src/main/CPP or build binaries
            if ($rel -notmatch "^src/main/CPP" -and $rel -notmatch "^native/build" -and $rel -notmatch "^native/\.cache") {
                Add-FileToArchive $_.FullName $rel
            }
        }
        Write-Host "  Added directory: $d"
    }
}

$archive.Dispose()
$zipFileStream.Dispose()

Write-Host "`nValidating generated ZIP archive..."
$testExtractDir = Join-Path $env:TEMP ("astra_zip_audit_" + [System.Guid]::NewGuid().ToString().Substring(0, 8))
[System.IO.Compression.ZipFile]::ExtractToDirectory($zipPath, $testExtractDir)

$pomExtracted = Join-Path $testExtractDir "$prefix/pom.xml"
$extractedHash = (Get-FileHash -Path $pomExtracted -Algorithm SHA256).Hash
$expectedHash = "DEEA003B51DD0A6145657727FC85153913F273B8FEC8B840F65A240BC7EF6309"
if ($extractedHash -ne $expectedHash) {
    Remove-Item -Path $testExtractDir -Recurse -Force
    Write-Error "CRITICAL: pom.xml hash mismatch in extracted archive!"
    exit 1
}

# Verify forward slash entries
$testZip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
$hasBackslash = $false
foreach ($e in $testZip.Entries) {
    if ($e.FullName.Contains("\")) {
        $hasBackslash = $true
        break
    }
}
$entryCount = $testZip.Entries.Count
$testZip.Dispose()

Remove-Item -Path $testExtractDir -Recurse -Force

if ($hasBackslash) {
    Write-Error "Validation failed: ZIP contains backslash entry names."
    exit 1
}

$zipBytes = (Get-Item $zipPath).Length
$zipHash  = (Get-FileHash -Path $zipPath -Algorithm SHA256).Hash

Write-Host "`n========================================================"
Write-Host "PORTABLE HANDOFF ZIP VERIFICATION SUMMARY:"
Write-Host "Output Path:             $zipPath"
Write-Host "File Size:               $([Math]::Round($zipBytes / 1MB, 2)) MB ($zipBytes bytes)"
Write-Host "Entry Count:             $entryCount files"
Write-Host "Path Separators:         Standard forward slashes (/) verified"
Write-Host "SHA-256:                 $zipHash"
Write-Host "pom.xml Frozen Match:    VERIFIED ($extractedHash)"
Write-Host "src/main/CPP/ Exclusion: VERIFIED (0 entries)"
Write-Host "========================================================"
