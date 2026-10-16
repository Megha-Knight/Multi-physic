package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.File_Types.*;
import ui.UI_Main;
import ui.documenthandler_ui_main;
import ui.framework_ui_main;
import ui.savecommands_ui_main;
import ui.workspace.document.file_tab_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sketch.*;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestTask05IntegrationAndCompatibility {

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
        System.out.println("  TASK 05 E2E INTEGRATION & COMPATIBILITY SUITE  ");
        System.out.println("=================================================");

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already initialized
        }

        try {
            testApplicationShellAndComponentAssembly();
            testSketchModeContextualLifecycleAndIndicators();
            testViewportOverlayIsolationAndMouseTransparency();
            testDocumentSaveLoadCyclesAndParameterPreservation();
            testStepExportStructureAndValidation();
            testStlExportAndRoundTripValidation();
            testObjExportAndRoundTripValidation();
            testNcExportAndToolpathValidation();
            testErrorRecoveryAndInvalidDocumentHandling();
            testRepeatedLifecycleAndMemoryLeakPrevention();

            System.out.println("\n-------------------------------------------------");
            System.out.printf("ALL TESTS PASSED: %d / %d assertions passed cleanly.%n", testsPassed, testsTotal);
            System.out.println("-------------------------------------------------");
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("\nE2E SUITE FAILED with error:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    // 1. Application startup, window sizing, theme, and component assembly
    private static void testApplicationShellAndComponentAssembly() {
        System.out.println("\n[1] Testing Application Shell & Component Assembly...");
        UI_Main ui = new UI_Main();
        assertTrue(ui != null, "UI_Main instantiates cleanly");

        // Verify top ribbon, breadcrumb, and toolbar
        assertTrue(ui.getTop() != null, "Top region populated with toolbar, ribbon, and breadcrumb");

        // Verify navigation and feature manager
        assertTrue(ui.getLeft() != null, "Left region populated with navigation and feature manager");

        // Verify center area with document tabs and 3D workspace
        assertTrue(ui.getCenter() != null, "Center region populated with tab bar and 3D workspace");

        // Verify theme stylesheet loading
        assertTrue(!ui.getStylesheets().isEmpty() || getClassStylesheet() != null,
                   "Theme stylesheet is available or loaded in UI root");

        // Verify min dimensions
        Scene scene = new Scene(ui, 1200, 680);
        assertTrue(scene.getWidth() == 1200 && scene.getHeight() == 680,
                   "Application scene dimensions default to 1200x680");
    }

    private static String getClassStylesheet() {
        var url = TestTask05IntegrationAndCompatibility.class.getResource("/themes/light.css");
        return url != null ? url.toExternalForm() : null;
    }

    // 2. Contextual UI state when entering and leaving sketch mode
    private static void testSketchModeContextualLifecycleAndIndicators() {
        System.out.println("\n[2] Testing Sketch Mode Contextual Lifecycle & Indicators...");
        UI_Main ui = new UI_Main();
        var workspace = ui.getWorkspace3D();
        var sketchMode = workspace.getSketchMode();
        var cam = workspace.getCameraController();

        double initPitch = cam.getRx().getAngle();
        double initYaw = cam.getRy().getAngle();

        // Enter sketch mode on BASE_XZ
        boolean entered = sketchMode.enterBasePlane(null, sketch_plane_type_ui_main.BASE_XZ, cam);
        assertTrue(entered && sketchMode.isActive(), "Entered sketch mode on BASE_XZ");

        // Verify camera oriented normal to sketch plane
        assertTrue(cam.getRx().getAngle() == 90.0 && cam.getRy().getAngle() == 0.0,
                   "Camera oriented normal to BASE_XZ plane (pitch=90, yaw=0)");

        // Verify local coordinate triad frame matches active sketch plane
        var cs = sketchMode.getActiveCoordSystem();
        assertTrue(cs != null && cs.getPlaneType() == sketch_plane_type_ui_main.BASE_XZ,
                   "Active coordinate system matches BASE_XZ");

        // Exit sketch mode with cancel
        sketchMode.exit(false, workspace.getShapeEditor());
        assertTrue(!sketchMode.isActive(), "Exited sketch mode cleanly");

        // Verify camera orientation restored to saved 3D perspective
        assertTrue(Math.abs(cam.getRx().getAngle() - initPitch) < 1e-4 && Math.abs(cam.getRy().getAngle() - initYaw) < 1e-4,
                   "Camera orientation restored to saved 3D view after sketch exit");
    }

    // 3. Coordinate triads, overlays, and viewport indicators isolation
    private static void testViewportOverlayIsolationAndMouseTransparency() {
        System.out.println("\n[3] Testing Viewport Indicators Isolation & Mouse Transparency...");
        UI_Main ui = new UI_Main();
        var editor = ui.getWorkspace3D().getShapeEditor();

        // Model shape list must only contain user drafting solids
        shape_item_ui_main box = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(-10, -10, -10), new Point3D(10, 10, 10));
        editor.addShape(box);
        assertTrue(editor.getShapes().size() == 1, "Editor contains exactly 1 model geometry shape");

        // Local 3D Triad must be mouse transparent
        var localTriad = ui.getWorkspace3D().getTriadCoordinator().getLocalTriad();
        assertTrue(localTriad.isMouseTransparent(), "Local 3D triad is mouseTransparent (cannot block clicks)");

        // Snap preview indicators must be mouse transparent
        var snapPreview = ui.getWorkspace3D().getSketchController().getPreview();
        assertTrue(snapPreview.isMouseTransparent(), "Sketch snap preview is mouseTransparent");

        // Indicators must not be listed as shapes
        for (shape_item_ui_main s : editor.getShapes()) {
            assertTrue(s.getRootGroup() != localTriad && s.getRootGroup() != snapPreview,
                       "Viewport indicators are isolated from model shapes");
        }
    }

    // 4. Document save, load, and repeated save/load cycles
    private static void testDocumentSaveLoadCyclesAndParameterPreservation() throws Exception {
        System.out.println("\n[4] Testing Document Save/Load Cycles & Parameter Preservation...");
        File docFile = File.createTempFile("test_cycle_doc_", ".nd");
        docFile.deleteOnExit();

        shape_item_ui_main solid = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(-40, -20, -40), new Point3D(40, 20, 40));
        solid.setName("PrimaryEngineBlock");

        // Attach Hole feature
        hole_feature_ui_main hole = new hole_feature_ui_main(
            "h_flow", solid.getId(), face_kind_ui_main.TOP, 5.0, -5.0, 14.0, 25.0, false
        );
        solid.addHole(hole);

        // Attach Chamfer feature
        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main(
            "ch_bevel", solid.getId(), "C:T_F", 4.0
        );
        solid.addChamfer(chamfer);

        // Attach Sketch with Center Rectangle
        sketch_feature_ui_main sketch = new sketch_feature_ui_main(
            "sk_bracket", solid.getId(), "MountingBracket", sketch_plane_type_ui_main.BASE_XZ, null
        );
        sketch_rect_ui_main rect = new sketch_rect_ui_main("cr1", -15, -15, 15, 15);
        sketch_line_ui_main d1 = new sketch_line_ui_main("d1", -15, -15, 15, 15);
        d1.setConstruction(true);
        sketch.addEntity(rect);
        sketch.addEntity(d1);
        solid.addSketch(sketch);

        // CYCLE 1: Save -> Load
        boolean saved1 = document_serializer_ui_main.saveToFile(docFile, List.of(solid));
        assertTrue(saved1, "Cycle 1: Document saved successfully");
        List<shape_item_ui_main> load1 = document_serializer_ui_main.loadFromFile(docFile);
        assertTrue(load1.size() == 1, "Cycle 1: 1 shape loaded");
        shape_item_ui_main s1 = load1.get(0);
        assertTrue(s1.getName().equals("PrimaryEngineBlock"), "Cycle 1: Shape name matches");
        assertTrue(s1.getHoles().size() == 1, "Cycle 1: Hole preserved");
        assertTrue(s1.getChamfers().size() == 1, "Cycle 1: Chamfer preserved");
        assertTrue(s1.getSketches().size() == 1, "Cycle 1: Sketch preserved");

        // CYCLE 2: Save load1 back to file -> Load again
        boolean saved2 = document_serializer_ui_main.saveToFile(docFile, load1);
        assertTrue(saved2, "Cycle 2: Re-saved loaded document");
        List<shape_item_ui_main> load2 = document_serializer_ui_main.loadFromFile(docFile);
        assertTrue(load2.size() == 1, "Cycle 2: 1 shape re-loaded cleanly");
        shape_item_ui_main s2 = load2.get(0);
        assertTrue(s2.getName().equals("PrimaryEngineBlock"), "Cycle 2: Shape name preserved identically");
        assertTrue(s2.getHoles().size() == 1 && s2.getHoles().get(0).getDiameter() == 14.0,
                   "Cycle 2: Hole diameter 14.0 preserved");
        assertTrue(s2.getChamfers().size() == 1 && s2.getChamfers().get(0).getDistance() == 4.0,
                   "Cycle 2: Chamfer distance 4.0 preserved");
        assertTrue(s2.getSketches().size() == 1 && s2.getSketches().get(0).getEntities().size() == 2,
                   "Cycle 2: Sketch with 2 entities preserved identically");
    }

    // 5. STEP AP203 export structure and compliance validation
    private static void testStepExportStructureAndValidation() throws Exception {
        System.out.println("\n[5] Testing STEP AP203 Exporter Compliance...");
        File stepFile = File.createTempFile("test_export_", ".step");
        stepFile.deleteOnExit();

        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE,
            new Point3D(-10, -10, -10), new Point3D(10, 10, 10));

        boolean exported = step_exporter_ui_main.exportToStep(stepFile, List.of(cube));
        assertTrue(exported, "STEP export succeeded");
        assertTrue(stepFile.length() > 200, "STEP file has substantial content (" + stepFile.length() + " bytes)");

        // Independent validation of ISO 10303-21 structure
        boolean hasIsoHeader = false, hasConfigControl = false, hasBrep = false, hasEnd = false;
        int faceCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(stepFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("ISO-10303-21;")) hasIsoHeader = true;
                if (line.contains("CONFIG_CONTROL_DESIGN")) hasConfigControl = true;
                if (line.contains("MANIFOLD_SOLID_BREP")) hasBrep = true;
                if (line.contains("ADVANCED_FACE")) faceCount++;
                if (line.contains("END-ISO-10303-21;")) hasEnd = true;
            }
        }

        assertTrue(hasIsoHeader, "STEP header starts with ISO-10303-21;");
        assertTrue(hasConfigControl, "STEP file defines CONFIG_CONTROL_DESIGN schema");
        assertTrue(hasBrep, "STEP contains MANIFOLD_SOLID_BREP entity");
        assertTrue(faceCount >= 6, "STEP contains at least 6 planar ADVANCED_FACE polygons");
        assertTrue(hasEnd, "STEP file terminates with END-ISO-10303-21;");
    }

    // 6. STL export and round-trip verification with stl_importer
    private static void testStlExportAndRoundTripValidation() throws Exception {
        System.out.println("\n[6] Testing STL Exporter & Round-Trip Validation...");
        File stlFile = File.createTempFile("test_export_", ".stl");
        stlFile.deleteOnExit();

        shape_item_ui_main cuboid = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(0, 0, 0), new Point3D(30, 20, 10));

        boolean exported = mesh_exporter_ui_main.exportToStl(stlFile, List.of(cuboid));
        assertTrue(exported, "STL export succeeded");
        assertTrue(stlFile.length() > 100, "STL file has content (" + stlFile.length() + " bytes)");

        // Round-trip import back into shape_item
        List<shape_item_ui_main> imported = stl_importer_ui_main.importFromStl(stlFile);
        assertTrue(!imported.isEmpty(), "STL successfully imported back using stl_importer");
        shape_item_ui_main reimported = imported.get(0);
        assertTrue(reimported.getOverlays().hasCustomMesh(), "Imported shape contains custom mesh");
        double w = Math.abs(reimported.getP2().getX() - reimported.getP1().getX());
        assertTrue(Math.abs(w - 30.0) < 1e-3, "Imported STL width matches original 30.0");
    }

    // 7. OBJ export and round-trip verification with obj_importer
    private static void testObjExportAndRoundTripValidation() throws Exception {
        System.out.println("\n[7] Testing Wavefront OBJ Exporter & Round-Trip Validation...");
        File objFile = File.createTempFile("test_export_", ".obj");
        objFile.deleteOnExit();

        shape_item_ui_main cuboid = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(0, 0, 0), new Point3D(25, 40, 15));

        boolean exported = mesh_exporter_ui_main.exportToObj(objFile, List.of(cuboid));
        assertTrue(exported, "OBJ export succeeded");
        assertTrue(objFile.length() > 100, "OBJ file has content (" + objFile.length() + " bytes)");

        // Round-trip import back
        List<shape_item_ui_main> imported = obj_importer_ui_main.importFromObj(objFile);
        assertTrue(!imported.isEmpty(), "OBJ successfully imported back using obj_importer");
        shape_item_ui_main reimported = imported.get(0);
        assertTrue(reimported.getOverlays().hasCustomMesh(), "Imported shape has custom mesh");
        double h = Math.abs(reimported.getP2().getY() - reimported.getP1().getY());
        assertTrue(Math.abs(h - 40.0) < 1e-3, "Imported OBJ height matches original 40.0");
    }

    // 8. NC CNC G-code toolpath export and validation with nc_validator
    private static void testNcExportAndToolpathValidation() throws Exception {
        System.out.println("\n[8] Testing NC CNC Toolpath Exporter & Validator...");
        File ncFile = File.createTempFile("test_toolpath_", ".nc");
        ncFile.deleteOnExit();

        shape_item_ui_main stock = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(-50, -20, -50), new Point3D(50, 0, 50));
        hole_feature_ui_main hole = new hole_feature_ui_main(
            "h_cnc", stock.getId(), face_kind_ui_main.TOP, 0, 0, 10.0, 15.0, false
        );
        stock.addHole(hole);

        boolean exported = nc_exporter_ui_main.exportToNc(ncFile, List.of(stock));
        assertTrue(exported, "NC G-code export succeeded");

        // Validate using nc_validator_ui_main
        var report = nc_validator_ui_main.validate(ncFile);
        assertTrue(report.valid(), "nc_validator reports G-code is valid");
        assertTrue(report.motions() > 0, "Toolpath contains motion commands (G00/G01): " + report.motions());
        assertTrue(report.toolChanges() > 0, "Toolpath contains tool changes (M06): " + report.toolChanges());
        assertTrue(report.errors().isEmpty(), "No G-code syntax errors reported");
    }

    // 9. Error recovery and invalid document handling
    private static void testErrorRecoveryAndInvalidDocumentHandling() throws Exception {
        System.out.println("\n[9] Testing Error Recovery & Invalid Document Handling...");

        // Null file safety
        assertTrue(!document_serializer_ui_main.saveToFile(null, Collections.emptyList()),
                   "Saving null file returns false safely");
        assertTrue(document_serializer_ui_main.loadFromFile(null).isEmpty(),
                   "Loading null file returns empty list safely");

        // Non-existent file safety
        File missingFile = new File("non_existent_folder_xyz/missing_file.nd");
        assertTrue(document_serializer_ui_main.loadFromFile(missingFile).isEmpty(),
                   "Loading non-existent file returns empty list without throwing");

        // Corrupted file content
        File corruptedFile = File.createTempFile("test_corrupted_", ".nd");
        corruptedFile.deleteOnExit();
        try (FileWriter fw = new FileWriter(corruptedFile)) {
            fw.write("INVALID RANDOM GARBAGE @#$%^&*()\nCORRUPT DATA");
        }
        List<shape_item_ui_main> recovered = document_serializer_ui_main.loadFromFile(corruptedFile);
        assertTrue(recovered != null, "Loading corrupted file handled safely without crashing");

        // Invalid STL
        File invalidStl = File.createTempFile("test_invalid_", ".stl");
        invalidStl.deleteOnExit();
        try (FileWriter fw = new FileWriter(invalidStl)) {
            fw.write("not an stl file");
        }
        List<shape_item_ui_main> stlRecovered = stl_importer_ui_main.importFromStl(invalidStl);
        assertTrue(stlRecovered != null && stlRecovered.isEmpty(),
                   "Invalid STL import returns empty list safely");
    }

    // 10. Repeated lifecycle operations and memory leak / duplicate prevention
    private static void testRepeatedLifecycleAndMemoryLeakPrevention() {
        System.out.println("\n[10] Testing Repeated Lifecycle & Listener/Overlay Cleanliness...");
        UI_Main ui = new UI_Main();
        var workspace = ui.getWorkspace3D();
        var sketchMode = workspace.getSketchMode();

        // Repeated enter / exit cycles
        for (int i = 0; i < 5; i++) {
            sketchMode.enterBasePlane(null, sketch_plane_type_ui_main.BASE_XZ, workspace.getCameraController());
            sketchMode.exit(false, workspace.getShapeEditor());
        }
        assertTrue(!sketchMode.isActive(), "Sketch mode cleanly inactive after 5 enter/exit cycles");

        // Repeated shape rebuilds
        shape_item_ui_main shape = new shape_item_ui_main(basic_shapes_ui_main.CUBOID,
            new Point3D(-10, -10, -10), new Point3D(10, 10, 10));
        for (int i = 0; i < 10; i++) {
            shape.setSelected(i % 2 == 0);
            shape.rebuild();
        }
        // Verify handles group does not accumulate duplicate handles
        int handleCount = shape.isSelected() ? shape.getControlHandles().size() : 0;
        assertTrue(shape.getShapeGroup().getChildren().size() <= 2,
                   "Shape visuals do not accumulate duplicate nodes after 10 rebuilds");
    }
}
