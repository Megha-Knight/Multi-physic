package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;

public final class face_geometry_helper_ui_main {

    public static final double CAD_TOLERANCE = 1e-4;

    private face_geometry_helper_ui_main() {}

    public record FaceInfo(Point3D origin, Point3D uAxis, Point3D vAxis, Point3D normal,
                           double width, double height, double thickness, double cylinderRadius, boolean isCap) {
        public boolean isCylinderCap() { return isCap && cylinderRadius > 0.0; }
    }

    public static double computeCylinderRadius(Point3D p1, Point3D p2) {
        if (p1 == null || p2 == null) return 1.0;
        return Math.max(0.1, p1.distance(new Point3D(p2.getX(), p1.getY(), p2.getZ())));
    }

    public static double computeCylinderHeight(Point3D p1, Point3D p2, double r) {
        if (p2 == null) return Math.max(6.0, r * 2.0);
        double dy = (p1 != null) ? Math.abs(p2.getY() - p1.getY()) : Math.abs(p2.getY());
        return dy > 0.1 ? dy : Math.max(6.0, r * 2.0);
    }

    public static FaceInfo getFaceInfo(shape_item_ui_main shape, face_kind_ui_main kind) {
        if (shape == null || kind == null) return createDefault();
        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        if (shape.getType() == basic_shapes_ui_main.CYLINDER) {
            double r = computeCylinderRadius(p1, p2), h = computeCylinderHeight(p1, p2, r), y0 = p1.getY();
            if (kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.TOP) {
                return new FaceInfo(new Point3D(p1.getX(), y0 - h, p1.getZ()),
                        new Point3D(1, 0, 0), new Point3D(0, 0, 1), new Point3D(0, -1, 0),
                        r * 2.0, r * 2.0, h, r, true);
            }
            if (kind == face_kind_ui_main.BOTTOM_CAP || kind == face_kind_ui_main.BOTTOM) {
                return new FaceInfo(new Point3D(p1.getX(), y0, p1.getZ()),
                        new Point3D(1, 0, 0), new Point3D(0, 0, -1), new Point3D(0, 1, 0),
                        r * 2.0, r * 2.0, h, r, true);
            }
            return new FaceInfo(new Point3D(p1.getX(), y0 - h * 0.5, p1.getZ()),
                    new Point3D(1, 0, 0), new Point3D(0, -1, 0), new Point3D(0, 0, 1),
                    2 * Math.PI * r, h, r, r, false);
        }

        if (shape.getType() == basic_shapes_ui_main.CONE) {
            double r = computeCylinderRadius(p1, p2), h = computeCylinderHeight(p1, p2, r), y0 = p1.getY();
            if (kind == face_kind_ui_main.BASE_CAP || kind == face_kind_ui_main.BOTTOM) {
                return new FaceInfo(new Point3D(p1.getX(), y0, p1.getZ()),
                        new Point3D(1, 0, 0), new Point3D(0, 0, -1), new Point3D(0, 1, 0),
                        r * 2.0, r * 2.0, h, r, true);
            }
            return new FaceInfo(new Point3D(p1.getX(), y0 - h * 0.5, p1.getZ()),
                    new Point3D(1, 0, 0), new Point3D(0, -1, 0), new Point3D(0, 0, 1),
                    2 * Math.PI * r, h, r, r, false);
        }

        if (shape.getType() == basic_shapes_ui_main.SPHERE) {
            double r = Math.max(0.1, p1.distance(p2));
            return new FaceInfo(p1, new Point3D(1, 0, 0), new Point3D(0, 0, 1), new Point3D(0, 1, 0),
                    2 * Math.PI * r, Math.PI * r, r, r, false);
        }

        if (shape.getType() == basic_shapes_ui_main.CIRCLE) {
            double r = Math.max(0.1, p1.distance(new Point3D(p2.getX(), p1.getY(), p2.getZ())));
            Point3D u = shape.getUAxis() != null ? shape.getUAxis() : new Point3D(1, 0, 0);
            Point3D v = shape.getVAxis() != null ? shape.getVAxis() : new Point3D(0, 0, 1);
            Point3D n = shape.getFaceNormal() != null ? shape.getFaceNormal() : new Point3D(0, -1, 0);
            return new FaceInfo(p1, u, v, n, r * 2.0, r * 2.0, 0.0, r, true);
        }

        if (shape.getType() == basic_shapes_ui_main.SQUARE || shape.getType() == basic_shapes_ui_main.RECTANGLE ||
            shape.getType() == basic_shapes_ui_main.EQUILATERAL_TRIANGLE || shape.getType() == basic_shapes_ui_main.RIGHT_TRIANGLE) {
            double tw = Math.max(0.1, Math.abs(p2.getX() - p1.getX())), td = Math.max(0.1, Math.abs(p2.getZ() - p1.getZ()));
            if (shape.getType() == basic_shapes_ui_main.SQUARE) { double s = Math.max(tw, td); tw = s; td = s; }
            Point3D tc = new Point3D((p1.getX() + p2.getX()) * 0.5, p1.getY(), (p1.getZ() + p2.getZ()) * 0.5);
            Point3D u = shape.getUAxis() != null ? shape.getUAxis() : new Point3D(1, 0, 0);
            Point3D v = shape.getVAxis() != null ? shape.getVAxis() : new Point3D(0, 0, 1);
            Point3D n = shape.getFaceNormal() != null ? shape.getFaceNormal() : new Point3D(0, -1, 0);
            return new FaceInfo(tc, u, v, n, tw, td, 0.0, 0.0, false);
        }

        double w, h, d;
        Point3D c;
        if (shape.getType() == basic_shapes_ui_main.CUBE) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), s = Math.max(Math.abs(dx), Math.abs(dz));
            w = s; h = s; d = s;
            c = new Point3D(p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5), p1.getY() - s * 0.5, p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5));
        } else {
            w = Math.max(0.1, Math.abs(p2.getX() - p1.getX()));
            d = Math.max(0.1, Math.abs(p2.getZ() - p1.getZ()));
            h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            c = new Point3D((p1.getX() + p2.getX()) * 0.5, p1.getY() - h * 0.5, (p1.getZ() + p2.getZ()) * 0.5);
        }
        var fr = hole_mesh_triangulator_ui_main.getCuboidFaceFrame(c, w, h, d, kind);
        double fw, fh, th = hole_mesh_triangulator_ui_main.getFaceThickness(w, h, d, kind);
        switch (kind) {
            case TOP, BOTTOM, TOP_CAP, BOTTOM_CAP -> { fw = w; fh = d; }
            case FRONT, BACK -> { fw = w; fh = h; }
            case LEFT, RIGHT -> { fw = d; fh = h; }
            default -> { fw = w; fh = d; }
        }
        Point3D orig = fr.origin(), n = fr.n(), uAx = fr.u(), vAx = fr.v();
        if (shape.getDrafts() != null) {
            for (var dr : shape.getDrafts()) {
                if (dr.isValid() && dr.getFaceKind() == kind && Math.abs(dr.getDraftAngle()) > 0.01) {
                    double rad = Math.toRadians(dr.getDraftAngle()), t = h * Math.tan(rad);
                    boolean pullUp = (dr.getPullDirection() == null || dr.getPullDirection().getY() <= 0);
                    boolean neutralTop = (dr.getNeutralKind() == face_kind_ui_main.TOP);
                    double tBot = neutralTop ? (pullUp ? -t : t) : 0.0;
                    double tTop = neutralTop ? 0.0 : (pullUp ? t : -t);
                    double shift = (tBot + tTop) * 0.5;
                    double sgn = pullUp ? 1.0 : -1.0;
                    switch (kind) {
                        case FRONT -> { orig = orig.add(0, 0, -shift); n = new Point3D(0, -sgn * Math.sin(rad), Math.cos(rad)).normalize(); vAx = new Point3D(0, -Math.cos(rad), -sgn * Math.sin(rad)).normalize(); fh = h / Math.cos(rad); }
                        case BACK -> { orig = orig.add(0, 0, shift); n = new Point3D(0, -sgn * Math.sin(rad), -Math.cos(rad)).normalize(); vAx = new Point3D(0, -Math.cos(rad), sgn * Math.sin(rad)).normalize(); fh = h / Math.cos(rad); }
                        case LEFT -> { orig = orig.add(shift, 0, 0); n = new Point3D(-Math.cos(rad), -sgn * Math.sin(rad), 0).normalize(); vAx = new Point3D(sgn * Math.sin(rad), -Math.cos(rad), 0).normalize(); fh = h / Math.cos(rad); }
                        case RIGHT -> { orig = orig.add(-shift, 0, 0); n = new Point3D(Math.cos(rad), -sgn * Math.sin(rad), 0).normalize(); vAx = new Point3D(-sgn * Math.sin(rad), -Math.cos(rad), 0).normalize(); fh = h / Math.cos(rad); }
                    }
                    break;
                }
            }
        }
        return new FaceInfo(orig, uAx, vAx, n, fw, fh, th, 0.0, false);
    }

    public static boolean fitsWithinFace(FaceInfo info, double u, double v, double outerRadius) {
        if (info == null || outerRadius <= 0.0) return false;
        if (info.isCylinderCap()) {
            return (Math.hypot(u, v) + outerRadius <= info.cylinderRadius() + CAD_TOLERANCE);
        }
        double hw = info.width() * 0.5, hh = info.height() * 0.5;
        return (Math.abs(u) + outerRadius <= hw + CAD_TOLERANCE) && (Math.abs(v) + outerRadius <= hh + CAD_TOLERANCE);
    }

    public static boolean fitsProfileWithinFace(FaceInfo info, double u, double v, double halfW, double halfD) {
        if (info == null || halfW <= 0.0 || halfD <= 0.0) return false;
        if (info.isCylinderCap()) {
            return (Math.hypot(Math.abs(u) + halfW, Math.abs(v) + halfD) <= info.cylinderRadius() + CAD_TOLERANCE);
        }
        double hw = info.width() * 0.5, hh = info.height() * 0.5;
        return (Math.abs(u) + halfW <= hw + CAD_TOLERANCE) && (Math.abs(v) + halfD <= hh + CAD_TOLERANCE);
    }

    public static FaceInfo getExtrudeFaceInfo(shape_item_ui_main shape, ui.workspace.drafting.extrude.extrude_feature_ui_main ext, face_kind_ui_main kind) {
        if (shape == null || ext == null) return createDefault();
        var f = ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main.getLocalFaceFrame(shape, ext.getFaceKind());
        if (f == null) return createDefault();
        Point3D n = f.n(), u = f.u(), v = f.v();
        Point3D baseCenter = f.origin().add(u.multiply(ext.getU())).add(v.multiply(ext.getV()));
        double len = ext.getHeight(), rad = ext.getRadius();
        double w = ext.getDiameter(), d = (ext.getWidth2() > 0.1 ? ext.getWidth2() : w);
        Point3D capCenter = baseCenter.add(n.multiply(len)), mid = baseCenter.add(n.multiply(len * 0.5));
        boolean isCircle = (ext.getProfileShape() == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.CIRCLE);

        if (kind == face_kind_ui_main.EXTRUDE_LATERAL) return new FaceInfo(mid, u, n, v, 2 * Math.PI * rad, len, rad, rad, false);
        if (kind == face_kind_ui_main.EXTRUDE_SIDE_POS_U) return new FaceInfo(mid.add(u.multiply(w * 0.5)), v, n, u, d, len, w, 0.0, false);
        if (kind == face_kind_ui_main.EXTRUDE_SIDE_NEG_U) return new FaceInfo(mid.add(u.multiply(-w * 0.5)), v.multiply(-1), n, u.multiply(-1), d, len, w, 0.0, false);
        if (kind == face_kind_ui_main.EXTRUDE_SIDE_POS_V) return new FaceInfo(mid.add(v.multiply(d * 0.5)), u.multiply(-1), n, v, w, len, d, 0.0, false);
        if (kind == face_kind_ui_main.EXTRUDE_SIDE_NEG_V) return new FaceInfo(mid.add(v.multiply(-d * 0.5)), u, n, v.multiply(-1), w, len, d, 0.0, false);
        return new FaceInfo(capCenter, u, v, n, w, (isCircle ? w : d), len, (isCircle ? rad : 0.0), isCircle);
    }

    private static FaceInfo createDefault() {
        return new FaceInfo(Point3D.ZERO, new Point3D(1, 0, 0), new Point3D(0, 0, 1),
                new Point3D(0, -1, 0), 100.0, 100.0, 100.0, 0.0, false);
    }
}
