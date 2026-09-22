package ui.workspace;

import javafx.geometry.Point3D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * documentserializer_ui_main.java
 * Serializes and deserializes CAD profile shapes to and from Astra .nd model files.
 */
public final class documentserializer_ui_main {

    private documentserializer_ui_main() {}

    public static boolean saveToFile(File file, List<shapeitem_ui_main> shapes) {
        if (file == null) return false;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# Astra Multi-Physics Model (.nd)");
            pw.println("# Format Version: 1.0");
            pw.println();
            if (shapes != null) {
                for (shapeitem_ui_main s : shapes) {
                    pw.println("SHAPE: " + s.getType().name());
                    Point3D p1 = s.getP1();
                    Point3D p2 = s.getP2();
                    pw.printf(java.util.Locale.US, "p1: %.4f, %.4f, %.4f%n", p1.getX(), p1.getY(), p1.getZ());
                    pw.printf(java.util.Locale.US, "p2: %.4f, %.4f, %.4f%n", p2.getX(), p2.getY(), p2.getZ());
                    pw.println();
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("[Astra] Error saving model to " + file.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }

    public static List<shapeitem_ui_main> loadFromFile(File file) {
        List<shapeitem_ui_main> list = new ArrayList<>();
        if (file == null || !file.exists() || file.length() == 0) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            basicshapes_ui_main currentType = null;
            Point3D p1 = null;
            Point3D p2 = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("SHAPE:")) {
                    if (currentType != null && p1 != null && p2 != null) {
                        list.add(new shapeitem_ui_main(currentType, p1, p2));
                    }
                    String typeStr = line.substring(6).trim();
                    try {
                        currentType = basicshapes_ui_main.valueOf(typeStr);
                    } catch (Exception ex) {
                        currentType = null;
                    }
                    p1 = null;
                    p2 = null;
                } else if (line.startsWith("p1:")) {
                    p1 = parsePoint(line.substring(3).trim());
                } else if (line.startsWith("p2:")) {
                    p2 = parsePoint(line.substring(3).trim());
                }
            }
            if (currentType != null && p1 != null && p2 != null) {
                list.add(new shapeitem_ui_main(currentType, p1, p2));
            }
        } catch (Exception e) {
            System.err.println("[Astra] Error loading model from " + file.getAbsolutePath() + ": " + e.getMessage());
        }
        return list;
    }

    public static List<shapeitem_ui_main> cloneShapes(List<shapeitem_ui_main> source) {
        List<shapeitem_ui_main> copy = new ArrayList<>();
        if (source != null) {
            for (shapeitem_ui_main s : source) {
                copy.add(new shapeitem_ui_main(s.getType(), s.getP1(), s.getP2()));
            }
        }
        return copy;
    }

    private static Point3D parsePoint(String str) {
        try {
            String[] parts = str.split(",");
            if (parts.length >= 3) {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                return new Point3D(x, y, z);
            }
        } catch (Exception ignored) {}
        return new Point3D(0, 0, 0);
    }
}
