package ui.workspace.drafting.features;

/**
 * dependency_type_ui_main.java
 * Classifies the semantic relationship between a dependent and its dependency.
 */
public enum dependency_type_ui_main {
    HOST("Host Body"),
    FACE("Face Reference"),
    FEATURE("Feature Dependency"),
    SEED("Seed Feature"),
    PATTERN("Pattern Dependency"),
    GEOMETRY("Geometry Dependency"),
    HOST_BODY("Host Body"),
    SEED_FEATURE("Seed Feature"),
    SKETCH_PROFILE("Sketch Profile"),
    FACE_REFERENCE("Face Reference");

    private final String label;

    dependency_type_ui_main(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
