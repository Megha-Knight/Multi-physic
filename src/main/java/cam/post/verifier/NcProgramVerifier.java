package cam.post.verifier;

import cam.model.ManufacturingSetup;
import cam.post.ControllerProfile;
import cam.post.NcProgram;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless, deterministic NC preflight verifier.
 * Independently verifies G-code block streams against machine and safety rules.
 */
public class NcProgramVerifier {

    private final ControllerProfile profile;
    private final ManufacturingSetup setup;

    public NcProgramVerifier(ControllerProfile profile, ManufacturingSetup setup) {
        this.profile = (profile != null) ? profile : ControllerProfile.createIso6983Standard();
        this.setup = setup;
    }

    public NcProgramVerifier(ControllerProfile profile) { this(profile, null); }
    public NcProgramVerifier()                          { this(ControllerProfile.createIso6983Standard(), null); }

    public NcVerificationReport verify(NcProgram program) {
        if (program == null) throw new IllegalArgumentException("NcProgram cannot be null.");
        return verify(program.getBlocks(), program.getProgramName());
    }

    public NcVerificationReport verify(List<String> blocks, String programName) {
        if (blocks == null) throw new IllegalArgumentException("Block list cannot be null.");
        List<NcVerificationFinding> findings = new ArrayList<>();
        NcInterpreterState state = new NcInterpreterState();

        findings.add(new NcVerificationFinding(NcFindingCode.UNSUPPORTED_COLLISION_CHECK, NcFindingSeverity.INFO, 0, "", "Preflight checks audit endpoints and stock boundaries. Swept 3D cutter volume, holder, and fixture checks require complete 3D solid models."));
        findings.add(new NcVerificationFinding(NcFindingCode.TOOL_HOLDER_CLEARANCE_UNVERIFIED, NcFindingSeverity.INFO, 0, "", "Tool holder geometry unmodeled; holder clearance unverified."));
        findings.add(new NcVerificationFinding(NcFindingCode.MACHINE_ENVELOPE_UNVERIFIED, NcFindingSeverity.INFO, 0, "", "Machine axis travel limits unmodeled; travel limits unverified."));

        if (setup == null || !setup.hasFixtureGeometry()) {
            findings.add(new NcVerificationFinding(NcFindingCode.FIXTURE_CLEARANCE_UNVERIFIED, NcFindingSeverity.WARNING, 0, "", "Fixture and clamp geometry is not configured in setup."));
        } else if (!setup.getFixtureBounds().isValid()) {
            findings.add(new NcVerificationFinding(NcFindingCode.INVALID_FIXTURE_BOUNDS, NcFindingSeverity.ERROR, 0, "", "Configured fixture bounding volume is inverted, degenerate, or invalid."));
        }

        for (int i = 0; i < blocks.size(); i++) verifyBlock(blocks.get(i), i, state, findings);

        if (!state.isProgramTerminated()) findings.add(new NcVerificationFinding(NcFindingCode.PROGRAM_END_MISSING, NcFindingSeverity.ERROR, blocks.size(), "", "Program ends without M30/M02 termination block."));
        if (state.isCannedCycleActive()) findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_NOT_CANCELLED, NcFindingSeverity.ERROR, blocks.size(), "", "Program finished with active canned cycle without G80."));

        return new NcVerificationReport(programName, profile.getName(), blocks.size(), findings, state);
    }

    private void verifyBlock(String line, int idx, NcInterpreterState state, List<NcVerificationFinding> findings) {
        NcBlockParser p = new NcBlockParser(line);
        if (p.isCommentOnly() || p.isProgramNumber() || p.isTapeHeaderOrRewind()) return;

        if (p.isMalformed()) findings.add(new NcVerificationFinding(NcFindingCode.MALFORMED_BLOCK_SYNTAX, NcFindingSeverity.ERROR, idx, line, "Malformed block token."));
        for (char c : p.getDuplicateWords()) findings.add(new NcVerificationFinding(NcFindingCode.DUPLICATE_ADDRESS_WORD, NcFindingSeverity.ERROR, idx, line, "Duplicate address word '" + c + "'."));
        if (p.hasConflictingModalCommands()) findings.add(new NcVerificationFinding(NcFindingCode.CONFLICTING_MODAL_COMMANDS, NcFindingSeverity.ERROR, idx, line, "Conflicting modal commands."));
        for (String cmd : p.getUnsupportedCommands()) findings.add(new NcVerificationFinding(NcFindingCode.UNSUPPORTED_NC_COMMAND, NcFindingSeverity.ERROR, idx, line, "Unsupported NC command '" + cmd + "'."));

        if (state.isProgramTerminated()) {
            findings.add(new NcVerificationFinding(NcFindingCode.MOTION_AFTER_PROGRAM_END, NcFindingSeverity.ERROR, idx, line, "Command after M30 termination."));
            return;
        }

        verifySetupAndTooling(p, idx, line, state, findings);
        verifySpindleAndFeed(p, idx, line, state, findings);
        verifyCyclesAndRetracts(p, idx, line, state, findings);

        if (p.hasG("G00") || p.hasG("G0")) state.setActiveMotionGroup("G00");
        if (p.hasG("G01") || p.hasG("G1")) state.setActiveMotionGroup("G01");

        boolean hasCoord = p.hasWord('X') || p.hasWord('Y') || p.hasWord('Z');
        if (hasCoord && !state.isCannedCycleActive() && !p.hasG("G53") && !p.hasG("G28")) {
            NcMotionVerifier.verifyLinearMotion(p, idx, line, state, profile, setup, findings);
        }

        if (p.hasM("M30") || p.hasM("M02")) {
            if (profile.isEmitToolLengthCancel() && state.isToolLengthCompActive()) {
                findings.add(new NcVerificationFinding(NcFindingCode.TOOL_LENGTH_NOT_CANCELLED, NcFindingSeverity.ERROR, idx, line, "Missing G49 tool length cancel before M30."));
            }
            if (state.isCannedCycleActive()) {
                findings.add(new NcVerificationFinding(NcFindingCode.CANNED_CYCLE_NOT_CANCELLED, NcFindingSeverity.ERROR, idx, line, "M30 executed with active canned cycle."));
            }
            state.setProgramTerminated(true);
        }
    }

    private void verifySetupAndTooling(NcBlockParser p, int idx, String line, NcInterpreterState state, List<NcVerificationFinding> findings) {
        if (p.hasG("G90")) { state.setActiveDistanceMode("G90"); state.setReferenceReturnPendingRestore(false); }
        if (p.hasG("G91")) state.setActiveDistanceMode("G91");
        if (p.hasG("G17")) state.setActivePlane("G17");
        if (p.hasG("G21")) {
            if (profile.getUnitMode() == ControllerProfile.UnitMode.INCH_G20) {
                findings.add(new NcVerificationFinding(NcFindingCode.UNIT_MODE_MISMATCH, NcFindingSeverity.ERROR, idx, line, "G21 metric programmed but profile is inch G20."));
            }
            state.setInitialized(true);
        }
        if (p.hasG("G20")) {
            if (profile.getUnitMode() == ControllerProfile.UnitMode.METRIC_G21) {
                findings.add(new NcVerificationFinding(NcFindingCode.UNIT_MODE_MISMATCH, NcFindingSeverity.ERROR, idx, line, "G20 inch programmed but profile is metric G21."));
            }
            state.setInitialized(true);
        }
        for (String g : p.getGCodes()) {
            if (g.matches("G5[4-9]")) {
                state.setActiveWcs(g);
                if (setup != null) {
                    var cs = setup.getCoordinateSystem(g);
                    if (cs == null || !cs.isValid()) {
                        findings.add(new NcVerificationFinding(NcFindingCode.COORDINATE_OFFSET_INVALID,
                            NcFindingSeverity.ERROR, idx, line, "WCS " + g + " is unconfigured or invalid in setup."));
                    }
                }
            }
        }

        if (p.hasWord('T')) {
            if (state.isCannedCycleActive()) findings.add(new NcVerificationFinding(NcFindingCode.TOOL_CHANGE_DURING_CYCLE,
                NcFindingSeverity.ERROR, idx, line, "Tool change attempted during active canned cycle."));
            if (p.hasM("M06")) {
                if (profile.isEmitToolLengthCancel() && state.isToolLengthCompActive() && !p.hasG("G49")) {
                    findings.add(new NcVerificationFinding(NcFindingCode.TOOL_LENGTH_NOT_CANCELLED,
                        NcFindingSeverity.ERROR, idx, line, "Tool change attempted without canceling previous compensation."));
                }
                state.setActiveTool(p.getWord('T').intValue());
            }
        }
        if (p.hasG("G43")) {
            if (p.hasWord('Z') && setup != null && setup.getStock() != null && p.getWord('Z') < setup.getStock().getTopZ() - 1e-4) {
                findings.add(new NcVerificationFinding(NcFindingCode.UNSAFE_COMPENSATION_POSITION,
                    NcFindingSeverity.ERROR, idx, line, String.format("G43 activated at unsafe height Z%.3f below stock top Z%.3f.", p.getWord('Z'), setup.getStock().getTopZ())));
            }
            if (p.hasWord('H')) {
                int h = p.getWord('H').intValue();
                state.setActiveHRegister(h);
                state.setToolLengthCompActive(true);
                int expH = profile.resolveHRegister(state.getActiveTool() != null ? state.getActiveTool() : 1);
                if (h != expH) findings.add(new NcVerificationFinding(NcFindingCode.TOOL_OFFSET_MISMATCH,
                    NcFindingSeverity.ERROR, idx, line, String.format("Tool length offset H%d does not match expected H%d for active tool T%d.", h, expH, state.getActiveTool())));
            } else {
                findings.add(new NcVerificationFinding(NcFindingCode.TOOL_OFFSET_MISMATCH, NcFindingSeverity.ERROR, idx, line, "G43 emitted without H offset register."));
            }
        }
        if (p.hasG("G49")) state.setToolLengthCompActive(false);
    }

    private void verifySpindleAndFeed(NcBlockParser p, int idx, String line, NcInterpreterState state, List<NcVerificationFinding> findings) {
        if (p.hasM("M03") || p.hasM("M04")) {
            state.setSpindleRunning(true);
            if (p.hasWord('S')) {
                state.setSpindleRpm(p.getWord('S'));
                if (p.getWord('S') <= 0.0) findings.add(new NcVerificationFinding(NcFindingCode.SPINDLE_RPM_NON_POSITIVE, NcFindingSeverity.ERROR, idx, line, "Spindle speed S non-positive."));
            }
        }
        if (p.hasM("M05")) state.setSpindleRunning(false);
        if (p.hasWord('F')) {
            state.setActiveFeedRate(p.getWord('F'));
            if (p.getWord('F') <= 0.0) findings.add(new NcVerificationFinding(NcFindingCode.FEED_RATE_NON_POSITIVE, NcFindingSeverity.ERROR, idx, line, "Feed rate F non-positive."));
        }
    }

    private void verifyCyclesAndRetracts(NcBlockParser p, int idx, String line, NcInterpreterState state, List<NcVerificationFinding> findings) {
        if (p.hasG("G53")) {
            if (state.isCannedCycleActive()) findings.add(new NcVerificationFinding(NcFindingCode.RETRACT_DURING_CYCLE, NcFindingSeverity.ERROR, idx, line, "G53 retract attempted during canned cycle."));
            if (p.hasWord('Z') && profile.getMachineRetractZ() != null && Math.abs(p.getWord('Z') - profile.getMachineRetractZ()) > 1e-4) {
                findings.add(new NcVerificationFinding(NcFindingCode.RETRACT_TARGET_INVALID, NcFindingSeverity.ERROR, idx, line, String.format("G53 target Z%.3f does not match configured machine Z%.3f.", p.getWord('Z'), profile.getMachineRetractZ())));
            }
        }
        if (p.hasG("G28")) {
            if (state.isCannedCycleActive()) findings.add(new NcVerificationFinding(NcFindingCode.RETRACT_DURING_CYCLE, NcFindingSeverity.ERROR, idx, line, "G28 return attempted during canned cycle."));
            if (p.hasG("G91") || "G91".equals(state.getActiveDistanceMode())) {
                state.setReferenceReturnPendingRestore(true);
            } else if ("G90".equals(state.getActiveDistanceMode())) {
                findings.add(new NcVerificationFinding(NcFindingCode.UNSAFE_REFERENCE_RETURN,
                    NcFindingSeverity.WARNING, idx, line, "G28 in absolute mode (G90) risks intermediate collision; use G91 G28 Z0."));
            }
        }

        if (p.hasG("G80") || p.hasG("G00") || p.hasG("G0") || p.hasG("G01") || p.hasG("G1")) {
            state.setCannedCycleActive(false);
            state.clearModalCycleParams();
        }
        if (p.hasG("G81") || p.hasG("G82") || p.hasG("G83") || p.hasG("G73")) {
            state.setCannedCycleActive(true);
            NcMotionVerifier.verifyCannedCycle(p, idx, line, state, profile, setup, findings);
        } else if (state.isCannedCycleActive() && (p.hasWord('X') || p.hasWord('Y') || p.hasWord('Z'))) {
            NcMotionVerifier.verifyCannedCycle(p, idx, line, state, profile, setup, findings);
        }
    }
}
