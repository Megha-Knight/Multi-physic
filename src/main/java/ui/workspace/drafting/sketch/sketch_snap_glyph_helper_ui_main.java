package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * sketch_snap_glyph_helper_ui_main.java
 * High-clarity CAD visual snap glyph generator for endpoints, midpoints,
 * centers, origin, and coordinate inferencing.
 */
public final class sketch_snap_glyph_helper_ui_main {

    private sketch_snap_glyph_helper_ui_main() {}

    public static Node createGlyph(Point3D worldPt, sketch_snap_type_ui_main type) {
        if (worldPt == null || type == null) return new Group();
        double x = worldPt.getX(), y = worldPt.getY(), z = worldPt.getZ();

        Group g = new Group();
        g.setTranslateZ(z);

        switch (type) {
            case ENDPOINT -> {
                // Hollow orange square
                Rectangle sq = new Rectangle(x - 3.5, y - 3.5, 7.0, 7.0);
                sq.setFill(Color.TRANSPARENT);
                sq.setStroke(Color.web("#F97316"));
                sq.setStrokeWidth(1.8);
                g.getChildren().add(sq);
            }
            case MIDPOINT -> {
                // Hollow cyan triangle
                Polygon tri = new Polygon(
                    x, y - 4.5,
                    x - 4.0, y + 3.5,
                    x + 4.0, y + 3.5
                );
                tri.setFill(Color.TRANSPARENT);
                tri.setStroke(Color.web("#06B6D4"));
                tri.setStrokeWidth(1.8);
                g.getChildren().add(tri);
            }
            case CENTER -> {
                // Cyan circle with crosshair
                Circle circ = new Circle(x, y, 4.0);
                circ.setFill(Color.TRANSPARENT);
                circ.setStroke(Color.web("#38BDF8"));
                circ.setStrokeWidth(1.6);
                Line h = new Line(x - 5.0, y, x + 5.0, y);
                Line v = new Line(x, y - 5.0, x, y + 5.0);
                h.setStroke(Color.web("#38BDF8")); h.setStrokeWidth(1.4);
                v.setStroke(Color.web("#38BDF8")); v.setStrokeWidth(1.4);
                g.getChildren().addAll(circ, h, v);
            }
            case ORIGIN -> {
                // Double concentric red circle
                Circle cOuter = new Circle(x, y, 5.0);
                Circle cInner = new Circle(x, y, 2.5);
                cOuter.setFill(Color.TRANSPARENT); cOuter.setStroke(Color.web("#EF4444")); cOuter.setStrokeWidth(1.8);
                cInner.setFill(Color.web("#EF4444")); cInner.setStroke(Color.web("#EF4444"));
                g.getChildren().addAll(cOuter, cInner);
            }
            case INTERSECTION -> {
                // Amber X cross
                Line l1 = new Line(x - 3.5, y - 3.5, x + 3.5, y + 3.5);
                Line l2 = new Line(x - 3.5, y + 3.5, x + 3.5, y - 3.5);
                l1.setStroke(Color.web("#F59E0B")); l1.setStrokeWidth(1.8);
                l2.setStroke(Color.web("#F59E0B")); l2.setStrokeWidth(1.8);
                g.getChildren().addAll(l1, l2);
            }
            case HORIZONTAL_ALIGN, VERTICAL_ALIGN -> {
                Circle dot = new Circle(x, y, 3.5);
                dot.setFill(Color.web("#FCD34D"));
                dot.setStroke(Color.web("#D97706"));
                dot.setStrokeWidth(1.2);
                g.getChildren().add(dot);
            }
        }
        return g;
    }
}
