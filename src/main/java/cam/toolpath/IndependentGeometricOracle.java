package cam.toolpath;

import cam.geometry.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Independent analytical geometric oracle for toolpath swept-envelope verification.
 * Employs exact segment-to-segment Euclidean distance minimization and continuous
 * capsule-polygon interference without point-sampling approximations.
 */
public final class IndependentGeometricOracle {

    public record OracleReport(boolean passed, double minDistanceMm, List<String> diagnostics) {}

    private IndependentGeometricOracle() {}

    public static OracleReport verifyContourFinishing(
            Toolpath toolpath, CamContour2D boundary, ContourSide side,
            double cutterRadius, double finishAllowance) {
        if (toolpath == null || boundary == null) {
            return new OracleReport(false, 0.0, List.of("Toolpath or boundary cannot be null."));
        }

        double requiredClearance = cutterRadius + finishAllowance;
        double minObservedDist = Double.MAX_VALUE;
        List<String> errors = new ArrayList<>();
        boolean inside = (side == ContourSide.INSIDE);

        for (int i = 0; i < toolpath.getSegments().size(); i++) {
            ToolpathSegment seg = toolpath.getSegments().get(i);
            if (!seg.motionType().isCutting()) continue;

            CamPoint2D t0 = new CamPoint2D(seg.start().x(), seg.start().y());
            CamPoint2D t1 = new CamPoint2D(seg.end().x(), seg.end().y());

            // 1. Independent material containment of endpoints
            if (boundary.isClosed()) {
                if (inside && (!boundary.contains(t0) || !boundary.contains(t1))) {
                    errors.add(String.format("Segment %d trajectory escapes cavity into retained material.", i));
                } else if (!inside && (boundary.contains(t0) || boundary.contains(t1))) {
                    errors.add(String.format("Segment %d trajectory penetrates nominal part solid.", i));
                }
            }

            // 2. Exact segment-to-segment distance across all boundary edges
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

        return new OracleReport(errors.isEmpty(), minObservedDist, errors);
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
        double projX = a.x() + t * dx, projY = a.y() + t * dy;
        return Math.hypot(p.x() - projX, p.y() - projY);
    }

    private static boolean segmentsIntersect(CamPoint2D a, CamPoint2D b, CamPoint2D c, CamPoint2D d) {
        double d1 = ccw(a, b, c), d2 = ccw(a, b, d);
        double d3 = ccw(c, d, a), d4 = ccw(c, d, b);
        return (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0)));
    }

    private static double ccw(CamPoint2D a, CamPoint2D b, CamPoint2D c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }
}
