package ui.workspace.drafting.extrude;

import java.util.Locale;
import java.util.UUID;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;

public class extrude_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private final face_kind_ui_main faceKind;
    private CutoutShape profileShape = CutoutShape.CIRCLE;
    private double u;
    private double v;
    private double diameter;
    private double width2 = 0.0;
    private double height;

    public extrude_feature_ui_main(String id, String ownerShapeId, String name, face_kind_ui_main faceKind,
                                  CutoutShape profileShape, double u, double v, double diameter, double width2, double height) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.name = name;
        this.faceKind = (faceKind != null) ? faceKind : face_kind_ui_main.TOP;
        this.profileShape = (profileShape != null) ? profileShape : CutoutShape.CIRCLE;
        this.u = u;
        this.v = v;
        this.diameter = diameter;
        this.width2 = width2;
        this.height = height;
    }

    public extrude_feature_ui_main(String id, String ownerShapeId, face_kind_ui_main faceKind,
                                  CutoutShape profileShape, double u, double v, double diameter, double width2, double height) {
        this(id, ownerShapeId, null, faceKind, profileShape, u, v, diameter, width2, height);
    }

    public String getId()                  { return id; }
    public String getOwnerShapeId()        { return ownerShapeId; }
    public String getName()                { return (name != null && !name.isBlank()) ? name : getDefaultName(); }
    public void setName(String name)       { this.name = name; }
    public face_kind_ui_main getFaceKind() { return faceKind; }
    public CutoutShape getProfileShape()   { return profileShape != null ? profileShape : CutoutShape.CIRCLE; }
    public void setProfileShape(CutoutShape s) { this.profileShape = s; }

    public double getU()                   { return u; }
    public void setU(double u)             { this.u = u; }
    public double getV()                   { return v; }
    public void setV(double v)             { this.v = v; }
    public double getDiameter()            { return diameter; }
    public void setDiameter(double d)      { this.diameter = d; }
    public double getRadius()              { return Math.max(0.05, diameter * 0.5); }
    public double getWidth2()              { return width2; }
    public void setWidth2(double w)        { this.width2 = w; }
    public double getHeight()              { return height; }
    public void setHeight(double h)        { this.height = h; }

    public String getDefaultName() {
        String shapeStr = (profileShape != null) ? profileShape.getLabel() : "Boss";
        return String.format(Locale.US, "%s Boss (%.1f x %.1f mm)", shapeStr, diameter, height);
    }

    public boolean isValid() {
        return diameter >= 0.1 && height >= 0.1 && faceKind != null && faceKind.isPlanar();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s [Size: %.1f mm, H: %.1f mm on %s]",
            getName(), diameter, height, faceKind.getLabel());
    }
}
