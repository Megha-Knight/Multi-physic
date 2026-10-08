package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sweep.sweep_feature_ui_main;
import ui.workspace.drafting.sweep.sweep_path_ui_main;

/**
 * sweep_topology_builder_ui_main.java
 * Generates topology derived faces, start/end caps, and trajectory surfaces for Sweep features.
 */
public final class sweep_topology_builder_ui_main {

    private sweep_topology_builder_ui_main() {}

    public static void populateSweepTopology(topology_body_ui_main body, shape_item_ui_main shape) {
        if (body == null || shape == null) return;

        for (sweep_feature_ui_main s : shape.getSweeps()) {
            if (!s.isValid()) continue;

            String rootId = body.getId() + ":F:SWEEP:" + s.getId();
            topology_derived_face_ui_main rootDf = new topology_derived_face_ui_main(
                body.getId(), rootId, s.getName(),
                null, s.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                100.0, 100.0, 0.0, 0.0, new Point3D(0, 0, 0)
            );
            body.addDerivedFace(rootDf);

            profile_reference_ui_main prof = s.getProfile();
            sweep_path_ui_main path = s.getPath();

            if (prof != null && path != null) {
                // Start Cap
                String startCapId = body.getId() + ":F:SWEEP:" + s.getId() + ":START_CAP";
                topology_derived_face_ui_main startDf = new topology_derived_face_ui_main(
                    body.getId(), startCapId, "Sweep Start Cap (" + prof.getName() + ")",
                    face_kind_ui_main.BOTTOM, s.getId(), hole_region_kind_ui_main.HOLE_FLOOR,
                    prof.getWidth(), prof.getHeight(), 0.0, 0.0, path.getWaypoints().get(0)
                );
                body.addDerivedFace(startDf);

                // End Cap
                String endCapId = body.getId() + ":F:SWEEP:" + s.getId() + ":END_CAP";
                topology_derived_face_ui_main endDf = new topology_derived_face_ui_main(
                    body.getId(), endCapId, "Sweep End Cap (" + prof.getName() + ")",
                    face_kind_ui_main.TOP, s.getId(), hole_region_kind_ui_main.ENTRY_OPENING,
                    prof.getWidth(), prof.getHeight(), 0.0, 0.0, path.getWaypoints().get(path.getWaypointCount() - 1)
                );
                body.addDerivedFace(endDf);

                // Side Surface
                String sideId = body.getId() + ":F:SWEEP:" + s.getId() + ":SIDE";
                topology_derived_face_ui_main sideDf = new topology_derived_face_ui_main(
                    body.getId(), sideId, "Swept Lateral Surface",
                    null, s.getId(), hole_region_kind_ui_main.HOLE_WALL,
                    prof.getWidth(), path.getLength(), 0.0, 0.0, new Point3D(0, 0, 0)
                );
                body.addDerivedFace(sideDf);
            }
        }
    }
}
