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
        return Math.max(0.1, p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
    }

    public static double computeCylinderHeight(Point3D p1, Point3D p2, double r) {
        if (p2 == null) return Math.max(6.0, r * 2.0);
        return Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
    }

    public static FaceInfo getFaceInfo(shape_item_ui_main shape, face_kind_ui_main kind) {
        if (shape == null || kind == null) return createDefault();
        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        if (shape.getType() == basic_shapes_ui_main.CYLINDER) {
            double r = computeCylinderRadius(p1, p2);
            double h = computeCylinderHeight(p1, p2, r);
            if (kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.TOP) {
                return new FaceInfo(new Point3D(p1.getX(), -h, p1.getZ()),
                        new Point3D(1, 0, 0), new Point3D(0, 0, 1), new Point3D(0, -1, 0),
                        r * 2.0, r * 2.0, h, r, true);
            }
            if (kind == face_kind_ui_main.BOTTOM_CAP || kind == face_kind_ui_main.BOTTOM) {
                return new FaceInfo(new Point3D(p1.getX(), 0, p1.getZ()),
                        new Point3D(1, 0, 0), new Point3D(0, 0, -1), new Point3D(0, 1, 0),
                        r * 2.0, r * 2.0, h, r, true);
            }
            return new FaceInfo(new Point3D(p1.getX(), -h * 0.5, p1.getZ()),
                    new Point3D(1, 0, 0), new Point3D(0, -1, 0), new Point3D(0, 0, 1),
                    2 * Math.PI * r, h, r, r, false);
        }

        if (shape.getType() == basic_shapes_ui_main.CONE) {
            double r = computeCylinderRadius(p1, p2);
            double h = computeCylinderHeight(p1, p2, r);
            if (kind == face_kind_ui_main.BASE_CAP || kind == face_kind_ui_main.BOTTOM) {
                return new FaceInfo(new Point3D(p1.getX(), 0, p1.getZ()),
                        new Point3D(1, 0, 0), new Point3D(0, 0, -1), new Point3D(0, 1, 0),
                        r * 2.0, r * 2.0, h, r, true);
            }
            return new FaceInfo(new Point3D(p1.getX(), -h * 0.5, p1.getZ()),
                    new Point3D(1, 0, 0), new Point3D(0, -1, 0), new Point3D(0, 0, 1),
                    2 * Math.PI * r, h, r, r, false);
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
        return new FaceInfo(fr.origin(), fr.u(), fr.v(), fr.n(), fw, fh, th, 0.0, false);
    }

    public static boolean fitsWithinFace(FaceInfo info, double u, double v, double outerRadius) {
        if (info == null || outerRadius <= 0.0) return false;
        if (info.isCylinderCap()) {
            return (Math.hypot(u, v) + outerRadius <= info.cylinderRadius() + CAD_TOLERANCE);
        }
        double hw = info.width() * 0.5, hh = info.height() * 0.5;
        return (Math.abs(u) + outerRadius <= hw + CAD_TOLERANCE) && (Math.abs(v) + outerRadius <= hh + CAD_TOLERANCE);
    }

    private static FaceInfo createDefault() {
        return new FaceInfo(Point3D.ZERO, new Point3D(1, 0, 0), new Point3D(0, 0, 1),
                new Point3D(0, -1, 0), 100.0, 100.0, 100.0, 0.0, false);
    }
}
