package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;

public final class edge_picker_ui_main {

    public static final double DEFAULT_TOLERANCE = 8.0;
    private static final int CIRCLE_CHORDS = 24;

    private edge_picker_ui_main() {}

    public static topology_edge_ui_main pickEdge(double[] worldRay, shape_item_ui_main shape) {
        return pickEdge(worldRay, shape, DEFAULT_TOLERANCE);
    }

    public static topology_edge_ui_main pickEdge(double[] worldRay, shape_item_ui_main shape, double tolerance) {
        if (worldRay == null || worldRay.length < 6 || shape == null || !shape.getType().is3D()) return null;
        double dx = worldRay[3], dy = worldRay[4], dz = worldRay[5], lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq < 1e-12 || Double.isNaN(lenSq)) return null;

        Point3D rayOriginWorld = new Point3D(worldRay[0], worldRay[1], worldRay[2]);
        Point3D rayDirWorld = new Point3D(dx, dy, dz).multiply(1.0 / Math.sqrt(lenSq));
        Point3D c = shape.getCenter();
        double rx = shape.getRotationX(), ry = shape.getRotationY();
        double wx = shape.getWorldX(), wy = shape.getWorldY(), wz = shape.getWorldZ();

        Point3D ro = face_picker_ui_main.inverseTransformPoint(rayOriginWorld, c, rx, ry, wx, wy, wz);
        Point3D rd = face_picker_ui_main.inverseTransformDir(rayDirWorld, rx, ry);

        topology_body_ui_main topo = shape.getTopology();
        if (topo == null) return null;

        topology_edge_ui_main bestEdge = null;
        double bestDist = Double.MAX_VALUE, bestT = Double.MAX_VALUE;
        double[] outParam = new double[2];

        for (topology_edge_ui_main edge : topo.getEdges()) {
            if (edge.getLength() <= 0.2) continue;
            double dist, t;
            if (edge.isLinear()) {
                dist = distRayToSegment(ro, rd, edge.getLocalP1(), edge.getLocalP2(), outParam);
                t = outParam[0];
            } else {
                double[] res = distRayToCircle(ro, rd, shape, edge, outParam);
                dist = res[0]; t = res[1];
            }
            if (dist <= tolerance) {
                if (dist < bestDist - 0.5 || (Math.abs(dist - bestDist) <= 0.5 && t < bestT)) {
                    bestEdge = edge;
                    bestDist = dist;
                    bestT = t;
                }
            }
        }
        return bestEdge;
    }

    private static double[] distRayToCircle(Point3D ro, Point3D rd, shape_item_ui_main shape,
                                           topology_edge_ui_main edge, double[] outParam) {
        Point3D center = edge.getLocalP1();
        double radius = edge.getLength() * 0.5;
        Point3D norm = resolveEdgeNormal(shape, edge);
        Point3D uAxis = (Math.abs(norm.getY()) > 0.8)
            ? new Point3D(1, 0, 0).crossProduct(norm).normalize()
            : new Point3D(0, 1, 0).crossProduct(norm).normalize();
        Point3D vAxis = norm.crossProduct(uAxis).normalize();

        double minDist = Double.MAX_VALUE, bestT = Double.MAX_VALUE;
        double step = 2.0 * Math.PI / CIRCLE_CHORDS;
        for (int i = 0; i < CIRCLE_CHORDS; i++) {
            double a1 = i * step, a2 = (i + 1) * step;
            Point3D q1 = center.add(uAxis.multiply(radius * Math.cos(a1))).add(vAxis.multiply(radius * Math.sin(a1)));
            Point3D q2 = center.add(uAxis.multiply(radius * Math.cos(a2))).add(vAxis.multiply(radius * Math.sin(a2)));
            double d = distRayToSegment(ro, rd, q1, q2, outParam);
            if (d < minDist) { minDist = d; bestT = outParam[0]; }
        }
        return new double[]{minDist, bestT};
    }

    public static Point3D resolveEdgeNormal(shape_item_ui_main shape, topology_edge_ui_main edge) {
        if (edge == null || shape == null) return new Point3D(0, 1, 0);
        String featId = edge.getCreatingFeatureId();
        if (featId != null) {
            for (hole_feature_ui_main h : shape.getHoles()) {
                if (featId.equals(h.getId())) {
                    face_kind_ui_main fk = edge.getName().startsWith("HOLE_EXIT_RIM")
                        ? opposite(h.getFaceKind()) : h.getFaceKind();
                    return face_geometry_helper_ui_main.getFaceInfo(shape, fk).normal();
                }
            }
            for (extrude_feature_ui_main ext : shape.getExtrusions()) {
                if (featId.equals(ext.getId())) {
                    return face_geometry_helper_ui_main.getFaceInfo(shape, ext.getFaceKind()).normal();
                }
            }
        }
        if (edge.getName().contains("TOP") || edge.getName().contains("BOT") || edge.getName().contains("BASE")) {
            return new Point3D(0, 1, 0);
        }
        for (String fid : edge.getAdjacentFaceIds()) {
            for (face_kind_ui_main fk : face_kind_ui_main.values()) {
                if (fid.contains(":F:" + fk.name())) {
                    return face_geometry_helper_ui_main.getFaceInfo(shape, fk).normal();
                }
            }
        }
        return new Point3D(0, 1, 0);
    }

    private static face_kind_ui_main opposite(face_kind_ui_main k) {
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

    public static double distRayToSegment(Point3D ro, Point3D rd, Point3D p1, Point3D p2, double[] outParam) {
        Point3D d2 = p2.subtract(p1), r = ro.subtract(p1);
        double b = rd.dotProduct(d2), c = d2.dotProduct(d2);
        double d = rd.dotProduct(r), e = d2.dotProduct(r);
        if (c < 1e-12) {
            double t = Math.max(0, -d);
            if (outParam != null) { outParam[0] = t; outParam[1] = 0; }
            return ro.add(rd.multiply(t)).distance(p1);
        }
        double denom = c - b * b, t, u;
        if (denom < 1e-9) {
            double u0 = Math.max(0, Math.min(1, e / c));
            t = Math.max(0, u0 * b - d);
            u = Math.max(0, Math.min(1, (e + t * b) / c));
        } else {
            t = (b * e - c * d) / denom;
            u = (e - b * d) / denom;
            if (u < 0) { u = 0; t = Math.max(0, -d); }
            else if (u > 1) { u = 1; t = Math.max(0, b - d); }
            if (t < 0) { t = 0; u = Math.max(0, Math.min(1, e / c)); }
        }
        if (outParam != null) { outParam[0] = t; outParam[1] = u; }
        Point3D ptRay = ro.add(rd.multiply(t)), ptSeg = p1.add(d2.multiply(u));
        return ptRay.distance(ptSeg);
    }
}
