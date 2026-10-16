package ui.workspace.drafting;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.framework_ui_main;
import ui.workspace.drafting.faces.edge_overlay_ui_main;
import ui.workspace.drafting.faces.face_overlay_ui_main;
import ui.workspace.drafting.faces.viewport_color_state_ui_main;
import ui.workspace.drafting.holes.hole_overlay_ui_main;

/**
 * shape_overlay_holder_ui_main.java
 * Encapsulates face, hole, edge, and custom imported mesh visuals inside shape_item_ui_main.
 * Ensures all visual elements reside in shape-local coordinates and follow all transforms.
 */
public class shape_overlay_holder_ui_main {

    private final face_overlay_ui_main faceOverlay = new face_overlay_ui_main();
    private final hole_overlay_ui_main holeOverlay = new hole_overlay_ui_main();
    private final edge_overlay_ui_main edgeOverlay = new edge_overlay_ui_main();
    private final MeshView customMeshView = new MeshView();
    private final Group group = new Group();
    private TriangleMesh customMesh = null;

    public shape_overlay_holder_ui_main() {
        group.setId("SHAPE_OVERLAYS_GROUP");
        group.getChildren().addAll(faceOverlay, holeOverlay, edgeOverlay, customMeshView);
        group.setMouseTransparent(true);
    }

    public face_overlay_ui_main getFaceOverlay() { return faceOverlay; }
    public hole_overlay_ui_main getHoleOverlay() { return holeOverlay; }
    public edge_overlay_ui_main getEdgeOverlay() { return edgeOverlay; }
    public MeshView getCustomMeshView() { return customMeshView; }
    public Group getGroup() { return group; }

    public void setCustomMesh(TriangleMesh mesh) {
        this.customMesh = mesh;
        customMeshView.setMesh(mesh);
        updateCustomMeshMaterial(false);
    }

    public TriangleMesh getCustomMesh() { return customMesh; }
    public boolean hasCustomMesh() { return customMesh != null; }

    public void updateCustomMeshMaterial(boolean selected) {
        if (customMesh == null) return;
        boolean focused = viewport_color_state_ui_main.isViewportFocused();
        Color c = selected ? Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)
                           : Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR);
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.web(focused ? "#BAE6FD" : "#5A6B7C"));
        mat.setSpecularPower(48.0);
        customMeshView.setMaterial(mat);
    }

    public void clearAll() {
        faceOverlay.highlightFace(null, null);
        faceOverlay.clearHover();
        faceOverlay.highlightDerivedFace(null, null);
        holeOverlay.highlightHole(null, null);
        edgeOverlay.clearHighlight();
    }
}
