package ui.workspace.drafting.revolve;

/**
 * revolve_axis_type_ui_main.java
 * Classifies the reference coordinate axis used for parametric revolution.
 */
public enum revolve_axis_type_ui_main {
    X_AXIS("X Axis"),
    Y_AXIS("Y Axis"),
    Z_AXIS("Z Axis"),
    CUSTOM_AXIS("Custom Axis");

    private final String label;

    revolve_axis_type_ui_main(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
