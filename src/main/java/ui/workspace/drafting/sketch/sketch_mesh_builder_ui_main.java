package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.List;

public final class sketch_mesh_builder_ui_main {

    private sketch_mesh_builder_ui_main() {}

    public static Group buildVisualGroup(sketch_feature_ui_main sketch, boolean isEditing) {
        Group grp = new Group();
        if (sketch == null || !sketch.isVisible()) return grp;

        sketch_coord_system_ui_main cs = sketch.getCoordSystem();
        Point3D normal = cs.getNormal();
        Point3D offset = normal.multiply(0.2); // lift slightly above surface

        // Draw Origin indicator if editing
        if (isEditing) {
            Point3D orig = cs.getOrigin().add(offset);
            grp.getChildren().add(createDot(orig, 1.5, Color.web("#EF4444")));
            grp.getChildren().add(createSegment(orig, orig.add(cs.getUAxis().multiply(15)), 0.6, Color.web("#EF4444")));
            grp.getChildren().add(createSegment(orig, orig.add(cs.getVAxis().multiply(15)), 0.6, Color.web("#10B981")));
        }

        for (sketch_entity_ui_main e : sketch.getEntities()) {
            Color col = e.isSelected() ? Color.web("#F59E0B") : (e.isConstruction() ? Color.web("#94A3B8") : Color.web("#0284C7"));
            if (e instanceof sketch_line_ui_main l) {
                Point3D p1 = cs.toWorldPoint(l.getStart()).add(offset);
                Point3D p2 = cs.toWorldPoint(l.getEnd()).add(offset);
                grp.getChildren().add(createSegment(p1, p2, 0.8, col));
                if (isEditing) {
                    grp.getChildren().add(createDot(p1, 1.2, col));
                    grp.getChildren().add(createDot(p2, 1.2, col));
                }
            } else if (e instanceof sketch_rect_ui_main r) {
                for (sketch_line_ui_main rl : r.toLines()) {
                    Point3D p1 = cs.toWorldPoint(rl.getStart()).add(offset);
                    Point3D p2 = cs.toWorldPoint(rl.getEnd()).add(offset);
                    grp.getChildren().add(createSegment(p1, p2, 0.8, col));
                }
            } else if (e instanceof sketch_circle_ui_main c) {
                int steps = 36;
                Point3D prev = null, first = null;
                for (int i = 0; i <= steps; i++) {
                    double angle = (2.0 * Math.PI * i) / steps;
                    double lx = c.getCenter().x() + c.getRadius() * Math.cos(angle);
                    double ly = c.getCenter().y() + c.getRadius() * Math.sin(angle);
                    Point3D cur = cs.toWorldPoint(lx, ly).add(offset);
                    if (prev != null) grp.getChildren().add(createSegment(prev, cur, 0.8, col));
                    else first = cur;
                    prev = cur;
                }
                if (isEditing) grp.getChildren().add(createDot(cs.toWorldPoint(c.getCenter()).add(offset), 1.2, col));
            }
        }
        return grp;
    }

    public static Sphere createDot(Point3D pt, double radius, Color color) {
        Sphere s = new Sphere(radius);
        s.setMaterial(new PhongMaterial(color));
        s.setTranslateX(pt.getX()); s.setTranslateY(pt.getY()); s.setTranslateZ(pt.getZ());
        return s;
    }

    public static Cylinder createSegment(Point3D p1, Point3D p2, double radius, Color color) {
        Point3D delta = p2.subtract(p1);
        double len = delta.magnitude();
        if (len < 1e-4) len = 1e-4;

        Cylinder cyl = new Cylinder(radius, len);
        cyl.setMaterial(new PhongMaterial(color));

        Point3D mid = p1.midpoint(p2);
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axis = delta.crossProduct(yAxis);
        double angle = -Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, delta.normalize().dotProduct(yAxis)))));

        cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        if (axis.magnitude() > 1e-6) {
            cyl.getTransforms().add(new Rotate(angle, axis.normalize()));
        }
        return cyl;
    }
}
