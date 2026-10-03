package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_geometry_helper_ui_main;

/**
 * face_reference_ui_main.java
 * Immutable reference model identifying a selected face on a 3D CAD body.
 * Distinguishes persistent identity (ownerShapeId, faceKind, faceId, localU, localV)
 * from recalculable runtime selection data (worldHitPoint, worldNormal, axes).
 */
public class face_reference_ui_main {

    private final String ownerShapeId;
    private final face_kind_ui_main faceKind;
    private final Point3D worldHitPoint;
    private final Point3D worldNormal;
    private final Point3D faceOrigin;
    private final Point3D uAxis;
    private final Point3D vAxis;
    private final double localHitU;
    private final double localHitV;
    private final double faceWidth;
    private final double faceHeight;

    public face_reference_ui_main(String ownerShapeId, face_kind_ui_main faceKind,
                                 Point3D worldHitPoint, Point3D worldNormal,
                                 Point3D faceOrigin, Point3D uAxis, Point3D vAxis,
                                 double localHitU, double localHitV,
                                 double faceWidth, double faceHeight) {
        this.ownerShapeId  = ownerShapeId;
        this.faceKind      = faceKind;
        this.worldHitPoint = (worldHitPoint != null) ? worldHitPoint : Point3D.ZERO;
        this.worldNormal   = (worldNormal != null && worldNormal.magnitude() > 1e-9) ? worldNormal.normalize() : new Point3D(0, -1, 0);
        this.faceOrigin    = (faceOrigin != null) ? faceOrigin : Point3D.ZERO;
        this.uAxis         = (uAxis != null && uAxis.magnitude() > 1e-9) ? uAxis.normalize() : new Point3D(1, 0, 0);
        this.vAxis         = (vAxis != null && vAxis.magnitude() > 1e-9) ? vAxis.normalize() : new Point3D(0, 0, 1);
        this.localHitU     = localHitU;
        this.localHitV     = localHitV;
        this.faceWidth     = faceWidth;
        this.faceHeight    = faceHeight;
    }

    public String getOwnerShapeId()        { return ownerShapeId; }
    public face_kind_ui_main getFaceKind() { return faceKind; }
    public String getFaceId()              { return (ownerShapeId != null && faceKind != null) ? ownerShapeId + ":" + faceKind.name() : ""; }
    public String getTopologyFaceId()      { return (ownerShapeId != null && faceKind != null) ? ownerShapeId + ":F:" + faceKind.name() : ""; }
    public Point3D getWorldHitPoint()      { return worldHitPoint; }
    public Point3D getWorldNormal()        { return worldNormal; }
    public Point3D getFaceOrigin()         { return faceOrigin; }
    public Point3D getUAxis()              { return uAxis; }
    public Point3D getVAxis()              { return vAxis; }
    public double getLocalHitU()           { return localHitU; }
    public double getLocalHitV()           { return localHitV; }
    public double getFaceWidth()           { return faceWidth; }
    public double getFaceHeight()          { return faceHeight; }
    public boolean isPlanar()              { return faceKind != null && faceKind.isPlanar(); }

    public Point3D toWorldPoint(double u, double v) {
        return faceOrigin.add(uAxis.multiply(u)).add(vAxis.multiply(v));
    }

    public Point3D toLocalCoords(Point3D worldPoint) {
        Point3D delta = worldPoint.subtract(faceOrigin);
        return new Point3D(delta.dotProduct(uAxis), delta.dotProduct(vAxis), delta.dotProduct(worldNormal));
    }

    public face_reference_ui_main refresh(shape_item_ui_main shape) {
        if (shape == null || faceKind == null) return this;
        return topology_geometry_helper_ui_main.reconstructFaceReference(shape, faceKind, localHitU, localHitV);
    }

    @Override
    public String toString() {
        return String.format("%s on [%s] @ (%.1f, %.1f) [faceId=%s]", faceKind.getLabel(), ownerShapeId, localHitU, localHitV, getFaceId());
    }
}
