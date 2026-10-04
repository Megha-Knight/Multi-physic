import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import ui.File_Types.document_serializer_ui_main;
import ui.featuremanager.feature_tree_node_ui_main;
import ui.workspace.drafting.faces.face_geometry_helper_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.faces.face_selection_resolver_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.drafting.shape_history_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.boundary_relationship_ui_main;
import ui.workspace.drafting.topology.edge_machining_analyzer_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_signature_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * TestStage6AdvancedHoles.java
 * Comprehensive Stage 6 Test Suite for Advanced Hole Types (Countersink & Counterbore).
 */
public class TestStage6AdvancedHoles {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    private static void check(String desc, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("[PASS] " + desc);
        } else {
            failedTests++;
            System.err.println("[FAIL] " + desc);
        }
    }

    private static void assertClose(String desc, double actual, double expected, double tol) {
        check(desc + " (" + actual + " ≈ " + expected + ")", Math.abs(actual - expected) <= tol);
    }

    public static void main(String[] args) {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}

        System.out.println("=================================================");
        System.out.println("Running Stage 6 Test Suite: Advanced Hole Types");
        System.out.println("=================================================");

        testCountersinkValidation();
        testCounterboreValidation();
        testPlanarCountersink();
        testPlanarCounterbore();
        testCylinderCountersinkAndCounterbore();
        testConeCountersinkAndCounterbore();
        testAdvancedHoleRimValidation();
        testFeatureManagerAndEditing();
        testSaveLoadSerialization();
        testUndoRedo();
        testTopologySignatureDeterminism();

        System.out.println("\n=================================================");
        System.out.printf("STAGE 6 RESULTS: %d/%d Passed (%d Failed)%n", passedTests, totalTests, failedTests);
        System.out.println("=================================================");

        if (failedTests > 0) {
            System.exit(1);
        } else {
            System.out.println("All Stage 6 Advanced Hole tests passed successfully!");
            System.exit(0);
        }
    }

    private static void testCountersinkValidation() {
        System.out.println("\n--- 1. Countersink Parameter Validation ---");
        // Valid countersink: Dia=8, Depth=15, CsDia=12, CsAngle=90
        hole_feature_ui_main validCs = new hole_feature_ui_main(
                "cs1", "shape1", HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                0, 0, 8.0, 15.0, false, 12.0, 90.0, 0, 0);
        check("Valid countersink is valid", validCs.isValid());
        check("Countersink outer radius is csDiameter / 2", Math.abs(validCs.getOuterRadius() - 6.0) < 1e-4);
        check("Countersink cone depth > 0", validCs.getConeDepth() > 0.1);

        // Invalid: csDia <= Dia
        hole_feature_ui_main invCsDia = new hole_feature_ui_main(
                "cs2", "shape1", HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                0, 0, 8.0, 15.0, false, 8.0, 90.0, 0, 0);
        check("Countersink with csDia <= Dia rejected", !invCsDia.isValid());

        // Invalid: csAngle <= 0
        hole_feature_ui_main invCsAngle = new hole_feature_ui_main(
                "cs3", "shape1", HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                0, 0, 8.0, 15.0, false, 12.0, 0.0, 0, 0);
        check("Countersink with csAngle <= 0 rejected", !invCsAngle.isValid());

        // Invalid: csAngle >= 180
        hole_feature_ui_main invCsAngle180 = new hole_feature_ui_main(
                "cs4", "shape1", HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                0, 0, 8.0, 15.0, false, 12.0, 180.0, 0, 0);
        check("Countersink with csAngle >= 180 rejected", !invCsAngle180.isValid());

        // Invalid: Depth <= ConeDepth (blind hole)
        double cd = validCs.getConeDepth();
        hole_feature_ui_main invCsDepth = new hole_feature_ui_main(
                "cs5", "shape1", HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                0, 0, 8.0, cd * 0.5, false, 12.0, 90.0, 0, 0);
        check("Countersink with blind depth < cone depth rejected", !invCsDepth.isValid());
    }

    private static void testCounterboreValidation() {
        System.out.println("\n--- 2. Counterbore Parameter Validation ---");
        // Valid counterbore: Dia=8, Depth=20, CbDia=14, CbDepth=6
        hole_feature_ui_main validCb = new hole_feature_ui_main(
                "cb1", "shape1", HoleType.COUNTERBORE, face_kind_ui_main.TOP,
                0, 0, 8.0, 20.0, false, 0, 90.0, 14.0, 6.0);
        check("Valid counterbore is valid", validCb.isValid());
        check("Counterbore outer radius is cbDiameter / 2", Math.abs(validCb.getOuterRadius() - 7.0) < 1e-4);

        // Invalid: cbDia <= Dia
        hole_feature_ui_main invCbDia = new hole_feature_ui_main(
                "cb2", "shape1", HoleType.COUNTERBORE, face_kind_ui_main.TOP,
                0, 0, 8.0, 20.0, false, 0, 90.0, 7.5, 6.0);
        check("Counterbore with cbDia <= Dia rejected", !invCbDia.isValid());

        // Invalid: cbDepth <= 0
        hole_feature_ui_main invCbDepth = new hole_feature_ui_main(
                "cb3", "shape1", HoleType.COUNTERBORE, face_kind_ui_main.TOP,
                0, 0, 8.0, 20.0, false, 0, 90.0, 14.0, 0.0);
        check("Counterbore with cbDepth <= 0 rejected", !invCbDepth.isValid());

        // Invalid: blind depth <= cbDepth
        hole_feature_ui_main invCbTotalDepth = new hole_feature_ui_main(
                "cb4", "shape1", HoleType.COUNTERBORE, face_kind_ui_main.TOP,
                0, 0, 8.0, 5.0, false, 0, 90.0, 14.0, 6.0);
        check("Counterbore with blind depth <= cbDepth rejected", !invCbTotalDepth.isValid());
    }

    private static void testPlanarCountersink() {
        System.out.println("\n--- 3. Planar Host Machining — Countersink ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-25, 0, -25), new Point3D(25, -50, 25));
        hole_feature_ui_main cs = new hole_feature_ui_main(
                "cs_top", cube.getId(), HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                0, 0, 6.0, 18.0, false, 12.0, 90.0, 0, 0);
        cube.addHole(cs);

        topology_body_ui_main topo = cube.getTopology();
        check("Topology generated for planar countersink", topo != null);

        var rem = topo.getDerivedFaceById(topo.getId() + ":F:TOP:REMAINING");
        check("Remaining face exists on TOP", rem != null);
        check("Remaining face inner loop has countersink outer radius (6.0)",
                rem != null && !rem.getInnerLoops().isEmpty() && Math.abs(rem.getInnerLoops().get(0).getRadius() - 6.0) < 1e-4);

        var wall = topo.getDerivedFaceById(topo.getId() + ":F:TOP:HOLE_WALL:cs_top");
        check("Hole Wall derived face exists", wall != null);

        var floor = topo.getDerivedFaceById(topo.getId() + ":F:TOP:HOLE_FLOOR:cs_top");
        check("Hole Floor derived face exists", floor != null);

        var res = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.TOP, 0, 0);
        check("Face resolver finds countersink feature at origin", res.isDerived() && res.targetHole() != null);
    }

    private static void testPlanarCounterbore() {
        System.out.println("\n--- 4. Planar Host Machining — Counterbore ---");
        shape_item_ui_main cuboid = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-30, 0, -20), new Point3D(30, -40, 20));
        hole_feature_ui_main cb = new hole_feature_ui_main(
                "cb_front", cuboid.getId(), HoleType.COUNTERBORE, face_kind_ui_main.FRONT,
                0, 0, 8.0, 25.0, false, 0, 90.0, 16.0, 5.0);
        cuboid.addHole(cb);

        topology_body_ui_main topo = cuboid.getTopology();
        check("Topology generated for planar counterbore", topo != null);

        var rem = topo.getDerivedFaceById(topo.getId() + ":F:FRONT:REMAINING");
        check("Remaining face exists on FRONT", rem != null);
        check("Remaining face inner loop has counterbore outer radius (8.0)",
                rem != null && !rem.getInnerLoops().isEmpty() && Math.abs(rem.getInnerLoops().get(0).getRadius() - 8.0) < 1e-4);

        var wall = topo.getDerivedFaceById(topo.getId() + ":F:FRONT:HOLE_WALL:cb_front");
        check("Hole Wall derived face exists on FRONT", wall != null);

        var floor = topo.getDerivedFaceById(topo.getId() + ":F:FRONT:HOLE_FLOOR:cb_front");
        check("Hole Floor derived face exists on FRONT", floor != null);
    }

    private static void testCylinderCountersinkAndCounterbore() {
        System.out.println("\n--- 5. Cylinder Host Machining (Cap & Lateral) ---");
        shape_item_ui_main cyl = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(25, -50, 0));
        hole_feature_ui_main csCap = new hole_feature_ui_main(
                "cs_cyl_cap", cyl.getId(), HoleType.COUNTERSINK, face_kind_ui_main.TOP_CAP,
                0, 0, 8.0, 20.0, false, 14.0, 90.0, 0, 0);
        hole_feature_ui_main cbLat = new hole_feature_ui_main(
                "cb_cyl_lat", cyl.getId(), HoleType.COUNTERBORE, face_kind_ui_main.CYLINDER_LATERAL,
                0, 0, 6.0, 15.0, false, 0, 90.0, 12.0, 4.0);
        cyl.addHole(csCap);
        cyl.addHole(cbLat);

        topology_body_ui_main topo = cyl.getTopology();
        check("Cylinder topology non-null with advanced holes", topo != null);

        var remCap = topo.getDerivedFaceById(topo.getId() + ":F:TOP_CAP:REMAINING");
        check("Cylinder TOP_CAP remaining face exists", remCap != null);
        check("Cylinder TOP_CAP inner loop has countersink radius (7.0)",
                remCap != null && !remCap.getInnerLoops().isEmpty() && Math.abs(remCap.getInnerLoops().get(0).getRadius() - 7.0) < 1e-4);

        var remLat = topo.getDerivedFaceById(topo.getId() + ":F:CYLINDER_LATERAL:REMAINING");
        check("Cylinder Lateral remaining face exists", remLat != null);

        var wallLat = topo.getDerivedFaceById(topo.getId() + ":F:CYLINDER_LATERAL:HOLE_WALL:cb_cyl_lat");
        check("Cylinder Lateral counterbore wall exists", wallLat != null);

        // Transformation safety
        cyl.applyWorldDelta(100, 50, -30);
        cyl.setRotationY(90.0);
        check("Cylinder transformed has 2 holes attached", cyl.getHoles().size() == 2);
        check("Transformed cylinder topology recomputes cleanly", cyl.getTopology() != null);
    }

    private static void testConeCountersinkAndCounterbore() {
        System.out.println("\n--- 6. Cone Host Machining (Base & Lateral) ---");
        shape_item_ui_main cone = new shape_item_ui_main(basic_shapes_ui_main.CONE, new Point3D(0, 0, 0), new Point3D(30, -60, 0));
        hole_feature_ui_main csLat = new hole_feature_ui_main(
                "cs_cone_lat", cone.getId(), HoleType.COUNTERSINK, face_kind_ui_main.CONE_LATERAL,
                0, -10, 6.0, 12.0, false, 10.0, 90.0, 0, 0);
        hole_feature_ui_main cbBase = new hole_feature_ui_main(
                "cb_cone_base", cone.getId(), HoleType.COUNTERBORE, face_kind_ui_main.BASE_CAP,
                0, 0, 8.0, 20.0, false, 0, 90.0, 16.0, 5.0);
        cone.addHole(csLat);
        cone.addHole(cbBase);

        topology_body_ui_main topo = cone.getTopology();
        check("Cone topology non-null with advanced holes", topo != null);

        var remLat = topo.getDerivedFaceById(topo.getId() + ":F:CONE_LATERAL:REMAINING");
        check("Cone Lateral remaining face exists", remLat != null);

        var wallLat = topo.getDerivedFaceById(topo.getId() + ":F:CONE_LATERAL:HOLE_WALL:cs_cone_lat");
        check("Cone Lateral countersink wall exists", wallLat != null);

        var remBase = topo.getDerivedFaceById(topo.getId() + ":F:BASE_CAP:REMAINING");
        check("Cone Base remaining face exists", remBase != null);

        // Transformed cone
        cone.applyWorldDelta(-50, 0, 120);
        cone.setRotationY(45.0);
        check("Cone transformed has 2 holes attached", cone.getHoles().size() == 2);
    }

    private static void testAdvancedHoleRimValidation() {
        System.out.println("\n--- 7. Rim & Boundary Validation for Advanced Holes ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-20, 0, -20), new Point3D(20, -40, 20));
        // Face width = 40, half-width = 20
        // Countersink: Dia=8, CsDia=16 (OuterR = 8). At U = 14: U + OuterR = 22 > 20 -> Crosses Edge
        hole_feature_ui_main csCrossing = new hole_feature_ui_main(
                "cs_cross", cube.getId(), HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                14, 0, 8.0, 15.0, false, 16.0, 90.0, 0, 0);
        boundary_relationship_ui_main rel = edge_machining_analyzer_ui_main.analyzeHole(cube, cube.getTopology(), csCrossing);
        check("Countersink crossing boundary detected", rel != null && !rel.isValid());
        check("Countersink crossing diagnostic mentions boundary", rel != null && rel.getDiagnosticMessage() != null);

        // Counterbore: Dia=6, CbDia=18 (OuterR = 9). At U = 0, V = 13: V + OuterR = 22 > 20 -> Crosses Edge
        hole_feature_ui_main cbCrossing = new hole_feature_ui_main(
                "cb_cross", cube.getId(), HoleType.COUNTERBORE, face_kind_ui_main.TOP,
                0, 13, 6.0, 15.0, false, 0, 90.0, 18.0, 4.0);
        boundary_relationship_ui_main relCb = edge_machining_analyzer_ui_main.analyzeHole(cube, cube.getTopology(), cbCrossing);
        check("Counterbore crossing boundary detected", relCb != null && !relCb.isValid());
    }

    private static void testFeatureManagerAndEditing() {
        System.out.println("\n--- 8. Feature Manager & Parameter Editing ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-25, 0, -25), new Point3D(25, -50, 25));
        hole_feature_ui_main cs = new hole_feature_ui_main(
                "cs_edit", cube.getId(), "Countersink01", HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                0, 0, 8.0, 15.0, false, 14.0, 90.0, 0, 0, hole_feature_ui_main.CutoutShape.CIRCLE, 0);
        cube.addHole(cs);

        feature_tree_node_ui_main node = feature_tree_node_ui_main.forHole(cube, cs);
        check("Feature Manager node created for countersink", node != null);
        check("Node label contains Countersink", node.getLabel().contains("Countersink"));

        // Chained edits on countersink
        cs.setDiameter(10.0);
        cs.setCsDiameter(18.0);
        cs.setCsAngle(120.0);
        cs.setDepth(25.0);
        check("Countersink diameter edited to 10.0", Math.abs(cs.getDiameter() - 10.0) < 1e-4);
        check("Countersink csDiameter edited to 18.0", Math.abs(cs.getCsDiameter() - 18.0) < 1e-4);
        check("Countersink csAngle edited to 120.0", Math.abs(cs.getCsAngle() - 120.0) < 1e-4);
        check("Countersink depth edited to 25.0", Math.abs(cs.getDepth() - 25.0) < 1e-4);
        check("Countersink remains valid after chained edits", cs.isValid());

        // Counterbore chained edits
        shape_item_ui_main cube2 = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-25, 0, -25), new Point3D(25, -50, 25));
        hole_feature_ui_main cb = new hole_feature_ui_main(
                "cb_edit", cube2.getId(), "Counterbore01", HoleType.COUNTERBORE, face_kind_ui_main.TOP,
                0, 0, 6.0, 20.0, false, 0, 90.0, 12.0, 5.0, hole_feature_ui_main.CutoutShape.CIRCLE, 0);
        cube2.addHole(cb);
        cb.setDiameter(8.0);
        cb.setCbDiameter(16.0);
        cb.setCbDepth(8.0);
        cb.setDepth(30.0);
        check("Counterbore diameter edited to 8.0", Math.abs(cb.getDiameter() - 8.0) < 1e-4);
        check("Counterbore cbDiameter edited to 16.0", Math.abs(cb.getCbDiameter() - 16.0) < 1e-4);
        check("Counterbore cbDepth edited to 8.0", Math.abs(cb.getCbDepth() - 8.0) < 1e-4);
        check("Counterbore depth edited to 30.0", Math.abs(cb.getDepth() - 30.0) < 1e-4);
        check("Counterbore remains valid after chained edits", cb.isValid());
    }

    private static void testSaveLoadSerialization() {
        System.out.println("\n--- 9. Save / Load Serialization ---");
        File tempFile = new File("scratch/stage6_test_model.nd");
        List<shape_item_ui_main> shapes = new ArrayList<>();
        shape_item_ui_main cyl = new shape_item_ui_main("cyl_ser", "CylPart", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(20, -50, 0), 10, 0, 20, 0, 0);
        hole_feature_ui_main cs = new hole_feature_ui_main(
                "cs_ser", "cyl_ser", "CS_Hole", HoleType.COUNTERSINK, face_kind_ui_main.CYLINDER_LATERAL,
                5.0, -10.0, 6.0, 15.0, false, 12.0, 82.0, 0, 0, hole_feature_ui_main.CutoutShape.CIRCLE, 0);
        hole_feature_ui_main cb = new hole_feature_ui_main(
                "cb_ser", "cyl_ser", "CB_Hole", HoleType.COUNTERBORE, face_kind_ui_main.TOP_CAP,
                2.0, 3.0, 8.0, 22.0, false, 0, 90.0, 14.0, 6.0, hole_feature_ui_main.CutoutShape.CIRCLE, 0);
        cyl.addHole(cs);
        cyl.addHole(cb);
        shapes.add(cyl);

        boolean saved = document_serializer_ui_main.saveToNd(tempFile, shapes);
        check("Model with advanced holes saved to .nd", saved && tempFile.exists());

        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tempFile);
        check("Loaded shapes list non-empty", loaded != null && !loaded.isEmpty());
        if (loaded != null && !loaded.isEmpty()) {
            shape_item_ui_main loadedCyl = loaded.get(0);
            check("Loaded shape has 2 holes", loadedCyl.getHoles().size() == 2);

            hole_feature_ui_main loadedCs = loadedCyl.getHoles().stream().filter(h -> h.getHoleType() == HoleType.COUNTERSINK).findFirst().orElse(null);
            check("Loaded countersink exists", loadedCs != null);
            if (loadedCs != null) {
                check("Loaded CS faceKind is CYLINDER_LATERAL", loadedCs.getFaceKind() == face_kind_ui_main.CYLINDER_LATERAL);
                assertClose("Loaded CS diameter", loadedCs.getDiameter(), 6.0, 1e-4);
                assertClose("Loaded CS csDiameter", loadedCs.getCsDiameter(), 12.0, 1e-4);
                assertClose("Loaded CS csAngle", loadedCs.getCsAngle(), 82.0, 1e-4);
                assertClose("Loaded CS depth", loadedCs.getDepth(), 15.0, 1e-4);
            }

            hole_feature_ui_main loadedCb = loadedCyl.getHoles().stream().filter(h -> h.getHoleType() == HoleType.COUNTERBORE).findFirst().orElse(null);
            check("Loaded counterbore exists", loadedCb != null);
            if (loadedCb != null) {
                check("Loaded CB faceKind is TOP_CAP", loadedCb.getFaceKind() == face_kind_ui_main.TOP_CAP);
                assertClose("Loaded CB diameter", loadedCb.getDiameter(), 8.0, 1e-4);
                assertClose("Loaded CB cbDiameter", loadedCb.getCbDiameter(), 14.0, 1e-4);
                assertClose("Loaded CB cbDepth", loadedCb.getCbDepth(), 6.0, 1e-4);
                assertClose("Loaded CB depth", loadedCb.getDepth(), 22.0, 1e-4);
            }
        }
        if (tempFile.exists()) tempFile.delete();
    }

    private static void testUndoRedo() {
        System.out.println("\n--- 10. Undo / Redo Integration ---");
        shape_history_ui_main history = new shape_history_ui_main();
        shape_item_ui_main cube = new shape_item_ui_main("cube_ur", "HistCube", basic_shapes_ui_main.CUBE,
                new Point3D(-25, 0, -25), new Point3D(25, -50, 25), 0, 0, 0, 0, 0);

        List<shape_item_ui_main> list = new ArrayList<>(List.of(cube));

        // State 0: Cube without holes
        history.pushSnapshot(list);

        // State 1: Add countersink
        hole_feature_ui_main cs = new hole_feature_ui_main(
                "cs_ur", cube.getId(), HoleType.COUNTERSINK, face_kind_ui_main.TOP,
                0, 0, 8.0, 20.0, false, 14.0, 90.0, 0, 0);
        cube.addHole(cs);

        // Undo -> Should have 0 holes
        List<shape_item_ui_main> undoList = history.undo(list);
        check("Undo restores state to 0 holes", undoList != null && undoList.get(0).getHoles().isEmpty());

        // Redo -> Should have 1 hole (countersink)
        List<shape_item_ui_main> redoList = history.redo(undoList);
        check("Redo restores countersink", redoList != null && redoList.get(0).getHoles().size() == 1
                && redoList.get(0).getHoles().get(0).getHoleType() == HoleType.COUNTERSINK);
    }

    private static void testTopologySignatureDeterminism() {
        System.out.println("\n--- 11. Topology Signature Determinism ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-25, 0, -25), new Point3D(25, -50, 25));
        hole_feature_ui_main cb = new hole_feature_ui_main(
                "cb_sig", cube.getId(), HoleType.COUNTERBORE, face_kind_ui_main.TOP,
                0, 0, 8.0, 20.0, false, 0, 90.0, 16.0, 5.0);
        cube.addHole(cb);

        String sig1 = topology_signature_ui_main.generateSignature(cube.getTopology());
        check("Signature is valid SHA-256", sig1 != null && sig1.length() == 64);

        cube.rebuild();
        String sig2 = topology_signature_ui_main.generateSignature(cube.getTopology());
        check("Signature is deterministic across rebuilds (sig1 == sig2)", sig1 != null && sig1.equals(sig2));
    }
}
