package ui.workspace.drafting.sketch;

import java.util.Locale;

public record sketch_point_2d_ui_main(double x, double y) {

    public static final sketch_point_2d_ui_main ZERO = new sketch_point_2d_ui_main(0, 0);
    public static final double TOLERANCE = 1e-4;

    public sketch_point_2d_ui_main add(sketch_point_2d_ui_main o) {
        return new sketch_point_2d_ui_main(this.x + o.x, this.y + o.y);
    }

    public sketch_point_2d_ui_main add(double dx, double dy) {
        return new sketch_point_2d_ui_main(this.x + dx, this.y + dy);
    }

    public sketch_point_2d_ui_main subtract(sketch_point_2d_ui_main o) {
        return new sketch_point_2d_ui_main(this.x - o.x, this.y - o.y);
    }

    public sketch_point_2d_ui_main multiply(double factor) {
        return new sketch_point_2d_ui_main(this.x * factor, this.y * factor);
    }

    public double distance(sketch_point_2d_ui_main o) {
        return Math.hypot(this.x - o.x, this.y - o.y);
    }

    public double distanceSq(sketch_point_2d_ui_main o) {
        double dx = this.x - o.x, dy = this.y - o.y;
        return dx * dx + dy * dy;
    }

    public double dot(sketch_point_2d_ui_main o) {
        return this.x * o.x + this.y * o.y;
    }

    public double cross(sketch_point_2d_ui_main o) {
        return this.x * o.y - this.y * o.x;
    }

    public double magnitude() {
        return Math.hypot(x, y);
    }

    public sketch_point_2d_ui_main normalize() {
        double m = magnitude();
        return (m > 1e-9) ? new sketch_point_2d_ui_main(x / m, y / m) : ZERO;
    }

    public sketch_point_2d_ui_main midpoint(sketch_point_2d_ui_main o) {
        return new sketch_point_2d_ui_main((this.x + o.x) * 0.5, (this.y + o.y) * 0.5);
    }

    public boolean equalsWithTolerance(sketch_point_2d_ui_main o, double tol) {
        if (o == null) return false;
        return Math.abs(this.x - o.x) <= tol && Math.abs(this.y - o.y) <= tol;
    }

    public boolean equalsWithTolerance(sketch_point_2d_ui_main o) {
        return equalsWithTolerance(o, TOLERANCE);
    }

    public static sketch_point_2d_ui_main parse(String s) {
        if (s == null || !s.contains(",")) return ZERO;
        String[] parts = s.split(",");
        try {
            return new sketch_point_2d_ui_main(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
        } catch (Exception e) {
            return ZERO;
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%.4f,%.4f", x, y);
    }
}
