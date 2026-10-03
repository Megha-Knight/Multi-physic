package ui.workspace.drafting.sketch;

import java.util.List;

/**
 * sketch_hit_tester_ui_main.java
 * Analytical 2D hit-testing and proximity checks for sketch entities.
 */
public final class sketch_hit_tester_ui_main {

    private sketch_hit_tester_ui_main() {}

    public static double distPointToSegment(sketch_point_2d_ui_main p, sketch_point_2d_ui_main a, sketch_point_2d_ui_main b) {
        if (p == null || a == null || b == null) return Double.MAX_VALUE;
        double abx = b.x() - a.x(), aby = b.y() - a.y();
        double apx = p.x() - a.x(), apy = p.y() - a.y();
        double abLenSq = abx * abx + aby * aby;
        if (abLenSq < 1e-12) return p.distance(a);

        double t = Math.max(0.0, Math.min(1.0, (apx * abx + apy * aby) / abLenSq));
        sketch_point_2d_ui_main proj = new sketch_point_2d_ui_main(a.x() + t * abx, a.y() + t * aby);
        return p.distance(proj);
    }

    public static double distToEntity(sketch_point_2d_ui_main p, sketch_entity_ui_main e) {
        if (p == null || e == null) return Double.MAX_VALUE;
        return switch (e.getType()) {
            case LINE -> {
                sketch_line_ui_main l = (sketch_line_ui_main) e;
                yield distPointToSegment(p, l.getStart(), l.getEnd());
            }
            case CIRCLE -> {
                sketch_circle_ui_main c = (sketch_circle_ui_main) e;
                double d = p.distance(c.getCenter());
                yield Math.abs(d - c.getRadius());
            }
            case ARC -> {
                sketch_arc_ui_main arc = (sketch_arc_ui_main) e;
                double d = p.distance(arc.getCenter());
                double radialDist = Math.abs(d - arc.getRadius());
                if (radialDist > 5.0) yield radialDist;
                double angleDeg = Math.toDegrees(Math.atan2(p.y() - arc.getCenter().y(), p.x() - arc.getCenter().x()));
                if (angleDeg < 0) angleDeg += 360.0;
                double a1 = arc.getStartAngle(), a2 = arc.getEndAngle();
                boolean within = (a1 <= a2) ? (angleDeg >= a1 && angleDeg <= a2) : (angleDeg >= a1 || angleDeg <= a2);
                yield within ? radialDist : Math.min(p.distance(arc.getStartPoint()), p.distance(arc.getEndPoint()));
            }
            case RECTANGLE -> {
                sketch_rect_ui_main r = (sketch_rect_ui_main) e;
                double minD = Double.MAX_VALUE;
                for (sketch_line_ui_main edge : r.toLines()) {
                    minD = Math.min(minD, distPointToSegment(p, edge.getStart(), edge.getEnd()));
                }
                yield minD;
            }
            default -> Double.MAX_VALUE;
        };
    }

    public static sketch_entity_ui_main findNearestEntity(sketch_point_2d_ui_main p,
                                                          List<sketch_entity_ui_main> entities,
                                                          double tolerance) {
        if (p == null || entities == null || entities.isEmpty()) return null;
        sketch_entity_ui_main best = null;
        double bestDist = tolerance;
        for (sketch_entity_ui_main e : entities) {
            double d = distToEntity(p, e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    public static int findNearEndpoint(sketch_point_2d_ui_main p, sketch_line_ui_main line, double tol) {
        if (p == null || line == null) return -1;
        if (p.distance(line.getStart()) <= tol) return 0;
        if (p.distance(line.getEnd()) <= tol) return 1;
        return -1;
    }
}
