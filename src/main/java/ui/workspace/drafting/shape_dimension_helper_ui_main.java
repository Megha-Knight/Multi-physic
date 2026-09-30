package ui.workspace.drafting;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * shape_dimension_helper_ui_main.java
 * Extracts, calculates, and applies geometric dimensions and world positions for CAD shapes.
 */
public final class shape_dimension_helper_ui_main {

    private shape_dimension_helper_ui_main() {}

    public static Map<String, Double> getDimensions(shape_item_ui_main item) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (item == null) return map;

        Point3D p1 = item.getP1(), p2 = item.getP2();
        basic_shapes_ui_main type = item.getType();
        double dist = (p1 != null && p2 != null) ? p1.distance(p2) : 0.0;
        double dx = (p1 != null && p2 != null) ? Math.abs(p2.getX() - p1.getX()) : 0.0;
        double dz = (p1 != null && p2 != null) ? Math.abs(p2.getZ() - p1.getZ()) : 0.0;

        switch (type) {
            case CIRCLE -> {
                map.put("Radius (mm)", dist);
                map.put("Diameter (mm)", dist * 2.0);
            }
            case SQUARE, CUBE -> {
                double s = Math.max(dx, dz);
                map.put("Side Length (mm)", Math.max(1.0, s));
            }
            case RECTANGLE -> {
                map.put("Width (mm)", Math.max(1.0, dx));
                map.put("Height (mm)", Math.max(1.0, dz));
            }
            case EQUILATERAL_TRIANGLE -> {
                map.put("Side Length (mm)", Math.max(1.0, dist));
                map.put("Height (mm)", Math.max(1.0, dist) * Math.sqrt(3.0) / 2.0);
            }
            case RIGHT_TRIANGLE -> {
                map.put("Base (mm)", Math.max(1.0, dx));
                map.put("Height (mm)", Math.max(1.0, dz));
            }
            case CYLINDER -> {
                double r = (p1 != null && p2 != null) ? p1.distance(new Point3D(p2.getX(), 0, p2.getZ())) : 0.0;
                double h = (p2 != null && Math.abs(p2.getY()) > 0.1) ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
                map.put("Radius (mm)", r);
                map.put("Diameter (mm)", r * 2.0);
                map.put("Height (mm)", h);
            }
            case SPHERE -> {
                double r = (p1 != null && p2 != null) ? p1.distance(new Point3D(p2.getX(), 0, p2.getZ())) : 0.0;
                map.put("Radius (mm)", r);
                map.put("Diameter (mm)", r * 2.0);
            }
            case CONE -> {
                double r = (p1 != null && p2 != null) ? p1.distance(new Point3D(p2.getX(), 0, p2.getZ())) : 0.0;
                double h = (p2 != null && Math.abs(p2.getY()) > 0.1) ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
                map.put("Base Radius (mm)", r);
                map.put("Base Diameter (mm)", r * 2.0);
                map.put("Height (mm)", h);
            }
            default -> {}
        }

        // CAD Coordinates: X = X, Y = Depth (Z_javafx), Z = Height (-Y_javafx)
        double curX = item.getWorldX() + (p1 != null ? p1.getX() : 0.0);
        double curY = item.getWorldZ() + (p1 != null ? p1.getZ() : 0.0);
        double curZ = -(item.getWorldY() + (p1 != null ? p1.getY() : 0.0));
        map.put("Position X (mm)", curX);
        map.put("Position Y (mm)", curY);
        map.put("Position Z (mm)", curZ);
        map.put("Rotation Y (Left/Right °)", item.getRotationY());
        map.put("Rotation X (Top/Bottom °)", item.getRotationX());
        return map;
    }

    public static void applyDimensions(shape_item_ui_main item, Map<String, Double> vals) {
        if (item == null || vals == null) return;
        if (vals.containsKey("Rotation Y (Left/Right °)")) item.setRotationY(vals.get("Rotation Y (Left/Right °)"));
        if (vals.containsKey("Rotation X (Top/Bottom °)")) item.setRotationX(vals.get("Rotation X (Top/Bottom °)"));
        if (vals.containsKey("Rotation Angle (°)")) item.setRotationAngle(vals.get("Rotation Angle (°)"));
        basic_shapes_ui_main type = item.getType();
        Point3D p1 = item.getP1(), p2 = item.getP2();

        double curX = item.getWorldX() + (p1 != null ? p1.getX() : 0.0);
        double curY = item.getWorldZ() + (p1 != null ? p1.getZ() : 0.0);
        double curZ = -(item.getWorldY() + (p1 != null ? p1.getY() : 0.0));

        double posX = getVal(vals, "Position X", curX);
        double posY = getVal(vals, "Position Y", curY);
        double posZ = getVal(vals, "Position Z", curZ);

        Point3D np1 = p1, np2 = p2;
        if (item.isOnFace()) {
            item.applyWorldDelta(posX - curX, -(posZ - curZ), posY - curY);
            Point3D u = item.getUAxis() != null ? item.getUAxis().normalize() : new Point3D(1, 0, 0);
            Point3D v = item.getVAxis() != null ? item.getVAxis().normalize() : new Point3D(0, 0, 1);
            switch (type) {
                case CIRCLE -> {
                    double r = getRadiusVal(vals, p1.distance(p2));
                    np2 = p1.add(u.multiply(r));
                }
                case SQUARE -> {
                    double s = getVal(vals, "Side Length", Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())));
                    np2 = p1.add(u.multiply(s)).add(v.multiply(s));
                }
                case RECTANGLE -> {
                    double w = getVal(vals, "Width", Math.abs(p2.getX() - p1.getX()));
                    double h = getVal(vals, "Height", Math.abs(p2.getZ() - p1.getZ()));
                    np2 = p1.add(u.multiply(w)).add(v.multiply(h));
                }
                case EQUILATERAL_TRIANGLE -> {
                    double s = getVal(vals, "Side Length", p1.distance(p2));
                    np2 = p1.add(u.multiply(s));
                }
                case RIGHT_TRIANGLE -> {
                    double b = getVal(vals, "Base", Math.abs(p2.getX() - p1.getX()));
                    double h = getVal(vals, "Height", Math.abs(p2.getZ() - p1.getZ()));
                    np2 = p1.add(u.multiply(b)).add(v.multiply(h));
                }
                default -> {}
            }
        } else {
            item.setWorldTranslation(posX, -posZ, posY);
            switch (type) {
                case CIRCLE -> {
                    double r = getRadiusVal(vals, (p1 != null && p2 != null) ? p1.distance(p2) : 10.0);
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(r, 0, 0);
                }
                case SQUARE -> {
                    double s = getVal(vals, "Side Length", (p1 != null && p2 != null) ? Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())) : 10.0);
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(s, 0, s);
                }
                case RECTANGLE -> {
                    double w = getVal(vals, "Width", (p1 != null && p2 != null) ? Math.abs(p2.getX() - p1.getX()) : 10.0);
                    double h = getVal(vals, "Height", (p1 != null && p2 != null) ? Math.abs(p2.getZ() - p1.getZ()) : 10.0);
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(w, 0, h);
                }
                case EQUILATERAL_TRIANGLE -> {
                    double s = getVal(vals, "Side Length", (p1 != null && p2 != null) ? p1.distance(p2) : 10.0);
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(s, 0, 0);
                }
                case RIGHT_TRIANGLE -> {
                    double b = getVal(vals, "Base", (p1 != null && p2 != null) ? Math.abs(p2.getX() - p1.getX()) : 10.0);
                    double h = getVal(vals, "Height", (p1 != null && p2 != null) ? Math.abs(p2.getZ() - p1.getZ()) : 10.0);
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(b, 0, h);
                }
                case CUBE -> {
                    double s = getVal(vals, "Side Length", (p1 != null && p2 != null) ? Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())) : 10.0);
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(s, 0, s);
                }
                case CYLINDER -> {
                    double r = getRadiusVal(vals, (p1 != null && p2 != null) ? p1.distance(new Point3D(p2.getX(), 0, p2.getZ())) : 10.0);
                    double h = getVal(vals, "Height", Math.max(6.0, r * 2.0));
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(r, -h, 0);
                }
                case SPHERE -> {
                    double r = getRadiusVal(vals, (p1 != null && p2 != null) ? p1.distance(new Point3D(p2.getX(), 0, p2.getZ())) : 10.0);
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(r, 0, 0);
                }
                case CONE -> {
                    double r = getVal(vals, "Base Radius", (p1 != null && p2 != null) ? p1.distance(new Point3D(p2.getX(), 0, p2.getZ())) : 10.0);
                    if (r <= 0.0 && (vals.containsKey("Base Diameter (mm)") || vals.containsKey("Base Diameter"))) {
                        r = getVal(vals, "Base Diameter", 20.0) * 0.5;
                    }
                    double h = getVal(vals, "Height", Math.max(6.0, r * 2.0));
                    np1 = new Point3D(0, 0, 0); np2 = new Point3D(r, -h, 0);
                }
                default -> {}
            }
        }
        item.setP1P2(np1, np2);
    }

    private static double getRadiusVal(Map<String, Double> vals, double fallback) {
        if (vals.containsKey("Radius (mm)")) return vals.get("Radius (mm)");
        if (vals.containsKey("Radius")) return vals.get("Radius");
        if (vals.containsKey("Diameter (mm)")) return vals.get("Diameter (mm)") * 0.5;
        if (vals.containsKey("Diameter")) return vals.get("Diameter") * 0.5;
        return fallback;
    }

    private static double getVal(Map<String, Double> vals, String baseKey, double fallback) {
        if (vals.containsKey(baseKey + " (mm)")) return vals.get(baseKey + " (mm)");
        if (vals.containsKey(baseKey)) return vals.get(baseKey);
        return fallback;
    }
}
