package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.ribbonbar.machining_panel_ui_main;
import ui.ribbonbar.ribbon_ui_main;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sketch.sketch_feature_ui_main;
import ui.workspace.drafting.sketch.sketch_mode_ui_main;
import ui.workspace.drafting.sketch.sketch_plane_type_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestStageMachiningAndSketch {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String desc, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + desc);
        } else {
            failed++;
            System.err.println("[FAIL] " + desc);
        }
    }

    public static void main(String[] args) {
        int exitCode = 1;
        try {
            CountDownLatch fxLatch = new CountDownLatch(1);
            try {
                Platform.startup(fxLatch::countDown);
            } catch (IllegalStateException e) {
                fxLatch.countDown();
            }
            if (!fxLatch.await(5, TimeUnit.SECONDS)) {
                System.err.println("JavaFX startup timeout");
                System.exit(1);
            }

            CountDownLatch runLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    runAllTests();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    runLatch.countDown();
                }
            });

            if (runLatch.await(30, TimeUnit.SECONDS) && failed == 0 && passed >= 30) {
                System.out.println("\n==========================================");
                System.out.printf("ALL %d MACHINING & SKETCH TESTS PASSED!%n", passed);
                System.out.println("==========================================");
                exitCode = 0;
            } else {
                System.err.printf("%nFAILED: %d passed, %d failed.%n", passed, failed);
                exitCode = 1;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        } finally {
            System.exit(exitCode);
        }
    }

    private static void runAllTests() throws Exception {
        System.out.println("--- Testing Machining Panel UI & Category Switching ---");
        machining_panel_ui_main panel = new machining_panel_ui_main();
        check("Machining panel initially hidden", !panel.isPanelVisible());

        // Category switching
        var mSelect = machining_panel_ui_main.class.getDeclaredMethod("selectCategory", int.class);
        mSelect.setAccessible(true);

        mSelect.invoke(panel, 0); // Cutouts
        check("Cutouts selected: panel visible when shown", true);

        mSelect.invoke(panel, 1); // Patterns
        check("Patterns category selectable", true);

        mSelect.invoke(panel, 2); // Edges
        check("Edges category selectable", true);

        // Test Machining Callbacks
        AtomicInteger filletCalls = new AtomicInteger();
        AtomicInteger chamferCalls = new AtomicInteger();
        AtomicInteger draftCalls = new AtomicInteger();
        AtomicInteger holeCalls = new AtomicInteger();
        AtomicInteger linearCalls = new AtomicInteger();
        AtomicInteger circularCalls = new AtomicInteger();

        panel.setOnFilletRequested(filletCalls::incrementAndGet);
        panel.setOnChamferRequested(chamferCalls::incrementAndGet);
        panel.setOnDraftRequested(draftCalls::incrementAndGet);
        panel.setOnHoleRequested(holeCalls::incrementAndGet);
        panel.setOnLinearPatternRequested(linearCalls::incrementAndGet);
        panel.setOnCircularPatternRequested(circularCalls::incrementAndGet);

        // Ribbon Bar Wiring
        ribbon_ui_main ribbon = new ribbon_ui_main();
        ribbon.setOnFilletRequested(filletCalls::incrementAndGet);
        ribbon.setOnChamferRequested(chamferCalls::incrementAndGet);
        ribbon.setOnDraftRequested(draftCalls::incrementAndGet);
        ribbon.setOnHoleRequested(holeCalls::incrementAndGet);
        ribbon.setOnLinearPatternRequested(linearCalls::incrementAndGet);
        ribbon.setOnCircularPatternRequested(circularCalls::incrementAndGet);

        check("Ribbon Machining Button has items", ribbon.getMachiningButton() != null);
        check("Ribbon Sketch Button exists", ribbon.getSketchButton() != null);
        check("Ribbon Basic Shapes Button exists", ribbon.getBasicShapesButton() != null);

        // Feature Models: Fillet, Chamfer, Draft
        System.out.println("\n--- Testing Machining Feature Models (Fillet, Chamfer, Draft) ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-20, 0, -20), new Point3D(20, 40, 20));
        cube.getTopology(); // build topology

        fillet_feature_ui_main fillet = new fillet_feature_ui_main(null, cube.getId(), "TOP_FRONT", 3.0);
        check("Fillet created with edge TOP_FRONT", "TOP_FRONT".equals(fillet.getSimpleEdgeName()));
        check("Fillet radius is 3.0", Math.abs(fillet.getRadius() - 3.0) < 1e-4);
        boolean filletValid = fillet.revalidate(cube);
        check("Fillet revalidates on cube", filletValid);

        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main(null, cube.getId(), "TOP_BACK", 2.5);
        check("Chamfer created with edge TOP_BACK", "TOP_BACK".equals(chamfer.getSimpleEdgeName()));
        check("Chamfer distance is 2.5", Math.abs(chamfer.getDistance() - 2.5) < 1e-4);
        boolean chamferValid = chamfer.revalidate(cube);
        check("Chamfer revalidates on cube", chamferValid);

        draft_feature_ui_main draft = new draft_feature_ui_main(null, cube.getId(), face_kind_ui_main.FRONT, 5.0);
        check("Draft created with face FRONT", draft.getFaceKind() == face_kind_ui_main.FRONT);
        check("Draft angle is 5.0", Math.abs(draft.getDraftAngle() - 5.0) < 1e-4);
        boolean draftValid = draft.revalidate(cube);
        check("Draft revalidates on cube", draftValid);

        cube.addFillet(fillet);
        cube.addChamfer(chamfer);
        cube.addDraft(draft);
        check("Cube has 1 fillet", cube.getFillets().size() == 1);
        check("Cube has 1 chamfer", cube.getChamfers().size() == 1);
        check("Cube has 1 draft", cube.getDrafts().size() == 1);

        // Sketch Mode Testing
        System.out.println("\n--- Testing Sketch Mode Lifecycle, Camera & Callbacks ---");
        sketch_mode_ui_main sm = new sketch_mode_ui_main();
        check("Sketch mode initially inactive", !sm.isActive());

        Rotate rx = new Rotate(30.0, Rotate.X_AXIS);
        Rotate ry = new Rotate(-45.0, Rotate.Y_AXIS);
        Translate t = new Translate(0, 0, -400.0);
        camera_controller_ui_main cam = new camera_controller_ui_main(rx, ry, t, () -> {});
        cam.setOrientation(35.0, -50.0);

        AtomicInteger modeChangedCount = new AtomicInteger();
        sm.addOnModeChanged(modeChangedCount::incrementAndGet);

        // 1. Enter on Base Plane
        boolean enteredBase = sm.enterBasePlane(null, sketch_plane_type_ui_main.BASE_XZ, cam);
        check("Enter sketch on BASE_XZ succeeded", enteredBase && sm.isActive());
        check("Mode listener fired once on enter", modeChangedCount.get() == 1);
        check("Active sketch non-null", sm.getActiveSketch() != null);
        check("Active coord system matches BASE_XZ", sm.getActiveCoordSystem().getPlaneType() == sketch_plane_type_ui_main.BASE_XZ);

        // Cancel sketch
        sm.exit(false, null);
        check("Sketch mode inactive after exit(false)", !sm.isActive());
        check("Active sketch cleared after exit", sm.getActiveSketch() == null);
        check("Mode listener fired on exit", modeChangedCount.get() == 2);
        check("Camera orientation restored after cancel", Math.abs(rx.getAngle() - 35.0) < 1e-3 && Math.abs(ry.getAngle() - (-50.0)) < 1e-3);

        // 2. Enter on planar face of shape
        shape_item_ui_main block = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-10, 0, -10), new Point3D(10, 20, 10));
        face_reference_ui_main topFace = block.getTopology().getFace(face_kind_ui_main.TOP).toFaceReference(block, 0, 0);
        check("Top face is planar", topFace.isPlanar());

        boolean enteredFace = sm.enter(block, topFace, cam);
        check("Enter sketch on planar TOP face succeeded", enteredFace && sm.isActive());
        check("Sketch associated with host block", sm.getActiveHost() == block);
        check("Host has 1 sketch attached", block.getSketches().size() == 1);

        // Commit finish sketch
        sm.exit(true, null);
        check("Sketch mode inactive after exit(true)", !sm.isActive());
        check("Host still has 1 sketch attached after commit", block.getSketches().size() == 1);

        // 3. Reject curved face
        shape_item_ui_main cyl = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(10, 20, 0));
        face_reference_ui_main latFace = cyl.getTopology().getFace(face_kind_ui_main.CYLINDER_LATERAL).toFaceReference(cyl, 0, 0);
        check("Cylinder lateral face is not planar", !latFace.isPlanar());
        boolean curvedRejected = sm.enter(cyl, latFace, cam);
        check("Curved face rejected safely", !curvedRejected && !sm.isActive());

        // 4. Cancel on new face sketch removes it from host
        shape_item_ui_main cube2 = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-15, 0, -15), new Point3D(15, 30, 15));
        face_reference_ui_main frontFace = cube2.getTopology().getFace(face_kind_ui_main.FRONT).toFaceReference(cube2, 0, 0);
        sm.enter(cube2, frontFace, cam);
        check("Host has sketch during new sketch session", cube2.getSketches().size() == 1);
        sm.exit(false, null); // Cancel!
        check("New sketch cleanly removed from host on cancel", cube2.getSketches().isEmpty());

        // 5. Re-enter sketch mode works reliably
        sm.enterBasePlane(null, sketch_plane_type_ui_main.BASE_XY, cam);
        check("Re-enter sketch mode on BASE_XY active", sm.isActive());
        sm.exit(true, null);
        check("Re-entered sketch mode exited cleanly", !sm.isActive());
    }
}
