package cam.toolpath;

import cam.model.CamPoint3D;

/**
 * An individual linear motion segment within a CNC toolpath trajectory.
 */
public record ToolpathSegment(
    MotionType motionType,
    CamPoint3D start,
    CamPoint3D end,
    double feedRateMmPerMin,
    double spindleRpm
) {
    public ToolpathSegment {
        if (motionType == null) throw new IllegalArgumentException("MotionType cannot be null.");
        if (start == null || end == null) throw new IllegalArgumentException("Start and End points cannot be null.");
        if (feedRateMmPerMin < 0.0) throw new IllegalArgumentException("Feed rate cannot be negative.");
        if (spindleRpm < 0.0) throw new IllegalArgumentException("Spindle speed cannot be negative.");
    }

    public double getLength() {
        return start.distanceTo(end);
    }

    public double getDurationSeconds(double machineRapidFeedMmPerMin) {
        double dist = getLength();
        if (dist <= 1e-9) return 0.0;
        double effectiveFeed = (motionType == MotionType.RAPID || motionType == MotionType.RETRACT)
            ? Math.max(100.0, machineRapidFeedMmPerMin)
            : Math.max(1.0, feedRateMmPerMin);
        return (dist / effectiveFeed) * 60.0;
    }
}
