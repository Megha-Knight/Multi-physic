package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * hole_cuboid_builder_ui_main.java
 * High-precision CSG mesh subtraction engine for Cuboids/Slabs with parametric holes.
 */
public final class hole_cuboid_builder_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private hole_cuboid_builder_ui_main() {}

    private record CuboidFace(face_kind_ui_main kind, Frame frame, double fw, double fh, double thick, face_kind_ui_main opp) {}
    private record LocalHole(hole_feature_ui_main h, double u, double v, Point3D center) {}
    private record Box(double uMin, double uMax, double vMin, double vMax) {}

    public static Node buildCuboidWithHoles(Point3D p1, Point3D p2, List<hole_feature_ui_main> holes,
                                            boolean isPreview, boolean isSelected) {
        double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
        if (w < 0.2 || d < 0.2) return shape_geometry_3d_ui_main.createCuboid(p1, p2, isPreview, isSelected);
        double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);

        Point3D c = new Point3D((p1.getX() + p2.getX()) * 0.5, p1.getY() - h * 0.5, (p1.getZ() + p2.getZ()) * 0.5);
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

            if (!faceHoles.isEmpty()) partitionAndBuildFace(cf, faceHoles, pts, fcs, false, null);
            else if (!oppHoles.isEmpty()) partitionAndBuildFace(cf, oppHoles, pts, fcs, true, getOppFace(faces, cf.opp));
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

    private static void partitionAndBuildFace(CuboidFace cf, List<hole_feature_ui_main> holes,
                                              List<Float> pts, List<Integer> fcs, boolean isExit, CuboidFace oppFace) {
        List<LocalHole> locals = new ArrayList<>(holes.size());
        for (hole_feature_ui_main h : holes) {
            if (!isExit) {
                Point3D hc = cf.frame.origin().add(cf.frame.u().multiply(h.getU())).add(cf.frame.v().multiply(h.getV()));
                locals.add(new LocalHole(h, h.getU(), h.getV(), hc));
            } else {
                Point3D entryPt = oppFace.frame.origin().add(oppFace.frame.u().multiply(h.getU())).add(oppFace.frame.v().multiply(h.getV()));
                Point3D hc = entryPt.subtract(oppFace.frame.n().multiply(cf.thick));
                Point3D toExit = hc.subtract(cf.frame.origin());
                locals.add(new LocalHole(h, toExit.dotProduct(cf.frame.u()), toExit.dotProduct(cf.frame.v()), hc));
            }
        }
        partitionBox(new Box(-cf.fw * 0.5, cf.fw * 0.5, -cf.fh * 0.5, cf.fh * 0.5), locals, cf, pts, fcs, isExit);
    }

    private static void partitionBox(Box b, List<LocalHole> holes, CuboidFace cf,
                                     List<Float> pts, List<Integer> fcs, boolean isExit) {
        if (holes == null || holes.isEmpty()) return;
        if (holes.size() == 1) {
            LocalHole lh = holes.get(0);
            Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(cf.frame, b.uMin, b.uMax, b.vMin, b.vMax);
            if (!isExit) {
                Point3D[] entry = hole_mesh_triangulator_ui_main.getCirclePoints(lh.center, cf.frame.u(), cf.frame.v(), lh.h.getOuterRadius(), SEGS);
                hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, entry, pts, fcs, false);
                buildCavity(cf.frame, cf.thick, lh.h, lh.center, entry, pts, fcs);
            } else {
                Point3D[] exit = hole_mesh_triangulator_ui_main.getCirclePoints(lh.center, cf.frame.u(), cf.frame.v(), lh.h.getRadius(), SEGS);
                hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, exit, pts, fcs, true);
            }
            return;
        }

        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (LocalHole lh : holes) {
            minU = Math.min(minU, lh.u); maxU = Math.max(maxU, lh.u); minV = Math.min(minV, lh.v); maxV = Math.max(maxV, lh.v);
        }
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
        if (splitU) {
            double splitVal = (holes.get(bestK - 1).u + holes.get(bestK).u) * 0.5;
            partitionBox(new Box(b.uMin, splitVal, b.vMin, b.vMax), left, cf, pts, fcs, isExit);
            partitionBox(new Box(splitVal, b.uMax, b.vMin, b.vMax), right, cf, pts, fcs, isExit);
        } else {
            double splitVal = (holes.get(bestK - 1).v + holes.get(bestK).v) * 0.5;
            partitionBox(new Box(b.uMin, b.uMax, b.vMin, splitVal), left, cf, pts, fcs, isExit);
            partitionBox(new Box(b.uMin, b.uMax, splitVal, b.vMax), right, cf, pts, fcs, isExit);
        }
    }

    private static void buildCavity(Frame f, double depth, hole_feature_ui_main h, Point3D hc,
                                    Point3D[] entry, List<Float> pts, List<Integer> fcs) {
        double rBore = h.getRadius(), totalDepth = h.isThroughAll() ? depth : Math.min(depth, h.getDepth());
        switch (h.getHoleType()) {
            case COUNTERSINK -> {
                double coneDepth = Math.min(totalDepth, h.getConeDepth());
                Point3D transCenter = hc.subtract(f.n().multiply(coneDepth));
                Point3D[] transCirc = hole_mesh_triangulator_ui_main.getCirclePoints(transCenter, f.u(), f.v(), rBore, SEGS);
                hole_advanced_mesh_helper_ui_main.buildFrustumWall(entry, transCirc, pts, fcs);
                Point3D endCenter = hc.subtract(f.n().multiply(totalDepth));
                Point3D[] boreEnd = hole_mesh_triangulator_ui_main.getCirclePoints(endCenter, f.u(), f.v(), rBore, SEGS);
                hole_mesh_triangulator_ui_main.buildCylindricalWall(transCirc, boreEnd, pts, fcs);
                if (!h.isThroughAll()) hole_mesh_triangulator_ui_main.buildCircleCap(endCenter, boreEnd, pts, fcs, true);
            }
            case COUNTERBORE -> {
                double cbDepth = Math.min(totalDepth, h.getCbDepth());
                Point3D recessCenter = hc.subtract(f.n().multiply(cbDepth));
                Point3D[] recessBottom = hole_mesh_triangulator_ui_main.getCirclePoints(recessCenter, f.u(), f.v(), h.getOuterRadius(), SEGS);
                hole_mesh_triangulator_ui_main.buildCylindricalWall(entry, recessBottom, pts, fcs);
                Point3D[] shoulderInner = hole_mesh_triangulator_ui_main.getCirclePoints(recessCenter, f.u(), f.v(), rBore, SEGS);
                hole_advanced_mesh_helper_ui_main.buildPlanarAnnulus(recessBottom, shoulderInner, pts, fcs);
                Point3D endCenter = hc.subtract(f.n().multiply(totalDepth));
                Point3D[] boreEnd = hole_mesh_triangulator_ui_main.getCirclePoints(endCenter, f.u(), f.v(), rBore, SEGS);
                hole_mesh_triangulator_ui_main.buildCylindricalWall(shoulderInner, boreEnd, pts, fcs);
                if (!h.isThroughAll()) hole_mesh_triangulator_ui_main.buildCircleCap(endCenter, boreEnd, pts, fcs, true);
            }
            default -> {
                Point3D endCenter = hc.subtract(f.n().multiply(totalDepth));
                Point3D[] boreEnd = hole_mesh_triangulator_ui_main.getCirclePoints(endCenter, f.u(), f.v(), rBore, SEGS);
                hole_mesh_triangulator_ui_main.buildCylindricalWall(entry, boreEnd, pts, fcs);
                if (!h.isThroughAll()) hole_mesh_triangulator_ui_main.buildCircleCap(endCenter, boreEnd, pts, fcs, true);
            }
        }
    }

    private static void buildStandardQuad(Frame f, double fw, double fh, List<Float> pts, List<Integer> fcs) {
        Point3D uH = f.u().multiply(fw * 0.5), vH = f.v().multiply(fh * 0.5);
        hole_mesh_triangulator_ui_main.addQuad(f.origin().subtract(uH).subtract(vH), f.origin().add(uH).subtract(vH),
                f.origin().add(uH).add(vH), f.origin().subtract(uH).add(vH), pts, fcs);
    }

    private static List<hole_feature_ui_main> findHoles(List<hole_feature_ui_main> holes, face_kind_ui_main face, double w, double h) {
        List<hole_feature_ui_main> res = new ArrayList<>();
        if (holes == null) return res;
        for (hole_feature_ui_main x : holes) if (x != null && x.getFaceKind() == face && x.isValid() && x.fitsWithinFace(w, h)) res.add(x);
        return res;
    }

    private static List<hole_feature_ui_main> findThroughHoles(List<hole_feature_ui_main> holes, face_kind_ui_main opp, double w, double h) {
        List<hole_feature_ui_main> res = new ArrayList<>();
        if (holes == null) return res;
        for (hole_feature_ui_main x : holes) if (x != null && x.getFaceKind() == opp && x.isValid() && x.isThroughAll() && x.fitsWithinFace(w, h)) res.add(x);
        return res;
    }

    private static CuboidFace getOppFace(CuboidFace[] faces, face_kind_ui_main kind) {
        for (CuboidFace cf : faces) if (cf.kind == kind) return cf;
        return faces[0];
    }
}
