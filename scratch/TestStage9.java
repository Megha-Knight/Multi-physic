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
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.shape_history_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.shell.shell_direction_ui_main;
import ui.workspace.drafting.shell.shell_feature_ui_main;
import ui.workspace.drafting.shell.shell_mesh_builder_ui_main;
import ui.workspace.drafting.shell.shell_solid_evaluator_ui_main;
import ui.workspace.drafting.shell.shell_validation_result_ui_main;
import ui.workspace.drafting.shell.shell_validator_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_signature_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TestStage9.java
 * Comprehensive Verification Test Suite for Stage 9: Shell / Thickness / Open-Face Solid Modeling.
 */
public class TestStage9 {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Running Stage 9 Test Suite: Shell & Thickness Modeling");
        System.out.println("=================================================");

        try {
            testFeatureCreationAndProperties();
            testThicknessAndDirectionValidation();
            testSingleFacePlanarShelling();
            testMultiFaceShelling();
            testOppositeAndAdjacentOpenings();
            testMeshGeometryAndWinding();
            testTopologyProvenanceAndAdjacency();
            testFeatureManagerIntegration();
            testSelectionAndProvenance();
            testParameterModificationAndRebuild();
            testSpatialTransforms();
            testSerializationSaveLoad();
            testUndoRedoLifecycle();
            testDeterministicSignatures();
            testBooleanInteraction();
            testMachiningFeatureCompatibility();
            testMultiBodyIsolation();
            testDegenerateAndEdgeCaseFailures();

            System.out.println("\n=================================================");
            System.out.printf("STAGE 9 RESULTS: %d/%d Passed (%d Failed)%n", passed, passed + failed, failed);
            System.out.println("=================================================");

            if (failed > 0) {
                System.err.println("Some Stage 9 tests failed!");
                System.exit(1);
            } else {
                System.out.println("All Stage 9 Shell / Thickness Solid Modeling tests passed successfully!");
            }
        } catch (Exception e) {
            System.err.println("Unexpected exception in Stage 9 test suite: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + message);
        } else {
            failed++;
            System.err.println("[FAIL] " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            passed++;
            System.out.println("[PASS] " + message);
        } else if (expected != null && expected.equals(actual)) {
            passed++;
            System.out.println("[PASS] " + message + " (" + actual + ")");
        } else {
            failed++;
            System.err.println("[FAIL] " + message + " (Expected: " + expected + ", Actual: " + actual + ")");
        }
    }

    private static void assertEquals(double expected, double actual, double eps, String message) {
        if (Math.abs(expected - actual) <= eps) {
            passed++;
            System.out.println("[PASS] " + message + " (" + actual + " ~ " + expected + ")");
        } else {
            failed++;
            System.err.println("[FAIL] " + message + " (Expected: " + expected + ", Actual: " + actual + ")");
        }
    }

    // ==========================================
    // 1. Feature Creation & Basic Properties (6 tests)
    // ==========================================
    private static void testFeatureCreationAndProperties() {
        System.out.println("\n--- 1. Feature Creation & Properties ---");

        shell_feature_ui_main shell = new shell_feature_ui_main("feat_sh_1", "body_box_1", "Main Shell", 4.5, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertEquals("feat_sh_1", shell.getId(), "Shell feature ID assigned correctly");
        assertEquals("body_box_1", shell.getOwnerShapeId(), "Shell owner shape ID assigned");
        assertEquals(4.5, shell.getThickness(), 1e-6, "Shell thickness parameter assigned");
        assertEquals(shell_direction_ui_main.INWARD, shell.getDirection(), "Shell direction is INWARD");
        assertTrue(shell.hasRemovedFace(face_kind_ui_main.TOP), "Shell contains open TOP face");
        assertEquals(feature_state_ui_main.CLEAN, shell.getState(), "Default feature state is CLEAN");
    }

    // ==========================================
    // 2. Thickness & Direction Validation (8 tests)
    // ==========================================
    private static void testThicknessAndDirectionValidation() {
        System.out.println("\n--- 2. Thickness & Direction Validation ---");

        shape_item_ui_main box = new shape_item_ui_main("b1", "Box 1", basic_shapes_ui_main.CUBE, new Point3D(-25, -25, -25), new Point3D(25, 25, 25), 0, 0, 0, 0, 0);

        // Invalid: thickness <= 0
        shell_validation_result_ui_main rZero = shell_validator_ui_main.validate(box, 0.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(!rZero.isValid(), "Reject zero thickness");

        shell_validation_result_ui_main rNeg = shell_validator_ui_main.validate(box, -2.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(!rNeg.isValid(), "Reject negative thickness");

        shell_validation_result_ui_main rNan = shell_validator_ui_main.validate(box, Double.NaN, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(!rNan.isValid(), "Reject NaN thickness");

        shell_validation_result_ui_main rInf = shell_validator_ui_main.validate(box, Double.POSITIVE_INFINITY, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(!rInf.isValid(), "Reject Infinity thickness");

        // Over-thickness (dimension is 50, half is 25)
        shell_validation_result_ui_main rTooThick = shell_validator_ui_main.validate(box, 30.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(!rTooThick.isValid(), "Reject over-thickness inward collapse");

        // Valid Inward
        shell_validation_result_ui_main rValidIn = shell_validator_ui_main.validate(box, 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(rValidIn.isValid(), "Accept valid inward thickness");

        // Valid Outward
        shell_validation_result_ui_main rValidOut = shell_validator_ui_main.validate(box, 10.0, shell_direction_ui_main.OUTWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(rValidOut.isValid(), "Accept valid outward thickness");

        // Outward direction serialization string
        assertEquals("OUTWARD", shell_direction_ui_main.OUTWARD.name(), "Direction enum name OUTWARD");
    }

    // ==========================================
    // 3. Single-Face Planar Shelling (7 tests)
    // ==========================================
    private static void testSingleFacePlanarShelling() {
        System.out.println("\n--- 3. Single-Face Planar Shelling ---");

        shape_item_ui_main box = new shape_item_ui_main("box100", "Box 100", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("sh1", box.getId(), "Top Shell", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));

        box.addShell(shell);
        box.rebuild();

        assertTrue(box.hasShells(), "Box has active shell");
        assertEquals(1, box.getShells().get(0).getRemovedFaces().size(), "One open face selected");
        assertTrue(box.getShells().get(0).hasRemovedFace(face_kind_ui_main.TOP), "Top face is open");

        // Evaluator produces inner mesh and wall rims
        TriangleMesh tm = shell_solid_evaluator_ui_main.evaluate(box, shell);
        assertTrue(tm != null, "Mesh generated for shell");
        assertTrue(tm.getPoints().size() > 0, "Mesh points generated for single-face shell");
        assertTrue(tm.getFaces().size() > 0, "Mesh faces generated for single-face shell");

        Node visualNode = shell_mesh_builder_ui_main.buildShellNode(box, shell, false);
        assertTrue(visualNode instanceof MeshView, "Shell visual node is MeshView");
    }

    // ==========================================
    // 4. Multi-Face Shelling (6 tests)
    // ==========================================
    private static void testMultiFaceShelling() {
        System.out.println("\n--- 4. Multi-Face Shelling ---");

        shape_item_ui_main box = new shape_item_ui_main("boxMulti", "Multi Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("sh_multi", box.getId(), "Multi Shell", 4.0, shell_direction_ui_main.INWARD,
                List.of(face_kind_ui_main.TOP, face_kind_ui_main.FRONT));

        shell_validation_result_ui_main res = shell_validator_ui_main.validate(box, shell.getThickness(), shell.getDirection(), shell.getRemovedFaces());
        assertTrue(res.isValid(), "Multi-face open selection is valid");

        box.addShell(shell);
        box.rebuild();

        assertEquals(2, box.getShells().get(0).getRemovedFaces().size(), "Two faces opened");
        assertTrue(box.getShells().get(0).hasRemovedFace(face_kind_ui_main.TOP), "Top open");
        assertTrue(box.getShells().get(0).hasRemovedFace(face_kind_ui_main.FRONT), "Front open");

        TriangleMesh mesh = shell_solid_evaluator_ui_main.evaluate(box, shell);
        assertTrue(mesh != null && mesh.getFaces().size() > 0, "Multi-face shell creates valid triangulated mesh");
        assertTrue(box.getShellHolder().hasValidShell(), "Shell holder reports hasValidShell");
    }

    // ==========================================
    // 5. Opposite & Adjacent Openings (6 tests)
    // ==========================================
    private static void testOppositeAndAdjacentOpenings() {
        System.out.println("\n--- 5. Opposite & Adjacent Openings (Tube/Pipe) ---");

        shape_item_ui_main tubeBox = new shape_item_ui_main("tube", "Tube Box", basic_shapes_ui_main.CUBOID, new Point3D(-30, -60, -30), new Point3D(30, 60, 30), 0, 0, 0, 0, 0);
        shell_feature_ui_main pipeShell = new shell_feature_ui_main("sh_pipe", tubeBox.getId(), "Pipe Shell", 5.0, shell_direction_ui_main.INWARD,
                List.of(face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM));

        assertTrue(shell_validator_ui_main.validate(tubeBox, 5.0, shell_direction_ui_main.INWARD, pipeShell.getRemovedFaces()).isValid(), "Opposite faces open (thru-tube) valid");

        tubeBox.addShell(pipeShell);
        tubeBox.rebuild();

        assertTrue(tubeBox.hasShells(), "Through-cavity pipe shell established");
        assertEquals(2, tubeBox.getShells().get(0).getRemovedFaces().size(), "Top & Bottom opened");

        // 3 Adjacent faces opening
        shell_feature_ui_main triOpen = new shell_feature_ui_main("sh_tri", tubeBox.getId(), "Tri Open", 4.0, shell_direction_ui_main.INWARD,
                List.of(face_kind_ui_main.TOP, face_kind_ui_main.FRONT, face_kind_ui_main.LEFT));
        assertTrue(shell_validator_ui_main.validate(tubeBox, 4.0, shell_direction_ui_main.INWARD, triOpen.getRemovedFaces()).isValid(), "3 open faces valid");
        tubeBox.clearShells();
        tubeBox.addShell(triOpen);
        tubeBox.rebuild();
        assertEquals(3, tubeBox.getShells().get(0).getRemovedFaces().size(), "3 faces open in shell holder");
        assertTrue(tubeBox.getShells().get(0).hasRemovedFace(face_kind_ui_main.LEFT), "Left face included in open faces");
    }

    // ==========================================
    // 6. Mesh Geometry & Double-Sided Winding (6 tests)
    // ==========================================
    private static void testMeshGeometryAndWinding() {
        System.out.println("\n--- 6. Mesh Geometry & Double-Sided Winding ---");

        shape_item_ui_main box = new shape_item_ui_main("bWinding", "Winding Box", basic_shapes_ui_main.CUBE, new Point3D(-50, -50, -50), new Point3D(50, 50, 50), 0, 0, 0, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("shW", box.getId(), "Winding Shell", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));

        TriangleMesh mesh = shell_solid_evaluator_ui_main.evaluate(box, shell);
        assertTrue(mesh != null, "Mesh successfully computed");
        int pointCount = mesh.getPoints().size() / 3;
        int faceCount = mesh.getFaces().size() / 6;

        assertTrue(pointCount >= 16, "Sufficient vertices generated for inner/outer shells (" + pointCount + ")");
        assertTrue(faceCount >= 18, "Sufficient double-sided faces generated (" + faceCount + ")");

        // Check Outward mesh evaluation
        shell_feature_ui_main shellOut = new shell_feature_ui_main("shWOut", box.getId(), "Outward Shell", 5.0, shell_direction_ui_main.OUTWARD, List.of(face_kind_ui_main.TOP));
        TriangleMesh meshOut = shell_solid_evaluator_ui_main.evaluate(box, shellOut);
        assertTrue(meshOut != null && meshOut.getFaces().size() > 0, "Outward mesh evaluated");
        assertTrue(meshOut.getPoints().size() > 0, "Outward vertices generated");
        assertTrue(meshOut.getFaces().size() == mesh.getFaces().size(), "Symmetric face count between inward and outward");
    }

    // ==========================================
    // 7. Topology Provenance & Adjacency (7 tests)
    // ==========================================
    private static void testTopologyProvenanceAndAdjacency() {
        System.out.println("\n--- 7. Topology Provenance & Adjacency ---");

        shape_item_ui_main box = new shape_item_ui_main("bTopo", "Topo Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("sh_topo", box.getId(), "Topo Shell", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));

        box.addShell(shell);
        box.rebuild();

        topology_body_ui_main topo = box.getTopology();
        assertTrue(topo != null, "Topology body exists");

        // Verify derived faces generated
        boolean hasOuter = false;
        boolean hasInner = false;
        boolean hasWall = false;
        boolean hasOpen = false;

        for (var face : topo.getDerivedFaces()) {
            if (face.getId().contains(":OUTER:")) hasOuter = true;
            if (face.getId().contains(":INNER:")) hasInner = true;
            if (face.getId().contains(":WALL:")) hasWall = true;
            if (face.getId().contains(":OPEN:")) hasOpen = true;
        }

        assertTrue(hasOuter, "Topology includes SHELL OUTER retained face");
        assertTrue(hasInner, "Topology includes SHELL INNER offset face");
        assertTrue(hasWall, "Topology includes SHELL WALL boundary wall");
        assertTrue(hasOpen, "Topology records SHELL OPEN provenance");
        assertEquals("bTopo", topo.getId(), "Topology body ID preserved");
        assertTrue(topo.getDerivedFaces().size() >= 6, "Topology contains updated face representations");
    }

    // ==========================================
    // 8. Feature Manager Integration (6 tests)
    // ==========================================
    private static void testFeatureManagerIntegration() {
        System.out.println("\n--- 8. Feature Manager Integration ---");

        shape_item_ui_main box = new shape_item_ui_main("bFM", "FM Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("feat_shell_fm", box.getId(), "Shell Boss", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        box.addShell(shell);
        box.rebuild();

        feature_tree_node_ui_main shellNode = feature_tree_node_ui_main.forShell(box, shell);

        assertTrue(shellNode.isShell(), "Feature tree node isShell is true");
        assertTrue(shellNode.getLabel().contains("Shell"), "Node label contains Shell");
        assertEquals(shell, shellNode.getShell(), "Node getShell matches shell feature");
        assertEquals(box, shellNode.getParentShape(), "Node parent shape matches host box");
        assertEquals("SHELL", feature_type_ui_main.SHELL.name(), "Feature type enum contains SHELL");
        assertEquals("Shell / Thickness", feature_type_ui_main.SHELL.getLabel(), "Feature type label is Shell / Thickness");
    }

    // ==========================================
    // 9. Selection & Provenance (6 tests)
    // ==========================================
    private static void testSelectionAndProvenance() {
        System.out.println("\n--- 9. Selection & Provenance ---");

        shape_item_ui_main box = new shape_item_ui_main("bSel", "Sel Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("sh_sel", box.getId(), "Sel Shell", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        box.addShell(shell);
        box.rebuild();

        topology_body_ui_main topo = box.getTopology();
        var innerFace = topo.getDerivedFaces().stream().filter(f -> f.getId().contains(":INNER:")).findFirst().orElse(null);
        assertTrue(innerFace != null, "Inner offset face found in topology");
        assertEquals("sh_sel", innerFace.getCreatingFeatureId(), "Inner face maps back to shell creating feature ID");

        var wallFace = topo.getDerivedFaces().stream().filter(f -> f.getId().contains(":WALL:")).findFirst().orElse(null);
        assertTrue(wallFace != null, "Wall connector face found in topology");
        assertEquals("sh_sel", wallFace.getCreatingFeatureId(), "Wall face maps back to shell creating feature ID");

        var outerFace = topo.getDerivedFaces().stream().filter(f -> f.getId().contains(":OUTER:")).findFirst().orElse(null);
        assertTrue(outerFace != null, "Outer face exists");
        assertEquals("bSel", outerFace.getBodyId(), "Outer face belongs to host body");
    }

    // ==========================================
    // 10. Parameter Modification & Rebuild (7 tests)
    // ==========================================
    private static void testParameterModificationAndRebuild() {
        System.out.println("\n--- 10. Parameter Modification & Rebuild ---");

        shape_item_ui_main box = new shape_item_ui_main("bRebuild", "Rebuild Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("sh_reb", box.getId(), "Param Shell", 4.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        box.addShell(shell);
        box.rebuild();

        String sig1 = topology_signature_ui_main.generateSignature(box.getTopology());

        // Parametric modification: thickness 4.0 -> 8.0
        shell.setThickness(8.0);
        box.rebuild();
        String sig2 = topology_signature_ui_main.generateSignature(box.getTopology());

        assertEquals(8.0, box.getShells().get(0).getThickness(), 1e-6, "Thickness updated to 8.0");
        assertTrue(!sig1.equals(sig2), "Topology signature changes when thickness is modified");

        // Parametric modification: direction INWARD -> OUTWARD
        shell.setDirection(shell_direction_ui_main.OUTWARD);
        box.rebuild();
        String sig3 = topology_signature_ui_main.generateSignature(box.getTopology());

        assertEquals(shell_direction_ui_main.OUTWARD, box.getShells().get(0).getDirection(), "Direction changed to OUTWARD");
        assertTrue(!sig2.equals(sig3), "Topology signature changes when direction changes");

        // Change open face list
        shell.setRemovedFaces(List.of(face_kind_ui_main.FRONT));
        box.rebuild();
        String sig4 = topology_signature_ui_main.generateSignature(box.getTopology());
        assertTrue(!sig3.equals(sig4), "Signature changes when open faces change");
        assertTrue(box.getShells().get(0).hasRemovedFace(face_kind_ui_main.FRONT), "Front face opened");
    }

    // ==========================================
    // 11. Spatial Transforms (6 tests)
    // ==========================================
    private static void testSpatialTransforms() {
        System.out.println("\n--- 11. Spatial Transforms ---");

        shape_item_ui_main box = new shape_item_ui_main("bXform", "Xform Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("sh_xf", box.getId(), "Xf Shell", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        box.addShell(shell);
        box.rebuild();

        box.setWorldTranslation(50.0, 25.0, -10.0);

        assertEquals(50.0, box.getWorldX(), 1e-6, "WorldX applied");
        assertEquals(25.0, box.getWorldY(), 1e-6, "WorldY applied");
        assertEquals(-10.0, box.getWorldZ(), 1e-6, "WorldZ applied");

        box.rebuild();
        assertTrue(box.hasShells(), "Shell persists after spatial transform");
        assertEquals(5.0, box.getShells().get(0).getThickness(), 1e-6, "Thickness invariant to translation");
        assertTrue(box.getShells().get(0).hasRemovedFace(face_kind_ui_main.TOP), "Open face invariant");
    }

    // ==========================================
    // 12. Serialization & Save/Load (7 tests)
    // ==========================================
    private static void testSerializationSaveLoad() {
        System.out.println("\n--- 12. Serialization & Save/Load ---");

        File tempFile = new File("scratch/test_stage9_shell.nd");
        tempFile.deleteOnExit();

        shape_item_ui_main box = new shape_item_ui_main("bSer", "Ser Box", basic_shapes_ui_main.CUBOID, new Point3D(-60, -45, -30), new Point3D(60, 45, 30), 10, 20, 30, 0, 0);
        shell_feature_ui_main shell = new shell_feature_ui_main("sh_ser_1", box.getId(), "Hollow Box", 6.5, shell_direction_ui_main.INWARD,
                List.of(face_kind_ui_main.TOP, face_kind_ui_main.FRONT));
        box.addShell(shell);
        box.rebuild();

        boolean saved = document_serializer_ui_main.saveToNd(tempFile, List.of(box));
        assertTrue(saved, "Successfully saved shell model to .nd");

        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tempFile);
        assertTrue(!loaded.isEmpty(), "Loaded shapes list is non-empty");
        assertEquals(1, loaded.size(), "1 shape loaded");

        shape_item_ui_main lBox = loaded.get(0);
        assertEquals("bSer", lBox.getId(), "Loaded box ID matches");
        assertTrue(lBox.hasShells(), "Loaded box has shell");
        assertEquals(1, lBox.getShells().size(), "Loaded box has 1 shell feature");
        shell_feature_ui_main lShell = lBox.getShells().get(0);
        assertEquals("sh_ser_1", lShell.getId(), "Loaded shell ID matches");
        assertEquals(6.5, lShell.getThickness(), 1e-6, "Loaded shell thickness matches 6.5");
        assertEquals(shell_direction_ui_main.INWARD, lShell.getDirection(), "Loaded shell direction is INWARD");
    }

    // ==========================================
    // 13. Undo / Redo Lifecycle (7 tests)
    // ==========================================
    private static void testUndoRedoLifecycle() {
        System.out.println("\n--- 13. Undo / Redo Lifecycle ---");

        shape_history_ui_main history = new shape_history_ui_main();
        shape_item_ui_main box = new shape_item_ui_main("bUndo", "Undo Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);

        List<shape_item_ui_main> state0 = List.of(box);
        history.pushSnapshot(state0);

        // State 1: add shell thickness 5.0
        shell_feature_ui_main s1 = new shell_feature_ui_main("sh_u1", box.getId(), "Shell U1", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        box.addShell(s1);
        box.rebuild();
        List<shape_item_ui_main> state1 = List.of(box);
        history.pushSnapshot(state1);

        // State 2: modify thickness 5.0 -> 10.0
        s1.setThickness(10.0);
        box.rebuild();
        List<shape_item_ui_main> state2 = List.of(box);

        assertEquals(10.0, box.getShells().get(0).getThickness(), 1e-6, "State 2 thickness is 10.0");

        // Undo to State 1
        List<shape_item_ui_main> uState1 = history.undo(state2);
        assertTrue(uState1 != null, "Undo succeeded");
        assertEquals(5.0, uState1.get(0).getShells().get(0).getThickness(), 1e-6, "Undo restored thickness to 5.0");

        // Undo to State 0
        List<shape_item_ui_main> uState0 = history.undo(uState1);
        assertTrue(!uState0.get(0).hasShells(), "Undo restored solid box with no shell");

        // Redo to State 1
        List<shape_item_ui_main> rState1 = history.redo(uState0);
        assertTrue(rState1.get(0).hasShells(), "Redo restored shell");
        assertEquals(5.0, rState1.get(0).getShells().get(0).getThickness(), 1e-6, "Redo restored thickness 5.0");

        // Redo to State 2
        List<shape_item_ui_main> rState2 = history.redo(rState1);
        assertEquals(10.0, rState2.get(0).getShells().get(0).getThickness(), 1e-6, "Redo restored thickness 10.0");
        assertTrue(rState2.get(0).hasShells(), "Redo has active shell");
    }

    // ==========================================
    // 14. Deterministic Signatures (17 tests)
    // ==========================================
    private static void testDeterministicSignatures() {
        System.out.println("\n--- 14. Deterministic Signatures ---");

        shape_item_ui_main b1 = new shape_item_ui_main("boxDet", "Det Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main s1 = new shell_feature_ui_main("sh_det", b1.getId(), "Det Shell", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        b1.addShell(s1);
        b1.rebuild();

        String sigA = topology_signature_ui_main.generateSignature(b1.getTopology());
        assertTrue(sigA != null && sigA.length() == 64, "Initial signature is valid 64-char SHA-256");

        // Recompute 10 times in place
        for (int i = 0; i < 10; i++) {
            b1.rebuild();
            assertEquals(sigA, topology_signature_ui_main.generateSignature(b1.getTopology()), "Deterministic rebuild loop iteration " + (i + 1));
        }

        // Build a second identical body independently
        shape_item_ui_main b2 = new shape_item_ui_main("boxDet", "Det Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        shell_feature_ui_main s2 = new shell_feature_ui_main("sh_det", b2.getId(), "Det Shell", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        b2.addShell(s2);
        b2.rebuild();

        String sigB = topology_signature_ui_main.generateSignature(b2.getTopology());
        assertEquals(sigA, sigB, "Independent identical models produce identical SHA-256 signatures");
        assertEquals(sigA.length(), 64, "Signature length is 64 hex characters");
        assertTrue(!sigA.isEmpty(), "Signature is non-empty");
        assertEquals(64, sigB.length(), "Independent signature length is 64");
        assertTrue(sigA.equals(sigB), "Exact hash equality across instances");
        assertTrue(sigB != null, "Non-null signature for second body");
    }

    // ==========================================
    // 15. Boolean Solid Interaction (6 tests)
    // ==========================================
    private static void testBooleanInteraction() {
        System.out.println("\n--- 15. Boolean Solid Interaction ---");

        shape_item_ui_main bA = new shape_item_ui_main("bA", "Box A", basic_shapes_ui_main.CUBE, new Point3D(-30, -30, -30), new Point3D(30, 30, 30), 0, 0, 0, 0, 0);
        shape_item_ui_main bB = new shape_item_ui_main("bB", "Box B", basic_shapes_ui_main.CUBE, new Point3D(-15, -30, -15), new Point3D(45, 30, 15), 0, 0, 0, 0, 0);

        boolean_feature_ui_main boolUnion = new boolean_feature_ui_main("bool_u1", bA.getId(), bB.getId(), boolean_op_type_ui_main.UNION, "Union");
        bA.addBoolean(boolUnion);
        bA.rebuild();

        assertTrue(bA.hasBooleans(), "Boolean Union completed");

        // Apply shell on unioned solid
        shell_feature_ui_main boolShell = new shell_feature_ui_main("sh_bool", bA.getId(), "Bool Shell", 4.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        shell_validation_result_ui_main valRes = shell_validator_ui_main.validate(bA, 4.0, shell_direction_ui_main.INWARD, boolShell.getRemovedFaces());
        assertTrue(valRes.isValid(), "Shell on boolean body passes validation");

        bA.addShell(boolShell);
        bA.rebuild();

        assertTrue(bA.hasShells(), "Boolean body successfully shelled");
        assertEquals(4.0, bA.getShells().get(0).getThickness(), 1e-6, "Shell thickness preserved on boolean body");
        assertTrue(topology_signature_ui_main.generateSignature(bA.getTopology()) != null, "Boolean + Shell generates valid deterministic signature");
        assertTrue(bA.getTopology().getDerivedFaceById(bA.getId() + ":F:BOOLEAN:bool_u1") != null, "Boolean derived face persists with shell");
    }

    // ==========================================
    // 16. Machining Feature Compatibility (Fillet/Chamfer/Draft) (6 tests)
    // ==========================================
    private static void testMachiningFeatureCompatibility() {
        System.out.println("\n--- 16. Machining Feature Compatibility ---");

        // Fillet + Shell
        shape_item_ui_main bFillet = new shape_item_ui_main("bF", "Box Fillet", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        fillet_feature_ui_main fillet = new fillet_feature_ui_main("f1", bFillet.getId(), "Fillet", "TOP_FRONT", 5.0);
        bFillet.addFillet(fillet);
        bFillet.rebuild();

        shell_feature_ui_main sFillet = new shell_feature_ui_main("sh_f", bFillet.getId(), "F Shell", 4.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(shell_validator_ui_main.validate(bFillet, 4.0, shell_direction_ui_main.INWARD, sFillet.getRemovedFaces()).isValid(), "Filleted box shell validation passes");
        bFillet.addShell(sFillet);
        bFillet.rebuild();
        assertTrue(bFillet.hasShells(), "Filleted box shelled");

        // Chamfer + Shell
        shape_item_ui_main bChamfer = new shape_item_ui_main("bC", "Box Chamfer", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main("c1", bChamfer.getId(), "Chamfer", "TOP_FRONT", 4.0);
        bChamfer.addChamfer(chamfer);
        bChamfer.rebuild();

        shell_feature_ui_main sChamfer = new shell_feature_ui_main("sh_c", bChamfer.getId(), "C Shell", 3.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(shell_validator_ui_main.validate(bChamfer, 3.0, shell_direction_ui_main.INWARD, sChamfer.getRemovedFaces()).isValid(), "Chamfered box shell validation passes");
        bChamfer.addShell(sChamfer);
        bChamfer.rebuild();
        assertTrue(bChamfer.hasShells(), "Chamfered box shelled");

        // Draft + Shell
        shape_item_ui_main bDraft = new shape_item_ui_main("bD", "Box Draft", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);
        draft_feature_ui_main draft = new draft_feature_ui_main("d1", bDraft.getId(), "Draft", face_kind_ui_main.FRONT, 3.0, new Point3D(0, -1, 0), face_kind_ui_main.BOTTOM);
        bDraft.addDraft(draft);
        bDraft.rebuild();

        shell_feature_ui_main sDraft = new shell_feature_ui_main("sh_d", bDraft.getId(), "D Shell", 3.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        assertTrue(shell_validator_ui_main.validate(bDraft, 3.0, shell_direction_ui_main.INWARD, sDraft.getRemovedFaces()).isValid(), "Drafted box shell validation passes");
        bDraft.addShell(sDraft);
        bDraft.rebuild();
        assertTrue(bDraft.hasShells(), "Drafted box shelled");
    }

    // ==========================================
    // 17. Multi-Body Isolation (6 tests)
    // ==========================================
    private static void testMultiBodyIsolation() {
        System.out.println("\n--- 17. Multi-Body Isolation ---");

        shape_item_ui_main body1 = new shape_item_ui_main("body1", "Body 1", basic_shapes_ui_main.CUBE, new Point3D(-40, -40, -40), new Point3D(40, 40, 40), 0, 0, 0, 0, 0);
        shape_item_ui_main body2 = new shape_item_ui_main("body2", "Body 2", basic_shapes_ui_main.CUBE, new Point3D(-25, -25, -25), new Point3D(25, 25, 25), 150, 0, 0, 0, 0);

        String b2SigBefore = topology_signature_ui_main.generateSignature(body2.getTopology());

        // Apply Shell only to Body 1
        shell_feature_ui_main sh1 = new shell_feature_ui_main("sh_iso", body1.getId(), "Iso Shell", 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        body1.addShell(sh1);
        body1.rebuild();

        assertTrue(body1.hasShells(), "Body 1 is shelled");
        assertTrue(!body2.hasShells(), "Body 2 is NOT shelled");
        assertEquals(b2SigBefore, topology_signature_ui_main.generateSignature(body2.getTopology()), "Body 2 signature unmodified by Body 1 shelling");
        assertEquals("body1", body1.getId(), "Body 1 identity preserved");
        assertEquals("body2", body2.getId(), "Body 2 identity preserved");
        assertEquals(150.0, body2.getWorldX(), 1e-6, "Body 2 translation isolated");
    }

    // ==========================================
    // 18. Degenerate & Edge Case Failures (10 tests)
    // ==========================================
    private static void testDegenerateAndEdgeCaseFailures() {
        System.out.println("\n--- 18. Degenerate & Edge Case Failures ---");

        shape_item_ui_main box = new shape_item_ui_main("bEdge", "Edge Box", basic_shapes_ui_main.CUBOID, new Point3D(-50, -40, -25), new Point3D(50, 40, 25), 0, 0, 0, 0, 0);

        // Null target shape
        assertTrue(!shell_validator_ui_main.validate(null, 5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP)).isValid(), "Reject null shape target");

        // Null direction
        assertTrue(!shell_validator_ui_main.validate(box, 5.0, null, List.of(face_kind_ui_main.TOP)).isValid(), "Reject null direction");

        // Unsupported shape (e.g. cylinder or sphere for planar shell)
        shape_item_ui_main sphere = new shape_item_ui_main("sph", "Sphere", basic_shapes_ui_main.SPHERE, new Point3D(0, 0, 0), new Point3D(20, 20, 20), 0, 0, 0, 0, 0);
        assertTrue(!shell_validator_ui_main.validate(sphere, 2.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP)).isValid(), "Reject unsupported curved body");

        // All 6 faces removed -> reject
        List<face_kind_ui_main> allFaces = List.of(
            face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM,
            face_kind_ui_main.FRONT, face_kind_ui_main.BACK,
            face_kind_ui_main.LEFT, face_kind_ui_main.RIGHT
        );
        assertTrue(!shell_validator_ui_main.validate(box, 4.0, shell_direction_ui_main.INWARD, allFaces).isValid(), "Reject removing all 6 faces of body");

        // Invalid face kind
        assertTrue(!shell_validator_ui_main.validate(box, 4.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.SPHERE_SURFACE)).isValid(), "Reject invalid face kind on planar solid");

        // Revalidate feature directly
        shell_feature_ui_main badShell = new shell_feature_ui_main("bad", box.getId(), "Bad", -5.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        badShell.revalidate(box);
        assertEquals(feature_state_ui_main.INVALID, badShell.getState(), "Invalid thickness marks feature state INVALID");
        assertTrue(badShell.getDiagnosticMessage() != null, "Diagnostic message populated on invalid feature");

        // Copy feature
        shell_feature_ui_main copy = badShell.copy();
        assertEquals(badShell.getId(), copy.getId(), "Copied feature has identical ID");
        assertEquals(badShell.getThickness(), copy.getThickness(), 1e-6, "Copied feature has identical thickness");
        assertEquals(badShell.getState(), copy.getState(), "Copied feature has identical state");
    }
}
