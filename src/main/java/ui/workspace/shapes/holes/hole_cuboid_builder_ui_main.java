package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;
import ui.workspace.shapes.holes.hole_stepped_helper_ui_main.HoleCluster;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * hole_cuboid_builder_ui_main.java
 * High-precision CSG mesh subtraction engine for Cuboids/Slabs with intersecting parametric holes.
 */
public final class hole_cuboid_builder_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private hole_cuboid_builder_ui_main() {}

    private record CuboidFace(face_kind_ui_main kind, Frame frame, double fw, double fh, double thick, face_kind_ui_main opp) {}
    private record Box(double uMin, double uMax, double vMin, double vMax) {}
    private record CuboidContext(Point3D c, double w, double h, double d, List<hole_feature_ui_main> allHoles) {}

    public static Node buildCuboidWithHoles(Point3D p1, Point3D p2, List<hole_feature_ui_main> holes,
                                            boolean isPreview, boolean isSelected) {
        double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
        if (w < 0.2 || d < 0.2) return shape_geometry_3d_ui_main.createCuboid(p1, p2, isPreview, isSelected);
        double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);

        Point3D c = new Point3D((p1.getX() + p2.getX()) * 0.5, p1.getY() - h * 0.5, (p1.getZ() + p2.getZ()) * 0.5);
        CuboidContext ctx = new CuboidContext(c, w, h, d, holes);
        double hw = w * 0.5, hh = h * 0.5, hd = d * 0.5;

        CuboidFace[] faces = new CuboidFace[]{
            new CuboidFace(face_kind_ui_main.TOP,    new Frame(new Point3D(c.getX(), c.getY() - hh, c.getZ()), new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1)), w, d, h, face_kind_ui_main.BOTTOM),
            new CuboidFace(face_kind_ui_main.BOTTOM, new Frame(new Point3D(c.getX(), c.getY() + hh, c.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1)), w, d, h, face_kind_ui_main.TOP),
            new CuboidFace(face_kind_ui_main.FRONT,  new Frame(new Point3D(c.getX(), c.getY(), c.getZ() + hd), new Point3D(0, 0, 1), new Point3D(1, 0, 0), new Point3D(0, -1, 0)), w, h, d, face_kind_ui_main.BACK),
            new CuboidFace(face_kind_ui_main.BACK,   new Frame(new Point3D(c.getX(), c.getY(), c.getZ() - hd), new Point3D(0, 0, -1), new Point3D(-1, 0, 0), new Point3D(0, -1, 0)), w, h, d, face_kind_ui_main.FRONT),
            new CuboidFace(face_kind_ui_main.RIGHT,  new Frame(new Point3D(c.getX() + hw, c.getY(), c.getZ()), new Point3D(1, 0, 0), new Point3D(0, 0, -1), new Point3D(0, -1, 0)), d, h, w, face_kind_ui_main.LEFT),
            new CuboidFace(face_kind_ui_main.LEFT,   new Frame(new Point3D(c.getX() - hw, c.getY(), c.getZ()), new Point3D(-1, 0, 0), new Point3D(0, 0, 1), new Point3D(0, -1, 0)), d, h, w, face_kind_ui_main.RIGHT)
        };

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        for (CuboidFace cf : faces) {
            List<hole_feature_ui_main> faceHoles = findHoles(holes, cf.kind, cf.fw, cf.fh);
            List<hole_feature_ui_main> oppHoles = findThroughHoles(holes, cf.opp, cf.fw, cf.fh);
            CuboidFace oppFace = getOppFace(faces, cf.opp);

            if (!faceHoles.isEmpty() || !oppHoles.isEmpty()) {
                partitionAndBuildFace(cf, ctx, faceHoles, oppHoles, pts, fcs, oppFace);
            } else {
                buildStandardQuad(cf.frame, cf.fw, cf.fh, pts, fcs);
            }
        }

        float[] pa = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()];     for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);
        mesh.getPoints().setAll(pa); mesh.getFaces().setAll(fa);
        int[] sga = new int[fa.length / 6]; java.util.Arrays.fill(sga, 1); mesh.getFaceSmoothingGroups().setAll(sga);
        MeshView mv = new MeshView(mesh); mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(isPreview, isSelected));
        return mv;
    }

    private static void partitionAndBuildFace(CuboidFace cf, CuboidContext ctx,
                                              List<hole_feature_ui_main> faceHoles,
                                              List<hole_feature_ui_main> oppHoles,
                                              List<Float> pts, List<Integer> fcs, CuboidFace oppFace) {
        List<HoleCluster> clusters = hole_stepped_helper_ui_main.clusterFaceHoles(cf.frame, oppFace.frame, cf.thick, faceHoles, oppHoles);
        partitionBox(new Box(-cf.fw * 0.5, cf.fw * 0.5, -cf.fh * 0.5, cf.fh * 0.5), clusters, cf, ctx, pts, fcs);
    }

    private static void partitionBox(Box b, List<HoleCluster> clusters, CuboidFace cf, CuboidContext ctx,
                                     List<Float> pts, List<Integer> fcs) {
        if (clusters == null || clusters.isEmpty()) return;
        if (clusters.size() == 1) {
            HoleCluster cl = clusters.get(0);
            Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(cf.frame, b.uMin, b.uMax, b.vMin, b.vMax);
            Point3D[] entry = hole_profile_helper_ui_main.getProfilePoints(cl.center, cf.frame.u(), cf.frame.v(), cl.primaryHole, cl.cutoutRadius, SEGS);
            hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, entry, pts, fcs, cl.isExitOnly());
            if (cl.hasEntry()) {
                if (cl.faceHoles.size() == 1) buildCavity(cf, ctx, cl.faceHoles.get(0), cl.center, entry, pts, fcs);
                else hole_stepped_helper_ui_main.buildSteppedCavity(cf.frame, cl.center, cl.faceHoles, cf.thick, pts, fcs);
            }
            return;
        }

        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (HoleCluster cl : clusters) { minU = Math.min(minU, cl.u); maxU = Math.max(maxU, cl.u); minV = Math.min(minV, cl.v); maxV = Math.max(maxV, cl.v); }
        boolean splitU = (maxU - minU >= maxV - minV);
        if (splitU && maxU - minU < 1e-4) splitU = false;
        if (!splitU && maxV - minV < 1e-4) splitU = true;

        if (splitU) clusters.sort(Comparator.comparingDouble(c -> c.u));
        else clusters.sort(Comparator.comparingDouble(c -> c.v));

        int bestK = 1; double maxGap = -1;
        for (int k = 1; k < clusters.size(); k++) {
            double gap = splitU ? (clusters.get(k).u - clusters.get(k - 1).u) : (clusters.get(k).v - clusters.get(k - 1).v);
            if (gap > maxGap) { maxGap = gap; bestK = k; }
        }

        List<HoleCluster> left = new ArrayList<>(clusters.subList(0, bestK)), right = new ArrayList<>(clusters.subList(bestK, clusters.size()));
        double sVal = splitU ? (clusters.get(bestK - 1).u + clusters.get(bestK).u) * 0.5 : (clusters.get(bestK - 1).v + clusters.get(bestK).v) * 0.5;
        partitionBox(new Box(b.uMin, splitU ? sVal : b.uMax, b.vMin, splitU ? b.vMax : sVal), left, cf, ctx, pts, fcs);
        partitionBox(new Box(splitU ? sVal : b.uMin, b.uMax, splitU ? b.vMin : sVal, b.vMax), right, cf, ctx, pts, fcs);
    }

    private static void buildCavity(CuboidFace cf, CuboidContext ctx, hole_feature_ui_main h, Point3D hc,
                                    Point3D[] entry, List<Float> pts, List<Integer> fcs) {
        Frame f = cf.frame;
        double rBore = h.getRadius();
        double totalDepth = hole_intersection_helper_ui_main.getEffectiveDepth(h, ctx.allHoles, ctx.c, ctx.w, ctx.h, ctx.d);
        if (totalDepth <= 1e-4) return;
        Point3D endCenter = hc.subtract(f.n().multiply(totalDepth)), boreStart = hc;

        if (h.getHoleType() == HoleType.COUNTERSINK) {
            double coneDepth = Math.min(totalDepth, h.getConeDepth());
            boreStart = hc.subtract(f.n().multiply(coneDepth));
            Point3D[] transCirc = hole_mesh_triangulator_ui_main.getCirclePoints(boreStart, f.u(), f.v(), rBore, SEGS);
            hole_advanced_mesh_helper_ui_main.buildFrustumWall(entry, transCirc, pts, fcs);
        } else if (h.getHoleType() == HoleType.COUNTERBORE) {
            double cbDepth = Math.min(totalDepth, h.getCbDepth());
            boreStart = hc.subtract(f.n().multiply(cbDepth));
            Point3D[] recessBottom = hole_mesh_triangulator_ui_main.getCirclePoints(boreStart, f.u(), f.v(), h.getOuterRadius(), SEGS);
            hole_mesh_triangulator_ui_main.buildCylindricalWall(entry, recessBottom, pts, fcs);
            Point3D[] shoulderInner = hole_mesh_triangulator_ui_main.getCirclePoints(boreStart, f.u(), f.v(), rBore, SEGS);
            hole_advanced_mesh_helper_ui_main.buildPlanarAnnulus(recessBottom, shoulderInner, pts, fcs);
        }

        hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(boreStart, endCenter, f.u(), f.v(), rBore,
                ctx.allHoles, h, ctx.c, ctx.w, ctx.h, ctx.d, pts, fcs);
        hole_intersection_helper_ui_main.buildCavityEndCapTrimmed(endCenter, f.u(), f.v(), h, rBore,
                ctx.allHoles, ctx.c, ctx.w, ctx.h, ctx.d, pts, fcs);
    }

    private static void buildStandardQuad(Frame f, double fw, double fh, List<Float> pts, List<Integer> fcs) {
        Point3D uH = f.u().multiply(fw * 0.5), vH = f.v().multiply(fh * 0.5);
        hole_mesh_triangulator_ui_main.addQuad(f.origin().subtract(uH).subtract(vH), f.origin().add(uH).subtract(vH), f.origin().add(uH).add(vH), f.origin().subtract(uH).add(vH), pts, fcs);
    }

    private static List<hole_feature_ui_main> findHoles(List<hole_feature_ui_main> holes, face_kind_ui_main face, double w, double h) {
        List<hole_feature_ui_main> res = new ArrayList<>();
        if (holes != null) for (hole_feature_ui_main x : holes) if (x != null && x.getFaceKind() == face && x.isValid() && x.fitsWithinFace(w, h)) res.add(x);
        return res;
    }

    private static List<hole_feature_ui_main> findThroughHoles(List<hole_feature_ui_main> holes, face_kind_ui_main opp, double w, double h) {
        List<hole_feature_ui_main> res = new ArrayList<>();
        if (holes != null) for (hole_feature_ui_main x : holes) if (x != null && x.getFaceKind() == opp && x.isValid() && x.isThroughAll() && x.fitsWithinFace(w, h)) res.add(x);
        return res;
    }

    private static CuboidFace getOppFace(CuboidFace[] faces, face_kind_ui_main kind) {
        for (CuboidFace cf : faces) if (cf.kind == kind) return cf;
        return faces[0];
    }
}
