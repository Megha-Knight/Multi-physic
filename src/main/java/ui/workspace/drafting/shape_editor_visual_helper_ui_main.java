package ui.workspace.drafting;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import ui.workspace.drafting.topology.derived_region_visual_builder_ui_main;
import ui.workspace.drafting.topology.topology_derived_face_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

import java.util.List;

/**
 * shape_editor_visual_helper_ui_main.java
 * Updates materials for 3D shapes and derived faces during focus and hover transitions.
 */
public final class shape_editor_visual_helper_ui_main {

    private shape_editor_visual_helper_ui_main() {}

    public static void refreshViewportColors(List<shape_item_ui_main> shapes) {
        if (shapes == null) return;
        PhongMaterial baseMat = shape_geometry_3d_ui_main.createMaterial(false, false);
        for (shape_item_ui_main s : shapes) {
            if (s != null) updateShapeMaterials(s, baseMat);
        }
    }

    public static void updateShapeMaterials(shape_item_ui_main shape, PhongMaterial baseMat) {
        if (shape == null) return;
        updateGroupMaterials(shape.getShapeGroup(), baseMat, false);
    }

    private static void updateGroupMaterials(Group g, PhongMaterial baseMat, boolean inDerived) {
        if (g == null) return;
        boolean derived = inDerived || "DERIVED_REGIONS_GROUP".equals(g.getId());
        for (Node child : g.getChildren()) {
            if (child instanceof Group sub) {
                updateGroupMaterials(sub, baseMat, derived);
            } else if (derived && child instanceof MeshView mv && child.getUserData() instanceof topology_derived_face_ui_main df) {
                mv.setMaterial(derived_region_visual_builder_ui_main.createMaterialForDerivedFace(df));
            } else if (!derived && child instanceof Shape3D s3d) {
                s3d.setMaterial(baseMat);
            }
        }
    }

    public static void updateDerivedFaceVisual(shape_item_ui_main shape, topology_derived_face_ui_main df) {
        if (shape == null || df == null) return;
        for (Node n : shape.getShapeGroup().getChildren()) {
            if (n instanceof Group g && "DERIVED_REGIONS_GROUP".equals(g.getId())) {
                for (Node child : g.getChildren()) {
                    if (child instanceof MeshView mv && child.getUserData() == df) {
                        mv.setMaterial(derived_region_visual_builder_ui_main.createMaterialForDerivedFace(df));
                        return;
                    }
                }
            }
        }
    }
}
