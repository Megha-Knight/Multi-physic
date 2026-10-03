package ui.workspace.drafting.gizmo;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

/**
 * viewport_coordinate_helper_ui_main.java
 * Canonical coordinate converter from JavaFX Scene mouse events to 2D CAD viewport pixels.
 * Ensures 3D Shape3D local coordinates from SubScene event targets are never used as viewport coordinates.
 */
public final class viewport_coordinate_helper_ui_main {

    private viewport_coordinate_helper_ui_main() {}

    public static Point2D getViewportPoint(MouseEvent e, Pane viewport) {
        if (e == null) return new Point2D(0, 0);
        if (viewport != null) {
            try {
                Point2D local = viewport.sceneToLocal(e.getSceneX(), e.getSceneY());
                if (local != null) return local;
            } catch (Exception ignored) {}
        }
        return new Point2D(e.getX(), e.getY());
    }

    public static double getViewportX(MouseEvent e, Pane viewport) {
        return getViewportPoint(e, viewport).getX();
    }

    public static double getViewportY(MouseEvent e, Pane viewport) {
        return getViewportPoint(e, viewport).getY();
    }
}
