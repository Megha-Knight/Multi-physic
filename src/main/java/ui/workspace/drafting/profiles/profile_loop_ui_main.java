package ui.workspace.drafting.profiles;

import javafx.geometry.Point3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * profile_loop_ui_main.java
 * Represents an ordered, planar closed boundary loop of 3D vertices for profile-driven modeling.
 */
public class profile_loop_ui_main {

    private final List<Point3D> points = new ArrayList<>();
    private final boolean closed;
    private Point3D normal;
    private Point3D center;

    public profile_loop_ui_main(List<Point3D> pts, boolean closed) {
        if (pts != null) {
            for (Point3D p : pts) {
                if (p != null) this.points.add(p);
            }
        }
        this.closed = closed;
        computeProperties();
    }

    public List<Point3D> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public int getPointCount() {
        return points.size();
    }

    public boolean isClosed() {
        return closed;
    }

    public Point3D getNormal() {
        return normal != null ? normal : new Point3D(0, 1, 0);
    }

    public Point3D getCenter() {
        return center != null ? center : new Point3D(0, 0, 0);
    }

    public double computeArea() {
        if (points.size() < 3) return 0.0;
        double sumX = 0, sumY = 0, sumZ = 0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            Point3D p1 = points.get(i);
            Point3D p2 = points.get((i + 1) % n);
            Point3D cross = p1.crossProduct(p2);
            sumX += cross.getX();
            sumY += cross.getY();
            sumZ += cross.getZ();
        }
        return 0.5 * Math.sqrt(sumX * sumX + sumY * sumY + sumZ * sumZ);
    }

    public profile_loop_ui_main resample(int targetCount) {
        if (points.isEmpty() || targetCount <= 0) return this;
        if (points.size() == targetCount) return this;

        List<Point3D> resampled = new ArrayList<>();
        double totalLen = 0.0;
        int n = points.size();
        double[] segLens = new double[n];
        for (int i = 0; i < n; i++) {
            Point3D p1 = points.get(i);
            Point3D p2 = points.get((i + 1) % n);
            segLens[i] = p1.distance(p2);
            totalLen += segLens[i];
        }

        if (totalLen <= 1e-6) {
            for (int i = 0; i < targetCount; i++) resampled.add(points.get(0));
            return new profile_loop_ui_main(resampled, closed);
        }

        double step = totalLen / targetCount;
        for (int i = 0; i < targetCount; i++) {
            double targetDist = i * step;
            double acc = 0.0;
            Point3D pt = points.get(0);
            for (int s = 0; s < n; s++) {
                if (acc + segLens[s] >= targetDist - 1e-9) {
                    double remain = targetDist - acc;
                    double t = segLens[s] > 1e-9 ? Math.max(0.0, Math.min(1.0, remain / segLens[s])) : 0.0;
                    Point3D pA = points.get(s);
                    Point3D pB = points.get((s + 1) % n);
                    pt = pA.add(pB.subtract(pA).multiply(t));
                    break;
                }
                acc += segLens[s];
            }
            resampled.add(pt);
        }
        return new profile_loop_ui_main(resampled, closed);
    }

    public profile_loop_ui_main reversed() {
        List<Point3D> rev = new ArrayList<>(points);
        Collections.reverse(rev);
        return new profile_loop_ui_main(rev, closed);
    }

    private void computeProperties() {
        if (points.isEmpty()) {
            normal = new Point3D(0, 1, 0);
            center = new Point3D(0, 0, 0);
            return;
        }
        double cx = 0, cy = 0, cz = 0;
        for (Point3D p : points) {
            cx += p.getX();
            cy += p.getY();
            cz += p.getZ();
        }
        center = new Point3D(cx / points.size(), cy / points.size(), cz / points.size());

        if (points.size() >= 3) {
            Point3D v1 = points.get(1).subtract(points.get(0));
            Point3D v2 = points.get(2).subtract(points.get(0));
            Point3D cr = v1.crossProduct(v2);
            double mag = cr.magnitude();
            normal = mag > 1e-6 ? cr.multiply(1.0 / mag) : new Point3D(0, 1, 0);
        } else {
            normal = new Point3D(0, 1, 0);
        }
    }
}
