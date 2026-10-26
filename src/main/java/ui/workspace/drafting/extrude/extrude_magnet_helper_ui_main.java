package ui.workspace.drafting.extrude;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_geometry_helper_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.Collection;

/**
 * extrude_magnet_helper_ui_main.java
 * Snaps 3D extrude profiles to the nearest solid planar face upon commit (mouse release).
 */
public final class extrude_magnet_helper_ui_main {

    /**
     * Snap threshold in millimetres. If an extrude footprint's center point is within
     * this distance of an existing solid body's planar face at mouse release, it snaps
     * flush to that face; beyond this distance it remains an independent ground shape.
     */
    public static final double SNAP_THRESHOLD_MM = 30.0;

    public record SnapResult(shape_item_ui_main host, face_reference_ui_main faceRef, double distance) {}

    private extrude_magnet_helper_ui_main() {}

    public static SnapResult trySnapToNearestFace(shape_item_ui_main item, basic_shapes_ui_main type,
                                                  Point3D p1, Point3D p2, shape_editor_ui_main editor) {
        return trySnapToNearestFace(item, type, p1, p2, editor != null ? editor.getShapes() : null);
    }

    public static SnapResult trySnapToNearestFace(shape_item_ui_main item, basic_shapes_ui_main type,
                                                  Point3D p1, Point3D p2, Collection<shape_item_ui_main> shapes) {
        if (item == null || type == null || p1 == null || p2 == null || shapes == null || shapes.isEmpty()) return null;
        if (!type.is3D() && !item.hasExtrusions() && type != basic_shapes_ui_main.EQUILATERAL_TRIANGLE && type != basic_shapes_ui_main.RIGHT_TRIANGLE) return null;

        Point3D drawCenter = shape_rotation_helper_ui_main.computeCenter(type, p1, p2);
        SnapResult best = findNearestFace(drawCenter, shapes, item.getId());
        if (best == null || best.distance > SNAP_THRESHOLD_MM) return null;

        applyFlushSnap(item, type, p1, p2, best);
        return best;
    }

    public static SnapResult trySnapExistingShape(shape_item_ui_main item, Collection<shape_item_ui_main> shapes) {
        if (item == null || shapes == null || shapes.size() <= 1) return null;
        Point3D worldCtr = item.getWorldCenter();
        SnapResult best = findNearestFace(worldCtr, shapes, item.getId());
        if (best == null || best.distance > SNAP_THRESHOLD_MM) return null;
        applyFlushSnap(item, item.getType(), item.getP1(), item.getP2(), best);
        item.rebuild();
        return best;
    }

    public static SnapResult findNearestFace(Point3D center, Collection<shape_item_ui_main> shapes, String excludeId) {
        if (center == null || shapes == null) return null;
        SnapResult best = null;
        double minDistance = Double.MAX_VALUE;

        for (shape_item_ui_main s : shapes) {
            if (s == null || s.isConsumed()) continue;
            if (!s.getType().is3D() && !s.hasExtrusions() && s.getType() != basic_shapes_ui_main.EQUILATERAL_TRIANGLE && s.getType() != basic_shapes_ui_main.RIGHT_TRIANGLE) continue;
            if (excludeId != null && excludeId.equals(s.getId())) continue;

            for (face_kind_ui_main kind : getPlanarFaces(s.getType())) {
                Point3D origin = topology_geometry_helper_ui_main.getFaceOrigin(s, kind);
                Point3D normal = topology_geometry_helper_ui_main.getFaceNormal(s, kind);
                Point3D uAxis  = topology_geometry_helper_ui_main.getFaceUAxis(s, kind);
                Point3D vAxis  = topology_geometry_helper_ui_main.getFaceVAxis(s, kind);
                double[] b = topology_geometry_helper_ui_main.getFaceBounds(s, kind);

                Point3D delta = center.subtract(origin);
                double u = delta.dotProduct(uAxis), v = delta.dotProduct(vAxis);
                double clampedU, clampedV;
                if (kind.isCylinderCap()) {
                    double r = b[0] * 0.5, len = Math.hypot(u, v);
                    clampedU = (len > r && len > 1e-6) ? u * (r / len) : u;
                    clampedV = (len > r && len > 1e-6) ? v * (r / len) : v;
                } else {
                    clampedU = Math.max(-b[0] * 0.5, Math.min(b[0] * 0.5, u));
                    clampedV = Math.max(-b[1] * 0.5, Math.min(b[1] * 0.5, v));
                }

                Point3D ptOnFace = origin.add(uAxis.multiply(clampedU)).add(vAxis.multiply(clampedV));
                double dist = center.distance(ptOnFace);
                if (dist < minDistance) {
                    minDistance = dist;
                    face_reference_ui_main ref = new face_reference_ui_main(
                        s.getId(), kind, ptOnFace, normal, origin, uAxis, vAxis, clampedU, clampedV, b[0], b[1]
                    );
                    best = new SnapResult(s, ref, dist);
                }
            }
            if (s.getType() == basic_shapes_ui_main.CYLINDER) {
                Point3D sp1 = s.getP1(), sp2 = s.getP2();
                double cr = Math.max(0.2, sp1.distance(new Point3D(sp2.getX(), 0, sp2.getZ())));
                double ch = Math.abs(sp2.getY()) > 0.1 ? Math.abs(sp2.getY()) : Math.max(6.0, cr * 2.0);
                double cwx = s.getWorldX() + sp1.getX(), cwz = s.getWorldZ() + sp1.getZ(), cwy = s.getWorldY() + sp1.getY();
                double dx = center.getX() - cwx, dz = center.getZ() - cwz, len = Math.hypot(dx, dz);
                if (len > 1e-4) {
                    double nx = dx / len, nz = dz / len, clampedY = Math.max(cwy - ch, Math.min(cwy, center.getY()));
                    Point3D ptOnCyl = new Point3D(cwx + cr * nx, clampedY, cwz + cr * nz);
                    double dist = center.distance(ptOnCyl);
                    if (dist < minDistance) {
                        minDistance = dist;
                        face_reference_ui_main ref = new face_reference_ui_main(
                            s.getId(), face_kind_ui_main.CYLINDER_LATERAL, ptOnCyl, new Point3D(nx, 0, nz), ptOnCyl,
                            new Point3D(-nz, 0, nx), new Point3D(0, -1, 0), 0, 0, 2 * Math.PI * cr, ch
                        );
                        best = new SnapResult(s, ref, dist);
                    }
                }
            }
        }
        return best;
    }

    private static void applyFlushSnap(shape_item_ui_main item, basic_shapes_ui_main type,
                                       Point3D p1, Point3D p2, SnapResult snap) {
        face_reference_ui_main ref = snap.faceRef();
        Point3D pt = ref.getWorldHitPoint();
        Point3D n  = ref.getWorldNormal();

        // Orient shape's top normal (0,-1,0) along face outward normal
        double nx = n.getX(), ny = n.getY(), nz = n.getZ();
        double hyp = Math.hypot(nx, nz);
        double rx = Math.toDegrees(Math.atan2(-hyp, -ny));
        double ry = (hyp > 1e-6) ? Math.toDegrees(Math.atan2(nx, nz)) : 0.0;
        item.setRotation(rx, ry);

        // Center footprint at local (0, 0)
        switch (type) {
            case CYLINDER, CONE -> {
                double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
                double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
                item.setP1P2(new Point3D(0, 0, 0), new Point3D(r, -h, 0));
            }
            case CUBOID -> {
                double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
                double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
                item.setP1P2(new Point3D(-w * 0.5, 0, -d * 0.5), new Point3D(w * 0.5, -h, d * 0.5));
            }
            case CUBE -> {
                double s = Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ()));
                item.setP1P2(new Point3D(-s * 0.5, 0, -s * 0.5), new Point3D(s * 0.5, -s, s * 0.5));
            }
            case EQUILATERAL_TRIANGLE, RIGHT_TRIANGLE -> {
                double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
                double h = item.hasExtrusions() ? item.getExtrusions().get(0).getHeight() : (Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : 25.0);
                item.setP1P2(new Point3D(-w * 0.5, 0, -d * 0.5), new Point3D(w * 0.5, -h, d * 0.5));
            }
            default -> {
                if (item.hasExtrusions()) {
                    double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
                    double h = item.getExtrusions().get(0).getHeight();
                    item.setP1P2(new Point3D(-w * 0.5, 0, -d * 0.5), new Point3D(w * 0.5, -h, d * 0.5));
                }
            }
        }

        item.setWorldTranslation(pt.getX(), pt.getY(), pt.getZ());
        item.setFacePlane(ref.getUAxis(), ref.getVAxis(), ref.getWorldNormal(),
                          snap.host().getId(), ref.getFaceKind(), ref.getLocalHitU(), ref.getLocalHitV());
    }

    private static face_kind_ui_main[] getPlanarFaces(basic_shapes_ui_main type) {
        return switch (type) {
            case CUBE, CUBOID, SQUARE, RECTANGLE -> new face_kind_ui_main[]{
                face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM, face_kind_ui_main.FRONT,
                face_kind_ui_main.BACK, face_kind_ui_main.RIGHT, face_kind_ui_main.LEFT
            };
            case CYLINDER, CIRCLE -> new face_kind_ui_main[]{face_kind_ui_main.TOP_CAP, face_kind_ui_main.BOTTOM_CAP};
            case CONE -> new face_kind_ui_main[]{face_kind_ui_main.BASE_CAP};
            default -> new face_kind_ui_main[]{face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM};
        };
    }
}
