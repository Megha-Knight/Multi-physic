package ui.workspace.drafting.gizmo;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

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
                yield new Point3D(cx, (type == basic_shapes_ui_main.CUBE) ? -s * 0.5 : 0, cz);
            }
            case RECTANGLE -> new Point3D((p1.getX() + p2.getX()) * 0.5, 0, (p1.getZ() + p2.getZ()) * 0.5);
            case CUBOID -> {
                double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
                double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
                yield new Point3D((p1.getX() + p2.getX()) * 0.5, p1.getY() - h * 0.5, (p1.getZ() + p2.getZ()) * 0.5);
            }
            case EQUILATERAL_TRIANGLE -> {
                double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
                double s = Math.sqrt(dx * dx + dz * dz), h = s * Math.sqrt(3.0) / 2.0;
                Point3D p3 = new Point3D(p1.getX() + dx * 0.5 - (dz / s) * h, 0, p1.getZ() + dz * 0.5 + (dx / s) * h);
                yield new Point3D((p1.getX() + p2.getX() + p3.getX()) / 3.0, 0, (p1.getZ() + p2.getZ() + p3.getZ()) / 3.0);
            }
            case RIGHT_TRIANGLE -> new Point3D((2.0 * p1.getX() + p2.getX()) / 3.0, 0, (2.0 * p1.getZ() + p2.getZ()) / 3.0);
            case CYLINDER, CONE -> {
                double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, p1.distance(new Point3D(p2.getX(), 0, p2.getZ())) * 2.0);
                yield new Point3D(p1.getX(), -h * 0.5, p1.getZ());
            }
            case SPHERE -> new Point3D(p1.getX(), -p1.distance(new Point3D(p2.getX(), 0, p2.getZ())), p1.getZ());
            default -> p1;
        };
    }

    public static double getExtentZ(shape_item_ui_main item) {
        if (item == null) return 20.0;
        Point3D p1 = item.getP1(), p2 = item.getP2();
        return switch (item.getType()) {
            case CUBE, SQUARE -> Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())) * 0.5;
            case RECTANGLE, CUBOID -> Math.abs(p2.getZ() - p1.getZ()) * 0.5;
            case CIRCLE, CYLINDER, SPHERE, CONE -> p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
            case EQUILATERAL_TRIANGLE, RIGHT_TRIANGLE -> p1.distance(p2) * 0.5;
            default -> 20.0;
        };
    }

    public static double getExtentY(shape_item_ui_main item) {
        if (item == null) return 15.0;
        Point3D p1 = item.getP1(), p2 = item.getP2();
        double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
        return switch (item.getType()) {
            case CUBE -> Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())) * 0.5;
            case CUBOID -> (Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())) * 0.5)) * 0.5;
            case CYLINDER, CONE -> (Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0)) * 0.5;
            case SPHERE -> r;
            default -> 12.0;
        };
    }

    public static Point3D getRotationHandlePos(shape_item_ui_main item) {
        if (item == null) return new Point3D(0, 0, 0);
        Point3D c = item.getCenter();
        double extZ = Math.max(12.0, getExtentZ(item));
        return new Point3D(c.getX(), c.getY(), c.getZ() + extZ + 24.0);
    }

    public static Point3D getRotationPitchHandlePos(shape_item_ui_main item) {
        if (item == null) return new Point3D(0, 0, 0);
        Point3D c = item.getCenter();
        double extY = Math.max(12.0, getExtentY(item));
        return new Point3D(c.getX(), c.getY() - extY - 24.0, c.getZ());
    }

    public static Point3D getRotationHandleWorldPos(shape_item_ui_main item) {
        if (item == null) return new Point3D(0, 0, 0);
        Point3D wc = item.getWorldCenter(), c = item.getCenter();
        Point3D rh = getRotationHandlePos(item);
        return transformPoint(rh, c, item.getRotationX(), item.getRotationY(), item.getWorldX(), item.getWorldY(), item.getWorldZ());
    }

    public static Point3D transformPoint(Point3D p, Point3D c, double rxDeg, double ryDeg, double wx, double wy, double wz) {
        double rx = Math.toRadians(rxDeg), ry = Math.toRadians(ryDeg);
        double dx = p.getX() - c.getX(), dy = p.getY() - c.getY(), dz = p.getZ() - c.getZ();
        double y1 = dy * Math.cos(rx) - dz * Math.sin(rx);
        double z1 = dy * Math.sin(rx) + dz * Math.cos(rx);
        double x2 = dx * Math.cos(ry) + z1 * Math.sin(ry);
        double z2 = -dx * Math.sin(ry) + z1 * Math.cos(ry);
        return new Point3D(c.getX() + x2 + wx, c.getY() + y1 + wy, c.getZ() + z2 + wz);
    }

    public static Point3D transformNormal(Point3D n, double rxDeg, double ryDeg) {
        double rx = Math.toRadians(rxDeg), ry = Math.toRadians(ryDeg);
        double ny1 = n.getY() * Math.cos(rx) - n.getZ() * Math.sin(rx);
        double nz1 = n.getY() * Math.sin(rx) + n.getZ() * Math.cos(rx);
        double nx2 = n.getX() * Math.cos(ry) + nz1 * Math.sin(ry);
        double nz2 = -n.getX() * Math.sin(ry) + nz1 * Math.cos(ry);
        return new Point3D(nx2, ny1, nz2);
    }

    public static boolean isPointNearShape(shape_item_ui_main item, Point3D groundPt, double threshold) {
        if (item == null || groundPt == null) return false;
        Point3D wc = item.getWorldCenter(), c = item.getCenter();
        double dx = groundPt.getX() - wc.getX(), dz = groundPt.getZ() - wc.getZ();
        double rad = Math.toRadians(-item.getRotationY());
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
        if (type == basic_shapes_ui_main.RECTANGLE || type == basic_shapes_ui_main.CUBOID) {
            double hw = Math.abs(p2.getX() - p1.getX()) * 0.5, hd = Math.abs(p2.getZ() - p1.getZ()) * 0.5;
            return Math.abs(lx - c.getX()) <= hw + threshold && Math.abs(lz - c.getZ()) <= hd + threshold;
        }
        return Math.hypot(dx, dz) <= Math.max(20.0, getExtentZ(item)) + threshold;
    }

    public static double calculateAngle(Point3D centerWorld, Point3D mouseHit) {
        if (centerWorld == null || mouseHit == null) return 0.0;
        double deg = Math.toDegrees(Math.atan2(mouseHit.getX() - centerWorld.getX(), mouseHit.getZ() - centerWorld.getZ()));
        return normalize360(deg < 0 ? deg + 360.0 : deg);
    }

    public static double normalize360(double deg) {
        double a = deg % 360.0;
        if (a < 0) a += 360.0;
        return Math.round(a * 10.0) / 10.0;
    }
}
