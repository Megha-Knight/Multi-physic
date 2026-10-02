package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;

import java.util.List;

/**
 * hole_mesh_triangulator_ui_main.java
 * High-precision triangulation utilities for annular polygonal faces and cylindrical cavities.
 */
public final class hole_mesh_triangulator_ui_main {

    public static final int CIRCLE_SEGS = 64;

    private hole_mesh_triangulator_ui_main() {}

    public record Frame(Point3D origin, Point3D n, Point3D u, Point3D v) {}

    public static boolean isOpposite(face_kind_ui_main f1, face_kind_ui_main f2) {
        if (f1 == null || f2 == null) return false;
        return (f1 == face_kind_ui_main.TOP && f2 == face_kind_ui_main.BOTTOM) || (f1 == face_kind_ui_main.BOTTOM && f2 == face_kind_ui_main.TOP) ||
               (f1 == face_kind_ui_main.FRONT && f2 == face_kind_ui_main.BACK) || (f1 == face_kind_ui_main.BACK && f2 == face_kind_ui_main.FRONT) ||
               (f1 == face_kind_ui_main.RIGHT && f2 == face_kind_ui_main.LEFT) || (f1 == face_kind_ui_main.LEFT && f2 == face_kind_ui_main.RIGHT);
    }

    public static Frame getCuboidFaceFrame(Point3D c, double w, double h, double d, face_kind_ui_main kind) {
        double hw = w * 0.5, hh = h * 0.5, hd = d * 0.5;
        return switch (kind) {
            case TOP, TOP_CAP       -> new Frame(new Point3D(c.getX(), c.getY() - hh, c.getZ()), new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1));
            case BOTTOM, BOTTOM_CAP -> new Frame(new Point3D(c.getX(), c.getY() + hh, c.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1));
            case FRONT  -> new Frame(new Point3D(c.getX(), c.getY(), c.getZ() + hd), new Point3D(0, 0, 1), new Point3D(1, 0, 0), new Point3D(0, -1, 0));
            case BACK   -> new Frame(new Point3D(c.getX(), c.getY(), c.getZ() - hd), new Point3D(0, 0, -1), new Point3D(-1, 0, 0), new Point3D(0, -1, 0));
            case RIGHT  -> new Frame(new Point3D(c.getX() + hw, c.getY(), c.getZ()), new Point3D(1, 0, 0), new Point3D(0, 0, -1), new Point3D(0, -1, 0));
            case LEFT   -> new Frame(new Point3D(c.getX() - hw, c.getY(), c.getZ()), new Point3D(-1, 0, 0), new Point3D(0, 0, 1), new Point3D(0, -1, 0));
            default     -> new Frame(c, new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1));
        };
    }

    public static Frame getCubeFaceFrame(Point3D c, double s, face_kind_ui_main kind) {
        return getCuboidFaceFrame(c, s, s, s, kind);
    }

    public static double getFaceThickness(double w, double h, double d, face_kind_ui_main kind) {
        if (kind == null) return h;
        return (kind == face_kind_ui_main.TOP || kind == face_kind_ui_main.BOTTOM || kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.BOTTOM_CAP) ? h
             : (kind == face_kind_ui_main.FRONT || kind == face_kind_ui_main.BACK) ? d : w;
    }

    public static Point3D computeExitPoint(Frame fEntry, double u, double v, double thickness) {
        Point3D p = fEntry.origin().add(fEntry.u().multiply(u)).add(fEntry.v().multiply(v));
        return p.subtract(fEntry.n().multiply(thickness));
    }

    public static double[] computeExitUV(Frame fExit, Frame fEntry, double u, double v, double thickness) {
        Point3D exitP = computeExitPoint(fEntry, u, v, thickness);
        Point3D d = exitP.subtract(fExit.origin());
        return new double[]{ d.dotProduct(fExit.u()), d.dotProduct(fExit.v()) };
    }

    public static Point3D[] getOctagonalPerimeter(Frame f, double uMin, double uMax, double vMin, double vMax) {
        double uMid = (uMin + uMax) * 0.5, vMid = (vMin + vMax) * 0.5;
        Point3D c = f.origin, u = f.u, v = f.v;
        return new Point3D[]{
            c.add(u.multiply(uMax)).add(v.multiply(vMid)),
            c.add(u.multiply(uMax)).add(v.multiply(vMax)),
            c.add(u.multiply(uMid)).add(v.multiply(vMax)),
            c.add(u.multiply(uMin)).add(v.multiply(vMax)),
            c.add(u.multiply(uMin)).add(v.multiply(vMid)),
            c.add(u.multiply(uMin)).add(v.multiply(vMin)),
            c.add(u.multiply(uMid)).add(v.multiply(vMin)),
            c.add(u.multiply(uMax)).add(v.multiply(vMin))
        };
    }

    public static Point3D[] getCirclePoints(Point3D center, Point3D u, Point3D v, double r, int segs) {
        Point3D[] circle = new Point3D[segs];
        for (int i = 0; i < segs; i++) {
            double a = i * 2.0 * Math.PI / segs;
            circle[i] = center.add(u.multiply(r * Math.cos(a))).add(v.multiply(r * Math.sin(a)));
        }
        return circle;
    }

    public static void triangulateAnnularFace(Point3D[] oct, Point3D[] circ, List<Float> pts, List<Integer> fcs, boolean reverse) {
        int n = circ.length;
        Point3D c = circ[0].add(circ[n / 2]).multiply(0.5);
        Point3D u = circ[0].subtract(c).normalize(), v = circ[n / 4].subtract(c).normalize();
        double[] phi = new double[9];
        for (int k = 0; k < 8; k++) {
            Point3D d = oct[k].subtract(c);
            double a = Math.atan2(d.dotProduct(v), d.dotProduct(u));
            phi[k] = (a < 0) ? (a + 2.0 * Math.PI) : a;
        }
        for (int k = 1; k < 8; k++) {
            while (phi[k] < phi[k - 1]) phi[k] += 2.0 * Math.PI;
        }
        phi[8] = phi[0] + 2.0 * Math.PI;
        while (phi[8] < phi[7]) phi[8] += 2.0 * Math.PI;

        int[] idx = new int[9];
        for (int k = 0; k < 8; k++) {
            idx[k] = (int) Math.round((phi[k] - phi[0]) / (2.0 * Math.PI) * n);
        }
        idx[8] = n;
        for (int k = 1; k < 8; k++) {
            if (idx[k] < idx[k - 1]) idx[k] = idx[k - 1];
            if (idx[k] > n) idx[k] = n;
        }
        int base = (int) Math.round(phi[0] / (2.0 * Math.PI) * n) % n;

        for (int k = 0; k < 8; k++) {
            Point3D o1 = oct[k], o2 = oct[(k + 1) % 8];
            int iStart = idx[k], iEnd = idx[k + 1];
            addTri(o1, o2, circ[(base + iEnd) % n], pts, fcs, reverse);
            for (int m = iStart; m < iEnd; m++) {
                addTri(o1, circ[(base + m + 1) % n], circ[(base + m) % n], pts, fcs, reverse);
            }
        }
    }

    public static void buildCylindricalWall(Point3D[] top, Point3D[] bot, List<Float> pts, List<Integer> fcs) {
        int segs = top.length;
        int[] topIdx = new int[segs], botIdx = new int[segs];
        for (int i = 0; i < segs; i++) {
            topIdx[i] = addVertex(top[i], pts);
            botIdx[i] = addVertex(bot[i], pts);
        }
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs;
            addTriIdx(topIdx[i], topIdx[next], botIdx[next], fcs, false);
            addTriIdx(topIdx[i], botIdx[next], botIdx[i], fcs, false);
        }
    }

    public static void addTriIdx(int i0, int i1, int i2, List<Integer> fcs, boolean rev) {
        fcs.add(rev ? i2 : i0); fcs.add(0);
        fcs.add(i1);            fcs.add(0);
        fcs.add(rev ? i0 : i2); fcs.add(0);
    }

    public static void buildCircleCap(Point3D center, Point3D[] circ, List<Float> pts, List<Integer> fcs, boolean facingEntry) {
        int segs = circ.length;
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs;
            addTri(center, circ[next], circ[i], pts, fcs, !facingEntry);
        }
    }

    public static void addQuad(Point3D p0, Point3D p1, Point3D p2, Point3D p3, List<Float> pts, List<Integer> fcs) {
        addTri(p0, p1, p2, pts, fcs, false);
        addTri(p0, p2, p3, pts, fcs, false);
    }

    public static void addTri(Point3D a, Point3D b, Point3D c, List<Float> pts, List<Integer> fcs, boolean rev) {
        int i0 = addVertex(rev ? c : a, pts), i1 = addVertex(b, pts), i2 = addVertex(rev ? a : c, pts);
        fcs.add(i0); fcs.add(0); fcs.add(i1); fcs.add(0); fcs.add(i2); fcs.add(0);
    }

    public static int addVertex(Point3D p, List<Float> pts) {
        int idx = pts.size() / 3;
        pts.add((float) p.getX()); pts.add((float) p.getY()); pts.add((float) p.getZ());
        return idx;
    }
}
