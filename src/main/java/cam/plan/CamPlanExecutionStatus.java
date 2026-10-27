package cam.plan;

/**
 * Execution status stages and terminal outcomes for CAM operation planning.
 */
public enum CamPlanExecutionStatus {
    NOT_STARTED,
    VALIDATION_FAILED,
    EXECUTION_FAILED,
    SIMULATION_FAILED,
    NC_GENERATION_FAILED,
    PREFLIGHT_FAILED,
    COMPLETED_WITH_WARNINGS,
    COMPLETED;

    public boolean isSuccess() {
        return this == COMPLETED || this == COMPLETED_WITH_WARNINGS;
    }

    public boolean isTerminalFailure() {
        return this == VALIDATION_FAILED
            || this == EXECUTION_FAILED
            || this == SIMULATION_FAILED
            || this == NC_GENERATION_FAILED
            || this == PREFLIGHT_FAILED;
    }
}
