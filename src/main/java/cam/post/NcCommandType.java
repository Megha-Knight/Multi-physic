package cam.post;

/**
 * Neutral NC machine command types independent of controller-specific syntax.
 */
public enum NcCommandType {
    COMMENT,
    PROGRAM_HEADER,
    SAFE_START,
    WORK_COORDINATE_SYSTEM,
    TOOL_CHANGE,
    SPINDLE_START,
    SPINDLE_STOP,
    COOLANT_ON,
    COOLANT_OFF,
    RAPID_MOVE,
    FEED_MOVE,
    PLUNGE_MOVE,
    CANNED_CYCLE_DRILL,
    CANNED_CYCLE_CANCEL,
    SAFE_RETRACT,
    PROGRAM_END
}
