package cam.geometry;

import java.util.*;

/**
 * Validated 2D planar contour for CAM finishing operations.
 * Supports open toolpath chains and closed loops with explicit winding,
 * immutability, defensive copying, and strict non-self-intersecting invariants.
 */
public class CamContour2D {

    public static final double EPSILON = 1e-6;

    private final List<CamPoint2D> vertices;
    private final boolean closed;
    private final double signedArea;

    public CamContour2D(List<CamPoint2D> inputVertices, boolean closed) {
        if (inputVertices == null) throw new IllegalArgumentException("Vertices list cannot be null.");
        int minSize = closed ? 3 : 2;
        if (inputVertices.size() < minSize) {
            throw new IllegalArgumentException((closed ? "Closed" : "Open") + " contour requires at least " + minSize + " vertices.");
        }

        for (int i = 0; i < inputVertices.size(); i++) {
            CamPoint2D pt = inputVertices.get(i);
            if (pt == null || !pt.isFinite()) {
                throw new IllegalArgumentException("Vertex at index " + i + " is null or non-finite.");
            }
        }

        int n = inputVertices.size();
        int edgeCount = closed ? n : n - 1;
        for (int i = 0; i < edgeCount; i++) {
            CamPoint2D p1 = inputVertices.get(i);
            CamPoint2D p2 = inputVertices.get((i + 1) % n);
            if (p1.distanceTo(p2) <= EPSILON) {
                throw new IllegalArgumentException("Zero-length segment or duplicate consecutive vertex at index " + i);
            }
        }

        for (int i = 0; i < edgeCount; i++) {
            CamSegment2D segA = new CamSegment2D(inputVertices.get(i), inputVertices.get((i + 1) % n));
            for (int j = i + 2; j < edgeCount; j++) {
                if (closed && i == 0 && j == edgeCount - 1) continue;
                CamSegment2D segB = new CamSegment2D(inputVertices.get(j), inputVertices.get((j + 1) % n));
                if (segA.intersectsStrict(segB)) {
                    throw new IllegalArgumentException("Contour self-intersects: edge " + i + " and edge " + j);
                }
            }
        }

        if (closed) {
            double a = computeSignedArea(inputVertices);
            if (Math.abs(a) <= EPSILON) {
                throw new IllegalArgumentException("Closed contour has zero area or collinear vertices.");
            }
            this.signedArea = a;
        } else {
            this.signedArea = 0.0;
        }

        this.vertices = List.copyOf(inputVertices);
        this.closed = closed;
    }

    public static CamContour2D open(List<CamPoint2D> vertices) {
        return new CamContour2D(vertices, false);
    }

    public static CamContour2D closed(List<CamPoint2D> vertices) {
        return new CamContour2D(vertices, true);
    }

    public static CamContour2D rectangle(double minX, double minY, double maxX, double maxY) {
        if (minX >= maxX || minY >= maxY) throw new IllegalArgumentException("Invalid rectangle bounds.");
        return closed(List.of(
            new CamPoint2D(minX, minY), new CamPoint2D(maxX, minY),
            new CamPoint2D(maxX, maxY), new CamPoint2D(minX, maxY)
        ));
    }

    public boolean isClosed()                       { return closed; }
    public List<CamPoint2D> getVertices()           { return vertices; }
    public int getVertexCount()                     { return vertices.size(); }
    public int getSegmentCount()                    { return closed ? vertices.size() : vertices.size() - 1; }
    public double getSignedArea()                   { return signedArea; }
    public double getArea()                         { return Math.abs(signedArea); }
    public boolean isCounterClockwise()             { return signedArea > 0; }
    public CamPoint2D getStart()                    { return vertices.get(0); }
    public CamPoint2D getEnd()                      { return vertices.get(vertices.size() - 1); }

    public CamSegment2D getSegment(int i) {
        int n = vertices.size();
        if (i < 0 || i >= getSegmentCount()) throw new IndexOutOfBoundsException("Segment index: " + i);
        return new CamSegment2D(vertices.get(i), vertices.get((i + 1) % n));
    }

    public double totalLength() {
        double len = 0;
        for (int i = 0; i < getSegmentCount(); i++) len += getSegment(i).length();
        return len;
    }

    public CamContour2D reverse() {
        List<CamPoint2D> rev = new ArrayList<>(vertices);
        Collections.reverse(rev);
        return new CamContour2D(rev, closed);
    }

    public boolean contains(CamPoint2D pt) {
        if (!closed || pt == null || !pt.isFinite()) return false;
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
        if (pt == null || !pt.isFinite()) return Double.NaN;
        double minD = Double.MAX_VALUE;
        for (int i = 0; i < getSegmentCount(); i++) {
            minD = Math.min(minD, getSegment(i).distanceTo(pt));
        }
        return minD;
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
