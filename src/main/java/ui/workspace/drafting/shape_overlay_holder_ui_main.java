package ui.workspace.drafting;

import javafx.scene.Group;
import ui.workspace.drafting.faces.edge_overlay_ui_main;
import ui.workspace.drafting.faces.face_overlay_ui_main;
import ui.workspace.drafting.holes.hole_overlay_ui_main;

/**
 * shape_overlay_holder_ui_main.java
 * Encapsulates face, hole, and edge visual overlays inside shape_item_ui_main's rootGroup.
 * Ensures overlays reside in shape-local coordinates and automatically follow all transforms.
 */
public class shape_overlay_holder_ui_main {

    private final face_overlay_ui_main faceOverlay = new face_overlay_ui_main();
    private final hole_overlay_ui_main holeOverlay = new hole_overlay_ui_main();
    private final edge_overlay_ui_main edgeOverlay = new edge_overlay_ui_main();
    private final Group group = new Group();

    public shape_overlay_holder_ui_main() {
        group.setId("SHAPE_OVERLAYS_GROUP");
        group.getChildren().addAll(faceOverlay, holeOverlay, edgeOverlay);
        group.setMouseTransparent(true);
    }

    public face_overlay_ui_main getFaceOverlay() { return faceOverlay; }
    public hole_overlay_ui_main getHoleOverlay() { return holeOverlay; }
    public edge_overlay_ui_main getEdgeOverlay() { return edgeOverlay; }
    public Group getGroup() { return group; }

    public void clearAll() {
        faceOverlay.highlightFace(null, null);
        faceOverlay.clearHover();
        faceOverlay.highlightDerivedFace(null, null);
        holeOverlay.highlightHole(null, null);
        edgeOverlay.clearHighlight();
    }
}
