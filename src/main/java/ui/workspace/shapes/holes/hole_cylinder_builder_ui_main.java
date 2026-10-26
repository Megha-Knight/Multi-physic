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
 * High-precision 3D mesh subtraction for Cylinders with parametric holes and patterns.
 */
public final class hole_cylinder_builder_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private hole_cylinder_builder_ui_main() {}

    public static Node buildCylinderWithHoles(Point3D p1, Point3D p2, List<hole_feature_ui_main> holes,
                                              boolean isPreview, boolean isSelected) {
        double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
        double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        if (r < 0.2 || holes == null || holes.isEmpty()) return shape_geometry_3d_ui_main.createCylinder(p1, p2, isPreview, isSelected);

        List<hole_feature_ui_main> topHoles = new ArrayList<>();
        List<hole_feature_ui_main> botHoles = new ArrayList<>();
        for (hole_feature_ui_main hole : holes) {
            if (hole != null && hole.isValid() && hole.fitsWithinCylinderCap(r)) {
                if (hole.getFaceKind() == face_kind_ui_main.TOP_CAP) topHoles.add(hole);
                else if (hole.getFaceKind() == face_kind_ui_main.BOTTOM_CAP) botHoles.add(hole);
            }
        }
        if (topHoles.isEmpty() && botHoles.isEmpty()) return shape_geometry_3d_ui_main.createCylinder(p1, p2, isPreview, isSelected);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();
        double cx = p1.getX(), cz = p1.getZ();

        int[] topIdx = new int[SEGS], botIdx = new int[SEGS];
        for (int i = 0; i < SEGS; i++) {
            double a = i * 2.0 * Math.PI / SEGS;
            topIdx[i] = addPt(pts, cx + r * Math.cos(a), -h, cz + r * Math.sin(a));
            botIdx[i] = addPt(pts, cx + r * Math.cos(a), 0, cz + r * Math.sin(a));
        }
        for (int i = 0; i < SEGS; i++) {
            int next = (i + 1) % SEGS;
            addTri(fcs, botIdx[i], botIdx[next], topIdx[next]);
            addTri(fcs, botIdx[i], topIdx[next], topIdx[i]);
        }

        buildCapWithOuterRing(cx, cz, -h, r, true, topIdx, topHoles, botHoles, holes, h, pts, fcs);
        buildCapWithOuterRing(cx, cz, 0.0, r, false, botIdx, botHoles, topHoles, holes, h, pts, fcs);

        float[] pa = new float[pts.size()];
        for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()];
        for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);

        mesh.getPoints().setAll(pa);
        mesh.getFaces().setAll(fa);
        int[] sga = new int[fa.length / 6];
        java.util.Arrays.fill(sga, 1);
        mesh.getFaceSmoothingGroups().setAll(sga);
        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(isPreview, isSelected));
        return mv;
    }

    public static void buildCapWithOuterRing(double cx, double cz, double y, double r, boolean isTop,
                                             int[] outerRing, List<hole_feature_ui_main> capHoles,
                                             List<hole_feature_ui_main> oppHoles,
                                             List<hole_feature_ui_main> allHoles, double h,
                                             List<Float> pts, List<Integer> fcs) {
        if (outerRing == null) {
            outerRing = new int[SEGS];
            for (int i = 0; i < SEGS; i++) {
                double a = i * 2.0 * Math.PI / SEGS;
                outerRing[i] = addPt(pts, cx + r * Math.cos(a), y, cz + r * Math.sin(a));
            }
        }
        List<hole_feature_ui_main> throughExits = new ArrayList<>();
        for (hole_feature_ui_main oh : oppHoles) if (oh.isThroughAll()) throughExits.add(oh);
        Point3D u = new Point3D(1, 0, 0), v = isTop ? new Point3D(0, 0, 1) : new Point3D(0, 0, -1);

        if (!capHoles.isEmpty()) {
            List<hole_stepped_helper_ui_main.HoleCluster> clusters = new ArrayList<>();
            for (hole_feature_ui_main hf : capHoles) {
                hole_stepped_helper_ui_main.HoleCluster match = null;
                for (hole_stepped_helper_ui_main.HoleCluster c : clusters) {
                    if (Math.hypot(hf.getU() - c.u, hf.getV() - c.v) < 0.5) { match = c; break; }
                }
                if (match == null) {
                    double hx = cx + hf.getU(), hz = cz + (isTop ? hf.getV() : -hf.getV());
                    match = new hole_stepped_helper_ui_main.HoleCluster(hf.getU(), hf.getV(), new Point3D(hx, y, hz));
                    clusters.add(match);
                }
                match.holes.add(hf);
            }
            if (clusters.size() == 1 && Math.hypot(clusters.get(0).u, clusters.get(0).v) < 1e-4) {
                hole_stepped_helper_ui_main.HoleCluster cl = clusters.get(0);
                Point3D[] innerProf = hole_profile_helper_ui_main.getProfilePoints(cl.center, u, v, cl.getPrimaryHole(), cl.getMaxOuterRadius(), SEGS);
                buildAnnulusWithRing(outerRing, innerProf, isTop, pts, fcs);
                if (cl.holes.size() == 1) {
                    hole_advanced_mesh_helper_ui_main.buildCylinderHoleCavity(cx, cz, cl.center.getX(), cl.center.getZ(), y, h, isTop, cl.holes.get(0), allHoles, r, pts, fcs);
                } else {
                    hole_stepped_helper_ui_main.buildCylinderSteppedCavity(cx, cz, cl.center.getX(), cl.center.getZ(), y, h, isTop, cl.holes, pts, fcs);
                }
            } else {
                hole_pattern_mesh_helper_ui_main.buildCylinderCapMultiHoles(cx, cz, y, r, isTop, outerRing, capHoles, false, pts, fcs);
                for (hole_feature_ui_main hole : capHoles) {
                    double hx = cx + hole.getU(), hz = cz + (isTop ? hole.getV() : -hole.getV());
                    hole_advanced_mesh_helper_ui_main.buildCylinderHoleCavity(cx, cz, hx, hz, y, h, isTop, hole, allHoles, r, pts, fcs);
                }
            }
        } else if (!throughExits.isEmpty()) {
            if (throughExits.size() == 1 && Math.hypot(throughExits.get(0).getU(), throughExits.get(0).getV()) < 1e-4) {
                hole_feature_ui_main oh = throughExits.get(0);
                double hx = cx + oh.getU(), hz = cz + (!isTop ? oh.getV() : -oh.getV());
                Point3D[] innerProf = hole_profile_helper_ui_main.getProfilePoints(new Point3D(hx, y, hz), u, v, oh, oh.getRadius(), SEGS);
                buildAnnulusWithRing(outerRing, innerProf, isTop, pts, fcs);
            } else {
                hole_pattern_mesh_helper_ui_main.buildCylinderCapMultiHoles(cx, cz, y, r, isTop, outerRing, throughExits, true, pts, fcs);
            }
        } else {
            int center = addPt(pts, cx, y, cz);
            for (int i = 0; i < SEGS; i++) {
                int next = (i + 1) % SEGS;
                if (isTop) addTri(fcs, center, outerRing[i], outerRing[next]);
                else addTri(fcs, center, outerRing[next], outerRing[i]);
            }
        }
    }

    private static void buildAnnulusWithRing(int[] outerRing, Point3D[] innerProf, boolean isTop, List<Float> pts, List<Integer> fcs) {
        int[] innerRing = new int[SEGS];
        for (int i = 0; i < SEGS; i++) innerRing[i] = addPt(pts, innerProf[i].getX(), innerProf[i].getY(), innerProf[i].getZ());
        for (int i = 0; i < SEGS; i++) {
            int n = (i + 1) % SEGS;
            if (isTop) {
                addTri(fcs, outerRing[i], outerRing[n], innerRing[n]);
                addTri(fcs, outerRing[i], innerRing[n], innerRing[i]);
            } else {
                addTri(fcs, outerRing[n], outerRing[i], innerRing[i]);
                addTri(fcs, outerRing[n], innerRing[i], innerRing[n]);
            }
        }
    }

    private static int addPt(List<Float> pts, double x, double y, double z) {
        int idx = pts.size() / 3; pts.add((float) x); pts.add((float) y); pts.add((float) z); return idx;
    }

    private static void addTri(List<Integer> fcs, int a, int b, int c) {
        fcs.add(a); fcs.add(0); fcs.add(b); fcs.add(0); fcs.add(c); fcs.add(0);
    }
}
