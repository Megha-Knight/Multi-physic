package ui.workspace.drafting.sketch;

public enum sketch_constraint_status_ui_main {
    UNDER_CONSTRAINED("Under-constrained"),
    FULLY_CONSTRAINED("Fully-constrained"),
    OVER_CONSTRAINED("Over-constrained"),
    INVALID("Invalid / Contradictory");

    private final String label;
    sketch_constraint_status_ui_main(String label) { this.label = label; }
    public String getLabel() { return label; }
}
