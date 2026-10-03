package ui.workspace.drafting.sketch;

public enum sketch_constraint_type_ui_main {
    COINCIDENT("Coincident", "Point A coincident with Point B or Origin"),
    HORIZONTAL("Horizontal", "Line segment is horizontal (dy = 0)"),
    VERTICAL("Vertical", "Line segment is vertical (dx = 0)"),
    PARALLEL("Parallel", "Two lines are parallel"),
    PERPENDICULAR("Perpendicular", "Two lines are perpendicular"),
    EQUAL("Equal", "Equal length for lines or equal radius for circles"),
    DISTANCE("Distance", "Fixed distance between points or along line"),
    RADIUS("Radius", "Fixed radius for circle or arc"),
    ANGLE("Angle", "Fixed angle between lines"),
    TANGENT("Tangent", "Line tangent to circle/arc");

    private final String label;
    private final String description;

    sketch_constraint_type_ui_main(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public boolean isDimensional() {
        return this == DISTANCE || this == RADIUS || this == ANGLE;
    }
}
