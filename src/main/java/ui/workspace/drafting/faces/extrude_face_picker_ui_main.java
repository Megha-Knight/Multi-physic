package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

public final class extrude_face_picker_ui_main {

    private extrude_face_picker_ui_main() {}

    public static face_picker_ui_main.Hit intersectExtrusions(shape_item_ui_main shape, Point3D ro, Point3D rd) {
        if (shape == null || !shape.hasExtrusions() || ro == null || rd == null) return null;
        face_picker_ui_main.Hit best = null;

        for (extrude_feature_ui_main ext : shape.getExtrusions()) {
            if (!ext.isValid() || ext.getState() == ui.workspace.drafting.features.feature_state_ui_main.INVALID) continue;
            Frame f = extrude_mesh_builder_ui_main.getLocalFaceFrame(shape, ext.getFaceKind());
            if (f == null) continue;

            Point3D n = f.n(), u = f.u(), v = f.v();
            Point3D baseCenter = f.origin().add(u.multiply(ext.getU())).add(v.multiply(ext.getV()));
            double len = ext.getHeight(), rad = ext.getRadius();
            double w = ext.getDiameter(), d = (ext.getWidth2() > 0.1) ? ext.getWidth2() : w;
            Point3D capCenter = baseCenter.add(n.multiply(len));

            // 1. Intersect Cap face (planar disk or rect)
            double denom = rd.dotProduct(n);
            if (denom < -1e-5) {
                double t = capCenter.subtract(ro).dotProduct(n) / denom;
                if (t > 1e-4 && (best == null || t < best.t())) {
                    Point3D p = ro.add(rd.multiply(t));
                    Point3D delta = p.subtract(capCenter);
                    double du = delta.dotProduct(u), dv = delta.dotProduct(v);
                    boolean hit = (ext.getProfileShape() == CutoutShape.CIRCLE)
                        ? (du * du + dv * dv <= rad * rad + 1e-4)
                        : (Math.abs(du) <= w * 0.5 + 1e-4 && Math.abs(dv) <= d * 0.5 + 1e-4);
                    if (hit) {
                        best = new face_picker_ui_main.Hit(
                            t, face_kind_ui_main.EXTRUDE_CAP, p, n, capCenter, u, v, w, (ext.getProfileShape() == CutoutShape.CIRCLE ? w : d), ext.getId()
                        );
                    }
                }
            }

            // 2. Intersect Lateral faces
            if (ext.getProfileShape() == CutoutShape.CIRCLE) {
                best = checkCylinderLateral(ro, rd, baseCenter, n, u, v, rad, len, ext.getId(), best);
            } else {
                best = checkBoxSides(ro, rd, baseCenter, n, u, v, w, d, len, ext.getId(), best);
            }
        }
        return best;
    }

    private static face_picker_ui_main.Hit checkCylinderLateral(Point3D ro, Point3D rd, Point3D baseCenter,
                                                               Point3D n, Point3D u, Point3D v, double rad,
                                                               double len, String extId, face_picker_ui_main.Hit prev) {
        Point3D rOrigin = ro.subtract(baseCenter);
        double roU = rOrigin.dotProduct(u), roV = rOrigin.dotProduct(v);
        double rdU = rd.dotProduct(u), rdV = rd.dotProduct(v);
        double a = rdU * rdU + rdV * rdV, b = 2 * (roU * rdU + roV * rdV), c = roU * roU + roV * roV - rad * rad;
        double disc = b * b - 4 * a * c;
        if (disc >= 0 && a > 1e-6) {
            double t = (-b - Math.sqrt(disc)) / (2 * a);
            if (t > 1e-4 && (prev == null || t < prev.t())) {
                Point3D p = ro.add(rd.multiply(t));
                double hVal = p.subtract(baseCenter).dotProduct(n);
                if (hVal >= -1e-4 && hVal <= len + 1e-4) {
                    Point3D localDelta = p.subtract(baseCenter);
                    Point3D nLat = u.multiply(localDelta.dotProduct(u)).add(v.multiply(localDelta.dotProduct(v))).normalize();
                    Point3D uAxis = n.crossProduct(nLat).normalize();
                    return new face_picker_ui_main.Hit(
                        t, face_kind_ui_main.EXTRUDE_LATERAL, p, nLat, baseCenter.add(n.multiply(len * 0.5)),
                        uAxis, n, 2 * Math.PI * rad, len, extId
                    );
                }
            }
        }
        return prev;
    }

    private static face_picker_ui_main.Hit checkBoxSides(Point3D ro, Point3D rd, Point3D baseCenter,
                                                          Point3D n, Point3D u, Point3D v, double w, double d,
                                                          double len, String extId,
                                                          face_picker_ui_main.Hit prev) {
        Point3D mid = baseCenter.add(n.multiply(len * 0.5));
        Point3D[] normals = new Point3D[]{u, u.multiply(-1), v, v.multiply(-1)};
        Point3D[] uAxes = new Point3D[]{v, v, u, u};
        face_kind_ui_main[] sideKinds = new face_kind_ui_main[]{
            face_kind_ui_main.EXTRUDE_SIDE_POS_U, face_kind_ui_main.EXTRUDE_SIDE_NEG_U,
            face_kind_ui_main.EXTRUDE_SIDE_POS_V, face_kind_ui_main.EXTRUDE_SIDE_NEG_V
        };
        Point3D[] origins = new Point3D[]{
            mid.add(u.multiply(w * 0.5)), mid.add(u.multiply(-w * 0.5)),
            mid.add(v.multiply(d * 0.5)), mid.add(v.multiply(-d * 0.5))
        };
        double[] widths = new double[]{d, d, w, w};

        for (int i = 0; i < 4; i++) {
            Point3D norm = normals[i];
            double denom = rd.dotProduct(norm);
            if (denom >= -1e-5) continue;
            double t = origins[i].subtract(ro).dotProduct(norm) / denom;
            if (t <= 1e-4 || (prev != null && t >= prev.t())) continue;
            Point3D p = ro.add(rd.multiply(t)), delta = p.subtract(origins[i]);
            double du = delta.dotProduct(uAxes[i]), dn = delta.dotProduct(n);
            if (Math.abs(du) <= widths[i] * 0.5 + 1e-3 && Math.abs(dn) <= len * 0.5 + 1e-3) {
                prev = new face_picker_ui_main.Hit(t, sideKinds[i], p, norm, origins[i], uAxes[i], n, widths[i], len, extId);
            }
        }
        return prev;
    }
}
