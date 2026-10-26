package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;
import ui.workspace.shapes.holes.hole_profile_helper_ui_main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class machined_face_hole_partitioner_ui_main {

    private machined_face_hole_partitioner_ui_main() {}

    public static void partitionAndBuildFace(shape_item_ui_main s, face_kind_ui_main kind,
                                            Point3D pt1, Point3D pt2, Point3D pt3, Point3D pt4,
                                            List<hole_feature_ui_main> holes,
                                            List<Float> pts, List<Integer> fcs) {
        if (holes == null || holes.isEmpty()) {
            addQuad(pt1, pt2, pt3, pt4, pts, fcs);
            return;
        }
        double w = Math.abs(s.getP2().getX() - s.getP1().getX());
        double h = Math.abs(s.getP2().getY() - s.getP1().getY());
        double d = Math.abs(s.getP2().getZ() - s.getP1().getZ());
        Frame nominal = hole_mesh_triangulator_ui_main.getCuboidFaceFrame(s.getCenter(), w, h, d, kind);
        Frame frame = alignFrameToFace(nominal, pt1, pt2, pt3, pt4);
        double fw = (kind == face_kind_ui_main.TOP || kind == face_kind_ui_main.BOTTOM) ? w :
                    (kind == face_kind_ui_main.FRONT || kind == face_kind_ui_main.BACK) ? w : d;
        double fh = (kind == face_kind_ui_main.TOP || kind == face_kind_ui_main.BOTTOM) ? d : h;

        partition(s, kind, frame, -fw * 0.5, fw * 0.5, -fh * 0.5, fh * 0.5, pt1, pt2, pt3, pt4, new ArrayList<>(holes), pts, fcs);
    }

    private static Frame alignFrameToFace(Frame nominal, Point3D pt1, Point3D pt2, Point3D pt3, Point3D pt4) {
        if (nominal == null || pt1 == null || pt2 == null || pt3 == null || pt4 == null) return nominal;
        Point3D e1 = pt2.subtract(pt1), e2 = pt4.subtract(pt1);
        Point3D n = e1.crossProduct(e2);
        if (n.magnitude() < 1e-6) {
            n = pt3.subtract(pt2).crossProduct(pt1.subtract(pt2));
            if (n.magnitude() < 1e-6) return nominal;
        }
        n = n.normalize();
        if (n.dotProduct(nominal.n()) < 0) n = n.multiply(-1.0);
        double dist = nominal.origin().subtract(pt1).dotProduct(n);
        Point3D orig = nominal.origin().subtract(n.multiply(dist));
        Point3D uProj = nominal.u().subtract(n.multiply(nominal.u().dotProduct(n)));
        if (uProj.magnitude() < 1e-6) return nominal;
        Point3D u = uProj.normalize();
        Point3D v = n.crossProduct(u);
        if (v.dotProduct(nominal.v()) < 0) v = v.multiply(-1.0);
        return new Frame(orig, n, u, v);
    }

    private static void partition(shape_item_ui_main s, face_kind_ui_main kind, Frame frame,
                                  double uMin, double uMax, double vMin, double vMax,
                                  Point3D pt1, Point3D pt2, Point3D pt3, Point3D pt4,
                                  List<hole_feature_ui_main> holes, List<Float> pts, List<Integer> fcs) {
        if (holes.isEmpty()) {
            addQuad(pt1, pt2, pt3, pt4, pts, fcs);
            return;
        }
        if (holes.size() == 1) {
            hole_feature_ui_main hole = holes.get(0);
            Point3D[] oct = new Point3D[]{
                pt1, pt1.midpoint(pt2), pt2, pt2.midpoint(pt3),
                pt3, pt3.midpoint(pt4), pt4, pt4.midpoint(pt1)
            };
            Point3D hc = frame.origin().add(frame.u().multiply(hole.getU())).add(frame.v().multiply(hole.getV()));
            Point3D[] entry = hole_profile_helper_ui_main.getProfilePoints(hc, frame.u(), frame.v(), hole, hole.getRadius(), 32);
            hole_mesh_triangulator_ui_main.triangulateAnnularFace(oct, entry, pts, fcs, false);
            buildCavity(s, frame, hole, hc, entry, pts, fcs);
            return;
        }

        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
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
            double gap = splitU ? (holes.get(k).getU() - holes.get(k - 1).getU()) : (holes.get(k).getV() - holes.get(k - 1).getV());
            if (gap > maxGap) { maxGap = gap; bestK = k; }
        }

        double sVal = splitU ? (holes.get(bestK - 1).getU() + holes.get(bestK).getU()) * 0.5 : (holes.get(bestK - 1).getV() + holes.get(bestK).getV()) * 0.5;
        List<hole_feature_ui_main> left = new ArrayList<>(holes.subList(0, bestK));
        List<hole_feature_ui_main> right = new ArrayList<>(holes.subList(bestK, holes.size()));

        if (splitU) {
            double span = Math.max(1e-3, uMax - uMin);
            double alpha = Math.max(0.05, Math.min(0.95, (sVal - uMin) / span));
            Point3D pTopSplit = pt1.add(pt2.subtract(pt1).multiply(alpha));
            Point3D pBotSplit = pt4.add(pt3.subtract(pt4).multiply(alpha));
            partition(s, kind, frame, uMin, sVal, vMin, vMax, pt1, pTopSplit, pBotSplit, pt4, left, pts, fcs);
            partition(s, kind, frame, sVal, uMax, vMin, vMax, pTopSplit, pt2, pt3, pBotSplit, right, pts, fcs);
        } else {
            double span = Math.max(1e-3, vMax - vMin);
            double beta = Math.max(0.05, Math.min(0.95, (sVal - vMin) / span));
            Point3D pRightSplit = pt2.add(pt3.subtract(pt2).multiply(beta));
            Point3D pLeftSplit = pt1.add(pt4.subtract(pt1).multiply(beta));
            partition(s, kind, frame, uMin, uMax, vMin, sVal, pt1, pt2, pRightSplit, pLeftSplit, left, pts, fcs);
            partition(s, kind, frame, uMin, uMax, sVal, vMax, pLeftSplit, pRightSplit, pt3, pt4, right, pts, fcs);
        }
    }

    private static void addQuad(Point3D a, Point3D b, Point3D c, Point3D d, List<Float> pts, List<Integer> fcs) {
        hole_mesh_triangulator_ui_main.addQuad(a, b, c, d, pts, fcs);
    }

    private static void buildCavity(shape_item_ui_main s, Frame f, hole_feature_ui_main h, Point3D hc,
                                    Point3D[] entry, List<Float> pts, List<Integer> fcs) {
        double w = Math.abs(s.getP2().getX() - s.getP1().getX());
        double hDim = Math.abs(s.getP2().getY() - s.getP1().getY()) > 0.1 ? Math.abs(s.getP2().getY() - s.getP1().getY()) : Math.max(6.0, w * 0.5);
        double d = Math.abs(s.getP2().getZ() - s.getP1().getZ());
        Point3D c = s.getCenter();
        List<hole_feature_ui_main> eff = s.getAllEffectiveHoles();
        double rBore = h.getRadius();
        double totalDepth = ui.workspace.shapes.holes.hole_intersection_helper_ui_main.getEffectiveDepth(h, eff, c, w, hDim, d);
        if (totalDepth <= 1e-4) return;
        Point3D endCenter = hc.subtract(f.n().multiply(totalDepth)), boreStart = hc;

        if (h.getHoleType() == hole_feature_ui_main.HoleType.COUNTERSINK) {
            double coneDepth = Math.min(totalDepth, h.getConeDepth());
            boreStart = hc.subtract(f.n().multiply(coneDepth));
            Point3D[] transCirc = hole_mesh_triangulator_ui_main.getCirclePoints(boreStart, f.u(), f.v(), rBore, 32);
            ui.workspace.shapes.holes.hole_advanced_mesh_helper_ui_main.buildFrustumWall(entry, transCirc, pts, fcs);
        } else if (h.getHoleType() == hole_feature_ui_main.HoleType.COUNTERBORE) {
            double cbDepth = Math.min(totalDepth, h.getCbDepth());
            boreStart = hc.subtract(f.n().multiply(cbDepth));
            Point3D[] recessBottom = hole_mesh_triangulator_ui_main.getCirclePoints(boreStart, f.u(), f.v(), h.getOuterRadius(), 32);
            hole_mesh_triangulator_ui_main.buildCylindricalWall(entry, recessBottom, pts, fcs);
            Point3D[] shoulderInner = hole_mesh_triangulator_ui_main.getCirclePoints(boreStart, f.u(), f.v(), rBore, 32);
            ui.workspace.shapes.holes.hole_advanced_mesh_helper_ui_main.buildPlanarAnnulus(recessBottom, shoulderInner, pts, fcs);
        }

        ui.workspace.shapes.holes.hole_intersection_helper_ui_main.buildCylindricalWallTrimmed(boreStart, endCenter, f.u(), f.v(), rBore,
                eff, h, c, w, hDim, d, pts, fcs);
        ui.workspace.shapes.holes.hole_intersection_helper_ui_main.buildCavityEndCapTrimmed(endCenter, f.u(), f.v(), h, rBore,
                eff, c, w, hDim, d, pts, fcs);
    }
}
