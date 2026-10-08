package ui.workspace.drafting.features;

/**
 * feature_type_ui_main.java
 * Classifies parametric features within the CAD feature-history graph.
 */
public enum feature_type_ui_main {
    BASE_SOLID("Base Solid"),
    SOLID_BODY("Solid Body"),
    SKETCH("Sketch"),
    SKETCH_PROFILE("Sketch Profile"),
    HOLE("Hole"),
    HOLE_FEATURE("Hole"),
    PATTERN("Pattern"),
    PATTERN_FEATURE("Pattern"),
    EXTRUDE("Extrusion"),
    EXTRUDE_FEATURE("Extrusion"),
    FILLET("Fillet"),
    CHAMFER("Chamfer"),
    DRAFT("Draft"),
    BOOLEAN("Boolean Operation"),
    BOOLEAN_UNION("Boolean Union"),
    BOOLEAN_SUBTRACT("Boolean Subtract"),
    BOOLEAN_INTERSECT("Boolean Intersect"),
    SHELL("Shell / Thickness"),
    SHELL_FEATURE("Shell"),
    LOFT("Loft"),
    LOFT_FEATURE("Loft"),
    SWEEP("Sweep"),
    SWEEP_FEATURE("Sweep");

    private final String label;

    feature_type_ui_main(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
