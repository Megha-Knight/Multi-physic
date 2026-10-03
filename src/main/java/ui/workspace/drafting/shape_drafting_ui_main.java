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
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.gizmo.viewport_coordinate_helper_ui_main;

import java.util.function.Consumer;

public class shape_drafting_ui_main {

    private final Pane viewportPane;
    private final PerspectiveCamera camera;
    private final camera_controller_ui_main cameraController;
    private final Group shapesGroup = new Group(), previewGroup = new Group();
    private final Label hudLabel;
    private Consumer<String> statusCallback;
    private shape_editor_ui_main editor;
    private basic_shapes_ui_main activeShape = basic_shapes_ui_main.NONE;
    private Point3D startPt = null;
    private face_reference_ui_main draftingFace = null;

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
    public basic_shapes_ui_main getActiveShape()       { return activeShape; }
    public face_reference_ui_main getDraftingFace()    { return draftingFace; }

    public void setShape(basic_shapes_ui_main shape) {
        this.activeShape = (shape != null) ? shape : basic_shapes_ui_main.NONE;
        this.startPt = null; previewGroup.getChildren().clear();

        if (activeShape.isDrawing()) {
            if (activeShape.is3D()) {
                draftingFace = null;
                if (editor != null) editor.selectShape(null);
            } else if (editor != null && editor.getActiveFace() != null && editor.getActiveFace().isPlanar()) {
                draftingFace = editor.getActiveFace();
            } else {
                draftingFace = null;
                if (editor != null) editor.selectShape(null);
            }
            cameraController.setEnabled(false);
            String tgt = (draftingFace != null) ? " on " + draftingFace.getFaceKind().getLabel() : " on ground grid";
            if (statusCallback != null)
                statusCallback.accept("Drafting " + activeShape.getLabel() + tgt + ": " + activeShape.getInstruction() + " [Esc to Cancel]");
            hudLabel.setText(activeShape.getLabel() + ": Click" + tgt + " to place start point");
            hudLabel.setVisible(true);
        } else {
            draftingFace = null;
            cameraController.setEnabled(true);
            hudLabel.setVisible(false);
            if (statusCallback != null) statusCallback.accept("Ready");
        }
    }

    public void cancelDrafting() { setShape(basic_shapes_ui_main.NONE); }
    public void cancel()         { cancelDrafting(); }

    private Point3D hitDrafting(MouseEvent e) {
        return screenToDraftingPlane(viewport_coordinate_helper_ui_main.getViewportX(e, viewportPane), viewport_coordinate_helper_ui_main.getViewportY(e, viewportPane));
    }

    private void attachListeners() {
        viewportPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!activeShape.isDrawing()) return;
            if (e.getButton() == MouseButton.SECONDARY) { cancelDrafting(); e.consume(); return; }
            if (e.getButton() == MouseButton.PRIMARY) {
                Point3D hit = hitDrafting(e);
                if (hit != null) { startPt = hit; e.consume(); }
            }
        });

        viewportPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!activeShape.isDrawing() || startPt == null) return;
            Point3D hit = hitDrafting(e);
            if (hit != null) { updatePreview(startPt, hit); e.consume(); }
        });

        viewportPane.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (!activeShape.isDrawing() || startPt == null) return;
            Point3D hit = hitDrafting(e);
            if (hit != null && hit.distance(startPt) > 0.5) {
                shape_item_ui_main item = new shape_item_ui_main(activeShape, startPt, hit);
                if (draftingFace != null) {
                    item.setFacePlane(draftingFace.getUAxis(), draftingFace.getVAxis(), draftingFace.getWorldNormal(),
                                      draftingFace.getOwnerShapeId(), draftingFace.getFaceKind());
                }
                if (editor != null) editor.addShape(item);
                else shapesGroup.getChildren().add(item.getRootGroup());
                if (statusCallback != null)
                    statusCallback.accept(activeShape.getLabel() + " created on " + (draftingFace != null ? draftingFace.getFaceKind().getLabel() : "ground") + ".");
            }
            previewGroup.getChildren().clear();
            startPt = null;
            hudLabel.setVisible(false);
            cancelDrafting();
            e.consume();
        });

        viewportPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && activeShape.isDrawing()) { cancelDrafting(); e.consume(); }
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
        if (type.is3D()) {
            return switch (type) {
                case CUBE      -> shape_geometry_3d_ui_main.createCube(p1, p2, isPreview);
                case CUBOID    -> shape_geometry_3d_ui_main.createCuboid(p1, p2, isPreview);
                case CYLINDER  -> shape_geometry_3d_ui_main.createCylinder(p1, p2, isPreview);
                case SPHERE    -> shape_geometry_3d_ui_main.createSphere(p1, p2, isPreview);
                case CONE      -> shape_geometry_3d_ui_main.createCone(p1, p2, isPreview);
                default -> null;
            };
        }
        Point3D u = (draftingFace != null) ? draftingFace.getUAxis() : new Point3D(1, 0, 0);
        Point3D v = (draftingFace != null) ? draftingFace.getVAxis() : new Point3D(0, 0, 1);
        Point3D n = (draftingFace != null) ? draftingFace.getWorldNormal() : new Point3D(0, -1, 0);
        return shape_geometry_ui_main.createShapeOnPlane(type, p1, p2, u, v, n, isPreview, false);
    }

    private String formatDims(basic_shapes_ui_main type, Point3D p1, Point3D p2) {
        Point3D u = (draftingFace != null) ? draftingFace.getUAxis() : new Point3D(1, 0, 0);
        Point3D v = (draftingFace != null) ? draftingFace.getVAxis() : new Point3D(0, 0, 1);
        Point3D delta = p2.subtract(p1);
        double du = Math.abs(delta.dotProduct(u)), dv = Math.abs(delta.dotProduct(v)), dist = p1.distance(p2);
        return switch (type) {
            case CIRCLE    -> String.format("Circle | Radius: %.1f mm | Dia: %.1f mm", dist, dist * 2);
            case SQUARE    -> String.format("Square | Side: %.1f mm", Math.max(du, dv));
            case RECTANGLE -> String.format("Rectangle | W: %.1f mm | H: %.1f mm", du, dv);
            case EQUILATERAL_TRIANGLE -> String.format("Equilateral Triangle | Side: %.1f mm", dist);
            case RIGHT_TRIANGLE -> String.format("Right Triangle | Base: %.1f mm | Height: %.1f mm", du, dv);
            case CUBE      -> String.format("Cube (3D) | Side: %.1f mm", Math.max(du, dv));
            case CUBOID    -> String.format("Cuboid (3D) | W: %.1f mm | D: %.1f mm", du, dv);
            case CYLINDER  -> String.format("Cylinder (3D) | Radius: %.1f mm | Height: %.1f mm", dist, dist * 2);
            case SPHERE    -> String.format("Sphere (3D) | Radius: %.1f mm | Dia: %.1f mm", dist, dist * 2);
            case CONE      -> String.format("Cone (3D) | Radius: %.1f mm | Height: %.1f mm", dist, dist * 2);
            default -> "";
        };
    }

    public Point3D screenToDraftingPlane(double sx, double sy) {
        if (draftingFace != null) return screenToPlane(draftingFace.getFaceOrigin(), draftingFace.getWorldNormal(), sx, sy);
        return screenToGround(sx, sy);
    }

    public Point3D screenToPlane(Point3D pOrigin, Point3D pNorm, double sx, double sy) {
        double w = viewportPane.getWidth(), h = viewportPane.getHeight();
        if (w <= 0 || h <= 0) return null;
        double fovRad = Math.toRadians(camera.getFieldOfView()), focalLen = (h / 2.0) / Math.tan(fovRad / 2.0);
        Point3D camOrigin = shapesGroup.sceneToLocal(camera.localToScene(new Point3D(0, 0, 0)));
        Point3D rayPtCam = shapesGroup.sceneToLocal(camera.localToScene(new Point3D((sx - w / 2.0) / focalLen, (sy - h / 2.0) / focalLen, 1.0)));
        Point3D rayDir = rayPtCam.subtract(camOrigin).normalize();
        double denom = rayDir.dotProduct(pNorm);
        if (Math.abs(denom) < 1e-5) return null;
        double s = pOrigin.subtract(camOrigin).dotProduct(pNorm) / denom;
        return (s <= 0) ? null : camOrigin.add(rayDir.multiply(s));
    }

    public Point3D screenToGround(double sx, double sy) {
        return screenToPlane(new Point3D(0, 0, 0), new Point3D(0, -1, 0), sx, sy);
    }
}
