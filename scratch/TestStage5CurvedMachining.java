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
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_signature_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * TestStage5CurvedMachining.java
 * Comprehensive Stage 5 Test Suite for Curved-Surface Machining (Cylinder & Cone).
 */
public class TestStage5CurvedMachining {

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
        System.out.println("Running Stage 5 Test Suite: Curved-Surface Machining");
        System.out.println("=================================================\n");

        testCylinderAnalyticalPicking();
        testCylinderTransformedPicking();
        testCylinderSimpleHoleMachining();
        testConeAnalyticalPicking();
        testConeTransformedPicking();
        testConeSimpleHoleMachining();
        testFeatureManagerAndEditing();
        testSaveLoadSerialization();
        testUndoRedoIntegration();
        testTopologySignatureStability();

        System.out.println("\n=================================================");
        System.out.println(String.format("STAGE 5 RESULTS: %d/%d Passed (%d Failed)", passedTests, totalTests, failedTests));
        System.out.println("=================================================");

        if (failedTests > 0) {
            System.exit(1);
        } else {
            System.out.println("All Stage 5 Curved-Surface Machining tests passed successfully!");
            System.exit(0);
        }
    }

    private static void testCylinderAnalyticalPicking() {
        System.out.println("--- 1. Cylinder Analytical Picking & Frames ---");
        shape_item_ui_main cyl = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(20, -50, 0));
        // Radius = 20mm, Height = 50mm, cylinder from Y=0 to Y=-50
        // Shoot ray from front (+Z=100) towards cylinder center (0, -25, 0)
        double[] rayFront = new double[]{0, -25, 100, 0, 0, -1};
        face_reference_ui_main hitFront = face_picker_ui_main.pickFace(rayFront, cyl);
        check("Cylinder Front Ray Hits Lateral Face", hitFront != null && hitFront.getFaceKind() == face_kind_ui_main.CYLINDER_LATERAL);
        if (hitFront != null) {
            assertClose("Hit Z on Front Rim", hitFront.getWorldHitPoint().getZ(), 20.0, 1e-3);
            assertClose("Hit Y at Mid-height", hitFront.getWorldHitPoint().getY(), -25.0, 1e-3);
            assertClose("Outward Normal Z = +1", hitFront.getWorldNormal().getZ(), 1.0, 1e-3);
            assertClose("Outward Normal Y = 0", hitFront.getWorldNormal().getY(), 0.0, 1e-3);

            Point3D u = hitFront.getUAxis(), v = hitFront.getVAxis(), n = hitFront.getWorldNormal();
            Point3D cross = u.crossProduct(v).normalize();
            check("Stable Tangent Frame (U x V aligned with N)", cross.dotProduct(n) > 0.99);
        }

        // Ray shooting above top cap (Y = -60) -> Miss
        double[] rayAbove = new double[]{0, -60, 100, 0, 0, -1};
        check("Ray above cylinder bounds rejected", face_picker_ui_main.pickFace(rayAbove, cyl) == null);

        // Ray shooting below bottom cap (Y = +10) -> Miss
        double[] rayBelow = new double[]{0, 10, 100, 0, 0, -1};
        check("Ray below cylinder bounds rejected", face_picker_ui_main.pickFace(rayBelow, cyl) == null);
    }

    private static void testCylinderTransformedPicking() {
        System.out.println("\n--- 2. Cylinder Transformed Picking (Translation & Rotation) ---");
        shape_item_ui_main cyl = new shape_item_ui_main("cyl1", "TestCylinder", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(20, -50, 0), 100, 0, 200, 0, 90);
        // Translated to (100, 0, 200), Rotated 90 deg around Y
        double[] ray = new double[]{200, -25, 200, -1, 0, 0};
        face_reference_ui_main hit = face_picker_ui_main.pickFace(ray, cyl);
        check("Transformed cylinder hit lateral face", hit != null && hit.getFaceKind() == face_kind_ui_main.CYLINDER_LATERAL);
        if (hit != null) {
            assertClose("Transformed hit X = 120", hit.getWorldHitPoint().getX(), 120.0, 1e-2);
            assertClose("Transformed hit Z = 200", hit.getWorldHitPoint().getZ(), 200.0, 1e-2);
            assertClose("Transformed outward normal X = +1", hit.getWorldNormal().getX(), 1.0, 1e-2);
        }
    }

    private static void testCylinderSimpleHoleMachining() {
        System.out.println("\n--- 3. Cylinder Simple Hole Machining & Topology ---");
        shape_item_ui_main cyl = new shape_item_ui_main("cyl_mach", "MachinedCylinder", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(20, -60, 0), 0, 0, 0, 0, 0);

        hole_feature_ui_main hole = new hole_feature_ui_main("hole_cyl_1", cyl.getId(), HoleType.SIMPLE,
                face_kind_ui_main.CYLINDER_LATERAL, 0, 0, 8.0, 10.0, false);

        check("Cylinder lateral hole is initially valid", hole.isValid());
        cyl.addHole(hole);
        topology_body_ui_main topo = cyl.getTopology();
        check("Topology generated for cylinder", topo != null);

        var rem = topo.getDerivedFaceById(topo.getId() + ":F:CYLINDER_LATERAL:REMAINING");
        check("Remaining face created on CYLINDER_LATERAL", rem != null);

        var wall = topo.getDerivedFaceById(topo.getId() + ":F:CYLINDER_LATERAL:HOLE_WALL:hole_cyl_1");
        check("Hole Wall created on CYLINDER_LATERAL", wall != null);

        var floor = topo.getDerivedFaceById(topo.getId() + ":F:CYLINDER_LATERAL:HOLE_FLOOR:hole_cyl_1");
        check("Hole Floor created on CYLINDER_LATERAL", floor != null);

        // Face selection resolver
        var res = face_selection_resolver_ui_main.resolveFace(cyl, face_kind_ui_main.CYLINDER_LATERAL, 0, 0);
        check("Face resolver finds hole on CYLINDER_LATERAL", res.isDerived() && res.targetHole() != null);
    }

    private static void testConeAnalyticalPicking() {
        System.out.println("\n--- 4. Cone Analytical Picking & Frames ---");
        shape_item_ui_main cone = new shape_item_ui_main(basic_shapes_ui_main.CONE, new Point3D(0, 0, 0), new Point3D(20, -50, 0));
        // Base at Y=0 (R=20), Apex at Y=-50 (R=0)
        // Mid-height Y = -25 -> Radius = 10mm
        double[] rayMid = new double[]{0, -25, 100, 0, 0, -1};
        face_reference_ui_main hitMid = face_picker_ui_main.pickFace(rayMid, cone);
        check("Cone Mid-height Ray Hits Lateral Face", hitMid != null && hitMid.getFaceKind() == face_kind_ui_main.CONE_LATERAL);
        if (hitMid != null) {
            assertClose("Hit Z on Cone Slant = 10", hitMid.getWorldHitPoint().getZ(), 10.0, 1e-2);
            assertClose("Hit Y on Cone Slant = -25", hitMid.getWorldHitPoint().getY(), -25.0, 1e-2);
            check("Outward Normal Y component > 0 (slant upward)", hitMid.getWorldNormal().getY() > 0);
            check("Outward Normal Z component > 0 (front-facing)", hitMid.getWorldNormal().getZ() > 0);

            Point3D u = hitMid.getUAxis(), v = hitMid.getVAxis(), n = hitMid.getWorldNormal();
            Point3D cross = u.crossProduct(v).normalize();
            check("Stable Tangent Frame for Cone", cross.dotProduct(n) > 0.99);
        }

        // Ray shooting above apex (Y = -60) -> Miss
        double[] rayAbove = new double[]{0, -60, 100, 0, 0, -1};
        check("Ray above cone apex rejected", face_picker_ui_main.pickFace(rayAbove, cone) == null);

        // Ray shooting outside base (X=30, Y=-25) -> Miss
        double[] rayMiss = new double[]{30, -25, 100, 0, 0, -1};
        check("Ray missing cone lateral radius rejected", face_picker_ui_main.pickFace(rayMiss, cone) == null);
    }

    private static void testConeTransformedPicking() {
        System.out.println("\n--- 5. Cone Transformed Picking (Translation & Rotation) ---");
        shape_item_ui_main cone = new shape_item_ui_main("cone1", "TestCone", basic_shapes_ui_main.CONE,
                new Point3D(0, 0, 0), new Point3D(20, -50, 0), 50, 0, 50, 0, 180);
        // Translated to (50, 0, 50), Rotated 180 deg around Y
        double[] ray = new double[]{50, -25, -50, 0, 0, 1};
        face_reference_ui_main hit = face_picker_ui_main.pickFace(ray, cone);
        check("Transformed cone hit lateral face", hit != null && hit.getFaceKind() == face_kind_ui_main.CONE_LATERAL);
        if (hit != null) {
            assertClose("Transformed hit X = 50", hit.getWorldHitPoint().getX(), 50.0, 1e-2);
            assertClose("Transformed hit Z = 40", hit.getWorldHitPoint().getZ(), 40.0, 1e-2);
        }
    }

    private static void testConeSimpleHoleMachining() {
        System.out.println("\n--- 6. Cone Simple Hole Machining & Topology ---");
        shape_item_ui_main cone = new shape_item_ui_main("cone_mach", "MachinedCone", basic_shapes_ui_main.CONE,
                new Point3D(0, 0, 0), new Point3D(20, -60, 0), 0, 0, 0, 0, 0);

        hole_feature_ui_main hole = new hole_feature_ui_main("hole_cone_1", cone.getId(), HoleType.SIMPLE,
                face_kind_ui_main.CONE_LATERAL, 0, 0, 6.0, 8.0, false);

        check("Cone lateral hole is valid", hole.isValid());
        cone.addHole(hole);
        topology_body_ui_main topo = cone.getTopology();
        check("Topology generated for cone", topo != null);

        var rem = topo.getDerivedFaceById(topo.getId() + ":F:CONE_LATERAL:REMAINING");
        check("Remaining face created on CONE_LATERAL", rem != null);

        var wall = topo.getDerivedFaceById(topo.getId() + ":F:CONE_LATERAL:HOLE_WALL:hole_cone_1");
        check("Hole Wall created on CONE_LATERAL", wall != null);

        var floor = topo.getDerivedFaceById(topo.getId() + ":F:CONE_LATERAL:HOLE_FLOOR:hole_cone_1");
        check("Hole Floor created on CONE_LATERAL", floor != null);
    }

    private static void testFeatureManagerAndEditing() {
        System.out.println("\n--- 7. Feature Manager & Parameter Editing ---");
        shape_item_ui_main cyl = new shape_item_ui_main("cyl_fm", "FMCylinder", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(25, -60, 0), 0, 0, 0, 0, 0);

        hole_feature_ui_main h1 = new hole_feature_ui_main("h_curv_1", cyl.getId(), HoleType.SIMPLE,
                face_kind_ui_main.CYLINDER_LATERAL, 0, 0, 10.0, 15.0, false);
        cyl.addHole(h1);

        feature_tree_node_ui_main holeNode = feature_tree_node_ui_main.forHole(cyl, h1);
        check("Feature Manager hole node created", holeNode != null);
        check("Feature Manager node is hole", holeNode.isHole());
        check("Feature Manager node label contains Ø10.0", holeNode.getLabel().contains("10.0"));

        // Edit parameters
        h1.setDiameter(12.0);
        h1.setDepth(20.0);
        cyl.rebuild();
        check("Hole diameter edited to 12.0", cyl.getHole("h_curv_1").getDiameter() == 12.0);
        check("Hole depth edited to 20.0", cyl.getHole("h_curv_1").getDepth() == 20.0);

        // Invalid parameter validation
        hole_feature_ui_main invalidHole = new hole_feature_ui_main("h_bad", cyl.getId(), HoleType.SIMPLE,
                face_kind_ui_main.CYLINDER_LATERAL, 0, 0, -5.0, 10.0, false);
        check("Negative diameter hole is marked invalid", !invalidHole.isValid());
    }

    private static void testSaveLoadSerialization() {
        System.out.println("\n--- 8. Save / Load Serialization ---");
        shape_item_ui_main cone = new shape_item_ui_main("cone_save", "SavedCone", basic_shapes_ui_main.CONE,
                new Point3D(0, 0, 0), new Point3D(30, -70, 0), 10, 20, 30, 0, 45);

        hole_feature_ui_main hole = new hole_feature_ui_main("hole_save_1", cone.getId(), HoleType.SIMPLE,
                face_kind_ui_main.CONE_LATERAL, 5.0, -10.0, 8.0, 12.0, false);
        cone.addHole(hole);

        File tmp = new File("scratch/test_stage5_model.nd");
        boolean saved = document_serializer_ui_main.saveToNd(tmp, List.of(cone));
        check("Curved model saved to .nd", saved);

        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tmp);
        check("Loaded shapes list non-empty", loaded != null && loaded.size() == 1);
        if (loaded != null && !loaded.isEmpty()) {
            shape_item_ui_main s = loaded.get(0);
            check("Loaded shape type is CONE", s.getType() == basic_shapes_ui_main.CONE);
            check("Loaded shape has 1 hole", s.getHoles().size() == 1);
            hole_feature_ui_main lh = s.getHoles().get(0);
            check("Loaded hole faceKind is CONE_LATERAL", lh.getFaceKind() == face_kind_ui_main.CONE_LATERAL);
            assertClose("Loaded hole U", lh.getU(), 5.0, 1e-3);
            assertClose("Loaded hole V", lh.getV(), -10.0, 1e-3);
            assertClose("Loaded hole Diameter", lh.getDiameter(), 8.0, 1e-3);
            assertClose("Loaded hole Depth", lh.getDepth(), 12.0, 1e-3);
        }
        if (tmp.exists()) tmp.delete();
    }

    private static void testUndoRedoIntegration() {
        System.out.println("\n--- 9. Undo / Redo Integration ---");
        shape_history_ui_main history = new shape_history_ui_main();
        shape_item_ui_main cyl = new shape_item_ui_main("cyl_hist", "HistCylinder", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(20, -50, 0), 0, 0, 0, 0, 0);

        List<shape_item_ui_main> list = new ArrayList<>(List.of(cyl));

        // State 0: Cylinder without holes
        history.pushSnapshot(list);

        // State 1: Add curved hole
        hole_feature_ui_main hole = new hole_feature_ui_main("h_hist", cyl.getId(), HoleType.SIMPLE,
                face_kind_ui_main.CYLINDER_LATERAL, 0, 0, 6.0, 10.0, false);
        cyl.addHole(hole);

        // Undo -> Should have 0 holes
        List<shape_item_ui_main> undoList = history.undo(list);
        check("Undo restores state to 0 holes", undoList != null && undoList.get(0).getHoles().isEmpty());

        // Redo -> Should have 1 hole on CYLINDER_LATERAL
        List<shape_item_ui_main> redoList = history.redo(undoList);
        check("Redo restores curved hole", redoList != null && redoList.get(0).getHoles().size() == 1
                && redoList.get(0).getHoles().get(0).getFaceKind() == face_kind_ui_main.CYLINDER_LATERAL);
    }

    private static void testTopologySignatureStability() {
        System.out.println("\n--- 10. Topology Signature Stability ---");
        shape_item_ui_main cyl = new shape_item_ui_main("cyl_sig", "SigCyl", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(25, -60, 0), 0, 0, 0, 0, 0);
        hole_feature_ui_main h = new hole_feature_ui_main("h_sig", cyl.getId(), HoleType.SIMPLE,
                face_kind_ui_main.CYLINDER_LATERAL, 0, 0, 8.0, 12.0, false);
        cyl.addHole(h);

        String sig1 = topology_signature_ui_main.generateSignature(cyl.getTopology());
        check("Signature is valid SHA-256", sig1 != null && sig1.length() == 64);

        cyl.rebuild();
        String sig2 = topology_signature_ui_main.generateSignature(cyl.getTopology());
        check("Signature is deterministic across rebuilds (sig1 == sig2)", sig1 != null && sig1.equals(sig2));
    }
}
