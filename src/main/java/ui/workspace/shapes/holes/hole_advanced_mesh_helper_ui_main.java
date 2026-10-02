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
        double totalDepth = h.isThroughAll() ? s : Math.min(s, h.getDepth());

        switch (h.getHoleType()) {
            case COUNTERSINK -> {
                double coneDepth = Math.min(totalDepth, h.getConeDepth());
                Point3D transCenter = hc.subtract(f.n().multiply(coneDepth));
                Point3D[] transCirc = hole_mesh_triangulator_ui_main.getCirclePoints(transCenter, f.u(), f.v(), rBore, SEGS);
                buildFrustumWall(entry, transCirc, pts, fcs);

                Point3D endCenter = hc.subtract(f.n().multiply(totalDepth));
                hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(transCenter, endCenter, f.u(), f.v(), rBore, allHoles, h, cubeCenter, s, pts, fcs);

                if (!h.isThroughAll() && !hole_intersection_helper_ui_main.isInsideAnyOtherHole(endCenter, allHoles, h, cubeCenter, s)) {
                    Point3D[] boreEnd = hole_mesh_triangulator_ui_main.getCirclePoints(endCenter, f.u(), f.v(), rBore, SEGS);
                    hole_mesh_triangulator_ui_main.buildCircleCap(endCenter, boreEnd, pts, fcs, true);
                }
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

                if (!h.isThroughAll() && !hole_intersection_helper_ui_main.isInsideAnyOtherHole(endCenter, allHoles, h, cubeCenter, s)) {
                    Point3D[] boreEnd = hole_mesh_triangulator_ui_main.getCirclePoints(endCenter, f.u(), f.v(), rBore, SEGS);
                    hole_mesh_triangulator_ui_main.buildCircleCap(endCenter, boreEnd, pts, fcs, true);
                }
            }
            default -> {
                Point3D endCenter = hc.subtract(f.n().multiply(totalDepth));
                if (h.getCutoutShape() == hole_feature_ui_main.CutoutShape.CIRCLE) {
                    hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(hc, endCenter, f.u(), f.v(), rBore, allHoles, h, cubeCenter, s, pts, fcs);
                } else {
                    Point3D[] topProf = hole_profile_helper_ui_main.getProfilePoints(hc, f.u(), f.v(), h, SEGS);
                    Point3D[] botProf = hole_profile_helper_ui_main.getProfilePoints(endCenter, f.u(), f.v(), h, SEGS);
                    hole_mesh_triangulator_ui_main.buildCylindricalWall(topProf, botProf, pts, fcs);
                }
                if (!h.isThroughAll() && !hole_intersection_helper_ui_main.isInsideAnyOtherHole(endCenter, allHoles, h, cubeCenter, s)) {
                    Point3D[] boreEnd = hole_profile_helper_ui_main.getProfilePoints(endCenter, f.u(), f.v(), h, rBore, SEGS);
                    hole_mesh_triangulator_ui_main.buildCircleCap(endCenter, boreEnd, pts, fcs, true);
                }
            }
        }
    }

    public static void buildCylinderHoleCavity(double cx, double cz, double hx, double hz,
                                               double entryY, double h, boolean isTop,
                                               hole_feature_ui_main hole, List<Float> pts, List<Integer> fcs) {
        double rBore = hole.getRadius();
        double dirY = isTop ? 1.0 : -1.0;
        double totalDepth = hole.isThroughAll() ? h : Math.min(h, hole.getDepth());
        double endY = entryY + dirY * totalDepth;

        switch (hole.getHoleType()) {
            case COUNTERSINK -> {
                double rCs = hole.getOuterRadius();
                double coneDepth = Math.min(totalDepth, hole.getConeDepth());
                double transY = entryY + dirY * coneDepth;
                buildCylinderRingToRing(hx, hz, entryY, rCs, transY, rBore, pts, fcs);
                buildCylinderRingToRing(hx, hz, transY, rBore, endY, rBore, pts, fcs);
                if (!hole.isThroughAll()) buildCylinderFloorCap(hx, hz, endY, rBore, isTop, pts, fcs);
            }
            case COUNTERBORE -> {
                double rCb = hole.getOuterRadius();
                double cbDepth = Math.min(totalDepth, hole.getCbDepth());
                double shoulderY = entryY + dirY * cbDepth;
                buildCylinderRingToRing(hx, hz, entryY, rCb, shoulderY, rCb, pts, fcs);
                buildCylinderPlanarAnnulus(hx, hz, shoulderY, rCb, rBore, isTop, pts, fcs);
                buildCylinderRingToRing(hx, hz, shoulderY, rBore, endY, rBore, pts, fcs);
                if (!hole.isThroughAll()) buildCylinderFloorCap(hx, hz, endY, rBore, isTop, pts, fcs);
            }
            default -> {
                buildCylinderRingToRing(hx, hz, entryY, rBore, endY, rBore, pts, fcs);
                if (!hole.isThroughAll()) buildCylinderFloorCap(hx, hz, endY, rBore, isTop, pts, fcs);
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

    private static void buildCylinderRingToRing(double hx, double hz, double y1, double r1,
                                                double y2, double r2, List<Float> pts, List<Integer> fcs) {
        for (int i = 0; i < SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
            Point3D p1 = new Point3D(hx + r1 * Math.cos(a1), y1, hz + r1 * Math.sin(a1));
            Point3D p2 = new Point3D(hx + r1 * Math.cos(a2), y1, hz + r1 * Math.sin(a2));
            Point3D q1 = new Point3D(hx + r2 * Math.cos(a1), y2, hz + r2 * Math.sin(a1));
            Point3D q2 = new Point3D(hx + r2 * Math.cos(a2), y2, hz + r2 * Math.sin(a2));
            hole_mesh_triangulator_ui_main.addQuad(p1, q1, q2, p2, pts, fcs);
        }
    }

    private static void buildCylinderPlanarAnnulus(double hx, double hz, double y, double rOut, double rIn,
                                                  boolean faceUp, List<Float> pts, List<Integer> fcs) {
        for (int i = 0; i < SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
            Point3D o1 = new Point3D(hx + rOut * Math.cos(a1), y, hz + rOut * Math.sin(a1));
            Point3D o2 = new Point3D(hx + rOut * Math.cos(a2), y, hz + rOut * Math.sin(a2));
            Point3D i1 = new Point3D(hx + rIn * Math.cos(a1), y, hz + rIn * Math.sin(a1));
            Point3D i2 = new Point3D(hx + rIn * Math.cos(a2), y, hz + rIn * Math.sin(a2));
            if (faceUp) hole_mesh_triangulator_ui_main.addQuad(o1, i1, i2, o2, pts, fcs);
            else hole_mesh_triangulator_ui_main.addQuad(o1, o2, i2, i1, pts, fcs);
        }
    }

    private static void buildCylinderFloorCap(double hx, double hz, double y, double r,
                                              boolean isTop, List<Float> pts, List<Integer> fcs) {
        Point3D bCenter = new Point3D(hx, y, hz);
        for (int i = 0; i < SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / SEGS, a2 = (i + 1) * 2.0 * Math.PI / SEGS;
            Point3D d1 = new Point3D(hx + r * Math.cos(a1), y, hz + r * Math.sin(a1));
            Point3D d2 = new Point3D(hx + r * Math.cos(a2), y, hz + r * Math.sin(a2));
            hole_mesh_triangulator_ui_main.addTri(bCenter, isTop ? d2 : d1, isTop ? d1 : d2, pts, fcs, false);
        }
    }
}
