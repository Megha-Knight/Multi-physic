package cam.model;

/**
 * Represents the raw material billet/stock bounding volume in CAM.
 */
public class StockDimensions {

    private final CamPoint3D min;
    private final CamPoint3D max;

    public StockDimensions(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Invalid stock bounds: min cannot exceed max.");
        }
        this.min = new CamPoint3D(minX, minY, minZ);
        this.max = new CamPoint3D(maxX, maxY, maxZ);
    }

    public StockDimensions(CamPoint3D min, CamPoint3D max) {
        this(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    public static StockDimensions fromFinishedPart(double minX, double minY, double minZ,
                                                  double maxX, double maxY, double maxZ,
                                                  double marginXY, double marginZTop, double marginZBottom) {
        return new StockDimensions(
            minX - Math.max(0.0, marginXY),
            minY - Math.max(0.0, marginXY),
            minZ - Math.max(0.0, marginZBottom),
            maxX + Math.max(0.0, marginXY),
            maxY + Math.max(0.0, marginXY),
            maxZ + Math.max(0.0, marginZTop)
        );
    }

    public CamPoint3D getMin() { return min; }
    public CamPoint3D getMax() { return max; }
    public double getLengthX() { return max.x() - min.x(); }
    public double getWidthY()  { return max.y() - min.y(); }
    public double getHeightZ() { return max.z() - min.z(); }
    public double getTopZ()    { return max.z(); }
    public double getBottomZ() { return min.z(); }

    public boolean isValid() {
        return min != null && max != null
            && Double.isFinite(min.x()) && Double.isFinite(min.y()) && Double.isFinite(min.z())
            && Double.isFinite(max.x()) && Double.isFinite(max.y()) && Double.isFinite(max.z())
            && min.x() <= max.x() && min.y() <= max.y() && min.z() <= max.z()
            && (max.x() - min.x() > 1e-9 || max.y() - min.y() > 1e-9 || max.z() - min.z() > 1e-9);
    }

    public boolean intersectsSegment(CamPoint3D p1, CamPoint3D p2, double margin) {
        if (p1 == null || p2 == null || !isValid()) return false;
        double m = Math.max(0.0, margin);
        double bx0 = min.x() - m, bx1 = max.x() + m;
        double by0 = min.y() - m, by1 = max.y() + m;
        double bz0 = min.z() - m, bz1 = max.z() + m;

        double dx = p2.x() - p1.x(), dy = p2.y() - p1.y(), dz = p2.z() - p1.z();
        double tMin = 0.0, tMax = 1.0;

        if (Math.abs(dx) < 1e-12) {
            if (p1.x() < bx0 || p1.x() > bx1) return false;
        } else {
            double t1 = (bx0 - p1.x()) / dx, t2 = (bx1 - p1.x()) / dx;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
            if (tMin > tMax) return false;
        }

        if (Math.abs(dy) < 1e-12) {
            if (p1.y() < by0 || p1.y() > by1) return false;
        } else {
            double t1 = (by0 - p1.y()) / dy, t2 = (by1 - p1.y()) / dy;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
            if (tMin > tMax) return false;
        }

        if (Math.abs(dz) < 1e-12) {
            if (p1.z() < bz0 || p1.z() > bz1) return false;
        } else {
            double t1 = (bz0 - p1.z()) / dz, t2 = (bz1 - p1.z()) / dz;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
            if (tMin > tMax) return false;
        }

        return tMin <= tMax && tMax >= 0.0 && tMin <= 1.0;
    }

    public boolean contains(CamPoint3D pt) {
        if (pt == null) return false;
        return pt.x() >= min.x() - 1e-6 && pt.x() <= max.x() + 1e-6
            && pt.y() >= min.y() - 1e-6 && pt.y() <= max.y() + 1e-6
            && pt.z() >= min.z() - 1e-6 && pt.z() <= max.z() + 1e-6;
    }
}
