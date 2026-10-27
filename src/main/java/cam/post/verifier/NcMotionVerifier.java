package cam.post.verifier;

import cam.model.CamPoint3D;
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
            findings.add(new NcVerificationFinding(NcFindingCode.INIT_MISSING, NcFindingSeverity.ERROR, idx, line, "Cutting motion before safe startup initialization."));
        }
        if (state.getActiveWcs() == null) {
            findings.add(new NcVerificationFinding(NcFindingCode.WORK_OFFSET_MISSING, NcFindingSeverity.ERROR, idx, line, "Cutting motion without active WCS (G54-G59)."));
        }
        if (!state.isSpindleRunning() || state.getSpindleRpm() <= 0.0) {
            findings.add(new NcVerificationFinding(NcFindingCode.SPINDLE_NOT_STARTED, NcFindingSeverity.ERROR, idx, line, "Cutting motion while spindle is stopped."));
        }
        if (state.getActiveFeedRate() <= 0.0) {
            findings.add(new NcVerificationFinding(NcFindingCode.FEED_RATE_MISSING, NcFindingSeverity.ERROR, idx, line, "Cutting motion without positive feed rate."));
        }
        if (profile.isEmitToolLengthComp() && !state.isToolLengthCompActive()) {
            findings.add(new NcVerificationFinding(NcFindingCode.TOOL_LENGTH_COMP_MISSING, NcFindingSeverity.ERROR, idx, line, "Cutting motion without active tool length comp (G43)."));
        }
    }

    public static void verifyCannedCycle(NcBlockParser p, int idx, String line, NcInterpreterState state,
                                         ControllerProfile profile, ManufacturingSetup setup,
                                         List<NcVerificationFinding> findings) {
        verifyCuttingPreconditions(idx, line, state, profile, findings);
        if (state.isReferenceReturnPendingRestore() && "G91".equals(state.getActiveDistanceMode())) {
            findings.add(new NcVerificationFinding(NcFindingCode.RETRACT_MODE_NOT_RESTORED, NcFindingSeverity.ERROR, idx, line, "Canned cycle commanded after G91 G28 without restoring G90."));
        }
        boolean isInitial = p.hasG("G81") || p.hasG("G82") || p.hasG("G83") || p.hasG("G73");
        if (isInitial && (!p.hasWord('Z') || !p.hasWord('R'))) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_INVALID_PARAMS, NcFindingSeverity.ERROR, idx, line, "Initial canned cycle missing Z depth or R plane."));
        }
        if (isInitial && Double.isFinite(state.getCurrentZ())) state.setModalCycleInitialZ(state.getCurrentZ());
        if (p.hasWord('Z')) state.setModalCycleZ(p.getWord('Z'));
        if (p.hasWord('R')) state.setModalCycleR(p.getWord('R'));
        if (p.hasG("G98")) state.setActiveReturnLevel("G98");
        if (p.hasG("G99")) state.setActiveReturnLevel("G99");

        double targetZ = state.getModalCycleZ();
        double rZ = state.getModalCycleR();
        if (Double.isFinite(targetZ) && Double.isFinite(rZ) && targetZ >= rZ) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_INVERTED_DEPTH, NcFindingSeverity.ERROR, idx, line, String.format("Canned cycle bottom Z (%.3f) >= R-plane (%.3f).", targetZ, rZ)));
        }
        if (Double.isFinite(rZ) && setup != null && setup.getStock() != null && rZ < setup.getStock().getTopZ() - 1e-4) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_SUB_SURFACE_R, NcFindingSeverity.ERROR, idx, line, String.format("Canned cycle R-plane (%.3f) below stock top (%.3f).", rZ, setup.getStock().getTopZ())));
        }
        if ((p.hasG("G83") || p.hasG("G73")) && (!p.hasWord('Q') || p.getWord('Q') <= 0.0)) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_INVALID_PARAMS, NcFindingSeverity.ERROR, idx, line, "Peck drilling cycle missing positive Q increment."));
            findings.add(new NcVerificationFinding(NcFindingCode.PECK_INCREMENT_INVALID, NcFindingSeverity.ERROR, idx, line, "Peck drilling cycle missing positive Q increment."));
        }
        if ("G91".equals(state.getActiveDistanceMode())) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_INCREMENTAL_UNSUPPORTED, NcFindingSeverity.ERROR, idx, line, "Canned cycle commanded under G91 incremental mode."));
        }
        if (p.hasG("G82") && p.hasWord('P') && p.getWord('P') < 0.0) {
            findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_INVALID_PARAMS, NcFindingSeverity.ERROR, idx, line, "G82 dwell P cannot be negative."));
        }

        double holeX = p.hasWord('X') ? p.getWord('X') : state.getCurrentX();
        double holeY = p.hasWord('Y') ? p.getWord('Y') : state.getCurrentY();
        checkStockViolations(idx, line, holeX, holeY, targetZ, setup, findings, true);

        double startZ = Double.isFinite(rZ) ? rZ : state.getCurrentZ();
        double initZ = Double.isFinite(state.getModalCycleInitialZ()) ? state.getModalCycleInitialZ() : state.getCurrentZ();
        double retZ = "G98".equals(state.getActiveReturnLevel()) ? (Double.isFinite(initZ) ? initZ : startZ) : startZ;

        if (setup != null && setup.hasFixtureGeometry() && setup.getFixtureBounds().isValid()
                && Double.isFinite(holeX) && Double.isFinite(holeY) && Double.isFinite(targetZ)) {
            double curX = Double.isFinite(state.getCurrentX()) ? state.getCurrentX() : holeX;
            double curY = Double.isFinite(state.getCurrentY()) ? state.getCurrentY() : holeY;
            double curZ = Double.isFinite(state.getCurrentZ()) ? state.getCurrentZ() : startZ;
            double toolRad = setup.resolveToolRadius(state.getActiveTool(), 0.0);
            double toolLen = setup.resolveToolLength(state.getActiveTool(), 50.0);
            var fb = setup.getFixtureBounds();
            if (fb.collidesWithSweptCylinder(new CamPoint3D(curX, curY, curZ), new CamPoint3D(holeX, holeY, curZ), toolRad, toolLen)
                    || fb.collidesWithSweptCylinder(new CamPoint3D(holeX, holeY, curZ), new CamPoint3D(holeX, holeY, startZ), toolRad, toolLen)
                    || fb.collidesWithSweptCylinder(new CamPoint3D(holeX, holeY, startZ), new CamPoint3D(holeX, holeY, targetZ), toolRad, toolLen)
                    || fb.collidesWithSweptCylinder(new CamPoint3D(holeX, holeY, targetZ), new CamPoint3D(holeX, holeY, retZ), toolRad, toolLen)) {
                findings.add(new NcVerificationFinding(NcFindingCode.FIXTURE_COLLISION_DETECTED, NcFindingSeverity.ERROR, idx, line, "Canned cycle penetrates modeled fixture envelope."));
            }
        }
        state.recordMotion(true);
        if (Double.isFinite(targetZ)) state.updatePosition(holeX, holeY, targetZ);
        state.updatePosition(holeX, holeY, retZ);
    }

    public static void verifyLinearMotion(NcBlockParser p, int idx, String line, NcInterpreterState state,
                                          ControllerProfile profile, ManufacturingSetup setup,
                                          List<NcVerificationFinding> findings) {
        if ((p.hasWord('X') && !Double.isFinite(p.getWord('X'))) || (p.hasWord('Y') && !Double.isFinite(p.getWord('Y'))) || (p.hasWord('Z') && !Double.isFinite(p.getWord('Z')))) {
            findings.add(new NcVerificationFinding(NcFindingCode.NON_FINITE_COORDINATE, NcFindingSeverity.ERROR, idx, line, "Non-finite coordinate (NaN/Infinity) in motion block."));
            return;
        }
        if (state.isReferenceReturnPendingRestore() && "G91".equals(state.getActiveDistanceMode())) {
            findings.add(new NcVerificationFinding(NcFindingCode.RETRACT_MODE_NOT_RESTORED, NcFindingSeverity.ERROR, idx, line, "Motion after G91 G28 without restoring absolute mode (G90)."));
        }
        boolean isInc = "G91".equals(state.getActiveDistanceMode());
        double newX = p.hasWord('X') ? (isInc && Double.isFinite(state.getCurrentX()) ? state.getCurrentX() + p.getWord('X') : p.getWord('X')) : state.getCurrentX();
        double newY = p.hasWord('Y') ? (isInc && Double.isFinite(state.getCurrentY()) ? state.getCurrentY() + p.getWord('Y') : p.getWord('Y')) : state.getCurrentY();
        double newZ = p.hasWord('Z') ? (isInc && Double.isFinite(state.getCurrentZ()) ? state.getCurrentZ() + p.getWord('Z') : p.getWord('Z')) : state.getCurrentZ();

        boolean isCutting = "G01".equals(state.getActiveMotionGroup());
        if (isCutting) {
            verifyCuttingPreconditions(idx, line, state, profile, findings);
            if (isInc && profile.getCoordinateMode() == ControllerProfile.CoordinateMode.ABSOLUTE_G90) {
                findings.add(new NcVerificationFinding(NcFindingCode.UNEXPECTED_INCREMENTAL_MODE, NcFindingSeverity.ERROR, idx, line, "Cutting in G91 incremental mode when profile expects G90."));
            }
            checkStockViolations(idx, line, newX, newY, newZ, setup, findings, false);
        } else if ("G00".equals(state.getActiveMotionGroup()) && setup != null && setup.getStock() != null
                && Double.isFinite(newZ) && !p.hasG("G53") && !p.hasG("G28")) {
            if (newZ < setup.getStock().getBottomZ() - 1e-4) {
                findings.add(new NcVerificationFinding(NcFindingCode.SAFETY_CLEARANCE_VIOLATION, NcFindingSeverity.ERROR, idx, line, String.format("Rapid traverse Z%.3f penetrates below stock bottom Z%.3f.", newZ, setup.getStock().getBottomZ())));
            }
        }
        if (setup != null && setup.hasFixtureGeometry() && setup.getFixtureBounds().isValid()
                && Double.isFinite(state.getCurrentX()) && Double.isFinite(newX)) {
            CamPoint3D p1 = new CamPoint3D(state.getCurrentX(), state.getCurrentY(), state.getCurrentZ());
            CamPoint3D p2 = new CamPoint3D(newX, newY, newZ);
            double toolRad = setup.resolveToolRadius(state.getActiveTool(), 0.0);
            double toolLen = setup.resolveToolLength(state.getActiveTool(), 50.0);
            if (setup.getFixtureBounds().collidesWithSweptCylinder(p1, p2, toolRad, toolLen)) {
                findings.add(new NcVerificationFinding(NcFindingCode.FIXTURE_COLLISION_DETECTED, NcFindingSeverity.ERROR, idx, line, "Motion segment collides with modeled fixture envelope."));
            }
        }
        if (state.getActiveMotionGroup() != null) state.recordMotion(isCutting);
        state.updatePosition(newX, newY, newZ);
    }

    private static void checkStockViolations(int idx, String line, double x, double y, double z,
                                             ManufacturingSetup setup, List<NcVerificationFinding> findings, boolean isCycle) {
        if (setup == null || setup.getStock() == null) return;
        var st = setup.getStock();
        String prefix = isCycle ? "Canned cycle" : "Cutting";
        if (Double.isFinite(x) && (x < st.getMin().x() - 1e-4 || x > st.getMax().x() + 1e-4)) {
            findings.add(new NcVerificationFinding(NcFindingCode.STOCK_BOUNDARY_VIOLATION, NcFindingSeverity.ERROR, idx, line, String.format("%s coordinate X%.3f exceeds stock boundary.", prefix, x)));
        }
        if (Double.isFinite(y) && (y < st.getMin().y() - 1e-4 || y > st.getMax().y() + 1e-4)) {
            findings.add(new NcVerificationFinding(NcFindingCode.STOCK_BOUNDARY_VIOLATION, NcFindingSeverity.ERROR, idx, line, String.format("%s coordinate Y%.3f exceeds stock boundary.", prefix, y)));
        }
        if (Double.isFinite(z) && z < st.getBottomZ() - 1e-4) {
            findings.add(new NcVerificationFinding(NcFindingCode.STOCK_DEPTH_VIOLATION, NcFindingSeverity.ERROR, idx, line, String.format("%s depth Z%.3f penetrates below stock bottom Z%.3f.", prefix, z, st.getBottomZ())));
        }
    }
}
