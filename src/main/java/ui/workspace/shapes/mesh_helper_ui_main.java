package ui.workspace.shapes;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * mesh_helper_ui_main.java
 * High-precision 3D mesh geometry and material generator for CAD coordinate systems.
 */
public final class mesh_helper_ui_main {

    private mesh_helper_ui_main() {}

    public static PhongMaterial createMaterial(Color diffuse, Color specular) {
        PhongMaterial mat = new PhongMaterial(diffuse);
        mat.setSpecularColor(specular);
        mat.setSpecularPower(32.0);
        return mat;
    }

    /**
     * Creates a smooth 24-sided 3D Cone mesh.
     * Apex points along -Y (Upward in JavaFX coordinates), base centered at (0, 0, 0).
     */
    public static Node createUpwardCone(double radius, double height, PhongMaterial material) {
        TriangleMesh mesh = new TriangleMesh();
        int sides = 24;

        float[] points = new float[(sides + 2) * 3];
        // Apex at (0, -height, 0)
        points[0] = 0;
        points[1] = (float) -height;
        points[2] = 0;

        // Base center at (0, 0, 0)
        points[3] = 0;
        points[4] = 0;
        points[5] = 0;

        for (int i = 0; i < sides; i++) {
            double a = i * 2 * Math.PI / sides;
            points[(i + 2) * 3]     = (float) (radius * Math.cos(a));
            points[(i + 2) * 3 + 1] = 0;
            points[(i + 2) * 3 + 2] = (float) (radius * Math.sin(a));
        }

        float[] texCoords = {0.5f, 0.5f};
        int[] faces = new int[sides * 2 * 6];
        int idx = 0;
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            faces[idx++] = 0;        faces[idx++] = 0;
            faces[idx++] = next + 2; faces[idx++] = 0;
            faces[idx++] = i + 2;    faces[idx++] = 0;
            faces[idx++] = 1;        faces[idx++] = 0;
            faces[idx++] = i + 2;    faces[idx++] = 0;
            faces[idx++] = next + 2; faces[idx++] = 0;
        }

        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);

        MeshView meshView = new MeshView(mesh);
        meshView.setMaterial(material);
        return meshView;
    }
}
