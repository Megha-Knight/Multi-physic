package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestPhase3ActualCadBehavior {

    private static int passed = 0;
    private static int failed = 0;

    private static void assertTrue(boolean cond, String msg) {
        if (cond) {
            System.out.println("  [PASS] " + msg);
            passed++;
        } else {
            System.err.println("  [FAIL] " + msg);
            failed++;
            throw new AssertionError("Assertion failed: " + msg);
        }
    }

    private static void assertEquals(double expected, double actual, double eps, String msg) {
        if (Math.abs(expected - actual) <= eps) {
            System.out.println(String.format("  [PASS] %s (expected=%.3f, actual=%.3f)", msg, expected, actual));
            passed++;
        } else {
            System.err.println(String.format("  [FAIL] %s (expected=%.3f, actual=%.3f)", msg, expected, actual));
            failed++;
            throw new AssertionError("Assertion failed: " + msg);
        }
    }

    public static void main(String[] args) {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        System.out.println("=================================================");
        System.out.println("Running MultiPhysics Phase 3 CAD Behavior Tests");
        System.out.println("=================================================");

        test1_ExtrusionOnCylinderScreenshotScenario();
        test2_ExtrusionOnTriangle();
        test3_ExtrusionOnRectangle();
        test4_RepeatedRebuildsWithoutDuplicateGeometry();
        test5_FeatureTreeStructure();
        test6_FilletOnSupportedEdge();
        test7_ChamferOnSupportedEdge();
        test8_PositiveAndNegativeDraft();
        test9_MachiningCombinedWithHoles();
        test10_SaveLoadAndExport();
        test11_FeatureManagerSelectionAndRefreshStability();

        System.out.println("=================================================");
        System.out.println(String.format("PHASE 3 RESULTS: %d/%d Passed (%d Failed)", passed, passed + failed, failed));
        System.out.println("=================================================");

        if (failed > 0) System.exit(1);
    }

    // 1. One extrusion on a cylinder, matching the supplied screenshot scenario.
    private static void test1_ExtrusionOnCylinderScreenshotScenario() {
        System.out.println("\n--- 1. Extrusion on Circle Profile (Screenshot Scenario) ---");
        // Drafting an extrusion with circle profile creates a 2D CIRCLE with an Extrude feature.
        Point3D start = new Point3D(0, 0, 0);
        Point3D hit = new Point3D(20, 0, 0);
        shape_item_ui_main circleProfile = new shape_item_ui_main(basic_shapes_ui_main.CIRCLE, start, hit);
        double rad = start.distance(hit);
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
            "ext_cyl_1", circleProfile.getId(), "Extrusion 1", face_kind_ui_main.TOP,
            CutoutShape.CIRCLE, 0.0, 0.0, rad * 2.0, rad * 2.0, 30.0
        );
        circleProfile.addExtrude(ext);
        circleProfile.rebuild();

        // Must produce exactly ONE body node in shapeGroup (the extruded cylinder), not blue cylinder + yellow cylinder
        List<Node> children = circleProfile.getShapeGroup().getChildren();
        assertTrue(children.size() == 1, "Must contain exactly 1 scene geometry node, got: " + children.size());
        Node geo = children.get(0);
        assertTrue(geo instanceof Cylinder, "Extruded circle node must be Cylinder");
        Cylinder cyl = (Cylinder) geo;
        assertEquals(rad, cyl.getRadius(), 0.01, "Cylinder radius must match drawn radius");
        assertEquals(30.0, cyl.getHeight(), 0.01, "Cylinder height must match extrudeHeight");
        javafx.scene.transform.Translate tr = (javafx.scene.transform.Translate) cyl.getTransforms().get(0);
        assertEquals(0.0, tr.getX(), 0.01, "Center X transform must be at origin");
        assertEquals(-15.0, tr.getY(), 0.01, "Cylinder center Y transform must be at -height/2 (-15.0)");
    }

    // 2. One extrusion on a triangle.
    private static void test2_ExtrusionOnTriangle() {
        System.out.println("\n--- 2. Extrusion on Triangle ---");
        shape_item_ui_main tri = new shape_item_ui_main(basic_shapes_ui_main.EQUILATERAL_TRIANGLE, new Point3D(0, 0, 0), new Point3D(30, 0, 30));
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
            "ext_tri", tri.getId(), "Extrusion Tri", face_kind_ui_main.TOP,
            CutoutShape.EQUILATERAL_TRIANGLE, 0.0, 0.0, 30.0, 30.0, 40.0
        );
        tri.addExtrude(ext);
        tri.rebuild();

        List<Node> children = tri.getShapeGroup().getChildren();
        assertTrue(children.size() == 1, "Must contain exactly 1 triangular prism node");
        assertTrue(children.get(0) instanceof MeshView, "Prism node must be MeshView");
        MeshView mv = (MeshView) children.get(0);
        TriangleMesh mesh = (TriangleMesh) mv.getMesh();
        assertTrue(mesh.getPoints().size() / 3 == 6, "Triangular prism must have exactly 6 vertices");
        assertTrue(mesh.getFaces().size() / 6 == 8, "Triangular prism must have exactly 8 faces");
    }

    // 3. One extrusion on a rectangle.
    private static void test3_ExtrusionOnRectangle() {
        System.out.println("\n--- 3. Extrusion on Rectangle ---");
        shape_item_ui_main rect = new shape_item_ui_main(basic_shapes_ui_main.RECTANGLE, new Point3D(0, 0, 0), new Point3D(40, 0, 20));
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
            "ext_rect", rect.getId(), "Extrusion Rect", face_kind_ui_main.TOP,
            CutoutShape.RECTANGLE, 0.0, 0.0, 40.0, 20.0, 25.0
        );
        rect.addExtrude(ext);
        rect.rebuild();

        List<Node> children = rect.getShapeGroup().getChildren();
        assertTrue(children.size() == 1, "Must contain exactly 1 box node");
        assertTrue(children.get(0) instanceof javafx.scene.shape.Box, "Extruded rectangle must be Box");
        javafx.scene.shape.Box b = (javafx.scene.shape.Box) children.get(0);
        assertEquals(40.0, b.getWidth(), 0.01, "Box width must equal 40.0");
        assertEquals(20.0, b.getDepth(), 0.01, "Box depth must equal 20.0");
        assertEquals(25.0, b.getHeight(), 0.01, "Box height must equal 25.0");
    }

    // 4. Repeated rebuilds without duplicate geometry.
    private static void test4_RepeatedRebuildsWithoutDuplicateGeometry() {
        System.out.println("\n--- 4. Repeated Rebuilds Without Duplicate Geometry ---");
        shape_item_ui_main item = new shape_item_ui_main(basic_shapes_ui_main.CIRCLE, new Point3D(0, 0, 0), new Point3D(15, 0, 0));
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
            "ext_rep", item.getId(), "Extrusion", face_kind_ui_main.TOP,
            CutoutShape.CIRCLE, 0.0, 0.0, 30.0, 30.0, 20.0
        );
        item.addExtrude(ext);

        for (int i = 0; i < 10; i++) {
            item.rebuild();
            assertTrue(item.getShapeGroup().getChildren().size() == 1, "Iteration " + i + ": exactly 1 node expected");
        }
    }

    // 5. Correct feature-tree parent/child structure.
    private static void test5_FeatureTreeStructure() {
        System.out.println("\n--- 5. Feature Tree Structure ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, 40, 40));
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
            "ext_child", host.getId(), "Boss Extrude 1", face_kind_ui_main.TOP,
            CutoutShape.CIRCLE, 0.0, 0.0, 16.0, 16.0, 15.0
        );
        host.addExtrude(ext);

        assertTrue(ext.getOwnerShapeId().equals(host.getId()), "Extrude ownerShapeId matches host ID");
        assertTrue(ext.revalidate(host), "Extrude revalidates cleanly against host");
        assertTrue(host.getExtrusions().contains(ext), "Host contains child extrusion feature");
    }

    // 6. Fillet on a supported edge.
    private static void test6_FilletOnSupportedEdge() {
        System.out.println("\n--- 6. Fillet on Supported Edge ---");
        shape_item_ui_main box = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, 40, 40));
        fillet_feature_ui_main fil = new fillet_feature_ui_main("fil_top_front", box.getId(), "TOP_FRONT", 4.0);
        box.addFillet(fil);
        box.rebuild();

        Node solid = ui.workspace.drafting.machining.machining_solid_mesh_builder_ui_main.buildSolid(box, false);
        assertTrue(solid instanceof MeshView, "Solid builder returns MeshView for fillet");
        MeshView mv = (MeshView) solid;
        TriangleMesh mesh = (TriangleMesh) mv.getMesh();
        assertTrue(mesh.getPoints().size() > 24, "Mesh contains filleted curved edge vertices, found: " + mesh.getPoints().size() / 3);
        assertTrue(mesh.getFaces().size() / 6 > 12, "Mesh contains rounded face strips, found: " + mesh.getFaces().size() / 6);
    }

    // 7. Chamfer on a supported edge.
    private static void test7_ChamferOnSupportedEdge() {
        System.out.println("\n--- 7. Chamfer on Supported Edge ---");
        shape_item_ui_main box = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, 40, 40));
        chamfer_feature_ui_main ch = new chamfer_feature_ui_main("ch_top_back", box.getId(), "TOP_BACK", 3.0);
        box.addChamfer(ch);
        box.rebuild();

        Node solid = ui.workspace.drafting.machining.machining_solid_mesh_builder_ui_main.buildSolid(box, false);
        assertTrue(solid instanceof MeshView, "Solid builder returns MeshView for chamfer");
        MeshView mv = (MeshView) solid;
        TriangleMesh mesh = (TriangleMesh) mv.getMesh();
        assertTrue(mesh.getPoints().size() >= 24, "Mesh contains chamfer vertices");
        assertTrue(mesh.getFaces().size() / 6 >= 14, "Mesh contains bevel face facets");
    }

    // 8. Positive and negative draft.
    private static void test8_PositiveAndNegativeDraft() {
        System.out.println("\n--- 8. Positive and Negative Draft ---");
        shape_item_ui_main boxPos = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, 40, 40));
        draft_feature_ui_main dPos = new draft_feature_ui_main("d_pos", boxPos.getId(), face_kind_ui_main.FRONT, 5.0);
        boxPos.addDraft(dPos);
        assertTrue(dPos.revalidate(boxPos), "Positive draft revalidates");
        Node solidPos = ui.workspace.drafting.machining.machining_solid_mesh_builder_ui_main.buildSolid(boxPos, false);
        assertTrue(solidPos != null, "Positive draft solid mesh built");

        shape_item_ui_main boxNeg = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, 40, 40));
        draft_feature_ui_main dNeg = new draft_feature_ui_main("d_neg", boxNeg.getId(), face_kind_ui_main.FRONT, -5.0);
        boxNeg.addDraft(dNeg);
        assertTrue(dNeg.revalidate(boxNeg), "Negative draft revalidates");
        Node solidNeg = ui.workspace.drafting.machining.machining_solid_mesh_builder_ui_main.buildSolid(boxNeg, false);
        assertTrue(solidNeg != null, "Negative draft solid mesh built");
    }

    // 9. Machining combined with holes.
    private static void test9_MachiningCombinedWithHoles() {
        System.out.println("\n--- 9. Machining Combined with Holes ---");
        shape_item_ui_main box = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(60, 40, 60));
        hole_feature_ui_main hole = new hole_feature_ui_main("h_top", box.getId(), face_kind_ui_main.TOP, 0, 0, 10.0, 20.0, false);
        box.addHole(hole);
        fillet_feature_ui_main fil = new fillet_feature_ui_main("fil_co", box.getId(), "BOTTOM_FRONT", 3.0);
        box.addFillet(fil);
        box.rebuild();

        assertTrue(box.hasHoles(), "Box retains hole feature");
        assertTrue(box.getFillets().size() == 1, "Box retains fillet feature");
        assertTrue(box.getShapeGroup().getChildren().size() >= 3, "ShapeGroup has solid with holes, bore walls, and machining");
    }

    // 10. Save/load and export after extrusion and machining.
    private static void test10_SaveLoadAndExport() {
        System.out.println("\n--- 10. Save/Load and Export ---");
        shape_item_ui_main item = new shape_item_ui_main(basic_shapes_ui_main.CIRCLE, new Point3D(0, 0, 0), new Point3D(20, 0, 0));
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
            "ext_save", item.getId(), "Extrude Circle", face_kind_ui_main.TOP,
            CutoutShape.CIRCLE, 0.0, 0.0, 40.0, 40.0, 35.0
        );
        item.addExtrude(ext);
        List<shape_item_ui_main> shapes = List.of(item);

        File f = new File("scratch/test_phase3_save.nd");
        ui.File_Types.document_serializer_ui_main.saveToFile(f, shapes);
        assertTrue(f.exists() && f.length() > 0, "Document file saved successfully");

        List<shape_item_ui_main> loaded = ui.File_Types.document_serializer_ui_main.loadFromFile(f);
        assertTrue(loaded.size() == 1, "Loaded exactly 1 shape");
        shape_item_ui_main loadedShape = loaded.get(0);
        assertTrue(loadedShape.hasExtrusions(), "Loaded shape retains extrusion");
        assertEquals(35.0, loadedShape.getExtrusions().get(0).getHeight(), 0.01, "Loaded extrusion height preserved");
        f.delete();
    }

    // 11. Feature Manager selection and refresh stability.
    private static void test11_FeatureManagerSelectionAndRefreshStability() {
        System.out.println("\n--- 11. Feature Manager Selection & Refresh Stability ---");
        javafx.scene.Group container = new javafx.scene.Group();
        javafx.scene.layout.Pane viewport = new javafx.scene.layout.Pane();
        javafx.scene.PerspectiveCamera camera = new javafx.scene.PerspectiveCamera();
        shape_editor_ui_main editor = new shape_editor_ui_main(
            container, viewport, null, null, e -> Point3D.ZERO, new javafx.scene.control.Label(), () -> false, camera
        );
        ui.featuremanager.featuremanager_ui_main fm = new ui.featuremanager.featuremanager_ui_main();
        shape_item_ui_main s1 = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(20, 20, 20));
        shape_item_ui_main s2 = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(50, 0, 50), new Point3D(70, 40, 50));
        editor.addShape(s1);
        editor.addShape(s2);

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];

        Platform.runLater(() -> {
            try {
                fm.bindToEditor(editor);
                for (int i = 0; i < 50; i++) {
                    editor.selectShape(i % 2 == 0 ? s1 : s2);
                }
            } catch (Throwable t) {
                err[0] = t;
            } finally {
                latch.countDown();
            }
        });

        try {
            boolean ok = latch.await(5, TimeUnit.SECONDS);
            assertTrue(ok, "Platform.runLater finished within 5s");
            assertTrue(err[0] == null, "Feature manager rapid selection completed without error");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
