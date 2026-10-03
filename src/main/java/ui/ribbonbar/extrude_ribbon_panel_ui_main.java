package ui.ribbonbar;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ui.framework_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;

import java.util.function.Consumer;

/**
 * extrude_ribbon_panel_ui_main.java
 * Dynamic Ribbon Panel for configuring and committing 3D Extrusion features.
 * Participates in centralized dynamic panel mutual exclusion.
 */
public class extrude_ribbon_panel_ui_main extends HBox implements dynamic_panel_entry_ui_main {

    public record ExtrudeConfig(CutoutShape profile, double diameter, double width2, double height) {}

    private final ComboBox<CutoutShape> profileCombo = new ComboBox<>();
    private final TextField diaField = new TextField("20.0");
    private final TextField w2Field = new TextField("15.0");
    private final TextField heightField = new TextField("25.0");
    private final VBox w2Box;
    private final Button applyBtn = new Button("Extrude");
    private Consumer<ExtrudeConfig> onApply;

    public extrude_ribbon_panel_ui_main() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(2, 6, 2, 6));
        setPrefHeight(framework_ui_main.SHAPES_PANEL_HEIGHT);
        setMaxHeight(framework_ui_main.SHAPES_PANEL_HEIGHT);
        setStyle("-fx-background-color: " + framework_ui_main.SHAPES_PANEL_BG + "; " +
                 "-fx-border-color: " + framework_ui_main.SHAPES_PANEL_BORDER + "; " +
                 "-fx-border-radius: 4; -fx-background-radius: 4;");

        profileCombo.getItems().addAll(CutoutShape.CIRCLE, CutoutShape.SQUARE, CutoutShape.RECTANGLE);
        profileCombo.setValue(CutoutShape.CIRCLE);
        profileCombo.setPrefWidth(90);
        profileCombo.setStyle("-fx-font-size: 9.5px; -fx-padding: 1 3 1 3;");

        VBox profCol = createFieldCol("Profile", profileCombo);
        VBox diaCol = createFieldCol("Size (mm)", diaField);
        w2Box = createFieldCol("W2 (mm)", w2Field);
        w2Box.setVisible(false);
        w2Box.setManaged(false);
        VBox heightCol = createFieldCol("Height (mm)", heightField);

        profileCombo.setOnAction(e -> {
            boolean isRect = (profileCombo.getValue() == CutoutShape.RECTANGLE);
            w2Box.setVisible(isRect);
            w2Box.setManaged(isRect);
        });

        applyBtn.setStyle("-fx-background-color: " + framework_ui_main.PRIMARY_BRAND_COLOR + "; " +
                          "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; " +
                          "-fx-padding: 3 8 3 8; -fx-background-radius: 3; -fx-cursor: hand;");
        applyBtn.setOnAction(e -> handleApply());

        Button closeBtn = new Button("✕");
        closeBtn.setPrefSize(18, 18);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 0; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #EF4444; -fx-font-size: 10px; -fx-text-fill: #FFFFFF; -fx-padding: 0; -fx-cursor: hand; -fx-background-radius: 2;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 0; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> hide());

        Separator sep = new Separator(Orientation.VERTICAL);
        sep.setPadding(new Insets(2, 0, 2, 0));

        getChildren().addAll(profCol, diaCol, w2Box, heightCol, applyBtn, sep, closeBtn);
        setVisible(false);
        setManaged(false);
    }

    public void setOnApply(Consumer<ExtrudeConfig> onApply) { this.onApply = onApply; }

    @Override public void show() { setVisible(true); setManaged(true); }
    @Override public void hide() { setVisible(false); setManaged(false); }
    public void toggle() { if (isVisible()) hide(); else show(); }
    @Override public boolean isPanelVisible() { return isVisible(); }
    @Override public Node asNode() { return this; }

    public CutoutShape getProfileShape() { return profileCombo.getValue(); }
    public void setProfileShape(CutoutShape s) { if (s != null) profileCombo.setValue(s); }
    public double getDiameter() { return parseDouble(diaField.getText(), 20.0); }
    public void setDiameter(double d) { diaField.setText(String.format(java.util.Locale.US, "%.1f", d)); }
    public double getWidth2() { return parseDouble(w2Field.getText(), 15.0); }
    public void setWidth2(double w) { w2Field.setText(String.format(java.util.Locale.US, "%.1f", w)); }
    public double getExtrudeHeight() { return parseDouble(heightField.getText(), 25.0); }
    public void setExtrudeHeight(double h) { heightField.setText(String.format(java.util.Locale.US, "%.1f", h)); }

    private void handleApply() {
        if (onApply != null) {
            onApply.accept(new ExtrudeConfig(getProfileShape(), getDiameter(), getWidth2(), getExtrudeHeight()));
        }
    }

    private VBox createFieldCol(String label, Node control) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 8px; -fx-text-fill: " + framework_ui_main.TEXT_MUTED_COLOR + ";");
        if (control instanceof TextField tf) {
            tf.setPrefWidth(48);
            tf.setPrefHeight(18);
            tf.setStyle("-fx-font-size: 9.5px; -fx-padding: 1 3 1 3; -fx-border-color: #CBD5E1; -fx-border-radius: 2;");
        }
        VBox col = new VBox(1, lbl, control);
        col.setAlignment(Pos.CENTER_LEFT);
        return col;
    }

    private static double parseDouble(String text, double def) {
        try { return Double.parseDouble(text.trim()); } catch (Exception e) { return def; }
    }
}
