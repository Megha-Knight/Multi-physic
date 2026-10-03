package ui.workspace.drafting.sketch;

import java.util.List;
import java.util.Locale;

public class sketch_arc_ui_main extends sketch_entity_ui_main {

    private sketch_point_2d_ui_main center;
    private double radius;
    private double startAngle;
    private double endAngle;

    public sketch_arc_ui_main(String id, sketch_point_2d_ui_main center, double radius, double startAngle, double endAngle) {
        super(id, sketch_geom_type_ui_main.ARC);
        this.center = (center != null) ? center : sketch_point_2d_ui_main.ZERO;
        this.radius = Math.max(1e-4, radius);
        this.startAngle = normalizeAngle(startAngle);
        this.endAngle = normalizeAngle(endAngle);
    }

    private static double normalizeAngle(double a) {
        double deg = a % 360.0;
        return deg < 0 ? deg + 360.0 : deg;
    }

    public sketch_point_2d_ui_main getCenter() { return center; }
    public void setCenter(sketch_point_2d_ui_main c) { if (c != null) this.center = c; }
    public double getRadius() { return radius; }
    public void setRadius(double r) { this.radius = Math.max(1e-4, r); }
    public double getStartAngle() { return startAngle; }
    public void setStartAngle(double a) { this.startAngle = normalizeAngle(a); }
    public double getEndAngle() { return endAngle; }
    public void setEndAngle(double a) { this.endAngle = normalizeAngle(a); }

    public double getAngularSpan() {
        double span = endAngle - startAngle;
        return span <= 0 ? span + 360.0 : span;
    }

    public sketch_point_2d_ui_main getStartPoint() {
        double rad = Math.toRadians(startAngle);
        return center.add(radius * Math.cos(rad), radius * Math.sin(rad));
    }

    public sketch_point_2d_ui_main getEndPoint() {
        double rad = Math.toRadians(endAngle);
        return center.add(radius * Math.cos(rad), radius * Math.sin(rad));
    }

    public sketch_point_2d_ui_main getMidPoint() {
        double midA = startAngle + getAngularSpan() * 0.5;
        double rad = Math.toRadians(midA);
        return center.add(radius * Math.cos(rad), radius * Math.sin(rad));
    }

    @Override
    public List<sketch_point_2d_ui_main> getSnapPoints() {
        return List.of(center, getStartPoint(), getEndPoint(), getMidPoint());
    }

    @Override
    public boolean containsPoint(sketch_point_2d_ui_main pt, double tolerance) {
        if (pt == null) return false;
        double dist = pt.distance(center);
        if (Math.abs(dist - radius) > tolerance) return false;
        double a = Math.toDegrees(Math.atan2(pt.y() - center.y(), pt.x() - center.x()));
        a = normalizeAngle(a);
        double span = getAngularSpan();
        double offset = normalizeAngle(a - startAngle);
        return offset <= span;
    }

    @Override
    public void translate(double dx, double dy) {
        center = center.add(dx, dy);
    }

    @Override
    public sketch_arc_ui_main copy() {
        sketch_arc_ui_main copy = new sketch_arc_ui_main(id, center, radius, startAngle, endAngle);
        copy.setSelected(selected);
        copy.setConstruction(construction);
        return copy;
    }

    @Override
    public boolean isValid() {
        return !Double.isNaN(center.x()) && !Double.isNaN(center.y()) &&
               !Double.isNaN(radius) && radius > 1e-4 && getAngularSpan() > 0.01;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Arc[%s: center=(%.2f,%.2f), r=%.2f, span=%.1f°]",
                id, center.x(), center.y(), radius, getAngularSpan());
    }
}
