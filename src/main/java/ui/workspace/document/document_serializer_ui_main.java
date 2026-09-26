package ui.workspace.document;

import javafx.geometry.Point3D;
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
 * Format Version 1.3 — preserves ID, name, 3D translation, and 360° rotation.
 */
public final class document_serializer_ui_main {

    private document_serializer_ui_main() {}

    public static boolean saveToFile(File file, List<shape_item_ui_main> shapes) {
        if (file == null) return false;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# Multiphysics Model (.nd)");
            pw.println("# Format Version: 1.3");
            pw.println();
            if (shapes != null) {
                for (shape_item_ui_main s : shapes) {
                    pw.println("SHAPE: " + s.getType().name());
                    pw.println("id: " + s.getId());
                    pw.println("name: " + s.getName());
                    Point3D p1 = s.getP1(), p2 = s.getP2();
                    pw.printf(java.util.Locale.US, "p1: %.4f, %.4f, %.4f%n", p1.getX(), p1.getY(), p1.getZ());
                    pw.printf(java.util.Locale.US, "p2: %.4f, %.4f, %.4f%n", p2.getX(), p2.getY(), p2.getZ());
                    if (s.getType().is3D() && (s.getWorldX() != 0 || s.getWorldY() != 0 || s.getWorldZ() != 0)) {
                        pw.printf(java.util.Locale.US, "tx: %.4f, %.4f, %.4f%n", s.getWorldX(), s.getWorldY(), s.getWorldZ());
                    }
                    if (s.getRotationAngle() != 0) {
                        pw.printf(java.util.Locale.US, "rot: %.4f%n", s.getRotationAngle());
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
            String line, id = null, name = null;
            basic_shapes_ui_main currentType = null;
            Point3D p1 = null, p2 = null;
            double tx = 0, ty = 0, tz = 0, rot = 0;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("SHAPE:")) {
                    if (currentType != null && p1 != null && p2 != null) {
                        list.add(new shape_item_ui_main(id, name, currentType, p1, p2, tx, ty, tz, rot));
                    }
                    String typeStr = line.substring(6).trim();
                    try { currentType = basic_shapes_ui_main.valueOf(typeStr); }
                    catch (Exception ex) { currentType = null; }
                    id = null; name = null; p1 = null; p2 = null; tx = 0; ty = 0; tz = 0; rot = 0;
                } else if (line.startsWith("id:")) {
                    id = line.substring(3).trim();
                } else if (line.startsWith("name:")) {
                    name = line.substring(5).trim();
                } else if (line.startsWith("p1:")) {
                    p1 = parsePoint(line.substring(3).trim());
                } else if (line.startsWith("p2:")) {
                    p2 = parsePoint(line.substring(3).trim());
                } else if (line.startsWith("tx:")) {
                    Point3D txPt = parsePoint(line.substring(3).trim());
                    tx = txPt.getX(); ty = txPt.getY(); tz = txPt.getZ();
                } else if (line.startsWith("rot:")) {
                    try { rot = Double.parseDouble(line.substring(4).trim()); } catch (Exception ignored) {}
                }
            }
            if (currentType != null && p1 != null && p2 != null) {
                list.add(new shape_item_ui_main(id, name, currentType, p1, p2, tx, ty, tz, rot));
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
                copy.add(new shape_item_ui_main(s.getId(), s.getName(), s.getType(), s.getP1(), s.getP2(),
                    s.getWorldX(), s.getWorldY(), s.getWorldZ(), s.getRotationAngle()));
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
