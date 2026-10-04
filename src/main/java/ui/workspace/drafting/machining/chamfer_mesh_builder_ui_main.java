package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;
import ui.workspace.drafting.topology.topology_face_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * chamfer_mesh_builder_ui_main.java
 * Generates 3D beveled mesh geometry for parametric edge chamfers.
 */
public final class chamfer_mesh_builder_ui_main {

    private chamfer_mesh_builder_ui_main() {}

    public static Node buildChamferNode(shape_item_ui_main shape, chamfer_feature_ui_main ch, boolean isSelected) {
        if (shape == null || ch == null || !ch.isValid()) return null;
        topology_body_ui_main topo = shape.getTopology();
        if (topo == null) return null;
        topology_edge_ui_main edge = topo.getEdge(ch.getSimpleEdgeName());
        if (edge == null) edge = topo.getEdgeById(ch.getEdgeId());
        if (edge == null) return null;

        List<String> adjFaces = edge.getAdjacentFaceIds();
        if (adjFaces.size() < 2) return null;
        topology_face_ui_main f1 = topo.getFaceById(adjFaces.get(0));
        topology_face_ui_main f2 = topo.getFaceById(adjFaces.get(1));
        if (f1 == null || f2 == null) return null;

        Point3D p1 = edge.getLocalP1(), p2 = edge.getLocalP2();
        Point3D n1 = getFaceOutwardNormal(f1.getFaceKind()), n2 = getFaceOutwardNormal(f2.getFaceKind());
        double d = ch.getDistance();

        // Inward offsets on the two intersecting faces
        Point3D off1 = n2.multiply(-d);
        Point3D off2 = n1.multiply(-d);

        Point3D v1 = p1.add(off1), v2 = p2.add(off1);
        Point3D v3 = p2.add(off2), v4 = p1.add(off2);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        float[] pts = new float[]{
            (float) v1.getX(), (float) v1.getY(), (float) v1.getZ(),
            (float) v2.getX(), (float) v2.getY(), (float) v2.getZ(),
            (float) v3.getX(), (float) v3.getY(), (float) v3.getZ(),
            (float) v4.getX(), (float) v4.getY(), (float) v4.getZ()
        };
        mesh.getPoints().setAll(pts);
        int[] fcs = new int[]{
            0, 0, 1, 1, 2, 2,  0, 0, 2, 2, 3, 3,
            0, 0, 2, 2, 1, 1,  0, 0, 3, 3, 2, 2
        };
        mesh.getFaces().setAll(fcs);
        int[] sg = new int[]{1, 1, 1, 1};
        mesh.getFaceSmoothingGroups().setAll(sg);

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        Color c = isSelected ? Color.web("#38BDF8") : Color.web("#F59E0B");
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.color(1, 1, 1, 0.5));
        mv.setMaterial(mat);
        mv.setId(ch.getId());
        return mv;
    }

    private static Point3D getFaceOutwardNormal(ui.workspace.drafting.faces.face_kind_ui_main kind) {
        return switch (kind) {
            case TOP -> new Point3D(0, -1, 0);
            case BOTTOM -> new Point3D(0, 1, 0);
            case FRONT -> new Point3D(0, 0, 1);
            case BACK -> new Point3D(0, 0, -1);
            case LEFT -> new Point3D(-1, 0, 0);
            case RIGHT -> new Point3D(1, 0, 0);
            default -> new Point3D(0, 1, 0);
        };
    }
}
