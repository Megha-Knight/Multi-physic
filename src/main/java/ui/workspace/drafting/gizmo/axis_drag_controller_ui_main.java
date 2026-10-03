package ui.workspace.drafting.gizmo;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.gizmo.translation_gizmo_ui_main.Axis;

import java.util.function.Consumer;

/**
 * axis_drag_controller_ui_main.java
 * Manages translation gizmo lifecycle and axis-constrained drag operations.
 * Anchors the gizmo to the top of the selected 3D object for clear visibility.
 */
public class axis_drag_controller_ui_main {

    private final translation_gizmo_ui_main gizmo = new translation_gizmo_ui_main();
    private final Pane viewport;
    private final PerspectiveCamera camera;
    private final camera_controller_ui_main camCtrl;
    private final Group shapesGroup;
    private Consumer<String> statusCallback;

    private shape_item_ui_main target = null;
    private Axis draggingAxis = null;
    private Point3D startGizmoOrigin = null;
    private double startTargetX = 0, startTargetY = 0, startTargetZ = 0;
    private double axisAnchorScalar = 0;
    private boolean dragging = false;

    private static final Point3D DIR_X = new Point3D(1, 0, 0);
    private static final Point3D DIR_Y = new Point3D(0, 0, 1);
    private static final Point3D DIR_Z = new Point3D(0, -1, 0);

    public axis_drag_controller_ui_main(Pane viewport, PerspectiveCamera camera,
                                        camera_controller_ui_main camCtrl, Group shapesGroup) {
        this.viewport = viewport; this.camera = camera; this.camCtrl = camCtrl; this.shapesGroup = shapesGroup;
        shapesGroup.getChildren().add(gizmo);
    }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }
    public void attachTo(shape_item_ui_main item) { this.target = item; gizmo.setVisible(false); }

    public void detach() {
        target = null; dragging = false; draggingAxis = null; startGizmoOrigin = null;
        gizmo.setVisible(false); gizmo.clearHighlight();
    }

    public Axis axisForNode(Node node) { return gizmo.isVisible() ? gizmo.axisForNode(node) : null; }
    public boolean isGizmoNode(Node node) { return gizmo.isGizmoNode(node); }

    public Point3D getGizmoOrigin() {
        if (target == null) return new Point3D(0, 0, 0);
        Point3D a = getAnchor(target), c = target.getCenter();
        return shape_rotation_helper_ui_main.transformPoint(
            a, c, target.getRotationX(), target.getRotationY(),
            target.getWorldX(), target.getWorldY(), target.getWorldZ()
        );
    }

    public Axis findAxisNearRay(MouseEvent e) {
        if (!gizmo.isVisible() || target == null) return null;
        double[] ray = world_raycaster_ui_main.buildRay(e, viewport, camera, shapesGroup);
        if (ray == null) return null;
        Point3D worldPt = getGizmoOrigin();
        Axis[] axes = {Axis.X, Axis.Y, Axis.Z};
        Point3D[] dirs = {DIR_X, DIR_Y, DIR_Z};
        for (int i = 0; i < 3; i++) {
            double s = world_raycaster_ui_main.projectOnAxis(ray, worldPt, dirs[i]);
            if (!Double.isNaN(s) && s >= -2.0 && s <= 45.0) {
                Point3D q = worldPt.add(dirs[i].multiply(s));
                if (world_raycaster_ui_main.distRayToPoint(ray, q) <= 12.0) return axes[i];
            }
        }
        return null;
    }

    public boolean onAxisPressed(Node hitNode, MouseEvent e) {
        if (target == null || !target.getType().is3D()) return false;
        Axis a = axisForNode(hitNode);
        if (a == null) a = findAxisNearRay(e);
        if (a == null) return false;

        draggingAxis = a; dragging = true; camCtrl.setEnabled(false);
        startGizmoOrigin = getGizmoOrigin();
        startTargetX = target.getWorldX(); startTargetY = target.getWorldY(); startTargetZ = target.getWorldZ();

        double[] ray = world_raycaster_ui_main.buildRay(e, viewport, camera, shapesGroup);
        axisAnchorScalar = computeAxisScalar(ray, a, startGizmoOrigin);
        if (Double.isNaN(axisAnchorScalar)) axisAnchorScalar = 0;
        return true;
    }

    public boolean onDrag(MouseEvent e) {
        if (!dragging || target == null || draggingAxis == null) return false;

        double[] ray = world_raycaster_ui_main.buildRay(e, viewport, camera, shapesGroup);
        double currentScalar = computeAxisScalar(ray, draggingAxis, startGizmoOrigin);

        if (!Double.isNaN(currentScalar) && !Double.isNaN(axisAnchorScalar)) {
            double delta = currentScalar - axisAnchorScalar;
            switch (draggingAxis) {
                case X -> target.setWorldTranslation(startTargetX + delta, startTargetY, startTargetZ);
                case Y -> target.setWorldTranslation(startTargetX, startTargetY, startTargetZ + delta);
                case Z -> target.setWorldTranslation(startTargetX, startTargetY - delta, startTargetZ);
            }
            updateGizmoPosition();

            if (statusCallback != null) {
                double px = target.getWorldX() + (target.getP1() != null ? target.getP1().getX() : 0.0);
                double py = target.getWorldZ() + (target.getP1() != null ? target.getP1().getZ() : 0.0);
                double pz = -(target.getWorldY() + (target.getP1() != null ? target.getP1().getY() : 0.0));
                statusCallback.accept(String.format("%s — Pos: X=%.1f mm Y=%.1f mm Z=%.1f mm", target.getType().getLabel(), px, py, pz));
            }
            return true;
        }
        return false;
    }

    public boolean onReleased() {
        if (!dragging) return false;
        dragging = false; draggingAxis = null; startGizmoOrigin = null;
        camCtrl.setEnabled(true); updateGizmoPosition();
        return true;
    }

    public void onHover(Axis a) {
        if (!gizmo.isVisible()) return;
        if (a != null) gizmo.highlight(a); else gizmo.clearHighlight();
    }

    public void updateGizmoPosition() {
        if (target == null || !gizmo.isVisible()) return;
        Point3D p = getGizmoOrigin();
        gizmo.moveTo(p.getX(), p.getY(), p.getZ(), 0, 0, 0);
    }

    private double computeAxisScalar(double[] ray, Axis axis, Point3D origin) {
        if (ray == null || target == null || origin == null) return Double.NaN;
        Point3D axisDir = switch (axis) {
            case X -> DIR_X;
            case Y -> DIR_Y;
            case Z -> DIR_Z;
        };
        return world_raycaster_ui_main.projectOnAxis(ray, origin, axisDir);
    }

    private static Point3D getAnchor(shape_item_ui_main item) {
        Point3D c = item.getCenter(), p1 = item.getP1(), p2 = item.getP2();
        return switch (item.getType()) {
            case CUBE -> {
                double s = Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ()));
                yield new Point3D(c.getX(), -s, c.getZ());
            }
            case CUBOID -> {
                double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())) * 0.5);
                yield new Point3D(c.getX(), p1.getY() - h, c.getZ());
            }
            case CYLINDER, CONE -> {
                double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ())), h = Math.max(6.0, r * 2.0);
                yield new Point3D(c.getX(), -h, c.getZ());
            }
            case SPHERE -> new Point3D(c.getX(), -2.0 * p1.distance(new Point3D(p2.getX(), 0, p2.getZ())), c.getZ());
            default -> new Point3D(c.getX(), 0, c.getZ());
        };
    }

    public translation_gizmo_ui_main getGizmo() { return gizmo; }
    public boolean isDragging() { return dragging; }
}
