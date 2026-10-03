package ui.File_Types;

import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * nc_hole_toolpath_ui_main.java
 * High-precision G-code generator for parametric CAD holes, countersinks,
 * counterbores, and pattern instances across all 3D face orientations.
 */
public final class nc_hole_toolpath_ui_main {

    private nc_hole_toolpath_ui_main() {}

    public record HoleGeom(Point3D center, Point3D norm, Point3D dir, double depth, double dia, double csDia, double csDepth, double cbDia, double cbDepth, hole_feature_ui_main.HoleType type, face_kind_ui_main face) {}

    public static HoleGeom resolveHole(shape_item_ui_main item, hole_feature_ui_main hole) {
        Point3D p1 = item.getP1(), p2 = item.getP2(), c = item.getCenter();
        double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
        double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
        face_kind_ui_main face = hole.getFaceKind();
        Frame f;
        double thickness;

        if (item.getType() == ui.workspace.shapes.basic_shapes_ui_main.CYLINDER) {
            double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
            thickness = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
            f = (face == face_kind_ui_main.BOTTOM_CAP)
                ? new Frame(new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1))
                : new Frame(new Point3D(p1.getX(), -thickness, p1.getZ()), new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1));
        } else {
            f = hole_mesh_triangulator_ui_main.getCuboidFaceFrame(c, w, h, d, face);
            thickness = hole_mesh_triangulator_ui_main.getFaceThickness(w, h, d, face);
        }

        Point3D locEntry = f.origin().add(f.u().multiply(hole.getU())).add(f.v().multiply(hole.getV()));
        Point3D wEntry = shape_rotation_helper_ui_main.transformPoint(locEntry, c, item.getRotationX(), item.getRotationY(), item.getWorldX(), item.getWorldY(), item.getWorldZ());
        Point3D wNorm = shape_rotation_helper_ui_main.transformNormal(f.n(), item.getRotationX(), item.getRotationY());
        Point3D wDir = wNorm.multiply(-1.0);

        Point3D cncCenter = new Point3D(wEntry.getX(), wEntry.getZ(), -wEntry.getY());
        Point3D cncNorm = new Point3D(wNorm.getX(), wNorm.getZ(), -wNorm.getY());
        Point3D cncDir = new Point3D(wDir.getX(), wDir.getZ(), -wDir.getY());

        double depth = hole.isThroughAll() ? (thickness + 2.0) : hole.getDepth();
        return new HoleGeom(cncCenter, cncNorm, cncDir, depth, hole.getDiameter(), hole.getCsDiameter(), hole.getConeDepth(), hole.getCbDiameter(), hole.getCbDepth(), hole.getHoleType(), face);
    }

    public static void writeDrillOperations(PrintWriter pw, List<HoleGeom> holes, double safeZ) {
        if (holes.isEmpty()) return;
        pw.println("(--------------------------------------------------)");
        pw.println("( OPERATION 1: PILOT / MAIN DRILLING CYCLE - TOOL T2 )");
        pw.println("(--------------------------------------------------)");
        pw.println("T2 M06       (Tool #2: Twist Drill)");
        pw.println("S4500 M03    (Spindle Clockwise: 4500 RPM)");
        pw.printf(Locale.US, "G00 Z%.4f (Rapid to Safety Clearance)%n", safeZ);

        for (HoleGeom h : holes) {
            writeSingleHoleCycle(pw, h, safeZ, h.depth, "Main Bore / Pilot Drill");
        }
        pw.println("G80          (Cancel Canned Cycle)");
        pw.printf(Locale.US, "G00 Z%.4f (Retract to Safety Level)%n%n", safeZ);
    }

    public static void writeCountersinkOperations(PrintWriter pw, List<HoleGeom> holes, double safeZ) {
        List<HoleGeom> csHoles = holes.stream().filter(h -> h.type == hole_feature_ui_main.HoleType.COUNTERSINK).toList();
        if (csHoles.isEmpty()) return;
        pw.println("(--------------------------------------------------)");
        pw.println("( OPERATION 2: COUNTERSINK CHAMFER CYCLE - TOOL T3 )");
        pw.println("(--------------------------------------------------)");
        pw.println("T3 M06       (Tool #3: 90 Deg Countersink Cutter)");
        pw.println("S2500 M03    (Spindle Clockwise: 2500 RPM)");
        pw.printf(Locale.US, "G00 Z%.4f%n", safeZ);

        for (HoleGeom h : csHoles) {
            writeSingleHoleCycle(pw, h, safeZ, h.csDepth, "Countersink Bevel");
        }
        pw.println("G80          (Cancel Canned Cycle)");
        pw.printf(Locale.US, "G00 Z%.4f%n%n", safeZ);
    }

    public static void writeCounterboreOperations(PrintWriter pw, List<HoleGeom> holes, double safeZ) {
        List<HoleGeom> cbHoles = holes.stream().filter(h -> h.type == hole_feature_ui_main.HoleType.COUNTERBORE).toList();
        if (cbHoles.isEmpty()) return;
        pw.println("(--------------------------------------------------)");
        pw.println("( OPERATION 3: COUNTERBORE RECESS CYCLE - TOOL T4 )");
        pw.println("(--------------------------------------------------)");
        pw.println("T4 M06       (Tool #4: Counterbore Pilot Cutter)");
        pw.println("S2000 M03    (Spindle Clockwise: 2000 RPM)");
        pw.printf(Locale.US, "G00 Z%.4f%n", safeZ);

        for (HoleGeom h : cbHoles) {
            writeSingleHoleCycle(pw, h, safeZ, h.cbDepth, "Counterbore Recess");
        }
        pw.println("G80          (Cancel Canned Cycle)");
        pw.printf(Locale.US, "G00 Z%.4f%n%n", safeZ);
    }

    private static void writeSingleHoleCycle(PrintWriter pw, HoleGeom h, double safeZ, double opDepth, String label) {
        Point3D p = h.center, n = h.norm;
        pw.printf(Locale.US, "(HOLE: %s | Face: %s | Center: %.3f, %.3f, %.3f | Depth: %.3f)%n", label, h.face.name(), p.getX(), p.getY(), p.getZ(), opDepth);
        pw.printf(Locale.US, "(AXIS: I=%.4f J=%.4f K=%.4f)%n", h.dir.getX(), h.dir.getY(), h.dir.getZ());

        if (n.getZ() > 0.5) { // TOP face or vertical hole
            double targetZ = p.getZ() - opDepth;
            double rPlane = p.getZ() + 2.0;
            pw.printf(Locale.US, "G00 X%.4f Y%.4f%n", p.getX(), p.getY());
            pw.printf(Locale.US, "G81 X%.4f Y%.4f Z%.4f R%.4f F250 (Drill Cycle)%n", p.getX(), p.getY(), targetZ, rPlane);
            pw.printf(Locale.US, "G00 Z%.4f%n", rPlane);
        } else if (n.getZ() < -0.5) { // BOTTOM face (WCS indexing / B-side)
            pw.println("(INDEX: B-AXIS 180 DEG FLIP FOR BOTTOM FACE)");
            double targetZ = p.getZ() + opDepth;
            double rPlane = p.getZ() - 2.0;
            pw.printf(Locale.US, "G00 X%.4f Y%.4f%n", p.getX(), p.getY());
            pw.printf(Locale.US, "G00 Z%.4f (Approach Datum)%n", rPlane);
            pw.printf(Locale.US, "G01 Z%.4f F200 (Inward Drill Feed)%n", targetZ);
            pw.printf(Locale.US, "G00 Z%.4f (Retract)%n", rPlane);
        } else if (Math.abs(n.getY()) > 0.5) { // FRONT / BACK face (G18 plane)
            pw.println("G18          (Working Plane XZ / Side Index)");
            double targetY = p.getY() + (n.getY() > 0 ? -opDepth : opDepth);
            pw.printf(Locale.US, "G00 X%.4f Z%.4f%n", p.getX(), p.getZ());
            pw.printf(Locale.US, "G00 Y%.4f (Rapid Approach)%n", p.getY() + (n.getY() > 0 ? 2.0 : -2.0));
            pw.printf(Locale.US, "G01 Y%.4f F200 (Horizontal Feed)%n", targetY);
            pw.printf(Locale.US, "G00 Y%.4f (Retract)%n", p.getY() + (n.getY() > 0 ? 2.0 : -2.0));
            pw.println("G17          (Restore XY Working Plane)");
        } else { // LEFT / RIGHT face (G19 plane)
            pw.println("G19          (Working Plane YZ / Side Index)");
            double targetX = p.getX() + (n.getX() > 0 ? -opDepth : opDepth);
            pw.printf(Locale.US, "G00 Y%.4f Z%.4f%n", p.getY(), p.getZ());
            pw.printf(Locale.US, "G00 X%.4f (Rapid Approach)%n", p.getX() + (n.getX() > 0 ? 2.0 : -2.0));
            pw.printf(Locale.US, "G01 X%.4f F200 (Horizontal Feed)%n", targetX);
            pw.printf(Locale.US, "G00 X%.4f (Retract)%n", p.getX() + (n.getX() > 0 ? 2.0 : -2.0));
            pw.println("G17          (Restore XY Working Plane)");
        }
    }
}
