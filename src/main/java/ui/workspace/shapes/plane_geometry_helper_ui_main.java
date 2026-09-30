package ui.workspace.shapes;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import ui.framework_ui_main;

import java.util.List;

/**
 * plane_geometry_helper_ui_main.java
 * High-precision 2D CAD profile generator on arbitrary 3D planar coordinate systems (u, v, normal).
 */
public final class plane_geometry_helper_ui_main {

    private static final double Z_OFFSET = 0.15;

    private plane_geometry_helper_ui_main() {}

    public static Node createCircle(Point3D center, Point3D current, Point3D u, Point3D v, Point3D n,
                                    boolean isPreview, boolean isSelected) {
        Group g = new Group();
        PhongMaterial mat = shape_geometry_ui_main.createMaterial(isPreview, isSelected);
        double r = center.distance(current);
        if (r < 0.2) return g;

        Point3D off = n.normalize().multiply(Z_OFFSET);
        Point3D c = center.add(off);
        Point3D uu = u.normalize(), vv = v.normalize();

        int sides = 120;
        double step = 2.0 * Math.PI / sides;
        for (int i = 0; i < sides; i++) {
            double a1 = i * step, a2 = (i + 1) * step;
            Point3D pt1 = c.add(uu.multiply(r * Math.cos(a1))).add(vv.multiply(r * Math.sin(a1)));
            Point3D pt2 = c.add(uu.multiply(r * Math.cos(a2))).add(vv.multiply(r * Math.sin(a2)));
            g.getChildren().add(shape_geometry_ui_main.createSegment(pt1, pt2, mat));
        }

        Sphere cDot = new Sphere(framework_ui_main.DRAFT_LINE_RADIUS * 2.0);
        cDot.setMaterial(mat);
        cDot.setTranslateX(c.getX()); cDot.setTranslateY(c.getY()); cDot.setTranslateZ(c.getZ());
        g.getChildren().add(cDot);
        if (isPreview) g.getChildren().add(shape_geometry_ui_main.createSegment(c, current.add(off), mat));
        return g;
    }

    public static Node createRectangle(Point3D start, Point3D current, Point3D u, Point3D v, Point3D n,
                                       boolean isPreview, boolean isSelected) {
        if (start.distance(current) < 0.2) return new Group();
        Point3D off = n.normalize().multiply(Z_OFFSET), s = start.add(off);
        Point3D delta = current.subtract(start);
        Point3D uu = u.normalize(), vv = v.normalize();
        double w = delta.dotProduct(uu), h = delta.dotProduct(vv);

        List<Point3D> pts = List.of(
            s, s.add(uu.multiply(w)), s.add(uu.multiply(w)).add(vv.multiply(h)), s.add(vv.multiply(h))
        );
        return shape_geometry_ui_main.createPolyline(pts, true, shape_geometry_ui_main.createMaterial(isPreview, isSelected));
    }

    public static Node createSquare(Point3D start, Point3D current, Point3D u, Point3D v, Point3D n,
                                    boolean isPreview, boolean isSelected) {
        Point3D delta = current.subtract(start);
        Point3D uu = u.normalize(), vv = v.normalize();
        double du = delta.dotProduct(uu), dv = delta.dotProduct(vv);
        double s = Math.max(Math.abs(du), Math.abs(dv));
        if (s < 0.2) return new Group();

        double su = (du >= 0) ? s : -s, sv = (dv >= 0) ? s : -s;
        Point3D p0 = start.add(n.normalize().multiply(Z_OFFSET));
        List<Point3D> pts = List.of(
            p0, p0.add(uu.multiply(su)), p0.add(uu.multiply(su)).add(vv.multiply(sv)), p0.add(vv.multiply(sv))
        );
        return shape_geometry_ui_main.createPolyline(pts, true, shape_geometry_ui_main.createMaterial(isPreview, isSelected));
    }

    public static Node createEquilateralTriangle(Point3D start, Point3D current, Point3D u, Point3D v, Point3D n,
                                                 boolean isPreview, boolean isSelected) {
        double dist = start.distance(current);
        if (dist < 0.2) return new Group();
        Point3D off = n.normalize().multiply(Z_OFFSET), s = start.add(off), e = current.add(off);
        Point3D delta = current.subtract(start);
        Point3D d = delta.multiply(1.0 / dist);
        Point3D perp = n.normalize().crossProduct(d).normalize();
        double h = dist * Math.sqrt(3.0) / 2.0;
        Point3D apex = s.add(d.multiply(dist * 0.5)).add(perp.multiply(h));

        return shape_geometry_ui_main.createPolyline(List.of(s, e, apex), true,
            shape_geometry_ui_main.createMaterial(isPreview, isSelected));
    }

    public static Node createRightTriangle(Point3D start, Point3D current, Point3D u, Point3D v, Point3D n,
                                           boolean isPreview, boolean isSelected) {
        if (start.distance(current) < 0.2) return new Group();
        Point3D off = n.normalize().multiply(Z_OFFSET), s = start.add(off);
        Point3D delta = current.subtract(start);
        Point3D uu = u.normalize(), vv = v.normalize();
        double w = delta.dotProduct(uu), h = delta.dotProduct(vv);

        Point3D corner = s.add(uu.multiply(w)), tip = s.add(uu.multiply(w)).add(vv.multiply(h));
        PhongMaterial mat = shape_geometry_ui_main.createMaterial(isPreview, isSelected);
        Group g = (Group) shape_geometry_ui_main.createPolyline(List.of(s, corner, tip), true, mat);

        double sw = Math.signum(w), sh = Math.signum(h);
        if (sw != 0 && sh != 0) {
            double sz = Math.min(3.0, Math.min(Math.abs(w), Math.abs(h)) * 0.25);
            if (sz > 0.4) {
                Point3D m1 = corner.add(uu.multiply(-sw * sz));
                Point3D m2 = m1.add(vv.multiply(sh * sz));
                Point3D m3 = corner.add(vv.multiply(sh * sz));
                g.getChildren().addAll(shape_geometry_ui_main.createSegment(m1, m2, mat), shape_geometry_ui_main.createSegment(m2, m3, mat));
            }
        }
        return g;
    }
}
