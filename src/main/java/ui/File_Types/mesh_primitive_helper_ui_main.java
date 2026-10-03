package ui.File_Types;

import javafx.geometry.Point3D;
import ui.File_Types.mesh_exporter_ui_main.Tri;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.List;

/**
 * mesh_primitive_helper_ui_main.java
 * Geometry tessellation helpers for basic CAD shapes exported to STL/OBJ/STEP.
 */
public final class mesh_primitive_helper_ui_main {

    private mesh_primitive_helper_ui_main() {}

    public static void addQuad(List<Tri> list, Point3D a, Point3D b, Point3D c, Point3D d, Point3D n) {
        list.add(new Tri(a, b, c, n));
        list.add(new Tri(a, c, d, n));
    }

    public static void addBox(List<Tri> l, double x1, double x2, double y1, double y2, double z1, double z2) {
        addQuad(l, new Point3D(x1, y2, z1), new Point3D(x2, y2, z1), new Point3D(x2, y2, z2), new Point3D(x1, y2, z2), new Point3D(0, 1, 0));
        addQuad(l, new Point3D(x1, y1, z2), new Point3D(x2, y1, z2), new Point3D(x2, y1, z1), new Point3D(x1, y1, z1), new Point3D(0, -1, 0));
        addQuad(l, new Point3D(x1, y1, z2), new Point3D(x2, y1, z2), new Point3D(x2, y2, z2), new Point3D(x1, y2, z2), new Point3D(0, 0, 1));
        addQuad(l, new Point3D(x2, y1, z1), new Point3D(x1, y1, z1), new Point3D(x1, y2, z1), new Point3D(x2, y2, z1), new Point3D(0, 0, -1));
        addQuad(l, new Point3D(x2, y1, z2), new Point3D(x2, y1, z1), new Point3D(x2, y2, z1), new Point3D(x2, y2, z2), new Point3D(1, 0, 0));
        addQuad(l, new Point3D(x1, y1, z1), new Point3D(x1, y1, z2), new Point3D(x1, y2, z2), new Point3D(x1, y2, z1), new Point3D(-1, 0, 0));
    }

    public static void addCyl(List<Tri> l, double cx, double cz, double r, double h) {
        int n = 32;
        for (int i = 0; i < n; i++) {
            double a1 = i * 2 * Math.PI / n, a2 = (i + 1) * 2 * Math.PI / n;
            double x1 = cx + r * Math.cos(a1), z1 = cz + r * Math.sin(a1);
            double x2 = cx + r * Math.cos(a2), z2 = cz + r * Math.sin(a2);
            l.add(new Tri(new Point3D(cx, h, cz), new Point3D(x1, h, z1), new Point3D(x2, h, z2), new Point3D(0, 1, 0)));
            l.add(new Tri(new Point3D(cx, 0, cz), new Point3D(x2, 0, z2), new Point3D(x1, 0, z1), new Point3D(0, -1, 0)));
            Point3D norm = new Point3D((Math.cos(a1) + Math.cos(a2)) * 0.5, 0, (Math.sin(a1) + Math.sin(a2)) * 0.5);
            addQuad(l, new Point3D(x1, 0, z1), new Point3D(x2, 0, z2), new Point3D(x2, h, z2), new Point3D(x1, h, z1), norm);
        }
    }

    public static void addCone(List<Tri> l, double cx, double cz, double r, double h) {
        int n = 32; Point3D apex = new Point3D(cx, h, cz);
        for (int i = 0; i < n; i++) {
            double a1 = i * 2 * Math.PI / n, a2 = (i + 1) * 2 * Math.PI / n;
            Point3D b1 = new Point3D(cx + r * Math.cos(a1), 0, cz + r * Math.sin(a1));
            Point3D b2 = new Point3D(cx + r * Math.cos(a2), 0, cz + r * Math.sin(a2));
            l.add(new Tri(new Point3D(cx, 0, cz), b2, b1, new Point3D(0, -1, 0)));
            l.add(new Tri(b1, b2, apex, b2.subtract(b1).crossProduct(apex.subtract(b1)).normalize()));
        }
    }

    public static void addSphere(List<Tri> l, double cx, double cz, double r) {
        int st = 16, sl = 24;
        for (int i = 0; i < st; i++) {
            double lt1 = Math.PI * (-0.5 + (double) i / st), lt2 = Math.PI * (-0.5 + (double) (i + 1) / st);
            double y1 = r * (1.0 + Math.sin(lt1)), y2 = r * (1.0 + Math.sin(lt2));
            double r1 = r * Math.cos(lt1), r2 = r * Math.cos(lt2);
            for (int j = 0; j < sl; j++) {
                double ln1 = 2 * Math.PI * j / sl, ln2 = 2 * Math.PI * (j + 1) / sl;
                Point3D a = new Point3D(cx + r1 * Math.cos(ln1), y1, cz + r1 * Math.sin(ln1));
                Point3D b = new Point3D(cx + r1 * Math.cos(ln2), y1, cz + r1 * Math.sin(ln2));
                Point3D c = new Point3D(cx + r2 * Math.cos(ln2), y2, cz + r2 * Math.sin(ln2));
                Point3D d = new Point3D(cx + r2 * Math.cos(ln1), y2, cz + r2 * Math.sin(ln1));
                addQuad(l, a, b, c, d, a.subtract(new Point3D(cx, r, cz)).normalize());
            }
        }
    }

    public static void addTri2D(List<Tri> l, Point3D p1, Point3D p2, basic_shapes_ui_main type) {
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), s = Math.sqrt(dx * dx + dz * dz);
        Point3D p3 = (type == basic_shapes_ui_main.EQUILATERAL_TRIANGLE && s > 1e-4)
            ? new Point3D(p1.getX() + dx * 0.5 - (dz / s) * s * Math.sqrt(3.0) / 2.0, 0, p1.getZ() + dz * 0.5 + (dx / s) * s * Math.sqrt(3.0) / 2.0)
            : new Point3D(p1.getX(), 0, p2.getZ());
        l.add(new Tri(new Point3D(p1.getX(), 0.5, p1.getZ()), new Point3D(p2.getX(), 0.5, p2.getZ()), new Point3D(p3.getX(), 0.5, p3.getZ()), new Point3D(0, 1, 0)));
        l.add(new Tri(new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(p3.getX(), 0, p3.getZ()), new Point3D(p2.getX(), 0, p2.getZ()), new Point3D(0, -1, 0)));
    }
}
