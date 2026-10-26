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
        double len = ext.getHeight(), w = ext.getDiameter(), d = (ext.getWidth2() > 0.1) ? ext.getWidth2() : w;
        String hostF = body.getId() + ":F:" + ext.getFaceKind().name();
        String latF = body.getId() + ":F:EXTRUDE_LATERAL:" + ext.getId();
        String capF = body.getId() + ":F:EXTRUDE_CAP:" + ext.getId();

        var ps = ext.getProfileShape();
        if (ps == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE) {
            linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_BASE_RIM_" + ext.getId(), null, null, baseCenter, baseCenter, false, ext.getDiameter(), ext.getId()), hostF, latF);
            linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_CAP_RIM_" + ext.getId(), null, null, capCenter, capCenter, false, ext.getDiameter(), ext.getId()), capF, latF);
        } else if (ps == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RECTANGLE || ps == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.SQUARE) {
            registerRectangularEdges(body, ext, info, baseCenter, len, w, d, hostF, capF);
        } else if (ps == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.EQUILATERAL_TRIANGLE || ps == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RIGHT_TRIANGLE) {
            registerTriangularEdges(body, ext, info, baseCenter, len, w, d, hostF, capF);
        }
    }

    private static void registerRectangularEdges(topology_body_ui_main body, extrude_feature_ui_main ext,
                                                face_geometry_helper_ui_main.FaceInfo info, Point3D baseCenter,
                                                double len, double w, double d, String hostF, String capF) {
        double hw = w * 0.5, hd = d * 0.5;
        Point3D u = info.uAxis(), v = info.vAxis(), n = info.normal();
        Point3D bNN = baseCenter.add(u.multiply(-hw)).add(v.multiply(-hd)), bPN = baseCenter.add(u.multiply(hw)).add(v.multiply(-hd));
        Point3D bPP = baseCenter.add(u.multiply(hw)).add(v.multiply(hd)), bNP = baseCenter.add(u.multiply(-hw)).add(v.multiply(hd));
        Point3D tNN = bNN.add(n.multiply(len)), tPN = bPN.add(n.multiply(len)), tPP = bPP.add(n.multiply(len)), tNP = bNP.add(n.multiply(len));

        String sPosU = body.getId() + ":F:EXTRUDE_SIDE_POS_U:" + ext.getId(), sNegU = body.getId() + ":F:EXTRUDE_SIDE_NEG_U:" + ext.getId();
        String sPosV = body.getId() + ":F:EXTRUDE_SIDE_POS_V:" + ext.getId(), sNegV = body.getId() + ":F:EXTRUDE_SIDE_NEG_V:" + ext.getId();

        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_BASE_NEG_V_" + ext.getId(), null, null, bNN, bPN, true, w, ext.getId()), hostF, sNegV);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_BASE_POS_V_" + ext.getId(), null, null, bNP, bPP, true, w, ext.getId()), hostF, sPosV);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_BASE_NEG_U_" + ext.getId(), null, null, bNN, bNP, true, d, ext.getId()), hostF, sNegU);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_BASE_POS_U_" + ext.getId(), null, null, bPN, bPP, true, d, ext.getId()), hostF, sPosU);

        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_CAP_NEG_V_" + ext.getId(), null, null, tNN, tPN, true, w, ext.getId()), capF, sNegV);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_CAP_POS_V_" + ext.getId(), null, null, tNP, tPP, true, w, ext.getId()), capF, sPosV);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_CAP_NEG_U_" + ext.getId(), null, null, tNN, tNP, true, d, ext.getId()), capF, sNegU);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_CAP_POS_U_" + ext.getId(), null, null, tPN, tPP, true, d, ext.getId()), capF, sPosU);

        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_LATERAL_NEG_U_NEG_V_" + ext.getId(), null, null, bNN, tNN, true, len, ext.getId()), sNegU, sNegV);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_LATERAL_POS_U_NEG_V_" + ext.getId(), null, null, bPN, tPN, true, len, ext.getId()), sPosU, sNegV);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_LATERAL_POS_U_POS_V_" + ext.getId(), null, null, bPP, tPP, true, len, ext.getId()), sPosU, sPosV);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_LATERAL_NEG_U_POS_V_" + ext.getId(), null, null, bNP, tNP, true, len, ext.getId()), sNegU, sPosV);
    }

    private static void registerTriangularEdges(topology_body_ui_main body, extrude_feature_ui_main ext,
                                               face_geometry_helper_ui_main.FaceInfo info, Point3D baseCenter,
                                               double len, double w, double d, String hostF, String capF) {
        javafx.geometry.Point2D[] p2 = new javafx.geometry.Point2D[3];
        if (ext.getProfileShape() == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RIGHT_TRIANGLE) {
            p2[0] = new javafx.geometry.Point2D(-w * 0.5, -d * 0.5); p2[1] = new javafx.geometry.Point2D(w * 0.5, -d * 0.5); p2[2] = new javafx.geometry.Point2D(-w * 0.5, d * 0.5);
        } else {
            double ht = w * Math.sqrt(3.0) * 0.5;
            p2[0] = new javafx.geometry.Point2D(-w * 0.5, -ht / 3.0); p2[1] = new javafx.geometry.Point2D(w * 0.5, -ht / 3.0); p2[2] = new javafx.geometry.Point2D(0.0, 2.0 * ht / 3.0);
        }
        Point3D u = info.uAxis(), v = info.vAxis(), n = info.normal();
        Point3D[] b = new Point3D[3], t = new Point3D[3];
        for (int i = 0; i < 3; i++) {
            b[i] = baseCenter.add(u.multiply(p2[i].getX())).add(v.multiply(p2[i].getY()));
            t[i] = b[i].add(n.multiply(len));
        }
        String sE01 = body.getId() + ":F:EXTRUDE_SIDE_E01:" + ext.getId();
        String sE12 = body.getId() + ":F:EXTRUDE_SIDE_E12:" + ext.getId();
        String sE20 = body.getId() + ":F:EXTRUDE_SIDE_E20:" + ext.getId();

        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_BASE_E01_" + ext.getId(), null, null, b[0], b[1], true, b[0].distance(b[1]), ext.getId()), hostF, sE01);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_BASE_E12_" + ext.getId(), null, null, b[1], b[2], true, b[1].distance(b[2]), ext.getId()), hostF, sE12);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_BASE_E20_" + ext.getId(), null, null, b[2], b[0], true, b[2].distance(b[0]), ext.getId()), hostF, sE20);

        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_CAP_E01_" + ext.getId(), null, null, t[0], t[1], true, t[0].distance(t[1]), ext.getId()), capF, sE01);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_CAP_E12_" + ext.getId(), null, null, t[1], t[2], true, t[1].distance(t[2]), ext.getId()), capF, sE12);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_CAP_E20_" + ext.getId(), null, null, t[2], t[0], true, t[2].distance(t[0]), ext.getId()), capF, sE20);

        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_LATERAL_V0_" + ext.getId(), null, null, b[0], t[0], true, len, ext.getId()), sE20, sE01);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_LATERAL_V1_" + ext.getId(), null, null, b[1], t[1], true, len, ext.getId()), sE01, sE12);
        linkEdge(body, new topology_edge_ui_main(body.getId(), "EXTRUDE_LATERAL_V2_" + ext.getId(), null, null, b[2], t[2], true, len, ext.getId()), sE12, sE20);
    }

    private static void linkEdge(topology_body_ui_main body, topology_edge_ui_main edge, String f1Id, String f2Id) {
        edge.addAdjacentFace(f1Id);
        edge.addAdjacentFace(f2Id);
        body.addEdge(edge);
        var f1 = body.getFaceById(f1Id); if (f1 != null) f1.addEdge(edge.getId());
        var f2 = body.getFaceById(f2Id); if (f2 != null) f2.addEdge(edge.getId());
    }
}
