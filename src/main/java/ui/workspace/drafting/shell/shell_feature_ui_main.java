package ui.workspace.drafting.shell;

import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.features.feature_type_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.*;

/**
 * shell_feature_ui_main.java
 * Parametric Shell / Hollow Thickness feature definition on a CAD solid body.
 */
public class shell_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private double thickness;
    private shell_direction_ui_main direction;
    private final Set<face_kind_ui_main> removedFaces = new LinkedHashSet<>();
    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private boolean visible = true;
    private String diagnosticMessage = null;

    public shell_feature_ui_main(String id, String ownerShapeId, String name,
                                 double thickness, shell_direction_ui_main direction,
                                 Collection<face_kind_ui_main> openFaces) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.name = (name != null && !name.isBlank()) ? name : "Shell";
        this.thickness = thickness;
        this.direction = (direction != null) ? direction : shell_direction_ui_main.INWARD;
        if (openFaces != null) {
            for (face_kind_ui_main f : openFaces) {
                if (f != null) this.removedFaces.add(f);
            }
        }
    }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getThickness() { return thickness; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public shell_direction_ui_main getDirection() { return direction; }
    public void setDirection(shell_direction_ui_main direction) { this.direction = direction != null ? direction : shell_direction_ui_main.INWARD; }

    public Set<face_kind_ui_main> getRemovedFaces() { return Collections.unmodifiableSet(removedFaces); }
    public boolean hasRemovedFace(face_kind_ui_main kind) { return removedFaces.contains(kind); }
    public void addRemovedFace(face_kind_ui_main kind) { if (kind != null) removedFaces.add(kind); }
    public void removeRemovedFace(face_kind_ui_main kind) { removedFaces.remove(kind); }
    public void clearRemovedFaces() { removedFaces.clear(); }
    public void setRemovedFaces(Collection<face_kind_ui_main> faces) {
        removedFaces.clear();
        if (faces != null) for (face_kind_ui_main f : faces) if (f != null) removedFaces.add(f);
    }

    public feature_type_ui_main getFeatureType() { return feature_type_ui_main.SHELL; }
    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main state) { this.state = state != null ? state : feature_state_ui_main.CLEAN; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    public boolean isValid() {
        return state != feature_state_ui_main.INVALID && thickness > 0 && !Double.isNaN(thickness) && !Double.isInfinite(thickness);
    }

    public void revalidate(shape_item_ui_main host) {
        var res = shell_validator_ui_main.validate(host, thickness, direction, removedFaces);
        if (!res.isValid()) {
            this.state = feature_state_ui_main.INVALID;
            this.diagnosticMessage = res.getReason();
        } else {
            if (this.state == feature_state_ui_main.INVALID) this.state = feature_state_ui_main.CLEAN;
            this.diagnosticMessage = null;
        }
    }

    public shell_feature_ui_main copy() {
        var copy = new shell_feature_ui_main(id, ownerShapeId, name, thickness, direction, removedFaces);
        copy.setState(state);
        copy.setVisible(visible);
        copy.setDiagnosticMessage(diagnosticMessage);
        return copy;
    }
}
