package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * face_overlay_ui_main.java
 * Real-time 3D CAD viewport highlight overlay for selected planar and curved faces.
 * Displays perimeter boundary lines, face normal indicator, and face origin.
 */
public class face_overlay_ui_main extends Group {

    private final PhongMaterial borderMat;
    private final PhongMaterial originMat;

    public face_overlay_ui_main() {
        borderMat = new PhongMaterial(Color.web("#00E5FF"));
        borderMat.setSpecularColor(Color.WHITE);
        originMat = new PhongMaterial(Color.web("#F59E0B"));
        originMat.setSpecularColor(Color.WHITE);
        setVisible(false);
        setMouseTransparent(true);
    }

    public void highlightFace(face_reference_ui_main face) {
        getChildren().clear();
        if (face == null) {
            setVisible(false);
            return;
        }

        Point3D n = face.getWorldNormal();
        Point3D origin = face.getFaceOrigin().add(n.multiply(0.15)); // subtle offset to avoid Z-fighting
        Point3D u = face.getUAxis(), v = face.getVAxis();
        double w = face.getFaceWidth(), h = face.getFaceHeight();

        face_kind_ui_main kind = face.getFaceKind();
        if (kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.BOTTOM_CAP || kind == face_kind_ui_main.BASE_CAP) {
            buildCircularBoundary(origin, u, v, w * 0.5);
        } else if (face.isPlanar()) {
            buildRectBoundary(origin, u, v, w, h);
        } else {
            buildCurvedMarker(face.getWorldHitPoint(), n);
        }

        Sphere centerDot = new Sphere(2.2);
        centerDot.setMaterial(originMat);
        centerDot.setTranslateX(origin.getX());
        centerDot.setTranslateY(origin.getY());
        centerDot.setTranslateZ(origin.getZ());
        getChildren().add(centerDot);

        setVisible(true);
    }

    private void buildRectBoundary(Point3D center, Point3D u, Point3D v, double w, double h) {
        double hw = w * 0.5, hh = h * 0.5;
        Point3D c1 = center.add(u.multiply(hw)).add(v.multiply(hh));
        Point3D c2 = center.add(u.multiply(-hw)).add(v.multiply(hh));
        Point3D c3 = center.add(u.multiply(-hw)).add(v.multiply(-hh));
        Point3D c4 = center.add(u.multiply(hw)).add(v.multiply(-hh));

        getChildren().add(createLine(c1, c2));
        getChildren().add(createLine(c2, c3));
        getChildren().add(createLine(c3, c4));
        getChildren().add(createLine(c4, c1));
    }

    private void buildCircularBoundary(Point3D center, Point3D u, Point3D v, double r) {
        int segs = 32;
        Point3D prev = null, first = null;
        for (int i = 0; i < segs; i++) {
            double ang = i * 2.0 * Math.PI / segs;
            Point3D pt = center.add(u.multiply(r * Math.cos(ang))).add(v.multiply(r * Math.sin(ang)));
            if (i == 0) first = pt;
            if (prev != null) getChildren().add(createLine(prev, pt));
            prev = pt;
        }
        if (prev != null && first != null) getChildren().add(createLine(prev, first));
    }

    private void buildCurvedMarker(Point3D hitPt, Point3D n) {
        Sphere s = new Sphere(3.0);
        s.setMaterial(borderMat);
        s.setTranslateX(hitPt.getX());
        s.setTranslateY(hitPt.getY());
        s.setTranslateZ(hitPt.getZ());
        getChildren().add(s);
        getChildren().add(createLine(hitPt, hitPt.add(n.multiply(15.0))));
    }

    private Cylinder createLine(Point3D p1, Point3D p2) {
        Point3D diff = p2.subtract(p1);
        double len = diff.magnitude();
        Cylinder cyl = new Cylinder(0.8, Math.max(0.1, len));
        cyl.setMaterial(borderMat);

        Point3D mid = p1.midpoint(p2);
        cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));

        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axis = yAxis.crossProduct(diff);
        if (axis.magnitude() > 1e-4) {
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, yAxis.dotProduct(diff.normalize())))));
            cyl.getTransforms().add(new Rotate(angle, axis.normalize()));
        } else if (diff.getY() < 0) {
            cyl.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        }
        return cyl;
    }
}
