package ui.workspace.drafting.topology;

import javafx.scene.paint.Color;
import ui.framework_ui_main;

/**
 * hole_surface_appearance_ui_main.java
 * Centralized visual conventions for hole-created internal surfaces and highlights.
 */
public final class hole_surface_appearance_ui_main {

    private hole_surface_appearance_ui_main() {}

    public static final Color DEFAULT_HOLE_WALL_COLOR = Color.web("#F59E0B"); // Translucent warm amber/yellow
    public static final double DEFAULT_HOLE_WALL_OPACITY = 0.50;

    public static final Color DEFAULT_HOLE_FLOOR_COLOR = Color.web("#FBBF24"); // Translucent amber cap
    public static final double DEFAULT_HOLE_FLOOR_OPACITY = 0.50;

    public static final Color DEFAULT_BORE_WALL_COLOR = Color.web("#F59E0B");
    public static final double DEFAULT_BORE_WALL_OPACITY = 0.50;

    public static final Color SELECTION_HIGHLIGHT_COLOR = Color.web(framework_ui_main.PRIMARY_BRAND_COLOR);
    public static final Color HOVER_HIGHLIGHT_COLOR = Color.web("#38BDF8");
    public static final Color FEATURE_HIGHLIGHT_COLOR = Color.web("#FDE68A");
    public static final Color DIAGNOSTIC_INVALID_COLOR = Color.web("#EF4444");

    public static topology_face_appearance_ui_main forRegion(hole_region_kind_ui_main region) {
        if (region == null) return topology_face_appearance_ui_main.DEFAULT;
        return switch (region) {
            case HOLE_WALL -> new topology_face_appearance_ui_main(DEFAULT_HOLE_WALL_COLOR, DEFAULT_HOLE_WALL_OPACITY, true);
            case HOLE_FLOOR -> new topology_face_appearance_ui_main(DEFAULT_HOLE_FLOOR_COLOR, DEFAULT_HOLE_FLOOR_OPACITY, true);
            case BORE_WALL -> new topology_face_appearance_ui_main(DEFAULT_BORE_WALL_COLOR, DEFAULT_BORE_WALL_OPACITY, true);
            default -> topology_face_appearance_ui_main.DEFAULT;
        };
    }
}
