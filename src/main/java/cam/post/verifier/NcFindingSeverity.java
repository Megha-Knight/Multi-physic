package cam.post.verifier;

/**
 * Severity level for NC preflight validation findings.
 */
public enum NcFindingSeverity {
    /** Informational notation or optimization advice. */
    INFO,
    /** Non-blocking warning (e.g. potential performance issue or non-standard convention). */
    WARNING,
    /** Blocking error that strictly forbids loading or executing the program on CNC machines. */
    ERROR
}
