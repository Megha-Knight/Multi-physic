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
    private double savedPitch = 30.0, savedYaw = -45.0;
    private Consumer<String> statusCallback = null;
    private final java.util.List<Runnable> modeListeners = new java.util.ArrayList<>();
    private camera_controller_ui_main lastCam = null;
    private boolean isNewSketch = false;

    public boolean isActive() { return active; }
    public sketch_feature_ui_main getActiveSketch() { return activeSketch; }
    public shape_item_ui_main getActiveHost() { return activeHost; }
    public sketch_coord_system_ui_main getActiveCoordSystem() { return activeCoordSystem; }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }
    public void setOnModeChanged(Runnable r) { modeListeners.clear(); if (r != null) modeListeners.add(r); }
    public void addOnModeChanged(Runnable r) { if (r != null && !modeListeners.contains(r)) modeListeners.add(r); }

    private void fireModeChanged() {
        for (Runnable r : modeListeners) { if (r != null) { try { r.run(); } catch (Exception ignored) {} } }
    }

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
            return enterBasePlane(host, sketch_plane_type_ui_main.BASE_XZ, camCtrl);
        }

        this.active = true;
        orientCamera(camCtrl);
        if (statusCallback != null) statusCallback.accept("SKETCH: pick Line/Circle/Rect/Arc, click twice to draw, Finish Sketch when done.");
        fireModeChanged();
        return true;
    }

    public boolean enterBasePlane(shape_item_ui_main host, sketch_plane_type_ui_main planeType, camera_controller_ui_main camCtrl) {
        if (planeType == null) planeType = sketch_plane_type_ui_main.BASE_XZ;
        this.activeHost = host;
        this.isNewSketch = true;
        this.activeSketch = new sketch_feature_ui_main(
            java.util.UUID.randomUUID().toString(), host != null ? host.getId() : null,
            "Sketch_" + planeType.name(), planeType, null
        );
        if (host != null) host.addSketch(activeSketch);
        this.activeCoordSystem = sketch_coord_system_ui_main.forBasePlane(planeType);
        this.active = true;
        orientCamera(camCtrl);
        if (statusCallback != null) statusCallback.accept("SKETCH on " + planeType + ": pick a tool, click twice to draw, Finish Sketch to exit.");
        fireModeChanged();
        return true;
    }

    public boolean enterSketch(sketch_feature_ui_main sk, shape_item_ui_main host, camera_controller_ui_main camCtrl) {
        if (sk == null) return false;
        this.activeSketch = sk;
        this.activeHost = host;
        this.isNewSketch = false;
        this.activeCoordSystem = sk.getCoordSystem();
        this.active = true;
        orientCamera(camCtrl);
        if (statusCallback != null) statusCallback.accept("Editing " + sk.getName() + ": pick tool to draw or edit, Finish Sketch when done.");
        fireModeChanged();
        return true;
    }

    public void exit(boolean commit, shape_editor_ui_main editor) {
        if (!active) return;
        String name = (activeSketch != null) ? activeSketch.getName() : "Sketch";
        if (commit && activeSketch != null) {
            if (activeHost == null && editor != null) {
                var holder = new shape_item_ui_main(
                    ui.workspace.shapes.basic_shapes_ui_main.NONE, new Point3D(0, 0, 0), new Point3D(0, 0, 0)
                );
                holder.setName(activeSketch.getName());
                holder.addSketch(activeSketch);
                this.activeHost = holder;
                editor.addShape(holder);
            }
            activeSketch.revalidate(activeHost);
            if (editor != null) {
                feature_regeneration_engine_ui_main.regenerateAll(editor.getShapes());
                editor.notifyShapesChanged();
            }
        } else if (!commit && isNewSketch && activeHost != null && activeSketch != null) {
            activeHost.removeSketch(activeSketch.getId());
            if (editor != null) editor.notifyShapesChanged();
        }

        if (lastCam != null) {
            lastCam.setOrientation(savedPitch, savedYaw);
            this.lastCam = null;
        }

        this.active = false;
        this.activeSketch = null;
        this.activeHost = null;
        this.activeCoordSystem = null;
        this.isNewSketch = false;

        if (statusCallback != null) {
            statusCallback.accept(commit ? "Finished " + name + ". Profile ready for downstream features." : "Cancelled " + name + ". 3D viewport restored.");
        }
        fireModeChanged();
    }

    private sketch_feature_ui_main findOrCreateFaceSketch(shape_item_ui_main host, face_reference_ui_main faceRef) {
        for (sketch_feature_ui_main s : host.getSketches()) {
            if (s.getFaceKind() == faceRef.getFaceKind()) {
                this.isNewSketch = false;
                return s;
            }
        }
        this.isNewSketch = true;
        sketch_feature_ui_main sk = new sketch_feature_ui_main(
            java.util.UUID.randomUUID().toString(), host.getId(), "Sketch_" + faceRef.getFaceKind().getLabel(),
            sketch_plane_type_ui_main.FACE, faceRef.getFaceKind()
        );
        host.addSketch(sk);
        return sk;
    }

    private void orientCamera(camera_controller_ui_main camCtrl) {
        if (camCtrl == null || activeCoordSystem == null) return;
        this.lastCam = camCtrl;
        if (camCtrl.getRx() != null) this.savedPitch = camCtrl.getRx().getAngle();
        if (camCtrl.getRy() != null) this.savedYaw = camCtrl.getRy().getAngle();
        Point3D n = activeCoordSystem.getNormal();
        double yaw = Math.toDegrees(Math.atan2(n.getX(), n.getZ()));
        double pitch = Math.toDegrees(Math.asin(-n.getY()));
        camCtrl.setOrientation(pitch, yaw);
    }
}
