package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * shape_editor_ui_main.java
 * Manages active document shape entities, selection, undo/redo, and 3D gizmo translation.
 */
public class shape_editor_ui_main {

    private final List<shape_item_ui_main> shapes = new ArrayList<>();
    private final shape_history_ui_main history = new shape_history_ui_main();
    private shape_item_ui_main selectedShape = null;

    private final Group container;
    private Consumer<String> statusCallback;
    private final axis_drag_controller_ui_main axisDrag;
    private Runnable onShapesChanged;
    private Consumer<shape_item_ui_main> onSelectionChanged;

    public shape_editor_ui_main(Group container, Pane viewport, SubScene subScene,
                                camera_controller_ui_main camCtrl,
                                Function<MouseEvent, Point3D> raycaster,
                                Label hudLabel, BooleanSupplier isDrawingActive,
                                PerspectiveCamera camera) {
        this.container = container;
        axisDrag = new axis_drag_controller_ui_main(viewport, camera, camCtrl, container);
        axisDrag.setStatusCallback(s -> { if (statusCallback != null) statusCallback.accept(s); });
        new shape_event_handler_ui_main(this, viewport, subScene, camCtrl, raycaster,
                                        hudLabel, isDrawingActive, axisDrag);
    }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }
    public void setOnShapesChanged(Runnable r) { this.onShapesChanged = r; }
    public void setOnSelectionChanged(Consumer<shape_item_ui_main> c) { this.onSelectionChanged = c; }
    public void notifyShapesChanged() { if (onShapesChanged != null) onShapesChanged.run(); }

    public void recordSnapshot() { history.pushSnapshot(shapes); }

    public String generateNextName(basic_shapes_ui_main type) {
        String pfx = "untitled_" + type.name().toLowerCase();
        int max = 0;
        for (shape_item_ui_main s : shapes) {
            String n = s.getName();
            if (n != null && n.startsWith(pfx)) {
                try {
                    int num = Integer.parseInt(n.substring(pfx.length()));
                    if (num > max) max = num;
                } catch (Exception ignored) {}
            }
        }
        return pfx + String.format(Locale.US, "%02d", max + 1);
    }

    public void addShape(shape_item_ui_main item) {
        if (item.getName() == null || item.getName().isBlank()) {
            item.setName(generateNextName(item.getType()));
        }
        history.pushSnapshot(shapes);
        shapes.add(item);
        container.getChildren().add(item.getRootGroup());
        selectShape(item);
        notifyShapesChanged();
    }

    public void removeShape(shape_item_ui_main item) {
        if (item == null) return;
        shapes.remove(item);
        container.getChildren().remove(item.getRootGroup());
        if (selectedShape == item) selectShape(null);
        notifyShapesChanged();
    }

    public void deleteSelected() {
        if (selectedShape != null) {
            history.pushSnapshot(shapes);
            String name = selectedShape.getName();
            removeShape(selectedShape);
            if (statusCallback != null) statusCallback.accept("Deleted " + name);
        }
    }

    public void copySelected() {
        if (selectedShape != null) {
            history.copy(selectedShape);
            if (statusCallback != null) statusCallback.accept("Copied " + selectedShape.getName());
        }
    }

    public void cutSelected() {
        if (selectedShape != null) { history.copy(selectedShape); deleteSelected(); }
    }

    public void paste() {
        if (history.hasClipboard()) {
            history.pushSnapshot(shapes);
            shape_item_ui_main item = history.paste(15.0);
            if (item != null) {
                item.setName(generateNextName(item.getType()));
                shapes.add(item);
                container.getChildren().add(item.getRootGroup());
                selectShape(item);
                notifyShapesChanged();
                if (statusCallback != null) statusCallback.accept("Pasted " + item.getName());
            }
        }
    }

    public void undo() {
        List<shape_item_ui_main> prev = history.undo(shapes);
        if (prev != null) { loadShapes(prev); if (statusCallback != null) statusCallback.accept("Undo"); }
    }

    public void redo() {
        List<shape_item_ui_main> next = history.redo(shapes);
        if (next != null) { loadShapes(next); if (statusCallback != null) statusCallback.accept("Redo"); }
    }

    public void loadShapes(List<shape_item_ui_main> newShapes) {
        selectShape(null);
        container.getChildren().clear();
        shapes.clear();
        container.getChildren().add(axisDrag.getGizmo());
        if (newShapes != null) {
            for (shape_item_ui_main s : newShapes) {
                if (s.getName() == null || s.getName().isBlank()) {
                    s.setName(generateNextName(s.getType()));
                }
                shapes.add(s);
                container.getChildren().add(s.getRootGroup());
            }
        }
        notifyShapesChanged();
    }

    public void openDimensionEditor(shape_item_ui_main shape) {
        if (shape == null) return;
        dimension_editor_dialog_ui_main.open(shape, this,
            container.getScene() != null ? container.getScene().getWindow() : null);
    }

    public void clearHistory() { history.clear(); }
    public List<shape_item_ui_main> getShapes() { return new ArrayList<>(shapes); }
    public shape_item_ui_main getSelectedShape()  { return selectedShape; }
    public axis_drag_controller_ui_main getAxisDrag() { return axisDrag; }

    public void selectShape(shape_item_ui_main item) {
        if (selectedShape != null) selectedShape.setSelected(false);
        selectedShape = item;
        if (selectedShape != null) {
            selectedShape.setSelected(true);
            axisDrag.attachTo(selectedShape);
            if (statusCallback != null) {
                String msg = selectedShape.getName() + " selected (" + selectedShape.getType().getLabel() + ")";
                statusCallback.accept(msg);
            }
        } else {
            axisDrag.detach();
        }
        if (onSelectionChanged != null) onSelectionChanged.accept(selectedShape);
    }

    public shape_item_ui_main findShapeNear(Point3D pt, double threshold) {
        if (selectedShape != null && selectedShape.isNear(pt, threshold)) return selectedShape;
        for (int i = shapes.size() - 1; i >= 0; i--) {
            if (shapes.get(i).isNear(pt, threshold)) return shapes.get(i);
        }
        return null;
    }

    public shape_item_ui_main findShapeByNode(javafx.scene.Node node) {
        if (node == null) return null;
        for (int i = shapes.size() - 1; i >= 0; i--) {
            if (shapes.get(i).containsNode(node)) return shapes.get(i);
        }
        return null;
    }
}
