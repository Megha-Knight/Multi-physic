package ui;

import ui.workspace.drafting.holes.hole_dialog_ui_main;
import ui.workspace.drafting.holes.linear_pattern_dialog_ui_main;
import ui.workspace.drafting.holes.circular_pattern_dialog_ui_main;

public final class ui_feature_action_helper_ui_main {
    private ui_feature_action_helper_ui_main() {}

    public static boolean isMachinableSolid(ui.workspace.drafting.shape_item_ui_main shape) {
        return shape != null && (shape.getType().is3D() || shape.hasValidExtrusions());
    }

    public static void handleHole(UI_Main app) {
        handleCutout(app, ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE);
    }

    public static void handleExtrude(UI_Main app) {
        var ed = app.getWorkspace3D().getShapeEditor();
        var s = ed.getSelectedShape();
        var sk = ed.getSelectedSketch();
        if (sk == null && s != null && s.hasSketches()) {
            sk = s.getSketches().get(s.getSketches().size() - 1);
        }
        if (sk != null) {
            if (!sk.isValid()) {
                app.getFooterBar().setStatusText("Sketch profile is open or invalid: cannot extrude.");
                return;
            }
            double height = 25.0;
            ed.recordSnapshot();
            String name = "Extrude (" + sk.getName() + ")";
            var cs = ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE;
            double dia = 20.0, w2 = 0.0;
            if (!sk.getEntities().isEmpty()) {
                var ent = sk.getEntities().get(0);
                if (ent instanceof ui.workspace.drafting.sketch.sketch_rect_ui_main r) {
                    cs = (Math.abs(r.getWidth() - r.getHeight()) < 1e-3) ? ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.SQUARE : ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RECTANGLE;
                    dia = r.getWidth(); w2 = r.getHeight();
                } else if (ent instanceof ui.workspace.drafting.sketch.sketch_circle_ui_main c) {
                    cs = ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE;
                    dia = c.getRadius() * 2.0;
                }
            }
            var ext = new ui.workspace.drafting.extrude.extrude_feature_ui_main(
                null, s != null ? s.getId() : sk.getOwnerShapeId(), name,
                sk.getFaceKind() != null ? sk.getFaceKind() : ui.workspace.drafting.faces.face_kind_ui_main.TOP,
                cs, 0.0, 0.0, dia, w2, height
            );
            ext.setSketchId(sk.getId());
            if (s != null) {
                s.addExtrude(ext);
                ed.notifyShapesChanged();
                ed.selectExtrude(s, ext);
            }
            app.getFooterBar().setStatusText("Extruded " + sk.getName() + " (Height: " + height + " mm)");
            return;
        }
        if (s != null && !s.getType().is3D()) {
            ed.recordSnapshot();
            double height = 25.0;
            var cs = switch (s.getType()) {
                case EQUILATERAL_TRIANGLE -> ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.EQUILATERAL_TRIANGLE;
                case RIGHT_TRIANGLE -> ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RIGHT_TRIANGLE;
                case RECTANGLE -> ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RECTANGLE;
                case SQUARE -> ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.SQUARE;
                default -> ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE;
            };
            double w = Math.max(0.2, Math.abs(s.getP2().getX() - s.getP1().getX())), d = Math.max(0.2, Math.abs(s.getP2().getZ() - s.getP1().getZ()));
            double dia = (cs == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE) ? s.getP1().distance(s.getP2()) : Math.max(w, d);
            String name = "Extrude (" + s.getName() + ")";
            var ext = new ui.workspace.drafting.extrude.extrude_feature_ui_main(
                null, s.getId(), name, s.getFaceKind() != null ? s.getFaceKind() : ui.workspace.drafting.faces.face_kind_ui_main.TOP,
                cs, 0.0, 0.0, dia, Math.min(w, d), height
            );
            s.addExtrude(ext);
            ed.notifyShapesChanged();
            ed.selectExtrude(s, ext);
            app.getFooterBar().setStatusText("Extruded " + s.getName() + " (Height: " + height + " mm)");
            return;
        }
        var f = ed.getActiveFace();
        if (s != null && f != null && f.isPlanar()) {
            ui.workspace.drafting.extrude.extrude_dialog_ui_main.open(s, f, ed, app.getScene() != null ? app.getScene().getWindow() : null);
        } else {
            app.getFooterBar().setStatusText(s == null ? "Select a sketch or 3D shape first." : (f == null ? "Select a planar face on the shape to extrude." : "Face is not planar."));
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

    public static void handleFillet(UI_Main app) { enterEdgePick(app, ui.workspace.drafting.machining.machining_dialog_ui_main.Kind.FILLET); }
    public static void handleChamfer(UI_Main app) { enterEdgePick(app, ui.workspace.drafting.machining.machining_dialog_ui_main.Kind.CHAMFER); }
    public static void handleDraft(UI_Main app) { openMachining(app, ui.workspace.drafting.machining.machining_dialog_ui_main.Kind.DRAFT); }

    private static void enterEdgePick(UI_Main app, ui.workspace.drafting.machining.machining_dialog_ui_main.Kind kind) {
        var ed = app.getWorkspace3D().getShapeEditor();
        var s = ed.getSelectedShape();
        if (!isMachinableSolid(s)) {
            app.getFooterBar().setStatusText("Select a 3D solid (cube/cuboid/cylinder/cone) first, then click " + (kind == ui.workspace.drafting.machining.machining_dialog_ui_main.Kind.FILLET ? "Fillet" : "Chamfer") + ".");
            return;
        }
        ed.enterEdgePickMode(kind);
        app.getFooterBar().setStatusText("Edge Pick: Click any edge on " + s.getName() + " to " + kind.name().toLowerCase() + " (Esc to cancel)");
    }

    private static void openMachining(UI_Main app, ui.workspace.drafting.machining.machining_dialog_ui_main.Kind kind) {
        var ed = app.getWorkspace3D().getShapeEditor();
        var s = ed.getSelectedShape();
        if (!isMachinableSolid(s)) {
            app.getFooterBar().setStatusText("Select a 3D solid (cube/cuboid/cylinder/cone) first, then use Fillet, Chamfer, or Draft.");
            return;
        }
        var w = app.getScene() != null ? app.getScene().getWindow() : null;
        var f = ed.getActiveFace();
        if (kind == ui.workspace.drafting.machining.machining_dialog_ui_main.Kind.DRAFT && f != null && f.getFaceKind() != null) {
            ui.workspace.drafting.machining.machining_dialog_ui_main.openCreateWithTarget(kind, s, f.getFaceKind().name(), ed, w);
        } else {
            ui.workspace.drafting.machining.machining_dialog_ui_main.openCreate(kind, s, ed, w);
        }
    }
}
