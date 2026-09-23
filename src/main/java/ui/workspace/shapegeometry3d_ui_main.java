package ui.workspace;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;

/**
 * shapegeometry3d_ui_main.java
 * High-precision 3D CAD primitive geometry generator for Cube, Cylinder, Sphere, and Cone.
 */
public final class shapegeometry3d_ui_main {

    private shapegeometry3d_ui_main() {}

    public static Node createCube(Point3D p1, Point3D p2, boolean isPreview) {
        double dx = p2.getX() - p1.getX();
        double dz = p2.getZ() - p1.getZ();
        double s = Math.max(Math.abs(dx), Math.abs(dz));
        if (s < 0.2) return new Group();

        Box box = new Box(s, s, s);
        box.setMaterial(shapegeometry_ui_main.createMaterial(isPreview));
        double cx = p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5);
        double cz = p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5);
        box.setTranslateX(cx);
        box.setTranslateY(-s * 0.5);
        box.setTranslateZ(cz);

        return new Group(box);
    }

    public static Node createCylinder(Point3D center, Point3D current, boolean isPreview) {
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2) return new Group();

        double height = Math.max(6.0, r * 2.0);
        Cylinder cyl = new Cylinder(r, height);
        cyl.setMaterial(shapegeometry_ui_main.createMaterial(isPreview));
        cyl.setTranslateX(center.getX());
        cyl.setTranslateY(-height * 0.5);
        cyl.setTranslateZ(center.getZ());

        return new Group(cyl);
    }

    public static Node createSphere(Point3D center, Point3D current, boolean isPreview) {
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2) return new Group();

        Sphere sph = new Sphere(r);
        sph.setMaterial(shapegeometry_ui_main.createMaterial(isPreview));
        sph.setTranslateX(center.getX());
        sph.setTranslateY(-r);
        sph.setTranslateZ(center.getZ());

        return new Group(sph);
    }

    public static Node createCone(Point3D center, Point3D current, boolean isPreview) {
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2) return new Group();

        double height = Math.max(6.0, r * 2.0);
        Node cone = meshhelper_ui_main.createUpwardCone(r, height, shapegeometry_ui_main.createMaterial(isPreview));
        cone.setTranslateX(center.getX());
        cone.setTranslateY(0);
        cone.setTranslateZ(center.getZ());

        return new Group(cone);
    }
}
