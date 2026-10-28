package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import java.util.List;

public final class shape_render_adapter_ui_main {
    private shape_render_adapter_ui_main() {}

    public static void buildShapeVisuals(shape_item_ui_main shape, Group shapeGroup, boolean selected, List<hole_feature_ui_main> eff) {
        if (shape == null || shapeGroup == null) return;
        basic_shapes_ui_main type = shape.getType();
        var overlays = shape.getOverlays();
        var shells = shape.getShellHolder();
        var lofts = shape.getLoftHolder();
        var sweeps = shape.getSweepHolder();
        var revolves = shape.getRevolveHolder();
        var machining = shape.getMachiningHolder();

        if (overlays.hasCustomMesh()) overlays.updateCustomMeshMaterial(selected);
        else if (shells.hasValidShell()) shapeGroup.getChildren().addAll(shells.buildVisuals(shape, selected));
        else if (lofts.hasValidLoft()) shapeGroup.getChildren().addAll(lofts.buildVisuals(shape, selected));
        else if (sweeps.hasValidSweep()) shapeGroup.getChildren().addAll(sweeps.buildVisuals(shape, selected));
        else if (revolves.hasValidRevolve()) shapeGroup.getChildren().addAll(revolves.buildVisuals(shape, selected));
        else if (machining.hasVisibleValidFeatures() && (type == basic_shapes_ui_main.CUBE || type == basic_shapes_ui_main.CUBOID || type == basic_shapes_ui_main.CYLINDER || type == basic_shapes_ui_main.CONE)) {
            Node machSolid = ui.workspace.drafting.machining.machining_solid_mesh_builder_ui_main.buildSolid(shape, selected);
            if (machSolid != null) shapeGroup.getChildren().add(machSolid);
        } else {
            Point3D p1 = shape.getP1(); Point3D p2 = shape.getP2();
            Point3D uAxis = shape.uAxis; Point3D vAxis = shape.vAxis; Point3D normal = shape.normal;
            boolean hasExt = shape.hasValidExtrusions();
            Node geo = switch (type) {
                case CIRCLE -> hasExt ? null : shape_geometry_ui_main.createCircle(p1, p2, uAxis, vAxis, normal, false, selected);
                case SQUARE -> hasExt ? null : shape_geometry_ui_main.createSquare(p1, p2, uAxis, vAxis, normal, false, selected);
                case RECTANGLE -> hasExt ? null : shape_geometry_ui_main.createRectangle(p1, p2, uAxis, vAxis, normal, false, selected);
                case EQUILATERAL_TRIANGLE -> hasExt ? null : shape_geometry_ui_main.createEquilateralTriangle(p1, p2, uAxis, vAxis, normal, false, selected);
                case RIGHT_TRIANGLE -> hasExt ? null : shape_geometry_ui_main.createRightTriangle(p1, p2, uAxis, vAxis, normal, false, selected);
                case CUBE -> eff.isEmpty() ? shape_geometry_3d_ui_main.createCube(p1, p2, false, selected) : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCubeWithHoles(p1, p2, eff, false, selected);
                case CUBOID -> eff.isEmpty() ? shape_geometry_3d_ui_main.createCuboid(p1, p2, false, selected) : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCuboidWithHoles(p1, p2, eff, false, selected);
                case CYLINDER -> eff.isEmpty() ? shape_geometry_3d_ui_main.createCylinder(p1, p2, false, selected) : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCylinderWithHoles(p1, p2, eff, false, selected);
                case SPHERE -> shape_geometry_3d_ui_main.createSphere(p1, p2, false, selected);
                case CONE -> eff.isEmpty() ? shape_geometry_3d_ui_main.createCone(p1, p2, false, selected) : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildConeWithHoles(p1, p2, eff, false, selected);
                default -> null;
            };
            if (geo != null) shapeGroup.getChildren().add(geo);
        }
        if (!eff.isEmpty()) shapeGroup.getChildren().add(ui.workspace.drafting.topology.derived_region_visual_builder_ui_main.buildDerivedRegionVisuals(shape, shape.getTopology()));
        if (machining.hasVisibleValidFeatures() && !shape.hasValidExtrusions() && type != basic_shapes_ui_main.CUBE && type != basic_shapes_ui_main.CUBOID && type != basic_shapes_ui_main.CYLINDER && type != basic_shapes_ui_main.CONE) {
            shapeGroup.getChildren().addAll(machining.buildVisuals(shape, selected));
        }
        for (var ext : shape.getExtrusions()) {
            if (ext.isVisible() && ext.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                shapeGroup.getChildren().add(ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main.buildExtrudeNode(shape, ext, selected, shape.getSelectedExtrudeId() != null && shape.getSelectedExtrudeId().equals(ext.getId())));
            }
        }
        for (var sk : shape.getSketches()) {
            if (sk.isVisible()) shapeGroup.getChildren().add(ui.workspace.drafting.sketch.sketch_mesh_builder_ui_main.buildVisualGroup(sk, false));
        }
    }
}
