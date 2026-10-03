package ui.workspace.drafting.sketch;

import ui.workspace.drafting.faces.face_kind_ui_main;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class sketch_serializer_ui_main {

    private sketch_serializer_ui_main() {}

    public static void writeSketch(PrintWriter pw, sketch_feature_ui_main s) {
        if (pw == null || s == null) return;
        pw.printf(Locale.US, "sketch: %s, %s, %s, %s, %s%n",
            s.getId(),
            s.getOwnerShapeId() != null ? s.getOwnerShapeId() : "NONE",
            s.getName(),
            s.getPlaneType().name(),
            s.getFaceKind() != null ? s.getFaceKind().name() : "NONE"
        );

        for (sketch_entity_ui_main e : s.getEntities()) {
            if (e instanceof sketch_line_ui_main l) {
                pw.printf(Locale.US, "  sgeom: %s, LINE, %.4f, %.4f, %.4f, %.4f, %b%n",
                    l.getId(), l.getStart().x(), l.getStart().y(), l.getEnd().x(), l.getEnd().y(), l.isConstruction());
            } else if (e instanceof sketch_circle_ui_main c) {
                pw.printf(Locale.US, "  sgeom: %s, CIRCLE, %.4f, %.4f, %.4f, %b%n",
                    c.getId(), c.getCenter().x(), c.getCenter().y(), c.getRadius(), c.isConstruction());
            } else if (e instanceof sketch_arc_ui_main a) {
                pw.printf(Locale.US, "  sgeom: %s, ARC, %.4f, %.4f, %.4f, %.4f, %.4f, %b%n",
                    a.getId(), a.getCenter().x(), a.getCenter().y(), a.getRadius(), a.getStartAngle(), a.getEndAngle(), a.isConstruction());
            } else if (e instanceof sketch_rect_ui_main r) {
                pw.printf(Locale.US, "  sgeom: %s, RECTANGLE, %.4f, %.4f, %.4f, %.4f, %b%n",
                    r.getId(), r.getP1().x(), r.getP1().y(), r.getP2().x(), r.getP2().y(), r.isConstruction());
            }
        }

        for (sketch_constraint_ui_main c : s.getConstraints()) {
            String gIds = String.join(";", c.getGeometryIds());
            pw.printf(Locale.US, "  sconstraint: %s, %s, %s, %.4f, %d, %d, %b%n",
                c.getId(), c.getType().name(), gIds, c.getParameter(), c.getPointIndexA(), c.getPointIndexB(), c.isActive());
        }
    }

    public static sketch_feature_ui_main parseSketchHeader(String line) {
        try {
            String data = line.startsWith("sketch:") ? line.substring(7).trim() : line.trim();
            String[] p = data.split(",");
            if (p.length >= 4) {
                String id = p[0].trim();
                String owner = p[1].trim().equalsIgnoreCase("NONE") ? null : p[1].trim();
                String name = p[2].trim();
                sketch_plane_type_ui_main pt = sketch_plane_type_ui_main.valueOf(p[3].trim());
                face_kind_ui_main fk = (p.length >= 5 && !p[4].trim().equalsIgnoreCase("NONE"))
                        ? face_kind_ui_main.valueOf(p[4].trim()) : null;
                return new sketch_feature_ui_main(id, owner, name, pt, fk);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void parseGeometryLine(String line, sketch_feature_ui_main target) {
        if (target == null || line == null) return;
        try {
            String data = line.trim().startsWith("sgeom:") ? line.trim().substring(6).trim() : line.trim();
            String[] p = data.split(",");
            if (p.length >= 3) {
                String id = p[0].trim();
                sketch_geom_type_ui_main type = sketch_geom_type_ui_main.valueOf(p[1].trim());
                switch (type) {
                    case LINE -> {
                        double x1 = Double.parseDouble(p[2].trim()), y1 = Double.parseDouble(p[3].trim());
                        double x2 = Double.parseDouble(p[4].trim()), y2 = Double.parseDouble(p[5].trim());
                        sketch_line_ui_main l = new sketch_line_ui_main(id, x1, y1, x2, y2);
                        if (p.length >= 7) l.setConstruction(Boolean.parseBoolean(p[6].trim()));
                        target.addEntity(l);
                    }
                    case CIRCLE -> {
                        double cx = Double.parseDouble(p[2].trim()), cy = Double.parseDouble(p[3].trim());
                        double r = Double.parseDouble(p[4].trim());
                        sketch_circle_ui_main c = new sketch_circle_ui_main(id, cx, cy, r);
                        if (p.length >= 6) c.setConstruction(Boolean.parseBoolean(p[5].trim()));
                        target.addEntity(c);
                    }
                    case ARC -> {
                        double cx = Double.parseDouble(p[2].trim()), cy = Double.parseDouble(p[3].trim());
                        double r = Double.parseDouble(p[4].trim()), sa = Double.parseDouble(p[5].trim()), ea = Double.parseDouble(p[6].trim());
                        sketch_arc_ui_main a = new sketch_arc_ui_main(id, new sketch_point_2d_ui_main(cx, cy), r, sa, ea);
                        if (p.length >= 8) a.setConstruction(Boolean.parseBoolean(p[7].trim()));
                        target.addEntity(a);
                    }
                    case RECTANGLE -> {
                        double x1 = Double.parseDouble(p[2].trim()), y1 = Double.parseDouble(p[3].trim());
                        double x2 = Double.parseDouble(p[4].trim()), y2 = Double.parseDouble(p[5].trim());
                        sketch_rect_ui_main r = new sketch_rect_ui_main(id, x1, y1, x2, y2);
                        if (p.length >= 7) r.setConstruction(Boolean.parseBoolean(p[6].trim()));
                        target.addEntity(r);
                    }
                    default -> {}
                }
            }
        } catch (Exception ignored) {}
    }

    public static void parseConstraintLine(String line, sketch_feature_ui_main target) {
        if (target == null || line == null) return;
        try {
            String data = line.trim().startsWith("sconstraint:") ? line.trim().substring(12).trim() : line.trim();
            String[] p = data.split(",");
            if (p.length >= 4) {
                String id = p[0].trim();
                sketch_constraint_type_ui_main type = sketch_constraint_type_ui_main.valueOf(p[1].trim());
                List<String> gIds = new ArrayList<>(Arrays.asList(p[2].trim().split(";")));
                double param = Double.parseDouble(p[3].trim());
                sketch_constraint_ui_main c = new sketch_constraint_ui_main(id, type, gIds, param);
                if (p.length >= 6) {
                    c.setPointIndexA(Integer.parseInt(p[4].trim()));
                    c.setPointIndexB(Integer.parseInt(p[5].trim()));
                }
                if (p.length >= 7) c.setActive(Boolean.parseBoolean(p[6].trim()));
                target.addConstraint(c);
            }
        } catch (Exception ignored) {}
    }
}
