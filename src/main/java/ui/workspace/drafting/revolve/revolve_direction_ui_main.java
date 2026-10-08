package ui.workspace.drafting.revolve;

/**
 * revolve_direction_ui_main.java
 * Rotation direction mode for parametric Revolve features.
 */
public enum revolve_direction_ui_main {
    FORWARD("Forward (CCW)"),
    REVERSE("Reverse (CW)"),
    SYMMETRIC("Symmetric");

    private final String label;

    revolve_direction_ui_main(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
