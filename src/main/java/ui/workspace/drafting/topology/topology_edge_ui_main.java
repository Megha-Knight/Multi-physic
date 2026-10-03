package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * topology_edge_ui_main.java
 * Analytical edge entity connecting two topology vertices and bounding two topology faces.
 */
public class topology_edge_ui_main implements topology_entity_ui_main {

    private final String id;
    private final String name;
    private final String bodyId;
    private final String v1Id;
    private final String v2Id;
    private final List<String> adjacentFaceIds = new ArrayList<>();
    private final Point3D localP1;
    private final Point3D localP2;
    private final boolean linear;

    public topology_edge_ui_main(String bodyId, String name, String v1Id, String v2Id,
                                Point3D localP1, Point3D localP2, boolean linear) {
        this.bodyId = Objects.requireNonNull(bodyId, "bodyId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.id = bodyId + ":E:" + name;
        this.v1Id = v1Id;
        this.v2Id = v2Id;
        this.localP1 = (localP1 != null) ? localP1 : Point3D.ZERO;
        this.localP2 = (localP2 != null) ? localP2 : Point3D.ZERO;
        this.linear = linear;
    }

    public void addAdjacentFace(String faceId) {
        if (faceId != null && !adjacentFaceIds.contains(faceId)) adjacentFaceIds.add(faceId);
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public String getBodyId() { return bodyId; }
    public String getV1Id() { return v1Id; }
    public String getV2Id() { return v2Id; }
    public Point3D getLocalP1() { return localP1; }
    public Point3D getLocalP2() { return localP2; }
    public boolean isLinear() { return linear; }
    public List<String> getAdjacentFaceIds() { return Collections.unmodifiableList(adjacentFaceIds); }

    public Point3D getLocalMidpoint() {
        return localP1.add(localP2).multiply(0.5);
    }

    public Point3D getLocalDirection() {
        Point3D d = localP2.subtract(localP1);
        double len = d.magnitude();
        return (len > 1e-9) ? d.multiply(1.0 / len) : new Point3D(1, 0, 0);
    }

    public double getLength() {
        return localP1.distance(localP2);
    }

    public Point3D computeWorldMidpoint(shape_item_ui_main shape) {
        if (shape == null) return getLocalMidpoint();
        Point3D c = shape.getCenter();
        return ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformPoint(
            getLocalMidpoint(), c, shape.getRotationX(), shape.getRotationY(),
            shape.getWorldX(), shape.getWorldY(), shape.getWorldZ()
        );
    }

    @Override
    public String toString() {
        return String.format("Edge[%s (len=%.2f, linear=%b)]", id, getLength(), linear);
    }
}
