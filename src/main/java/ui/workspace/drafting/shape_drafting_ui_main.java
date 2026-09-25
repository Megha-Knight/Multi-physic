package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.shape_geometry_3d_ui_main;
import ui.workspace.shapes.shape_geometry_ui_main;

import java.util.function.Consumer;

/**
 * shape_drafting_ui_main.java
 * Drafting mode controller for creating 2D profiles on the horizontal XY ground plane (Z_cad = 0).
 */
public class shape_drafting_ui_main {

    private final Pane viewportPane;
    private final PerspectiveCamera camera;
    private final camera_controller_ui_main cameraController;
    private final Group shapesGroup  = new Group();
    private final Group previewGroup = new Group();
    private final Label hudLabel;
    private Consumer<String> statusCallback;
    private shape_editor_ui_main editor;

    private basic_shapes_ui_main activeShape = basic_shapes_ui_main.NONE;
    private Point3D startPt = null;

    public shape_drafting_ui_main(Pane viewportPane, PerspectiveCamera camera,
                                  camera_controller_ui_main cameraController, Label hudLabel) {
        this.viewportPane = viewportPane;
        this.camera = camera;
        this.cameraController = cameraController;
        this.hudLabel = hudLabel;
        attachListeners();
    }

    public void setEditor(shape_editor_ui_main editor) { this.editor = editor; }
    public shape_editor_ui_main getEditor()            { return editor; }
    public Group getShapesGroup()                      { return shapesGroup; }
    public Group getPreviewGroup()                     { return previewGroup; }
    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }

    public void setShape(basic_shapes_ui_main shape) {
        this.activeShape = (shape != null) ? shape : basic_shapes_ui_main.NONE;
        this.startPt = null;
        previewGroup.getChildren().clear();

        if (activeShape.isDrawing()) {
            if (editor != null) editor.selectShape(null);
            cameraController.setEnabled(false);
            if (statusCallback != null)
                statusCallback.accept("Drafting " + activeShape.getLabel() + ": "
                    + activeShape.getInstruction() + " [Esc to Cancel]");
            hudLabel.setText(activeShape.getLabel() + ": Click on ground grid to place start point");
            hudLabel.setVisible(true);
        } else {
            cameraController.setEnabled(true);
            hudLabel.setVisible(false);
            if (statusCallback != null) statusCallback.accept("Ready");
        }
    }

    public void cancelDrafting() { setShape(basic_shapes_ui_main.NONE); }
    public void cancel()         { cancelDrafting(); }

    private void attachListeners() {
        viewportPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!activeShape.isDrawing()) return;
            if (e.getButton() == MouseButton.SECONDARY) {
                cancelDrafting(); e.consume(); return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                Point3D hit = screenToGround(e.getX(), e.getY());
                if (hit != null) { startPt = hit; e.consume(); }
            }
        });

        viewportPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!activeShape.isDrawing() || startPt == null) return;
            Point3D hit = screenToGround(e.getX(), e.getY());
            if (hit != null) { updatePreview(startPt, hit); e.consume(); }
        });

        viewportPane.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (!activeShape.isDrawing() || startPt == null) return;
            Point3D hit = screenToGround(e.getX(), e.getY());
            if (hit != null && hit.distance(startPt) > 0.5) {
                shape_item_ui_main item = new shape_item_ui_main(activeShape, startPt, hit);
                if (editor != null) {
                    editor.addShape(item);
                } else {
                    shapesGroup.getChildren().add(item.getRootGroup());
                }
                if (statusCallback != null)
                    statusCallback.accept(activeShape.getLabel()
                        + " created. Drag body to move, drag handles to reshape.");
            }
            previewGroup.getChildren().clear();
            startPt = null;
            hudLabel.setVisible(false);
            cancelDrafting();
            e.consume();
        });

        viewportPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && activeShape.isDrawing()) {
                cancelDrafting(); e.consume();
            }
        });
    }

    private void updatePreview(Point3D p1, Point3D p2) {
        previewGroup.getChildren().clear();
        Node preview = createGeometry(activeShape, p1, p2, true);
        if (preview != null) {
            previewGroup.getChildren().add(preview);
            hudLabel.setText(formatDims(activeShape, p1, p2));
            hudLabel.setVisible(true);
        }
    }

    private Node createGeometry(basic_shapes_ui_main type, Point3D p1, Point3D p2, boolean isPreview) {
        return switch (type) {
            case CIRCLE    -> shape_geometry_ui_main.createCircle(p1, p2, isPreview);
            case SQUARE    -> shape_geometry_ui_main.createSquare(p1, p2, isPreview);
            case RECTANGLE -> shape_geometry_ui_main.createRectangle(p1, p2, isPreview);
            case EQUILATERAL_TRIANGLE -> shape_geometry_ui_main.createEquilateralTriangle(p1, p2, isPreview);
            case RIGHT_TRIANGLE -> shape_geometry_ui_main.createRightTriangle(p1, p2, isPreview);
            case CUBE      -> shape_geometry_3d_ui_main.createCube(p1, p2, isPreview);
            case CYLINDER  -> shape_geometry_3d_ui_main.createCylinder(p1, p2, isPreview);
            case SPHERE    -> shape_geometry_3d_ui_main.createSphere(p1, p2, isPreview);
            case CONE      -> shape_geometry_3d_ui_main.createCone(p1, p2, isPreview);
            default -> null;
        };
    }

    private String formatDims(basic_shapes_ui_main type, Point3D p1, Point3D p2) {
        double dx = Math.abs(p2.getX() - p1.getX());
        double dz = Math.abs(p2.getZ() - p1.getZ());
        double dist = p1.distance(p2);
        return switch (type) {
            case CIRCLE    -> String.format("Circle  |  Radius: %.1f mm  |  Dia: %.1f mm", dist, dist * 2);
            case SQUARE    -> String.format("Square  |  Side: %.1f mm", Math.max(dx, dz));
            case RECTANGLE -> String.format("Rectangle  |  W: %.1f mm  |  H: %.1f mm", dx, dz);
            case EQUILATERAL_TRIANGLE -> String.format("Equilateral Triangle  |  Side: %.1f mm", dist);
            case RIGHT_TRIANGLE -> String.format("Right Triangle  |  Base: %.1f mm  |  Height: %.1f mm", dx, dz);
            case CUBE      -> String.format("Cube (3D)  |  Side: %.1f mm", Math.max(dx, dz));
            case CYLINDER  -> String.format("Cylinder (3D)  |  Radius: %.1f mm  |  Height: %.1f mm", dist, dist * 2);
            case SPHERE    -> String.format("Sphere (3D)  |  Radius: %.1f mm  |  Dia: %.1f mm", dist, dist * 2);
            case CONE      -> String.format("Cone (3D)  |  Radius: %.1f mm  |  Height: %.1f mm", dist, dist * 2);
            default -> "";
        };
    }

    public Point3D screenToGround(double sx, double sy) {
        double w = viewportPane.getWidth(), h = viewportPane.getHeight();
        if (w <= 0 || h <= 0) return null;

        double fovRad = Math.toRadians(camera.getFieldOfView());
        double focalLen = (h / 2.0) / Math.tan(fovRad / 2.0);
        double dx = sx - (w / 2.0);
        double dy = sy - (h / 2.0);

        Point3D camOrigin = camera.localToScene(new Point3D(0, 0, 0));
        Point3D rayPtCam  = camera.localToScene(new Point3D(dx / focalLen, dy / focalLen, 1.0));
        Point3D rayDir    = rayPtCam.subtract(camOrigin).normalize();

        if (Math.abs(rayDir.getY()) < 1e-4) return null;
        double s = -camOrigin.getY() / rayDir.getY();
        if (s <= 0) return null;

        Point3D hit = camOrigin.add(rayDir.multiply(s));
        return new Point3D(hit.getX(), 0, hit.getZ());
    }

    public basic_shapes_ui_main getActiveShape() { return activeShape; }
}
