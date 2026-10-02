package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.util.List;

/**
 * hole_advanced_mesh_helper_ui_main.java
 * High-precision tessellation helper for Stage D Countersink and Counterbore cavities.
 * Generates true conical frustum walls and stepped cylindrical recesses with planar shoulders.
 */
public final class hole_advanced_mesh_helper_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private hole_advanced_mesh_helper_ui_main() {}

    public static void buildCubeHoleCavity(Frame f, double s, hole_feature_ui_main h, Point3D hc,
                                           Point3D[] entry, List<Float> pts, List<Integer> fcs) {
        buildCubeHoleCavity(f, s, h, hc, entry, null, null, pts, fcs);
    }

    public static void buildCubeHoleCavity(Frame f, double s, hole_feature_ui_main h, Point3D hc,
                                           Point3D[] entry, List<hole_feature_ui_main> allHoles,
                                           Point3D cubeCenter, List<Float> pts, List<Integer> fcs) {
        double rBore = h.getRadius();
        double totalDepth = hole_intersection_helper_ui_main.getEffectiveDepth(h, allHoles, cubeCenter, s, s, s);

        switch (h.getHoleType()) {
            case COUNTERSINK -> {
                double coneDepth = Math.min(totalDepth, h.getConeDepth());
                Point3D transCenter = hc.subtract(f.n().multiply(coneDepth));
                Point3D[] transCirc = hole_mesh_triangulator_ui_main.getCirclePoints(transCenter, f.u(), f.v(), rBore, SEGS);
                buildFrustumWall(entry, transCirc, pts, fcs);

                Point3D endCenter = hc.subtract(f.n().multiply(totalDepth));
                hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(transCenter, endCenter, f.u(), f.v(), rBore, allHoles, h, cubeCenter, s, pts, fcs);
                hole_intersection_helper_ui_main.buildCavityEndCapTrimmed(endCenter, f.u(), f.v(), h, rBore, allHoles, cubeCenter, s, s, s, pts, fcs);
            }
            case COUNTERBORE -> {
                double cbDepth = Math.min(totalDepth, h.getCbDepth());
                Point3D recessCenter = hc.subtract(f.n().multiply(cbDepth));
                Point3D[] recessBottom = hole_mesh_triangulator_ui_main.getCirclePoints(recessCenter, f.u(), f.v(), h.getOuterRadius(), SEGS);
                hole_mesh_triangulator_ui_main.buildCylindricalWall(entry, recessBottom, pts, fcs);

                Point3D[] shoulderInner = hole_mesh_triangulator_ui_main.getCirclePoints(recessCenter, f.u(), f.v(), rBore, SEGS);
                buildPlanarAnnulus(recessBottom, shoulderInner, pts, fcs);

                Point3D endCenter = hc.subtract(f.n().multiply(totalDepth));
                hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(recessCenter, endCenter, f.u(), f.v(), rBore, allHoles, h, cubeCenter, s, pts, fcs);
                hole_intersection_helper_ui_main.buildCavityEndCapTrimmed(endCenter, f.u(), f.v(), h, rBore, allHoles, cubeCenter, s, s, s, pts, fcs);
            }
            default -> {
                Point3D endCenter = hc.subtract(f.n().multiply(totalDepth));
                hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(hc, endCenter, f.u(), f.v(), rBore, allHoles, h, cubeCenter, s, pts, fcs);
                hole_intersection_helper_ui_main.buildCavityEndCapTrimmed(endCenter, f.u(), f.v(), h, rBore, allHoles, cubeCenter, s, s, s, pts, fcs);
            }
        }
    }

    public static void buildCylinderHoleCavity(double cx, double cz, double hx, double hz,
                                               double entryY, double h, boolean isTop,
                                               hole_feature_ui_main hole, List<Float> pts, List<Integer> fcs) {
        buildCylinderHoleCavity(cx, cz, hx, hz, entryY, h, isTop, hole, null, 50.0, pts, fcs);
    }

    public static void buildCylinderHoleCavity(double cx, double cz, double hx, double hz,
                                               double entryY, double h, boolean isTop,
                                               hole_feature_ui_main hole, List<hole_feature_ui_main> allHoles,
                                               double rCyl, List<Float> pts, List<Integer> fcs) {
        Point3D shapeCenter = new Point3D(cx, -h * 0.5, cz);
        double totalDepth = hole_intersection_helper_ui_main.getEffectiveDepth(hole, allHoles, shapeCenter, 2 * rCyl, h, 2 * rCyl);
        Point3D hc = new Point3D(hx, entryY, hz);
        Point3D dir = isTop ? new Point3D(0, 1, 0) : new Point3D(0, -1, 0);
        Point3D u = new Point3D(1, 0, 0), v = isTop ? new Point3D(0, 0, 1) : new Point3D(0, 0, -1);
        Point3D endCenter = hc.add(dir.multiply(totalDepth));
        double rBore = hole.getRadius();

        switch (hole.getHoleType()) {
            case COUNTERSINK -> {
                double coneDepth = Math.min(totalDepth, hole.getConeDepth());
                Point3D transCenter = hc.add(dir.multiply(coneDepth));
                Point3D[] entry = hole_mesh_triangulator_ui_main.getCirclePoints(hc, u, v, hole.getOuterRadius(), SEGS);
                Point3D[] transCirc = hole_mesh_triangulator_ui_main.getCirclePoints(transCenter, u, v, rBore, SEGS);
                buildFrustumWall(entry, transCirc, pts, fcs);
                hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(transCenter, endCenter, u, v, rBore, allHoles, hole, shapeCenter, 2 * rCyl, h, 2 * rCyl, pts, fcs);
                hole_intersection_helper_ui_main.buildCavityEndCapTrimmed(endCenter, u, v, hole, rBore, allHoles, shapeCenter, 2 * rCyl, h, 2 * rCyl, pts, fcs);
            }
            case COUNTERBORE -> {
                double cbDepth = Math.min(totalDepth, hole.getCbDepth());
                Point3D recessCenter = hc.add(dir.multiply(cbDepth));
                Point3D[] entry = hole_mesh_triangulator_ui_main.getCirclePoints(hc, u, v, hole.getOuterRadius(), SEGS);
                Point3D[] recessBottom = hole_mesh_triangulator_ui_main.getCirclePoints(recessCenter, u, v, hole.getOuterRadius(), SEGS);
                hole_mesh_triangulator_ui_main.buildCylindricalWall(entry, recessBottom, pts, fcs);
                Point3D[] shoulderInner = hole_mesh_triangulator_ui_main.getCirclePoints(recessCenter, u, v, rBore, SEGS);
                buildPlanarAnnulus(recessBottom, shoulderInner, pts, fcs);
                hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(recessCenter, endCenter, u, v, rBore, allHoles, hole, shapeCenter, 2 * rCyl, h, 2 * rCyl, pts, fcs);
                hole_intersection_helper_ui_main.buildCavityEndCapTrimmed(endCenter, u, v, hole, rBore, allHoles, shapeCenter, 2 * rCyl, h, 2 * rCyl, pts, fcs);
            }
            default -> {
                hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(hc, endCenter, u, v, rBore, allHoles, hole, shapeCenter, 2 * rCyl, h, 2 * rCyl, pts, fcs);
                hole_intersection_helper_ui_main.buildCavityEndCapTrimmed(endCenter, u, v, hole, rBore, allHoles, shapeCenter, 2 * rCyl, h, 2 * rCyl, pts, fcs);
            }
        }
    }

    public static void buildFrustumWall(Point3D[] topCirc, Point3D[] botCirc, List<Float> pts, List<Integer> fcs) {
        for (int i = 0; i < SEGS; i++) {
            int next = (i + 1) % SEGS;
            hole_mesh_triangulator_ui_main.addTri(topCirc[i], topCirc[next], botCirc[next], pts, fcs, false);
            hole_mesh_triangulator_ui_main.addTri(topCirc[i], botCirc[next], botCirc[i], pts, fcs, false);
        }
    }

    public static void buildPlanarAnnulus(Point3D[] outer, Point3D[] inner, List<Float> pts, List<Integer> fcs) {
        for (int i = 0; i < SEGS; i++) {
            int next = (i + 1) % SEGS;
            hole_mesh_triangulator_ui_main.addTri(outer[i], outer[next], inner[i], pts, fcs, false);
            hole_mesh_triangulator_ui_main.addTri(outer[next], inner[next], inner[i], pts, fcs, false);
        }
    }
}
