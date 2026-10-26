package cam.operation;

/**
 * Retract strategy for deep-hole peck drilling cycles.
 */
public enum PeckRetractStrategy {
    /**
     * G83 Deep-Hole Peck Drilling:
     * Full retract to the approach/R-plane after each peck for complete chip evacuation and coolant wash.
     */
    FULL_RETRACT("G83", "Full retract to R-plane for complete chip evacuation"),

    /**
     * G73 High-Speed Chip-Breaking:
     * Partial retract (relief step) by a small clearance increment (e.g. 0.5-1.0mm) to break chips without exiting hole.
     */
    CHIP_BREAK("G73", "Partial retract to break continuous chips without exiting hole");

    private final String standardCycleCode;
    private final String description;

    PeckRetractStrategy(String standardCycleCode, String description) {
        this.standardCycleCode = standardCycleCode;
        this.description = description;
    }

    public String getStandardCycleCode() { return standardCycleCode; }
    public String getDescription()       { return description; }
}
