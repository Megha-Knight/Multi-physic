package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.features.feature_regeneration_engine_ui_main;

import java.util.List;
import java.util.function.Consumer;

/**
 * sketch_mode_ui_main.java
 * Governs the lifecycle of Sketch Mode: face attachment, coordinate system alignment,
 * camera orientation, and feature regeneration on close.
 */
public class sketch_mode_ui_main {

    private boolean active = false;
    private sketch_feature_ui_main activeSketch = null;
    private shape_item_ui_main activeHost = null;
    private sketch_coord_system_ui_main activeCoordSystem = null;
    private double savedPitch = 30.0, savedYaw = -45.0, savedDist = -400.0;
    private Consumer<String> statusCallback = null;
    private Runnable onModeChanged = null;

    public boolean isActive() { return active; }
    public sketch_feature_ui_main getActiveSketch() { return activeSketch; }
    public shape_item_ui_main getActiveHost() { return activeHost; }
    public sketch_coord_system_ui_main getActiveCoordSystem() { return activeCoordSystem; }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }
    public void setOnModeChanged(Runnable r) { this.onModeChanged = r; }

    public boolean enter(shape_item_ui_main host, face_reference_ui_main faceRef, camera_controller_ui_main camCtrl) {
        if (faceRef != null && !faceRef.isPlanar()) {
            if (statusCallback != null) statusCallback.accept("Cannot sketch on curved face: " + faceRef.getFaceKind().getLabel());
            return false;
        }

        if (host != null && faceRef != null) {
            this.activeHost = host;
            this.activeSketch = findOrCreateFaceSketch(host, faceRef);
            this.activeCoordSystem = sketch_coord_system_ui_main.forFaceReference(faceRef);
        } else {
            this.activeHost = null;
            this.activeSketch = new sketch_feature_ui_main("sk-" + System.currentTimeMillis(), null, "Sketch", sketch_plane_type_ui_main.BASE_XZ, null);
            this.activeCoordSystem = sketch_coord_system_ui_main.forBasePlane(sketch_plane_type_ui_main.BASE_XZ);
        }

        this.active = true;
        orientCamera(camCtrl);
        if (statusCallback != null) statusCallback.accept("Entered SKETCH MODE: " + activeSketch.getName() + " on " + activeCoordSystem.getPlaneType());
        if (onModeChanged != null) onModeChanged.run();
        return true;
    }

    public boolean enterBasePlane(shape_item_ui_main host, sketch_plane_type_ui_main planeType, camera_controller_ui_main camCtrl) {
        this.activeHost = host;
        this.activeSketch = new sketch_feature_ui_main("sk-" + System.currentTimeMillis(), host != null ? host.getId() : null, "Sketch_" + planeType.name(), planeType, null);
        if (host != null) host.addSketch(activeSketch);
        this.activeCoordSystem = sketch_coord_system_ui_main.forBasePlane(planeType);
        this.active = true;
        orientCamera(camCtrl);
        if (statusCallback != null) statusCallback.accept("Entered SKETCH MODE on " + planeType);
        if (onModeChanged != null) onModeChanged.run();
        return true;
    }

    public boolean enterSketch(sketch_feature_ui_main sk, shape_item_ui_main host, camera_controller_ui_main camCtrl) {
        if (sk == null) return false;
        this.activeSketch = sk;
        this.activeHost = host;
        this.activeCoordSystem = sk.getCoordSystem();
        this.active = true;
        orientCamera(camCtrl);
        if (statusCallback != null) statusCallback.accept("Entered SKETCH MODE: " + sk.getName());
        if (onModeChanged != null) onModeChanged.run();
        return true;
    }

    public void exit(boolean commit, shape_editor_ui_main editor) {
        if (!active) return;
        if (commit && activeSketch != null) {
            activeSketch.revalidate(activeHost);
            if (editor != null) {
                feature_regeneration_engine_ui_main.regenerateAll(editor.getShapes());
                editor.notifyShapesChanged();
            }
        }
        String name = (activeSketch != null) ? activeSketch.getName() : "Sketch";
        this.active = false;
        this.activeSketch = null;
        this.activeHost = null;
        this.activeCoordSystem = null;

        if (statusCallback != null) statusCallback.accept("Closed " + name + ". 3D viewport restored.");
        if (onModeChanged != null) onModeChanged.run();
    }

    private sketch_feature_ui_main findOrCreateFaceSketch(shape_item_ui_main host, face_reference_ui_main faceRef) {
        for (sketch_feature_ui_main s : host.getSketches()) {
            if (s.getFaceKind() == faceRef.getFaceKind()) return s;
        }
        sketch_feature_ui_main sk = new sketch_feature_ui_main(
            "sk-" + System.currentTimeMillis(), host.getId(), "Sketch_" + faceRef.getFaceKind().getLabel(),
            sketch_plane_type_ui_main.FACE, faceRef.getFaceKind()
        );
        host.addSketch(sk);
        return sk;
    }

    private void orientCamera(camera_controller_ui_main camCtrl) {
        if (camCtrl == null || activeCoordSystem == null) return;
        Point3D n = activeCoordSystem.getNormal();
        double yaw = Math.toDegrees(Math.atan2(n.getX(), n.getZ()));
        double pitch = Math.toDegrees(Math.asin(-n.getY()));
        camCtrl.setOrientation(pitch, yaw);
    }
}
