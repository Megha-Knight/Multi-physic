package ui.workspace.drafting.topology;

import javafx.scene.paint.Color;
import ui.workspace.drafting.faces.viewport_color_state_ui_main;

/**
 * hole_surface_appearance_ui_main.java
 * Centralized visual conventions for hole-created internal surfaces and highlights.
 * Derived programmatically from owning shape base color (baseColor.darker()).
 */
public final class hole_surface_appearance_ui_main {

    private hole_surface_appearance_ui_main() {}

    public static final double DEFAULT_HOLE_WALL_OPACITY = 0.50;
    public static final double DEFAULT_HOLE_FLOOR_OPACITY = 0.50;
    public static final double DEFAULT_BORE_WALL_OPACITY = 0.50;

    public static Color getHoleColor(hole_region_kind_ui_main region) {
        return viewport_color_state_ui_main.getHoleBaseColor();
    }

    public static double getHoleOpacity(hole_region_kind_ui_main region) {
        return 1.0;
    }

    public static Color getHighlightColor() {
        return viewport_color_state_ui_main.getFaceHighlightColor();
    }

    public static double getHighlightOpacity() {
        return viewport_color_state_ui_main.getFaceHighlightOpacity();
    }

    public static topology_face_appearance_ui_main forRegion(hole_region_kind_ui_main region) {
        if (region == null) return topology_face_appearance_ui_main.DEFAULT;
        return switch (region) {
            case HOLE_WALL -> new topology_face_appearance_ui_main(viewport_color_state_ui_main.getHoleBaseColor(), DEFAULT_HOLE_WALL_OPACITY, true);
            case HOLE_FLOOR -> new topology_face_appearance_ui_main(viewport_color_state_ui_main.getHoleBaseColor(), DEFAULT_HOLE_FLOOR_OPACITY, true);
            case BORE_WALL -> new topology_face_appearance_ui_main(viewport_color_state_ui_main.getHoleBaseColor(), DEFAULT_BORE_WALL_OPACITY, true);
            default -> topology_face_appearance_ui_main.DEFAULT;
        };
    }
}
