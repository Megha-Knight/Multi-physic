package ui.workspace.shapes;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import ui.framework_ui_main;

/**
 * shape_geometry_3d_ui_main.java
 * High-precision 3D CAD primitive geometry generator for Cube, Cylinder, Sphere, and Cone.
 */
public final class shape_geometry_3d_ui_main {

    private shape_geometry_3d_ui_main() {}

    public static PhongMaterial createMaterial(boolean isPreview, boolean isSelected) {
        Color c = isPreview ? Color.web(framework_ui_main.DRAFT_PREVIEW_COLOR)
                : isSelected ? Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)
                : Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR);
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.web(isSelected ? "#BAE6FD" : "#5A6B7C"));
        mat.setSpecularPower(48.0);
        return mat;
    }

    public static Node createCube(Point3D p1, Point3D p2, boolean isPreview) {
        return createCube(p1, p2, isPreview, false);
    }

    public static Node createCube(Point3D p1, Point3D p2, boolean isPreview, boolean isSelected) {
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
        double s = Math.max(Math.abs(dx), Math.abs(dz));
        if (s < 0.2) return new Group();

        Box box = new Box(s, s, s);
        box.setMaterial(createMaterial(isPreview, isSelected));
        double cx = p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5);
        double cz = p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5);
        box.setTranslateX(cx);
        box.setTranslateY(-s * 0.5);
        box.setTranslateZ(cz);
        return new Group(box);
    }

    public static Node createCylinder(Point3D center, Point3D current, boolean isPreview) {
        return createCylinder(center, current, isPreview, false);
    }

    public static Node createCylinder(Point3D center, Point3D current, boolean isPreview, boolean isSelected) {
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2) return new Group();

        double height = Math.abs(current.getY()) > 0.1 ? Math.abs(current.getY()) : Math.max(6.0, r * 2.0);
        Cylinder cyl = new Cylinder(r, height);
        cyl.setMaterial(createMaterial(isPreview, isSelected));
        cyl.setTranslateX(center.getX());
        cyl.setTranslateY(-height * 0.5);
        cyl.setTranslateZ(center.getZ());
        return new Group(cyl);
    }

    public static Node createSphere(Point3D center, Point3D current, boolean isPreview) {
        return createSphere(center, current, isPreview, false);
    }

    public static Node createSphere(Point3D center, Point3D current, boolean isPreview, boolean isSelected) {
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2) return new Group();

        Sphere sph = new Sphere(r);
        sph.setMaterial(createMaterial(isPreview, isSelected));
        sph.setTranslateX(center.getX());
        sph.setTranslateY(-r);
        sph.setTranslateZ(center.getZ());
        return new Group(sph);
    }

    public static Node createCone(Point3D center, Point3D current, boolean isPreview) {
        return createCone(center, current, isPreview, false);
    }

    public static Node createCone(Point3D center, Point3D current, boolean isPreview, boolean isSelected) {
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2) return new Group();

        double height = Math.abs(current.getY()) > 0.1 ? Math.abs(current.getY()) : Math.max(6.0, r * 2.0);
        Node cone = mesh_helper_ui_main.createUpwardCone(r, height, createMaterial(isPreview, isSelected));
        cone.setTranslateX(center.getX());
        cone.setTranslateY(0);
        cone.setTranslateZ(center.getZ());
        return new Group(cone);
    }
}
