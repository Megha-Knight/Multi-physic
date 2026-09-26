package ui.workspace.document;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * nc_exporter_ui_main.java
 * CNC Numerical Control (G-code .nc) toolpath and legacy CAD model exporter for Astra.
 */
public final class nc_exporter_ui_main {

    private nc_exporter_ui_main() {}

    public static boolean exportToNc(File file, List<shape_item_ui_main> shapes) {
        if (file == null) return false;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("(==================================================)");
            pw.printf(Locale.US, "( ASTRA CAD/CAM NUMERICAL CONTROL EXPORT: %s )%n", file.getName());
            pw.printf(Locale.US, "( GENERATED: %s )%n", timestamp);
            pw.println("( POST-PROCESSOR: GENERIC 3-AXIS CNC / ISO G-CODE )");
            pw.println("(==================================================)");
            pw.println();

            // Machine initialization
            pw.println("G21          (Metric Units: Millimeters)");
            pw.println("G90          (Absolute Programming)");
            pw.println("G17          (XY Working Plane Selection)");
            pw.println("G94          (Feedrate: mm/min)");
            pw.println("G40 G80      (Cancel Cutter Radius Comp & Canned Cycles)");
            pw.println("T1 M06       (Tool #1: Flat End Mill 6mm)");
            pw.println("S6000 M03    (Spindle Clockwise: 6000 RPM)");
            pw.println("G00 Z15.0000 (Rapid to Safety Clearance)");
            pw.println();

            if (shapes != null) {
                for (shape_item_ui_main item : shapes) {
                    pw.println("(--------------------------------------------------)");
                    pw.printf(Locale.US, "( FEATURE: %s [%s] )%n", item.getName(), item.getType().name());
                    // Embed model metadata for lossless reopening in Astra
                    pw.printf(Locale.US, "(SHAPE: %s)%n", item.getType().name());
                    pw.printf(Locale.US, "(id: %s)%n", item.getId());
                    pw.printf(Locale.US, "(name: %s)%n", item.getName());
                    Point3D p1 = item.getP1(), p2 = item.getP2();
                    pw.printf(Locale.US, "(p1: %.4f, %.4f, %.4f)%n", p1.getX(), p1.getY(), p1.getZ());
                    pw.printf(Locale.US, "(p2: %.4f, %.4f, %.4f)%n", p2.getX(), p2.getY(), p2.getZ());
                    if (item.getType().is3D() && (item.getWorldX() != 0 || item.getWorldY() != 0 || item.getWorldZ() != 0)) {
                        pw.printf(Locale.US, "(tx: %.4f, %.4f, %.4f)%n", item.getWorldX(), item.getWorldY(), item.getWorldZ());
                    }
                    if (item.getRotationAngle() != 0) {
                        pw.printf(Locale.US, "(rot: %.4f)%n", item.getRotationAngle());
                    }

                    writeFeatureToolpath(pw, item);
                    pw.println();
                }
            }

            // End of program
            pw.println("(--------------------------------------------------)");
            pw.println("G00 Z25.0000 (Retract to Final Clearance)");
            pw.println("M05          (Spindle Stop)");
            pw.println("G00 X0.0000 Y0.0000 (Return to Home)");
            pw.println("M30          (End of Program / Reset)");
            return true;
        } catch (Exception e) {
            System.err.println("[Astra] Error exporting NC: " + e.getMessage());
            return false;
        }
    }

    private static void writeFeatureToolpath(PrintWriter pw, shape_item_ui_main item) {
        basic_shapes_ui_main type = item.getType();
        Point3D p1 = item.getP1(), p2 = item.getP2(), c = item.getCenter();
        double rot = item.getRotationAngle(), rad = Math.toRadians(rot);
        double wx = type.is3D() ? item.getWorldX() : 0, wz = type.is3D() ? item.getWorldZ() : 0;
        double zCut = type.is3D() ? -Math.max(2.0, Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : 10.0) : -1.0;

        if (type == basic_shapes_ui_main.CIRCLE || type == basic_shapes_ui_main.CYLINDER
                || type == basic_shapes_ui_main.SPHERE || type == basic_shapes_ui_main.CONE) {
            double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
            Point3D center = xform(p1, c, rad, wx, wz);
            pw.printf(Locale.US, "G00 X%.4f Y%.4f (Rapid to Center)%n", center.getX(), center.getZ());
            pw.printf(Locale.US, "G00 X%.4f Y%.4f (Rapid to Perimeter)%n", center.getX() + r, center.getZ());
            pw.println("G01 Z" + String.format(Locale.US, "%.4f", zCut) + " F300 (Plunge Feed)");
            pw.printf(Locale.US, "G02 X%.4f Y%.4f I%.4f J0.0000 F1200 (Full Circular Interpolation)%n",
                      center.getX() + r, center.getZ(), -r);
            pw.println("G00 Z5.0000 (Retract)");
            return;
        }

        // Polygon or Box perimeter toolpath
        List<Point3D> contour;
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ();
        if (type == basic_shapes_ui_main.CUBE || type == basic_shapes_ui_main.SQUARE) {
            double s = Math.max(Math.abs(dx), Math.abs(dz));
            double x1 = p1.getX(), x2 = p1.getX() + (dx >= 0 ? s : -s);
            double z1 = p1.getZ(), z2 = p1.getZ() + (dz >= 0 ? s : -s);
            contour = List.of(new Point3D(x1, 0, z1), new Point3D(x2, 0, z1), new Point3D(x2, 0, z2), new Point3D(x1, 0, z2));
        } else if (type == basic_shapes_ui_main.RECTANGLE) {
            contour = List.of(p1, new Point3D(p2.getX(), 0, p1.getZ()), p2, new Point3D(p1.getX(), 0, p2.getZ()));
        } else if (type == basic_shapes_ui_main.EQUILATERAL_TRIANGLE) {
            double s = Math.sqrt(dx * dx + dz * dz), h = s * Math.sqrt(3.0) / 2.0;
            contour = List.of(p1, p2, new Point3D(p1.getX() + dx * 0.5 - (dz / s) * h, 0, p1.getZ() + dz * 0.5 + (dx / s) * h));
        } else {
            contour = List.of(p1, new Point3D(p2.getX(), 0, p1.getZ()), new Point3D(p1.getX(), 0, p2.getZ()));
        }

        Point3D start = xform(contour.get(0), c, rad, wx, wz);
        pw.printf(Locale.US, "G00 X%.4f Y%.4f (Position to Start)%n", start.getX(), start.getZ());
        pw.println("G01 Z" + String.format(Locale.US, "%.4f", zCut) + " F300 (Plunge Feed)");
        for (int i = 1; i < contour.size(); i++) {
            Point3D pt = xform(contour.get(i), c, rad, wx, wz);
            pw.printf(Locale.US, "G01 X%.4f Y%.4f F1200%n", pt.getX(), pt.getZ());
        }
        pw.printf(Locale.US, "G01 X%.4f Y%.4f F1200 (Close Profile)%n", start.getX(), start.getZ());
        pw.println("G00 Z5.0000 (Retract)");
    }

    private static Point3D xform(Point3D p, Point3D c, double rad, double wx, double wz) {
        double dx = p.getX() - c.getX(), dz = p.getZ() - c.getZ();
        double rx = dx * Math.cos(rad) + dz * Math.sin(rad);
        double rz = -dx * Math.sin(rad) + dz * Math.cos(rad);
        return new Point3D(c.getX() + rx + wx, 0, c.getZ() + rz + wz);
    }
}
