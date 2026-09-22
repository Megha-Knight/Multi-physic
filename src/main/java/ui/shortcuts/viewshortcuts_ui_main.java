package ui.shortcuts;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import ui.framework_ui_main;
import ui.workspace.cameracontroller_ui_main;

import java.util.HashMap;
import java.util.Map;

/**
 * viewshortcuts_ui_main.java
 * Dedicated keyboard shortcuts for 3D Viewport camera orientations and projections:
 * Orthographic toggle (Ctrl+Shift+O), Axonometric (Ctrl+Shift+A),
 * Isometric (Ctrl+Shift+I), Dimetric (Ctrl+Shift+D), and Principal Views (T/B/F/K/L/R).
 */
public class viewshortcuts_ui_main {

    public static final KeyCombination VIEW_TOP          = new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_BOTTOM       = new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_FRONT        = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_BACK         = new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_LEFT         = new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_RIGHT        = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_ISOMETRIC    = new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_AXONOMETRIC  = new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_DIMETRIC    = new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination VIEW_ORTHOGRAPHIC= new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);

    private final Map<KeyCombination, Runnable> actionMap = new HashMap<>();
    private cameracontroller_ui_main cameraController;
    private Runnable onToggleOrthographic;

    public viewshortcuts_ui_main() {
        registerDefaults();
    }

    public viewshortcuts_ui_main(cameracontroller_ui_main cameraController) {
        this.cameraController = cameraController;
        registerDefaults();
    }

    public void setCameraController(cameracontroller_ui_main cameraController) {
        this.cameraController = cameraController;
    }

    public void setOnToggleOrthographic(Runnable onToggleOrthographic) {
        this.onToggleOrthographic = onToggleOrthographic;
    }

    private void registerDefaults() {
        actionMap.put(VIEW_TOP,          () -> setOrientation(-89.9, 0.0, "Top View (Ctrl+Shift+T)"));
        actionMap.put(VIEW_BOTTOM,       () -> setOrientation(89.9, 0.0, "Bottom View (Ctrl+Shift+B)"));
        actionMap.put(VIEW_FRONT,        () -> setOrientation(0.0, 0.0, "Front View (Ctrl+Shift+F)"));
        actionMap.put(VIEW_BACK,         () -> setOrientation(0.0, 180.0, "Back View (Ctrl+Shift+K)"));
        actionMap.put(VIEW_LEFT,         () -> setOrientation(0.0, 90.0, "Left View (Ctrl+Shift+L)"));
        actionMap.put(VIEW_RIGHT,        () -> setOrientation(0.0, -90.0, "Right View (Ctrl+Shift+R)"));
        actionMap.put(VIEW_ISOMETRIC,    () -> setOrientation(framework_ui_main.ISO_PITCH, framework_ui_main.ISO_YAW, "Isometric View (Ctrl+Shift+I)"));
        actionMap.put(VIEW_AXONOMETRIC,  () -> setOrientation(framework_ui_main.ISO_PITCH, framework_ui_main.ISO_YAW, "Axonometric View (Ctrl+Shift+A)"));
        actionMap.put(VIEW_DIMETRIC,     () -> setOrientation(framework_ui_main.DIMETRIC_PITCH, framework_ui_main.DIMETRIC_YAW, "Dimetric View (Ctrl+Shift+D)"));
        actionMap.put(VIEW_ORTHOGRAPHIC, () -> {
            if (onToggleOrthographic != null) {
                onToggleOrthographic.run();
                System.out.println("[ViewShortcut] Toggle Orthographic/Perspective (Ctrl+Shift+O)");
            }
        });
    }

    private void setOrientation(double pitch, double yaw, String viewName) {
        System.out.println("[ViewShortcut] " + viewName);
        if (cameraController != null) {
            cameraController.setOrientation(pitch, yaw);
        }
    }

    public void register(KeyCombination combination, Runnable action) {
        if (combination != null && action != null) {
            actionMap.put(combination, action);
        }
    }

    public void attach(Scene scene) {
        if (scene == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    }

    private void handleKeyEvent(KeyEvent event) {
        for (Map.Entry<KeyCombination, Runnable> entry : actionMap.entrySet()) {
            if (entry.getKey().match(event)) {
                entry.getValue().run();
                event.consume();
                return;
            }
        }
    }

    public Map<KeyCombination, Runnable> getActionMap() {
        return actionMap;
    }
}
