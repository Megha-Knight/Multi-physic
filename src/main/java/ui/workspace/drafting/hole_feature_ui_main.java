package ui.workspace.drafting;

import java.util.UUID;

/**
 * hole_feature_ui_main.java
 * Parametric Hole Feature model for solid CAD bodies.
 * Supports Simple cylindrical holes with blind depth or through-all cut.
 */
public class hole_feature_ui_main {

    public enum HoleType {
        SIMPLE("Simple Hole"),
        COUNTERSINK("Countersink Hole"),
        COUNTERBORE("Counterbore Hole");

        private final String label;
        HoleType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final String id;
    private final String ownerShapeId;
    private final HoleType holeType;
    private final face_kind_ui_main faceKind;
    private double u;
    private double v;
    private double diameter;
    private double depth;
    private boolean throughAll;

    public hole_feature_ui_main(String id, String ownerShapeId, face_kind_ui_main faceKind,
                                double u, double v, double diameter, double depth, boolean throughAll) {
        this(id, ownerShapeId, HoleType.SIMPLE, faceKind, u, v, diameter, depth, throughAll);
    }

    public hole_feature_ui_main(String id, String ownerShapeId, HoleType holeType, face_kind_ui_main faceKind,
                                double u, double v, double diameter, double depth, boolean throughAll) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.holeType = (holeType != null) ? holeType : HoleType.SIMPLE;
        this.faceKind = (faceKind != null) ? faceKind : face_kind_ui_main.TOP;
        this.u = u;
        this.v = v;
        this.diameter = diameter;
        this.depth = depth;
        this.throughAll = throughAll;
    }

    public String getId()             { return id; }
    public String getOwnerShapeId()    { return ownerShapeId; }
    public HoleType getHoleType()      { return holeType; }
    public face_kind_ui_main getFaceKind() { return faceKind; }

    public double getU()              { return u; }
    public void setU(double u)        { this.u = u; }

    public double getV()              { return v; }
    public void setV(double v)        { this.v = v; }

    public double getDiameter()       { return diameter; }
    public void setDiameter(double d) { this.diameter = d; }

    public double getRadius()         { return Math.max(0.05, diameter * 0.5); }

    public double getDepth()          { return depth; }
    public void setDepth(double d)    { this.depth = d; }

    public boolean isThroughAll()     { return throughAll; }
    public void setThroughAll(boolean b) { this.throughAll = b; }

    public boolean isValid() {
        return diameter >= 0.1 && (throughAll || depth >= 0.1) && faceKind != null && faceKind.isPlanar();
    }

    @Override
    public String toString() {
        String depthStr = throughAll ? "Through-All" : String.format(java.util.Locale.US, "Depth: %.1f mm", depth);
        return String.format(java.util.Locale.US, "%s (Dia: %.1f mm, %s) on %s",
            holeType.getLabel(), diameter, depthStr, faceKind.getLabel());
    }
}
