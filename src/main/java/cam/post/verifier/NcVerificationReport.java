package cam.post.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured outcome of an independent NC program preflight verification audit.
 */
public class NcVerificationReport {

    private final String programName;
    private final String profileName;
    private final int totalBlocks;
    private final List<NcVerificationFinding> findings;
    private final NcInterpreterState finalState;

    public NcVerificationReport(String programName, String profileName, int totalBlocks,
                                List<NcVerificationFinding> findings, NcInterpreterState finalState) {
        this.programName = (programName != null) ? programName : "UNKNOWN_PROGRAM";
        this.profileName = (profileName != null) ? profileName : "UNKNOWN_PROFILE";
        this.totalBlocks = totalBlocks;
        this.findings = (findings != null) ? Collections.unmodifiableList(new ArrayList<>(findings)) : Collections.emptyList();
        this.finalState = finalState;
    }

    public boolean isApproved() {
        return getErrors().isEmpty();
    }

    public boolean hasBlockingErrors() {
        return !isApproved();
    }

    public List<NcVerificationFinding> getErrors() {
        return findings.stream().filter(f -> f.severity() == NcFindingSeverity.ERROR).toList();
    }

    public List<NcVerificationFinding> getWarnings() {
        return findings.stream().filter(f -> f.severity() == NcFindingSeverity.WARNING).toList();
    }

    public List<NcVerificationFinding> getInfos() {
        return findings.stream().filter(f -> f.severity() == NcFindingSeverity.INFO).toList();
    }

    public String getProgramName()             { return programName; }
    public String getProfileName()             { return profileName; }
    public int getTotalBlocks()                { return totalBlocks; }
    public List<NcVerificationFinding> getFindings() { return findings; }
    public NcInterpreterState getFinalState()  { return finalState; }

    public String formatSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("NC Verification Report: %s (Profile: %s)%n", programName, profileName));
        sb.append(String.format("Result: %s (Errors: %d, Warnings: %d, Infos: %d)%n",
            isApproved() ? "APPROVED" : "REJECTED (BLOCKING ERRORS)",
            getErrors().size(), getWarnings().size(), getInfos().size()));
        sb.append(String.format("Total Blocks: %d, Tool Changes: %d, Motion Blocks: %d%n",
            totalBlocks,
            finalState != null ? finalState.getToolChangeCount() : 0,
            finalState != null ? finalState.getTotalMotionBlocks() : 0));
        if (finalState != null && Double.isFinite(finalState.getMinZ())) {
            sb.append(String.format("Z Travel Bounds: [%.3f, %.3f]%n", finalState.getMinZ(), finalState.getMaxZ()));
        }
        for (NcVerificationFinding f : findings) {
            sb.append("  ").append(f.toString()).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
