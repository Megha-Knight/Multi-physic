package ui.workspace.drafting.shell;

/**
 * shell_direction_ui_main.java
 * Defines the wall thickness offset direction for solid shelling operations.
 */
public enum shell_direction_ui_main {
    INWARD("Inward", -1.0),
    OUTWARD("Outward", 1.0);

    private final String label;
    private final double offsetMultiplier;

    shell_direction_ui_main(String label, double offsetMultiplier) {
        this.label = label;
        this.offsetMultiplier = offsetMultiplier;
    }

    public String getLabel() {
        return label;
    }

    public double getOffsetMultiplier() {
        return offsetMultiplier;
    }

    public boolean isInward() {
        return this == INWARD;
    }

    public boolean isOutward() {
        return this == OUTWARD;
    }
}
