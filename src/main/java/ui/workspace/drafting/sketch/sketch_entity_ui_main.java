package ui.workspace.drafting.sketch;

import java.util.List;

public abstract class sketch_entity_ui_main {

    protected final String id;
    protected final sketch_geom_type_ui_main type;
    protected boolean selected = false;
    protected boolean construction = false;

    protected sketch_entity_ui_main(String id, sketch_geom_type_ui_main type) {
        this.id = id;
        this.type = type;
    }

    public String getId() { return id; }
    public sketch_geom_type_ui_main getType() { return type; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean sel) { this.selected = sel; }
    public boolean isConstruction() { return construction; }
    public void setConstruction(boolean c) { this.construction = c; }

    public abstract List<sketch_point_2d_ui_main> getSnapPoints();
    public abstract boolean containsPoint(sketch_point_2d_ui_main pt, double tolerance);
    public abstract void translate(double dx, double dy);
    public abstract sketch_entity_ui_main copy();
    public abstract boolean isValid();
}
