package cam.model;

/**
 * Immutable 3D Cartesian vector for CAM toolpath directions and normals.
 */
public record CamVector3D(double dx, double dy, double dz) {

    public static final CamVector3D UNIT_X = new CamVector3D(1.0, 0.0, 0.0);
    public static final CamVector3D UNIT_Y = new CamVector3D(0.0, 1.0, 0.0);
    public static final CamVector3D UNIT_Z = new CamVector3D(0.0, 0.0, 1.0);

    public double length() {
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public CamVector3D normalize() {
        double len = length();
        if (len < 1e-12) return UNIT_Z;
        return new CamVector3D(dx / len, dy / len, dz / len);
    }

    public double dot(CamVector3D o) {
        if (o == null) return 0.0;
        return dx * o.dx + dy * o.dy + dz * o.dz;
    }

    public CamVector3D cross(CamVector3D o) {
        if (o == null) return UNIT_Z;
        return new CamVector3D(
            dy * o.dz - dz * o.dy,
            dz * o.dx - dx * o.dz,
            dx * o.dy - dy * o.dx
        );
    }

    public CamVector3D multiply(double factor) {
        return new CamVector3D(dx * factor, dy * factor, dz * factor);
    }
}
