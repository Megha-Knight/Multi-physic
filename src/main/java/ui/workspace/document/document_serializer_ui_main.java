package ui.workspace.document;

import javafx.geometry.Point3D;
import ui.workspace.drafting.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * document_serializer_ui_main.java
 * Serializes and deserializes CAD shapes to/from Multiphysics .nd files.
 * Format Version 1.4 — preserves ID, name, 3D translation, 360° rotation, and face-based sketch planes.
 */
public final class document_serializer_ui_main {

    private document_serializer_ui_main() {}

    public static boolean saveToFile(File file, List<shape_item_ui_main> shapes) {
        if (file == null) return false;
        String name = file.getName().toLowerCase();
        if (name.endsWith(".step") || name.endsWith(".stp")) return step_exporter_ui_main.exportToStep(file, shapes);
        if (name.endsWith(".stl")) return mesh_exporter_ui_main.exportToStl(file, shapes);
        if (name.endsWith(".obj")) return mesh_exporter_ui_main.exportToObj(file, shapes);
        if (name.endsWith(".nc")) return nc_exporter_ui_main.exportToNc(file, shapes);
        return saveToNd(file, shapes);
    }

    private static boolean saveToNd(File file, List<shape_item_ui_main> shapes) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# Multiphysics Model (.nd)");
            pw.println("# Format Version: 1.4");
            pw.println();
            if (shapes != null) {
                for (shape_item_ui_main s : shapes) {
                    pw.println("SHAPE: " + s.getType().name());
                    pw.println("id: " + s.getId());
                    pw.println("name: " + s.getName());
                    Point3D p1 = s.getP1(), p2 = s.getP2();
                    pw.printf(java.util.Locale.US, "p1: %.4f, %.4f, %.4f%n", p1.getX(), p1.getY(), p1.getZ());
                    pw.printf(java.util.Locale.US, "p2: %.4f, %.4f, %.4f%n", p2.getX(), p2.getY(), p2.getZ());
                    if (s.getWorldX() != 0 || s.getWorldY() != 0 || s.getWorldZ() != 0) {
                        pw.printf(java.util.Locale.US, "tx: %.4f, %.4f, %.4f%n", s.getWorldX(), s.getWorldY(), s.getWorldZ());
                    }
                    if (s.getRotationY() != 0) pw.printf(java.util.Locale.US, "rot: %.4f%n", s.getRotationY());
                    if (s.getRotationX() != 0) pw.printf(java.util.Locale.US, "rotX: %.4f%n", s.getRotationX());
                    if (s.isOnFace()) {
                        Point3D u = s.getUAxis(), v = s.getVAxis(), n = s.getFaceNormal();
                        pw.printf(java.util.Locale.US, "uAxis: %.4f, %.4f, %.4f%n", u.getX(), u.getY(), u.getZ());
                        pw.printf(java.util.Locale.US, "vAxis: %.4f, %.4f, %.4f%n", v.getX(), v.getY(), v.getZ());
                        pw.printf(java.util.Locale.US, "norm: %.4f, %.4f, %.4f%n", n.getX(), n.getY(), n.getZ());
                        if (s.getFaceOwnerId() != null) pw.println("faceOwner: " + s.getFaceOwnerId());
                        if (s.getFaceKind() != null) pw.println("faceKind: " + s.getFaceKind().name());
                    }
                    pw.println();
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("[Multiphysics] Error saving: " + e.getMessage());
            return false;
        }
    }

    public static List<shape_item_ui_main> loadFromFile(File file) {
        List<shape_item_ui_main> list = new ArrayList<>();
        if (file == null || !file.exists() || file.length() == 0) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line, id = null, name = null, faceOwner = null;
            face_kind_ui_main faceKind = null;
            basic_shapes_ui_main currentType = null;
            Point3D p1 = null, p2 = null, uAxis = null, vAxis = null, norm = null;
            double tx = 0, ty = 0, tz = 0, rotY = 0, rotX = 0;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("(") && line.endsWith(")")) line = line.substring(1, line.length() - 1).trim();

                if (line.startsWith("SHAPE:")) {
                    if (currentType != null && p1 != null && p2 != null) {
                        shape_item_ui_main item = new shape_item_ui_main(id, name, currentType, p1, p2, tx, ty, tz, rotX, rotY);
                        if (uAxis != null) item.setFacePlane(uAxis, vAxis, norm, faceOwner, faceKind);
                        list.add(item);
                    }
                    String typeStr = line.substring(6).trim();
                    try { currentType = basic_shapes_ui_main.valueOf(typeStr); } catch (Exception ex) { currentType = null; }
                    id = null; name = null; p1 = null; p2 = null; tx = 0; ty = 0; tz = 0; rotY = 0; rotX = 0;
                    uAxis = null; vAxis = null; norm = null; faceOwner = null; faceKind = null;
                } else if (line.startsWith("id:")) id = line.substring(3).trim();
                else if (line.startsWith("name:")) name = line.substring(5).trim();
                else if (line.startsWith("p1:")) p1 = parsePoint(line.substring(3).trim());
                else if (line.startsWith("p2:")) p2 = parsePoint(line.substring(3).trim());
                else if (line.startsWith("tx:")) { Point3D p = parsePoint(line.substring(3).trim()); tx = p.getX(); ty = p.getY(); tz = p.getZ(); }
                else if (line.startsWith("rot:")) { try { rotY = Double.parseDouble(line.substring(4).trim()); } catch (Exception ignored) {} }
                else if (line.startsWith("rotX:")) { try { rotX = Double.parseDouble(line.substring(5).trim()); } catch (Exception ignored) {} }
                else if (line.startsWith("uAxis:")) uAxis = parsePoint(line.substring(6).trim());
                else if (line.startsWith("vAxis:")) vAxis = parsePoint(line.substring(6).trim());
                else if (line.startsWith("norm:")) norm = parsePoint(line.substring(5).trim());
                else if (line.startsWith("faceOwner:")) faceOwner = line.substring(10).trim();
                else if (line.startsWith("faceKind:")) { try { faceKind = face_kind_ui_main.valueOf(line.substring(9).trim()); } catch (Exception ignored) {} }
            }
            if (currentType != null && p1 != null && p2 != null) {
                shape_item_ui_main item = new shape_item_ui_main(id, name, currentType, p1, p2, tx, ty, tz, rotX, rotY);
                if (uAxis != null) item.setFacePlane(uAxis, vAxis, norm, faceOwner, faceKind);
                list.add(item);
            }
        } catch (Exception e) {
            System.err.println("[Multiphysics] Error loading: " + e.getMessage());
        }
        return list;
    }

    public static List<shape_item_ui_main> cloneShapes(List<shape_item_ui_main> source) {
        List<shape_item_ui_main> copy = new ArrayList<>();
        if (source != null) {
            for (shape_item_ui_main s : source) {
                shape_item_ui_main item = new shape_item_ui_main(s.getId(), s.getName(), s.getType(), s.getP1(), s.getP2(),
                    s.getWorldX(), s.getWorldY(), s.getWorldZ(), s.getRotationX(), s.getRotationY());
                if (s.isOnFace()) item.setFacePlane(s.getUAxis(), s.getVAxis(), s.getFaceNormal(), s.getFaceOwnerId(), s.getFaceKind());
                copy.add(item);
            }
        }
        return copy;
    }

    private static Point3D parsePoint(String str) {
        try {
            String[] parts = str.split(",");
            if (parts.length >= 3) {
                return new Point3D(Double.parseDouble(parts[0].trim()),
                                   Double.parseDouble(parts[1].trim()),
                                   Double.parseDouble(parts[2].trim()));
            }
        } catch (Exception ignored) {}
        return new Point3D(0, 0, 0);
    }
}
