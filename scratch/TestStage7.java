import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import ui.File_Types.document_serializer_ui_main;
import ui.featuremanager.feature_tree_node_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.machining.*;
import ui.workspace.drafting.shape_history_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_signature_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestStage7 {

    private static int totalTests = 0;
    private static int passedTests = 0;

    private static void assertTrue(boolean condition, String message) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("[PASS] " + message);
        } else {
            System.err.println("[FAIL] " + message);
            throw new AssertionError("Test failed: " + message);
        }
    }

    private static void assertEquals(double expected, double actual, double delta, String message) {
        totalTests++;
        if (Math.abs(expected - actual) <= delta) {
            passedTests++;
            System.out.println("[PASS] " + message + " (" + actual + " ~ " + expected + ")");
        } else {
            System.err.println("[FAIL] " + message + " Expected: " + expected + " but got: " + actual);
            throw new AssertionError("Test failed: " + message + " Expected: " + expected + " got: " + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        totalTests++;
        if (java.util.Objects.equals(expected, actual)) {
            passedTests++;
            System.out.println("[PASS] " + message + " (" + actual + ")");
        } else {
            System.err.println("[FAIL] " + message + " Expected: " + expected + " but got: " + actual);
            throw new AssertionError("Test failed: " + message + " Expected: " + expected + " got: " + actual);
        }
    }

    public static void main(String[] args) {
        System.out.println("\n=================================================");
        System.out.println("Running Stage 7 Test Suite: Chamfer, Fillet, Draft");
        System.out.println("=================================================\n");

        testFilletFeatures();
        testChamferFeatures();
        testDraftFeatures();
        testFeatureChaining();
        testNegativeValidation();
        testTransforms();
        testSaveLoadSerialization();
        testUndoRedo();
        testTopologySignatureDeterminism();

        System.out.println("\n=================================================");
        System.out.println("STAGE 7 RESULTS: " + passedTests + "/" + totalTests + " Passed (0 Failed)");
        System.out.println("=================================================");
    }

    private static void testFilletFeatures() {
        System.out.println("--- 1. Fillet Feature & Geometry ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20));
        fillet_feature_ui_main fillet = new fillet_feature_ui_main("fillet_01", cube.getId(), "Test Fillet", "TOP_FRONT", 3.0);

        assertTrue(fillet.isValid(), "Fillet with R=3.0 is valid");
        assertEquals("fillet_01", fillet.getId(), "Fillet ID matches");
        assertEquals("TOP_FRONT", fillet.getEdgeId(), "Fillet Edge ID matches");
        assertEquals(3.0, fillet.getRadius(), 1e-4, "Fillet radius is 3.0");

        cube.addFillet(fillet);
        assertTrue(!cube.getFillets().isEmpty(), "Cube has fillet feature attached");
        assertEquals(fillet, cube.getFillet("fillet_01"), "Cube returns fillet by ID");

        // Geometry verification
        Node node = fillet_mesh_builder_ui_main.buildFilletNode(cube, fillet, false);
        assertTrue(node instanceof MeshView, "Fillet generates MeshView 3D geometry");
        MeshView mv = (MeshView) node;
        assertTrue(mv.getMesh() != null, "Fillet MeshView contains non-null TriangleMesh");

        // Topology verification
        topology_body_ui_main topo = cube.getTopology();
        assertTrue(topo != null, "Topology body exists for filleted cube");
        var df = topo.getDerivedFaceById(cube.getId() + ":F:FILLET:fillet_01");
        assertTrue(df != null, "Topology contains derived blend face for fillet");
        assertEquals(3.0, df.getRadius(), 1e-4, "Derived face stores fillet radius");

        // Feature Manager integration
        feature_tree_node_ui_main ftNode = feature_tree_node_ui_main.forFillet(cube, fillet);
        assertTrue(ftNode.isFillet(), "Feature Manager node is Fillet");
        assertTrue(ftNode.getLabel().contains("Fillet") || ftNode.getLabel().contains("fillet") || ftNode.getLabel().contains("Test Fillet"), "Feature Manager label contains Fillet");

        // Parametric editing
        fillet.setRadius(5.0);
        cube.rebuild();
        assertEquals(5.0, fillet.getRadius(), 1e-4, "Fillet radius successfully edited to 5.0");
        topology_body_ui_main updatedTopo = cube.getTopology();
        var updatedDf = updatedTopo.getDerivedFaceById(cube.getId() + ":F:FILLET:fillet_01");
        assertEquals(5.0, updatedDf.getRadius(), 1e-4, "Topology reflects updated fillet radius 5.0");
    }

    private static void testChamferFeatures() {
        System.out.println("\n--- 2. Chamfer Feature & Geometry ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-25, -25, -25), new Point3D(25, 25, 25));
        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main("chamfer_01", cube.getId(), "Test Chamfer", "TOP_LEFT", 4.0);

        assertTrue(chamfer.isValid(), "Chamfer with d=4.0 is valid");
        assertEquals("chamfer_01", chamfer.getId(), "Chamfer ID matches");
        assertEquals("TOP_LEFT", chamfer.getEdgeId(), "Chamfer Edge ID matches");
        assertEquals(4.0, chamfer.getDistance(), 1e-4, "Chamfer distance is 4.0");

        cube.addChamfer(chamfer);
        assertTrue(!cube.getChamfers().isEmpty(), "Cube has chamfer feature attached");
        assertEquals(chamfer, cube.getChamfer("chamfer_01"), "Cube returns chamfer by ID");

        // Geometry verification
        Node node = chamfer_mesh_builder_ui_main.buildChamferNode(cube, chamfer, false);
        assertTrue(node instanceof MeshView, "Chamfer generates MeshView 3D geometry");
        MeshView mv = (MeshView) node;
        assertTrue(mv.getMesh() != null, "Chamfer MeshView contains non-null TriangleMesh");

        // Topology verification
        topology_body_ui_main topo = cube.getTopology();
        assertTrue(topo != null, "Topology body exists for chamfered cube");
        var df = topo.getDerivedFaceById(cube.getId() + ":F:CHAMFER:chamfer_01");
        assertTrue(df != null, "Topology contains derived bevel face for chamfer");
        assertEquals(4.0, df.getDepth(), 1e-4, "Derived face stores chamfer distance");

        // Feature Manager integration
        feature_tree_node_ui_main ftNode = feature_tree_node_ui_main.forChamfer(cube, chamfer);
        assertTrue(ftNode.isChamfer(), "Feature Manager node is Chamfer");
        assertTrue(ftNode.getLabel().contains("Chamfer") || ftNode.getLabel().contains("chamfer") || ftNode.getLabel().contains("Test Chamfer"), "Feature Manager label contains Chamfer");

        // Parametric editing
        chamfer.setDistance(6.5);
        cube.rebuild();
        assertEquals(6.5, chamfer.getDistance(), 1e-4, "Chamfer distance successfully edited to 6.5");
        topology_body_ui_main updatedTopo = cube.getTopology();
        var updatedDf = updatedTopo.getDerivedFaceById(cube.getId() + ":F:CHAMFER:chamfer_01");
        assertEquals(6.5, updatedDf.getDepth(), 1e-4, "Topology reflects updated chamfer distance 6.5");
    }

    private static void testDraftFeatures() {
        System.out.println("\n--- 3. Draft / Taper Feature & Geometry ---");
        shape_item_ui_main cuboid = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-30, -20, -15), new Point3D(30, 20, 15));
        draft_feature_ui_main draft = new draft_feature_ui_main("draft_01", cuboid.getId(), "Test Draft", face_kind_ui_main.FRONT, 5.0, face_kind_ui_main.BOTTOM);

        assertTrue(draft.isValid(), "Draft with 5.0 deg is valid");
        assertEquals("draft_01", draft.getId(), "Draft ID matches");
        assertEquals(face_kind_ui_main.FRONT, draft.getFaceKind(), "Draft face is FRONT");
        assertEquals(5.0, draft.getDraftAngle(), 1e-4, "Draft angle is 5.0 deg");
        assertEquals(face_kind_ui_main.BOTTOM, draft.getNeutralKind(), "Draft neutral face is BOTTOM");

        cuboid.addDraft(draft);
        assertTrue(!cuboid.getDrafts().isEmpty(), "Cuboid has draft feature attached");
        assertEquals(draft, cuboid.getDraft("draft_01"), "Cuboid returns draft by ID");

        // Geometry verification
        Node node = draft_mesh_builder_ui_main.buildDraftNode(cuboid, draft, false);
        assertTrue(node instanceof MeshView, "Draft generates MeshView 3D tapered geometry");
        MeshView mv = (MeshView) node;
        assertTrue(mv.getMesh() != null, "Draft MeshView contains non-null TriangleMesh");

        // Topology verification
        topology_body_ui_main topo = cuboid.getTopology();
        assertTrue(topo != null, "Topology body exists for drafted cuboid");
        var df = topo.getDerivedFaceById(cuboid.getId() + ":F:DRAFT:draft_01");
        assertTrue(df != null, "Topology contains derived draft face");

        // Feature Manager integration
        feature_tree_node_ui_main ftNode = feature_tree_node_ui_main.forDraft(cuboid, draft);
        assertTrue(ftNode.isDraft(), "Feature Manager node is Draft");
        assertTrue(ftNode.getLabel().contains("Draft") || ftNode.getLabel().contains("draft") || ftNode.getLabel().contains("Test Draft"), "Feature Manager label contains Draft");

        // Parametric editing
        draft.setDraftAngle(7.5);
        cuboid.rebuild();
        assertEquals(7.5, draft.getDraftAngle(), 1e-4, "Draft angle successfully edited to 7.5 deg");
    }

    private static void testFeatureChaining() {
        System.out.println("\n--- 4. Multi-Feature Interaction & Chaining ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-30, -30, -30), new Point3D(30, 30, 30));

        // 1. Chamfer on TOP_FRONT
        chamfer_feature_ui_main c = new chamfer_feature_ui_main("c1", cube.getId(), "Chamfer1", "TOP_FRONT", 4.0);
        cube.addChamfer(c);

        // 2. Fillet on TOP_BACK
        fillet_feature_ui_main f = new fillet_feature_ui_main("f1", cube.getId(), "Fillet1", "TOP_BACK", 3.5);
        cube.addFillet(f);

        // 3. Blind Hole on TOP
        hole_feature_ui_main h = new hole_feature_ui_main("h1", cube.getId(), face_kind_ui_main.TOP, 0, 0, 10.0, 20.0, false);
        cube.addHole(h);

        topology_body_ui_main topo = cube.getTopology();
        assertTrue(topo != null, "Chained topology is non-null");
        assertTrue(topo.getDerivedFaceById(cube.getId() + ":F:CHAMFER:c1") != null, "Chained Chamfer derived face exists");
        assertTrue(topo.getDerivedFaceById(cube.getId() + ":F:FILLET:f1") != null, "Chained Fillet derived face exists");
        assertTrue(topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_WALL:h1") != null, "Chained Hole Wall derived face exists");
        assertTrue(topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_FLOOR:h1") != null, "Chained Hole Floor derived face exists");

        // Chamfer + Countersink on another body
        shape_item_ui_main cube2 = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-25, -25, -25), new Point3D(25, 25, 25));
        chamfer_feature_ui_main c2 = new chamfer_feature_ui_main("c2", cube2.getId(), "Chamfer2", "FRONT_LEFT", 3.0);
        cube2.addChamfer(c2);
        hole_feature_ui_main cs = new hole_feature_ui_main("cs1", cube2.getId(), hole_feature_ui_main.HoleType.COUNTERSINK,
                face_kind_ui_main.TOP, 0, 0, 8.0, 20.0, false, 14.0, 90.0, 0, 0);
        cube2.addHole(cs);

        topology_body_ui_main topo2 = cube2.getTopology();
        assertTrue(topo2 != null, "Chamfer + Countersink topology is non-null");
        assertTrue(topo2.getDerivedFaceById(cube2.getId() + ":F:CHAMFER:c2") != null, "Chamfer derived face exists in CS combo");
        assertTrue(topo2.getDerivedFaceById(cube2.getId() + ":F:TOP:HOLE_WALL:cs1") != null, "Countersink derived face exists in combo");

        // Draft + Through Hole
        shape_item_ui_main cube3 = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-25, -25, -25), new Point3D(25, 25, 25));
        draft_feature_ui_main d3 = new draft_feature_ui_main("d3", cube3.getId(), "Draft3", face_kind_ui_main.LEFT, 4.0, face_kind_ui_main.BOTTOM);
        cube3.addDraft(d3);
        hole_feature_ui_main th = new hole_feature_ui_main("th1", cube3.getId(), face_kind_ui_main.TOP, 0, 0, 10.0, 50.0, true);
        cube3.addHole(th);

        topology_body_ui_main topo3 = cube3.getTopology();
        assertTrue(topo3 != null, "Draft + Through Hole topology is non-null");
        assertTrue(topo3.getDerivedFaceById(cube3.getId() + ":F:DRAFT:d3") != null, "Draft derived face exists in combo");
        assertTrue(topo3.getDerivedFaceById(cube3.getId() + ":F:TOP:BORE_WALL:th1") != null, "Through Bore wall exists in Draft combo");
    }

    private static void testNegativeValidation() {
        System.out.println("\n--- 5. Negative Validation Tests ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20));

        // Fillet negative tests
        fillet_feature_ui_main negR = new fillet_feature_ui_main("f_neg", cube.getId(), "Neg Fillet", "TOP_FRONT", -2.0);
        assertTrue(!negR.isValid(), "Fillet with radius <= 0 is invalid");
        assertTrue(!negR.revalidate(cube), "Fillet with radius <= 0 revalidation returns false");
        assertEquals(feature_state_ui_main.INVALID, negR.getState(), "Fillet with radius <= 0 marked INVALID");

        fillet_feature_ui_main tooLargeR = new fillet_feature_ui_main("f_big", cube.getId(), "Big Fillet", "TOP_FRONT", 25.0);
        assertTrue(!tooLargeR.revalidate(cube), "Fillet radius exceeding local half-dimension (25 > 20) is rejected");
        assertEquals(feature_state_ui_main.INVALID, tooLargeR.getState(), "Excessive fillet radius marked INVALID");

        fillet_feature_ui_main badEdge = new fillet_feature_ui_main("f_bad_edge", cube.getId(), "Bad Edge Fillet", "NON_EXISTENT_EDGE", 2.0);
        assertTrue(!badEdge.revalidate(cube), "Fillet on unknown edge is rejected");
        assertEquals(feature_state_ui_main.INVALID, badEdge.getState(), "Unknown edge fillet marked INVALID");

        // Chamfer negative tests
        chamfer_feature_ui_main negD = new chamfer_feature_ui_main("c_neg", cube.getId(), "Neg Chamfer", "TOP_FRONT", 0.0);
        assertTrue(!negD.isValid(), "Chamfer with distance <= 0 is invalid");
        assertTrue(!negD.revalidate(cube), "Chamfer with distance <= 0 revalidation returns false");
        assertEquals(feature_state_ui_main.INVALID, negD.getState(), "Chamfer with distance <= 0 marked INVALID");

        chamfer_feature_ui_main tooLargeD = new chamfer_feature_ui_main("c_big", cube.getId(), "Big Chamfer", "TOP_FRONT", 30.0);
        assertTrue(!tooLargeD.revalidate(cube), "Chamfer distance exceeding local half-dimension (30 > 20) is rejected");
        assertEquals(feature_state_ui_main.INVALID, tooLargeD.getState(), "Excessive chamfer distance marked INVALID");

        chamfer_feature_ui_main badEdgeC = new chamfer_feature_ui_main("c_bad_edge", cube.getId(), "Bad Edge Chamfer", "NON_EXISTENT_EDGE", 2.0);
        assertTrue(!badEdgeC.revalidate(cube), "Chamfer on unknown edge is rejected");
        assertEquals(feature_state_ui_main.INVALID, badEdgeC.getState(), "Unknown edge chamfer marked INVALID");

        // Draft negative tests
        draft_feature_ui_main negAngle = new draft_feature_ui_main("d_zero", cube.getId(), "Zero Draft", face_kind_ui_main.FRONT, 0.0, face_kind_ui_main.BOTTOM);
        assertTrue(!negAngle.isValid(), "Draft with angle 0 is invalid");
        assertTrue(!negAngle.revalidate(cube), "Draft with angle 0 revalidation returns false");

        draft_feature_ui_main bigAngle = new draft_feature_ui_main("d_big", cube.getId(), "Big Draft", face_kind_ui_main.FRONT, 60.0, face_kind_ui_main.BOTTOM);
        assertTrue(!bigAngle.isValid(), "Draft with angle > 45 deg is invalid");
        assertTrue(!bigAngle.revalidate(cube), "Draft with angle > 45 deg revalidation returns false");
        assertEquals(feature_state_ui_main.INVALID, bigAngle.getState(), "Excessive draft angle marked INVALID");

        draft_feature_ui_main topDraft = new draft_feature_ui_main("d_top", cube.getId(), "Top Draft", face_kind_ui_main.TOP, 5.0, face_kind_ui_main.BOTTOM);
        assertTrue(!topDraft.revalidate(cube), "Draft on TOP end-cap face is rejected");
        assertEquals(feature_state_ui_main.INVALID, topDraft.getState(), "TOP face draft marked INVALID");
    }

    private static void testTransforms() {
        System.out.println("\n--- 6. Transform Invariance (Translation & Rotation) ---");
        shape_item_ui_main cube = new shape_item_ui_main("tx_cube", "TX Block", basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20), 100, 50, -80, 45, 30);
        chamfer_feature_ui_main c = new chamfer_feature_ui_main("c_tx", cube.getId(), "TX Chamfer", "TOP_FRONT", 3.0);
        fillet_feature_ui_main f = new fillet_feature_ui_main("f_tx", cube.getId(), "TX Fillet", "TOP_LEFT", 2.5);
        draft_feature_ui_main d = new draft_feature_ui_main("d_tx", cube.getId(), "TX Draft", face_kind_ui_main.FRONT, 4.0, face_kind_ui_main.BOTTOM);

        cube.addChamfer(c);
        cube.addFillet(f);
        cube.addDraft(d);

        // Verification after transform
        assertEquals(100.0, cube.getWorldX(), 1e-4, "World X preserved");
        assertEquals(50.0, cube.getWorldY(), 1e-4, "World Y preserved");
        assertEquals(-80.0, cube.getWorldZ(), 1e-4, "World Z preserved");
        assertEquals(45.0, cube.getRotationX(), 1e-4, "Rotation X preserved");
        assertEquals(30.0, cube.getRotationY(), 1e-4, "Rotation Y preserved");

        topology_body_ui_main topo = cube.getTopology();
        assertTrue(topo != null, "Topology successfully generated for transformed body");
        assertTrue(topo.getDerivedFaceById(cube.getId() + ":F:CHAMFER:c_tx") != null, "Transformed body contains Chamfer derived face");
        assertTrue(topo.getDerivedFaceById(cube.getId() + ":F:FILLET:f_tx") != null, "Transformed body contains Fillet derived face");
        assertTrue(topo.getDerivedFaceById(cube.getId() + ":F:DRAFT:d_tx") != null, "Transformed body contains Draft derived face");
    }

    private static void testSaveLoadSerialization() {
        System.out.println("\n--- 7. Save / Load Serialization ---");
        File tempFile = new File("scratch/test_stage7_model.nd");
        tempFile.deleteOnExit();

        shape_item_ui_main cube = new shape_item_ui_main("host_cube_01", "Main Block", basic_shapes_ui_main.CUBE,
                new Point3D(-25, -25, -25), new Point3D(25, 25, 25), 10, 20, 30, 15, 45);

        chamfer_feature_ui_main c = new chamfer_feature_ui_main("c_save", cube.getId(), "Bevel Edge", "TOP_RIGHT", 3.5);
        fillet_feature_ui_main f = new fillet_feature_ui_main("f_save", cube.getId(), "Round Edge", "FRONT_LEFT", 2.8);
        draft_feature_ui_main d = new draft_feature_ui_main("d_save", cube.getId(), "Taper Face", face_kind_ui_main.RIGHT, 6.0, face_kind_ui_main.BOTTOM);

        cube.addChamfer(c);
        cube.addFillet(f);
        cube.addDraft(d);

        List<shape_item_ui_main> list = new ArrayList<>();
        list.add(cube);

        boolean saved = document_serializer_ui_main.saveToNd(tempFile, list);
        assertTrue(saved, "Successfully saved model with Chamfer, Fillet, and Draft to .nd");

        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tempFile);
        assertTrue(!loaded.isEmpty(), "Loaded shapes list is non-empty");
        assertEquals(1, loaded.size(), "Exactly 1 shape loaded");

        shape_item_ui_main lCube = loaded.get(0);
        assertEquals("host_cube_01", lCube.getId(), "Loaded shape ID matches");
        assertEquals(basic_shapes_ui_main.CUBE, lCube.getType(), "Loaded shape type is CUBE");

        // Verify Chamfer
        assertEquals(1, lCube.getChamfers().size(), "Loaded cube has 1 chamfer");
        chamfer_feature_ui_main lc = lCube.getChamfers().get(0);
        assertEquals("c_save", lc.getId(), "Loaded chamfer ID matches");
        assertEquals("TOP_RIGHT", lc.getEdgeId(), "Loaded chamfer Edge ID matches");
        assertEquals(3.5, lc.getDistance(), 1e-4, "Loaded chamfer distance is 3.5");

        // Verify Fillet
        assertEquals(1, lCube.getFillets().size(), "Loaded cube has 1 fillet");
        fillet_feature_ui_main lf = lCube.getFillets().get(0);
        assertEquals("f_save", lf.getId(), "Loaded fillet ID matches");
        assertEquals("FRONT_LEFT", lf.getEdgeId(), "Loaded fillet Edge ID matches");
        assertEquals(2.8, lf.getRadius(), 1e-4, "Loaded fillet radius is 2.8");

        // Verify Draft
        assertEquals(1, lCube.getDrafts().size(), "Loaded cube has 1 draft");
        draft_feature_ui_main ld = lCube.getDrafts().get(0);
        assertEquals("d_save", ld.getId(), "Loaded draft ID matches");
        assertEquals(face_kind_ui_main.RIGHT, ld.getFaceKind(), "Loaded draft faceKind is RIGHT");
        assertEquals(6.0, ld.getDraftAngle(), 1e-4, "Loaded draft angle is 6.0 deg");
        assertEquals(face_kind_ui_main.BOTTOM, ld.getNeutralKind(), "Loaded draft neutralKind is BOTTOM");

        // Verify topology of deserialized model
        topology_body_ui_main lTopo = lCube.getTopology();
        assertTrue(lTopo != null, "Loaded cube builds valid topology");
        assertTrue(lTopo.getDerivedFaceById(lCube.getId() + ":F:CHAMFER:c_save") != null, "Loaded Chamfer derived face exists in topology");
        assertTrue(lTopo.getDerivedFaceById(lCube.getId() + ":F:FILLET:f_save") != null, "Loaded Fillet derived face exists in topology");
        assertTrue(lTopo.getDerivedFaceById(lCube.getId() + ":F:DRAFT:d_save") != null, "Loaded Draft derived face exists in topology");
    }

    private static void testUndoRedo() {
        System.out.println("\n--- 8. Undo / Redo Integration ---");
        shape_history_ui_main history = new shape_history_ui_main();
        List<shape_item_ui_main> state0 = new ArrayList<>();
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20));
        state0.add(cube);

        // Snapshot before features
        history.pushSnapshot(state0);

        // Add features
        cube.addChamfer(new chamfer_feature_ui_main("c_ur", cube.getId(), "Undo Chamfer", "TOP_FRONT", 3.0));
        cube.addFillet(new fillet_feature_ui_main("f_ur", cube.getId(), "Undo Fillet", "TOP_BACK", 2.5));
        cube.addDraft(new draft_feature_ui_main("d_ur", cube.getId(), "Undo Draft", face_kind_ui_main.FRONT, 4.0, face_kind_ui_main.BOTTOM));

        List<shape_item_ui_main> state1 = new ArrayList<>();
        state1.add(cube);

        assertEquals(1, cube.getChamfers().size(), "Pre-undo state has 1 chamfer");
        assertEquals(1, cube.getFillets().size(), "Pre-undo state has 1 fillet");
        assertEquals(1, cube.getDrafts().size(), "Pre-undo state has 1 draft");

        // Undo
        List<shape_item_ui_main> undone = history.undo(state1);
        assertTrue(undone != null, "Undo returns valid shapes list");
        shape_item_ui_main uCube = undone.get(0);
        assertEquals(0, uCube.getChamfers().size(), "Undone state has 0 chamfers");
        assertEquals(0, uCube.getFillets().size(), "Undone state has 0 fillets");
        assertEquals(0, uCube.getDrafts().size(), "Undone state has 0 drafts");

        // Redo
        List<shape_item_ui_main> redone = history.redo(undone);
        assertTrue(redone != null, "Redo returns valid shapes list");
        shape_item_ui_main rCube = redone.get(0);
        assertEquals(1, rCube.getChamfers().size(), "Redone state has 1 chamfer");
        assertEquals(1, rCube.getFillets().size(), "Redone state has 1 fillet");
        assertEquals(1, rCube.getDrafts().size(), "Redone state has 1 draft");
        assertEquals("c_ur", rCube.getChamfers().get(0).getId(), "Redone chamfer ID matches");
        assertEquals("f_ur", rCube.getFillets().get(0).getId(), "Redone fillet ID matches");
        assertEquals("d_ur", rCube.getDrafts().get(0).getId(), "Redone draft ID matches");
    }

    private static void testTopologySignatureDeterminism() {
        System.out.println("\n--- 9. Topology Signature Determinism ---");
        shape_item_ui_main cube1 = new shape_item_ui_main("sig_cube", "Block", basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20), 0, 0, 0, 0, 0);
        cube1.addChamfer(new chamfer_feature_ui_main("c_sig", "sig_cube", "Chamfer", "TOP_FRONT", 3.0));
        cube1.addFillet(new fillet_feature_ui_main("f_sig", "sig_cube", "Fillet", "TOP_BACK", 2.0));
        cube1.addDraft(new draft_feature_ui_main("d_sig", "sig_cube", "Draft", face_kind_ui_main.FRONT, 5.0, face_kind_ui_main.BOTTOM));

        String sig1 = topology_signature_ui_main.generateSignature(cube1.getTopology());
        assertTrue(sig1 != null && sig1.length() == 64, "Topology signature is valid 64-char SHA-256 hash");

        // Rebuild and verify determinism
        cube1.rebuild();
        String sig2 = topology_signature_ui_main.generateSignature(cube1.getTopology());
        assertEquals(sig1, sig2, "Topology signature is deterministic across rebuilds (sig1 == sig2)");

        // Modify parameter and verify signature changes
        cube1.getChamfers().get(0).setDistance(4.5);
        cube1.rebuild();
        String sig3 = topology_signature_ui_main.generateSignature(cube1.getTopology());
        assertTrue(!sig1.equals(sig3), "Topology signature changes when chamfer parameter is modified");
    }
}
