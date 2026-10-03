package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import java.util.Collection;
import java.util.List;

/**
 * topology_validator_ui_main.java
 * Lightweight geometry and topology sanity validator for Astra CAD.
 */
public final class topology_validator_ui_main {

    private topology_validator_ui_main() {}

    public record ValidationResult(boolean valid, String message, int errorCount) {
        public static ValidationResult ok() { return new ValidationResult(true, "OK", 0); }
        public static ValidationResult fail(String msg) { return new ValidationResult(false, msg, 1); }
    }

    public static ValidationResult validatePoint(Point3D p, String label) {
        if (p == null) return ValidationResult.fail(label + " is null");
        if (Double.isNaN(p.getX()) || Double.isNaN(p.getY()) || Double.isNaN(p.getZ())) {
            return ValidationResult.fail(label + " contains NaN: " + p);
        }
        if (Double.isInfinite(p.getX()) || Double.isInfinite(p.getY()) || Double.isInfinite(p.getZ())) {
            return ValidationResult.fail(label + " contains Infinity: " + p);
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validateFaceReference(face_reference_ui_main ref, Collection<shape_item_ui_main> shapes) {
        if (ref == null) return ValidationResult.fail("FaceReference is null");
        if (ref.getOwnerShapeId() == null || ref.getFaceKind() == null) {
            return ValidationResult.fail("Incomplete face reference identity");
        }
        var pRes = validatePoint(ref.getWorldHitPoint(), "FaceReference.worldHitPoint");
        if (!pRes.valid()) return pRes;
        var nRes = validatePoint(ref.getWorldNormal(), "FaceReference.worldNormal");
        if (!nRes.valid()) return nRes;

        if (shapes != null) {
            shape_item_ui_main host = null;
            for (shape_item_ui_main s : shapes) if (s != null && ref.getOwnerShapeId().equals(s.getId())) { host = s; break; }
            if (host == null) return ValidationResult.fail("Host shape not found: " + ref.getOwnerShapeId());
            topology_body_ui_main topo = topology_body_ui_main.buildTopology(host);
            if (topo == null || topo.getFace(ref.getFaceKind()) == null) {
                return ValidationResult.fail("Face " + ref.getFaceKind() + " does not exist on host " + host.getId());
            }
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validateHoleFeature(hole_feature_ui_main hole, shape_item_ui_main host) {
        if (hole == null) return ValidationResult.fail("Hole is null");
        if (Double.isNaN(hole.getU()) || Double.isNaN(hole.getV()) || Double.isNaN(hole.getDiameter()) || Double.isNaN(hole.getDepth())) {
            return ValidationResult.fail("Hole has NaN parameters: " + hole.getId());
        }
        if (hole.getDiameter() < 0.1) return ValidationResult.fail("Hole diameter too small (< 0.1mm)");
        if (host != null) {
            if (!hole.getOwnerShapeId().equals(host.getId())) {
                return ValidationResult.fail("Hole owner mismatch: expected " + host.getId() + " but got " + hole.getOwnerShapeId());
            }
            if (!topology_geometry_helper_ui_main.fitsWithinFace(host, hole.getFaceKind(), hole.getU(), hole.getV(), hole.getOuterRadius())) {
                return ValidationResult.fail("Hole " + hole.getId() + " exceeds host face boundary");
            }
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validateMesh(TriangleMesh mesh) {
        if (mesh == null) return ValidationResult.ok();
        float[] points = new float[mesh.getPoints().size()];
        mesh.getPoints().toArray(points);
        for (int i = 0; i < points.length; i++) {
            if (Float.isNaN(points[i]) || Float.isInfinite(points[i])) {
                return ValidationResult.fail("Mesh point index " + i + " is NaN or Infinite");
            }
        }
        int numPoints = points.length / 3;
        int[] faces = new int[mesh.getFaces().size()];
        mesh.getFaces().toArray(faces);
        for (int i = 0; i < faces.length; i += 6) {
            int p1 = faces[i], p2 = faces[i + 2], p3 = faces[i + 4];
            if (p1 < 0 || p1 >= numPoints || p2 < 0 || p2 >= numPoints || p3 < 0 || p3 >= numPoints) {
                return ValidationResult.fail("Mesh face references out-of-bounds point index at face offset " + i);
            }
            if (p1 == p2 || p2 == p3 || p1 == p3) {
                return ValidationResult.fail("Degenerate triangle with duplicate vertices at face offset " + i);
            }
        }
        return ValidationResult.ok();
    }
}
