package cam.toolpath;

import cam.geometry.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Independent analytical geometric oracle for toolpath swept-envelope verification.
 * Employs exact segment-to-segment Euclidean distance minimization, collinear intersection
 * detection, and independent ray-casting material containment without point-sampling.
 */
public final class IndependentGeometricOracle {

    public record OracleReport(boolean passed, double minDistanceMm, List<String> diagnostics) {}

    private IndependentGeometricOracle() {}

    public static OracleReport verifyContourFinishing(
            Toolpath toolpath, CamContour2D boundary, ContourSide side,
            double cutterRadius, double finishAllowance) {
        return verifyContourFinishingWithIslands(toolpath, boundary, List.of(), side, cutterRadius, finishAllowance, 0.0);
    }

    public static OracleReport verifyContourFinishingWithIslands(
            Toolpath toolpath, CamContour2D boundary, List<CamContour2D> islands,
            ContourSide side, double cutterRadius, double finishAllowance, double chordalSagittaTolerance) {
        if (toolpath == null || boundary == null) {
            return new OracleReport(false, 0.0, List.of("Toolpath or boundary cannot be null."));
        }

        double requiredClearance = cutterRadius + finishAllowance + chordalSagittaTolerance;
        double minObservedDist = Double.MAX_VALUE;
        List<String> errors = new ArrayList<>();
        boolean inside = (side == ContourSide.INSIDE);

        List<CamPoint2D> bVerts = boundary.getVertices();

        for (int i = 0; i < toolpath.getSegments().size(); i++) {
            ToolpathSegment seg = toolpath.getSegments().get(i);
            if (!seg.motionType().isCutting()) continue;

            CamPoint2D t0 = new CamPoint2D(seg.start().x(), seg.start().y());
            CamPoint2D t1 = new CamPoint2D(seg.end().x(), seg.end().y());
            CamPoint2D tMid = new CamPoint2D((t0.x() + t1.x()) * 0.5, (t0.y() + t1.y()) * 0.5);

            // 1. Independent material containment (ray casting parity)
            if (boundary.isClosed()) {
                boolean in0 = pointInPolygon(t0, bVerts);
                boolean in1 = pointInPolygon(t1, bVerts);
                boolean inM = pointInPolygon(tMid, bVerts);

                if (inside && (!in0 || !in1 || !inM)) {
                    errors.add(String.format("Segment %d trajectory escapes cavity into retained material.", i));
                } else if (!inside && (in0 || in1 || inM)) {
                    errors.add(String.format("Segment %d trajectory penetrates nominal part solid.", i));
                }
            }

            // 2. Island material retention
            if (islands != null) {
                for (int isIdx = 0; isIdx < islands.size(); isIdx++) {
                    CamContour2D isl = islands.get(isIdx);
                    if (!isl.isClosed()) continue;
                    List<CamPoint2D> iVerts = isl.getVertices();
                    if (pointInPolygon(t0, iVerts) || pointInPolygon(t1, iVerts) || pointInPolygon(tMid, iVerts)) {
                        errors.add(String.format("Segment %d trajectory penetrates retained island %d.", i, isIdx));
                    }
                    for (int ib = 0; ib < isl.getSegmentCount(); ib++) {
                        CamSegment2D iSeg = isl.getSegment(ib);
                        double idist = segmentToSegmentDistance(t0, t1, iSeg.p1(), iSeg.p2());
                        if (idist < minObservedDist) minObservedDist = idist;
                        if (idist < requiredClearance - 1e-3) {
                            errors.add(String.format("Segment %d swept capsule violates island %d edge %d: dist = %.4f mm < %.4f mm",
                                i, isIdx, ib, idist, requiredClearance));
                            break;
                        }
                    }
                }
            }

            // 3. Exact segment-to-segment distance across all boundary edges
            for (int b = 0; b < boundary.getSegmentCount(); b++) {
                CamSegment2D bSeg = boundary.getSegment(b);
                double d = segmentToSegmentDistance(t0, t1, bSeg.p1(), bSeg.p2());
                if (d < minObservedDist) minObservedDist = d;

                if (boundary.isClosed() && d < requiredClearance - 1e-3) {
                    errors.add(String.format("Segment %d swept capsule violates boundary edge %d: dist = %.4f mm < %.4f mm",
                            i, b, d, requiredClearance));
                    break;
                }
            }
        }

        return new OracleReport(errors.isEmpty(), minObservedDist, Collections.unmodifiableList(errors));
    }

    public static boolean pointInPolygon(CamPoint2D pt, List<CamPoint2D> poly) {
        if (poly == null || poly.size() < 3) return false;
        boolean inside = false;
        double px = pt.x(), py = pt.y();
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            double xi = poly.get(i).x(), yi = poly.get(i).y();
            double xj = poly.get(j).x(), yj = poly.get(j).y();
            boolean intersect = ((yi > py) != (yj > py))
                && (px < (xj - xi) * (py - yi) / (yj - yi + 1e-15) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    public static double segmentToSegmentDistance(CamPoint2D a, CamPoint2D b, CamPoint2D c, CamPoint2D d) {
        if (segmentsIntersect(a, b, c, d)) return 0.0;
        double d1 = pointToSegmentDistance(a, c, d);
        double d2 = pointToSegmentDistance(b, c, d);
        double d3 = pointToSegmentDistance(c, a, b);
        double d4 = pointToSegmentDistance(d, a, b);
        return Math.min(Math.min(d1, d2), Math.min(d3, d4));
    }

    public static double pointToSegmentDistance(CamPoint2D p, CamPoint2D a, CamPoint2D b) {
        double dx = b.x() - a.x(), dy = b.y() - a.y();
        double len2 = dx * dx + dy * dy;
        if (len2 <= 1e-12) return p.distanceTo(a);
        double t = ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / len2;
        t = Math.max(0.0, Math.min(1.0, t));
        return Math.hypot(p.x() - (a.x() + t * dx), p.y() - (a.y() + t * dy));
    }

    public static boolean segmentsIntersect(CamPoint2D a, CamPoint2D b, CamPoint2D c, CamPoint2D d) {
        double d1 = ccw(a, b, c), d2 = ccw(a, b, d);
        double d3 = ccw(c, d, a), d4 = ccw(c, d, b);
        if (((d1 > 1e-9 && d2 < -1e-9) || (d1 < -1e-9 && d2 > 1e-9))
                && ((d3 > 1e-9 && d4 < -1e-9) || (d3 < -1e-9 && d4 > 1e-9))) {
            return true;
        }
        if (Math.abs(d1) <= 1e-9 && onSegment(a, b, c)) return true;
        if (Math.abs(d2) <= 1e-9 && onSegment(a, b, d)) return true;
        if (Math.abs(d3) <= 1e-9 && onSegment(c, d, a)) return true;
        if (Math.abs(d4) <= 1e-9 && onSegment(c, d, b)) return true;
        return false;
    }

    private static boolean onSegment(CamPoint2D p, CamPoint2D q, CamPoint2D r) {
        return r.x() >= Math.min(p.x(), q.x()) - 1e-9 && r.x() <= Math.max(p.x(), q.x()) + 1e-9
            && r.y() >= Math.min(p.y(), q.y()) - 1e-9 && r.y() <= Math.max(p.y(), q.y()) + 1e-9;
    }

    private static double ccw(CamPoint2D a, CamPoint2D b, CamPoint2D c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }
}
