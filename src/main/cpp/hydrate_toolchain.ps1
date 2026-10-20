# hydrate_toolchain.ps1
# Deterministically hydrates and verifies src/main/cpp toolchain from a source location or manifest
param(
    [string]$SourcePath = "E:\LLVM",
    [switch]$VerifyOnly = $false
)

$ErrorActionPreference = "Stop"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$MANIFEST_PATH = Join-Path $SCRIPT_DIR "TOOLCHAIN_MANIFEST.md"

if (-not (Test-Path $MANIFEST_PATH)) {
    Write-Error "Toolchain manifest not found at $MANIFEST_PATH"
    exit 1
}

Write-Host "=========================================================="
Write-Host "Astra / MultiPhysics Toolchain Hydration & Integrity Check"
Write-Host "Destination: $SCRIPT_DIR"
Write-Host "Source:      $SourcePath"
Write-Host "=========================================================="

# Read manifest table lines
$lines = Get-Content $MANIFEST_PATH
$inTable = $false
$missingFiles = 0
$corruptFiles = 0
$validFiles = 0

foreach ($line in $lines) {
    if ($line -match '^\|\s*`([^`]+)`\s*\|\s*([^\|]+)\|\s*([0-9]+)\s*\|\s*`([0-9A-Fa-f]+)`\s*\|') {
        $relPath = $matches[1]
        $category = $matches[2].Trim()
        $expectedSize = [int64]$matches[3].Trim()
        $expectedHash = $matches[4].Trim()

        $dstFile = Join-Path $SCRIPT_DIR ($relPath.Replace("/", "\"))

        if (-not (Test-Path $dstFile)) {
            $missingFiles++
            if (-not $VerifyOnly) {
                $srcFile = Join-Path $SourcePath ($relPath.Replace("/", "\"))
                if (Test-Path $srcFile) {
                    $parent = Split-Path -Parent $dstFile
                    if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
                    Copy-Item -Path $srcFile -Destination $dstFile -Force
                    $actualHash = (Get-FileHash -Path $dstFile -Algorithm SHA256).Hash
                    if ($actualHash -eq $expectedHash) {
                        Write-Host "Hydrated: $relPath"
                        $validFiles++
                    } else {
                        Write-Error "Hydration hash mismatch for $relPath"
                        $corruptFiles++
                    }
                } else {
                    Write-Warning "Source file missing for hydration: $srcFile"
                }
            }
        } else {
            # File exists, verify size & hash
            $item = Get-Item $dstFile
            if ($item.Length -ne $expectedSize) {
                Write-Warning "Size mismatch for $relPath (expected $expectedSize, got $($item.Length))"
                $corruptFiles++
            } else {
                $actualHash = (Get-FileHash -Path $dstFile -Algorithm SHA256).Hash
                if ($actualHash -ne $expectedHash) {
                    Write-Warning "Hash mismatch for $relPath"
                    $corruptFiles++
                } else {
                    $validFiles++
                }
            }
        }
    }
}

Write-Host "`nToolchain Verification Summary:"
Write-Host "  Valid Files:   $validFiles"
Write-Host "  Missing Files: $missingFiles"
Write-Host "  Corrupt Files: $corruptFiles"

if ($corruptFiles -gt 0 -or ($VerifyOnly -and $missingFiles -gt 0)) {
    Write-Error "Toolchain integrity check failed."
    exit 1
}

Write-Host "Toolchain verification successful! Ready for build.`n"
