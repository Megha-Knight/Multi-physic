package ui.workspace.drafting.sweep;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.loft.loft_mesh_quad_helper_ui_main;
import ui.workspace.drafting.profiles.profile_loop_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * sweep_solid_evaluator_ui_main.java
 * Evaluates 3D triangulated swept solids along trajectory paths.
 */
public final class sweep_solid_evaluator_ui_main {

    private static final int PROFILE_RES = 32;
    private static final int PATH_STEPS = 16;

    private sweep_solid_evaluator_ui_main() {}

    public static TriangleMesh evaluate(shape_item_ui_main host, sweep_feature_ui_main sweep) {
        if (sweep == null || !sweep.isValid()) return null;
        profile_reference_ui_main prof = sweep.getProfile();
        sweep_path_ui_main path = sweep.getPath();
        if (prof == null || path == null) return null;

        profile_loop_ui_main baseLoop = prof.getLoop().resample(PROFILE_RES);
        List<Point3D> basePts = baseLoop.getPoints();
        Point3D baseCenter = baseLoop.getCenter();

        List<Point3D> pathPts = path.samplePoints(PATH_STEPS);
        if (pathPts.size() < 2) return null;

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);

        List<List<Point3D>> rings = new ArrayList<>();
        Point3D p0 = pathPts.get(0);

        for (Point3D currPathPt : pathPts) {
            Point3D delta = currPathPt.subtract(p0);
            List<Point3D> ring = new ArrayList<>();
            for (Point3D bp : basePts) {
                ring.add(bp.add(delta));
            }
            rings.add(ring);
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

        for (int r = 0; r < rings.size() - 1; r++) {
            int base1 = ringBaseIndices.get(r);
            int base2 = ringBaseIndices.get(r + 1);
            loft_mesh_quad_helper_ui_main.addQuadStrip(mesh.getFaces(), base1, base2, PROFILE_RES);
        }

        if (sweep.isSolid()) {
            // Start Cap Center
            Point3D startCenter = baseCenter.add(pathPts.get(0).subtract(p0));
            int cStartIdx = vertexIndex++;
            mesh.getPoints().addAll((float) startCenter.getX(), (float) startCenter.getY(), (float) startCenter.getZ());
            loft_mesh_quad_helper_ui_main.addCapFan(mesh.getFaces(), cStartIdx, ringBaseIndices.get(0), PROFILE_RES, true);

            // End Cap Center
            Point3D endCenter = baseCenter.add(pathPts.get(pathPts.size() - 1).subtract(p0));
            int cEndIdx = vertexIndex++;
            mesh.getPoints().addAll((float) endCenter.getX(), (float) endCenter.getY(), (float) endCenter.getZ());
            loft_mesh_quad_helper_ui_main.addCapFan(mesh.getFaces(), cEndIdx, ringBaseIndices.get(rings.size() - 1), PROFILE_RES, false);
        }

        return mesh;
    }
}
