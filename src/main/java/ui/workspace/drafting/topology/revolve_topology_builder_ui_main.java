package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.revolve.revolve_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

/**
 * revolve_topology_builder_ui_main.java
 * Generates topology derived faces, rotational side walls, and sector start/end caps for Revolve features.
 */
public final class revolve_topology_builder_ui_main {

    private revolve_topology_builder_ui_main() {}

    public static void populateRevolveTopology(topology_body_ui_main body, shape_item_ui_main shape) {
        if (body == null || shape == null) return;

        for (revolve_feature_ui_main r : shape.getRevolves()) {
            if (!r.isValid()) continue;

            String rootId = body.getId() + ":F:REVOLVE:" + r.getId();
            topology_derived_face_ui_main rootDf = new topology_derived_face_ui_main(
                body.getId(), rootId, r.getName(),
                null, r.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                100.0, 100.0, r.getAngle(), 0.0, new Point3D(0, 0, 0)
            );
            body.addDerivedFace(rootDf);

            profile_reference_ui_main prof = r.getProfile();
            if (prof != null && prof.getLoop() != null) {
                double rad = prof.getRadius() > 0 ? prof.getRadius() : prof.getWidth() * 0.5;

                // Rotational Side Surface
                String sideId = body.getId() + ":F:REVOLVE:" + r.getId() + ":SIDE";
                topology_derived_face_ui_main sideDf = new topology_derived_face_ui_main(
                    body.getId(), sideId, "Revolved Lateral Surface",
                    null, r.getId(), hole_region_kind_ui_main.HOLE_WALL,
                    prof.getWidth(), prof.getHeight(), r.getAngle(), rad, new Point3D(0, 0, 0)
                );
                body.addDerivedFace(sideDf);

                boolean isFull360 = Math.abs(r.getAngle()) >= 360.0 - 1e-3;
                if (!isFull360 && r.isSolid()) {
                    // Start Cap (at 0 deg)
                    String startCapId = body.getId() + ":F:REVOLVE:" + r.getId() + ":START_CAP";
                    topology_derived_face_ui_main startDf = new topology_derived_face_ui_main(
                        body.getId(), startCapId, "Revolve Start Cap (" + prof.getName() + ")",
                        face_kind_ui_main.BOTTOM, r.getId(), hole_region_kind_ui_main.HOLE_FLOOR,
                        prof.getWidth(), prof.getHeight(), 0.0, rad, prof.getLoop().getCenter()
                    );
                    body.addDerivedFace(startDf);

                    // End Cap (at angle deg)
                    String endCapId = body.getId() + ":F:REVOLVE:" + r.getId() + ":END_CAP";
                    Point3D endCenter = r.getAxis().rotatePoint(prof.getLoop().getCenter(), r.getAngle());
                    topology_derived_face_ui_main endDf = new topology_derived_face_ui_main(
                        body.getId(), endCapId, "Revolve End Cap (" + prof.getName() + ")",
                        face_kind_ui_main.TOP, r.getId(), hole_region_kind_ui_main.ENTRY_OPENING,
                        prof.getWidth(), prof.getHeight(), r.getAngle(), rad, endCenter
                    );
                    body.addDerivedFace(endDf);
                }
            }
        }
    }
}
