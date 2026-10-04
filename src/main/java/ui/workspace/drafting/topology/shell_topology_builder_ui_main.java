package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shell.shell_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.List;

/**
 * shell_topology_builder_ui_main.java
 * Generates topology derived faces, inner offset regions, and wall connectors for Shell features.
 */
public final class shell_topology_builder_ui_main {

    private static final List<face_kind_ui_main> BOX_FACES = List.of(
        face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM,
        face_kind_ui_main.FRONT, face_kind_ui_main.BACK,
        face_kind_ui_main.LEFT, face_kind_ui_main.RIGHT
    );

    private shell_topology_builder_ui_main() {}

    public static void populateShellTopology(topology_body_ui_main body, shape_item_ui_main shape) {
        if (body == null || shape == null) return;

        for (shell_feature_ui_main sh : shape.getShells()) {
            if (!sh.isValid()) continue;

            String dirTag = sh.getDirection().name();
            double depthSign = sh.getDirection().isInward() ? 1.0 : -1.0;
            String rootId = body.getId() + ":F:SHELL:" + sh.getId() + ":" + dirTag;
            topology_derived_face_ui_main rootDf = new topology_derived_face_ui_main(
                body.getId(), rootId, sh.getName(),
                null, sh.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                sh.getThickness(), sh.getThickness() * depthSign, 0.0, 0.0, new Point3D(0, 0, 0)
            );
            body.addDerivedFace(rootDf);

            for (face_kind_ui_main k : BOX_FACES) {
                if (sh.hasRemovedFace(k)) {
                    // Open Face Opening
                    String openId = body.getId() + ":F:SHELL:" + sh.getId() + ":" + dirTag + ":OPEN:" + k.name();
                    topology_derived_face_ui_main openDf = new topology_derived_face_ui_main(
                        body.getId(), openId, "Shell Opening (" + k.getLabel() + ")",
                        k, sh.getId(), hole_region_kind_ui_main.ENTRY_OPENING,
                        20.0, 20.0, 0.0, 0.0, new Point3D(0, 0, 0)
                    );
                    body.addDerivedFace(openDf);

                    // Wall Connector
                    String wallId = body.getId() + ":F:SHELL:" + sh.getId() + ":" + dirTag + ":WALL:" + k.name();
                    topology_derived_face_ui_main wallDf = new topology_derived_face_ui_main(
                        body.getId(), wallId, "Shell Wall (" + k.getLabel() + ")",
                        k, sh.getId(), hole_region_kind_ui_main.HOLE_WALL,
                        sh.getThickness(), 20.0 * depthSign, sh.getThickness(), sh.getThickness(), new Point3D(0, 0, 0)
                    );
                    body.addDerivedFace(wallDf);
                } else {
                    // Outer Retained Face
                    String outId = body.getId() + ":F:SHELL:" + sh.getId() + ":" + dirTag + ":OUTER:" + k.name();
                    topology_derived_face_ui_main outDf = new topology_derived_face_ui_main(
                        body.getId(), outId, "Shell Outer (" + k.getLabel() + ")",
                        k, sh.getId(), hole_region_kind_ui_main.REMAINING_FACE,
                        20.0, 20.0, 0.0, 0.0, new Point3D(0, 0, 0)
                    );
                    body.addDerivedFace(outDf);

                    // Inner Offset Face
                    String inId = body.getId() + ":F:SHELL:" + sh.getId() + ":" + dirTag + ":INNER:" + k.name();
                    topology_derived_face_ui_main inDf = new topology_derived_face_ui_main(
                        body.getId(), inId, "Shell Inner (" + k.getLabel() + ")",
                        k, sh.getId(), (k == face_kind_ui_main.BOTTOM ? hole_region_kind_ui_main.HOLE_FLOOR : hole_region_kind_ui_main.HOLE_WALL),
                        20.0, 20.0, sh.getThickness() * depthSign, sh.getThickness(), new Point3D(0, 0, 0)
                    );
                    body.addDerivedFace(inDf);
                }
            }
        }
    }
}
