import javafx.application.Platform;
import javafx.geometry.Point3D;
import ui.featuremanager.feature_tree_node_ui_main;
import ui.featuremanager.featuremanager_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_selection_resolver_ui_main;
import ui.workspace.drafting.features.*;
import ui.workspace.drafting.holes.*;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.*;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.File_Types.document_serializer_ui_main;

import java.io.File;
import java.util.*;

/**
 * TestStage4EdgeMachining.java
 * Comprehensive Stage 4 Test Suite for Edge-Aware Machining Topology & Adjacent Face Reasoning.
 */
public class TestStage4EdgeMachining {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        System.out.println("=================================================");
        System.out.println("Running Stage 4 Test Suite: Edge-Aware Topology");
        System.out.println("=================================================\n");

        testContainedFeatureValidation();
        testTangentBoundaryValidation();
        testSingleEdgeCrossingDetection();
        testBodyCornerCrossingDetection();
        testCylinderCapBoundaryDetection();
        testCavityCollisionDetection();
        testTopologyCleanlinessWithInvalidFeatures();
        testFaceSelectionSafetyWithInvalidFeatures();
        testDynamicRevalidationAndHealing();
        testDependencyGraphIntegration();
        testFeatureManagerTreeIntegration();
        testSerializationAndSignatureInvariance();

        System.out.println("\n=================================================");
        System.out.println(String.format("STAGE 4 RESULTS: %d/%d Passed (%d Failed)", passedTests, totalTests, failedTests));
        System.out.println("=================================================");

        if (failedTests > 0) {
            System.exit(1);
        } else {
            System.out.println("All Stage 4 Edge-Aware Machining tests passed successfully!");
            System.exit(0);
        }
    }

    private static void assertTrue(String name, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("[PASS] " + name);
        } else {
            failedTests++;
            System.err.println("[FAIL] " + name);
        }
    }

    private static void assertEquals(String name, Object expected, Object actual) {
        totalTests++;
        if (Objects.equals(expected, actual)) {
            passedTests++;
            System.out.println("[PASS] " + name);
        } else {
            failedTests++;
            System.err.println(String.format("[FAIL] %s - Expected: %s, Actual: %s", name, expected, actual));
        }
    }

    private static void testContainedFeatureValidation() {
        System.out.println("\n--- Category 1: Contained Feature Validation ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube1", "Test Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-20, 20, -20), new Point3D(20, -20, 20), 0, 0, 0);
        hole_feature_ui_main hole = new hole_feature_ui_main("H1", "Cube1", face_kind_ui_main.TOP, 0, 0, 10.0, 15.0, false);
        cube.addHole(hole);

        boundary_relationship_ui_main rel = edge_machining_analyzer_ui_main.analyzeHole(cube, cube.getTopology(), hole);
        assertTrue("Hole in center is CONTAINED", rel.getCondition() == boundary_condition_ui_main.CONTAINED);
        assertTrue("Contained relationship is valid", rel.isValid());
        assertTrue("Margin distance is positive (~15mm)", rel.getMarginDistance() > 14.9);
        assertEquals("State is CLEAN", feature_state_ui_main.CLEAN, hole.getState());

        topology_body_ui_main topo = cube.getTopology();
        assertTrue("Derived remaining face exists", topo.getDerivedFaceById("Cube1:F:TOP:REMAINING") != null);
        assertTrue("Hole wall exists", topo.getDerivedFaceById("Cube1:F:TOP:HOLE_WALL:H1") != null);
        assertTrue("Hole floor exists", topo.getDerivedFaceById("Cube1:F:TOP:HOLE_FLOOR:H1") != null);
    }

    private static void testTangentBoundaryValidation() {
        System.out.println("\n--- Category 2: Tangent Boundary Validation ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Tan", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-20, 20, -20), new Point3D(20, -20, 20), 0, 0, 0);
        hole_feature_ui_main hole = new hole_feature_ui_main("H_Tan", "Cube_Tan", face_kind_ui_main.TOP, 15.0, 0.0, 10.0, 15.0, false);
        cube.addHole(hole);

        boundary_relationship_ui_main rel = edge_machining_analyzer_ui_main.analyzeHole(cube, cube.getTopology(), hole);
        assertTrue("Hole at u=15 is TANGENT", rel.getCondition() == boundary_condition_ui_main.TANGENT);
        assertEquals("Affected edge is TOP_RIGHT", "TOP_RIGHT", rel.getAffectedEdgeName());
        assertEquals("Adjacent face is RIGHT", face_kind_ui_main.RIGHT, rel.getAdjacentFaceKind());
        assertTrue("State is INVALID for tangent hole", hole.getState() == feature_state_ui_main.INVALID);
        assertTrue("Diagnostic mentions boundary", hole.getDiagnosticMessage() != null && hole.getDiagnosticMessage().contains("boundary"));
    }

    private static void testSingleEdgeCrossingDetection() {
        System.out.println("\n--- Category 3: Single Edge Crossing Detection ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Edge", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-20, 20, -20), new Point3D(20, -20, 20), 0, 0, 0);

        hole_feature_ui_main hRight = new hole_feature_ui_main("H_R", "Cube_Edge", face_kind_ui_main.TOP, 18.0, 0.0, 10.0, 15.0, false);
        cube.addHole(hRight);
        boundary_relationship_ui_main relR = edge_machining_analyzer_ui_main.analyzeHole(cube, cube.getTopology(), hRight);
        assertTrue("Hole crossing right edge is CROSSES_EDGE", relR.getCondition() == boundary_condition_ui_main.CROSSES_EDGE);
        assertEquals("Edge is TOP_RIGHT", "TOP_RIGHT", relR.getAffectedEdgeName());
        assertEquals("Adjacent face is RIGHT", face_kind_ui_main.RIGHT, relR.getAdjacentFaceKind());
        assertTrue("Overlap is ~3mm", Math.abs(relR.getOverlapDistance() - 3.0) < 1e-3);
        assertTrue("Hole marked INVALID", hRight.getState() == feature_state_ui_main.INVALID);

        hole_feature_ui_main hFrontLeft = new hole_feature_ui_main("H_FL", "Cube_Edge", face_kind_ui_main.FRONT, -18.0, 0.0, 10.0, 15.0, false);
        cube.addHole(hFrontLeft);
        boundary_relationship_ui_main relFL = edge_machining_analyzer_ui_main.analyzeHole(cube, cube.getTopology(), hFrontLeft);
        assertTrue("FRONT hole crossing left edge is CROSSES_EDGE", relFL.getCondition() == boundary_condition_ui_main.CROSSES_EDGE);
        assertEquals("Edge is FRONT_LEFT", "FRONT_LEFT", relFL.getAffectedEdgeName());
        assertEquals("Adjacent face is LEFT", face_kind_ui_main.LEFT, relFL.getAdjacentFaceKind());
    }

    private static void testBodyCornerCrossingDetection() {
        System.out.println("\n--- Category 4: Body Corner Crossing Detection ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Corner", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-20, 20, -20), new Point3D(20, -20, 20), 0, 0, 0);
        hole_feature_ui_main hCorner = new hole_feature_ui_main("H_C", "Cube_Corner", face_kind_ui_main.TOP, 18.0, 18.0, 10.0, 15.0, false);
        cube.addHole(hCorner);

        boundary_relationship_ui_main rel = edge_machining_analyzer_ui_main.analyzeHole(cube, cube.getTopology(), hCorner);
        assertTrue("Corner hole is CROSSES_CORNER", rel.getCondition() == boundary_condition_ui_main.CROSSES_CORNER);
        assertEquals("Corner vertex is TOP_FRONT_RIGHT", "TOP_FRONT_RIGHT", rel.getInvolvedVertexName());
        assertTrue("State is INVALID", hCorner.getState() == feature_state_ui_main.INVALID);
        assertTrue("Diagnostic mentions corner", hCorner.getDiagnosticMessage().contains("corner"));
    }

    private static void testCylinderCapBoundaryDetection() {
        System.out.println("\n--- Category 5: Cylinder Cap Boundary Detection ---");
        shape_item_ui_main cyl = new shape_item_ui_main("Cyl1", "Cylinder", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(25, -50, 0), 0, 0, 0);

        hole_feature_ui_main hContained = new hole_feature_ui_main("H_Cyl_Center", "Cyl1", face_kind_ui_main.TOP_CAP, 0, 0, 12.0, 20.0, false);
        cyl.addHole(hContained);
        boundary_relationship_ui_main relC = edge_machining_analyzer_ui_main.analyzeHole(cyl, cyl.getTopology(), hContained);
        assertTrue("Cylinder center hole is CONTAINED", relC.getCondition() == boundary_condition_ui_main.CONTAINED);

        hole_feature_ui_main hTan = new hole_feature_ui_main("H_Cyl_Tan", "Cyl1", face_kind_ui_main.TOP_CAP, 19.0, 0.0, 12.0, 20.0, false);
        cyl.addHole(hTan);
        boundary_relationship_ui_main relT = edge_machining_analyzer_ui_main.analyzeHole(cyl, cyl.getTopology(), hTan);
        assertTrue("Cylinder hole at rim is TANGENT", relT.getCondition() == boundary_condition_ui_main.TANGENT);
        assertEquals("Edge is TOP_RIM", "TOP_RIM", relT.getAffectedEdgeName());
        assertEquals("Adjacent face is CYLINDER_LATERAL", face_kind_ui_main.CYLINDER_LATERAL, relT.getAdjacentFaceKind());

        hole_feature_ui_main hCross = new hole_feature_ui_main("H_Cyl_Cross", "Cyl1", face_kind_ui_main.TOP_CAP, 0.0, 22.0, 12.0, 20.0, false);
        cyl.addHole(hCross);
        boundary_relationship_ui_main relCross = edge_machining_analyzer_ui_main.analyzeHole(cyl, cyl.getTopology(), hCross);
        assertTrue("Cylinder crossing hole is CROSSES_EDGE", relCross.getCondition() == boundary_condition_ui_main.CROSSES_EDGE);
        assertEquals("Edge is TOP_RIM", "TOP_RIM", relCross.getAffectedEdgeName());
    }

    private static void testCavityCollisionDetection() {
        System.out.println("\n--- Category 6: Cavity Collision Detection ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Coll", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-30, 30, -30), new Point3D(30, -30, 30), 0, 0, 0);

        hole_feature_ui_main h1 = new hole_feature_ui_main("H1", "Cube_Coll", "Hole Alpha", hole_feature_ui_main.HoleType.SIMPLE,
                face_kind_ui_main.TOP, -5.0, 0.0, 12.0, 20.0, false, 0, 90, 0, 0, hole_feature_ui_main.CutoutShape.CIRCLE, 0);
        hole_feature_ui_main h2 = new hole_feature_ui_main("H2", "Cube_Coll", "Hole Beta", hole_feature_ui_main.HoleType.SIMPLE,
                face_kind_ui_main.TOP, 3.0, 0.0, 12.0, 20.0, false, 0, 90, 0, 0, hole_feature_ui_main.CutoutShape.CIRCLE, 0);

        cube.addHole(h1);
        cube.addHole(h2);

        boundary_relationship_ui_main rel2 = edge_machining_analyzer_ui_main.analyzeHole(cube, cube.getTopology(), h2);
        assertTrue("Colliding hole is INTERSECTING_CAVITIES", rel2.getCondition() == boundary_condition_ui_main.INTERSECTING_CAVITIES);
        assertTrue("Diagnostic mentions cavity collision", rel2.getDiagnosticMessage().contains("intersects another cavity") || rel2.getDiagnosticMessage().contains("Intersecting holes"));
        assertTrue("Second hole marked INVALID", h2.getState() == feature_state_ui_main.INVALID);
    }

    private static void testTopologyCleanlinessWithInvalidFeatures() {
        System.out.println("\n--- Category 7: Topology Cleanliness with Invalid Features ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Clean", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-25, 25, -25), new Point3D(25, -25, 25), 0, 0, 0);

        hole_feature_ui_main validHole = new hole_feature_ui_main("HV", "Cube_Clean", face_kind_ui_main.TOP, 0, 0, 10.0, 20.0, false);
        hole_feature_ui_main invalidHole = new hole_feature_ui_main("HI", "Cube_Clean", face_kind_ui_main.TOP, 23.0, 0, 10.0, 20.0, false);

        cube.addHole(validHole);
        cube.addHole(invalidHole);

        topology_body_ui_main topo = cube.getTopology();
        topology_derived_face_ui_main rem = topo.getDerivedFaceById("Cube_Clean:F:TOP:REMAINING");
        assertTrue("Remaining face exists", rem != null);
        assertEquals("Remaining face has exactly 2 loops (1 outer + 1 inner from valid hole)", 2, rem.getLoops().size());

        assertTrue("Valid hole wall exists", topo.getDerivedFaceById("Cube_Clean:F:TOP:HOLE_WALL:HV") != null);
        assertTrue("Invalid hole wall does NOT exist", topo.getDerivedFaceById("Cube_Clean:F:TOP:HOLE_WALL:HI") == null);
    }

    private static void testFaceSelectionSafetyWithInvalidFeatures() {
        System.out.println("\n--- Category 8: Face Selection Safety with Invalid Features ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Sel", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-25, 25, -25), new Point3D(25, -25, 25), 0, 0, 0);

        hole_feature_ui_main invalidHole = new hole_feature_ui_main("HI", "Cube_Sel", face_kind_ui_main.TOP, 23.0, 0, 10.0, 20.0, false);
        cube.addHole(invalidHole);

        var res = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.TOP, 23.0, 0.0);
        assertTrue("Result is not null", res != null);
        assertTrue("Does NOT resolve to invalid hole", res.targetHole() == null);
        assertTrue("Resolves to remaining face or base face", res.getResolvedId().contains("REMAINING") || res.getResolvedId().contains("TOP"));
    }

    private static void testDynamicRevalidationAndHealing() {
        System.out.println("\n--- Category 9: Dynamic Revalidation and Healing ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Heal", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-20, 20, -20), new Point3D(20, -20, 20), 0, 0, 0);

        hole_feature_ui_main hole = new hole_feature_ui_main("H_Heal", "Cube_Heal", face_kind_ui_main.TOP, 10.0, 0.0, 30.0, 15.0, false);
        cube.addHole(hole);
        assertTrue("Initially INVALID", hole.getState() == feature_state_ui_main.INVALID);

        hole.setDiameter(8.0);
        cube.revalidateFeatures();
        cube.rebuild();

        assertTrue("Healed to CLEAN/VALID", hole.getState() == feature_state_ui_main.CLEAN);
        assertTrue("Relationship is now CONTAINED", hole.getBoundaryRelationship().getCondition() == boundary_condition_ui_main.CONTAINED);
        assertTrue("Topology derived faces now successfully generated", cube.getTopology().getDerivedFaceById("Cube_Heal:F:TOP:HOLE_WALL:H_Heal") != null);
    }

    private static void testDependencyGraphIntegration() {
        System.out.println("\n--- Category 10: Dependency Graph Integration ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Dep", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-30, 30, -30), new Point3D(30, -30, 30), 0, 0, 0);

        hole_feature_ui_main seed = new hole_feature_ui_main("SeedHole", "Cube_Dep", face_kind_ui_main.TOP, 0, 0, 10.0, 15.0, false);
        cube.addHole(seed);

        hole_pattern_ui_main pat = new hole_pattern_ui_main("Pat1", "Cube_Dep", "SeedHole", hole_pattern_ui_main.LinearDirection.U_DIR, 3, 10.0);
        cube.addPattern(pat);

        feature_dependency_graph_ui_main graph = new feature_dependency_graph_ui_main();
        graph.buildFromShapes(List.of(cube));
        graph.validate(List.of(cube));

        assertEquals("All features clean initially", 0, graph.validateDependencies(List.of(cube)).size());

        seed.setU(28.0);
        cube.revalidateFeatures();
        graph.validate(List.of(cube));

        assertTrue("Seed hole is INVALID", seed.getState() == feature_state_ui_main.INVALID);
        assertTrue("Pattern is also marked INVALID due to upstream seed", pat.getState() == feature_state_ui_main.INVALID);
    }

    private static void testFeatureManagerTreeIntegration() {
        System.out.println("\n--- Category 11: Feature Manager Tree Integration ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Tree", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-20, 20, -20), new Point3D(20, -20, 20), 0, 0, 0);

        hole_feature_ui_main validH = new hole_feature_ui_main("H_Val", "Cube_Tree", face_kind_ui_main.TOP, 0, 0, 10.0, 15.0, false);
        hole_feature_ui_main invH = new hole_feature_ui_main("H_Inv", "Cube_Tree", face_kind_ui_main.TOP, 19.0, 0, 10.0, 15.0, false);
        cube.addHole(validH);
        cube.addHole(invH);

        feature_tree_node_ui_main nodeVal = feature_tree_node_ui_main.forHole(cube, validH);
        feature_tree_node_ui_main nodeInv = feature_tree_node_ui_main.forHole(cube, invH);

        assertTrue("Valid node does not contain [INVALID]", !nodeVal.getLabel().contains("[INVALID]"));
        assertTrue("Invalid node label contains [INVALID]", nodeInv.getLabel().contains("[INVALID]"));
    }

    private static void testSerializationAndSignatureInvariance() {
        System.out.println("\n--- Category 12: Serialization and Signature Invariance ---");
        shape_item_ui_main cube = new shape_item_ui_main("Cube_Ser", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-20, 20, -20), new Point3D(20, -20, 20), 0, 0, 0);
        hole_feature_ui_main hole = new hole_feature_ui_main("H_Ser", "Cube_Ser", face_kind_ui_main.TOP, 0, 0, 10.0, 15.0, false);
        cube.addHole(hole);

        String sigBefore = topology_signature_ui_main.generateSignature(cube.getTopology());
        assertTrue("Signature is valid SHA-256", sigBefore != null && sigBefore.length() == 64);

        File tmpFile = new File("scratch/test_stage4_ser.nd");
        document_serializer_ui_main.saveToFile(tmpFile, List.of(cube));
        assertTrue("Saved .nd file exists", tmpFile.exists() && tmpFile.length() > 0);

        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tmpFile);
        assertEquals("Loaded 1 shape", 1, loaded.size());
        shape_item_ui_main loadedCube = loaded.get(0);

        String sigAfter = topology_signature_ui_main.generateSignature(loadedCube.getTopology());
        assertEquals("Topology signature identical after deserialization", sigBefore, sigAfter);
        assertEquals("Loaded hole state is CLEAN", feature_state_ui_main.CLEAN, loadedCube.getHoles().get(0).getState());

        tmpFile.delete();
    }
}
