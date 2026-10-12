package ui.workspace.drafting.faces;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import ui.framework_ui_main;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * viewport_color_state_ui_main.java
 * Three-state CAD viewport color state manager:
 * - GRAY: when 3D viewport does not have focus (ribbon, navigation bar, panels clicked)
 * - BLUE: when 3D viewport has focus
 * - PER-FACE YELLOW: when a face or hole region is hovered or selected (reduced opacity 0.28)
 * - HOLE REGIONS: programmatically darker shade of base color (baseColor.darker())
 */
public final class viewport_color_state_ui_main {

    private viewport_color_state_ui_main() {}

    private static boolean viewportFocused = true;
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public static final Color FACE_HIGHLIGHT_COLOR = Color.web("#F59E0B");
    public static final double FACE_HIGHLIGHT_OPACITY = 0.28;

    public static boolean isViewportFocused() {
        return viewportFocused;
    }

    public static void setViewportFocused(boolean focused) {
        if (viewportFocused == focused) return;
        viewportFocused = focused;
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }

    public static void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public static Color getBaseColor() {
        return viewportFocused
            ? Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)
            : Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR);
    }

    public static Color getHoleBaseColor() {
        return getBaseColor().darker();
    }

    public static Color getFaceHighlightColor() {
        return FACE_HIGHLIGHT_COLOR;
    }

    public static double getFaceHighlightOpacity() {
        return FACE_HIGHLIGHT_OPACITY;
    }

    public static PhongMaterial getFaceHighlightMaterial() {
        Color c = Color.color(
            FACE_HIGHLIGHT_COLOR.getRed(),
            FACE_HIGHLIGHT_COLOR.getGreen(),
            FACE_HIGHLIGHT_COLOR.getBlue(),
            FACE_HIGHLIGHT_OPACITY
        );
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.color(1, 1, 1, 0.3));
        mat.setSpecularPower(32.0);
        return mat;
    }
}
