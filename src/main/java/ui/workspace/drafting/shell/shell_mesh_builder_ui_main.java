package ui.workspace.drafting.shell;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.framework_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

/**
 * shell_mesh_builder_ui_main.java
 * Constructs interactive 3D JavaFX MeshView nodes for parametric shell solid bodies.
 */
public final class shell_mesh_builder_ui_main {

    private shell_mesh_builder_ui_main() {}

    public static Node buildShellNode(shape_item_ui_main shape, shell_feature_ui_main shell, boolean isSelected) {
        if (shape == null || shell == null || !shell.isValid()) return null;

        TriangleMesh mesh = shell_solid_evaluator_ui_main.evaluate(shape, shell);
        if (mesh == null || mesh.getPoints().size() == 0) return null;

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        Color c = isSelected ? Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)
                : Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR);
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.web(isSelected ? "#BAE6FD" : "#5A6B7C"));
        mat.setSpecularPower(48.0);
        mv.setMaterial(mat);
        mv.setId(shell.getId());
        return mv;
    }
}
