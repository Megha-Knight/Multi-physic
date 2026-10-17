package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

import java.util.ArrayList;
import java.util.List;

public final class machining_cylinder_mesh_helper_ui_main {
    private static final int SEGS = 32;

    private machining_cylinder_mesh_helper_ui_main() {}

    public static Node buildMachinedCylinder(shape_item_ui_main shape, boolean isSelected) {
        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        double r = Math.max(0.2, p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
        double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        double cx = p1.getX(), cz = p1.getZ();

        double chTop = getDist(shape, "TOP_RIM"), filTop = getRad(shape, "TOP_RIM");
        double chBot = getDist(shape, "BOTTOM_RIM"), filBot = getRad(shape, "BOTTOM_RIM");
        double mTop = Math.min(r * 0.9, Math.max(chTop, filTop)), mBot = Math.min(r * 0.9, Math.max(chBot, filBot));
        if (mTop <= 0.0 && mBot <= 0.0) return null;

        List<Float> pts = new ArrayList<>(); List<Integer> fcs = new ArrayList<>();
        int cBot = addPt(pts, cx, 0, cz), cTop = addPt(pts, cx, -h, cz);

        int nBot = (filBot > 0) ? 4 : (chBot > 0 ? 1 : 0);
        int nTop = (filTop > 0) ? 4 : (chTop > 0 ? 1 : 0);

        int[][] botRings = new int[nBot + 1][SEGS];
        for (int a = 0; a <= nBot; a++) {
            double phi = (nBot == 0) ? 0 : (Math.PI * 0.5 * a / nBot);
            double ra = (mBot <= 0) ? r : (filBot > 0 ? (r - mBot) + mBot * Math.sin(phi) : (r - mBot) + mBot * (double) a / nBot);
            double ya = (mBot <= 0) ? 0 : (filBot > 0 ? -mBot * (1.0 - Math.cos(phi)) : -mBot * (double) a / nBot);
            for (int i = 0; i < SEGS; i++) {
                double ang = i * 2.0 * Math.PI / SEGS;
                botRings[a][i] = addPt(pts, cx + ra * Math.cos(ang), ya, cz + ra * Math.sin(ang));
            }
        }

        int[][] topRings = new int[nTop + 1][SEGS];
        for (int a = 0; a <= nTop; a++) {
            double phi = (nTop == 0) ? 0 : (Math.PI * 0.5 * a / nTop);
            double ra = (mTop <= 0) ? r : (filTop > 0 ? r - mTop * (1.0 - Math.sin(phi)) : r - mTop * (1.0 - (double) a / nTop));
            double ya = (mTop <= 0) ? -h : (filTop > 0 ? -(h - mTop) - mTop * Math.cos(phi) : -(h - mTop) - mTop * (double) a / nTop);
            for (int i = 0; i < SEGS; i++) {
                double ang = i * 2.0 * Math.PI / SEGS;
                topRings[a][i] = addPt(pts, cx + ra * Math.cos(ang), ya, cz + ra * Math.sin(ang));
            }
        }

        for (int i = 0; i < SEGS; i++) {
            int n = (i + 1) % SEGS;
            addTri(fcs, cBot, botRings[0][n], botRings[0][i]);
            for (int a = 0; a < nBot; a++) addQuad(fcs, botRings[a][i], botRings[a][n], botRings[a + 1][n], botRings[a + 1][i]);
            addQuad(fcs, botRings[nBot][i], botRings[nBot][n], topRings[nTop][n], topRings[nTop][i]);
            for (int a = 0; a < nTop; a++) addQuad(fcs, topRings[a][i], topRings[a][n], topRings[a + 1][n], topRings[a + 1][i]);
            addTri(fcs, cTop, topRings[0][i], topRings[0][n]);
        }
        return createView(pts, fcs, isSelected);
    }

    public static Node buildMachinedCone(shape_item_ui_main shape, boolean isSelected) {
        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        double r = Math.max(0.2, p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
        double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        double cx = p1.getX(), cz = p1.getZ();

        double chBase = getDist(shape, "BASE_RIM"), filBase = getRad(shape, "BASE_RIM");
        double mBase = Math.min(r * 0.9, Math.max(chBase, filBase));
        if (mBase <= 0.0) return null;

        List<Float> pts = new ArrayList<>(); List<Integer> fcs = new ArrayList<>();
        int cBot = addPt(pts, cx, 0, cz), cApex = addPt(pts, cx, -h, cz);
        int nBase = (filBase > 0) ? 4 : 1;
        int[][] rings = new int[nBase + 1][SEGS];
        for (int a = 0; a <= nBase; a++) {
            double phi = (Math.PI * 0.5 * a / nBase);
            double ra = (filBase > 0) ? (r - mBase) + mBase * Math.sin(phi) : (r - mBase) + mBase * (double) a / nBase;
            double ya = (filBase > 0) ? -mBase * (1.0 - Math.cos(phi)) : -mBase * (double) a / nBase;
            for (int i = 0; i < SEGS; i++) {
                double ang = i * 2.0 * Math.PI / SEGS;
                rings[a][i] = addPt(pts, cx + ra * Math.cos(ang), ya, cz + ra * Math.sin(ang));
            }
        }

        for (int i = 0; i < SEGS; i++) {
            int n = (i + 1) % SEGS;
            addTri(fcs, cBot, rings[0][n], rings[0][i]);
            for (int a = 0; a < nBase; a++) addQuad(fcs, rings[a][i], rings[a][n], rings[a + 1][n], rings[a + 1][i]);
            addTri(fcs, cApex, rings[nBase][i], rings[nBase][n]);
        }
        return createView(pts, fcs, isSelected);
    }

    private static MeshView createView(List<Float> pts, List<Integer> fcs, boolean sel) {
        float[] pa = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);
        TriangleMesh m = new TriangleMesh(); m.getPoints().setAll(pa); m.getFaces().setAll(fa);
        m.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        int[] sg = new int[fa.length / 6]; java.util.Arrays.fill(sg, 1); m.getFaceSmoothingGroups().setAll(sg);
        MeshView mv = new MeshView(m); mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(false, sel));
        return mv;
    }

    private static int addPt(List<Float> pts, double x, double y, double z) {
        int idx = pts.size() / 3; pts.add((float) x); pts.add((float) y); pts.add((float) z); return idx;
    }

    private static void addTri(List<Integer> fcs, int a, int b, int c) {
        fcs.add(a); fcs.add(0); fcs.add(b); fcs.add(1); fcs.add(c); fcs.add(2);
        fcs.add(a); fcs.add(0); fcs.add(c); fcs.add(2); fcs.add(b); fcs.add(1);
    }

    private static void addQuad(List<Integer> fcs, int a, int b, int c, int d) {
        addTri(fcs, a, b, c); addTri(fcs, a, c, d);
    }

    private static double getDist(shape_item_ui_main s, String name) {
        for (var c : s.getChamfers()) if (c.isValid() && (name.equalsIgnoreCase(c.getSimpleEdgeName()) || name.equalsIgnoreCase(c.getEdgeId()))) return c.getDistance();
        return 0.0;
    }

    private static double getRad(shape_item_ui_main s, String name) {
        for (var f : s.getFillets()) if (f.isValid() && (name.equalsIgnoreCase(f.getSimpleEdgeName()) || name.equalsIgnoreCase(f.getEdgeId()))) return f.getRadius();
        return 0.0;
    }
}
