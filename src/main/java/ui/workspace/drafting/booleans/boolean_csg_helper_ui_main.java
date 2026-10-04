package ui.workspace.drafting.booleans;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

/**
 * boolean_csg_helper_ui_main.java
 * Geometric primitives and spatial analysis for CAD Boolean CSG operations.
 */
public final class boolean_csg_helper_ui_main {

    private boolean_csg_helper_ui_main() {}

    public static double[] getBoxBounds(shape_item_ui_main s) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        double w, h, d, cx, cz;
        if (s.getType() == basic_shapes_ui_main.CUBE) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
            double sz = Math.max(Math.abs(dx), Math.abs(dz));
            w = sz; h = sz; d = sz;
            cx = p1.getX() + (dx >= 0 ? sz * 0.5 : -sz * 0.5);
            cz = p1.getZ() + (dz >= 0 ? sz * 0.5 : -sz * 0.5);
        } else {
            w = Math.max(0.1, Math.abs(p2.getX() - p1.getX()));
            d = Math.max(0.1, Math.abs(p2.getZ() - p1.getZ()));
            h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            cx = (p1.getX() + p2.getX()) * 0.5;
            cz = (p1.getZ() + p2.getZ()) * 0.5;
        }
        return new double[]{cx - w * 0.5, cx + w * 0.5, -h, 0.0, cz - d * 0.5, cz + d * 0.5};
    }

    public static double[] getCylinderBounds(shape_item_ui_main s) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        double r = Math.max(0.1, p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
        double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        return new double[]{p1.getX(), p1.getZ(), r, -h, 0.0};
    }

    public static boolean isBoxOverlapping(double[] b1, double tx1, double tz1, double[] b2, double tx2, double tz2) {
        double minX1 = b1[0] + tx1, maxX1 = b1[1] + tx1, minX2 = b2[0] + tx2, maxX2 = b2[1] + tx2;
        double minY1 = b1[2], maxY1 = b1[3], minY2 = b2[2], maxY2 = b2[3];
        double minZ1 = b1[4] + tz1, maxZ1 = b1[5] + tz1, minZ2 = b2[4] + tz2, maxZ2 = b2[5] + tz2;
        return (minX1 < maxX2 && maxX1 > minX2) && (minY1 < maxY2 && maxY1 > minY2) && (minZ1 < maxZ2 && maxZ1 > minZ2);
    }

    public static boolean isPointInBox(Point3D p, double[] b, double tx, double tz) {
        double px = p.getX(), py = p.getY(), pz = p.getZ();
        return px >= (b[0] + tx - 1e-4) && px <= (b[1] + tx + 1e-4)
            && py >= (b[2] - 1e-4) && py <= (b[3] + 1e-4)
            && pz >= (b[4] + tz - 1e-4) && pz <= (b[5] + tz + 1e-4);
    }

    public static boolean isPointInCylinder(Point3D p, double cx, double cz, double r, double minY, double maxY, double tx, double tz) {
        double px = p.getX() - (cx + tx), py = p.getY(), pz = p.getZ() - (cz + tz);
        double distSq = px * px + pz * pz;
        return distSq <= (r * r + 1e-4) && py >= (minY - 1e-4) && py <= (maxY + 1e-4);
    }

    public static boolean isPointInsideBody(Point3D pt, shape_item_ui_main s) {
        if (s == null) return false;
        if (s.getType() == basic_shapes_ui_main.CUBE || s.getType() == basic_shapes_ui_main.CUBOID) {
            double[] b = getBoxBounds(s);
            return isPointInBox(pt, b, s.getWorldX(), s.getWorldZ());
        } else if (s.getType() == basic_shapes_ui_main.CYLINDER) {
            double[] c = getCylinderBounds(s);
            return isPointInCylinder(pt, c[0], c[1], c[2], c[3], c[4], s.getWorldX(), s.getWorldZ());
        } else if (s.getType() == basic_shapes_ui_main.CONE) {
            double[] c = getCylinderBounds(s);
            double py = pt.getY();
            if (py < c[3] - 1e-4 || py > c[4] + 1e-4) return false;
            double h = Math.abs(c[3]);
            double ratio = (h > 1e-4) ? (1.0 - Math.abs(py) / h) : 0.0;
            double curR = c[2] * Math.max(0.0, ratio);
            double px = pt.getX() - (c[0] + s.getWorldX()), pz = pt.getZ() - (c[1] + s.getWorldZ());
            return (px * px + pz * pz) <= (curR * curR + 1e-4);
        }
        return false;
    }
}
