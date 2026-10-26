package cam.toolpath;

import cam.model.CamPoint3D;
import cam.model.ManufacturingSetup;
import java.util.List;

/**
 * Geometric, topological, and kinematic safety validator for toolpath trajectories.
 */
public final class ToolpathValidator {

    private ToolpathValidator() {}

    public record ValidationResult(boolean isValid, List<String> errors) {}

    public static ValidationResult validate(Toolpath toolpath, ManufacturingSetup setup) {
        List<String> errors = new java.util.ArrayList<>();
        if (toolpath == null || toolpath.isEmpty()) {
            errors.add("Toolpath is null or empty.");
            return new ValidationResult(false, errors);
        }

        List<ToolpathSegment> segs = toolpath.getSegments();

        // 1. Path Continuity Check
        for (int i = 0; i < segs.size() - 1; i++) {
            CamPoint3D currentEnd = segs.get(i).end();
            CamPoint3D nextStart = segs.get(i + 1).start();
            double gap = currentEnd.distanceTo(nextStart);
            if (gap > 1e-4) {
                errors.add(String.format("Path discontinuity between segment %d and %d: gap = %.6f mm", i, i + 1, gap));
            }
        }

        // 2. Safety Plane Transit Check
        if (setup != null && setup.getSafetyPlane() != null) {
            double safetyZ = setup.getSafetyPlane().getClearanceZ();
            for (int i = 0; i < segs.size(); i++) {
                var s = segs.get(i);
                if (s.motionType() == MotionType.RAPID) {
                    double xyDist = s.start().distanceXY(s.end());
                    if (xyDist > 1e-3) { // Horizontal or 3D transit
                        if (s.start().z() < safetyZ - 1e-3 || s.end().z() < safetyZ - 1e-3) {
                            errors.add(String.format("Rapid transit at segment %d occurs below safety clearance (startZ=%.3f, endZ=%.3f, safetyZ=%.3f)",
                                    i, s.start().z(), s.end().z(), safetyZ));
                        }
                    }
                }
            }
        }

        // 3. Motion Type Feed Rate Consistency Check
        for (int i = 0; i < segs.size(); i++) {
            var s = segs.get(i);
            if (s.motionType().isCutting() && s.feedRateMmPerMin() <= 0.0) {
                errors.add(String.format("Cutting segment %d has non-positive feed rate (%.2f mm/min)", i, s.feedRateMmPerMin()));
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static ValidationResult validatePocketRoughing(Toolpath toolpath, cam.geometry.CamPolygon2D boundary,
                                                          double cutterRadius, double allowance,
                                                          double targetDepth, ManufacturingSetup setup) {
        var baseRes = validate(toolpath, setup);
        List<String> errors = new java.util.ArrayList<>(baseRes.errors());
        if (boundary == null || toolpath == null) {
            errors.add("Boundary or toolpath cannot be null for pocket validation.");
            return new ValidationResult(false, errors);
        }

        double requiredClearance = cutterRadius + allowance;
        double minAllowedZ = -targetDepth - 1e-4;

        for (int i = 0; i < toolpath.getSegments().size(); i++) {
            var s = toolpath.getSegments().get(i);
            if (s.motionType().isCutting()) {
                // Depth check
                if (s.start().z() < minAllowedZ || s.end().z() < minAllowedZ) {
                    errors.add(String.format("Cutting segment %d exceeds maximum depth: minZ = %.4f mm < %.4f mm",
                            i, Math.min(s.start().z(), s.end().z()), minAllowedZ));
                }

                // Footprint boundary containment check
                cam.geometry.CamPoint2D pt = new cam.geometry.CamPoint2D(s.end().x(), s.end().y());
                if (!boundary.contains(pt)) {
                    errors.add(String.format("Cutter center at segment %d (%.3f, %.3f) is outside pocket boundary.", i, pt.x(), pt.y()));
                } else {
                    double dist = boundary.distanceToBoundary(pt);
                    if (dist < requiredClearance - 1e-4) {
                        errors.add(String.format("Cutter footprint at segment %d violates wall margin: dist=%.4f mm < required %.4f mm",
                                i, dist, requiredClearance));
                    }
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
