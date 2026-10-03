package ui.workspace.drafting.sketch;

import java.util.ArrayList;
import java.util.List;

public class sketch_profile_loop_ui_main {

    private final List<sketch_point_2d_ui_main> vertices = new ArrayList<>();
    private boolean outer = true;

    public sketch_profile_loop_ui_main(List<sketch_point_2d_ui_main> points, boolean outer) {
        if (points != null) this.vertices.addAll(points);
        this.outer = outer;
    }

    public List<sketch_point_2d_ui_main> getVertices() { return new ArrayList<>(vertices); }
    public int size() { return vertices.size(); }
    public boolean isOuter() { return outer; }
    public void setOuter(boolean o) { this.outer = o; }

    public double getSignedArea() {
        int n = vertices.size();
        if (n < 3) return 0.0;
        double a = 0.0;
        for (int i = 0; i < n; i++) {
            sketch_point_2d_ui_main p1 = vertices.get(i);
            sketch_point_2d_ui_main p2 = vertices.get((i + 1) % n);
            a += (p1.x() * p2.y() - p2.x() * p1.y());
        }
        return a * 0.5;
    }

    public double getAbsArea() { return Math.abs(getSignedArea()); }
    public boolean isCounterClockwise() { return getSignedArea() > 0.0; }

    public boolean containsPoint(sketch_point_2d_ui_main pt) {
        int n = vertices.size();
        if (n < 3 || pt == null) return false;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            sketch_point_2d_ui_main pi = vertices.get(i), pj = vertices.get(j);
            if (((pi.y() > pt.y()) != (pj.y() > pt.y())) &&
                (pt.x() < (pj.x() - pi.x()) * (pt.y() - pi.y()) / (pj.y() - pi.y() + 1e-12) + pi.x())) {
                inside = !inside;
            }
        }
        return inside;
    }

    public boolean encloses(sketch_profile_loop_ui_main innerLoop) {
        if (innerLoop == null || innerLoop.size() == 0) return false;
        for (sketch_point_2d_ui_main v : innerLoop.getVertices()) {
            if (!containsPoint(v)) return false;
        }
        return true;
    }

    public boolean hasSelfIntersection() {
        int n = vertices.size();
        if (n < 4) return false;
        for (int i = 0; i < n; i++) {
            sketch_point_2d_ui_main a1 = vertices.get(i), a2 = vertices.get((i + 1) % n);
            for (int j = i + 2; j < n; j++) {
                if (i == 0 && j == n - 1) continue; // adjacent at wrap-around
                sketch_point_2d_ui_main b1 = vertices.get(j), b2 = vertices.get((j + 1) % n);
                if (segmentsIntersect(a1, a2, b1, b2)) return true;
            }
        }
        return false;
    }

    private static boolean segmentsIntersect(sketch_point_2d_ui_main a1, sketch_point_2d_ui_main a2,
                                             sketch_point_2d_ui_main b1, sketch_point_2d_ui_main b2) {
        double d1 = cross(a2.subtract(a1), b1.subtract(a1));
        double d2 = cross(a2.subtract(a1), b2.subtract(a1));
        double d3 = cross(b2.subtract(b1), a1.subtract(b1));
        double d4 = cross(b2.subtract(b1), a2.subtract(b1));
        return ((d1 > 1e-6 && d2 < -1e-6) || (d1 < -1e-6 && d2 > 1e-6)) &&
               ((d3 > 1e-6 && d4 < -1e-6) || (d3 < -1e-6 && d4 > 1e-6));
    }

    private static double cross(sketch_point_2d_ui_main v1, sketch_point_2d_ui_main v2) {
        return v1.x() * v2.y() - v1.y() * v2.x();
    }
}
