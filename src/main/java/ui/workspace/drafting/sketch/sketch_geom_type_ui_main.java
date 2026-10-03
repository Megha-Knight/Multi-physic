package ui.workspace.drafting.sketch;

public enum sketch_geom_type_ui_main {
    LINE("Line"),
    CIRCLE("Circle"),
    ARC("Arc"),
    RECTANGLE("Rectangle"),
    POLYLINE("Polyline");

    private final String label;
    sketch_geom_type_ui_main(String label) { this.label = label; }
    public String getLabel() { return label; }
}
