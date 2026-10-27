package cam.post;

import cam.model.CamPoint3D;
import cam.model.ManufacturingSetup;
import cam.tool.CamTool;
import cam.toolpath.Toolpath;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic ISO 6983 / Fanuc-compatible NC postprocessor with modal state tracking,
 * tool-length compensation (G43/G49), coordinate suppression, and canned cycle emission.
 */
public class Iso6983Postprocessor implements NcPostprocessor {

    private final ControllerProfile profile;

    public Iso6983Postprocessor(ControllerProfile profile) {
        this.profile = (profile != null) ? profile : ControllerProfile.createIso6983Standard();
    }

    public Iso6983Postprocessor() { this(ControllerProfile.createIso6983Standard()); }

    @Override public ControllerProfile getProfile() { return profile; }

    @Override
    public NcProgram postprocess(ManufacturingSetup setup, Toolpath toolpath, CamTool tool, int toolNumber) {
        List<NcCommand> commands = NcCommandBuilder.buildFromToolpath(setup, toolpath, tool, toolNumber, profile);
        return formatProgram(commands, (setup != null) ? setup.getName() : "TOOLPATH");
    }

    @Override
    public NcProgram postprocess(ManufacturingSetup setup) {
        if (setup == null) throw new IllegalArgumentException("ManufacturingSetup cannot be null.");
        List<NcCommand> commands = NcCommandBuilder.buildFromSetup(setup, profile);
        return formatProgram(commands, setup.getName());
    }

    private NcProgram formatProgram(List<NcCommand> commands, String progName) {
        List<String> blocks = new ArrayList<>();
        ModalState modal = new ModalState();
        int lineNum = profile.getLineNumberIncrement();
        int toolChanges = 0;

        for (NcCommand cmd : commands) {
            String block = formatCommand(cmd, modal);
            if (block != null && !block.isBlank()) {
                if (cmd.type() == NcCommandType.PROGRAM_HEADER || cmd.type() == NcCommandType.COMMENT) {
                    blocks.add(block);
                } else if (cmd.type() == NcCommandType.PROGRAM_END) {
                    blocks.add(profile.isLineNumbersEnabled() ? "N" + lineNum + " " + profile.getProgramEndCommand() : profile.getProgramEndCommand());
                    blocks.add("%");
                } else {
                    blocks.add(profile.isLineNumbersEnabled() ? "N" + lineNum + " " + block : block);
                    lineNum += profile.getLineNumberIncrement();
                }
            }
            if (cmd.type() == NcCommandType.TOOL_CHANGE) toolChanges++;
        }
        return new NcProgram(progName, blocks, profile.getName(), toolChanges);
    }

    private String formatCommand(NcCommand cmd, ModalState modal) {
        return switch (cmd.type()) {
            case COMMENT -> "(" + cmd.text() + ")";
            case PROGRAM_HEADER -> "%" + System.lineSeparator() + "O" + NcFormatUtils.sanitizeProgName(cmd.text());
            case SAFE_START -> { modal.setSetupExecuted(true); yield "G90 G21 G17"; }
            case WORK_COORDINATE_SYSTEM -> { modal.setActiveWcs(cmd.text()); yield cmd.text(); }
            case TOOL_CHANGE -> {
                if (modal.isCannedCycleActive()) {
                    throw new IllegalStateException("Tool change attempted while canned cycle is active without G80 cancellation.");
                }
                modal.setActiveToolNumber(cmd.toolNumber());
                modal.setActiveMotionGroup(null);
                yield "T" + cmd.toolNumber() + " M06 (" + cmd.text() + ")";
            }
            case TOOL_LENGTH_COMP -> {
                modal.setToolLengthCompActive(true);
                modal.setActiveHRegister(cmd.toolNumber());
                yield "G43 H" + cmd.toolNumber() + " Z" + profile.formatNumber(cmd.rPlaneZ());
            }
            case TOOL_LENGTH_CANCEL -> {
                modal.setToolLengthCompActive(false);
                yield "G49";
            }
            case SPINDLE_START -> {
                NcFormatUtils.validatePositive(cmd.spindleRpm(), "Spindle RPM");
                modal.setActiveSpindleRpm(cmd.spindleRpm());
                modal.setSpindleRunning(true);
                yield "S" + (int) Math.round(cmd.spindleRpm()) + " M03";
            }
            case SPINDLE_STOP -> { modal.setSpindleRunning(false); yield "M05"; }
            case RAPID_MOVE -> formatMotion(cmd.target(), "G00", 0.0, modal);
            case FEED_MOVE, PLUNGE_MOVE -> {
                NcFormatUtils.validatePositive(cmd.feedRateMmMin(), "Feed rate");
                NcFormatUtils.validateCuttingState(modal);
                yield formatMotion(cmd.target(), "G01", cmd.feedRateMmMin(), modal);
            }
            case CANNED_CYCLE_DRILL -> {
                NcFormatUtils.validateCuttingState(modal);
                NcFormatUtils.validatePositive(cmd.feedRateMmMin(), "Cycle feed rate");
                yield formatCannedCycle(cmd, modal);
            }
            case CANNED_CYCLE_CANCEL -> { modal.setActiveMotionGroup("G80"); yield "G80"; }
            case SAFE_RETRACT -> {
                if (modal.isCannedCycleActive()) {
                    throw new IllegalStateException("Safe retract attempted while canned cycle is active without G80 cancellation.");
                }
                modal.setActiveMotionGroup(null);
                yield profile.getSafeRetractCommand();
            }
            case PROGRAM_END -> profile.getProgramEndCommand();
            default -> throw new UnsupportedOperationException("Unsupported NC command: " + cmd.type());
        };
    }

    private String formatMotion(CamPoint3D target, String motionCode, double feedRate, ModalState modal) {
        NcFormatUtils.validateCoordinates(target);
        StringBuilder sb = new StringBuilder();

        boolean motionChanged = !motionCode.equals(modal.getActiveMotionGroup());
        if (!profile.isModalSuppressionEnabled() || motionChanged) {
            sb.append(motionCode);
            modal.setActiveMotionGroup(motionCode);
        }

        NcFormatUtils.appendCoordinate(sb, "X", target.x(), modal.getCurrentX(), profile);
        NcFormatUtils.appendCoordinate(sb, "Y", target.y(), modal.getCurrentY(), profile);
        NcFormatUtils.appendCoordinate(sb, "Z", target.z(), modal.getCurrentZ(), profile);

        if (feedRate > 0.0 && (!profile.isModalSuppressionEnabled() || Math.abs(feedRate - modal.getActiveFeedRate()) > 1e-4)) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("F").append(profile.formatNumber(feedRate));
            modal.setActiveFeedRate(feedRate);
        }

        modal.updatePosition(target.x(), target.y(), target.z());
        return sb.toString().trim();
    }

    private String formatCannedCycle(NcCommand cmd, ModalState modal) {
        CamPoint3D target = cmd.target();
        NcFormatUtils.validateCoordinates(target);
        StringBuilder sb = new StringBuilder();

        sb.append(cmd.text());
        sb.append(" X").append(profile.formatNumber(target.x()));
        sb.append(" Y").append(profile.formatNumber(target.y()));
        sb.append(" Z").append(profile.formatNumber(target.z()));
        sb.append(" R").append(profile.formatNumber(cmd.rPlaneZ()));
        if (cmd.peckDepthMm() > 0.0) {
            sb.append(" Q").append(profile.formatNumber(cmd.peckDepthMm()));
        }
        sb.append(" F").append(profile.formatNumber(cmd.feedRateMmMin()));

        modal.setActiveMotionGroup(cmd.text());
        modal.updatePosition(target.x(), target.y(), target.z());
        modal.setActiveFeedRate(cmd.feedRateMmMin());
        return sb.toString();
    }
}
