package cam.tool;

import java.util.UUID;

/**
 * Validated cutting tool definition for CAM operations.
 */
public class CamTool {

    private final String id;
    private final String name;
    private final ToolType type;
    private final double diameter;
    private final double fluteLength;
    private final double overallLength;
    private final double tipAngleDegrees;
    private final int fluteCount;

    public CamTool(String id, String name, ToolType type, double diameter,
                   double fluteLength, double overallLength, double tipAngleDegrees, int fluteCount) {
        if (diameter <= 0.0) throw new IllegalArgumentException("Tool diameter must be positive.");
        if (fluteLength <= 0.0) throw new IllegalArgumentException("Flute length must be positive.");
        if (overallLength < fluteLength) throw new IllegalArgumentException("Overall length cannot be less than flute length.");
        if (tipAngleDegrees <= 0.0 || tipAngleDegrees > 180.0) {
            throw new IllegalArgumentException("Tip angle must be between 0 and 180 degrees.");
        }
        if (fluteCount < 1) throw new IllegalArgumentException("Flute count must be at least 1.");

        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : type.getDisplayName() + " " + diameter + "mm";
        this.type = (type != null) ? type : ToolType.SPOT_DRILL;
        this.diameter = diameter;
        this.fluteLength = fluteLength;
        this.overallLength = overallLength;
        this.tipAngleDegrees = tipAngleDegrees;
        this.fluteCount = fluteCount;
    }

    public static CamTool createSpotDrill(String name, double diameter, double tipAngleDegrees) {
        return new CamTool(null, name, ToolType.SPOT_DRILL, diameter, diameter * 2.5, diameter * 6.0, tipAngleDegrees, 2);
    }

    public String getId()               { return id; }
    public String getName()             { return name; }
    public ToolType getType()           { return type; }
    public double getDiameter()         { return diameter; }
    public double getRadius()           { return diameter * 0.5; }
    public double getFluteLength()      { return fluteLength; }
    public double getOverallLength()    { return overallLength; }
    public double getTipAngleDegrees()  { return tipAngleDegrees; }
    public int getFluteCount()          { return fluteCount; }

    public double calculateTipHeight(double cutDiameter) {
        double d = Math.min(cutDiameter, diameter);
        double halfAngleRad = Math.toRadians(tipAngleDegrees * 0.5);
        return (d * 0.5) / Math.tan(halfAngleRad);
    }
}
