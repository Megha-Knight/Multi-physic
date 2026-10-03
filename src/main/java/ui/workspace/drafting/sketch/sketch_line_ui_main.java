package ui.workspace.drafting.sketch;

import java.util.List;
import java.util.Locale;

public class sketch_line_ui_main extends sketch_entity_ui_main {

    private sketch_point_2d_ui_main start;
    private sketch_point_2d_ui_main end;

    public sketch_line_ui_main(String id, sketch_point_2d_ui_main start, sketch_point_2d_ui_main end) {
        super(id, sketch_geom_type_ui_main.LINE);
        this.start = (start != null) ? start : sketch_point_2d_ui_main.ZERO;
        this.end = (end != null) ? end : sketch_point_2d_ui_main.ZERO;
    }

    public sketch_line_ui_main(String id, double x1, double y1, double x2, double y2) {
        this(id, new sketch_point_2d_ui_main(x1, y1), new sketch_point_2d_ui_main(x2, y2));
    }

    public sketch_point_2d_ui_main getStart() { return start; }
    public void setStart(sketch_point_2d_ui_main p) { if (p != null) this.start = p; }
    public sketch_point_2d_ui_main getEnd() { return end; }
    public void setEnd(sketch_point_2d_ui_main p) { if (p != null) this.end = p; }

    public double getLength() { return start.distance(end); }
    public sketch_point_2d_ui_main getMidpoint() { return start.midpoint(end); }
    public sketch_point_2d_ui_main getDirection() { return end.subtract(start).normalize(); }
    public double getAngleDeg() {
        double rad = Math.atan2(end.y() - start.y(), end.x() - start.x());
        double deg = Math.toDegrees(rad);
        return deg < 0 ? deg + 360 : deg;
    }

    @Override
    public List<sketch_point_2d_ui_main> getSnapPoints() {
        return List.of(start, end, getMidpoint());
    }

    @Override
    public boolean containsPoint(sketch_point_2d_ui_main pt, double tolerance) {
        if (pt == null) return false;
        double l2 = start.distanceSq(end);
        if (l2 < 1e-8) return pt.distance(start) <= tolerance;
        double t = Math.max(0, Math.min(1, pt.subtract(start).dot(end.subtract(start)) / l2));
        sketch_point_2d_ui_main projection = start.add(end.subtract(start).multiply(t));
        return pt.distance(projection) <= tolerance;
    }

    @Override
    public void translate(double dx, double dy) {
        start = start.add(dx, dy);
        end = end.add(dx, dy);
    }

    @Override
    public sketch_line_ui_main copy() {
        sketch_line_ui_main copy = new sketch_line_ui_main(id, start, end);
        copy.setSelected(selected);
        copy.setConstruction(construction);
        return copy;
    }

    @Override
    public boolean isValid() {
        return !Double.isNaN(start.x()) && !Double.isNaN(start.y()) &&
               !Double.isNaN(end.x()) && !Double.isNaN(end.y()) &&
               getLength() > 1e-5;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Line[%s: (%.2f,%.2f)->(%.2f,%.2f), len=%.2f]",
                id, start.x(), start.y(), end.x(), end.y(), getLength());
    }
}
