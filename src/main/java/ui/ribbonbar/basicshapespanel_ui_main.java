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
import ui.framework_ui_main;
import ui.workspace.basicshapes_ui_main;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * basicshapespanel_ui_main.java
 * Dynamic Ribbon Gallery Panel for Basic2D and Basic3D geometric designs.
 * Remains open during interactive drafting until closed by X mark or Basic Shapes button.
 */
public class basicshapespanel_ui_main extends HBox {

    private final Button btnCat2D = new Button("Basic2D");
    private final Button btnCat3D = new Button("Basic3D");
    private final HBox pane2D;
    private final HBox pane3D;
    private Consumer<basicshapes_ui_main> onShapeSelected;

    public basicshapespanel_ui_main() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(2, 6, 2, 6));
        setPrefHeight(framework_ui_main.SHAPES_PANEL_HEIGHT);
        setMaxHeight(framework_ui_main.SHAPES_PANEL_HEIGHT);
        setStyle("-fx-background-color: " + framework_ui_main.SHAPES_PANEL_BG + "; " +
                 "-fx-border-color: " + framework_ui_main.SHAPES_PANEL_BORDER + "; " +
                 "-fx-border-radius: 4; -fx-background-radius: 4;");

        // Category Switcher
        initCategoryBtn(btnCat2D, true);
        initCategoryBtn(btnCat3D, false);
        btnCat2D.setOnAction(e -> selectCategory(true));
        btnCat3D.setOnAction(e -> selectCategory(false));
        VBox catCol = new VBox(2, btnCat2D, btnCat3D);
        catCol.setAlignment(Pos.CENTER);

        // Content Panels for 2D and 3D
        pane2D = build2DShapesPane();
        pane3D = build3DShapesPane();
        pane3D.setVisible(false);
        pane3D.setManaged(false);

        StackPane contentStack = new StackPane(pane2D, pane3D);
        contentStack.setAlignment(Pos.CENTER_LEFT);

        // Close Button (✕)
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

    public void setAnchorNode(Node node) { /* kept for API compatibility */ }
    public void setOnShapeSelected(Consumer<basicshapes_ui_main> cb) { this.onShapeSelected = cb; }
    public void show() { setVisible(true); setManaged(true); }
    public void hide() { setVisible(false); setManaged(false); }
    public void toggle() { if (isVisible()) hide(); else show(); }

    private void selectCategory(boolean is2D) {
        pane2D.setVisible(is2D); pane2D.setManaged(is2D);
        pane3D.setVisible(!is2D); pane3D.setManaged(!is2D);
        btnCat2D.setStyle(getCategoryStyle(is2D));
        btnCat3D.setStyle(getCategoryStyle(!is2D));
    }

    private void initCategoryBtn(Button b, boolean active) {
        b.setPrefWidth(60);
        b.setPrefHeight(18);
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

    private HBox build2DShapesPane() {
        return new HBox(4,
            createShapeItem("Circle", "/icons/circle.png", basicshapes_ui_main.CIRCLE),
            createShapeItem("Square", "/icons/square.png", basicshapes_ui_main.SQUARE),
            createShapeItem("Rectangle", "/icons/rectangle.png", basicshapes_ui_main.RECTANGLE),
            createShapeItem("Equilateral", "/icons/triangle_equilateral.png", basicshapes_ui_main.EQUILATERAL_TRIANGLE),
            createShapeItem("Right Angle", "/icons/triangle_right.png", basicshapes_ui_main.RIGHT_TRIANGLE)
        );
    }

    private HBox build3DShapesPane() {
        return new HBox(4,
            createShapeItem("Cube", "/icons/cube_3d.png", basicshapes_ui_main.CUBE),
            createShapeItem("Cylinder", "/icons/cylinder_3d.png", basicshapes_ui_main.CYLINDER),
            createShapeItem("Sphere", "/icons/sphere_3d.png", basicshapes_ui_main.SPHERE),
            createShapeItem("Cone", "/icons/cone_3d.png", basicshapes_ui_main.CONE)
        );
    }

    private Button createShapeItem(String label, String iconPath, basicshapes_ui_main shape) {
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

        String base = "-fx-background-color: transparent; -fx-background-radius: 3; -fx-padding: 1;";
        String hover = "-fx-background-color: " + framework_ui_main.SHAPES_ITEM_HOVER_BG + "; -fx-background-radius: 3; -fx-padding: 1; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        b.setOnAction(e -> {
            if (onShapeSelected != null) onShapeSelected.accept(shape);
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
}
