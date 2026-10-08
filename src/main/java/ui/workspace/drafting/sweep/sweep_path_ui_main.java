package ui.workspace.drafting.sweep;

import javafx.geometry.Point3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * sweep_path_ui_main.java
 * Represents straight or polyline 3D trajectories along which a profile is swept.
 */
public class sweep_path_ui_main {

    private final String id;
    private String name;
    private final List<Point3D> waypoints = new ArrayList<>();

    public sweep_path_ui_main(String id, String name, List<Point3D> pts) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "SweepPath";
        if (pts != null) {
            for (Point3D p : pts) if (p != null) this.waypoints.add(p);
        }
    }

    public static sweep_path_ui_main createStraightLine(String id, String name, Point3D start, Point3D end) {
        return new sweep_path_ui_main(id, name, List.of(start, end));
    }

    public static sweep_path_ui_main createAxisPath(String id, String name, double length) {
        return new sweep_path_ui_main(id, name, List.of(new Point3D(0, 0, 0), new Point3D(0, length, 0)));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Point3D> getWaypoints() { return Collections.unmodifiableList(waypoints); }
    public int getWaypointCount() { return waypoints.size(); }

    public double getLength() {
        if (waypoints.size() < 2) return 0.0;
        double len = 0.0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            len += waypoints.get(i).distance(waypoints.get(i + 1));
        }
        return len;
    }

    public List<Point3D> samplePoints(int steps) {
        int st = Math.max(2, steps);
        List<Point3D> sampled = new ArrayList<>();
        double totalLen = getLength();
        if (totalLen <= 1e-6 || waypoints.size() < 2) {
            Point3D base = waypoints.isEmpty() ? new Point3D(0, 0, 0) : waypoints.get(0);
            for (int i = 0; i < st; i++) sampled.add(base);
            return sampled;
        }

        double stepDist = totalLen / (st - 1);
        int n = waypoints.size();
        for (int i = 0; i < st; i++) {
            double targetDist = i * stepDist;
            double acc = 0.0;
            Point3D pt = waypoints.get(0);
            for (int s = 0; s < n - 1; s++) {
                double seg = waypoints.get(s).distance(waypoints.get(s + 1));
                if (acc + seg >= targetDist - 1e-9) {
                    double rem = targetDist - acc;
                    double t = seg > 1e-9 ? Math.max(0.0, Math.min(1.0, rem / seg)) : 0.0;
                    Point3D pA = waypoints.get(s);
                    Point3D pB = waypoints.get(s + 1);
                    pt = pA.add(pB.subtract(pA).multiply(t));
                    break;
                }
                acc += seg;
            }
            sampled.add(pt);
        }
        return sampled;
    }
}
