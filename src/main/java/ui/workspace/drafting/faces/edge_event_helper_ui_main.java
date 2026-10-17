package ui.workspace.drafting.faces;

import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.Group;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.gizmo.world_raycaster_ui_main;
import ui.workspace.drafting.machining.machining_dialog_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;

public final class edge_event_helper_ui_main {

    private edge_event_helper_ui_main() {}

    public static boolean handlePress(shape_editor_ui_main editor, MouseEvent e, Node hitNode,
                                      Pane viewport, PerspectiveCamera camera, Group container) {
        if (editor == null || !editor.isEdgePickMode()) return false;
        if (e.getButton() != MouseButton.PRIMARY) return false;

        shape_item_ui_main sel = editor.getSelectedShape();
        if (sel == null || camera == null || viewport == null || container == null) return false;

        double[] ray = world_raycaster_ui_main.buildRay(e, viewport, camera, container);
        topology_edge_ui_main edge = edge_picker_ui_main.pickEdge(ray, sel);
        if (edge != null) {
            machining_dialog_ui_main.Kind kind = editor.getEdgePickKind();
            editor.exitEdgePickMode();
            var window = (container.getScene() != null) ? container.getScene().getWindow() : null;
            machining_dialog_ui_main.openCreateWithTarget(kind, sel, edge.getName(), editor, window);
            e.consume();
            return true;
        }

        shape_item_ui_main other = editor.findShapeByNode(hitNode);
        if (other != null && other != sel && other.getType().is3D()) {
            editor.selectShape(other);
            e.consume();
            return true;
        }

        editor.postStatus("Edge not detected near click. Click directly on an edge or press Esc to cancel.");
        e.consume();
        return true;
    }
}
