package ui.workspace.drafting.sketch;

public enum sketch_plane_type_ui_main {
    FACE("Planar Face"),
    BASE_XY("Base XY Plane"),
    BASE_XZ("Base XZ Plane (Ground)"),
    BASE_YZ("Base YZ Plane");

    private final String label;
    sketch_plane_type_ui_main(String label) { this.label = label; }
    public String getLabel() { return label; }
    public boolean isBasePlane() { return this != FACE; }
}
