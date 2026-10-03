package ui.workspace.drafting.sketch;

import java.util.ArrayList;
import java.util.List;

public final class sketch_snap_system_ui_main {

    public static final double DEFAULT_SNAP_TOLERANCE = 5.0;

    private sketch_snap_system_ui_main() {}

    public record SnapResult(
        sketch_point_2d_ui_main snapPoint,
        sketch_snap_type_ui_main type,
        String targetEntityId,
        double distance
    ) {}

    public static SnapResult findBestSnap(sketch_point_2d_ui_main cursor, List<sketch_entity_ui_main> entities, double tol) {
        if (cursor == null) return null;
        double bestDist = tol;
        SnapResult best = null;

        double origDist = cursor.distance(sketch_point_2d_ui_main.ZERO);
        if (origDist <= bestDist) {
            bestDist = origDist;
            best = new SnapResult(sketch_point_2d_ui_main.ZERO, sketch_snap_type_ui_main.ORIGIN, "ORIGIN", origDist);
        }

        if (entities == null || entities.isEmpty()) return best;

        for (sketch_entity_ui_main ent : entities) {
            if (!ent.isValid()) continue;
            if (ent instanceof sketch_line_ui_main l) {
                double dStart = cursor.distance(l.getStart());
                if (dStart < bestDist) {
                    bestDist = dStart;
                    best = new SnapResult(l.getStart(), sketch_snap_type_ui_main.ENDPOINT, l.getId(), dStart);
                }
                double dEnd = cursor.distance(l.getEnd());
                if (dEnd < bestDist) {
                    bestDist = dEnd;
                    best = new SnapResult(l.getEnd(), sketch_snap_type_ui_main.ENDPOINT, l.getId(), dEnd);
                }
                double dMid = cursor.distance(l.getMidpoint());
                if (dMid < bestDist) {
                    bestDist = dMid;
                    best = new SnapResult(l.getMidpoint(), sketch_snap_type_ui_main.MIDPOINT, l.getId(), dMid);
                }
            } else if (ent instanceof sketch_circle_ui_main circ) {
                double dCenter = cursor.distance(circ.getCenter());
                if (dCenter < bestDist) {
                    bestDist = dCenter;
                    best = new SnapResult(circ.getCenter(), sketch_snap_type_ui_main.CENTER, circ.getId(), dCenter);
                }
            } else if (ent instanceof sketch_arc_ui_main arc) {
                double dCenter = cursor.distance(arc.getCenter());
                if (dCenter < bestDist) {
                    bestDist = dCenter;
                    best = new SnapResult(arc.getCenter(), sketch_snap_type_ui_main.CENTER, arc.getId(), dCenter);
                }
                double dStart = cursor.distance(arc.getStartPoint());
                if (dStart < bestDist) {
                    bestDist = dStart;
                    best = new SnapResult(arc.getStartPoint(), sketch_snap_type_ui_main.ENDPOINT, arc.getId(), dStart);
                }
                double dEnd = cursor.distance(arc.getEndPoint());
                if (dEnd < bestDist) {
                    bestDist = dEnd;
                    best = new SnapResult(arc.getEndPoint(), sketch_snap_type_ui_main.ENDPOINT, arc.getId(), dEnd);
                }
            } else if (ent instanceof sketch_rect_ui_main r) {
                for (sketch_point_2d_ui_main p : r.getSnapPoints()) {
                    double d = cursor.distance(p);
                    if (d < bestDist) {
                        bestDist = d;
                        best = new SnapResult(p, sketch_snap_type_ui_main.ENDPOINT, r.getId(), d);
                    }
                }
            }
        }

        // Check horizontal / vertical alignment if no direct point snapped
        if (best == null) {
            for (sketch_entity_ui_main ent : entities) {
                for (sketch_point_2d_ui_main sp : ent.getSnapPoints()) {
                    if (Math.abs(cursor.y() - sp.y()) <= tol * 0.5) {
                        return new SnapResult(new sketch_point_2d_ui_main(cursor.x(), sp.y()),
                                              sketch_snap_type_ui_main.HORIZONTAL_ALIGN, ent.getId(), Math.abs(cursor.y() - sp.y()));
                    }
                    if (Math.abs(cursor.x() - sp.x()) <= tol * 0.5) {
                        return new SnapResult(new sketch_point_2d_ui_main(sp.x(), cursor.y()),
                                              sketch_snap_type_ui_main.VERTICAL_ALIGN, ent.getId(), Math.abs(cursor.x() - sp.x()));
                    }
                }
            }
        }

        return best;
    }
}
