package ui.workspace.drafting.sketch;

import java.util.List;
import java.util.Map;

public final class sketch_constraint_evaluator_ui_main {

    public static final double TOLERANCE = 1e-4;

    private sketch_constraint_evaluator_ui_main() {}

    public static boolean isSatisfied(sketch_constraint_ui_main c, List<? extends sketch_entity_ui_main> entities) {
        Map<String, sketch_entity_ui_main> map = new java.util.HashMap<>();
        if (entities != null) for (var e : entities) map.put(e.getId(), e);
        return isSatisfied(c, map, TOLERANCE);
    }

    public static boolean isSatisfied(sketch_constraint_ui_main c, Map<String, sketch_entity_ui_main> geomMap, double tol) {
        if (c == null || !c.isActive() || geomMap == null) return false;
        sketch_entity_ui_main g1 = geomMap.get(c.getFirstGeometryId());
        sketch_entity_ui_main g2 = geomMap.get(c.getSecondGeometryId());
        if (g1 == null && !"ORIGIN".equalsIgnoreCase(c.getFirstGeometryId())) return false;

        return switch (c.getType()) {
            case HORIZONTAL -> (g1 instanceof sketch_line_ui_main l) && Math.abs(l.getEnd().y() - l.getStart().y()) <= tol;
            case VERTICAL -> (g1 instanceof sketch_line_ui_main l) && Math.abs(l.getEnd().x() - l.getStart().x()) <= tol;
            case PARALLEL -> checkParallel(g1, g2, tol);
            case PERPENDICULAR -> checkPerpendicular(g1, g2, tol);
            case EQUAL -> checkEqual(g1, g2, tol);
            case DISTANCE -> checkDistance(c, g1, g2, tol);
            case RADIUS -> checkRadius(c, g1, tol);
            case ANGLE -> checkAngle(c, g1, g2, tol);
            case COINCIDENT -> checkCoincident(c, g1, g2, geomMap, tol);
            case TANGENT -> checkTangent(g1, g2, tol);
        };
    }

    private static boolean checkParallel(sketch_entity_ui_main g1, sketch_entity_ui_main g2, double tol) {
        if (g1 instanceof sketch_line_ui_main l1 && g2 instanceof sketch_line_ui_main l2) {
            sketch_point_2d_ui_main d1 = l1.getDirection(), d2 = l2.getDirection();
            return Math.abs(d1.cross(d2)) <= tol;
        }
        return false;
    }

    private static boolean checkPerpendicular(sketch_entity_ui_main g1, sketch_entity_ui_main g2, double tol) {
        if (g1 instanceof sketch_line_ui_main l1 && g2 instanceof sketch_line_ui_main l2) {
            sketch_point_2d_ui_main d1 = l1.getDirection(), d2 = l2.getDirection();
            return Math.abs(d1.dot(d2)) <= tol;
        }
        return false;
    }

    private static boolean checkEqual(sketch_entity_ui_main g1, sketch_entity_ui_main g2, double tol) {
        if (g1 instanceof sketch_line_ui_main l1 && g2 instanceof sketch_line_ui_main l2) {
            return Math.abs(l1.getLength() - l2.getLength()) <= tol;
        }
        if (g1 instanceof sketch_circle_ui_main c1 && g2 instanceof sketch_circle_ui_main c2) {
            return Math.abs(c1.getRadius() - c2.getRadius()) <= tol;
        }
        return false;
    }

    private static boolean checkDistance(sketch_constraint_ui_main c, sketch_entity_ui_main g1, sketch_entity_ui_main g2, double tol) {
        if (g1 instanceof sketch_line_ui_main l && g2 == null) {
            return Math.abs(l.getLength() - c.getParameter()) <= tol;
        }
        sketch_point_2d_ui_main p1 = extractPoint(c, g1, c.getPointIndexA());
        sketch_point_2d_ui_main p2 = extractPoint(c, g2, c.getPointIndexB());
        if (p1 != null && p2 != null) {
            return Math.abs(p1.distance(p2) - c.getParameter()) <= tol;
        }
        return false;
    }

    private static boolean checkRadius(sketch_constraint_ui_main c, sketch_entity_ui_main g1, double tol) {
        if (g1 instanceof sketch_circle_ui_main circ) return Math.abs(circ.getRadius() - c.getParameter()) <= tol;
        if (g1 instanceof sketch_arc_ui_main arc) return Math.abs(arc.getRadius() - c.getParameter()) <= tol;
        return false;
    }

    private static boolean checkAngle(sketch_constraint_ui_main c, sketch_entity_ui_main g1, sketch_entity_ui_main g2, double tol) {
        if (g1 instanceof sketch_line_ui_main l1 && g2 instanceof sketch_line_ui_main l2) {
            double a1 = l1.getAngleDeg(), a2 = l2.getAngleDeg();
            double diff = Math.abs(a1 - a2) % 180.0;
            if (diff > 90.0) diff = 180.0 - diff;
            return Math.abs(diff - (c.getParameter() % 180.0)) <= tol * 100.0; // degree tolerance
        }
        return false;
    }

    private static boolean checkCoincident(sketch_constraint_ui_main c, sketch_entity_ui_main g1, sketch_entity_ui_main g2,
                                           Map<String, sketch_entity_ui_main> map, double tol) {
        sketch_point_2d_ui_main p1 = extractPoint(c, g1, c.getPointIndexA());
        sketch_point_2d_ui_main p2 = "ORIGIN".equalsIgnoreCase(c.getSecondGeometryId())
                ? sketch_point_2d_ui_main.ZERO : extractPoint(c, g2, c.getPointIndexB());
        return p1 != null && p2 != null && p1.distance(p2) <= tol;
    }

    private static boolean checkTangent(sketch_entity_ui_main g1, sketch_entity_ui_main g2, double tol) {
        sketch_line_ui_main line = (g1 instanceof sketch_line_ui_main l) ? l : (g2 instanceof sketch_line_ui_main l2 ? l2 : null);
        sketch_circle_ui_main circ = (g1 instanceof sketch_circle_ui_main c) ? c : (g2 instanceof sketch_circle_ui_main c2 ? c2 : null);
        if (line != null && circ != null) {
            double l2 = line.getStart().distanceSq(line.getEnd());
            if (l2 < 1e-8) return false;
            sketch_point_2d_ui_main v = circ.getCenter().subtract(line.getStart());
            sketch_point_2d_ui_main dir = line.getEnd().subtract(line.getStart());
            double t = v.dot(dir) / l2;
            sketch_point_2d_ui_main proj = line.getStart().add(dir.multiply(t));
            return Math.abs(proj.distance(circ.getCenter()) - circ.getRadius()) <= tol;
        }
        return false;
    }

    public static sketch_point_2d_ui_main extractPoint(sketch_constraint_ui_main c, sketch_entity_ui_main g, int ptIdx) {
        if (g == null) return null;
        if (g instanceof sketch_line_ui_main l) {
            if (ptIdx == 0) return l.getStart();
            if (ptIdx == 1) return l.getEnd();
            if (ptIdx == 2) return l.getMidpoint();
            return l.getStart();
        }
        if (g instanceof sketch_circle_ui_main circ) return circ.getCenter();
        if (g instanceof sketch_arc_ui_main arc) {
            if (ptIdx == 0) return arc.getStartPoint();
            if (ptIdx == 1) return arc.getEndPoint();
            return arc.getCenter();
        }
        return null;
    }

    public static boolean hasContradiction(List<sketch_constraint_ui_main> constraints) {
        if (constraints == null || constraints.size() < 2) return false;
        for (int i = 0; i < constraints.size(); i++) {
            sketch_constraint_ui_main c1 = constraints.get(i);
            if (!c1.isActive()) continue;
            for (int j = i + 1; j < constraints.size(); j++) {
                sketch_constraint_ui_main c2 = constraints.get(j);
                if (!c2.isActive()) continue;
                if (isContradictoryPair(c1, c2)) return true;
            }
        }
        return false;
    }

    public static boolean isContradictoryPair(sketch_constraint_ui_main c1, sketch_constraint_ui_main c2) {
        String g1 = c1.getFirstGeometryId(), g2 = c2.getFirstGeometryId();
        if (g1 != null && g1.equals(g2)) {
            if ((c1.getType() == sketch_constraint_type_ui_main.HORIZONTAL && c2.getType() == sketch_constraint_type_ui_main.VERTICAL) ||
                (c1.getType() == sketch_constraint_type_ui_main.VERTICAL && c2.getType() == sketch_constraint_type_ui_main.HORIZONTAL)) {
                return true;
            }
            if (c1.getType() == sketch_constraint_type_ui_main.DISTANCE && c2.getType() == sketch_constraint_type_ui_main.DISTANCE) {
                if (c1.getSecondGeometryId() == null && c2.getSecondGeometryId() == null &&
                    Math.abs(c1.getParameter() - c2.getParameter()) > TOLERANCE) {
                    return true;
                }
            }
            if (c1.getType() == sketch_constraint_type_ui_main.RADIUS && c2.getType() == sketch_constraint_type_ui_main.RADIUS) {
                if (Math.abs(c1.getParameter() - c2.getParameter()) > TOLERANCE) return true;
            }
        }
        return false;
    }
}
