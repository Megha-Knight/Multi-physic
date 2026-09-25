package ui.workspace.camera;

import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.framework_ui_main;

/**
 * camera_controller_ui_main.java
 * High-responsiveness camera orbit & zoom controller.
 * Attaches directly to the workspace Pane to ensure 100% reliable mouse capture.
 */
public class camera_controller_ui_main {

    private final Rotate rx;
    private final Rotate ry;
    private final Translate t;
    private final Runnable onChange;

    private double mouseOldX;
    private double mouseOldY;
    private boolean enabled = true;

    public camera_controller_ui_main(Rotate rx, Rotate ry, Translate t, Runnable onChange) {
        this.rx = rx;
        this.ry = ry;
        this.t = t;
        this.onChange = onChange;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void attach(Pane container) {
        container.setOnMousePressed((MouseEvent event) -> {
            if (!enabled && event.isPrimaryButtonDown()) return;
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        container.setOnMouseDragged((MouseEvent event) -> {
            if (!enabled && event.isPrimaryButtonDown()) return;
            double dx = event.getSceneX() - mouseOldX;
            double dy = event.getSceneY() - mouseOldY;

            ry.setAngle(ry.getAngle() + dx * 0.45);

            double newPitch = rx.getAngle() - dy * 0.45;
            if (newPitch >  89.9) newPitch =  89.9;
            if (newPitch < -89.9) newPitch = -89.9;
            rx.setAngle(newPitch);

            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
            if (onChange != null) onChange.run();
        });

        container.setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY();
            double newDist = t.getZ() + delta * 1.5;
            if (newDist >  -100.0) newDist =  -100.0;
            if (newDist < -1800.0) newDist = -1800.0;
            t.setZ(newDist);
            if (onChange != null) onChange.run();
        });
    }

    public void setOrientation(double pitch, double yaw) {
        rx.setAngle(pitch);
        ry.setAngle(yaw);
        if (onChange != null) onChange.run();
    }

    public void resetView() { setIsometricView(); }

    public void setIsometricView() {
        rx.setAngle(framework_ui_main.ISO_PITCH);
        ry.setAngle(framework_ui_main.ISO_YAW);
        t.setZ(-400);
        if (onChange != null) onChange.run();
    }

    public void setDimetricView() {
        rx.setAngle(framework_ui_main.DIMETRIC_PITCH);
        ry.setAngle(framework_ui_main.DIMETRIC_YAW);
        t.setZ(-400);
        if (onChange != null) onChange.run();
    }

    public void setTrimetricView() {
        rx.setAngle(framework_ui_main.TRIMETRIC_PITCH);
        ry.setAngle(framework_ui_main.TRIMETRIC_YAW);
        t.setZ(-400);
        if (onChange != null) onChange.run();
    }

    public Rotate getRx() { return rx; }
    public Rotate getRy() { return ry; }
    public Translate getTranslate() { return t; }
}
