package cam.operation;

import cam.model.CamPoint3D;
import cam.model.ManufacturingSetup;
import cam.tool.CamTool;
import cam.tool.ToolType;
import cam.toolpath.MotionType;
import cam.toolpath.Toolpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Validated deep-hole peck drilling operation (G83 Full Retract / G73 Chip Break)
 * with exact-depth termination, progressive axial plunges, and collision-free transit.
 */
public class DeepHoleDrillingOperation implements CamOperation {

    private final String id;
    private String name;
    private final CamTool tool;
    private final List<CamPoint3D> holeCenters = new ArrayList<>();
    private double finalDepth;
    private double peckDepth;
    private PeckRetractStrategy retractStrategy = PeckRetractStrategy.FULL_RETRACT;
    private double plungeFeedRateMmMin;
    private double spindleRpm;
    private double localClearanceMm = 2.0;
    private double chipBreakReliefMm = 0.8;
    private double approachReliefMm = 0.5;

    public DeepHoleDrillingOperation(String id, String name, CamTool tool,
                                     double finalDepth, double peckDepth,
                                     double plungeFeedRateMmMin, double spindleRpm) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "Deep-Hole Peck Drilling";
        this.tool = tool;
        this.finalDepth = finalDepth;
        this.peckDepth = peckDepth;
        this.plungeFeedRateMmMin = plungeFeedRateMmMin;
        this.spindleRpm = spindleRpm;
    }

    public void addHoleCenter(CamPoint3D pt) { if (pt != null) holeCenters.add(pt); }
    public void addHoleCenter(double x, double y, double z) { addHoleCenter(new CamPoint3D(x, y, z)); }
    public List<CamPoint3D> getHoleCenters() { return Collections.unmodifiableList(holeCenters); }

    @Override public String getId()                  { return id; }
    @Override public String getName()                { return name; }
    @Override public CamOperationType getOperationType() { return CamOperationType.DEEP_HOLE_DRILLING; }
    @Override public CamTool getTool()               { return tool; }

    public double getFinalDepth()                    { return finalDepth; }
    public void setFinalDepth(double d)              { this.finalDepth = d; }
    public double getPeckDepth()                     { return peckDepth; }
    public void setPeckDepth(double d)               { this.peckDepth = d; }
    public PeckRetractStrategy getRetractStrategy()  { return retractStrategy; }
    public void setRetractStrategy(PeckRetractStrategy s) { this.retractStrategy = (s != null) ? s : PeckRetractStrategy.FULL_RETRACT; }
    public double getPlungeFeedRate()                { return plungeFeedRateMmMin; }
    public void setPlungeFeedRate(double f)          { this.plungeFeedRateMmMin = f; }
    public double getSpindleRpm()                    { return spindleRpm; }
    public void setSpindleRpm(double rpm)            { this.spindleRpm = rpm; }
    public double getLocalClearance()                { return localClearanceMm; }
    public void setLocalClearance(double clr)        { this.localClearanceMm = clr; }

    @Override
    public String validate(ManufacturingSetup setup) {
        if (setup == null) return "Manufacturing setup cannot be null.";
        if (tool == null) return "Tool cannot be null.";
        if (tool.getType() != ToolType.TWIST_DRILL) {
            return "Deep-hole peck drilling requires a TWIST_DRILL cutter (got: " + tool.getType() + ").";
        }
        if (finalDepth <= 0.0) return "Final depth must be strictly positive (got: " + finalDepth + ").";
        if (peckDepth <= 0.0) return "Peck depth must be strictly positive (got: " + peckDepth + ").";
        if (finalDepth > tool.getFluteLength()) {
            return "Final depth (" + finalDepth + " mm) exceeds tool flute length (" + tool.getFluteLength() + " mm).";
        }
        if (plungeFeedRateMmMin <= 0.0) return "Plunge feed rate must be strictly positive.";
        if (spindleRpm <= 0.0) return "Spindle speed must be strictly positive.";
        if (holeCenters.isEmpty()) return "At least one hole center must be defined.";

        var stock = setup.getStock();
        if (stock != null) {
            for (int i = 0; i < holeCenters.size(); i++) {
                CamPoint3D pt = holeCenters.get(i);
                if (pt.x() < stock.getMin().x() || pt.x() > stock.getMax().x()
                    || pt.y() < stock.getMin().y() || pt.y() > stock.getMax().y()) {
                    return "Hole center #" + (i + 1) + " (" + pt.x() + ", " + pt.y() + ") is outside stock boundaries.";
                }
            }
        }
        return null;
    }

    @Override
    public Toolpath generateToolpath(ManufacturingSetup setup) {
        String err = validate(setup);
        if (err != null) throw new IllegalStateException("Cannot generate toolpath: " + err);

        Toolpath path = new Toolpath(id);
        double safetyZ = setup.getSafetyPlane().getClearanceZ();
        CamPoint3D currentPos = new CamPoint3D(holeCenters.get(0).x(), holeCenters.get(0).y(), safetyZ);

        for (CamPoint3D hole : holeCenters) {
            double surfaceZ = hole.z();
            double approachZ = surfaceZ + localClearanceMm;
            double targetZ = surfaceZ - finalDepth;

            // 1. Rapid at safety plane to above target hole XY
            CamPoint3D aboveHoleSafety = new CamPoint3D(hole.x(), hole.y(), safetyZ);
            if (currentPos.distanceTo(aboveHoleSafety) > 1e-4) {
                path.addSegment(MotionType.RAPID, currentPos, aboveHoleSafety, 0.0, spindleRpm);
                currentPos = aboveHoleSafety;
            }

            // 2. Rapid down to approach R-plane
            CamPoint3D atApproach = new CamPoint3D(hole.x(), hole.y(), approachZ);
            path.addSegment(MotionType.RAPID, currentPos, atApproach, 0.0, spindleRpm);
            currentPos = atApproach;

            // 3. Progressive Pecks
            double currentZ = surfaceZ;
            while (currentZ > targetZ + 1e-6) {
                double nextZ = Math.max(targetZ, currentZ - peckDepth);

                // Cutting plunge to next peck depth
                CamPoint3D atPeckDepth = new CamPoint3D(hole.x(), hole.y(), nextZ);
                path.addSegment(MotionType.PLUNGE, currentPos, atPeckDepth, plungeFeedRateMmMin, spindleRpm);
                currentPos = atPeckDepth;

                if (Math.abs(nextZ - targetZ) < 1e-6) {
                    break; // Reached final programmed depth
                }

                // Intermediate Peck Retract
                if (retractStrategy == PeckRetractStrategy.FULL_RETRACT) {
                    // Full exit to approach R-plane
                    path.addSegment(MotionType.RETRACT, currentPos, atApproach, 0.0, spindleRpm);
                    currentPos = atApproach;

                    // Rapid back down to just above previous bottom
                    double reEnterZ = Math.min(approachZ, nextZ + approachReliefMm);
                    CamPoint3D reEnterPos = new CamPoint3D(hole.x(), hole.y(), reEnterZ);
                    path.addSegment(MotionType.RAPID, currentPos, reEnterPos, 0.0, spindleRpm);
                    currentPos = reEnterPos;
                } else {
                    // Chip break partial relief
                    double reliefZ = Math.min(approachZ, nextZ + chipBreakReliefMm);
                    CamPoint3D reliefPos = new CamPoint3D(hole.x(), hole.y(), reliefZ);
                    path.addSegment(MotionType.RETRACT, currentPos, reliefPos, 0.0, spindleRpm);
                    currentPos = reliefPos;
                }

                currentZ = nextZ;
            }

            // 4. Final Retract out of hole to approach level
            path.addSegment(MotionType.RETRACT, currentPos, atApproach, 0.0, spindleRpm);
            currentPos = atApproach;

            // 5. Retract to safety clearance for transition
            path.addSegment(MotionType.RETRACT, currentPos, aboveHoleSafety, 0.0, spindleRpm);
            currentPos = aboveHoleSafety;
        }

        return path;
    }
}
