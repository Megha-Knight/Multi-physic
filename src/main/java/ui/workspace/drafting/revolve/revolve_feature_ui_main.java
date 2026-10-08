package ui.workspace.drafting.revolve;

import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.features.feature_type_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.UUID;

/**
 * revolve_feature_ui_main.java
 * Parametric Revolve feature definition rotating a 2D profile around a 3D axis.
 */
public class revolve_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private profile_reference_ui_main profile;
    private revolve_axis_ui_main axis;
    private double angle = 360.0;
    private revolve_direction_ui_main direction = revolve_direction_ui_main.FORWARD;
    private boolean solid = true;
    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private boolean visible = true;
    private String diagnosticMessage = null;

    public revolve_feature_ui_main(String id, String ownerShapeId, String name,
                                  profile_reference_ui_main profile, revolve_axis_ui_main axis,
                                  double angle, revolve_direction_ui_main direction, boolean solid) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.name = (name != null && !name.isBlank()) ? name : "Revolve";
        this.profile = profile;
        this.axis = axis != null ? axis : revolve_axis_ui_main.yAxis();
        this.angle = angle;
        this.direction = direction != null ? direction : revolve_direction_ui_main.FORWARD;
        this.solid = solid;
        revalidate(null);
    }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public profile_reference_ui_main getProfile() { return profile; }
    public void setProfile(profile_reference_ui_main profile) { this.profile = profile; revalidate(null); }
    public revolve_axis_ui_main getAxis() { return axis; }
    public void setAxis(revolve_axis_ui_main axis) { this.axis = axis; revalidate(null); }
    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; revalidate(null); }
    public revolve_direction_ui_main getDirection() { return direction; }
    public void setDirection(revolve_direction_ui_main direction) { this.direction = direction != null ? direction : revolve_direction_ui_main.FORWARD; revalidate(null); }
    public boolean isSolid() { return solid; }
    public void setSolid(boolean solid) { this.solid = solid; revalidate(null); }

    public feature_type_ui_main getFeatureType() { return feature_type_ui_main.REVOLVE; }
    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main state) { this.state = state != null ? state : feature_state_ui_main.CLEAN; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    public boolean isValid() {
        if (state == feature_state_ui_main.INVALID) return false;
        return revolve_validator_ui_main.validate(profile, axis, angle, solid).isValid();
    }

    public void revalidate(shape_item_ui_main host) {
        var res = revolve_validator_ui_main.validate(profile, axis, angle, solid);
        if (!res.isValid()) {
            this.state = feature_state_ui_main.INVALID;
            this.diagnosticMessage = res.getMessage();
        } else {
            this.state = feature_state_ui_main.CLEAN;
            this.diagnosticMessage = null;
        }
    }

    public revolve_feature_ui_main copy() {
        revolve_feature_ui_main cp = new revolve_feature_ui_main(id, ownerShapeId, name, profile, axis, angle, direction, solid);
        cp.setState(state);
        cp.setVisible(visible);
        cp.setDiagnosticMessage(diagnosticMessage);
        return cp;
    }
}
