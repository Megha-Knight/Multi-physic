package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_geometry_helper_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

/**
 * topology_geometry_helper_ui_main.java
 * Analytical geometry calculations for topology faces based strictly on shape parameters and transforms.
 */
public final class topology_geometry_helper_ui_main {

    private topology_geometry_helper_ui_main() {}

    public static Point3D getFaceOrigin(shape_item_ui_main shape, face_kind_ui_main kind) {
        if (shape == null || kind == null) return Point3D.ZERO;
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, kind);
        return shape_rotation_helper_ui_main.transformPoint(
            info.origin(), shape.getCenter(), shape.getRotationX(), shape.getRotationY(),
            shape.getWorldX(), shape.getWorldY(), shape.getWorldZ()
        );
    }

    public static Point3D getFaceNormal(shape_item_ui_main shape, face_kind_ui_main kind) {
        if (shape == null || kind == null) return new Point3D(0, -1, 0);
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, kind);
        return shape_rotation_helper_ui_main.transformNormal(info.normal(), shape.getRotationX(), shape.getRotationY()).normalize();
    }

    public static Point3D getFaceUAxis(shape_item_ui_main shape, face_kind_ui_main kind) {
        if (shape == null || kind == null) return new Point3D(1, 0, 0);
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, kind);
        return shape_rotation_helper_ui_main.transformNormal(info.uAxis(), shape.getRotationX(), shape.getRotationY()).normalize();
    }

    public static Point3D getFaceVAxis(shape_item_ui_main shape, face_kind_ui_main kind) {
        if (shape == null || kind == null) return new Point3D(0, 0, 1);
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, kind);
        return shape_rotation_helper_ui_main.transformNormal(info.vAxis(), shape.getRotationX(), shape.getRotationY()).normalize();
    }

    public static double[] getFaceBounds(shape_item_ui_main shape, face_kind_ui_main kind) {
        if (shape == null || kind == null) return new double[]{100.0, 100.0, 100.0};
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, kind);
        return new double[]{info.width(), info.height(), info.thickness()};
    }

    public static face_kind_ui_main getFaceKind(String faceId) {
        if (faceId == null || !faceId.contains(":")) return null;
        for (String p : faceId.split(":")) {
            try { return face_kind_ui_main.valueOf(p); } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public static face_reference_ui_main reconstructFaceReference(shape_item_ui_main shape, face_kind_ui_main kind, double u, double v) {
        return reconstructFaceReference(shape, kind, u, v, null);
    }

    public static face_reference_ui_main reconstructFaceReference(shape_item_ui_main shape, face_kind_ui_main kind, double u, double v, String targetFeatureId) {
        if (shape == null || kind == null) return null;
        if (targetFeatureId != null && shape.getExtrusions() != null) {
            for (var ext : shape.getExtrusions()) {
                if (targetFeatureId.equals(ext.getId())) {
                    var info = face_geometry_helper_ui_main.getExtrudeFaceInfo(shape, ext, kind);
                    Point3D c = shape.getCenter();
                    double rx = shape.getRotationX(), ry = shape.getRotationY(), wx = shape.getWorldX(), wy = shape.getWorldY(), wz = shape.getWorldZ();
                    Point3D origin = shape_rotation_helper_ui_main.transformPoint(info.origin(), c, rx, ry, wx, wy, wz);
                    Point3D normal = shape_rotation_helper_ui_main.transformNormal(info.normal(), rx, ry).normalize();
                    Point3D uAx = shape_rotation_helper_ui_main.transformNormal(info.uAxis(), rx, ry).normalize();
                    Point3D vAx = shape_rotation_helper_ui_main.transformNormal(info.vAxis(), rx, ry).normalize();
                    Point3D worldHit = origin.add(uAx.multiply(u)).add(vAx.multiply(v));
                    if (kind == face_kind_ui_main.EXTRUDE_LATERAL) {
                        Point3D radVec = worldHit.subtract(origin);
                        Point3D radNorm = radVec.subtract(vAx.multiply(radVec.dotProduct(vAx)));
                        if (radNorm.magnitude() > 1e-4) normal = radNorm.normalize();
                    }
                    return new face_reference_ui_main(
                        shape.getId(), targetFeatureId, kind, worldHit, normal, origin, uAx, vAx, u, v, info.width(), info.height()
                    );
                }
            }
        }
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, kind);
        Point3D origin = getFaceOrigin(shape, kind);
        Point3D normal = getFaceNormal(shape, kind);
        Point3D uAx = getFaceUAxis(shape, kind);
        Point3D vAx = getFaceVAxis(shape, kind);
        Point3D worldHit = origin.add(uAx.multiply(u)).add(vAx.multiply(v));
        return new face_reference_ui_main(
            shape.getId(), kind, worldHit, normal, origin, uAx, vAx, u, v, info.width(), info.height()
        );
    }

    public static boolean fitsWithinFace(shape_item_ui_main shape, face_kind_ui_main kind, double u, double v, double outerRadius) {
        if (shape == null || kind == null) return false;
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, kind);
        return face_geometry_helper_ui_main.fitsWithinFace(info, u, v, outerRadius);
    }
}
