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

                // Swept cutter footprint boundary containment check across complete segment
                cam.geometry.CamPoint2D p0 = new cam.geometry.CamPoint2D(s.start().x(), s.start().y());
                cam.geometry.CamPoint2D p1 = new cam.geometry.CamPoint2D(s.end().x(), s.end().y());
                double segLen = p0.distanceTo(p1);
                int samples = Math.max(2, (int) Math.ceil(segLen / Math.max(1.0, cutterRadius * 0.5)) + 1);

                for (int k = 0; k < samples; k++) {
                    double t = (double) k / (samples - 1);
                    cam.geometry.CamPoint2D pt = new cam.geometry.CamPoint2D(
                        p0.x() + t * (p1.x() - p0.x()),
                        p0.y() + t * (p1.y() - p0.y())
                    );
                    if (!boundary.contains(pt)) {
                        errors.add(String.format("Swept cutter path at segment %d (t=%.2f) crosses outside pocket boundary.", i, t));
                        break;
                    }
                    double dist = boundary.distanceToBoundary(pt);
                    if (dist < requiredClearance - 1e-4) {
                        errors.add(String.format("Swept cutter footprint at segment %d (t=%.2f) violates wall margin: dist=%.4f mm < required %.4f mm",
                                i, t, dist, requiredClearance));
                        break;
                    }
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static ValidationResult validateContourFinishing(Toolpath toolpath, cam.geometry.CamContour2D partBoundary,
                                                            cam.geometry.ContourSide side, double cutterRadius,
                                                            double allowance, double targetDepth, ManufacturingSetup setup) {
        var baseRes = validate(toolpath, setup);
        List<String> errors = new java.util.ArrayList<>(baseRes.errors());
        if (partBoundary == null || toolpath == null) {
            errors.add("Boundary or toolpath cannot be null for contour validation.");
            return new ValidationResult(false, errors);
        }
        double minAllowedZ = -targetDepth - 1e-4;
        boolean inside = (side == cam.geometry.ContourSide.INSIDE);

        for (int i = 0; i < toolpath.getSegments().size(); i++) {
            var s = toolpath.getSegments().get(i);
            if (s.motionType().isCutting()) {
                if (s.start().z() < minAllowedZ || s.end().z() < minAllowedZ) {
                    errors.add(String.format("Cutting segment %d exceeds maximum depth: minZ = %.4f mm < %.4f mm",
                            i, Math.min(s.start().z(), s.end().z()), minAllowedZ));
                }
                cam.geometry.CamPoint2D p0 = new cam.geometry.CamPoint2D(s.start().x(), s.start().y());
                cam.geometry.CamPoint2D p1 = new cam.geometry.CamPoint2D(s.end().x(), s.end().y());
                double segLen = p0.distanceTo(p1);
                int samples = Math.max(2, (int) Math.ceil(segLen / Math.max(0.5, cutterRadius * 0.5)) + 1);

                for (int k = 0; k < samples; k++) {
                    double t = (double) k / (samples - 1);
                    cam.geometry.CamPoint2D pt = new cam.geometry.CamPoint2D(
                        p0.x() + t * (p1.x() - p0.x()), p0.y() + t * (p1.y() - p0.y())
                    );
                    if (partBoundary.isClosed()) {
                        if (inside && !partBoundary.contains(pt)) {
                            errors.add(String.format("Swept cutter path at segment %d (t=%.2f) escapes cavity into part material.", i, t));
                            break;
                        } else if (!inside && partBoundary.contains(pt)) {
                            errors.add(String.format("Swept cutter path at segment %d (t=%.2f) gouges inside closed part boundary.", i, t));
                            break;
                        }
                        double dist = partBoundary.distanceToBoundary(pt);
                        double reqClearance = cutterRadius + allowance;
                        if (dist < reqClearance - 1e-3) {
                            errors.add(String.format("Swept cutter footprint at segment %d (t=%.2f) violates boundary clearance: dist=%.4f mm < required %.4f mm",
                                    i, t, dist, reqClearance));
                            break;
                        }
                    }
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
