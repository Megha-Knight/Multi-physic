package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * hole_cylinder_builder_ui_main.java
 * High-precision 3D mesh subtraction for Cylinders with axial parametric holes.
 */
public final class hole_cylinder_builder_ui_main {

    private static final int SEGS = 24;

    private hole_cylinder_builder_ui_main() {}

    public static Node buildCylinderWithHoles(Point3D p1, Point3D p2, List<hole_feature_ui_main> holes,
                                             boolean isPreview, boolean isSelected) {
        double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
        double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        if (r < 0.2) return shape_geometry_3d_ui_main.createCylinder(p1, p2, isPreview, isSelected);

        hole_feature_ui_main hole = (holes != null && !holes.isEmpty()) ? holes.get(0) : null;
        if (hole == null || !hole.isValid() || (hole.getFaceKind() != face_kind_ui_main.TOP_CAP && hole.getFaceKind() != face_kind_ui_main.BOTTOM_CAP)) {
            return shape_geometry_3d_ui_main.createCylinder(p1, p2, isPreview, isSelected);
        }
        if (!hole.fitsWithinCylinderCap(r)) {
            return shape_geometry_3d_ui_main.createCylinder(p1, p2, isPreview, isSelected);
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        double hr = hole.getRadius();
        double depth = hole.isThroughAll() ? h : Math.min(h, hole.getDepth());
        boolean isTop = (hole.getFaceKind() == face_kind_ui_main.TOP_CAP);

        double cx = p1.getX(), cz = p1.getZ();
        double hx = cx + hole.getU(), hz = cz + (isTop ? hole.getV() : -hole.getV());
        double entryY = isTop ? -h : 0, endY = isTop ? (-h + depth) : -depth;

        // Outer lateral wall of cylinder
        for (int i = 0; i < SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
            Point3D t1 = new Point3D(cx + r * Math.cos(a1), -h, cz + r * Math.sin(a1));
            Point3D t2 = new Point3D(cx + r * Math.cos(a2), -h, cz + r * Math.sin(a2));
            Point3D b1 = new Point3D(cx + r * Math.cos(a1), 0, cz + r * Math.sin(a1));
            Point3D b2 = new Point3D(cx + r * Math.cos(a2), 0, cz + r * Math.sin(a2));
            addQuad(t1, t2, b2, b1, pts, fcs);
        }

        // Annular entry cap
        buildAnnulus(cx, cz, hx, hz, entryY, r, hr, isTop, pts, fcs);

        // Hole cylindrical interior wall
        for (int i = 0; i < SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
            Point3D h1 = new Point3D(hx + hr * Math.cos(a1), entryY, hz + hr * Math.sin(a1));
            Point3D h2 = new Point3D(hx + hr * Math.cos(a2), entryY, hz + hr * Math.sin(a2));
            Point3D d1 = new Point3D(hx + hr * Math.cos(a1), endY, hz + hr * Math.sin(a1));
            Point3D d2 = new Point3D(hx + hr * Math.cos(a2), endY, hz + hr * Math.sin(a2));
            addQuad(h1, d1, d2, h2, pts, fcs);
        }

        if (hole.isThroughAll()) {
            buildAnnulus(cx, cz, hx, hz, isTop ? 0 : -h, r, hr, !isTop, pts, fcs);
        } else {
            // Blind hole bottom cap
            Point3D bCenter = new Point3D(hx, endY, hz);
            for (int i = 0; i < SEGS; i++) {
                double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
                Point3D d1 = new Point3D(hx + hr * Math.cos(a1), endY, hz + hr * Math.sin(a1));
                Point3D d2 = new Point3D(hx + hr * Math.cos(a2), endY, hz + hr * Math.sin(a2));
                addTri(bCenter, isTop ? d2 : d1, isTop ? d1 : d2, pts, fcs);
            }
            // Opposite full cap
            double oppY = isTop ? 0 : -h;
            Point3D oppCenter = new Point3D(cx, oppY, cz);
            for (int i = 0; i < SEGS; i++) {
                double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
                Point3D c1 = new Point3D(cx + r * Math.cos(a1), oppY, cz + r * Math.sin(a1));
                Point3D c2 = new Point3D(cx + r * Math.cos(a2), oppY, cz + r * Math.sin(a2));
                addTri(oppCenter, isTop ? c1 : c2, isTop ? c2 : c1, pts, fcs);
            }
        }

        float[] pa = new float[pts.size()];
        for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()];
        for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);

        mesh.getPoints().setAll(pa);
        mesh.getFaces().setAll(fa);
        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(isPreview, isSelected));
        return mv;
    }

    private static void buildAnnulus(double cx, double cz, double hx, double hz, double y, double rOut, double rIn,
                                     boolean faceUp, List<Float> pts, List<Integer> fcs) {
        for (int i = 0; i < SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
            Point3D o1 = new Point3D(cx + rOut * Math.cos(a1), y, cz + rOut * Math.sin(a1));
            Point3D o2 = new Point3D(cx + rOut * Math.cos(a2), y, cz + rOut * Math.sin(a2));
            Point3D i1 = new Point3D(hx + rIn * Math.cos(a1), y, hz + rIn * Math.sin(a1));
            Point3D i2 = new Point3D(hx + rIn * Math.cos(a2), y, hz + rIn * Math.sin(a2));
            if (faceUp) {
                addQuad(o1, i1, i2, o2, pts, fcs);
            } else {
                addQuad(o1, o2, i2, i1, pts, fcs);
            }
        }
    }

    private static void addQuad(Point3D p0, Point3D p1, Point3D p2, Point3D p3, List<Float> pts, List<Integer> fcs) {
        addTri(p0, p1, p2, pts, fcs);
        addTri(p0, p2, p3, pts, fcs);
    }

    private static void addTri(Point3D a, Point3D b, Point3D c, List<Float> pts, List<Integer> fcs) {
        int i0 = addVertex(a, pts), i1 = addVertex(b, pts), i2 = addVertex(c, pts);
        fcs.add(i0); fcs.add(0); fcs.add(i1); fcs.add(0); fcs.add(i2); fcs.add(0);
    }

    private static int addVertex(Point3D p, List<Float> pts) {
        int idx = pts.size() / 3;
        pts.add((float) p.getX()); pts.add((float) p.getY()); pts.add((float) p.getZ());
        return idx;
    }
}
