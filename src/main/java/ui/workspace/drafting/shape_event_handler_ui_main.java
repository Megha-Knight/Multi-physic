package ui.workspace.drafting;

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
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.drafting.translation_gizmo_ui_main.Axis;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * shape_event_handler_ui_main.java
 * Mouse and keyboard event controller for interactive 2D/3D CAD editing & 360° rotation.
 */
public class shape_event_handler_ui_main {

    private enum EditMode { IDLE, AXIS_DRAG, MOVE_SHAPE, RESHAPE_HANDLE, ROTATE_SHAPE }

    private final shape_editor_ui_main editor;
    private final Pane viewport;
    private final SubScene subScene;
    private final camera_controller_ui_main cameraController;
    private final Function<MouseEvent, Point3D> groundRaycaster;
    private final Label hudLabel;
    private final BooleanSupplier isDrawingActive;
    private final axis_drag_controller_ui_main axisDrag;

    private EditMode mode = EditMode.IDLE;
    private int activeHandleIdx = -1;
    private Point3D lastHit = null;
    private double startDragX = 0, startAngle = 0;

    public shape_event_handler_ui_main(shape_editor_ui_main editor, Pane viewport, SubScene subScene,
                                       camera_controller_ui_main camCtrl, Function<MouseEvent, Point3D> raycaster,
                                       Label hudLabel, BooleanSupplier isDrawingActive,
                                       axis_drag_controller_ui_main axisDrag) {
        this.editor = editor; this.viewport = viewport; this.subScene = subScene;
        this.cameraController = camCtrl; this.groundRaycaster = raycaster;
        this.hudLabel = hudLabel; this.isDrawingActive = isDrawingActive;
        this.axisDrag = axisDrag;
        viewport.setFocusTraversable(true);
        if (subScene != null) subScene.setFocusTraversable(true);
        attach();
    }

    private void attach() {
        Node target = (subScene != null) ? subScene : viewport;
        target.addEventFilter(MouseEvent.MOUSE_MOVED,    this::handleMoved);
        target.addEventFilter(MouseEvent.MOUSE_PRESSED,  this::handlePressed);
        target.addEventFilter(MouseEvent.MOUSE_DRAGGED,  this::handleDragged);
        target.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleReleased);
        target.addEventFilter(KeyEvent.KEY_PRESSED,      this::handleKeyPressed);
        viewport.addEventFilter(KeyEvent.KEY_PRESSED,    this::handleKeyPressed);
    }

    private void setCursor(Cursor c) { if (subScene != null) subScene.setCursor(c); viewport.setCursor(c); }

    private void handleMoved(MouseEvent e) {
        if (isDrawingActive != null && isDrawingActive.getAsBoolean()) return;
        if (mode != EditMode.IDLE) return;
        Node hitNode = pickNode(e);
        Axis axis = axisDrag.axisForNode(hitNode);
        if (axis == null) axis = axisDrag.findAxisNearRay(e);
        if (axis != null) {
            axisDrag.onHover(hitNode);
            setCursor(axis == Axis.X ? Cursor.H_RESIZE : axis == Axis.Y ? Cursor.CROSSHAIR : Cursor.V_RESIZE);
            return;
        }
        axisDrag.onHover(null);
        shape_item_ui_main sel = editor.getSelectedShape();
        Point3D gHit = groundRaycaster.apply(e);
        if (sel != null && (sel.findHandleByNode(hitNode) >= 0
                || (gHit != null && sel.findHandleNear(gHit, 8.0) >= 0))) {
            setCursor(Cursor.CROSSHAIR); return;
        }
        if (editor.findShapeByNode(hitNode) != null || (gHit != null && editor.findShapeNear(gHit, 6.0) != null)) {
            setCursor(Cursor.MOVE); return;
        }
        setCursor(Cursor.DEFAULT);
    }

    private void handlePressed(MouseEvent e) {
        if (isDrawingActive != null && isDrawingActive.getAsBoolean()) return;
        viewport.requestFocus();
        if (subScene != null) subScene.requestFocus();
        Node hitNode = pickNode(e);
        Point3D hit = groundRaycaster.apply(e);
        shape_item_ui_main sel = editor.getSelectedShape();

        // Right-click or Shift/Alt drag on shape directly rotates 360°
        if (e.getButton() == MouseButton.SECONDARY || e.isShiftDown() || e.isAltDown()) {
            shape_item_ui_main rShape = editor.findShapeByNode(hitNode);
            if (rShape == null && hit != null) rShape = editor.findShapeNear(hit, 8.0);
            if (rShape != null) {
                editor.recordSnapshot(); editor.selectShape(rShape);
                mode = EditMode.ROTATE_SHAPE; activeHandleIdx = -1;
                startDragX = e.getX(); startAngle = rShape.getRotationAngle();
                cameraController.setEnabled(false); e.consume(); return;
            }
        }
        if (e.getButton() != MouseButton.PRIMARY) return;
        editor.recordSnapshot();

        if (axisDrag.onAxisPressed(hitNode, e)) {
            mode = EditMode.AXIS_DRAG; cameraController.setEnabled(false);
            hudLabel.setVisible(true); e.consume(); return;
        }

        if (sel != null) {
            int hIdx = sel.findHandleByNode(hitNode);
            if (hIdx < 0 && hit != null) hIdx = sel.findHandleNear(hit, 8.0);
            if (hIdx >= 0) {
                mode = sel.isRotationHandle(hIdx) ? EditMode.ROTATE_SHAPE : EditMode.RESHAPE_HANDLE;
                activeHandleIdx = hIdx; startDragX = e.getX(); startAngle = sel.getRotationAngle();
                cameraController.setEnabled(false); e.consume(); return;
            }
        }

        shape_item_ui_main hitShape = editor.findShapeByNode(hitNode);
        if (hitShape == null && hit != null) hitShape = editor.findShapeNear(hit, 8.0);
        if (hitShape != null) {
            editor.selectShape(hitShape);
            if (e.getClickCount() == 2) {
                editor.openDimensionEditor(hitShape); e.consume(); return;
            }
            mode = EditMode.MOVE_SHAPE; lastHit = hit;
            cameraController.setEnabled(false); e.consume();
        } else if (sel != null) {
            editor.selectShape(null); cameraController.setEnabled(true);
        }
    }

    private void handleDragged(MouseEvent e) {
        if (mode == EditMode.IDLE) return;
        if (mode == EditMode.AXIS_DRAG) {
            if (axisDrag.onDrag(e)) {
                shape_item_ui_main sel = editor.getSelectedShape();
                if (sel != null) hudLabel.setText(sel.formatDimensions());
                hudLabel.setVisible(true);
            }
            e.consume(); return;
        }
        shape_item_ui_main sel = editor.getSelectedShape();
        Point3D hit = groundRaycaster.apply(e);
        if (sel == null) return;
        if (mode == EditMode.ROTATE_SHAPE) {
            double angle = (activeHandleIdx >= 0 && hit != null)
                ? shape_rotation_helper_ui_main.calculateAngle(sel.getWorldCenter(), hit)
                : shape_rotation_helper_ui_main.normalize360(startAngle + (e.getX() - startDragX) * 0.8);
            sel.setRotationAngle(angle);
            axisDrag.updateGizmoPosition();
            hudLabel.setText(String.format("%s | Rotation: %.1f° (360°)", sel.getName(), sel.getRotationAngle()));
            hudLabel.setVisible(true);
            e.consume(); return;
        }
        if (hit == null) return;
        if (mode == EditMode.RESHAPE_HANDLE) {
            sel.moveHandle(activeHandleIdx, hit);
            axisDrag.updateGizmoPosition();
            hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true);
        } else if (mode == EditMode.MOVE_SHAPE && lastHit != null) {
            sel.translate(hit.getX() - lastHit.getX(), hit.getZ() - lastHit.getZ());
            axisDrag.updateGizmoPosition();
            hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true);
            lastHit = hit;
        }
        e.consume();
    }

    private void handleReleased(MouseEvent e) {
        boolean wasBusy = (mode != EditMode.IDLE);
        if (mode == EditMode.AXIS_DRAG) axisDrag.onReleased();
        if (wasBusy) cameraController.setEnabled(true);
        mode = EditMode.IDLE; activeHandleIdx = -1; lastHit = null;
        hudLabel.setVisible(false);
        if (wasBusy) e.consume();
    }

    private void handleKeyPressed(KeyEvent e) {
        if (editor.getSelectedShape() == null) return;
        if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) { editor.deleteSelected(); e.consume(); }
        else if (e.getCode() == KeyCode.ESCAPE) {
            if (axisDrag.isDragging()) axisDrag.onReleased();
            editor.selectShape(null); e.consume();
        }
    }
    private static Node pickNode(MouseEvent e) { return (e.getPickResult() != null) ? e.getPickResult().getIntersectedNode() : null; }
}
