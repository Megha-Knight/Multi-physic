package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.util.List;

/**
 * hole_intersection_helper_ui_main.java
 * High-precision 3D void collision and cavity trimming helper for intersecting holes.
 * Trims internal walls and end caps where cross-drilled and opposing holes meet inside solids.
 */
public final class hole_intersection_helper_ui_main {

    private hole_intersection_helper_ui_main() {}

    public static boolean isOpposingFace(face_kind_ui_main f1, face_kind_ui_main f2) {
        if (f1 == null || f2 == null) return false;
        return (f1 == face_kind_ui_main.TOP && f2 == face_kind_ui_main.BOTTOM) ||
               (f1 == face_kind_ui_main.BOTTOM && f2 == face_kind_ui_main.TOP) ||
               (f1 == face_kind_ui_main.FRONT && f2 == face_kind_ui_main.BACK) ||
               (f1 == face_kind_ui_main.BACK && f2 == face_kind_ui_main.FRONT) ||
               (f1 == face_kind_ui_main.LEFT && f2 == face_kind_ui_main.RIGHT) ||
               (f1 == face_kind_ui_main.RIGHT && f2 == face_kind_ui_main.LEFT) ||
               (f1 == face_kind_ui_main.TOP_CAP && f2 == face_kind_ui_main.BOTTOM_CAP) ||
               (f1 == face_kind_ui_main.BOTTOM_CAP && f2 == face_kind_ui_main.TOP_CAP);
    }

    public static double getEffectiveDepth(hole_feature_ui_main cur, List<hole_feature_ui_main> allHoles,
                                           Point3D center, double w, double hDim, double d) {
        if (cur == null) return 0.0;
        double thick = hole_mesh_triangulator_ui_main.getFaceThickness(w, hDim, d, cur.getFaceKind());
        double curDepth = cur.isThroughAll() ? thick : Math.min(thick, cur.getDepth());
        if (allHoles == null || center == null) return curDepth;

        Frame fCur = hole_mesh_triangulator_ui_main.getCuboidFaceFrame(center, w, hDim, d, cur.getFaceKind());
        Point3D hcCur = fCur.origin().add(fCur.u().multiply(cur.getU())).add(fCur.v().multiply(cur.getV()));
        Point3D dirCur = fCur.n().multiply(-1.0);

        for (hole_feature_ui_main other : allHoles) {
            if (other == null || other == cur || other.getId().equals(cur.getId())) continue;
            if (!isOpposingFace(cur.getFaceKind(), other.getFaceKind())) continue;

            Frame fOth = hole_mesh_triangulator_ui_main.getCuboidFaceFrame(center, w, hDim, d, other.getFaceKind());
            Point3D hcOth = fOth.origin().add(fOth.u().multiply(other.getU())).add(fOth.v().multiply(other.getV()));
            Point3D delta = hcOth.subtract(hcCur);
            Point3D perp = delta.subtract(dirCur.multiply(delta.dotProduct(dirCur)));
            if (perp.magnitude() > 0.5) continue;

            double othDepth = other.isThroughAll() ? thick : Math.min(thick, other.getDepth());
            if (curDepth + othDepth < thick - 1e-4) continue;

            double rCur = cur.getRadius(), rOth = other.getRadius();
            if (rCur < rOth - 0.1 || (Math.abs(rCur - rOth) <= 0.1 && cur.getId().compareTo(other.getId()) > 0)) {
                curDepth = Math.min(curDepth, Math.max(0.0, thick - othDepth));
            }
        }
        return curDepth;
    }

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
        return r <= (holeR + 1e-2);
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
        if (H < 1e-4) return;
        if (!hasOthers) {
            Point3D[] r1 = hole_profile_helper_ui_main.getProfilePoints(c1, u, v, cur, radius, segs);
            Point3D[] r2 = hole_profile_helper_ui_main.getProfilePoints(c2, u, v, cur, radius, segs);
            hole_mesh_triangulator_ui_main.buildCylindricalWall(r1, r2, pts, fcs);
            return;
        }

        int K = Math.max(4, (int) Math.ceil(H / 0.75));
        int[][] vIdx = new int[K + 1][segs];
        Point3D[][] rings = new Point3D[K + 1][segs];
        for (int j = 0; j <= K; j++) {
            double t = (double) j / K;
            Point3D cj = c1.multiply(1.0 - t).add(c2.multiply(t));
            rings[j] = hole_profile_helper_ui_main.getProfilePoints(cj, u, v, cur, radius, segs);
            for (int i = 0; i < segs; i++) vIdx[j][i] = hole_mesh_triangulator_ui_main.addVertex(rings[j][i], pts);
        }

        for (int j = 0; j < K; j++) {
            for (int i = 0; i < segs; i++) {
                int next = (i + 1) % segs;
                Point3D p0 = rings[j][i], p1 = rings[j][next], p2 = rings[j + 1][next], p3 = rings[j + 1][i];
                Point3D midA = p0.add(p1).add(p2).multiply(1.0 / 3.0);
                if (!isInsideAnyOtherHole(midA, allHoles, cur, center, w, hDim, d)) {
                    hole_mesh_triangulator_ui_main.addTriIdx(vIdx[j][i], vIdx[j][next], vIdx[j + 1][next], fcs, false);
                }
                Point3D midB = p0.add(p2).add(p3).multiply(1.0 / 3.0);
                if (!isInsideAnyOtherHole(midB, allHoles, cur, center, w, hDim, d)) {
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

    public static void buildCavityEndCapTrimmed(Point3D center, Point3D u, Point3D v, hole_feature_ui_main h, double rBore,
                                                List<hole_feature_ui_main> allHoles, Point3D shapeCenter,
                                                double w, double hDim, double d, List<Float> pts, List<Integer> fcs) {
        if (h == null || h.isThroughAll()) return;
        int segs = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;
        boolean hasOthers = (allHoles != null && allHoles.size() > 1 && shapeCenter != null);
        if (!hasOthers) {
            Point3D[] boreEnd = hole_profile_helper_ui_main.getProfilePoints(center, u, v, h, rBore, segs);
            hole_mesh_triangulator_ui_main.buildCircleCap(center, boreEnd, pts, fcs, true);
            return;
        }
        int nRings = 6;
        Point3D[][] ringPts = new Point3D[nRings + 1][segs];
        double rBase = (rBore > 0 ? rBore : h.getRadius());
        for (int m = 1; m <= nRings; m++) {
            ringPts[m] = hole_profile_helper_ui_main.getProfilePoints(center, u, v, h, rBase * ((double) m / nRings), segs);
        }
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs;
            Point3D mid0 = center.add(ringPts[1][next]).add(ringPts[1][i]).multiply(1.0 / 3.0);
            if (!isInsideAnyOtherHole(mid0, allHoles, h, shapeCenter, w, hDim, d)) {
                hole_mesh_triangulator_ui_main.addTri(center, ringPts[1][next], ringPts[1][i], pts, fcs, false);
            }
        }
        for (int m = 1; m < nRings; m++) {
            for (int i = 0; i < segs; i++) {
                int next = (i + 1) % segs;
                Point3D a = ringPts[m][i], b = ringPts[m][next], c = ringPts[m + 1][next], pD = ringPts[m + 1][i];
                Point3D mid = a.add(b).add(c).add(pD).multiply(0.25);
                if (!isInsideAnyOtherHole(mid, allHoles, h, shapeCenter, w, hDim, d)) {
                    hole_mesh_triangulator_ui_main.addTri(a, c, pD, pts, fcs, false);
                    hole_mesh_triangulator_ui_main.addTri(a, b, c, pts, fcs, false);
                }
            }
        }
    }
}
