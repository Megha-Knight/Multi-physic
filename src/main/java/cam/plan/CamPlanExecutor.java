package cam.plan;

import cam.model.ManufacturingSetup;
import cam.operation.CamOperationType;
import cam.post.Iso6983Postprocessor;
import cam.post.NcProgram;
import cam.post.verifier.NcProgramVerifier;
import cam.post.verifier.NcVerificationReport;
import cam.simulation.StockRemovalGrid;
import cam.simulation.StockRemovalSimulator;
import cam.toolpath.Toolpath;

import java.util.*;

/**
 * Headless orchestration engine executing validated CAM operation plans through
 * dependency sequencing, toolpath generation, stock simulation, NC postprocessing,
 * and independent preflight verification.
 */
public class CamPlanExecutor {

    public CamPlanValidationResult validate(CamOperationPlan plan) {
        if (plan == null) return CamPlanValidationResult.invalid("Operation plan cannot be null.");
        ManufacturingSetup setup = plan.getSetup();
        if (setup == null) return CamPlanValidationResult.invalid("Manufacturing setup cannot be null.");
        if (setup.getStock() == null) return CamPlanValidationResult.invalid("Setup stock dimensions cannot be null.");
        if (setup.getMcs() == null) return CamPlanValidationResult.invalid("Setup machine coordinate system cannot be null.");
        if (plan.getProfile() == null) return CamPlanValidationResult.invalid("Controller profile cannot be null.");

        CamPlanValidationResult seqResult = CamPlanSequencer.sequence(plan);
        if (!seqResult.isValid()) return seqResult;

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>(seqResult.getWarnings());

        for (CamPlannedOperation op : plan.getOperations()) {
            if (op.getOperation() == null) {
                errors.add("Operation '" + op.getId() + "' has null executable operation reference.");
                continue;
            }
            if (op.getTool() == null) {
                errors.add("Operation '" + op.getId() + "' has null tool reference.");
                continue;
            }
            if (!isSupported(op.getOperationType())) {
                errors.add("Operation '" + op.getId() + "' specifies unsupported operation type: " + op.getOperationType());
                continue;
            }
            String opErr = op.getOperation().validate(setup);
            if (opErr != null && !opErr.isBlank()) {
                errors.add("Operation '" + op.getId() + "' validation error: " + opErr);
            }
        }

        if (!errors.isEmpty()) return CamPlanValidationResult.invalid(errors, warnings);
        return CamPlanValidationResult.valid(seqResult.getResolvedOrder(), warnings);
    }

    public CamPlanExecutionResult execute(CamOperationPlan plan) {
        CamPlanValidationResult valResult = validate(plan);
        if (!valResult.isValid()) {
            return CamPlanExecutionResult.failure(plan != null ? plan.getName() : "Unknown",
                CamPlanExecutionStatus.VALIDATION_FAILED, valResult.getErrors());
        }

        ManufacturingSetup setup = plan.getSetup();
        List<String> order = valResult.getResolvedOrder();
        List<CamPlannedOperation> orderedOps = new ArrayList<>();
        for (String id : order) orderedOps.add(plan.getOperation(id));

        Map<String, Toolpath> toolpaths = new LinkedHashMap<>();
        try {
            for (CamPlannedOperation pop : orderedOps) {
                Toolpath tp = pop.getOperation().generateToolpath(setup);
                if (tp == null || tp.isEmpty()) {
                    return CamPlanExecutionResult.failure(plan.getName(),
                        CamPlanExecutionStatus.EXECUTION_FAILED, "Operation '" + pop.getId() + "' produced empty or null toolpath.");
                }
                toolpaths.put(pop.getId(), tp);
            }
        } catch (Exception e) {
            return CamPlanExecutionResult.failure(plan.getName(),
                CamPlanExecutionStatus.EXECUTION_FAILED, "Toolpath generation error: " + e.getMessage());
        }

        StockRemovalGrid grid = null;
        try {
            grid = new StockRemovalGrid(setup.getStock(), 1.0);
            StockRemovalSimulator sim = new StockRemovalSimulator(grid);
            for (CamPlannedOperation pop : orderedOps) {
                sim.simulateToolpath(toolpaths.get(pop.getId()), pop.getTool());
            }
        } catch (Exception e) {
            return CamPlanExecutionResult.failure(plan.getName(),
                CamPlanExecutionStatus.SIMULATION_FAILED, "Stock simulation error: " + e.getMessage());
        }

        NcProgram ncProgram;
        try {
            setup.clearOperations();
            for (CamPlannedOperation pop : orderedOps) setup.addOperation(pop.getOperation());
            Iso6983Postprocessor post = new Iso6983Postprocessor(plan.getProfile());
            ncProgram = post.postprocess(setup);
        } catch (Exception e) {
            return CamPlanExecutionResult.failure(plan.getName(),
                CamPlanExecutionStatus.NC_GENERATION_FAILED, "NC postprocessing error: " + e.getMessage());
        }

        NcProgramVerifier verifier = new NcProgramVerifier(plan.getProfile(), setup);
        NcVerificationReport report = verifier.verify(ncProgram);

        CamPlanExecutionStatus status;
        if (!report.isApproved()) {
            status = CamPlanExecutionStatus.PREFLIGHT_FAILED;
        } else if (!report.getWarnings().isEmpty()) {
            status = CamPlanExecutionStatus.COMPLETED_WITH_WARNINGS;
        } else {
            status = CamPlanExecutionStatus.COMPLETED;
        }

        return new CamPlanExecutionResult(plan.getName(), status, List.of(),
            order, toolpaths, grid, ncProgram, report);
    }

    public static boolean isSupported(CamOperationType type) {
        return type == CamOperationType.SPOT_DRILLING
            || type == CamOperationType.DEEP_HOLE_DRILLING
            || type == CamOperationType.PRISMATIC_ROUGHING;
    }
}
