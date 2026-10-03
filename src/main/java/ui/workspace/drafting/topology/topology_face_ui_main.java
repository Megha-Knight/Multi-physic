package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * topology_face_ui_main.java
 * Analytical face entity bounded by topology edges and corner vertices.
 */
public class topology_face_ui_main implements topology_entity_ui_main {

    private final String id;
    private final String name;
    private final String bodyId;
    private final face_kind_ui_main faceKind;
    private final boolean planar;
    private final double width;
    private final double height;
    private final List<String> edgeIds = new ArrayList<>();
    private final List<String> vertexIds = new ArrayList<>();

    public topology_face_ui_main(String bodyId, face_kind_ui_main faceKind, double width, double height) {
        this.bodyId = Objects.requireNonNull(bodyId, "bodyId cannot be null");
        this.faceKind = Objects.requireNonNull(faceKind, "faceKind cannot be null");
        this.id = bodyId + ":F:" + faceKind.name();
        this.name = faceKind.getLabel();
        this.planar = faceKind.isPlanar();
        this.width = width;
        this.height = height;
    }

    public void addEdge(String edgeId) {
        if (edgeId != null && !edgeIds.contains(edgeId)) edgeIds.add(edgeId);
    }

    public void addVertex(String vertexId) {
        if (vertexId != null && !vertexIds.contains(vertexId)) vertexIds.add(vertexId);
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    public String getBodyId() { return bodyId; }
    public face_kind_ui_main getFaceKind() { return faceKind; }
    public boolean isPlanar() { return planar; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<String> getEdgeIds() { return Collections.unmodifiableList(edgeIds); }
    public List<String> getVertexIds() { return Collections.unmodifiableList(vertexIds); }

    public face_reference_ui_main toFaceReference(shape_item_ui_main shape, double localU, double localV) {
        if (shape == null) return null;
        var info = ui.workspace.drafting.faces.face_geometry_helper_ui_main.getFaceInfo(shape, faceKind);
        Point3D c = shape.getCenter();
        double rx = shape.getRotationX(), ry = shape.getRotationY(), wx = shape.getWorldX(), wy = shape.getWorldY(), wz = shape.getWorldZ();
        Point3D worldOrigin = ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformPoint(info.origin(), c, rx, ry, wx, wy, wz);
        Point3D worldNorm = ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformNormal(info.normal(), rx, ry).normalize();
        Point3D worldU = ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformNormal(info.uAxis(), rx, ry).normalize();
        Point3D worldV = ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main.transformNormal(info.vAxis(), rx, ry).normalize();
        Point3D worldHit = worldOrigin.add(worldU.multiply(localU)).add(worldV.multiply(localV));
        return new face_reference_ui_main(
            shape.getId(), faceKind, worldHit, worldNorm,
            worldOrigin, worldU, worldV, localU, localV, width, height
        );
    }

    @Override
    public String toString() {
        return String.format("Face[%s (%s, %.1fx%.1f, edges=%d, verts=%d)]", id, faceKind.name(), width, height, edgeIds.size(), vertexIds.size());
    }
}
