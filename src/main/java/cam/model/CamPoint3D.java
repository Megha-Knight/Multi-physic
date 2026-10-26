package cam.model;

/**
 * Immutable 3D Cartesian point for CAM calculations.
 * Pure Java record with zero graphical framework dependencies.
 */
public record CamPoint3D(double x, double y, double z) {

    public static final CamPoint3D ZERO = new CamPoint3D(0.0, 0.0, 0.0);

    public CamPoint3D add(CamVector3D v) {
        if (v == null) return this;
        return new CamPoint3D(x + v.dx(), y + v.dy(), z + v.dz());
    }

    public CamVector3D subtract(CamPoint3D other) {
        if (other == null) return new CamVector3D(x, y, z);
        return new CamVector3D(x - other.x, y - other.y, z - other.z);
    }

    public double distanceTo(CamPoint3D other) {
        if (other == null) return Double.NaN;
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceXY(CamPoint3D other) {
        if (other == null) return Double.NaN;
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
