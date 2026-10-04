package ui.workspace.drafting.booleans;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.framework_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

/**
 * boolean_mesh_builder_ui_main.java
 * Constructs 3D JavaFX nodes from evaluated Boolean solid CSG geometry.
 */
public final class boolean_mesh_builder_ui_main {

    private boolean_mesh_builder_ui_main() {}

    public static Node buildBooleanNode(shape_item_ui_main target, shape_item_ui_main tool,
                                        boolean_feature_ui_main feature, boolean isSelected) {
        if (target == null || tool == null || feature == null || !feature.isValid()) return null;

        TriangleMesh mesh = boolean_solid_evaluator_ui_main.evaluate(target, tool, feature.getOpType());
        if (mesh == null || mesh.getPoints().size() == 0) return null;

        MeshView mv = new MeshView(mesh);
        Color c = isSelected ? Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)
                : Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR);
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.web(isSelected ? "#BAE6FD" : "#5A6B7C"));
        mat.setSpecularPower(48.0);
        mv.setMaterial(mat);
        return mv;
    }
}
