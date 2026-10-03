package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.drafting.shape_editor_ui_main;

public class sketch_controller_ui_main {

    public enum State { IDLE, LINE_END, CIRCLE_RADIUS, RECT_CORNER2, ARC_START, ARC_END, DRAGGING }

    private final sketch_mode_ui_main mode;
    private final sketch_view_ui_main view = new sketch_view_ui_main();
    private final sketch_preview_ui_main preview = new sketch_preview_ui_main();
    private final sketch_hud_overlay_ui_main hud;

    private sketch_tool_type_ui_main activeTool = sketch_tool_type_ui_main.SELECT;
    private State state = State.IDLE;
    private sketch_point_2d_ui_main startPt = null;
    private sketch_point_2d_ui_main arcCenter = null;
    private double arcRadius = 0.0, arcStartAngle = 0.0;
    private sketch_entity_ui_main selectedEntity = null;

    public sketch_controller_ui_main(sketch_mode_ui_main mode, Runnable onClose) {
        this.mode = mode;
        this.hud = new sketch_hud_overlay_ui_main(onClose);
        mode.setOnModeChanged(this::onModeUpdated);
    }

    public Group getView() { return view; } public Group getPreview() { return preview; }
    public sketch_hud_overlay_ui_main getHud() { return hud; } public State getState() { return state; }
    public sketch_tool_type_ui_main getActiveTool() { return activeTool; }
    public void setTool(sketch_tool_type_ui_main t) { this.activeTool = t; cancelActiveDrawing(); }
    public sketch_entity_ui_main getSelectedEntity() { return selectedEntity; }

    public void cancelActiveDrawing() {
        state = State.IDLE; startPt = null; arcCenter = null;
        preview.clearAll();
    }

    private void onModeUpdated() {
        if (mode.isActive()) {
            view.render(mode.getActiveSketch());
            hud.update(mode.getActiveSketch());
        } else {
            view.getChildren().clear();
            preview.clearAll();
            hud.setVisible(false);
            selectedEntity = null;
        }
    }

    public sketch_point_2d_ui_main getSnappedPoint(MouseEvent e, Pane viewport, PerspectiveCamera camera, Group group) {
        if (!mode.isActive() || mode.getActiveCoordSystem() == null) return null;
        sketch_point_2d_ui_main raw = sketch_canvas_mapper_ui_main.screenToSketch(e, viewport, camera, group, mode.getActiveCoordSystem());
        if (raw == null) return null;
        var snap = sketch_snap_system_ui_main.findBestSnap(raw, mode.getActiveSketch().getEntities(), 0.5);
        if (snap != null) {
            preview.showSnapIndicator(mode.getActiveCoordSystem().toWorldPoint(snap.snapPoint()), snap.type());
            return snap.snapPoint();
        }
        preview.clearSnap();
        return raw;
    }

    public void handleMouseMoved(MouseEvent e, Pane viewport, PerspectiveCamera camera, Group group) {
        if (!mode.isActive()) return;
        sketch_point_2d_ui_main p = getSnappedPoint(e, viewport, camera, group);
        if (p == null) return;
        sketch_coord_system_ui_main cs = mode.getActiveCoordSystem();
        switch (state) {
            case LINE_END -> preview.showLinePreview(cs.toWorldPoint(startPt), cs.toWorldPoint(p));
            case CIRCLE_RADIUS -> preview.showCirclePreview(startPt, startPt.distance(p), cs);
            case RECT_CORNER2 -> preview.showRectPreview(startPt, p, cs);
            case ARC_START -> preview.showCirclePreview(arcCenter, arcCenter.distance(p), cs);
            case ARC_END -> {
                double end = Math.toDegrees(Math.atan2(p.y() - arcCenter.y(), p.x() - arcCenter.x()));
                preview.showArcPreview(arcCenter, arcRadius, arcStartAngle, end < 0 ? end + 360.0 : end, cs);
            }
            default -> {}
        }
    }

    public boolean handleMousePressed(MouseEvent e, Pane viewport, PerspectiveCamera camera, Group group, shape_editor_ui_main editor) {
        if (!mode.isActive() || e.getButton() != MouseButton.PRIMARY) return false;
        sketch_point_2d_ui_main p = getSnappedPoint(e, viewport, camera, group);
        if (p == null) return false;
        sketch_feature_ui_main sk = mode.getActiveSketch();
        switch (activeTool) {
            case SELECT -> handleSelectClick(p, sk);
            case LINE -> handleLineClick(p, sk, editor);
            case CIRCLE -> handleCircleClick(p, sk, editor);
            case RECTANGLE -> handleRectClick(p, sk, editor);
            case ARC -> handleArcClick(p, sk, editor);
        }
        hud.update(sk);
        return true;
    }

    private void handleSelectClick(sketch_point_2d_ui_main p, sketch_feature_ui_main sk) {
        sketch_entity_ui_main near = sketch_hit_tester_ui_main.findNearestEntity(p, sk.getEntities(), 1.0);
        if (selectedEntity != null) selectedEntity.setSelected(false);
        selectedEntity = near;
        if (selectedEntity != null) selectedEntity.setSelected(true);
        view.render(sk);
    }

    private void handleLineClick(sketch_point_2d_ui_main p, sketch_feature_ui_main sk, shape_editor_ui_main editor) {
        if (state == State.IDLE) {
            startPt = p; state = State.LINE_END;
        } else if (state == State.LINE_END) {
            if (startPt.distance(p) > 0.05) {
                if (editor != null) editor.recordSnapshot();
                sk.addEntity(new sketch_line_ui_main("L-" + System.currentTimeMillis(), startPt.x(), startPt.y(), p.x(), p.y()));
                view.render(sk);
            }
            cancelActiveDrawing();
        }
    }

    private void handleCircleClick(sketch_point_2d_ui_main p, sketch_feature_ui_main sk, shape_editor_ui_main editor) {
        if (state == State.IDLE) {
            startPt = p; state = State.CIRCLE_RADIUS;
        } else if (state == State.CIRCLE_RADIUS) {
            double r = startPt.distance(p);
            if (r > 0.05) {
                if (editor != null) editor.recordSnapshot();
                sk.addEntity(new sketch_circle_ui_main("C-" + System.currentTimeMillis(), startPt.x(), startPt.y(), r));
                view.render(sk);
            }
            cancelActiveDrawing();
        }
    }

    private void handleRectClick(sketch_point_2d_ui_main p, sketch_feature_ui_main sk, shape_editor_ui_main editor) {
        if (state == State.IDLE) {
            startPt = p; state = State.RECT_CORNER2;
        } else if (state == State.RECT_CORNER2) {
            if (Math.abs(p.x() - startPt.x()) > 0.05 && Math.abs(p.y() - startPt.y()) > 0.05) {
                if (editor != null) editor.recordSnapshot();
                sk.addEntity(new sketch_rect_ui_main("R-" + System.currentTimeMillis(), startPt.x(), startPt.y(), p.x(), p.y()));
                view.render(sk);
            }
            cancelActiveDrawing();
        }
    }

    private void handleArcClick(sketch_point_2d_ui_main p, sketch_feature_ui_main sk, shape_editor_ui_main editor) {
        if (state == State.IDLE) {
            arcCenter = p; state = State.ARC_START;
        } else if (state == State.ARC_START) {
            arcRadius = arcCenter.distance(p);
            if (arcRadius > 0.05) {
                arcStartAngle = Math.toDegrees(Math.atan2(p.y() - arcCenter.y(), p.x() - arcCenter.x()));
                if (arcStartAngle < 0) arcStartAngle += 360.0;
                state = State.ARC_END;
            } else cancelActiveDrawing();
        } else if (state == State.ARC_END) {
            double end = Math.toDegrees(Math.atan2(p.y() - arcCenter.y(), p.x() - arcCenter.x()));
            if (end < 0) end += 360.0;
            if (Math.abs(end - arcStartAngle) > 0.1) {
                if (editor != null) editor.recordSnapshot();
                sk.addEntity(new sketch_arc_ui_main("A-" + System.currentTimeMillis(), arcCenter, arcRadius, arcStartAngle, end));
                view.render(sk);
            }
            cancelActiveDrawing();
        }
    }

    public boolean handleKeyPressed(KeyEvent e, shape_editor_ui_main editor) {
        if (!mode.isActive()) return false;
        if (e.getCode() == KeyCode.ESCAPE) {
            cancelActiveDrawing();
            if (selectedEntity != null) {
                selectedEntity.setSelected(false);
                selectedEntity = null;
                view.render(mode.getActiveSketch());
            }
            e.consume();
            return true;
        }
        if ((e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) && selectedEntity != null) {
            if (editor != null) editor.recordSnapshot();
            mode.getActiveSketch().removeEntity(selectedEntity.getId());
            selectedEntity = null;
            view.render(mode.getActiveSketch());
            hud.update(mode.getActiveSketch());
            e.consume();
            return true;
        }
        return false;
    }
}
