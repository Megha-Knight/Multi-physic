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

    public boolean contains(CamPoint3D pt) {
        if (pt == null) return false;
        return pt.x() >= min.x() - 1e-6 && pt.x() <= max.x() + 1e-6
            && pt.y() >= min.y() - 1e-6 && pt.y() <= max.y() + 1e-6
            && pt.z() >= min.z() - 1e-6 && pt.z() <= max.z() + 1e-6;
    }
}
