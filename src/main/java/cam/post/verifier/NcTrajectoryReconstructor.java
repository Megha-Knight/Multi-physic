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
 * Resolves modal coordinates, omitted axes, incremental positioning, and canned-cycle peck motions.
 */
public class NcTrajectoryReconstructor {

    public enum CoordinateFrame { WORKPIECE_WCS, MACHINE_G53, UNRESOLVED }

    public record ReconstructedMotion(
        CamPoint3D start, CamPoint3D end, MotionType motionType,
        double feedRate, int toolNumber, CoordinateFrame frame, int blockIndex, String blockText
    ) {
        public ReconstructedMotion(CamPoint3D start, CamPoint3D end, MotionType motionType,
                                   double feedRate, int toolNumber, int blockIndex, String blockText) {
            this(start, end, motionType, feedRate, toolNumber, CoordinateFrame.WORKPIECE_WCS, blockIndex, blockText);
        }
    }

    public record TrajectoryReconstructionResult(
        boolean success, List<ReconstructedMotion> motions, List<String> errors, Toolpath toolpath
    ) {}

    public static TrajectoryReconstructionResult reconstruct(NcProgram p, ControllerProfile prof) { return reconstruct(p, prof, null); }
    public static TrajectoryReconstructionResult reconstruct(NcProgram p, ControllerProfile prof, cam.model.ManufacturingSetup s) {
        if (p == null) throw new IllegalArgumentException("NcProgram cannot be null.");
        return reconstruct(p.getBlocks(), prof, s);
    }
    public static TrajectoryReconstructionResult reconstruct(List<String> b, ControllerProfile prof) { return reconstruct(b, prof, null); }

    public static TrajectoryReconstructionResult reconstruct(List<String> blocks, ControllerProfile profile, cam.model.ManufacturingSetup setup) {
        if (blocks == null) throw new IllegalArgumentException("Block list cannot be null.");
        ControllerProfile prof = (profile != null) ? profile : ControllerProfile.createIso6983Standard();
        List<ReconstructedMotion> motions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Toolpath toolpath = new Toolpath("RECONSTRUCTED");

        double curX = Double.NaN, curY = Double.NaN, curZ = Double.NaN;
        String activeMotionGroup = null, activeDistanceMode = "G90", activeWcs = null;
        int activeTool = 1;
        double activeFeed = 0.0, cycleZ = Double.NaN, cycleR = Double.NaN, cycleQ = Double.NaN;
        boolean terminated = false, toolLengthCompActive = false, cycleActive = false;

        for (int i = 0; i < blocks.size(); i++) {
            String line = blocks.get(i);
            NcBlockParser p = new NcBlockParser(line);
            if (p.isCommentOnly() || p.isProgramNumber() || p.isTapeHeaderOrRewind()) continue;

            if (p.isMalformed()) errors.add("Block " + i + ": Malformed syntax: " + line);
            for (String cmd : p.getUnsupportedCommands()) errors.add("Block " + i + ": Unsupported command '" + cmd + "'.");
            if (p.hasG("G20") && prof.getUnitMode() == ControllerProfile.UnitMode.METRIC_G21) errors.add("Block " + i + ": G20 inch mode in metric profile.");
            if (p.hasG("G21") && prof.getUnitMode() == ControllerProfile.UnitMode.INCH_G20) errors.add("Block " + i + ": G21 metric mode in inch profile.");

            if (p.hasG("G90")) activeDistanceMode = "G90";
            if (p.hasG("G91")) activeDistanceMode = "G91";
            for (String g : p.getGCodes()) if (g.matches("G5[4-9]")) activeWcs = g;
            if (p.hasWord('T')) activeTool = p.getWord('T').intValue();
            if (p.hasWord('F')) activeFeed = p.getWord('F');
            if (p.hasG("G00") || p.hasG("G0")) activeMotionGroup = "G00";
            if (p.hasG("G01") || p.hasG("G1")) activeMotionGroup = "G01";
            if (p.hasG("G02") || p.hasG("G03")) {
                errors.add("Block " + i + ": Circular motion G02/G03 encountered; postprocessor contract specifies linear-only interpolation.");
                activeMotionGroup = "G01";
            }
            if (p.hasG("G43")) { toolLengthCompActive = true; if (p.hasWord('Z')) curZ = p.getWord('Z'); }
            if (p.hasG("G49")) toolLengthCompActive = false;

            if (p.hasG("G28")) {
                boolean isInc = p.hasG("G91") || "G91".equals(activeDistanceMode);
                double ix = p.hasWord('X') ? (isInc && Double.isFinite(curX) ? curX + p.getWord('X') : p.getWord('X')) : curX;
                double iy = p.hasWord('Y') ? (isInc && Double.isFinite(curY) ? curY + p.getWord('Y') : p.getWord('Y')) : curY;
                double iz = p.hasWord('Z') ? (isInc && Double.isFinite(curZ) ? curZ + p.getWord('Z') : p.getWord('Z')) : curZ;
                if ("G90".equals(activeDistanceMode) && !p.hasG("G91")) errors.add("Block " + i + ": G28 in G90 risks intermediate collision; use G91 G28 Z0.");
                if (Double.isFinite(curX) && Double.isFinite(ix)) {
                    CamPoint3D sPt = new CamPoint3D(curX, curY, curZ), iPt = new CamPoint3D(ix, iy, iz);
                    if (!sPt.equals(iPt)) { motions.add(new ReconstructedMotion(sPt, iPt, MotionType.RAPID, activeFeed, activeTool, CoordinateFrame.WORKPIECE_WCS, i, line)); toolpath.addSegment(MotionType.RAPID, sPt, iPt, activeFeed, 0.0); }
                    double hz = (prof.getMachineRetractZ() != null) ? prof.getMachineRetractZ() : 0.0;
                    CamPoint3D hPt = new CamPoint3D(ix, iy, hz);
                    motions.add(new ReconstructedMotion(iPt, hPt, MotionType.RAPID, activeFeed, activeTool, CoordinateFrame.MACHINE_G53, i, line));
                    toolpath.addSegment(MotionType.RAPID, iPt, hPt, activeFeed, 0.0);
                    curX = ix; curY = iy; curZ = hz;
                }
            }

            boolean isCycle = p.hasG("G81") || p.hasG("G82") || p.hasG("G83") || p.hasG("G73");
            if (isCycle || (cycleActive && (p.hasWord('X') || p.hasWord('Y') || p.hasWord('Z')))) {
                if (p.hasWord('Z')) cycleZ = p.getWord('Z');
                if (p.hasWord('R')) cycleR = p.getWord('R');
                if (p.hasWord('Q')) cycleQ = p.getWord('Q');
                if (isCycle) cycleActive = true;
                double hX = p.hasWord('X') ? p.getWord('X') : curX, hY = p.hasWord('Y') ? p.getWord('Y') : curY;
                if (Double.isFinite(hX) && Double.isFinite(hY) && Double.isFinite(cycleZ) && Double.isFinite(cycleR)) {
                    CamPoint3D appPt = new CamPoint3D(hX, hY, cycleR);
                    motions.add(new ReconstructedMotion(new CamPoint3D(curX, curY, curZ), appPt, MotionType.RAPID, activeFeed, activeTool, CoordinateFrame.WORKPIECE_WCS, i, line));
                    toolpath.addSegment(MotionType.RAPID, new CamPoint3D(curX, curY, curZ), appPt, activeFeed, 0.0);
                    if (p.hasG("G83") && cycleQ > 0.0) {
                        double cd = cycleR;
                        while (cd > cycleZ + 1e-6) {
                            double nd = Math.max(cd - cycleQ, cycleZ);
                            CamPoint3D ps = new CamPoint3D(hX, hY, cd), pe = new CamPoint3D(hX, hY, nd);
                            motions.add(new ReconstructedMotion(ps, pe, MotionType.PLUNGE, activeFeed, activeTool, CoordinateFrame.WORKPIECE_WCS, i, line));
                            toolpath.addSegment(MotionType.PLUNGE, ps, pe, activeFeed, 0.0);
                            motions.add(new ReconstructedMotion(pe, appPt, MotionType.RAPID, activeFeed, activeTool, CoordinateFrame.WORKPIECE_WCS, i, line));
                            toolpath.addSegment(MotionType.RAPID, pe, appPt, activeFeed, 0.0);
                            cd = nd;
                        }
                    } else {
                        CamPoint3D bPt = new CamPoint3D(hX, hY, cycleZ);
                        motions.add(new ReconstructedMotion(appPt, bPt, MotionType.PLUNGE, activeFeed, activeTool, CoordinateFrame.WORKPIECE_WCS, i, line));
                        toolpath.addSegment(MotionType.PLUNGE, appPt, bPt, activeFeed, 0.0);
                        motions.add(new ReconstructedMotion(bPt, appPt, MotionType.RAPID, activeFeed, activeTool, CoordinateFrame.WORKPIECE_WCS, i, line));
                        toolpath.addSegment(MotionType.RAPID, bPt, appPt, activeFeed, 0.0);
                    }
                    curX = hX; curY = hY; curZ = cycleR;
                }
            }
            if (p.hasG("G80")) cycleActive = false;

            boolean isMotion = p.hasWord('X') || p.hasWord('Y') || p.hasWord('Z');
            if (isMotion && !p.hasG("G28") && !p.hasG("G43") && !cycleActive && !isCycle) {
                if (terminated) errors.add("Block " + i + ": Motion commanded after program termination M30.");
                if (activeMotionGroup == null) { errors.add("Block " + i + ": Motion commanded without active motion group (G00/G01)."); continue; }
                CoordinateFrame frame = CoordinateFrame.WORKPIECE_WCS;
                if (p.hasG("G53")) frame = CoordinateFrame.MACHINE_G53;
                else if (activeWcs == null) { frame = CoordinateFrame.UNRESOLVED; errors.add("Block " + i + ": Motion commanded without active work coordinate system (G54-G59); physical tool-tip position is unresolved."); }
                else if (setup != null) {
                    var cs = setup.getCoordinateSystem(activeWcs);
                    if (cs == null || !cs.isValid()) { frame = CoordinateFrame.UNRESOLVED; errors.add("Block " + i + ": WCS " + activeWcs + " unconfigured/invalid."); }
                }
                if ("G01".equals(activeMotionGroup) && !toolLengthCompActive && !p.hasG("G53")) {
                    frame = CoordinateFrame.UNRESOLVED; errors.add("Block " + i + ": Cutting motion without G43 tool length compensation.");
                }
                boolean isInc = "G91".equals(activeDistanceMode);
                double tx = p.hasWord('X') ? (isInc && Double.isFinite(curX) ? curX + p.getWord('X') : p.getWord('X')) : curX;
                double ty = p.hasWord('Y') ? (isInc && Double.isFinite(curY) ? curY + p.getWord('Y') : p.getWord('Y')) : curY;
                double tz = p.hasWord('Z') ? (isInc && Double.isFinite(curZ) ? curZ + p.getWord('Z') : p.getWord('Z')) : curZ;
                if (Double.isFinite(curX) && Double.isFinite(tx)) {
                    CamPoint3D sPt = new CamPoint3D(curX, curY, curZ), ePt = new CamPoint3D(tx, ty, tz);
                    MotionType mt = "G01".equals(activeMotionGroup) ? MotionType.FEED : MotionType.RAPID;
                    motions.add(new ReconstructedMotion(sPt, ePt, mt, activeFeed, activeTool, frame, i, line));
                    toolpath.addSegment(mt, sPt, ePt, activeFeed, 0.0);
                }
                if (Double.isFinite(tx)) curX = tx; if (Double.isFinite(ty)) curY = ty; if (Double.isFinite(tz)) curZ = tz;
            }
            if (p.hasM("M30") || p.hasM("M02")) terminated = true;
        }
        return new TrajectoryReconstructionResult(errors.isEmpty(), Collections.unmodifiableList(motions), errors, toolpath);
    }
}
