package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.faces.face_geometry_helper_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

public final class extrude_topology_builder_ui_main {

    private extrude_topology_builder_ui_main() {}

    public static void populateExtrudeTopology(topology_body_ui_main body, shape_item_ui_main shape) {
        if (body == null || shape == null || !shape.hasExtrusions()) return;

        for (extrude_feature_ui_main ext : shape.getExtrusions()) {
            if (!ext.isValid() || ext.getState() == ui.workspace.drafting.features.feature_state_ui_main.INVALID) continue;

            var info = ui.workspace.drafting.faces.face_geometry_helper_ui_main.getFaceInfo(shape, ext.getFaceKind());
            Point3D baseCenter = info.origin().add(info.uAxis().multiply(ext.getU())).add(info.vAxis().multiply(ext.getV()));
            Point3D capCenter = baseCenter.add(info.normal().multiply(ext.getHeight()));
            Point3D latCenter = baseCenter.add(info.normal().multiply(ext.getHeight() * 0.5));
            double w = ext.getDiameter(), h = (ext.getWidth2() > 0.1 ? ext.getWidth2() : ext.getDiameter());

            String capId = body.getId() + ":F:EXTRUDE_CAP:" + ext.getId();
            topology_derived_face_ui_main cap = new topology_derived_face_ui_main(
                body.getId(), capId, "Extrude Cap (" + ext.getName() + ")",
                ui.workspace.drafting.faces.face_kind_ui_main.EXTRUDE_CAP, ext.getId(), hole_region_kind_ui_main.HOLE_FLOOR,
                w, h, ext.getHeight(), ext.getRadius(), capCenter
            );
            body.addDerivedFace(cap);

            String latId = body.getId() + ":F:EXTRUDE_LATERAL:" + ext.getId();
            topology_derived_face_ui_main lat = new topology_derived_face_ui_main(
                body.getId(), latId, "Extrude Side (" + ext.getName() + ")",
                ui.workspace.drafting.faces.face_kind_ui_main.EXTRUDE_LATERAL, ext.getId(), hole_region_kind_ui_main.HOLE_WALL,
                Math.PI * ext.getDiameter(), ext.getHeight(), ext.getHeight(), ext.getRadius(),
                latCenter
            );
            body.addDerivedFace(lat);

            if (ext.getProfileShape() != ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE) {
                ui.workspace.drafting.faces.face_kind_ui_main[] sKinds = new ui.workspace.drafting.faces.face_kind_ui_main[]{
                    ui.workspace.drafting.faces.face_kind_ui_main.EXTRUDE_SIDE_POS_U, ui.workspace.drafting.faces.face_kind_ui_main.EXTRUDE_SIDE_NEG_U,
                    ui.workspace.drafting.faces.face_kind_ui_main.EXTRUDE_SIDE_POS_V, ui.workspace.drafting.faces.face_kind_ui_main.EXTRUDE_SIDE_NEG_V
                };
                for (var sk : sKinds) {
                    var sInfo = ui.workspace.drafting.faces.face_geometry_helper_ui_main.getExtrudeFaceInfo(shape, ext, sk);
                    String sId = body.getId() + ":F:" + sk.name() + ":" + ext.getId();
                    body.addDerivedFace(new topology_derived_face_ui_main(
                        body.getId(), sId, "Extrude Side (" + sk.getLabel() + ")",
                        sk, ext.getId(), hole_region_kind_ui_main.HOLE_WALL,
                        sInfo.width(), sInfo.height(), ext.getHeight(), 0.0, sInfo.origin()
                    ));
                }
            }

            derived_edge_helper_ui_main.registerExtrusionEdges(body, shape, ext);
            for (var hole : shape.getEffectiveHolesForFeature(ext.getId())) {
                if (hole.isValid() && hole.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                    var hInfo = face_geometry_helper_ui_main.getExtrudeFaceInfo(shape, ext, hole.getFaceKind());
                    String targetFaceKind = (hole.getFaceKind() == face_kind_ui_main.EXTRUDE_CAP || !hole.getFaceKind().isExtrudeSide()) ? "EXTRUDE_CAP" : hole.getFaceKind().name();
                    String wallId = body.getId() + ":F:" + targetFaceKind + ":" + ext.getId() + ":HOLE_WALL:" + hole.getId();
                    Point3D hCenter = hInfo.origin().add(hInfo.uAxis().multiply(hole.getU())).add(hInfo.vAxis().multiply(hole.getV()));
                    topology_derived_face_ui_main wallFace = new topology_derived_face_ui_main(
                        body.getId(), wallId, "Hole Wall (" + hole.getName() + ")",
                        hole.getFaceKind(), hole.getId(), hole_region_kind_ui_main.HOLE_WALL,
                        Math.PI * hole.getDiameter(), hole.getDepth(), hole.getDepth(), hole.getRadius(), hCenter
                    );
                    body.addDerivedFace(wallFace);
                    derived_edge_helper_ui_main.registerHoleRims(body, shape, hole.getFaceKind(), hole);
                }
            }
        }
    }
}
