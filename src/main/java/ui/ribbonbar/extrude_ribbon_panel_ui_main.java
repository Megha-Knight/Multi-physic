package ui.ribbonbar;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ui.framework_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * extrude_ribbon_panel_ui_main.java
 * Dynamic Ribbon Panel for creating 3D Extrusion bodies.
 * Presents an icon gallery identical in style to Basic Shapes (one icon per profile).
 * Clicking a profile icon:
 *   1. Arms shape_drafting_ui_main with the matching 3D shape type, using the same
 *      "click-to-arm, drag for footprint" mechanism that Basic Shapes already uses —
 *      no parallel drafting system is built.
 *   2. Reveals a small inline Height field so the user can set extrusion depth before
 *      or after placing the footprint drag (the height is read from this field when
 *      the shape is committed). This is the chosen height-setting mechanism: an
 *      inline numeric field, not a second gesture. It pre-fills with 25.0 mm.
 * The onProfileArmed callback receives the CutoutShape and the matching basic_shapes_ui_main
 * value. Callers are responsible for forwarding these to the drafter.
 * Participates in centralized dynamic panel mutual exclusion.
 */
public class extrude_ribbon_panel_ui_main extends HBox implements dynamic_panel_entry_ui_main {

    public record ExtrudeConfig(CutoutShape profile, double diameter, double width2, double height) {}

    /** Called when the user clicks a profile icon. Callers arm the drafter. */
    private Consumer<basic_shapes_ui_main> onProfileArmed;
    /** Legacy apply callback kept for backward compatibility (existing applyExtrudeConfig callers). */
    private Consumer<ExtrudeConfig> onApply;

    // Height field — the sole "set height" mechanism for the extrude flow.
    private final TextField heightField = new TextField("25.0");
    private CutoutShape armedProfile = CutoutShape.CIRCLE;
    private double armedDiameter = 20.0;

    public extrude_ribbon_panel_ui_main() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(2, 6, 2, 6));
        setPrefHeight(framework_ui_main.SHAPES_PANEL_HEIGHT);
        setMaxHeight(framework_ui_main.SHAPES_PANEL_HEIGHT);
        setStyle("-fx-background-color: " + framework_ui_main.SHAPES_PANEL_BG + "; " +
                 "-fx-border-color: " + framework_ui_main.SHAPES_PANEL_BORDER + "; " +
                 "-fx-border-radius: 4; -fx-background-radius: 4;");

        // Profile icon gallery — same visual style as basicshapespanel_ui_main
        HBox gallery = new HBox(4,
            createProfileItem("Circle",     "/icons/circle.png",               CutoutShape.CIRCLE,               basic_shapes_ui_main.CIRCLE),
            createProfileItem("Square",     "/icons/square.png",               CutoutShape.SQUARE,               basic_shapes_ui_main.SQUARE),
            createProfileItem("Rectangle",  "/icons/rectangle.png",            CutoutShape.RECTANGLE,            basic_shapes_ui_main.RECTANGLE),
            createProfileItem("Equilateral","/icons/triangle_equilateral.png", CutoutShape.EQUILATERAL_TRIANGLE, basic_shapes_ui_main.EQUILATERAL_TRIANGLE),
            createProfileItem("Right Angle","/icons/triangle_right.png",       CutoutShape.RIGHT_TRIANGLE,       basic_shapes_ui_main.RIGHT_TRIANGLE)
        );
        gallery.setAlignment(Pos.CENTER_LEFT);

        // Inline height field (the chosen height-setting mechanism — documented above)
        heightField.setPrefWidth(48);
        heightField.setPrefHeight(18);
        heightField.setStyle("-fx-font-size: 9.5px; -fx-padding: 1 3 1 3; -fx-border-color: #CBD5E1; -fx-border-radius: 2;");
        Label heightLbl = new Label("Height (mm)");
        heightLbl.setStyle("-fx-font-size: 8px; -fx-text-fill: " + framework_ui_main.TEXT_MUTED_COLOR + ";");
        VBox heightCol = new VBox(1, heightLbl, heightField);
        heightCol.setAlignment(Pos.CENTER_LEFT);

        Separator sep1 = new Separator(Orientation.VERTICAL);
        sep1.setPadding(new Insets(2, 2, 2, 2));

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setPrefSize(18, 18);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 0; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #EF4444; -fx-font-size: 10px; -fx-text-fill: #FFFFFF; -fx-padding: 0; -fx-cursor: hand; -fx-background-radius: 2;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 0; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> hide());

        getChildren().addAll(gallery, sep1, heightCol, closeBtn);
        setVisible(false);
        setManaged(false);
    }

    @FunctionalInterface
    public interface ExtrudeArmHandler { void onArmed(basic_shapes_ui_main shape, CutoutShape profile, double height); }
    private ExtrudeArmHandler onExtrudeArmed;
    public void setOnExtrudeArmed(ExtrudeArmHandler cb) { this.onExtrudeArmed = cb; }

    /** Arms the drafter with the matching 3D shape (same path as Basic Shapes). */
    public void setOnProfileArmed(Consumer<basic_shapes_ui_main> cb) { this.onProfileArmed = cb; }
    /** Legacy apply callback (for direct extrude-on-face path). */
    public void setOnApply(Consumer<ExtrudeConfig> onApply) { this.onApply = onApply; }

    @Override public void show()  { setVisible(true);  setManaged(true); }
    @Override public void hide()  { setVisible(false); setManaged(false); }
    public void toggle() { if (isVisible()) hide(); else show(); }
    @Override public boolean isPanelVisible() { return isVisible(); }
    @Override public Node asNode() { return this; }

    private double armedWidth2 = 15.0;

    /** Returns the currently armed profile shape. */
    public CutoutShape getProfileShape()         { return armedProfile; }
    /** Set externally (e.g. from dropdown menu). */
    public void setProfileShape(CutoutShape s)   { if (s != null) armedProfile = s; }
    public double getDiameter()                  { return armedDiameter; }
    public void setDiameter(double d)            { armedDiameter = d; }
    public double getWidth2()                    { return armedWidth2; }
    public void setWidth2(double w)              { armedWidth2 = w; }
    public double getExtrudeHeight()             { return parseDouble(heightField.getText(), 25.0); }
    public void setExtrudeHeight(double h)       { heightField.setText(String.format(java.util.Locale.US, "%.1f", h)); }

    private void handleApply() {
        if (onApply != null) {
            onApply.accept(new ExtrudeConfig(armedProfile, armedDiameter, armedWidth2, getExtrudeHeight()));
        }
    }

    /**
     * Creates a profile icon button. Clicking it:
     *   - arms the profile (stores cutoutShape + 3D shape type)
     *   - fires onProfileArmed so the caller can arm the drafter
     */
    private Button createProfileItem(String label, String iconPath, CutoutShape cutout, basic_shapes_ui_main shape3d) {
        Button b = new Button();
        b.setPrefWidth(54); b.setPrefHeight(40);
        VBox vb = new VBox(1);
        vb.setAlignment(Pos.CENTER);
        Node icon = loadIcon(iconPath);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 8.5px; -fx-text-fill: " + framework_ui_main.TEXT_MAIN_COLOR + ";");
        if (icon != null) vb.getChildren().add(icon);
        vb.getChildren().add(lbl);
        b.setGraphic(vb);

        String base  = "-fx-background-color: transparent; -fx-background-radius: 3; -fx-padding: 1;";
        String hover = "-fx-background-color: " + framework_ui_main.SHAPES_ITEM_HOVER_BG + "; -fx-background-radius: 3; -fx-padding: 1; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        b.setOnAction(e -> {
            armedProfile = cutout;
            if (onExtrudeArmed != null) onExtrudeArmed.onArmed(shape3d, cutout, getExtrudeHeight());
            if (onProfileArmed != null) onProfileArmed.accept(shape3d);
        });
        return b;
    }

    private Node loadIcon(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                ImageView iv = new ImageView(new Image(is));
                iv.setFitWidth(15); iv.setFitHeight(15);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static double parseDouble(String text, double def) {
        try { return Double.parseDouble(text.trim()); } catch (Exception e) { return def; }
    }
}
