package ui.File_Types;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_pattern_ui_main;
import java.util.ArrayList;
import java.util.List;

public final class document_parse_helper_ui_main {
    private document_parse_helper_ui_main() {}

    public static Point3D parsePoint(String str) {
        try {
            String[] p = str.split(",");
            return new Point3D(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()), Double.parseDouble(p[2].trim()));
        } catch (Exception e) { return new Point3D(0, 0, 0); }
    }

    public static void parseHoleLine(String str, String parentId, List<hole_feature_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 7) {
                String id = p[0].trim(); face_kind_ui_main kind = face_kind_ui_main.valueOf(p[1].trim());
                double u = Double.parseDouble(p[2].trim()), v = Double.parseDouble(p[3].trim()), dia = Double.parseDouble(p[4].trim()), depth = Double.parseDouble(p[5].trim());
                boolean through = Boolean.parseBoolean(p[6].trim());
                hole_feature_ui_main.HoleType type = hole_feature_ui_main.HoleType.SIMPLE;
                double csDia = 0, csAngle = 90.0, cbDia = 0, cbDepth = 0;
                if (p.length >= 12) {
                    try { type = hole_feature_ui_main.HoleType.valueOf(p[7].trim()); } catch (Exception ignored) {}
                    csDia = Double.parseDouble(p[8].trim()); csAngle = Double.parseDouble(p[9].trim());
                    cbDia = Double.parseDouble(p[10].trim()); cbDepth = Double.parseDouble(p[11].trim());
                }
                hole_feature_ui_main.CutoutShape cShape = hole_feature_ui_main.CutoutShape.CIRCLE; double w2 = 0.0; String hName = null;
                if (p.length >= 14) { try { cShape = hole_feature_ui_main.CutoutShape.valueOf(p[12].trim()); } catch (Exception ignored) {} try { w2 = Double.parseDouble(p[13].trim()); } catch (Exception ignored) {} }
                if (p.length >= 15) try { hName = java.net.URLDecoder.decode(p[14].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception e) { hName = p[14].trim(); }
                out.add(new hole_feature_ui_main(id, parentId, hName, type, kind, u, v, dia, depth, through, csDia, csAngle, cbDia, cbDepth, cShape, w2));
            }
        } catch (Exception ignored) {}
    }

    public static void parsePatternLine(String str, String parentId, List<hole_pattern_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 12) {
                String id = p[0].trim(), ownerId = p[1].trim().isEmpty() ? parentId : p[1].trim(), seedId = p[2].trim();
                hole_pattern_ui_main.PatternType pType = hole_pattern_ui_main.PatternType.valueOf(p[3].trim());
                int count = Integer.parseInt(p[4].trim());
                hole_pattern_ui_main.LinearDirection dir = ("ALONG_V".equalsIgnoreCase(p[5].trim()) || "V_DIR".equalsIgnoreCase(p[5].trim())) ? hole_pattern_ui_main.LinearDirection.V_DIR : hole_pattern_ui_main.LinearDirection.U_DIR;
                double spacing = Double.parseDouble(p[6].trim()), cU = Double.parseDouble(p[7].trim()), cV = Double.parseDouble(p[8].trim()), span = Double.parseDouble(p[9].trim());
                boolean cw = Boolean.parseBoolean(p[10].trim()), full = Boolean.parseBoolean(p[11].trim());
                if (count >= 2 && !seedId.contains("-inst-") && !seedId.equals(id)) out.add(new hole_pattern_ui_main(id, ownerId, seedId, pType, count, dir, spacing, cU, cV, span, cw, full));
            }
        } catch (Exception ignored) {}
    }

    public static void parseExtrudeLine(String str, String parentId, List<ui.workspace.drafting.extrude.extrude_feature_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 8) {
                String id = p[0].trim(); face_kind_ui_main kind = face_kind_ui_main.valueOf(p[1].trim());
                hole_feature_ui_main.CutoutShape shape = hole_feature_ui_main.CutoutShape.valueOf(p[2].trim());
                double u = Double.parseDouble(p[3].trim()), v = Double.parseDouble(p[4].trim()), dia = Double.parseDouble(p[5].trim()), w2 = Double.parseDouble(p[6].trim()), h = Double.parseDouble(p[7].trim());
                String name = null;
                if (p.length >= 9) try { name = java.net.URLDecoder.decode(p[8].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception e) { name = p[8].trim(); }
                var ext = new ui.workspace.drafting.extrude.extrude_feature_ui_main(id, parentId, name, kind, shape, u, v, dia, w2, h);
                if (p.length >= 10 && !p[9].trim().isEmpty() && !p[9].trim().equalsIgnoreCase("NONE")) ext.setSketchId(p[9].trim());
                out.add(ext);
            }
        } catch (Exception ignored) {}
    }

    public static void parseChamferLine(String str, String parentId, List<ui.workspace.drafting.machining.chamfer_feature_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 3) {
                String id = p[0].trim(), edgeId = p[1].trim(), name = null; double dist = Double.parseDouble(p[2].trim());
                if (p.length >= 4) try { name = java.net.URLDecoder.decode(p[3].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) {}
                out.add(new ui.workspace.drafting.machining.chamfer_feature_ui_main(id, parentId, name, edgeId, dist));
            }
        } catch (Exception ignored) {}
    }

    public static void parseFilletLine(String str, String parentId, List<ui.workspace.drafting.machining.fillet_feature_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 3) {
                String id = p[0].trim(), edgeId = p[1].trim(), name = null; double rad = Double.parseDouble(p[2].trim());
                if (p.length >= 4) try { name = java.net.URLDecoder.decode(p[3].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) {}
                out.add(new ui.workspace.drafting.machining.fillet_feature_ui_main(id, parentId, name, edgeId, rad));
            }
        } catch (Exception ignored) {}
    }

    public static void parseDraftLine(String str, String parentId, List<ui.workspace.drafting.machining.draft_feature_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 3) {
                String id = p[0].trim(); face_kind_ui_main kind = face_kind_ui_main.valueOf(p[1].trim());
                double angle = Double.parseDouble(p[2].trim()); face_kind_ui_main neutral = null; String name = null;
                if (p.length >= 4 && !p[3].trim().equalsIgnoreCase("NONE")) try { neutral = face_kind_ui_main.valueOf(p[3].trim()); } catch (Exception ignored) {}
                if (p.length >= 5) try { name = java.net.URLDecoder.decode(p[4].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) {}
                out.add(new ui.workspace.drafting.machining.draft_feature_ui_main(id, parentId, name, kind, angle, neutral));
            }
        } catch (Exception ignored) {}
    }

    public static void parseBooleanLine(String str, String parentId, List<ui.workspace.drafting.booleans.boolean_feature_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 4) {
                String id = p[0].trim();
                ui.workspace.drafting.booleans.boolean_op_type_ui_main op = ui.workspace.drafting.booleans.boolean_op_type_ui_main.valueOf(p[1].trim());
                String targetId = p[2].trim().isEmpty() ? parentId : p[2].trim(), toolId = p[3].trim(), name = null;
                if (p.length >= 5) try { name = java.net.URLDecoder.decode(p[4].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) {}
                out.add(new ui.workspace.drafting.booleans.boolean_feature_ui_main(id, targetId, toolId, op, name));
            }
        } catch (Exception ignored) {}
    }

    public static void parseShellLine(String str, String parentId, List<ui.workspace.drafting.shell.shell_feature_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 4) {
                String id = p[0].trim();
                double t = Double.parseDouble(p[1].trim());
                ui.workspace.drafting.shell.shell_direction_ui_main dir = ui.workspace.drafting.shell.shell_direction_ui_main.valueOf(p[2].trim());
                List<face_kind_ui_main> faces = new ArrayList<>();
                if (!"NONE".equalsIgnoreCase(p[3].trim()) && !p[3].trim().isEmpty()) {
                    for (String fs : p[3].trim().split("[+;]")) {
                        try { faces.add(face_kind_ui_main.valueOf(fs.trim())); } catch (Exception ignored) {}
                    }
                }
                String name = null;
                if (p.length >= 5) try { name = java.net.URLDecoder.decode(p[4].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) {}
                out.add(new ui.workspace.drafting.shell.shell_feature_ui_main(id, parentId, name, t, dir, faces));
            }
        } catch (Exception ignored) {}
    }

    public static void commitShape(List<ui.workspace.drafting.shape_item_ui_main> list, ui.workspace.shapes.basic_shapes_ui_main type, String id, String name,
                                   Point3D p1, Point3D p2, double tx, double ty, double tz, double rotX, double rotY,
                                   Point3D uAxis, Point3D vAxis, Point3D norm, String faceOwner, face_kind_ui_main faceKind,
                                   boolean consumed, String consumedBy,
                                   List<hole_feature_ui_main> holes, List<hole_pattern_ui_main> patterns,
                                   List<ui.workspace.drafting.extrude.extrude_feature_ui_main> extrusions,
                                   List<ui.workspace.drafting.sketch.sketch_feature_ui_main> sketches,
                                   List<ui.workspace.drafting.machining.chamfer_feature_ui_main> chamfers,
                                   List<ui.workspace.drafting.machining.fillet_feature_ui_main> fillets,
                                   List<ui.workspace.drafting.machining.draft_feature_ui_main> drafts,
                                   List<ui.workspace.drafting.booleans.boolean_feature_ui_main> booleans,
                                   List<ui.workspace.drafting.shell.shell_feature_ui_main> shells) {
        if (type == null || p1 == null || p2 == null) return;
        var item = new ui.workspace.drafting.shape_item_ui_main(id, name, type, p1, p2, tx, ty, tz, rotX, rotY);
        if (uAxis != null) item.setFacePlane(uAxis, vAxis, norm, faceOwner, faceKind);
        item.setConsumed(consumed);
        if (consumedBy != null) item.setConsumedBy(consumedBy);
        for (hole_feature_ui_main h : holes) item.addHole(h);
        for (hole_pattern_ui_main p : patterns) {
            hole_feature_ui_main seed = null;
            for (hole_feature_ui_main h : item.getHoles()) if (h.getId().equals(p.getSeedHoleId())) { seed = h; break; }
            if (seed == null || seed.getId().contains("-inst-") || !seed.getOwnerShapeId().equals(item.getId())) continue;
            boolean matchesPat = false;
            for (hole_pattern_ui_main ep : patterns) if (ep.getId().equals(seed.getId())) { matchesPat = true; break; }
            if (!matchesPat) item.addPattern(p);
        }
        for (var ext : extrusions) item.addExtrude(ext);
        for (var sk : sketches) item.addSketch(sk);
        if (chamfers != null) for (var c : chamfers) item.addChamfer(c);
        if (fillets != null) for (var f : fillets) item.addFillet(f);
        if (drafts != null) for (var d : drafts) item.addDraft(d);
        if (booleans != null) for (var b : booleans) item.addBoolean(b);
        if (shells != null) for (var sh : shells) item.addShell(sh);
        list.add(item);
    }
}
