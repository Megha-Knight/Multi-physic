package ui.workspace.drafting.machining;

import javafx.scene.Node;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

public final class machining_solid_mesh_builder_ui_main {

    private machining_solid_mesh_builder_ui_main() {}

    public static Node buildSolid(shape_item_ui_main shape, boolean isSelected) {
        if (shape == null) return null;
        if (shape.getType() == basic_shapes_ui_main.CYLINDER) {
            return machining_cylinder_mesh_helper_ui_main.buildMachinedCylinder(shape, isSelected);
        }
        if (shape.getType() == basic_shapes_ui_main.CONE) {
            return machining_cylinder_mesh_helper_ui_main.buildMachinedCone(shape, isSelected);
        }
        return box_machining_mesh_helper_ui_main.buildMachinedBox(shape, isSelected);
    }
}
