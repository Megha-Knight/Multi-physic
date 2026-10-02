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
    private record LocalHole(hole_feature_ui_main h, double u, double v, Point3D center, List<hole_feature_ui_main> group) {}
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

            if (!faceHoles.isEmpty()) partitionAndBuildFace(cf, ctx, faceHoles, pts, fcs, false, null);
            else if (!oppHoles.isEmpty()) partitionAndBuildFace(cf, ctx, oppHoles, pts, fcs, true, getOppFace(faces, cf.opp));
            else buildStandardQuad(cf.frame, cf.fw, cf.fh, pts, fcs);
        }

        float[] pa = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()];     for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);
        mesh.getPoints().setAll(pa); mesh.getFaces().setAll(fa);
        int[] sga = new int[fa.length / 6]; java.util.Arrays.fill(sga, 1); mesh.getFaceSmoothingGroups().setAll(sga);
        MeshView mv = new MeshView(mesh); mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(isPreview, isSelected));
        return mv;
    }

    private static void partitionAndBuildFace(CuboidFace cf, CuboidContext ctx, List<hole_feature_ui_main> holes,
                                              List<Float> pts, List<Integer> fcs, boolean isExit, CuboidFace oppFace) {
        List<LocalHole> locals = new ArrayList<>();
        for (hole_feature_ui_main h : holes) {
            Point3D hc; double u, v;
            if (!isExit) {
                u = h.getU(); v = h.getV();
                hc = cf.frame.origin().add(cf.frame.u().multiply(u)).add(cf.frame.v().multiply(v));
            } else {
                Point3D ep = oppFace.frame.origin().add(oppFace.frame.u().multiply(h.getU())).add(oppFace.frame.v().multiply(h.getV()));
                hc = ep.subtract(oppFace.frame.n().multiply(cf.thick));
                Point3D te = hc.subtract(cf.frame.origin());
                u = te.dotProduct(cf.frame.u()); v = te.dotProduct(cf.frame.v());
            }
            LocalHole match = null;
            for (LocalHole lh : locals) if (Math.hypot(u - lh.u, v - lh.v) < 0.5) { match = lh; break; }
            if (match == null) {
                List<hole_feature_ui_main> grp = new ArrayList<>(); grp.add(h);
                locals.add(new LocalHole(h, u, v, hc, grp));
            } else { match.group.add(h); }
        }
        partitionBox(new Box(-cf.fw * 0.5, cf.fw * 0.5, -cf.fh * 0.5, cf.fh * 0.5), locals, cf, ctx, pts, fcs, isExit);
    }

    private static void partitionBox(Box b, List<LocalHole> holes, CuboidFace cf, CuboidContext ctx,
                                     List<Float> pts, List<Integer> fcs, boolean isExit) {
        if (holes == null || holes.isEmpty()) return;
        if (holes.size() == 1) {
            LocalHole lh = holes.get(0);
            Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(cf.frame, b.uMin, b.uMax, b.vMin, b.vMax);
            if (!isExit) {
                double rEntry = 0;
                for (hole_feature_ui_main x : lh.group) rEntry = Math.max(rEntry, x.getOuterRadius());
                Point3D[] entry = hole_profile_helper_ui_main.getProfilePoints(lh.center, cf.frame.u(), cf.frame.v(), lh.h, rEntry, SEGS);
                hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, entry, pts, fcs, false);
                if (lh.group.size() == 1) buildCavity(cf, ctx, lh.h, lh.center, entry, pts, fcs);
                else hole_stepped_helper_ui_main.buildSteppedCavity(cf.frame, lh.center, lh.group, cf.thick, pts, fcs);
            } else {
                double rExit = Double.MAX_VALUE;
                for (hole_feature_ui_main x : lh.group) if (x.isThroughAll()) rExit = Math.min(rExit, x.getRadius());
                if (rExit < Double.MAX_VALUE) {
                    Point3D[] exit = hole_profile_helper_ui_main.getProfilePoints(lh.center, cf.frame.u(), cf.frame.v(), lh.h, rExit, SEGS);
                    hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, exit, pts, fcs, true);
                }
            }
            return;
        }

        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (LocalHole lh : holes) { minU = Math.min(minU, lh.u); maxU = Math.max(maxU, lh.u); minV = Math.min(minV, lh.v); maxV = Math.max(maxV, lh.v); }
        boolean splitU = (maxU - minU >= maxV - minV);
        if (splitU && maxU - minU < 1e-4) splitU = false;
        if (!splitU && maxV - minV < 1e-4) splitU = true;

        if (splitU) holes.sort(Comparator.comparingDouble(lh -> lh.u));
        else holes.sort(Comparator.comparingDouble(lh -> lh.v));

        int bestK = 1; double maxGap = -1;
        for (int k = 1; k < holes.size(); k++) {
            double gap = splitU ? (holes.get(k).u - holes.get(k - 1).u) : (holes.get(k).v - holes.get(k - 1).v);
            if (gap > maxGap) { maxGap = gap; bestK = k; }
        }

        List<LocalHole> left = new ArrayList<>(holes.subList(0, bestK)), right = new ArrayList<>(holes.subList(bestK, holes.size()));
        double sVal = splitU ? (holes.get(bestK - 1).u + holes.get(bestK).u) * 0.5 : (holes.get(bestK - 1).v + holes.get(bestK).v) * 0.5;
        partitionBox(new Box(b.uMin, splitU ? sVal : b.uMax, b.vMin, splitU ? b.vMax : sVal), left, cf, ctx, pts, fcs, isExit);
        partitionBox(new Box(splitU ? sVal : b.uMin, b.uMax, splitU ? b.vMin : sVal, b.vMax), right, cf, ctx, pts, fcs, isExit);
    }

    private static void buildCavity(CuboidFace cf, CuboidContext ctx, hole_feature_ui_main h, Point3D hc,
                                    Point3D[] entry, List<Float> pts, List<Integer> fcs) {
        Frame f = cf.frame;
        double rBore = h.getRadius(), totalDepth = h.isThroughAll() ? cf.thick : Math.min(cf.thick, h.getDepth());
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

        if (h.getCutoutShape() == hole_feature_ui_main.CutoutShape.CIRCLE) {
            hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(boreStart, endCenter, f.u(), f.v(), rBore,
                    ctx.allHoles, h, ctx.c, ctx.w, ctx.h, ctx.d, pts, fcs);
        } else {
            Point3D[] topProf = hole_profile_helper_ui_main.getProfilePoints(boreStart, f.u(), f.v(), h, SEGS);
            Point3D[] botProf = hole_profile_helper_ui_main.getProfilePoints(endCenter, f.u(), f.v(), h, SEGS);
            hole_mesh_triangulator_ui_main.buildCylindricalWall(topProf, botProf, pts, fcs);
        }
        if (!h.isThroughAll() && !hole_intersection_helper_ui_main.isInsideAnyOtherHole(endCenter, ctx.allHoles, h, ctx.c, ctx.w, ctx.h, ctx.d)) {
            Point3D[] boreEnd = hole_profile_helper_ui_main.getProfilePoints(endCenter, f.u(), f.v(), h, rBore, SEGS);
            hole_mesh_triangulator_ui_main.buildCircleCap(endCenter, boreEnd, pts, fcs, true);
        }
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
