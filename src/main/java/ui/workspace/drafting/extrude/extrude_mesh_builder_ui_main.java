package ui.workspace.drafting.extrude;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

public final class extrude_mesh_builder_ui_main {

    private extrude_mesh_builder_ui_main() {}

    public static Node buildExtrudeNode(shape_item_ui_main shape, extrude_feature_ui_main ext, boolean isSelected) {
        return buildExtrudeNode(shape, ext, isSelected, false);
    }

    public static Node buildExtrudeNode(shape_item_ui_main shape, extrude_feature_ui_main ext, boolean isSelected, boolean isFocused) {
        if (shape == null || ext == null || !ext.isValid()) return new Group();

        hole_mesh_triangulator_ui_main.Frame f = getLocalFaceFrame(shape, ext.getFaceKind());
        if (f == null) return new Group();

        Point3D baseCenter = f.origin().add(f.u().multiply(ext.getU())).add(f.v().multiply(ext.getV()));
        double len = ext.getHeight();
        Point3D mid = baseCenter.add(f.n().multiply(len * 0.5));
        var mat = createExtrudeMaterial(isSelected, isFocused);

        Node geoNode = null;
        if (ext.getSketchId() != null) {
            var sk = shape.getSketch(ext.getSketchId());
            if (sk != null && sk.isValid()) {
                geoNode = ui.workspace.drafting.sketch.sketch_extrude_bridge_ui_main.buildExtrusion(shape, sk, ext.getHeight(), isSelected || isFocused);
            }
        }
        if (geoNode == null) {
            var effHoles = shape.getEffectiveHolesForFeature(ext.getId());
            if (effHoles != null && !effHoles.isEmpty()) {
                geoNode = extrude_hole_mesh_helper_ui_main.buildExtrudeWithHoles(f, ext, effHoles, mat);
            }
        }
        if (geoNode == null) {
            CutoutShape ps = ext.getProfileShape();
            if (ps == CutoutShape.CIRCLE) {
                Cylinder cyl = new Cylinder(ext.getRadius(), len);
                cyl.setMaterial(mat);
                cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
                orientCylinder(cyl, f.n());
                geoNode = cyl;
            } else if (ps == CutoutShape.EQUILATERAL_TRIANGLE || ps == CutoutShape.RIGHT_TRIANGLE) {
                geoNode = extrude_prism_mesh_helper_ui_main.buildPrismNode(f, ext, mat);
            } else {
                double w = ext.getDiameter();
                double d = (ext.getWidth2() > 0.1) ? ext.getWidth2() : w;
                geoNode = createOrientedBox(f.n(), mid, w, d, len, mat, ext.getFaceKind());
            }
        }

        if (geoNode != null) geoNode.setUserData(ext);
        return geoNode != null ? geoNode : new Group();
    }

    public static javafx.scene.paint.PhongMaterial createExtrudeMaterial(boolean isSelected, boolean isFocused) {
        if (isFocused) {
            var m = new javafx.scene.paint.PhongMaterial(javafx.scene.paint.Color.web("#F59E0B"));
            m.setSpecularColor(javafx.scene.paint.Color.web("#FFFBEB"));
            m.setSpecularPower(50.0);
            return m;
        }
        return shape_geometry_3d_ui_main.createMaterial(false, isSelected);
    }

    public static extrude_feature_ui_main findExtrudeFromNode(Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (n.getUserData() instanceof extrude_feature_ui_main ext) return ext;
        }
        return null;
    }

    private static Node createOrientedBox(Point3D n, Point3D mid, double w, double d, double len, javafx.scene.paint.PhongMaterial mat, face_kind_ui_main kind) {
        Box box;
        if (kind == face_kind_ui_main.TOP || kind == face_kind_ui_main.BOTTOM || kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.BOTTOM_CAP) {
            box = new Box(w, len, d);
        } else if (kind == face_kind_ui_main.FRONT || kind == face_kind_ui_main.BACK) {
            box = new Box(w, d, len);
        } else {
            box = new Box(len, d, w);
        }
        box.setMaterial(mat);
        box.setTranslateX(mid.getX());
        box.setTranslateY(mid.getY());
        box.setTranslateZ(mid.getZ());
        return box;
    }

    private static void orientCylinder(Cylinder cyl, Point3D n) {
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axis = yAxis.crossProduct(n);
        double dot = Math.max(-1.0, Math.min(1.0, yAxis.dotProduct(n)));
        if (dot < -0.999) {
            cyl.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        } else if (dot < 0.999 && axis.magnitude() > 1e-4) {
            double ang = Math.toDegrees(Math.acos(dot));
            cyl.getTransforms().add(new Rotate(ang, axis));
        }
    }

    public static hole_mesh_triangulator_ui_main.Frame getLocalFaceFrame(shape_item_ui_main shape, face_kind_ui_main kind) {
        if (shape == null || kind == null) return null;
        var info = ui.workspace.drafting.faces.face_geometry_helper_ui_main.getFaceInfo(shape, kind);
        return new hole_mesh_triangulator_ui_main.Frame(info.origin(), info.normal(), info.uAxis(), info.vAxis());
    }
}
