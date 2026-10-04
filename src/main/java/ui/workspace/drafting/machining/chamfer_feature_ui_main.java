package ui.workspace.drafting.machining;

import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * chamfer_feature_ui_main.java
 * Parametric Edge Chamfer feature creating planar beveled transitions on solid edges.
 */
public class chamfer_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private String edgeId;
    private double distance;
    private boolean visible = true;
    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private String diagnosticMessage = null;

    public chamfer_feature_ui_main(String id, String ownerShapeId, String name, String edgeId, double distance) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.name = (name != null && !name.isBlank()) ? name : getDefaultName(distance);
        this.edgeId = Objects.requireNonNull(edgeId, "edgeId cannot be null");
        this.distance = distance;
    }

    public chamfer_feature_ui_main(String id, String ownerShapeId, String edgeId, double distance) {
        this(id, ownerShapeId, null, edgeId, distance);
    }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getName() { return (name != null && !name.isBlank()) ? name : getDefaultName(distance); }
    public void setName(String name) { this.name = name; }
    public String getEdgeId() { return edgeId; }
    public void setEdgeId(String edgeId) { this.edgeId = edgeId; }
    public double getDistance() { return distance; }
    public void setDistance(double d) { this.distance = d; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }
    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main s) { this.state = (s != null) ? s : feature_state_ui_main.CLEAN; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    public String getSimpleEdgeName() {
        if (edgeId == null) return "";
        int idx = edgeId.lastIndexOf(":E:");
        return idx >= 0 ? edgeId.substring(idx + 3) : edgeId;
    }

    public static String getDefaultName(double d) {
        return String.format(Locale.US, "Chamfer (%.1f mm)", d);
    }

    public boolean isValid() {
        if (state == feature_state_ui_main.INVALID) return false;
        if (distance <= 0.01 || edgeId == null || edgeId.isBlank()) return false;
        return true;
    }

    public boolean revalidate(shape_item_ui_main host) {
        if (host == null || !host.getId().equals(ownerShapeId)) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Host body not found";
            return false;
        }
        if (distance <= 0.01) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Chamfer distance must be > 0";
            return false;
        }
        topology_body_ui_main topo = host.getTopology();
        if (topo == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Host topology unavailable";
            return false;
        }
        String simpleName = getSimpleEdgeName();
        topology_edge_ui_main edge = topo.getEdge(simpleName);
        if (edge == null) edge = topo.getEdgeById(edgeId);
        if (edge == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Target edge " + edgeId + " not found on host";
            return false;
        }
        double edgeLen = edge.getLength();
        if (distance >= edgeLen * 0.5 + 1e-4) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = String.format(Locale.US, "Chamfer distance (%.1f mm) exceeds supported local geometry (max %.1f mm)", distance, edgeLen * 0.5);
            return false;
        }
        state = feature_state_ui_main.CLEAN;
        diagnosticMessage = null;
        return true;
    }
}
