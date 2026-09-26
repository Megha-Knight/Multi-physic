package ui.workspace.document;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * mesh_exporter_ui_main.java
 * High-fidelity STL (Stereolithography) and OBJ (Wavefront) 3D mesh exporter for Astra CAD models.
 */
public final class mesh_exporter_ui_main {

    public record Tri(Point3D a, Point3D b, Point3D c, Point3D n) {}

    private mesh_exporter_ui_main() {}

    public static boolean exportToStl(File file, List<shape_item_ui_main> shapes) {
        if (file == null) return false;
        List<Tri> triangles = generateAllTriangles(shapes);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("solid AstraModel");
            for (Tri t : triangles) {
                pw.printf(Locale.US, "  facet normal %.6f %.6f %.6f%n", t.n.getX(), t.n.getY(), t.n.getZ());
                pw.println("    outer loop");
                pw.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", t.a.getX(), t.a.getY(), t.a.getZ());
                pw.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", t.b.getX(), t.b.getY(), t.b.getZ());
                pw.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", t.c.getX(), t.c.getY(), t.c.getZ());
                pw.println("    endloop");
                pw.println("  endfacet");
            }
            pw.println("endsolid AstraModel");
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean exportToObj(File file, List<shape_item_ui_main> shapes) {
        if (file == null) return false;
        List<Tri> triangles = generateAllTriangles(shapes);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# Astra CAD Model (Wavefront OBJ)");
            pw.println("o AstraModel");
            int vIdx = 1;
            for (Tri t : triangles) {
                pw.printf(Locale.US, "v %.6f %.6f %.6f%n", t.a.getX(), t.a.getY(), t.a.getZ());
                pw.printf(Locale.US, "v %.6f %.6f %.6f%n", t.b.getX(), t.b.getY(), t.b.getZ());
                pw.printf(Locale.US, "v %.6f %.6f %.6f%n", t.c.getX(), t.c.getY(), t.c.getZ());
                pw.printf(Locale.US, "vn %.6f %.6f %.6f%n", t.n.getX(), t.n.getY(), t.n.getZ());
                pw.printf("f %d//%d %d//%d %d//%d%n", vIdx, vIdx / 3 + 1, vIdx + 1, vIdx / 3 + 1, vIdx + 2, vIdx / 3 + 1);
                vIdx += 3;
            }
            return true;
        } catch (Exception e) { return false; }
    }

    public static List<Tri> generateAllTriangles(List<shape_item_ui_main> shapes) {
        List<Tri> result = new ArrayList<>();
        if (shapes != null) for (shape_item_ui_main item : shapes) result.addAll(generateShapeTriangles(item));
        return result;
    }

    private static List<Tri> generateShapeTriangles(shape_item_ui_main item) {
        List<Tri> localTris = new ArrayList<>();
        basic_shapes_ui_main type = item.getType();
        Point3D p1 = item.getP1(), p2 = item.getP2();
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));

        switch (type) {
            case CUBE, SQUARE -> {
                double s = Math.max(Math.abs(dx), Math.abs(dz));
                double x1 = p1.getX(), x2 = p1.getX() + (dx >= 0 ? s : -s);
                double z1 = p1.getZ(), z2 = p1.getZ() + (dz >= 0 ? s : -s);
                double y1 = 0, y2 = (type == basic_shapes_ui_main.CUBE) ? s : 0.5;
                addBox(localTris, Math.min(x1, x2), Math.max(x1, x2), y1, y2, Math.min(z1, z2), Math.max(z1, z2));
            }
            case RECTANGLE -> {
                double minX = Math.min(p1.getX(), p2.getX()), maxX = Math.max(p1.getX(), p2.getX());
                double minZ = Math.min(p1.getZ(), p2.getZ()), maxZ = Math.max(p1.getZ(), p2.getZ());
                addBox(localTris, minX, maxX, 0, 0.5, minZ, maxZ);
            }
            case CYLINDER -> {
                double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
                addCylinder(localTris, p1.getX(), p1.getZ(), r, h);
            }
            case CONE -> {
                double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
                addCone(localTris, p1.getX(), p1.getZ(), r, h);
            }
            case SPHERE -> addSphere(localTris, p1.getX(), p1.getZ(), r);
            case CIRCLE -> addCylinder(localTris, p1.getX(), p1.getZ(), p1.distance(p2), 0.5);
            default -> addTriangle2D(localTris, p1, p2, type);
        }

        // Transform all vertices with item's centroid, yaw rotation, and world translation
        Point3D c = item.getCenter();
        double rot = item.getRotationAngle(), rad = Math.toRadians(rot);
        double wx = item.getType().is3D() ? item.getWorldX() : 0;
        double wy = item.getType().is3D() ? -item.getWorldY() : 0;
        double wz = item.getType().is3D() ? item.getWorldZ() : 0;

        List<Tri> transformed = new ArrayList<>();
        for (Tri t : localTris) {
            transformed.add(new Tri(
                xform(t.a, c, rad, wx, wy, wz),
                xform(t.b, c, rad, wx, wy, wz),
                xform(t.c, c, rad, wx, wy, wz),
                xformNormal(t.n, rad)
            ));
        }
        return transformed;
    }

    private static Point3D xform(Point3D p, Point3D c, double rad, double wx, double wy, double wz) {
        double dx = p.getX() - c.getX(), dz = p.getZ() - c.getZ();
        return new Point3D(c.getX() + dx * Math.cos(rad) + dz * Math.sin(rad) + wx, p.getY() + wy, c.getZ() - dx * Math.sin(rad) + dz * Math.cos(rad) + wz);
    }

    private static Point3D xformNormal(Point3D n, double rad) {
        return new Point3D(n.getX() * Math.cos(rad) + n.getZ() * Math.sin(rad), n.getY(), -n.getX() * Math.sin(rad) + n.getZ() * Math.cos(rad));
    }

    private static void addQuad(List<Tri> list, Point3D a, Point3D b, Point3D c, Point3D d, Point3D n) {
        list.add(new Tri(a, b, c, n)); list.add(new Tri(a, c, d, n));
    }

    private static void addBox(List<Tri> list, double x1, double x2, double y1, double y2, double z1, double z2) {
        addQuad(list, new Point3D(x1, y2, z1), new Point3D(x2, y2, z1), new Point3D(x2, y2, z2), new Point3D(x1, y2, z2), new Point3D(0, 1, 0));
        addQuad(list, new Point3D(x1, y1, z2), new Point3D(x2, y1, z2), new Point3D(x2, y1, z1), new Point3D(x1, y1, z1), new Point3D(0, -1, 0));
        addQuad(list, new Point3D(x1, y1, z2), new Point3D(x2, y1, z2), new Point3D(x2, y2, z2), new Point3D(x1, y2, z2), new Point3D(0, 0, 1));
        addQuad(list, new Point3D(x2, y1, z1), new Point3D(x1, y1, z1), new Point3D(x1, y2, z1), new Point3D(x2, y2, z1), new Point3D(0, 0, -1));
        addQuad(list, new Point3D(x2, y1, z2), new Point3D(x2, y1, z1), new Point3D(x2, y2, z1), new Point3D(x2, y2, z2), new Point3D(1, 0, 0));
        addQuad(list, new Point3D(x1, y1, z1), new Point3D(x1, y1, z2), new Point3D(x1, y2, z2), new Point3D(x1, y2, z1), new Point3D(-1, 0, 0));
    }

    private static void addCylinder(List<Tri> list, double cx, double cz, double r, double h) {
        int n = 32;
        for (int i = 0; i < n; i++) {
            double a1 = i * 2 * Math.PI / n, a2 = (i + 1) * 2 * Math.PI / n;
            double x1 = cx + r * Math.cos(a1), z1 = cz + r * Math.sin(a1);
            double x2 = cx + r * Math.cos(a2), z2 = cz + r * Math.sin(a2);
            list.add(new Tri(new Point3D(cx, h, cz), new Point3D(x1, h, z1), new Point3D(x2, h, z2), new Point3D(0, 1, 0)));
            list.add(new Tri(new Point3D(cx, 0, cz), new Point3D(x2, 0, z2), new Point3D(x1, 0, z1), new Point3D(0, -1, 0)));
            Point3D norm = new Point3D((Math.cos(a1) + Math.cos(a2)) * 0.5, 0, (Math.sin(a1) + Math.sin(a2)) * 0.5);
            addQuad(list, new Point3D(x1, 0, z1), new Point3D(x2, 0, z2), new Point3D(x2, h, z2), new Point3D(x1, h, z1), norm);
        }
    }

    private static void addCone(List<Tri> list, double cx, double cz, double r, double h) {
        int n = 32; Point3D apex = new Point3D(cx, h, cz);
        for (int i = 0; i < n; i++) {
            double a1 = i * 2 * Math.PI / n, a2 = (i + 1) * 2 * Math.PI / n;
            Point3D b1 = new Point3D(cx + r * Math.cos(a1), 0, cz + r * Math.sin(a1));
            Point3D b2 = new Point3D(cx + r * Math.cos(a2), 0, cz + r * Math.sin(a2));
            list.add(new Tri(new Point3D(cx, 0, cz), b2, b1, new Point3D(0, -1, 0)));
            Point3D norm = b2.subtract(b1).crossProduct(apex.subtract(b1)).normalize();
            list.add(new Tri(b1, b2, apex, norm));
        }
    }

    private static void addSphere(List<Tri> list, double cx, double cz, double r) {
        int stacks = 16, slices = 24;
        for (int i = 0; i < stacks; i++) {
            double lat1 = Math.PI * (-0.5 + (double) i / stacks), lat2 = Math.PI * (-0.5 + (double) (i + 1) / stacks);
            double y1 = r * (1.0 + Math.sin(lat1)), y2 = r * (1.0 + Math.sin(lat2));
            double r1 = r * Math.cos(lat1), r2 = r * Math.cos(lat2);
            for (int j = 0; j < slices; j++) {
                double lon1 = 2 * Math.PI * j / slices, lon2 = 2 * Math.PI * (j + 1) / slices;
                Point3D pA = new Point3D(cx + r1 * Math.cos(lon1), y1, cz + r1 * Math.sin(lon1));
                Point3D pB = new Point3D(cx + r1 * Math.cos(lon2), y1, cz + r1 * Math.sin(lon2));
                Point3D pC = new Point3D(cx + r2 * Math.cos(lon2), y2, cz + r2 * Math.sin(lon2));
                Point3D pD = new Point3D(cx + r2 * Math.cos(lon1), y2, cz + r2 * Math.sin(lon1));
                addQuad(list, pA, pB, pC, pD, pA.subtract(new Point3D(cx, r, cz)).normalize());
            }
        }
    }

    private static void addTriangle2D(List<Tri> list, Point3D p1, Point3D p2, basic_shapes_ui_main type) {
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
        Point3D p3;
        if (type == basic_shapes_ui_main.EQUILATERAL_TRIANGLE) {
            double s = Math.sqrt(dx * dx + dz * dz), h = s * Math.sqrt(3.0) / 2.0;
            p3 = new Point3D(p1.getX() + dx * 0.5 - (dz / s) * h, 0, p1.getZ() + dz * 0.5 + (dx / s) * h);
        } else {
            p3 = new Point3D(p1.getX(), 0, p2.getZ());
        }
        list.add(new Tri(new Point3D(p1.getX(), 0.5, p1.getZ()), new Point3D(p2.getX(), 0.5, p2.getZ()), new Point3D(p3.getX(), 0.5, p3.getZ()), new Point3D(0, 1, 0)));
        list.add(new Tri(new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(p3.getX(), 0, p3.getZ()), new Point3D(p2.getX(), 0, p2.getZ()), new Point3D(0, -1, 0)));
    }
}
