package ui.workspace.drafting;

import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.Group;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.gizmo.world_raycaster_ui_main;
import ui.workspace.drafting.topology.topology_derived_face_ui_main;

/**
 * shape_hover_handler_ui_main.java
 * Resolves per-face and per-hole-region hover state from mouse movement and world rays.
 */
public final class shape_hover_handler_ui_main {

    private shape_hover_handler_ui_main() {}

    public static void handleHover(MouseEvent e, Pane viewport, PerspectiveCamera camera, Group container,
                                   shape_editor_ui_main editor, Node hitNode) {
        if (editor == null) return;

        if (editor.isEdgePickMode()) {
            shape_item_ui_main sel = editor.getSelectedShape();
            if (sel == null) sel = editor.findShapeByNode(hitNode);
            if (sel != null && sel.getType().is3D() && camera != null && viewport != null && container != null) {
                double[] ray = world_raycaster_ui_main.buildRay(e, viewport, camera, container);
                editor.setHoveredEdge(ui.workspace.drafting.faces.edge_picker_ui_main.pickEdge(ray, sel));
            } else {
                editor.clearEdgeHover();
            }
            return;
        }
        editor.clearEdgeHover();

        // 1. Check if hovering a derived hole region MeshView
        if (hitNode != null && hitNode.getUserData() instanceof topology_derived_face_ui_main df) {
            shape_item_ui_main shape = editor.findShapeByNode(hitNode);
            editor.setHoveredDerivedFace(shape, df);
            editor.setHoveredFace(null);
            return;
        }

        // 2. Check if hovering a 3D base primitive shape
        shape_item_ui_main hitShape = editor.findShapeByNode(hitNode);
        if (hitShape != null && hitShape.getType().is3D() && camera != null && viewport != null && container != null) {
            double[] ray = world_raycaster_ui_main.buildRay(e, viewport, camera, container);
            face_reference_ui_main face = face_picker_ui_main.pickFace(ray, hitShape);
            if (face != null) {
                editor.setHoveredFace(face);
                editor.setHoveredDerivedFace(null, null);
                return;
            }
        }

        // 3. Clear hover if over empty space or non-face node
        editor.setHoveredFace(null);
        editor.setHoveredDerivedFace(null, null);
    }
}
