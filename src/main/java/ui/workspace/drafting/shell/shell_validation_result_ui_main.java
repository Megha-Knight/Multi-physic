package ui.workspace.drafting.shell;

/**
 * shell_validation_result_ui_main.java
 * Immutable validation assessment for CAD shell feature definitions.
 */
public final class shell_validation_result_ui_main {

    private final boolean valid;
    private final String reason;

    private shell_validation_result_ui_main(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason != null ? reason : "";
    }

    public static shell_validation_result_ui_main success() {
        return new shell_validation_result_ui_main(true, "Valid");
    }

    public static shell_validation_result_ui_main failure(String reason) {
        return new shell_validation_result_ui_main(false, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }
}
