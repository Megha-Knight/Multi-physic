package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.List;

public final class sketch_extrude_bridge_ui_main {

    private sketch_extrude_bridge_ui_main() {}

    public static Group buildExtrusion(sketch_feature_ui_main sketch, double height, boolean selected) {
        return buildExtrusion(null, sketch, height, selected);
    }

    public static Group buildExtrusion(shape_item_ui_main shape, sketch_feature_ui_main sketch, double height, boolean selected) {
        Group grp = new Group();
        if (sketch == null || !sketch.isValid() || height <= 1e-4) return grp;

        sketch_profile_loop_ui_main outer = sketch.getProfileResult().outerLoop();
        if (outer == null || outer.size() < 3) return grp;

        List<sketch_point_2d_ui_main> pts2d = outer.getVertices();
        int n = pts2d.size();

        Point3D origin, normal, uAxis, vAxis;
        if (shape != null && sketch.getFaceKind() != null) {
            var f = ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main.getLocalFaceFrame(shape, sketch.getFaceKind());
            if (f != null) {
                origin = f.origin(); normal = f.n(); uAxis = f.u(); vAxis = f.v();
            } else {
                sketch_coord_system_ui_main cs = sketch.getCoordSystem();
                origin = cs.getOrigin(); normal = cs.getNormal(); uAxis = cs.getUAxis(); vAxis = cs.getVAxis();
            }
        } else {
            sketch_coord_system_ui_main cs = sketch.getCoordSystem();
            origin = cs.getOrigin(); normal = cs.getNormal(); uAxis = cs.getUAxis(); vAxis = cs.getVAxis();
        }

        float[] points = new float[n * 2 * 3];
        for (int i = 0; i < n; i++) {
            sketch_point_2d_ui_main p2 = pts2d.get(i);
            Point3D basePt = origin.add(uAxis.multiply(p2.x())).add(vAxis.multiply(p2.y()));
            Point3D topPt = basePt.add(normal.multiply(height));
            points[i * 3]     = (float) basePt.getX();
            points[i * 3 + 1] = (float) basePt.getY();
            points[i * 3 + 2] = (float) basePt.getZ();

            points[(n + i) * 3]     = (float) topPt.getX();
            points[(n + i) * 3 + 1] = (float) topPt.getY();
            points[(n + i) * 3 + 2] = (float) topPt.getZ();
        }

        float[] texCoords = {0, 0, 1, 0, 0, 1, 1, 1};
        int numFaces = (n - 2) * 2 + n * 2;
        int[] faces = new int[numFaces * 6];
        int fIdx = 0;

        for (int i = 1; i < n - 1; i++) {
            faces[fIdx++] = 0; faces[fIdx++] = 0;
            faces[fIdx++] = i + 1; faces[fIdx++] = 1;
            faces[fIdx++] = i; faces[fIdx++] = 2;
        }

        for (int i = 1; i < n - 1; i++) {
            faces[fIdx++] = n; faces[fIdx++] = 0;
            faces[fIdx++] = n + i; faces[fIdx++] = 1;
            faces[fIdx++] = n + i + 1; faces[fIdx++] = 2;
        }

        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n, b1 = i, b2 = next, t1 = n + i, t2 = n + next;
            faces[fIdx++] = b1; faces[fIdx++] = 0;
            faces[fIdx++] = b2; faces[fIdx++] = 1;
            faces[fIdx++] = t2; faces[fIdx++] = 2;
            faces[fIdx++] = b1; faces[fIdx++] = 0;
            faces[fIdx++] = t2; faces[fIdx++] = 2;
            faces[fIdx++] = t1; faces[fIdx++] = 3;
        }

        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);

        MeshView mv = new MeshView(mesh);
        Color color = selected ? Color.web("#F59E0B") : Color.web("#3B82F6");
        PhongMaterial mat = new PhongMaterial(color);
        mat.setSpecularColor(Color.WHITE);
        mv.setMaterial(mat);

        grp.getChildren().add(mv);
        return grp;
    }
}
