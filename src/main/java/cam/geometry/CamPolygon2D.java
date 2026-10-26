package cam.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validated closed 2D planar polygon for CAM pocket boundaries and inward clearing offsets.
 */
public class CamPolygon2D {

    public static final double EPSILON = 1e-6;
    private final List<CamPoint2D> vertices;
    private final double signedArea;

    public CamPolygon2D(List<CamPoint2D> inputVertices) {
        if (inputVertices == null || inputVertices.size() < 3) {
            throw new IllegalArgumentException("Polygon must contain at least 3 vertices.");
        }
        for (CamPoint2D pt : inputVertices) {
            if (pt == null || !pt.isFinite()) {
                throw new IllegalArgumentException("Polygon vertices must have finite non-null coordinates.");
            }
        }

        int n = inputVertices.size();
        for (int i = 0; i < n; i++) {
            CamPoint2D v1 = inputVertices.get(i);
            CamPoint2D v2 = inputVertices.get((i + 1) % n);
            if (v1.distanceTo(v2) <= EPSILON) {
                throw new IllegalArgumentException("Polygon contains degenerate zero-length edge at index " + i);
            }
        }

        // Self-intersection check across non-adjacent edges
        for (int i = 0; i < n; i++) {
            CamSegment2D segA = new CamSegment2D(inputVertices.get(i), inputVertices.get((i + 1) % n));
            for (int j = i + 2; j < n; j++) {
                if (i == 0 && j == n - 1) continue; // adjacent wrap-around
                CamSegment2D segB = new CamSegment2D(inputVertices.get(j), inputVertices.get((j + 1) % n));
                if (segA.intersectsStrict(segB)) {
                    throw new IllegalArgumentException("Polygon edges intersect: edge " + i + " and edge " + j);
                }
            }
        }

        double area = computeSignedArea(inputVertices);
        if (Math.abs(area) <= EPSILON) {
            throw new IllegalArgumentException("Polygon has zero area or colinear vertices.");
        }

        List<CamPoint2D> ccwList = new ArrayList<>(inputVertices);
        if (area < 0) {
            Collections.reverse(ccwList);
            area = -area;
        }

        this.vertices = Collections.unmodifiableList(ccwList);
        this.signedArea = area;
    }

    public static CamPolygon2D createRectangle(double minX, double minY, double maxX, double maxY) {
        if (minX >= maxX || minY >= maxY) throw new IllegalArgumentException("Invalid rectangle bounds.");
        return new CamPolygon2D(List.of(
            new CamPoint2D(minX, minY),
            new CamPoint2D(maxX, minY),
            new CamPoint2D(maxX, maxY),
            new CamPoint2D(minX, maxY)
        ));
    }

    public List<CamPoint2D> getVertices() { return vertices; }
    public int getVertexCount() { return vertices.size(); }
    public double getArea() { return signedArea; }

    public boolean contains(CamPoint2D pt) {
        if (pt == null || !pt.isFinite()) return false;
        boolean inside = false;
        int n = vertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            CamPoint2D vi = vertices.get(i), vj = vertices.get(j);
            if (((vi.y() > pt.y()) != (vj.y() > pt.y()))
                && (pt.x() < (vj.x() - vi.x()) * (pt.y() - vi.y()) / (vj.y() - vi.y()) + vi.x())) {
                inside = !inside;
            }
        }
        return inside;
    }

    public double distanceToBoundary(CamPoint2D pt) {
        if (pt == null) return Double.NaN;
        double minDist = Double.MAX_VALUE;
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            CamSegment2D seg = new CamSegment2D(vertices.get(i), vertices.get((i + 1) % n));
            minDist = Math.min(minDist, seg.distanceTo(pt));
        }
        return minDist;
    }

    public CamPolygon2D createInwardOffset(double distance) {
        if (distance <= EPSILON) return this;
        int n = vertices.size();
        List<CamSegment2D> shiftedEdges = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            CamPoint2D p1 = vertices.get(i), p2 = vertices.get((i + 1) % n);
            double dx = p2.x() - p1.x(), dy = p2.y() - p1.y();
            double len = Math.sqrt(dx * dx + dy * dy);
            // Inward normal for CCW polygon: (-dy/len, dx/len)
            double nx = -dy / len * distance, ny = dx / len * distance;
            shiftedEdges.add(new CamSegment2D(new CamPoint2D(p1.x() + nx, p1.y() + ny), new CamPoint2D(p2.x() + nx, p2.y() + ny)));
        }

        List<CamPoint2D> offsetPts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            CamSegment2D e1 = shiftedEdges.get((i - 1 + n) % n);
            CamSegment2D e2 = shiftedEdges.get(i);
            CamPoint2D inter = CamSegment2D.lineIntersection(e1.p1(), e1.p2(), e2.p1(), e2.p2());
            if (inter == null || !inter.isFinite()) return null; // Parallel or degenerate
            offsetPts.add(inter);
        }

        try {
            CamPolygon2D candidate = new CamPolygon2D(offsetPts);
            // Verify candidate is strictly contained inside parent polygon
            for (CamPoint2D pt : candidate.getVertices()) {
                if (!this.contains(pt) || this.distanceToBoundary(pt) < distance - 1e-4) {
                    return null; // Collapsed or escaped
                }
            }
            return candidate;
        } catch (IllegalArgumentException ex) {
            return null; // Self-intersection or collapse
        }
    }

    private static double computeSignedArea(List<CamPoint2D> pts) {
        double a = 0;
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            CamPoint2D p1 = pts.get(i), p2 = pts.get((i + 1) % n);
            a += p1.x() * p2.y() - p2.x() * p1.y();
        }
        return a * 0.5;
    }
}
