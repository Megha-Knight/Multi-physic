package ui.workspace;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.framework_ui_main;
import ui.viewcube.viewcube_ui_main;

/**
 * workspace_ui_main.java
 * Central 3D Engineering Viewport with integrated View Cube in top-right corner.
 * Supports exact Isometric, Axonometric, Perspective, and Orthographic projection modes.
 */
public class workspace_ui_main extends StackPane {

    private final SubScene subScene;
    private final Group root3D;
    private final coordinatesystem_ui_main coordSystem;
    private final PerspectiveCamera camera;

    // Camera transforms with standard mathematical isometric initialization
    private final Rotate rx = new Rotate(framework_ui_main.ISO_PITCH, Rotate.X_AXIS);
    private final Rotate ry = new Rotate(framework_ui_main.ISO_YAW, Rotate.Y_AXIS);
    private final Translate t = new Translate(0, -30, -400);

    private final Pane labelOverlay;
    private final Label originLabel, xLabel, yLabel, zLabel, hudDimLabel;
    private final viewcube_ui_main viewCube;
    private final cameracontroller_ui_main controller;
    private final shapedrafting_ui_main drafter;
    private final shapeeditor_ui_main shapeEditor;

    private boolean isOrthographic = false;
    private double savedDist = -400.0;

    public workspace_ui_main() {
        getStyleClass().add("workspace-viewport");
        setStyle("-fx-background-color: " + framework_ui_main.WORKSPACE_BG + ";");

        root3D = new Group();
        coordSystem = new coordinatesystem_ui_main();
        root3D.getChildren().add(coordSystem);

        AmbientLight ambientLight = new AmbientLight(Color.web("#E2E8F0"));
        PointLight pointLight = new PointLight(Color.web("#FFFFFF"));
        pointLight.setTranslateX(150);
        pointLight.setTranslateY(-300);
        pointLight.setTranslateZ(-250);
        root3D.getChildren().addAll(ambientLight, pointLight);

        camera = new PerspectiveCamera(true);
        camera.setNearClip(1.0);
        camera.setFarClip(10000.0);
        camera.setFieldOfView(45.0);

        Group cameraXGroup = new Group();
        Group cameraYGroup = new Group();
        cameraYGroup.getTransforms().add(ry);
        cameraXGroup.getTransforms().add(rx);
        camera.getTransforms().add(t);

        cameraXGroup.getChildren().add(camera);
        cameraYGroup.getChildren().add(cameraXGroup);
        root3D.getChildren().add(cameraYGroup);

        subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#F8FAFC"));
        subScene.setCamera(camera);

        subScene.widthProperty().bind(this.widthProperty());
        subScene.heightProperty().bind(this.heightProperty());

        labelOverlay = new Pane();
        labelOverlay.setMouseTransparent(true);
        originLabel = createLabel("(0,0,0)", "#334155", 10, true);
        xLabel      = createLabel("X", "#DC2626", 11, true);
        yLabel      = createLabel("Y", "#16A34A", 11, true);
        zLabel      = createLabel("Z", "#2563EB", 11, true);
        hudDimLabel = createLabel("", "#005A85", 11, true);
        hudDimLabel.setLayoutX(14);
        hudDimLabel.setLayoutY(14);
        hudDimLabel.setVisible(false);
        labelOverlay.getChildren().addAll(originLabel, xLabel, yLabel, zLabel, hudDimLabel);

        viewCube = new viewcube_ui_main(rx, ry);
        StackPane.setAlignment(viewCube, Pos.TOP_RIGHT);
        StackPane.setMargin(viewCube, new Insets(framework_ui_main.VIEW_CUBE_MARGIN));

        getChildren().addAll(subScene, labelOverlay, viewCube);

        controller = new cameracontroller_ui_main(rx, ry, t, this::updateLabels);
        controller.attach(this);
        viewCube.setOnSnapView(controller::setOrientation);

        drafter = new shapedrafting_ui_main(this, camera, controller, hudDimLabel);
        shapeEditor = new shapeeditor_ui_main(drafter.getShapesGroup(), this, subScene, controller,
            drafter, hudDimLabel, () -> drafter.getActiveShape().isDrawing());
        drafter.setEditor(shapeEditor);
        root3D.getChildren().addAll(drafter.getShapesGroup(), drafter.getPreviewGroup());

        widthProperty().addListener((obs, o, n) -> updateLabels());
        heightProperty().addListener((obs, o, n) -> updateLabels());
        Platform.runLater(this::updateLabels);
    }

    public void toggleProjection() {
        setOrthographic(!isOrthographic);
    }

    public void setOrthographic(boolean ortho) {
        this.isOrthographic = ortho;
        if (ortho) {
            savedDist = t.getZ();
            camera.setFieldOfView(2.5);
            camera.setFarClip(50000.0);
            double ratio = Math.tan(Math.toRadians(22.5)) / Math.tan(Math.toRadians(1.25));
            t.setZ(savedDist * ratio);
        } else {
            camera.setFieldOfView(45.0);
            camera.setFarClip(10000.0);
            t.setZ(savedDist);
        }
        updateLabels();
    }

    public boolean isOrthographic() { return isOrthographic; }

    private Label createLabel(String text, String colorHex, double fontSize, boolean bold) {
        Label lbl = new Label(text);
        String weight = bold ? "bold" : "normal";
        lbl.setStyle(String.format(
            "-fx-text-fill: %s; -fx-font-size: %.0fpx; -fx-font-weight: %s; " +
            "-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 1 3 1 3; " +
            "-fx-background-radius: 2; -fx-border-color: rgba(200,200,200,0.5); -fx-border-radius: 2;",
            colorHex, fontSize, weight
        ));
        return lbl;
    }

    private void updateLabels() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        projectPointToLabel(coordSystem.getOriginPoint(), originLabel, w, h, 6, 6);
        projectPointToLabel(coordSystem.getXTipPoint(), xLabel, w, h, 8, -6);
        projectPointToLabel(coordSystem.getYTipPoint(), yLabel, w, h, 8, 6);
        projectPointToLabel(coordSystem.getZTipPoint(), zLabel, w, h, -10, -10);
    }

    private void projectPointToLabel(Point3D worldPt, Label label, double w, double h, double offX, double offY) {
        Point3D scene3D = coordSystem.localToScene(worldPt);
        Point3D camPt   = camera.sceneToLocal(scene3D);
        if (camPt.getZ() > 0) {
            double fovRad = Math.toRadians(camera.getFieldOfView());
            double focalLen = (h / 2.0) / Math.tan(fovRad / 2.0);
            double projX = (w / 2.0) + (camPt.getX() * focalLen / camPt.getZ());
            double projY = (h / 2.0) + (camPt.getY() * focalLen / camPt.getZ());
            label.setLayoutX(projX + offX);
            label.setLayoutY(projY + offY);
            label.setVisible(true);
        } else {
            label.setVisible(false);
        }
    }

    public cameracontroller_ui_main getCameraController() { return controller; }
    public PerspectiveCamera getCamera() { return camera; }
    public viewcube_ui_main getViewCube() { return viewCube; }
    public shapedrafting_ui_main getDrafter() { return drafter; }
    public shapeeditor_ui_main getShapeEditor() { return shapeEditor; }
}
