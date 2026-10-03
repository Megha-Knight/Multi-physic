package ui.workspace.drafting.topology;

/**
 * hole_region_kind_ui_main.java
 * Semantic classification of topology regions created or modified by hole features.
 */
public enum hole_region_kind_ui_main {
    REMAINING_FACE("Remaining Face", false, false, true),
    HOLE_WALL("Hole Wall", true, true, false),
    HOLE_FLOOR("Hole Floor", true, false, false),
    BORE_WALL("Bore Wall", true, true, false),
    ENTRY_OPENING("Entry Opening", false, false, false),
    EXIT_OPENING("Exit Opening", false, false, false);

    private final String label;
    private final boolean interior;
    private final boolean wall;
    private final boolean remainingFace;

    hole_region_kind_ui_main(String label, boolean interior, boolean wall, boolean remainingFace) {
        this.label = label;
        this.interior = interior;
        this.wall = wall;
        this.remainingFace = remainingFace;
    }

    public String getLabel() { return label; }
    public boolean isInterior() { return interior; }
    public boolean isWall() { return wall; }
    public boolean isFloor() { return this == HOLE_FLOOR; }
    public boolean isRemainingFace() { return remainingFace; }
    public boolean isOpening() { return this == ENTRY_OPENING || this == EXIT_OPENING; }
    public boolean isThroughBore() { return this == BORE_WALL; }
}
