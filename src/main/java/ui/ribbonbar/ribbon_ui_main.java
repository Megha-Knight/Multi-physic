package ui.ribbonbar;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import ui.framework_ui_main;
import ui.workspace.basicshapes_ui_main;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * ribbon_ui_main.java
 * Compact Ribbon Bar with Files and Basic Shapes geometric design groups.
 */
public class ribbon_ui_main extends HBox {

    private final splitbutton_ui_main newButton;
    private final splitbutton_ui_main openButton;
    private final splitbutton_ui_main saveButton;
    private final splitbutton_ui_main basicShapesButton;

    private Consumer<basicshapes_ui_main> onShapeSelected;
    private basicshapes_ui_main currentShape = basicshapes_ui_main.CIRCLE;

    public ribbon_ui_main() {
        setPadding(new Insets(2, 8, 2, 8));
        setPrefHeight(framework_ui_main.RIBBON_BAR_HEIGHT);
        setMinHeight(framework_ui_main.RIBBON_BAR_HEIGHT - 2);
        setMaxHeight(framework_ui_main.RIBBON_BAR_HEIGHT + 4);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        getStyleClass().add("ribbon-bar");

        setStyle(
            "-fx-background-color: " + framework_ui_main.RIBBON_BAR_BG + ";" +
            "-fx-border-color: " + framework_ui_main.RIBBON_BAR_BORDER + ";" +
            "-fx-border-width: 0 0 1 0;"
        );

        Color iconColor = Color.web(framework_ui_main.PRIMARY_BRAND_COLOR);
        double iconSz = framework_ui_main.RIBBON_ICON_SIZE;

        newButton = new splitbutton_ui_main("New", ribbonicons_ui_main.createNewIcon(iconSz, iconColor), () -> {});
        newButton.addMenuItem("New Simulation Project", () -> {});
        newButton.addMenuItem("New 3D Part Model", () -> {});
        newButton.addMenuItem("New Multiphysics Study", () -> {});

        openButton = new splitbutton_ui_main("Open", ribbonicons_ui_main.createOpenIcon(iconSz, iconColor), () -> {});
        openButton.addMenuItem("Open Project (*.astra)...", () -> {});
        openButton.addMenuItem("Open Recent...", () -> {});
        openButton.addMenuItem("Import CAD (STEP/IGES/STL)...", () -> {});

        saveButton = new splitbutton_ui_main("Save", ribbonicons_ui_main.createSaveIcon(iconSz, iconColor), () -> {});
        saveButton.addMenuItem("Save Project", () -> {});
        saveButton.addMenuItem("Save As...", () -> {});
        saveButton.addMenuItem("Export Geometry...", () -> {});

        basicShapesButton = new splitbutton_ui_main("Basic Shapes", loadIcon("/icons/basic_shapes.png"), 68.0,
            () -> triggerShapeSelection(currentShape));
        addShapeItem("Circle", "/icons/circle.png", basicshapes_ui_main.CIRCLE);
        addShapeItem("Square", "/icons/square.png", basicshapes_ui_main.SQUARE);
        addShapeItem("Rectangle", "/icons/rectangle.png", basicshapes_ui_main.RECTANGLE);
        addShapeItem("Equilateral Triangle", "/icons/triangle_equilateral.png", basicshapes_ui_main.EQUILATERAL_TRIANGLE);
        addShapeItem("Right Triangle", "/icons/triangle_right.png", basicshapes_ui_main.RIGHT_TRIANGLE);

        getChildren().addAll(buildFilesGroup(), buildShapesGroup());
    }

    private void addShapeItem(String title, String iconPath, basicshapes_ui_main shape) {
        basicShapesButton.addMenuItem(title, loadIcon(iconPath), () -> {
            this.currentShape = shape;
            triggerShapeSelection(shape);
        });
    }

    private void triggerShapeSelection(basicshapes_ui_main shape) {
        if (onShapeSelected != null) onShapeSelected.accept(shape);
    }

    private VBox buildFilesGroup() {
        return wrapGroup(new HBox(2, newButton, openButton, saveButton), "Files");
    }

    private VBox buildShapesGroup() {
        return wrapGroup(new HBox(2, basicShapesButton), "Basic Shapes");
    }

    private VBox wrapGroup(HBox buttonsRow, String labelText) {
        buttonsRow.setAlignment(Pos.CENTER);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: " + framework_ui_main.RIBBON_GROUP_FONT_SIZE + "px; -fx-text-fill: " + framework_ui_main.TEXT_MUTED_COLOR + "; -fx-font-weight: bold;");
        VBox box = new VBox(1, buttonsRow, lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(1, 2, 1, 2));
        Separator sep = new Separator(Orientation.VERTICAL);
        sep.setPadding(new Insets(4, 3, 4, 3));
        HBox withSep = new HBox(2, box, sep);
        withSep.setAlignment(Pos.CENTER_LEFT);
        return new VBox(withSep);
    }

    private static Node loadIcon(String path) {
        try (InputStream is = ribbon_ui_main.class.getResourceAsStream(path)) {
            if (is != null) {
                ImageView iv = new ImageView(new Image(is));
                iv.setFitWidth(15);
                iv.setFitHeight(15);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception ignored) {}
        return new Circle(5, Color.web(framework_ui_main.PRIMARY_BRAND_COLOR));
    }

    public splitbutton_ui_main getNewButton()         { return newButton; }
    public splitbutton_ui_main getOpenButton()        { return openButton; }
    public splitbutton_ui_main getSaveButton()        { return saveButton; }
    public splitbutton_ui_main getBasicShapesButton() { return basicShapesButton; }

    public void setOnShapeSelected(Consumer<basicshapes_ui_main> cb) { this.onShapeSelected = cb; }

    public void setOnSave(Runnable r) {
        saveButton.setPrimaryAction(r);
        if (!saveButton.getDropMenu().getItems().isEmpty()) {
            saveButton.getDropMenu().getItems().get(0).setOnAction(e -> { if (r != null) r.run(); });
        }
    }

    public void setOnSaveAs(Runnable r) {
        if (saveButton.getDropMenu().getItems().size() > 1) {
            saveButton.getDropMenu().getItems().get(1).setOnAction(e -> { if (r != null) r.run(); });
        }
    }
}
