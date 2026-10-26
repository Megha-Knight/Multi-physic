package cam.tool;

/**
 * Enumeration of cutting tool types in Astra CAM.
 */
public enum ToolType {
    SPOT_DRILL("Spot Drill", true),
    TWIST_DRILL("Twist Drill", true),
    FLAT_ENDMILL("Flat End Mill", false),
    BALL_ENDMILL("Ball End Mill", false),
    BULLNOSE_ENDMILL("Bullnose End Mill", false),
    CHAMFER_MILL("Chamfer Mill", false);

    private final String displayName;
    private final boolean holemaking;

    ToolType(String displayName, boolean holemaking) {
        this.displayName = displayName;
        this.holemaking = holemaking;
    }

    public String getDisplayName() { return displayName; }
    public boolean isHolemaking()   { return holemaking; }
}
