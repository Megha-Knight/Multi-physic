package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_face_ui_main;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * draft_feature_ui_main.java
 * Parametric Face Draft / Taper feature applying angular slope to planar faces.
 */
public class draft_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private face_kind_ui_main faceKind;
    private double draftAngle; // in degrees [-45, 45]
    private Point3D pullDirection;
    private face_kind_ui_main neutralKind;
    private boolean visible = true;
    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private String diagnosticMessage = null;

    public draft_feature_ui_main(String id, String ownerShapeId, String name,
                                 face_kind_ui_main faceKind, double draftAngle,
                                 Point3D pullDirection, face_kind_ui_main neutralKind) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.faceKind = Objects.requireNonNull(faceKind, "faceKind cannot be null");
        this.draftAngle = draftAngle;
        this.pullDirection = (pullDirection != null) ? pullDirection : new Point3D(0, -1, 0);
        this.neutralKind = (neutralKind != null) ? neutralKind : face_kind_ui_main.BOTTOM;
        this.name = (name != null && !name.isBlank()) ? name : getDefaultName(draftAngle);
    }

    public draft_feature_ui_main(String id, String ownerShapeId, String name,
                                 face_kind_ui_main faceKind, double draftAngle,
                                 face_kind_ui_main neutralKind) {
        this(id, ownerShapeId, name, faceKind, draftAngle, new Point3D(0, -1, 0), neutralKind);
    }

    public draft_feature_ui_main(String id, String ownerShapeId, face_kind_ui_main faceKind, double draftAngle) {
        this(id, ownerShapeId, null, faceKind, draftAngle, new Point3D(0, -1, 0), face_kind_ui_main.BOTTOM);
    }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getName() { return (name != null && !name.isBlank()) ? name : getDefaultName(draftAngle); }
    public void setName(String name) { this.name = name; }
    public face_kind_ui_main getFaceKind() { return faceKind; }
    public void setFaceKind(face_kind_ui_main k) { this.faceKind = k; }
    public double getDraftAngle() { return draftAngle; }
    public void setDraftAngle(double a) { this.draftAngle = a; }
    public Point3D getPullDirection() { return pullDirection; }
    public void setPullDirection(Point3D d) { this.pullDirection = d; }
    public face_kind_ui_main getNeutralKind() { return neutralKind; }
    public void setNeutralKind(face_kind_ui_main k) { this.neutralKind = k; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }
    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main s) { this.state = (s != null) ? s : feature_state_ui_main.CLEAN; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    public static String getDefaultName(double a) {
        return String.format(Locale.US, "Draft (%.1f°)", a);
    }

    public boolean isValid() {
        if (state == feature_state_ui_main.INVALID) return false;
        if (faceKind == null || Math.abs(draftAngle) < 0.1 || Math.abs(draftAngle) > 45.0) return false;
        return true;
    }

    public boolean revalidate(shape_item_ui_main host) {
        if (host == null || !host.getId().equals(ownerShapeId)) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Host body not found";
            return false;
        }
        if (faceKind == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Target face kind is null";
            return false;
        }
        if (Math.abs(draftAngle) < 0.1 || Math.abs(draftAngle) > 45.0) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = String.format(Locale.US, "Draft angle (%.1f°) must be between 0.1° and 45.0°", draftAngle);
            return false;
        }
        topology_body_ui_main topo = host.getTopology();
        if (topo == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Host topology unavailable";
            return false;
        }
        topology_face_ui_main targetFace = topo.getFace(faceKind);
        if (targetFace == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Target face " + faceKind.getLabel() + " not found on host";
            return false;
        }
        if (!faceKind.isPlanar() || faceKind == face_kind_ui_main.TOP || faceKind == face_kind_ui_main.BOTTOM) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Draft is only supported on planar side faces (FRONT, BACK, LEFT, RIGHT)";
            return false;
        }
        state = feature_state_ui_main.CLEAN;
        diagnosticMessage = null;
        return true;
    }
}
