package ui.workspace.drafting.sketch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class sketch_constraint_ui_main {

    private final String id;
    private final sketch_constraint_type_ui_main type;
    private final List<String> geometryIds = new ArrayList<>();
    private int pointIndexA = -1; // 0=start/corner0, 1=end/corner1, 2=midpoint/center, -1=entire geometry/unspecified
    private int pointIndexB = -1;
    private double parameter = 0.0; // distance, radius, or angle
    private boolean active = true;

    public sketch_constraint_ui_main(String id, sketch_constraint_type_ui_main type, List<String> geomIds, double parameter) {
        this.id = id;
        this.type = (type != null) ? type : sketch_constraint_type_ui_main.COINCIDENT;
        if (geomIds != null) this.geometryIds.addAll(geomIds);
        this.parameter = parameter;
    }

    public sketch_constraint_ui_main(String id, sketch_constraint_type_ui_main type, String g1, String g2, double parameter) {
        this.id = id;
        this.type = (type != null) ? type : sketch_constraint_type_ui_main.COINCIDENT;
        if (g1 != null) this.geometryIds.add(g1);
        if (g2 != null) this.geometryIds.add(g2);
        this.parameter = parameter;
    }

    public sketch_constraint_ui_main(String id, sketch_constraint_type_ui_main type, String g1, double parameter) {
        this(id, type, g1, null, parameter);
    }

    public String getId() { return id; }
    public sketch_constraint_type_ui_main getType() { return type; }
    public List<String> getGeometryIds() { return new ArrayList<>(geometryIds); }
    public String getFirstGeometryId() { return geometryIds.isEmpty() ? null : geometryIds.get(0); }
    public String getSecondGeometryId() { return geometryIds.size() > 1 ? geometryIds.get(1) : null; }
    public int getPointIndexA() { return pointIndexA; }
    public void setPointIndexA(int idx) { this.pointIndexA = idx; }
    public int getPointIndexB() { return pointIndexB; }
    public void setPointIndexB(int idx) { this.pointIndexB = idx; }
    public double getParameter() { return parameter; }
    public void setParameter(double p) { this.parameter = p; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }

    public sketch_constraint_ui_main copy() {
        sketch_constraint_ui_main c = new sketch_constraint_ui_main(id, type, geometryIds, parameter);
        c.pointIndexA = pointIndexA;
        c.pointIndexB = pointIndexB;
        c.active = active;
        return c;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Constraint[%s: %s on %s, param=%.2f, active=%b]",
                id, type.getLabel(), geometryIds, parameter, active);
    }
}
