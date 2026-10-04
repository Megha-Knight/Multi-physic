package ui.workspace.drafting.booleans;

import javafx.scene.shape.TriangleMesh;
import java.util.ArrayList;
import java.util.List;

/**
 * boolean_box_mesh_helper_ui_main.java
 * Mesh generation helper for Box and Cylinder CSG solid operations.
 */
public final class boolean_box_mesh_helper_ui_main {

    private boolean_box_mesh_helper_ui_main() {}

    public static void addBoxBoundaryFacesWithHole(List<Float> pts, List<Integer> faces,
                                                   double x0, double x1, double y0, double y1, double z0, double z1,
                                                   double cx, double cz, double r, int sides, int wallStart) {
        // Bottom face (y0)
        addQuad(pts, faces, (float)x0, (float)y0, (float)z1, (float)x1, (float)y0, (float)z1, (float)x1, (float)y0, (float)z0, (float)x0, (float)y0, (float)z0);
        // Front face (z1)
        addQuad(pts, faces, (float)x0, (float)y0, (float)z1, (float)x1, (float)y0, (float)z1, (float)x1, (float)y1, (float)z1, (float)x0, (float)y1, (float)z1);
        // Back face (z0)
        addQuad(pts, faces, (float)x1, (float)y0, (float)z0, (float)x0, (float)y0, (float)z0, (float)x0, (float)y1, (float)z0, (float)x1, (float)y1, (float)z0);
        // Left face (x0)
        addQuad(pts, faces, (float)x0, (float)y0, (float)z0, (float)x0, (float)y0, (float)z1, (float)x0, (float)y1, (float)z1, (float)x0, (float)y1, (float)z0);
        // Right face (x1)
        addQuad(pts, faces, (float)x1, (float)y0, (float)z1, (float)x1, (float)y0, (float)z0, (float)x1, (float)y1, (float)z0, (float)x1, (float)y1, (float)z1);

        // Top face with circular hole
        int c0 = pts.size() / 3; pts.add((float)x0); pts.add((float)y1); pts.add((float)z0);
        int c1 = pts.size() / 3; pts.add((float)x1); pts.add((float)y1); pts.add((float)z0);
        int c2 = pts.size() / 3; pts.add((float)x1); pts.add((float)y1); pts.add((float)z1);
        int c3 = pts.size() / 3; pts.add((float)x0); pts.add((float)y1); pts.add((float)z1);

        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            int r0 = wallStart + i * 2, r1 = wallStart + next * 2;
            int corner = (i < sides / 4) ? c1 : (i < sides / 2) ? c2 : (i < 3 * sides / 4) ? c3 : c0;
            faces.add(corner); faces.add(0); faces.add(r1); faces.add(0); faces.add(r0); faces.add(0);
        }
    }

    public static TriangleMesh buildStandardBoxMesh(double x0, double x1, double y0, double y1, double z0, double z1) {
        TriangleMesh mesh = new TriangleMesh();
        List<Float> pts = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();
        addQuad(pts, faces, (float)x0, (float)y0, (float)z1, (float)x1, (float)y0, (float)z1, (float)x1, (float)y0, (float)z0, (float)x0, (float)y0, (float)z0);
        addQuad(pts, faces, (float)x0, (float)y1, (float)z0, (float)x1, (float)y1, (float)z0, (float)x1, (float)y1, (float)z1, (float)x0, (float)y1, (float)z1);
        addQuad(pts, faces, (float)x0, (float)y0, (float)z1, (float)x1, (float)y0, (float)z1, (float)x1, (float)y1, (float)z1, (float)x0, (float)y1, (float)z1);
        addQuad(pts, faces, (float)x1, (float)y0, (float)z0, (float)x0, (float)y0, (float)z0, (float)x0, (float)y1, (float)z0, (float)x1, (float)y1, (float)z0);
        addQuad(pts, faces, (float)x0, (float)y0, (float)z0, (float)x0, (float)y0, (float)z1, (float)x0, (float)y1, (float)z1, (float)x0, (float)y1, (float)z0);
        addQuad(pts, faces, (float)x1, (float)y0, (float)z1, (float)x1, (float)y0, (float)z0, (float)x1, (float)y1, (float)z0, (float)x1, (float)y1, (float)z1);
        mesh.getPoints().setAll(toFloatArray(pts));
        mesh.getTexCoords().setAll(0.5f, 0.5f);
        mesh.getFaces().setAll(toIntArray(faces));
        return mesh;
    }

    public static TriangleMesh buildBoxSubtractedMesh(double bx0, double bx1, double by0, double by1, double bz0, double bz1,
                                                      double sx0, double sx1, double sy0, double sy1, double sz0, double sz1) {
        TriangleMesh mesh = new TriangleMesh();
        List<Float> pts = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();
        addQuad(pts, faces, (float)bx0, (float)by0, (float)bz1, (float)bx1, (float)by0, (float)bz1, (float)bx1, (float)by0, (float)bz0, (float)bx0, (float)by0, (float)bz0);
        addQuad(pts, faces, (float)bx0, (float)by0, (float)bz0, (float)bx0, (float)by0, (float)bz1, (float)bx0, (float)by1, (float)bz1, (float)bx0, (float)by1, (float)bz0);
        addQuad(pts, faces, (float)bx1, (float)by0, (float)bz1, (float)bx1, (float)by0, (float)bz0, (float)bx1, (float)by1, (float)bz0, (float)bx1, (float)by1, (float)bz1);
        addQuad(pts, faces, (float)sx0, (float)sy0, (float)sz0, (float)sx1, (float)sy0, (float)sz0, (float)sx1, (float)sy0, (float)sz1, (float)sx0, (float)sy0, (float)sz1);
        addQuad(pts, faces, (float)sx0, (float)sy0, (float)sz0, (float)sx0, (float)sy1, (float)sz0, (float)sx1, (float)sy1, (float)sz0, (float)sx1, (float)sy0, (float)sz0);
        mesh.getPoints().setAll(toFloatArray(pts));
        mesh.getTexCoords().setAll(0.5f, 0.5f);
        mesh.getFaces().setAll(toIntArray(faces));
        return mesh;
    }

    public static TriangleMesh buildCylinderMesh(double cx, double cz, double r, double y0, double y1, int sides) {
        TriangleMesh mesh = new TriangleMesh();
        List<Float> pts = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();
        int baseCenter = pts.size() / 3; pts.add((float)cx); pts.add((float)y0); pts.add((float)cz);
        int topCenter = pts.size() / 3; pts.add((float)cx); pts.add((float)y1); pts.add((float)cz);
        int ringStart = pts.size() / 3;
        for (int i = 0; i < sides; i++) {
            double a = i * 2.0 * Math.PI / sides;
            float px = (float)(cx + r * Math.cos(a)), pz = (float)(cz + r * Math.sin(a));
            pts.add(px); pts.add((float)y0); pts.add(pz);
            pts.add(px); pts.add((float)y1); pts.add(pz);
        }
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            int b0 = ringStart + i * 2, t0 = b0 + 1, b1 = ringStart + next * 2, t1 = b1 + 1;
            faces.add(b0); faces.add(0); faces.add(t0); faces.add(0); faces.add(t1); faces.add(0);
            faces.add(b0); faces.add(0); faces.add(t1); faces.add(0); faces.add(b1); faces.add(0);
            faces.add(baseCenter); faces.add(0); faces.add(b1); faces.add(0); faces.add(b0); faces.add(0);
            faces.add(topCenter); faces.add(0); faces.add(t0); faces.add(0); faces.add(t1); faces.add(0);
        }
        mesh.getPoints().setAll(toFloatArray(pts));
        mesh.getTexCoords().setAll(0.5f, 0.5f);
        mesh.getFaces().setAll(toIntArray(faces));
        return mesh;
    }

    public static void addQuad(List<Float> pts, List<Integer> faces, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        int i0 = pts.size() / 3; pts.add(x0); pts.add(y0); pts.add(z0);
        int i1 = pts.size() / 3; pts.add(x1); pts.add(y1); pts.add(z1);
        int i2 = pts.size() / 3; pts.add(x2); pts.add(y2); pts.add(z2);
        int i3 = pts.size() / 3; pts.add(x3); pts.add(y3); pts.add(z3);
        faces.add(i0); faces.add(0); faces.add(i1); faces.add(0); faces.add(i2); faces.add(0);
        faces.add(i0); faces.add(0); faces.add(i2); faces.add(0); faces.add(i3); faces.add(0);
    }

    public static float[] toFloatArray(List<Float> l) { float[] a = new float[l.size()]; for (int i = 0; i < l.size(); i++) a[i] = l.get(i); return a; }
    public static int[] toIntArray(List<Integer> l) { int[] a = new int[l.size()]; for (int i = 0; i < l.size(); i++) a[i] = l.get(i); return a; }
}
