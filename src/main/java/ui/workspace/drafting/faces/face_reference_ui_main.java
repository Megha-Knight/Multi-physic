package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;

/**
 * face_reference_ui_main.java
 * Immutable reference model identifying a selected face on a 3D CAD body.
 * Carries world-space orientation, face origin, and orthonormal in-plane axes (U, V).
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
        this.worldHitPoint = worldHitPoint;
        this.worldNormal   = worldNormal.normalize();
        this.faceOrigin    = faceOrigin;
        this.uAxis         = uAxis.normalize();
        this.vAxis         = vAxis.normalize();
        this.localHitU     = localHitU;
        this.localHitV     = localHitV;
        this.faceWidth     = faceWidth;
        this.faceHeight    = faceHeight;
    }

    public String getOwnerShapeId()     { return ownerShapeId; }
    public face_kind_ui_main getFaceKind() { return faceKind; }
    public Point3D getWorldHitPoint()   { return worldHitPoint; }
    public Point3D getWorldNormal()     { return worldNormal; }
    public Point3D getFaceOrigin()      { return faceOrigin; }
    public Point3D getUAxis()           { return uAxis; }
    public Point3D getVAxis()           { return vAxis; }
    public double getLocalHitU()        { return localHitU; }
    public double getLocalHitV()        { return localHitV; }
    public double getFaceWidth()        { return faceWidth; }
    public double getFaceHeight()       { return faceHeight; }
    public boolean isPlanar()           { return faceKind.isPlanar(); }

    /**
     * Converts face-local 2D (u, v) offsets into a 3D world coordinate.
     */
    public Point3D toWorldPoint(double u, double v) {
        return faceOrigin.add(uAxis.multiply(u)).add(vAxis.multiply(v));
    }

    /**
     * Projects an arbitrary 3D world point onto the face plane, returning local (u, v).
     */
    public Point3D toLocalCoords(Point3D worldPoint) {
        Point3D delta = worldPoint.subtract(faceOrigin);
        return new Point3D(delta.dotProduct(uAxis), delta.dotProduct(vAxis), delta.dotProduct(worldNormal));
    }

    @Override
    public String toString() {
        return String.format("%s on [%s] @ (%.1f, %.1f)", faceKind.getLabel(), ownerShapeId, localHitU, localHitV);
    }
}
