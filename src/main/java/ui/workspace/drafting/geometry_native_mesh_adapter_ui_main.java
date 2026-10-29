package ui.workspace.drafting;

import backend.native_bridge.geometry_tessellation_snapshot_ui_main;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public final class geometry_native_mesh_adapter_ui_main {
    private geometry_native_mesh_adapter_ui_main() {}

    public static MeshView createMeshView(geometry_tessellation_snapshot_ui_main snapshot, boolean selected) {
        if (snapshot == null || snapshot.getVertexCount() == 0 || snapshot.getTriangleCount() == 0) return null;

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(snapshot.getPositions());
        mesh.getTexCoords().setAll(0f, 0f);

        int[] triIndices = snapshot.getIndices();
        int[] faces = new int[triIndices.length * 2];
        for (int i = 0; i < triIndices.length / 3; i++) {
            faces[6 * i + 0] = triIndices[3 * i + 0];
            faces[6 * i + 1] = 0;
            faces[6 * i + 2] = triIndices[3 * i + 1];
            faces[6 * i + 3] = 0;
            faces[6 * i + 4] = triIndices[3 * i + 2];
            faces[6 * i + 5] = 0;
        }
        mesh.getFaces().setAll(faces);

        MeshView mv = new MeshView(mesh);
        Color baseColor = selected ? Color.web("#4A90E2") : Color.web("#90A4AE");
        PhongMaterial mat = new PhongMaterial(baseColor);
        mv.setMaterial(mat);
        mv.setCullFace(CullFace.BACK);
        mv.setDrawMode(DrawMode.FILL);

        mv.getProperties().put("ASTRA_GEOM_REVISION", snapshot.getRevision());
        mv.getProperties().put("ASTRA_GEOM_SNAPSHOT", snapshot);
        return mv;
    }
}
