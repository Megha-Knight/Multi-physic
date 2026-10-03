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
    EXTRUDE_FEATURE("Extrusion");

    private final String label;

    feature_type_ui_main(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
