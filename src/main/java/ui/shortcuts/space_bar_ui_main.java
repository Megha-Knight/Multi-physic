package ui.shortcuts;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ui.workspace.camera.camera_controller_ui_main;

/**
 * space_bar_ui_main.java
 * Dedicated shortcut handler for Spacebar.
 * Resets the 3D viewport camera to the default isometric orientation and zoom.
 */
public class space_bar_ui_main {

    private camera_controller_ui_main cameraController;
    private Runnable customResetAction;

    public space_bar_ui_main() {
    }

    public space_bar_ui_main(camera_controller_ui_main cameraController) {
        this.cameraController = cameraController;
    }

    public void setCameraController(camera_controller_ui_main cameraController) {
        this.cameraController = cameraController;
    }

    public void setCustomResetAction(Runnable customResetAction) {
        this.customResetAction = customResetAction;
    }

    public void executeReset() {
        if (customResetAction != null) {
            customResetAction.run();
        } else if (cameraController != null) {
            cameraController.resetView();
        }
        System.out.println("[ResetView] Space: Reset 3D View to Isometric");
    }

    public void attach(Scene scene) {
        if (scene == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    }

    private void handleKeyEvent(KeyEvent event) {
        if (event.getTarget() instanceof TextInputControl) {
            return;
        }
        if (event.getCode() == KeyCode.SPACE && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
            executeReset();
            event.consume();
        }
    }
}
