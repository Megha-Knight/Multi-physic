package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;

public class edge_overlay_ui_main extends Group {

    private static final int CIRCLE_SEGMENTS = 32;
    private static final double LINE_RADIUS = 1.2;
    private final PhongMaterial edgeMat;

    public edge_overlay_ui_main() {
        edgeMat = new PhongMaterial(Color.web("#F59E0B"));
        edgeMat.setSpecularColor(Color.WHITE);
        setVisible(false);
        setMouseTransparent(true);
    }

    public void highlightEdge(shape_item_ui_main shape, topology_edge_ui_main edge) {
        getChildren().clear();
        if (shape == null || edge == null) {
            setVisible(false);
            return;
        }

        if (edge.isLinear()) {
            Point3D p1 = edge.getLocalP1(), p2 = edge.getLocalP2();
            getChildren().addAll(createCylinder(p1, p2, LINE_RADIUS), createSphere(p1, LINE_RADIUS * 1.5), createSphere(p2, LINE_RADIUS * 1.5));
        } else {
            Point3D center = edge.getLocalP1();
            double radius = edge.getLength() * 0.5;
            Point3D norm = edge_picker_ui_main.resolveEdgeNormal(shape, edge);
            Point3D uAxis = (Math.abs(norm.getY()) > 0.8)
                ? new Point3D(1, 0, 0).crossProduct(norm).normalize()
                : new Point3D(0, 1, 0).crossProduct(norm).normalize();
            Point3D vAxis = norm.crossProduct(uAxis).normalize();

            double step = 2.0 * Math.PI / CIRCLE_SEGMENTS;
            Point3D prevPt = null, firstPt = null;
            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                double a = i * step;
                Point3D pt = center.add(uAxis.multiply(radius * Math.cos(a))).add(vAxis.multiply(radius * Math.sin(a)));
                if (firstPt == null) firstPt = pt;
                if (prevPt != null) getChildren().add(createCylinder(prevPt, pt, LINE_RADIUS));
                prevPt = pt;
            }
            if (prevPt != null && firstPt != null) getChildren().add(createCylinder(prevPt, firstPt, LINE_RADIUS));
        }
        setVisible(true);
    }

    public void clearHighlight() {
        getChildren().clear();
        setVisible(false);
    }

    private Cylinder createCylinder(Point3D p1, Point3D p2, double rad) {
        Point3D diff = p2.subtract(p1);
        double len = diff.magnitude();
        Cylinder cyl = new Cylinder(rad, len);
        cyl.setMaterial(edgeMat);
        Point3D mid = p1.add(p2).multiply(0.5);
        cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        Point3D yAxis = new Point3D(0, 1, 0), axis = yAxis.crossProduct(diff);
        double ang = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, yAxis.dotProduct(diff) / (len + 1e-12)))));
        if (axis.magnitude() > 1e-6) cyl.getTransforms().add(new Rotate(ang, axis));
        return cyl;
    }

    private Sphere createSphere(Point3D p, double rad) {
        Sphere s = new Sphere(rad);
        s.setMaterial(edgeMat);
        s.setTranslateX(p.getX()); s.setTranslateY(p.getY()); s.setTranslateZ(p.getZ());
        return s;
    }
}
