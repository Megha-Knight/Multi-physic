package ui.workspace.drafting.revolve;

import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.profiles.profile_validator_ui_main;

/**
 * revolve_validator_ui_main.java
 * Validates parametric Revolve feature configurations, profiles, axes, and angles.
 */
public final class revolve_validator_ui_main {

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String msg) {
            this.valid = valid;
            this.message = msg;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String msg) { return new ValidationResult(false, msg); }
    }

    private revolve_validator_ui_main() {}

    public static ValidationResult validate(profile_reference_ui_main profile, revolve_axis_ui_main axis,
                                            double angle, boolean requireSolid) {
        if (profile == null) {
            return ValidationResult.fail("Revolve profile reference cannot be null.");
        }
        var pRes = profile_validator_ui_main.validate(profile, requireSolid);
        if (!pRes.isValid()) {
            return ValidationResult.fail("Invalid revolve profile: " + pRes.getMessage());
        }

        if (axis == null || !axis.isValid()) {
            return ValidationResult.fail("Revolve rotation axis cannot be null or zero-length.");
        }

        if (Double.isNaN(angle) || Double.isInfinite(angle) || Math.abs(angle) < 1e-3) {
            return ValidationResult.fail("Revolve angle must be a non-zero finite number (found: " + angle + ").");
        }

        return ValidationResult.ok();
    }
}
