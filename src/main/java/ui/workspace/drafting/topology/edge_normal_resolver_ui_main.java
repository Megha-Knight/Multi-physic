package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.faces.face_geometry_helper_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

public final class edge_normal_resolver_ui_main {

    private edge_normal_resolver_ui_main() {}

    public static Point3D resolveEdgeNormal(shape_item_ui_main shape, topology_edge_ui_main edge) {
        if (edge == null || shape == null) return new Point3D(0, 1, 0);
        String name = edge.getName() != null ? edge.getName() : "";
        String featId = edge.getCreatingFeatureId();

        if (featId != null) {
            for (hole_feature_ui_main h : shape.getHoles()) {
                if (featId.equals(h.getId())) {
                    face_kind_ui_main fk = name.startsWith("HOLE_EXIT_RIM") ? opposite(h.getFaceKind()) : h.getFaceKind();
                    return face_geometry_helper_ui_main.getFaceInfo(shape, fk).normal();
                }
            }
            for (extrude_feature_ui_main ext : shape.getExtrusions()) {
                if (featId.equals(ext.getId())) {
                    return resolveExtrusionEdgeNormal(shape, ext, edge);
                }
            }
        }

        // B-Rep Adjacent face normal derivation
        Point3D acc = new Point3D(0, 0, 0);
        int faceCount = 0;
        for (String fid : edge.getAdjacentFaceIds()) {
            for (face_kind_ui_main fk : face_kind_ui_main.values()) {
                if (fid.contains(":F:" + fk.name())) {
                    acc = acc.add(face_geometry_helper_ui_main.getFaceInfo(shape, fk).normal());
                    faceCount++;
                }
            }
        }
        if (faceCount > 0 && acc.magnitude() > 1e-5) return acc.normalize();
        return new Point3D(0, 1, 0);
    }

    public static Point3D resolveExtrusionEdgeNormal(shape_item_ui_main shape, extrude_feature_ui_main ext, topology_edge_ui_main edge) {
        Point3D nCap = face_geometry_helper_ui_main.getFaceInfo(shape, ext.getFaceKind()).normal();
        String name = edge.getName();

        if (name.contains("POS_U")) {
            return nCap.add(new Point3D(1, 0, 0)).normalize();
        } else if (name.contains("NEG_U")) {
            return nCap.add(new Point3D(-1, 0, 0)).normalize();
        } else if (name.contains("POS_V")) {
            return nCap.add(new Point3D(0, 0, 1)).normalize();
        } else if (name.contains("NEG_V")) {
            return nCap.add(new Point3D(0, 0, -1)).normalize();
        } else if (name.contains("CAP_RIM")) {
            // Circular edge: midpoint radial direction
            Point3D mid = edge.getLocalP1().add(edge.getLocalP2()).multiply(0.5);
            Point3D radial = new Point3D(mid.getX(), 0, mid.getZ());
            if (radial.magnitude() > 1e-5) return nCap.add(radial.normalize()).normalize();
            return nCap;
        }
        return nCap;
    }

    public static double computeSignedDihedralAngle(Point3D p1, Point3D p2, Point3D n1, Point3D n2, Point3D face1Inward) {
        if (n1 == null || n2 == null || p1 == null || p2 == null || face1Inward == null) return 0.0;
        Point3D edgeVec = p2.subtract(p1);
        double len = edgeVec.magnitude();
        if (len < 1e-8) return 0.0;

        Point3D tEdge = edgeVec.multiply(1.0 / len);
        Point3D tBoundary = face1Inward.crossProduct(n1).normalize();
        double align = tEdge.dotProduct(tBoundary);
        double sgn = Math.signum(align);
        if (Math.abs(sgn) < 1e-6) sgn = 1.0;
        Point3D tHalfEdge = tEdge.multiply(sgn);

        Point3D cross = n1.crossProduct(n2);
        double sinTheta = cross.dotProduct(tHalfEdge);
        double cosTheta = Math.max(-1.0, Math.min(1.0, n1.dotProduct(n2)));
        return Math.atan2(sinTheta, cosTheta);
    }

    public static face_kind_ui_main opposite(face_kind_ui_main k) {
        if (k == null) return face_kind_ui_main.TOP;
        return switch (k) {
            case TOP -> face_kind_ui_main.BOTTOM;
            case BOTTOM -> face_kind_ui_main.TOP;
            case FRONT -> face_kind_ui_main.BACK;
            case BACK -> face_kind_ui_main.FRONT;
            case LEFT -> face_kind_ui_main.RIGHT;
            case RIGHT -> face_kind_ui_main.LEFT;
            case TOP_CAP -> face_kind_ui_main.BOTTOM_CAP;
            case BOTTOM_CAP -> face_kind_ui_main.TOP_CAP;
            default -> face_kind_ui_main.TOP;
        };
    }
}
