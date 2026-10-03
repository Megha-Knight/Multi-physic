package ui.workspace.drafting.sketch;

import java.util.*;

public final class sketch_solver_ui_main {

    private sketch_solver_ui_main() {}

    public record SolveResult(
        sketch_constraint_status_ui_main status,
        int solvedCount,
        int unsatisfiedCount,
        String message
    ) {}

    public static SolveResult solve(List<sketch_entity_ui_main> geometries, List<sketch_constraint_ui_main> constraints) {
        if (geometries == null || geometries.isEmpty()) {
            return new SolveResult(sketch_constraint_status_ui_main.UNDER_CONSTRAINED, 0, 0, "No geometry");
        }
        if (constraints == null || constraints.isEmpty()) {
            return new SolveResult(sketch_constraint_status_ui_main.UNDER_CONSTRAINED, 0, 0, "No constraints");
        }

        if (sketch_constraint_evaluator_ui_main.hasContradiction(constraints)) {
            return new SolveResult(sketch_constraint_status_ui_main.INVALID, 0, constraints.size(), "Contradictory constraints detected");
        }

        Map<String, sketch_entity_ui_main> map = new HashMap<>();
        for (sketch_entity_ui_main g : geometries) map.put(g.getId(), g);

        List<sketch_constraint_ui_main> sorted = new ArrayList<>(constraints);
        sorted.sort(Comparator.comparingInt(c -> getConstraintPriority(c.getType())));

        int iterations = 3;
        for (int iter = 0; iter < iterations; iter++) {
            for (sketch_constraint_ui_main c : sorted) {
                if (!c.isActive()) continue;
                applyConstraint(c, map);
            }
        }

        int satisfied = 0, unsatisfied = 0;
        for (sketch_constraint_ui_main c : constraints) {
            if (!c.isActive()) continue;
            if (sketch_constraint_evaluator_ui_main.isSatisfied(c, map, 1e-3)) satisfied++;
            else unsatisfied++;
        }

        int dof = estimateDof(geometries, constraints, satisfied);
        sketch_constraint_status_ui_main status;
        if (unsatisfied > 0) status = sketch_constraint_status_ui_main.INVALID;
        else if (dof <= 0) status = sketch_constraint_status_ui_main.FULLY_CONSTRAINED;
        else status = sketch_constraint_status_ui_main.UNDER_CONSTRAINED;

        return new SolveResult(status, satisfied, unsatisfied, "Satisfied: " + satisfied + "/" + constraints.size() + ", DOF: " + dof);
    }

    private static int getConstraintPriority(sketch_constraint_type_ui_main type) {
        return switch (type) {
            case COINCIDENT -> 1;
            case HORIZONTAL, VERTICAL -> 2;
            case PARALLEL, PERPENDICULAR -> 3;
            case EQUAL -> 4;
            case DISTANCE, RADIUS, ANGLE -> 5;
            default -> 6;
        };
    }

    private static void applyConstraint(sketch_constraint_ui_main c, Map<String, sketch_entity_ui_main> map) {
        sketch_entity_ui_main g1 = map.get(c.getFirstGeometryId());
        sketch_entity_ui_main g2 = map.get(c.getSecondGeometryId());

        switch (c.getType()) {
            case HORIZONTAL -> {
                if (g1 instanceof sketch_line_ui_main l) {
                    l.setEnd(new sketch_point_2d_ui_main(l.getEnd().x(), l.getStart().y()));
                }
            }
            case VERTICAL -> {
                if (g1 instanceof sketch_line_ui_main l) {
                    l.setEnd(new sketch_point_2d_ui_main(l.getStart().x(), l.getEnd().y()));
                }
            }
            case RADIUS -> {
                if (g1 instanceof sketch_circle_ui_main circ && c.getParameter() > 1e-4) circ.setRadius(c.getParameter());
                if (g1 instanceof sketch_arc_ui_main arc && c.getParameter() > 1e-4) arc.setRadius(c.getParameter());
            }
            case DISTANCE -> {
                if (g1 instanceof sketch_line_ui_main l && g2 == null && c.getParameter() > 1e-4) {
                    sketch_point_2d_ui_main dir = l.getDirection();
                    if (dir.magnitude() < 1e-6) dir = new sketch_point_2d_ui_main(1, 0);
                    l.setEnd(l.getStart().add(dir.multiply(c.getParameter())));
                }
            }
            case EQUAL -> {
                if (g1 instanceof sketch_line_ui_main l1 && g2 instanceof sketch_line_ui_main l2) {
                    sketch_point_2d_ui_main dir2 = l2.getDirection();
                    if (dir2.magnitude() < 1e-6) dir2 = new sketch_point_2d_ui_main(1, 0);
                    l2.setEnd(l2.getStart().add(dir2.multiply(l1.getLength())));
                }
                if (g1 instanceof sketch_circle_ui_main c1 && g2 instanceof sketch_circle_ui_main c2) {
                    c2.setRadius(c1.getRadius());
                }
            }
            case COINCIDENT -> {
                sketch_point_2d_ui_main targetPt = "ORIGIN".equalsIgnoreCase(c.getSecondGeometryId())
                        ? sketch_point_2d_ui_main.ZERO
                        : sketch_constraint_evaluator_ui_main.extractPoint(c, g2, c.getPointIndexB());
                if (targetPt != null && g1 instanceof sketch_line_ui_main l) {
                    if (c.getPointIndexA() == 1) l.setEnd(targetPt);
                    else l.setStart(targetPt);
                } else if (targetPt != null && g1 instanceof sketch_circle_ui_main circ) {
                    circ.setCenter(targetPt);
                }
            }
            default -> {}
        }
    }

    private static int estimateDof(List<sketch_entity_ui_main> geometries, List<sketch_constraint_ui_main> constraints, int satisfied) {
        int initialDof = 0;
        for (sketch_entity_ui_main g : geometries) {
            initialDof += switch (g.getType()) {
                case LINE -> 4; // x1, y1, x2, y2
                case CIRCLE -> 3; // cx, cy, r
                case ARC -> 5; // cx, cy, r, startA, endA
                case RECTANGLE -> 4; // x, y, w, h
                case POLYLINE -> 4;
            };
        }
        int removedDof = 0;
        for (sketch_constraint_ui_main c : constraints) {
            if (!c.isActive()) continue;
            removedDof += switch (c.getType()) {
                case COINCIDENT -> 2;
                case HORIZONTAL, VERTICAL, RADIUS, DISTANCE, ANGLE, EQUAL -> 1;
                case PARALLEL, PERPENDICULAR, TANGENT -> 1;
            };
        }
        return Math.max(0, initialDof - removedDof);
    }
}
