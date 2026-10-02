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
        if (shape == null || ext == null || !ext.isValid()) return new Group();

        hole_mesh_triangulator_ui_main.Frame f = getLocalFaceFrame(shape, ext.getFaceKind());
        if (f == null) return new Group();

        Point3D baseCenter = f.origin().add(f.u().multiply(ext.getU())).add(f.v().multiply(ext.getV()));
        double len = ext.getHeight();
        Point3D mid = baseCenter.add(f.n().multiply(len * 0.5));
        var mat = shape_geometry_3d_ui_main.createMaterial(false, isSelected);

        Node geoNode;
        if (ext.getProfileShape() == CutoutShape.CIRCLE) {
            Cylinder cyl = new Cylinder(ext.getRadius(), len);
            cyl.setMaterial(mat);
            cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
            orientCylinder(cyl, f.n());
            geoNode = cyl;
        } else {
            double w = ext.getDiameter();
            double d = (ext.getWidth2() > 0.1) ? ext.getWidth2() : w;
            geoNode = createOrientedBox(f.n(), mid, w, d, len, mat, ext.getFaceKind());
        }

        geoNode.setUserData(ext);
        return geoNode;
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
}
