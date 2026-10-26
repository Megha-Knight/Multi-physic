package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.CullFace;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;

/** Builds conformal overlays for polygonal extrusion faces. */
public final class extrude_face_overlay_helper_ui_main {
    private extrude_face_overlay_helper_ui_main() {}

    public static void addPolygonFace(Group target, java.util.List<Point3D> polygon, Point3D normal,
                                     PhongMaterial fill, PhongMaterial line) {
        if (polygon == null || polygon.size() < 3) return;
        Point3D offset = normal.multiply(0.22);
        TriangleMesh mesh = new TriangleMesh();
        for (Point3D point : polygon) {
            Point3D p = point.add(offset);
            mesh.getPoints().addAll((float)p.getX(), (float)p.getY(), (float)p.getZ());
        }
        mesh.getTexCoords().addAll(0, 0, 1, 0, 0, 1);
        for (int i = 1; i < polygon.size() - 1; i++) mesh.getFaces().addAll(0, 0, i, 1, i + 1, 2);
        MeshView view = new MeshView(mesh); view.setMaterial(fill); view.setCullFace(CullFace.NONE); target.getChildren().add(view);
        addLoop(target, polygon.stream().map(p -> p.add(normal.multiply(0.25))).toArray(Point3D[]::new), line);
    }

    public static void addTriangleCap(Group target, Point3D center, Point3D u, Point3D v, Point3D n,
                                     double width, double depth, CutoutShape profile,
                                     PhongMaterial fill, PhongMaterial line) {
        Point3D[] p = triangle(center, u, v, width, depth, profile, n.multiply(0.22));
        TriangleMesh mesh = new TriangleMesh();
        for (Point3D q : p) mesh.getPoints().addAll((float)q.getX(), (float)q.getY(), (float)q.getZ());
        mesh.getTexCoords().addAll(0, 0, 1, 0, 0.5f, 1);
        mesh.getFaces().addAll(0, 0, 1, 1, 2, 2);
        MeshView view = new MeshView(mesh); view.setMaterial(fill); view.setCullFace(CullFace.NONE); target.getChildren().add(view);
        addLoop(target, p, line);
    }

    public static void addRectFace(Group target, Point3D center, Point3D u, Point3D v, Point3D n,
                                   double width, double height, PhongMaterial fill, PhongMaterial line) {
        target.getChildren().add(face_overlay_mesh_helper_ui_main.createRectSurface(center, u, v, n, width, height, fill));
        Point3D off = n.multiply(0.25), hu = u.multiply(width * 0.5), hv = v.multiply(height * 0.5);
        Point3D[] p = {center.add(hu).add(hv).add(off), center.subtract(hu).add(hv).add(off),
            center.subtract(hu).subtract(hv).add(off), center.add(hu).subtract(hv).add(off)};
        addLoop(target, p, line);
    }

    private static Point3D[] triangle(Point3D c, Point3D u, Point3D v, double w, double d,
                                      CutoutShape profile, Point3D off) {
        if (profile == CutoutShape.RIGHT_TRIANGLE) return new Point3D[]{
            c.add(u.multiply(-w * 0.5)).add(v.multiply(-d * 0.5)).add(off),
            c.add(u.multiply(w * 0.5)).add(v.multiply(-d * 0.5)).add(off),
            c.add(u.multiply(-w * 0.5)).add(v.multiply(d * 0.5)).add(off)};
        double h = w * Math.sqrt(3.0) * 0.5;
        return new Point3D[]{c.add(u.multiply(-w * 0.5)).add(v.multiply(-h / 3.0)).add(off),
            c.add(u.multiply(w * 0.5)).add(v.multiply(-h / 3.0)).add(off),
            c.add(v.multiply(2.0 * h / 3.0)).add(off)};
    }

    public static void buildCircularBoundary(Group target, Point3D center, Point3D u, Point3D v, double radius,
                                             PhongMaterial material) {
        int segments = 32; Point3D previous = null, first = null;
        for (int i = 0; i < segments; i++) {
            double angle = i * 2.0 * Math.PI / segments;
            Point3D point = center.add(u.multiply(radius * Math.cos(angle))).add(v.multiply(radius * Math.sin(angle)));
            if (i == 0) first = point;
            if (previous != null) target.getChildren().add(line(previous, point, material));
            previous = point;
        }
        if (previous != null && first != null) target.getChildren().add(line(previous, first, material));
    }

    public static void buildCurvedMarker(Group target, Point3D point, Point3D normal, PhongMaterial material) {
        javafx.scene.shape.Sphere sphere = new javafx.scene.shape.Sphere(3.0);
        sphere.setMaterial(material); sphere.setTranslateX(point.getX()); sphere.setTranslateY(point.getY());
        sphere.setTranslateZ(point.getZ()); target.getChildren().addAll(sphere, line(point, point.add(normal.multiply(15.0)), material));
    }

    private static void addLoop(Group target, Point3D[] points, PhongMaterial material) {
        for (int i = 0; i < points.length; i++) target.getChildren().add(line(points[i], points[(i + 1) % points.length], material));
    }

    private static Cylinder line(Point3D a, Point3D b, PhongMaterial material) {
        Point3D diff = b.subtract(a), y = new Point3D(0, 1, 0), axis = y.crossProduct(diff);
        Cylinder c = new Cylinder(0.8, Math.max(0.1, diff.magnitude())); c.setMaterial(material);
        Point3D mid = a.midpoint(b); c.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        if (axis.magnitude() > 1e-4) c.getTransforms().add(new Rotate(Math.toDegrees(Math.acos(
            Math.max(-1.0, Math.min(1.0, y.dotProduct(diff.normalize()))))), axis.normalize()));
        else if (diff.getY() < 0) c.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        return c;
    }
}
