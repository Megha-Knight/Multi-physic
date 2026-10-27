package cam.post.verifier;

import cam.model.ManufacturingSetup;
import cam.post.ControllerProfile;

import java.util.List;

/**
 * Validates motion blocks, cutting preconditions, canned cycles, and stock depth bounds.
 */
public final class NcMotionVerifier {

    private NcMotionVerifier() {}

    public static void verifyCuttingPreconditions(int idx, String line, NcInterpreterState state,
                                                  ControllerProfile profile, List<NcVerificationFinding> findings) {
        if (!state.isInitialized()) {
            findings.add(new NcVerificationFinding(NcFindingCode.INIT_MISSING,
                NcFindingSeverity.ERROR, idx, line, "Cutting motion executed before safe startup initialization."));
        }
        if (state.getActiveWcs() == null) {
            findings.add(new NcVerificationFinding(NcFindingCode.WORK_OFFSET_MISSING,
                NcFindingSeverity.ERROR, idx, line, "Cutting motion executed without active work coordinate system (G54-G59)."));
        }
        if (!state.isSpindleRunning() || state.getSpindleRpm() <= 0.0) {
            findings.add(new NcVerificationFinding(NcFindingCode.SPINDLE_NOT_STARTED,
                NcFindingSeverity.ERROR, idx, line, "Cutting motion executed while spindle is stopped."));
        }
        if (state.getActiveFeedRate() <= 0.0) {
            findings.add(new NcVerificationFinding(NcFindingCode.FEED_RATE_MISSING,
                NcFindingSeverity.ERROR, idx, line, "Cutting motion executed without programmed positive feed rate."));
        }
        if (profile.isEmitToolLengthComp() && !state.isToolLengthCompActive()) {
            findings.add(new NcVerificationFinding(NcFindingCode.TOOL_LENGTH_COMP_MISSING,
                NcFindingSeverity.ERROR, idx, line, "Cutting motion executed without active tool length compensation (G43)."));
        }
    }

    public static void verifyCannedCycle(NcBlockParser p, int idx, String line, NcInterpreterState state,
                                         ControllerProfile profile, ManufacturingSetup setup,
                                         List<NcVerificationFinding> findings) {
        verifyCuttingPreconditions(idx, line, state, profile, findings);
        double targetZ = p.hasWord('Z') ? p.getWord('Z') : state.getCurrentZ();
        double rZ = p.hasWord('R') ? p.getWord('R') : Double.NaN;

        if (Double.isFinite(targetZ) && Double.isFinite(rZ) && targetZ >= rZ) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_INVERTED_DEPTH,
                NcFindingSeverity.ERROR, idx, line, String.format("Canned cycle bottom Z (%.3f) >= R-plane (%.3f).", targetZ, rZ)));
        }
        if (Double.isFinite(rZ) && setup != null && setup.getStock() != null && rZ < setup.getStock().getTopZ() - 1e-4) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_SUB_SURFACE_R,
                NcFindingSeverity.ERROR, idx, line, String.format("Canned cycle R-plane (%.3f) below stock top surface (%.3f).", rZ, setup.getStock().getTopZ())));
        }
        if ((p.hasG("G83") || p.hasG("G73")) && (!p.hasWord('Q') || p.getWord('Q') <= 0.0)) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_INVALID_PARAMS,
                NcFindingSeverity.ERROR, idx, line, "Peck drilling cycle missing positive Q increment."));
        }
        state.recordMotion(true);
    }

    public static void verifyLinearMotion(NcBlockParser p, int idx, String line, NcInterpreterState state,
                                          ControllerProfile profile, ManufacturingSetup setup,
                                          List<NcVerificationFinding> findings) {
        if ((p.hasWord('X') && !Double.isFinite(p.getWord('X')))
                || (p.hasWord('Y') && !Double.isFinite(p.getWord('Y')))
                || (p.hasWord('Z') && !Double.isFinite(p.getWord('Z')))) {
            findings.add(new NcVerificationFinding(NcFindingCode.NON_FINITE_COORDINATE,
                NcFindingSeverity.ERROR, idx, line, "Non-finite coordinate (NaN/Infinity) encountered in motion block."));
            return;
        }

        double newX = p.hasWord('X') ? p.getWord('X') : state.getCurrentX();
        double newY = p.hasWord('Y') ? p.getWord('Y') : state.getCurrentY();
        double newZ = p.hasWord('Z') ? p.getWord('Z') : state.getCurrentZ();

        boolean isCutting = "G01".equals(state.getActiveMotionGroup());
        if (isCutting) {
            verifyCuttingPreconditions(idx, line, state, profile, findings);
            if (setup != null && setup.getStock() != null && Double.isFinite(newZ)
                    && newZ < setup.getStock().getBottomZ() - 1e-4) {
                findings.add(new NcVerificationFinding(NcFindingCode.STOCK_DEPTH_VIOLATION,
                    NcFindingSeverity.ERROR, idx, line, String.format("Cutting depth Z%.3f penetrates below stock bottom Z%.3f.", newZ, setup.getStock().getBottomZ())));
            }
        }
        if (state.getActiveMotionGroup() != null) {
            state.recordMotion(isCutting);
        }
        state.updatePosition(newX, newY, newZ);
    }
}
