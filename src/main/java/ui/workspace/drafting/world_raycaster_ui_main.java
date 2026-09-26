package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.layout.Pane;

/**
 * world_raycaster_ui_main.java
 * Constructs world-space mouse rays from camera and viewport state.
 * Provides axis-constrained projection for stable XYZ translation dragging.
 */
public final class world_raycaster_ui_main {

    private world_raycaster_ui_main() {}

    public static double[] buildRay(double sx, double sy,
                                    Pane viewport, PerspectiveCamera camera) {
        return buildRay(sx, sy, viewport, camera, null);
    }

    public static double[] buildRay(double sx, double sy,
                                    Pane viewport, PerspectiveCamera camera, Group coordinateGroup) {
        double w = viewport.getWidth(), h = viewport.getHeight();
        if (w <= 0 || h <= 0 || camera == null) return null;

        double fovRad   = Math.toRadians(camera.getFieldOfView());
        double focalLen = (h / 2.0) / Math.tan(fovRad / 2.0);
        double ndcX     = (sx - w / 2.0) / focalLen;
        double ndcY     = (sy - h / 2.0) / focalLen;

        Point3D camOriginScene = camera.localToScene(new Point3D(0, 0, 0));
        Point3D rayPtScene     = camera.localToScene(new Point3D(ndcX, ndcY, 1.0));

        Point3D camOrigin = (coordinateGroup != null)
            ? coordinateGroup.sceneToLocal(camOriginScene) : camOriginScene;
        Point3D rayPt = (coordinateGroup != null)
            ? coordinateGroup.sceneToLocal(rayPtScene) : rayPtScene;
        Point3D dir = rayPt.subtract(camOrigin).normalize();

        return new double[]{
            camOrigin.getX(), camOrigin.getY(), camOrigin.getZ(),
            dir.getX(),       dir.getY(),       dir.getZ()
        };
    }

    public static Point3D hitGround(double[] ray) {
        if (ray == null || Math.abs(ray[4]) < 1e-4) return null;
        double s = -ray[1] / ray[4];
        if (s <= 0) return null;
        return new Point3D(ray[0] + ray[3] * s, 0, ray[2] + ray[5] * s);
    }

    public static double projectOnAxis(double[] ray, Point3D axisPoint, Point3D axis) {
        if (ray == null) return Double.NaN;

        double ox = ray[0], oy = ray[1], oz = ray[2];
        double dx = ray[3], dy = ray[4], dz = ray[5];
        double ax = axis.getX(), ay = axis.getY(), az = axis.getZ();
        double px = axisPoint.getX(), py = axisPoint.getY(), pz = axisPoint.getZ();

        double wx = ox - px, wy = oy - py, wz = oz - pz;
        double d_dot_d = dx*dx + dy*dy + dz*dz;
        double d_dot_a = dx*ax + dy*ay + dz*az;
        double a_dot_a = ax*ax + ay*ay + az*az;
        double w_dot_d = wx*dx + wy*dy + wz*dz;
        double w_dot_a = wx*ax + wy*ay + wz*az;

        double denom = d_dot_d * a_dot_a - d_dot_a * d_dot_a;
        if (Math.abs(denom) < 1e-5) return Double.NaN;
        return (d_dot_d * w_dot_a - d_dot_a * w_dot_d) / denom;
    }

    public static double distRayToPoint(double[] ray, Point3D pt) {
        if (ray == null || pt == null) return Double.MAX_VALUE;
        double vx = pt.getX() - ray[0], vy = pt.getY() - ray[1], vz = pt.getZ() - ray[2];
        double t = vx * ray[3] + vy * ray[4] + vz * ray[5];
        double px = ray[0] + ray[3] * t, py = ray[1] + ray[4] * t, pz = ray[2] + ray[5] * t;
        double dx = pt.getX() - px, dy = pt.getY() - py, dz = pt.getZ() - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
