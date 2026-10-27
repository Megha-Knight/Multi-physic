package cam.geometry;

/**
 * Immutable configuration for CAM toolpath lead-in and lead-out moves.
 * Validates non-negative parameters and bounds angular sweeps.
 */
public record CamLeadConfig(CamLeadType type, double linearLength, double arcRadius, double arcAngleDegrees) {

    public static final double DEFAULT_ARC_ANGLE_DEGREES = 90.0;
    public static final double MIN_LEAD_DIMENSION = 1e-4;

    public CamLeadConfig {
        if (type == null) {
            throw new IllegalArgumentException("CamLeadType cannot be null.");
        }
        if (type == CamLeadType.TANGENT_LINE || type == CamLeadType.PERPENDICULAR_LINE) {
            if (linearLength < MIN_LEAD_DIMENSION || !Double.isFinite(linearLength)) {
                throw new IllegalArgumentException("Linear lead length must be >= " + MIN_LEAD_DIMENSION + " (got: " + linearLength + ").");
            }
        } else if (type == CamLeadType.CIRCULAR_ARC) {
            if (arcRadius < MIN_LEAD_DIMENSION || !Double.isFinite(arcRadius)) {
                throw new IllegalArgumentException("Arc radius must be >= " + MIN_LEAD_DIMENSION + " (got: " + arcRadius + ").");
            }
            if (arcAngleDegrees <= 0.0 || arcAngleDegrees > 180.0 || !Double.isFinite(arcAngleDegrees)) {
                throw new IllegalArgumentException("Arc sweep angle must be in (0, 180] degrees (got: " + arcAngleDegrees + ").");
            }
        }
    }

    public static CamLeadConfig none() {
        return new CamLeadConfig(CamLeadType.NONE, 0.0, 0.0, 0.0);
    }

    public static CamLeadConfig tangentLine(double length) {
        return new CamLeadConfig(CamLeadType.TANGENT_LINE, length, 0.0, 0.0);
    }

    public static CamLeadConfig perpendicularLine(double length) {
        return new CamLeadConfig(CamLeadType.PERPENDICULAR_LINE, length, 0.0, 0.0);
    }

    public static CamLeadConfig circularArc(double radius, double angleDegrees) {
        return new CamLeadConfig(CamLeadType.CIRCULAR_ARC, 0.0, radius, angleDegrees);
    }

    public static CamLeadConfig circularArc(double radius) {
        return circularArc(radius, DEFAULT_ARC_ANGLE_DEGREES);
    }
}
