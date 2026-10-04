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
 * fillet_mesh_builder_ui_main.java
 * Generates 3D rounded arc mesh geometry for parametric edge fillets.
 */
public final class fillet_mesh_builder_ui_main {

    private static final int SEGS = 16;

    private fillet_mesh_builder_ui_main() {}

    public static Node buildFilletNode(shape_item_ui_main shape, fillet_feature_ui_main fl, boolean isSelected) {
        if (shape == null || fl == null || !fl.isValid()) return null;
        topology_body_ui_main topo = shape.getTopology();
        if (topo == null) return null;
        topology_edge_ui_main edge = topo.getEdge(fl.getSimpleEdgeName());
        if (edge == null) edge = topo.getEdgeById(fl.getEdgeId());
        if (edge == null) return null;

        List<String> adjFaces = edge.getAdjacentFaceIds();
        if (adjFaces.size() < 2) return null;
        topology_face_ui_main f1 = topo.getFaceById(adjFaces.get(0));
        topology_face_ui_main f2 = topo.getFaceById(adjFaces.get(1));
        if (f1 == null || f2 == null) return null;

        Point3D p1 = edge.getLocalP1(), p2 = edge.getLocalP2();
        Point3D n1 = getFaceOutwardNormal(f1.getFaceKind()), n2 = getFaceOutwardNormal(f2.getFaceKind());
        double r = fl.getRadius();

        // Arc center is offset by -r along both face normals
        Point3D arcCenterP1 = p1.subtract(n1.multiply(r)).subtract(n2.multiply(r));
        Point3D arcCenterP2 = p2.subtract(n1.multiply(r)).subtract(n2.multiply(r));

        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        for (int i = 0; i <= SEGS; i++) {
            double t = (double) i / SEGS;
            double angle = t * (Math.PI * 0.5);
            Point3D rad = n2.multiply(r * Math.cos(angle)).add(n1.multiply(r * Math.sin(angle)));
            Point3D pt1 = arcCenterP1.add(rad);
            Point3D pt2 = arcCenterP2.add(rad);

            pts.add((float) pt1.getX()); pts.add((float) pt1.getY()); pts.add((float) pt1.getZ());
            pts.add((float) pt2.getX()); pts.add((float) pt2.getY()); pts.add((float) pt2.getZ());
        }

        for (int i = 0; i < SEGS; i++) {
            int p0 = i * 2, p1_idx = i * 2 + 1;
            int p2_idx = (i + 1) * 2 + 1, p3_idx = (i + 1) * 2;
            fcs.add(p0); fcs.add(0); fcs.add(p1_idx); fcs.add(1); fcs.add(p2_idx); fcs.add(2);
            fcs.add(p0); fcs.add(0); fcs.add(p2_idx); fcs.add(2); fcs.add(p3_idx); fcs.add(3);
            fcs.add(p0); fcs.add(0); fcs.add(p2_idx); fcs.add(2); fcs.add(p1_idx); fcs.add(1);
            fcs.add(p0); fcs.add(0); fcs.add(p3_idx); fcs.add(3); fcs.add(p2_idx); fcs.add(2);
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        float[] pa = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);
        mesh.getPoints().setAll(pa); mesh.getFaces().setAll(fa);
        int[] sg = new int[fa.length / 6]; java.util.Arrays.fill(sg, 1); mesh.getFaceSmoothingGroups().setAll(sg);

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        Color c = isSelected ? Color.web("#38BDF8") : Color.web("#10B981");
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.color(1, 1, 1, 0.5));
        mv.setMaterial(mat);
        mv.setId(fl.getId());
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
