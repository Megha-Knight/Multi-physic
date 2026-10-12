package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import ui.workspace.drafting.extrude.extrude_magnet_helper_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.List;

public class TestTask2ExtrudeMagnet {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {});
        Thread.sleep(300);
        System.out.println("=================================================");
        System.out.println("Running Task 2: Extrude Magnet Attach Tests");
        System.out.println("=================================================");

        testSnapToTopFace();
        testSnapToFrontFace();
        testOutsideThreshold_remainsGround();
        testNoShapes_remainsGround();
        testSnapToCylinderTopCap();
        testSnapToRightFace_handDerived();
        testSnapToRotatedHost_handDerived();

        System.out.println();
        System.out.printf("TASK 2 RESULTS: %d/%d Passed (%d Failed)%n", passed, passed + failed, failed);
        System.out.println("=================================================");
        System.exit(failed > 0 ? 1 : 0);
    }

    private static void pass(String n) { passed++; System.out.println("[PASS] " + n); }
    private static void fail(String n, String r) { failed++; System.out.println("[FAIL] " + n + " -- " + r); }

    private static shape_item_ui_main makeCube(double wx, double wy, double wz) {
        var s = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0,0,0), new Point3D(20,-20,20), wx, wy, wz);
        return s;
    }

    private static void testSnapToTopFace() {
        System.out.println("\n--- 1. Snap to Top Face within threshold ---");
        var host = makeCube(0, 0, 0); // Cube sits at X=[0,20], Y=[-20,0], Z=[0,20]. Top face is at Y=-20, X=[0,20], Z=[0,20].
        Point3D drawCenter = new Point3D(10, -22, 10); // 2mm above top face at Y=-20.
        var snap = extrude_magnet_helper_ui_main.findNearestFace(drawCenter, List.of(host), null);
        if (snap == null) { fail("Snap to Top Face", "null result"); return; }
        if (snap.faceRef().getFaceKind() != face_kind_ui_main.TOP) {
            fail("Snap to Top Face", "expected TOP got " + snap.faceRef().getFaceKind()); return;
        }
        if (snap.distance() > 30.0) { fail("Snap distance <= 30", "got " + snap.distance()); return; }

        var item = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(10, -20, 10), new Point3D(15, -20, 10));
        var snapped = extrude_magnet_helper_ui_main.trySnapToNearestFace(item, basic_shapes_ui_main.CYLINDER, new Point3D(10, -20, 10), new Point3D(15, -20, 10), List.of(host));
        if (snapped == null || !item.isOnFace()) { fail("trySnapToNearestFace top", "item not on face"); return; }
        if (item.getFaceKind() != face_kind_ui_main.TOP) { fail("item faceKind TOP", "got " + item.getFaceKind()); return; }
        if (Math.abs(item.getWorldY() - (-20.0)) > 0.01) { fail("item worldY flush on top (-20)", "got " + item.getWorldY()); return; }
        pass("Snap to Top Face within threshold (flush at Y=-20)");
    }

    private static void testSnapToFrontFace() {
        System.out.println("\n--- 2. Snap to Front Face ---");
        var host = makeCube(0, 0, 0); // Front face is at maxZ = 20, Y=[-20,0], X=[0,20]. Normal is (0,0,1).
        Point3D drawCenter = new Point3D(10, -10, 25); // 5mm away from front face at mid-height
        var snap = extrude_magnet_helper_ui_main.findNearestFace(drawCenter, List.of(host), null);
        if (snap == null || snap.faceRef().getFaceKind() != face_kind_ui_main.FRONT) {
            fail("Snap to Front Face", snap == null ? "null" : snap.faceRef().getFaceKind().name()); return;
        }
        var item = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(10, 0, 10));
        var snapped = extrude_magnet_helper_ui_main.trySnapToNearestFace(item, basic_shapes_ui_main.CUBE, new Point3D(10, -10, 25), new Point3D(15, -10, 25), List.of(host));
        if (snapped == null || !item.isOnFace() || item.getFaceKind() != face_kind_ui_main.FRONT) {
            fail("item snapped to FRONT", "failed"); return;
        }
        if (Math.abs(item.getWorldZ() - 20.0) > 0.01) { fail("item worldZ at front (20)", "got " + item.getWorldZ()); return; }
        double rx = item.getRotationX(); if (rx > 180) rx -= 360;
        if (Math.abs(rx - (-90.0)) > 0.1) { fail("item rotationX along +Z (-90)", "got " + item.getRotationX()); return; }
        pass("Snap to Front Face flush with correct rotation along normal");
    }

    private static void testOutsideThreshold_remainsGround() {
        System.out.println("\n--- 3. Outside snap threshold (remains ground) ---");
        var host = makeCube(0, 0, 0);
        Point3D farPt = new Point3D(100, 0, 100); // Distance >> 30mm
        var item = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(90, 0, 90), new Point3D(110, 0, 110));
        var snapped = extrude_magnet_helper_ui_main.trySnapToNearestFace(item, basic_shapes_ui_main.CUBE, new Point3D(90, 0, 90), new Point3D(110, 0, 110), List.of(host));
        if (snapped != null || item.isOnFace()) {
            fail("Outside threshold remains ground", "incorrectly snapped"); return;
        }
        pass("Outside threshold (>30mm) remains ground-based shape");
    }

    private static void testNoShapes_remainsGround() {
        System.out.println("\n--- 4. Empty scene (remains ground) ---");
        var item = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(10, 0, 10));
        var snapped = extrude_magnet_helper_ui_main.trySnapToNearestFace(item, basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(10, 0, 10), List.of());
        if (snapped != null || item.isOnFace()) {
            fail("Empty scene remains ground", "incorrectly snapped"); return;
        }
        pass("Empty scene remains independent ground-based shape");
    }

    private static void testSnapToCylinderTopCap() {
        System.out.println("\n--- 5. Snap to Cylinder Top Cap ---");
        var cyl = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(15, -30, 0));
        Point3D drawCenter = new Point3D(0, -15, 0); // 15mm from top cap at Y=-30
        var snap = extrude_magnet_helper_ui_main.findNearestFace(drawCenter, List.of(cyl), null);
        if (snap == null || snap.faceRef().getFaceKind() != face_kind_ui_main.TOP_CAP) {
            fail("Snap to Cylinder Top Cap", snap == null ? "null" : snap.faceRef().getFaceKind().name()); return;
        }
        pass("Snap to Cylinder Top Cap within threshold");
    }

    private static void testSnapToRightFace_handDerived() {
        System.out.println("\n--- 6. Snap to RIGHT Face (Hand-derived math check) ---");
        var host = makeCube(0, 0, 0); // Right face is at maxX = 20, normal = (1, 0, 0).
        Point3D drawCenter = new Point3D(25, -10, 10); // 5mm away in +X direction from right face center
        var snap = extrude_magnet_helper_ui_main.findNearestFace(drawCenter, List.of(host), null);
        if (snap == null || snap.faceRef().getFaceKind() != face_kind_ui_main.RIGHT) {
            fail("Snap to RIGHT Face", snap == null ? "null" : snap.faceRef().getFaceKind().name()); return;
        }
        var item = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(10, 0, 10));
        var snapped = extrude_magnet_helper_ui_main.trySnapToNearestFace(item, basic_shapes_ui_main.CUBE, new Point3D(25, -10, 10), new Point3D(35, -10, 10), List.of(host));
        if (snapped == null || !item.isOnFace() || item.getFaceKind() != face_kind_ui_main.RIGHT) {
            fail("item snapped to RIGHT", "failed"); return;
        }
        // Hand-derived: Right face is at X=20.0, normal is (1,0,0)
        // Rotation formula: rx = -90, ry = atan2(1, 0) = 90
        if (Math.abs(item.getWorldX() - 20.0) > 0.01) { fail("worldX at right face (20.0)", "got " + item.getWorldX()); return; }
        double rx = item.getRotationX(); if (rx > 180) rx -= 360;
        double ry = item.getRotationY(); if (ry > 180) ry -= 360;
        if (Math.abs(rx - (-90.0)) > 0.1) { fail("item rotationX along +X (-90)", "got " + rx); return; }
        if (Math.abs(ry - 90.0) > 0.1) { fail("item rotationY along +X (90)", "got " + ry); return; }
        pass("Snap to RIGHT Face flush (worldX=20, rx=-90, ry=90 hand-derived)");
    }

    private static void testSnapToRotatedHost_handDerived() {
        System.out.println("\n--- 7. Snap to Rotated Host (45deg RotY) ---");
        var host = makeCube(0, 0, 0);
        host.setRotation(0, 45.0); // 45 deg Y rotation around cube center (10, -10, 10)
        // With host rotated by 45 deg around Y, front face normal (0,0,1) rotates to (sin 45, 0, cos 45) = (0.7071, 0, 0.7071)
        // The nearest face algorithm measures in world space:
        Point3D normal = ui.workspace.drafting.topology.topology_geometry_helper_ui_main.getFaceNormal(host, face_kind_ui_main.FRONT);
        Point3D origin = ui.workspace.drafting.topology.topology_geometry_helper_ui_main.getFaceOrigin(host, face_kind_ui_main.FRONT);
        Point3D drawCenter = origin.add(normal.multiply(5.0)); // 5mm outward from rotated front face
        var snap = extrude_magnet_helper_ui_main.findNearestFace(drawCenter, List.of(host), null);
        if (snap == null || snap.faceRef().getFaceKind() != face_kind_ui_main.FRONT) {
            fail("Snap to Rotated Host FRONT", snap == null ? "null" : snap.faceRef().getFaceKind().name()); return;
        }
        var item = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(5, -10, 0));
        var snapped = extrude_magnet_helper_ui_main.trySnapToNearestFace(item, basic_shapes_ui_main.CYLINDER, drawCenter, drawCenter.add(5, 0, 0), List.of(host));
        if (snapped == null || !item.isOnFace()) { fail("trySnap to rotated host", "failed"); return; }
        // Hand-derived: rotated normal has ry = 45.0 deg
        double ry = item.getRotationY(); if (ry > 180) ry -= 360;
        if (Math.abs(ry - 45.0) > 0.1) { fail("item rotationY matches host rotation (45.0)", "got " + ry); return; }
        pass("Snap to Rotated Host (ry=45.0 matches rotated face outward normal)");
    }
}
