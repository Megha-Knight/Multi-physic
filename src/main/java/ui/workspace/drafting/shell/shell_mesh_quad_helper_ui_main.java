package ui.workspace.drafting.shell;

import javafx.scene.shape.TriangleMesh;
import java.util.ArrayList;
import java.util.List;

/**
 * shell_mesh_quad_helper_ui_main.java
 * Quad tessellation utilities for thin-walled shell geometric solid models.
 */
public final class shell_mesh_quad_helper_ui_main {

    private shell_mesh_quad_helper_ui_main() {}

    public static void addQuad(List<Float> pts, List<Integer> faces,
                               double x0, double y0, double z0,
                               double x1, double y1, double z1,
                               double x2, double y2, double z2,
                               double x3, double y3, double z3) {
        int i0 = pts.size() / 3; pts.add((float)x0); pts.add((float)y0); pts.add((float)z0);
        int i1 = pts.size() / 3; pts.add((float)x1); pts.add((float)y1); pts.add((float)z1);
        int i2 = pts.size() / 3; pts.add((float)x2); pts.add((float)y2); pts.add((float)z2);
        int i3 = pts.size() / 3; pts.add((float)x3); pts.add((float)y3); pts.add((float)z3);
        faces.add(i0); faces.add(0); faces.add(i1); faces.add(0); faces.add(i2); faces.add(0);
        faces.add(i0); faces.add(0); faces.add(i2); faces.add(0); faces.add(i3); faces.add(0);
        // Double-sided winding for watertight rendering
        faces.add(i0); faces.add(0); faces.add(i2); faces.add(0); faces.add(i1); faces.add(0);
        faces.add(i0); faces.add(0); faces.add(i3); faces.add(0); faces.add(i2); faces.add(0);
    }

    public static TriangleMesh toMesh(List<Float> pts, List<Integer> faces) {
        TriangleMesh mesh = new TriangleMesh();
        float[] p = new float[pts.size()];
        for (int i = 0; i < pts.size(); i++) p[i] = pts.get(i);
        int[] f = new int[faces.size()];
        for (int i = 0; i < faces.size(); i++) f[i] = faces.get(i);
        mesh.getPoints().setAll(p);
        mesh.getTexCoords().setAll(0.5f, 0.5f);
        mesh.getFaces().setAll(f);
        return mesh;
    }
}
