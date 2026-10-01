package ui.File_Types;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
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
 * High-fidelity STL (Stereolithography) and Wavefront OBJ 3D mesh exporter.
 * Directly exports parametric hole boolean geometry and primitives.
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
                pw.printf(Locale.US, "    outer loop%n      vertex %.6f %.6f %.6f%n      vertex %.6f %.6f %.6f%n      vertex %.6f %.6f %.6f%n    endloop%n  endfacet%n",
                    t.a.getX(), t.a.getY(), t.a.getZ(), t.b.getX(), t.b.getY(), t.b.getZ(), t.c.getX(), t.c.getY(), t.c.getZ());
            }
            pw.println("endsolid AstraModel");
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean exportToObj(File file, List<shape_item_ui_main> shapes) {
        if (file == null) return false;
        List<Tri> triangles = generateAllTriangles(shapes);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# Astra CAD Wavefront OBJ Export\no AstraModel");
            int vIdx = 1;
            for (Tri t : triangles) {
                pw.printf(Locale.US, "v %.6f %.6f %.6f%n", t.a.getX(), t.a.getY(), t.a.getZ());
                pw.printf(Locale.US, "v %.6f %.6f %.6f%n", t.b.getX(), t.b.getY(), t.b.getZ());
                pw.printf(Locale.US, "v %.6f %.6f %.6f%n", t.c.getX(), t.c.getY(), t.c.getZ());
                pw.printf(Locale.US, "vn %.6f %.6f %.6f%n", t.n.getX(), t.n.getY(), t.n.getZ());
                int nIdx = vIdx / 3 + 1;
                pw.printf("f %d//%d %d//%d %d//%d%n", vIdx, nIdx, vIdx + 1, nIdx, vIdx + 2, nIdx);
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
        List<Tri> local = new ArrayList<>();
        if (item.hasHoles()) {
            List<Tri> meshTris = extractHoleMeshTriangles(item);
            if (!meshTris.isEmpty()) return transformTriangles(item, meshTris);
        }
        basic_shapes_ui_main type = item.getType();
        Point3D p1 = item.getP1(), p2 = item.getP2();
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));

        switch (type) {
            case CUBE, SQUARE -> {
                double s = Math.max(Math.abs(dx), Math.abs(dz));
                double x2 = p1.getX() + (dx >= 0 ? s : -s), z2 = p1.getZ() + (dz >= 0 ? s : -s);
                addBox(local, Math.min(p1.getX(), x2), Math.max(p1.getX(), x2), 0, (type == basic_shapes_ui_main.CUBE) ? s : 0.5, Math.min(p1.getZ(), z2), Math.max(p1.getZ(), z2));
            }
            case RECTANGLE -> addBox(local, Math.min(p1.getX(), p2.getX()), Math.max(p1.getX(), p2.getX()), 0, 0.5, Math.min(p1.getZ(), p2.getZ()), Math.max(p1.getZ(), p2.getZ()));
            case CUBOID -> {
                double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(Math.abs(dx), Math.abs(dz)) * 0.5);
                addBox(local, Math.min(p1.getX(), p2.getX()), Math.max(p1.getX(), p2.getX()), 0, h, Math.min(p1.getZ(), p2.getZ()), Math.max(p1.getZ(), p2.getZ()));
            }
            case CYLINDER -> addCyl(local, p1.getX(), p1.getZ(), r, Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0));
            case CONE -> addCone(local, p1.getX(), p1.getZ(), r, Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0));
            case SPHERE -> addSphere(local, p1.getX(), p1.getZ(), r);
            case CIRCLE -> addCyl(local, p1.getX(), p1.getZ(), p1.distance(p2), 0.5);
            default -> addTri2D(local, p1, p2, type);
        }
        return transformTriangles(item, local);
    }

    private static List<Tri> extractHoleMeshTriangles(shape_item_ui_main item) {
        List<Tri> list = new ArrayList<>();
        Node geo = (item.getType() == basic_shapes_ui_main.CUBE) ? ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCubeWithHoles(item.getP1(), item.getP2(), item.getHoles(), false, false)
            : (item.getType() == basic_shapes_ui_main.CUBOID) ? ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCuboidWithHoles(item.getP1(), item.getP2(), item.getHoles(), false, false)
            : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCylinderWithHoles(item.getP1(), item.getP2(), item.getHoles(), false, false);
        List<MeshView> mvs = new ArrayList<>();
        if (geo instanceof MeshView mv) mvs.add(mv);
        else if (geo instanceof Group g) for (Node child : g.getChildren()) if (child instanceof MeshView mv) mvs.add(mv);
        for (MeshView mv : mvs) {
            if (mv.getMesh() instanceof TriangleMesh tm) {
                float[] pts = tm.getPoints().toArray(null); int[] faces = tm.getFaces().toArray(null);
                if (pts != null && faces != null) {
                    for (int i = 0; i < faces.length; i += 6) {
                        int i0 = faces[i] * 3, i1 = faces[i + 2] * 3, i2 = faces[i + 4] * 3;
                        Point3D a = new Point3D(pts[i0], -pts[i0 + 1], pts[i0 + 2]);
                        Point3D b = new Point3D(pts[i1], -pts[i1 + 1], pts[i1 + 2]);
                        Point3D c = new Point3D(pts[i2], -pts[i2 + 1], pts[i2 + 2]);
                        Point3D n = b.subtract(a).crossProduct(c.subtract(a));
                        list.add(new Tri(a, b, c, n.magnitude() > 1e-5 ? n.normalize() : new Point3D(0, 1, 0)));
                    }
                }
            }
        }
        return list;
    }

    private static List<Tri> transformTriangles(shape_item_ui_main item, List<Tri> local) {
        Point3D c = item.getCenter();
        double rx = item.getRotationX(), ry = item.getRotationY(), wx = item.getWorldX(), wy = -item.getWorldY(), wz = item.getWorldZ();
        List<Tri> res = new ArrayList<>();
        for (Tri t : local) {
            res.add(new Tri(
                ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformPoint(t.a, c, rx, ry, wx, wy, wz),
                ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformPoint(t.b, c, rx, ry, wx, wy, wz),
                ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformPoint(t.c, c, rx, ry, wx, wy, wz),
                ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformNormal(t.n, rx, ry)
            ));
        }
        return res;
    }

    private static void addQuad(List<Tri> list, Point3D a, Point3D b, Point3D c, Point3D d, Point3D n) {
        list.add(new Tri(a, b, c, n)); list.add(new Tri(a, c, d, n));
    }

    private static void addBox(List<Tri> l, double x1, double x2, double y1, double y2, double z1, double z2) {
        addQuad(l, new Point3D(x1, y2, z1), new Point3D(x2, y2, z1), new Point3D(x2, y2, z2), new Point3D(x1, y2, z2), new Point3D(0, 1, 0));
        addQuad(l, new Point3D(x1, y1, z2), new Point3D(x2, y1, z2), new Point3D(x2, y1, z1), new Point3D(x1, y1, z1), new Point3D(0, -1, 0));
        addQuad(l, new Point3D(x1, y1, z2), new Point3D(x2, y1, z2), new Point3D(x2, y2, z2), new Point3D(x1, y2, z2), new Point3D(0, 0, 1));
        addQuad(l, new Point3D(x2, y1, z1), new Point3D(x1, y1, z1), new Point3D(x1, y2, z1), new Point3D(x2, y2, z1), new Point3D(0, 0, -1));
        addQuad(l, new Point3D(x2, y1, z2), new Point3D(x2, y1, z1), new Point3D(x2, y2, z1), new Point3D(x2, y2, z2), new Point3D(1, 0, 0));
        addQuad(l, new Point3D(x1, y1, z1), new Point3D(x1, y1, z2), new Point3D(x1, y2, z2), new Point3D(x1, y2, z1), new Point3D(-1, 0, 0));
    }

    private static void addCyl(List<Tri> l, double cx, double cz, double r, double h) {
        int n = 32;
        for (int i = 0; i < n; i++) {
            double a1 = i * 2 * Math.PI / n, a2 = (i + 1) * 2 * Math.PI / n;
            double x1 = cx + r * Math.cos(a1), z1 = cz + r * Math.sin(a1), x2 = cx + r * Math.cos(a2), z2 = cz + r * Math.sin(a2);
            l.add(new Tri(new Point3D(cx, h, cz), new Point3D(x1, h, z1), new Point3D(x2, h, z2), new Point3D(0, 1, 0)));
            l.add(new Tri(new Point3D(cx, 0, cz), new Point3D(x2, 0, z2), new Point3D(x1, 0, z1), new Point3D(0, -1, 0)));
            Point3D norm = new Point3D((Math.cos(a1) + Math.cos(a2)) * 0.5, 0, (Math.sin(a1) + Math.sin(a2)) * 0.5);
            addQuad(l, new Point3D(x1, 0, z1), new Point3D(x2, 0, z2), new Point3D(x2, h, z2), new Point3D(x1, h, z1), norm);
        }
    }

    private static void addCone(List<Tri> l, double cx, double cz, double r, double h) {
        int n = 32; Point3D apex = new Point3D(cx, h, cz);
        for (int i = 0; i < n; i++) {
            double a1 = i * 2 * Math.PI / n, a2 = (i + 1) * 2 * Math.PI / n;
            Point3D b1 = new Point3D(cx + r * Math.cos(a1), 0, cz + r * Math.sin(a1)), b2 = new Point3D(cx + r * Math.cos(a2), 0, cz + r * Math.sin(a2));
            l.add(new Tri(new Point3D(cx, 0, cz), b2, b1, new Point3D(0, -1, 0)));
            l.add(new Tri(b1, b2, apex, b2.subtract(b1).crossProduct(apex.subtract(b1)).normalize()));
        }
    }

    private static void addSphere(List<Tri> l, double cx, double cz, double r) {
        int st = 16, sl = 24;
        for (int i = 0; i < st; i++) {
            double lt1 = Math.PI * (-0.5 + (double) i / st), lt2 = Math.PI * (-0.5 + (double) (i + 1) / st);
            double y1 = r * (1.0 + Math.sin(lt1)), y2 = r * (1.0 + Math.sin(lt2)), r1 = r * Math.cos(lt1), r2 = r * Math.cos(lt2);
            for (int j = 0; j < sl; j++) {
                double ln1 = 2 * Math.PI * j / sl, ln2 = 2 * Math.PI * (j + 1) / sl;
                Point3D a = new Point3D(cx + r1 * Math.cos(ln1), y1, cz + r1 * Math.sin(ln1)), b = new Point3D(cx + r1 * Math.cos(ln2), y1, cz + r1 * Math.sin(ln2));
                Point3D c = new Point3D(cx + r2 * Math.cos(ln2), y2, cz + r2 * Math.sin(ln2)), d = new Point3D(cx + r2 * Math.cos(ln1), y2, cz + r2 * Math.sin(ln1));
                addQuad(l, a, b, c, d, a.subtract(new Point3D(cx, r, cz)).normalize());
            }
        }
    }

    private static void addTri2D(List<Tri> l, Point3D p1, Point3D p2, basic_shapes_ui_main type) {
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), s = Math.sqrt(dx * dx + dz * dz);
        Point3D p3 = (type == basic_shapes_ui_main.EQUILATERAL_TRIANGLE && s > 1e-4)
            ? new Point3D(p1.getX() + dx * 0.5 - (dz / s) * s * Math.sqrt(3.0) / 2.0, 0, p1.getZ() + dz * 0.5 + (dx / s) * s * Math.sqrt(3.0) / 2.0)
            : new Point3D(p1.getX(), 0, p2.getZ());
        l.add(new Tri(new Point3D(p1.getX(), 0.5, p1.getZ()), new Point3D(p2.getX(), 0.5, p2.getZ()), new Point3D(p3.getX(), 0.5, p3.getZ()), new Point3D(0, 1, 0)));
        l.add(new Tri(new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(p3.getX(), 0, p3.getZ()), new Point3D(p2.getX(), 0, p2.getZ()), new Point3D(0, -1, 0)));
    }
}
