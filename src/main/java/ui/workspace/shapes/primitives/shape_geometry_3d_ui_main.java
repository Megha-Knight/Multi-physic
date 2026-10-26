package ui.workspace.shapes.primitives;

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

    /** Typed return from {@link #getCubeDimensions} — eliminates silent index-transposition risk. */
    public record CubeDimensions(double cx, double cy, double cz, double s) {}

    public static PhongMaterial createMaterial(boolean isPreview, boolean isSelected) {
        if (isPreview) {
            PhongMaterial mat = new PhongMaterial(Color.web(framework_ui_main.DRAFT_PREVIEW_COLOR));
            mat.setSpecularColor(Color.web("#BAE6FD"));
            mat.setSpecularPower(48.0);
            return mat;
        }
        Color c = isSelected
                ? Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)
                : Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR);
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.web(isSelected ? "#BAE6FD" : "#64748B"));
        mat.setSpecularPower(32.0);
        return mat;
    }

    public static Node createCube(Point3D p1, Point3D p2, boolean isPreview) {
        return createCube(p1, p2, isPreview, false);
    }

    public static Node createCube(Point3D p1, Point3D p2, boolean isPreview, boolean isSelected) {
        CubeDimensions dim = getCubeDimensions(p1, p2);
        if (dim.s() < 0.2) return new Group();

        Box box = new Box(dim.s(), dim.s(), dim.s());
        box.setMaterial(createMaterial(isPreview, isSelected));
        box.setTranslateX(dim.cx());
        box.setTranslateY(dim.cy());
        box.setTranslateZ(dim.cz());
        return new Group(box);
    }

    /**
     * Returns the cube's centre and uniform side-length for the given p1/p2,
     * using exactly the same formula as {@link #createCube}.
     *
     * @return {@link CubeDimensions} with cx/cy/cz = box centre, s = uniform side length,
     *         where cy = -s*0.5 (box sits on Y=0, extends downward)
     */
    public static CubeDimensions getCubeDimensions(Point3D p1, Point3D p2) {
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
        double s  = Math.max(Math.abs(dx), Math.abs(dz));
        double cx = p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5);
        double cz = p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5);
        double cy = -s * 0.5;
        return new CubeDimensions(cx, cy, cz, s);
    }

    public static Node createCuboid(Point3D p1, Point3D p2, boolean isPreview) {
        return createCuboid(p1, p2, isPreview, false);
    }

    public static Node createCuboid(Point3D p1, Point3D p2, boolean isPreview, boolean isSelected) {
        double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
        if (w < 0.2 || d < 0.2) return new Group();
        double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
        Box box = new Box(w, h, d);
        box.setMaterial(createMaterial(isPreview, isSelected));
        box.setTranslateX((p1.getX() + p2.getX()) * 0.5);
        box.setTranslateY(p1.getY() - h * 0.5);
        box.setTranslateZ((p1.getZ() + p2.getZ()) * 0.5);
        return new Group(box);
    }

    public static Node createCylinder(Point3D center, Point3D current, boolean isPreview) {
        return createCylinder(center, current, isPreview, false);
    }

    public static Node createCylinder(Point3D center, Point3D current, boolean isPreview, boolean isSelected) {
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2) return new Group();

        double height = Math.abs(current.getY()) > 0.1 ? Math.abs(current.getY()) : Math.max(6.0, r * 2.0);
        Cylinder cyl = new Cylinder(r, height, 64);
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

        Sphere sph = new Sphere(r, 64);
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
