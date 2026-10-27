package cam.geometry;

import java.util.Collections;
import java.util.List;

/**
 * Immutable geometric representation of a toolpath lead-in or lead-out move.
 * Provides endpoint continuity, tangency vector at contour, and discretised path points.
 */
public class CamLeadMove {

    private final CamLeadType type;
    private final CamPoint2D startPoint;
    private final CamPoint2D endPoint;
    private final CamPoint2D arcCenter;
    private final boolean isClockwiseArc;
    private final List<CamPoint2D> pathPoints;
    private final CamPoint2D tangentAtContour;

    public CamLeadMove(CamLeadType type, CamPoint2D startPoint, CamPoint2D endPoint,
                       CamPoint2D arcCenter, boolean isClockwiseArc,
                       List<CamPoint2D> pathPoints, CamPoint2D tangentAtContour) {
        if (type == null) throw new IllegalArgumentException("CamLeadType cannot be null.");
        if (startPoint == null || !startPoint.isFinite()) throw new IllegalArgumentException("Start point must be finite.");
        if (endPoint == null || !endPoint.isFinite()) throw new IllegalArgumentException("End point must be finite.");
        if (pathPoints == null || pathPoints.size() < 2) throw new IllegalArgumentException("Path points must contain at least 2 vertices.");

        this.type = type;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.arcCenter = arcCenter;
        this.isClockwiseArc = isClockwiseArc;
        this.pathPoints = List.copyOf(pathPoints);
        this.tangentAtContour = tangentAtContour;
    }

    public static CamLeadMove linear(CamLeadType type, CamPoint2D start, CamPoint2D end, CamPoint2D tangent) {
        return new CamLeadMove(type, start, end, null, false, List.of(start, end), tangent);
    }

    public CamLeadType getType()                    { return type; }
    public CamPoint2D getStartPoint()               { return startPoint; }
    public CamPoint2D getEndPoint()                 { return endPoint; }
    public CamPoint2D getArcCenter()                { return arcCenter; }
    public boolean isClockwiseArc()                 { return isClockwiseArc; }
    public List<CamPoint2D> getPathPoints()         { return pathPoints; }
    public CamPoint2D getTangentAtContour()         { return tangentAtContour; }

    public double length() {
        double total = 0.0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            total += pathPoints.get(i).distanceTo(pathPoints.get(i + 1));
        }
        return total;
    }

    @Override
    public String toString() {
        return "CamLeadMove{type=" + type + ", start=" + startPoint + ", end=" + endPoint + ", length=" + String.format("%.3f", length()) + "}";
    }
}
