package cam.operation;

import cam.geometry.*;
import cam.model.CamPoint3D;
import cam.model.ManufacturingSetup;
import cam.tool.CamTool;
import cam.toolpath.MotionType;
import cam.toolpath.Toolpath;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Deterministic 3D Waterline (Z-Level) Finishing Operation for steep/vertical triangulated surfaces.
 * Generates bounded constant-Z contour slices, cutter-radius compensation, and safe rapid transitions.
 */
public class WaterlineFinishingOperation implements CamOperation {

    private final String id;
    private String name;
    private final CamTriangulatedSurface surface;
    private final CamTool tool;
    private double topZ;
    private double bottomZ;
    private double stepdown;
    private double cuttingFeedRateMmMin;
    private double plungeFeedRateMmMin;
    private double spindleRpm;
    private double clearanceHeightZ = 25.0;
    private double localClearanceMm = 2.0;
    private boolean cutterRadiusCompensation = true;
    private ContourSide side = ContourSide.OUTSIDE;
    private int maxLevels = 1000;

    public WaterlineFinishingOperation(String id, String name, CamTriangulatedSurface surface, CamTool tool,
                                       double topZ, double bottomZ, double stepdown,
                                       double cuttingFeedRateMmMin, double plungeFeedRateMmMin, double spindleRpm) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "3D Waterline Finishing";
        this.surface = surface;
        this.tool = tool;
        this.topZ = topZ;
        this.bottomZ = bottomZ;
        this.stepdown = stepdown;
        this.cuttingFeedRateMmMin = cuttingFeedRateMmMin;
        this.plungeFeedRateMmMin = plungeFeedRateMmMin;
        this.spindleRpm = spindleRpm;
    }

    @Override public String getId()                         { return id; }
    @Override public String getName()                       { return name; }
    @Override public CamOperationType getOperationType()    { return CamOperationType.WATERLINE_FINISHING; }
    @Override public CamTool getTool()                      { return tool; }
    public CamTriangulatedSurface getSurface()              { return surface; }

    public double getTopZ()                                 { return topZ; }
    public void setTopZ(double z)                           { this.topZ = z; }
    public double getBottomZ()                              { return bottomZ; }
    public void setBottomZ(double z)                        { this.bottomZ = z; }
    public double getStepdown()                             { return stepdown; }
    public void setStepdown(double s)                       { this.stepdown = s; }
    public double getClearanceHeight()                      { return clearanceHeightZ; }
    public void setClearanceHeight(double z)                { this.clearanceHeightZ = z; }
    public boolean isCutterRadiusCompensation()             { return cutterRadiusCompensation; }
    public void setCutterRadiusCompensation(boolean c)      { this.cutterRadiusCompensation = c; }
    public ContourSide getSide()                            { return side; }
    public void setSide(ContourSide s)                      { this.side = (s != null) ? s : ContourSide.OUTSIDE; }
    public int getMaxLevels()                               { return maxLevels; }
    public void setMaxLevels(int max)                       { this.maxLevels = max; }

    @Override
    public String validate(ManufacturingSetup setup) {
        if (setup == null) return "Manufacturing setup cannot be null.";
        if (surface == null || surface.getTriangleCount() == 0) return "Triangulated surface cannot be null or empty.";
        if (tool == null) return "Tool cannot be null.";
        if (tool.getType().isHolemaking()) return "Waterline finishing requires a milling cutter (got: " + tool.getType() + ").";
        if (tool.getDiameter() <= 0.0 || !Double.isFinite(tool.getDiameter())) return "Tool diameter must be positive and finite.";
        if (stepdown <= 0.0 || !Double.isFinite(stepdown)) return "Stepdown must be strictly positive and finite.";
        if (!Double.isFinite(topZ) || !Double.isFinite(bottomZ)) return "Z bounds must be finite.";
        if (topZ < bottomZ) return "Top Z (" + topZ + ") cannot be less than bottom Z (" + bottomZ + ").";
        if (topZ > surface.getMaxZ() + 1e-4 || bottomZ < surface.getMinZ() - 1e-4) {
            return "Z bounds [" + bottomZ + ", " + topZ + "] exceed surface vertical domain [" + surface.getMinZ() + ", " + surface.getMaxZ() + "].";
        }
        int numLevels = (int) Math.ceil((topZ - bottomZ) / stepdown) + 1;
        if (numLevels > maxLevels) return "Requested Z levels (" + numLevels + ") exceeds configured maximum limit (" + maxLevels + ").";
        if (cuttingFeedRateMmMin <= 0.0 || plungeFeedRateMmMin <= 0.0 || spindleRpm <= 0.0) return "Feed rates and spindle RPM must be strictly positive.";

        double clearZ = (setup.getSafetyPlane() != null) ? setup.getSafetyPlane().getClearanceZ() : clearanceHeightZ;
        if (clearZ <= surface.getMaxZ()) return "Clearance height must be strictly above surface maximum Z.";
        if (setup.getStock() != null) {
            var st = setup.getStock();
            if (surface.getMinZ() < st.getBottomZ() - 1e-4) return "Surface depth penetrates below stock bottom.";
            if (clearZ <= st.getTopZ()) return "Clearance height must be strictly above stock top.";
        }
        return null;
    }

    @Override
    public Toolpath generateToolpath(ManufacturingSetup setup) {
        String err = validate(setup);
        if (err != null) throw new IllegalStateException("Cannot generate toolpath: " + err);

        double clearZ = (setup != null && setup.getSafetyPlane() != null) ? setup.getSafetyPlane().getClearanceZ() : clearanceHeightZ;
        Toolpath path = new Toolpath(id);

        List<Double> zLevels = new ArrayList<>();
        double currZ = topZ;
        while (currZ >= bottomZ - 1e-6) {
            zLevels.add(currZ);
            if (Math.abs(currZ - bottomZ) < 1e-6) break;
            currZ -= stepdown;
            if (currZ < bottomZ && Math.abs(currZ + stepdown - bottomZ) > 1e-6) {
                zLevels.add(bottomZ);
                break;
            }
        }

        CamPoint3D curPos = null;

        for (double z : zLevels) {
            List<CamContour2D> contours = CamWaterlineSlicer.slice(surface, z);
            for (CamContour2D rawContour : contours) {
                CamContour2D effContour = rawContour;
                if (cutterRadiusCompensation && tool.getRadius() > 1e-6) {
                    if (rawContour.isClosed() || !side.isClosedOnly()) {
                        CamContourOffsetResult offRes = CamContourOffsetEngine.offset(rawContour, tool.getRadius(), side);
                        if (offRes.isSuccess()) {
                            effContour = offRes.getOffsetContour();
                        } else {
                            continue; // skip collapsed or unfeasible offset
                        }
                    }
                }

                List<CamPoint2D> pts = effContour.getVertices();
                if (pts.size() < 2) continue;

                CamPoint2D firstPt = pts.get(0);
                CamPoint3D clearAbove = new CamPoint3D(firstPt.x(), firstPt.y(), clearZ);
                CamPoint3D plungeStart = new CamPoint3D(firstPt.x(), firstPt.y(), z + localClearanceMm);
                CamPoint3D cutStart = new CamPoint3D(firstPt.x(), firstPt.y(), z);

                if (curPos == null) {
                    path.addSegment(MotionType.RAPID, clearAbove, clearAbove, 0.0, spindleRpm);
                } else if (curPos.distanceTo(cutStart) > 1e-6) {
                    CamPoint3D curRetract = new CamPoint3D(curPos.x(), curPos.y(), clearZ);
                    path.addSegment(MotionType.RETRACT, curPos, curRetract, 0.0, spindleRpm);
                    path.addSegment(MotionType.RAPID, curRetract, clearAbove, 0.0, spindleRpm);
                }

                path.addSegment(MotionType.RAPID, clearAbove, plungeStart, 0.0, spindleRpm);
                path.addSegment(MotionType.PLUNGE, plungeStart, cutStart, plungeFeedRateMmMin, spindleRpm);
                curPos = cutStart;

                for (int i = 1; i < pts.size(); i++) {
                    CamPoint2D pt = pts.get(i);
                    CamPoint3D next = new CamPoint3D(pt.x(), pt.y(), z);
                    path.addSegment(MotionType.FEED, curPos, next, cuttingFeedRateMmMin, spindleRpm);
                    curPos = next;
                }

                if (effContour.isClosed()) {
                    path.addSegment(MotionType.FEED, curPos, cutStart, cuttingFeedRateMmMin, spindleRpm);
                    curPos = cutStart;
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
