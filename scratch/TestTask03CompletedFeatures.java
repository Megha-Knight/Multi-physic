package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.TriangleMesh;
import application.application_error_handler_ui_main;
import ui.File_Types.obj_importer_ui_main;
import ui.File_Types.stl_importer_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sketch.sketch_entity_ui_main;
import ui.workspace.drafting.sketch.sketch_line_ui_main;
import ui.workspace.drafting.sketch.sketch_point_2d_ui_main;
import ui.workspace.drafting.sketch.sketch_profile_analyzer_ui_main;
import ui.workspace.drafting.sketch.sketch_profile_loop_ui_main;
import ui.workspace.drafting.sketch.sketch_profile_type_ui_main;
import ui.workspace.drafting.sketch.sketch_snap_glyph_helper_ui_main;
import ui.workspace.drafting.sketch.sketch_snap_type_ui_main;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TestTask03CompletedFeatures {

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
        System.out.println("  TASK 03 COMPLETED FEATURES VERIFICATION SUITE  ");
        System.out.println("=================================================");

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already started
        }

        try {
            testStlAsciiImport();
            testStlBinaryImport();
            testObjImport();
            testCenterRectangleAndConstructionGeometry();
            testProfileAnalyzerFiltersConstructionGeometry();
            testSnapGlyphGeneration();
            testGlobalExceptionHandler();

            System.out.println("\n-------------------------------------------------");
            System.out.printf("ALL TESTS PASSED: %d / %d assertions passed cleanly.%n", testsPassed, testsTotal);
            System.out.println("-------------------------------------------------");
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("\nTEST SUITE FAILED with error:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testStlAsciiImport() throws Exception {
        System.out.println("\n[Test 1] Testing ASCII STL Importer...");
        String asciiStl =
            "solid test_triangle\n" +
            "  facet normal 0.0 0.0 1.0\n" +
            "    outer loop\n" +
            "      vertex 0.0 0.0 0.0\n" +
            "      vertex 10.0 0.0 0.0\n" +
            "      vertex 0.0 10.0 0.0\n" +
            "    endloop\n" +
            "  endfacet\n" +
            "endsolid test_triangle\n";

        File tempFile = File.createTempFile("test_ascii_", ".stl");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(asciiStl.getBytes(StandardCharsets.UTF_8));
        }

        List<shape_item_ui_main> shapes = stl_importer_ui_main.importFromStl(tempFile);
        assertTrue(!shapes.isEmpty(), "shape_item_ui_main loaded from ASCII STL");
        shape_item_ui_main shape = shapes.get(0);
        assertTrue(shape.getOverlays().hasCustomMesh(), "Shape item contains custom mesh");
        TriangleMesh mesh = shape.getOverlays().getCustomMesh();
        assertTrue(mesh != null, "Mesh successfully created from ASCII STL");
        assertTrue(mesh.getPoints().size() == 9, "Mesh has 3 vertices (9 float coordinates)");
        assertTrue(mesh.getFaces().size() == 6, "Mesh has 1 triangle face (6 indices)");

        double width = Math.abs(shape.getP2().getX() - shape.getP1().getX());
        double height = Math.abs(shape.getP2().getY() - shape.getP1().getY());
        assertTrue(Math.abs(width - 10.0) < 1e-4, "Shape width accurately calculated (10.0)");
        assertTrue(Math.abs(height - 10.0) < 1e-4, "Shape height accurately calculated (10.0)");
        assertTrue(shape.getName().contains("test_ascii_"), "Shape name contains filename");
    }

    private static void testStlBinaryImport() throws Exception {
        System.out.println("\n[Test 2] Testing Binary STL Importer...");
        File tempFile = File.createTempFile("test_binary_", ".stl");
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] header = new byte[80];
            fos.write(header);
            ByteBuffer countBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            countBuf.putInt(2); // 2 facets
            fos.write(countBuf.array());

            // Facet 1: (0,0,0), (10,0,0), (0,10,0)
            ByteBuffer facetBuf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN);
            facetBuf.putFloat(0f); facetBuf.putFloat(0f); facetBuf.putFloat(1f); // normal
            facetBuf.putFloat(0f); facetBuf.putFloat(0f); facetBuf.putFloat(0f); // v1
            facetBuf.putFloat(10f); facetBuf.putFloat(0f); facetBuf.putFloat(0f); // v2
            facetBuf.putFloat(0f); facetBuf.putFloat(10f); facetBuf.putFloat(0f); // v3
            facetBuf.putShort((short) 0);
            fos.write(facetBuf.array());

            // Facet 2: (10,0,0), (10,10,0), (0,10,0)
            facetBuf.clear();
            facetBuf.putFloat(0f); facetBuf.putFloat(0f); facetBuf.putFloat(1f); // normal
            facetBuf.putFloat(10f); facetBuf.putFloat(0f); facetBuf.putFloat(0f); // v1
            facetBuf.putFloat(10f); facetBuf.putFloat(10f); facetBuf.putFloat(0f); // v2
            facetBuf.putFloat(0f); facetBuf.putFloat(10f); facetBuf.putFloat(0f); // v3
            facetBuf.putShort((short) 0);
            fos.write(facetBuf.array());
        }

        List<shape_item_ui_main> shapes = stl_importer_ui_main.importFromStl(tempFile);
        assertTrue(!shapes.isEmpty(), "shape_item_ui_main loaded from Binary STL");
        shape_item_ui_main shape = shapes.get(0);
        assertTrue(shape.getOverlays().hasCustomMesh(), "Binary STL shape has custom mesh");
        TriangleMesh mesh = shape.getOverlays().getCustomMesh();
        assertTrue(mesh.getPoints().size() == 18, "Mesh has 6 vertices (18 float coordinates)");
        assertTrue(mesh.getFaces().size() == 12, "Mesh has 2 triangle faces (12 indices)");

        double width = Math.abs(shape.getP2().getX() - shape.getP1().getX());
        double height = Math.abs(shape.getP2().getY() - shape.getP1().getY());
        assertTrue(Math.abs(width - 10.0) < 1e-4, "Binary STL width is 10.0");
        assertTrue(Math.abs(height - 10.0) < 1e-4, "Binary STL height is 10.0");
    }

    private static void testObjImport() throws Exception {
        System.out.println("\n[Test 3] Testing Wavefront OBJ Importer...");
        String objContent =
            "# Wavefront OBJ test quad\n" +
            "v 0.0 0.0 0.0\n" +
            "v 20.0 0.0 0.0\n" +
            "v 20.0 30.0 0.0\n" +
            "v 0.0 30.0 0.0\n" +
            "f 1 2 3 4\n";

        File tempFile = File.createTempFile("test_obj_", ".obj");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(objContent.getBytes(StandardCharsets.UTF_8));
        }

        List<shape_item_ui_main> shapes = obj_importer_ui_main.importFromObj(tempFile);
        assertTrue(!shapes.isEmpty(), "shape_item_ui_main loaded from OBJ");
        shape_item_ui_main shape = shapes.get(0);
        assertTrue(shape.getOverlays().hasCustomMesh(), "OBJ shape has custom mesh");
        TriangleMesh mesh = shape.getOverlays().getCustomMesh();
        assertTrue(mesh.getPoints().size() == 12, "Mesh has 4 vertices (12 coordinates)");
        assertTrue(mesh.getFaces().size() == 12, "Mesh quad triangulated into 2 triangles (12 indices)");

        double width = Math.abs(shape.getP2().getX() - shape.getP1().getX());
        double height = Math.abs(shape.getP2().getY() - shape.getP1().getY());
        assertTrue(Math.abs(width - 20.0) < 1e-4, "OBJ width is 20.0");
        assertTrue(Math.abs(height - 30.0) < 1e-4, "OBJ height is 30.0");
    }

    private static void testCenterRectangleAndConstructionGeometry() {
        System.out.println("\n[Test 4] Testing Center-Rectangle Geometry & Construction Lines...");
        List<sketch_entity_ui_main> list = new ArrayList<>();
        sketch_point_2d_ui_main center = new sketch_point_2d_ui_main(10.0, 20.0);
        sketch_point_2d_ui_main corner = new sketch_point_2d_ui_main(30.0, 50.0);

        // Center rect symmetry math:
        double dx = Math.abs(corner.x() - center.x());
        double dy = Math.abs(corner.y() - center.y());
        double minX = center.x() - dx;
        double maxX = center.x() + dx;
        double minY = center.y() - dy;
        double maxY = center.y() + dy;

        sketch_point_2d_ui_main p1 = new sketch_point_2d_ui_main(minX, minY);
        sketch_point_2d_ui_main p2 = new sketch_point_2d_ui_main(maxX, minY);
        sketch_point_2d_ui_main p3 = new sketch_point_2d_ui_main(maxX, maxY);
        sketch_point_2d_ui_main p4 = new sketch_point_2d_ui_main(minX, maxY);

        sketch_line_ui_main e1 = new sketch_line_ui_main("e1", p1, p2);
        sketch_line_ui_main e2 = new sketch_line_ui_main("e2", p2, p3);
        sketch_line_ui_main e3 = new sketch_line_ui_main("e3", p3, p4);
        sketch_line_ui_main e4 = new sketch_line_ui_main("e4", p4, p1);

        sketch_line_ui_main d1 = new sketch_line_ui_main("d1", p1, p3);
        d1.setConstruction(true);
        sketch_line_ui_main d2 = new sketch_line_ui_main("d2", p2, p4);
        d2.setConstruction(true);

        list.add(e1); list.add(e2); list.add(e3); list.add(e4);
        list.add(d1); list.add(d2);

        assertTrue(list.size() == 6, "List has 6 entities (4 edges + 2 diagonals)");
        assertTrue(!e1.isConstruction() && !e2.isConstruction() && !e3.isConstruction() && !e4.isConstruction(),
                   "4 perimeter edges are solid model geometry");
        assertTrue(d1.isConstruction() && d2.isConstruction(),
                   "2 diagonals are flagged as construction geometry");
    }

    private static void testProfileAnalyzerFiltersConstructionGeometry() {
        System.out.println("\n[Test 5] Testing Profile Analyzer Filters Out Construction Diagonals...");
        List<sketch_entity_ui_main> entities = new ArrayList<>();
        sketch_point_2d_ui_main p1 = new sketch_point_2d_ui_main(0, 0);
        sketch_point_2d_ui_main p2 = new sketch_point_2d_ui_main(100, 0);
        sketch_point_2d_ui_main p3 = new sketch_point_2d_ui_main(100, 50);
        sketch_point_2d_ui_main p4 = new sketch_point_2d_ui_main(0, 50);

        // 4 boundary lines
        entities.add(new sketch_line_ui_main("l1", p1, p2));
        entities.add(new sketch_line_ui_main("l2", p2, p3));
        entities.add(new sketch_line_ui_main("l3", p3, p4));
        entities.add(new sketch_line_ui_main("l4", p4, p1));

        // 2 construction diagonals intersecting in the middle
        sketch_line_ui_main diag1 = new sketch_line_ui_main("d1", p1, p3);
        diag1.setConstruction(true);
        sketch_line_ui_main diag2 = new sketch_line_ui_main("d2", p2, p4);
        diag2.setConstruction(true);
        entities.add(diag1);
        entities.add(diag2);

        sketch_profile_analyzer_ui_main.AnalysisResult result = sketch_profile_analyzer_ui_main.analyze(entities);

        assertTrue(result.isValid(), "Profile result is valid and extrudable");
        assertTrue(result.type() == sketch_profile_type_ui_main.CLOSED,
                   "Profile type is CLOSED");
        assertTrue(result.loops().size() == 1,
                   "Profile analyzer finds exactly 1 closed loop, ignoring construction lines");
        sketch_profile_loop_ui_main loop = result.outerLoop();
        assertTrue(loop != null, "Outer loop exists");
        assertTrue(loop.getVertices().size() == 4, "Loop contains exactly the 4 boundary vertices");
        double area = loop.getAbsArea();
        assertTrue(Math.abs(area - 5000.0) < 1e-4, "Loop area is 100 * 50 = 5000.0");
    }

    private static void testSnapGlyphGeneration() {
        System.out.println("\n[Test 6] Testing CAD-Standard Snapping Glyphs...");
        Point3D pt = new Point3D(10, 20, 0);

        Node glyphEndpoint = sketch_snap_glyph_helper_ui_main.createGlyph(pt, sketch_snap_type_ui_main.ENDPOINT);
        assertTrue(glyphEndpoint instanceof Group && ((Group) glyphEndpoint).getChildren().get(0) instanceof Rectangle,
                   "Endpoint snap produces a Square glyph");

        Node glyphMidpoint = sketch_snap_glyph_helper_ui_main.createGlyph(pt, sketch_snap_type_ui_main.MIDPOINT);
        assertTrue(glyphMidpoint instanceof Group && ((Group) glyphMidpoint).getChildren().get(0) instanceof Polygon,
                   "Midpoint snap produces a Triangle glyph");

        Node glyphCenter = sketch_snap_glyph_helper_ui_main.createGlyph(pt, sketch_snap_type_ui_main.CENTER);
        assertTrue(glyphCenter instanceof Group && ((Group) glyphCenter).getChildren().size() == 3,
                   "Center snap produces a Crosshair Circle glyph (circle + 2 lines)");

        Node glyphOrigin = sketch_snap_glyph_helper_ui_main.createGlyph(pt, sketch_snap_type_ui_main.ORIGIN);
        assertTrue(glyphOrigin instanceof Group && ((Group) glyphOrigin).getChildren().size() == 2,
                   "Origin snap produces a Double Concentric Circle glyph");

        Node glyphIntersection = sketch_snap_glyph_helper_ui_main.createGlyph(pt, sketch_snap_type_ui_main.INTERSECTION);
        assertTrue(glyphIntersection instanceof Group && ((Group) glyphIntersection).getChildren().size() == 2,
                   "Intersection snap produces an 'X' Cross glyph (2 diagonal lines)");
    }

    private static void testGlobalExceptionHandler() {
        System.out.println("\n[Test 7] Testing Global Uncaught Exception Handler...");
        RuntimeException testEx = new RuntimeException("Test Diagnostic Error from Task 03");
        try {
            application_error_handler_ui_main handler = new application_error_handler_ui_main();
            handler.uncaughtException(Thread.currentThread(), testEx);
            assertTrue(true, "application_error_handler_ui_main handled exception safely without crashing JVM");
        } catch (Throwable t) {
            assertTrue(false, "application_error_handler_ui_main threw an unexpected exception: " + t.getMessage());
        }
    }
}
