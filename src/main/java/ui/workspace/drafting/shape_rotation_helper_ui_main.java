package ui.workspace.drafting;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;

/**
 * shape_rotation_helper_ui_main.java
 * Helper for 360-degree CAD object rotation calculations and handle positioning.
 */
public final class shape_rotation_helper_ui_main {

    private shape_rotation_helper_ui_main() {}

    public static Point3D computeCenter(basic_shapes_ui_main type, Point3D p1, Point3D p2) {
        if (p1 == null) return new Point3D(0, 0, 0);
        if (p2 == null || type == basic_shapes_ui_main.CIRCLE || type == basic_shapes_ui_main.CYLINDER
                || type == basic_shapes_ui_main.SPHERE || type == basic_shapes_ui_main.CONE) {
            return p1;
        }
        return new Point3D((p1.getX() + p2.getX()) * 0.5, 0, (p1.getZ() + p2.getZ()) * 0.5);
    }

    public static Point3D getRotationHandlePos(shape_item_ui_main item) {
        if (item == null) return new Point3D(0, 0, 0);
        Point3D c = item.getCenter();
        Point3D p1 = item.getP1(), p2 = item.getP2();
        basic_shapes_ui_main type = item.getType();

        if (type.is3D()) {
            double h = 20.0;
            if (type == basic_shapes_ui_main.CUBE) {
                h = Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ()));
            } else if (type == basic_shapes_ui_main.CYLINDER || type == basic_shapes_ui_main.CONE) {
                double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
                h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
            } else if (type == basic_shapes_ui_main.SPHERE) {
                h = p1.distance(new Point3D(p2.getX(), 0, p2.getZ())) * 2.0;
            }
            return new Point3D(c.getX(), -h - 14.0, c.getZ());
        }

        double r = 25.0;
        if (type == basic_shapes_ui_main.CIRCLE) {
            r = p1.distance(p2) + 12.0;
        } else {
            double dz = Math.abs(p2.getZ() - p1.getZ());
            r = dz * 0.5 + 14.0;
        }
        return new Point3D(c.getX(), 0, c.getZ() - r);
    }

    public static double calculateAngle(Point3D centerWorld, Point3D mouseHit) {
        if (centerWorld == null || mouseHit == null) return 0.0;
        double dx = mouseHit.getX() - centerWorld.getX();
        double dz = mouseHit.getZ() - centerWorld.getZ();
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        if (deg < 0) deg += 360.0;
        return normalize360(deg);
    }

    public static double normalize360(double deg) {
        double a = deg % 360.0;
        if (a < 0) a += 360.0;
        return Math.round(a * 10.0) / 10.0;
    }
}
