package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_geometry_helper_ui_main;

public class sketch_coord_system_ui_main {

    private final sketch_plane_type_ui_main planeType;
    private final String ownerShapeId;
    private final face_kind_ui_main faceKind;
    private Point3D origin;
    private Point3D uAxis;
    private Point3D vAxis;
    private Point3D normal;

    public sketch_coord_system_ui_main(sketch_plane_type_ui_main planeType, String ownerShapeId, face_kind_ui_main faceKind) {
        this.planeType = (planeType != null) ? planeType : sketch_plane_type_ui_main.BASE_XZ;
        this.ownerShapeId = ownerShapeId;
        this.faceKind = faceKind;
        initBaseAxes();
    }

    public static sketch_coord_system_ui_main forFace(shape_item_ui_main shape, face_kind_ui_main faceKind) {
        sketch_coord_system_ui_main cs = new sketch_coord_system_ui_main(sketch_plane_type_ui_main.FACE, shape != null ? shape.getId() : null, faceKind);
        if (shape != null) cs.updateFromHost(shape);
        return cs;
    }

    public static sketch_coord_system_ui_main forFaceReference(face_reference_ui_main faceRef) {
        if (faceRef == null) return forBasePlane(sketch_plane_type_ui_main.BASE_XZ);
        sketch_coord_system_ui_main cs = new sketch_coord_system_ui_main(sketch_plane_type_ui_main.FACE, faceRef.getOwnerShapeId(), faceRef.getFaceKind());
        cs.origin = faceRef.getFaceOrigin();
        cs.uAxis = faceRef.getUAxis();
        cs.vAxis = faceRef.getVAxis();
        cs.normal = faceRef.getWorldNormal();
        return cs;
    }

    public static sketch_coord_system_ui_main forBasePlane(sketch_plane_type_ui_main type) {
        return new sketch_coord_system_ui_main(type != null ? type : sketch_plane_type_ui_main.BASE_XZ, null, null);
    }

    private void initBaseAxes() {
        switch (planeType) {
            case BASE_XY -> {
                origin = Point3D.ZERO;
                uAxis = new Point3D(1, 0, 0);
                vAxis = new Point3D(0, -1, 0); // -Y is up in Astra CAD world
                normal = new Point3D(0, 0, 1);
            }
            case BASE_YZ -> {
                origin = Point3D.ZERO;
                uAxis = new Point3D(0, 0, 1);
                vAxis = new Point3D(0, -1, 0);
                normal = new Point3D(1, 0, 0);
            }
            case BASE_XZ, FACE -> {
                origin = Point3D.ZERO;
                uAxis = new Point3D(1, 0, 0);
                vAxis = new Point3D(0, 0, 1);
                normal = new Point3D(0, -1, 0);
            }
        }
    }

    public void updateFromHost(shape_item_ui_main shape) {
        if (planeType != sketch_plane_type_ui_main.FACE || shape == null || faceKind == null) return;
        this.origin = topology_geometry_helper_ui_main.getFaceOrigin(shape, faceKind);
        this.normal = topology_geometry_helper_ui_main.getFaceNormal(shape, faceKind);
        this.uAxis = topology_geometry_helper_ui_main.getFaceUAxis(shape, faceKind);
        this.vAxis = topology_geometry_helper_ui_main.getFaceVAxis(shape, faceKind);
    }

    public Point3D toWorldPoint(double sketchX, double sketchY) {
        return origin.add(uAxis.multiply(sketchX)).add(vAxis.multiply(sketchY));
    }

    public Point3D toWorldPoint(sketch_point_2d_ui_main pt) {
        return (pt != null) ? toWorldPoint(pt.x(), pt.y()) : origin;
    }

    public sketch_point_2d_ui_main toSketchPoint(Point3D worldPoint) {
        if (worldPoint == null) return sketch_point_2d_ui_main.ZERO;
        Point3D delta = worldPoint.subtract(origin);
        return new sketch_point_2d_ui_main(delta.dotProduct(uAxis), delta.dotProduct(vAxis));
    }

    public Point3D projectWorldToPlane(Point3D worldPoint) {
        sketch_point_2d_ui_main sp = toSketchPoint(worldPoint);
        return toWorldPoint(sp);
    }

    public sketch_plane_type_ui_main getPlaneType() { return planeType; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public face_kind_ui_main getFaceKind() { return faceKind; }
    public String getFaceId() { return (ownerShapeId != null && faceKind != null) ? ownerShapeId + ":" + faceKind.name() : ""; }
    public Point3D getOrigin() { return origin; }
    public Point3D getUAxis() { return uAxis; }
    public Point3D getVAxis() { return vAxis; }
    public Point3D getNormal() { return normal; }
}
