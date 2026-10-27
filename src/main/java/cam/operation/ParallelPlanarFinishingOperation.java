package cam.operation;

import cam.geometry.CamTriangulatedSurface;
import cam.geometry.CamTriangulatedSurface.Interval1D;
import cam.model.CamPoint3D;
import cam.model.ManufacturingSetup;
import cam.tool.CamTool;
import cam.tool.ToolType;
import cam.toolpath.MotionType;
import cam.toolpath.Toolpath;

import java.util.List;
import java.util.UUID;

/**
 * Deterministic 3D Parallel Planar Finishing Operation for bounded triangulated surfaces.
 * Evaluates exact flat-endmill disk contact, stepover bounds, and safe obstacle clearance.
 */
public class ParallelPlanarFinishingOperation implements CamOperation {

    private final String id;
    private String name;
    private final CamTriangulatedSurface surface;
    private final CamTool tool;
    private double stepoverMm;
    private double maxSegmentLengthMm = 5.0;
    private double cuttingFeedRateMmMin;
    private double plungeFeedRateMmMin;
    private double spindleRpm;
    private double clearanceHeightZ = 25.0;
    private double localClearanceMm = 2.0;
    private boolean bidirectional = true;
    private boolean cutterRadiusCompensation = true;

    public ParallelPlanarFinishingOperation(String id, String name, CamTriangulatedSurface surface,
                                           CamTool tool, double stepoverMm, double cuttingFeedRateMmMin,
                                           double plungeFeedRateMmMin, double spindleRpm) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "3D Parallel Planar Finishing";
        this.surface = surface;
        this.tool = tool;
        this.stepoverMm = stepoverMm;
        this.cuttingFeedRateMmMin = cuttingFeedRateMmMin;
        this.plungeFeedRateMmMin = plungeFeedRateMmMin;
        this.spindleRpm = spindleRpm;
    }

    @Override public String getId()                         { return id; }
    @Override public String getName()                       { return name; }
    @Override public CamOperationType getOperationType()    { return CamOperationType.PARALLEL_PLANAR_FINISHING; }
    @Override public CamTool getTool()                      { return tool; }
    public CamTriangulatedSurface getSurface()              { return surface; }

    public double getStepover()                             { return stepoverMm; }
    public void setStepover(double stepover)                { this.stepoverMm = stepover; }
    public double getMaxSegmentLength()                     { return maxSegmentLengthMm; }
    public void setMaxSegmentLength(double maxLen)          { this.maxSegmentLengthMm = maxLen; }
    public double getClearanceHeight()                      { return clearanceHeightZ; }
    public void setClearanceHeight(double z)                { this.clearanceHeightZ = z; }
    public boolean isBidirectional()                        { return bidirectional; }
    public void setBidirectional(boolean b)                 { this.bidirectional = b; }
    public boolean isCutterRadiusCompensation()             { return cutterRadiusCompensation; }
    public void setCutterRadiusCompensation(boolean c)      { this.cutterRadiusCompensation = c; }

    @Override
    public String validate(ManufacturingSetup setup) {
        if (setup == null) return "Manufacturing setup cannot be null.";
        if (surface == null || surface.getTriangleCount() == 0) return "Triangulated surface cannot be null or empty.";
        String topErr = surface.validateSingleSheetTopology();
        if (topErr != null) return "Invalid surface topology: " + topErr;

        if (tool == null) return "Tool cannot be null.";
        if (tool.getType() != ToolType.FLAT_ENDMILL) {
            return "Parallel planar finishing requires a flat endmill (got: " + tool.getType() + ").";
        }
        if (tool.getDiameter() <= 0.0 || !Double.isFinite(tool.getDiameter())) return "Tool diameter must be positive and finite.";
        if (stepoverMm <= 0.0 || !Double.isFinite(stepoverMm)) return "Stepover must be positive and finite.";
        if (stepoverMm > tool.getDiameter() + 1e-4) return "Stepover (" + stepoverMm + "mm) cannot exceed tool diameter (" + tool.getDiameter() + "mm).";
        if (maxSegmentLengthMm <= 0.0 || !Double.isFinite(maxSegmentLengthMm)) return "Maximum segment length must be positive and finite.";
        if (cuttingFeedRateMmMin <= 0.0 || plungeFeedRateMmMin <= 0.0) return "Feed rates must be strictly positive.";
        if (spindleRpm <= 0.0) return "Spindle RPM must be strictly positive.";

        double clearZ = (setup.getSafetyPlane() != null) ? setup.getSafetyPlane().getClearanceZ() : clearanceHeightZ;
        if (clearZ <= surface.getMaxZ()) return "Clearance height must be strictly above surface maximum Z.";
        if (setup.getStock() != null) {
            var st = setup.getStock();
            if (surface.getMinZ() < st.getBottomZ() - 1e-4) return "Surface depth penetrates below stock bottom.";
            if (clearZ <= st.getTopZ()) return "Clearance height must be strictly above stock top.";
        }
        return null;
    }

    private double evaluateZ(double x, double y) {
        double r = cutterRadiusCompensation ? tool.getRadius() : 0.0;
        double z = (r > 1e-6) ? surface.queryCutterDiskHeight(x, y, r) : surface.queryHeight(x, y);
        if (Double.isNaN(z)) z = surface.queryHeight(x, y);
        return Double.isNaN(z) ? surface.getMinZ() : z;
    }

    @Override
    public Toolpath generateToolpath(ManufacturingSetup setup) {
        String err = validate(setup);
        if (err != null) throw new IllegalStateException("Cannot generate toolpath: " + err);

        double clearZ = (setup != null && setup.getSafetyPlane() != null) ? setup.getSafetyPlane().getClearanceZ() : clearanceHeightZ;
        Toolpath path = new Toolpath(id);

        double minY = surface.getMinY(), maxY = surface.getMaxY();
        double spanY = maxY - minY;
        int passCount = (spanY < 1e-6) ? 1 : Math.max(2, (int) Math.ceil(spanY / stepoverMm) + 1);
        double stepY = (passCount > 1) ? spanY / (passCount - 1) : 0.0;
        CamPoint3D curPos = null;

        for (int k = 0; k < passCount; k++) {
            double curY = (passCount == 1) ? minY : minY + k * stepY;
            List<Interval1D> intervals = surface.computePassIntervals(curY);
            if (intervals.isEmpty()) continue;

            boolean forward = !bidirectional || (k % 2 == 0);
            int startIdx = forward ? 0 : intervals.size() - 1;
            int stepIdx = forward ? 1 : -1;

            for (int i = startIdx; forward ? i < intervals.size() : i >= 0; i += stepIdx) {
                Interval1D iv = intervals.get(i);
                double x1 = forward ? iv.startX() : iv.endX(), x2 = forward ? iv.endX() : iv.startX();
                double len = Math.abs(x2 - x1);
                int segCount = (len < 1e-6) ? 1 : Math.max(2, (int) Math.ceil(len / maxSegmentLengthMm) + 1);

                double zFirst = evaluateZ(x1, curY);
                CamPoint3D leadIn = new CamPoint3D(x1, curY, zFirst);
                CamPoint3D clearPos = new CamPoint3D(x1, curY, clearZ);

                if (curPos == null) {
                    path.addSegment(MotionType.RAPID, clearPos, clearPos, 0.0, spindleRpm);
                } else if (curPos.distanceTo(leadIn) > 1e-6) {
                    CamPoint3D curRetract = new CamPoint3D(curPos.x(), curPos.y(), clearZ);
                    path.addSegment(MotionType.RETRACT, curPos, curRetract, 0.0, spindleRpm);
                    path.addSegment(MotionType.RAPID, curRetract, clearPos, 0.0, spindleRpm);
                }

                CamPoint3D plungeStart = new CamPoint3D(x1, curY, zFirst + localClearanceMm);
                path.addSegment(MotionType.RAPID, clearPos, plungeStart, 0.0, spindleRpm);
                path.addSegment(MotionType.PLUNGE, plungeStart, leadIn, plungeFeedRateMmMin, spindleRpm);
                curPos = leadIn;

                for (int s = 1; s < segCount; s++) {
                    double t = (double) s / (segCount - 1);
                    double xs = x1 + t * (x2 - x1);
                    double zs = evaluateZ(xs, curY);
                    CamPoint3D next = new CamPoint3D(xs, curY, zs);
                    path.addSegment(MotionType.FEED, curPos, next, cuttingFeedRateMmMin, spindleRpm);
                    curPos = next;
                }
            }
        }
        if (curPos != null) {
            CamPoint3D finalClear = new CamPoint3D(curPos.x(), curPos.y(), clearZ);
            path.addSegment(MotionType.RETRACT, curPos, finalClear, 0.0, spindleRpm);
        }
        return path;
    }
}
