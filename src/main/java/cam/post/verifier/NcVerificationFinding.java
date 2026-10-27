package cam.post.verifier;

/**
 * An individual diagnostic finding generated during independent NC program verification.
 *
 * @param code        Stable finding code classifying the issue.
 * @param severity    Severity (INFO, WARNING, ERROR).
 * @param blockIndex  0-based index of the block in the program.
 * @param blockText   Raw G-code text of the offending block.
 * @param message     Human-readable explanation and remediation instructions.
 */
public record NcVerificationFinding(
    NcFindingCode code,
    NcFindingSeverity severity,
    int blockIndex,
    String blockText,
    String message
) {
    public boolean isBlocking() {
        return severity == NcFindingSeverity.ERROR;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s at block %d ('%s'): %s",
            severity, code, blockIndex, blockText != null ? blockText.trim() : "", message);
    }
}
