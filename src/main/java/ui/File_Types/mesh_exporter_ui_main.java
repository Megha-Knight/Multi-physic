package ui.File_Types;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.holes.hole_feature_ui_main;
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
 * Directly exports parametric hole boolean geometry, pattern instances, and primitives.
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
        List<hole_feature_ui_main> eff = item.getAllEffectiveHoles();
        if (eff != null && !eff.isEmpty()) {
            List<Tri> meshTris = extractHoleMeshTriangles(item, eff);
            if (!meshTris.isEmpty()) return transformTriangles(item, meshTris);
        }
        List<Tri> local = new ArrayList<>();
        basic_shapes_ui_main type = item.getType();
        Point3D p1 = item.getP1(), p2 = item.getP2();
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));

        switch (type) {
            case CUBE, SQUARE -> {
                double s = Math.max(Math.abs(dx), Math.abs(dz));
                double x2 = p1.getX() + (dx >= 0 ? s : -s), z2 = p1.getZ() + (dz >= 0 ? s : -s);
                mesh_primitive_helper_ui_main.addBox(local, Math.min(p1.getX(), x2), Math.max(p1.getX(), x2), 0, (type == basic_shapes_ui_main.CUBE) ? s : 0.5, Math.min(p1.getZ(), z2), Math.max(p1.getZ(), z2));
            }
            case RECTANGLE -> mesh_primitive_helper_ui_main.addBox(local, Math.min(p1.getX(), p2.getX()), Math.max(p1.getX(), p2.getX()), 0, 0.5, Math.min(p1.getZ(), p2.getZ()), Math.max(p1.getZ(), p2.getZ()));
            case CUBOID -> {
                double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(Math.abs(dx), Math.abs(dz)) * 0.5);
                mesh_primitive_helper_ui_main.addBox(local, Math.min(p1.getX(), p2.getX()), Math.max(p1.getX(), p2.getX()), 0, h, Math.min(p1.getZ(), p2.getZ()), Math.max(p1.getZ(), p2.getZ()));
            }
            case CYLINDER -> mesh_primitive_helper_ui_main.addCyl(local, p1.getX(), p1.getZ(), r, Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0));
            case CONE -> mesh_primitive_helper_ui_main.addCone(local, p1.getX(), p1.getZ(), r, Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0));
            case SPHERE -> mesh_primitive_helper_ui_main.addSphere(local, p1.getX(), p1.getZ(), r);
            case CIRCLE -> mesh_primitive_helper_ui_main.addCyl(local, p1.getX(), p1.getZ(), p1.distance(p2), 0.5);
            default -> mesh_primitive_helper_ui_main.addTri2D(local, p1, p2, type);
        }
        return transformTriangles(item, local);
    }

    private static List<Tri> extractHoleMeshTriangles(shape_item_ui_main item, List<hole_feature_ui_main> eff) {
        List<Tri> list = new ArrayList<>();
        Node geo = (item.getType() == basic_shapes_ui_main.CUBE) ? ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCubeWithHoles(item.getP1(), item.getP2(), eff, false, false)
            : (item.getType() == basic_shapes_ui_main.CUBOID) ? ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCuboidWithHoles(item.getP1(), item.getP2(), eff, false, false)
            : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCylinderWithHoles(item.getP1(), item.getP2(), eff, false, false);
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
}
