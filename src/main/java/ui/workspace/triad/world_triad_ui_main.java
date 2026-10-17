package ui.workspace.triad;

import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import java.util.function.BiConsumer;

/**
 * world_triad_ui_main.java
 * Persistent heads-up global reference triad anchored in the bottom-left viewport corner.
 * Rotates synchronously with camera orientation and supports 1-click orthogonal view snapping.
 */
public class world_triad_ui_main extends StackPane {

    private final Rotate triadRx = new Rotate(0, Rotate.X_AXIS);
    private final Rotate triadRy = new Rotate(0, Rotate.Y_AXIS);
    private final Label frameBadge = new Label("WORLD");
    private BiConsumer<Double, Double> onSnapView = null;

    private static final double LEN = 22.0;
    private static final double RAD = 0.85;

    public world_triad_ui_main(Rotate mainRx, Rotate mainRy) {
        double sz = 78.0;
        setPrefSize(sz, sz + 18);
        setMinSize(sz, sz + 18);
        setMaxSize(sz, sz + 18);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: transparent;");

        triadRx.angleProperty().bind(mainRx.angleProperty().negate());
        triadRy.angleProperty().bind(mainRy.angleProperty().negate());

        Group root3D = new Group();
        Group pivot = new Group(buildAxes(), buildOrigin());
        pivot.getTransforms().setAll(triadRx, triadRy);
        root3D.getChildren().add(pivot);

        AmbientLight ambient = new AmbientLight(Color.web("#E2E8F0"));
        PointLight light = new PointLight(Color.web("#FFFFFF"));
        light.setTranslateX(100); light.setTranslateY(-100); light.setTranslateZ(-200);
        root3D.getChildren().addAll(ambient, light);

        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setNearClip(1.0); cam.setFarClip(1000.0);
        cam.getTransforms().add(new Translate(0, 0, -85));

        SubScene subScene = new SubScene(root3D, sz, sz, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(cam);

        frameBadge.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 8.5px; -fx-font-weight: bold; " +
                            "-fx-background-color: rgba(15, 23, 42, 0.75); -fx-padding: 1 4 1 4; -fx-background-radius: 3;");

        VBox layout = new VBox(2, subScene, frameBadge);
        layout.setAlignment(Pos.CENTER);
        getChildren().add(layout);

        Tooltip.install(this, new Tooltip("World Reference Frame\nX (Red): Right | Y (Green): Depth | Z (Blue): Up\nClick axis to align view"));

        subScene.setOnMouseClicked(e -> {
            if (!e.isConsumed()) snap(ui.framework_ui_main.ISO_PITCH, ui.framework_ui_main.ISO_YAW);
        });
    }

    public void setOnSnapView(BiConsumer<Double, Double> snapCb) {
        this.onSnapView = snapCb;
    }

    private void snap(double pitch, double yaw) {
        if (onSnapView != null) onSnapView.accept(pitch, yaw);
    }

    public void setLocalActive(boolean active, String planeName) {
        if (active) {
            frameBadge.setText("WORLD | " + (planeName != null ? planeName.toUpperCase() : "LOCAL"));
            frameBadge.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 8px; -fx-font-weight: bold; " +
                                "-fx-background-color: rgba(15, 23, 42, 0.85); -fx-padding: 1 4 1 4; -fx-background-radius: 3;");
        } else {
            frameBadge.setText("WORLD");
            frameBadge.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 8.5px; -fx-font-weight: bold; " +
                                "-fx-background-color: rgba(15, 23, 42, 0.75); -fx-padding: 1 4 1 4; -fx-background-radius: 3;");
        }
    }

    private Group buildAxes() {
        Group g = new Group();
        double offset = LEN / 2.0;

        // X Axis (Red -> +X Right)
        Cylinder xLine = new Cylinder(RAD, LEN);
        xLine.setMaterial(new PhongMaterial(Color.web("#DC2626")));
        xLine.getTransforms().addAll(new Translate(offset, 0, 0), new Rotate(-90, Rotate.Z_AXIS));
        Sphere xTip = new Sphere(RAD * 1.8);
        xTip.setMaterial(new PhongMaterial(Color.web("#EF4444")));
        xTip.setTranslateX(LEN);
        xLine.setOnMouseClicked(e -> { snap(0.0, -90.0); e.consume(); });
        xTip.setOnMouseClicked(e -> { snap(0.0, -90.0); e.consume(); });

        // Y Axis (Green -> +Z Depth)
        Cylinder yLine = new Cylinder(RAD, LEN);
        yLine.setMaterial(new PhongMaterial(Color.web("#16A34A")));
        yLine.getTransforms().addAll(new Translate(0, 0, offset), new Rotate(90, Rotate.X_AXIS));
        Sphere yTip = new Sphere(RAD * 1.8);
        yTip.setMaterial(new PhongMaterial(Color.web("#22C55E")));
        yTip.setTranslateZ(LEN);
        yLine.setOnMouseClicked(e -> { snap(0.0, 180.0); e.consume(); });
        yTip.setOnMouseClicked(e -> { snap(0.0, 180.0); e.consume(); });

        // Z Axis (Blue -> -Y Up)
        Cylinder zLine = new Cylinder(RAD, LEN);
        zLine.setMaterial(new PhongMaterial(Color.web("#2563EB")));
        zLine.getTransforms().add(new Translate(0, -offset, 0));
        Sphere zTip = new Sphere(RAD * 1.8);
        zTip.setMaterial(new PhongMaterial(Color.web("#3B82F6")));
        zTip.setTranslateY(-LEN);
        zLine.setOnMouseClicked(e -> { snap(-89.9, 0.0); e.consume(); });
        zTip.setOnMouseClicked(e -> { snap(-89.9, 0.0); e.consume(); });

        g.getChildren().addAll(xLine, xTip, yLine, yTip, zLine, zTip);
        return g;
    }

    private Sphere buildOrigin() {
        Sphere o = new Sphere(RAD * 1.5);
        o.setMaterial(new PhongMaterial(Color.web("#1E293B")));
        o.setOnMouseClicked(e -> { snap(ui.framework_ui_main.ISO_PITCH, ui.framework_ui_main.ISO_YAW); e.consume(); });
        return o;
    }
}
