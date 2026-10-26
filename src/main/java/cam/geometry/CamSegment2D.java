package cam.geometry;

/**
 * Immutable 2D line segment for polygon edges and toolpath intersection tests.
 */
public record CamSegment2D(CamPoint2D p1, CamPoint2D p2) {

    public static final double EPSILON = 1e-6;

    public CamSegment2D {
        if (p1 == null || p2 == null) {
            throw new IllegalArgumentException("Segment endpoints cannot be null.");
        }
    }

    public double length() {
        return p1.distanceTo(p2);
    }

    public boolean isDegenerate() {
        return length() <= EPSILON;
    }

    public double distanceTo(CamPoint2D pt) {
        if (pt == null) return Double.NaN;
        double l2 = Math.pow(p1.distanceTo(p2), 2);
        if (l2 <= EPSILON) return pt.distanceTo(p1);

        double t = ((pt.x() - p1.x()) * (p2.x() - p1.x()) + (pt.y() - p1.y()) * (p2.y() - p1.y())) / l2;
        t = Math.max(0.0, Math.min(1.0, t));
        CamPoint2D proj = new CamPoint2D(p1.x() + t * (p2.x() - p1.x()), p1.y() + t * (p2.y() - p1.y()));
        return pt.distanceTo(proj);
    }

    public boolean intersectsStrict(CamSegment2D other) {
        if (other == null) return false;
        double ccw1 = ccw(p1, p2, other.p1);
        double ccw2 = ccw(p1, p2, other.p2);
        double ccw3 = ccw(other.p1, other.p2, p1);
        double ccw4 = ccw(other.p1, other.p2, p2);

        return ((ccw1 > EPSILON && ccw2 < -EPSILON) || (ccw1 < -EPSILON && ccw2 > EPSILON))
            && ((ccw3 > EPSILON && ccw4 < -EPSILON) || (ccw3 < -EPSILON && ccw4 > EPSILON));
    }

    private static double ccw(CamPoint2D a, CamPoint2D b, CamPoint2D c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    public static CamPoint2D lineIntersection(CamPoint2D a, CamPoint2D b, CamPoint2D c, CamPoint2D d) {
        double a1 = b.y() - a.y(), b1 = a.x() - b.x(), c1 = a1 * a.x() + b1 * a.y();
        double a2 = d.y() - c.y(), b2 = c.x() - d.x(), c2 = a2 * c.x() + b2 * c.y();
        double det = a1 * b2 - a2 * b1;
        if (Math.abs(det) < EPSILON) return null;
        return new CamPoint2D((b2 * c1 - b1 * c2) / det, (a1 * c2 - a2 * c1) / det);
    }
}
