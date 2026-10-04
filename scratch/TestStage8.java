package scratch;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.File_Types.document_serializer_ui_main;
import ui.featuremanager.feature_tree_node_ui_main;
import ui.workspace.drafting.booleans.boolean_feature_ui_main;
import ui.workspace.drafting.booleans.boolean_mesh_builder_ui_main;
import ui.workspace.drafting.booleans.boolean_op_type_ui_main;
import ui.workspace.drafting.booleans.boolean_solid_evaluator_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.shape_history_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_signature_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * TestStage8.java
 * Comprehensive Verification Test Suite for Stage 8: Boolean Solid Operations & Multi-Body Modeling.
 */
public class TestStage8 {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Running Stage 8 Test Suite: Boolean Solid Operations");
        System.out.println("=================================================");

        try {
            testMultiBodyRepresentation();
            testBooleanUnion();
            testBooleanSubtract();
            testBooleanIntersect();
            testTransformsAndParametricRebuild();
            testNegativeValidation();
            testFeatureChaining();
            testSaveLoadSerialization();
            testUndoRedo();
            testDeterministicSignatures();

            System.out.println("\n=================================================");
            System.out.printf("STAGE 8 RESULTS: %d/%d Passed (%d Failed)%n", passed, passed + failed, failed);
            System.out.println("=================================================");

            if (failed > 0) {
                System.err.println("Some Stage 8 tests failed!");
                System.exit(1);
            } else {
                System.out.println("All Stage 8 Boolean Solid Modeling tests passed successfully!");
            }
        } catch (Exception e) {
            System.err.println("Unexpected exception in Stage 8 test suite: " + e.getMessage());
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

    private static void testMultiBodyRepresentation() {
        System.out.println("\n--- 1. Multi-Body Model & Body Identity ---");
        shape_item_ui_main bodyA = new shape_item_ui_main("Body01", "Main Block", basic_shapes_ui_main.CUBE, new Point3D(-30, -30, -30), new Point3D(30, 30, 30), 0, 0, 0, 0, 0);
        shape_item_ui_main bodyB = new shape_item_ui_main("Body02", "Cylinder Tool", basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(10, 40, 0), 0, 0, 0, 0, 0);
        shape_item_ui_main bodyC = new shape_item_ui_main("Body03", "Aux Box", basic_shapes_ui_main.CUBOID, new Point3D(-15, -10, -10), new Point3D(15, 10, 10), 50, 0, 0, 0, 0);

        assertEquals("Body01", bodyA.getId(), "Body A has deterministic ID Body01");
        assertEquals("Body02", bodyB.getId(), "Body B has deterministic ID Body02");
        assertEquals("Body03", bodyC.getId(), "Body C has deterministic ID Body03");

        assertTrue(!bodyA.isConsumed(), "Body A initially not consumed");
        assertTrue(!bodyB.isConsumed(), "Body B initially not consumed");

        // Set tool consumed state
        bodyB.setConsumed(true);
        bodyB.setConsumedBy("Body01");
        assertTrue(bodyB.isConsumed(), "Body B marked consumed");
        assertEquals("Body01", bodyB.getConsumedBy(), "Body B consumed by Body01");
        assertTrue(!bodyB.getRootGroup().isVisible(), "Consumed body root group is hidden from top-level rendering");
    }

    private static void testBooleanUnion() {
        System.out.println("\n--- 2. Boolean Union (A ∪ B) ---");
        shape_item_ui_main host = new shape_item_ui_main("host_box", "Host", basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20), 0, 0, 0, 0, 0);
        shape_item_ui_main tool = new shape_item_ui_main("tool_box", "Tool", basic_shapes_ui_main.CUBOID, new Point3D(10, -20, -20), new Point3D(40, 20, 20), 0, 0, 0, 0, 0);

        boolean_feature_ui_main union = new boolean_feature_ui_main("u1", host.getId(), tool.getId(), boolean_op_type_ui_main.UNION, "Union Block");
        assertTrue(union.isValid(), "Union feature is valid");
        assertEquals(boolean_op_type_ui_main.UNION, union.getOpType(), "Operation type is UNION");

        host.addBoolean(union);
        assertTrue(host.hasBooleans(), "Host has boolean feature attached");
        assertEquals(1, host.getBooleans().size(), "Host has 1 boolean feature");
        assertEquals(union, host.getBoolean("u1"), "Host returns boolean by ID");

        // Geometry evaluation
        TriangleMesh mesh = boolean_solid_evaluator_ui_main.evaluate(host, tool, boolean_op_type_ui_main.UNION);
        assertTrue(mesh != null, "Union produces non-null solid TriangleMesh");
        assertTrue(mesh.getPoints().size() > 0, "Union mesh has vertices");
        assertTrue(mesh.getFaces().size() > 0, "Union mesh has faces");

        Node visual = boolean_mesh_builder_ui_main.buildBooleanNode(host, tool, union, false);
        assertTrue(visual instanceof MeshView, "Union builder returns MeshView");

        // Topology evaluation
        topology_body_ui_main topo = host.getTopology();
        assertTrue(topo != null, "Topology generated for Union host");
        assertTrue(topo.getDerivedFaceById(host.getId() + ":F:BOOLEAN:u1") != null, "Union derived face exists");
        assertTrue(topo.getDerivedFaceById(host.getId() + ":F:BOOLEAN:u1:UNION") != null, "Union region derived face exists");

        // Feature tree node
        feature_tree_node_ui_main fNode = feature_tree_node_ui_main.forBoolean(host, union);
        assertTrue(fNode.isBoolean(), "Feature tree node isBoolean");
        assertTrue(fNode.getLabel().contains("Union"), "Feature tree node label contains Union");
    }

    private static void testBooleanSubtract() {
        System.out.println("\n--- 3. Boolean Subtract (A - B) & Cavity Mesh ---");
        // Acceptance test: Box A minus Cylinder B -> Box with cylindrical cavity!
        shape_item_ui_main box = new shape_item_ui_main("host_cube", "Block", basic_shapes_ui_main.CUBE, new Point3D(-30, -30, -30), new Point3D(30, 30, 30), 0, 0, 0, 0, 0);
        shape_item_ui_main cyl = new shape_item_ui_main("tool_cyl", "Cyl Tool", basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(10, 40, 0), 0, 0, 0, 0, 0);

        boolean_feature_ui_main sub = new boolean_feature_ui_main("sub1", box.getId(), cyl.getId(), boolean_op_type_ui_main.SUBTRACT, "Cut Cyl Cavity");
        assertTrue(sub.isValid(), "Subtract feature is valid");
        assertEquals(boolean_op_type_ui_main.SUBTRACT, sub.getOpType(), "Operation type is SUBTRACT");

        box.addBoolean(sub);
        cyl.setConsumed(true);
        cyl.setConsumedBy(box.getId());

        // Mesh verification: Real cylindrical cavity created
        TriangleMesh cavMesh = boolean_solid_evaluator_ui_main.evaluateBoxMinusCylinder(box, cyl);
        assertTrue(cavMesh != null, "Box minus Cylinder produces non-null TriangleMesh");
        assertTrue(cavMesh.getPoints().size() >= 64 * 3, "Cavity mesh contains cylindrical internal vertices");
        assertTrue(cavMesh.getFaces().size() >= 64 * 6, "Cavity mesh contains internal cylindrical cavity faces");

        Node visualNode = boolean_mesh_builder_ui_main.buildBooleanNode(box, cyl, sub, false);
        assertTrue(visualNode instanceof MeshView, "Subtract builder returns MeshView");

        // Topology verification: Cavity region derived face exists
        topology_body_ui_main topo = box.getTopology();
        assertTrue(topo != null, "Topology body exists for subtracted box");
        assertTrue(topo.getDerivedFaceById(box.getId() + ":F:BOOLEAN:sub1") != null, "Subtract derived face exists in topology");
        assertTrue(topo.getDerivedFaceById(box.getId() + ":F:BOOLEAN:sub1:CAVITY") != null, "Subtract cavity derived face exists in topology");

        // Non-intersecting subtraction (tool far away)
        shape_item_ui_main farCyl = new shape_item_ui_main("far_cyl", "Far Tool", basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(5, 20, 0), 500, 0, 500, 0, 0);
        TriangleMesh intactMesh = boolean_solid_evaluator_ui_main.evaluateBoxMinusCylinder(box, farCyl);
        assertTrue(intactMesh != null, "Non-intersecting subtract produces valid intact mesh");
    }

    private static void testBooleanIntersect() {
        System.out.println("\n--- 4. Boolean Intersect (A ∩ B) ---");
        shape_item_ui_main boxA = new shape_item_ui_main("boxA", "Box A", basic_shapes_ui_main.CUBE, new Point3D(-25, -25, -25), new Point3D(25, 25, 25), 0, 0, 0, 0, 0);
        shape_item_ui_main boxB = new shape_item_ui_main("boxB", "Box B", basic_shapes_ui_main.CUBOID, new Point3D(-10, -25, -10), new Point3D(10, 25, 10), 0, 0, 0, 0, 0);

        boolean_feature_ui_main isect = new boolean_feature_ui_main("is1", boxA.getId(), boxB.getId(), boolean_op_type_ui_main.INTERSECT, "Common Core");
        assertTrue(isect.isValid(), "Intersect feature is valid");
        assertEquals(boolean_op_type_ui_main.INTERSECT, isect.getOpType(), "Operation type is INTERSECT");

        boxA.addBoolean(isect);

        TriangleMesh mesh = boolean_solid_evaluator_ui_main.evaluate(boxA, boxB, boolean_op_type_ui_main.INTERSECT);
        assertTrue(mesh != null, "Intersect produces non-null solid TriangleMesh");
        assertTrue(mesh.getPoints().size() > 0, "Intersect mesh has vertices");

        topology_body_ui_main topo = boxA.getTopology();
        assertTrue(topo != null, "Topology body exists for intersection");
        assertTrue(topo.getDerivedFaceById(boxA.getId() + ":F:BOOLEAN:is1") != null, "Intersect derived face exists in topology");
        assertTrue(topo.getDerivedFaceById(boxA.getId() + ":F:BOOLEAN:is1:INTERSECT") != null, "Intersect region face exists");

        // Non-intersecting intersection -> empty mesh cleanly handled
        shape_item_ui_main boxFar = new shape_item_ui_main("boxFar", "Far Box", basic_shapes_ui_main.CUBE, new Point3D(-10, -10, -10), new Point3D(10, 10, 10), 200, 0, 200, 0, 0);
        TriangleMesh emptyMesh = boolean_solid_evaluator_ui_main.evaluate(boxA, boxFar, boolean_op_type_ui_main.INTERSECT);
        assertTrue(emptyMesh != null && emptyMesh.getPoints().size() == 0, "Non-overlapping intersection evaluates cleanly to empty mesh");
    }

    private static void testTransformsAndParametricRebuild() {
        System.out.println("\n--- 5. Transforms & Parametric Rebuild ---");
        shape_item_ui_main host = new shape_item_ui_main("tx_host", "TX Host", basic_shapes_ui_main.CUBE, new Point3D(-30, -30, -30), new Point3D(30, 30, 30), 100, 50, -50, 45, 30);
        shape_item_ui_main tool = new shape_item_ui_main("tx_tool", "TX Tool", basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(12, 50, 0), 100, 50, -50, 0, 0);

        boolean_feature_ui_main sub = new boolean_feature_ui_main("tx_sub", host.getId(), tool.getId(), boolean_op_type_ui_main.SUBTRACT, "TX Cavity");
        host.addBoolean(sub);

        // Verification of transforms
        assertEquals(100.0, host.getWorldX(), 1e-4, "Target World X preserved");
        assertEquals(50.0, host.getWorldY(), 1e-4, "Target World Y preserved");
        assertEquals(-50.0, host.getWorldZ(), 1e-4, "Target World Z preserved");

        // Move tool body and trigger rebuild
        tool.setWorldTranslation(105, 50, -45);
        host.rebuild();

        TriangleMesh rebuiltMesh = boolean_solid_evaluator_ui_main.evaluate(host, tool, boolean_op_type_ui_main.SUBTRACT);
        assertTrue(rebuiltMesh != null && rebuiltMesh.getPoints().size() > 0, "Boolean geometry cleanly recomputed after tool translation");
    }

    private static void testNegativeValidation() {
        System.out.println("\n--- 6. Negative Validation Tests ---");
        shape_item_ui_main cubeA = new shape_item_ui_main("cube_a", "Cube A", basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20), 0, 0, 0, 0, 0);
        shape_item_ui_main cubeB = new shape_item_ui_main("cube_b", "Cube B", basic_shapes_ui_main.CUBE, new Point3D(-10, -10, -10), new Point3D(10, 10, 10), 0, 0, 0, 0, 0);

        // 1. Target == Tool
        boolean_feature_ui_main sameBody = new boolean_feature_ui_main("b_same", cubeA.getId(), cubeA.getId(), boolean_op_type_ui_main.SUBTRACT, "Self Subtract");
        assertTrue(!sameBody.isValid(), "Self-boolean is invalid");
        assertTrue(!sameBody.revalidate(cubeA, cubeA), "Self-boolean revalidate returns false");
        assertEquals(feature_state_ui_main.INVALID, sameBody.getState(), "Self-boolean marked INVALID");

        // 2. Missing target
        boolean_feature_ui_main noTarget = new boolean_feature_ui_main("b_notarget", null, cubeB.getId(), boolean_op_type_ui_main.SUBTRACT, "No Target");
        assertTrue(!noTarget.isValid(), "Missing target is invalid");
        assertTrue(!noTarget.revalidate(null, cubeB), "Missing target revalidate returns false");

        // 3. Missing tool
        boolean_feature_ui_main noTool = new boolean_feature_ui_main("b_notool", cubeA.getId(), null, boolean_op_type_ui_main.SUBTRACT, "No Tool");
        assertTrue(!noTool.isValid(), "Missing tool is invalid");
        assertTrue(!noTool.revalidate(cubeA, null), "Missing tool revalidate returns false");

        // 4. Invalid geometry host
        cubeB.setState(feature_state_ui_main.INVALID);
        boolean_feature_ui_main badTool = new boolean_feature_ui_main("b_badtool", cubeA.getId(), cubeB.getId(), boolean_op_type_ui_main.SUBTRACT, "Bad Tool");
        assertTrue(!badTool.revalidate(cubeA, cubeB), "Boolean with invalid body geometry rejected");
    }

    private static void testFeatureChaining() {
        System.out.println("\n--- 7. Feature Chaining (Machining + Holes + Booleans) ---");
        // Base Body -> Chamfer -> Hole -> Boolean Subtract
        shape_item_ui_main body = new shape_item_ui_main("chain_block", "Block", basic_shapes_ui_main.CUBE, new Point3D(-30, -30, -30), new Point3D(30, 30, 30), 0, 0, 0, 0, 0);
        chamfer_feature_ui_main ch = new chamfer_feature_ui_main("ch_c", body.getId(), "Edge Bevel", "TOP_FRONT", 4.0);
        body.addChamfer(ch);

        fillet_feature_ui_main fl = new fillet_feature_ui_main("fl_c", body.getId(), "Edge Round", "TOP_BACK", 3.0);
        body.addFillet(fl);

        hole_feature_ui_main h = new hole_feature_ui_main("h_c", body.getId(), face_kind_ui_main.TOP, 0, 0, 8.0, 15.0, false);
        body.addHole(h);

        shape_item_ui_main toolCyl = new shape_item_ui_main("chain_tool", "Tool", basic_shapes_ui_main.CYLINDER, new Point3D(10, 0, 0), new Point3D(18, 40, 0), 0, 0, 0, 0, 0);
        boolean_feature_ui_main boolSub = new boolean_feature_ui_main("bool_c", body.getId(), toolCyl.getId(), boolean_op_type_ui_main.SUBTRACT, "Tool Cut");
        body.addBoolean(boolSub);

        topology_body_ui_main topo = body.getTopology();
        assertTrue(topo != null, "Chained topology is non-null");
        assertTrue(topo.getDerivedFaceById(body.getId() + ":F:CHAMFER:ch_c") != null, "Chained Chamfer derived face exists");
        assertTrue(topo.getDerivedFaceById(body.getId() + ":F:FILLET:fl_c") != null, "Chained Fillet derived face exists");
        assertTrue(topo.getDerivedFaceById(body.getId() + ":F:TOP:HOLE_WALL:h_c") != null, "Chained Hole Wall derived face exists");
        assertTrue(topo.getDerivedFaceById(body.getId() + ":F:BOOLEAN:bool_c") != null, "Chained Boolean derived face exists");
        assertTrue(topo.getDerivedFaceById(body.getId() + ":F:BOOLEAN:bool_c:CAVITY") != null, "Chained Boolean Cavity derived face exists");
    }

    private static void testSaveLoadSerialization() {
        System.out.println("\n--- 8. Save / Load Serialization ---");
        File tempFile = new File("scratch/test_stage8_multibody.nd");
        tempFile.deleteOnExit();

        shape_item_ui_main target = new shape_item_ui_main("target_body", "Target Solid", basic_shapes_ui_main.CUBE, new Point3D(-25, -25, -25), new Point3D(25, 25, 25), 10, 20, 30, 0, 0);
        shape_item_ui_main tool = new shape_item_ui_main("tool_body", "Tool Solid", basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(8, 30, 0), 10, 20, 30, 0, 0);
        tool.setConsumed(true);
        tool.setConsumedBy("target_body");

        boolean_feature_ui_main bf = new boolean_feature_ui_main("b_save", target.getId(), tool.getId(), boolean_op_type_ui_main.SUBTRACT, "Pocket Cut");
        target.addBoolean(bf);

        List<shape_item_ui_main> list = new ArrayList<>();
        list.add(target);
        list.add(tool);

        boolean saved = document_serializer_ui_main.saveToNd(tempFile, list);
        assertTrue(saved, "Successfully saved multi-body model with Boolean feature to .nd");

        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tempFile);
        assertTrue(!loaded.isEmpty(), "Loaded shapes list is non-empty");
        assertEquals(2, loaded.size(), "Exactly 2 shapes loaded in multi-body model");

        shape_item_ui_main lTarget = loaded.get(0);
        assertEquals("target_body", lTarget.getId(), "Loaded target body ID matches");
        assertEquals(1, lTarget.getBooleans().size(), "Loaded target body has 1 boolean feature");
        boolean_feature_ui_main lBf = lTarget.getBooleans().get(0);
        assertEquals("b_save", lBf.getId(), "Loaded boolean ID matches");
        assertEquals(boolean_op_type_ui_main.SUBTRACT, lBf.getOpType(), "Loaded boolean operation type is SUBTRACT");
        assertEquals("tool_body", lBf.getToolBodyId(), "Loaded boolean tool body ID matches");

        shape_item_ui_main lTool = loaded.get(1);
        assertEquals("tool_body", lTool.getId(), "Loaded tool body ID matches");
        assertTrue(lTool.isConsumed(), "Loaded tool body consumed state is preserved");
        assertEquals("target_body", lTool.getConsumedBy(), "Loaded tool body consumedBy is preserved");

        // Verify topology of deserialized target
        topology_body_ui_main lTopo = lTarget.getTopology();
        assertTrue(lTopo != null, "Loaded target builds valid topology");
        assertTrue(lTopo.getDerivedFaceById(lTarget.getId() + ":F:BOOLEAN:b_save") != null, "Loaded Boolean derived face exists in topology");
    }

    private static void testUndoRedo() {
        System.out.println("\n--- 9. Undo / Redo Integration ---");
        shape_history_ui_main history = new shape_history_ui_main();
        List<shape_item_ui_main> state0 = new ArrayList<>();
        shape_item_ui_main target = new shape_item_ui_main("u_target", "Target", basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20), 0, 0, 0, 0, 0);
        shape_item_ui_main tool = new shape_item_ui_main("u_tool", "Tool", basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(8, 25, 0), 0, 0, 0, 0, 0);
        state0.add(target);
        state0.add(tool);

        // Snapshot before boolean
        history.pushSnapshot(state0);

        // Add boolean subtract
        target.addBoolean(new boolean_feature_ui_main("b_ur", target.getId(), tool.getId(), boolean_op_type_ui_main.SUBTRACT, "UR Cut"));
        tool.setConsumed(true);
        tool.setConsumedBy(target.getId());

        List<shape_item_ui_main> state1 = new ArrayList<>();
        state1.add(target);
        state1.add(tool);

        assertEquals(1, target.getBooleans().size(), "Pre-undo target has 1 boolean feature");
        assertTrue(tool.isConsumed(), "Pre-undo tool is consumed");

        // Undo
        List<shape_item_ui_main> undone = history.undo(state1);
        assertTrue(undone != null, "Undo returns valid shapes list");
        shape_item_ui_main uTarget = undone.get(0);
        shape_item_ui_main uTool = undone.get(1);
        assertEquals(0, uTarget.getBooleans().size(), "Undone target has 0 booleans");
        assertTrue(!uTool.isConsumed(), "Undone tool is not consumed");

        // Redo
        List<shape_item_ui_main> redone = history.redo(undone);
        assertTrue(redone != null, "Redo returns valid shapes list");
        shape_item_ui_main rTarget = redone.get(0);
        shape_item_ui_main rTool = redone.get(1);
        assertEquals(1, rTarget.getBooleans().size(), "Redone target has 1 boolean");
        assertEquals("b_ur", rTarget.getBooleans().get(0).getId(), "Redone boolean ID matches");
        assertTrue(rTool.isConsumed(), "Redone tool is consumed");
    }

    private static void testDeterministicSignatures() {
        System.out.println("\n--- 10. Deterministic SHA-256 Topology Signatures ---");
        shape_item_ui_main target = new shape_item_ui_main("sig_target", "Target", basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20), 0, 0, 0, 0, 0);
        shape_item_ui_main tool = new shape_item_ui_main("sig_tool", "Tool", basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(6, 25, 0), 0, 0, 0, 0, 0);
        target.addBoolean(new boolean_feature_ui_main("b_sig", target.getId(), tool.getId(), boolean_op_type_ui_main.SUBTRACT, "Sig Cut"));

        String sig1 = topology_signature_ui_main.generateSignature(target.getTopology());
        assertTrue(sig1 != null && sig1.length() == 64, "Topology signature is a valid 64-char SHA-256 hash");

        // Rebuild and verify determinism
        target.rebuild();
        String sig2 = topology_signature_ui_main.generateSignature(target.getTopology());
        assertEquals(sig1, sig2, "Topology signature is deterministic across rebuilds (sig1 == sig2)");

        // Modify boolean operation type and verify signature change
        target.getBooleans().get(0).setOpType(boolean_op_type_ui_main.UNION);
        target.rebuild();
        String sig3 = topology_signature_ui_main.generateSignature(target.getTopology());
        assertTrue(!sig1.equals(sig3), "Topology signature changes when boolean operation type changes");
    }
}
