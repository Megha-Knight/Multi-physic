package ui.featuremanager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.sketch.sketch_feature_ui_main;
import ui.workspace.drafting.topology.shape_face_appearance_helper_ui_main;
import ui.workspace.drafting.topology.topology_face_appearance_ui_main;

import java.util.function.Consumer;

/**
 * feature_context_menu_helper_ui_main.java
 * Context menu factory for Feature Manager tree items (Shapes, Sketches, Holes, Extrudes, Derived Faces).
 */
public final class feature_context_menu_helper_ui_main {

    private feature_context_menu_helper_ui_main() {}

    public static ContextMenu createMenu(feature_tree_node_ui_main item, shape_editor_ui_main editor,
                                         Consumer<sketch_feature_ui_main> onSketch) {
        if (item == null) return null;
        ContextMenu cm = new ContextMenu();

        if (item.isDerivedFace()) {
            var df = item.getDerivedFace();
            var s = item.getParentShape();
            MenuItem sel = new MenuItem("Select Derived Face");
            sel.setOnAction(e -> { if (editor != null && s != null) editor.selectShape(s); });
            MenuItem color = new MenuItem("Change Color...");
            color.setOnAction(e -> openColorPicker(s, df.getId(), editor));
            MenuItem reset = new MenuItem("Reset Color");
            reset.setOnAction(e -> {
                shape_face_appearance_helper_ui_main.clearAppearance(s.getId(), df.getId());
                df.setAppearanceOverride(null);
                if (editor != null) editor.notifyShapesChanged();
            });
            cm.getItems().addAll(sel, color, reset);
        } else if (item.isHole()) {
            MenuItem edit = new MenuItem("Edit / Resize..."), del = new MenuItem("Delete Hole"),
                     ren = new MenuItem("Rename..."), tog = new MenuItem(item.getHole().isVisible() ? "Suppress / Hide" : "Unsuppress / Show");
            edit.setOnAction(e -> editor.openHoleEditor(item.getParentShape(), item.getHole()));
            del.setOnAction(e -> { item.getParentShape().removeHole(item.getHole().getId()); editor.notifyShapesChanged(); });
            ren.setOnAction(e -> promptRename("Rename Hole", item.getHole().getName(), n -> { item.getHole().setName(n); item.getParentShape().rebuild(); editor.notifyShapesChanged(); }));
            tog.setOnAction(e -> { item.getHole().setVisible(!item.getHole().isVisible()); item.getParentShape().rebuild(); editor.notifyShapesChanged(); });
            cm.getItems().addAll(edit, ren, tog, new SeparatorMenuItem(), del);
        } else if (item.isPattern()) {
            MenuItem edit = new MenuItem("Edit Pattern..."), del = new MenuItem("Delete Pattern"),
                     tog = new MenuItem(item.getPattern().isVisible() ? "Suppress / Hide" : "Unsuppress / Show");
            edit.setOnAction(e -> editor.openPatternEditor(item.getParentShape(), item.getPattern()));
            del.setOnAction(e -> { item.getParentShape().removePattern(item.getPattern().getId()); editor.notifyShapesChanged(); });
            tog.setOnAction(e -> { item.getPattern().setVisible(!item.getPattern().isVisible()); item.getParentShape().rebuild(); editor.notifyShapesChanged(); });
            cm.getItems().addAll(edit, tog, new SeparatorMenuItem(), del);
        } else if (item.isExtrude()) {
            MenuItem edit = new MenuItem("Edit / Resize..."), del = new MenuItem("Delete Extrude"),
                     ren = new MenuItem("Rename..."), tog = new MenuItem(item.getExtrude().isVisible() ? "Suppress / Hide" : "Unsuppress / Show");
            edit.setOnAction(e -> editor.openExtrudeEditor(item.getParentShape(), item.getExtrude()));
            del.setOnAction(e -> { item.getParentShape().removeExtrude(item.getExtrude().getId()); editor.notifyShapesChanged(); });
            ren.setOnAction(e -> promptRename("Rename Extrude", item.getExtrude().getName(), n -> { item.getExtrude().setName(n); item.getParentShape().rebuild(); editor.notifyShapesChanged(); }));
            tog.setOnAction(e -> { item.getExtrude().setVisible(!item.getExtrude().isVisible()); item.getParentShape().rebuild(); editor.notifyShapesChanged(); });
            cm.getItems().addAll(edit, ren, tog, new SeparatorMenuItem(), del);
        } else if (item.isSketch()) {
            MenuItem edit = new MenuItem("Edit Sketch..."), del = new MenuItem("Delete Sketch");
            edit.setOnAction(e -> { if (onSketch != null) onSketch.accept(item.getSketch()); });
            del.setOnAction(e -> { if (item.getParentShape() != null) { item.getParentShape().removeSketch(item.getSketch().getId()); editor.notifyShapesChanged(); } });
            cm.getItems().addAll(edit, new SeparatorMenuItem(), del);
        } else if (item.isShape()) {
            MenuItem edit = new MenuItem("Edit Dimensions..."), del = new MenuItem("Delete Shape"), ren = new MenuItem("Rename...");
            edit.setOnAction(e -> editor.openDimensionEditor(item.getShape()));
            del.setOnAction(e -> editor.removeShape(item.getShape()));
            ren.setOnAction(e -> promptRename("Rename Shape", item.getShape().getName(), n -> { item.getShape().setName(n); editor.notifyShapesChanged(); }));
            cm.getItems().addAll(edit, ren, new SeparatorMenuItem(), del);
        }
        return cm;
    }

    private static void openColorPicker(ui.workspace.drafting.shape_item_ui_main shape, String faceId, shape_editor_ui_main editor) {
        if (shape == null || faceId == null) return;
        Stage st = new Stage();
        st.initModality(Modality.APPLICATION_MODAL);
        st.setTitle("Derived Face Appearance");
        ColorPicker cp = new ColorPicker(Color.web("#F59E0B"));
        Button apply = new Button("Apply"), cancel = new Button("Cancel");
        apply.setOnAction(e -> {
            topology_face_appearance_ui_main app = new topology_face_appearance_ui_main(cp.getValue(), 0.85, true);
            shape_face_appearance_helper_ui_main.setAppearance(shape.getId(), faceId, app);
            if (editor != null) {
                editor.recordSnapshot();
                editor.notifyShapesChanged();
            }
            st.close();
        });
        cancel.setOnAction(e -> st.close());
        HBox btns = new HBox(8, cancel, apply);
        btns.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(12, new Label("Select appearance color for " + faceId + ":"), cp, btns);
        root.setPadding(new Insets(12));
        st.setScene(new Scene(root, 280, 140));
        st.show();
    }

    private static void promptRename(String title, String cur, Consumer<String> onRename) {
        TextInputDialog d = new TextInputDialog(cur);
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText("New Name:");
        d.showAndWait().ifPresent(n -> { if (!n.isBlank()) onRename.accept(n.trim()); });
    }
}
