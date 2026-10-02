package ui.workspace.drafting.holes;

import java.util.Locale;
import java.util.UUID;
import ui.workspace.drafting.faces.face_kind_ui_main;

/**
 * hole_feature_ui_main.java
 * Parametric Hole Feature model for solid CAD bodies.
 * Supports Simple, Countersink, and Counterbore holes.
 * Total depth convention: depth represents total cavity depth from entry face.
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

    public enum CutoutShape {
        CIRCLE("Circle"), SQUARE("Square"), RECTANGLE("Rectangle"),
        EQUILATERAL_TRIANGLE("Equilateral Triangle"), RIGHT_TRIANGLE("Right Triangle");
        private final String label;
        CutoutShape(String label) { this.label = label; }
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

    // Stage D: Countersink parameters (Ds: countersink diameter, A: included angle in degrees)
    private double csDiameter;
    private double csAngle = 90.0;

    // Stage D: Counterbore parameters (Db: counterbore diameter, Hb: counterbore depth)
    private double cbDiameter;
    private double cbDepth;

    // 2D Profile Cutout extension
    private CutoutShape cutoutShape = CutoutShape.CIRCLE;
    private double width2 = 0.0;

    public hole_feature_ui_main(String id, String ownerShapeId, face_kind_ui_main faceKind,
                                double u, double v, double diameter, double depth, boolean throughAll) {
        this(id, ownerShapeId, HoleType.SIMPLE, faceKind, u, v, diameter, depth, throughAll, 0, 90.0, 0, 0);
    }

    public hole_feature_ui_main(String id, String ownerShapeId, HoleType holeType, face_kind_ui_main faceKind,
                                double u, double v, double diameter, double depth, boolean throughAll) {
        this(id, ownerShapeId, holeType, faceKind, u, v, diameter, depth, throughAll, 0, 90.0, 0, 0);
    }

    public hole_feature_ui_main(String id, String ownerShapeId, HoleType holeType, face_kind_ui_main faceKind,
                                double u, double v, double diameter, double depth, boolean throughAll,
                                double csDiameter, double csAngle, double cbDiameter, double cbDepth) {
        this(id, ownerShapeId, holeType, faceKind, u, v, diameter, depth, throughAll, csDiameter, csAngle, cbDiameter, cbDepth, CutoutShape.CIRCLE, 0.0);
    }

    public hole_feature_ui_main(String id, String ownerShapeId, HoleType holeType, face_kind_ui_main faceKind,
                                double u, double v, double diameter, double depth, boolean throughAll,
                                double csDiameter, double csAngle, double cbDiameter, double cbDepth,
                                CutoutShape cutoutShape, double width2) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.holeType = (holeType != null) ? holeType : HoleType.SIMPLE;
        this.faceKind = (faceKind != null) ? faceKind : face_kind_ui_main.TOP;
        this.u = u;
        this.v = v;
        this.diameter = diameter;
        this.depth = depth;
        this.throughAll = throughAll;
        this.csDiameter = csDiameter;
        this.csAngle = csAngle;
        this.cbDiameter = cbDiameter;
        this.cbDepth = cbDepth;
        this.cutoutShape = (cutoutShape != null) ? cutoutShape : CutoutShape.CIRCLE;
        this.width2 = width2;
    }

    public String getId()                  { return id; }
    public String getOwnerShapeId()         { return ownerShapeId; }
    public HoleType getHoleType()           { return holeType; }
    public face_kind_ui_main getFaceKind()  { return faceKind; }

    public double getU()                   { return u; }
    public void setU(double u)             { this.u = u; }

    public double getV()                   { return v; }
    public void setV(double v)             { this.v = v; }

    public double getDiameter()            { return diameter; }
    public void setDiameter(double d)      { this.diameter = d; }
    public double getRadius()              { return Math.max(0.05, diameter * 0.5); }

    public double getDepth()               { return depth; }
    public void setDepth(double d)         { this.depth = d; }

    public boolean isThroughAll()          { return throughAll; }
    public void setThroughAll(boolean b)   { this.throughAll = b; }

    public double getCsDiameter()          { return csDiameter; }
    public void setCsDiameter(double d)    { this.csDiameter = d; }

    public double getCsAngle()             { return csAngle; }
    public void setCsAngle(double a)       { this.csAngle = a; }

    public double getCbDiameter()          { return cbDiameter; }
    public void setCbDiameter(double d)    { this.cbDiameter = d; }

    public double getCbDepth()             { return cbDepth; }
    public void setCbDepth(double d)       { this.cbDepth = d; }

    public double getConeDepth() {
        if (holeType != HoleType.COUNTERSINK || csAngle <= 0.0 || csAngle >= 180.0) return 0.0;
        double deltaR = (csDiameter - diameter) * 0.5;
        if (deltaR <= 0.0) return 0.0;
        return deltaR / Math.tan(Math.toRadians(csAngle * 0.5));
    }

    public double getOuterRadius() {
        return switch (holeType) {
            case COUNTERSINK -> Math.max(0.05, csDiameter * 0.5);
            case COUNTERBORE -> Math.max(0.05, cbDiameter * 0.5);
            default -> getRadius();
        };
    }

    public double getOuterDiameter() {
        return getOuterRadius() * 2.0;
    }

    public boolean isValid() {
        if (diameter < 0.1 || faceKind == null || !faceKind.isPlanar()) return false;
        return switch (holeType) {
            case SIMPLE -> (throughAll || depth >= 0.1);
            case COUNTERSINK -> (csDiameter > diameter + 1e-4) && (csAngle > 0.1 && csAngle < 179.9)
                    && (getConeDepth() > 1e-4) && (throughAll || depth > getConeDepth() + 1e-4);
            case COUNTERBORE -> (cbDiameter > diameter + 1e-4) && (cbDepth >= 0.1)
                    && (throughAll || depth > cbDepth + 1e-4);
        };
    }

    public boolean fitsWithinFace(double faceWidth, double faceHeight) {
        if (!isValid() || faceWidth <= 0 || faceHeight <= 0) return false;
        double r = getOuterRadius();
        if (getOuterDiameter() >= Math.min(faceWidth, faceHeight)) return false;
        return (Math.abs(u) + r <= faceWidth * 0.5 + 1e-4) && (Math.abs(v) + r <= faceHeight * 0.5 + 1e-4);
    }

    public boolean fitsWithinCylinderCap(double capRadius) {
        if (!isValid() || capRadius <= 0) return false;
        double r = getOuterRadius();
        return (Math.hypot(u, v) + r < capRadius * 0.99);
    }

    public CutoutShape getCutoutShape()         { return cutoutShape != null ? cutoutShape : CutoutShape.CIRCLE; }
    public void setCutoutShape(CutoutShape s)   { this.cutoutShape = s; }
    public double getWidth2()                  { return width2; }
    public void setWidth2(double w)            { this.width2 = w; }

    @Override
    public String toString() {
        String depthStr = throughAll ? "Through-All" : String.format(Locale.US, "Depth: %.1f mm", depth);
        if (cutoutShape != null && cutoutShape != CutoutShape.CIRCLE) {
            return String.format(Locale.US, "%s Cutout (Size: %.1f mm, %s) on %s",
                cutoutShape.getLabel(), diameter, depthStr, faceKind.getLabel());
        }
        return switch (holeType) {
            case COUNTERSINK -> String.format(Locale.US, "Countersink (D: %.1f mm, Ds: %.1f mm, %.1f deg, %s) on %s",
                diameter, csDiameter, csAngle, depthStr, faceKind.getLabel());
            case COUNTERBORE -> String.format(Locale.US, "Counterbore (D: %.1f mm, Db: %.1f mm, Hb: %.1f mm, %s) on %s",
                diameter, cbDiameter, cbDepth, depthStr, faceKind.getLabel());
            default -> String.format(Locale.US, "Simple Hole (Dia: %.1f mm, %s) on %s",
                diameter, depthStr, faceKind.getLabel());
        };
    }
}
