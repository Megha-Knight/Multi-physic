package ui.workspace.drafting.helical;

import javafx.geometry.Point3D;
import ui.workspace.drafting.revolve.revolve_axis_ui_main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * helical_path_ui_main.java
 * Represents a parametric 3D helical/spiral path with pitch, radius, turns, and axial direction.
 */
public class helical_path_ui_main {

    private final String id;
    private String name;
    private revolve_axis_ui_main axis;
    private double radius = 10.0;
    private double pitch = 10.0;
    private double turns = 3.0;
    private boolean clockwise = true;

    public helical_path_ui_main(String id, String name, revolve_axis_ui_main axis,
                               double radius, double pitch, double turns, boolean clockwise) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "HelicalPath";
        this.axis = axis != null ? axis : revolve_axis_ui_main.yAxis();
        this.radius = Math.max(1e-3, radius);
        this.pitch = Math.max(1e-3, pitch);
        this.turns = Math.max(0.1, turns);
        this.clockwise = clockwise;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public revolve_axis_ui_main getAxis() { return axis; }
    public void setAxis(revolve_axis_ui_main axis) { this.axis = axis; }
    public double getRadius() { return radius; }
    public void setRadius(double r) { this.radius = Math.max(1e-3, r); }
    public double getPitch() { return pitch; }
    public void setPitch(double p) { this.pitch = Math.max(1e-3, p); }
    public double getTurns() { return turns; }
    public void setTurns(double t) { this.turns = Math.max(0.1, t); }
    public boolean isClockwise() { return clockwise; }
    public void setClockwise(boolean cw) { this.clockwise = cw; }

    public double getHeight() {
        return pitch * turns;
    }

    public double getLength() {
        double circ = 2.0 * Math.PI * radius;
        double singleTurnLen = Math.sqrt(circ * circ + pitch * pitch);
        return singleTurnLen * turns;
    }

    public List<Point3D> samplePoints(int stepsPerTurn) {
        int spt = Math.max(8, stepsPerTurn);
        int totalSteps = (int) Math.ceil(turns * spt);
        List<Point3D> pts = new ArrayList<>();

        Point3D axDir = axis.getDirection();
        Point3D axOrigin = axis.getOrigin();

        // Find reference perpendicular vector
        Point3D perp = Math.abs(axDir.getY()) < 0.9 ? new Point3D(0, 1, 0) : new Point3D(1, 0, 0);
        Point3D u = axDir.crossProduct(perp).normalize().multiply(radius);
        Point3D v = axDir.crossProduct(u).normalize().multiply(radius);

        for (int i = 0; i <= totalSteps; i++) {
            double frac = (double) i / (double) totalSteps;
            double currentAngle = (clockwise ? 1.0 : -1.0) * 2.0 * Math.PI * turns * frac;
            double currentHeight = frac * getHeight();

            Point3D radial = u.multiply(Math.cos(currentAngle)).add(v.multiply(Math.sin(currentAngle)));
            Point3D axial = axDir.multiply(currentHeight);
            pts.add(axOrigin.add(radial).add(axial));
        }

        return Collections.unmodifiableList(pts);
    }
}
