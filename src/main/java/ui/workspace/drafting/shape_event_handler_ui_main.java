package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.drafting.gizmo.translation_gizmo_ui_main.Axis;
import ui.workspace.drafting.gizmo.axis_drag_controller_ui_main;
import ui.workspace.drafting.gizmo.world_raycaster_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;
import ui.workspace.drafting.gizmo.viewport_coordinate_helper_ui_main;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class shape_event_handler_ui_main {

    private enum EditMode { IDLE, AXIS_DRAG, MOVE_SHAPE, RESHAPE_HANDLE, ROTATE_SHAPE }

    private final shape_editor_ui_main editor;
    private final Pane viewport; private final SubScene subScene;
    private final camera_controller_ui_main cameraController; private final Function<MouseEvent, Point3D> groundRaycaster;
    private final Label hudLabel; private final BooleanSupplier isDrawingActive;
    private final axis_drag_controller_ui_main axisDrag; private final PerspectiveCamera camera; private final Group container;

    private EditMode mode = EditMode.IDLE; private int activeHandleIdx = -1; private Point3D startHit = null;
    private double startDragVx = 0, startDragVy = 0, startWorldX = 0, startWorldY = 0, startWorldZ = 0, dragPlaneY = 0;
    private double startAngleX = 0, startAngleY = 0; private boolean hasMoved = false, wasShift = false;

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
    private double getVx(MouseEvent e) { return viewport_coordinate_helper_ui_main.getViewportX(e, viewport); }
    private double getVy(MouseEvent e) { return viewport_coordinate_helper_ui_main.getViewportY(e, viewport); }

    private Point3D raycastPlane(MouseEvent e, double yPlane) {
        if (camera != null && container != null) {
            Point3D hit = world_raycaster_ui_main.hitPlaneY(world_raycaster_ui_main.buildRay(e, viewport, camera, container), yPlane);
            if (hit != null) return hit;
        }
        return groundRaycaster.apply(e);
    }

    private void startMode(EditMode m, int hIdx, MouseEvent e) {
        mode = m; activeHandleIdx = hIdx; startDragVx = getVx(e); startDragVy = getVy(e);
        hasMoved = false; wasShift = e.isShiftDown(); cameraController.setEnabled(false); e.consume();
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
                editor.selectShape(rShape); startAngleX = rShape.getRotationX(); startAngleY = rShape.getRotationY();
                startMode(EditMode.ROTATE_SHAPE, -1, e); return;
            }
        }
        if (e.getButton() != MouseButton.PRIMARY) return;
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
                    face_reference_ui_main face = face_picker_ui_main.pickFace(world_raycaster_ui_main.buildRay(e, viewport, camera, container), hitShape);
                    editor.setActiveFace(face); if (face != null) editor.checkHoleHit(face, hitShape);
                } else editor.setActiveFace(null);
            }
            if (e.getClickCount() == 2) {
                if (editor.getSelectedExtrude() != null) editor.openExtrudeEditor(hitShape, editor.getSelectedExtrude());
                else if (editor.getSelectedHole() != null) editor.openHoleEditor(hitShape, editor.getSelectedHole());
                else editor.openDimensionEditor(hitShape);
                e.consume(); return;
            }
            startWorldX = hitShape.getWorldX(); startWorldY = hitShape.getWorldY(); startWorldZ = hitShape.getWorldZ();
            dragPlaneY = hitShape.getWorldCenter().getY(); startHit = raycastPlane(e, dragPlaneY);
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

        if (mode == EditMode.AXIS_DRAG) {
            if (axisDrag.onDrag(e)) {
                if (!hasMoved) { editor.recordSnapshot(); hasMoved = true; }
                hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true);
            }
            e.consume(); return;
        }
        if (mode == EditMode.ROTATE_SHAPE) {
            double nx = shape_rotation_helper_ui_main.normalize360(startAngleX - (vy - startDragVy) * 0.8);
            double ny = shape_rotation_helper_ui_main.normalize360(startAngleY + (vx - startDragVx) * 0.8);
            if (!hasMoved && (Math.abs(nx - startAngleX) > 0.05 || Math.abs(ny - startAngleY) > 0.05)) { editor.recordSnapshot(); hasMoved = true; }
            sel.setRotation(nx, ny); axisDrag.updateGizmoPosition(); editor.updateActiveFace();
            hudLabel.setText(String.format("%s | Rot Y: %.1f° | Rot X: %.1f°", sel.getName(), sel.getRotationY(), sel.getRotationX()));
            hudLabel.setVisible(true); e.consume(); return;
        }
        if (mode == EditMode.RESHAPE_HANDLE) {
            Point3D hit = raycastPlane(e, sel.getWorldCenter().getY());
            if (hit != null) {
                if (!hasMoved) { editor.recordSnapshot(); hasMoved = true; }
                sel.moveHandle(activeHandleIdx, hit); axisDrag.updateGizmoPosition(); editor.updateActiveFace(); hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true);
            }
            e.consume(); return;
        }
        if (mode == EditMode.MOVE_SHAPE) {
            if (!hasMoved && Math.hypot(vx - startDragVx, vy - startDragVy) >= 1.0) { editor.recordSnapshot(); hasMoved = true; }
            if (e.isShiftDown()) {
                if (!wasShift) { startWorldX = sel.getWorldX(); startWorldY = sel.getWorldY(); startWorldZ = sel.getWorldZ(); startDragVx = vx; startDragVy = vy; wasShift = true; }
                sel.setWorldTranslation(startWorldX, startWorldY + (vy - startDragVy) * 0.8, startWorldZ);
            } else {
                if (wasShift) { startWorldX = sel.getWorldX(); startWorldY = sel.getWorldY(); startWorldZ = sel.getWorldZ(); startHit = raycastPlane(e, dragPlaneY); startDragVx = vx; startDragVy = vy; wasShift = false; }
                Point3D hit = raycastPlane(e, dragPlaneY);
                if (hit != null && startHit != null) sel.setWorldTranslation(startWorldX + (hit.getX() - startHit.getX()), startWorldY, startWorldZ + (hit.getZ() - startHit.getZ()));
                else {
                    double yr = Math.toRadians(cameraController.getRy().getAngle()), cy = Math.cos(yr), sy = Math.sin(yr);
                    sel.setWorldTranslation(startWorldX + ((vx - startDragVx) * 0.8 * cy - (vy - startDragVy) * 0.8 * sy), startWorldY, startWorldZ + (-(vx - startDragVx) * 0.8 * sy - (vy - startDragVy) * 0.8 * cy));
                }
            }
            axisDrag.updateGizmoPosition(); editor.updateActiveFace(); hudLabel.setText(sel.formatDimensions()); hudLabel.setVisible(true); e.consume();
        }
    }

    private void handleReleased(MouseEvent e) {
        boolean wasBusy = (mode != EditMode.IDLE);
        if (mode == EditMode.AXIS_DRAG) axisDrag.onReleased();
        if (wasBusy) { cameraController.setEnabled(true); if (hasMoved) editor.notifyShapesChanged(); }
        mode = EditMode.IDLE; activeHandleIdx = -1; startHit = null; hasMoved = false; hudLabel.setVisible(false); if (wasBusy) e.consume();
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
