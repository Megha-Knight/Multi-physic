package ui.workspace.drafting.topology;

import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import java.util.List;

/**
 * edge_machining_analyzer_ui_main.java
 * Analytical boundary-aware reasoning for features near or crossing body edges.
 */
public final class edge_machining_analyzer_ui_main {

    public static final double TANGENT_TOLERANCE = 1e-4;

    private edge_machining_analyzer_ui_main() {}

    public static boundary_relationship_ui_main analyzeHole(shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole) {
        if (shape == null || hole == null) {
            return new boundary_relationship_ui_main(
                boundary_condition_ui_main.CONTAINED, null, null, null, null, null, null, null, null,
                0, 0, ui.workspace.drafting.features.feature_state_ui_main.INVALID, "Null shape or hole"
            );
        }
        face_kind_ui_main kind = hole.getFaceKind();
        String srcFaceId = (body != null) ? body.getId() + ":F:" + kind.name() : shape.getId() + ":F:" + kind.name();

        var collision = checkIntersections(shape, hole, srcFaceId);
        if (collision != null) return collision;

        if (kind.isCylinderCap() || kind == face_kind_ui_main.BASE_CAP) {
            return analyzeCylinderCap(shape, body, hole, kind, srcFaceId);
        }
        if (kind.isLateral()) {
            return analyzeLateralFace(shape, body, hole, kind, srcFaceId);
        }
        return analyzeBoxFace(shape, body, hole, kind, srcFaceId);
    }

    private static boundary_relationship_ui_main checkIntersections(shape_item_ui_main shape, hole_feature_ui_main hole, String srcFaceId) {
        List<hole_feature_ui_main> all = shape.getHoles();
        for (hole_feature_ui_main other : all) {
            if (other == hole || other.getId().equals(hole.getId())) continue;
            if (other.getFaceKind() == hole.getFaceKind()) {
                double dist = Math.hypot(hole.getU() - other.getU(), hole.getV() - other.getV());
                double req = hole.getRadius() + other.getRadius();
                if (dist < req - 1e-3) {
                    double overlap = req - dist;
                    String diag = String.format(java.util.Locale.US,
                        "Feature intersects another cavity (%s). Intersecting holes detected. Multi-cavity intersection topology is not supported yet.",
                        other.getName());
                    return boundary_relationship_ui_main.intersectingCavities(hole.getFaceKind(), srcFaceId, other.getId(), overlap, diag);
                }
            }
        }
        return null;
    }

    private static boundary_relationship_ui_main analyzeBoxFace(
            shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole,
            face_kind_ui_main kind, String srcFaceId) {
        var bounds = topology_geometry_helper_ui_main.getFaceBounds(shape, kind);
        double fw = bounds[0], fh = bounds[1], r = hole.getOuterRadius();
        double u = hole.getU(), v = hole.getV(), hw = fw * 0.5, hh = fh * 0.5;
        double minDu = Math.min(u + hw, hw - u), minDv = Math.min(v + hh, hh - v), margin = Math.min(minDu, minDv);
        boolean overU = (minDu < r - TANGENT_TOLERANCE), overV = (minDv < r - TANGENT_TOLERANCE);
        boolean tanU = Math.abs(minDu - r) <= TANGENT_TOLERANCE, tanV = Math.abs(minDv - r) <= TANGENT_TOLERANCE;
        String bId = (body != null) ? body.getId() : shape.getId();

        if ((overU && overV) || (overU && tanV) || (tanU && overV)) {
            String vName = getCornerVertexName(kind, u >= 0, v >= 0);
            String edgeName = getEdgeNameForDirection(kind, minDu < minDv, u >= 0, v >= 0);
            face_kind_ui_main adjKind = getAdjacentFaceForDirection(kind, minDu < minDv, u >= 0, v >= 0);
            double overlap = Math.max(r - minDu, r - minDv);
            String diag = String.format("Feature crosses a body corner (%s). Hole intersects face boundary; multi-face hole topology is not supported yet.", vName);
            return boundary_relationship_ui_main.crossesCorner(kind, srcFaceId, bId + ":V:" + vName, vName, bId + ":E:" + edgeName, edgeName, adjKind, bId + ":F:" + adjKind.name(), overlap, diag);
        }
        if (overU || overV) {
            boolean isU = overU;
            String edgeName = getEdgeNameForDirection(kind, isU, u >= 0, v >= 0);
            face_kind_ui_main adjKind = getAdjacentFaceForDirection(kind, isU, u >= 0, v >= 0);
            double overlap = isU ? (r - minDu) : (r - minDv);
            String diag = String.format("Feature crosses adjacent face boundary (%s via %s). Hole intersects face boundary; multi-face hole topology is not supported yet.", adjKind.getLabel(), edgeName);
            return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + edgeName, edgeName, adjKind, bId + ":F:" + adjKind.name(), overlap, diag);
        }
        if (tanU || tanV) {
            boolean isU = tanU;
            String edgeName = getEdgeNameForDirection(kind, isU, u >= 0, v >= 0);
            face_kind_ui_main adjKind = getAdjacentFaceForDirection(kind, isU, u >= 0, v >= 0);
            String diag = String.format("Feature touches host-face boundary (%s). Hole intersects face boundary; multi-face hole topology is not supported yet.", edgeName);
            return boundary_relationship_ui_main.tangent(kind, srcFaceId, bId + ":E:" + edgeName, edgeName, adjKind, bId + ":F:" + adjKind.name(), margin, diag);
        }
        return boundary_relationship_ui_main.contained(kind, srcFaceId, margin - r);
    }

    private static boundary_relationship_ui_main analyzeLateralFace(
            shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole,
            face_kind_ui_main kind, String srcFaceId) {
        var bounds = topology_geometry_helper_ui_main.getFaceBounds(shape, kind);
        double hh = bounds[1] * 0.5, r = hole.getOuterRadius(), v = Math.abs(hole.getV());
        double margin = hh - v;
        String bId = (body != null) ? body.getId() : shape.getId();
        boolean isTop = hole.getV() >= 0;
        String rimEdge = (kind == face_kind_ui_main.CONE_LATERAL) ? "BASE_RIM" : (isTop ? "TOP_RIM" : "BOTTOM_RIM");
        face_kind_ui_main adj = (kind == face_kind_ui_main.CONE_LATERAL) ? face_kind_ui_main.BASE_CAP : (isTop ? face_kind_ui_main.TOP_CAP : face_kind_ui_main.BOTTOM_CAP);

        if (v + r > hh + TANGENT_TOLERANCE) {
            double overlap = (v + r) - hh;
            String diag = String.format("Feature crosses adjacent face boundary (%s via %s). Hole intersects face boundary; multi-face hole topology is not supported yet.", adj.getLabel(), rimEdge);
            return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, adj, bId + ":F:" + adj.name(), overlap, diag);
        }
        if (Math.abs(v + r - hh) <= TANGENT_TOLERANCE) {
            String diag = String.format("Feature touches host-face boundary (%s). Hole intersects face boundary; multi-face hole topology is not supported yet.", rimEdge);
            return boundary_relationship_ui_main.tangent(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, adj, bId + ":F:" + adj.name(), margin - r, diag);
        }
        return boundary_relationship_ui_main.contained(kind, srcFaceId, margin - r);
    }

    private static boundary_relationship_ui_main analyzeCylinderCap(
            shape_item_ui_main shape, topology_body_ui_main body, hole_feature_ui_main hole,
            face_kind_ui_main kind, String srcFaceId) {
        var bounds = topology_geometry_helper_ui_main.getFaceBounds(shape, kind);
        double capRadius = bounds[0] * 0.5, r = hole.getOuterRadius(), distCenter = Math.hypot(hole.getU(), hole.getV()), margin = capRadius - distCenter;
        String bId = (body != null) ? body.getId() : shape.getId(), rimEdge = (kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.TOP) ? "TOP_RIM" : (kind == face_kind_ui_main.BASE_CAP ? "BASE_RIM" : "BOTTOM_RIM");
        face_kind_ui_main adj = (kind == face_kind_ui_main.BASE_CAP) ? face_kind_ui_main.CONE_LATERAL : face_kind_ui_main.CYLINDER_LATERAL;

        if (distCenter + r > capRadius + TANGENT_TOLERANCE) {
            double overlap = (distCenter + r) - capRadius;
            String diag = String.format("Feature crosses adjacent face boundary (%s via %s). Hole intersects face boundary; multi-face hole topology is not supported yet.", adj.getLabel(), rimEdge);
            return boundary_relationship_ui_main.crossesEdge(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, adj, bId + ":F:" + adj.name(), overlap, diag);
        }
        if (Math.abs(distCenter + r - capRadius) <= TANGENT_TOLERANCE) {
            String diag = String.format("Feature touches host-face boundary (%s). Hole intersects face boundary; multi-face hole topology is not supported yet.", rimEdge);
            return boundary_relationship_ui_main.tangent(kind, srcFaceId, bId + ":E:" + rimEdge, rimEdge, adj, bId + ":F:" + adj.name(), margin, diag);
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

