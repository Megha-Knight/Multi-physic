package ui.ribbonbar;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ui.framework_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * machining_panel_ui_main.java
 * Dynamic Ribbon Gallery Panel for Machining Cutouts (Circle, Square, Rectangle, Triangles) and Patterns.
 */
public class machining_panel_ui_main extends HBox implements dynamic_panel_entry_ui_main {

    private final Button btnCatCutouts = new Button("Cutouts");
    private final Button btnCatPatterns = new Button("Patterns");
    private final HBox paneCutouts;
    private final HBox panePatterns;

    private Consumer<CutoutShape> onCutoutRequested;
    private Runnable onHoleRequested;
    private Runnable onLinearPatternRequested;
    private Runnable onCircularPatternRequested;

    public machining_panel_ui_main() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(2, 6, 2, 6));
        setPrefHeight(framework_ui_main.SHAPES_PANEL_HEIGHT);
        setMaxHeight(framework_ui_main.SHAPES_PANEL_HEIGHT);
        setStyle("-fx-background-color: " + framework_ui_main.SHAPES_PANEL_BG + "; " +
                 "-fx-border-color: " + framework_ui_main.SHAPES_PANEL_BORDER + "; " +
                 "-fx-border-radius: 4; -fx-background-radius: 4;");

        initCategoryBtn(btnCatCutouts, true);
        initCategoryBtn(btnCatPatterns, false);
        btnCatCutouts.setOnAction(e -> selectCategory(true));
        btnCatPatterns.setOnAction(e -> selectCategory(false));
        VBox catCol = new VBox(2, btnCatCutouts, btnCatPatterns);
        catCol.setAlignment(Pos.CENTER);

        Color iconColor = Color.web(framework_ui_main.PRIMARY_BRAND_COLOR);
        paneCutouts = new HBox(4,
            createItem("Circle", loadIcon("/icons/circle.png", ribbonicons_ui_main.createHoleIcon(15, iconColor)), () -> {
                if (onCutoutRequested != null) onCutoutRequested.accept(CutoutShape.CIRCLE);
                if (onHoleRequested != null) onHoleRequested.run();
            }),
            createItem("Square", loadIcon("/icons/square.png", null), () -> {
                if (onCutoutRequested != null) onCutoutRequested.accept(CutoutShape.SQUARE);
            }),
            createItem("Rectangle", loadIcon("/icons/rectangle.png", null), () -> {
                if (onCutoutRequested != null) onCutoutRequested.accept(CutoutShape.RECTANGLE);
            }),
            createItem("Equilateral", loadIcon("/icons/triangle_equilateral.png", null), () -> {
                if (onCutoutRequested != null) onCutoutRequested.accept(CutoutShape.EQUILATERAL_TRIANGLE);
            }),
            createItem("Right Angle", loadIcon("/icons/triangle_right.png", null), () -> {
                if (onCutoutRequested != null) onCutoutRequested.accept(CutoutShape.RIGHT_TRIANGLE);
            })
        );
        panePatterns = new HBox(4,
            createItem("Linear", ribbonicons_ui_main.createLinearPatternIcon(15, iconColor), () -> {
                if (onLinearPatternRequested != null) onLinearPatternRequested.run();
            }),
            createItem("Circular", ribbonicons_ui_main.createCircularPatternIcon(15, iconColor), () -> {
                if (onCircularPatternRequested != null) onCircularPatternRequested.run();
            })
        );
        panePatterns.setVisible(false);
        panePatterns.setManaged(false);

        StackPane contentStack = new StackPane(paneCutouts, panePatterns);
        contentStack.setAlignment(Pos.CENTER_LEFT);

        Button closeBtn = new Button("✕");
        closeBtn.setPrefSize(18, 18);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 0; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #EF4444; -fx-font-size: 10px; -fx-text-fill: #FFFFFF; -fx-padding: 0; -fx-cursor: hand; -fx-background-radius: 2;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 0; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> hide());

        Separator sep = new Separator(Orientation.VERTICAL);
        sep.setPadding(new Insets(2, 0, 2, 0));

        getChildren().addAll(catCol, sep, contentStack, closeBtn);
        setVisible(false);
        setManaged(false);
    }

    public void setAnchorNode(Node node) { /* kept for API parity */ }
    public void setOnCutoutRequested(Consumer<CutoutShape> r) { this.onCutoutRequested = r; }
    public void setOnHoleRequested(Runnable r) { this.onHoleRequested = r; }
    public void setOnLinearPatternRequested(Runnable r) { this.onLinearPatternRequested = r; }
    public void setOnCircularPatternRequested(Runnable r) { this.onCircularPatternRequested = r; }

    public void show() { setVisible(true); setManaged(true); }
    public void hide() { setVisible(false); setManaged(false); }
    public void toggle() { if (isVisible()) hide(); else show(); }
    @Override public boolean isPanelVisible() { return isVisible(); }
    @Override public Node asNode() { return this; }

    private void selectCategory(boolean isCutouts) {
        paneCutouts.setVisible(isCutouts); paneCutouts.setManaged(isCutouts);
        panePatterns.setVisible(!isCutouts); panePatterns.setManaged(!isCutouts);
        btnCatCutouts.setStyle(getCategoryStyle(isCutouts));
        btnCatPatterns.setStyle(getCategoryStyle(!isCutouts));
    }

    private void initCategoryBtn(Button b, boolean active) {
        b.setPrefWidth(60); b.setPrefHeight(18);
        b.setStyle(getCategoryStyle(active));
    }

    private String getCategoryStyle(boolean active) {
        if (active) {
            return "-fx-background-color: " + framework_ui_main.SHAPES_CATEGORY_ACTIVE_BG + "; " +
                   "-fx-text-fill: " + framework_ui_main.SHAPES_CATEGORY_ACTIVE_TXT + "; " +
                   "-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 3; -fx-padding: 1 4 1 4; -fx-cursor: hand;";
        }
        return "-fx-background-color: transparent; -fx-text-fill: " + framework_ui_main.SHAPES_CATEGORY_INACTIVE_TXT + "; " +
               "-fx-font-size: 9px; -fx-background-radius: 3; -fx-padding: 1 4 1 4; -fx-cursor: hand;";
    }

    private Button createItem(String label, Node icon, Runnable action) {
        Button b = new Button();
        b.setPrefWidth(54); b.setPrefHeight(40);
        VBox vb = new VBox(1);
        vb.setAlignment(Pos.CENTER);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 8.5px; -fx-text-fill: " + framework_ui_main.TEXT_MAIN_COLOR + ";");
        if (icon != null) vb.getChildren().add(icon);
        vb.getChildren().add(lbl);
        b.setGraphic(vb);

        String base = "-fx-background-color: transparent; -fx-background-radius: 3; -fx-padding: 1;";
        String hover = "-fx-background-color: " + framework_ui_main.SHAPES_ITEM_HOVER_BG + "; -fx-background-radius: 3; -fx-padding: 1; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        b.setOnAction(e -> { if (action != null) action.run(); });
        return b;
    }

    private Node loadIcon(String path, Node fallback) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                ImageView iv = new ImageView(new Image(is));
                iv.setFitWidth(15); iv.setFitHeight(15);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception ignored) {}
        return fallback;
    }
}
