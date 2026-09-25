package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.camera.camera_controller_ui_main;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * shape_editor_ui_main.java
 * Manages active document shape entities, selection, undo/redo, and clipboard operations.
 */
public class shape_editor_ui_main {

    private final List<shape_item_ui_main> shapes = new ArrayList<>();
    private final shape_history_ui_main history = new shape_history_ui_main();
    private shape_item_ui_main selectedShape = null;

    private final Group container;
    private Consumer<String> statusCallback;

    public shape_editor_ui_main(Group container, Pane viewport,
                                camera_controller_ui_main camCtrl,
                                Function<MouseEvent, Point3D> raycaster,
                                Label hudLabel,
                                BooleanSupplier isDrawingActive) {
        this.container = container;
        new shape_event_handler_ui_main(this, viewport, camCtrl, raycaster, hudLabel, isDrawingActive);
    }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }

    public void recordSnapshot() { history.pushSnapshot(shapes); }

    public void addShape(shape_item_ui_main item) {
        history.pushSnapshot(shapes);
        shapes.add(item);
        container.getChildren().add(item.getRootGroup());
        selectShape(item);
    }

    public void removeShape(shape_item_ui_main item) {
        if (item == null) return;
        shapes.remove(item);
        container.getChildren().remove(item.getRootGroup());
        if (selectedShape == item) selectShape(null);
    }

    public void deleteSelected() {
        if (selectedShape != null) {
            history.pushSnapshot(shapes);
            String name = selectedShape.getType().getLabel();
            removeShape(selectedShape);
            if (statusCallback != null) statusCallback.accept("Deleted " + name);
        }
    }

    public void copySelected() {
        if (selectedShape != null) {
            history.copy(selectedShape);
            if (statusCallback != null)
                statusCallback.accept("Copied " + selectedShape.getType().getLabel());
        }
    }

    public void cutSelected() {
        if (selectedShape != null) {
            history.copy(selectedShape);
            deleteSelected();
        }
    }

    public void paste() {
        if (history.hasClipboard()) {
            history.pushSnapshot(shapes);
            shape_item_ui_main item = history.paste(15.0);
            if (item != null) {
                shapes.add(item);
                container.getChildren().add(item.getRootGroup());
                selectShape(item);
                if (statusCallback != null)
                    statusCallback.accept("Pasted " + item.getType().getLabel());
            }
        }
    }

    public void undo() {
        List<shape_item_ui_main> prev = history.undo(shapes);
        if (prev != null) {
            loadShapes(prev);
            if (statusCallback != null) statusCallback.accept("Undo");
        }
    }

    public void redo() {
        List<shape_item_ui_main> next = history.redo(shapes);
        if (next != null) {
            loadShapes(next);
            if (statusCallback != null) statusCallback.accept("Redo");
        }
    }

    public void loadShapes(List<shape_item_ui_main> newShapes) {
        selectShape(null);
        container.getChildren().clear();
        shapes.clear();
        if (newShapes != null) {
            for (shape_item_ui_main s : newShapes) {
                shapes.add(s);
                container.getChildren().add(s.getRootGroup());
            }
        }
    }

    public void clearHistory() { history.clear(); }
    public List<shape_item_ui_main> getShapes() { return new ArrayList<>(shapes); }
    public shape_item_ui_main getSelectedShape() { return selectedShape; }

    public void selectShape(shape_item_ui_main item) {
        if (selectedShape != null) selectedShape.setSelected(false);
        selectedShape = item;
        if (selectedShape != null) {
            selectedShape.setSelected(true);
            if (statusCallback != null)
                statusCallback.accept(selectedShape.getType().getLabel()
                    + " selected. Drag to move, handles to reshape.");
        }
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
