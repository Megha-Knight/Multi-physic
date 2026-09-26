package ui.workspace.shapes;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.framework_ui_main;

import java.util.List;

/**
 * shape_geometry_ui_main.java
 * High-precision CAD geometry generator for 2D profile drafting
 * on the horizontal XY ground plane (Z_cad = 0).
 */
public final class shape_geometry_ui_main {

    private shape_geometry_ui_main() {}

    public static PhongMaterial createMaterial(boolean isPreview) {
        return createMaterial(isPreview, false);
    }

    public static PhongMaterial createMaterial(boolean isPreview, boolean isSelected) {
        Color c = isPreview ? Color.web(framework_ui_main.DRAFT_PREVIEW_COLOR)
                : isSelected ? Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)
                : Color.web(framework_ui_main.OBJECT_UNSELECTED_EDGE_COLOR);
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.web(isSelected ? "#E0F2FE" : framework_ui_main.OBJECT_UNSELECTED_COLOR));
        return mat;
    }

    public static Node createSegment(Point3D p1, Point3D p2, PhongMaterial mat) {
        Point3D diff = p2.subtract(p1);
        double len = diff.magnitude();
        if (len < 1e-3) return new Group();

        Cylinder cyl = new Cylinder(framework_ui_main.DRAFT_LINE_RADIUS, len);
        cyl.setMaterial(mat);

        Point3D mid = p1.midpoint(p2);
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axis = yAxis.crossProduct(diff);
        double angle = Math.toDegrees(Math.acos(
            Math.max(-1.0, Math.min(1.0, yAxis.dotProduct(diff.normalize())))
        ));

        cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        if (axis.magnitude() > 1e-4) {
            cyl.getTransforms().add(new Rotate(angle, axis.normalize()));
        } else if (diff.getY() < 0) {
            cyl.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        }
        return cyl;
    }

    public static Node createPolyline(List<Point3D> points, boolean closed, PhongMaterial mat) {
        Group g = new Group();
        int count = points.size();
        for (int i = 0; i < (closed ? count : count - 1); i++) {
            Point3D p1 = points.get(i);
            Point3D p2 = points.get((i + 1) % count);
            g.getChildren().add(createSegment(p1, p2, mat));
            Sphere dot = new Sphere(framework_ui_main.DRAFT_LINE_RADIUS * 1.8);
            dot.setMaterial(mat);
            dot.setTranslateX(p1.getX());
            dot.setTranslateY(p1.getY());
            dot.setTranslateZ(p1.getZ());
            g.getChildren().add(dot);
        }
        return g;
    }

    public static Node createCircle(Point3D center, Point3D current, boolean isPreview) {
        return createCircle(center, current, isPreview, false);
    }

    public static Node createCircle(Point3D center, Point3D current, boolean isPreview, boolean isSelected) {
        Group g = new Group();
        PhongMaterial mat = createMaterial(isPreview, isSelected);
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2) return g;

        int sides = 120;
        double step = 2.0 * Math.PI / sides;
        for (int i = 0; i < sides; i++) {
            double a1 = i * step, a2 = (i + 1) * step;
            Point3D pt1 = new Point3D(center.getX() + r * Math.cos(a1), 0, center.getZ() + r * Math.sin(a1));
            Point3D pt2 = new Point3D(center.getX() + r * Math.cos(a2), 0, center.getZ() + r * Math.sin(a2));
            g.getChildren().add(createSegment(pt1, pt2, mat));
        }

        Sphere cDot = new Sphere(framework_ui_main.DRAFT_LINE_RADIUS * 2.0);
        cDot.setMaterial(mat);
        cDot.setTranslateX(center.getX()); cDot.setTranslateZ(center.getZ());
        g.getChildren().add(cDot);
        if (isPreview) g.getChildren().add(createSegment(center, current, mat));
        return g;
    }

    public static Node createSquare(Point3D start, Point3D current, boolean isPreview) {
        return createSquare(start, current, isPreview, false);
    }

    public static Node createSquare(Point3D start, Point3D current, boolean isPreview, boolean isSelected) {
        double dx = current.getX() - start.getX(), dz = current.getZ() - start.getZ();
        double s = Math.max(Math.abs(dx), Math.abs(dz));
        if (s < 0.2) return new Group();
        double x1 = start.getX() + (dx >= 0 ? s : -s), z1 = start.getZ() + (dz >= 0 ? s : -s);
        List<Point3D> pts = List.of(
            new Point3D(start.getX(), 0, start.getZ()), new Point3D(x1, 0, start.getZ()),
            new Point3D(x1, 0, z1), new Point3D(start.getX(), 0, z1)
        );
        return createPolyline(pts, true, createMaterial(isPreview, isSelected));
    }

    public static Node createRectangle(Point3D start, Point3D current, boolean isPreview) {
        return createRectangle(start, current, isPreview, false);
    }

    public static Node createRectangle(Point3D start, Point3D current, boolean isPreview, boolean isSelected) {
        if (start.distance(current) < 0.2) return new Group();
        List<Point3D> pts = List.of(
            new Point3D(start.getX(), 0, start.getZ()), new Point3D(current.getX(), 0, start.getZ()),
            new Point3D(current.getX(), 0, current.getZ()), new Point3D(start.getX(), 0, current.getZ())
        );
        return createPolyline(pts, true, createMaterial(isPreview, isSelected));
    }

    public static Node createEquilateralTriangle(Point3D start, Point3D current, boolean isPreview) {
        return createEquilateralTriangle(start, current, isPreview, false);
    }

    public static Node createEquilateralTriangle(Point3D start, Point3D current, boolean isPreview, boolean isSelected) {
        double dx = current.getX() - start.getX(), dz = current.getZ() - start.getZ();
        double s = Math.sqrt(dx * dx + dz * dz);
        if (s < 0.2) return new Group();

        double h = s * Math.sqrt(3.0) / 2.0;
        double ux = dx / s, uz = dz / s;
        Point3D apex = new Point3D(start.getX() + dx * 0.5 - uz * h, 0, start.getZ() + dz * 0.5 + ux * h);
        List<Point3D> pts = List.of(new Point3D(start.getX(), 0, start.getZ()), new Point3D(current.getX(), 0, current.getZ()), apex);
        return createPolyline(pts, true, createMaterial(isPreview, isSelected));
    }

    public static Node createRightTriangle(Point3D start, Point3D current, boolean isPreview) {
        return createRightTriangle(start, current, isPreview, false);
    }

    public static Node createRightTriangle(Point3D start, Point3D current, boolean isPreview, boolean isSelected) {
        if (start.distance(current) < 0.2) return new Group();
        Point3D p0 = new Point3D(start.getX(), 0, start.getZ());
        Point3D p1 = new Point3D(current.getX(), 0, start.getZ());
        Point3D p2 = new Point3D(start.getX(), 0, current.getZ());
        PhongMaterial mat = createMaterial(isPreview, isSelected);
        Group g = (Group) createPolyline(List.of(p0, p1, p2), true, mat);

        double signX = Math.signum(current.getX() - start.getX()), signZ = Math.signum(current.getZ() - start.getZ());
        if (signX != 0 && signZ != 0) {
            double sz = Math.min(3.0, Math.min(Math.abs(current.getX() - start.getX()), Math.abs(current.getZ() - start.getZ())) * 0.25);
            if (sz > 0.4) {
                Point3D m1 = new Point3D(start.getX() + signX * sz, 0, start.getZ());
                Point3D m2 = new Point3D(start.getX() + signX * sz, 0, start.getZ() + signZ * sz);
                Point3D m3 = new Point3D(start.getX(), 0, start.getZ() + signZ * sz);
                g.getChildren().addAll(createSegment(m1, m2, mat), createSegment(m2, m3, mat));
            }
        }
        return g;
    }
}
