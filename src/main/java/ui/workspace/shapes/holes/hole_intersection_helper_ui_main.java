package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.util.List;

/**
 * hole_intersection_helper_ui_main.java
 * High-precision 3D void collision and cavity trimming helper for intersecting holes.
 * Trims internal cylindrical walls where cross-drilled holes intersect inside solids.
 */
public final class hole_intersection_helper_ui_main {

    private hole_intersection_helper_ui_main() {}

    public static boolean isInsideHole(Point3D p, hole_feature_ui_main h, Point3D center, double w, double hDim, double d) {
        if (h == null || center == null) return false;
        Frame f = hole_mesh_triangulator_ui_main.getCuboidFaceFrame(center, w, hDim, d, h.getFaceKind());
        Point3D hc = f.origin().add(f.u().multiply(h.getU())).add(f.v().multiply(h.getV()));
        Point3D drillDir = f.n().multiply(-1.0);
        double thick = hole_mesh_triangulator_ui_main.getFaceThickness(w, hDim, d, h.getFaceKind());
        double totalDepth = h.isThroughAll() ? thick : Math.min(thick, h.getDepth());

        Point3D v = p.subtract(hc);
        double t = v.dotProduct(drillDir);
        if (t < -0.1 || t > totalDepth + 0.1) return false;

        Point3D vPerp = v.subtract(drillDir.multiply(t));
        if (h.getCutoutShape() != hole_feature_ui_main.CutoutShape.CIRCLE) {
            double du = vPerp.dotProduct(f.u()), dv = vPerp.dotProduct(f.v());
            return hole_profile_helper_ui_main.isInside2DProfile(du, dv, h);
        }
        double r = vPerp.magnitude(), holeR = h.getRadius();
        if (h.getHoleType() == HoleType.COUNTERSINK) {
            double cd = Math.min(totalDepth, h.getConeDepth());
            if (t <= cd && cd > 1e-4) holeR = h.getOuterRadius() - (t / cd) * (h.getOuterRadius() - h.getRadius());
        } else if (h.getHoleType() == HoleType.COUNTERBORE) {
            if (t <= Math.min(totalDepth, h.getCbDepth())) holeR = h.getOuterRadius();
        }
        return r < (holeR - 0.05);
    }

    public static boolean isInsideHole(Point3D p, hole_feature_ui_main h, Point3D cubeCenter, double s) {
        return isInsideHole(p, h, cubeCenter, s, s, s);
    }

    public static boolean isInsideAnyOtherHole(Point3D p, List<hole_feature_ui_main> allHoles,
                                               hole_feature_ui_main cur, Point3D center, double w, double hDim, double d) {
        if (allHoles == null || center == null) return false;
        for (hole_feature_ui_main other : allHoles) {
            if (other != null && other != cur && !other.getId().equals(cur.getId())) {
                if (isInsideHole(p, other, center, w, hDim, d)) return true;
            }
        }
        return false;
    }

    public static boolean isInsideAnyOtherHole(Point3D p, List<hole_feature_ui_main> allHoles,
                                               hole_feature_ui_main cur, Point3D cubeCenter, double s) {
        return isInsideAnyOtherHole(p, allHoles, cur, cubeCenter, s, s, s);
    }

    public static void buildCylindricalWallTrimmed(Point3D c1, Point3D c2, Point3D u, Point3D v, double radius,
                                                   List<hole_feature_ui_main> allHoles, hole_feature_ui_main cur,
                                                   Point3D center, double w, double hDim, double d,
                                                   List<Float> pts, List<Integer> fcs) {
        int segs = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;
        double H = c1.distance(c2);
        boolean hasOthers = (allHoles != null && allHoles.size() > 1 && center != null);
        if (!hasOthers || H < 1e-4) {
            Point3D[] r1 = hole_mesh_triangulator_ui_main.getCirclePoints(c1, u, v, radius, segs);
            Point3D[] r2 = hole_mesh_triangulator_ui_main.getCirclePoints(c2, u, v, radius, segs);
            hole_mesh_triangulator_ui_main.buildCylindricalWall(r1, r2, pts, fcs);
            return;
        }

        int K = Math.max(2, (int) Math.ceil(H / 1.5));
        int[][] vIdx = new int[K + 1][segs];
        Point3D[][] rings = new Point3D[K + 1][segs];
        for (int j = 0; j <= K; j++) {
            double t = (double) j / K;
            Point3D cj = c1.multiply(1.0 - t).add(c2.multiply(t));
            rings[j] = hole_mesh_triangulator_ui_main.getCirclePoints(cj, u, v, radius, segs);
            for (int i = 0; i < segs; i++) {
                vIdx[j][i] = hole_mesh_triangulator_ui_main.addVertex(rings[j][i], pts);
            }
        }

        for (int j = 0; j < K; j++) {
            for (int i = 0; i < segs; i++) {
                int next = (i + 1) % segs;
                Point3D p00 = rings[j][i], p01 = rings[j][next];
                Point3D p11 = rings[j + 1][next], p10 = rings[j + 1][i];
                Point3D mid = (p00.add(p01).add(p11).add(p10)).multiply(0.25);
                if (!isInsideAnyOtherHole(mid, allHoles, cur, center, w, hDim, d)) {
                    hole_mesh_triangulator_ui_main.addTriIdx(vIdx[j][i], vIdx[j][next], vIdx[j + 1][next], fcs, false);
                    hole_mesh_triangulator_ui_main.addTriIdx(vIdx[j][i], vIdx[j + 1][next], vIdx[j + 1][i], fcs, false);
                }
            }
        }
    }

    public static void buildCylindricalWallTrimmed(Point3D c1, Point3D c2, Point3D u, Point3D v, double radius,
                                                   List<hole_feature_ui_main> allHoles, hole_feature_ui_main cur,
                                                   Point3D cubeCenter, double s, List<Float> pts, List<Integer> fcs) {
        buildCylindricalWallTrimmed(c1, c2, u, v, radius, allHoles, cur, cubeCenter, s, s, s, pts, fcs);
    }
}
