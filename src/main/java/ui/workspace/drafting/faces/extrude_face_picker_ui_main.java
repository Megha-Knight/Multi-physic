package ui.workspace.drafting.faces;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import java.util.List;
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
                    boolean hit;
                    if (ext.getProfileShape() == CutoutShape.CIRCLE) {
                        hit = (du * du + dv * dv <= rad * rad + 1e-4);
                    } else if (ext.getProfileShape() == CutoutShape.RIGHT_TRIANGLE) {
                        hit = (du >= -w * 0.5 - 1e-4 && dv >= -d * 0.5 - 1e-4 && (du / w + dv / d) <= 0.0 + 1e-4);
                    } else if (ext.getProfileShape() == CutoutShape.EQUILATERAL_TRIANGLE) {
                        double ht = w * Math.sqrt(3.0) * 0.5;
                        hit = (dv >= -ht / 3.0 - 1e-4 && dv <= 2.0 * ht / 3.0 + 1e-4 && Math.abs(du) <= (2.0 * ht / 3.0 - dv) / Math.sqrt(3.0) + 1e-4);
                    } else {
                        hit = (Math.abs(du) <= w * 0.5 + 1e-4 && Math.abs(dv) <= d * 0.5 + 1e-4);
                    }
                    if (hit) {
                        boolean triangular = ext.getProfileShape() == CutoutShape.RIGHT_TRIANGLE
                            || ext.getProfileShape() == CutoutShape.EQUILATERAL_TRIANGLE;
                        best = new face_picker_ui_main.Hit(t, face_kind_ui_main.EXTRUDE_CAP, p, n, capCenter, u, v,
                            w, (ext.getProfileShape() == CutoutShape.CIRCLE ? w : d), ext.getId(),
                            triangular ? "CAP_TRIANGLE" : null, triangular ? trianglePolygon(capCenter, u, v, w, d, ext.getProfileShape()) : List.of());
                    }
                }
            }

            // 2. Intersect Lateral faces
            if (ext.getProfileShape() == CutoutShape.CIRCLE) {
                best = checkCylinderLateral(ro, rd, baseCenter, n, u, v, rad, len, ext.getId(), best);
            } else if (ext.getProfileShape() == CutoutShape.RIGHT_TRIANGLE || ext.getProfileShape() == CutoutShape.EQUILATERAL_TRIANGLE) {
                best = checkPrismSides(ro, rd, f, ext, best);
            } else {
                best = checkBoxSides(ro, rd, baseCenter, n, u, v, w, d, len, ext.getId(), best);
            }
        }
        return best;
    }

    private static face_picker_ui_main.Hit checkPrismSides(Point3D ro, Point3D rd, Frame f, extrude_feature_ui_main ext, face_picker_ui_main.Hit prev) {
        double w = ext.getDiameter(), len = ext.getHeight(), d = (ext.getWidth2() > 0.1) ? ext.getWidth2() : w;
        Point3D baseCenter = f.origin().add(f.u().multiply(ext.getU())).add(f.v().multiply(ext.getV()));
        Point2D[] p2 = new Point2D[3];
        if (ext.getProfileShape() == CutoutShape.RIGHT_TRIANGLE) {
            p2[0] = new Point2D(-w * 0.5, -d * 0.5); p2[1] = new Point2D(w * 0.5, -d * 0.5); p2[2] = new Point2D(-w * 0.5, d * 0.5);
        } else {
            double ht = w * Math.sqrt(3.0) * 0.5;
            p2[0] = new Point2D(-w * 0.5, -ht / 3.0); p2[1] = new Point2D(w * 0.5, -ht / 3.0); p2[2] = new Point2D(0.0, 2.0 * ht / 3.0);
        }
        for (int i = 0; i < 3; i++) {
            Point2D v0 = p2[i], v1 = p2[(i + 1) % 3];
            Point3D b0 = baseCenter.add(f.u().multiply(v0.getX())).add(f.v().multiply(v0.getY()));
            Point3D b1 = baseCenter.add(f.u().multiply(v1.getX())).add(f.v().multiply(v1.getY()));
            Point3D edgeDir = b1.subtract(b0); double sideWidth = edgeDir.magnitude();
            if (sideWidth < 1e-4) continue;
            Point3D uSide = edgeDir.multiply(1.0 / sideWidth);
            Point3D sideNorm = uSide.crossProduct(f.n()).normalize();
            Point3D centroid = baseCenter.add(f.u().multiply((p2[0].getX() + p2[1].getX() + p2[2].getX()) / 3.0))
                .add(f.v().multiply((p2[0].getY() + p2[1].getY() + p2[2].getY()) / 3.0));
            if (sideNorm.dotProduct(centroid.subtract(b0.midpoint(b1))) > 0.0) sideNorm = sideNorm.multiply(-1.0);
            Point3D sideOrigin = b0.midpoint(b1).add(f.n().multiply(len * 0.5));
            double denom = rd.dotProduct(sideNorm);
            if (denom >= -1e-5) continue;
            double t = sideOrigin.subtract(ro).dotProduct(sideNorm) / denom;
            if (t <= 1e-4 || (prev != null && t >= prev.t())) continue;
            Point3D hitP = ro.add(rd.multiply(t)), delta = hitP.subtract(sideOrigin);
            double du = delta.dotProduct(uSide), dn = delta.dotProduct(f.n());
            if (Math.abs(du) <= sideWidth * 0.5 + 1e-3 && Math.abs(dn) <= len * 0.5 + 1e-3) {
                Point3D top0 = b0.add(f.n().multiply(len)), top1 = b1.add(f.n().multiply(len));
                String[] sideIds = {"SIDE_E01", "SIDE_E12", "SIDE_E20"};
                prev = new face_picker_ui_main.Hit(t, face_kind_ui_main.EXTRUDE_LATERAL, hitP, sideNorm,
                    sideOrigin, uSide, f.n(), sideWidth, len, ext.getId(), sideIds[i], List.of(b0, b1, top1, top0));
            }
        }
        return prev;
    }

    public static List<Point3D> resolveLocalPolygon(shape_item_ui_main shape, extrude_feature_ui_main ext, String part) {
        if (shape == null || ext == null || part == null) return List.of();
        if (ext.getProfileShape() != CutoutShape.RIGHT_TRIANGLE && ext.getProfileShape() != CutoutShape.EQUILATERAL_TRIANGLE) return List.of();
        Frame f = extrude_mesh_builder_ui_main.getLocalFaceFrame(shape, ext.getFaceKind());
        if (f == null) return List.of();
        double w = ext.getDiameter(), d = ext.getWidth2() > 0.1 ? ext.getWidth2() : w, len = ext.getHeight();
        Point3D base = f.origin().add(f.u().multiply(ext.getU())).add(f.v().multiply(ext.getV()));
        if (part.equals("CAP_TRIANGLE")) return trianglePolygon(base.add(f.n().multiply(len)), f.u(), f.v(), w, d, ext.getProfileShape());
        if (!part.startsWith("SIDE_")) return List.of();
        Point2D[] q = profilePoints(ext.getProfileShape(), w, d);
        int index = part.equals("SIDE_E01") ? 0 : part.equals("SIDE_E12") ? 1 : part.equals("SIDE_E20") ? 2 : -1;
        if (index < 0) return List.of();
        Point3D b0 = base.add(f.u().multiply(q[index].getX())).add(f.v().multiply(q[index].getY()));
        Point2D q1 = q[(index + 1) % 3];
        Point3D b1 = base.add(f.u().multiply(q1.getX())).add(f.v().multiply(q1.getY()));
        return List.of(b0, b1, b1.add(f.n().multiply(len)), b0.add(f.n().multiply(len)));
    }

    private static Point2D[] profilePoints(CutoutShape profile, double w, double d) {
        if (profile == CutoutShape.RIGHT_TRIANGLE) return new Point2D[]{new Point2D(-w * 0.5, -d * 0.5),
            new Point2D(w * 0.5, -d * 0.5), new Point2D(-w * 0.5, d * 0.5)};
        double h = w * Math.sqrt(3.0) * 0.5;
        return new Point2D[]{new Point2D(-w * 0.5, -h / 3.0), new Point2D(w * 0.5, -h / 3.0), new Point2D(0, 2 * h / 3.0)};
    }

    private static List<Point3D> trianglePolygon(Point3D c, Point3D u, Point3D v, double w, double d, CutoutShape profile) {
        if (profile == CutoutShape.RIGHT_TRIANGLE) return List.of(
            c.add(u.multiply(-w * 0.5)).add(v.multiply(-d * 0.5)),
            c.add(u.multiply(w * 0.5)).add(v.multiply(-d * 0.5)),
            c.add(u.multiply(-w * 0.5)).add(v.multiply(d * 0.5)));
        double h = w * Math.sqrt(3.0) * 0.5;
        return List.of(c.add(u.multiply(-w * 0.5)).add(v.multiply(-h / 3.0)),
            c.add(u.multiply(w * 0.5)).add(v.multiply(-h / 3.0)), c.add(v.multiply(2.0 * h / 3.0)));
    }

    private static face_picker_ui_main.Hit checkCylinderLateral(Point3D ro, Point3D rd, Point3D baseCenter,
                                                               Point3D n, Point3D u, Point3D v, double rad,
                                                               double len, String extId, face_picker_ui_main.Hit prev) {
        Point3D rOrigin = ro.subtract(baseCenter);
        double roU = rOrigin.dotProduct(u), roV = rOrigin.dotProduct(v);
        double rdU = rd.dotProduct(u), rdV = rd.dotProduct(v);
        double a = rdU * rdU + rdV * rdV, b = 2 * (roU * rdU + roV * rdV), c = roU * roU + roV * roV - rad * rad;
        double disc = b * b - 4 * a * c;
        if (disc < 0 || a <= 1e-6) return prev;
        double root = Math.sqrt(disc);
        double[] roots = {(-b - root) / (2 * a), (-b + root) / (2 * a)};
        for (double t : roots) {
            if (t <= 1e-4 || (prev != null && t >= prev.t())) continue;
            Point3D p = ro.add(rd.multiply(t));
            double hVal = p.subtract(baseCenter).dotProduct(n);
            if (hVal < -1e-4 || hVal > len + 1e-4) continue;
            Point3D localDelta = p.subtract(baseCenter);
            Point3D nLat = u.multiply(localDelta.dotProduct(u)).add(v.multiply(localDelta.dotProduct(v))).normalize();
            if (rd.dotProduct(nLat) >= -1e-5) continue;
            Point3D uAxis = n.crossProduct(nLat).normalize();
            prev = new face_picker_ui_main.Hit(t, face_kind_ui_main.EXTRUDE_LATERAL, p, nLat,
                baseCenter.add(n.multiply(len * 0.5)), uAxis, n, 2 * Math.PI * rad, len, extId);
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
