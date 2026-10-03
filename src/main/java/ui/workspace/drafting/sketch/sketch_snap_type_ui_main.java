package ui.workspace.drafting.sketch;

public enum sketch_snap_type_ui_main {
    ENDPOINT("Endpoint"),
    MIDPOINT("Midpoint"),
    CENTER("Center"),
    ORIGIN("Origin (0,0)"),
    INTERSECTION("Intersection"),
    HORIZONTAL_ALIGN("Horizontal Alignment"),
    VERTICAL_ALIGN("Vertical Alignment");

    private final String label;
    sketch_snap_type_ui_main(String label) { this.label = label; }
    public String getLabel() { return label; }
}
