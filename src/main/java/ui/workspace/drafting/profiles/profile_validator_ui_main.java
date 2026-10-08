package ui.workspace.drafting.profiles;

import javafx.geometry.Point3D;
import java.util.List;

/**
 * profile_validator_ui_main.java
 * Validates cross-section profiles for geometric correctness and closed-loop requirements.
 */
public final class profile_validator_ui_main {

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

    private profile_validator_ui_main() {}

    public static ValidationResult validate(profile_reference_ui_main profile, boolean requireClosed) {
        if (profile == null) {
            return ValidationResult.fail("Profile reference cannot be null.");
        }
        profile_loop_ui_main loop = profile.getLoop();
        if (loop == null) {
            return ValidationResult.fail("Profile loop geometry cannot be null.");
        }
        if (loop.getPointCount() < 3) {
            return ValidationResult.fail("Profile must contain at least 3 vertices (found: " + loop.getPointCount() + ").");
        }
        if (requireClosed && !loop.isClosed()) {
            return ValidationResult.fail("Profile must be closed for solid creation.");
        }

        List<Point3D> pts = loop.getPoints();
        for (int i = 0; i < pts.size(); i++) {
            Point3D p = pts.get(i);
            if (p == null || Double.isNaN(p.getX()) || Double.isNaN(p.getY()) || Double.isNaN(p.getZ())
                || Double.isInfinite(p.getX()) || Double.isInfinite(p.getY()) || Double.isInfinite(p.getZ())) {
                return ValidationResult.fail("Profile vertex " + i + " contains invalid or NaN/Infinity coordinate.");
            }
        }

        for (int i = 0; i < pts.size(); i++) {
            Point3D p1 = pts.get(i);
            Point3D p2 = pts.get((i + 1) % pts.size());
            if (p1.distance(p2) < 1e-9) {
                return ValidationResult.fail("Profile contains duplicate zero-length segment between vertices " + i + " and " + ((i + 1) % pts.size()) + ".");
            }
        }

        double area = loop.computeArea();
        if (area <= 1e-6) {
            return ValidationResult.fail("Profile area must be greater than zero (computed area: " + area + ").");
        }

        return ValidationResult.ok();
    }
}
