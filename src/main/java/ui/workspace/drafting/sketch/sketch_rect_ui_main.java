package ui.workspace.drafting.sketch;

import java.util.List;
import java.util.Locale;

public class sketch_rect_ui_main extends sketch_entity_ui_main {

    private sketch_point_2d_ui_main p1;
    private sketch_point_2d_ui_main p2;

    public sketch_rect_ui_main(String id, sketch_point_2d_ui_main p1, sketch_point_2d_ui_main p2) {
        super(id, sketch_geom_type_ui_main.RECTANGLE);
        this.p1 = (p1 != null) ? p1 : sketch_point_2d_ui_main.ZERO;
        this.p2 = (p2 != null) ? p2 : sketch_point_2d_ui_main.ZERO;
    }

    public sketch_rect_ui_main(String id, double x1, double y1, double x2, double y2) {
        this(id, new sketch_point_2d_ui_main(x1, y1), new sketch_point_2d_ui_main(x2, y2));
    }

    public sketch_point_2d_ui_main getP1() { return p1; }
    public void setP1(sketch_point_2d_ui_main p) { if (p != null) this.p1 = p; }
    public sketch_point_2d_ui_main getP2() { return p2; }
    public void setP2(sketch_point_2d_ui_main p) { if (p != null) this.p2 = p; }

    public double getMinX() { return Math.min(p1.x(), p2.x()); }
    public double getMaxX() { return Math.max(p1.x(), p2.x()); }
    public double getMinY() { return Math.min(p1.y(), p2.y()); }
    public double getMaxY() { return Math.max(p1.y(), p2.y()); }

    public double getWidth()  { return Math.abs(p2.x() - p1.x()); }
    public double getHeight() { return Math.abs(p2.y() - p1.y()); }

    public sketch_point_2d_ui_main getCornerTopLeft()     { return new sketch_point_2d_ui_main(getMinX(), getMinY()); }
    public sketch_point_2d_ui_main getCornerTopRight()    { return new sketch_point_2d_ui_main(getMaxX(), getMinY()); }
    public sketch_point_2d_ui_main getCornerBottomRight() { return new sketch_point_2d_ui_main(getMaxX(), getMaxY()); }
    public sketch_point_2d_ui_main getCornerBottomLeft()  { return new sketch_point_2d_ui_main(getMinX(), getMaxY()); }
    public sketch_point_2d_ui_main getCenter() { return new sketch_point_2d_ui_main((p1.x() + p2.x()) * 0.5, (p1.y() + p2.y()) * 0.5); }

    public List<sketch_line_ui_main> toLines() {
        sketch_point_2d_ui_main tl = getCornerTopLeft();
        sketch_point_2d_ui_main tr = getCornerTopRight();
        sketch_point_2d_ui_main br = getCornerBottomRight();
        sketch_point_2d_ui_main bl = getCornerBottomLeft();
        return List.of(
            new sketch_line_ui_main(id + ":T", tl, tr),
            new sketch_line_ui_main(id + ":R", tr, br),
            new sketch_line_ui_main(id + ":B", br, bl),
            new sketch_line_ui_main(id + ":L", bl, tl)
        );
    }

    @Override
    public List<sketch_point_2d_ui_main> getSnapPoints() {
        sketch_point_2d_ui_main tl = getCornerTopLeft();
        sketch_point_2d_ui_main tr = getCornerTopRight();
        sketch_point_2d_ui_main br = getCornerBottomRight();
        sketch_point_2d_ui_main bl = getCornerBottomLeft();
        return List.of(
            tl, tr, br, bl, getCenter(),
            tl.midpoint(tr), tr.midpoint(br), br.midpoint(bl), bl.midpoint(tl)
        );
    }

    @Override
    public boolean containsPoint(sketch_point_2d_ui_main pt, double tolerance) {
        if (pt == null) return false;
        for (sketch_line_ui_main line : toLines()) {
            if (line.containsPoint(pt, tolerance)) return true;
        }
        return false;
    }

    public boolean isInside(sketch_point_2d_ui_main pt) {
        if (pt == null) return false;
        return pt.x() >= getMinX() && pt.x() <= getMaxX() &&
               pt.y() >= getMinY() && pt.y() <= getMaxY();
    }

    @Override
    public void translate(double dx, double dy) {
        p1 = p1.add(dx, dy);
        p2 = p2.add(dx, dy);
    }

    @Override
    public sketch_rect_ui_main copy() {
        sketch_rect_ui_main copy = new sketch_rect_ui_main(id, p1, p2);
        copy.setSelected(selected);
        copy.setConstruction(construction);
        return copy;
    }

    @Override
    public boolean isValid() {
        return !Double.isNaN(p1.x()) && !Double.isNaN(p1.y()) &&
               !Double.isNaN(p2.x()) && !Double.isNaN(p2.y()) &&
               getWidth() > 1e-4 && getHeight() > 1e-4;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Rect[%s: (%.2f,%.2f)->(%.2f,%.2f), W=%.2f, H=%.2f]",
                id, p1.x(), p1.y(), p2.x(), p2.y(), getWidth(), getHeight());
    }
}
