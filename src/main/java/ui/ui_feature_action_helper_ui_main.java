package ui;

import ui.workspace.drafting.holes.hole_dialog_ui_main;
import ui.workspace.drafting.holes.linear_pattern_dialog_ui_main;
import ui.workspace.drafting.holes.circular_pattern_dialog_ui_main;

public final class ui_feature_action_helper_ui_main {
    private ui_feature_action_helper_ui_main() {}

    public static void handleHole(UI_Main app) {
        handleCutout(app, ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE);
    }

    public static void handleExtrude(UI_Main app) {
        var ed = app.getWorkspace3D().getShapeEditor();
        var s = ed.getSelectedShape();
        var f = ed.getActiveFace();
        if (s != null && f != null && f.isPlanar()) {
            ui.workspace.drafting.extrude.extrude_dialog_ui_main.open(s, f, ed, app.getScene() != null ? app.getScene().getWindow() : null);
        } else {
            app.getFooterBar().setStatusText(s == null ? "Select a 3D shape first." : (f == null ? "Select a planar face on the shape to extrude." : "Face is not planar."));
        }
    }

    public static void applyExtrudeConfig(UI_Main app, ui.ribbonbar.extrude_ribbon_panel_ui_main.ExtrudeConfig cfg) {
        var ed = app.getWorkspace3D().getShapeEditor();
        var s = ed.getSelectedShape();
        var f = ed.getActiveFace();
        if (s != null && f != null && f.isPlanar() && cfg != null) {
            ed.recordSnapshot();
            String name = "Boss " + (s.getExtrusions().size() + 1);
            var ext = new ui.workspace.drafting.extrude.extrude_feature_ui_main(
                null, s.getId(), name, f.getFaceKind(), cfg.profile(), 0.0, 0.0, cfg.diameter(), cfg.width2(), cfg.height()
            );
            s.addExtrude(ext);
            ed.notifyShapesChanged();
            ed.selectExtrude(s, ext);
            app.getFooterBar().setStatusText("Extruded " + name + " on " + f.getFaceKind().getLabel());
        } else {
            app.getFooterBar().setStatusText(s == null ? "Select a 3D shape first." : (f == null ? "Select a planar face on the shape to extrude." : "Face is not planar."));
        }
    }

    public static void handleCutout(UI_Main app, ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape shape) {
        var ed = app.getWorkspace3D().getShapeEditor();
        var s = ed.getSelectedShape();
        var f = ed.getActiveFace();
        if (s != null && f != null && f.isPlanar()) {
            hole_dialog_ui_main.open(s, f, ed, app.getScene() != null ? app.getScene().getWindow() : null, shape);
        } else {
            app.getFooterBar().setStatusText(s == null ? "Select a 3D shape first." : (f == null ? "Select a planar face on the shape to place a cutout." : "Face is not planar."));
        }
    }

    public static void handleLinearPattern(UI_Main app) {
        var ed = app.getWorkspace3D().getShapeEditor();
        var s = ed.getSelectedShape();
        if (s != null && s.hasHoles()) {
            linear_pattern_dialog_ui_main.open(s, ed, app.getScene() != null ? app.getScene().getWindow() : null);
        } else {
            app.getFooterBar().setStatusText(s == null ? "Select a shape first." : "Selected shape has no seed hole for pattern.");
        }
    }

    public static void handleCircularPattern(UI_Main app) {
        var ed = app.getWorkspace3D().getShapeEditor();
        var s = ed.getSelectedShape();
        if (s != null && s.hasHoles()) {
            circular_pattern_dialog_ui_main.open(s, ed, app.getScene() != null ? app.getScene().getWindow() : null);
        } else {
            app.getFooterBar().setStatusText(s == null ? "Select a shape first." : "Selected shape has no seed hole for pattern.");
        }
    }
}
