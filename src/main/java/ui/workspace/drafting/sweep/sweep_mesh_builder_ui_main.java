package ui.workspace.drafting.sweep;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.framework_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

/**
 * sweep_mesh_builder_ui_main.java
 * Constructs JavaFX 3D MeshViews for parametric Sweep features.
 */
public final class sweep_mesh_builder_ui_main {

    private sweep_mesh_builder_ui_main() {}

    public static Node buildSweepNode(shape_item_ui_main host, sweep_feature_ui_main sweep, boolean selected) {
        if (host == null || sweep == null || !sweep.isValid()) return null;
        TriangleMesh tm = sweep_solid_evaluator_ui_main.evaluate(host, sweep);
        if (tm == null || tm.getPoints().size() == 0) return null;

        MeshView mv = new MeshView(tm);
        mv.setCullFace(CullFace.NONE);
        mv.setDrawMode(DrawMode.FILL);

        Color col = selected
            ? Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)
            : Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR);

        PhongMaterial mat = new PhongMaterial(col);
        mat.setSpecularColor(Color.web(selected ? "#BAE6FD" : "#5A6B7C"));
        mat.setSpecularPower(48.0);
        mv.setMaterial(mat);
        mv.setId(sweep.getId());

        return mv;
    }
}
