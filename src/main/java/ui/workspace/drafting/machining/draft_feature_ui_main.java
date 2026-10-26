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
        this.neutralKind = (neutralKind != null) ? neutralKind : face_kind_ui_main.BOTTOM;
        Point3D nN = getFaceNormal(this.neutralKind);
        this.pullDirection = (pullDirection != null) ? pullDirection : (nN != null ? nN.multiply(-1.0) : new Point3D(0, -1, 0));
        this.name = (name != null && !name.isBlank()) ? name : getDefaultName(draftAngle);
    }

    public draft_feature_ui_main(String id, String ownerShapeId, String name,
                                 face_kind_ui_main faceKind, double draftAngle,
                                 face_kind_ui_main neutralKind) {
        this(id, ownerShapeId, name, faceKind, draftAngle, null, neutralKind);
    }

    public draft_feature_ui_main(String id, String ownerShapeId, face_kind_ui_main faceKind, double draftAngle) {
        this(id, ownerShapeId, null, faceKind, draftAngle, null, face_kind_ui_main.BOTTOM);
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

    public static Point3D getFaceNormal(face_kind_ui_main kind) {
        if (kind == null) return null;
        return switch (kind) {
            case TOP, TOP_CAP -> new Point3D(0, -1, 0);
            case BOTTOM, BOTTOM_CAP, BASE_CAP -> new Point3D(0, 1, 0);
            case FRONT -> new Point3D(0, 0, 1);
            case BACK -> new Point3D(0, 0, -1);
            case LEFT -> new Point3D(-1, 0, 0);
            case RIGHT -> new Point3D(1, 0, 0);
            default -> null;
        };
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
        if (host.getType() == ui.workspace.shapes.basic_shapes_ui_main.SPHERE
                || host.getType() == ui.workspace.shapes.basic_shapes_ui_main.CYLINDER
                || host.getType() == ui.workspace.shapes.basic_shapes_ui_main.CONE) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = host.getType().name() + " does not support planar face draft operations";
            return false;
        }
        if (faceKind == null || neutralKind == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Draft face or neutral face is null";
            return false;
        }
        if (Math.abs(draftAngle) < 0.1 || Math.abs(draftAngle) > 45.0) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = String.format(Locale.US, "Draft angle (%.1f°) must be between 0.1° and 45.0°", draftAngle);
            return false;
        }
        Point3D nFace = getFaceNormal(faceKind);
        Point3D nNeutral = getFaceNormal(neutralKind);
        if (nFace == null || nNeutral == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Draft is only supported on planar box faces";
            return false;
        }
        if (Math.abs(nFace.dotProduct(nNeutral)) > 0.1) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Draft face cannot be parallel or opposite to neutral face";
            return false;
        }
        if (pullDirection != null && pullDirection.magnitude() > 1e-4) {
            Point3D dNorm = pullDirection.normalize();
            if (Math.abs(dNorm.dotProduct(nNeutral)) < 1e-3) {
                state = feature_state_ui_main.INVALID;
                diagnosticMessage = "Pull direction cannot be parallel to neutral face";
                return false;
            }
            if (Math.abs(dNorm.dotProduct(nFace)) > 0.999) {
                state = feature_state_ui_main.INVALID;
                diagnosticMessage = "Pull direction cannot be parallel to draft face normal";
                return false;
            }
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
        state = feature_state_ui_main.CLEAN;
        diagnosticMessage = null;
        return true;
    }
}
