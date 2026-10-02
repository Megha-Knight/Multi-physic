package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.util.ArrayList;
import java.util.List;

/**
 * hole_stepped_helper_ui_main.java
 * High-precision geometry generator for coaxial stepped holes and multi-tier concentric bores.
 */
public final class hole_stepped_helper_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private hole_stepped_helper_ui_main() {}

    public record Step(double radius, double depth) {}

    public static final class HoleCluster {
        public final double u, v;
        public final Point3D center;
        public final List<hole_feature_ui_main> faceHoles = new ArrayList<>();
        public final List<hole_feature_ui_main> oppHoles = new ArrayList<>();
        public final List<hole_feature_ui_main> holes = faceHoles;
        public hole_feature_ui_main primaryHole;
        public double cutoutRadius;

        public HoleCluster(double u, double v, Point3D center) {
            this.u = u; this.v = v; this.center = center;
        }

        public HoleCluster(double u, double v, Point3D center, hole_feature_ui_main h, double r, boolean isExit) {
            this(u, v, center); this.primaryHole = h; this.cutoutRadius = r;
            if (isExit) oppHoles.add(h); else faceHoles.add(h);
        }
        public void addEntry(hole_feature_ui_main h) {
            faceHoles.add(h);
            if (h.getOuterRadius() > cutoutRadius) { cutoutRadius = h.getOuterRadius(); primaryHole = h; }
        }
        public void addExit(hole_feature_ui_main h) {
            oppHoles.add(h);
            if (h.getRadius() > cutoutRadius) { cutoutRadius = h.getRadius(); primaryHole = h; }
        }
        public boolean hasEntry() { return !faceHoles.isEmpty(); }
        public boolean isExitOnly() { return faceHoles.isEmpty(); }
        public double getMaxOuterRadius() { return cutoutRadius; }
        public hole_feature_ui_main getPrimaryHole() { return primaryHole; }
        public List<hole_feature_ui_main> getHoles() { return faceHoles; }
    }

    public static List<HoleCluster> clusterFaceHoles(Frame f, Frame fOpp, double thick,
                                                     List<hole_feature_ui_main> faceHoles,
                                                     List<hole_feature_ui_main> oppHoles) {
        List<HoleCluster> clusters = new ArrayList<>();
        if (faceHoles != null) for (hole_feature_ui_main h : faceHoles) {
            double u = h.getU(), v = h.getV(); HoleCluster m = findMatch(clusters, u, v);
            if (m == null) {
                Point3D hc = f.origin().add(f.u().multiply(u)).add(f.v().multiply(v));
                clusters.add(new HoleCluster(u, v, hc, h, h.getOuterRadius(), false));
            } else m.addEntry(h);
        }
        if (oppHoles != null && fOpp != null) for (hole_feature_ui_main h : oppHoles) {
            double[] uv = hole_mesh_triangulator_ui_main.computeExitUV(f, fOpp, h.getU(), h.getV(), thick);
            double u = uv[0], v = uv[1]; HoleCluster m = findMatch(clusters, u, v);
            if (m == null) {
                Point3D hc = f.origin().add(f.u().multiply(u)).add(f.v().multiply(v));
                clusters.add(new HoleCluster(u, v, hc, h, h.getRadius(), true));
            } else m.addExit(h);
        }
        return clusters;
    }

    private static HoleCluster findMatch(List<HoleCluster> clusters, double u, double v) {
        for (HoleCluster c : clusters) if (Math.hypot(u - c.u, v - c.v) < 0.5) return c;
        return null;
    }

    public static List<HoleCluster> clusterHoles(List<hole_feature_ui_main> holes, Frame f, boolean isExit) {
        return clusterFaceHoles(f, null, 0, isExit ? null : holes, null);
    }

    public static List<Step> computeSteps(List<hole_feature_ui_main> holes, double maxThick) {
        List<hole_feature_ui_main> sorted = new ArrayList<>(holes);
        sorted.sort((a, b) -> Double.compare(b.getRadius(), a.getRadius()));

        List<Step> steps = new ArrayList<>();
        for (hole_feature_ui_main h : sorted) {
            double r = h.getRadius();
            double d = h.isThroughAll() ? maxThick : Math.min(maxThick, h.getDepth());
            if (steps.isEmpty()) {
                steps.add(new Step(r, d));
            } else {
                Step prev = steps.get(steps.size() - 1);
                if (r < prev.radius() - 1e-4 && d > prev.depth() + 1e-4) {
                    steps.add(new Step(r, d));
                }
            }
        }
        return steps;
    }

    public static void buildSteppedCavity(Frame f, Point3D hc, List<hole_feature_ui_main> holes,
                                          double maxThick, List<Float> pts, List<Integer> fcs) {
        List<Step> steps = computeSteps(holes, maxThick);
        if (steps.isEmpty()) return;

        boolean throughAll = false;
        for (hole_feature_ui_main h : holes) if (h.isThroughAll()) throughAll = true;

        for (int k = 0; k < steps.size(); k++) {
            Step curr = steps.get(k);
            double zStart = (k == 0) ? 0.0 : steps.get(k - 1).depth();
            double zEnd = curr.depth();
            double r = curr.radius();

            Point3D topC = hc.subtract(f.n().multiply(zStart));
            Point3D botC = hc.subtract(f.n().multiply(zEnd));
            Point3D[] topCirc = hole_mesh_triangulator_ui_main.getCirclePoints(topC, f.u(), f.v(), r, SEGS);
            Point3D[] botCirc = hole_mesh_triangulator_ui_main.getCirclePoints(botC, f.u(), f.v(), r, SEGS);

            hole_mesh_triangulator_ui_main.buildCylindricalWall(topCirc, botCirc, pts, fcs);

            if (k < steps.size() - 1) {
                double rNext = steps.get(k + 1).radius();
                Point3D[] inCirc = hole_mesh_triangulator_ui_main.getCirclePoints(botC, f.u(), f.v(), rNext, SEGS);
                hole_advanced_mesh_helper_ui_main.buildPlanarAnnulus(botCirc, inCirc, pts, fcs);
            } else if (!throughAll && zEnd < maxThick - 1e-4) {
                hole_mesh_triangulator_ui_main.buildCircleCap(botC, botCirc, pts, fcs, true);
            }
        }
    }

    public static void buildCubeFaceWithClusters(Frame f, Frame fOpp, double s,
                                                 List<hole_feature_ui_main> faceHoles,
                                                 List<hole_feature_ui_main> oppHoles,
                                                 List<hole_feature_ui_main> allHoles, Point3D center,
                                                 List<Float> pts, List<Integer> fcs) {
        List<HoleCluster> clusters = clusterFaceHoles(f, fOpp, s, faceHoles, oppHoles);
        if (clusters.size() == 1) {
            HoleCluster cl = clusters.get(0); double hw = s * 0.5;
            Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, -hw, hw, -hw, hw);
            Point3D[] entry = hole_profile_helper_ui_main.getProfilePoints(cl.center, f.u(), f.v(), cl.primaryHole, cl.cutoutRadius, SEGS);
            hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, entry, pts, fcs, cl.isExitOnly());
            if (cl.hasEntry()) {
                if (cl.faceHoles.size() == 1) {
                    hole_advanced_mesh_helper_ui_main.buildCubeHoleCavity(f, s, cl.faceHoles.get(0), cl.center, entry, allHoles, center, pts, fcs);
                } else buildSteppedCavity(f, cl.center, cl.faceHoles, s, pts, fcs);
            }
        } else {
            hole_pattern_mesh_helper_ui_main.partitionAndBuildCubeFace(f, s, clusters, allHoles, center, pts, fcs);
        }
    }

    public static void buildCubeFaceWithClusters(Frame f, double s, List<hole_feature_ui_main> faceHoles,
                                                 List<hole_feature_ui_main> allHoles, Point3D center,
                                                 List<Float> pts, List<Integer> fcs) {
        buildCubeFaceWithClusters(f, null, s, faceHoles, null, allHoles, center, pts, fcs);
    }
    public static void buildCubeExitFaceWithClusters(Frame f, double s, List<hole_feature_ui_main> oppHoles,
                                                     Point3D center, List<Float> pts, List<Integer> fcs) {
        buildCubeFaceWithClusters(f, null, s, null, oppHoles, null, center, pts, fcs);
    }

    public static void buildCylinderSteppedCavity(double cx, double cz, double hx, double hz, double y,
                                                  double maxH, boolean isTop, List<hole_feature_ui_main> holes,
                                                  List<Float> pts, List<Integer> fcs) {
        List<Step> steps = computeSteps(holes, maxH);
        if (steps.isEmpty()) return;
        boolean throughAll = false;
        for (hole_feature_ui_main h : holes) if (h.isThroughAll()) throughAll = true;

        for (int k = 0; k < steps.size(); k++) {
            Step curr = steps.get(k);
            double zStart = (k == 0) ? 0.0 : steps.get(k - 1).depth(), zEnd = curr.depth(), r = curr.radius();
            double y1 = isTop ? (y + zStart) : (y - zStart), y2 = isTop ? (y + zEnd) : (y - zEnd);
            Point3D c1 = new Point3D(hx, y1, hz), c2 = new Point3D(hx, y2, hz);
            Point3D u = new Point3D(1, 0, 0), v = new Point3D(0, 0, 1);
            Point3D[] tCirc = hole_mesh_triangulator_ui_main.getCirclePoints(c1, u, v, r, SEGS);
            Point3D[] bCirc = hole_mesh_triangulator_ui_main.getCirclePoints(c2, u, v, r, SEGS);
            if (isTop) hole_mesh_triangulator_ui_main.buildCylindricalWall(tCirc, bCirc, pts, fcs);
            else hole_mesh_triangulator_ui_main.buildCylindricalWall(bCirc, tCirc, pts, fcs);

            if (k < steps.size() - 1) {
                double rNext = steps.get(k + 1).radius();
                Point3D[] inCirc = hole_mesh_triangulator_ui_main.getCirclePoints(c2, u, v, rNext, SEGS);
                hole_advanced_mesh_helper_ui_main.buildPlanarAnnulus(bCirc, inCirc, pts, fcs);
            } else if (!throughAll && zEnd < maxH - 1e-4) {
                hole_mesh_triangulator_ui_main.buildCircleCap(c2, bCirc, pts, fcs, isTop);
            }
        }
    }
}
