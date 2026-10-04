package ui.workspace.drafting.topology;

/**
 * boundary_condition_ui_main.java
 * Classification of feature spatial relationship to host face boundaries.
 */
public enum boundary_condition_ui_main {
    CONTAINED("Contained in Face"),
    TANGENT("Tangent to Boundary"),
    CROSSES_EDGE("Crosses Face Boundary"),
    CROSSES_CORNER("Crosses Body Corner"),
    INTERSECTING_CAVITIES("Intersects Cavity");

    private final String label;

    boundary_condition_ui_main(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFullyContained() {
        return this == CONTAINED;
    }

    public boolean isMultiFaceOrBoundary() {
        return this == TANGENT || this == CROSSES_EDGE || this == CROSSES_CORNER;
    }

    public boolean isCollision() {
        return this == INTERSECTING_CAVITIES;
    }
}
