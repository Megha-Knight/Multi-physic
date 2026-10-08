package ui.workspace.drafting.revolve;

import javafx.scene.shape.ObservableFaceArray;

/**
 * revolve_mesh_quad_helper_ui_main.java
 * Helper to generate triangulated quad strips and rotational sector end-caps for Revolve meshes.
 */
public final class revolve_mesh_quad_helper_ui_main {

    private revolve_mesh_quad_helper_ui_main() {}

    public static void addQuadStrip(ObservableFaceArray faces, int baseRing1, int baseRing2, int count) {
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            int a = baseRing1 + i;
            int b = baseRing1 + next;
            int c = baseRing2 + next;
            int d = baseRing2 + i;

            // Triangle 1: a-b-c & double-sided
            faces.addAll(a, 0, b, 0, c, 0);
            faces.addAll(a, 0, c, 0, b, 0);
            // Triangle 2: a-c-d & double-sided
            faces.addAll(a, 0, c, 0, d, 0);
            faces.addAll(a, 0, d, 0, c, 0);
        }
    }

    public static void addCapFan(ObservableFaceArray faces, int centerIdx, int baseRing, int count, boolean flip) {
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            int p1 = baseRing + i;
            int p2 = baseRing + next;
            if (flip) {
                faces.addAll(centerIdx, 0, p2, 0, p1, 0);
                faces.addAll(centerIdx, 0, p1, 0, p2, 0);
            } else {
                faces.addAll(centerIdx, 0, p1, 0, p2, 0);
                faces.addAll(centerIdx, 0, p2, 0, p1, 0);
            }
        }
    }
}
