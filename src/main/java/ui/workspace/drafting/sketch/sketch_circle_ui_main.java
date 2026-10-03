package ui.workspace.drafting.sketch;

import java.util.List;
import java.util.Locale;

public class sketch_circle_ui_main extends sketch_entity_ui_main {

    private sketch_point_2d_ui_main center;
    private double radius;

    public sketch_circle_ui_main(String id, sketch_point_2d_ui_main center, double radius) {
        super(id, sketch_geom_type_ui_main.CIRCLE);
        this.center = (center != null) ? center : sketch_point_2d_ui_main.ZERO;
        this.radius = Math.max(1e-4, radius);
    }

    public sketch_circle_ui_main(String id, double cx, double cy, double radius) {
        this(id, new sketch_point_2d_ui_main(cx, cy), radius);
    }

    public sketch_point_2d_ui_main getCenter() { return center; }
    public void setCenter(sketch_point_2d_ui_main c) { if (c != null) this.center = c; }
    public double getRadius() { return radius; }
    public void setRadius(double r) { this.radius = Math.max(1e-4, r); }
    public double getDiameter() { return radius * 2.0; }

    @Override
    public List<sketch_point_2d_ui_main> getSnapPoints() {
        return List.of(
            center,
            center.add(radius, 0),
            center.add(-radius, 0),
            center.add(0, radius),
            center.add(0, -radius)
        );
    }

    @Override
    public boolean containsPoint(sketch_point_2d_ui_main pt, double tolerance) {
        if (pt == null) return false;
        double dist = pt.distance(center);
        return Math.abs(dist - radius) <= tolerance;
    }

    public boolean isInside(sketch_point_2d_ui_main pt) {
        return pt != null && pt.distance(center) <= radius;
    }

    @Override
    public void translate(double dx, double dy) {
        center = center.add(dx, dy);
    }

    @Override
    public sketch_circle_ui_main copy() {
        sketch_circle_ui_main copy = new sketch_circle_ui_main(id, center, radius);
        copy.setSelected(selected);
        copy.setConstruction(construction);
        return copy;
    }

    @Override
    public boolean isValid() {
        return !Double.isNaN(center.x()) && !Double.isNaN(center.y()) &&
               !Double.isNaN(radius) && radius > 1e-4;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Circle[%s: center=(%.2f,%.2f), r=%.2f]",
                id, center.x(), center.y(), radius);
    }
}
