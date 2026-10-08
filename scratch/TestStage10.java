package scratch;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.File_Types.document_serializer_ui_main;
import ui.featuremanager.feature_tree_node_ui_main;
import ui.workspace.drafting.booleans.boolean_feature_ui_main;
import ui.workspace.drafting.booleans.boolean_op_type_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.features.feature_type_ui_main;
import ui.workspace.drafting.loft.loft_feature_ui_main;
import ui.workspace.drafting.loft.loft_mesh_builder_ui_main;
import ui.workspace.drafting.loft.loft_solid_evaluator_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.profiles.profile_loop_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.profiles.profile_validator_ui_main;
import ui.workspace.drafting.shape_history_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.shell.shell_direction_ui_main;
import ui.workspace.drafting.shell.shell_feature_ui_main;
import ui.workspace.drafting.sweep.sweep_feature_ui_main;
import ui.workspace.drafting.sweep.sweep_mesh_builder_ui_main;
import ui.workspace.drafting.sweep.sweep_orientation_ui_main;
import ui.workspace.drafting.sweep.sweep_path_ui_main;
import ui.workspace.drafting.sweep.sweep_solid_evaluator_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_signature_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * TestStage10.java
 * Comprehensive Verification Test Suite for Stage 10: Loft / Sweep / Advanced Feature-Based Solid Creation.
 */
public class TestStage10 {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Running Stage 10 Test Suite: Loft & Sweep Solid Modeling");
        System.out.println("=================================================");

        try {
            testProfileLoopCreationAndResampling();
            testProfileReferenceCreation();
            testProfileValidation();
            testLoftFeatureModel();
            testTwoSectionLoftMesh();
            testMultiSectionLoftMesh();
            testLoftCapsAndWinding();
            testLoftTopologyIntegration();
            testSweepFeatureModel();
            testSweepPathAndResampling();
            testSweepSolidMeshGeneration();
            testSweepOrientationModes();
            testSweepTopologyIntegration();
            testFeatureManagerIntegration();
            testSelectionAndProvenance();
            testParameterModificationAndRebuild();
            testSpatialTransforms();
            testSerializationSaveLoad();
            testUndoRedoLifecycle();
            testDeterministicSignatures();
            testBooleanSolidInteractions();
            testShellInteractions();
            testMachiningFeatureInteractions();
            testMultiBodyIsolation();
            testDegenerateAndEdgeCaseFailures();
        } catch (Exception e) {
            System.err.println("[CRITICAL ERROR] Test suite aborted with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.printf("STAGE 10 RESULTS: %d/%d Passed (%d Failed)\n", passed, (passed + failed), failed);
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        } else {
            System.out.println("All Stage 10 Loft and Sweep Solid Modeling tests passed successfully!");
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("[PASS] " + description);
            passed++;
        } else {
            System.out.println("[FAIL] " + description);
            failed++;
        }
    }

    private static void testProfileLoopCreationAndResampling() {
        System.out.println("\n--- 1. Profile Loop Creation & Resampling ---");

        List<Point3D> squarePts = List.of(
            new Point3D(0, 0, 0),
            new Point3D(40, 0, 0),
            new Point3D(40, 0, 40),
            new Point3D(0, 0, 40)
        );
        profile_loop_ui_main loop = new profile_loop_ui_main(squarePts, true);

        check(loop.getPointCount() == 4, "Loop vertex count is 4");
        check(loop.isClosed(), "Loop is marked closed");
        check(Math.abs(loop.computeArea() - 1600.0) < 1e-3, "Loop area is 1600.0 for 40x40 square");

        Point3D normal = loop.getNormal();
        check(Math.abs(Math.abs(normal.getY()) - 1.0) < 1e-3, "Loop plane normal points along Y axis");

        profile_loop_ui_main resampled = loop.resample(8);
        check(resampled.getPointCount() == 8, "Resampled loop vertex count is 8");
        check(resampled.isClosed(), "Resampled loop remains closed");
        check(Math.abs(resampled.computeArea() - 1600.0) < 50.0, "Resampled loop area approximates original area");

        profile_reference_ui_main circProf = profile_reference_ui_main.createCircle("circ_loop", "Circ Loop", 20.0, 0.0, 16);
        profile_loop_ui_main circleLoop = circProf.getLoop();
        check(circleLoop.getPointCount() == 16, "Circle loop has 16 vertices");
        check(circleLoop.isClosed(), "Circle loop is closed");
        check(circleLoop.computeArea() > 1100.0 && circleLoop.computeArea() < 1300.0, "Circle loop area matches pi*r^2");
    }

    private static void testProfileReferenceCreation() {
        System.out.println("\n--- 2. Profile Reference Creation ---");

        profile_reference_ui_main rectProf = profile_reference_ui_main.createRectangle("rect_ref", "Rect Ref", 40, 30, 0.0);
        check(rectProf.getId().equals("rect_ref"), "Rectangle profile reference ID matches");
        check(rectProf.getType() == profile_reference_ui_main.ProfileType.RECTANGLE, "Profile type is RECTANGLE");
        check(rectProf.getLoop().isClosed(), "Rectangle profile is closed");
        check(rectProf.getLoop().getPointCount() == 4, "Rectangle profile loop has 4 vertices");

        profile_reference_ui_main circProf = profile_reference_ui_main.createCircle("circ_ref", "Circ Ref", 15.0, 50.0, 16);
        check(circProf.getId().equals("circ_ref"), "Circle profile reference ID matches");
        check(circProf.getType() == profile_reference_ui_main.ProfileType.CIRCLE, "Profile type is CIRCLE");
        check(circProf.getLoop().isClosed(), "Circle profile is closed");
        check(circProf.getElevation() == 50.0, "Circle profile elevation is 50.0");

        List<Point3D> triPts = List.of(new Point3D(0, 0, 0), new Point3D(30, 0, 0), new Point3D(15, 0, 25));
        profile_reference_ui_main polyProf = profile_reference_ui_main.createPolygon("poly_ref", "Poly Ref", triPts, 0.0);
        check(polyProf.getType() == profile_reference_ui_main.ProfileType.POLYGON, "Profile type is POLYGON");
        check(polyProf.getLoop().getPointCount() == 3, "Polygon profile loop has 3 vertices");
    }

    private static void testProfileValidation() {
        System.out.println("\n--- 3. Profile Validation ---");

        profile_reference_ui_main validProf = profile_reference_ui_main.createRectangle("val_rect", "Val Rect", 50, 50, 0.0);
        check(profile_validator_ui_main.validate(validProf, true).isValid(), "Valid square profile passes solid validation");
        check(profile_validator_ui_main.validate(validProf, true).getMessage() == null, "Validation error message is null for valid profile");

        profile_reference_ui_main zeroArea = profile_reference_ui_main.createRectangle("zero_area", "Zero Area", 0, 50, 0.0);
        check(!profile_validator_ui_main.validate(zeroArea, true).isValid(), "Zero-area profile rejected for solid");

        profile_reference_ui_main openProf = new profile_reference_ui_main("open_prof", "Open", profile_reference_ui_main.ProfileType.POLYGON,
            new profile_loop_ui_main(List.of(new Point3D(0, 0, 0), new Point3D(10, 0, 0)), false), 10, 0, 0, 0);
        check(!profile_validator_ui_main.validate(openProf, true).isValid(), "Open profile rejected for solid creation");

        check(!profile_validator_ui_main.validate(null, true).isValid(), "Null profile reference rejected");
    }

    private static void testLoftFeatureModel() {
        System.out.println("\n--- 4. Loft Feature Model ---");

        profile_reference_ui_main s1 = profile_reference_ui_main.createRectangle("sec1", "Sec 1", 40, 40, 0.0);
        profile_reference_ui_main s2 = profile_reference_ui_main.createRectangle("sec2", "Sec 2", 20, 20, 50.0);

        List<profile_reference_ui_main> sections = new ArrayList<>(List.of(s1, s2));
        loft_feature_ui_main loft = new loft_feature_ui_main("loft1", "host_shape", "Loft 1", sections, true);

        check(loft.getId().equals("loft1"), "Loft ID is loft1");
        check(loft.getOwnerShapeId().equals("host_shape"), "Loft owner shape ID matches");
        check(loft.getName().equals("Loft 1"), "Loft name matches");
        check(loft.isSolid(), "Loft is solid mode");
        check(loft.getSections().size() == 2, "Loft section count is 2");
        check(loft.isValid(), "Loft with 2 valid sections is valid");
        check(loft.getState() == feature_state_ui_main.CLEAN, "Loft initial state is CLEAN");

        // Single section loft must be invalid
        loft_feature_ui_main singleSecLoft = new loft_feature_ui_main("loft_single", "host_shape", "Single Section", List.of(s1), true);
        check(!singleSecLoft.isValid(), "Loft with only 1 section is invalid");
        check(singleSecLoft.getState() == feature_state_ui_main.INVALID, "Single-section loft state is INVALID");
    }

    private static void testTwoSectionLoftMesh() {
        System.out.println("\n--- 5. Two-Section Loft Mesh Generation ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 50, 20));
        profile_reference_ui_main s1 = profile_reference_ui_main.createRectangle("s1", "Sec 1", 40, 40, 0.0);
        profile_reference_ui_main s2 = profile_reference_ui_main.createRectangle("s2", "Sec 2", 20, 20, 50.0);

        loft_feature_ui_main loft = new loft_feature_ui_main("loft_2sec", host.getId(), "2-Sec Loft", List.of(s1, s2), true);
        host.addLoft(loft);

        TriangleMesh mesh = loft_solid_evaluator_ui_main.evaluate(host, loft);
        check(mesh != null, "Loft mesh evaluator returns non-null mesh");
        check(mesh.getPoints().size() > 0, "Loft mesh contains point vertices");
        check(mesh.getFaces().size() > 0, "Loft mesh contains triangulated faces");

        Node visualNode = loft_mesh_builder_ui_main.buildLoftNode(host, loft, false);
        check(visualNode instanceof MeshView, "Loft visual node is JavaFX MeshView");
        check(visualNode.getId().equals(loft.getId()), "Loft visual node ID matches feature ID");
    }

    private static void testMultiSectionLoftMesh() {
        System.out.println("\n--- 6. Multi-Section (3+) Loft Mesh Generation ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 80, 20));
        profile_reference_ui_main s1 = profile_reference_ui_main.createRectangle("s1", "Sec 1", 40, 40, 0.0);
        profile_reference_ui_main s2 = profile_reference_ui_main.createRectangle("s2", "Sec 2", 25, 25, 40.0);
        profile_reference_ui_main s3 = profile_reference_ui_main.createRectangle("s3", "Sec 3", 10, 10, 80.0);

        loft_feature_ui_main loft3 = new loft_feature_ui_main("loft_3sec", host.getId(), "3-Sec Loft", List.of(s1, s2, s3), true);
        host.addLoft(loft3);

        TriangleMesh mesh3 = loft_solid_evaluator_ui_main.evaluate(host, loft3);
        check(mesh3 != null, "3-section loft evaluates successfully");
        check(mesh3.getPoints().size() >= 24 * 3, "3-section loft has sufficient vertices for multi-tier walls & caps");

        // 4-section loft
        profile_reference_ui_main s4 = profile_reference_ui_main.createRectangle("s4", "Sec 4", 30, 30, 120.0);
        loft_feature_ui_main loft4 = new loft_feature_ui_main("loft_4sec", host.getId(), "4-Sec Loft", List.of(s1, s2, s3, s4), true);
        TriangleMesh mesh4 = loft_solid_evaluator_ui_main.evaluate(host, loft4);
        check(mesh4 != null && mesh4.getFaces().size() > mesh3.getFaces().size(), "4-section loft produces more faces than 3-section loft");
    }

    private static void testLoftCapsAndWinding() {
        System.out.println("\n--- 7. Loft Caps & Winding ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-15, 0, -15), new Point3D(15, 30, 15));
        profile_reference_ui_main s1 = profile_reference_ui_main.createCircle("circ_bot", "Circ Bot", 15.0, 0.0, 16);
        profile_reference_ui_main s2 = profile_reference_ui_main.createCircle("circ_top", "Circ Top", 10.0, 30.0, 16);

        loft_feature_ui_main solidLoft = new loft_feature_ui_main("loft_cap_test", host.getId(), "Capped Loft", List.of(s1, s2), true);
        TriangleMesh meshSolid = loft_solid_evaluator_ui_main.evaluate(host, solidLoft);

        loft_feature_ui_main surfLoft = new loft_feature_ui_main("loft_surf_test", host.getId(), "Surface Loft", List.of(s1, s2), false);
        TriangleMesh meshSurf = loft_solid_evaluator_ui_main.evaluate(host, surfLoft);

        check(meshSolid != null && meshSurf != null, "Both solid and surface lofts evaluate");
        check(meshSolid.getFaces().size() > meshSurf.getFaces().size(), "Solid loft includes end caps generating more triangles than surface loft");
    }

    private static void testLoftTopologyIntegration() {
        System.out.println("\n--- 8. Loft Topology Integration ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 50, 20));
        profile_reference_ui_main s1 = profile_reference_ui_main.createRectangle("s1", "Sec 1", 40, 40, 0.0);
        profile_reference_ui_main s2 = profile_reference_ui_main.createRectangle("s2", "Sec 2", 20, 20, 50.0);

        loft_feature_ui_main loft = new loft_feature_ui_main("loft_topo", host.getId(), "Topo Loft", List.of(s1, s2), true);
        host.addLoft(loft);

        topology_body_ui_main topo = host.getTopology();
        check(topo != null, "Host body topology is non-null with loft");

        boolean hasStartCap = false, hasEndCap = false, hasSideFace = false;
        for (var df : topo.getDerivedFaces()) {
            if (df.getId().contains(":START_CAP")) hasStartCap = true;
            if (df.getId().contains(":END_CAP")) hasEndCap = true;
            if (df.getId().contains(":SIDE")) hasSideFace = true;
        }

        check(hasStartCap, "Topology contains Loft START_CAP derived face");
        check(hasEndCap, "Topology contains Loft END_CAP derived face");
        check(hasSideFace, "Topology contains Loft SIDE transition derived face");
    }

    private static void testSweepFeatureModel() {
        System.out.println("\n--- 9. Sweep Feature Model ---");

        profile_reference_ui_main prof = profile_reference_ui_main.createCircle("sweep_prof", "Sweep Prof", 10.0, 0.0, 16);
        sweep_path_ui_main path = sweep_path_ui_main.createStraightLine("sw_path1", "Straight", new Point3D(0, 0, 0), new Point3D(0, 100, 0));

        sweep_feature_ui_main sweep = new sweep_feature_ui_main("sw1", "host1", "Sweep 1", prof, path, sweep_orientation_ui_main.FOLLOW_PATH, true);

        check(sweep.getId().equals("sw1"), "Sweep ID is sw1");
        check(sweep.getOwnerShapeId().equals("host1"), "Sweep owner shape ID matches");
        check(sweep.getOrientation() == sweep_orientation_ui_main.FOLLOW_PATH, "Sweep orientation is FOLLOW_PATH");
        check(sweep.isSolid(), "Sweep is solid mode");
        check(sweep.isValid(), "Sweep with valid profile & path is valid");
        check(sweep.getState() == feature_state_ui_main.CLEAN, "Sweep initial state is CLEAN");

        sweep_feature_ui_main invalidSweep = new sweep_feature_ui_main("sw_bad", "host1", "Bad Sweep", null, path, sweep_orientation_ui_main.FIXED, true);
        check(!invalidSweep.isValid(), "Sweep with null profile is invalid");
        check(invalidSweep.getState() == feature_state_ui_main.INVALID, "Invalid sweep feature state is INVALID");
    }

    private static void testSweepPathAndResampling() {
        System.out.println("\n--- 10. Sweep Path & Resampling ---");

        sweep_path_ui_main linePath = sweep_path_ui_main.createStraightLine("lpath", "Line", new Point3D(0, 0, 0), new Point3D(0, 100, 0));
        check(linePath.getWaypoints().size() == 2, "Line path has 2 waypoints");
        check(Math.abs(linePath.getLength() - 100.0) < 1e-3, "Line path length is 100.0");

        List<Point3D> polyWaypoints = List.of(
            new Point3D(0, 0, 0),
            new Point3D(50, 0, 0),
            new Point3D(50, 50, 0)
        );
        sweep_path_ui_main polyPath = new sweep_path_ui_main("poly_path", "Poly", polyWaypoints);
        check(polyPath.getWaypoints().size() == 3, "Polyline path has 3 waypoints");
        check(Math.abs(polyPath.getLength() - 100.0) < 1e-3, "Polyline path total length is 100.0");

        List<Point3D> sampled = polyPath.samplePoints(10);
        check(sampled.size() == 10, "Resampled polyline has 10 waypoints");
    }

    private static void testSweepSolidMeshGeneration() {
        System.out.println("\n--- 11. Sweep Solid Mesh Generation ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-15, 0, -15), new Point3D(15, 100, 15));
        profile_reference_ui_main prof = profile_reference_ui_main.createRectangle("sw_rect", "Sw Rect", 20, 20, 0.0);
        sweep_path_ui_main path = sweep_path_ui_main.createStraightLine("sw_p1", "Path", new Point3D(0, 0, 0), new Point3D(0, 100, 0));

        sweep_feature_ui_main sweep = new sweep_feature_ui_main("sw_mesh_test", host.getId(), "Sweep Solid", prof, path, sweep_orientation_ui_main.FIXED, true);
        host.addSweep(sweep);

        TriangleMesh mesh = sweep_solid_evaluator_ui_main.evaluate(host, sweep);
        check(mesh != null, "Sweep mesh evaluator returns non-null mesh");
        check(mesh.getPoints().size() > 0, "Sweep mesh contains vertex points");
        check(mesh.getFaces().size() > 0, "Sweep mesh contains triangular faces");

        Node visualNode = sweep_mesh_builder_ui_main.buildSweepNode(host, sweep, false);
        check(visualNode instanceof MeshView, "Sweep visual node is JavaFX MeshView");
        check(visualNode.getId().equals(sweep.getId()), "Sweep visual node ID matches feature ID");
    }

    private static void testSweepOrientationModes() {
        System.out.println("\n--- 12. Sweep Orientation Modes ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(50, 60, 50));
        profile_reference_ui_main prof = profile_reference_ui_main.createRectangle("sw_prof_orient", "Prof", 10, 10, 0.0);

        List<Point3D> curvedWaypoints = List.of(new Point3D(0, 0, 0), new Point3D(30, 20, 10), new Point3D(50, 60, 40));
        sweep_path_ui_main path = new sweep_path_ui_main("curve_path", "Curved", curvedWaypoints);

        sweep_feature_ui_main fixedSweep = new sweep_feature_ui_main("sw_fixed", host.getId(), "Fixed", prof, path, sweep_orientation_ui_main.FIXED, true);
        sweep_feature_ui_main followSweep = new sweep_feature_ui_main("sw_follow", host.getId(), "Follow", prof, path, sweep_orientation_ui_main.FOLLOW_PATH, true);

        TriangleMesh meshFixed = sweep_solid_evaluator_ui_main.evaluate(host, fixedSweep);
        TriangleMesh meshFollow = sweep_solid_evaluator_ui_main.evaluate(host, followSweep);

        check(meshFixed != null, "FIXED orientation sweep generates valid mesh");
        check(meshFollow != null, "FOLLOW_PATH orientation sweep generates valid mesh");
        check(meshFixed.getFaces().size() == meshFollow.getFaces().size(), "Face count topology matches between orientation modes");
    }

    private static void testSweepTopologyIntegration() {
        System.out.println("\n--- 13. Sweep Topology Integration ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-10, 0, -10), new Point3D(10, 60, 10));
        profile_reference_ui_main prof = profile_reference_ui_main.createCircle("sw_circ", "Circ", 12.0, 0.0, 16);
        sweep_path_ui_main path = sweep_path_ui_main.createStraightLine("sw_tp", "Path", new Point3D(0, 0, 0), new Point3D(0, 60, 0));

        sweep_feature_ui_main sweep = new sweep_feature_ui_main("sw_topo", host.getId(), "Sweep Topo", prof, path, sweep_orientation_ui_main.FIXED, true);
        host.addSweep(sweep);

        topology_body_ui_main topo = host.getTopology();
        check(topo != null, "Sweep host body topology exists");

        boolean hasStartCap = false, hasEndCap = false, hasSideFace = false;
        for (var df : topo.getDerivedFaces()) {
            if (df.getId().contains(":START_CAP")) hasStartCap = true;
            if (df.getId().contains(":END_CAP")) hasEndCap = true;
            if (df.getId().contains(":SIDE")) hasSideFace = true;
        }

        check(hasStartCap, "Topology contains Sweep START_CAP derived face");
        check(hasEndCap, "Topology contains Sweep END_CAP derived face");
        check(hasSideFace, "Topology contains Sweep SIDE lateral derived face");
    }

    private static void testFeatureManagerIntegration() {
        System.out.println("\n--- 14. Feature Manager Integration ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(40, 40, 40));
        loft_feature_ui_main loft = new loft_feature_ui_main("fm_loft", host.getId(), "My Loft", List.of(
            profile_reference_ui_main.createRectangle("p1", "P1", 40, 40, 0.0),
            profile_reference_ui_main.createRectangle("p2", "P2", 20, 20, 40.0)
        ), true);
        host.addLoft(loft);

        sweep_feature_ui_main sweep = new sweep_feature_ui_main("fm_sweep", host.getId(), "My Sweep",
            profile_reference_ui_main.createCircle("pc", "PC", 10, 0.0, 16),
            sweep_path_ui_main.createStraightLine("sp_line", "Line", new Point3D(0, 0, 0), new Point3D(0, 50, 0)),
            sweep_orientation_ui_main.FIXED, true);
        host.addSweep(sweep);

        feature_tree_node_ui_main loftNode = feature_tree_node_ui_main.forLoft(host, loft);
        check(loftNode.isLoft(), "Loft tree node isLoft() is true");
        check(loftNode.getLoft() == loft, "Loft tree node getLoft() matches feature");
        check(loftNode.getParentShape() == host, "Loft tree node shape matches host");

        feature_tree_node_ui_main sweepNode = feature_tree_node_ui_main.forSweep(host, sweep);
        check(sweepNode.isSweep(), "Sweep tree node isSweep() is true");
        check(sweepNode.getSweep() == sweep, "Sweep tree node getSweep() matches feature");
        check(sweepNode.getParentShape() == host, "Sweep tree node shape matches host");

        check(feature_type_ui_main.LOFT.getLabel().contains("Loft"), "LOFT feature type display name contains Loft");
        check(feature_type_ui_main.SWEEP.getLabel().contains("Sweep"), "SWEEP feature type display name contains Sweep");
    }

    private static void testSelectionAndProvenance() {
        System.out.println("\n--- 15. Bidirectional Selection & Provenance ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(30, 60, 30));
        loft_feature_ui_main loft = new loft_feature_ui_main("loft_sel", host.getId(), "Sel Loft", List.of(
            profile_reference_ui_main.createRectangle("p1", "P1", 30, 30, 0.0),
            profile_reference_ui_main.createRectangle("p2", "P2", 15, 15, 60.0)
        ), true);
        host.addLoft(loft);

        topology_body_ui_main topo = host.getTopology();
        boolean foundFace = false;
        for (var df : topo.getDerivedFaces()) {
            if (df.getCreatingFeatureId() != null && df.getCreatingFeatureId().equals(loft.getId())) {
                foundFace = true;
                break;
            }
        }
        check(foundFace, "Loft derived face maps to creating feature ID in topology body");
    }

    private static void testParameterModificationAndRebuild() {
        System.out.println("\n--- 16. Parameter Modification & Rebuild ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(40, 40, 40));
        profile_reference_ui_main s1 = profile_reference_ui_main.createCircle("circ1", "C1", 10.0, 0.0, 16);
        profile_reference_ui_main s2 = profile_reference_ui_main.createCircle("circ2", "C2", 20.0, 40.0, 16);

        loft_feature_ui_main loft = new loft_feature_ui_main("rebuild_loft", host.getId(), "Param Loft", List.of(s1, s2), true);
        host.addLoft(loft);

        String sig1 = topology_signature_ui_main.generateSignature(host.getTopology());

        // Modify section 1 radius 10 -> 15
        profile_reference_ui_main s1Mod = profile_reference_ui_main.createCircle("circ1", "C1", 15.0, 0.0, 16);
        loft.setSections(List.of(s1Mod, s2));
        host.rebuild();

        String sig2 = topology_signature_ui_main.generateSignature(host.getTopology());
        check(!sig1.equals(sig2), "Topology signature changes when loft section profile is modified");
    }

    private static void testSpatialTransforms() {
        System.out.println("\n--- 17. Spatial Transforms ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(20, 50, 20));
        loft_feature_ui_main loft = new loft_feature_ui_main("tx_loft", host.getId(), "Tx Loft", List.of(
            profile_reference_ui_main.createRectangle("p1", "P1", 20, 20, 0.0),
            profile_reference_ui_main.createRectangle("p2", "P2", 10, 10, 50.0)
        ), true);
        host.addLoft(loft);

        host.setWorldTranslation(100.0, 50.0, -25.0);
        host.setRotation(45.0, 90.0);

        check(host.getWorldX() == 100.0, "WorldX transform applied");
        check(host.getWorldY() == 50.0, "WorldY transform applied");
        check(host.getWorldZ() == -25.0, "WorldZ transform applied");
        check(host.getLofts().size() == 1, "Loft persists under world transformation");
    }

    private static void testSerializationSaveLoad() {
        System.out.println("\n--- 18. Serialization & Save/Load ---");

        shape_item_ui_main host = new shape_item_ui_main("ser_shape_1", "Ser Host", basic_shapes_ui_main.CUBOID,
            new Point3D(-20, 0, -20), new Point3D(20, 50, 20), 10, 20, 30, 0, 0);

        loft_feature_ui_main loft = new loft_feature_ui_main("ser_loft_1", host.getId(), "Serialized Loft", List.of(
            profile_reference_ui_main.createRectangle("sp1", "SP1", 40, 40, 0.0),
            profile_reference_ui_main.createRectangle("sp2", "SP2", 20, 20, 50.0)
        ), true);
        host.addLoft(loft);

        sweep_feature_ui_main sweep = new sweep_feature_ui_main("ser_sw_1", host.getId(), "Serialized Sweep",
            profile_reference_ui_main.createCircle("sc", "SC", 12, 0.0, 16),
            sweep_path_ui_main.createStraightLine("sp_path", "Path", new Point3D(0, 0, 0), new Point3D(0, 75, 0)),
            sweep_orientation_ui_main.FOLLOW_PATH, true);
        host.addSweep(sweep);

        File tmpFile = new File("scratch/test_stage10_save.nd");
        document_serializer_ui_main.saveToFile(tmpFile, List.of(host));
        check(tmpFile.exists() && tmpFile.length() > 0, "Document saved with Loft & Sweep features");

        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tmpFile);
        check(loaded != null && !loaded.isEmpty(), "Document loaded successfully");
        shape_item_ui_main loadedHost = loaded.get(0);
        check(loadedHost.getLofts().size() == 1, "Loaded shape contains 1 Loft feature");
        check(loadedHost.getLofts().get(0).getId().equals("ser_loft_1"), "Loaded Loft ID matches");
        check(loadedHost.getSweeps().size() == 1, "Loaded shape contains 1 Sweep feature");
        check(loadedHost.getSweeps().get(0).getId().equals("ser_sw_1"), "Loaded Sweep ID matches");

        if (tmpFile.exists()) tmpFile.delete();
    }

    private static void testUndoRedoLifecycle() {
        System.out.println("\n--- 19. Undo / Redo Lifecycle ---");

        shape_history_ui_main history = new shape_history_ui_main();
        shape_item_ui_main host = new shape_item_ui_main("undo_host", "Undo Host", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(30, 30, 30), 0, 0, 0, 0, 0);

        List<shape_item_ui_main> state0 = List.of(host);
        history.pushSnapshot(state0);

        // State 1: add Loft
        loft_feature_ui_main loft = new loft_feature_ui_main("undo_loft", host.getId(), "Undo Loft", List.of(
            profile_reference_ui_main.createRectangle("p1", "P1", 30, 30, 0.0),
            profile_reference_ui_main.createRectangle("p2", "P2", 15, 15, 50.0)
        ), true);
        host.addLoft(loft);
        host.rebuild();
        List<shape_item_ui_main> state1 = List.of(host);
        history.pushSnapshot(state1);

        // State 2: modify loft
        loft.setSolid(false);
        host.rebuild();
        List<shape_item_ui_main> state2 = List.of(host);

        // Undo to State 1
        List<shape_item_ui_main> uState1 = history.undo(state2);
        check(uState1 != null && uState1.get(0).getLofts().get(0).isSolid(), "Undo restored solid loft");

        // Undo to State 0
        List<shape_item_ui_main> uState0 = history.undo(uState1);
        check(uState0 != null && uState0.get(0).getLofts().isEmpty(), "Undo restored solid without Loft");

        // Redo to State 1
        List<shape_item_ui_main> rState1 = history.redo(uState0);
        check(rState1 != null && rState1.get(0).getLofts().size() == 1, "Redo restored Loft feature");
    }

    private static void testDeterministicSignatures() {
        System.out.println("\n--- 20. Deterministic SHA-256 Signatures ---");

        shape_item_ui_main host1 = new shape_item_ui_main("det_shape_1", "Host 1", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(40, 50, 40), 0, 0, 0, 0, 0);
        loft_feature_ui_main loft1 = new loft_feature_ui_main("loft_det", host1.getId(), "Det Loft", List.of(
            profile_reference_ui_main.createRectangle("dp1", "DP1", 40, 40, 0.0),
            profile_reference_ui_main.createRectangle("dp2", "DP2", 20, 20, 50.0)
        ), true);
        host1.addLoft(loft1);

        String baseSig = topology_signature_ui_main.generateSignature(host1.getTopology());
        check(baseSig != null && baseSig.length() == 64, "Initial signature is valid 64-char SHA-256 hex");

        for (int i = 1; i <= 10; i++) {
            host1.rebuild();
            String iterSig = topology_signature_ui_main.generateSignature(host1.getTopology());
            check(baseSig.equals(iterSig), "Deterministic rebuild loop iteration " + i + " matches base signature");
        }
    }

    private static void testBooleanSolidInteractions() {
        System.out.println("\n--- 21. Boolean Solid Interactions ---");

        shape_item_ui_main target = new shape_item_ui_main("bool_target", "Target Box", basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(50, 50, 50), 0, 0, 0, 0, 0);
        shape_item_ui_main tool = new shape_item_ui_main("bool_tool", "Tool Loft", basic_shapes_ui_main.CUBOID, new Point3D(10, 0, 10), new Point3D(40, 60, 40), 0, 0, 0, 0, 0);

        loft_feature_ui_main loftTool = new loft_feature_ui_main("tool_loft", tool.getId(), "Tool Loft", List.of(
            profile_reference_ui_main.createRectangle("tp1", "TP1", 30, 30, 0.0),
            profile_reference_ui_main.createRectangle("tp2", "TP2", 15, 15, 60.0)
        ), true);
        tool.addLoft(loftTool);

        boolean_feature_ui_main boolCut = new boolean_feature_ui_main("bcut1", target.getId(), tool.getId(), boolean_op_type_ui_main.SUBTRACT, "Loft Cut");
        target.addBoolean(boolCut);

        check(target.hasBooleans(), "Target body has boolean feature");
        check(target.getBooleans().get(0).getOpType() == boolean_op_type_ui_main.SUBTRACT, "Boolean operation is SUBTRACT");
    }

    private static void testShellInteractions() {
        System.out.println("\n--- 22. Shell / Thickness Interactions ---");

        shape_item_ui_main host = new shape_item_ui_main("shell_loft_host", "Loft Solid", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(40, 60, 40), 0, 0, 0, 0, 0);
        loft_feature_ui_main loft = new loft_feature_ui_main("loft_for_shell", host.getId(), "Loft Base", List.of(
            profile_reference_ui_main.createRectangle("lp1", "LP1", 40, 40, 0.0),
            profile_reference_ui_main.createRectangle("lp2", "LP2", 20, 20, 60.0)
        ), true);
        host.addLoft(loft);

        shell_feature_ui_main shell = new shell_feature_ui_main("sh_on_loft", host.getId(), "Loft Shell", 3.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        host.addShell(shell);

        check(host.hasShells(), "Host body accepts shell on loft feature");
        check(host.getShells().get(0).getThickness() == 3.0, "Shell thickness is 3.0");
    }

    private static void testMachiningFeatureInteractions() {
        System.out.println("\n--- 23. Machining (Fillet, Chamfer, Draft) Interactions ---");

        shape_item_ui_main host = new shape_item_ui_main("mach_host", "Mach Host", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(50, 50, 50), 0, 0, 0, 0, 0);

        loft_feature_ui_main loft = new loft_feature_ui_main("loft_mach", host.getId(), "Loft", List.of(
            profile_reference_ui_main.createRectangle("mp1", "MP1", 50, 50, 0.0),
            profile_reference_ui_main.createRectangle("mp2", "MP2", 30, 30, 50.0)
        ), true);
        host.addLoft(loft);

        fillet_feature_ui_main fillet = new fillet_feature_ui_main("fil1", host.getId(), "Fillet 1", "EDGE_TOP_FRONT", 2.0);
        host.addFillet(fillet);
        check(!host.getFillets().isEmpty(), "Fillet feature added to loft host body");

        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main("ch1", host.getId(), "Chamfer 1", "EDGE_BOT_BACK", 1.5);
        host.addChamfer(chamfer);
        check(!host.getChamfers().isEmpty(), "Chamfer feature added to loft host body");

        draft_feature_ui_main draft = new draft_feature_ui_main("dr1", host.getId(), "Draft 1", face_kind_ui_main.FRONT, 3.0, face_kind_ui_main.BOTTOM);
        host.addDraft(draft);
        check(!host.getDrafts().isEmpty(), "Draft feature added to loft host body");
    }

    private static void testMultiBodyIsolation() {
        System.out.println("\n--- 24. Multi-Body Isolation ---");

        shape_item_ui_main body1 = new shape_item_ui_main("mb_body1", "Body 1", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(30, 30, 30), 0, 0, 0, 0, 0);
        shape_item_ui_main body2 = new shape_item_ui_main("mb_body2", "Body 2", basic_shapes_ui_main.CUBE, new Point3D(100, 0, 0), new Point3D(150, 50, 50), 100, 0, 0, 0, 0);

        loft_feature_ui_main loft1 = new loft_feature_ui_main("loft_body1", body1.getId(), "Body 1 Loft", List.of(
            profile_reference_ui_main.createRectangle("bp1", "BP1", 30, 30, 0.0),
            profile_reference_ui_main.createRectangle("bp2", "BP2", 15, 15, 30.0)
        ), true);
        body1.addLoft(loft1);

        String sig2Before = topology_signature_ui_main.generateSignature(body2.getTopology());

        // Modifying body 1 must not alter body 2
        loft1.setSections(List.of(
            profile_reference_ui_main.createRectangle("bp1_mod", "BP1 Mod", 35, 35, 0.0),
            profile_reference_ui_main.createRectangle("bp2", "BP2", 15, 15, 30.0)
        ));
        body1.rebuild();

        String sig2After = topology_signature_ui_main.generateSignature(body2.getTopology());
        check(sig2Before.equals(sig2After), "Body 2 topology signature is unchanged when modifying Body 1 Loft");
    }

    private static void testDegenerateAndEdgeCaseFailures() {
        System.out.println("\n--- 25. Degenerate & Edge Case Failures ---");

        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(20, 20, 20));

        // 1. Loft with empty sections
        loft_feature_ui_main emptyLoft = new loft_feature_ui_main("empty_loft", host.getId(), "Empty", new ArrayList<>(), true);
        check(!emptyLoft.isValid(), "Empty loft is invalid");
        check(emptyLoft.getState() == feature_state_ui_main.INVALID, "Empty loft state is INVALID");

        // 2. Sweep with zero length path
        sweep_path_ui_main zeroPath = sweep_path_ui_main.createStraightLine("zp", "Zero", new Point3D(0, 0, 0), new Point3D(0, 0, 0));
        sweep_feature_ui_main zeroSweep = new sweep_feature_ui_main("zero_sw", host.getId(), "Zero Len",
            profile_reference_ui_main.createCircle("zc", "ZC", 10, 0.0, 16), zeroPath, sweep_orientation_ui_main.FIXED, true);
        check(!zeroSweep.isValid(), "Sweep with zero-length path is invalid");
        check(zeroSweep.getState() == feature_state_ui_main.INVALID, "Zero-length sweep feature state is INVALID");

        // 3. Evaluator null tolerance
        check(loft_solid_evaluator_ui_main.evaluate(null, null) == null, "Loft evaluator handles null inputs gracefully");
        check(sweep_solid_evaluator_ui_main.evaluate(null, null) == null, "Sweep evaluator handles null inputs gracefully");
        check(loft_mesh_builder_ui_main.buildLoftNode(null, null, false) == null, "Loft mesh builder handles null gracefully");
        check(sweep_mesh_builder_ui_main.buildSweepNode(null, null, false) == null, "Sweep mesh builder handles null gracefully");
    }
}
