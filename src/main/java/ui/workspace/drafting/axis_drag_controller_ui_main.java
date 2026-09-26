package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.drafting.translation_gizmo_ui_main.Axis;

import java.util.function.Consumer;

/**
 * axis_drag_controller_ui_main.java
 * Manages the translation gizmo lifecycle and axis-constrained drag operations.
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
    private double axisAnchorScalar = 0;
    private boolean dragging = false;

    private static final Point3D DIR_X = new Point3D(1, 0, 0);
    private static final Point3D DIR_Y = new Point3D(0, 0, 1);
    private static final Point3D DIR_Z = new Point3D(0, -1, 0);

    public axis_drag_controller_ui_main(Pane viewport, PerspectiveCamera camera,
                                        camera_controller_ui_main camCtrl, Group shapesGroup) {
        this.viewport    = viewport;
        this.camera      = camera;
        this.camCtrl     = camCtrl;
        this.shapesGroup = shapesGroup;
        shapesGroup.getChildren().add(gizmo);
    }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }

    public void attachTo(shape_item_ui_main item) {
        this.target = item;
        if (item != null && item.getType().is3D()) {
            gizmo.toFront();
            updateGizmoPosition();
            gizmo.setVisible(true);
        } else {
            gizmo.setVisible(false);
        }
    }

    public void detach() {
        target = null;
        dragging = false;
        draggingAxis = null;
        gizmo.setVisible(false);
        gizmo.clearHighlight();
    }

    public Axis axisForNode(Node node) {
        return gizmo.isVisible() ? gizmo.axisForNode(node) : null;
    }

    public boolean isGizmoNode(Node node) { return gizmo.isGizmoNode(node); }

    public Axis findAxisNearRay(MouseEvent e) {
        if (!gizmo.isVisible() || target == null) return null;
        double[] ray = world_raycaster_ui_main.buildRay(e.getX(), e.getY(), viewport, camera, shapesGroup);
        if (ray == null) return null;
        Point3D anchor = getAnchor(target);
        Point3D worldPt = new Point3D(anchor.getX() + target.getWorldX(),
            anchor.getY() + target.getWorldY(), anchor.getZ() + target.getWorldZ());
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

        draggingAxis = a;
        dragging = true;
        camCtrl.setEnabled(false);

        double[] ray = world_raycaster_ui_main.buildRay(e.getX(), e.getY(), viewport, camera, shapesGroup);
        axisAnchorScalar = computeAxisScalar(ray, a);
        return true;
    }

    public boolean onDrag(MouseEvent e) {
        if (!dragging || target == null || draggingAxis == null) return false;

        double[] ray = world_raycaster_ui_main.buildRay(e.getX(), e.getY(), viewport, camera, shapesGroup);
        double currentScalar = computeAxisScalar(ray, draggingAxis);

        if (!Double.isNaN(currentScalar) && !Double.isNaN(axisAnchorScalar)) {
            double delta = currentScalar - axisAnchorScalar;
            axisAnchorScalar = currentScalar;

            switch (draggingAxis) {
                case X -> target.applyWorldDelta(delta, 0, 0);
                case Y -> target.applyWorldDelta(0, 0, delta);
                case Z -> target.applyWorldDelta(0, -delta, 0);
            }
            updateGizmoPosition();

            if (statusCallback != null)
                statusCallback.accept(String.format(
                    "%s — Pos: X=%.1f Y=%.1f Z=%.1f",
                    target.getType().getLabel(), target.getWorldX(), target.getWorldZ(), -target.getWorldY()
                ));
        }
        return true;
    }

    public boolean onReleased() {
        if (!dragging) return false;
        dragging = false;
        draggingAxis = null;
        camCtrl.setEnabled(true);
        updateGizmoPosition();
        return true;
    }

    public void onHover(Node node) {
        if (!gizmo.isVisible()) return;
        Axis a = axisForNode(node);
        if (a != null) gizmo.highlight(a);
        else gizmo.clearHighlight();
    }

    public void updateGizmoPosition() {
        if (target == null) return;
        Point3D a = getAnchor(target);
        gizmo.moveTo(target.getWorldX(), target.getWorldY(), target.getWorldZ(),
                     a.getX(), a.getY(), a.getZ());
    }

    private double computeAxisScalar(double[] ray, Axis axis) {
        if (ray == null || target == null) return Double.NaN;
        Point3D a = getAnchor(target);
        Point3D worldPt = new Point3D(
            a.getX() + target.getWorldX(),
            a.getY() + target.getWorldY(),
            a.getZ() + target.getWorldZ()
        );
        Point3D axisDir = switch (axis) {
            case X -> DIR_X;
            case Y -> DIR_Y;
            case Z -> DIR_Z;
        };
        return world_raycaster_ui_main.projectOnAxis(ray, worldPt, axisDir);
    }

    private static Point3D getAnchor(shape_item_ui_main item) {
        Point3D p1 = item.getP1(), p2 = item.getP2();
        return switch (item.getType()) {
            case CUBE -> {
                double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
                double s = Math.max(Math.abs(dx), Math.abs(dz));
                double cx = p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5);
                double cz = p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5);
                yield new Point3D(cx, -s, cz);
            }
            case CYLINDER, CONE -> {
                double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
                double h = Math.max(6.0, r * 2.0);
                yield new Point3D(p1.getX(), -h, p1.getZ());
            }
            case SPHERE -> {
                double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
                yield new Point3D(p1.getX(), -2.0 * r, p1.getZ());
            }
            default -> new Point3D((p1.getX() + p2.getX()) * 0.5, 0, (p1.getZ() + p2.getZ()) * 0.5);
        };
    }

    public translation_gizmo_ui_main getGizmo() { return gizmo; }
    public boolean isDragging() { return dragging; }
}
