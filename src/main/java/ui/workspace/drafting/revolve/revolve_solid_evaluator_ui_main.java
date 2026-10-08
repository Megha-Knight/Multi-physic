package ui.workspace.drafting.revolve;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.profiles.profile_loop_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.List;

/**
 * revolve_solid_evaluator_ui_main.java
 * Evaluates 3D rotational solid geometry by revolving 2D profile loops around an axis.
 */
public final class revolve_solid_evaluator_ui_main {

    private revolve_solid_evaluator_ui_main() {}

    public static TriangleMesh evaluate(shape_item_ui_main host, revolve_feature_ui_main feature) {
        if (feature == null || !feature.isValid()) return null;
        profile_reference_ui_main prof = feature.getProfile();
        revolve_axis_ui_main axis = feature.getAxis();
        if (prof == null || prof.getLoop() == null || axis == null) return null;

        profile_loop_ui_main loop = prof.getLoop();
        List<Point3D> basePts = loop.getPoints();
        int pointCount = basePts.size();
        if (pointCount < 3) return null;

        double totalAngle = feature.getAngle();
        boolean isFull360 = Math.abs(totalAngle) >= 360.0 - 1e-3;
        double clampedAngle = isFull360 ? 360.0 : Math.min(360.0, Math.abs(totalAngle));

        double startAngle = 0.0, endAngle = clampedAngle;
        if (feature.getDirection() == revolve_direction_ui_main.REVERSE) {
            startAngle = 0.0;
            endAngle = -clampedAngle;
        } else if (feature.getDirection() == revolve_direction_ui_main.SYMMETRIC) {
            startAngle = -clampedAngle * 0.5;
            endAngle = clampedAngle * 0.5;
        }

        int steps = Math.max(4, (int) Math.ceil((clampedAngle / 360.0) * 24.0));
        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);

        // Generate angular rings
        int totalRings = isFull360 ? steps : (steps + 1);
        for (int s = 0; s < totalRings; s++) {
            double frac = (double) s / (double) steps;
            double ang = startAngle + frac * (endAngle - startAngle);
            for (Point3D p : basePts) {
                Point3D rotated = axis.rotatePoint(p, ang);
                mesh.getPoints().addAll((float) rotated.getX(), (float) rotated.getY(), (float) rotated.getZ());
            }
        }

        // Connect rings with side quads
        for (int s = 0; s < steps; s++) {
            int ring1 = s * pointCount;
            int ring2 = ((s + 1) % (isFull360 ? steps : (steps + 1))) * pointCount;
            revolve_mesh_quad_helper_ui_main.addQuadStrip(mesh.getFaces(), ring1, ring2, pointCount);
        }

        // Add start and end caps for partial revolutions
        if (!isFull360 && feature.isSolid()) {
            // Start cap center & fan
            Point3D c0 = axis.rotatePoint(loop.getCenter(), startAngle);
            int startCenterIdx = mesh.getPoints().size() / 3;
            mesh.getPoints().addAll((float) c0.getX(), (float) c0.getY(), (float) c0.getZ());
            revolve_mesh_quad_helper_ui_main.addCapFan(mesh.getFaces(), startCenterIdx, 0, pointCount, true);

            // End cap center & fan
            Point3D c1 = axis.rotatePoint(loop.getCenter(), endAngle);
            int endCenterIdx = mesh.getPoints().size() / 3;
            mesh.getPoints().addAll((float) c1.getX(), (float) c1.getY(), (float) c1.getZ());
            int endRingBase = steps * pointCount;
            revolve_mesh_quad_helper_ui_main.addCapFan(mesh.getFaces(), endCenterIdx, endRingBase, pointCount, false);
        }

        return mesh;
    }
}
