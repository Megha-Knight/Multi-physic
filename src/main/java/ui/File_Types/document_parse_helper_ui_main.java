package ui.File_Types;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_pattern_ui_main;
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
                String id = p[0].trim();
                face_kind_ui_main kind = face_kind_ui_main.valueOf(p[1].trim());
                double u = Double.parseDouble(p[2].trim()), v = Double.parseDouble(p[3].trim());
                double dia = Double.parseDouble(p[4].trim()), depth = Double.parseDouble(p[5].trim());
                boolean through = Boolean.parseBoolean(p[6].trim());
                hole_feature_ui_main.HoleType type = hole_feature_ui_main.HoleType.SIMPLE;
                double csDia = 0, csAngle = 90.0, cbDia = 0, cbDepth = 0;
                if (p.length >= 12) {
                    try { type = hole_feature_ui_main.HoleType.valueOf(p[7].trim()); } catch (Exception ignored) {}
                    csDia = Double.parseDouble(p[8].trim());
                    csAngle = Double.parseDouble(p[9].trim());
                    cbDia = Double.parseDouble(p[10].trim());
                    cbDepth = Double.parseDouble(p[11].trim());
                }
                hole_feature_ui_main.CutoutShape cShape = hole_feature_ui_main.CutoutShape.CIRCLE;
                double w2 = 0.0;
                String hName = null;
                if (p.length >= 14) {
                    try { cShape = hole_feature_ui_main.CutoutShape.valueOf(p[12].trim()); } catch (Exception ignored) {}
                    try { w2 = Double.parseDouble(p[13].trim()); } catch (Exception ignored) {}
                }
                if (p.length >= 15) {
                    try { hName = java.net.URLDecoder.decode(p[14].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception e) { hName = p[14].trim(); }
                }
                out.add(new hole_feature_ui_main(
                    id, parentId, hName, type, kind, u, v, dia, depth, through, csDia, csAngle, cbDia, cbDepth, cShape, w2
                ));
            }
        } catch (Exception ignored) {}
    }

    public static void parsePatternLine(String str, String parentId, List<hole_pattern_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 12) {
                String id = p[0].trim();
                String ownerId = p[1].trim().isEmpty() ? parentId : p[1].trim();
                String seedId = p[2].trim();
                hole_pattern_ui_main.PatternType pType = hole_pattern_ui_main.PatternType.valueOf(p[3].trim());
                int count = Integer.parseInt(p[4].trim());
                hole_pattern_ui_main.LinearDirection dir = hole_pattern_ui_main.LinearDirection.valueOf(p[5].trim());
                double spacing = Double.parseDouble(p[6].trim());
                double cU = Double.parseDouble(p[7].trim()), cV = Double.parseDouble(p[8].trim());
                double span = Double.parseDouble(p[9].trim());
                boolean cw = Boolean.parseBoolean(p[10].trim());
                boolean full = Boolean.parseBoolean(p[11].trim());
                if (count >= 2 && !seedId.contains("-inst-") && !seedId.equals(id)) {
                    out.add(new hole_pattern_ui_main(id, ownerId, seedId, pType, count, dir, spacing, cU, cV, span, cw, full));
                }
            }
        } catch (Exception ignored) {}
    }

    public static void parseExtrudeLine(String str, String parentId, List<ui.workspace.drafting.extrude.extrude_feature_ui_main> out) {
        try {
            String[] p = str.split(",");
            if (p.length >= 8) {
                String id = p[0].trim();
                face_kind_ui_main kind = face_kind_ui_main.valueOf(p[1].trim());
                hole_feature_ui_main.CutoutShape shape = hole_feature_ui_main.CutoutShape.valueOf(p[2].trim());
                double u = Double.parseDouble(p[3].trim()), v = Double.parseDouble(p[4].trim());
                double dia = Double.parseDouble(p[5].trim()), w2 = Double.parseDouble(p[6].trim());
                double h = Double.parseDouble(p[7].trim());
                String name = null;
                if (p.length >= 9) {
                    try { name = java.net.URLDecoder.decode(p[8].trim(), java.nio.charset.StandardCharsets.UTF_8); } catch (Exception e) { name = p[8].trim(); }
                }
                out.add(new ui.workspace.drafting.extrude.extrude_feature_ui_main(id, parentId, name, kind, shape, u, v, dia, w2, h));
            }
        } catch (Exception ignored) {}
    }
}
