package ui.workspace.drafting.gizmo;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

/**
 * world_raycaster_ui_main.java
 * Constructs world-space mouse rays from camera and viewport state.
 * Provides axis-constrained projection for stable XYZ translation dragging.
 */
public final class world_raycaster_ui_main {

    private world_raycaster_ui_main() {}

    public static double[] buildRay(MouseEvent e, Pane viewport, PerspectiveCamera camera) {
        return buildRay(e, viewport, camera, null);
    }

    public static double[] buildRay(MouseEvent e, Pane viewport, PerspectiveCamera camera, Group coordinateGroup) {
        Point2D p = viewport_coordinate_helper_ui_main.getViewportPoint(e, viewport);
        return buildRay(p.getX(), p.getY(), viewport, camera, coordinateGroup);
    }

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
        return hitPlaneY(ray, 0);
    }

    public static Point3D hitPlaneY(double[] ray, double yPlane) {
        if (ray == null || Math.abs(ray[4]) < 1e-4) return null;
        double s = (yPlane - ray[1]) / ray[4];
        if (s <= 0) return null;
        return new Point3D(ray[0] + ray[3] * s, yPlane, ray[2] + ray[5] * s);
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

    public static double intersectTriangle(double[] ray, Point3D v0, Point3D v1, Point3D v2) {
        return intersectTriangle(ray, v0, v1, v2, null);
    }

    public static double intersectTriangle(double[] ray, Point3D v0, Point3D v1, Point3D v2, double[] outBary) {
        if (ray == null || ray.length < 6 || v0 == null || v1 == null || v2 == null) return -1.0;
        try {
            var bridge = backend.native_bridge.astra_native_bridge_ui_main.getInstance();
            if (bridge.isAvailable()) {
                double[] orig = new double[]{ ray[0], ray[1], ray[2] };
                double[] dir  = new double[]{ ray[3], ray[4], ray[5] };
                double[] p0   = new double[]{ v0.getX(), v0.getY(), v0.getZ() };
                double[] p1   = new double[]{ v1.getX(), v1.getY(), v1.getZ() };
                double[] p2   = new double[]{ v2.getX(), v2.getY(), v2.getZ() };
                return bridge.rayTriangleIntersect(orig, dir, p0, p1, p2, outBary);
            }
        } catch (Throwable ignored) {}
        return intersectTriangleJava(ray, v0, v1, v2, outBary);
    }

    public static double intersectTriangleJava(double[] ray, Point3D v0, Point3D v1, Point3D v2, double[] outBary) {
        if (ray == null || ray.length < 6 || v0 == null || v1 == null || v2 == null) return -1.0;
        double e1x = v1.getX() - v0.getX(), e1y = v1.getY() - v0.getY(), e1z = v1.getZ() - v0.getZ();
        double e2x = v2.getX() - v0.getX(), e2y = v2.getY() - v0.getY(), e2z = v2.getZ() - v0.getZ();
        double dx = ray[3], dy = ray[4], dz = ray[5];
        double pvx = dy * e2z - dz * e2y, pvy = dz * e2x - dx * e2z, pvz = dx * e2y - dy * e2x;
        double det = e1x * pvx + e1y * pvy + e1z * pvz;
        if (Math.abs(det) < 1e-7) return -1.0;
        double invDet = 1.0 / det;
        double tvx = ray[0] - v0.getX(), tvy = ray[1] - v0.getY(), tvz = ray[2] - v0.getZ();
        double u = (tvx * pvx + tvy * pvy + tvz * pvz) * invDet;
        if (u < 0.0 || u > 1.0) return -1.0;
        double qvx = tvy * e1z - tvz * e1y, qvy = tvz * e1x - tvx * e1z, qvz = tvx * e1y - tvy * e1x;
        double v = (dx * qvx + dy * qvy + dz * qvz) * invDet;
        if (v < 0.0 || u + v > 1.0) return -1.0;
        double t = (e2x * qvx + e2y * qvy + e2z * qvz) * invDet;
        if (t < 1e-7) return -1.0;
        if (outBary != null && outBary.length >= 2) {
            outBary[0] = u;
            outBary[1] = v;
        }
        return t;
    }

    public static int findNearestTriangleHit(double[] ray, float[] points, int[] faces, double[] outNearestT) {
        if (ray == null || points == null || faces == null) return -1;
        try {
            var bridge = backend.native_bridge.astra_native_bridge_ui_main.getInstance();
            if (bridge.isAvailable()) {
                double[] orig = new double[]{ ray[0], ray[1], ray[2] };
                double[] dir  = new double[]{ ray[3], ray[4], ray[5] };
                double[] hitInfo = new double[3];
                int hitIdx = bridge.rayMeshIntersectNearestF32(orig, dir, points, faces, 6, hitInfo);
                if (hitIdx >= 0) {
                    if (outNearestT != null && outNearestT.length > 0) outNearestT[0] = hitInfo[0];
                    return hitIdx;
                }
                return -1;
            }
        } catch (Throwable ignored) {}
        return findNearestTriangleHitJava(ray, points, faces, outNearestT);
    }

    public static int findNearestTriangleHitJava(double[] ray, float[] points, int[] faces, double[] outNearestT) {
        if (ray == null || points == null || faces == null) return -1;
        double minT = Double.MAX_VALUE;
        int hitIndex = -1;
        for (int i = 0; i < faces.length; i += 6) {
            int i0 = faces[i] * 3, i1 = faces[i + 2] * 3, i2 = faces[i + 4] * 3;
            if (i0 + 2 >= points.length || i1 + 2 >= points.length || i2 + 2 >= points.length) continue;
            Point3D v0 = new Point3D(points[i0], points[i0 + 1], points[i0 + 2]);
            Point3D v1 = new Point3D(points[i1], points[i1 + 1], points[i1 + 2]);
            Point3D v2 = new Point3D(points[i2], points[i2 + 1], points[i2 + 2]);
            double t = intersectTriangleJava(ray, v0, v1, v2, null);
            if (t > 1e-6 && t < minT) {
                minT = t;
                hitIndex = i / 6;
            }
        }
        if (hitIndex >= 0 && outNearestT != null && outNearestT.length > 0) outNearestT[0] = minT;
        return hitIndex;
    }
}
