package cam.plan;

import cam.post.NcProgram;
import cam.post.verifier.NcVerificationReport;
import cam.simulation.StockRemovalGrid;
import cam.toolpath.Toolpath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Immutable aggregated result of a complete CAM plan execution run,
 * tracking stage status, toolpaths, simulation results, NC output, and export eligibility.
 */
public class CamPlanExecutionResult {

    private final String planName;
    private final CamPlanExecutionStatus status;
    private final List<String> errors;
    private final List<String> executionOrder;
    private final Map<String, Toolpath> toolpaths;
    private final StockRemovalGrid simulationGrid;
    private final NcProgram ncProgram;
    private final NcVerificationReport verificationReport;
    private final boolean exportEligible;

    public CamPlanExecutionResult(String planName, CamPlanExecutionStatus status,
                                  List<String> errors, List<String> executionOrder,
                                  Map<String, Toolpath> toolpaths, StockRemovalGrid simulationGrid,
                                  NcProgram ncProgram, NcVerificationReport verificationReport) {
        this.planName = (planName != null) ? planName : "Operation Plan";
        this.status = (status != null) ? status : CamPlanExecutionStatus.NOT_STARTED;
        this.errors = (errors != null) ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
        this.executionOrder = (executionOrder != null) ? Collections.unmodifiableList(new ArrayList<>(executionOrder)) : List.of();
        this.toolpaths = (toolpaths != null) ? Collections.unmodifiableMap(new LinkedHashMap<>(toolpaths)) : Map.of();
        this.simulationGrid = simulationGrid;
        this.ncProgram = ncProgram;
        this.verificationReport = verificationReport;
        this.exportEligible = this.status.isSuccess() && ncProgram != null
            && verificationReport != null && verificationReport.isApproved();
    }

    public static CamPlanExecutionResult failure(String planName, CamPlanExecutionStatus status, List<String> errors) {
        return new CamPlanExecutionResult(planName, status, errors, List.of(), Map.of(), null, null, null);
    }

    public static CamPlanExecutionResult failure(String planName, CamPlanExecutionStatus status, String singleError) {
        return failure(planName, status, List.of(singleError));
    }

    public void exportNc(Path destination, boolean overwrite) throws IOException {
        if (destination == null) throw new IllegalArgumentException("Destination path cannot be null.");
        if (!isExportEligible() || ncProgram == null) {
            throw new IllegalStateException("NC program is not eligible for export due to preflight errors or unexecuted plan.");
        }
        if (Files.exists(destination) && !overwrite) {
            throw new FileAlreadyExistsException(destination.toString(), null, "File already exists and overwrite is false.");
        }
        Path parent = destination.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.writeString(destination, ncProgram.toGcodeText(), StandardCharsets.UTF_8);
    }

    public String getPlanName()                             { return planName; }
    public CamPlanExecutionStatus getStatus()               { return status; }
    public boolean isSuccess()                              { return status.isSuccess(); }
    public boolean isExportEligible()                       { return exportEligible; }
    public List<String> getErrors()                         { return errors; }
    public List<String> getExecutionOrder()                 { return executionOrder; }
    public Map<String, Toolpath> getToolpaths()             { return toolpaths; }
    public Toolpath getToolpath(String opId)                { return toolpaths.get(opId); }
    public StockRemovalGrid getSimulationGrid()             { return simulationGrid; }
    public NcProgram getNcProgram()                         { return ncProgram; }
    public NcVerificationReport getVerificationReport()     { return verificationReport; }

    @Override
    public String toString() {
        return "CamPlanExecutionResult{status=" + status + ", exportEligible=" + exportEligible
            + ", order=" + executionOrder + ", errors=" + errors.size() + "}";
    }
}
