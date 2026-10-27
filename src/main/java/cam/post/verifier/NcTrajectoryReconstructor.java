package cam.post.verifier;

import cam.model.CamPoint3D;
import cam.post.ControllerProfile;
import cam.post.NcProgram;
import cam.toolpath.MotionType;
import cam.toolpath.Toolpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reconstructs the actual effective 3D toolpath trajectory from emitted NC G-code blocks.
 * Resolves modal coordinates, omitted axes, and incremental vs absolute positioning.
 */
public class NcTrajectoryReconstructor {

    public record ReconstructedMotion(
        CamPoint3D start,
        CamPoint3D end,
        MotionType motionType,
        double feedRate,
        int toolNumber,
        int blockIndex,
        String blockText
    ) {}

    public record TrajectoryReconstructionResult(
        boolean success,
        List<ReconstructedMotion> motions,
        List<String> errors,
        Toolpath toolpath
    ) {}

    public static TrajectoryReconstructionResult reconstruct(NcProgram program, ControllerProfile profile) {
        if (program == null) throw new IllegalArgumentException("NcProgram cannot be null.");
        return reconstruct(program.getBlocks(), profile);
    }

    public static TrajectoryReconstructionResult reconstruct(List<String> blocks, ControllerProfile profile) {
        if (blocks == null) throw new IllegalArgumentException("Block list cannot be null.");
        ControllerProfile prof = (profile != null) ? profile : ControllerProfile.createIso6983Standard();

        List<ReconstructedMotion> motions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Toolpath toolpath = new Toolpath("RECONSTRUCTED");

        double curX = Double.NaN, curY = Double.NaN, curZ = Double.NaN;
        String activeMotionGroup = null;
        String activeDistanceMode = "G90";
        int activeTool = 1;
        double activeFeed = 0.0;
        boolean terminated = false;

        for (int i = 0; i < blocks.size(); i++) {
            String line = blocks.get(i);
            NcBlockParser p = new NcBlockParser(line);
            if (p.isCommentOnly() || p.isProgramNumber() || p.isTapeHeaderOrRewind()) continue;

            if (p.isMalformed()) errors.add("Block " + i + ": Malformed syntax: " + line);
            for (String cmd : p.getUnsupportedCommands()) {
                errors.add("Block " + i + ": Unsupported command '" + cmd + "'.");
            }

            if (p.hasG("G90")) activeDistanceMode = "G90";
            if (p.hasG("G91")) activeDistanceMode = "G91";
            if (p.hasWord('T')) activeTool = p.getWord('T').intValue();
            if (p.hasWord('F')) activeFeed = p.getWord('F');

            if (p.hasG("G00") || p.hasG("G0")) activeMotionGroup = "G00";
            if (p.hasG("G01") || p.hasG("G1")) activeMotionGroup = "G01";
            if (p.hasG("G02") || p.hasG("G03")) {
                errors.add("Block " + i + ": Circular motion G02/G03 encountered; postprocessor contract specifies linear-only interpolation.");
                activeMotionGroup = "G01";
            }

            if (p.hasG("G43") && p.hasWord('Z')) {
                curZ = p.getWord('Z');
            }

            boolean isMotion = p.hasWord('X') || p.hasWord('Y') || p.hasWord('Z');
            if (isMotion && !p.hasG("G53") && !p.hasG("G28") && !p.hasG("G43")) {
                if (terminated) errors.add("Block " + i + ": Motion commanded after program termination M30.");
                if (activeMotionGroup == null) {
                    errors.add("Block " + i + ": Motion commanded without active motion group (G00/G01).");
                    continue;
                }

                boolean isInc = "G91".equals(activeDistanceMode);
                double targetX = p.hasWord('X') ? (isInc && Double.isFinite(curX) ? curX + p.getWord('X') : p.getWord('X')) : curX;
                double targetY = p.hasWord('Y') ? (isInc && Double.isFinite(curY) ? curY + p.getWord('Y') : p.getWord('Y')) : curY;
                double targetZ = p.hasWord('Z') ? (isInc && Double.isFinite(curZ) ? curZ + p.getWord('Z') : p.getWord('Z')) : curZ;

                if (Double.isFinite(curX) && Double.isFinite(curY) && Double.isFinite(curZ)
                        && Double.isFinite(targetX) && Double.isFinite(targetY) && Double.isFinite(targetZ)) {
                    CamPoint3D startPt = new CamPoint3D(curX, curY, curZ);
                    CamPoint3D endPt = new CamPoint3D(targetX, targetY, targetZ);
                    MotionType mType = "G01".equals(activeMotionGroup) ? MotionType.FEED : MotionType.RAPID;
                    motions.add(new ReconstructedMotion(startPt, endPt, mType, activeFeed, activeTool, i, line));
                    toolpath.addSegment(mType, startPt, endPt, activeFeed, 0.0);
                }

                if (Double.isFinite(targetX)) curX = targetX;
                if (Double.isFinite(targetY)) curY = targetY;
                if (Double.isFinite(targetZ)) curZ = targetZ;
            }

            if (p.hasM("M30") || p.hasM("M02")) terminated = true;
        }

        return new TrajectoryReconstructionResult(errors.isEmpty(), Collections.unmodifiableList(motions), errors, toolpath);
    }
}
