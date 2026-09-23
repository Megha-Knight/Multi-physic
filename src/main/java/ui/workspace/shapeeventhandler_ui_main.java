package ui.workspace;

import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * shapeeventhandler_ui_main.java
 * Mouse and keyboard event controller for interactive 2D and 3D CAD shape editing.
 */
public class shapeeventhandler_ui_main {

    private enum EditMode { IDLE, MOVE_SHAPE, RESHAPE_HANDLE }

    private final shapeeditor_ui_main editor;
    private final Pane viewport;
    private final SubScene subScene;
    private final cameracontroller_ui_main cameraController;
    private final Function<MouseEvent, Point3D> groundRaycaster;
    private final Label hudLabel;
    private final BooleanSupplier isDrawingActive;

    private EditMode mode = EditMode.IDLE;
    private int activeHandleIdx = -1;
    private Point3D lastHit = null;

    public shapeeventhandler_ui_main(shapeeditor_ui_main editor, Pane viewport, SubScene subScene,
                                     cameracontroller_ui_main camCtrl,
                                     Function<MouseEvent, Point3D> raycaster,
                                     Label hudLabel, BooleanSupplier isDrawingActive) {
        this.editor = editor;
        this.viewport = viewport;
        this.subScene = subScene;
        this.cameraController = camCtrl;
        this.groundRaycaster = raycaster;
        this.hudLabel = hudLabel;
        this.isDrawingActive = isDrawingActive;
        viewport.setFocusTraversable(true);
        if (subScene != null) subScene.setFocusTraversable(true);
        attach();
    }

    private void attach() {
        Node target = (subScene != null) ? subScene : viewport;
        target.addEventFilter(MouseEvent.MOUSE_MOVED, this::handleMoved);
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handlePressed);
        target.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleDragged);
        target.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleReleased);
        target.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        viewport.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    private void setCursor(Cursor c) {
        if (subScene != null) subScene.setCursor(c);
        viewport.setCursor(c);
    }

    private void handleMoved(MouseEvent e) {
        if (isDrawingActive != null && isDrawingActive.getAsBoolean()) return;
        if (mode != EditMode.IDLE) return;
        Node hitNode = (e.getPickResult() != null) ? e.getPickResult().getIntersectedNode() : null;
        shapeitem_ui_main sel = editor.getSelectedShape();
        Point3D gHit = groundRaycaster.apply(e);

        if (sel != null && (sel.findHandleByNode(hitNode) >= 0 || (hitNode == null && gHit != null && sel.findHandleNear(gHit, 5.0) >= 0))) {
            setCursor(Cursor.CROSSHAIR);
            return;
        }
        if (editor.findShapeByNode(hitNode) != null || (gHit != null && editor.findShapeNear(gHit, 4.0) != null)) {
            setCursor(Cursor.MOVE);
            return;
        }
        setCursor(Cursor.DEFAULT);
    }

    private void handlePressed(MouseEvent e) {
        if (isDrawingActive != null && isDrawingActive.getAsBoolean()) return;
        viewport.requestFocus();
        if (subScene != null) subScene.requestFocus();
        if (e.getButton() != MouseButton.PRIMARY) return;
        Point3D hit = groundRaycaster.apply(e);
        Node hitNode = (e.getPickResult() != null) ? e.getPickResult().getIntersectedNode() : null;

        shapeitem_ui_main sel = editor.getSelectedShape();
        if (sel != null) {
            int hIdx = sel.findHandleByNode(hitNode);
            if (hIdx < 0 && hitNode == null && hit != null) hIdx = sel.findHandleNear(hit, 5.5);
            if (hIdx >= 0) {
                editor.recordSnapshot();
                mode = EditMode.RESHAPE_HANDLE;
                activeHandleIdx = hIdx;
                cameraController.setEnabled(false);
                e.consume();
                return;
            }
        }

        shapeitem_ui_main hitShape = editor.findShapeByNode(hitNode);
        if (hitShape == null && hit != null) hitShape = editor.findShapeNear(hit, 4.5);

        if (hitShape != null) {
            editor.recordSnapshot();
            editor.selectShape(hitShape);
            mode = EditMode.MOVE_SHAPE;
            lastHit = (hit != null) ? hit : new Point3D(e.getX(), 0, e.getY());
            cameraController.setEnabled(false);
            e.consume();
        } else if (sel != null) {
            editor.selectShape(null);
            cameraController.setEnabled(true);
        }
    }

    private void handleDragged(MouseEvent e) {
        if (mode == EditMode.IDLE || editor.getSelectedShape() == null) return;
        Point3D hit = groundRaycaster.apply(e);
        if (hit == null) return;
        shapeitem_ui_main sel = editor.getSelectedShape();
        if (mode == EditMode.RESHAPE_HANDLE) {
            sel.moveHandle(activeHandleIdx, hit);
            hudLabel.setText(sel.formatDimensions());
            hudLabel.setVisible(true);
        } else if (mode == EditMode.MOVE_SHAPE && lastHit != null) {
            sel.translate(hit.getX() - lastHit.getX(), hit.getZ() - lastHit.getZ());
            lastHit = hit;
            hudLabel.setText(String.format("Position: X=%.1f, Z=%.1f", hit.getX(), hit.getZ()));
            hudLabel.setVisible(true);
        }
        e.consume();
    }

    private void handleReleased(MouseEvent e) {
        if (mode != EditMode.IDLE) {
            mode = EditMode.IDLE;
            cameraController.setEnabled(true);
            hudLabel.setVisible(false);
            e.consume();
        }
    }

    private void handleKeyPressed(KeyEvent e) {
        if (editor.getSelectedShape() != null) {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                editor.deleteSelected();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                editor.selectShape(null);
                e.consume();
            }
        }
    }
}
