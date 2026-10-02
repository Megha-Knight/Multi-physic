package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;

/**
 * hole_profile_helper_ui_main.java
 * High-precision parametric 2D profile sampler for machining cutouts (Circle, Square, Rectangle, Triangles).
 */
public final class hole_profile_helper_ui_main {

    private hole_profile_helper_ui_main() {}

    public static Point3D[] getProfilePoints(Point3D center, Point3D u, Point3D v, hole_feature_ui_main h, double rOverride, int segs) {
        if (h == null || h.getCutoutShape() == CutoutShape.CIRCLE) {
            double r = (rOverride > 0) ? rOverride : (h != null ? h.getRadius() : 5.0);
            return hole_mesh_triangulator_ui_main.getCirclePoints(center, u, v, r, segs);
        }

        Point3D[] pts = new Point3D[segs];
        double dim1 = (rOverride > 0) ? rOverride * 2.0 : h.getDiameter();
        double dim2 = (h.getWidth2() > 0.1) ? h.getWidth2() : dim1 * 0.6;

        switch (h.getCutoutShape()) {
            case SQUARE -> samplePolygon(center, u, v, getSquareVertices(dim1), pts);
            case RECTANGLE -> samplePolygon(center, u, v, getRectVertices(dim1, dim2), pts);
            case EQUILATERAL_TRIANGLE -> samplePolygon(center, u, v, getEquilateralVertices(dim1), pts);
            case RIGHT_TRIANGLE -> samplePolygon(center, u, v, getRightTriangleVertices(dim1, (h.getWidth2() > 0.1) ? h.getWidth2() : dim1), pts);
            default -> {
                double r = (rOverride > 0) ? rOverride : h.getRadius();
                return hole_mesh_triangulator_ui_main.getCirclePoints(center, u, v, r, segs);
            }
        }
        return pts;
    }

    public static Point3D[] getProfilePoints(Point3D center, Point3D u, Point3D v, hole_feature_ui_main h, int segs) {
        return getProfilePoints(center, u, v, h, -1, segs);
    }

    private static double[][] getSquareVertices(double s) {
        double h = s * 0.5;
        return new double[][]{ { h, -h }, { h, h }, { -h, h }, { -h, -h } };
    }

    private static double[][] getRectVertices(double w, double h) {
        double hw = w * 0.5, hh = h * 0.5;
        return new double[][]{ { hw, -hh }, { hw, hh }, { -hw, hh }, { -hw, -hh } };
    }

    private static double[][] getEquilateralVertices(double s) {
        double ht = s * Math.sqrt(3.0) * 0.5;
        double yTop = ht * (2.0 / 3.0), yBot = -ht * (1.0 / 3.0), hw = s * 0.5;
        return new double[][]{ { hw, yBot }, { 0, yTop }, { -hw, yBot } };
    }

    private static double[][] getRightTriangleVertices(double b, double h) {
        double hw = b * 0.5, hh = h * 0.5;
        return new double[][]{ { hw, -hh }, { -hw, hh }, { -hw, -hh } };
    }

    private static void samplePolygon(Point3D center, Point3D u, Point3D v, double[][] poly, Point3D[] out) {
        int nVerts = poly.length, segs = out.length;
        double totalLen = 0;
        double[] edgeLens = new double[nVerts];
        for (int i = 0; i < nVerts; i++) {
            int next = (i + 1) % nVerts;
            edgeLens[i] = Math.hypot(poly[next][0] - poly[i][0], poly[next][1] - poly[i][1]);
            totalLen += edgeLens[i];
        }

        for (int k = 0; k < segs; k++) {
            double targetDist = (k / (double) segs) * totalLen;
            double accumulated = 0;
            int edgeIdx = 0;
            while (edgeIdx < nVerts - 1 && accumulated + edgeLens[edgeIdx] < targetDist) {
                accumulated += edgeLens[edgeIdx];
                edgeIdx++;
            }
            int nextIdx = (edgeIdx + 1) % nVerts;
            double t = (edgeLens[edgeIdx] > 1e-6) ? (targetDist - accumulated) / edgeLens[edgeIdx] : 0.0;
            double px = poly[edgeIdx][0] * (1.0 - t) + poly[nextIdx][0] * t;
            double py = poly[edgeIdx][1] * (1.0 - t) + poly[nextIdx][1] * t;
            out[k] = center.add(u.multiply(px)).add(v.multiply(py));
        }
    }

    public static boolean isInside2DProfile(double du, double dv, hole_feature_ui_main h) {
        if (h == null || h.getCutoutShape() == CutoutShape.CIRCLE) {
            return (du * du + dv * dv) < (h.getRadius() - 0.05) * (h.getRadius() - 0.05);
        }
        double dim1 = h.getDiameter();
        double dim2 = (h.getWidth2() > 0.1) ? h.getWidth2() : dim1 * 0.6;
        return switch (h.getCutoutShape()) {
            case SQUARE -> Math.abs(du) < (dim1 * 0.5 - 0.05) && Math.abs(dv) < (dim1 * 0.5 - 0.05);
            case RECTANGLE -> Math.abs(du) < (dim1 * 0.5 - 0.05) && Math.abs(dv) < (dim2 * 0.5 - 0.05);
            case EQUILATERAL_TRIANGLE -> isPointInPoly(du, dv, getEquilateralVertices(dim1));
            case RIGHT_TRIANGLE -> isPointInPoly(du, dv, getRightTriangleVertices(dim1, (h.getWidth2() > 0.1) ? h.getWidth2() : dim1));
            default -> (du * du + dv * dv) < (h.getRadius() - 0.05) * (h.getRadius() - 0.05);
        };
    }

    private static boolean isPointInPoly(double px, double py, double[][] poly) {
        boolean inside = false;
        int n = poly.length;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly[i][0], yi = poly[i][1], xj = poly[j][0], yj = poly[j][1];
            boolean intersect = ((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi + 1e-9) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}
