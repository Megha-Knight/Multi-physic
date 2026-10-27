package cam.post;

import cam.model.CamPoint3D;

/**
 * Immutable neutral machine command representing an atomic NC instruction
 * independent of controller-specific syntax.
 */
public record NcCommand(
    NcCommandType type,
    CamPoint3D target,
    double feedRateMmMin,
    double spindleRpm,
    int toolNumber,
    String text,
    double rPlaneZ,
    double peckDepthMm,
    double dwellSeconds
) {
    public static NcCommand comment(String text) {
        return new NcCommand(NcCommandType.COMMENT, null, 0, 0, 0, text, 0, 0, 0);
    }

    public static NcCommand programHeader(String progName) {
        return new NcCommand(NcCommandType.PROGRAM_HEADER, null, 0, 0, 0, progName, 0, 0, 0);
    }

    public static NcCommand safeStart() {
        return new NcCommand(NcCommandType.SAFE_START, null, 0, 0, 0, null, 0, 0, 0);
    }

    public static NcCommand workCoordinateSystem(String wcs) {
        return new NcCommand(NcCommandType.WORK_COORDINATE_SYSTEM, null, 0, 0, 0, wcs, 0, 0, 0);
    }

    public static NcCommand toolChange(int toolNumber, String toolName) {
        return new NcCommand(NcCommandType.TOOL_CHANGE, null, 0, 0, toolNumber, toolName, 0, 0, 0);
    }

    public static NcCommand toolLengthComp(int hRegister, double zClearance) {
        return new NcCommand(NcCommandType.TOOL_LENGTH_COMP, null, 0, 0, hRegister, null, zClearance, 0, 0);
    }

    public static NcCommand toolLengthCancel() {
        return new NcCommand(NcCommandType.TOOL_LENGTH_CANCEL, null, 0, 0, 0, null, 0, 0, 0);
    }

    public static NcCommand spindleStart(double rpm) {
        return new NcCommand(NcCommandType.SPINDLE_START, null, 0, rpm, 0, null, 0, 0, 0);
    }

    public static NcCommand spindleStop() {
        return new NcCommand(NcCommandType.SPINDLE_STOP, null, 0, 0, 0, null, 0, 0, 0);
    }

    public static NcCommand rapid(CamPoint3D target) {
        return new NcCommand(NcCommandType.RAPID_MOVE, target, 0, 0, 0, null, 0, 0, 0);
    }

    public static NcCommand feed(CamPoint3D target, double feedRate, double rpm) {
        return new NcCommand(NcCommandType.FEED_MOVE, target, feedRate, rpm, 0, null, 0, 0, 0);
    }

    public static NcCommand plunge(CamPoint3D target, double feedRate, double rpm) {
        return new NcCommand(NcCommandType.PLUNGE_MOVE, target, feedRate, rpm, 0, null, 0, 0, 0);
    }

    public static NcCommand cannedCycle(String cycleGcode, CamPoint3D target, double rPlaneZ,
                                        double peckDepthMm, double feedRate) {
        return new NcCommand(NcCommandType.CANNED_CYCLE_DRILL, target, feedRate, 0, 0, cycleGcode, rPlaneZ, peckDepthMm, 0);
    }

    public static NcCommand cannedCycleCancel() {
        return new NcCommand(NcCommandType.CANNED_CYCLE_CANCEL, null, 0, 0, 0, null, 0, 0, 0);
    }

    public static NcCommand safeRetract() {
        return new NcCommand(NcCommandType.SAFE_RETRACT, null, 0, 0, 0, null, 0, 0, 0);
    }

    public static NcCommand programEnd() {
        return new NcCommand(NcCommandType.PROGRAM_END, null, 0, 0, 0, null, 0, 0, 0);
    }
}
