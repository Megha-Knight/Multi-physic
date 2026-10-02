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
        public final List<hole_feature_ui_main> holes = new ArrayList<>();
        public HoleCluster(double u, double v, Point3D center) { this.u = u; this.v = v; this.center = center; }
        public double getMaxOuterRadius() {
            double maxR = 0; for (hole_feature_ui_main h : holes) maxR = Math.max(maxR, h.getOuterRadius()); return maxR;
        }
        public double getMinExitRadius() {
            double minR = Double.MAX_VALUE;
            for (hole_feature_ui_main h : holes) if (h.isThroughAll()) minR = Math.min(minR, h.getRadius());
            return minR == Double.MAX_VALUE ? 0 : minR;
        }
        public boolean hasThroughAll() {
            for (hole_feature_ui_main h : holes) if (h.isThroughAll()) return true; return false;
        }
        public hole_feature_ui_main getPrimaryHole() { return holes.isEmpty() ? null : holes.get(0); }
    }

    public static List<HoleCluster> clusterHoles(List<hole_feature_ui_main> holes, Frame f, boolean isExit) {
        List<HoleCluster> clusters = new ArrayList<>();
        if (holes == null || holes.isEmpty()) return clusters;
        for (hole_feature_ui_main h : holes) {
            double u = h.getU(), v = isExit ? -h.getV() : h.getV();
            HoleCluster match = null;
            for (HoleCluster c : clusters) {
                if (Math.hypot(u - c.u, v - c.v) < 0.5) {
                    match = c;
                    break;
                }
            }
            if (match == null) {
                Point3D hc = f.origin().add(f.u().multiply(u)).add(f.v().multiply(v));
                match = new HoleCluster(u, v, hc);
                clusters.add(match);
            }
            match.holes.add(h);
        }
        return clusters;
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

    public static void buildCubeFaceWithClusters(Frame f, double s, List<hole_feature_ui_main> faceHoles,
                                                 List<hole_feature_ui_main> allHoles, Point3D center,
                                                 List<Float> pts, List<Integer> fcs) {
        List<HoleCluster> clusters = clusterHoles(faceHoles, f, false);
        if (clusters.size() == 1) {
            HoleCluster cl = clusters.get(0);
            double hw = s * 0.5;
            Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, -hw, hw, -hw, hw);
            Point3D[] entry = hole_profile_helper_ui_main.getProfilePoints(cl.center, f.u(), f.v(), cl.getPrimaryHole(), cl.getMaxOuterRadius(), SEGS);
            hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, entry, pts, fcs, false);
            if (cl.holes.size() == 1) {
                hole_advanced_mesh_helper_ui_main.buildCubeHoleCavity(f, s, cl.holes.get(0), cl.center, entry, allHoles, center, pts, fcs);
            } else {
                buildSteppedCavity(f, cl.center, cl.holes, s, pts, fcs);
            }
        } else {
            hole_pattern_mesh_helper_ui_main.partitionAndBuildCubeFace(f, s, faceHoles, allHoles, center, pts, fcs, false);
        }
    }

    public static void buildCubeExitFaceWithClusters(Frame f, double s, List<hole_feature_ui_main> oppHoles,
                                                     Point3D center, List<Float> pts, List<Integer> fcs) {
        List<HoleCluster> clusters = clusterHoles(oppHoles, f, true);
        if (clusters.size() == 1) {
            HoleCluster cl = clusters.get(0);
            double rExit = cl.getMinExitRadius();
            if (rExit > 0) {
                double hw = s * 0.5;
                Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, -hw, hw, -hw, hw);
                Point3D[] exit = hole_profile_helper_ui_main.getProfilePoints(cl.center, f.u(), f.v(), cl.getPrimaryHole(), rExit, SEGS);
                hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, exit, pts, fcs, true);
            }
        } else {
            hole_pattern_mesh_helper_ui_main.partitionAndBuildCubeFace(f, s, oppHoles, oppHoles, center, pts, fcs, true);
        }
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
            double zStart = (k == 0) ? 0.0 : steps.get(k - 1).depth();
            double zEnd = curr.depth();
            double r = curr.radius();

            double y1 = isTop ? (y + zStart) : (y - zStart);
            double y2 = isTop ? (y + zEnd) : (y - zEnd);

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
