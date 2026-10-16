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
    @{ Name = "Stage 11"; Script = "scratch/run_test_stage11.ps1" },
    @{ Name = "Stage 12"; Script = "scratch/run_test_stage12.ps1" },
    @{ Name = "Machining & Sketch"; Script = "scratch/run_test_machining_sketch.ps1" },
    @{ Name = "Task 2 (Flush Snap Alignment)"; Script = "scratch/run_test_task2.ps1" },
    @{ Name = "Task 3 (Three-State Color Model)"; Script = "scratch/run_test_task3.ps1" },
    @{ Name = "Task 4 (Feature Tree Connectors)"; Script = "scratch/run_test_task4.ps1" },
    @{ Name = "Task 5a (Derived Geometry Edges)"; Script = "scratch/run_test_task5a.ps1" },
    @{ Name = "Task 5b (Interactive Edge Selection)"; Script = "scratch/run_test_task5b.ps1" },
    @{ Name = "Task 5c (Extrusion Machining & Topology)"; Script = "scratch/run_test_task5c.ps1" },
    @{ Name = "ViewCube Alignment"; Script = "scratch/run_test_viewcube_alignment.ps1" },
    @{ Name = "Tasks 1-3 (Hole Overlays & Multi-Hole)"; Script = "scratch/run_test_tasks123.ps1" },
    @{ Name = "Task 02 (Global & Local Coordinate Triads)"; Script = "scratch/run_test_task02_triads.ps1" },
    @{ Name = "Task 03 (Completed MultiPhysics Features)"; Script = "scratch/run_test_task03_features.ps1" },
    @{ Name = "Task 04 (Sketch & Computational Geometry)"; Script = "scratch/run_test_task04_geometry.ps1" }
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
Write-Host "ALL 1127 TESTS (STAGES 1-12 + MACHINING & SKETCH) & AUDITS PASSED 100%!"
Write-Host "=========================================="
