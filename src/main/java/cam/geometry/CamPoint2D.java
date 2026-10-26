package cam.geometry;

/**
 * Immutable 2D Cartesian point for CAM pocket geometry.
 * Pure Java record with zero graphical framework dependencies.
 */
public record CamPoint2D(double x, double y) {

    public static final CamPoint2D ZERO = new CamPoint2D(0.0, 0.0);

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y);
    }

    public double distanceTo(CamPoint2D other) {
        if (other == null) return Double.NaN;
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public CamPoint2D add(double dx, double dy) {
        return new CamPoint2D(x + dx, y + dy);
    }

    public CamPoint2D subtract(CamPoint2D other) {
        if (other == null) return this;
        return new CamPoint2D(x - other.x, y - other.y);
    }

    public double dot(CamPoint2D other) {
        if (other == null) return 0.0;
        return x * other.x + y * other.y;
    }

    public double cross(CamPoint2D other) {
        if (other == null) return 0.0;
        return x * other.y - y * other.x;
    }
}
