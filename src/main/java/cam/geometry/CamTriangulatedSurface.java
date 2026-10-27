package cam.geometry;

import cam.model.CamPoint3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Validated, bounded triangulated surface for 3D CAM finishing operations.
 * Resolves surface height queries, boundary tests, and disjoint pass intervals.
 */
public class CamTriangulatedSurface {

    public record Interval1D(double startX, double endX) {
        public Interval1D {
            if (startX > endX) throw new IllegalArgumentException("startX cannot exceed endX.");
        }
        public double length() { return endX - startX; }
    }

    private final List<CamTriangle3D> triangles;
    private final double minX, maxX, minY, maxY, minZ, maxZ;

    public CamTriangulatedSurface(List<CamTriangle3D> triangles) {
        if (triangles == null || triangles.isEmpty()) {
            throw new IllegalArgumentException("Surface triangle list cannot be null or empty.");
        }
        double bMinX = Double.POSITIVE_INFINITY, bMaxX = Double.NEGATIVE_INFINITY;
        double bMinY = Double.POSITIVE_INFINITY, bMaxY = Double.NEGATIVE_INFINITY;
        double bMinZ = Double.POSITIVE_INFINITY, bMaxZ = Double.NEGATIVE_INFINITY;

        List<CamTriangle3D> copy = new ArrayList<>(triangles.size());
        for (CamTriangle3D tri : triangles) {
            if (tri == null) throw new IllegalArgumentException("Surface cannot contain null triangles.");
            copy.add(tri);
            bMinX = Math.min(bMinX, tri.getMinX());
            bMaxX = Math.max(bMaxX, tri.getMaxX());
            bMinY = Math.min(bMinY, tri.getMinY());
            bMaxY = Math.max(bMaxY, tri.getMaxY());
            bMinZ = Math.min(bMinZ, tri.getMinZ());
            bMaxZ = Math.max(bMaxZ, tri.getMaxZ());
        }
        this.triangles = Collections.unmodifiableList(copy);
        this.minX = bMinX; this.maxX = bMaxX;
        this.minY = bMinY; this.maxY = bMaxY;
        this.minZ = bMinZ; this.maxZ = bMaxZ;
    }

    public List<CamTriangle3D> getTriangles() { return triangles; }
    public int getTriangleCount()            { return triangles.size(); }
    public double getMinX()                  { return minX; }
    public double getMaxX()                  { return maxX; }
    public double getMinY()                  { return minY; }
    public double getMaxY()                  { return maxY; }
    public double getMinZ()                  { return minZ; }
    public double getMaxZ()                  { return maxZ; }

    public double queryHeight(double x, double y) {
        if (x < minX - 1e-9 || x > maxX + 1e-9 || y < minY - 1e-9 || y > maxY + 1e-9) {
            return Double.NaN;
        }
        double topZ = Double.NaN;
        for (CamTriangle3D tri : triangles) {
            if (tri.containsPoint2D(x, y)) {
                double z = tri.evaluateZ(x, y);
                if (Double.isFinite(z)) {
                    if (Double.isNaN(topZ) || z > topZ) {
                        topZ = z;
                    }
                }
            }
        }
        return topZ;
    }

    public boolean containsPoint2D(double x, double y) {
        return !Double.isNaN(queryHeight(x, y));
    }

    public List<Interval1D> computePassIntervals(double y) {
        if (y < minY - 1e-9 || y > maxY + 1e-9) return Collections.emptyList();
        List<Interval1D> raw = new ArrayList<>();
        for (CamTriangle3D tri : triangles) {
            double[] span = tri.intersectYLine(y);
            if (span.length == 2 && span[1] >= span[0] - 1e-9) {
                raw.add(new Interval1D(span[0], span[1]));
            }
        }
        if (raw.isEmpty()) return Collections.emptyList();
        raw.sort(Comparator.comparingDouble(Interval1D::startX));

        List<Interval1D> merged = new ArrayList<>();
        Interval1D cur = raw.get(0);
        for (int i = 1; i < raw.size(); i++) {
            Interval1D next = raw.get(i);
            if (next.startX() <= cur.endX() + 1e-6) {
                cur = new Interval1D(cur.startX(), Math.max(cur.endX(), next.endX()));
            } else {
                merged.add(cur);
                cur = next;
            }
        }
        merged.add(cur);
        return merged;
    }

    public static CamTriangulatedSurface createPlanarRectangle(double x1, double y1, double x2, double y2, double z) {
        CamPoint3D p1 = new CamPoint3D(x1, y1, z);
        CamPoint3D p2 = new CamPoint3D(x2, y1, z);
        CamPoint3D p3 = new CamPoint3D(x2, y2, z);
        CamPoint3D p4 = new CamPoint3D(x1, y2, z);
        return new CamTriangulatedSurface(List.of(
            new CamTriangle3D(p1, p2, p3),
            new CamTriangle3D(p1, p3, p4)
        ));
    }

    public static CamTriangulatedSurface createSlopedRamp(double x1, double y1, double x2, double y2, double z1, double z2) {
        CamPoint3D p1 = new CamPoint3D(x1, y1, z1);
        CamPoint3D p2 = new CamPoint3D(x2, y1, z1);
        CamPoint3D p3 = new CamPoint3D(x2, y2, z2);
        CamPoint3D p4 = new CamPoint3D(x1, y2, z2);
        return new CamTriangulatedSurface(List.of(
            new CamTriangle3D(p1, p2, p3),
            new CamTriangle3D(p1, p3, p4)
        ));
    }
}
