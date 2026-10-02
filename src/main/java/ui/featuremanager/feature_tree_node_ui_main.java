package ui.featuremanager;

import javafx.scene.image.Image;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

public class feature_tree_node_ui_main {

    private final shape_item_ui_main shape;
    private final hole_feature_ui_main hole;
    private final shape_item_ui_main parentShape;

    private feature_tree_node_ui_main(shape_item_ui_main shape, hole_feature_ui_main hole, shape_item_ui_main parentShape) {
        this.shape = shape;
        this.hole = hole;
        this.parentShape = parentShape;
    }

    public static feature_tree_node_ui_main forShape(shape_item_ui_main shape) {
        return new feature_tree_node_ui_main(shape, null, null);
    }

    public static feature_tree_node_ui_main forHole(shape_item_ui_main parentShape, hole_feature_ui_main hole) {
        return new feature_tree_node_ui_main(null, hole, parentShape);
    }

    public boolean isShape() { return shape != null; }
    public boolean isHole() { return hole != null; }
    public shape_item_ui_main getShape() { return shape; }
    public hole_feature_ui_main getHole() { return hole; }
    public shape_item_ui_main getParentShape() { return parentShape; }

    public String getLabel() {
        if (isShape()) return featuremanager_ui_main.formatItemLabel(shape);
        if (isHole()) {
            String name = hole.getName();
            String detail = hole.isThroughAll() ? "Through" : String.format(java.util.Locale.US, "d:%.1f", hole.getDepth());
            return String.format(java.util.Locale.US, "%s [Ø%.1f, %s, %s]", name, hole.getDiameter(), detail, hole.getFaceKind().getLabel());
        }
        return "";
    }

    public Image getIcon() {
        if (isShape()) return loadIcon(getShapePath(shape.getType()));
        if (isHole()) return loadIcon(getHolePath(hole));
        return null;
    }

    private static String getShapePath(basic_shapes_ui_main type) {
        return switch (type) {
            case CIRCLE -> "/icons/circle.png";
            case SQUARE -> "/icons/square.png";
            case RECTANGLE -> "/icons/rectangle.png";
            case EQUILATERAL_TRIANGLE -> "/icons/triangle_equilateral.png";
            case RIGHT_TRIANGLE -> "/icons/triangle_right.png";
            case CUBE -> "/icons/cube_3d.png";
            case CUBOID -> "/icons/cuboid_3d.png";
            case CYLINDER -> "/icons/cylinder_3d.png";
            case SPHERE -> "/icons/sphere_3d.png";
            case CONE -> "/icons/cone_3d.png";
            default -> "/icons/basic_shapes.png";
        };
    }

    private static String getHolePath(hole_feature_ui_main h) {
        if (h.getCutoutShape() != null) {
            return switch (h.getCutoutShape()) {
                case SQUARE -> "/icons/square.png";
                case RECTANGLE -> "/icons/rectangle.png";
                case EQUILATERAL_TRIANGLE -> "/icons/triangle_equilateral.png";
                case RIGHT_TRIANGLE -> "/icons/triangle_right.png";
                default -> "/icons/circle.png";
            };
        }
        return "/icons/circle.png";
    }

    private static Image loadIcon(String path) {
        try {
            var stream = feature_tree_node_ui_main.class.getResourceAsStream(path);
            return (stream != null) ? new Image(stream) : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
