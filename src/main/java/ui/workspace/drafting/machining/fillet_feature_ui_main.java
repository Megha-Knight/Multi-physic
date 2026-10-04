package ui.workspace.drafting.machining;

import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * fillet_feature_ui_main.java
 * Parametric Edge Fillet feature creating rounded blend transitions on solid edges.
 */
public class fillet_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private String edgeId;
    private double radius;
    private boolean visible = true;
    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private String diagnosticMessage = null;

    public fillet_feature_ui_main(String id, String ownerShapeId, String name, String edgeId, double radius) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.name = (name != null && !name.isBlank()) ? name : getDefaultName(radius);
        this.edgeId = Objects.requireNonNull(edgeId, "edgeId cannot be null");
        this.radius = radius;
    }

    public fillet_feature_ui_main(String id, String ownerShapeId, String edgeId, double radius) {
        this(id, ownerShapeId, null, edgeId, radius);
    }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getName() { return (name != null && !name.isBlank()) ? name : getDefaultName(radius); }
    public void setName(String name) { this.name = name; }
    public String getEdgeId() { return edgeId; }
    public void setEdgeId(String edgeId) { this.edgeId = edgeId; }
    public double getRadius() { return radius; }
    public void setRadius(double r) { this.radius = r; }
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

    public static String getDefaultName(double r) {
        return String.format(Locale.US, "Fillet (R%.1f mm)", r);
    }

    public boolean isValid() {
        if (state == feature_state_ui_main.INVALID) return false;
        if (radius <= 0.01 || edgeId == null || edgeId.isBlank()) return false;
        return true;
    }

    public boolean revalidate(shape_item_ui_main host) {
        if (host == null || !host.getId().equals(ownerShapeId)) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Host body not found";
            return false;
        }
        if (radius <= 0.01) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Fillet radius must be > 0";
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
        if (radius >= edgeLen * 0.5 + 1e-4) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = String.format(Locale.US, "Fillet radius (%.1f mm) exceeds supported local geometry (max %.1f mm)", radius, edgeLen * 0.5);
            return false;
        }
        state = feature_state_ui_main.CLEAN;
        diagnosticMessage = null;
        return true;
    }
}
