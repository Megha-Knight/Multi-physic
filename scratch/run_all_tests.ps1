$tests = @(
    @{ Name = "Stage 1 & 2"; Script = "scratch/run_test_ribbon_topology.ps1" },
    @{ Name = "Stage 3"; Script = "scratch/run_test_stage3.ps1" },
    @{ Name = "Stage 4"; Script = "scratch/run_test_stage4.ps1" },
    @{ Name = "Stage K"; Script = "scratch/run_test_stage_k.ps1" },
    @{ Name = "Stage 5"; Script = "scratch/run_test_stage5.ps1" },
    @{ Name = "Stage 6"; Script = "scratch/run_test_stage6.ps1" },
    @{ Name = "Stage 7"; Script = "scratch/run_test_stage7.ps1" },
    @{ Name = "Stage 8"; Script = "scratch/run_test_stage8.ps1" },
    @{ Name = "Stage 9"; Script = "scratch/run_test_stage9.ps1" },
    @{ Name = "Stage 10"; Script = "scratch/run_test_stage10.ps1" },
    @{ Name = "Stage 11"; Script = "scratch/run_test_stage11.ps1" }
)

Write-Host "=========================================="
Write-Host "Running Complete Test Suite (Stages 1 - 11)"
Write-Host "=========================================="

foreach ($t in $tests) {
    Write-Host "`n>>> Running $($t.Name) ($($t.Script))..."
    & powershell -ExecutionPolicy Bypass -File $t.Script
    if ($LASTEXITCODE -ne 0) {
        Write-Error "$($t.Name) FAILED with exit code $LASTEXITCODE"
        exit 1
    }
}

Write-Host "`n>>> Running Line Audit..."
& powershell -ExecutionPolicy Bypass -File scratch/audit_lines.ps1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Line Audit FAILED"
    exit 1
}

Write-Host "`n>>> Running Compile Sources..."
& powershell -ExecutionPolicy Bypass -File scratch/compile_sources.ps1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compile Sources FAILED"
    exit 1
}

Write-Host "`n=========================================="
Write-Host "ALL 1048 TESTS (STAGES 1-11) & AUDITS PASSED 100%!"
Write-Host "=========================================="
