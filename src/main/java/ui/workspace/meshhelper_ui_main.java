package ui.workspace;

import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * meshhelper_ui_main.java
 * High-precision 3D mesh geometry, material generator, and ray intersection math for CAD primitives.
 */
public final class meshhelper_ui_main {

    private meshhelper_ui_main() {}

    public static PhongMaterial createMaterial(Color diffuse, Color specular) {
        PhongMaterial mat = new PhongMaterial(diffuse);
        mat.setSpecularColor(specular);
        mat.setSpecularPower(32.0);
        return mat;
    }

    public static Point3D[] screenToRay(Camera camera, double w, double h, double sx, double sy) {
        if (w <= 0 || h <= 0 || camera == null) return null;
        double fov = (camera instanceof PerspectiveCamera pc) ? pc.getFieldOfView() : 45.0;
        double fovRad = Math.toRadians(fov);
        double focalLen = (h / 2.0) / Math.tan(fovRad / 2.0);
        double dx = sx - (w / 2.0);
        double dy = sy - (h / 2.0);

        Point3D origin = camera.localToScene(new Point3D(0, 0, 0));
        Point3D rayPtCam = camera.localToScene(new Point3D(dx / focalLen, dy / focalLen, 1.0));
        Point3D dir = rayPtCam.subtract(origin).normalize();
        return new Point3D[] { origin, dir };
    }

    public static Point3D screenToGround(Camera camera, double w, double h, double sx, double sy) {
        Point3D[] ray = screenToRay(camera, w, h, sx, sy);
        if (ray == null) return null;
        Point3D origin = ray[0], dir = ray[1];
        if (Math.abs(dir.getY()) < 1e-4) return null;
        double s = -origin.getY() / dir.getY();
        if (s <= 0) return null;
        Point3D hit = origin.add(dir.multiply(s));
        return new Point3D(hit.getX(), 0, hit.getZ());
    }

    public static double distancePointToRay(Point3D pt, Point3D origin, Point3D dir) {
        Point3D v = pt.subtract(origin);
        double t = v.dotProduct(dir);
        if (t <= 0) return pt.distance(origin);
        Point3D closest = origin.add(dir.multiply(t));
        return pt.distance(closest);
    }

    public static boolean rayHitsAABB(Point3D origin, Point3D dir,
                                      double xMin, double xMax,
                                      double yMin, double yMax,
                                      double zMin, double zMax) {
        double dx = Math.abs(dir.getX()) < 1e-6 ? (dir.getX() >= 0 ? 1e-6 : -1e-6) : dir.getX();
        double dy = Math.abs(dir.getY()) < 1e-6 ? (dir.getY() >= 0 ? 1e-6 : -1e-6) : dir.getY();
        double dz = Math.abs(dir.getZ()) < 1e-6 ? (dir.getZ() >= 0 ? 1e-6 : -1e-6) : dir.getZ();

        double tMin = Math.max(Math.max(Math.min((xMin - origin.getX()) / dx, (xMax - origin.getX()) / dx),
                                       Math.min((yMin - origin.getY()) / dy, (yMax - origin.getY()) / dy)),
                               Math.min((zMin - origin.getZ()) / dz, (zMax - origin.getZ()) / dz));
        double tMax = Math.min(Math.min(Math.max((xMin - origin.getX()) / dx, (xMax - origin.getX()) / dx),
                                       Math.max((yMin - origin.getY()) / dy, (yMax - origin.getY()) / dy)),
                               Math.max((zMin - origin.getZ()) / dz, (zMax - origin.getZ()) / dz));
        return tMax >= Math.max(tMin, 0.0);
    }

    public static boolean rayHitsVerticalCylinder(Point3D origin, Point3D dir, Point3D center, double radius, double height) {
        Point3D pXZ = new Point3D(center.getX(), 0, center.getZ());
        Point3D oXZ = new Point3D(origin.getX(), 0, origin.getZ());
        Point3D dXZ = new Point3D(dir.getX(), 0, dir.getZ());
        double dMag = dXZ.magnitude();
        if (dMag < 1e-6) return pXZ.distance(oXZ) <= radius && origin.getY() >= -height && origin.getY() <= 0;
        dXZ = dXZ.multiply(1.0 / dMag);
        double t2D = pXZ.subtract(oXZ).dotProduct(dXZ);
        if (t2D < 0) return false;
        Point3D closest2D = oXZ.add(dXZ.multiply(t2D));
        if (pXZ.distance(closest2D) > radius) return false;
        double yAtHit = origin.getY() + dir.getY() * (t2D / dMag);
        return yAtHit >= -height - 2.0 && yAtHit <= 2.0;
    }

    public static boolean rayHitsSphere(Point3D origin, Point3D dir, Point3D center, double radius) {
        return distancePointToRay(center, origin, dir) <= radius;
    }

    public static Node createUpwardCone(double radius, double height, PhongMaterial material) {
        TriangleMesh mesh = new TriangleMesh();
        int sides = 24;
        float[] points = new float[(sides + 2) * 3];
        points[0] = 0; points[1] = (float) -height; points[2] = 0;
        points[3] = 0; points[4] = 0; points[5] = 0;
        for (int i = 0; i < sides; i++) {
            double a = i * 2 * Math.PI / sides;
            points[(i + 2) * 3]     = (float) (radius * Math.cos(a));
            points[(i + 2) * 3 + 1] = 0;
            points[(i + 2) * 3 + 2] = (float) (radius * Math.sin(a));
        }
        float[] texCoords = {0.5f, 0.5f};
        int[] faces = new int[sides * 2 * 6];
        int idx = 0;
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            faces[idx++] = 0;        faces[idx++] = 0;
            faces[idx++] = next + 2; faces[idx++] = 0;
            faces[idx++] = i + 2;    faces[idx++] = 0;
            faces[idx++] = 1;        faces[idx++] = 0;
            faces[idx++] = i + 2;    faces[idx++] = 0;
            faces[idx++] = next + 2; faces[idx++] = 0;
        }
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);
        MeshView meshView = new MeshView(mesh);
        meshView.setMaterial(material);
        return meshView;
    }
}
