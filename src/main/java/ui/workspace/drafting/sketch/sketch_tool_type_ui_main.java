package ui.workspace.drafting.sketch;

/**
 * sketch_tool_type_ui_main.java
 * Interactive sketch viewport drawing tools.
 */
public enum sketch_tool_type_ui_main {
    SELECT("Select", "Select & edit sketch geometry"),
    LINE("Line", "Draw 2-point line segments"),
    CIRCLE("Circle", "Draw center-radius circles"),
    RECTANGLE("Rectangle", "Draw 2-corner rectangles"),
    ARC("Arc", "Draw 3-point center-start-end circular arcs");

    private final String label;
    private final String description;

    sketch_tool_type_ui_main(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public boolean isDrawingTool() { return this != SELECT; }
}
