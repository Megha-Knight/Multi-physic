package ui.workspace.shapes.primitives;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;
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
 * High-precision CAD geometry generator for 2D profile drafting.
 */
public final class shape_geometry_ui_main {

    private static final Point3D DEF_U = new Point3D(1, 0, 0);
    private static final Point3D DEF_V = new Point3D(0, 0, 1);
    private static final Point3D DEF_N = new Point3D(0, -1, 0);

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
            dot.setTranslateX(p1.getX()); dot.setTranslateY(p1.getY()); dot.setTranslateZ(p1.getZ());
            g.getChildren().add(dot);
        }
        return g;
    }

    public static Node createShapeOnPlane(basic_shapes_ui_main type, Point3D p1, Point3D p2,
                                         Point3D u, Point3D v, Point3D n, boolean isPreview, boolean isSelected) {
        Point3D uu = (u != null) ? u : DEF_U, vv = (v != null) ? v : DEF_V, nn = (n != null) ? n : DEF_N;
        return switch (type) {
            case CIRCLE -> plane_geometry_helper_ui_main.createCircle(p1, p2, uu, vv, nn, isPreview, isSelected);
            case SQUARE -> plane_geometry_helper_ui_main.createSquare(p1, p2, uu, vv, nn, isPreview, isSelected);
            case RECTANGLE -> plane_geometry_helper_ui_main.createRectangle(p1, p2, uu, vv, nn, isPreview, isSelected);
            case EQUILATERAL_TRIANGLE -> plane_geometry_helper_ui_main.createEquilateralTriangle(p1, p2, uu, vv, nn, isPreview, isSelected);
            case RIGHT_TRIANGLE -> plane_geometry_helper_ui_main.createRightTriangle(p1, p2, uu, vv, nn, isPreview, isSelected);
            default -> null;
        };
    }

    public static Node createCircle(Point3D center, Point3D current, boolean isPreview) { return createCircle(center, current, isPreview, false); }
    public static Node createCircle(Point3D center, Point3D current, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.CIRCLE, center, current, DEF_U, DEF_V, DEF_N, isPreview, isSelected);
    }
    public static Node createCircle(Point3D center, Point3D current, Point3D u, Point3D v, Point3D n, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.CIRCLE, center, current, u, v, n, isPreview, isSelected);
    }

    public static Node createSquare(Point3D start, Point3D current, boolean isPreview) { return createSquare(start, current, isPreview, false); }
    public static Node createSquare(Point3D start, Point3D current, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.SQUARE, start, current, DEF_U, DEF_V, DEF_N, isPreview, isSelected);
    }
    public static Node createSquare(Point3D start, Point3D current, Point3D u, Point3D v, Point3D n, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.SQUARE, start, current, u, v, n, isPreview, isSelected);
    }

    public static Node createRectangle(Point3D start, Point3D current, boolean isPreview) { return createRectangle(start, current, isPreview, false); }
    public static Node createRectangle(Point3D start, Point3D current, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.RECTANGLE, start, current, DEF_U, DEF_V, DEF_N, isPreview, isSelected);
    }
    public static Node createRectangle(Point3D start, Point3D current, Point3D u, Point3D v, Point3D n, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.RECTANGLE, start, current, u, v, n, isPreview, isSelected);
    }

    public static Node createEquilateralTriangle(Point3D start, Point3D current, boolean isPreview) { return createEquilateralTriangle(start, current, isPreview, false); }
    public static Node createEquilateralTriangle(Point3D start, Point3D current, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.EQUILATERAL_TRIANGLE, start, current, DEF_U, DEF_V, DEF_N, isPreview, isSelected);
    }
    public static Node createEquilateralTriangle(Point3D start, Point3D current, Point3D u, Point3D v, Point3D n, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.EQUILATERAL_TRIANGLE, start, current, u, v, n, isPreview, isSelected);
    }

    public static Node createRightTriangle(Point3D start, Point3D current, boolean isPreview) { return createRightTriangle(start, current, isPreview, false); }
    public static Node createRightTriangle(Point3D start, Point3D current, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.RIGHT_TRIANGLE, start, current, DEF_U, DEF_V, DEF_N, isPreview, isSelected);
    }
    public static Node createRightTriangle(Point3D start, Point3D current, Point3D u, Point3D v, Point3D n, boolean isPreview, boolean isSelected) {
        return createShapeOnPlane(basic_shapes_ui_main.RIGHT_TRIANGLE, start, current, u, v, n, isPreview, isSelected);
    }
}
