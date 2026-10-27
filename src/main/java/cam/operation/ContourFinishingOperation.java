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
 * 2D/2.5D CAM Contour Finishing Operation.
 * Integrates deterministic contour offset geometry, lead-in/lead-out transitions,
 * multi-level axial stepdown, and fail-closed gouge and collision validation.
 */
public class ContourFinishingOperation implements CamOperation {

    private final String id;
    private String name;
    private final CamContour2D contour;
    private final CamTool tool;
    private final ContourSide side;
    private CamLeadConfig leadInConfig = CamLeadConfig.circularArc(3.0);
    private CamLeadConfig leadOutConfig = CamLeadConfig.circularArc(3.0);
    private double topZ = 0.0;
    private double targetDepth;
    private double stepdown;
    private double finishStockAllowance = 0.0;
    private double cuttingFeedRateMmMin;
    private double plungeFeedRateMmMin;
    private double spindleRpm;
    private double localClearanceMm = 2.0;

    public ContourFinishingOperation(String id, String name, CamContour2D contour, CamTool tool,
                                     ContourSide side, double targetDepth, double stepdown,
                                     double cuttingFeedRateMmMin, double plungeFeedRateMmMin, double spindleRpm) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "Contour Finishing";
        this.contour = contour;
        this.tool = tool;
        this.side = side;
        this.targetDepth = targetDepth;
        this.stepdown = stepdown;
        this.cuttingFeedRateMmMin = cuttingFeedRateMmMin;
        this.plungeFeedRateMmMin = plungeFeedRateMmMin;
        this.spindleRpm = spindleRpm;
    }

    @Override public String getId()                         { return id; }
    @Override public String getName()                       { return name; }
    @Override public CamOperationType getOperationType()    { return CamOperationType.CONTOUR_FINISHING; }
    @Override public CamTool getTool()                      { return tool; }
    public CamContour2D getContour()                        { return contour; }
    public ContourSide getSide()                            { return side; }

    public CamLeadConfig getLeadInConfig()                  { return leadInConfig; }
    public void setLeadInConfig(CamLeadConfig cfg)          { this.leadInConfig = (cfg != null) ? cfg : CamLeadConfig.none(); }
    public CamLeadConfig getLeadOutConfig()                 { return leadOutConfig; }
    public void setLeadOutConfig(CamLeadConfig cfg)         { this.leadOutConfig = (cfg != null) ? cfg : CamLeadConfig.none(); }

    public double getTopZ()                                 { return topZ; }
    public void setTopZ(double z)                           { this.topZ = z; }
    public double getTargetDepth()                          { return targetDepth; }
    public double getStepdown()                             { return stepdown; }
    public double getFinishStockAllowance()                 { return finishStockAllowance; }
    public void setFinishStockAllowance(double allowance)   { this.finishStockAllowance = allowance; }
    public double getLocalClearance()                       { return localClearanceMm; }
    public void setLocalClearance(double clr)               { this.localClearanceMm = clr; }

    @Override
    public String validate(ManufacturingSetup setup) {
        if (setup == null) return "Manufacturing setup cannot be null.";
        if (contour == null) return "Contour geometry cannot be null.";
        if (tool == null) return "Finishing tool cannot be null.";
        if (tool.getType().isHolemaking()) {
            return "Contour finishing requires a milling cutter (got holemaking tool: " + tool.getType() + ").";
        }
        if (side == null) return "Machining side cannot be null.";
        if (!contour.isClosed() && side.isClosedOnly()) {
            return "Open contour cannot specify closed-only machining side: " + side;
        }
        if (targetDepth <= 0.0) return "Target depth must be strictly positive (got: " + targetDepth + ").";
        if (stepdown <= 0.0) return "Stepdown must be strictly positive (got: " + stepdown + ").";
        if (finishStockAllowance < 0.0) return "Finish stock allowance cannot be negative.";
        if (cuttingFeedRateMmMin <= 0.0 || plungeFeedRateMmMin <= 0.0 || spindleRpm <= 0.0) {
            return "Feed rates and spindle speed must be strictly positive.";
        }

        double offsetDist = tool.getRadius() + finishStockAllowance;
        CamContourOffsetResult offsetRes = CamContourOffsetEngine.offset(contour, offsetDist, side);
        if (!offsetRes.isSuccess()) {
            return "Contour finishing offset failed: " + offsetRes.getErrors();
        }

        CamLeadResult leadRes = CamLeadEngine.generateLeads(resolveEffContour(offsetRes.getOffsetContour()), side,
            leadInConfig, leadOutConfig, contour);
        if (!leadRes.isSuccess()) {
            return "Contour finishing lead generation failed: " + leadRes.getErrors();
        }

        var stock = setup.getStock();
        if (stock != null) {
            for (CamPoint2D pt : leadRes.getCombinedPath().getVertices()) {
                if (pt.x() < stock.getMin().x() || pt.x() > stock.getMax().x()
                    || pt.y() < stock.getMin().y() || pt.y() > stock.getMax().y()) {
                    return "Finishing toolpath trajectory extends outside stock envelope.";
                }
            }
        }
        return null;
    }

    @Override
    public Toolpath generateToolpath(ManufacturingSetup setup) {
        String err = validate(setup);
        if (err != null) throw new IllegalStateException("Cannot generate finishing toolpath: " + err);

        double offsetDist = tool.getRadius() + finishStockAllowance;
        CamContourOffsetResult offsetRes = CamContourOffsetEngine.offset(contour, offsetDist, side);
        CamLeadResult leadRes = CamLeadEngine.generateLeads(resolveEffContour(offsetRes.getOffsetContour()), side,
            leadInConfig, leadOutConfig, contour);

        CamContour2D pathContour = leadRes.getCombinedPath();
        Toolpath path = new Toolpath(id);
        double safetyZ = setup.getSafetyPlane().getClearanceZ();
        double approachZ = topZ + localClearanceMm;
        CamPoint2D startPt = pathContour.getStart();
        CamPoint3D currentPos = new CamPoint3D(startPt.x(), startPt.y(), safetyZ);

        List<Double> levels = computeZLevels();
        for (double z : levels) {
            // Rapid approach to XY at safety plane
            CamPoint3D aboveEntrySafety = new CamPoint3D(startPt.x(), startPt.y(), safetyZ);
            if (currentPos.distanceTo(aboveEntrySafety) > 1e-4) {
                path.addSegment(MotionType.RAPID, currentPos, aboveEntrySafety, 0.0, spindleRpm);
                currentPos = aboveEntrySafety;
            }

            // Rapid to approach clearance
            CamPoint3D atApproach = new CamPoint3D(startPt.x(), startPt.y(), approachZ);
            path.addSegment(MotionType.RAPID, currentPos, atApproach, 0.0, spindleRpm);
            currentPos = atApproach;

            // Plunge to cutting depth
            CamPoint3D atCutZ = new CamPoint3D(startPt.x(), startPt.y(), z);
            path.addSegment(MotionType.PLUNGE, currentPos, atCutZ, plungeFeedRateMmMin, spindleRpm);
            currentPos = atCutZ;

            // Cut full combined path (lead-in, contour, lead-out)
            for (int i = 1; i < pathContour.getVertexCount(); i++) {
                CamPoint2D nextPt = pathContour.getVertices().get(i);
                CamPoint3D nextPos = new CamPoint3D(nextPt.x(), nextPt.y(), z);
                path.addSegment(MotionType.FEED, currentPos, nextPos, cuttingFeedRateMmMin, spindleRpm);
                currentPos = nextPos;
            }

            // Retract to approach clearance then to safety clearance
            CamPoint3D retPos = new CamPoint3D(currentPos.x(), currentPos.y(), approachZ);
            path.addSegment(MotionType.RETRACT, currentPos, retPos, 0.0, spindleRpm);
            CamPoint3D safePos = new CamPoint3D(currentPos.x(), currentPos.y(), safetyZ);
            path.addSegment(MotionType.RETRACT, retPos, safePos, 0.0, spindleRpm);
            currentPos = safePos;
        }
        return path;
    }

    private List<Double> computeZLevels() {
        List<Double> levels = new ArrayList<>();
        double remaining = targetDepth, curZ = topZ;
        while (remaining > 1e-4) {
            double step = Math.min(stepdown, remaining);
            curZ -= step;
            levels.add(curZ);
            remaining -= step;
        }
        return levels;
    }

    private CamContour2D resolveEffContour(CamContour2D c) {
        if (!contour.isClosed() || (leadInConfig.type() == CamLeadType.NONE && leadOutConfig.type() == CamLeadType.NONE)) return c;
        int best = 0; double maxL = 0;
        for (int i = 0; i < c.getSegmentCount(); i++) {
            double len = c.getSegment(i).length();
            if (len > maxL) { maxL = len; best = i; }
        }
        var seg = c.getSegment(best);
        CamPoint2D mid = new CamPoint2D((seg.p1().x() + seg.p2().x()) * 0.5, (seg.p1().y() + seg.p2().y()) * 0.5);
        int n = c.getVertexCount();
        List<CamPoint2D> pts = new ArrayList<>(n + 1);
        pts.add(mid);
        for (int i = 0; i < n; i++) pts.add(c.getVertices().get((best + 1 + i) % n));
        return CamContour2D.closed(pts);
    }
}
