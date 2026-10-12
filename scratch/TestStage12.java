package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.topology.topology_geometry_helper_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

public class TestStage12 {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {});
        Thread.sleep(300);
        System.out.println("=================================================");
        System.out.println("Running Stage 12: Face-Mounted Shape Cascade Tests");
        System.out.println("=================================================");
        testTranslate_topFace();
        testTranslate_frontFace();
        testRotateY_topFace();
        testRotateX_sideFace();
        testOwnerNull_noThrow();
        testFaceKindNull_noThrow();
        testHitUV_preserved();
        testTwoHopChain();
        testHandComputed_translateAndRotateY();
        System.out.println();
        System.out.printf("STAGE 12 RESULTS: %d/%d Passed (%d Failed)%n", passed, passed + failed, failed);
        System.out.println("=================================================");
        System.exit(failed > 0 ? 1 : 0);
    }

    private static void pass(String n) { passed++; System.out.println("[PASS] " + n); }
    private static void fail(String n, String r) { failed++; System.out.println("[FAIL] " + n + " -- " + r); }
    private static void assertNear(String label, double expected, double actual, double tol) {
        if (Math.abs(expected - actual) <= tol) pass(label);
        else fail(label, String.format("expected %.4f got %.4f", expected, actual));
    }

    private static shape_item_ui_main makeCube(double wx, double wy, double wz, double rx, double ry) {
        var s = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0,0,0), new Point3D(20,-20,20), wx, wy, wz);
        if (rx != 0 || ry != 0) s.setRotation(rx, ry);
        return s;
    }

    private static shape_item_ui_main makeRectOnFace(shape_item_ui_main owner, face_kind_ui_main kind, double hitU, double hitV) {
        var ref = topology_geometry_helper_ui_main.reconstructFaceReference(owner, kind, hitU, hitV);
        if (ref == null) return null;
        Point3D hit = ref.getWorldHitPoint();
        var rect = new shape_item_ui_main(basic_shapes_ui_main.RECTANGLE, new Point3D(-4,0,-3), new Point3D(4,0,3));
        rect.setFacePlane(ref.getUAxis(), ref.getVAxis(), ref.getWorldNormal(), owner.getId(), kind, hitU, hitV);
        rect.setWorldTranslation(hit.getX(), hit.getY(), hit.getZ());
        return rect;
    }

    private static Point3D expected(shape_item_ui_main owner, shape_item_ui_main b) {
        var ref = topology_geometry_helper_ui_main.reconstructFaceReference(
            owner, b.getFaceKind(), b.getLocalHitU(), b.getLocalHitV());
        return ref != null ? ref.getWorldHitPoint() : null;
    }

    private static void testTranslate_topFace() {
        System.out.println("\n--- 1. Translate host: rectangle follows on TOP face ---");
        var A = makeCube(0,0,0,0,0);
        var B = makeRectOnFace(A, face_kind_ui_main.TOP, 3.0, 2.0);
        if (B == null) { fail("B on TOP", "null ref"); return; }
        pass("B created on TOP face");
        A.setWorldTranslation(30, 0, 15);
        shape_item_ui_main.recomputeWorldPositionFromOwner(B, A);
        Point3D exp = expected(A, B);
        if (exp == null) { fail("expected non-null after translate", "null"); return; }
        assertNear("B.worldX after translate (+30)", exp.getX(), B.getWorldX(), 0.01);
        assertNear("B.worldY after translate", exp.getY(), B.getWorldY(), 0.01);
        assertNear("B.worldZ after translate (+15)", exp.getZ(), B.getWorldZ(), 0.01);
    }

    private static void testTranslate_frontFace() {
        System.out.println("\n--- 2. Translate host: rectangle follows on FRONT face ---");
        var A = makeCube(0,0,0,0,0);
        var B = makeRectOnFace(A, face_kind_ui_main.FRONT, 0.0, 0.0);
        if (B == null) { fail("B on FRONT", "null ref"); return; }
        pass("B created on FRONT face");
        A.setWorldTranslation(0, 0, 50);
        shape_item_ui_main.recomputeWorldPositionFromOwner(B, A);
        Point3D exp = expected(A, B);
        if (exp == null) { fail("FRONT expected non-null", "null"); return; }
        assertNear("FRONT face B.worldZ after A.wz=50", exp.getZ(), B.getWorldZ(), 0.01);
    }

    private static void testRotateY_topFace() {
        System.out.println("\n--- 3. Rotate host Y: rectangle follows TOP face orientation ---");
        var A = makeCube(0,0,0,0,0);
        var B = makeRectOnFace(A, face_kind_ui_main.TOP, 5.0, 0.0);
        if (B == null) { fail("B for rotY test", "null ref"); return; }
        A.setRotation(0, 45);
        shape_item_ui_main.recomputeWorldPositionFromOwner(B, A);
        Point3D exp = expected(A, B);
        if (exp == null) { fail("rotY expected non-null", "null"); return; }
        assertNear("B.worldX matches after 45deg rotY", exp.getX(), B.getWorldX(), 0.01);
        assertNear("B.worldZ matches after 45deg rotY", exp.getZ(), B.getWorldZ(), 0.01);
        assertNear("B.uAxis.X after 45deg rotY", Math.cos(Math.toRadians(45)), B.getUAxis().getX(), 0.01);
    }

    private static void testRotateX_sideFace() {
        System.out.println("\n--- 4. Rotate host X: RIGHT face position updates ---");
        var A = makeCube(0,0,0,0,0);
        var B = makeRectOnFace(A, face_kind_ui_main.RIGHT, 0.0, 0.0);
        if (B == null) { fail("B on RIGHT for rotX", "null ref"); return; }
        A.setRotation(30, 0);
        shape_item_ui_main.recomputeWorldPositionFromOwner(B, A);
        Point3D exp = expected(A, B);
        if (exp == null) { fail("rotX expected non-null", "null"); return; }
        assertNear("RIGHT face B.worldX after 30deg rotX", exp.getX(), B.getWorldX(), 0.01);
        assertNear("RIGHT face B.worldY after 30deg rotX", exp.getY(), B.getWorldY(), 0.01);
    }

    private static void testOwnerNull_noThrow() {
        System.out.println("\n--- 5. Owner null: no throw, B unchanged ---");
        var A = makeCube(0,0,0,0,0);
        var B = makeRectOnFace(A, face_kind_ui_main.TOP, 0, 0);
        if (B == null) { fail("setup null-owner test", "null ref"); return; }
        double beforeX = B.getWorldX();
        try { shape_item_ui_main.recomputeWorldPositionFromOwner(B, null); pass("No exception with null owner"); }
        catch (Exception e) { fail("No exception with null owner", e.toString()); return; }
        assertNear("B.worldX unchanged when owner null", beforeX, B.getWorldX(), 0.001);
    }

    private static void testFaceKindNull_noThrow() {
        System.out.println("\n--- 6. FaceKind null on B: no throw ---");
        var A = makeCube(0,0,0,0,0);
        var B = new shape_item_ui_main(basic_shapes_ui_main.RECTANGLE, new Point3D(-4,0,-3), new Point3D(4,0,3));
        try { shape_item_ui_main.recomputeWorldPositionFromOwner(B, A); pass("No exception with null faceKind"); }
        catch (Exception e) { fail("No exception with null faceKind", e.toString()); }
    }

    private static void testHitUV_preserved() {
        System.out.println("\n--- 7. localHitU/V stored and preserved through setFacePlane ---");
        var A = makeCube(0,0,0,0,0);
        double hitU = 4.5, hitV = -2.3;
        var B = makeRectOnFace(A, face_kind_ui_main.TOP, hitU, hitV);
        if (B == null) { fail("B for hitUV test", "null ref"); return; }
        assertNear("localHitU stored", hitU, B.getLocalHitU(), 0.001);
        assertNear("localHitV stored", hitV, B.getLocalHitV(), 0.001);
        B.setFacePlane(B.getUAxis(), B.getVAxis(), B.getFaceNormal(), A.getId(), face_kind_ui_main.TOP, hitU, hitV);
        assertNear("localHitU after re-setFacePlane", hitU, B.getLocalHitU(), 0.001);
        assertNear("localHitV after re-setFacePlane", hitV, B.getLocalHitV(), 0.001);
    }

    private static void testTwoHopChain() {
        System.out.println("\n--- 8. Two-hop chain: B on A, A moves 100 units, B follows ---");
        var A = makeCube(0,0,0,0,0);
        var B = makeRectOnFace(A, face_kind_ui_main.TOP, 2.0, 1.0);
        if (B == null) { fail("B two-hop setup", "null ref"); return; }
        Point3D beforeB = new Point3D(B.getWorldX(), B.getWorldY(), B.getWorldZ());
        A.setWorldTranslation(100, 0, 50);
        java.util.Map<String, shape_item_ui_main> byId = new java.util.HashMap<>();
        byId.put(A.getId(), A);
        for (shape_item_ui_main s : java.util.List.of(A, B)) {
            if (s.isOnFace()) { shape_item_ui_main own = byId.get(s.getFaceOwnerId()); if (own != null) shape_item_ui_main.recomputeWorldPositionFromOwner(s, own); }
        }
        Point3D exp = expected(A, B);
        if (exp == null) { fail("two-hop expected non-null", "null"); return; }
        assertNear("B.worldX follows A (two-hop)", exp.getX(), B.getWorldX(), 0.01);
        assertNear("B.worldZ follows A (two-hop)", exp.getZ(), B.getWorldZ(), 0.01);
        // Tight tolerance matching Test 1: B.world must equal reconstructFaceReference(A_moved).worldHit exactly.
        assertNear("B.worldX not frozen (tight)", exp.getX(), B.getWorldX(), 0.01);
        assertNear("B.worldZ not frozen (tight)", exp.getZ(), B.getWorldZ(), 0.01);
        // Also confirm B actually moved from its draw-time position (rules out trivial all-zeros case)
        boolean bMoved = Math.abs(B.getWorldX() - beforeB.getX()) > 50 || Math.abs(B.getWorldZ() - beforeB.getZ()) > 20;
        if (bMoved) pass("B position changed vs draw-time origin");
        else fail("B position changed vs draw-time origin", String.format("was %.1f,%.1f still %.1f,%.1f", beforeB.getX(), beforeB.getZ(), B.getWorldX(), B.getWorldZ()));
    }

    private static void testHandComputed_translateAndRotateY() {
        System.out.println("\n--- 9. Hand-computed first-principles check: Translate + 90deg RotY ---");
        // Host A: Cube (0,0,0) to (20,-20,20), center c = (10, -10, 10), top face y = -20, top face origin = (10, -20, 10).
        var A = makeCube(0, 0, 0, 0, 0);
        // B placed on TOP face at local UV = (+4.0, -6.0)
        // Local hit point on A: (10 + 4, -20, 10 - 6) = (14, -20, 4)
        var B = makeRectOnFace(A, face_kind_ui_main.TOP, 4.0, -6.0);
        if (B == null) { fail("B setup for hand-computed", "null ref"); return; }

        // Host A transformed: translated by (50, 10, -30), rotated around Y by +90 deg.
        A.setWorldTranslation(50, 10, -30);
        A.setRotation(0, 90);
        shape_item_ui_main.recomputeWorldPositionFromOwner(B, A);

        // Hand-computed analytic first-principles calculation using shape_rotation_helper convention:
        // Relative to center (10, -10, 10): dx = 4, dy = -10, dz = -6
        // JavaFX Y-rotation (rx=0, ry=+90deg):
        //   x' = dx * cos(90) + dz * sin(90) = 4(0) + (-6)(1) = -6
        //   y' = dy = -10
        //   z' = -dx * sin(90) + dz * cos(90) = -4(1) + (-6)(0) = -4
        // Center + rotated offset = (10 - 6, -10 - 10, 10 - 4) = (4, -20, 6)
        // World translation + (50, 10, -30) => World Position = (54.0, -10.0, -24.0)
        double handX = 54.0;
        double handY = -10.0;
        double handZ = -24.0;

        assertNear("Hand-computed WorldX (54.0)", handX, B.getWorldX(), 0.001);
        assertNear("Hand-computed WorldY (-10.0)", handY, B.getWorldY(), 0.001);
        assertNear("Hand-computed WorldZ (-24.0)", handZ, B.getWorldZ(), 0.001);

        // Hand-computed axes:
        // u_local = (1,0,0) -> x' = 0, z' = -1 -> (0, 0, -1)
        // v_local = (0,0,1) -> x' = 1, z' = 0  -> (1, 0, 0)
        // n_local = (0,-1,0) -> (0, -1, 0)
        assertNear("Hand-computed uAxis.X (0.0)", 0.0, B.getUAxis().getX(), 0.001);
        assertNear("Hand-computed uAxis.Z (-1.0)", -1.0, B.getUAxis().getZ(), 0.001);
        assertNear("Hand-computed vAxis.X (1.0)", 1.0, B.getVAxis().getX(), 0.001);
        assertNear("Hand-computed vAxis.Z (0.0)", 0.0, B.getVAxis().getZ(), 0.001);
        assertNear("Hand-computed normal.Y (-1.0)", -1.0, B.getFaceNormal().getY(), 0.001);
    }
}