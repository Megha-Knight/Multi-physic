package ui.workspace.camera;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * coordinate_system_ui_main.java
 * Expansive engineering floor grid extending across all visible space,
 * with enhanced contrast for clear visibility in top-down and isometric views.
 */
public class coordinate_system_ui_main extends Group {

    public static final double AXIS_LENGTH = 55.0;
    public static final double AXIS_RADIUS = 0.35;

    private final Point3D originPoint = new Point3D(0, 0, 0);
    private final Point3D xTipPoint   = new Point3D(AXIS_LENGTH + 6, 0, 0);
    private final Point3D yTipPoint   = new Point3D(0, 0, AXIS_LENGTH + 6);
    private final Point3D zTipPoint   = new Point3D(0, -(AXIS_LENGTH + 6), 0);

    public coordinate_system_ui_main() {
        buildGroundGrid();
        buildAxes();
        buildOrigin();
    }

    public Point3D getOriginPoint() { return originPoint; }
    public Point3D getXTipPoint()   { return xTipPoint; }
    public Point3D getYTipPoint()   { return yTipPoint; }
    public Point3D getZTipPoint()   { return zTipPoint; }

    private void buildAxes() {
        PhongMaterial redMat   = new PhongMaterial(Color.web("#DC2626"));
        PhongMaterial greenMat = new PhongMaterial(Color.web("#16A34A"));
        PhongMaterial blueMat  = new PhongMaterial(Color.web("#2563EB"));

        // X AXIS (Red -> Right / +X)
        Cylinder xAxis = new Cylinder(AXIS_RADIUS, AXIS_LENGTH);
        xAxis.setMaterial(redMat);
        xAxis.getTransforms().addAll(
            new Translate(AXIS_LENGTH / 2.0, 0, 0),
            new Rotate(-90, Rotate.Z_AXIS)
        );

        // Y AXIS (Green -> Depth / +Z)
        Cylinder yAxis = new Cylinder(AXIS_RADIUS, AXIS_LENGTH);
        yAxis.setMaterial(greenMat);
        yAxis.getTransforms().addAll(
            new Translate(0, 0, AXIS_LENGTH / 2.0),
            new Rotate(90, Rotate.X_AXIS)
        );

        // Z AXIS (Blue -> Upward / -Y)
        Cylinder zAxis = new Cylinder(AXIS_RADIUS, AXIS_LENGTH);
        zAxis.setMaterial(blueMat);
        zAxis.getTransforms().add(new Translate(0, -AXIS_LENGTH / 2.0, 0));

        getChildren().addAll(xAxis, yAxis, zAxis);
    }

    private void buildOrigin() {
        Sphere originSphere = new Sphere(1.5);
        originSphere.setMaterial(new PhongMaterial(Color.web("#1E293B")));
        getChildren().add(originSphere);
    }

    /** Builds expansive floor grid with calibrated contrast for top-down visibility. */
    private void buildGroundGrid() {
        Group gridGroup = new Group();

        PhongMaterial normalMat    = new PhongMaterial(Color.web("#CBD5E1"));
        PhongMaterial redAxisMat   = new PhongMaterial(Color.web("#F87171"));
        PhongMaterial greenAxisMat = new PhongMaterial(Color.web("#4ADE80"));

        int gridLines = 60;
        double gridSpacing = 30.0;
        double totalSize = gridLines * gridSpacing;

        for (int i = -gridLines / 2; i <= gridLines / 2; i++) {
            double pos = i * gridSpacing;

            PhongMaterial zMat = (i == 0) ? greenAxisMat : normalMat;
            double zThick = (i == 0) ? 0.4 : 0.18;
            Box zLine = new Box(zThick, 0.05, totalSize);
            zLine.setTranslateX(pos);
            zLine.setTranslateY(0.2);
            zLine.setMaterial(zMat);

            PhongMaterial xMat = (i == 0) ? redAxisMat : normalMat;
            double xThick = (i == 0) ? 0.4 : 0.18;
            Box xLine = new Box(totalSize, 0.05, xThick);
            xLine.setTranslateZ(pos);
            xLine.setTranslateY(0.2);
            xLine.setMaterial(xMat);

            gridGroup.getChildren().addAll(zLine, xLine);
        }

        getChildren().add(gridGroup);
    }
}
