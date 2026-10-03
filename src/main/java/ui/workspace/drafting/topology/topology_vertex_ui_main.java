package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * topology_vertex_ui_main.java
 * Analytical vertex entity of a CAD body with stable ID and adjacency relations.
 */
public class topology_vertex_ui_main implements topology_entity_ui_main {

    private final String id;
    private final String name;
    private final String bodyId;
    private final Point3D localPos;
    private final List<String> adjacentEdgeIds = new ArrayList<>();
    private final List<String> adjacentFaceIds = new ArrayList<>();

    public topology_vertex_ui_main(String bodyId, String name, Point3D localPos) {
        this.bodyId = Objects.requireNonNull(bodyId, "bodyId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.id = bodyId + ":V:" + name;
        this.localPos = (localPos != null) ? localPos : Point3D.ZERO;
    }

    public void addAdjacentEdge(String edgeId) {
        if (edgeId != null && !adjacentEdgeIds.contains(edgeId)) adjacentEdgeIds.add(edgeId);
    }

    public void addAdjacentFace(String faceId) {
        if (faceId != null && !adjacentFaceIds.contains(faceId)) adjacentFaceIds.add(faceId);
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public String getBodyId() { return bodyId; }
    public Point3D getLocalPos() { return localPos; }
    public List<String> getAdjacentEdgeIds() { return Collections.unmodifiableList(adjacentEdgeIds); }
    public List<String> getAdjacentFaceIds() { return Collections.unmodifiableList(adjacentFaceIds); }

    public Point3D computeWorldPos(shape_item_ui_main shape) {
        if (shape == null) return localPos;
        Point3D c = shape.getCenter();
        return shape_rotation_helper_ui_main.transformPoint(
            localPos, c, shape.getRotationX(), shape.getRotationY(),
            shape.getWorldX(), shape.getWorldY(), shape.getWorldZ()
        );
    }

    @Override
    public String toString() {
        return String.format("Vertex[%s @ (%.2f, %.2f, %.2f)]", id, localPos.getX(), localPos.getY(), localPos.getZ());
    }
}
