package ui.workspace.drafting.holes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_geometry_helper_ui_main;

/**
 * hole_pattern_ui_main.java
 * Parametric Hole Pattern feature (Linear Pattern and Circular Pattern).
 * Replicates a seed hole parametrically without duplicating base geometry definitions.
 */
public class hole_pattern_ui_main {

    public enum PatternType {
        LINEAR("Linear Pattern"), CIRCULAR("Circular Pattern");
        private final String label;
        PatternType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum LinearDirection {
        U_DIR("U Direction"), V_DIR("V Direction");
        public static final LinearDirection ALONG_U = U_DIR;
        public static final LinearDirection ALONG_V = V_DIR;
        private final String label;
        LinearDirection(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final String id, ownerShapeId, seedHoleId;
    private final PatternType patternType;
    private LinearDirection direction = LinearDirection.U_DIR;
    private int count = 2;
    private double spacing = 15.0, centerU = 0.0, centerV = 0.0, angularSpan = 360.0;
    private boolean clockwise = false, fullCircle = true, visible = true;
    private ui.workspace.drafting.features.feature_state_ui_main state = ui.workspace.drafting.features.feature_state_ui_main.CLEAN;
    private String diagnosticMessage = null;

    public static hole_pattern_ui_main createLinear(String ownerId, String seedId, int count, LinearDirection dir, double spacing) {
        return new hole_pattern_ui_main(UUID.randomUUID().toString(), ownerId, seedId, dir, count, spacing);
    }
    public static hole_pattern_ui_main createCircular(String ownerId, String seedId, int count, double cu, double cv, double span, boolean cw, boolean full) {
        return new hole_pattern_ui_main(UUID.randomUUID().toString(), ownerId, seedId, cu, cv, count, span, cw, full);
    }
    public hole_pattern_ui_main(String id, String ownerShapeId, String seedHoleId, LinearDirection direction, int count, double spacing) {
        this(id, ownerShapeId, seedHoleId, PatternType.LINEAR, count, direction, spacing, 0, 0, 360, false, true);
    }
    public hole_pattern_ui_main(String id, String ownerShapeId, String seedHoleId, double centerU, double centerV, int count, double angularSpan, boolean clockwise, boolean fullCircle) {
        this(id, ownerShapeId, seedHoleId, PatternType.CIRCULAR, count, LinearDirection.U_DIR, 15, centerU, centerV, angularSpan, clockwise, fullCircle);
    }
    public hole_pattern_ui_main(String id, String ownerShapeId, String seedHoleId, PatternType type, int count,
                                LinearDirection dir, double spacing, double cu, double cv, double span, boolean cw, boolean full) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId; this.seedHoleId = seedHoleId; this.patternType = (type != null) ? type : PatternType.LINEAR;
        this.direction = (dir != null) ? dir : LinearDirection.U_DIR; this.count = count; this.spacing = spacing;
        this.centerU = cu; this.centerV = cv; this.angularSpan = span; this.clockwise = cw; this.fullCircle = full;
    }

    public ui.workspace.drafting.features.feature_state_ui_main getState() { return state; }
    public void setState(ui.workspace.drafting.features.feature_state_ui_main s) { this.state = (s != null) ? s : ui.workspace.drafting.features.feature_state_ui_main.CLEAN; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getSeedHoleId() { return seedHoleId; }
    public PatternType getPatternType() { return patternType; }
    public LinearDirection getDirection() { return direction; }
    public LinearDirection getLinearDirection() { return direction; }
    public void setDirection(LinearDirection d) { this.direction = d; }
    public int getCount() { return count; }
    public int getInstanceCount() { return count; }
    public void setCount(int c) { this.count = c; }
    public void setInstanceCount(int c) { this.count = c; }
    public double getSpacing() { return spacing; }
    public double getLinearSpacing() { return spacing; }
    public void setSpacing(double s) { this.spacing = s; }
    public void setLinearSpacing(double s) { this.spacing = s; }
    public double getCenterU() { return centerU; }
    public double getCircularCenterU() { return centerU; }
    public void setCenterU(double u) { this.centerU = u; }
    public double getCenterV() { return centerV; }
    public double getCircularCenterV() { return centerV; }
    public void setCenterV(double v) { this.centerV = v; }
    public double getAngularSpan() { return angularSpan; }
    public void setAngularSpan(double span) { this.angularSpan = span; }
    public boolean isClockwise() { return clockwise; }
    public void setClockwise(boolean cw) { this.clockwise = cw; }
    public boolean isFullCircle() { return fullCircle; }
    public void setFullCircle(boolean fc) { this.fullCircle = fc; }

    public record Pos2D(double u, double v) {}

    public List<Pos2D> getAllPositions(hole_feature_ui_main seed) {
        List<Pos2D> list = new ArrayList<>();
        if (seed == null || count < 1) return list;
        list.add(new Pos2D(seed.getU(), seed.getV()));
        if (patternType == PatternType.LINEAR) {
            for (int i = 1; i < count; i++) {
                double du = (direction == LinearDirection.U_DIR) ? i * spacing : 0.0;
                double dv = (direction == LinearDirection.V_DIR) ? i * spacing : 0.0;
                list.add(new Pos2D(seed.getU() + du, seed.getV() + dv));
            }
        } else {
            double du0 = seed.getU() - centerU, dv0 = seed.getV() - centerV;
            double radius = Math.hypot(du0, dv0), baseAngle = Math.atan2(dv0, du0);
            double step = (fullCircle || Math.abs(angularSpan - 360.0) < 1e-4) ? (2.0 * Math.PI / count) : (Math.toRadians(angularSpan) / (count - 1));
            double dirSign = clockwise ? -1.0 : 1.0;
            for (int i = 1; i < count; i++) {
                double a = baseAngle + dirSign * i * step;
                list.add(new Pos2D(centerU + radius * Math.cos(a), centerV + radius * Math.sin(a)));
            }
        }
        return list;
    }

    public List<hole_feature_ui_main> generateDerivedHoles(hole_feature_ui_main seed) {
        List<hole_feature_ui_main> derived = new ArrayList<>();
        if (seed == null || count < 2) return derived;
        List<Pos2D> positions = getAllPositions(seed);
        for (int i = 1; i < positions.size(); i++) {
            Pos2D pos = positions.get(i);
            derived.add(new hole_feature_ui_main(
                id + "-inst-" + i, seed.getOwnerShapeId(), seed.getHoleType(), seed.getFaceKind(),
                pos.u(), pos.v(), seed.getDiameter(), seed.getDepth(), seed.isThroughAll(),
                seed.getCsDiameter(), seed.getCsAngle(), seed.getCbDiameter(), seed.getCbDepth()
            ));
        }
        return derived;
    }

    public boolean revalidate(shape_item_ui_main host) {
        if (host == null || !host.getId().equals(ownerShapeId)) {
            state = ui.workspace.drafting.features.feature_state_ui_main.INVALID; diagnosticMessage = "Host body not found"; return false;
        }
        hole_feature_ui_main seed = null;
        for (hole_feature_ui_main h : host.getHoles()) if (h.getId().equals(seedHoleId)) { seed = h; break; }
        if (seed == null) {
            state = ui.workspace.drafting.features.feature_state_ui_main.INVALID; diagnosticMessage = "Missing seed hole: " + seedHoleId; return false;
        }
        if (seed.getState() == ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
            state = ui.workspace.drafting.features.feature_state_ui_main.INVALID; diagnosticMessage = "Seed hole is invalid"; return false;
        }
        for (Pos2D p : getAllPositions(seed)) {
            if (!topology_geometry_helper_ui_main.fitsWithinFace(host, seed.getFaceKind(), p.u(), p.v(), seed.getOuterRadius())) {
                state = ui.workspace.drafting.features.feature_state_ui_main.INVALID; diagnosticMessage = "Pattern instances exceed face boundary"; return false;
            }
        }
        if (state == ui.workspace.drafting.features.feature_state_ui_main.INVALID && diagnosticMessage != null && (diagnosticMessage.contains("boundary") || diagnosticMessage.contains("Seed"))) {
            state = ui.workspace.drafting.features.feature_state_ui_main.CLEAN; diagnosticMessage = null;
        }
        return true;
    }

    public boolean isValid(hole_feature_ui_main seed) {
        if (seed == null || !seed.isValid() || count < 2 || count > 1000) return false;
        if (patternType == PatternType.LINEAR) return spacing >= 0.1;
        double radius = Math.hypot(seed.getU() - centerU, seed.getV() - centerV);
        return radius >= 0.1 && angularSpan >= 0.1 && angularSpan <= 360.0;
    }

    public boolean fitsWithinFace(hole_feature_ui_main seed, double faceW, double faceH) {
        if (!isValid(seed) || faceW <= 0 || faceH <= 0) return false;
        double r = seed.getOuterRadius(), hw = faceW * 0.5, hh = faceH * 0.5;
        for (Pos2D p : getAllPositions(seed)) if (Math.abs(p.u()) + r > hw + 1e-4 || Math.abs(p.v()) + r > hh + 1e-4) return false;
        return true;
    }

    public boolean fitsWithinCylinderCap(hole_feature_ui_main seed, double capRadius) {
        if (!isValid(seed) || capRadius <= 0) return false;
        for (Pos2D p : getAllPositions(seed)) if (Math.hypot(p.u(), p.v()) + seed.getOuterRadius() > capRadius + 1e-4) return false;
        return true;
    }

    public boolean hasOverlappingInstances(hole_feature_ui_main seed) {
        if (seed == null || count < 2) return false;
        List<Pos2D> pts = getAllPositions(seed);
        double minClearance = 2.0 * seed.getOuterRadius() - 1e-4;
        for (int i = 0; i < pts.size(); i++) {
            for (int j = i + 1; j < pts.size(); j++) {
                if (Math.hypot(pts.get(i).u() - pts.get(j).u(), pts.get(i).v() - pts.get(j).v()) < minClearance) return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return (patternType == PatternType.LINEAR)
            ? String.format(Locale.US, "Linear Pattern (%s, Count: %d, Spacing: %.1f mm)", direction.getLabel(), count, spacing)
            : String.format(Locale.US, "Circular Pattern (Count: %d, Span: %.1f°)", count, angularSpan);
    }
}
