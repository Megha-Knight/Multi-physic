package ui.workspace.triad;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.sketch.sketch_coord_system_ui_main;
import ui.workspace.drafting.sketch.sketch_plane_type_ui_main;

/**
 * coordinate_triad_math_ui_main.java
 * Authoritative mathematical calculations, orthonormal basis verification,
 * and coordinate frame extraction for global and local CAD triads.
 */
public final class coordinate_triad_math_ui_main {

    public static final double CAD_TOLERANCE = 1e-4;

    public static final Point3D WORLD_ORIGIN = Point3D.ZERO;
    public static final Point3D WORLD_X = new Point3D(1, 0, 0);  // Right (+X)
    public static final Point3D WORLD_Y = new Point3D(0, 0, 1);  // Depth/Floor (+Z)
    public static final Point3D WORLD_Z = new Point3D(0, -1, 0); // Up/Sky (-Y)

    private coordinate_triad_math_ui_main() {}

    public record TriadFrame(
        Point3D origin,
        Point3D uAxis,
        Point3D vAxis,
        Point3D wAxis,
        String name,
        boolean isLocal
    ) {
        public boolean isValid() {
            return isOrthonormal(uAxis, vAxis, wAxis, 1e-3)
                && isRightHanded(uAxis, vAxis, wAxis, 1e-3);
        }
    }

    public static TriadFrame getWorldFrame() {
        return new TriadFrame(WORLD_ORIGIN, WORLD_X, WORLD_Y, WORLD_Z, "World", false);
    }

    public static boolean isOrthonormal(Point3D u, Point3D v, Point3D w, double tol) {
        if (u == null || v == null || w == null) return false;
        double lu = u.magnitude(), lv = v.magnitude(), lw = w.magnitude();
        if (Math.abs(lu - 1.0) > tol || Math.abs(lv - 1.0) > tol || Math.abs(lw - 1.0) > tol) return false;
        if (Math.abs(u.dotProduct(v)) > tol) return false;
        if (Math.abs(v.dotProduct(w)) > tol) return false;
        if (Math.abs(w.dotProduct(u)) > tol) return false;
        return true;
    }

    public static boolean isRightHanded(Point3D u, Point3D v, Point3D w, double tol) {
        if (u == null || v == null || w == null) return false;
        Point3D cross = u.crossProduct(v);
        if (cross.dotProduct(w) <= 0.0) return false;
        return cross.distance(w) <= tol;
    }

    public static TriadFrame resolveLocalFrame(sketch_coord_system_ui_main cs) {
        if (cs == null) return null;
        Point3D origin = cs.getOrigin() != null ? cs.getOrigin() : Point3D.ZERO;
        Point3D u = cs.getUAxis() != null ? cs.getUAxis().normalize() : WORLD_X;
        Point3D v = cs.getVAxis() != null ? cs.getVAxis().normalize() : WORLD_Y;

        Point3D w = cs.getNormal() != null ? cs.getNormal().normalize() : u.crossProduct(v).normalize();
        if (w.magnitude() < 1e-4) {
            return new TriadFrame(origin, WORLD_X, WORLD_Y, WORLD_Z, "Default Plane", true);
        }
        if (u.crossProduct(v).dotProduct(w) < 0.0) {
            v = w.crossProduct(u).normalize();
        } else {
            w = u.crossProduct(v).normalize();
        }

        String name = (cs.getPlaneType() != null) ? cs.getPlaneType().name() : "Local";
        if (cs.getPlaneType() == sketch_plane_type_ui_main.FACE && cs.getFaceKind() != null) {
            name = cs.getFaceKind().getLabel();
        }
        return new TriadFrame(origin, u, v, w, name, true);
    }

    public static TriadFrame resolveFaceFrame(face_reference_ui_main faceRef) {
        if (faceRef == null) return null;
        Point3D origin = faceRef.getFaceOrigin() != null ? faceRef.getFaceOrigin() : Point3D.ZERO;
        Point3D u = faceRef.getUAxis() != null ? faceRef.getUAxis().normalize() : WORLD_X;
        Point3D v = faceRef.getVAxis() != null ? faceRef.getVAxis().normalize() : WORLD_Y;
        Point3D w = faceRef.getWorldNormal() != null ? faceRef.getWorldNormal().normalize() : u.crossProduct(v).normalize();
        if (u.crossProduct(v).dotProduct(w) < 0.0) {
            v = w.crossProduct(u).normalize();
        } else {
            w = u.crossProduct(v).normalize();
        }
        String name = (faceRef.getFaceKind() != null) ? faceRef.getFaceKind().getLabel() : "Face";
        return new TriadFrame(origin, u, v, w, name, true);
    }

    public static Point3D worldToLocal(Point3D worldPt, TriadFrame frame) {
        if (worldPt == null || frame == null) return Point3D.ZERO;
        Point3D delta = worldPt.subtract(frame.origin());
        return new Point3D(
            delta.dotProduct(frame.uAxis()),
            delta.dotProduct(frame.vAxis()),
            delta.dotProduct(frame.wAxis())
        );
    }

    public static Point3D localToWorld(Point3D localPt, TriadFrame frame) {
        if (localPt == null || frame == null) return Point3D.ZERO;
        return frame.origin()
            .add(frame.uAxis().multiply(localPt.getX()))
            .add(frame.vAxis().multiply(localPt.getY()))
            .add(frame.wAxis().multiply(localPt.getZ()));
    }
}
