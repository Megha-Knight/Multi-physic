package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_geometry_helper_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.holes.hole_mesh_builder_ui_main;

public final class derived_edge_helper_ui_main {

    private derived_edge_helper_ui_main() {}

    public static void registerHoleRims(topology_body_ui_main body, shape_item_ui_main shape,
                                        face_kind_ui_main kind, hole_feature_ui_main h) {
        if (body == null || shape == null || kind == null || h == null) return;
        Point3D entryCenter;
        var entryInfo = h.isOnFeature() ? null : face_geometry_helper_ui_main.getFaceInfo(shape, h.getFaceKind());
        if (h.isOnFeature()) {
            extrude_feature_ui_main ext = shape.getExtrusions().stream().filter(e -> e.getId().equals(h.getTargetFeatureId())).findFirst().orElse(null);
            if (ext != null) {
                var fInfo = face_geometry_helper_ui_main.getExtrudeFaceInfo(shape, ext, h.getFaceKind());
                entryCenter = fInfo.origin().add(fInfo.uAxis().multiply(h.getU())).add(fInfo.vAxis().multiply(h.getV()));
            } else entryCenter = Point3D.ZERO;
        } else {
            entryCenter = entryInfo.origin().add(entryInfo.uAxis().multiply(h.getU())).add(entryInfo.vAxis().multiply(h.getV()));
        }

        if (kind == h.getFaceKind() || (h.isOnFeature() && (kind == h.getFaceKind() || kind == face_kind_ui_main.EXTRUDE_CAP))) {
            // Entry face rim
            String entryName = "HOLE_RIM_" + h.getId();
            topology_edge_ui_main entryRim = new topology_edge_ui_main(
                body.getId(), entryName, null, null,
                entryCenter, entryCenter, false, h.getDiameter(), h.getId()
            );
            String targetFaceKind = (h.getFaceKind() == face_kind_ui_main.EXTRUDE_CAP || !h.getFaceKind().isExtrudeSide()) ? "EXTRUDE_CAP" : h.getFaceKind().name();
            String remId = h.isOnFeature() ? (body.getId() + ":F:" + targetFaceKind + ":" + h.getTargetFeatureId()) : (body.getId() + ":F:" + kind.name() + ":REMAINING");
            String wallId = h.isOnFeature() ? (remId + ":HOLE_WALL:" + h.getId()) : (body.getId() + ":F:" + kind.name() + (h.isThroughAll() ? ":BORE_WALL:" : ":HOLE_WALL:") + h.getId());
            entryRim.addAdjacentFace(remId);
            entryRim.addAdjacentFace(wallId);
            body.addEdge(entryRim);

            var remFace = body.getDerivedFaceById(remId);
            if (remFace != null) remFace.addEdge(entryRim.getId());
            var wallFace = body.getDerivedFaceById(wallId);
            if (wallFace != null) wallFace.addEdge(entryRim.getId());
        } else if (h.isThroughAll() && entryInfo != null) {
            // Exit face rim: borehole drills straight through in direction -entryNormal
            Point3D exitCenter = entryCenter.subtract(entryInfo.normal().multiply(entryInfo.thickness()));
            String exitName = "HOLE_EXIT_RIM_" + h.getId();
            topology_edge_ui_main exitRim = new topology_edge_ui_main(
                body.getId(), exitName, null, null,
                exitCenter, exitCenter, false, h.getDiameter(), h.getId()
            );
            String exitRemId = body.getId() + ":F:" + kind.name() + ":REMAINING";
            String exitWallId = body.getId() + ":F:" + kind.name() + ":BORE_WALL:" + h.getId();
            exitRim.addAdjacentFace(exitRemId);
            exitRim.addAdjacentFace(exitWallId);
            body.addEdge(exitRim);

            var exitRem = body.getDerivedFaceById(exitRemId);
            if (exitRem != null) exitRem.addEdge(exitRim.getId());
            var exitWall = body.getDerivedFaceById(exitWallId);
            if (exitWall != null) exitWall.addEdge(exitRim.getId());
        }
    }

    public static void registerExtrusionEdges(topology_body_ui_main body, shape_item_ui_main shape,
                                              extrude_feature_ui_main ext) {
        if (body == null || shape == null || ext == null) return;
        var info = face_geometry_helper_ui_main.getFaceInfo(shape, ext.getFaceKind());
        Point3D baseCenter = info.origin().add(info.uAxis().multiply(ext.getU())).add(info.vAxis().multiply(ext.getV()));
        Point3D capCenter = baseCenter.add(info.normal().multiply(ext.getHeight()));

        String baseName = "EXTRUDE_BASE_RIM_" + ext.getId();
        topology_edge_ui_main baseRim = new topology_edge_ui_main(
            body.getId(), baseName, null, null,
            baseCenter, baseCenter, false, ext.getDiameter(), ext.getId()
        );
        String hostFaceId = body.getId() + ":F:" + ext.getFaceKind().name();
        String latFaceId = body.getId() + ":F:EXTRUDE_LATERAL:" + ext.getId();
        String capFaceId = body.getId() + ":F:EXTRUDE_CAP:" + ext.getId();

        baseRim.addAdjacentFace(hostFaceId);
        baseRim.addAdjacentFace(latFaceId);
        body.addEdge(baseRim);

        String capName = "EXTRUDE_CAP_RIM_" + ext.getId();
        topology_edge_ui_main capRim = new topology_edge_ui_main(
            body.getId(), capName, null, null,
            capCenter, capCenter, false, ext.getDiameter(), ext.getId()
        );
        capRim.addAdjacentFace(capFaceId);
        capRim.addAdjacentFace(latFaceId);
        body.addEdge(capRim);
    }
}
