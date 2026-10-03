package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * topology_boundary_loop_ui_main.java
 * Analytical boundary loop on a face (outer perimeter loop or inner hole cutout loop).
 */
public class topology_boundary_loop_ui_main {

    private final String loopId;
    private final boolean outer;
    private final boolean circular;
    private final double radius;
    private final Point3D center;
    private final List<Point3D> vertices = new ArrayList<>();

    public static topology_boundary_loop_ui_main createCircular(String loopId, boolean outer, Point3D center, double radius) {
        return new topology_boundary_loop_ui_main(loopId, outer, true, radius, center, List.of());
    }

    public static topology_boundary_loop_ui_main createPolygon(String loopId, boolean outer, List<Point3D> verts) {
        return new topology_boundary_loop_ui_main(loopId, outer, false, 0.0, null, verts);
    }

    public static topology_boundary_loop_ui_main createRectangular(String loopId, double width, double height) {
        double hw = width * 0.5, hh = height * 0.5;
        List<Point3D> pts = List.of(
            new Point3D(-hw, -hh, 0),
            new Point3D(hw, -hh, 0),
            new Point3D(hw, hh, 0),
            new Point3D(-hw, hh, 0)
        );
        return new topology_boundary_loop_ui_main(loopId, true, false, 0.0, new Point3D(0, 0, 0), pts);
    }

    private topology_boundary_loop_ui_main(String loopId, boolean outer, boolean circular, double radius, Point3D center, List<Point3D> verts) {
        this.loopId = loopId;
        this.outer = outer;
        this.circular = circular;
        this.radius = radius;
        this.center = center != null ? center : new Point3D(0, 0, 0);
        if (verts != null) this.vertices.addAll(verts);
    }

    public String getLoopId() { return loopId; }
    public boolean isOuter() { return outer; }
    public boolean isCircular() { return circular; }
    public double getRadius() { return radius; }
    public Point3D getCenter() { return center; }
    public List<Point3D> getVertices() { return Collections.unmodifiableList(vertices); }

    public double getArea() {
        if (circular) return Math.PI * radius * radius;
        if (vertices.size() < 3) return 0.0;
        double a = 0.0;
        for (int i = 0; i < vertices.size(); i++) {
            Point3D p1 = vertices.get(i), p2 = vertices.get((i + 1) % vertices.size());
            a += (p1.getX() * p2.getY() - p2.getX() * p1.getY());
        }
        return Math.abs(a) * 0.5;
    }

    public boolean contains(double u, double v) {
        if (circular) {
            double du = u - center.getX(), dv = v - center.getY();
            return (du * du + dv * dv) <= (radius * radius + 1e-4);
        }
        if (vertices.size() < 3) return false;
        boolean inside = false;
        for (int i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            double xi = vertices.get(i).getX(), yi = vertices.get(i).getY();
            double xj = vertices.get(j).getX(), yj = vertices.get(j).getY();
            if (((yi > v) != (yj > v)) && (u < (xj - xi) * (v - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }
}
