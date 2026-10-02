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
            topIdx[i] = hole_mesh_triangulator_ui_main.addVertex(new Point3D(cx + r * Math.cos(a), -h, cz + r * Math.sin(a)), pts);
            botIdx[i] = hole_mesh_triangulator_ui_main.addVertex(new Point3D(cx + r * Math.cos(a), 0, cz + r * Math.sin(a)), pts);
        }
        for (int i = 0; i < SEGS; i++) {
            int next = (i + 1) % SEGS;
            hole_mesh_triangulator_ui_main.addTriIdx(topIdx[i], topIdx[next], botIdx[next], fcs, false);
            hole_mesh_triangulator_ui_main.addTriIdx(topIdx[i], botIdx[next], botIdx[i], fcs, false);
        }

        buildCap(cx, cz, -h, r, true, topHoles, botHoles, holes, h, pts, fcs);
        buildCap(cx, cz, 0.0, r, false, botHoles, topHoles, holes, h, pts, fcs);

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

    private static void buildCap(double cx, double cz, double y, double r, boolean isTop,
                                 List<hole_feature_ui_main> capHoles, List<hole_feature_ui_main> oppHoles,
                                 List<hole_feature_ui_main> allHoles, double h, List<Float> pts, List<Integer> fcs) {
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
            if (clusters.size() == 1) {
                hole_stepped_helper_ui_main.HoleCluster cl = clusters.get(0);
                Point3D[] innerProf = hole_profile_helper_ui_main.getProfilePoints(cl.center, u, v, cl.getPrimaryHole(), cl.getMaxOuterRadius(), SEGS);
                buildAnnulus(cx, cz, innerProf, y, r, isTop, pts, fcs);
                if (cl.holes.size() == 1) {
                    hole_advanced_mesh_helper_ui_main.buildCylinderHoleCavity(cx, cz, cl.center.getX(), cl.center.getZ(), y, h, isTop, cl.holes.get(0), allHoles, r, pts, fcs);
                } else {
                    hole_stepped_helper_ui_main.buildCylinderSteppedCavity(cx, cz, cl.center.getX(), cl.center.getZ(), y, h, isTop, cl.holes, pts, fcs);
                }
            } else {
                hole_pattern_mesh_helper_ui_main.buildCylinderCapMultiHoles(cx, cz, y, r, isTop, capHoles, false, pts, fcs);
                for (hole_feature_ui_main hole : capHoles) {
                    double hx = cx + hole.getU(), hz = cz + (isTop ? hole.getV() : -hole.getV());
                    hole_advanced_mesh_helper_ui_main.buildCylinderHoleCavity(cx, cz, hx, hz, y, h, isTop, hole, allHoles, r, pts, fcs);
                }
            }
        } else if (!throughExits.isEmpty()) {
            if (throughExits.size() == 1) {
                hole_feature_ui_main oh = throughExits.get(0);
                double hx = cx + oh.getU(), hz = cz + (!isTop ? oh.getV() : -oh.getV());
                Point3D[] innerProf = hole_profile_helper_ui_main.getProfilePoints(new Point3D(hx, y, hz), u, v, oh, oh.getRadius(), SEGS);
                buildAnnulus(cx, cz, innerProf, y, r, isTop, pts, fcs);
            } else {
                hole_pattern_mesh_helper_ui_main.buildCylinderCapMultiHoles(cx, cz, y, r, isTop, throughExits, true, pts, fcs);
            }
        } else {
            Point3D center = new Point3D(cx, y, cz);
            for (int i = 0; i < SEGS; i++) {
                double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
                Point3D c1 = new Point3D(cx + r * Math.cos(a1), y, cz + r * Math.sin(a1));
                Point3D c2 = new Point3D(cx + r * Math.cos(a2), y, cz + r * Math.sin(a2));
                hole_mesh_triangulator_ui_main.addTri(center, isTop ? c1 : c2, isTop ? c2 : c1, pts, fcs, false);
            }
        }
    }

    private static void buildAnnulus(double cx, double cz, Point3D[] innerProf, double y, double rOut,
                                     boolean faceUp, List<Float> pts, List<Integer> fcs) {
        for (int i = 0; i < SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
            Point3D o1 = new Point3D(cx + rOut * Math.cos(a1), y, cz + rOut * Math.sin(a1));
            Point3D o2 = new Point3D(cx + rOut * Math.cos(a2), y, cz + rOut * Math.sin(a2));
            Point3D i1 = innerProf[i], i2 = innerProf[(i + 1) % SEGS];
            if (faceUp) hole_mesh_triangulator_ui_main.addQuad(o1, i1, i2, o2, pts, fcs);
            else hole_mesh_triangulator_ui_main.addQuad(o1, o2, i2, i1, pts, fcs);
        }
    }
}
