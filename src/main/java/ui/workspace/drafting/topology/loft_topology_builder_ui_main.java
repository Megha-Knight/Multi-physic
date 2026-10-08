package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.loft.loft_feature_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.List;

/**
 * loft_topology_builder_ui_main.java
 * Generates topology derived faces, start/end caps, and transition regions for Loft features.
 */
public final class loft_topology_builder_ui_main {

    private loft_topology_builder_ui_main() {}

    public static void populateLoftTopology(topology_body_ui_main body, shape_item_ui_main shape) {
        if (body == null || shape == null) return;

        for (loft_feature_ui_main l : shape.getLofts()) {
            if (!l.isValid()) continue;

            String rootId = body.getId() + ":F:LOFT:" + l.getId();
            topology_derived_face_ui_main rootDf = new topology_derived_face_ui_main(
                body.getId(), rootId, l.getName(),
                null, l.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                100.0, 100.0, 0.0, 0.0, new Point3D(0, 0, 0)
            );
            body.addDerivedFace(rootDf);

            List<profile_reference_ui_main> secs = l.getSections();
            if (secs.size() >= 2) {
                // Start Cap
                String startCapId = body.getId() + ":F:LOFT:" + l.getId() + ":START_CAP";
                topology_derived_face_ui_main startDf = new topology_derived_face_ui_main(
                    body.getId(), startCapId, "Loft Start Cap (" + secs.get(0).getName() + ")",
                    face_kind_ui_main.BOTTOM, l.getId(), hole_region_kind_ui_main.HOLE_FLOOR,
                    secs.get(0).getWidth(), secs.get(0).getHeight(), secs.get(0).getElevation(), secs.get(0).getRadius() > 0 ? secs.get(0).getRadius() : secs.get(0).getWidth() * 0.5, secs.get(0).getLoop().getCenter()
                );
                body.addDerivedFace(startDf);

                // End Cap
                String endCapId = body.getId() + ":F:LOFT:" + l.getId() + ":END_CAP";
                topology_derived_face_ui_main endDf = new topology_derived_face_ui_main(
                    body.getId(), endCapId, "Loft End Cap (" + secs.get(secs.size() - 1).getName() + ")",
                    face_kind_ui_main.TOP, l.getId(), hole_region_kind_ui_main.ENTRY_OPENING,
                    secs.get(secs.size() - 1).getWidth(), secs.get(secs.size() - 1).getHeight(), secs.get(secs.size() - 1).getElevation(), secs.get(secs.size() - 1).getRadius() > 0 ? secs.get(secs.size() - 1).getRadius() : secs.get(secs.size() - 1).getWidth() * 0.5, secs.get(secs.size() - 1).getLoop().getCenter()
                );
                body.addDerivedFace(endDf);

                // Side Faces between successive sections
                for (int s = 0; s < secs.size() - 1; s++) {
                    String sideId = body.getId() + ":F:LOFT:" + l.getId() + ":SIDE:" + s;
                    double h = Math.abs(secs.get(s + 1).getElevation() - secs.get(s).getElevation());
                    topology_derived_face_ui_main sideDf = new topology_derived_face_ui_main(
                        body.getId(), sideId, "Loft Transition Face " + (s + 1),
                        null, l.getId(), hole_region_kind_ui_main.HOLE_WALL,
                        secs.get(s).getWidth(), secs.get(s + 1).getWidth(), h, (secs.get(s).getWidth() + secs.get(s + 1).getWidth()) * 0.25, new Point3D(0, 0, 0)
                    );
                    body.addDerivedFace(sideDf);
                }
            }
        }
    }
}
