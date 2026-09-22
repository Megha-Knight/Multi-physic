package ui.viewcube;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

/**
 * viewcube_ui_main.java
 * Interactive 3D CAD View Cube with clickable faces:
 * TOP, BOTTOM, FRONT, BACK, LEFT, RIGHT, and Isometric reset.
 */
public class viewcube_ui_main extends StackPane {

    private final Rotate cubeRx;
    private final Rotate cubeRy;
    private BiConsumer<Double, Double> onSnapView;
    private static final double H = 16.0; // Scaled cube half-size (~15% reduction)

    public viewcube_ui_main(Rotate mainRx, Rotate mainRy) {
        double sz = ui.framework_ui_main.VIEW_CUBE_SIZE; // 98px
        setPrefSize(sz, sz);
        setMinSize(sz, sz);
        setMaxSize(sz, sz);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: transparent;");

        cubeRx = new Rotate(mainRx.getAngle(), Rotate.X_AXIS);
        cubeRy = new Rotate(mainRy.getAngle(), Rotate.Y_AXIS);
        cubeRx.angleProperty().bind(mainRx.angleProperty());
        cubeRy.angleProperty().bind(mainRy.angleProperty());

        Group root3D = new Group();
        Group cubePivot = new Group(buildFlushFaces(), buildMiniAxes());
        cubePivot.getTransforms().addAll(cubeRy, cubeRx);
        root3D.getChildren().add(cubePivot);

        AmbientLight ambient = new AmbientLight(Color.web("#E2E8F0"));
        PointLight light = new PointLight(Color.web("#FFFFFF"));
        light.setTranslateX(100);
        light.setTranslateY(-100);
        light.setTranslateZ(-200);
        root3D.getChildren().addAll(ambient, light);

        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setNearClip(1.0);
        cam.setFarClip(1000.0);
        cam.getTransforms().add(new Translate(0, 0, -102));

        SubScene subScene = new SubScene(root3D, sz, sz, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(cam);
        getChildren().add(subScene);

        // Click outside faces or on background snaps to true Isometric View
        subScene.setOnMouseClicked(e -> {
            if (!e.isConsumed()) snap(ui.framework_ui_main.ISO_PITCH, ui.framework_ui_main.ISO_YAW);
        });
    }

    public void setOnSnapView(BiConsumer<Double, Double> onSnapView) {
        this.onSnapView = onSnapView;
    }

    private void snap(double pitch, double yaw) {
        if (onSnapView != null) onSnapView.accept(pitch, yaw);
    }

    private Group buildFlushFaces() {
        Group g = new Group();
        // FRONT (Z = -H, facing camera along +Z) -> Front View (0, 0)
        g.getChildren().add(createFace(new Point3D(-H,-H,-H), new Point3D(H,-H,-H),
                                       new Point3D(-H, H,-H), new Point3D(H, H,-H), "FRONT", 0.0, 0.0));
        // BACK (Z = +H, facing away from camera) -> Back View (0, 180)
        g.getChildren().add(createFace(new Point3D(H,-H, H), new Point3D(-H,-H, H),
                                       new Point3D(H, H, H), new Point3D(-H, H, H), "BACK", 0.0, 180.0));
        // TOP (Y = -H, sky / +Z in world) -> Top View (-89.9, 0)
        g.getChildren().add(createFace(new Point3D(-H,-H, H), new Point3D(H,-H, H),
                                       new Point3D(-H,-H,-H), new Point3D(H,-H,-H), "TOP", -89.9, 0.0));
        // BOTTOM (Y = +H) -> Bottom View (89.9, 0)
        g.getChildren().add(createFace(new Point3D(-H, H,-H), new Point3D(H, H,-H),
                                       new Point3D(-H, H, H), new Point3D(H, H, H), "BOTTOM", 89.9, 0.0));
        // RIGHT (X = +H) -> Right View (0, -90)
        g.getChildren().add(createFace(new Point3D(H,-H,-H), new Point3D(H,-H, H),
                                       new Point3D(H, H,-H), new Point3D(H, H, H), "RIGHT", 0.0, -90.0));
        // LEFT (X = -H) -> Left View (0, 90)
        g.getChildren().add(createFace(new Point3D(-H,-H, H), new Point3D(-H,-H,-H),
                                       new Point3D(-H, H, H), new Point3D(-H, H,-H), "LEFT", 0.0, 90.0));
        return g;
    }

    private MeshView createFace(Point3D p00, Point3D p10, Point3D p01, Point3D p11, String text, double pitch, double yaw) {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
            (float) p00.getX(), (float) p00.getY(), (float) p00.getZ(),
            (float) p10.getX(), (float) p10.getY(), (float) p10.getZ(),
            (float) p01.getX(), (float) p01.getY(), (float) p01.getZ(),
            (float) p11.getX(), (float) p11.getY(), (float) p11.getZ()
        );
        mesh.getTexCoords().addAll(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f);
        mesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 1, 1, 3, 3, 2, 2);
        MeshView mv = new MeshView(mesh);
        mv.setMaterial(createFaceMaterial(text));
        mv.setCullFace(CullFace.NONE);
        mv.setOnMouseClicked(e -> {
            snap(pitch, yaw);
            e.consume();
        });
        return mv;
    }

    private PhongMaterial createFaceMaterial(String text) {
        BufferedImage bi = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(255, 255, 255));
        g.fillRect(0, 0, 128, 128);
        g.setColor(new java.awt.Color(203, 213, 225));
        g.setStroke(new BasicStroke(6));
        g.drawRect(3, 3, 122, 122);
        g.setColor(new java.awt.Color(30, 41, 59));
        g.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        int x = (128 - fm.stringWidth(text)) / 2;
        int y = ((128 - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(text, x, y);
        g.dispose();

        Image fxImage = SwingFXUtils.toFXImage(bi, null);
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(fxImage);
        return mat;
    }

    private Group buildMiniAxes() {
        Group g = new Group();
        double len = 18.0, rad = 0.70, offset = H + len / 2.0;

        // X: Red (Right -> +X)
        Cylinder xLine = new Cylinder(rad, len);
        xLine.setMaterial(new PhongMaterial(Color.web("#DC2626")));
        xLine.getTransforms().addAll(new Translate(offset, 0, 0), new Rotate(-90, Rotate.Z_AXIS));

        // Y: Green (Depth / Floor -> +Z)
        Cylinder yLine = new Cylinder(rad, len);
        yLine.setMaterial(new PhongMaterial(Color.web("#16A34A")));
        yLine.getTransforms().addAll(new Translate(0, 0, offset), new Rotate(90, Rotate.X_AXIS));

        // Z: Blue (Upward / Sky -> -Y)
        Cylinder zLine = new Cylinder(rad, len);
        zLine.setMaterial(new PhongMaterial(Color.web("#2563EB")));
        zLine.getTransforms().add(new Translate(0, -offset, 0));

        g.getChildren().addAll(xLine, yLine, zLine);
        return g;
    }
}
