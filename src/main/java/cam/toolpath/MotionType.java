package cam.toolpath;

/**
 * Standard motion types for CNC toolpath trajectories.
 */
public enum MotionType {
    RAPID("G0", "Rapid non-cutting transit at machine traverse rate"),
    FEED("G1", "Linear cutting feed at programmed feed rate"),
    PLUNGE("G1_PLUNGE", "Z-axis downward plunge feed into stock"),
    RETRACT("G0_RETRACT", "Rapid retract out of workpiece to clearance level");

    private final String gcodeEquivalent;
    private final String description;

    MotionType(String gcodeEquivalent, String description) {
        this.gcodeEquivalent = gcodeEquivalent;
        this.description = description;
    }

    public String getGcodeEquivalent() { return gcodeEquivalent; }
    public String getDescription()     { return description; }
    public boolean isCutting()         { return this == FEED || this == PLUNGE; }
}
