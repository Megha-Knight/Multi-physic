package cam.post;

import cam.model.CamPoint3D;
import cam.model.ManufacturingSetup;
import cam.operation.CamOperation;
import cam.operation.DeepHoleDrillingOperation;
import cam.operation.PeckRetractStrategy;
import cam.operation.SpotDrillingOperation;
import cam.tool.CamTool;
import cam.toolpath.MotionType;
import cam.toolpath.Toolpath;
import cam.toolpath.ToolpathSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds neutral NC command streams from CAM setups, operations, and toolpaths.
 */
public class NcCommandBuilder {

    public static List<NcCommand> buildFromSetup(ManufacturingSetup setup, ControllerProfile profile) {
        if (setup == null) throw new IllegalArgumentException("ManufacturingSetup cannot be null.");
        List<NcCommand> commands = new ArrayList<>();

        commands.add(NcCommand.programHeader(setup.getName()));
        commands.add(NcCommand.comment("Target Part: " + setup.getTargetPartId()));
        commands.add(NcCommand.safeStart());

        String wcs = (setup.getMcs() != null) ? setup.getMcs().getName() : profile.getDefaultWcs();
        commands.add(NcCommand.workCoordinateSystem(wcs));

        int toolCounter = 1;
        for (CamOperation op : setup.getOperations()) {
            commands.add(NcCommand.comment("--- Operation: " + op.getName() + " ---"));
            CamTool tool = op.getTool();
            int tNum = toolCounter++;
            commands.add(NcCommand.toolChange(tNum, tool.getName()));

            if (profile.supportsCannedCycles() && op instanceof SpotDrillingOperation spotOp) {
                appendSpotDrillingCanned(commands, spotOp, setup);
            } else if (profile.supportsCannedCycles() && op instanceof DeepHoleDrillingOperation deepOp) {
                appendDeepDrillingCanned(commands, deepOp, setup);
            } else {
                Toolpath tp = op.generateToolpath(setup);
                appendToolpathSegments(commands, tp);
            }
        }

        commands.add(NcCommand.safeRetract());
        commands.add(NcCommand.programEnd());
        return commands;
    }

    public static List<NcCommand> buildFromToolpath(ManufacturingSetup setup, Toolpath toolpath,
                                                   CamTool tool, int toolNumber, ControllerProfile profile) {
        if (toolpath == null || toolpath.isEmpty()) throw new IllegalArgumentException("Toolpath cannot be null or empty.");
        List<NcCommand> commands = new ArrayList<>();

        commands.add(NcCommand.programHeader((setup != null) ? setup.getName() : "TOOLPATH_PROG"));
        commands.add(NcCommand.safeStart());
        String wcs = (setup != null && setup.getMcs() != null) ? setup.getMcs().getName() : profile.getDefaultWcs();
        commands.add(NcCommand.workCoordinateSystem(wcs));

        String toolName = (tool != null) ? tool.getName() : "Standard Tool";
        commands.add(NcCommand.toolChange(toolNumber > 0 ? toolNumber : 1, toolName));

        appendToolpathSegments(commands, toolpath);

        commands.add(NcCommand.safeRetract());
        commands.add(NcCommand.programEnd());
        return commands;
    }

    private static void appendToolpathSegments(List<NcCommand> commands, Toolpath tp) {
        if (tp == null || tp.isEmpty()) return;
        double activeRpm = tp.getSegments().get(0).spindleRpm();
        if (activeRpm > 0.0) {
            commands.add(NcCommand.spindleStart(activeRpm));
        }

        for (ToolpathSegment seg : tp.getSegments()) {
            CamPoint3D target = seg.end();
            if (seg.motionType() == MotionType.RAPID || seg.motionType() == MotionType.RETRACT) {
                commands.add(NcCommand.rapid(target));
            } else if (seg.motionType() == MotionType.PLUNGE) {
                commands.add(NcCommand.plunge(target, seg.feedRateMmPerMin(), seg.spindleRpm()));
            } else {
                commands.add(NcCommand.feed(target, seg.feedRateMmPerMin(), seg.spindleRpm()));
            }
        }
    }

    private static void appendSpotDrillingCanned(List<NcCommand> commands, SpotDrillingOperation op, ManufacturingSetup setup) {
        commands.add(NcCommand.spindleStart(op.getSpindleRpm()));
        double clearanceZ = (setup.getSafetyPlane() != null) ? setup.getSafetyPlane().getRetractZ() : 5.0;

        for (CamPoint3D hole : op.getHoleCenters()) {
            double finalZ = hole.z() - op.getSpotDepth();
            double rZ = hole.z() + op.getLocalClearance();
            commands.add(NcCommand.rapid(new CamPoint3D(hole.x(), hole.y(), clearanceZ)));
            commands.add(NcCommand.cannedCycle("G81", new CamPoint3D(hole.x(), hole.y(), finalZ), rZ, 0.0, op.getPlungeFeedRate()));
        }
        commands.add(NcCommand.cannedCycleCancel());
    }

    private static void appendDeepDrillingCanned(List<NcCommand> commands, DeepHoleDrillingOperation op, ManufacturingSetup setup) {
        commands.add(NcCommand.spindleStart(op.getSpindleRpm()));
        String cycle = (op.getRetractStrategy() == PeckRetractStrategy.CHIP_BREAK) ? "G73" : "G83";
        double clearanceZ = (setup.getSafetyPlane() != null) ? setup.getSafetyPlane().getRetractZ() : 5.0;

        for (CamPoint3D hole : op.getHoleCenters()) {
            double finalZ = hole.z() - op.getFinalDepth();
            double rZ = hole.z() + 2.0; // standard R-plane clearance
            commands.add(NcCommand.rapid(new CamPoint3D(hole.x(), hole.y(), clearanceZ)));
            commands.add(NcCommand.cannedCycle(cycle, new CamPoint3D(hole.x(), hole.y(), finalZ), rZ, op.getPeckDepth(), op.getPlungeFeedRate()));
        }
        commands.add(NcCommand.cannedCycleCancel());
    }
}
