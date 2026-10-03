package ui.workspace.drafting.sketch;

import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.*;

public class sketch_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private final sketch_plane_type_ui_main planeType;
    private final face_kind_ui_main faceKind;
    private final sketch_coord_system_ui_main coordSystem;
    private final List<sketch_entity_ui_main> entities = new ArrayList<>();
    private final List<sketch_constraint_ui_main> constraints = new ArrayList<>();

    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private String diagnosticMessage = null;
    private boolean visible = true;
    private sketch_profile_analyzer_ui_main.AnalysisResult profileResult = null;
    private sketch_solver_ui_main.SolveResult solveResult = null;

    public sketch_feature_ui_main(String id, String ownerShapeId, String name,
                                  sketch_plane_type_ui_main planeType, face_kind_ui_main faceKind) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.name = (name != null && !name.isBlank()) ? name : "Sketch 1";
        this.planeType = (planeType != null) ? planeType : sketch_plane_type_ui_main.BASE_XZ;
        this.faceKind = faceKind;
        this.coordSystem = new sketch_coord_system_ui_main(this.planeType, this.ownerShapeId, this.faceKind);
    }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public sketch_plane_type_ui_main getPlaneType() { return planeType; }
    public face_kind_ui_main getFaceKind() { return faceKind; }
    public sketch_coord_system_ui_main getCoordSystem() { return coordSystem; }
    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main s) { this.state = (s != null) ? s : feature_state_ui_main.CLEAN; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    public List<sketch_entity_ui_main> getEntities() { return new ArrayList<>(entities); }
    public void addEntity(sketch_entity_ui_main e) { if (e != null) { entities.add(e); regenerate(); } }
    public void removeEntity(String eId) {
        entities.removeIf(e -> e.getId().equals(eId));
        constraints.removeIf(c -> c.getGeometryIds().contains(eId));
        regenerate();
    }
    public sketch_entity_ui_main getEntity(String eId) {
        for (sketch_entity_ui_main e : entities) if (e.getId().equals(eId)) return e;
        return null;
    }

    public List<sketch_constraint_ui_main> getConstraints() { return new ArrayList<>(constraints); }
    public void addConstraint(sketch_constraint_ui_main c) { if (c != null) { constraints.add(c); regenerate(); } }
    public void removeConstraint(String cId) { constraints.removeIf(c -> c.getId().equals(cId)); regenerate(); }
    public sketch_constraint_ui_main getConstraint(String cId) {
        for (sketch_constraint_ui_main c : constraints) if (c.getId().equals(cId)) return c;
        return null;
    }

    public sketch_profile_analyzer_ui_main.AnalysisResult getProfileResult() { return profileResult; }
    public sketch_profile_analyzer_ui_main.AnalysisResult getProfile() { return profileResult; }
    public sketch_solver_ui_main.SolveResult getSolveResult() { return solveResult; }

    public boolean revalidate(shape_item_ui_main host) {
        if (planeType == sketch_plane_type_ui_main.FACE) {
            if (host == null || !host.getId().equals(ownerShapeId)) {
                state = feature_state_ui_main.INVALID;
                diagnosticMessage = "Host body missing";
                return false;
            }
            if (faceKind != null) {
                var topo = host.getTopology();
                if (topo == null || topo.getFace(faceKind) == null) {
                    state = feature_state_ui_main.INVALID;
                    diagnosticMessage = "Face " + faceKind.name() + " does not exist on host";
                    return false;
                }
            }
            coordSystem.updateFromHost(host);
        }
        return regenerate();
    }

    public boolean regenerate() {
        solveResult = sketch_solver_ui_main.solve(entities, constraints);
        if (solveResult.status() == sketch_constraint_status_ui_main.INVALID) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = solveResult.message();
            profileResult = null;
            return false;
        }
        if (entities.isEmpty()) {
            state = feature_state_ui_main.CLEAN;
            diagnosticMessage = null;
            profileResult = new sketch_profile_analyzer_ui_main.AnalysisResult(
                sketch_profile_type_ui_main.OPEN, Collections.emptyList(), null, Collections.emptyList(), "Empty sketch"
            );
            return true;
        }
        profileResult = sketch_profile_analyzer_ui_main.analyze(entities);
        if (profileResult.type() == sketch_profile_type_ui_main.SELF_INTERSECTING ||
            profileResult.type() == sketch_profile_type_ui_main.INVALID) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = profileResult.message();
            return false;
        }
        state = feature_state_ui_main.CLEAN;
        diagnosticMessage = null;
        return true;
    }

    public boolean isValid() { return state != feature_state_ui_main.INVALID; }

    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(";G=").append(entities.size()).append(";");
        for (sketch_entity_ui_main e : entities) sb.append(e.getId()).append(":").append(e.getType().name()).append(",");
        sb.append(";C=").append(constraints.size()).append(";");
        for (sketch_constraint_ui_main c : constraints) sb.append(c.getId()).append(":").append(c.getType().name()).append(",");
        sb.append(";P=").append(profileResult != null ? profileResult.type().name() : "NONE");
        return sb.toString();
    }

    public sketch_feature_ui_main copy() {
        sketch_feature_ui_main c = new sketch_feature_ui_main(id, ownerShapeId, name, planeType, faceKind);
        for (sketch_entity_ui_main e : entities) c.entities.add(e.copy());
        for (sketch_constraint_ui_main cn : constraints) c.constraints.add(cn.copy());
        c.state = state;
        c.diagnosticMessage = diagnosticMessage;
        c.visible = visible;
        c.regenerate();
        return c;
    }
}
