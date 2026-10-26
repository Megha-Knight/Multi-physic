package cam.operation;

import cam.geometry.CamPoint2D;
import cam.geometry.CamPolygon2D;
import cam.model.CamPoint3D;
import cam.model.ManufacturingSetup;
import cam.tool.CamTool;
import cam.toolpath.MotionType;
import cam.toolpath.Toolpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 2.5D Prismatic Pocket Roughing engine generating deterministic concentric
 * clearing passes with multi-level axial stepdown, safe entry, and footprint validation.
 */
public class PrismaticRoughingOperation implements CamOperation {

    private final String id;
    private String name;
    private final CamPolygon2D boundary;
    private final CamTool tool;
    private double topZ = 0.0;
    private double targetDepth;
    private double stepdown;
    private double stepover;
    private double boundaryAllowance = 0.5;
    private double cuttingFeedRateMmMin;
    private double plungeFeedRateMmMin;
    private double spindleRpm;
    private double localClearanceMm = 2.0;

    public PrismaticRoughingOperation(String id, String name, CamPolygon2D boundary, CamTool tool,
                                      double targetDepth, double stepdown, double stepover,
                                      double cuttingFeedRateMmMin, double plungeFeedRateMmMin, double spindleRpm) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "Prismatic Pocket Roughing";
        this.boundary = boundary;
        this.tool = tool;
        this.targetDepth = targetDepth;
        this.stepdown = stepdown;
        this.stepover = stepover;
        this.cuttingFeedRateMmMin = cuttingFeedRateMmMin;
        this.plungeFeedRateMmMin = plungeFeedRateMmMin;
        this.spindleRpm = spindleRpm;
    }

    @Override public String getId()                  { return id; }
    @Override public String getName()                { return name; }
    @Override public CamOperationType getOperationType() { return CamOperationType.PRISMATIC_ROUGHING; }
    @Override public CamTool getTool()               { return tool; }
    public CamPolygon2D getBoundary()                { return boundary; }

    public double getTopZ()                          { return topZ; }
    public void setTopZ(double z)                    { this.topZ = z; }
    public double getTargetDepth()                   { return targetDepth; }
    public double getStepdown()                      { return stepdown; }
    public double getStepover()                      { return stepover; }
    public double getBoundaryAllowance()             { return boundaryAllowance; }
    public void setBoundaryAllowance(double a)       { this.boundaryAllowance = a; }
    public double getLocalClearance()                { return localClearanceMm; }
    public void setLocalClearance(double clr)        { this.localClearanceMm = clr; }

    @Override
    public String validate(ManufacturingSetup setup) {
        if (setup == null) return "Manufacturing setup cannot be null.";
        if (boundary == null) return "Pocket boundary polygon cannot be null.";
        if (tool == null) return "Milling tool cannot be null.";
        if (tool.getType().isHolemaking()) {
            return "Pocket roughing requires a milling cutter (got holemaking tool: " + tool.getType() + ").";
        }
        if (targetDepth <= 0.0) return "Target depth must be strictly positive (got: " + targetDepth + ").";
        if (stepdown <= 0.0) return "Stepdown must be strictly positive (got: " + stepdown + ").";
        if (stepover <= 0.0) return "Stepover must be strictly positive (got: " + stepover + ").";
        if (stepover > tool.getDiameter() * 0.85 + 1e-6) {
            return String.format("Stepover (%.2f mm) exceeds maximum allowable 85%% of tool diameter (%.2f mm).",
                    stepover, tool.getDiameter() * 0.85);
        }
        if (boundaryAllowance < 0.0) return "Boundary allowance cannot be negative.";
        if (cuttingFeedRateMmMin <= 0.0 || plungeFeedRateMmMin <= 0.0 || spindleRpm <= 0.0) {
            return "Feed rates and spindle speed must be strictly positive.";
        }

        double initialOffset = tool.getRadius() + boundaryAllowance;
        CamPolygon2D firstOffset = boundary.createInwardOffset(initialOffset);
        if (firstOffset == null) {
            return String.format("Cutter radius (%.2f mm) + allowance (%.2f mm) collapses pocket boundary.",
                    tool.getRadius(), boundaryAllowance);
        }

        var stock = setup.getStock();
        if (stock != null) {
            for (CamPoint2D pt : boundary.getVertices()) {
                if (pt.x() < stock.getMin().x() || pt.x() > stock.getMax().x()
                    || pt.y() < stock.getMin().y() || pt.y() > stock.getMax().y()) {
                    return "Pocket boundary extends outside stock dimensions.";
                }
            }
        }
        return null;
    }

    @Override
    public Toolpath generateToolpath(ManufacturingSetup setup) {
        String err = validate(setup);
        if (err != null) throw new IllegalStateException("Cannot generate roughing toolpath: " + err);

        List<CamPolygon2D> loops = computeClearingLoops();
        if (loops.isEmpty()) throw new IllegalStateException("No valid clearing passes could be generated.");

        Toolpath path = new Toolpath(id);
        double safetyZ = setup.getSafetyPlane().getClearanceZ();
        double finalZ = topZ - targetDepth;
        CamPoint2D initPt = loops.get(0).getVertices().get(0);
        CamPoint3D currentPos = new CamPoint3D(initPt.x(), initPt.y(), safetyZ);

        double curZ = topZ;
        while (curZ > finalZ + 1e-6) {
            double levelZ = Math.max(finalZ, curZ - stepdown);

            for (CamPolygon2D loop : loops) {
                List<CamPoint2D> v = loop.getVertices();
                CamPoint2D start2d = v.get(0);
                double approachZ = levelZ + localClearanceMm;

                // 1. Rapid at safety plane to above entry XY
                CamPoint3D aboveEntrySafety = new CamPoint3D(start2d.x(), start2d.y(), safetyZ);
                if (currentPos.distanceTo(aboveEntrySafety) > 1e-4) {
                    path.addSegment(MotionType.RAPID, currentPos, aboveEntrySafety, 0.0, spindleRpm);
                    currentPos = aboveEntrySafety;
                }

                // 2. Rapid down to local approach level
                CamPoint3D atApproach = new CamPoint3D(start2d.x(), start2d.y(), approachZ);
                path.addSegment(MotionType.RAPID, currentPos, atApproach, 0.0, spindleRpm);
                currentPos = atApproach;

                // 3. Plunge feed into material to level Z
                CamPoint3D atCutZ = new CamPoint3D(start2d.x(), start2d.y(), levelZ);
                path.addSegment(MotionType.PLUNGE, currentPos, atCutZ, plungeFeedRateMmMin, spindleRpm);
                currentPos = atCutZ;

                // 4. Cutting feed around clearing loop
                for (int i = 1; i <= v.size(); i++) {
                    CamPoint2D pt = v.get(i % v.size());
                    CamPoint3D nextCutPos = new CamPoint3D(pt.x(), pt.y(), levelZ);
                    path.addSegment(MotionType.FEED, currentPos, nextCutPos, cuttingFeedRateMmMin, spindleRpm);
                    currentPos = nextCutPos;
                }

                // 5. Retract up to approach level
                path.addSegment(MotionType.RETRACT, currentPos, atApproach, 0.0, spindleRpm);
                currentPos = atApproach;

                // Retract to safety plane
                path.addSegment(MotionType.RETRACT, currentPos, aboveEntrySafety, 0.0, spindleRpm);
                currentPos = aboveEntrySafety;
            }

            curZ = levelZ;
        }

        return path;
    }

    public List<CamPolygon2D> computeClearingLoops() {
        List<CamPolygon2D> list = new ArrayList<>();
        double baseOffset = tool.getRadius() + boundaryAllowance;
        double currentOffset = baseOffset;

        while (true) {
            CamPolygon2D loop = boundary.createInwardOffset(currentOffset);
            if (loop == null) break;
            list.add(loop);
            currentOffset += stepover;
        }
        return Collections.unmodifiableList(list);
    }
}
