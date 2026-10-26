package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;

import java.util.ArrayList;
import java.util.List;

/**
 * box_corner_mesh_helper_ui_main.java
 * Constructs watertight 2-manifold topological corner closures for box machining.
 */
public final class box_corner_mesh_helper_ui_main {

    private box_corner_mesh_helper_ui_main() {}

    public static void addCurve(List<Integer> loop, int[] arc, boolean reverse) {
        if (arc == null || arc.length == 0) return;
        if (!reverse) {
            for (int idx : arc) {
                if (loop.isEmpty() || loop.get(loop.size() - 1) != idx) loop.add(idx);
            }
        } else {
            for (int i = arc.length - 1; i >= 0; i--) {
                int idx = arc[i];
                if (loop.isEmpty() || loop.get(loop.size() - 1) != idx) loop.add(idx);
            }
        }
    }

    public static void closeCorner(List<Float> pts, List<Integer> fcs, Point3D norm,
                                   Point3D nA, Point3D nB, Point3D nC,
                                   int[] a, boolean ra, int[] b, boolean rb, int[] c, boolean rc,
                                   double ma, double mb, double mc, int anA, int anB, int anC) {
        int count = (ma > 1e-4 ? 1 : 0) + (mb > 1e-4 ? 1 : 0) + (mc > 1e-4 ? 1 : 0);
        if (count == 0) return;

        if (count == 1) {
            int[] curve = (ma > 1e-4) ? a : ((mb > 1e-4) ? b : c);
            boolean rev = (ma > 1e-4) ? ra : ((mb > 1e-4) ? rb : rc);
            int anchor = (ma > 1e-4) ? anA : ((mb > 1e-4) ? anB : anC);
            Point3D fn = (ma > 1e-4) ? nA : ((mb > 1e-4) ? nB : nC);
            List<Integer> arc = new ArrayList<>();
            addCurve(arc, curve, rev);
            Point3D pan = getP(pts, anchor);
            for (int i = 0; i < arc.size() - 1; i++) {
                int u = arc.get(i), v = arc.get(i + 1);
                Point3D pu = getP(pts, u), pv = getP(pts, v);
                Point3D cross = pu.subtract(pan).crossProduct(pv.subtract(pan));
                if (cross.dotProduct(fn) >= 0) addTri(fcs, anchor, u, v);
                else addTri(fcs, anchor, v, u);
            }
            return;
        }

        List<Integer> loop = new ArrayList<>();
        addCurve(loop, a, ra);
        addCurve(loop, b, rb);
        addCurve(loop, c, rc);
        addCornerClosure(pts, fcs, loop, norm);
    }

    public static void addCornerClosure(List<Float> pts, List<Integer> fcs, List<Integer> loop, Point3D normalHint) {
        if (loop == null || loop.size() < 3) return;
        List<Integer> clean = new ArrayList<>();
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int idx : loop) {
            if (seen.add(idx)) {
                if (!clean.isEmpty()) {
                    Point3D pPrev = getP(pts, clean.get(clean.size() - 1));
                    Point3D pCur = getP(pts, idx);
                    if (pCur.subtract(pPrev).magnitude() < 1e-4) continue;
                }
                clean.add(idx);
            }
        }
        if (clean.size() > 1) {
            Point3D pFirst = getP(pts, clean.get(0));
            Point3D pLast = getP(pts, clean.get(clean.size() - 1));
            if (pFirst.subtract(pLast).magnitude() < 1e-4) clean.remove(clean.size() - 1);
        }
        int n = clean.size();
        if (n < 3) return;

        if (n == 3) {
            int a = clean.get(0), b = clean.get(1), c = clean.get(2);
            Point3D pa = getP(pts, a), pb = getP(pts, b), pc = getP(pts, c);
            Point3D cross = pb.subtract(pa).crossProduct(pc.subtract(pa));
            if (cross.magnitude() < 1e-5) return;
            if (cross.dotProduct(normalHint) >= 0) addTri(fcs, a, b, c);
            else addTri(fcs, a, c, b);
            return;
        }

        double sx = 0, sy = 0, sz = 0;
        for (int idx : clean) {
            Point3D p = getP(pts, idx);
            sx += p.getX(); sy += p.getY(); sz += p.getZ();
        }
        int hub = addPt(pts, sx / n, sy / n, sz / n);
        Point3D ph = getP(pts, hub);
        Point3D loopNormal = Point3D.ZERO;
        for (int i = 0; i < n; i++) {
            int u = clean.get(i), v = clean.get((i + 1) % n);
            Point3D pu = getP(pts, u), pv = getP(pts, v);
            loopNormal = loopNormal.add(pu.subtract(ph).crossProduct(pv.subtract(ph)));
        }
        boolean flip = (loopNormal.dotProduct(normalHint) < 0);

        for (int i = 0; i < n; i++) {
            int u = flip ? clean.get((n - i) % n) : clean.get(i);
            int v = flip ? clean.get(n - 1 - i) : clean.get((i + 1) % n);
            if (u == v) continue;
            Point3D pu = getP(pts, u), pv = getP(pts, v);
            Point3D cross = pu.subtract(ph).crossProduct(pv.subtract(ph));
            if (cross.magnitude() < 1e-5) continue;
            addTri(fcs, hub, u, v);
        }
    }

    private static Point3D getP(List<Float> pts, int idx) {
        return new Point3D(pts.get(idx * 3), pts.get(idx * 3 + 1), pts.get(idx * 3 + 2));
    }

    private static int addPt(List<Float> pts, double x, double y, double z) {
        int idx = pts.size() / 3;
        pts.add((float) x); pts.add((float) y); pts.add((float) z);
        return idx;
    }

    private static void addTri(List<Integer> fcs, int a, int b, int c) {
        if (a == b || b == c || a == c) return;
        fcs.add(a); fcs.add(0); fcs.add(b); fcs.add(1); fcs.add(c); fcs.add(2);
    }
}
