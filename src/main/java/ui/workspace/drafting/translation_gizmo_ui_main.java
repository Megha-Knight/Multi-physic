package ui.workspace.drafting;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.framework_ui_main;

/**
 * translation_gizmo_ui_main.java
 * CAD-style XYZ axis translation gizmo for selected 3D objects.
 * Three coloured arms (X=red, Y=green, Z=blue) with pick-able shafts and tip spheres.
 */
public class translation_gizmo_ui_main extends Group {

    public enum Axis { X, Y, Z }

    private final Sphere tipX, tipY, tipZ;
    private final Cylinder shaftX, shaftY, shaftZ;

    private final PhongMaterial matX, matY, matZ;
    private final PhongMaterial matXHov, matYHov, matZHov;

    public translation_gizmo_ui_main() {
        matX    = mat(framework_ui_main.GIZMO_X_COLOR);
        matY    = mat(framework_ui_main.GIZMO_Y_COLOR);
        matZ    = mat(framework_ui_main.GIZMO_Z_COLOR);
        matXHov = mat(framework_ui_main.GIZMO_X_HOVER);
        matYHov = mat(framework_ui_main.GIZMO_Y_HOVER);
        matZHov = mat(framework_ui_main.GIZMO_Z_HOVER);

        double L = framework_ui_main.GIZMO_LENGTH;
        double r = framework_ui_main.GIZMO_SHAFT_RADIUS;
        double tr = framework_ui_main.GIZMO_TIP_RADIUS;

        // X arm (+X red, CAD X)
        shaftX = shaft(r, L, matX);
        shaftX.getTransforms().addAll(new Translate(L * 0.5, 0, 0), new Rotate(-90, Rotate.Z_AXIS));
        shaftX.setUserData(Axis.X);
        tipX = tip(tr, matX);
        tipX.setTranslateX(L);
        tipX.setUserData(Axis.X);

        // Y arm (+Z green, CAD Y depth)
        shaftY = shaft(r, L, matY);
        shaftY.getTransforms().addAll(new Translate(0, 0, L * 0.5), new Rotate(90, Rotate.X_AXIS));
        shaftY.setUserData(Axis.Y);
        tipY = tip(tr, matY);
        tipY.setTranslateZ(L);
        tipY.setUserData(Axis.Y);

        // Z arm (-Y blue, CAD Z vertical height)
        shaftZ = shaft(r, L, matZ);
        shaftZ.getTransforms().add(new Translate(0, -L * 0.5, 0));
        shaftZ.setUserData(Axis.Z);
        tipZ = tip(tr, matZ);
        tipZ.setTranslateY(-L);
        tipZ.setUserData(Axis.Z);

        Sphere orig = new Sphere(r * 2.2);
        orig.setMaterial(mat("#334155"));

        getChildren().addAll(shaftX, tipX, shaftY, tipY, shaftZ, tipZ, orig);
        setVisible(false);
        setMouseTransparent(false);
    }

    public void highlight(Axis axis) {
        tipX.setMaterial(axis == Axis.X ? matXHov : matX);
        shaftX.setMaterial(axis == Axis.X ? matXHov : matX);
        tipY.setMaterial(axis == Axis.Y ? matYHov : matY);
        shaftY.setMaterial(axis == Axis.Y ? matYHov : matY);
        tipZ.setMaterial(axis == Axis.Z ? matZHov : matZ);
        shaftZ.setMaterial(axis == Axis.Z ? matZHov : matZ);
    }

    public void clearHighlight() {
        tipX.setMaterial(matX);
        shaftX.setMaterial(matX);
        tipY.setMaterial(matY);
        shaftY.setMaterial(matY);
        tipZ.setMaterial(matZ);
        shaftZ.setMaterial(matZ);
    }

    public void moveTo(double wx, double wy, double wz,
                       double localCx, double localCy, double localCz) {
        setTranslateX(localCx + wx);
        setTranslateY(localCy + wy);
        setTranslateZ(localCz + wz);
    }

    public Axis axisForNode(javafx.scene.Node node) {
        if (node == tipX || node == shaftX) return Axis.X;
        if (node == tipY || node == shaftY) return Axis.Y;
        if (node == tipZ || node == shaftZ) return Axis.Z;
        javafx.scene.Node cur = node;
        while (cur != null) {
            if (cur.getUserData() instanceof Axis a) return a;
            cur = cur.getParent();
        }
        return null;
    }

    public boolean isGizmoNode(javafx.scene.Node node) {
        javafx.scene.Node cur = node;
        while (cur != null) {
            if (cur == this) return true;
            cur = cur.getParent();
        }
        return false;
    }

    private static PhongMaterial mat(String hex) {
        PhongMaterial m = new PhongMaterial(Color.web(hex));
        m.setSpecularColor(Color.WHITE);
        m.setSpecularPower(20);
        return m;
    }

    private static Cylinder shaft(double r, double length, PhongMaterial mat) {
        Cylinder c = new Cylinder(r, length);
        c.setMaterial(mat);
        return c;
    }

    private static Sphere tip(double r, PhongMaterial mat) {
        Sphere s = new Sphere(r);
        s.setMaterial(mat);
        return s;
    }
}
