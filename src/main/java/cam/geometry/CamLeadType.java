package cam.geometry;

/**
 * Types of toolpath lead-in and lead-out moves for CAM finishing.
 */
public enum CamLeadType {
    NONE,
    TANGENT_LINE,
    CIRCULAR_ARC,
    PERPENDICULAR_LINE
}
