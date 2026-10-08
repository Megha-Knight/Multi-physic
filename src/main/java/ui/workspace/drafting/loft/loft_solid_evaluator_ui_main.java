package ui.workspace.drafting.loft;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.profiles.profile_loop_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * loft_solid_evaluator_ui_main.java
 * Evaluates 3D triangulated solid loft geometry across multiple section profiles.
 */
public final class loft_solid_evaluator_ui_main {

    private static final int RESOLUTION = 32;

    private loft_solid_evaluator_ui_main() {}

    public static TriangleMesh evaluate(shape_item_ui_main host, loft_feature_ui_main loft) {
        if (loft == null || !loft.isValid()) return null;
        List<profile_reference_ui_main> sections = loft.getSections();
        if (sections.size() < 2) return null;

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);

        List<List<Point3D>> rings = new ArrayList<>();
        for (profile_reference_ui_main sec : sections) {
            profile_loop_ui_main loop = sec.getLoop();
            if (loop == null) return null;
            profile_loop_ui_main resampled = loop.resample(RESOLUTION);
            rings.add(resampled.getPoints());
        }

        int vertexIndex = 0;
        List<Integer> ringBaseIndices = new ArrayList<>();

        for (List<Point3D> ring : rings) {
            ringBaseIndices.add(vertexIndex);
            for (Point3D pt : ring) {
                mesh.getPoints().addAll((float) pt.getX(), (float) pt.getY(), (float) pt.getZ());
                vertexIndex++;
            }
        }

        // Generate side quad strips between successive section rings
        for (int r = 0; r < rings.size() - 1; r++) {
            int base1 = ringBaseIndices.get(r);
            int base2 = ringBaseIndices.get(r + 1);
            loft_mesh_quad_helper_ui_main.addQuadStrip(mesh.getFaces(), base1, base2, RESOLUTION);
        }

        if (loft.isSolid()) {
            // Start Cap Center
            Point3D cStart = sections.get(0).getLoop().getCenter();
            int cStartIdx = vertexIndex++;
            mesh.getPoints().addAll((float) cStart.getX(), (float) cStart.getY(), (float) cStart.getZ());
            loft_mesh_quad_helper_ui_main.addCapFan(mesh.getFaces(), cStartIdx, ringBaseIndices.get(0), RESOLUTION, true);

            // End Cap Center
            Point3D cEnd = sections.get(sections.size() - 1).getLoop().getCenter();
            int cEndIdx = vertexIndex++;
            mesh.getPoints().addAll((float) cEnd.getX(), (float) cEnd.getY(), (float) cEnd.getZ());
            loft_mesh_quad_helper_ui_main.addCapFan(mesh.getFaces(), cEndIdx, ringBaseIndices.get(rings.size() - 1), RESOLUTION, false);
        }

        return mesh;
    }
}
