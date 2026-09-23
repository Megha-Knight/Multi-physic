package ui.workspace;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.function.BooleanSupplier;

/**
 * shapeeventhandler_ui_main.java
 * Mouse and keyboard event controller for interactive 2D and 3D CAD shape editing and translation.
 */
public class shapeeventhandler_ui_main {

    private enum EditMode { IDLE, MOVE_SHAPE, RESHAPE_HANDLE }

    private final shapeeditor_ui_main editor;
    private final Pane viewport;
    private final SubScene subScene;
    private final cameracontroller_ui_main cameraController;
    private final shapedrafting_ui_main drafter;
    private final Label hudLabel;
    private final BooleanSupplier isDrawingActive;

    private EditMode mode = EditMode.IDLE;
    private int activeHandleIdx = -1;
    private Point3D lastHit = null;

    public shapeeventhandler_ui_main(shapeeditor_ui_main editor, Pane viewport, SubScene subScene,
                                     cameracontroller_ui_main camCtrl,
                                     shapedrafting_ui_main drafter,
                                     Label hudLabel, BooleanSupplier isDrawingActive) {
        this.editor = editor;
        this.viewport = viewport;
        this.subScene = subScene;
        this.cameraController = camCtrl;
        this.drafter = drafter;
        this.hudLabel = hudLabel;
        this.isDrawingActive = isDrawingActive;
        viewport.setFocusTraversable(true);
        if (subScene != null) subScene.setFocusTraversable(true);
        attach();
    }

    private void attach() {
        viewport.addEventFilter(MouseEvent.MOUSE_MOVED, this::handleMoved);
        viewport.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handlePressed);
        viewport.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleDragged);
        viewport.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleReleased);
        viewport.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        if (subScene != null) subScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    private void setCursor(Cursor c) {
        if (subScene != null) subScene.setCursor(c);
        viewport.setCursor(c);
    }

    private Point2D getViewportCoords(MouseEvent e) {
        return viewport.sceneToLocal(e.getSceneX(), e.getSceneY());
    }

    private void handleMoved(MouseEvent e) {
        if (isDrawingActive != null && isDrawingActive.getAsBoolean()) return;
        if (mode != EditMode.IDLE) return;

        Point2D loc = getViewportCoords(e);
        double sx = loc.getX(), sy = loc.getY();
        Point3D groundHit = drafter.screenToGround(sx, sy);
        Point3D[] ray = drafter.screenToRay(sx, sy);
        Point3D o = (ray != null) ? ray[0] : null, d = (ray != null) ? ray[1] : null;

        shapeitem_ui_main sel = editor.getSelectedShape();
        if (sel != null && editor.findHandle(sel, o, d, groundHit) >= 0) {
            setCursor(Cursor.CROSSHAIR);
            return;
        }
        if (editor.findShape(o, d, groundHit) != null) {
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

        Point2D loc = getViewportCoords(e);
        double sx = loc.getX(), sy = loc.getY();
        Point3D groundHit = drafter.screenToGround(sx, sy);
        Point3D[] ray = drafter.screenToRay(sx, sy);
        Point3D o = (ray != null) ? ray[0] : null, d = (ray != null) ? ray[1] : null;

        shapeitem_ui_main sel = editor.getSelectedShape();
        if (sel != null) {
            int hIdx = editor.findHandle(sel, o, d, groundHit);
            if (hIdx >= 0) {
                editor.recordSnapshot();
                mode = EditMode.RESHAPE_HANDLE;
                activeHandleIdx = hIdx;
                cameraController.setEnabled(false);
                e.consume();
                return;
            }
        }

        shapeitem_ui_main hitShape = editor.findShape(o, d, groundHit);
        if (hitShape != null) {
            editor.recordSnapshot();
            editor.selectShape(hitShape);
            mode = EditMode.MOVE_SHAPE;
            lastHit = (groundHit != null) ? groundHit : new Point3D(sx, 0, sy);
            cameraController.setEnabled(false);
            e.consume();
        } else if (sel != null) {
            editor.selectShape(null);
            cameraController.setEnabled(true);
        }
    }

    private void handleDragged(MouseEvent e) {
        if (mode == EditMode.IDLE || editor.getSelectedShape() == null) return;
        Point2D loc = getViewportCoords(e);
        Point3D groundHit = drafter.screenToGround(loc.getX(), loc.getY());
        if (groundHit == null) return;

        shapeitem_ui_main sel = editor.getSelectedShape();
        if (mode == EditMode.RESHAPE_HANDLE) {
            sel.moveHandle(activeHandleIdx, groundHit);
            hudLabel.setText(sel.formatDimensions());
            hudLabel.setVisible(true);
            e.consume();
        } else if (mode == EditMode.MOVE_SHAPE && lastHit != null) {
            double dx = groundHit.getX() - lastHit.getX(), dz = groundHit.getZ() - lastHit.getZ();
            if (Math.abs(dx) > 0.001 || Math.abs(dz) > 0.001) {
                sel.translate(dx, dz);
                lastHit = groundHit;
                hudLabel.setText(String.format("Moved %s | X: %.1f mm, Z: %.1f mm",
                        sel.getType().getLabel(), groundHit.getX(), groundHit.getZ()));
                hudLabel.setVisible(true);
            }
            e.consume();
        }
    }

    private void handleReleased(MouseEvent e) {
        if (mode != EditMode.IDLE) {
            mode = EditMode.IDLE;
            activeHandleIdx = -1;
            lastHit = null;
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
