package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;

/**
 * sketch_view_ui_main.java
 * Visualizes committed sketch geometry and constraint indicators in the 3D viewport.
 */
public class sketch_view_ui_main extends Group {

    private static final Color DEFAULT_COLOR = Color.web("#1E293B");
    private static final Color SELECTED_COLOR = Color.web("#0284C7");
    private static final Color INVALID_COLOR = Color.web("#DC2626");

    public sketch_view_ui_main() {
        setMouseTransparent(true);
    }

    public void render(sketch_feature_ui_main sketch) {
        getChildren().clear();
        if (sketch == null || !sketch.isVisible()) return;

        sketch_coord_system_ui_main cs = sketch.getCoordSystem();
        if (cs == null) return;

        Color baseColor = sketch.isValid() ? DEFAULT_COLOR : INVALID_COLOR;

        for (sketch_entity_ui_main e : sketch.getEntities()) {
            Color color = e.isSelected() ? SELECTED_COLOR : baseColor;
            double strokeWidth = e.isSelected() ? 2.4 : 1.6;

            switch (e.getType()) {
                case LINE -> renderLine((sketch_line_ui_main) e, cs, color, strokeWidth);
                case CIRCLE -> renderCircle((sketch_circle_ui_main) e, cs, color, strokeWidth);
                case ARC -> renderArc((sketch_arc_ui_main) e, cs, color, strokeWidth);
                case RECTANGLE -> renderRect((sketch_rect_ui_main) e, cs, color, strokeWidth);
                default -> {}
            }
        }
    }

    private void renderLine(sketch_line_ui_main line, sketch_coord_system_ui_main cs, Color color, double width) {
        Point3D p1 = cs.toWorldPoint(line.getStart());
        Point3D p2 = cs.toWorldPoint(line.getEnd());
        Line l = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        l.setTranslateZ((p1.getZ() + p2.getZ()) * 0.5);
        l.setStroke(color);
        l.setStrokeWidth(width);
        getChildren().add(l);
    }

    private void renderCircle(sketch_circle_ui_main circle, sketch_coord_system_ui_main cs, Color color, double width) {
        Polyline poly = new Polyline();
        int segments = 48;
        double r = circle.getRadius();
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double u = circle.getCenter().x() + r * Math.cos(angle);
            double v = circle.getCenter().y() + r * Math.sin(angle);
            Point3D wpt = cs.toWorldPoint(u, v);
            poly.getPoints().addAll(wpt.getX(), wpt.getY());
        }
        poly.setStroke(color);
        poly.setStrokeWidth(width);
        getChildren().add(poly);
    }

    private void renderArc(sketch_arc_ui_main arc, sketch_coord_system_ui_main cs, Color color, double width) {
        Polyline poly = new Polyline();
        double a1 = arc.getStartAngle(), a2 = arc.getEndAngle(), r = arc.getRadius();
        double span = (a2 >= a1) ? (a2 - a1) : (360.0 - (a1 - a2));
        int segments = Math.max(8, (int) (span / 6.0));
        for (int i = 0; i <= segments; i++) {
            double ang = Math.toRadians(a1 + span * i / segments);
            Point3D wpt = cs.toWorldPoint(arc.getCenter().x() + r * Math.cos(ang), arc.getCenter().y() + r * Math.sin(ang));
            poly.getPoints().addAll(wpt.getX(), wpt.getY());
        }
        poly.setStroke(color);
        poly.setStrokeWidth(width);
        getChildren().add(poly);
    }

    private void renderRect(sketch_rect_ui_main rect, sketch_coord_system_ui_main cs, Color color, double width) {
        for (sketch_line_ui_main seg : rect.toLines()) {
            renderLine(seg, cs, color, width);
        }
    }
}
