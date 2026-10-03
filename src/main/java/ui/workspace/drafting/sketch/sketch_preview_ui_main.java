package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;

/**
 * sketch_preview_ui_main.java
 * Visual-only preview and snap indicator rendering in 3D viewport.
 * Never persists any node into the CAD model.
 */
public class sketch_preview_ui_main extends Group {

    private static final Color PREVIEW_COLOR = Color.web("#0284C7");
    private static final Color SNAP_COLOR = Color.web("#F59E0B");

    private final Group previewGeometryGroup = new Group();
    private final Group snapIndicatorGroup = new Group();

    public sketch_preview_ui_main() {
        setMouseTransparent(true);
        getChildren().addAll(previewGeometryGroup, snapIndicatorGroup);
    }

    public void clearPreview() {
        previewGeometryGroup.getChildren().clear();
    }

    public void clearSnap() {
        snapIndicatorGroup.getChildren().clear();
    }

    public void clearAll() {
        clearPreview();
        clearSnap();
    }
    public Group getPreviewGeometryGroup() { return previewGeometryGroup; }
    public boolean isPreviewEmpty() { return previewGeometryGroup.getChildren().isEmpty(); }

    public void showLinePreview(Point3D p1, Point3D p2) {
        clearPreview();
        if (p1 == null || p2 == null) return;
        Line l = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        l.setTranslateZ((p1.getZ() + p2.getZ()) * 0.5);
        l.setStroke(PREVIEW_COLOR);
        l.setStrokeWidth(1.8);
        l.getStrokeDashArray().addAll(6.0, 4.0);
        previewGeometryGroup.getChildren().add(l);
    }

    public void showCirclePreview(sketch_point_2d_ui_main center, double radius, sketch_coord_system_ui_main cs) {
        clearPreview();
        if (center == null || radius <= 0.05 || cs == null) return;
        Polyline poly = new Polyline();
        int segments = 48;
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double u = center.x() + radius * Math.cos(angle);
            double v = center.y() + radius * Math.sin(angle);
            Point3D wpt = cs.toWorldPoint(u, v);
            poly.getPoints().addAll(wpt.getX(), wpt.getY());
        }
        poly.setStroke(PREVIEW_COLOR);
        poly.setStrokeWidth(1.5);
        poly.getStrokeDashArray().addAll(5.0, 3.0);
        previewGeometryGroup.getChildren().add(poly);
    }

    public void showRectPreview(sketch_point_2d_ui_main c1, sketch_point_2d_ui_main c2, sketch_coord_system_ui_main cs) {
        clearPreview();
        if (c1 == null || c2 == null || cs == null) return;
        double minU = Math.min(c1.x(), c2.x()), maxU = Math.max(c1.x(), c2.x());
        double minV = Math.min(c1.y(), c2.y()), maxV = Math.max(c1.y(), c2.y());

        Polyline poly = new Polyline();
        Point3D p1 = cs.toWorldPoint(minU, minV);
        Point3D p2 = cs.toWorldPoint(maxU, minV);
        Point3D p3 = cs.toWorldPoint(maxU, maxV);
        Point3D p4 = cs.toWorldPoint(minU, maxV);
        poly.getPoints().addAll(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY(), p4.getX(), p4.getY(), p1.getX(), p1.getY());
        poly.setStroke(PREVIEW_COLOR);
        poly.setStrokeWidth(1.6);
        poly.getStrokeDashArray().addAll(6.0, 4.0);
        previewGeometryGroup.getChildren().add(poly);
    }

    public void showArcPreview(sketch_point_2d_ui_main center, double radius, double a1, double a2, sketch_coord_system_ui_main cs) {
        clearPreview();
        if (center == null || radius <= 0.05 || cs == null) return;
        Polyline poly = new Polyline();
        double span = (a2 >= a1) ? (a2 - a1) : (360.0 - (a1 - a2));
        int segments = Math.max(8, (int) (span / 6.0));
        for (int i = 0; i <= segments; i++) {
            double ang = Math.toRadians(a1 + span * i / segments);
            Point3D wpt = cs.toWorldPoint(center.x() + radius * Math.cos(ang), center.y() + radius * Math.sin(ang));
            poly.getPoints().addAll(wpt.getX(), wpt.getY());
        }
        poly.setStroke(PREVIEW_COLOR);
        poly.setStrokeWidth(1.6);
        poly.getStrokeDashArray().addAll(6.0, 4.0);
        previewGeometryGroup.getChildren().add(poly);
    }

    public void showSnapIndicator(Point3D worldPt, sketch_snap_type_ui_main snapType) {
        clearSnap();
        if (worldPt == null || snapType == null) return;
        Circle dot = new Circle(worldPt.getX(), worldPt.getY(), 4.5);
        dot.setTranslateZ(worldPt.getZ());
        dot.setFill(Color.TRANSPARENT);
        dot.setStroke(SNAP_COLOR);
        dot.setStrokeWidth(2.0);
        snapIndicatorGroup.getChildren().add(dot);
    }
}
