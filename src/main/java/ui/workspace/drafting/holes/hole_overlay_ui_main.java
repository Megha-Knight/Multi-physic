package ui.workspace.drafting.holes;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;
import ui.workspace.shapes.holes.hole_profile_helper_ui_main;

public class hole_overlay_ui_main extends Group {

    private final PhongMaterial ringMat;
    private final PhongMaterial dotMat;

    public hole_overlay_ui_main() {
        ringMat = new PhongMaterial(Color.web("#00E5FF"));
        ringMat.setSpecularColor(Color.WHITE);
        dotMat = new PhongMaterial(Color.web("#F59E0B"));
        dotMat.setSpecularColor(Color.WHITE);
        setVisible(false);
        setMouseTransparent(true);
    }

    public void highlightHole(shape_item_ui_main shape, hole_feature_ui_main hole) {
        getChildren().clear();
        if (shape == null || hole == null) {
            setVisible(false);
            return;
        }
        buildProfileRing(shape, hole, false);
        if (hole.isThroughAll()) {
            buildProfileRing(shape, hole, true);
        }
        setVisible(true);
    }

    private void buildProfileRing(shape_item_ui_main shape, hole_feature_ui_main hole, boolean isExit) {
        hole_mesh_triangulator_ui_main.Frame localFrame = getLocalFrame(shape, hole.getFaceKind());
        if (localFrame == null) return;

        double u = hole.getU(), v = hole.getV();
        Point3D localOrigin = localFrame.origin();
        Point3D localN = localFrame.n(), localU = localFrame.u(), localV = localFrame.v();

        if (isExit) {
            face_kind_ui_main opp = getOpposite(hole.getFaceKind());
            if (opp == null) return;
            hole_mesh_triangulator_ui_main.Frame oppFrame = getLocalFrame(shape, opp);
            if (oppFrame == null) return;
            double th = getShapeThickness(shape, hole.getFaceKind());
            double[] exitUV = hole_mesh_triangulator_ui_main.computeExitUV(oppFrame, localFrame, u, v, th);
            localFrame = oppFrame;
            localOrigin = localFrame.origin();
            localN = localFrame.n();
            localU = localFrame.u();
            localV = localFrame.v();
            u = exitUV[0];
            v = exitUV[1];
        }

        Point3D c = shape.getCenter();
        double rx = shape.getRotationX(), ry = shape.getRotationY(), wx = shape.getWorldX(), wy = shape.getWorldY(), wz = shape.getWorldZ();
        Point3D worldOrigin = shape_rotation_helper_ui_main.transformPoint(localOrigin, c, rx, ry, wx, wy, wz);
        Point3D worldN = shape_rotation_helper_ui_main.transformNormal(localN, rx, ry).normalize();
        Point3D worldU = shape_rotation_helper_ui_main.transformNormal(localU, rx, ry).normalize();
        Point3D worldV = shape_rotation_helper_ui_main.transformNormal(localV, rx, ry).normalize();

        Point3D center = worldOrigin.add(worldU.multiply(u)).add(worldV.multiply(v)).add(worldN.multiply(0.2));
        Point3D[] pts = hole_profile_helper_ui_main.getProfilePoints(center, worldU, worldV, hole, 32);
        for (int i = 0; i < pts.length; i++) {
            getChildren().add(createLine(pts[i], pts[(i + 1) % pts.length], 0.7));
        }

        Sphere dot = new Sphere(1.5);
        dot.setMaterial(dotMat);
        dot.setTranslateX(center.getX()); dot.setTranslateY(center.getY()); dot.setTranslateZ(center.getZ());
        getChildren().add(dot);
    }

    public static hole_feature_ui_main findHoleAt(shape_item_ui_main shape, face_reference_ui_main face) {
        if (shape == null || face == null || !shape.hasHoles()) return null;
        for (hole_feature_ui_main h : shape.getHoles()) {
            if (h.getFaceKind() == face.getFaceKind()) {
                double dist = Math.hypot(face.getLocalHitU() - h.getU(), face.getLocalHitV() - h.getV());
                if (dist <= h.getOuterRadius() + 3.0) return h;
            }
            if (h.isThroughAll() && hole_mesh_triangulator_ui_main.isOpposite(h.getFaceKind(), face.getFaceKind())) {
                hole_mesh_triangulator_ui_main.Frame fEntry = getLocalFrame(shape, h.getFaceKind());
                hole_mesh_triangulator_ui_main.Frame fExit = getLocalFrame(shape, face.getFaceKind());
                if (fEntry != null && fExit != null) {
                    double th = getShapeThickness(shape, h.getFaceKind());
                    double[] exitUV = hole_mesh_triangulator_ui_main.computeExitUV(fExit, fEntry, h.getU(), h.getV(), th);
                    double dist = Math.hypot(face.getLocalHitU() - exitUV[0], face.getLocalHitV() - exitUV[1]);
                    if (dist <= h.getOuterRadius() + 3.0) return h;
                }
            }
        }
        return null;
    }

    private static hole_mesh_triangulator_ui_main.Frame getLocalFrame(shape_item_ui_main shape, face_kind_ui_main kind) {
        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        if (shape.getType() == basic_shapes_ui_main.CUBE) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), s = Math.max(Math.abs(dx), Math.abs(dz));
            Point3D c = new Point3D(p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5), p1.getY() - s * 0.5, p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5));
            return hole_mesh_triangulator_ui_main.getCubeFaceFrame(c, s, kind);
        } else if (shape.getType() == basic_shapes_ui_main.CUBOID) {
            double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
            double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            Point3D c = new Point3D((p1.getX() + p2.getX()) * 0.5, p1.getY() - h * 0.5, (p1.getZ() + p2.getZ()) * 0.5);
            return hole_mesh_triangulator_ui_main.getCuboidFaceFrame(c, w, h, d, kind);
        } else if (shape.getType() == basic_shapes_ui_main.CYLINDER) {
            double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ())), h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
            if (kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.TOP)
                return new hole_mesh_triangulator_ui_main.Frame(new Point3D(p1.getX(), -h, p1.getZ()), new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1));
            if (kind == face_kind_ui_main.BOTTOM_CAP || kind == face_kind_ui_main.BOTTOM)
                return new hole_mesh_triangulator_ui_main.Frame(new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1));
        }
        return null;
    }

    private static double getShapeThickness(shape_item_ui_main shape, face_kind_ui_main kind) {
        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        if (shape.getType() == basic_shapes_ui_main.CUBE) {
            return Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ()));
        } else if (shape.getType() == basic_shapes_ui_main.CUBOID) {
            double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
            double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            return hole_mesh_triangulator_ui_main.getFaceThickness(w, h, d, kind);
        } else if (shape.getType() == basic_shapes_ui_main.CYLINDER) {
            double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
            return Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        }
        return 50.0;
    }

    private static face_kind_ui_main getOpposite(face_kind_ui_main f) {
        return switch (f) {
            case TOP -> face_kind_ui_main.BOTTOM;
            case BOTTOM -> face_kind_ui_main.TOP;
            case TOP_CAP -> face_kind_ui_main.BOTTOM_CAP;
            case BOTTOM_CAP -> face_kind_ui_main.TOP_CAP;
            case FRONT -> face_kind_ui_main.BACK;
            case BACK -> face_kind_ui_main.FRONT;
            case RIGHT -> face_kind_ui_main.LEFT;
            case LEFT -> face_kind_ui_main.RIGHT;
            default -> null;
        };
    }

    private Cylinder createLine(Point3D p1, Point3D p2, double rad) {
        Point3D diff = p2.subtract(p1);
        double len = diff.magnitude();
        Cylinder cyl = new Cylinder(rad, len);
        cyl.setMaterial(ringMat);
        Point3D mid = p1.add(p2).multiply(0.5);
        cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axis = yAxis.crossProduct(diff);
        double ang = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, yAxis.dotProduct(diff) / (len + 1e-12)))));
        if (axis.magnitude() > 1e-6) cyl.getTransforms().add(new Rotate(ang, axis));
        return cyl;
    }
}
