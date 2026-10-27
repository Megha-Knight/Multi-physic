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

        double holeX = p.hasWord('X') ? p.getWord('X') : state.getCurrentX();
        double holeY = p.hasWord('Y') ? p.getWord('Y') : state.getCurrentY();
        if (setup != null && setup.getStock() != null) {
            if (Double.isFinite(holeX) && (holeX < setup.getStock().getMin().x() - 1e-4 || holeX > setup.getStock().getMax().x() + 1e-4)) {
                findings.add(new NcVerificationFinding(NcFindingCode.STOCK_BOUNDARY_VIOLATION,
                    NcFindingSeverity.ERROR, idx, line, String.format("Canned cycle hole X%.3f exceeds stock boundary.", holeX)));
            }
            if (Double.isFinite(holeY) && (holeY < setup.getStock().getMin().y() - 1e-4 || holeY > setup.getStock().getMax().y() + 1e-4)) {
                findings.add(new NcVerificationFinding(NcFindingCode.STOCK_BOUNDARY_VIOLATION,
                    NcFindingSeverity.ERROR, idx, line, String.format("Canned cycle hole Y%.3f exceeds stock boundary.", holeY)));
            }
        }
        state.recordMotion(true);
        state.updatePosition(holeX, holeY, targetZ);
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

        boolean isIncremental = "G91".equals(state.getActiveDistanceMode());
        double newX = p.hasWord('X')
            ? (isIncremental && Double.isFinite(state.getCurrentX()) ? state.getCurrentX() + p.getWord('X') : p.getWord('X'))
            : state.getCurrentX();
        double newY = p.hasWord('Y')
            ? (isIncremental && Double.isFinite(state.getCurrentY()) ? state.getCurrentY() + p.getWord('Y') : p.getWord('Y'))
            : state.getCurrentY();
        double newZ = p.hasWord('Z')
            ? (isIncremental && Double.isFinite(state.getCurrentZ()) ? state.getCurrentZ() + p.getWord('Z') : p.getWord('Z'))
            : state.getCurrentZ();

        boolean isCutting = "G01".equals(state.getActiveMotionGroup());
        if (isCutting) {
            verifyCuttingPreconditions(idx, line, state, profile, findings);
            if (isIncremental && profile.getCoordinateMode() == ControllerProfile.CoordinateMode.ABSOLUTE_G90) {
                findings.add(new NcVerificationFinding(NcFindingCode.UNEXPECTED_INCREMENTAL_MODE,
                    NcFindingSeverity.ERROR, idx, line, "Cutting motion executed in incremental distance mode (G91) when profile expects absolute (G90)."));
            }
            if (setup != null && setup.getStock() != null) {
                if (Double.isFinite(newX) && (newX < setup.getStock().getMin().x() - 1e-4 || newX > setup.getStock().getMax().x() + 1e-4)) {
                    findings.add(new NcVerificationFinding(NcFindingCode.STOCK_BOUNDARY_VIOLATION,
                        NcFindingSeverity.ERROR, idx, line, String.format("Cutting coordinate X%.3f exceeds stock boundary.", newX)));
                }
                if (Double.isFinite(newY) && (newY < setup.getStock().getMin().y() - 1e-4 || newY > setup.getStock().getMax().y() + 1e-4)) {
                    findings.add(new NcVerificationFinding(NcFindingCode.STOCK_BOUNDARY_VIOLATION,
                        NcFindingSeverity.ERROR, idx, line, String.format("Cutting coordinate Y%.3f exceeds stock boundary.", newY)));
                }
                if (Double.isFinite(newZ) && newZ < setup.getStock().getBottomZ() - 1e-4) {
                    findings.add(new NcVerificationFinding(NcFindingCode.STOCK_DEPTH_VIOLATION,
                        NcFindingSeverity.ERROR, idx, line, String.format("Cutting depth Z%.3f penetrates below stock bottom Z%.3f.", newZ, setup.getStock().getBottomZ())));
                }
            }
        } else if ("G00".equals(state.getActiveMotionGroup()) && setup != null && setup.getStock() != null
                && Double.isFinite(newZ) && !p.hasG("G53") && !p.hasG("G28")) {
            if (newZ < setup.getStock().getBottomZ() - 1e-4) {
                findings.add(new NcVerificationFinding(NcFindingCode.SAFETY_CLEARANCE_VIOLATION,
                    NcFindingSeverity.ERROR, idx, line, String.format("Rapid traverse Z%.3f penetrates below stock bottom Z%.3f.",
                    newZ, setup.getStock().getBottomZ())));
            }
        }

        if (state.getActiveMotionGroup() != null) {
            state.recordMotion(isCutting);
        }
        state.updatePosition(newX, newY, newZ);
    }
}
