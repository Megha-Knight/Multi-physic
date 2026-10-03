package ui;

import javafx.scene.control.ChoiceDialog;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sketch.sketch_mode_ui_main;
import ui.workspace.drafting.sketch.sketch_plane_type_ui_main;

import java.util.List;

/**
 * ui_sketch_action_helper_ui_main.java
 * High-level dispatch for initiating, configuring, and closing Interactive Sketch Mode.
 */
public final class ui_sketch_action_helper_ui_main {

    private ui_sketch_action_helper_ui_main() {}

    public static void handleCreateOrEnterSketch(UI_Main app, sketch_mode_ui_main sketchMode) {
        if (app == null || sketchMode == null) return;
        var ws = app.getWorkspace3D();
        shape_editor_ui_main ed = ws.getShapeEditor();
        shape_item_ui_main selShape = ed.getSelectedShape();
        face_reference_ui_main activeFace = ed.getActiveFace();

        if (selShape != null && activeFace != null) {
            if (!activeFace.isPlanar()) {
                app.getFooterBar().setStatusText("Cannot sketch on curved face: " + activeFace.getFaceKind().getLabel());
                return;
            }
            sketchMode.enter(selShape, activeFace, ws.getCameraController());
            return;
        }

        if (selShape != null && activeFace == null) {
            app.getFooterBar().setStatusText("Please click a planar face on " + selShape.getName() + " to attach sketch.");
            return;
        }

        // No shape selected: prompt base reference plane (XY, XZ, YZ)
        sketch_plane_type_ui_main chosen = promptBasePlane(app);
        if (chosen != null) {
            sketchMode.enterBasePlane(null, chosen, ws.getCameraController());
        }
    }

    private static sketch_plane_type_ui_main promptBasePlane(UI_Main app) {
        ChoiceDialog<sketch_plane_type_ui_main> dlg = new ChoiceDialog<>(
            sketch_plane_type_ui_main.BASE_XZ,
            List.of(sketch_plane_type_ui_main.BASE_XY, sketch_plane_type_ui_main.BASE_XZ, sketch_plane_type_ui_main.BASE_YZ)
        );
        dlg.setTitle("New Sketch Reference Plane");
        dlg.setHeaderText("No planar face selected.");
        dlg.setContentText("Select base coordinate reference plane:");
        if (app.getScene() != null && app.getScene().getWindow() != null) {
            dlg.initOwner(app.getScene().getWindow());
        }
        return dlg.showAndWait().orElse(null);
    }
}
