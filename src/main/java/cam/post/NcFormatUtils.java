package cam.post;

import cam.model.CamPoint3D;

/**
 * Common formatting and safety validation utilities for NC postprocessing.
 */
public final class NcFormatUtils {

    private NcFormatUtils() {}

    public static void validateCoordinates(CamPoint3D pt) {
        if (pt == null || !Double.isFinite(pt.x()) || !Double.isFinite(pt.y()) || !Double.isFinite(pt.z())) {
            throw new IllegalArgumentException("Invalid non-finite toolpath coordinate: " + pt);
        }
    }

    public static void validatePositive(double val, String name) {
        if (Double.isNaN(val) || val <= 0.0) {
            throw new IllegalArgumentException(name + " must be strictly positive (got: " + val + ").");
        }
    }

    public static void validateCuttingState(ModalState modal) {
        if (!modal.isSetupExecuted()) {
            throw new IllegalStateException("Cutting motion attempted before machine setup initialization.");
        }
        if (!modal.isSpindleRunning() || modal.getActiveSpindleRpm() <= 0.0) {
            throw new IllegalStateException("Cutting motion attempted while spindle is stopped.");
        }
    }

    public static void validateCannedCycle(double bottomZ, double rPlaneZ, double peckDepth, double stockTopZ) {
        if (bottomZ >= rPlaneZ) {
            throw new IllegalArgumentException(String.format(
                "Canned cycle bottom Z (%.3f) must be strictly below reference R-plane (%.3f).", bottomZ, rPlaneZ));
        }
        if (rPlaneZ < stockTopZ - 1e-4) {
            throw new IllegalArgumentException(String.format(
                "Canned cycle R-plane (%.3f) cannot be below stock top surface (%.3f).", rPlaneZ, stockTopZ));
        }
        if (Double.isNaN(peckDepth) || peckDepth < 0.0) {
            throw new IllegalArgumentException("Peck increment Q cannot be negative (got: " + peckDepth + ").");
        }
    }

    public static void appendCoordinate(StringBuilder sb, String axis, double targetVal,
                                        double currentVal, ControllerProfile profile) {
        boolean coordChanged = Double.isNaN(currentVal) || Math.abs(targetVal - currentVal) > 1e-4;
        if (!profile.isModalSuppressionEnabled() || coordChanged) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(axis).append(profile.formatNumber(targetVal));
        }
    }

    public static String sanitizeProgName(String name) {
        if (name == null || name.isBlank()) return "1001";
        String digits = name.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? "1001" : digits.substring(0, Math.min(4, digits.length()));
    }
}
