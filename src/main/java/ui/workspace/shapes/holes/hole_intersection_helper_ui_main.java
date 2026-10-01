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

    public static double[] getIntersectionInterval(Point3D hc, Point3D drillDir, Point3D rayRadial,
                                                   List<hole_feature_ui_main> allHoles, hole_feature_ui_main cur,
                                                   Point3D center, double w, double hDim, double d) {
        if (allHoles == null || center == null) return null;
        double bestT1 = Double.MAX_VALUE, bestT2 = -Double.MAX_VALUE;
        boolean found = false;

        for (hole_feature_ui_main other : allHoles) {
            if (other == null || other == cur || other.getId().equals(cur.getId())) continue;
            Frame fOther = hole_mesh_triangulator_ui_main.getCuboidFaceFrame(center, w, hDim, d, other.getFaceKind());
            Point3D hcOther = fOther.origin().add(fOther.u().multiply(other.getU())).add(fOther.v().multiply(other.getV()));
            Point3D otherDir = fOther.n().multiply(-1.0);
            double otherR = other.getRadius(), thick = hole_mesh_triangulator_ui_main.getFaceThickness(w, hDim, d, other.getFaceKind());
            double otherDepth = other.isThroughAll() ? thick : Math.min(thick, other.getDepth());

            Point3D w0 = hc.add(rayRadial).subtract(hcOther);
            double dDotO = drillDir.dotProduct(otherDir), wDotO = w0.dotProduct(otherDir);
            Point3D dPerp = drillDir.subtract(otherDir.multiply(dDotO)), wPerp = w0.subtract(otherDir.multiply(wDotO));

            double A = dPerp.dotProduct(dPerp);
            if (A < 1e-6) continue;
            double B = wPerp.dotProduct(dPerp), C = wPerp.dotProduct(wPerp) - otherR * otherR;
            double disc = B * B - A * C;
            if (disc < 0) continue;

            double sqrtDisc = Math.sqrt(disc), t1 = (-B - sqrtDisc) / A, t2 = (-B + sqrtDisc) / A;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }

            double lam1 = wDotO + t1 * dDotO, lam2 = wDotO + t2 * dDotO;
            if ((lam1 < -0.1 && lam2 < -0.1) || (lam1 > otherDepth + 0.1 && lam2 > otherDepth + 0.1)) continue;

            bestT1 = Math.min(bestT1, t1);
            bestT2 = Math.max(bestT2, t2);
            found = true;
        }
        return found ? new double[]{ bestT1, bestT2 } : null;
    }

    public static double[] getIntersectionInterval(Point3D hc, Point3D drillDir, Point3D rayRadial,
                                                   List<hole_feature_ui_main> allHoles, hole_feature_ui_main cur,
                                                   Point3D cubeCenter, double s) {
        return getIntersectionInterval(hc, drillDir, rayRadial, allHoles, cur, cubeCenter, s, s, s);
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

        Point3D drillDir = c2.subtract(c1).normalize();
        double[] tA = new double[segs], tB = new double[segs];
        Point3D[] rVec = new Point3D[segs];
        boolean anyInter = false;

        for (int i = 0; i < segs; i++) {
            double angle = i * 2.0 * Math.PI / segs;
            rVec[i] = u.multiply(radius * Math.cos(angle)).add(v.multiply(radius * Math.sin(angle)));
            double[] inter = getIntersectionInterval(c1, drillDir, rVec[i], allHoles, cur, center, w, hDim, d);
            if (inter != null && inter[0] < H && inter[1] > 0) {
                tA[i] = Math.max(0.0, Math.min(H, inter[0]));
                tB[i] = Math.max(0.0, Math.min(H, inter[1]));
                if (tB[i] > tA[i] + 1e-4) anyInter = true;
            } else {
                tA[i] = H; tB[i] = H;
            }
        }

        if (!anyInter) {
            Point3D[] r1 = hole_mesh_triangulator_ui_main.getCirclePoints(c1, u, v, radius, segs);
            Point3D[] r2 = hole_mesh_triangulator_ui_main.getCirclePoints(c2, u, v, radius, segs);
            hole_mesh_triangulator_ui_main.buildCylindricalWall(r1, r2, pts, fcs);
            return;
        }

        int[] topIdx = new int[segs], cutA_Idx = new int[segs], cutB_Idx = new int[segs], botIdx = new int[segs];
        for (int i = 0; i < segs; i++) {
            topIdx[i] = hole_mesh_triangulator_ui_main.addVertex(c1.add(rVec[i]), pts);
            cutA_Idx[i] = hole_mesh_triangulator_ui_main.addVertex(c1.add(rVec[i]).add(drillDir.multiply(tA[i])), pts);
            cutB_Idx[i] = hole_mesh_triangulator_ui_main.addVertex(c1.add(rVec[i]).add(drillDir.multiply(tB[i])), pts);
            botIdx[i] = hole_mesh_triangulator_ui_main.addVertex(c1.add(rVec[i]).add(drillDir.multiply(H)), pts);
        }

        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs;
            if (tA[i] > 1e-3 || tA[next] > 1e-3) {
                hole_mesh_triangulator_ui_main.addTriIdx(topIdx[i], topIdx[next], cutA_Idx[next], fcs, false);
                hole_mesh_triangulator_ui_main.addTriIdx(topIdx[i], cutA_Idx[next], cutA_Idx[i], fcs, false);
            }
            if (tB[i] < H - 1e-3 || tB[next] < H - 1e-3) {
                hole_mesh_triangulator_ui_main.addTriIdx(cutB_Idx[i], cutB_Idx[next], botIdx[next], fcs, false);
                hole_mesh_triangulator_ui_main.addTriIdx(cutB_Idx[i], botIdx[next], botIdx[i], fcs, false);
            }
        }
    }

    public static void buildCylindricalWallTrimmed(Point3D c1, Point3D c2, Point3D u, Point3D v, double radius,
                                                   List<hole_feature_ui_main> allHoles, hole_feature_ui_main cur,
                                                   Point3D cubeCenter, double s, List<Float> pts, List<Integer> fcs) {
        buildCylindricalWallTrimmed(c1, c2, u, v, radius, allHoles, cur, cubeCenter, s, s, s, pts, fcs);
    }
}
