package cam.geometry;

import java.util.*;

/**
 * Deterministic 2D contour offset engine for CAM profile finishing.
 * Handles open paths and closed boundaries, inside/outside orientation,
 * miter joins with configurable limits, and collapse/self-intersection detection.
 */
public final class CamContourOffsetEngine {

    public static final double DEFAULT_MITER_LIMIT = 2.0;

    private CamContourOffsetEngine() {}

    public static CamContourOffsetResult offset(CamContour2D contour, double distance, ContourSide side) {
        return offset(contour, distance, side, DEFAULT_MITER_LIMIT);
    }

    public static CamContourOffsetResult offset(CamContour2D contour, double distance, ContourSide side, double miterLimit) {
        if (contour == null) return CamContourOffsetResult.failure("Contour cannot be null.");
        if (distance <= CamContour2D.EPSILON || !Double.isFinite(distance)) {
            return CamContourOffsetResult.failure("Offset distance must be strictly positive and finite (got: " + distance + ").");
        }
        if (side == null) return CamContourOffsetResult.failure("ContourSide cannot be null.");
        if (miterLimit < 1.0 || !Double.isFinite(miterLimit)) {
            return CamContourOffsetResult.failure("Miter limit must be >= 1.0 (got: " + miterLimit + ").");
        }

        ContourSide travelSide;
        try {
            travelSide = side.resolveTravelSide(contour.isClosed(), contour.isCounterClockwise());
        } catch (IllegalArgumentException ex) {
            return CamContourOffsetResult.failure(ex.getMessage());
        }

        int segCount = contour.getSegmentCount();
        List<CamSegment2D> shiftedEdges = new ArrayList<>(segCount);

        for (int i = 0; i < segCount; i++) {
            CamSegment2D seg = contour.getSegment(i);
            CamPoint2D p1 = seg.p1(), p2 = seg.p2();
            double dx = p2.x() - p1.x(), dy = p2.y() - p1.y();
            double len = Math.sqrt(dx * dx + dy * dy);
            double nx = (travelSide == ContourSide.LEFT) ? -dy / len : dy / len;
            double ny = (travelSide == ContourSide.LEFT) ? dx / len : -dx / len;

            CamPoint2D sp1 = new CamPoint2D(p1.x() + nx * distance, p1.y() + ny * distance);
            CamPoint2D sp2 = new CamPoint2D(p2.x() + nx * distance, p2.y() + ny * distance);
            shiftedEdges.add(new CamSegment2D(sp1, sp2));
        }

        List<CamPoint2D> offsetPts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (contour.isClosed()) {
            for (int i = 0; i < segCount; i++) {
                CamSegment2D e1 = shiftedEdges.get((i - 1 + segCount) % segCount);
                CamSegment2D e2 = shiftedEdges.get(i);
                CamPoint2D cornerV = contour.getVertices().get(i);
                addCornerJoin(offsetPts, warnings, cornerV, e1, e2, distance, miterLimit);
            }
        } else {
            offsetPts.add(shiftedEdges.get(0).p1());
            for (int i = 1; i < segCount; i++) {
                CamSegment2D e1 = shiftedEdges.get(i - 1);
                CamSegment2D e2 = shiftedEdges.get(i);
                CamPoint2D cornerV = contour.getVertices().get(i);
                addCornerJoin(offsetPts, warnings, cornerV, e1, e2, distance, miterLimit);
            }
            offsetPts.add(shiftedEdges.get(segCount - 1).p2());
        }

        // Deduplicate consecutive points
        List<CamPoint2D> cleanPts = new ArrayList<>();
        for (CamPoint2D pt : offsetPts) {
            if (cleanPts.isEmpty() || cleanPts.get(cleanPts.size() - 1).distanceTo(pt) > CamContour2D.EPSILON) {
                cleanPts.add(pt);
            }
        }
        if (contour.isClosed() && cleanPts.size() > 1
                && cleanPts.get(0).distanceTo(cleanPts.get(cleanPts.size() - 1)) <= CamContour2D.EPSILON) {
            cleanPts.remove(cleanPts.size() - 1);
        }

        CamContour2D candidate;
        try {
            candidate = new CamContour2D(cleanPts, contour.isClosed());
        } catch (IllegalArgumentException ex) {
            return CamContourOffsetResult.failure("Offset self-intersection or collapse: " + ex.getMessage());
        }

        if (contour.isClosed()) {
            if (candidate.isCounterClockwise() != contour.isCounterClockwise()) {
                return CamContourOffsetResult.failure("Inverted loop: resulting contour winding flipped from "
                    + (contour.isCounterClockwise() ? "CCW" : "CW") + " to "
                    + (candidate.isCounterClockwise() ? "CCW" : "CW") + ".");
            }
            if (side == ContourSide.INSIDE) {
                if (candidate.getArea() >= contour.getArea() - 1e-4) {
                    return CamContourOffsetResult.failure("Inside offset collapse: offset area (" + candidate.getArea()
                        + ") >= original area (" + contour.getArea() + ").");
                }
                for (CamPoint2D pt : candidate.getVertices()) {
                    if (!contour.contains(pt)) {
                        return CamContourOffsetResult.failure("Inside offset breached original boundary.");
                    }
                    if (contour.distanceToBoundary(pt) < distance - 1e-4) {
                        return CamContourOffsetResult.failure("Inside offset collapsed into opposing boundary (distance "
                            + contour.distanceToBoundary(pt) + " < " + distance + ").");
                    }
                }
            } else if (side == ContourSide.OUTSIDE) {
                if (candidate.getArea() <= contour.getArea() + 1e-4) {
                    return CamContourOffsetResult.failure("Outside offset collapse: offset area (" + candidate.getArea()
                        + ") <= original area (" + contour.getArea() + ").");
                }
            }
        }

        return CamContourOffsetResult.success(candidate, warnings);
    }

    private static void addCornerJoin(List<CamPoint2D> pts, List<String> warnings,
                                      CamPoint2D cornerV, CamSegment2D e1, CamSegment2D e2,
                                      double distance, double miterLimit) {
        CamPoint2D miterPt = CamSegment2D.lineIntersection(e1.p1(), e1.p2(), e2.p1(), e2.p2());
        if (miterPt != null && miterPt.isFinite()) {
            double miterDist = cornerV.distanceTo(miterPt);
            if (miterDist <= miterLimit * distance + 1e-5) {
                pts.add(miterPt);
                return;
            }
            warnings.add("Miter limit exceeded at corner " + cornerV + "; applied beveled join.");
        }
        pts.add(e1.p2());
        pts.add(e2.p1());
    }
}
