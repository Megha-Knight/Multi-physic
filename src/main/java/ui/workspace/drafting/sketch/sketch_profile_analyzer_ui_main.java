package ui.workspace.drafting.sketch;

import java.util.*;

public final class sketch_profile_analyzer_ui_main {

    public static final double CAD_TOLERANCE = 1e-4;

    private sketch_profile_analyzer_ui_main() {}

    public record AnalysisResult(
        sketch_profile_type_ui_main type,
        List<sketch_profile_loop_ui_main> loops,
        sketch_profile_loop_ui_main outerLoop,
        List<sketch_profile_loop_ui_main> innerLoops,
        String message
    ) {
        public boolean isValid() { return type.isExtrudable(); }
    }

    public static AnalysisResult analyze(List<sketch_entity_ui_main> entities) {
        if (entities == null || entities.isEmpty()) {
            return new AnalysisResult(sketch_profile_type_ui_main.INVALID, Collections.emptyList(), null, Collections.emptyList(), "No geometry");
        }

        List<sketch_line_ui_main> segments = new ArrayList<>();
        List<sketch_profile_loop_ui_main> circularLoops = new ArrayList<>();

        for (sketch_entity_ui_main e : entities) {
            if (e.isConstruction() || !e.isValid()) continue;
            if (e instanceof sketch_line_ui_main l) segments.add(l.copy());
            else if (e instanceof sketch_rect_ui_main r) segments.addAll(r.toLines());
            else if (e instanceof sketch_circle_ui_main c) circularLoops.add(circleToLoop(c));
        }

        List<sketch_profile_loop_ui_main> allLoops = new ArrayList<>(circularLoops);
        if (!segments.isEmpty()) {
            List<sketch_profile_loop_ui_main> polyLoops = extractLoopsFromSegments(segments, CAD_TOLERANCE);
            if (polyLoops == null) {
                return new AnalysisResult(sketch_profile_type_ui_main.OPEN, Collections.emptyList(), null, Collections.emptyList(), "Open line chain");
            }
            allLoops.addAll(polyLoops);
        }

        if (allLoops.isEmpty()) {
            return new AnalysisResult(sketch_profile_type_ui_main.INVALID, Collections.emptyList(), null, Collections.emptyList(), "No closed profiles found");
        }

        for (sketch_profile_loop_ui_main loop : allLoops) {
            if (loop.hasSelfIntersection()) {
                return new AnalysisResult(sketch_profile_type_ui_main.SELF_INTERSECTING, allLoops, null, Collections.emptyList(), "Self-intersection detected in profile");
            }
        }

        allLoops.sort((a, b) -> Double.compare(b.getAbsArea(), a.getAbsArea()));
        sketch_profile_loop_ui_main outer = allLoops.get(0);
        outer.setOuter(true);
        List<sketch_profile_loop_ui_main> inners = new ArrayList<>();

        for (int i = 1; i < allLoops.size(); i++) {
            sketch_profile_loop_ui_main loop = allLoops.get(i);
            if (outer.encloses(loop)) {
                loop.setOuter(false);
                inners.add(loop);
            }
        }

        sketch_profile_type_ui_main pType = inners.isEmpty() ? sketch_profile_type_ui_main.CLOSED : sketch_profile_type_ui_main.MULTIPLE_LOOPS;
        String msg = String.format("Profile verified: %d loop(s), %d inner hole(s)", allLoops.size(), inners.size());
        return new AnalysisResult(pType, allLoops, outer, inners, msg);
    }

    private static sketch_profile_loop_ui_main circleToLoop(sketch_circle_ui_main c) {
        int steps = 32;
        List<sketch_point_2d_ui_main> pts = new ArrayList<>(steps);
        for (int i = 0; i < steps; i++) {
            double angle = (2.0 * Math.PI * i) / steps;
            pts.add(new sketch_point_2d_ui_main(c.getCenter().x() + c.getRadius() * Math.cos(angle),
                                                c.getCenter().y() + c.getRadius() * Math.sin(angle)));
        }
        return new sketch_profile_loop_ui_main(pts, true);
    }

    private static List<sketch_profile_loop_ui_main> extractLoopsFromSegments(List<sketch_line_ui_main> input, double tol) {
        List<sketch_line_ui_main> remaining = new ArrayList<>(input);
        List<sketch_profile_loop_ui_main> loops = new ArrayList<>();

        while (!remaining.isEmpty()) {
            sketch_line_ui_main first = remaining.remove(0);
            List<sketch_point_2d_ui_main> poly = new ArrayList<>();
            poly.add(first.getStart());
            sketch_point_2d_ui_main currentEnd = first.getEnd();

            boolean closed = false;
            while (true) {
                if (currentEnd.distance(first.getStart()) <= tol) {
                    closed = true;
                    break;
                }
                int nextIdx = findNextSegment(currentEnd, remaining, tol);
                if (nextIdx < 0) break; // open chain

                sketch_line_ui_main next = remaining.remove(nextIdx);
                poly.add(currentEnd);
                if (currentEnd.distance(next.getStart()) <= tol) currentEnd = next.getEnd();
                else currentEnd = next.getStart();
            }

            if (!closed) return null; // open chain detected
            loops.add(new sketch_profile_loop_ui_main(poly, true));
        }
        return loops;
    }

    private static int findNextSegment(sketch_point_2d_ui_main pt, List<sketch_line_ui_main> list, double tol) {
        for (int i = 0; i < list.size(); i++) {
            sketch_line_ui_main line = list.get(i);
            if (line.getStart().distance(pt) <= tol || line.getEnd().distance(pt) <= tol) return i;
        }
        return -1;
    }
}
