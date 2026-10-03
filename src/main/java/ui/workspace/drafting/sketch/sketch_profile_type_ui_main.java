package ui.workspace.drafting.sketch;

public enum sketch_profile_type_ui_main {
    OPEN("Open Profile"),
    CLOSED("Closed Profile"),
    MULTIPLE_LOOPS("Multiple Loops (Outer + Inner)"),
    SELF_INTERSECTING("Self-Intersecting Profile"),
    INVALID("Invalid / Degenerate Profile");

    private final String label;
    sketch_profile_type_ui_main(String label) { this.label = label; }
    public String getLabel() { return label; }
    public boolean isExtrudable() { return this == CLOSED || this == MULTIPLE_LOOPS; }
}
