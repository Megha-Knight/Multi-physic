package ui.workspace.drafting;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * shape_handles_ui_main.java
 * Utility for calculating and modifying control handles for 2D and 3D shapes.
 */
public final class shape_handles_ui_main {

    private shape_handles_ui_main() {}

    public static List<Point3D> getControlHandles(basic_shapes_ui_main type, Point3D p1, Point3D p2) {
        List<Point3D> list = new ArrayList<>();
        double r = p1.distance(p2);
        if (type == basic_shapes_ui_main.CIRCLE) {
            list.add(p1); // 0: center
            list.add(new Point3D(p1.getX() + r, 0, p1.getZ())); // 1: +X
            list.add(new Point3D(p1.getX() - r, 0, p1.getZ())); // 2: -X
            list.add(new Point3D(p1.getX(), 0, p1.getZ() + r)); // 3: +Z
            list.add(new Point3D(p1.getX(), 0, p1.getZ() - r)); // 4: -Z
        } else if (type == basic_shapes_ui_main.CYLINDER || type == basic_shapes_ui_main.CONE) {
            double h = Math.max(6.0, r * 2.0);
            list.add(p1);
            list.add(new Point3D(p2.getX(), 0, p2.getZ()));
            list.add(new Point3D(p1.getX(), -h, p1.getZ()));
        } else if (type == basic_shapes_ui_main.SPHERE) {
            list.add(p1);
            list.add(new Point3D(p2.getX(), -r, p2.getZ()));
            list.add(new Point3D(p1.getX(), -2.0 * r, p1.getZ()));
        } else if (type == basic_shapes_ui_main.SQUARE || type == basic_shapes_ui_main.CUBE) {
            double s = Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ()));
            double x1 = p1.getX() + (p2.getX() >= p1.getX() ? s : -s);
            double z1 = p1.getZ() + (p2.getZ() >= p1.getZ() ? s : -s);
            list.add(p1);
            list.add(new Point3D(x1, 0, p1.getZ()));
            list.add(new Point3D(x1, 0, z1));
            list.add(new Point3D(p1.getX(), 0, z1));
            if (type == basic_shapes_ui_main.CUBE)
                list.add(new Point3D((p1.getX() + x1) * 0.5, -s, (p1.getZ() + z1) * 0.5));
        } else if (type == basic_shapes_ui_main.RECTANGLE) {
            list.add(p1);
            list.add(new Point3D(p2.getX(), 0, p1.getZ()));
            list.add(p2);
            list.add(new Point3D(p1.getX(), 0, p2.getZ()));
        } else if (type == basic_shapes_ui_main.EQUILATERAL_TRIANGLE) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
            double s = Math.sqrt(dx * dx + dz * dz), h = s * Math.sqrt(3.0) / 2.0;
            list.add(p1);
            list.add(p2);
            list.add(new Point3D(p1.getX() + dx * 0.5 - (dz / s) * h, 0,
                p1.getZ() + dz * 0.5 + (dx / s) * h));
        } else if (type == basic_shapes_ui_main.RIGHT_TRIANGLE) {
            list.add(p1);
            list.add(new Point3D(p2.getX(), 0, p1.getZ()));
            list.add(new Point3D(p1.getX(), 0, p2.getZ()));
        }
        return list;
    }

    public static Point3D[] moveHandle(basic_shapes_ui_main type, Point3D p1, Point3D p2, int index, Point3D newPos) {
        Point3D newP1 = p1, newP2 = p2;
        if (type == basic_shapes_ui_main.CIRCLE) {
            if (index == 0) {
                double dx = newPos.getX() - p1.getX(), dz = newPos.getZ() - p1.getZ();
                newP1 = new Point3D(p1.getX() + dx, 0, p1.getZ() + dz);
                newP2 = new Point3D(p2.getX() + dx, 0, p2.getZ() + dz);
            } else {
                double newR = Math.max(1.0, p1.distance(new Point3D(newPos.getX(), 0, newPos.getZ())));
                newP2 = new Point3D(p1.getX() + newR, 0, p1.getZ());
            }
        } else if (type == basic_shapes_ui_main.SQUARE || type == basic_shapes_ui_main.CUBE) {
            double dirX = (p2.getX() >= p1.getX()) ? 1.0 : -1.0;
            double dirZ = (p2.getZ() >= p1.getZ()) ? 1.0 : -1.0;
            if (index == 0) {
                double s = Math.max(1.0, Math.max(Math.abs(p2.getX() - newPos.getX()), Math.abs(p2.getZ() - newPos.getZ())));
                newP1 = new Point3D(p2.getX() - dirX * s, 0, p2.getZ() - dirZ * s);
            } else if (index == 2 || (index == 4 && type == basic_shapes_ui_main.CUBE)) {
                double s = Math.max(1.0, Math.max(Math.abs(newPos.getX() - p1.getX()), Math.abs(newPos.getZ() - p1.getZ())));
                newP2 = new Point3D(p1.getX() + dirX * s, 0, p1.getZ() + dirZ * s);
            } else if (index == 1) {
                double s = Math.max(1.0, Math.abs(newPos.getX() - p1.getX()));
                newP2 = new Point3D(p1.getX() + dirX * s, 0, p2.getZ());
            } else if (index == 3) {
                double s = Math.max(1.0, Math.abs(newPos.getZ() - p1.getZ()));
                newP2 = new Point3D(p2.getX(), 0, p1.getZ() + dirZ * s);
            }
        } else if (type == basic_shapes_ui_main.RECTANGLE) {
            if (index == 0) newP1 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 2) newP2 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 1) { newP2 = new Point3D(newPos.getX(), 0, p2.getZ()); newP1 = new Point3D(p1.getX(), 0, newPos.getZ()); }
            else if (index == 3) { newP1 = new Point3D(newPos.getX(), 0, p1.getZ()); newP2 = new Point3D(p2.getX(), 0, newPos.getZ()); }
        } else if (type == basic_shapes_ui_main.EQUILATERAL_TRIANGLE) {
            if (index == 0) newP1 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 1) newP2 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 2) {
                double dist = Math.max(1.0, p1.distance(new Point3D(newPos.getX(), 0, newPos.getZ())));
                double newS = dist * 2.0 / Math.sqrt(3.0);
                Point3D curDir = (p2.distance(p1) > 1e-3) ? p2.subtract(p1).normalize() : new Point3D(1, 0, 0);
                newP2 = p1.add(curDir.multiply(newS));
            }
        } else if (type == basic_shapes_ui_main.RIGHT_TRIANGLE) {
            if (index == 0) newP1 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 1) newP2 = new Point3D(newPos.getX(), 0, p2.getZ());
            else if (index == 2) newP2 = new Point3D(p2.getX(), 0, newPos.getZ());
        } else if (type.is3D()) {
            if (index == 0) {
                double dx = newPos.getX() - p1.getX(), dz = newPos.getZ() - p1.getZ();
                newP1 = new Point3D(p1.getX() + dx, p1.getY(), p1.getZ() + dz);
                newP2 = new Point3D(p2.getX() + dx, p2.getY(), p2.getZ() + dz);
            } else {
                newP2 = new Point3D(newPos.getX(), 0, newPos.getZ());
            }
        }
        return new Point3D[]{newP1, newP2};
    }
}
