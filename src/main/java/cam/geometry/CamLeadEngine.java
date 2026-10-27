package cam.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic generator for CAM toolpath lead-in and lead-out moves.
 * Produces tangent linear and circular arc moves with endpoint continuity,
 * tangency validation, and part-boundary gouge protection.
 */
public final class CamLeadEngine {

    private static final int ARC_STEPS = 12;

    private CamLeadEngine() {}

    public static CamLeadResult generateLeads(CamContour2D contour, ContourSide side,
                                             CamLeadConfig inConfig, CamLeadConfig outConfig) {
        return generateLeads(contour, 0, side, inConfig, outConfig, null);
    }

    public static CamLeadResult generateLeads(CamContour2D contour, ContourSide side,
                                             CamLeadConfig inConfig, CamLeadConfig outConfig,
                                             CamContour2D partBoundary) {
        return generateLeads(contour, 0, side, inConfig, outConfig, partBoundary);
    }

    public static CamLeadResult generateLeads(CamContour2D contour, int entryIdx, ContourSide side,
                                             CamLeadConfig inCfg, CamLeadConfig outCfg,
                                             CamContour2D partBoundary) {
        if (contour == null) return CamLeadResult.failure("Contour cannot be null.");
        if (side == null) return CamLeadResult.failure("ContourSide cannot be null.");
        if (inCfg == null) inCfg = CamLeadConfig.none();
        if (outCfg == null) outCfg = CamLeadConfig.none();

        int n = contour.getVertexCount();
        if (entryIdx < 0 || entryIdx >= n) {
            return CamLeadResult.failure("Invalid entry index: " + entryIdx + " (vertex count: " + n + ").");
        }

        ContourSide travelSide;
        try {
            travelSide = side.resolveTravelSide(contour.isClosed(), contour.isCounterClockwise());
        } catch (IllegalArgumentException ex) {
            return CamLeadResult.failure(ex.getMessage());
        }

        CamPoint2D pIn = contour.getVertices().get(entryIdx);
        CamPoint2D pNext = contour.getVertices().get((entryIdx + 1) % n);
        CamPoint2D tIn = unitTangent(pIn, pNext);
        CamPoint2D nIn = normal(tIn, travelSide);

        int exitIdx = contour.isClosed() ? entryIdx : n - 1;
        CamPoint2D pOut = contour.getVertices().get(exitIdx);
        CamPoint2D pPrev = contour.getVertices().get(contour.isClosed() ? (exitIdx - 1 + n) % n : exitIdx - 1);
        CamPoint2D tOut = unitTangent(pPrev, pOut);
        CamPoint2D nOut = normal(tOut, travelSide);

        CamLeadMove leadIn = createLeadIn(inCfg, pIn, tIn, nIn);
        CamLeadMove leadOut = createLeadOut(outCfg, pOut, tOut, nOut);

        List<String> errors = new ArrayList<>();
        validateGouge(leadIn, partBoundary, pIn, "Lead-in", errors);
        validateGouge(leadOut, partBoundary, pOut, "Lead-out", errors);
        if (!errors.isEmpty()) return CamLeadResult.failure(errors);

        List<CamPoint2D> combined = new ArrayList<>();
        if (leadIn != null) combined.addAll(leadIn.getPathPoints());
        else combined.add(pIn);

        if (contour.isClosed()) {
            for (int i = 1; i <= n; i++) combined.add(contour.getVertices().get((entryIdx + i) % n));
        } else {
            for (int i = 1; i < n; i++) combined.add(contour.getVertices().get(i));
        }

        if (leadOut != null) {
            List<CamPoint2D> outPts = leadOut.getPathPoints();
            for (int i = 1; i < outPts.size(); i++) combined.add(outPts.get(i));
        }

        return CamLeadResult.success(leadIn, leadOut, CamContour2D.open(combined));
    }

    private static CamLeadMove createLeadIn(CamLeadConfig cfg, CamPoint2D p, CamPoint2D t, CamPoint2D n) {
        if (cfg.type() == CamLeadType.NONE) return null;
        if (cfg.type() == CamLeadType.TANGENT_LINE) {
            CamPoint2D start = new CamPoint2D(p.x() - t.x() * cfg.linearLength(), p.y() - t.y() * cfg.linearLength());
            return CamLeadMove.linear(CamLeadType.TANGENT_LINE, start, p, t);
        }
        if (cfg.type() == CamLeadType.PERPENDICULAR_LINE) {
            CamPoint2D start = new CamPoint2D(p.x() + n.x() * cfg.linearLength(), p.y() + n.y() * cfg.linearLength());
            return CamLeadMove.linear(CamLeadType.PERPENDICULAR_LINE, start, p, t);
        }
        double r = cfg.arcRadius(), theta = Math.toRadians(cfg.arcAngleDegrees());
        CamPoint2D center = new CamPoint2D(p.x() + n.x() * r, p.y() + n.y() * r);
        double endAng = Math.atan2(p.y() - center.y(), p.x() - center.x());
        boolean cw = (t.x() * n.y() - t.y() * n.x()) < 0;
        double startAng = cw ? endAng + theta : endAng - theta;
        List<CamPoint2D> pts = sampleArc(center, r, startAng, endAng, cw);
        return new CamLeadMove(CamLeadType.CIRCULAR_ARC, pts.get(0), p, center, cw, pts, t);
    }

    private static CamLeadMove createLeadOut(CamLeadConfig cfg, CamPoint2D p, CamPoint2D t, CamPoint2D n) {
        if (cfg.type() == CamLeadType.NONE) return null;
        if (cfg.type() == CamLeadType.TANGENT_LINE) {
            CamPoint2D end = new CamPoint2D(p.x() + t.x() * cfg.linearLength(), p.y() + t.y() * cfg.linearLength());
            return CamLeadMove.linear(CamLeadType.TANGENT_LINE, p, end, t);
        }
        if (cfg.type() == CamLeadType.PERPENDICULAR_LINE) {
            CamPoint2D end = new CamPoint2D(p.x() + n.x() * cfg.linearLength(), p.y() + n.y() * cfg.linearLength());
            return CamLeadMove.linear(CamLeadType.PERPENDICULAR_LINE, p, end, t);
        }
        double r = cfg.arcRadius(), theta = Math.toRadians(cfg.arcAngleDegrees());
        CamPoint2D center = new CamPoint2D(p.x() + n.x() * r, p.y() + n.y() * r);
        double startAng = Math.atan2(p.y() - center.y(), p.x() - center.x());
        boolean cw = (t.x() * n.y() - t.y() * n.x()) < 0;
        double endAng = cw ? startAng - theta : startAng + theta;
        List<CamPoint2D> pts = sampleArc(center, r, startAng, endAng, cw);
        return new CamLeadMove(CamLeadType.CIRCULAR_ARC, p, pts.get(pts.size() - 1), center, cw, pts, t);
    }

    private static List<CamPoint2D> sampleArc(CamPoint2D c, double r, double a1, double a2, boolean cw) {
        List<CamPoint2D> pts = new ArrayList<>(ARC_STEPS + 1);
        double span = cw ? (a2 > a1 ? a2 - a1 - 2 * Math.PI : a2 - a1) : (a2 < a1 ? a2 - a1 + 2 * Math.PI : a2 - a1);
        for (int i = 0; i <= ARC_STEPS; i++) {
            double ang = a1 + span * (i / (double) ARC_STEPS);
            pts.add(new CamPoint2D(c.x() + r * Math.cos(ang), c.y() + r * Math.sin(ang)));
        }
        return pts;
    }

    private static void validateGouge(CamLeadMove lead, CamContour2D part, CamPoint2D touch, String name, List<String> errs) {
        if (lead == null || part == null) return;
        List<CamPoint2D> pts = lead.getPathPoints();
        for (int i = 0; i < pts.size() - 1; i++) {
            CamSegment2D seg = new CamSegment2D(pts.get(i), pts.get(i + 1));
            for (int s = 0; s < part.getSegmentCount(); s++) {
                if (seg.intersectsStrict(part.getSegment(s))) {
                    errs.add(name + " intersects part boundary at segment " + s + ".");
                    return;
                }
            }
        }
        if (part.isClosed()) {
            for (CamPoint2D pt : pts) {
                if (pt.distanceTo(touch) > CamContour2D.EPSILON && part.contains(pt)) {
                    errs.add(name + " penetrates inside closed part boundary.");
                    return;
                }
            }
        }
    }

    private static CamPoint2D unitTangent(CamPoint2D p1, CamPoint2D p2) {
        double dx = p2.x() - p1.x(), dy = p2.y() - p1.y(), len = Math.hypot(dx, dy);
        return new CamPoint2D(dx / len, dy / len);
    }

    private static CamPoint2D normal(CamPoint2D t, ContourSide side) {
        return (side == ContourSide.LEFT) ? new CamPoint2D(-t.y(), t.x()) : new CamPoint2D(t.y(), -t.x());
    }
}
