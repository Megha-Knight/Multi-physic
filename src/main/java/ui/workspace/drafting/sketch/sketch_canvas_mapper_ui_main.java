package ui.workspace.drafting.sketch;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.drafting.gizmo.viewport_coordinate_helper_ui_main;
import ui.workspace.drafting.gizmo.world_raycaster_ui_main;

/**
 * sketch_canvas_mapper_ui_main.java
 * High-precision mathematical mapping between screen mouse events,
 * world-space 3D rays, sketch planes, and 2D parametric (u, v) coordinates.
 */
public final class sketch_canvas_mapper_ui_main {

    public static final double PARALLEL_EPSILON = 1e-6;

    private sketch_canvas_mapper_ui_main() {}

    /**
     * Intersects a world-space ray with an arbitrary 3D plane defined by origin and normal.
     * @return 3D world intersection point, or null if parallel or behind ray origin.
     */
    public static Point3D intersectPlane(double[] ray, Point3D planeOrigin, Point3D planeNormal) {
        if (ray == null || ray.length < 6 || planeOrigin == null || planeNormal == null) return null;
        double dx = ray[3], dy = ray[4], dz = ray[5];
        if (Double.isNaN(dx) || Double.isNaN(dy) || Double.isNaN(dz)) return null;

        double nx = planeNormal.getX(), ny = planeNormal.getY(), nz = planeNormal.getZ();
        double denom = dx * nx + dy * ny + dz * nz;
        if (Math.abs(denom) < PARALLEL_EPSILON) return null; // Ray parallel to plane

        double ox = ray[0], oy = ray[1], oz = ray[2];
        double wox = planeOrigin.getX() - ox;
        double woy = planeOrigin.getY() - oy;
        double woz = planeOrigin.getZ() - oz;
        double num = wox * nx + woy * ny + woz * nz;

        double t = num / denom;
        if (t <= 0.0 || Double.isInfinite(t) || Double.isNaN(t)) return null; // Behind camera or invalid

        return new Point3D(ox + dx * t, oy + dy * t, oz + dz * t);
    }

    /**
     * Maps a MouseEvent on viewport directly into 2D sketch plane coordinates (u, v).
     */
    public static sketch_point_2d_ui_main screenToSketch(MouseEvent e, Pane viewport, PerspectiveCamera camera,
                                                         Group coordinateGroup, sketch_coord_system_ui_main cs) {
        if (e == null || viewport == null || camera == null || cs == null) return null;
        double[] ray = world_raycaster_ui_main.buildRay(e, viewport, camera, coordinateGroup);
        if (ray == null) return null;

        Point3D worldHit = intersectPlane(ray, cs.getOrigin(), cs.getNormal());
        if (worldHit == null) return null;

        return cs.toSketchPoint(worldHit);
    }

    /**
     * Maps viewport pixel coordinates (vx, vy) into 2D sketch plane coordinates (u, v).
     */
    public static sketch_point_2d_ui_main viewportToSketch(double vx, double vy, Pane viewport,
                                                           PerspectiveCamera camera, Group coordinateGroup,
                                                           sketch_coord_system_ui_main cs) {
        if (viewport == null || camera == null || cs == null) return null;
        double[] ray = world_raycaster_ui_main.buildRay(vx, vy, viewport, camera, coordinateGroup);
        if (ray == null) return null;

        Point3D worldHit = intersectPlane(ray, cs.getOrigin(), cs.getNormal());
        if (worldHit == null) return null;

        return cs.toSketchPoint(worldHit);
    }

    /**
     * Maps a 2D sketch point (u, v) into 3D world coordinates.
     */
    public static Point3D sketchToWorld(sketch_point_2d_ui_main pt, sketch_coord_system_ui_main cs) {
        if (pt == null || cs == null) return Point3D.ZERO;
        return cs.toWorldPoint(pt);
    }

    /**
     * Maps a 3D world coordinate into 2D sketch plane coordinates (u, v).
     */
    public static sketch_point_2d_ui_main worldToSketch(Point3D worldPt, sketch_coord_system_ui_main cs) {
        if (worldPt == null || cs == null) return sketch_point_2d_ui_main.ZERO;
        return cs.toSketchPoint(worldPt);
    }

    /**
     * Projects a 3D point onto the 3D sketch plane.
     */
    public static Point3D projectWorldToSketchPlane(Point3D worldPt, sketch_coord_system_ui_main cs) {
        if (worldPt == null || cs == null) return Point3D.ZERO;
        return cs.projectWorldToPlane(worldPt);
    }
}
