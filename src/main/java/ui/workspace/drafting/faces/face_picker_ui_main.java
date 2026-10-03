package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;

public final class face_picker_ui_main {

    private record Hit(double t, face_kind_ui_main kind, Point3D localPt, Point3D localNormal, Point3D localOrigin, Point3D localU, Point3D localV, double w, double h) {}

    private face_picker_ui_main() {}

    public static face_reference_ui_main pickFace(double[] worldRay, shape_item_ui_main shape) {
        if (worldRay == null || worldRay.length < 6 || shape == null || !shape.getType().is3D()) return null;
        double dx = worldRay[3], dy = worldRay[4], dz = worldRay[5], lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq < 1e-12 || Double.isNaN(lenSq)) return null;
        Point3D rayOriginWorld = new Point3D(worldRay[0], worldRay[1], worldRay[2]);
        Point3D rayDirWorld = new Point3D(dx, dy, dz).multiply(1.0 / Math.sqrt(lenSq));
        Point3D c = shape.getCenter();
        double rx = shape.getRotationX(), ry = shape.getRotationY(), wx = shape.getWorldX(), wy = shape.getWorldY(), wz = shape.getWorldZ();
        Point3D rayOriginLocal = inverseTransformPoint(rayOriginWorld, c, rx, ry, wx, wy, wz);
        Point3D rayDirLocal = inverseTransformDir(rayDirWorld, rx, ry);

        Hit best = intersectShapeLocal(shape, rayOriginLocal, rayDirLocal);
        if (best == null || best.t <= 0) return null;

        Point3D worldHit = shape_rotation_helper_ui_main.transformPoint(best.localPt, c, rx, ry, wx, wy, wz);
        Point3D worldNorm = shape_rotation_helper_ui_main.transformNormal(best.localNormal, rx, ry).normalize();
        Point3D worldOrigin = shape_rotation_helper_ui_main.transformPoint(best.localOrigin, c, rx, ry, wx, wy, wz);
        Point3D worldU = shape_rotation_helper_ui_main.transformNormal(best.localU, rx, ry).normalize(), worldV = shape_rotation_helper_ui_main.transformNormal(best.localV, rx, ry).normalize();
        Point3D delta = worldHit.subtract(worldOrigin);
        return new face_reference_ui_main(shape.getId(), best.kind, worldHit, worldNorm,
            worldOrigin, worldU, worldV, delta.dotProduct(worldU), delta.dotProduct(worldV), best.w, best.h);
    }

    public static face_reference_ui_main refreshFace(face_reference_ui_main prev, shape_item_ui_main shape) {
        if (prev == null || shape == null || !shape.getType().is3D()) return null;
        return ui.workspace.drafting.topology.topology_geometry_helper_ui_main.reconstructFaceReference(
            shape, prev.getFaceKind(), prev.getLocalHitU(), prev.getLocalHitV()
        );
    }

    private static Hit getBoxFace(Point3D p1, Point3D p2, face_kind_ui_main kind, boolean isCube) {
        double w, h, d, cx, cz;
        if (isCube) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), s = Math.max(Math.abs(dx), Math.abs(dz));
            w = s; h = s; d = s; cx = p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5); cz = p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5);
        } else {
            w = Math.abs(p2.getX() - p1.getX()); d = Math.abs(p2.getZ() - p1.getZ());
            h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            cx = (p1.getX() + p2.getX()) * 0.5; cz = (p1.getZ() + p2.getZ()) * 0.5;
        }
        double minX = cx - w * 0.5, maxX = cx + w * 0.5, minZ = cz - d * 0.5, maxZ = cz + d * 0.5;
        return switch (kind) {
            case TOP -> new Hit(0, kind, null, new Point3D(0, -1, 0), new Point3D(cx, -h, cz), new Point3D(1, 0, 0), new Point3D(0, 0, 1), w, d);
            case BOTTOM -> new Hit(0, kind, null, new Point3D(0, 1, 0), new Point3D(cx, 0, cz), new Point3D(1, 0, 0), new Point3D(0, 0, -1), w, d);
            case FRONT -> new Hit(0, kind, null, new Point3D(0, 0, 1), new Point3D(cx, -h * 0.5, maxZ), new Point3D(1, 0, 0), new Point3D(0, -1, 0), w, h);
            case BACK -> new Hit(0, kind, null, new Point3D(0, 0, -1), new Point3D(cx, -h * 0.5, minZ), new Point3D(-1, 0, 0), new Point3D(0, -1, 0), w, h);
            case RIGHT -> new Hit(0, kind, null, new Point3D(1, 0, 0), new Point3D(maxX, -h * 0.5, cz), new Point3D(0, 0, -1), new Point3D(0, -1, 0), d, h);
            case LEFT -> new Hit(0, kind, null, new Point3D(-1, 0, 0), new Point3D(minX, -h * 0.5, cz), new Point3D(0, 0, 1), new Point3D(0, -1, 0), d, h);
            default -> null;
        };
    }

    private static Hit intersectShapeLocal(shape_item_ui_main s, Point3D ro, Point3D rd) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        return switch (s.getType()) {
            case CUBE -> intersectBox(p1, p2, ro, rd, true);
            case CUBOID -> intersectBox(p1, p2, ro, rd, false);
            case CYLINDER -> intersectCylinder(p1, p2, ro, rd);
            case CONE -> intersectCone(p1, p2, ro, rd);
            case SPHERE -> intersectSphere(p1, p2, ro, rd);
            default -> null;
        };
    }

    private static Hit intersectBox(Point3D p1, Point3D p2, Point3D ro, Point3D rd, boolean isCube) {
        Hit best = null;
        for (face_kind_ui_main k : new face_kind_ui_main[]{face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM, face_kind_ui_main.FRONT, face_kind_ui_main.BACK, face_kind_ui_main.RIGHT, face_kind_ui_main.LEFT})
            best = checkQuad(ro, rd, getBoxFace(p1, p2, k, isCube), best);
        return best;
    }

    private static Hit checkQuad(Point3D ro, Point3D rd, Hit d, Hit prev) {
        if (d == null) return prev;
        double denom = rd.dotProduct(d.localNormal);
        if (denom >= -1e-5) return prev;
        double t = d.localOrigin.subtract(ro).dotProduct(d.localNormal) / denom;
        if (t <= 1e-4 || (prev != null && t >= prev.t)) return prev;
        Point3D p = ro.add(rd.multiply(t)), delta = p.subtract(d.localOrigin);
        double u = delta.dotProduct(d.localU), v = delta.dotProduct(d.localV);
        if (Math.abs(u) > d.w * 0.5 + 1e-3 || Math.abs(v) > d.h * 0.5 + 1e-3) return prev;
        return new Hit(t, d.kind, p, d.localNormal, d.localOrigin, d.localU, d.localV, d.w, d.h);
    }

    private static Hit intersectCylinder(Point3D p1, Point3D p2, Point3D ro, Point3D rd) {
        double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ())), h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        Hit best = checkDisk(ro, rd, new Point3D(p1.getX(), -h, p1.getZ()), new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1), r, face_kind_ui_main.TOP_CAP, null);
        best = checkDisk(ro, rd, new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1), r, face_kind_ui_main.BOTTOM_CAP, best);
        double ox = ro.getX() - p1.getX(), oz = ro.getZ() - p1.getZ(), dx = rd.getX(), dz = rd.getZ();
        double a = dx * dx + dz * dz, b = 2 * (ox * dx + oz * dz), c = ox * ox + oz * oz - r * r, disc = b * b - 4 * a * c;
        if (disc >= 0 && a > 1e-6) {
            double t = (-b - Math.sqrt(disc)) / (2 * a);
            if (t > 1e-4 && (best == null || t < best.t)) {
                Point3D p = ro.add(rd.multiply(t));
                if (p.getY() >= -h && p.getY() <= 0) {
                    Point3D n = new Point3D(p.getX() - p1.getX(), 0, p.getZ() - p1.getZ()).normalize();
                    Point3D u = new Point3D(-n.getZ(), 0, n.getX()).normalize(), v = new Point3D(0, -1, 0);
                    best = new Hit(t, face_kind_ui_main.CYLINDER_LATERAL, p, n, new Point3D(p1.getX(), -h * 0.5, p1.getZ()), u, v, 2 * Math.PI * r, h);
                }
            }
        }
        return best;
    }

    private static Hit checkDisk(Point3D ro, Point3D rd, Point3D fc, Point3D n, Point3D u, Point3D v, double r, face_kind_ui_main kind, Hit prev) {
        double denom = rd.dotProduct(n);
        if (denom >= -1e-5) return prev;
        double t = fc.subtract(ro).dotProduct(n) / denom;
        if (t <= 1e-4 || (prev != null && t >= prev.t)) return prev;
        Point3D p = ro.add(rd.multiply(t));
        if (new Point3D(p.getX(), 0, p.getZ()).distance(new Point3D(fc.getX(), 0, fc.getZ())) > r) return prev;
        return new Hit(t, kind, p, n, fc, u, v, r * 2.0, r * 2.0);
    }

    private static Hit intersectCone(Point3D p1, Point3D p2, Point3D ro, Point3D rd) {
        double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ())), h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        Hit best = checkDisk(ro, rd, new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1), r, face_kind_ui_main.BASE_CAP, null);
        Point3D apex = new Point3D(p1.getX(), -h, p1.getZ());
        double k = r / h, k2 = k * k;
        double ox = ro.getX() - apex.getX(), oz = ro.getZ() - apex.getZ(), oy = ro.getY() - apex.getY();
        double dx = rd.getX(), dz = rd.getZ(), dy = rd.getY();
        double a = dx * dx + dz * dz - k2 * dy * dy, b = 2 * (ox * dx + oz * dz - k2 * oy * dy), c = ox * ox + oz * oz - k2 * oy * oy;
        double disc = b * b - 4 * a * c;
        if (disc >= 0 && Math.abs(a) > 1e-6) {
            double sq = Math.sqrt(disc), t1 = (-b - sq) / (2 * a), t2 = (-b + sq) / (2 * a);
            double t = (t1 > 1e-4) ? t1 : t2;
            if (t > 1e-4 && (best == null || t < best.t)) {
                Point3D p = ro.add(rd.multiply(t));
                if (p.getY() >= -h && p.getY() <= 0) {
                    Point3D n = new Point3D(p.getX() - p1.getX(), k * r, p.getZ() - p1.getZ()).normalize();
                    Point3D u = new Point3D(-n.getZ(), 0, n.getX()).normalize();
                    best = new Hit(t, face_kind_ui_main.CONE_LATERAL, p, n, apex, u, u.crossProduct(n).normalize(), r * 2, h);
                }
            }
        }
        return best;
    }

    private static Hit intersectSphere(Point3D p1, Point3D p2, Point3D ro, Point3D rd) {
        double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ())), scY = -r;
        Point3D sc = new Point3D(p1.getX(), scY, p1.getZ()), oc = ro.subtract(sc);
        double b = 2.0 * oc.dotProduct(rd), c = oc.dotProduct(oc) - r * r, disc = b * b - 4.0 * c;
        if (disc < 0) return null;
        double t = (-b - Math.sqrt(disc)) * 0.5;
        if (t <= 1e-4) return null;
        Point3D p = ro.add(rd.multiply(t)), n = p.subtract(sc).normalize();
        Point3D u = (Math.abs(n.getY()) > 0.9) ? new Point3D(1, 0, 0).crossProduct(n).normalize() : new Point3D(0, 1, 0).crossProduct(n).normalize();
        return new Hit(t, face_kind_ui_main.SPHERE_SURFACE, p, n, sc, u, u.crossProduct(n).normalize(), r * 2, r * 2);
    }

    public static Point3D inverseTransformPoint(Point3D p, Point3D c, double rxDeg, double ryDeg, double wx, double wy, double wz) {
        double rx = Math.toRadians(rxDeg), ry = Math.toRadians(ryDeg), dx = p.getX() - wx - c.getX(), dy = p.getY() - wy - c.getY(), dz = p.getZ() - wz - c.getZ();
        double dx1 = dx * Math.cos(ry) - dz * Math.sin(ry), z1 = dx * Math.sin(ry) + dz * Math.cos(ry);
        double dy2 = dy * Math.cos(rx) + z1 * Math.sin(rx), dz2 = -dy * Math.sin(rx) + z1 * Math.cos(rx);
        return new Point3D(c.getX() + dx1, c.getY() + dy2, c.getZ() + dz2);
    }

    public static Point3D inverseTransformDir(Point3D d, double rxDeg, double ryDeg) {
        if (d == null || d.magnitude() < 1e-9) return new Point3D(0, 0, 1);
        double rx = Math.toRadians(rxDeg), ry = Math.toRadians(ryDeg), dx1 = d.getX() * Math.cos(ry) - d.getZ() * Math.sin(ry), z1 = d.getX() * Math.sin(ry) + d.getZ() * Math.cos(ry);
        double dy2 = d.getY() * Math.cos(rx) + z1 * Math.sin(rx), dz2 = -d.getY() * Math.sin(rx) + z1 * Math.cos(rx), len = Math.sqrt(dx1 * dx1 + dy2 * dy2 + dz2 * dz2);
        return len < 1e-9 ? new Point3D(0, 0, 1) : new Point3D(dx1 / len, dy2 / len, dz2 / len);
    }
}
