package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.util.*;

/**
 * hole_pattern_mesh_helper_ui_main.java
 * High-precision meshing engine for multi-hole planar patterns (linear and circular).
 * Uses recursive BSP cell subdivision for box faces and Delaunay triangulation for cylinder caps.
 */
public final class hole_pattern_mesh_helper_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private hole_pattern_mesh_helper_ui_main() {}

    public record Box(double uMin, double uMax, double vMin, double vMax) {}

    public static void partitionAndBuildCubeFace(Frame f, double s, List<hole_feature_ui_main> faceHoles,
                                                 List<Float> pts, List<Integer> fcs, boolean isExit) {
        partitionAndBuildCubeFace(f, s, faceHoles, null, null, pts, fcs, isExit);
    }

    public static void partitionAndBuildCubeFace(Frame f, double s, List<hole_feature_ui_main> faceHoles,
                                                 List<hole_feature_ui_main> allHoles, Point3D cubeCenter,
                                                 List<Float> pts, List<Integer> fcs, boolean isExit) {
        double hw = s * 0.5;
        Box root = new Box(-hw, hw, -hw, hw);
        partitionBox(root, new ArrayList<>(faceHoles), allHoles, cubeCenter, f, s, pts, fcs, isExit);
    }

    private static void partitionBox(Box b, List<hole_feature_ui_main> holes,
                                     List<hole_feature_ui_main> allHoles, Point3D cubeCenter,
                                     Frame f, double s, List<Float> pts, List<Integer> fcs, boolean isExit) {
        if (holes == null || holes.isEmpty()) return;
        if (holes.size() == 1) {
            hole_feature_ui_main h = holes.get(0);
            if (!isExit) {
                double rEntry = h.getOuterRadius();
                Point3D hc = f.origin().add(f.u().multiply(h.getU())).add(f.v().multiply(h.getV()));
                Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, b.uMin, b.uMax, b.vMin, b.vMax);
                Point3D[] entry = hole_mesh_triangulator_ui_main.getCirclePoints(hc, f.u(), f.v(), rEntry, SEGS);
                hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, entry, pts, fcs, false);
                hole_advanced_mesh_helper_ui_main.buildCubeHoleCavity(f, s, h, hc, entry, allHoles, cubeCenter, pts, fcs);
            } else {
                Point3D hc = f.origin().add(f.u().multiply(h.getU())).add(f.v().multiply(-h.getV()));
                Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, b.uMin, b.uMax, b.vMin, b.vMax);
                Point3D[] exit = hole_mesh_triangulator_ui_main.getCirclePoints(hc, f.u(), f.v(), h.getRadius(), SEGS);
                hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, exit, pts, fcs, true);
            }
            return;
        }

        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE;
        double minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (hole_feature_ui_main h : holes) {
            minU = Math.min(minU, h.getU()); maxU = Math.max(maxU, h.getU());
            minV = Math.min(minV, h.getV()); maxV = Math.max(maxV, h.getV());
        }

        boolean splitU = (maxU - minU >= maxV - minV);
        if (splitU && maxU - minU < 1e-4) splitU = false;
        if (!splitU && maxV - minV < 1e-4) splitU = true;

        if (splitU) holes.sort(Comparator.comparingDouble(hole_feature_ui_main::getU));
        else holes.sort(Comparator.comparingDouble(hole_feature_ui_main::getV));

        int bestK = 1; double maxGap = -1;
        for (int k = 1; k < holes.size(); k++) {
            double gap = splitU ? (holes.get(k).getU() - holes.get(k - 1).getU())
                                : (holes.get(k).getV() - holes.get(k - 1).getV());
            if (gap > maxGap) { maxGap = gap; bestK = k; }
        }

        List<hole_feature_ui_main> left = new ArrayList<>(holes.subList(0, bestK));
        List<hole_feature_ui_main> right = new ArrayList<>(holes.subList(bestK, holes.size()));

        if (splitU) {
            double splitVal = (holes.get(bestK - 1).getU() + holes.get(bestK).getU()) * 0.5;
            partitionBox(new Box(b.uMin, splitVal, b.vMin, b.vMax), left, allHoles, cubeCenter, f, s, pts, fcs, isExit);
            partitionBox(new Box(splitVal, b.uMax, b.vMin, b.vMax), right, allHoles, cubeCenter, f, s, pts, fcs, isExit);
        } else {
            double splitVal = (holes.get(bestK - 1).getV() + holes.get(bestK).getV()) * 0.5;
            partitionBox(new Box(b.uMin, b.uMax, b.vMin, splitVal), left, allHoles, cubeCenter, f, s, pts, fcs, isExit);
            partitionBox(new Box(b.uMin, b.uMax, splitVal, b.vMax), right, allHoles, cubeCenter, f, s, pts, fcs, isExit);
        }
    }

    record Pt2D(double x, double z) {}
    record Tri2D(Pt2D a, Pt2D b, Pt2D c) {
        boolean containsInCircumcircle(Pt2D p) {
            double d = 2 * (a.x * (b.z - c.z) + b.x * (c.z - a.z) + c.x * (a.z - b.z));
            if (Math.abs(d) < 1e-9) return false;
            double ux = ((a.x * a.x + a.z * a.z) * (b.z - c.z) + (b.x * b.x + b.z * b.z) * (c.z - a.z) + (c.x * c.x + c.z * c.z) * (a.z - b.z)) / d;
            double uz = ((a.x * a.x + a.z * a.z) * (c.x - b.x) + (b.x * b.x + b.z * b.z) * (a.x - c.x) + (c.x * c.x + c.z * c.z) * (b.x - a.x)) / d;
            double r = Math.hypot(a.x - ux, a.z - uz);
            return Math.hypot(p.x - ux, p.z - uz) < r - 1e-7;
        }
        Pt2D centroid() { return new Pt2D((a.x + b.x + c.x) / 3.0, (a.z + b.z + c.z) / 3.0); }
        boolean shares(Pt2D p1, Pt2D p2, Pt2D p3) {
            return a.equals(p1) || a.equals(p2) || a.equals(p3) ||
                   b.equals(p1) || b.equals(p2) || b.equals(p3) ||
                   c.equals(p1) || c.equals(p2) || c.equals(p3);
        }
    }
    record Edge2D(Pt2D p1, Pt2D p2) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge2D e)) return false;
            return (p1.equals(e.p1) && p2.equals(e.p2)) || (p1.equals(e.p2) && p2.equals(e.p1));
        }
        @Override
        public int hashCode() { return p1.hashCode() + p2.hashCode(); }
    }

    public static void buildCylinderCapMultiHoles(double cx, double cz, double y, double rCyl,
                                                  boolean faceUp, List<hole_feature_ui_main> capHoles,
                                                  boolean isExit, List<Float> pts, List<Integer> fcs) {
        List<Pt2D> points = new ArrayList<>();
        for (int i = 0; i < SEGS; i++) {
            double a = i * 2.0 * Math.PI / SEGS;
            points.add(new Pt2D(cx + rCyl * Math.cos(a), cz + rCyl * Math.sin(a)));
        }
        for (hole_feature_ui_main h : capHoles) {
            double hr = isExit ? h.getRadius() : h.getOuterRadius();
            double hx = cx + h.getU(), hz = cz + (faceUp ? h.getV() : -h.getV());
            for (int i = 0; i < SEGS; i++) {
                double a = i * 2.0 * Math.PI / SEGS;
                points.add(new Pt2D(hx + hr * Math.cos(a), hz + hr * Math.sin(a)));
            }
        }

        List<Tri2D> tris = triangulateDelaunay(points, cx, cz, rCyl, capHoles, faceUp, isExit);
        for (Tri2D t : tris) {
            Point3D pA = new Point3D(t.a.x, y, t.a.z), pB = new Point3D(t.b.x, y, t.b.z), pC = new Point3D(t.c.x, y, t.c.z);
            double cr = (t.b.x - t.a.x) * (t.c.z - t.a.z) - (t.b.z - t.a.z) * (t.c.x - t.a.x);
            boolean ccw = cr > 0;
            hole_mesh_triangulator_ui_main.addTri(pA, pB, pC, pts, fcs, faceUp ? ccw : !ccw);
        }
    }

    private static List<Tri2D> triangulateDelaunay(List<Pt2D> points, double cx, double cz, double rCyl,
                                                  List<hole_feature_ui_main> capHoles, boolean faceUp, boolean isExit) {
        Pt2D s1 = new Pt2D(-10000, -10000), s2 = new Pt2D(10000, -10000), s3 = new Pt2D(0, 10000);
        List<Tri2D> mesh = new ArrayList<>();
        mesh.add(new Tri2D(s1, s2, s3));

        for (Pt2D p : points) {
            List<Tri2D> badTris = new ArrayList<>();
            for (Tri2D t : mesh) if (t.containsInCircumcircle(p)) badTris.add(t);

            Map<Edge2D, Integer> edgeCounts = new HashMap<>();
            for (Tri2D t : badTris) {
                edgeCounts.put(new Edge2D(t.a, t.b), edgeCounts.getOrDefault(new Edge2D(t.a, t.b), 0) + 1);
                edgeCounts.put(new Edge2D(t.b, t.c), edgeCounts.getOrDefault(new Edge2D(t.b, t.c), 0) + 1);
                edgeCounts.put(new Edge2D(t.c, t.a), edgeCounts.getOrDefault(new Edge2D(t.c, t.a), 0) + 1);
            }
            mesh.removeAll(badTris);
            for (Map.Entry<Edge2D, Integer> entry : edgeCounts.entrySet()) {
                if (entry.getValue() == 1) mesh.add(new Tri2D(entry.getKey().p1, entry.getKey().p2, p));
            }
        }
        mesh.removeIf(t -> t.shares(s1, s2, s3));

        List<Tri2D> valid = new ArrayList<>();
        for (Tri2D t : mesh) {
            Pt2D c = t.centroid();
            if (Math.hypot(c.x - cx, c.z - cz) > rCyl - 1e-4) continue;
            boolean inHole = false;
            for (hole_feature_ui_main h : capHoles) {
                double hr = isExit ? h.getRadius() : h.getOuterRadius();
                double hx = cx + h.getU(), hz = cz + (faceUp ? h.getV() : -h.getV());
                if (Math.hypot(c.x - hx, c.z - hz) < hr - 1e-4) { inHole = true; break; }
            }
            if (!inHole) valid.add(t);
        }
        return valid;
    }
}
