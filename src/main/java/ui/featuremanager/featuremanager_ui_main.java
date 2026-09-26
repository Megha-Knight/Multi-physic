package ui.featuremanager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import ui.framework_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

/**
 * featuremanager_ui_main.java
 * Synchronized Feature Manager tree list representing canvas CAD shapes.
 */
public class featuremanager_ui_main extends BorderPane {

    private final ListView<shape_item_ui_main> listView = new ListView<>();
    private shape_editor_ui_main editor;
    private boolean syncLock = false;

    public featuremanager_ui_main() {
        getStyleClass().add("feature-manager-pane");
        setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #C9D1D9; -fx-border-width: 0 1 0 0;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 10, 4, 10));
        header.setPrefHeight(26);
        header.setStyle("-fx-background-color: #F1F5F9; -fx-border-color: #CBD5E1; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("Feature Manager");
        titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1F2933;");
        header.getChildren().add(titleLabel);
        setTop(header);

        listView.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        listView.setCellFactory(lv -> new FeatureCell());
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (syncLock || editor == null) return;
            syncLock = true;
            try {
                editor.selectShape(newVal);
            } finally {
                syncLock = false;
            }
        });

        listView.setOnMouseClicked(e -> {
            if (e.getTarget() == listView && editor != null) {
                editor.selectShape(null);
            }
        });

        setCenter(listView);
    }

    public void bindToEditor(shape_editor_ui_main ed) {
        this.editor = ed;
        if (editor == null) return;
        editor.setOnShapesChanged(this::refresh);
        editor.setOnSelectionChanged(this::updateSelectionFromCanvas);
        refresh();
    }

    public void refresh() {
        if (editor == null) return;
        syncLock = true;
        try {
            listView.getSelectionModel().clearSelection();
            listView.getItems().setAll(editor.getShapes());
            shape_item_ui_main sel = editor.getSelectedShape();
            if (sel != null && editor.getShapes().contains(sel)) {
                listView.getSelectionModel().select(sel);
                listView.scrollTo(sel);
            }
        } finally {
            syncLock = false;
        }
    }

    private void updateSelectionFromCanvas(shape_item_ui_main sel) {
        if (syncLock) return;
        syncLock = true;
        try {
            if (sel != null && listView.getItems().contains(sel)) {
                listView.getSelectionModel().select(sel);
                listView.scrollTo(sel);
            } else {
                listView.getSelectionModel().clearSelection();
            }
        } finally {
            syncLock = false;
        }
    }

    private class FeatureCell extends ListCell<shape_item_ui_main> {
        private final ImageView iconView = new ImageView();
        private final Label nameLabel = new Label();
        private final HBox row = new HBox(6, iconView, nameLabel);

        FeatureCell() {
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(3, 8, 3, 8));
            iconView.setFitWidth(14);
            iconView.setFitHeight(14);
            nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;");

            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !isEmpty() && editor != null) {
                    editor.openDimensionEditor(getItem());
                    e.consume();
                }
            });
        }

        @Override
        protected void updateItem(shape_item_ui_main item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                nameLabel.setText(item.getName());
                Image img = loadShapeIcon(item.getType());
                if (img != null) iconView.setImage(img);
                setGraphic(row);
                setText(null);
                if (isSelected()) {
                    setStyle("-fx-background-color: " + framework_ui_main.FEATURE_ROW_SELECTED_BG +
                             "; -fx-border-color: " + framework_ui_main.OBJECT_SELECTED_COLOR +
                             "; -fx-border-width: 0 0 0 3;");
                    nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0369A1;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
                    nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: normal; -fx-text-fill: #1E293B;");
                }
            }
        }
    }

    private static Image loadShapeIcon(basic_shapes_ui_main type) {
        String p = switch (type) {
            case CIRCLE -> "/icons/circle.png";
            case SQUARE -> "/icons/square.png";
            case RECTANGLE -> "/icons/rectangle.png";
            case EQUILATERAL_TRIANGLE -> "/icons/triangle_equilateral.png";
            case RIGHT_TRIANGLE -> "/icons/triangle_right.png";
            case CUBE -> "/icons/cube_3d.png";
            case CYLINDER -> "/icons/cylinder_3d.png";
            case SPHERE -> "/icons/sphere_3d.png";
            case CONE -> "/icons/cone_3d.png";
            default -> "/icons/basic_shapes.png";
        };
        try {
            var res = featuremanager_ui_main.class.getResourceAsStream(p);
            return (res != null) ? new Image(res) : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
