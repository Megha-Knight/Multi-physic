package ui.workspace.drafting.sweep;

/**
 * sweep_orientation_ui_main.java
 * Orientation mode for profile alignment along a sweep trajectory.
 */
public enum sweep_orientation_ui_main {
    FIXED("Fixed Orientation"),
    FOLLOW_PATH("Follow Path");

    private final String label;

    sweep_orientation_ui_main(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
