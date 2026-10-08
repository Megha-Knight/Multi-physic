package ui.workspace.drafting.sweep;

import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.features.feature_type_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.profiles.profile_validator_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.UUID;

/**
 * sweep_feature_ui_main.java
 * Parametric Sweep feature definition creating 3D solids along 3D trajectories.
 */
public class sweep_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private profile_reference_ui_main profile;
    private sweep_path_ui_main path;
    private sweep_orientation_ui_main orientation;
    private boolean solid = true;
    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private boolean visible = true;
    private String diagnosticMessage = null;

    public sweep_feature_ui_main(String id, String ownerShapeId, String name,
                                 profile_reference_ui_main profile, sweep_path_ui_main path,
                                 sweep_orientation_ui_main orientation, boolean solid) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.name = (name != null && !name.isBlank()) ? name : "Sweep";
        this.profile = profile;
        this.path = path;
        this.orientation = orientation != null ? orientation : sweep_orientation_ui_main.FIXED;
        this.solid = solid;
        revalidate(null);
    }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public profile_reference_ui_main getProfile() { return profile; }
    public void setProfile(profile_reference_ui_main profile) { this.profile = profile; revalidate(null); }
    public sweep_path_ui_main getPath() { return path; }
    public void setPath(sweep_path_ui_main path) { this.path = path; revalidate(null); }
    public sweep_orientation_ui_main getOrientation() { return orientation; }
    public void setOrientation(sweep_orientation_ui_main orientation) { this.orientation = orientation != null ? orientation : sweep_orientation_ui_main.FIXED; revalidate(null); }
    public boolean isSolid() { return solid; }
    public void setSolid(boolean solid) { this.solid = solid; revalidate(null); }

    public feature_type_ui_main getFeatureType() { return feature_type_ui_main.SWEEP; }
    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main state) { this.state = state != null ? state : feature_state_ui_main.CLEAN; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    public boolean isValid() {
        if (state == feature_state_ui_main.INVALID || profile == null || path == null) return false;
        if (!profile_validator_ui_main.validate(profile, solid).isValid()) return false;
        return path.getWaypointCount() >= 2 && path.getLength() > 1e-6;
    }

    public void revalidate(shape_item_ui_main host) {
        if (profile == null) {
            this.state = feature_state_ui_main.INVALID;
            this.diagnosticMessage = "Sweep profile reference is missing.";
            return;
        }
        var pRes = profile_validator_ui_main.validate(profile, solid);
        if (!pRes.isValid()) {
            this.state = feature_state_ui_main.INVALID;
            this.diagnosticMessage = "Invalid sweep profile: " + pRes.getMessage();
            return;
        }
        if (path == null || path.getWaypointCount() < 2 || path.getLength() <= 1e-6) {
            this.state = feature_state_ui_main.INVALID;
            this.diagnosticMessage = "Sweep path must have at least 2 points and non-zero length.";
            return;
        }
        this.state = feature_state_ui_main.CLEAN;
        this.diagnosticMessage = null;
    }

    public sweep_feature_ui_main copy() {
        sweep_feature_ui_main cp = new sweep_feature_ui_main(id, ownerShapeId, name, profile, path, orientation, solid);
        cp.setState(state);
        cp.setVisible(visible);
        cp.setDiagnosticMessage(diagnosticMessage);
        return cp;
    }
}
