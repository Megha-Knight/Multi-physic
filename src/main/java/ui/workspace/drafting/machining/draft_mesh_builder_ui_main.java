package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_face_ui_main;

/**
 * draft_mesh_builder_ui_main.java
 * Generates 3D tapered mesh geometry for parametric face draft features.
 */
public final class draft_mesh_builder_ui_main {

    private draft_mesh_builder_ui_main() {}

    public static Node buildDraftNode(shape_item_ui_main shape, draft_feature_ui_main dr, boolean isSelected) {
        if (shape == null || dr == null || !dr.isValid()) return null;
        topology_body_ui_main topo = shape.getTopology();
        if (topo == null) return null;
        topology_face_ui_main face = topo.getFace(dr.getFaceKind());
        if (face == null) return null;

        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
        double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
        double cx = (p1.getX() + p2.getX()) * 0.5, cz = (p1.getZ() + p2.getZ()) * 0.5;
        double hw = w * 0.5, hd = d * 0.5;

        double angleRad = Math.toRadians(dr.getDraftAngle());
        double taperOffset = h * Math.tan(angleRad);

        // Quad coordinates for the drafted face
        Point3D v1, v2, v3, v4;
        face_kind_ui_main fk = dr.getFaceKind();
        if (fk == face_kind_ui_main.FRONT) {
            v1 = new Point3D(cx - hw, 0, cz + hd);
            v2 = new Point3D(cx + hw, 0, cz + hd);
            v3 = new Point3D(cx + hw - taperOffset, -h, cz + hd);
            v4 = new Point3D(cx - hw + taperOffset, -h, cz + hd);
        } else if (fk == face_kind_ui_main.BACK) {
            v1 = new Point3D(cx + hw, 0, cz - hd);
            v2 = new Point3D(cx - hw, 0, cz - hd);
            v3 = new Point3D(cx - hw + taperOffset, -h, cz - hd);
            v4 = new Point3D(cx + hw - taperOffset, -h, cz - hd);
        } else if (fk == face_kind_ui_main.LEFT) {
            v1 = new Point3D(cx - hw, 0, cz - hd);
            v2 = new Point3D(cx - hw, 0, cz + hd);
            v3 = new Point3D(cx - hw, -h, cz + hd - taperOffset);
            v4 = new Point3D(cx - hw, -h, cz - hd + taperOffset);
        } else { // RIGHT
            v1 = new Point3D(cx + hw, 0, cz + hd);
            v2 = new Point3D(cx + hw, 0, cz - hd);
            v3 = new Point3D(cx + hw, -h, cz - hd + taperOffset);
            v4 = new Point3D(cx + hw, -h, cz + hd - taperOffset);
        }

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
        Color c = isSelected ? Color.web("#38BDF8") : Color.web("#8B5CF6");
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.color(1, 1, 1, 0.5));
        mv.setMaterial(mat);
        mv.setId(dr.getId());
        return mv;
    }
}
