package ui.workspace.drafting.topology;

import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import java.util.Objects;

/**
 * edge_machining_analyzer_ui_main.java
 * Analytical boundary-aware reasoning for features near or crossing body edges.
 */
public final class edge_machining_analyzer_ui_main {

    public static final double TANGENT_TOLERANCE = 1e-4;

    private edge_machining_analyzer_ui_main() {}

    public static boundary_relationship_ui_main analyzeHole(shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole) {
        if (shape == null || hole == null) return new boundary_relationship_ui_main(boundary_condition_ui_main.CONTAINED, null, null, null, null, null, null, null, null, 0, 0, ui.workspace.drafting.features.feature_state_ui_main.INVALID, "Null shape or hole");
        face_kind_ui_main kind = hole.getFaceKind(); String srcFaceId = (body != null) ? body.getId() + ":F:" + kind.name() : shape.getId() + ":F:" + kind.name();
        var collision = checkIntersections(shape, hole, srcFaceId); if (collision != null) return collision;
        if (hole.isOnFeature()) return analyzeExtrudeHole(shape, body, hole, kind);
        if (kind.isCylinderCap() || kind == face_kind_ui_main.BASE_CAP) return analyzeCylinderCap(shape, body, hole, kind, srcFaceId);
        if (kind.isLateral()) return analyzeLateralFace(shape, body, hole, kind, srcFaceId);
        return analyzeBoxFace(shape, body, hole, kind, srcFaceId);
    }

    private static boundary_relationship_ui_main checkIntersections(shape_item_ui_main shape, hole_feature_ui_main hole, String srcFaceId) {
        for (hole_feature_ui_main other : shape.getHoles()) {
            if (other == hole || other.getId().equals(hole.getId())) continue;
            boolean sameTgt = Objects.equals(hole.getTargetFeatureId(), other.getTargetFeatureId());
            if (sameTgt && other.getFaceKind() == hole.getFaceKind()) {
                double dist = Math.hypot(hole.getU() - other.getU(), hole.getV() - other.getV()), req = hole.getRadius() + other.getRadius();
                if (dist < req - 1e-3) return boundary_relationship_ui_main.intersectingCavities(hole.getFaceKind(), srcFaceId, other.getId(), req - dist, String.format(java.util.Locale.US, "Feature intersects another cavity (%s). Intersecting holes detected. Multi-cavity intersection topology is not supported yet.", other.getName()));
            }
        }
        return null;
    }

    private static boundary_relationship_ui_main analyzeExtrudeHole(shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole, face_kind_ui_main kind) {
        String extId = hole.getTargetFeatureId(), bId = (body != null) ? body.getId() : shape.getId();
        var ext = shape.getExtrusions().stream().filter(e -> e.getId().equals(extId)).findFirst().orElse(null);
        if (ext == null) return boundary_relationship_ui_main.contained(kind, bId + ":F:EXTRUDE_CAP:" + extId, 0.0);
        if (kind == face_kind_ui_main.EXTRUDE_LATERAL) return boundary_relationship_ui_main.crossesEdge(kind, bId + ":F:EXTRUDE_LATERAL:" + extId, null, null, null, null, 0.0, "Machining on curved extrusion lateral surface is not supported.");
        double r = hole.getOuterRadius(), u = hole.getU(), v = hole.getV();
        if (kind.isExtrudeSide()) {
            var sInfo = ui.workspace.drafting.faces.face_geometry_helper_ui_main.getExtrudeFaceInfo(shape, ext, kind);
            double shw = sInfo.width() * 0.5, shh = sInfo.height() * 0.5, minDu = Math.min(u + shw, shw - u), minDv = Math.min(v + shh, shh - v);
            String sId = bId + ":F:" + kind.name() + ":" + extId;
            if (minDu < r - TANGENT_TOLERANCE || minDv < r - TANGENT_TOLERANCE) return boundary_relationship_ui_main.crossesEdge(kind, sId, null, "EXTRUDE_SIDE_EDGE", kind, null, Math.max(r - minDu, r - minDv), "Cutout crosses extrusion side boundary.");
            return boundary_relationship_ui_main.contained(kind, sId, Math.min(minDu, minDv) - r);
        }
        String srcFaceId = bId + ":F:EXTRUDE_CAP:" + extId;
        if (ext.getProfileShape() == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE) {
            String rimEdge = "EXTRUDE_CAP_RIM_" + extId; double capR = ext.getRadius(), dist = Math.hypot(u, v);
            if (dist + r > capR + TANGENT_TOLERANCE) return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, kind, bId + ":F:EXTRUDE_LATERAL:" + extId, dist + r - capR, "Cutout crosses extrusion rim.");
            return boundary_relationship_ui_main.contained(kind, srcFaceId, capR - dist - r);
        }
        if (ext.getProfileShape() == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.EQUILATERAL_TRIANGLE || ext.getProfileShape() == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RIGHT_TRIANGLE) {
            return analyzeTriangleExtrudeCap(ext, bId, kind, srcFaceId, u, v, r);
        }
        double hw = ext.getDiameter() * 0.5, hh = ((ext.getWidth2() > 0.1 ? ext.getWidth2() : ext.getDiameter())) * 0.5;
        double minDu = Math.min(u + hw, hw - u), minDv = Math.min(v + hh, hh - v), margin = Math.min(minDu, minDv);
        if (minDu < r - TANGENT_TOLERANCE || minDv < r - TANGENT_TOLERANCE) {
            boolean isU = (minDu < minDv);
            String eName = isU ? (u >= 0 ? "EXTRUDE_CAP_POS_U_" + extId : "EXTRUDE_CAP_NEG_U_" + extId) : (v >= 0 ? "EXTRUDE_CAP_POS_V_" + extId : "EXTRUDE_CAP_NEG_V_" + extId);
            String aFace = isU ? (u >= 0 ? bId + ":F:EXTRUDE_SIDE_POS_U:" + extId : bId + ":F:EXTRUDE_SIDE_NEG_U:" + extId) : (v >= 0 ? bId + ":F:EXTRUDE_SIDE_POS_V:" + extId : bId + ":F:EXTRUDE_SIDE_NEG_V:" + extId);
            return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + eName, eName, kind, aFace, Math.max(r - minDu, r - minDv), "Cutout crosses extrusion cap edge.");
        }
        return boundary_relationship_ui_main.contained(kind, srcFaceId, margin - r);
    }

    private static boundary_relationship_ui_main analyzeTriangleExtrudeCap(ui.workspace.drafting.extrude.extrude_feature_ui_main ext, String bId, face_kind_ui_main kind, String srcFaceId, double u, double v, double r) {
        double w = ext.getDiameter(), d = (ext.getWidth2() > 0.1 ? ext.getWidth2() : w);
        javafx.geometry.Point2D[] p2 = new javafx.geometry.Point2D[3];
        if (ext.getProfileShape() == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RIGHT_TRIANGLE) {
            p2[0] = new javafx.geometry.Point2D(-w * 0.5, -d * 0.5); p2[1] = new javafx.geometry.Point2D(w * 0.5, -d * 0.5); p2[2] = new javafx.geometry.Point2D(-w * 0.5, d * 0.5);
        } else {
            double ht = w * Math.sqrt(3.0) * 0.5;
            p2[0] = new javafx.geometry.Point2D(-w * 0.5, -ht / 3.0); p2[1] = new javafx.geometry.Point2D(w * 0.5, -ht / 3.0); p2[2] = new javafx.geometry.Point2D(0.0, 2.0 * ht / 3.0);
        }
        String[] eNames = {"EXTRUDE_CAP_E01_" + ext.getId(), "EXTRUDE_CAP_E12_" + ext.getId(), "EXTRUDE_CAP_E20_" + ext.getId()};
        String[] sFaces = {bId + ":F:EXTRUDE_SIDE_E01:" + ext.getId(), bId + ":F:EXTRUDE_SIDE_E12:" + ext.getId(), bId + ":F:EXTRUDE_SIDE_E20:" + ext.getId()};
        double minMargin = Double.MAX_VALUE; int bestIdx = 0;
        for (int i = 0; i < 3; i++) {
            javafx.geometry.Point2D pA = p2[i], pB = p2[(i + 1) % 3];
            double eu = pB.getX() - pA.getX(), ev = pB.getY() - pA.getY(), eLen = Math.hypot(eu, ev);
            double margin = ((u - pA.getX()) * (-ev) + (v - pA.getY()) * eu) / eLen;
            if (margin < minMargin) { minMargin = margin; bestIdx = i; }
        }
        if (minMargin < r - TANGENT_TOLERANCE) {
            return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + eNames[bestIdx], eNames[bestIdx], kind, sFaces[bestIdx], r - minMargin, "Cutout crosses triangular extrusion cap edge.");
        }
        return boundary_relationship_ui_main.contained(kind, srcFaceId, minMargin - r);
    }

    private static boundary_relationship_ui_main analyzeBoxFace(shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole, face_kind_ui_main kind, String srcFaceId) {
        var bounds = topology_geometry_helper_ui_main.getFaceBounds(shape, kind);
        double fw = bounds[0], fh = bounds[1], r = hole.getOuterRadius(), u = hole.getU(), v = hole.getV(), hw = fw * 0.5, hh = fh * 0.5;
        double minDu = Math.min(u + hw, hw - u), minDv = Math.min(v + hh, hh - v), margin = Math.min(minDu, minDv);
        boolean overU = (minDu < r - TANGENT_TOLERANCE), overV = (minDv < r - TANGENT_TOLERANCE), tanU = Math.abs(minDu - r) <= TANGENT_TOLERANCE, tanV = Math.abs(minDv - r) <= TANGENT_TOLERANCE;
        String bId = (body != null) ? body.getId() : shape.getId();
        if ((overU && overV) || (overU && tanV) || (tanU && overV)) {
            String vName = getCornerVertexName(kind, u >= 0, v >= 0), edgeName = getEdgeNameForDirection(kind, minDu < minDv, u >= 0, v >= 0);
            face_kind_ui_main adjKind = getAdjacentFaceForDirection(kind, minDu < minDv, u >= 0, v >= 0);
            return boundary_relationship_ui_main.crossesCorner(kind, srcFaceId, bId + ":V:" + vName, vName, bId + ":E:" + edgeName, edgeName, adjKind, bId + ":F:" + adjKind.name(), Math.max(r - minDu, r - minDv), "Feature crosses a body corner (" + vName + "). Hole intersects face boundary; multi-face hole topology is not supported yet.");
        }
        if (overU || overV) {
            String edgeName = getEdgeNameForDirection(kind, overU, u >= 0, v >= 0); face_kind_ui_main adjKind = getAdjacentFaceForDirection(kind, overU, u >= 0, v >= 0);
            return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + edgeName, edgeName, adjKind, bId + ":F:" + adjKind.name(), overU ? (r - minDu) : (r - minDv), "Feature crosses adjacent face boundary (" + adjKind.getLabel() + " via " + edgeName + "). Hole intersects face boundary; multi-face hole topology is not supported yet.");
        }
        if (tanU || tanV) {
            String edgeName = getEdgeNameForDirection(kind, tanU, u >= 0, v >= 0); face_kind_ui_main adjKind = getAdjacentFaceForDirection(kind, tanU, u >= 0, v >= 0);
            return boundary_relationship_ui_main.tangent(kind, srcFaceId, bId + ":E:" + edgeName, edgeName, adjKind, bId + ":F:" + adjKind.name(), margin, "Feature touches host-face boundary (" + edgeName + "). Hole intersects face boundary; multi-face hole topology is not supported yet.");
        }
        return boundary_relationship_ui_main.contained(kind, srcFaceId, margin - r);
    }

    private static boundary_relationship_ui_main analyzeLateralFace(shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole, face_kind_ui_main kind, String srcFaceId) {
        var bounds = topology_geometry_helper_ui_main.getFaceBounds(shape, kind);
        double hh = bounds[1] * 0.5, r = hole.getOuterRadius(), v = Math.abs(hole.getV()), margin = hh - v;
        String bId = (body != null) ? body.getId() : shape.getId(); boolean isTop = hole.getV() >= 0;
        String rimEdge = (kind == face_kind_ui_main.CONE_LATERAL) ? "BASE_RIM" : (isTop ? "TOP_RIM" : "BOTTOM_RIM");
        face_kind_ui_main adj = (kind == face_kind_ui_main.CONE_LATERAL) ? face_kind_ui_main.BASE_CAP : (isTop ? face_kind_ui_main.TOP_CAP : face_kind_ui_main.BOTTOM_CAP);
        if (v + r > hh + TANGENT_TOLERANCE) {
            return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, adj, bId + ":F:" + adj.name(), (v + r) - hh, "Feature crosses adjacent face boundary (" + adj.getLabel() + " via " + rimEdge + "). Hole intersects face boundary; multi-face hole topology is not supported yet.");
        }
        if (Math.abs(v + r - hh) <= TANGENT_TOLERANCE) {
            return boundary_relationship_ui_main.tangent(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, adj, bId + ":F:" + adj.name(), margin - r, "Feature touches host-face boundary (" + rimEdge + "). Hole intersects face boundary; multi-face hole topology is not supported yet.");
        }
        return boundary_relationship_ui_main.contained(kind, srcFaceId, margin - r);
    }

    private static boundary_relationship_ui_main analyzeCylinderCap(shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole, face_kind_ui_main kind, String srcFaceId) {
        var bounds = topology_geometry_helper_ui_main.getFaceBounds(shape, kind);
        double capRadius = bounds[0] * 0.5, r = hole.getOuterRadius(), distCenter = Math.hypot(hole.getU(), hole.getV()), margin = capRadius - distCenter;
        String bId = (body != null) ? body.getId() : shape.getId(), rimEdge = (kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.TOP) ? "TOP_RIM" : (kind == face_kind_ui_main.BASE_CAP ? "BASE_RIM" : "BOTTOM_RIM");
        face_kind_ui_main adj = (kind == face_kind_ui_main.BASE_CAP) ? face_kind_ui_main.CONE_LATERAL : face_kind_ui_main.CYLINDER_LATERAL;
        if (distCenter + r > capRadius + TANGENT_TOLERANCE) {
            return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, adj, bId + ":F:" + adj.name(), (distCenter + r) - capRadius, "Feature crosses adjacent face boundary (" + adj.getLabel() + " via " + rimEdge + "). Hole intersects face boundary; multi-face hole topology is not supported yet.");
        }
        if (Math.abs(distCenter + r - capRadius) <= TANGENT_TOLERANCE) {
            return boundary_relationship_ui_main.tangent(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, adj, bId + ":F:" + adj.name(), margin, "Feature touches host-face boundary (" + rimEdge + "). Hole intersects face boundary; multi-face hole topology is not supported yet.");
        }
        return boundary_relationship_ui_main.contained(kind, srcFaceId, margin - r);
    }

    public static String getEdgeNameForDirection(face_kind_ui_main f, boolean isU, boolean posU, boolean posV) {
        return switch (f) {
            case TOP -> isU ? (posU ? "TOP_RIGHT" : "TOP_LEFT") : (posV ? "TOP_FRONT" : "TOP_BACK");
            case BOTTOM -> isU ? (posU ? "BOTTOM_RIGHT" : "BOTTOM_LEFT") : (posV ? "BOTTOM_BACK" : "BOTTOM_FRONT");
            case FRONT -> isU ? (posU ? "FRONT_RIGHT" : "FRONT_LEFT") : (posV ? "BOTTOM_FRONT" : "TOP_FRONT");
            case BACK -> isU ? (posU ? "BACK_LEFT" : "BACK_RIGHT") : (posV ? "BOTTOM_BACK" : "TOP_BACK");
            case RIGHT -> isU ? (posU ? "BACK_RIGHT" : "FRONT_RIGHT") : (posV ? "BOTTOM_RIGHT" : "TOP_RIGHT");
            case LEFT -> isU ? (posU ? "FRONT_LEFT" : "BACK_LEFT") : (posV ? "BOTTOM_LEFT" : "TOP_LEFT");
            default -> isU ? "EDGE_U" : "EDGE_V";
        };
    }

    public static face_kind_ui_main getAdjacentFaceForDirection(face_kind_ui_main f, boolean isU, boolean posU, boolean posV) {
        return switch (f) {
            case TOP -> isU ? (posU ? face_kind_ui_main.RIGHT : face_kind_ui_main.LEFT) : (posV ? face_kind_ui_main.FRONT : face_kind_ui_main.BACK);
            case BOTTOM -> isU ? (posU ? face_kind_ui_main.RIGHT : face_kind_ui_main.LEFT) : (posV ? face_kind_ui_main.BACK : face_kind_ui_main.FRONT);
            case FRONT -> isU ? (posU ? face_kind_ui_main.RIGHT : face_kind_ui_main.LEFT) : (posV ? face_kind_ui_main.BOTTOM : face_kind_ui_main.TOP);
            case BACK -> isU ? (posU ? face_kind_ui_main.LEFT : face_kind_ui_main.RIGHT) : (posV ? face_kind_ui_main.BOTTOM : face_kind_ui_main.TOP);
            case RIGHT -> isU ? (posU ? face_kind_ui_main.BACK : face_kind_ui_main.FRONT) : (posV ? face_kind_ui_main.BOTTOM : face_kind_ui_main.TOP);
            case LEFT -> isU ? (posU ? face_kind_ui_main.FRONT : face_kind_ui_main.BACK) : (posV ? face_kind_ui_main.BOTTOM : face_kind_ui_main.TOP);
            default -> face_kind_ui_main.TOP;
        };
    }

    public static String getCornerVertexName(face_kind_ui_main f, boolean posU, boolean posV) {
        return switch (f) {
            case TOP -> posU ? (posV ? "TOP_FRONT_RIGHT" : "TOP_BACK_RIGHT") : (posV ? "TOP_FRONT_LEFT" : "TOP_BACK_LEFT");
            case BOTTOM -> posU ? (posV ? "BOTTOM_BACK_RIGHT" : "BOTTOM_FRONT_RIGHT") : (posV ? "BOTTOM_BACK_LEFT" : "BOTTOM_FRONT_LEFT");
            case FRONT -> posU ? (posV ? "BOTTOM_FRONT_RIGHT" : "TOP_FRONT_RIGHT") : (posV ? "BOTTOM_FRONT_LEFT" : "TOP_FRONT_LEFT");
            case BACK -> posU ? (posV ? "BOTTOM_BACK_LEFT" : "TOP_BACK_LEFT") : (posV ? "BOTTOM_BACK_RIGHT" : "TOP_BACK_RIGHT");
            case RIGHT -> posU ? (posV ? "BOTTOM_BACK_RIGHT" : "TOP_BACK_RIGHT") : (posV ? "BOTTOM_FRONT_RIGHT" : "TOP_FRONT_RIGHT");
            case LEFT -> posU ? (posV ? "BOTTOM_FRONT_LEFT" : "TOP_FRONT_LEFT") : (posV ? "BOTTOM_BACK_LEFT" : "TOP_BACK_LEFT");
            default -> "CORNER_VERTEX";
        };
    }
}
