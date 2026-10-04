package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.booleans.boolean_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

/**
 * boolean_topology_builder_ui_main.java
 * Generates topology derived faces and cavity regions for Boolean solid operations.
 */
public final class boolean_topology_builder_ui_main {

    private boolean_topology_builder_ui_main() {}

    public static void populateBooleanTopology(topology_body_ui_main body, shape_item_ui_main shape) {
        if (body == null || shape == null) return;

        for (boolean_feature_ui_main b : shape.getBooleans()) {
            if (!b.isValid()) continue;
            String fId = body.getId() + ":F:BOOLEAN:" + b.getId();
            topology_derived_face_ui_main df = new topology_derived_face_ui_main(
                body.getId(), fId, b.getName(),
                null, b.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                20.0, 20.0, 0.0, 0.0, new Point3D(0, 0, 0)
            );
            body.addDerivedFace(df);

            // Operation-specific derived face
            String subId = switch (b.getOpType()) {
                case SUBTRACT  -> body.getId() + ":F:BOOLEAN:" + b.getId() + ":CAVITY";
                case UNION     -> body.getId() + ":F:BOOLEAN:" + b.getId() + ":UNION";
                case INTERSECT -> body.getId() + ":F:BOOLEAN:" + b.getId() + ":INTERSECT";
            };
            topology_derived_face_ui_main opFace = new topology_derived_face_ui_main(
                body.getId(), subId, b.getOpType().getLabel() + " Region (" + b.getName() + ")",
                null, b.getId(), hole_region_kind_ui_main.HOLE_WALL,
                20.0, 20.0, 10.0, 5.0, new Point3D(0, 0, 0)
            );
            body.addDerivedFace(opFace);
        }
    }
}
