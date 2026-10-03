package ui.workspace.drafting.features;

/**
 * feature_state_ui_main.java
 * Lifecycle state of a parametric CAD feature in the dependency graph.
 */
public enum feature_state_ui_main {
    CLEAN("Clean"),
    DIRTY("Dirty"),
    INVALID("Invalid"),
    SUPPRESSED("Suppressed");

    public static final feature_state_ui_main VALID = CLEAN;

    private final String label;

    feature_state_ui_main(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isValid() {
        return this != INVALID;
    }

    public boolean isGenerative() {
        return this == CLEAN || this == DIRTY;
    }
}
