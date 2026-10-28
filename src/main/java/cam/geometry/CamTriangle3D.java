package cam.geometry;

import cam.model.CamPoint3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validated, immutable 3D triangle for bounded CAD/CAM surface representations.
 * Provides exact analytical planar evaluation, 2D XY projection, and line intersection.
 */
public class CamTriangle3D {

    private final CamPoint3D a, b, c;
    private final double normA, normB, normC, planeD;
    private final double minX, maxX, minY, maxY, minZ, maxZ;
    private final double area3D;
    private final boolean vertical;

    public CamTriangle3D(CamPoint3D a, CamPoint3D b, CamPoint3D c) {
        if (a == null || b == null || c == null) {
            throw new IllegalArgumentException("Triangle vertices cannot be null.");
        }
        if (!isFinitePoint(a) || !isFinitePoint(b) || !isFinitePoint(c)) {
            throw new IllegalArgumentException("Triangle vertex coordinates must be finite.");
        }
        this.a = a;
        this.b = b;
        this.c = c;

        double ux = b.x() - a.x(), uy = b.y() - a.y(), uz = b.z() - a.z();
        double vx = c.x() - a.x(), vy = c.y() - a.y(), vz = c.z() - a.z();

        this.normA = uy * vz - uz * vy;
        this.normB = uz * vx - ux * vz;
        this.normC = ux * vy - uy * vx;

        double normLen = Math.sqrt(normA * normA + normB * normB + normC * normC);
        this.area3D = 0.5 * normLen;
        if (normLen < 1e-9 || area3D < 1e-9) {
            throw new IllegalArgumentException("Degenerate triangle: collinear vertices or near-zero area.");
        }

        this.planeD = -(normA * a.x() + normB * a.y() + normC * a.z());
        this.vertical = Math.abs(normC) < 1e-9;

        this.minX = Math.min(a.x(), Math.min(b.x(), c.x()));
        this.maxX = Math.max(a.x(), Math.max(b.x(), c.x()));
        this.minY = Math.min(a.y(), Math.min(b.y(), c.y()));
        this.maxY = Math.max(a.y(), Math.max(b.y(), c.y()));
        this.minZ = Math.min(a.z(), Math.min(b.z(), c.z()));
        this.maxZ = Math.max(a.z(), Math.max(b.z(), c.z()));
    }

    private static boolean isFinitePoint(CamPoint3D p) {
        return Double.isFinite(p.x()) && Double.isFinite(p.y()) && Double.isFinite(p.z());
    }

    public CamPoint3D a()        { return a; }
    public CamPoint3D b()        { return b; }
    public CamPoint3D c()        { return c; }
    public double getArea3D()    { return area3D; }
    public boolean isVertical()  { return vertical; }

    public double getMinX()      { return minX; }
    public double getMaxX()      { return maxX; }
    public double getMinY()      { return minY; }
    public double getMaxY()      { return maxY; }
    public double getMinZ()      { return minZ; }
    public double getMaxZ()      { return maxZ; }

    public double getNormalX()   { return normA; }
    public double getNormalY()   { return normB; }
    public double getNormalZ()   { return normC; }

    public boolean containsPoint2D(double x, double y) {
        if (vertical || x < minX - 1e-9 || x > maxX + 1e-9 || y < minY - 1e-9 || y > maxY + 1e-9) {
            return false;
        }
        double l1 = ((b.x() - x) * (c.y() - y) - (b.y() - y) * (c.x() - x)) / normC;
        double l2 = ((c.x() - x) * (a.y() - y) - (c.y() - y) * (a.x() - x)) / normC;
        double l3 = 1.0 - l1 - l2;
        double eps = 1e-9;
        return (l1 >= -eps && l2 >= -eps && l3 >= -eps);
    }

    public double evaluateZ(double x, double y) {
        if (vertical) return Double.NaN;
        return -(normA * x + normB * y + planeD) / normC;
    }

    public double[] intersectYLine(double y) {
        if (y < minY - 1e-9 || y > maxY + 1e-9) return new double[0];
        List<Double> xs = new ArrayList<>(3);
        intersectEdgeY(a, b, y, xs);
        intersectEdgeY(b, c, y, xs);
        intersectEdgeY(c, a, y, xs);
        if (xs.isEmpty()) return new double[0];
        Collections.sort(xs);
        return new double[]{ xs.get(0), xs.get(xs.size() - 1) };
    }

    private void intersectEdgeY(CamPoint3D p1, CamPoint3D p2, double y, List<Double> out) {
        double y1 = p1.y(), y2 = p2.y();
        if ((y1 <= y + 1e-9 && y2 >= y - 1e-9) || (y2 <= y + 1e-9 && y1 >= y - 1e-9)) {
            double dy = y2 - y1;
            if (Math.abs(dy) < 1e-9) {
                out.add(p1.x());
                out.add(p2.x());
            } else {
                double t = (y - y1) / dy;
                if (t >= -1e-9 && t <= 1.0 + 1e-9) {
                    double clampedT = Math.max(0.0, Math.min(1.0, t));
                    out.add(p1.x() + clampedT * (p2.x() - p1.x()));
                }
            }
        }
    }

    public CamPoint3D[] intersectZPlane(double z) {
        if (z < minZ - 1e-9 || z > maxZ + 1e-9) return new CamPoint3D[0];
        if (Math.abs(normA) < 1e-9 && Math.abs(normB) < 1e-9) return new CamPoint3D[0];
        List<CamPoint3D> pts = new ArrayList<>(3);
        intersectEdgeZ(a, b, z, pts);
        intersectEdgeZ(b, c, z, pts);
        intersectEdgeZ(c, a, z, pts);
        if (pts.size() < 2) return new CamPoint3D[0];
        List<CamPoint3D> unique = new ArrayList<>(2);
        for (CamPoint3D p : pts) {
            boolean dup = false;
            for (CamPoint3D u : unique) {
                if (p.distanceTo(u) < 1e-6) { dup = true; break; }
            }
            if (!dup) unique.add(p);
        }
        if (unique.size() < 2) return new CamPoint3D[0];
        if (unique.get(0).distanceTo(unique.get(1)) < 1e-6) return new CamPoint3D[0];
        return new CamPoint3D[]{ unique.get(0), unique.get(1) };
    }

    private void intersectEdgeZ(CamPoint3D p1, CamPoint3D p2, double z, List<CamPoint3D> out) {
        double z1 = p1.z(), z2 = p2.z();
        if ((z1 <= z + 1e-9 && z2 >= z - 1e-9) || (z2 <= z + 1e-9 && z1 >= z - 1e-9)) {
            double dz = z2 - z1;
            if (Math.abs(dz) < 1e-9) {
                out.add(p1);
                out.add(p2);
            } else {
                double t = (z - z1) / dz;
                if (t >= -1e-9 && t <= 1.0 + 1e-9) {
                    double ct = Math.max(0.0, Math.min(1.0, t));
                    out.add(new CamPoint3D(p1.x() + ct * (p2.x() - p1.x()), p1.y() + ct * (p2.y() - p1.y()), z));
                }
            }
        }
    }
}
