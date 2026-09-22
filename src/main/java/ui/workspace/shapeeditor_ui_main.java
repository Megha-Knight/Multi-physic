package ui.workspace;

import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * shapeeditor_ui_main.java
 * Manages shape selection, interactive moving across the ground plane, and corner handle reshaping.
 */
public class shapeeditor_ui_main {

    private enum EditMode { IDLE, MOVE_SHAPE, RESHAPE_HANDLE }

    private final List<shapeitem_ui_main> shapes = new ArrayList<>();
    private shapeitem_ui_main selectedShape = null;
    private EditMode mode = EditMode.IDLE;
    private int activeHandleIdx = -1;
    private Point3D lastHit = null;

    private final Group container;
    private final Pane viewport;
    private final cameracontroller_ui_main cameraController;
    private final Function<MouseEvent, Point3D> groundRaycaster;
    private final Label hudLabel;
    private final java.util.function.BooleanSupplier isDrawingActive;
    private Consumer<String> statusCallback;

    public shapeeditor_ui_main(Group container, Pane viewport, cameracontroller_ui_main camCtrl,
                               Function<MouseEvent, Point3D> raycaster, Label hudLabel,
                               java.util.function.BooleanSupplier isDrawingActive) {
        this.container = container;
        this.viewport = viewport;
        this.cameraController = camCtrl;
        this.groundRaycaster = raycaster;
        this.hudLabel = hudLabel;
        this.isDrawingActive = isDrawingActive;
        attachListeners();
    }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }

    public void addShape(shapeitem_ui_main item) {
        shapes.add(item);
        container.getChildren().add(item.getRootGroup());
        selectShape(item);
    }

    public void removeShape(shapeitem_ui_main item) {
        if (item == null) return;
        shapes.remove(item);
        container.getChildren().remove(item.getRootGroup());
        if (selectedShape == item) selectShape(null);
        if (statusCallback != null) statusCallback.accept("Deleted shape.");
    }

    public void selectShape(shapeitem_ui_main item) {
        if (selectedShape != null) selectedShape.setSelected(false);
        selectedShape = item;
        if (selectedShape != null) {
            selectedShape.setSelected(true);
            if (statusCallback != null) {
                statusCallback.accept(selectedShape.getType().getLabel() + " selected. Drag to move, or drag corner handles to reshape.");
            }
        }
    }

    private void attachListeners() {
        viewport.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (isDrawingActive != null && isDrawingActive.getAsBoolean()) return;
            if (mode != EditMode.IDLE) return;
            Point3D hit = groundRaycaster.apply(e);
            if (hit == null) { viewport.setCursor(Cursor.DEFAULT); return; }
            if (selectedShape != null && selectedShape.findHandleNear(hit, 5.0) >= 0) {
                viewport.setCursor(Cursor.CROSSHAIR);
            } else if (findShapeNear(hit, 4.0) != null) {
                viewport.setCursor(Cursor.MOVE);
            } else {
                viewport.setCursor(Cursor.DEFAULT);
            }
        });

        viewport.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (isDrawingActive != null && isDrawingActive.getAsBoolean()) return;
            if (e.getButton() != MouseButton.PRIMARY) return;
            Point3D hit = groundRaycaster.apply(e);
            if (hit == null) return;

            if (selectedShape != null) {
                int hIdx = selectedShape.findHandleNear(hit, 5.5);
                if (hIdx >= 0) {
                    mode = EditMode.RESHAPE_HANDLE;
                    activeHandleIdx = hIdx;
                    cameraController.setEnabled(false);
                    e.consume();
                    return;
                }
            }

            shapeitem_ui_main hitShape = findShapeNear(hit, 4.5);
            if (hitShape != null) {
                selectShape(hitShape);
                mode = EditMode.MOVE_SHAPE;
                lastHit = hit;
                cameraController.setEnabled(false);
                e.consume();
            } else if (selectedShape != null) {
                selectShape(null);
                cameraController.setEnabled(true);
            }
        });

        viewport.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (mode == EditMode.IDLE || selectedShape == null) return;
            Point3D hit = groundRaycaster.apply(e);
            if (hit == null) return;

            if (mode == EditMode.RESHAPE_HANDLE) {
                selectedShape.moveHandle(activeHandleIdx, hit);
                hudLabel.setText(selectedShape.formatDimensions());
                hudLabel.setVisible(true);
                e.consume();
            } else if (mode == EditMode.MOVE_SHAPE && lastHit != null) {
                double dx = hit.getX() - lastHit.getX();
                double dz = hit.getZ() - lastHit.getZ();
                selectedShape.translate(dx, dz);
                lastHit = hit;
                hudLabel.setText(String.format("Position: X=%.1f, Z=%.1f", hit.getX(), hit.getZ()));
                hudLabel.setVisible(true);
                e.consume();
            }
        });

        viewport.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (mode != EditMode.IDLE) {
                mode = EditMode.IDLE;
                cameraController.setEnabled(true);
                hudLabel.setVisible(false);
                if (selectedShape != null && statusCallback != null) {
                    statusCallback.accept(selectedShape.formatDimensions());
                }
                e.consume();
            }
        });

        viewport.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (selectedShape != null) {
                if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                    removeShape(selectedShape);
                    e.consume();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    selectShape(null);
                    e.consume();
                }
            }
        });
    }

    private shapeitem_ui_main findShapeNear(Point3D pt, double threshold) {
        if (selectedShape != null && selectedShape.isNear(pt, threshold)) return selectedShape;
        for (int i = shapes.size() - 1; i >= 0; i--) {
            if (shapes.get(i).isNear(pt, threshold)) return shapes.get(i);
        }
        return null;
    }

    public shapeitem_ui_main getSelectedShape() { return selectedShape; }
    public boolean isEditing() { return mode != EditMode.IDLE; }
}
