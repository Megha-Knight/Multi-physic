package ui.workspace.drafting;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;

/**
 * shape_rotation_helper_ui_main.java
 * High-precision CAD object center calculation and 360° rotation handling.
 */
public final class shape_rotation_helper_ui_main {

    private shape_rotation_helper_ui_main() {}

    public static Point3D computeCenter(basic_shapes_ui_main type, Point3D p1, Point3D p2) {
        if (p1 == null) return new Point3D(0, 0, 0);
        if (p2 == null) return p1;
        return switch (type) {
            case CUBE, SQUARE -> {
                double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
                double s = Math.max(Math.abs(dx), Math.abs(dz));
                double cx = p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5);
                double cz = p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5);
                yield new Point3D(cx, 0, cz);
            }
            case RECTANGLE -> new Point3D((p1.getX() + p2.getX()) * 0.5, 0, (p1.getZ() + p2.getZ()) * 0.5);
            case EQUILATERAL_TRIANGLE -> {
                double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
                double s = Math.sqrt(dx * dx + dz * dz), h = s * Math.sqrt(3.0) / 2.0;
                Point3D p3 = new Point3D(p1.getX() + dx * 0.5 - (dz / s) * h, 0, p1.getZ() + dz * 0.5 + (dx / s) * h);
                yield new Point3D((p1.getX() + p2.getX() + p3.getX()) / 3.0, 0, (p1.getZ() + p2.getZ() + p3.getZ()) / 3.0);
            }
            case RIGHT_TRIANGLE -> new Point3D((2.0 * p1.getX() + p2.getX()) / 3.0, 0, (2.0 * p1.getZ() + p2.getZ()) / 3.0);
            default -> p1; // CIRCLE, CYLINDER, SPHERE, CONE all have p1 as center
        };
    }

    public static double getExtentZ(shape_item_ui_main item) {
        if (item == null) return 20.0;
        Point3D p1 = item.getP1(), p2 = item.getP2();
        return switch (item.getType()) {
            case CUBE, SQUARE -> Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())) * 0.5;
            case RECTANGLE -> Math.abs(p2.getZ() - p1.getZ()) * 0.5;
            case CIRCLE, CYLINDER, SPHERE, CONE -> p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
            case EQUILATERAL_TRIANGLE, RIGHT_TRIANGLE -> p1.distance(p2) * 0.5;
            default -> 20.0;
        };
    }

    public static Point3D getRotationHandlePos(shape_item_ui_main item) {
        if (item == null) return new Point3D(0, 0, 0);
        Point3D c = item.getCenter();
        double extZ = Math.max(12.0, getExtentZ(item));
        // Placed on horizontal ground plane, 24 units in front (+Z) of object boundary
        return new Point3D(c.getX(), 0, c.getZ() + extZ + 24.0);
    }

    public static Point3D getRotationHandleWorldPos(shape_item_ui_main item) {
        if (item == null) return new Point3D(0, 0, 0);
        Point3D wc = item.getWorldCenter();
        double extZ = Math.max(12.0, getExtentZ(item));
        double rad = Math.toRadians(item.getRotationAngle());
        double off = extZ + 24.0;
        return new Point3D(wc.getX() + off * Math.sin(rad), 0, wc.getZ() + off * Math.cos(rad));
    }

    public static boolean isPointNearShape(shape_item_ui_main item, Point3D groundPt, double threshold) {
        if (item == null || groundPt == null) return false;
        Point3D wc = item.getWorldCenter(), c = item.getCenter();
        double dx = groundPt.getX() - wc.getX(), dz = groundPt.getZ() - wc.getZ();
        double rad = Math.toRadians(-item.getRotationAngle());
        double lx = c.getX() + (dx * Math.cos(rad) + dz * Math.sin(rad));
        double lz = c.getZ() + (-dx * Math.sin(rad) + dz * Math.cos(rad));
        basic_shapes_ui_main type = item.getType();
        Point3D p1 = item.getP1(), p2 = item.getP2();
        if (type == basic_shapes_ui_main.CIRCLE || type == basic_shapes_ui_main.CYLINDER
                || type == basic_shapes_ui_main.SPHERE || type == basic_shapes_ui_main.CONE) {
            double r = p1.distance(p2), d = Math.hypot(lx - p1.getX(), lz - p1.getZ());
            return Math.abs(d - r) <= threshold || d <= r;
        }
        if (type == basic_shapes_ui_main.CUBE || type == basic_shapes_ui_main.SQUARE) {
            double s = Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())) * 0.5;
            return Math.abs(lx - c.getX()) <= s + threshold && Math.abs(lz - c.getZ()) <= s + threshold;
        }
        if (type == basic_shapes_ui_main.RECTANGLE) {
            double hw = Math.abs(p2.getX() - p1.getX()) * 0.5, hd = Math.abs(p2.getZ() - p1.getZ()) * 0.5;
            return Math.abs(lx - c.getX()) <= hw + threshold && Math.abs(lz - c.getZ()) <= hd + threshold;
        }
        return Math.hypot(dx, dz) <= Math.max(20.0, getExtentZ(item)) + threshold;
    }

    public static double calculateAngle(Point3D centerWorld, Point3D mouseHit) {
        if (centerWorld == null || mouseHit == null) return 0.0;
        double dx = mouseHit.getX() - centerWorld.getX();
        double dz = mouseHit.getZ() - centerWorld.getZ();
        double deg = Math.toDegrees(Math.atan2(dx, dz));
        if (deg < 0) deg += 360.0;
        return normalize360(deg);
    }

    public static double normalize360(double deg) {
        double a = deg % 360.0;
        if (a < 0) a += 360.0;
        return Math.round(a * 10.0) / 10.0;
    }
}
