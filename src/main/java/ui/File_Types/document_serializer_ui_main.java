package ui.File_Types;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_pattern_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class document_serializer_ui_main {

    private document_serializer_ui_main() {}

    public static boolean saveToFile(File file, List<shape_item_ui_main> shapes) {
        if (file == null) return false;
        String n = file.getName().toLowerCase();
        if (n.endsWith(".step") || n.endsWith(".stp")) return step_exporter_ui_main.exportToStep(file, shapes);
        if (n.endsWith(".stl")) return mesh_exporter_ui_main.exportToStl(file, shapes);
        if (n.endsWith(".obj")) return mesh_exporter_ui_main.exportToObj(file, shapes);
        if (n.endsWith(".nc")) return nc_exporter_ui_main.exportToNc(file, shapes);
        return saveToNd(file, shapes);
    }

    public static boolean saveToNd(File file, List<shape_item_ui_main> shapes) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# Multiphysics Model (.nd)\n# Format Version: 1.8\n");
            if (shapes != null) {
                for (shape_item_ui_main s : shapes) {
                    pw.println("SHAPE: " + s.getType().name());
                    pw.println("id: " + s.getId()); pw.println("name: " + s.getName());
                    Point3D p1 = s.getP1(), p2 = s.getP2();
                    pw.printf(java.util.Locale.US, "p1: %.4f, %.4f, %.4f%np2: %.4f, %.4f, %.4f%n", p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ());
                    if (s.getWorldX() != 0 || s.getWorldY() != 0 || s.getWorldZ() != 0) pw.printf(java.util.Locale.US, "tx: %.4f, %.4f, %.4f%n", s.getWorldX(), s.getWorldY(), s.getWorldZ());
                    if (s.getRotationY() != 0) pw.printf(java.util.Locale.US, "rot: %.4f%n", s.getRotationY());
                    if (s.getRotationX() != 0) pw.printf(java.util.Locale.US, "rotX: %.4f%n", s.getRotationX());
                    if (s.isConsumed()) pw.println("consumed: true," + (s.getConsumedBy() != null ? s.getConsumedBy() : "NONE"));
                    if (s.isOnFace()) {
                        Point3D u = s.getUAxis(), v = s.getVAxis(), n = s.getFaceNormal();
                        pw.printf(java.util.Locale.US, "uAxis: %.4f, %.4f, %.4f%nvAxis: %.4f, %.4f, %.4f%nnorm: %.4f, %.4f, %.4f%n", u.getX(), u.getY(), u.getZ(), v.getX(), v.getY(), v.getZ(), n.getX(), n.getY(), n.getZ());
                        if (s.getFaceOwnerId() != null) pw.println("faceOwner: " + s.getFaceOwnerId());
                        if (s.getFaceKind() != null) pw.println("faceKind: " + s.getFaceKind().name());
                    }
                    for (hole_feature_ui_main h : s.getHoles()) {
                        pw.printf(java.util.Locale.US, "hole: %s, %s, %.4f, %.4f, %.4f, %.4f, %b, %s, %.4f, %.4f, %.4f, %.4f, %s, %.4f, %s%n",
                            h.getId(), h.getFaceKind().name(), h.getU(), h.getV(), h.getDiameter(), h.getDepth(), h.isThroughAll(),
                            h.getHoleType().name(), h.getCsDiameter(), h.getCsAngle(), h.getCbDiameter(), h.getCbDepth(), h.getCutoutShape().name(), h.getWidth2(), encode(h.getName()));
                    }
                    for (var ext : s.getExtrusions()) pw.printf(java.util.Locale.US, "extrude: %s, %s, %s, %.4f, %.4f, %.4f, %.4f, %.4f, %s, %s%n", ext.getId(), ext.getFaceKind().name(), ext.getProfileShape().name(), ext.getU(), ext.getV(), ext.getDiameter(), ext.getWidth2(), ext.getHeight(), encode(ext.getName()), ext.getSketchId() != null ? ext.getSketchId() : "NONE");
                    for (hole_pattern_ui_main p : s.getPatterns()) pw.printf(java.util.Locale.US, "pattern: %s, %s, %s, %s, %d, %s, %.4f, %.4f, %.4f, %.4f, %b, %b%n", p.getId(), p.getOwnerShapeId(), p.getSeedHoleId(), p.getPatternType().name(), p.getInstanceCount(), p.getLinearDirection().name(), p.getLinearSpacing(), p.getCircularCenterU(), p.getCircularCenterV(), p.getAngularSpan(), p.isClockwise(), p.isFullCircle());
                    for (var c : s.getChamfers()) pw.printf(java.util.Locale.US, "chamfer: %s, %s, %.4f, %s%n", c.getId(), c.getEdgeId(), c.getDistance(), encode(c.getName()));
                    for (var f : s.getFillets()) pw.printf(java.util.Locale.US, "fillet: %s, %s, %.4f, %s%n", f.getId(), f.getEdgeId(), f.getRadius(), encode(f.getName()));
                    for (var d : s.getDrafts()) pw.printf(java.util.Locale.US, "draft: %s, %s, %.4f, %s, %s%n", d.getId(), d.getFaceKind().name(), d.getDraftAngle(), d.getNeutralKind() != null ? d.getNeutralKind().name() : "NONE", encode(d.getName()));
                    for (var b : s.getBooleans()) pw.printf(java.util.Locale.US, "boolean: %s, %s, %s, %s, %s%n", b.getId(), b.getOpType().name(), b.getTargetBodyId(), b.getToolBodyId(), encode(b.getName()));
                    for (var sh : s.getShells()) {
                        String rf = String.join("+", sh.getRemovedFaces().stream().map(Enum::name).toList());
                        pw.printf(java.util.Locale.US, "shell: %s, %.4f, %s, %s, %s%n", sh.getId(), sh.getThickness(), sh.getDirection().name(), rf.isEmpty() ? "NONE" : rf, encode(sh.getName()));
                    }
                    for (var lf : s.getLofts()) pw.printf(java.util.Locale.US, "loft: %s, %b, %s%n", lf.getId(), lf.isSolid(), encode(lf.getName()));
                    for (var sw : s.getSweeps()) pw.printf(java.util.Locale.US, "sweep: %s, %s, %b, %s%n", sw.getId(), sw.getOrientation().name(), sw.isSolid(), encode(sw.getName()));
                    for (var rv : s.getRevolves()) {
                        var ax = rv.getAxis(); Point3D o = ax != null ? ax.getOrigin() : Point3D.ZERO, d = ax != null ? ax.getDirection() : new Point3D(0, 1, 0);
                        String axT = ax != null ? ax.getType().name() : "GLOBAL_Y";
                        pw.printf(java.util.Locale.US, "revolve: %s, %.4f, %s, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %s, %b, %s%n", rv.getId(), rv.getAngle(), axT, o.getX(), o.getY(), o.getZ(), d.getX(), d.getY(), d.getZ(), rv.getDirection().name(), rv.isSolid(), encode(rv.getName()));
                    }
                    for (var sk : s.getSketches()) ui.workspace.drafting.sketch.sketch_serializer_ui_main.writeSketch(pw, sk);
                    for (var e : ui.workspace.drafting.topology.shape_face_appearance_helper_ui_main.getOverridesForShape(s.getId()).entrySet()) pw.println("face_app: " + e.getKey() + "," + e.getValue().formatNd());
                    pw.println();
                }
            }
            return true;
        } catch (Exception e) { return false; }
    }

    private static String encode(String str) {
        if (str == null) return "";
        try { return java.net.URLEncoder.encode(str, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) { return ""; }
    }

    public static List<shape_item_ui_main> loadFromFile(File file) {
        List<shape_item_ui_main> list = new ArrayList<>();
        if (file == null || !file.exists() || file.length() == 0) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line, id = null, name = null, faceOwner = null, consumedBy = null;
            boolean consumed = false; face_kind_ui_main faceKind = null; basic_shapes_ui_main currentType = null;
            Point3D p1 = null, p2 = null, uAxis = null, vAxis = null, norm = null;
            double tx = 0, ty = 0, tz = 0, rotY = 0, rotX = 0;
            List<hole_feature_ui_main> pendingHoles = new ArrayList<>();
            List<hole_pattern_ui_main> pendingPatterns = new ArrayList<>();
            List<ui.workspace.drafting.extrude.extrude_feature_ui_main> pendingExtrusions = new ArrayList<>();
            List<ui.workspace.drafting.sketch.sketch_feature_ui_main> pendingSketches = new ArrayList<>();
            List<ui.workspace.drafting.machining.chamfer_feature_ui_main> pendingChamfers = new ArrayList<>();
            List<ui.workspace.drafting.machining.fillet_feature_ui_main> pendingFillets = new ArrayList<>();
            List<ui.workspace.drafting.machining.draft_feature_ui_main> pendingDrafts = new ArrayList<>();
            List<ui.workspace.drafting.booleans.boolean_feature_ui_main> pendingBooleans = new ArrayList<>();
            List<ui.workspace.drafting.shell.shell_feature_ui_main> pendingShells = new ArrayList<>();
            List<ui.workspace.drafting.loft.loft_feature_ui_main> pendingLofts = new ArrayList<>();
            List<ui.workspace.drafting.sweep.sweep_feature_ui_main> pendingSweeps = new ArrayList<>();
            List<ui.workspace.drafting.revolve.revolve_feature_ui_main> pendingRevolves = new ArrayList<>();
            ui.workspace.drafting.sketch.sketch_feature_ui_main currentSketch = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("(") && line.endsWith(")")) line = line.substring(1, line.length() - 1).trim();

                if (line.startsWith("SHAPE:")) {
                    if (currentSketch != null) { pendingSketches.add(currentSketch); currentSketch = null; }
                    document_parse_helper_ui_main.commitShape(list, currentType, id, name, p1, p2, tx, ty, tz, rotX, rotY, uAxis, vAxis, norm, faceOwner, faceKind, consumed, consumedBy, pendingHoles, pendingPatterns, pendingExtrusions, pendingSketches, pendingChamfers, pendingFillets, pendingDrafts, pendingBooleans, pendingShells, pendingLofts, pendingSweeps, pendingRevolves);
                    try { currentType = basic_shapes_ui_main.valueOf(line.substring(6).trim()); } catch (Exception ex) { currentType = null; }
                    id = null; name = null; p1 = null; p2 = null; tx = 0; ty = 0; tz = 0; rotY = 0; rotX = 0; consumed = false; consumedBy = null;
                    uAxis = null; vAxis = null; norm = null; faceOwner = null; faceKind = null;
                    pendingHoles.clear(); pendingPatterns.clear(); pendingExtrusions.clear(); pendingSketches.clear(); pendingChamfers.clear(); pendingFillets.clear(); pendingDrafts.clear(); pendingBooleans.clear(); pendingShells.clear(); pendingLofts.clear(); pendingSweeps.clear(); pendingRevolves.clear();
                } else if (line.startsWith("id:")) id = line.substring(3).trim();
                else if (line.startsWith("name:")) name = line.substring(5).trim();
                else if (line.startsWith("p1:")) p1 = document_parse_helper_ui_main.parsePoint(line.substring(3).trim());
                else if (line.startsWith("p2:")) p2 = document_parse_helper_ui_main.parsePoint(line.substring(3).trim());
                else if (line.startsWith("tx:")) { Point3D p = document_parse_helper_ui_main.parsePoint(line.substring(3).trim()); tx = p.getX(); ty = p.getY(); tz = p.getZ(); }
                else if (line.startsWith("rot:")) { try { rotY = Double.parseDouble(line.substring(4).trim()); } catch (Exception ignored) {} }
                else if (line.startsWith("rotX:")) { try { rotX = Double.parseDouble(line.substring(5).trim()); } catch (Exception ignored) {} }
                else if (line.startsWith("consumed:")) { String[] cp = line.substring(9).trim().split(","); consumed = Boolean.parseBoolean(cp[0].trim()); if (cp.length > 1 && !cp[1].trim().equalsIgnoreCase("NONE")) consumedBy = cp[1].trim(); }
                else if (line.startsWith("uAxis:")) uAxis = document_parse_helper_ui_main.parsePoint(line.substring(6).trim());
                else if (line.startsWith("vAxis:")) vAxis = document_parse_helper_ui_main.parsePoint(line.substring(6).trim());
                else if (line.startsWith("norm:")) norm = document_parse_helper_ui_main.parsePoint(line.substring(5).trim());
                else if (line.startsWith("faceOwner:")) faceOwner = line.substring(10).trim();
                else if (line.startsWith("faceKind:")) { try { faceKind = face_kind_ui_main.valueOf(line.substring(9).trim()); } catch (Exception ignored) {} }
                else if (line.startsWith("hole:")) document_parse_helper_ui_main.parseHoleLine(line.substring(5).trim(), id, pendingHoles);
                else if (line.startsWith("pattern:")) document_parse_helper_ui_main.parsePatternLine(line.substring(8).trim(), id, pendingPatterns);
                else if (line.startsWith("extrude:")) document_parse_helper_ui_main.parseExtrudeLine(line.substring(8).trim(), id, pendingExtrusions);
                else if (line.startsWith("chamfer:")) document_parse_helper_ui_main.parseChamferLine(line.substring(8).trim(), id, pendingChamfers);
                else if (line.startsWith("fillet:")) document_parse_helper_ui_main.parseFilletLine(line.substring(7).trim(), id, pendingFillets);
                else if (line.startsWith("draft:")) document_parse_helper_ui_main.parseDraftLine(line.substring(6).trim(), id, pendingDrafts);
                else if (line.startsWith("boolean:")) document_parse_helper_ui_main.parseBooleanLine(line.substring(8).trim(), id, pendingBooleans);
                else if (line.startsWith("shell:")) document_parse_helper_ui_main.parseShellLine(line.substring(6).trim(), id, pendingShells);
                else if (line.startsWith("loft:")) document_parse_helper_ui_main.parseLoftLine(line.substring(5).trim(), id, pendingLofts);
                else if (line.startsWith("sweep:")) document_parse_helper_ui_main.parseSweepLine(line.substring(6).trim(), id, pendingSweeps);
                else if (line.startsWith("revolve:")) document_parse_helper_ui_main.parseRevolveLine(line.substring(8).trim(), id, pendingRevolves);
                else if (line.startsWith("sketch:")) {
                    if (currentSketch != null) pendingSketches.add(currentSketch);
                    currentSketch = ui.workspace.drafting.sketch.sketch_serializer_ui_main.parseSketchHeader(line);
                } else if (line.startsWith("sgeom:")) ui.workspace.drafting.sketch.sketch_serializer_ui_main.parseGeometryLine(line, currentSketch);
                else if (line.startsWith("sconstraint:")) ui.workspace.drafting.sketch.sketch_serializer_ui_main.parseConstraintLine(line, currentSketch);
                else if (line.startsWith("face_app:")) {
                    String[] parts = line.substring(9).trim().split(",", 2);
                    if (parts.length == 2 && id != null) {
                        var app = ui.workspace.drafting.topology.topology_face_appearance_ui_main.parseNd(parts[1].trim());
                        ui.workspace.drafting.topology.shape_face_appearance_helper_ui_main.setAppearance(id, parts[0].trim(), app);
                    }
                }
            }
            if (currentSketch != null) pendingSketches.add(currentSketch);
            document_parse_helper_ui_main.commitShape(list, currentType, id, name, p1, p2, tx, ty, tz, rotX, rotY, uAxis, vAxis, norm, faceOwner, faceKind, consumed, consumedBy, pendingHoles, pendingPatterns, pendingExtrusions, pendingSketches, pendingChamfers, pendingFillets, pendingDrafts, pendingBooleans, pendingShells, pendingLofts, pendingSweeps, pendingRevolves);
        } catch (Exception ignored) {}
        return list;
    }

    public static List<shape_item_ui_main> cloneShapes(List<shape_item_ui_main> original) {
        List<shape_item_ui_main> copies = new ArrayList<>();
        if (original == null) return copies;
        for (shape_item_ui_main s : original) {
            shape_item_ui_main item = new shape_item_ui_main(s.getId(), s.getName(), s.getType(), s.getP1(), s.getP2(),
                    s.getWorldX(), s.getWorldY(), s.getWorldZ(), s.getRotationX(), s.getRotationY());
            if (s.isOnFace()) item.setFacePlane(s.getUAxis(), s.getVAxis(), s.getFaceNormal(), s.getFaceOwnerId(), s.getFaceKind());
            item.setState(s.getState()); item.setConsumed(s.isConsumed()); item.setConsumedBy(s.getConsumedBy());
            for (hole_feature_ui_main h : s.getHoles()) {
                var ch = new hole_feature_ui_main(h.getId(), h.getOwnerShapeId(), h.getName(), h.getHoleType(), h.getFaceKind(),
                    h.getU(), h.getV(), h.getDiameter(), h.getDepth(), h.isThroughAll(), h.getCsDiameter(), h.getCsAngle(),
                    h.getCbDiameter(), h.getCbDepth(), h.getCutoutShape(), h.getWidth2());
                ch.setState(h.getState()); ch.setVisible(h.isVisible()); ch.setDiagnosticMessage(h.getDiagnosticMessage());
                item.addHole(ch);
            }
            for (hole_pattern_ui_main p : s.getPatterns()) {
                var cp = new hole_pattern_ui_main(p.getId(), p.getOwnerShapeId(), p.getSeedHoleId(), p.getPatternType(), p.getInstanceCount(), p.getLinearDirection(), p.getLinearSpacing(), p.getCircularCenterU(), p.getCircularCenterV(), p.getAngularSpan(), p.isClockwise(), p.isFullCircle());
                cp.setState(p.getState()); cp.setVisible(p.isVisible()); cp.setDiagnosticMessage(p.getDiagnosticMessage());
                item.addPattern(cp);
            }
            for (ui.workspace.drafting.extrude.extrude_feature_ui_main ext : s.getExtrusions()) {
                var cext = new ui.workspace.drafting.extrude.extrude_feature_ui_main(ext.getId(), ext.getOwnerShapeId(), ext.getName(), ext.getFaceKind(), ext.getProfileShape(), ext.getU(), ext.getV(), ext.getDiameter(), ext.getWidth2(), ext.getHeight());
                cext.setSketchId(ext.getSketchId()); cext.setState(ext.getState()); cext.setVisible(ext.isVisible()); cext.setDiagnosticMessage(ext.getDiagnosticMessage());
                item.addExtrude(cext);
            }
            for (var c : s.getChamfers()) item.addChamfer(new ui.workspace.drafting.machining.chamfer_feature_ui_main(c.getId(), c.getOwnerShapeId(), c.getName(), c.getEdgeId(), c.getDistance()));
            for (var f : s.getFillets()) item.addFillet(new ui.workspace.drafting.machining.fillet_feature_ui_main(f.getId(), f.getOwnerShapeId(), f.getName(), f.getEdgeId(), f.getRadius()));
            for (var d : s.getDrafts()) item.addDraft(new ui.workspace.drafting.machining.draft_feature_ui_main(d.getId(), d.getOwnerShapeId(), d.getName(), d.getFaceKind(), d.getDraftAngle(), d.getNeutralKind()));
            for (var b : s.getBooleans()) item.addBoolean(b.copy());
            for (var sh : s.getShells()) item.addShell(sh.copy());
            for (var lf : s.getLofts()) item.addLoft(lf.copy());
            for (var sw : s.getSweeps()) item.addSweep(sw.copy());
            for (var rv : s.getRevolves()) item.addRevolve(rv.copy());
            for (var sk : s.getSketches()) item.addSketch(sk.copy());
            ui.workspace.drafting.topology.shape_face_appearance_helper_ui_main.copyOverrides(s.getId(), item.getId());
            copies.add(item);
        }
        return copies;
    }
}
