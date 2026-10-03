package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.drafting.gizmo.translation_gizmo_ui_main.Axis;
import ui.workspace.drafting.gizmo.axis_drag_controller_ui_main;
import ui.workspace.drafting.gizmo.world_raycaster_ui_main; import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class shape_event_handler_ui_main {

    private enum EditMode { IDLE, AXIS_DRAG, MOVE_SHAPE, RESHAPE_HANDLE, ROTATE_SHAPE }

    private final shape_editor_ui_main editor;
    private final Pane viewport; private final SubScene subScene;
    private final camera_controller_ui_main cameraController;
    private final Function<MouseEvent, Point3D> groundRaycaster;
    private final Label hudLabel; private final BooleanSupplier isDrawingActive;
    private final axis_drag_controller_ui_main axisDrag;
    private final PerspectiveCamera camera; private final Group container;

    private EditMode mode = EditMode.IDLE;
    private int activeHandleIdx = -1;
    private Point3D lastHit = null;
    private double startDragX = 0, startDragY = 0, startAngleX = 0, startAngleY = 0;

    public shape_event_handler_ui_main(shape_editor_ui_main editor, Pane viewport, SubScene subScene,
                                       camera_controller_ui_main camCtrl, Function<MouseEvent, Point3D> raycaster,
                                       Label hudLabel, BooleanSupplier isDrawingActive, axis_drag_controller_ui_main axisDrag,
                                       PerspectiveCamera camera, Group container) {
        this.editor = editor; this.viewport = viewport; this.subScene = subScene;
        this.cameraController = camCtrl; this.groundRaycaster = raycaster; this.hudLabel = hudLabel;
        this.isDrawingActive = isDrawingActive; this.axisDrag = axisDrag; this.camera = camera; this.container = container;
        viewport.setFocusTraversable(true); if (subScene != null) subScene.setFocusTraversable(true);
        Node target = (subScene != null) ? subScene : viewport;
        target.addEventFilter(MouseEvent.MOUSE_MOVED, this::handleMoved); target.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handlePressed);
        target.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleDragged); target.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleReleased);
        target.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed); viewport.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    private void setCursor(Cursor c) { if (subScene != null) subScene.setCursor(c); viewport.setCursor(c); }

    private double getVx(MouseEvent e) { if (viewport != null) { var p = viewport.sceneToLocal(e.getSceneX(), e.getSceneY()); if (p != null) return p.getX(); } return e.getX(); }
    private double getVy(MouseEvent e) { if (viewport != null) { var p = viewport.sceneToLocal(e.getSceneX(), e.getSceneY()); if (p != null) return p.getY(); } return e.getY(); }

    private Point3D raycastPlane(MouseEvent e, double yPlane) {
        if (camera != null && container != null) {
            Point3D hit = world_raycaster_ui_main.hitPlaneY(world_raycaster_ui_main.buildRay(getVx(e), getVy(e), viewport, camera, container), yPlane);
            if (hit != null) return hit;
        }
        return groundRaycaster.apply(e);
    }

    private void startMode(EditMode m, int hIdx, MouseEvent e) {
        mode = m; activeHandleIdx = hIdx; startDragX = getVx(e); startDragY = getVy(e);
        cameraController.setEnabled(false); e.consume();
    }

    private void handleMoved(MouseEvent e) {
        if ((isDrawingActive != null && isDrawingActive.getAsBoolean()) || mode != EditMode.IDLE) return;
        Node hitNode = pickNode(e); Axis axis = axisDrag.axisForNode(hitNode);
        if (axis == null) axis = axisDrag.findAxisNearRay(e);
        axisDrag.onHover(axis);
        if (axis != null) { setCursor(axis == Axis.X ? Cursor.H_RESIZE : axis == Axis.Y ? Cursor.CROSSHAIR : Cursor.V_RESIZE); return; }
        shape_item_ui_main sel = editor.getSelectedShape();
        if (sel != null && sel.findHandleByNode(hitNode) >= 0) { setCursor(Cursor.CROSSHAIR); return; }
        Point3D gHit = groundRaycaster.apply(e);
        setCursor((editor.findShapeByNode(hitNode) != null || (gHit != null && editor.findShapeNear(gHit, 6.0) != null)) ? Cursor.MOVE : Cursor.DEFAULT);
    }

    private void handlePressed(MouseEvent e) {
        if (isDrawingActive != null && isDrawingActive.getAsBoolean()) return;
        viewport.requestFocus(); if (subScene != null) subScene.requestFocus();
        Node hitNode = pickNode(e); shape_item_ui_main sel = editor.getSelectedShape();

        if (e.getButton() == MouseButton.SECONDARY || e.isAltDown()) {
            shape_item_ui_main rShape = editor.findShapeByNode(hitNode);
            if (rShape == null) { Point3D hit = raycastPlane(e, 0); if (hit != null) rShape = editor.findShapeNear(hit, 8.0); }
            if (rShape != null) {
                editor.recordSnapshot(); editor.selectShape(rShape);
                startAngleX = rShape.getRotationX(); startAngleY = rShape.getRotationY();
                startMode(EditMode.ROTATE_SHAPE, -1, e); return;
            }
        }
        if (e.getButton() != MouseButton.PRIMARY) return;
        editor.recordSnapshot();
        if (sel != null && sel.getType().is3D() && axisDrag.onAxisPressed(hitNode, e)) { startMode(EditMode.AXIS_DRAG, -1, e); return; }
        int hIdx; if (sel != null && (hIdx = sel.findHandleByNode(hitNode)) >= 0) { startMode(EditMode.RESHAPE_HANDLE, hIdx, e); return; }
        shape_item_ui_main hitShape = editor.findShapeByNode(hitNode);
        if (hitShape == null && sel != null) { Point3D hitSel = raycastPlane(e, sel.getWorldCenter().getY()); if (hitSel != null && sel.isNear(hitSel, 8.0)) hitShape = sel; }
        if (hitShape == null) { Point3D hit = raycastPlane(e, 0); if (hit != null) hitShape = editor.findShapeNear(hit, 8.0); }
        if (hitShape != null) {
            editor.selectShape(hitShape);
            var hitExt = ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main.findExtrudeFromNode(hitNode);
            if (hitExt != null) editor.selectExtrude(hitShape, hitExt);
            else {
                editor.selectExtrude(hitShape, null);
                if (hitShape.getType().is3D()) {
                    face_reference_ui_main face = face_picker_ui_main.pickFace(world_raycaster_ui_main.buildRay(getVx(e), getVy(e), viewport, camera, container), hitShape);
                    editor.setActiveFace(face); if (face != null) editor.checkHoleHit(face, hitShape);
                } else editor.setActiveFace(null);
            }
            if (e.getClickCount() == 2) {
                if (editor.getSelectedExtrude() != null) editor.openExtrudeEditor(hitShape, editor.getSelectedExtrude());
                else if (editor.getSelectedHole() != null) editor.openHoleEditor(hitShape, editor.getSelectedHole());
                else editor.openDimensionEditor(hitShape);
                e.consume(); return;
            }
            lastHit = raycastPlane(e, hitShape.getWorldCenter().getY());
            startMode(EditMode.MOVE_SHAPE, -1, e); return;
        }

        if (sel != null) {
            Point3D hit = raycastPlane(e, sel.getWorldCenter().getY());
            if (hit != null && (hIdx = sel.findHandleNear(hit, 6.0)) >= 0) { startMode(EditMode.RESHAPE_HANDLE, hIdx, e); return; }
            editor.setActiveFace(null); editor.selectExtrude(null, null); editor.selectHole(null, null); editor.selectShape(null); cameraController.setEnabled(true);
        }
    }

    private void handleDragged(MouseEvent e) {
        if (mode == EditMode.IDLE) return;
        shape_item_ui_main sel = editor.getSelectedShape();
        if (sel == null) return;

        double vx = getVx(e), vy = getVy(e);
        if (mode == EditMode.AXIS_DRAG) { if (axisDrag.onDrag(e)) { hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true); } e.consume(); return; }
        if (mode == EditMode.ROTATE_SHAPE) {
            double nx = shape_rotation_helper_ui_main.normalize360(startAngleX - (vy - startDragY) * 0.8);
            double ny = shape_rotation_helper_ui_main.normalize360(startAngleY + (vx - startDragX) * 0.8);
            sel.setRotation(nx, ny); axisDrag.updateGizmoPosition(); editor.updateActiveFace();
            hudLabel.setText(String.format("%s | Rot Y: %.1f° | Rot X: %.1f°", sel.getName(), sel.getRotationY(), sel.getRotationX()));
            hudLabel.setVisible(true); e.consume(); return;
        }
        if (mode == EditMode.RESHAPE_HANDLE) {
            Point3D hit = raycastPlane(e, sel.getWorldCenter().getY());
            if (hit != null) { sel.moveHandle(activeHandleIdx, hit); axisDrag.updateGizmoPosition(); editor.updateActiveFace(); hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true); }
            e.consume(); return;
        }
        if (mode == EditMode.MOVE_SHAPE) {
            if (e.isShiftDown()) {
                sel.applyWorldDelta(0, (vy - startDragY) * 0.8, 0); startDragY = vy; lastHit = raycastPlane(e, sel.getWorldCenter().getY());
            } else {
                Point3D hit = raycastPlane(e, sel.getWorldCenter().getY());
                if (hit != null && lastHit != null) sel.translate(hit.getX() - lastHit.getX(), hit.getZ() - lastHit.getZ());
                else {
                    double yr = Math.toRadians(cameraController.getRy().getAngle()), cy = Math.cos(yr), sy = Math.sin(yr);
                    double mx = (vx - startDragX) * 0.8, my = (vy - startDragY) * 0.8;
                    sel.translate(mx * cy - my * sy, -mx * sy - my * cy); startDragX = vx; startDragY = vy;
                }
                lastHit = hit;
            }
            axisDrag.updateGizmoPosition(); editor.updateActiveFace(); hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true); e.consume();
        }
    }

    private void handleReleased(MouseEvent e) {
        boolean wasBusy = (mode != EditMode.IDLE);
        if (mode == EditMode.AXIS_DRAG) axisDrag.onReleased();
        if (wasBusy) { cameraController.setEnabled(true); editor.notifyShapesChanged(); }
        mode = EditMode.IDLE; activeHandleIdx = -1; lastHit = null; hudLabel.setVisible(false); if (wasBusy) e.consume();
    }

    private void handleKeyPressed(KeyEvent e) {
        shape_item_ui_main sel = editor.getSelectedShape();
        if (sel == null) return;
        if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) { editor.deleteSelected(); e.consume(); return; }
        if (e.getCode() == KeyCode.ESCAPE) { if (editor.getActiveFace() != null) editor.setActiveFace(null); else editor.selectShape(null); e.consume(); return; }
        double s = (e.isControlDown() || e.isAltDown()) ? 0.2 : 2.0;
        boolean moved = switch (e.getCode()) {
            case LEFT -> { sel.applyWorldDelta(-s, 0, 0); yield true; } case RIGHT -> { sel.applyWorldDelta(s, 0, 0); yield true; }
            case UP -> { sel.applyWorldDelta(0, e.isShiftDown() ? -s : 0, e.isShiftDown() ? 0 : -s); yield true; }
            case DOWN -> { sel.applyWorldDelta(0, e.isShiftDown() ? s : 0, e.isShiftDown() ? 0 : s); yield true; }
            case PAGE_UP -> { sel.applyWorldDelta(0, -s, 0); yield true; } case PAGE_DOWN -> { sel.applyWorldDelta(0, s, 0); yield true; }
            default -> false;
        };
        if (moved) { axisDrag.updateGizmoPosition(); editor.updateActiveFace(); hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true); editor.notifyShapesChanged(); e.consume(); }
    }
    private static Node pickNode(MouseEvent e) { return (e.getPickResult() != null) ? e.getPickResult().getIntersectedNode() : null; }
}
