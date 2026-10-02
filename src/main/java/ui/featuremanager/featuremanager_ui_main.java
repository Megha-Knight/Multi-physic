package ui.featuremanager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import ui.framework_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;

public class featuremanager_ui_main extends BorderPane {

    private final TreeView<feature_tree_node_ui_main> treeView = new TreeView<>();
    private final TreeItem<feature_tree_node_ui_main> rootItem = new TreeItem<>(null);
    private shape_editor_ui_main editor;
    private boolean syncLock = false;

    public featuremanager_ui_main() {
        getStyleClass().add("feature-manager-pane");
        setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #C9D1D9; -fx-border-width: 0 1 0 0;");

        HBox header = new HBox(); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(4, 10, 4, 10)); header.setPrefHeight(26);
        header.setStyle("-fx-background-color: #F1F5F9; -fx-border-color: #CBD5E1; -fx-border-width: 0 0 1 0;");
        Label titleLabel = new Label("Feature Manager"); titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1F2933;");
        header.getChildren().add(titleLabel); setTop(header);

        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);
        treeView.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        treeView.setCellFactory(tv -> new FeatureTreeCell());
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (syncLock || editor == null || newVal == null || newVal.getValue() == null) return;
            syncLock = true;
            try {
                feature_tree_node_ui_main val = newVal.getValue();
                if (val.isShape()) {
                    editor.selectShape(val.getShape());
                    editor.selectHole(val.getShape(), null);
                } else if (val.isHole()) {
                    editor.selectHole(val.getParentShape(), val.getHole());
                }
            } finally {
                syncLock = false;
            }
        });

        treeView.setOnMouseClicked(e -> {
            if (e.getTarget() == treeView && editor != null) {
                editor.selectHole(null, null);
                editor.selectShape(null);
            }
        });
        setCenter(treeView);
    }

    public void bindToEditor(shape_editor_ui_main ed) {
        this.editor = ed;
        if (editor == null) return;
        editor.setOnShapesChanged(this::refresh);
        editor.setOnSelectionChanged(s -> syncFromCanvas());
        editor.setOnHoleSelectionChanged((s, h) -> syncFromCanvas());
        refresh();
    }

    public void refresh() {
        if (editor == null) return;
        syncLock = true;
        try {
            rootItem.getChildren().clear();
            for (shape_item_ui_main s : editor.getShapes()) {
                TreeItem<feature_tree_node_ui_main> sNode = new TreeItem<>(feature_tree_node_ui_main.forShape(s));
                sNode.setExpanded(true);
                for (hole_feature_ui_main h : s.getHoles()) {
                    sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forHole(s, h)));
                }
                rootItem.getChildren().add(sNode);
            }
            syncFromCanvas();
        } finally {
            syncLock = false;
        }
    }

    public TreeView<feature_tree_node_ui_main> getTreeView() { return treeView; }

    private void syncFromCanvas() {
        if (editor == null) return;
        shape_item_ui_main selShape = editor.getSelectedShape();
        hole_feature_ui_main selHole = editor.getSelectedHole();
        if (selShape == null) { treeView.getSelectionModel().clearSelection(); return; }

        for (TreeItem<feature_tree_node_ui_main> sItem : rootItem.getChildren()) {
            if (sItem.getValue().getShape() == selShape) {
                if (selHole != null) {
                    for (TreeItem<feature_tree_node_ui_main> hItem : sItem.getChildren()) {
                        if (hItem.getValue().getHole() == selHole) {
                            sItem.setExpanded(true);
                            treeView.getSelectionModel().select(hItem);
                            treeView.scrollTo(treeView.getRow(hItem));
                            return;
                        }
                    }
                }
                treeView.getSelectionModel().select(sItem);
                treeView.scrollTo(treeView.getRow(sItem));
                return;
            }
        }
    }

    public static String formatItemLabel(shape_item_ui_main item) {
        if (item == null) return "";
        String lbl = item.getName();
        if (item.hasHoles() || item.hasPatterns()) {
            lbl += " [";
            if (item.hasHoles()) { int c = item.getHoles().size(); lbl += c + (c == 1 ? " Hole" : " Holes"); }
            if (item.hasHoles() && item.hasPatterns()) lbl += ", ";
            if (item.hasPatterns()) { int p = item.getPatterns().size(); lbl += p + (p == 1 ? " Pattern" : " Patterns"); }
            lbl += "]";
        }
        return lbl;
    }

    private class FeatureTreeCell extends TreeCell<feature_tree_node_ui_main> {
        private final ImageView iconView = new ImageView();
        private final Label nameLabel = new Label();
        private final HBox row = new HBox(6, iconView, nameLabel);

        FeatureTreeCell() {
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 4, 2, 4));
            iconView.setFitWidth(14);
            iconView.setFitHeight(14);
            nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;");

            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !isEmpty() && editor != null) {
                    feature_tree_node_ui_main val = getItem();
                    if (val.isHole()) editor.openHoleEditor(val.getParentShape(), val.getHole());
                    else if (val.isShape()) editor.openDimensionEditor(val.getShape());
                    e.consume();
                }
            });
        }

        @Override
        protected void updateItem(feature_tree_node_ui_main item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null); setText(null); setContextMenu(null); setStyle("-fx-background-color: transparent;");
            } else {
                nameLabel.setText(item.getLabel());
                iconView.setImage(item.getIcon());
                setGraphic(row); setText(null);
                setContextMenu(createContextMenu(item));
                if (isSelected()) {
                    setStyle("-fx-background-color: " + framework_ui_main.FEATURE_ROW_SELECTED_BG + "; -fx-border-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-border-width: 0 0 0 3;");
                    nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0369A1;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
                    nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: normal; -fx-text-fill: #1E293B;");
                }
            }
        }

        private ContextMenu createContextMenu(feature_tree_node_ui_main item) {
            ContextMenu cm = new ContextMenu();
            if (item.isHole()) {
                MenuItem edit = new MenuItem("Edit / Resize..."); edit.setOnAction(e -> editor.openHoleEditor(item.getParentShape(), item.getHole()));
                MenuItem ren = new MenuItem("Rename..."); ren.setOnAction(e -> promptRenameHole(item.getParentShape(), item.getHole()));
                MenuItem del = new MenuItem("Delete Hole"); del.setOnAction(e -> { item.getParentShape().removeHole(item.getHole().getId()); editor.notifyShapesChanged(); });
                cm.getItems().addAll(edit, ren, new SeparatorMenuItem(), del);
            } else if (item.isShape()) {
                MenuItem edit = new MenuItem("Edit Dimensions..."); edit.setOnAction(e -> editor.openDimensionEditor(item.getShape()));
                MenuItem ren = new MenuItem("Rename..."); ren.setOnAction(e -> promptRenameShape(item.getShape()));
                MenuItem del = new MenuItem("Delete Shape"); del.setOnAction(e -> editor.removeShape(item.getShape()));
                cm.getItems().addAll(edit, ren, new SeparatorMenuItem(), del);
            }
            return cm;
        }
    }

    private void promptRenameHole(shape_item_ui_main shape, hole_feature_ui_main hole) {
        TextInputDialog d = new TextInputDialog(hole.getName()); d.setTitle("Rename Hole"); d.setHeaderText(null); d.setContentText("New Name:");
        d.showAndWait().ifPresent(name -> { if (!name.isBlank()) { hole.setName(name.trim()); shape.rebuild(); editor.notifyShapesChanged(); } });
    }

    private void promptRenameShape(shape_item_ui_main shape) {
        TextInputDialog d = new TextInputDialog(shape.getName()); d.setTitle("Rename Shape"); d.setHeaderText(null); d.setContentText("New Name:");
        d.showAndWait().ifPresent(name -> { if (!name.isBlank()) { shape.setName(name.trim()); editor.notifyShapesChanged(); } });
    }
}
