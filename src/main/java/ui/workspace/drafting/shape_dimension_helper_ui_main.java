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
        double dist = p1.distance(p2);
        double dx = Math.abs(p2.getX() - p1.getX());
        double dz = Math.abs(p2.getZ() - p1.getZ());

        switch (type) {
            case CIRCLE -> {
                map.put("Radius", dist);
                map.put("Diameter", dist * 2.0);
            }
            case SQUARE, CUBE -> {
                double s = Math.max(dx, dz);
                map.put("Side Length", Math.max(1.0, s));
            }
            case RECTANGLE -> {
                map.put("Width", Math.max(1.0, dx));
                map.put("Height", Math.max(1.0, dz));
            }
            case EQUILATERAL_TRIANGLE -> {
                map.put("Side Length", Math.max(1.0, dist));
                map.put("Height", Math.max(1.0, dist) * Math.sqrt(3.0) / 2.0);
            }
            case RIGHT_TRIANGLE -> {
                map.put("Base", Math.max(1.0, dx));
                map.put("Height", Math.max(1.0, dz));
            }
            case CYLINDER -> {
                double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
                double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
                map.put("Radius", r);
                map.put("Diameter", r * 2.0);
                map.put("Height", h);
            }
            case SPHERE -> {
                double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
                map.put("Radius", r);
                map.put("Diameter", r * 2.0);
            }
            case CONE -> {
                double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
                double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
                map.put("Base Radius", r);
                map.put("Base Diameter", r * 2.0);
                map.put("Height", h);
            }
            default -> {}
        }

        // CAD Coordinates: X = X, Y = Depth (Z_javafx), Z = Height (-Y_javafx)
        if (type.is3D()) {
            map.put("Position X", item.getWorldX());
            map.put("Position Y", item.getWorldZ());
            map.put("Position Z", -item.getWorldY());
        } else {
            map.put("Position X", p1.getX());
            map.put("Position Y", p1.getZ());
            map.put("Position Z", 0.0);
        }
        map.put("Rotation Angle (°)", item.getRotationAngle());
        return map;
    }

    public static void applyDimensions(shape_item_ui_main item, Map<String, Double> vals) {
        if (item == null || vals == null) return;
        if (vals.containsKey("Rotation Angle (°)")) {
            item.setRotationAngle(vals.get("Rotation Angle (°)"));
        }
        basic_shapes_ui_main type = item.getType();
        Point3D p1 = item.getP1(), p2 = item.getP2();

        double posX = vals.getOrDefault("Position X", type.is3D() ? item.getWorldX() : p1.getX());
        double posY = vals.getOrDefault("Position Y", type.is3D() ? item.getWorldZ() : p1.getZ());
        double posZ = vals.getOrDefault("Position Z", type.is3D() ? -item.getWorldY() : 0.0);

        Point3D np1 = p1, np2 = p2;

        switch (type) {
            case CIRCLE -> {
                double r = vals.getOrDefault("Radius", p1.distance(p2));
                np1 = new Point3D(posX, 0, posY);
                np2 = new Point3D(posX + r, 0, posY);
            }
            case SQUARE -> {
                double s = vals.getOrDefault("Side Length", Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())));
                np1 = new Point3D(posX, 0, posY);
                np2 = new Point3D(posX + s, 0, posY + s);
            }
            case RECTANGLE -> {
                double w = vals.getOrDefault("Width", Math.abs(p2.getX() - p1.getX()));
                double h = vals.getOrDefault("Height", Math.abs(p2.getZ() - p1.getZ()));
                np1 = new Point3D(posX, 0, posY);
                np2 = new Point3D(posX + w, 0, posY + h);
            }
            case EQUILATERAL_TRIANGLE -> {
                double s = vals.getOrDefault("Side Length", p1.distance(p2));
                np1 = new Point3D(posX, 0, posY);
                np2 = new Point3D(posX + s, 0, posY);
            }
            case RIGHT_TRIANGLE -> {
                double b = vals.getOrDefault("Base", Math.abs(p2.getX() - p1.getX()));
                double h = vals.getOrDefault("Height", Math.abs(p2.getZ() - p1.getZ()));
                np1 = new Point3D(posX, 0, posY);
                np2 = new Point3D(posX + b, 0, posY + h);
            }
            case CUBE -> {
                double s = vals.getOrDefault("Side Length", Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ())));
                np1 = new Point3D(0, 0, 0);
                np2 = new Point3D(s, 0, s);
                item.setWorldTranslation(posX, -posZ, posY);
            }
            case CYLINDER -> {
                double r = vals.getOrDefault("Radius", p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
                double h = vals.getOrDefault("Height", Math.max(6.0, r * 2.0));
                np1 = new Point3D(0, 0, 0);
                np2 = new Point3D(r, -h, 0);
                item.setWorldTranslation(posX, -posZ, posY);
            }
            case SPHERE -> {
                double r = vals.getOrDefault("Radius", p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
                np1 = new Point3D(0, 0, 0);
                np2 = new Point3D(r, 0, 0);
                item.setWorldTranslation(posX, -posZ, posY);
            }
            case CONE -> {
                double r = vals.getOrDefault("Base Radius", p1.distance(new Point3D(p2.getX(), 0, p2.getZ())));
                double h = vals.getOrDefault("Height", Math.max(6.0, r * 2.0));
                np1 = new Point3D(0, 0, 0);
                np2 = new Point3D(r, -h, 0);
                item.setWorldTranslation(posX, -posZ, posY);
            }
            default -> {}
        }

        item.setP1P2(np1, np2);
    }
}
