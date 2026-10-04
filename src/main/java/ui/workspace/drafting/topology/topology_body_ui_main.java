package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import java.util.*;

/**
 * topology_body_ui_main.java
 * Analytical topology representation of a CAD solid body containing faces, edges, and vertices.
 */
public class topology_body_ui_main implements topology_entity_ui_main {

    private final String bodyId;
    private final basic_shapes_ui_main type;
    private final Map<face_kind_ui_main, topology_face_ui_main> faces = new LinkedHashMap<>();
    private final Map<String, topology_derived_face_ui_main> derivedFaces = new LinkedHashMap<>();
    private final Map<String, topology_edge_ui_main> edges = new LinkedHashMap<>();
    private final Map<String, topology_vertex_ui_main> vertices = new LinkedHashMap<>();

    public topology_body_ui_main(String bodyId, basic_shapes_ui_main type) {
        this.bodyId = Objects.requireNonNull(bodyId, "bodyId cannot be null");
        this.type = type;
    }

    @Override public String getId() { return bodyId; }
    @Override public String getName() { return (type != null ? type.getLabel() : "Body") + " [" + bodyId + "]"; }
    public basic_shapes_ui_main getType() { return type; }

    public void addFace(topology_face_ui_main face) { if (face != null) faces.put(face.getFaceKind(), face); }
    public void addDerivedFace(topology_derived_face_ui_main df) { if (df != null) derivedFaces.put(df.getId(), df); }
    public void addEdge(topology_edge_ui_main edge) { if (edge != null) edges.put(edge.getName(), edge); }
    public void addVertex(topology_vertex_ui_main vertex) { if (vertex != null) vertices.put(vertex.getName(), vertex); }

    public topology_face_ui_main getFace(face_kind_ui_main kind) { return faces.get(kind); }
    public topology_face_ui_main getFaceById(String faceId) {
        for (topology_face_ui_main f : faces.values()) if (f.getId().equals(faceId)) return f;
        return null;
    }
    public topology_derived_face_ui_main getDerivedFaceById(String id) { return derivedFaces.get(id); }
    public Collection<topology_derived_face_ui_main> getDerivedFaces() { return Collections.unmodifiableCollection(derivedFaces.values()); }
    public List<topology_derived_face_ui_main> getDerivedFacesForFeature(String fid) {
        List<topology_derived_face_ui_main> l = new ArrayList<>();
        if (fid != null) for (var df : derivedFaces.values()) if (fid.equals(df.getCreatingFeatureId())) l.add(df);
        return l;
    }
    public topology_edge_ui_main getEdge(String name) { return edges.get(name); }
    public topology_edge_ui_main getEdgeById(String edgeId) {
        for (topology_edge_ui_main e : edges.values()) if (e.getId().equals(edgeId)) return e;
        return null;
    }
    public topology_vertex_ui_main getVertex(String name) { return vertices.get(name); }
    public topology_vertex_ui_main getVertexById(String vertexId) {
        for (topology_vertex_ui_main v : vertices.values()) if (v.getId().equals(vertexId)) return v;
        return null;
    }

    public Collection<topology_face_ui_main> getFaces() { return Collections.unmodifiableCollection(faces.values()); }
    public Collection<topology_edge_ui_main> getEdges() { return Collections.unmodifiableCollection(edges.values()); }
    public Collection<topology_vertex_ui_main> getVertices() { return Collections.unmodifiableCollection(vertices.values()); }

    public int getFaceCount() { return faces.size(); }
    public int getDerivedFaceCount() { return derivedFaces.size(); }
    public int getEdgeCount() { return edges.size(); }
    public int getVertexCount() { return vertices.size(); }

    public static topology_body_ui_main buildTopology(shape_item_ui_main shape) {
        if (shape == null) return null;
        topology_body_ui_main body = new topology_body_ui_main(shape.getId(), shape.getType());
        if (shape.getType() == basic_shapes_ui_main.CUBE || shape.getType() == basic_shapes_ui_main.CUBOID) {
            buildBoxTopology(body, shape);
        } else if (shape.getType() == basic_shapes_ui_main.CYLINDER) {
            buildCylinderTopology(body, shape);
        } else if (shape.getType() == basic_shapes_ui_main.CONE) {
            buildConeTopology(body, shape);
        } else if (shape.getType() == basic_shapes_ui_main.SPHERE) {
            body.addFace(new topology_face_ui_main(body.getId(), face_kind_ui_main.SPHERE_SURFACE, 100, 100));
        }
        hole_topology_builder_ui_main.populateDerivedFaces(body, shape);
        machining_feature_topology_builder_ui_main.populateMachiningTopology(body, shape);
        return body;
    }

    private static void buildBoxTopology(topology_body_ui_main b, shape_item_ui_main s) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        double w, h, d, cx, cz;
        if (s.getType() == basic_shapes_ui_main.CUBE) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), sz = Math.max(Math.abs(dx), Math.abs(dz));
            w = sz; h = sz; d = sz;
            cx = p1.getX() + (dx >= 0 ? sz * 0.5 : -sz * 0.5); cz = p1.getZ() + (dz >= 0 ? sz * 0.5 : -sz * 0.5);
        } else {
            w = Math.max(0.1, Math.abs(p2.getX() - p1.getX())); d = Math.max(0.1, Math.abs(p2.getZ() - p1.getZ()));
            h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            cx = (p1.getX() + p2.getX()) * 0.5; cz = (p1.getZ() + p2.getZ()) * 0.5;
        }
        double x0 = cx - w * 0.5, x1 = cx + w * 0.5, y0 = 0.0, y1 = -h, z0 = cz - d * 0.5, z1 = cz + d * 0.5;
        // 8 Vertices
        addBoxV(b, "TOP_FRONT_LEFT", new Point3D(x0, y1, z1)); addBoxV(b, "TOP_FRONT_RIGHT", new Point3D(x1, y1, z1));
        addBoxV(b, "TOP_BACK_LEFT", new Point3D(x0, y1, z0));  addBoxV(b, "TOP_BACK_RIGHT", new Point3D(x1, y1, z0));
        addBoxV(b, "BOTTOM_FRONT_LEFT", new Point3D(x0, y0, z1)); addBoxV(b, "BOTTOM_FRONT_RIGHT", new Point3D(x1, y0, z1));
        addBoxV(b, "BOTTOM_BACK_LEFT", new Point3D(x0, y0, z0));  addBoxV(b, "BOTTOM_BACK_RIGHT", new Point3D(x1, y0, z0));
        // 12 Edges
        addBoxE(b, "TOP_FRONT", "TOP_FRONT_LEFT", "TOP_FRONT_RIGHT", "TOP", "FRONT");
        addBoxE(b, "TOP_BACK", "TOP_BACK_LEFT", "TOP_BACK_RIGHT", "TOP", "BACK");
        addBoxE(b, "TOP_LEFT", "TOP_BACK_LEFT", "TOP_FRONT_LEFT", "TOP", "LEFT");
        addBoxE(b, "TOP_RIGHT", "TOP_BACK_RIGHT", "TOP_FRONT_RIGHT", "TOP", "RIGHT");
        addBoxE(b, "BOTTOM_FRONT", "BOTTOM_FRONT_LEFT", "BOTTOM_FRONT_RIGHT", "BOTTOM", "FRONT");
        addBoxE(b, "BOTTOM_BACK", "BOTTOM_BACK_LEFT", "BOTTOM_BACK_RIGHT", "BOTTOM", "BACK");
        addBoxE(b, "BOTTOM_LEFT", "BOTTOM_BACK_LEFT", "BOTTOM_FRONT_LEFT", "BOTTOM", "LEFT");
        addBoxE(b, "BOTTOM_RIGHT", "BOTTOM_BACK_RIGHT", "BOTTOM_FRONT_RIGHT", "BOTTOM", "RIGHT");
        addBoxE(b, "FRONT_LEFT", "BOTTOM_FRONT_LEFT", "TOP_FRONT_LEFT", "FRONT", "LEFT");
        addBoxE(b, "FRONT_RIGHT", "BOTTOM_FRONT_RIGHT", "TOP_FRONT_RIGHT", "FRONT", "RIGHT");
        addBoxE(b, "BACK_LEFT", "BOTTOM_BACK_LEFT", "TOP_BACK_LEFT", "BACK", "LEFT");
        addBoxE(b, "BACK_RIGHT", "BOTTOM_BACK_RIGHT", "TOP_BACK_RIGHT", "BACK", "RIGHT");
        // 6 Faces
        addBoxF(b, face_kind_ui_main.TOP, w, d, new String[]{"TOP_FRONT", "TOP_RIGHT", "TOP_BACK", "TOP_LEFT"}, new String[]{"TOP_FRONT_LEFT", "TOP_FRONT_RIGHT", "TOP_BACK_RIGHT", "TOP_BACK_LEFT"});
        addBoxF(b, face_kind_ui_main.BOTTOM, w, d, new String[]{"BOTTOM_FRONT", "BOTTOM_RIGHT", "BOTTOM_BACK", "BOTTOM_LEFT"}, new String[]{"BOTTOM_FRONT_LEFT", "BOTTOM_FRONT_RIGHT", "BOTTOM_BACK_RIGHT", "BOTTOM_BACK_LEFT"});
        addBoxF(b, face_kind_ui_main.FRONT, w, h, new String[]{"TOP_FRONT", "FRONT_RIGHT", "BOTTOM_FRONT", "FRONT_LEFT"}, new String[]{"TOP_FRONT_LEFT", "TOP_FRONT_RIGHT", "BOTTOM_FRONT_RIGHT", "BOTTOM_FRONT_LEFT"});
        addBoxF(b, face_kind_ui_main.BACK, w, h, new String[]{"TOP_BACK", "BACK_RIGHT", "BOTTOM_BACK", "BACK_LEFT"}, new String[]{"TOP_BACK_LEFT", "TOP_BACK_RIGHT", "BOTTOM_BACK_RIGHT", "BOTTOM_BACK_LEFT"});
        addBoxF(b, face_kind_ui_main.LEFT, d, h, new String[]{"TOP_LEFT", "FRONT_LEFT", "BOTTOM_LEFT", "BACK_LEFT"}, new String[]{"TOP_BACK_LEFT", "TOP_FRONT_LEFT", "BOTTOM_FRONT_LEFT", "BOTTOM_BACK_LEFT"});
        addBoxF(b, face_kind_ui_main.RIGHT, d, h, new String[]{"TOP_RIGHT", "FRONT_RIGHT", "BOTTOM_RIGHT", "BACK_RIGHT"}, new String[]{"TOP_FRONT_RIGHT", "TOP_BACK_RIGHT", "BOTTOM_BACK_RIGHT", "BOTTOM_FRONT_RIGHT"});
    }

    private static void addBoxV(topology_body_ui_main b, String name, Point3D pos) {
        b.addVertex(new topology_vertex_ui_main(b.bodyId, name, pos));
    }
    private static void addBoxE(topology_body_ui_main b, String name, String v1Name, String v2Name, String f1Name, String f2Name) {
        topology_vertex_ui_main v1 = b.getVertex(v1Name), v2 = b.getVertex(v2Name);
        topology_edge_ui_main e = new topology_edge_ui_main(b.bodyId, name, v1.getId(), v2.getId(), v1.getLocalPos(), v2.getLocalPos(), true);
        String f1Id = b.bodyId + ":F:" + f1Name, f2Id = b.bodyId + ":F:" + f2Name;
        e.addAdjacentFace(f1Id); e.addAdjacentFace(f2Id);
        v1.addAdjacentEdge(e.getId()); v1.addAdjacentFace(f1Id); v1.addAdjacentFace(f2Id);
        v2.addAdjacentEdge(e.getId()); v2.addAdjacentFace(f1Id); v2.addAdjacentFace(f2Id);
        b.addEdge(e);
    }
    private static void addBoxF(topology_body_ui_main b, face_kind_ui_main kind, double w, double h, String[] edges, String[] verts) {
        topology_face_ui_main f = new topology_face_ui_main(b.bodyId, kind, w, h);
        for (String en : edges) f.addEdge(b.bodyId + ":E:" + en);
        for (String vn : verts) f.addVertex(b.bodyId + ":V:" + vn);
        b.addFace(f);
    }

    private static void buildCylinderTopology(topology_body_ui_main b, shape_item_ui_main s) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        double r = Math.max(0.1, p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
        double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        topology_face_ui_main topCap = new topology_face_ui_main(b.bodyId, face_kind_ui_main.TOP_CAP, r * 2.0, r * 2.0);
        topology_face_ui_main botCap = new topology_face_ui_main(b.bodyId, face_kind_ui_main.BOTTOM_CAP, r * 2.0, r * 2.0);
        topology_face_ui_main lateral = new topology_face_ui_main(b.bodyId, face_kind_ui_main.CYLINDER_LATERAL, 2 * Math.PI * r, h);
        topology_edge_ui_main topRim = new topology_edge_ui_main(b.bodyId, "TOP_RIM", null, null, new Point3D(p1.getX(), -h, p1.getZ()), new Point3D(p1.getX(), -h, p1.getZ()), false);
        topology_edge_ui_main botRim = new topology_edge_ui_main(b.bodyId, "BOTTOM_RIM", null, null, new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(p1.getX(), 0, p1.getZ()), false);
        topRim.addAdjacentFace(topCap.getId()); topRim.addAdjacentFace(lateral.getId());
        botRim.addAdjacentFace(botCap.getId()); botRim.addAdjacentFace(lateral.getId());
        topCap.addEdge(topRim.getId()); botCap.addEdge(botRim.getId());
        lateral.addEdge(topRim.getId()); lateral.addEdge(botRim.getId());
        b.addFace(topCap); b.addFace(botCap); b.addFace(lateral);
        b.addEdge(topRim); b.addEdge(botRim);
    }

    private static void buildConeTopology(topology_body_ui_main b, shape_item_ui_main s) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        double r = Math.max(0.1, p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
        double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        topology_face_ui_main baseCap = new topology_face_ui_main(b.bodyId, face_kind_ui_main.BASE_CAP, r * 2.0, r * 2.0);
        topology_face_ui_main lateral = new topology_face_ui_main(b.bodyId, face_kind_ui_main.CONE_LATERAL, 2 * Math.PI * r, h);
        topology_edge_ui_main baseRim = new topology_edge_ui_main(b.bodyId, "BASE_RIM", null, null, new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(p1.getX(), 0, p1.getZ()), false);
        baseRim.addAdjacentFace(baseCap.getId()); baseRim.addAdjacentFace(lateral.getId());
        baseCap.addEdge(baseRim.getId()); lateral.addEdge(baseRim.getId());
        b.addFace(baseCap); b.addFace(lateral); b.addEdge(baseRim);
    }
}
