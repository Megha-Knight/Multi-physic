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
 * Validated spot-drilling operation generating deterministic CNC toolpaths
 * with rapid positioning, controlled axial plunge, and safety retracts.
 */
public class SpotDrillingOperation implements CamOperation {

    private final String id;
    private String name;
    private final CamTool tool;
    private final List<CamPoint3D> holeCenters = new ArrayList<>();
    private double spotDepth;
    private double plungeFeedRateMmMin;
    private double spindleRpm;
    private double dwellSeconds;
    private double localClearanceMm;

    public SpotDrillingOperation(String id, String name, CamTool tool,
                                 double spotDepth, double plungeFeedRateMmMin, double spindleRpm) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "Spot Drilling";
        this.tool = tool;
        this.spotDepth = spotDepth;
        this.plungeFeedRateMmMin = plungeFeedRateMmMin;
        this.spindleRpm = spindleRpm;
        this.dwellSeconds = 0.5;
        this.localClearanceMm = 2.0;
    }

    public void addHoleCenter(CamPoint3D pt) { if (pt != null) holeCenters.add(pt); }
    public void addHoleCenter(double x, double y, double z) { addHoleCenter(new CamPoint3D(x, y, z)); }
    public List<CamPoint3D> getHoleCenters() { return Collections.unmodifiableList(holeCenters); }

    @Override public String getId()                  { return id; }
    @Override public String getName()                { return name; }
    @Override public CamOperationType getOperationType() { return CamOperationType.SPOT_DRILLING; }
    @Override public CamTool getTool()               { return tool; }

    public double getSpotDepth()                     { return spotDepth; }
    public void setSpotDepth(double d)               { this.spotDepth = d; }
    public double getPlungeFeedRate()                { return plungeFeedRateMmMin; }
    public void setPlungeFeedRate(double f)          { this.plungeFeedRateMmMin = f; }
    public double getSpindleRpm()                    { return spindleRpm; }
    public void setSpindleRpm(double rpm)            { this.spindleRpm = rpm; }
    public double getDwellSeconds()                  { return dwellSeconds; }
    public void setDwellSeconds(double sec)          { this.dwellSeconds = sec; }
    public double getLocalClearance()                { return localClearanceMm; }
    public void setLocalClearance(double clr)        { this.localClearanceMm = clr; }

    @Override
    public String validate(ManufacturingSetup setup) {
        if (setup == null) return "Manufacturing setup cannot be null.";
        if (tool == null) return "Tool cannot be null.";
        if (tool.getType() != ToolType.SPOT_DRILL && tool.getType() != ToolType.CHAMFER_MILL) {
            return "Spot drilling requires a SPOT_DRILL or CHAMFER_MILL cutter (got: " + tool.getType() + ").";
        }
        if (spotDepth <= 0.0) return "Spot depth must be strictly positive (got: " + spotDepth + ").";
        if (plungeFeedRateMmMin <= 0.0) return "Plunge feed rate must be strictly positive.";
        if (spindleRpm <= 0.0) return "Spindle speed must be strictly positive.";
        if (holeCenters.isEmpty()) return "At least one hole center must be defined.";
        if (spotDepth > tool.getFluteLength()) {
            return "Spot depth (" + spotDepth + " mm) exceeds tool flute length (" + tool.getFluteLength() + " mm).";
        }

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
            double targetZ = surfaceZ - spotDepth;

            // 1. Rapid at safety plane to above target hole XY
            CamPoint3D aboveHoleSafety = new CamPoint3D(hole.x(), hole.y(), safetyZ);
            if (currentPos.distanceTo(aboveHoleSafety) > 1e-4) {
                path.addSegment(MotionType.RAPID, currentPos, aboveHoleSafety, 0.0, spindleRpm);
                currentPos = aboveHoleSafety;
            }

            // 2. Rapid down to local approach clearance
            CamPoint3D atApproach = new CamPoint3D(hole.x(), hole.y(), approachZ);
            path.addSegment(MotionType.RAPID, currentPos, atApproach, 0.0, spindleRpm);
            currentPos = atApproach;

            // 3. Plunge cut to spot depth
            CamPoint3D atDepth = new CamPoint3D(hole.x(), hole.y(), targetZ);
            path.addSegment(MotionType.PLUNGE, currentPos, atDepth, plungeFeedRateMmMin, spindleRpm);
            currentPos = atDepth;

            // 4. Retract back to approach clearance
            path.addSegment(MotionType.RETRACT, currentPos, atApproach, 0.0, spindleRpm);
            currentPos = atApproach;

            // 5. Retract up to safety plane for inter-hole transit
            path.addSegment(MotionType.RETRACT, currentPos, aboveHoleSafety, 0.0, spindleRpm);
            currentPos = aboveHoleSafety;
        }

        return path;
    }
}
