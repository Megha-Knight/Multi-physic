package ui.featuremanager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import ui.framework_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.sketch.sketch_feature_ui_main;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class feature_tree_cell_ui_main extends TreeCell<feature_tree_node_ui_main> {

    private final ImageView iconView = new ImageView();
    private final Label nameLabel = new Label();
    private final HBox row = new HBox(4);
    private final Supplier<shape_editor_ui_main> editorSupplier;
    private final Supplier<Consumer<sketch_feature_ui_main>> sketchCallbackSupplier;

    public feature_tree_cell_ui_main(Supplier<shape_editor_ui_main> editorSupplier,
                                     Supplier<Consumer<sketch_feature_ui_main>> sketchCallbackSupplier) {
        this.editorSupplier = editorSupplier;
        this.sketchCallbackSupplier = sketchCallbackSupplier;
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(1, 4, 1, 2));
        iconView.setFitWidth(14);
        iconView.setFitHeight(14);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;");

        setOnMouseClicked(e -> {
            shape_editor_ui_main editor = editorSupplier != null ? editorSupplier.get() : null;
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !isEmpty() && editor != null) {
                feature_tree_node_ui_main val = getItem();
                if (val.isHole()) editor.openHoleEditor(val.getParentShape(), val.getHole());
                else if (val.isPattern()) editor.openPatternEditor(val.getParentShape(), val.getPattern());
                else if (val.isExtrude()) editor.openExtrudeEditor(val.getParentShape(), val.getExtrude());
                else if (val.isSketch() && sketchCallbackSupplier != null && sketchCallbackSupplier.get() != null) {
                    sketchCallbackSupplier.get().accept(val.getSketch());
                } else if (val.isShape()) editor.openDimensionEditor(val.getShape());
                e.consume();
            }
        });
    }

    @Override
    protected void updateItem(feature_tree_node_ui_main item, boolean empty) {
        super.updateItem(item, empty);
        setDisclosureNode(null);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            setContextMenu(null);
            setStyle("-fx-background-color: transparent;");
        } else {
            row.getChildren().clear();
            TreeItem<feature_tree_node_ui_main> treeItem = getTreeItem();
            double rowH = getHeight() > 10.0 ? getHeight() : feature_tree_connector_ui_main.ROW_HEIGHT;

            Canvas lines = feature_tree_connector_ui_main.renderGuideLines(treeItem, rowH);
            if (lines != null) row.getChildren().add(lines);

            Node disc = feature_tree_connector_ui_main.createDisclosureNode(treeItem, rowH);
            row.getChildren().add(disc);

            nameLabel.setText(item.getLabel());
            iconView.setImage(item.getIcon());
            row.getChildren().addAll(iconView, nameLabel);

            setGraphic(row);
            setText(null);

            shape_editor_ui_main ed = editorSupplier != null ? editorSupplier.get() : null;
            Consumer<sketch_feature_ui_main> onSk = sketchCallbackSupplier != null ? sketchCallbackSupplier.get() : null;
            setContextMenu(feature_context_menu_helper_ui_main.createMenu(item, ed, onSk));

            setStyle(isSelected()
                    ? "-fx-background-color: " + framework_ui_main.FEATURE_ROW_SELECTED_BG + "; -fx-border-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-border-width: 0 0 0 3;"
                    : "-fx-background-color: transparent; -fx-border-width: 0;");
            nameLabel.setStyle(isSelected()
                    ? "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0369A1;"
                    : "-fx-font-size: 11px; -fx-font-weight: normal; -fx-text-fill: #1E293B;");
        }
    }
}
