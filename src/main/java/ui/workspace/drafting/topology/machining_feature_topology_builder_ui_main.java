package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

/**
 * machining_feature_topology_builder_ui_main.java
 * Generates topology derived faces for Chamfer, Fillet, and Draft operations.
 */
public final class machining_feature_topology_builder_ui_main {

    private machining_feature_topology_builder_ui_main() {}

    public static void populateMachiningTopology(topology_body_ui_main body, shape_item_ui_main shape) {
        if (body == null || shape == null) return;

        for (chamfer_feature_ui_main ch : shape.getChamfers()) {
            if (!ch.isValid()) continue;
            String fId = body.getId() + ":F:CHAMFER:" + ch.getId();
            topology_derived_face_ui_main df = new topology_derived_face_ui_main(
                body.getId(), fId, "Chamfer Face (" + ch.getName() + ")",
                null, ch.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                ch.getDistance() * Math.sqrt(2), 20.0, ch.getDistance(), ch.getDistance(), new Point3D(0, 0, 0)
            );
            body.addDerivedFace(df);
        }

        for (fillet_feature_ui_main fl : shape.getFillets()) {
            if (!fl.isValid()) continue;
            String fId = body.getId() + ":F:FILLET:" + fl.getId();
            topology_derived_face_ui_main df = new topology_derived_face_ui_main(
                body.getId(), fId, "Fillet Face (" + fl.getName() + ")",
                null, fl.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                fl.getRadius() * Math.PI * 0.5, 20.0, fl.getRadius(), fl.getRadius(), new Point3D(0, 0, 0)
            );
            body.addDerivedFace(df);
        }

        for (draft_feature_ui_main dr : shape.getDrafts()) {
            if (!dr.isValid()) continue;
            String fId = body.getId() + ":F:DRAFT:" + dr.getId();
            topology_derived_face_ui_main df = new topology_derived_face_ui_main(
                body.getId(), fId, "Draft Face (" + dr.getName() + ")",
                dr.getFaceKind(), dr.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                20.0, 20.0, 0.0, 0.0, new Point3D(0, 0, 0)
            );
            body.addDerivedFace(df);
        }
    }
}
