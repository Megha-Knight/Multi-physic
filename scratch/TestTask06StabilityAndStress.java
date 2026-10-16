package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import ui.File_Types.document_serializer_ui_main;
import ui.UI_Main;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sketch.*;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestTask06StabilityAndStress {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertTrue(boolean cond, String msg) {
        testsTotal++;
        if (cond) {
            testsPassed++;
            System.out.println("  [PASS] " + msg);
        } else {
            System.err.println("  [FAIL] " + msg);
            throw new AssertionError("Assertion failed: " + msg);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" TASK 06 STABILITY, STRESS & BOUNDARY HARNESS   ");
        System.out.println("=================================================");

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        try {
            testRepeatedToolSwitchingAndInterruption();
            testSketchPlaneSwitchingAndBasisIntegrity();
            testOverlayNodeCleanlinessUnderStress();
            testDegenerateAndBoundaryGeometryHandling();
            testStressDocumentSaveLoadCycles();
            testCameraBoundaryAndExtremeOrientations();
            testMultiBodyFeatureRegenerationStress();

            System.out.println("\n-------------------------------------------------");
            System.out.printf("ALL TESTS PASSED: %d / %d assertions passed cleanly.%n", testsPassed, testsTotal);
            System.out.println("-------------------------------------------------");
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("\nTASK 06 SUITE FAILED with error:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    // 1. Rapid tool switching and state interruption (50 cycles)
    private static void testRepeatedToolSwitchingAndInterruption() {
        System.out.println("\n[1] Testing Rapid Tool Switching & Interruption Stress (50 cycles)...");
        sketch_mode_ui_main mode = new sketch_mode_ui_main();
        sketch_controller_ui_main ctrl = new sketch_controller_ui_main(mode, () -> {}, () -> {});

        mode.enterBasePlane(null, sketch_plane_type_ui_main.BASE_XZ, null);
        assertTrue(mode.isActive(), "Sketch mode active for tool switching test");

        sketch_tool_type_ui_main[] tools = sketch_tool_type_ui_main.values();
        for (int i = 0; i < 50; i++) {
            sketch_tool_type_ui_main tool = tools[i % tools.length];
            ctrl.setTool(tool);
            assertTrue(ctrl.getActiveTool() == tool, "Tool switched to: " + tool);
            assertTrue(ctrl.getState() == sketch_controller_ui_main.State.IDLE,
                       "Controller state reset to IDLE on tool switch (" + tool + ")");
        }

        // Test cancel mid-drawing
        ctrl.setTool(sketch_tool_type_ui_main.LINE);
        ctrl.cancelActiveDrawing();
        assertTrue(ctrl.getState() == sketch_controller_ui_main.State.IDLE, "State is IDLE after cancelActiveDrawing");
        mode.exit(false, null);
        assertTrue(!mode.isActive(), "Sketch mode exited cleanly");
    }

    // 2. Rapid sketch-plane switching across all 3 base planes & faces
    private static void testSketchPlaneSwitchingAndBasisIntegrity() {
        System.out.println("\n[2] Testing Sketch-Plane Switching & Frame Orthogonality...");
        UI_Main ui = new UI_Main();
        var workspace = ui.getWorkspace3D();
        sketch_mode_ui_main mode = workspace.getSketchMode();
        camera_controller_ui_main cam = workspace.getCameraController();

        sketch_plane_type_ui_main[] planes = {
            sketch_plane_type_ui_main.BASE_XY,
            sketch_plane_type_ui_main.BASE_XZ,
            sketch_plane_type_ui_main.BASE_YZ
        };

        for (int cycle = 0; cycle < 10; cycle++) {
            for (sketch_plane_type_ui_main p : planes) {
                mode.enterBasePlane(null, p, cam);
                assertTrue(mode.isActive(), "Entered " + p + " cycle " + cycle);
                sketch_coord_system_ui_main cs = mode.getActiveCoordSystem();
                assertTrue(cs != null, "Coordinate system non-null for " + p);

                // Verify basis vectors are unit length and orthogonal
                Point3D u = cs.getUAxis();
                Point3D v = cs.getVAxis();
                Point3D n = cs.getNormal();
                assertTrue(Math.abs(u.magnitude() - 1.0) < 1e-4, p + ": U is unit vector");
                assertTrue(Math.abs(v.magnitude() - 1.0) < 1e-4, p + ": V is unit vector");
                assertTrue(Math.abs(n.magnitude() - 1.0) < 1e-4, p + ": Normal is unit vector");
                assertTrue(Math.abs(u.dotProduct(v)) < 1e-4, p + ": U is orthogonal to V");
                assertTrue(Math.abs(u.dotProduct(n)) < 1e-4, p + ": U is orthogonal to Normal");
                assertTrue(Math.abs(v.dotProduct(n)) < 1e-4, p + ": V is orthogonal to Normal");

                // Right-handed check: U x V = Normal
                Point3D cross = u.crossProduct(v);
                assertTrue(cross.distance(n) < 1e-4, p + ": Right-handed rule holds (U x V = Normal)");

                mode.exit(false, null);
                assertTrue(!mode.isActive(), "Exited " + p + " cleanly");
            }
        }
    }

    // 3. Viewport overlay node cleanliness under stress (50 shape creations/deletions)
    private static void testOverlayNodeCleanlinessUnderStress() {
        System.out.println("\n[3] Testing Viewport Overlay Node Cleanliness (50 operations)...");
        UI_Main ui = new UI_Main();
        shape_editor_ui_main editor = ui.getWorkspace3D().getShapeEditor();

        for (int i = 0; i < 50; i++) {
            shape_item_ui_main shape = new shape_item_ui_main(
                basic_shapes_ui_main.CUBOID,
                new Point3D(-10, -10, -10), new Point3D(10, 10, 10)
            );
            editor.addShape(shape);

            // Select and rebuild
            shape.setSelected(true);
            shape.rebuild();

            // Deselect and rebuild
            shape.setSelected(false);
            shape.rebuild();

            // Remove shape
            editor.removeShape(shape);
        }

        assertTrue(editor.getShapes().isEmpty(), "Editor shapes list is empty after 50 add/remove cycles");
    }

    // 4. Degenerate & boundary geometry handling
    private static void testDegenerateAndBoundaryGeometryHandling() {
        System.out.println("\n[4] Testing Degenerate & Boundary Geometry Handling...");

        // Degenerate circle with 0 radius
        sketch_circle_ui_main zeroCirc = new sketch_circle_ui_main("c_zero", 5.0, 5.0, 0.0);
        assertTrue(zeroCirc.getRadius() > 0.0, "Zero radius circle clamped safely to positive epsilon");
        assertTrue(zeroCirc.getCenter() != null, "Circle center point preserved");

        // Zero-length line
        sketch_line_ui_main zeroLine = new sketch_line_ui_main("l_zero", 10.0, 10.0, 10.0, 10.0);
        assertTrue(zeroLine.getLength() == 0.0, "Zero-length line entity has length 0.0");

        // Profile analyzer with empty list
        var analysisEmpty = sketch_profile_analyzer_ui_main.analyze(Collections.emptyList());
        assertTrue(analysisEmpty.type() == sketch_profile_type_ui_main.INVALID,
                   "Empty sketch classified as ProfileType.INVALID");
        assertTrue(!analysisEmpty.isValid(), "Empty sketch is not extrudable");

        // Bowtie self-intersecting polygon
        List<sketch_entity_ui_main> bowtie = List.of(
            new sketch_line_ui_main("l1", 0, 0, 10, 10),
            new sketch_line_ui_main("l2", 10, 10, 10, 0),
            new sketch_line_ui_main("l3", 10, 0, 0, 10),
            new sketch_line_ui_main("l4", 0, 10, 0, 0)
        );
        var analysisBowtie = sketch_profile_analyzer_ui_main.analyze(bowtie);
        assertTrue(analysisBowtie.type() == sketch_profile_type_ui_main.SELF_INTERSECTING,
                   "Bowtie self-intersecting lines detected safely without exception");
        assertTrue(!analysisBowtie.isValid(), "Bowtie is rejected as extrudable");
    }

    // 5. Stress document save/load cycles (10 continuous roundtrips)
    private static void testStressDocumentSaveLoadCycles() throws Exception {
        System.out.println("\n[5] Testing Multi-Cycle Document Save/Load Stress (10 cycles)...");
        File tempFile = File.createTempFile("stress_doc_", ".nd");
        tempFile.deleteOnExit();

        shape_item_ui_main base = new shape_item_ui_main(
            basic_shapes_ui_main.CUBOID, new Point3D(-30, -15, -30), new Point3D(30, 15, 30)
        );
        base.setName("StressSolid");
        base.addHole(new hole_feature_ui_main("h1", base.getId(), face_kind_ui_main.TOP, 0, 0, 10, 15, false));
        base.addChamfer(new chamfer_feature_ui_main("ch1", base.getId(), "C:T_F", 3.0));
        base.addFillet(new fillet_feature_ui_main("fi1", base.getId(), "C:T_B", 2.0));

        List<shape_item_ui_main> currentShapes = List.of(base);

        for (int cycle = 1; cycle <= 10; cycle++) {
            boolean saved = document_serializer_ui_main.saveToFile(tempFile, currentShapes);
            assertTrue(saved, "Cycle " + cycle + ": Saved successfully");
            currentShapes = document_serializer_ui_main.loadFromFile(tempFile);
            assertTrue(currentShapes.size() == 1, "Cycle " + cycle + ": Loaded 1 shape");
            shape_item_ui_main s = currentShapes.get(0);
            assertTrue(s.getName().equals("StressSolid"), "Cycle " + cycle + ": Shape name retained");
            assertTrue(s.getHoles().size() == 1, "Cycle " + cycle + ": Hole retained");
            assertTrue(s.getChamfers().size() == 1, "Cycle " + cycle + ": Chamfer retained");
            assertTrue(s.getFillets().size() == 1, "Cycle " + cycle + ": Fillet retained");
        }
    }

    // 6. Camera movement boundary and extreme orientations
    private static void testCameraBoundaryAndExtremeOrientations() {
        System.out.println("\n[6] Testing Camera Movement Boundary & Orientations...");
        UI_Main ui = new UI_Main();
        camera_controller_ui_main cam = ui.getWorkspace3D().getCameraController();

        // Extreme pitch tests (-90, +90, -89.9, 89.9, 0.0, -180, 180)
        double[] pitches = {-90.0, 90.0, -89.9, 89.9, 0.0, -180.0, 180.0};
        for (double p : pitches) {
            cam.setOrientation(p, 45.0);
            assertTrue(cam.getRx().getAngle() == p, "Camera pitch set to: " + p);
        }

        // Full 360 yaw rotations
        for (double y = -360.0; y <= 360.0; y += 90.0) {
            cam.setOrientation(30.0, y);
            assertTrue(cam.getRy().getAngle() == y, "Camera yaw set to: " + y);
        }

        // Standard perspective
        cam.setOrientation(30.0, -45.0);
        assertTrue(cam.getRx().getAngle() == 30.0 && cam.getRy().getAngle() == -45.0,
                   "Camera restores standard perspective (30, -45)");
    }

    // 7. Multi-body feature regeneration stress
    private static void testMultiBodyFeatureRegenerationStress() {
        System.out.println("\n[7] Testing Multi-Body Feature Regeneration Cascade...");
        List<shape_item_ui_main> bodies = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            shape_item_ui_main b = new shape_item_ui_main(
                basic_shapes_ui_main.CUBOID,
                new Point3D(i * 50, 0, 0), new Point3D(i * 50 + 30, 20, 20)
            );
            b.addHole(new hole_feature_ui_main("h_" + i, b.getId(), face_kind_ui_main.TOP, 0, 0, 8.0, 10.0, false));
            bodies.add(b);
        }

        for (int pass = 0; pass < 5; pass++) {
            ui.workspace.drafting.features.feature_regeneration_engine_ui_main.regenerateAll(bodies);
            for (int i = 0; i < 5; i++) {
                assertTrue(bodies.get(i).getHoles().size() == 1,
                           "Pass " + pass + " body " + i + " has 1 hole after cascade regeneration");
            }
        }
    }
}
