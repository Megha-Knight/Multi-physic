package scratch;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.File_Types.document_parse_helper_ui_main;
import ui.File_Types.document_serializer_ui_main;
import ui.featuremanager.feature_tree_node_ui_main;
import ui.workspace.drafting.booleans.boolean_feature_ui_main;
import ui.workspace.drafting.booleans.boolean_op_type_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.features.feature_type_ui_main;
import ui.workspace.drafting.helical.helical_path_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.profiles.profile_loop_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.profiles.profile_validator_ui_main;
import ui.workspace.drafting.revolve.revolve_axis_type_ui_main;
import ui.workspace.drafting.revolve.revolve_axis_ui_main;
import ui.workspace.drafting.revolve.revolve_direction_ui_main;
import ui.workspace.drafting.revolve.revolve_editor_dialog_ui_main;
import ui.workspace.drafting.revolve.revolve_feature_ui_main;
import ui.workspace.drafting.revolve.revolve_mesh_builder_ui_main;
import ui.workspace.drafting.revolve.revolve_mesh_quad_helper_ui_main;
import ui.workspace.drafting.revolve.revolve_solid_evaluator_ui_main;
import ui.workspace.drafting.revolve.revolve_validator_ui_main;
import ui.workspace.drafting.revolve.shape_revolve_holder_ui_main;
import ui.workspace.drafting.shape_history_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.shell.shell_direction_ui_main;
import ui.workspace.drafting.shell.shell_feature_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_derived_face_ui_main;
import ui.workspace.drafting.topology.topology_signature_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TestStage11.java
 * Comprehensive Verification Test Suite for Stage 11: Revolve / Rotational Features / Helical & Axial Solid Modeling.
 */
public class TestStage11 {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Running Stage 11 Test Suite: Revolve & Helical Modeling");
        System.out.println("=================================================");

        try {
            javafx.application.Platform.startup(() -> {});
        } catch (Throwable ignored) {}

        try {
            testFeatureTypeEnums();
            testRevolveAxisTypesAndRotation();
            testRevolveDirectionEnums();
            testRevolveFeatureModel();
            testRevolveValidation();
            testFull360RevolveSolidMesh();
            testPartialRevolveSolidMesh();
            testReverseAndSymmetricRevolveMesh();
            testRevolveMeshBuilder();
            testRevolveMeshQuadAndCapHelpers();
            testRevolveTopologyIntegration();
            testShapeRevolveHolderOperations();
            testFeatureManagerIntegration();
            testHelicalPathModelAndSampling();
            testHelicalValidationAndPitch();
            testParameterModificationAndRebuild();
            testSpatialTransforms();
            testSerializationSaveLoad();
            testDocumentParseHelper();
            testUndoRedoLifecycle();
            testDeterministicSignatures();
            testBooleanSolidInteractions();
            testShellInteractions();
            testMachiningFeatureInteractions();
            testMultiBodyIsolation();
            testDegenerateAndEdgeCaseFailures();
            testRevolveEditorDialog();
        } catch (Exception e) {
            System.err.println("[CRITICAL ERROR] Test suite aborted with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n=================================================");
        System.out.printf("STAGE 11 RESULTS: %d/%d Passed (%d Failed)\n", passed, (passed + failed), failed);
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        } else {
            System.out.println("All Stage 11 Revolve and Helical Solid Modeling tests passed successfully!");
            System.exit(0);
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("[PASS] " + description);
            passed++;
        } else {
            System.err.println("[FAIL] " + description);
            failed++;
        }
    }

    private static profile_reference_ui_main createSampleProfile(String id, double xOff, double yOff, double radius) {
        return profile_reference_ui_main.createCircle(id, "Circ " + id, radius, 0.0, 16);
    }

    private static profile_reference_ui_main createRectangleProfile(String id, double width, double height) {
        return profile_reference_ui_main.createRectangle(id, "Rect " + id, width, height, 0.0);
    }

    // --- 1. Feature Type Enums ---
    private static void testFeatureTypeEnums() {
        System.out.println("\n--- 1. Feature Type Enums ---");
        check(feature_type_ui_main.REVOLVE != null, "REVOLVE enum defined");
        check(feature_type_ui_main.REVOLVE_FEATURE != null, "REVOLVE_FEATURE enum defined");
        check(feature_type_ui_main.HELIX != null, "HELIX enum defined");
        check(feature_type_ui_main.HELIX_FEATURE != null, "HELIX_FEATURE enum defined");
        check(feature_type_ui_main.REVOLVE.getLabel().contains("Revolve"), "REVOLVE display name matches");
    }

    // --- 2. Revolve Axis Types and Rodrigues Rotation ---
    private static void testRevolveAxisTypesAndRotation() {
        System.out.println("\n--- 2. Revolve Axis Types and Rodrigues Rotation ---");
        check(revolve_axis_type_ui_main.X_AXIS != null, "X_AXIS enum exists");
        check(revolve_axis_type_ui_main.Y_AXIS != null, "Y_AXIS enum exists");
        check(revolve_axis_type_ui_main.Z_AXIS != null, "Z_AXIS enum exists");
        check(revolve_axis_type_ui_main.CUSTOM_AXIS != null, "CUSTOM_AXIS enum exists");
        check("X Axis".equals(revolve_axis_type_ui_main.X_AXIS.getLabel()), "X Axis label matches");
        check("Y Axis".equals(revolve_axis_type_ui_main.Y_AXIS.getLabel()), "Y Axis label matches");
        check("Z Axis".equals(revolve_axis_type_ui_main.Z_AXIS.getLabel()), "Z Axis label matches");
        check("Custom Axis".equals(revolve_axis_type_ui_main.CUSTOM_AXIS.getLabel()), "Custom Axis label matches");

        revolve_axis_ui_main xAxis = revolve_axis_ui_main.xAxis();
        check(xAxis.getType() == revolve_axis_type_ui_main.X_AXIS, "X axis type is X_AXIS");
        check(xAxis.getDirection().equals(new Point3D(1, 0, 0)), "X axis direction is (1,0,0)");

        revolve_axis_ui_main yAxis = revolve_axis_ui_main.yAxis();
        check(yAxis.getType() == revolve_axis_type_ui_main.Y_AXIS, "Y axis type is Y_AXIS");
        check(yAxis.getDirection().equals(new Point3D(0, 1, 0)), "Y axis direction is (0,1,0)");

        revolve_axis_ui_main zAxis = revolve_axis_ui_main.zAxis();
        check(zAxis.getType() == revolve_axis_type_ui_main.Z_AXIS, "Z axis type is Z_AXIS");
        check(zAxis.getDirection().equals(new Point3D(0, 0, 1)), "Z axis direction is (0,0,1)");

        Point3D pt = new Point3D(10, 0, 0);
        Point3D rot90 = zAxis.rotatePoint(pt, 90.0);
        check(Math.abs(rot90.getX()) < 1e-4, "90-deg Z-rot X ~ 0");
        check(Math.abs(rot90.getY() - 10.0) < 1e-4, "90-deg Z-rot Y ~ 10");
        check(Math.abs(rot90.getZ()) < 1e-4, "90-deg Z-rot Z ~ 0");

        Point3D rot180 = zAxis.rotatePoint(pt, 180.0);
        check(Math.abs(rot180.getX() - (-10.0)) < 1e-4, "180-deg Z-rot X ~ -10");
        check(Math.abs(rot180.getY()) < 1e-4, "180-deg Z-rot Y ~ 0");

        revolve_axis_ui_main custom = new revolve_axis_ui_main(revolve_axis_type_ui_main.CUSTOM_AXIS, new Point3D(5, 0, 0), new Point3D(0, 1, 0));
        check(custom.isValid(), "Custom axis is valid");
        check(custom.getOrigin().getX() == 5.0, "Custom axis origin X is 5.0");
        Point3D customRot = custom.rotatePoint(new Point3D(15, 0, 0), 180.0);
        check(Math.abs(customRot.getX() - (-5.0)) < 1e-4, "Custom axis rotation X correct around offset origin");
    }

    // --- 3. Revolve Direction Enums ---
    private static void testRevolveDirectionEnums() {
        System.out.println("\n--- 3. Revolve Direction Enums ---");
        check(revolve_direction_ui_main.FORWARD != null, "FORWARD direction enum");
        check(revolve_direction_ui_main.REVERSE != null, "REVERSE direction enum");
        check(revolve_direction_ui_main.SYMMETRIC != null, "SYMMETRIC direction enum");
        check(revolve_direction_ui_main.FORWARD.getLabel().contains("Forward"), "Forward label match");
        check(revolve_direction_ui_main.REVERSE.getLabel().contains("Reverse"), "Reverse label match");
        check(revolve_direction_ui_main.SYMMETRIC.getLabel().contains("Symmetric"), "Symmetric label match");
    }

    // --- 4. Revolve Feature Model ---
    private static void testRevolveFeatureModel() {
        System.out.println("\n--- 4. Revolve Feature Model ---");
        profile_reference_ui_main prof = createSampleProfile("Prof_1", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_1", "host_1", "Revolve 1", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);

        check("Rev_1".equals(rev.getId()), "Feature ID match");
        check("host_1".equals(rev.getOwnerShapeId()), "Owner shape ID match");
        check("Revolve 1".equals(rev.getName()), "Feature name match");
        check(rev.getAngle() == 360.0, "Feature angle match");
        check(rev.getDirection() == revolve_direction_ui_main.FORWARD, "Feature direction match");
        check(rev.isSolid(), "Feature isSolid match");
        check(rev.isValid(), "Feature is initially valid");
        check(rev.isVisible(), "Feature is initially visible");
        check(rev.getState() == feature_state_ui_main.CLEAN, "Feature state is CLEAN");

        rev.setName("Updated Rev");
        check("Updated Rev".equals(rev.getName()), "Feature name setter works");
        rev.setVisible(false);
        check(!rev.isVisible(), "Feature visibility setter works");
    }

    // --- 5. Revolve Validation ---
    private static void testRevolveValidation() {
        System.out.println("\n--- 5. Revolve Validation ---");
        profile_reference_ui_main prof = createSampleProfile("Prof_1", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_1", "host_1", "Revolve 1", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);

        check(revolve_validator_ui_main.validate(rev.getProfile(), rev.getAxis(), rev.getAngle(), rev.isSolid()).isValid(), "Valid feature passes validation");

        revolve_feature_ui_main nullProf = new revolve_feature_ui_main("Rev_NullProf", "host_1", "Revolve NullProf", null, axis, 360.0, revolve_direction_ui_main.FORWARD, true);
        check(!revolve_validator_ui_main.validate(nullProf.getProfile(), nullProf.getAxis(), nullProf.getAngle(), nullProf.isSolid()).isValid(), "Null profile rejected");
        check(!nullProf.isValid(), "Null profile marked invalid");

        revolve_feature_ui_main zeroAngle = new revolve_feature_ui_main("Rev_ZeroAng", "host_1", "Revolve ZeroAng", prof, axis, 0.0, revolve_direction_ui_main.FORWARD, true);
        check(!revolve_validator_ui_main.validate(zeroAngle.getProfile(), zeroAngle.getAxis(), zeroAngle.getAngle(), zeroAngle.isSolid()).isValid(), "Zero angle rejected");

        revolve_feature_ui_main negAngle = new revolve_feature_ui_main("Rev_NegAng", "host_1", "Revolve NegAng", prof, axis, -45.0, revolve_direction_ui_main.FORWARD, true);
        check(revolve_validator_ui_main.validate(negAngle.getProfile(), negAngle.getAxis(), negAngle.getAngle(), negAngle.isSolid()).isValid(), "Signed angle validated");
    }

    // --- 6. Full 360 Revolve Solid Mesh ---
    private static void testFull360RevolveSolidMesh() {
        System.out.println("\n--- 6. Full 360 Revolve Solid Mesh ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        profile_reference_ui_main prof = createSampleProfile("Prof_Circle", 30, 0, 10);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_360", host.getId(), "Revolve 360", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);

        TriangleMesh mesh = revolve_solid_evaluator_ui_main.evaluate(host, rev);
        check(mesh != null, "Full 360 revolve generates non-null mesh");
        check(mesh.getPoints().size() > 0, "Mesh points generated: " + mesh.getPoints().size());
        check(mesh.getFaces().size() > 0, "Mesh faces generated: " + mesh.getFaces().size());
    }

    // --- 7. Partial Revolve Solid Mesh ---
    private static void testPartialRevolveSolidMesh() {
        System.out.println("\n--- 7. Partial Revolve Solid Mesh ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        profile_reference_ui_main prof = createRectangleProfile("Prof_Rect", 20, 10);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev90 = new revolve_feature_ui_main("Rev_90", host.getId(), "Revolve 90", prof, axis, 90.0, revolve_direction_ui_main.FORWARD, true);

        TriangleMesh mesh = revolve_solid_evaluator_ui_main.evaluate(host, rev90);
        check(mesh != null, "90-deg revolve mesh evaluated");
        check(mesh.getPoints().size() > 0, "Partial mesh points non-empty");
        check(mesh.getFaces().size() > 0, "Partial mesh faces non-empty (includes end caps)");
    }

    // --- 8. Reverse and Symmetric Revolve Mesh ---
    private static void testReverseAndSymmetricRevolveMesh() {
        System.out.println("\n--- 8. Reverse and Symmetric Revolve Mesh ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        profile_reference_ui_main prof = createRectangleProfile("Prof_Rect", 20, 10);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();

        revolve_feature_ui_main revRev = new revolve_feature_ui_main("Rev_Rev", host.getId(), "Revolve Rev", prof, axis, 120.0, revolve_direction_ui_main.REVERSE, true);
        TriangleMesh meshRev = revolve_solid_evaluator_ui_main.evaluate(host, revRev);
        check(meshRev != null && meshRev.getFaces().size() > 0, "REVERSE direction revolve generated mesh");

        revolve_feature_ui_main revSym = new revolve_feature_ui_main("Rev_Sym", host.getId(), "Revolve Sym", prof, axis, 120.0, revolve_direction_ui_main.SYMMETRIC, true);
        TriangleMesh meshSym = revolve_solid_evaluator_ui_main.evaluate(host, revSym);
        check(meshSym != null && meshSym.getFaces().size() > 0, "SYMMETRIC direction revolve generated mesh");
    }

    // --- 9. Revolve Mesh Builder ---
    private static void testRevolveMeshBuilder() {
        System.out.println("\n--- 9. Revolve Mesh Builder ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        profile_reference_ui_main prof = createSampleProfile("Prof_1", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_1", host.getId(), "Revolve 1", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);

        Node view = revolve_mesh_builder_ui_main.buildRevolveNode(host, rev, false);
        check(view instanceof MeshView, "Revolve mesh builder produces MeshView");
        check(((MeshView) view).getMesh() instanceof TriangleMesh, "MeshView contains TriangleMesh");
        check(((MeshView) view).getMaterial() != null, "MeshView has PhongMaterial");

        Node viewSel = revolve_mesh_builder_ui_main.buildRevolveNode(host, rev, true);
        check(viewSel instanceof MeshView, "Selected Revolve mesh builder produces MeshView");
    }

    // --- 9b. Revolve Mesh Quad & Cap Helpers ---
    private static void testRevolveMeshQuadAndCapHelpers() {
        System.out.println("\n--- 9b. Revolve Mesh Quad & Cap Helpers ---");
        TriangleMesh tm = new TriangleMesh();
        tm.getTexCoords().addAll(0, 0);
        revolve_mesh_quad_helper_ui_main.addQuadStrip(tm.getFaces(), 0, 4, 4);
        check(tm.getFaces().size() == 4 * 2 * 2 * 6, "addQuadStrip generates double-sided triangles");

        TriangleMesh tmCap = new TriangleMesh();
        tmCap.getTexCoords().addAll(0, 0);
        revolve_mesh_quad_helper_ui_main.addCapFan(tmCap.getFaces(), 8, 0, 4, false);
        check(tmCap.getFaces().size() == 4 * 2 * 6, "addCapFan generates double-sided cap fan triangles");
    }

    // --- 10. Revolve Topology Integration ---
    private static void testRevolveTopologyIntegration() {
        System.out.println("\n--- 10. Revolve Topology Integration ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        profile_reference_ui_main prof = createRectangleProfile("Prof_1", 20, 10);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_1", host.getId(), "Revolve 1", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);
        host.addRevolve(rev);
        host.rebuild();

        topology_body_ui_main topo = host.getTopology();
        check(topo != null, "Topology body non-null after revolve rebuild");
        check(topo.getDerivedFaces().size() > 0, "Revolve derived faces registered in topology: " + topo.getDerivedFaces().size());

        topology_derived_face_ui_main face = topo.getDerivedFaces().iterator().next();
        check("Rev_1".equals(face.getCreatingFeatureId()), "Derived face creatingFeatureId matches revolve id");
    }

    // --- 11. Shape Revolve Holder Operations ---
    private static void testShapeRevolveHolderOperations() {
        System.out.println("\n--- 11. Shape Revolve Holder Operations ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        check(!host.getRevolveHolder().hasRevolves(), "Initially no revolves");

        profile_reference_ui_main prof = createSampleProfile("Prof_1", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev1 = new revolve_feature_ui_main("Rev_1", host.getId(), "Revolve 1", prof, axis, 180.0, revolve_direction_ui_main.FORWARD, true);
        revolve_feature_ui_main rev2 = new revolve_feature_ui_main("Rev_2", host.getId(), "Revolve 2", prof, axis, 90.0, revolve_direction_ui_main.FORWARD, true);

        host.addRevolve(rev1);
        host.addRevolve(rev2);
        check(host.getRevolves().size() == 2, "Holder contains 2 revolves");
        check(host.getRevolve("Rev_1") == rev1, "Get revolve by ID Rev_1");
        check(host.getRevolve("Rev_2") == rev2, "Get revolve by ID Rev_2");

        host.removeRevolve("Rev_1");
        check(host.getRevolves().size() == 1, "Holder contains 1 revolve after removal");
        check(host.getRevolve("Rev_1") == null, "Rev_1 is gone");

        host.clearRevolves();
        check(host.getRevolves().isEmpty(), "Holder is empty after clear");
    }

    // --- 12. Feature Manager Integration ---
    private static void testFeatureManagerIntegration() {
        System.out.println("\n--- 12. Feature Manager Integration ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        profile_reference_ui_main prof = createSampleProfile("Prof_1", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_Main", host.getId(), "Revolve Main", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);

        feature_tree_node_ui_main node = feature_tree_node_ui_main.forRevolve(host, rev);
        check(node != null, "Feature tree node created for revolve");
        check(node.isRevolve(), "node.isRevolve() returns true");
        check(node.getRevolve() == rev, "node.getRevolve() returns feature");
        check(node.getLabel().contains("Revolve Main"), "Node label contains name");
        check(node.getIcon() != null, "Node icon non-null");

        rev.setState(feature_state_ui_main.INVALID);
        check(node.getLabel().contains("[INVALID]"), "Invalid feature node label displays [INVALID]");
    }

    // --- 13. Helical Path Model and Sampling ---
    private static void testHelicalPathModelAndSampling() {
        System.out.println("\n--- 13. Helical Path Model and Sampling ---");
        helical_path_ui_main helix = new helical_path_ui_main("helix_1", "Helix 1", revolve_axis_ui_main.zAxis(), 25.0, 10.0, 5.0, true);

        check(helix.getRadius() == 25.0, "Helix radius matches");
        check(helix.getPitch() == 10.0, "Helix pitch matches");
        check(helix.getTurns() == 5.0, "Helix turns match");
        check(helix.isClockwise(), "Helix clockwise matches");
        check(Math.abs(helix.getHeight() - 50.0) < 1e-4, "Helix total height is pitch * turns = 50.0");

        List<Point3D> samples = helix.samplePoints(32);
        check(samples != null && samples.size() > 0, "Helix samples non-empty: " + samples.size());
        Point3D start = samples.get(0);
        check(Math.abs(start.getZ()) < 1e-3, "Helix start Z is 0.0");

        Point3D end = samples.get(samples.size() - 1);
        check(Math.abs(end.getZ() - 50.0) < 1e-1, "Helix end Z is total height 50.0");
    }

    // --- 14. Helical Validation and Pitch ---
    private static void testHelicalValidationAndPitch() {
        System.out.println("\n--- 14. Helical Validation and Pitch ---");
        helical_path_ui_main validHelix = new helical_path_ui_main("helix_v", "Helix V", revolve_axis_ui_main.zAxis(), 10.0, 5.0, 3.0, true);
        check(validHelix.getHeight() == 15.0, "Helix height calculation valid");
        check(validHelix.getLength() > 0, "Helix length calculation non-zero");

        helical_path_ui_main ccwHelix = new helical_path_ui_main("helix_ccw", "Helix CCW", revolve_axis_ui_main.zAxis(), 10.0, 5.0, 2.0, false);
        check(!ccwHelix.isClockwise(), "Counter-clockwise correctly flagged");

        validHelix.setRadius(15.0);
        validHelix.setPitch(8.0);
        validHelix.setTurns(4.0);
        validHelix.setClockwise(false);
        check(validHelix.getRadius() == 15.0, "Helix radius updated");
        check(validHelix.getPitch() == 8.0, "Helix pitch updated");
        check(validHelix.getTurns() == 4.0, "Helix turns updated");
    }

    // --- 15. Parameter Modification and Rebuild ---
    private static void testParameterModificationAndRebuild() {
        System.out.println("\n--- 15. Parameter Modification and Rebuild ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        profile_reference_ui_main prof = createSampleProfile("Prof_1", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_Param", host.getId(), "Revolve Param", prof, axis, 180.0, revolve_direction_ui_main.FORWARD, true);

        host.addRevolve(rev);
        host.rebuild();
        String sig1 = topology_signature_ui_main.generateSignature(host.getTopology());

        // Mutate angle parameter
        rev.setAngle(270.0);
        host.rebuild();
        String sig2 = topology_signature_ui_main.generateSignature(host.getTopology());

        check(rev.getAngle() == 270.0, "Angle parameter updated");
        check(sig1 != null && sig2 != null, "Signatures computed");
        check(!sig1.equals(sig2), "Topology signature changes when angle parameter is modified");
    }

    // --- 16. Spatial Transforms ---
    private static void testSpatialTransforms() {
        System.out.println("\n--- 16. Spatial Transforms ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20));
        profile_reference_ui_main prof = createSampleProfile("Prof_1", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_Trans", host.getId(), "Revolve Trans", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);
        host.addRevolve(rev);

        host.setWorldTranslation(50.0, 30.0, 20.0);
        host.rebuild();

        check(host.getWorldX() == 50.0, "Host translated X");
        check(host.getWorldY() == 30.0, "Host translated Y");
        check(host.getWorldZ() == 20.0, "Host translated Z");
        check(host.getTopology() != null, "Topology body valid under translation");
    }

    // --- 17. Serialization Save and Load ---
    private static void testSerializationSaveLoad() {
        System.out.println("\n--- 17. Serialization Save and Load ---");
        File tempFile = null;
        try {
            tempFile = File.createTempFile("test_stage11_revolve", ".nd");
            List<shape_item_ui_main> shapes = new ArrayList<>();
            shape_item_ui_main host = new shape_item_ui_main("ser_shape", "Ser Shape", basic_shapes_ui_main.CUBOID, new Point3D(-20, 0, -20), new Point3D(20, 20, 20), 0, 0, 0, 0, 0);
            profile_reference_ui_main prof = createRectangleProfile("Prof_Ser", 20, 10);
            revolve_axis_ui_main axis = new revolve_axis_ui_main(revolve_axis_type_ui_main.Y_AXIS, new Point3D(0, 0, 0), new Point3D(0, 1, 0));
            revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_Ser", host.getId(), "Revolve Ser", prof, axis, 270.0, revolve_direction_ui_main.SYMMETRIC, true);
            host.addRevolve(rev);
            host.rebuild();
            shapes.add(host);

            document_serializer_ui_main.saveToFile(tempFile, shapes);
            check(tempFile.exists() && tempFile.length() > 0, "Serialized .nd file created");

            List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tempFile);
            check(loaded != null && loaded.size() == 1, "Loaded 1 shape from file");

            shape_item_ui_main loadedHost = loaded.get(0);
            check(loadedHost.getRevolves().size() == 1, "Loaded shape contains 1 revolve");
            revolve_feature_ui_main loadedRev = loadedHost.getRevolves().get(0);
            check("Rev_Ser".equals(loadedRev.getId()), "Loaded revolve ID match");
            check(loadedRev.getAngle() == 270.0, "Loaded revolve angle match");
            check(loadedRev.getDirection() == revolve_direction_ui_main.SYMMETRIC, "Loaded revolve direction match");
            check(loadedRev.getAxis().getType() == revolve_axis_type_ui_main.Y_AXIS, "Loaded revolve axis type match");
        } catch (Exception e) {
            check(false, "Serialization test failed with exception: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
        }
    }

    // --- 17b. Document Parse Helper ---
    private static void testDocumentParseHelper() {
        System.out.println("\n--- 17b. Document Parse Helper ---");
        List<revolve_feature_ui_main> out = new ArrayList<>();
        document_parse_helper_ui_main.parseRevolveLine("rev_test,360.0,X_AXIS,0.0,0.0,0.0,1.0,0.0,0.0,FORWARD,true,Test+Rev", "host_shape", out);
        check(out.size() == 1, "parseRevolveLine parses valid revolve string");
        revolve_feature_ui_main revParsed = out.get(0);
        check("rev_test".equals(revParsed.getId()), "Parsed revolve ID match");
        check("Test Rev".equals(revParsed.getName()), "Parsed revolve Name match");
        check(revParsed.getAxis().getType() == revolve_axis_type_ui_main.X_AXIS, "Parsed axis type match");
        check(revParsed.getDirection() == revolve_direction_ui_main.FORWARD, "Parsed direction match");
    }

    // --- 18. Undo Redo Lifecycle ---
    private static void testUndoRedoLifecycle() {
        System.out.println("\n--- 18. Undo Redo Lifecycle ---");
        shape_history_ui_main history = new shape_history_ui_main();
        shape_item_ui_main host = new shape_item_ui_main("undo_host", "Undo Host", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(30, 30, 30), 0, 0, 0, 0, 0);

        List<shape_item_ui_main> state0 = List.of(host);
        history.pushSnapshot(state0);

        // State 1: add Revolve
        profile_reference_ui_main prof = createSampleProfile("Prof_Undo", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_Undo", host.getId(), "Revolve Undo", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);
        host.addRevolve(rev);
        host.rebuild();
        List<shape_item_ui_main> state1 = List.of(host);
        history.pushSnapshot(state1);

        // State 2: modify Revolve
        rev.setSolid(false);
        host.rebuild();
        List<shape_item_ui_main> state2 = List.of(host);

        // Undo to State 1
        List<shape_item_ui_main> uState1 = history.undo(state2);
        check(uState1 != null && uState1.get(0).getRevolves().get(0).isSolid(), "Undo restored solid revolve");

        // Undo to State 0
        List<shape_item_ui_main> uState0 = history.undo(uState1);
        check(uState0 != null && uState0.get(0).getRevolves().isEmpty(), "Undo restored solid without Revolve");

        // Redo to State 1
        List<shape_item_ui_main> rState1 = history.redo(uState0);
        check(rState1 != null && rState1.get(0).getRevolves().size() == 1, "Redo restored Revolve feature");
    }

    // --- 19. Deterministic Signatures ---
    private static void testDeterministicSignatures() {
        System.out.println("\n--- 19. Deterministic Signatures ---");
        shape_item_ui_main host1 = new shape_item_ui_main("det_shape_1", "Host 1", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(40, 50, 40), 0, 0, 0, 0, 0);
        profile_reference_ui_main prof1 = createSampleProfile("Prof_A", 20, 0, 5);
        revolve_axis_ui_main axis1 = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev1 = new revolve_feature_ui_main("Rev_A", host1.getId(), "Revolve A", prof1, axis1, 360.0, revolve_direction_ui_main.FORWARD, true);
        host1.addRevolve(rev1);

        String baseSig = topology_signature_ui_main.generateSignature(host1.getTopology());
        check(baseSig != null && baseSig.length() == 64, "Initial signature is valid 64-char SHA-256 hex");

        for (int i = 1; i <= 10; i++) {
            host1.rebuild();
            String iterSig = topology_signature_ui_main.generateSignature(host1.getTopology());
            check(baseSig.equals(iterSig), "Deterministic rebuild loop iteration " + i + " matches base signature");
        }
    }

    // --- 20. Boolean Solid Interactions ---
    private static void testBooleanSolidInteractions() {
        System.out.println("\n--- 20. Boolean Solid Interactions ---");
        shape_item_ui_main target = new shape_item_ui_main("bool_target", "Target Box", basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(50, 50, 50), 0, 0, 0, 0, 0);
        shape_item_ui_main tool = new shape_item_ui_main("bool_tool", "Tool Revolve", basic_shapes_ui_main.CUBOID, new Point3D(10, 0, 10), new Point3D(40, 60, 40), 0, 0, 0, 0, 0);

        profile_reference_ui_main prof = createSampleProfile("Prof_Bool", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("tool_rev", tool.getId(), "Tool Revolve", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);
        tool.addRevolve(rev);

        boolean_feature_ui_main boolCut = new boolean_feature_ui_main("bcut1", target.getId(), tool.getId(), boolean_op_type_ui_main.SUBTRACT, "Revolve Cut");
        target.addBoolean(boolCut);

        check(target.hasBooleans(), "Target body has boolean feature");
        check(target.getBooleans().get(0).getOpType() == boolean_op_type_ui_main.SUBTRACT, "Boolean operation is SUBTRACT");
    }

    // --- 21. Shell Interactions ---
    private static void testShellInteractions() {
        System.out.println("\n--- 21. Shell Interactions ---");
        shape_item_ui_main host = new shape_item_ui_main("shell_host", "Revolve Solid", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(40, 60, 40), 0, 0, 0, 0, 0);
        profile_reference_ui_main prof = createSampleProfile("Prof_Shell", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("rev_for_shell", host.getId(), "Revolve Base", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);
        host.addRevolve(rev);

        shell_feature_ui_main shell = new shell_feature_ui_main("sh_on_rev", host.getId(), "Revolve Shell", 3.0, shell_direction_ui_main.INWARD, List.of(face_kind_ui_main.TOP));
        host.addShell(shell);

        check(host.hasShells(), "Host body accepts shell on revolve feature");
        check(host.getShells().get(0).getThickness() == 3.0, "Shell thickness is 3.0");
    }

    // --- 22. Machining Feature Interactions ---
    private static void testMachiningFeatureInteractions() {
        System.out.println("\n--- 22. Machining Feature Interactions ---");
        shape_item_ui_main host = new shape_item_ui_main("mach_host", "Mach Host", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(50, 50, 50), 0, 0, 0, 0, 0);
        profile_reference_ui_main prof = createSampleProfile("Prof_Mach", 20, 0, 5);
        revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev = new revolve_feature_ui_main("rev_mach", host.getId(), "Revolve", prof, axis, 360.0, revolve_direction_ui_main.FORWARD, true);
        host.addRevolve(rev);

        fillet_feature_ui_main fillet = new fillet_feature_ui_main("fil1", host.getId(), "Fillet 1", "EDGE_TOP_FRONT", 2.0);
        host.addFillet(fillet);
        check(!host.getFillets().isEmpty(), "Fillet feature added to revolve host body");

        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main("ch1", host.getId(), "Chamfer 1", "EDGE_BOT_BACK", 1.5);
        host.addChamfer(chamfer);
        check(!host.getChamfers().isEmpty(), "Chamfer feature added to revolve host body");

        draft_feature_ui_main draft = new draft_feature_ui_main("dr1", host.getId(), "Draft 1", face_kind_ui_main.FRONT, 3.0, face_kind_ui_main.BOTTOM);
        host.addDraft(draft);
        check(!host.getDrafts().isEmpty(), "Draft feature added to revolve host body");
    }

    // --- 23. Multi-Body Isolation ---
    private static void testMultiBodyIsolation() {
        System.out.println("\n--- 23. Multi-Body Isolation ---");
        shape_item_ui_main body1 = new shape_item_ui_main("mb_body1", "Body 1", basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(30, 30, 30), 0, 0, 0, 0, 0);
        shape_item_ui_main body2 = new shape_item_ui_main("mb_body2", "Body 2", basic_shapes_ui_main.CUBE, new Point3D(100, 0, 0), new Point3D(150, 50, 50), 100, 0, 0, 0, 0);

        profile_reference_ui_main prof1 = createSampleProfile("Prof_Body1", 20, 0, 5);
        revolve_axis_ui_main axis1 = revolve_axis_ui_main.zAxis();
        revolve_feature_ui_main rev1 = new revolve_feature_ui_main("rev_body1", body1.getId(), "Body 1 Revolve", prof1, axis1, 360.0, revolve_direction_ui_main.FORWARD, true);
        body1.addRevolve(rev1);

        String sig2Before = topology_signature_ui_main.generateSignature(body2.getTopology());

        // Modifying body 1 must not alter body 2
        rev1.setAngle(180.0);
        body1.rebuild();

        String sig2After = topology_signature_ui_main.generateSignature(body2.getTopology());
        check(sig2Before.equals(sig2After), "Body 2 topology signature is unchanged when modifying Body 1 Revolve");
    }

    // --- 24. Degenerate and Edge Case Failures ---
    private static void testDegenerateAndEdgeCaseFailures() {
        System.out.println("\n--- 24. Degenerate and Edge Case Failures ---");
        shape_item_ui_main host = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(0, 0, 0), new Point3D(20, 20, 20));

        TriangleMesh nullMesh = revolve_solid_evaluator_ui_main.evaluate(host, null);
        check(nullMesh == null, "Null revolve feature yields null mesh");

        List<Point3D> degeneratePts = new ArrayList<>();
        degeneratePts.add(new Point3D(0, 0, 0));
        degeneratePts.add(new Point3D(10, 0, 0)); // Only 2 points
        profile_loop_ui_main degenLoop = new profile_loop_ui_main(degeneratePts, false);
        profile_reference_ui_main degenProf = new profile_reference_ui_main("Prof_Degen", "Degen", profile_reference_ui_main.ProfileType.POLYGON, degenLoop, 10, 0, 0, 0);
        revolve_feature_ui_main degenRev = new revolve_feature_ui_main("Rev_Degen", host.getId(), "Degen Rev", degenProf, revolve_axis_ui_main.zAxis(), 360.0, revolve_direction_ui_main.FORWARD, true);

        TriangleMesh degenMesh = revolve_solid_evaluator_ui_main.evaluate(host, degenRev);
        check(degenMesh == null, "Degenerate profile (< 3 points) yields null mesh");

        revolve_axis_ui_main zeroAxis = new revolve_axis_ui_main(revolve_axis_type_ui_main.CUSTOM_AXIS, Point3D.ZERO, Point3D.ZERO);
        check(!zeroAxis.isValid(), "Zero-length axis direction is invalid");
    }

    // --- 25. Revolve Editor Dialog ---
    private static void testRevolveEditorDialog() throws Exception {
        System.out.println("\n--- 25. Revolve Editor Dialog ---");
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        javafx.application.Platform.runLater(() -> {
            try {
                profile_reference_ui_main prof = createSampleProfile("Prof_Dlg", 20, 0, 5);
                revolve_axis_ui_main axis = revolve_axis_ui_main.zAxis();
                revolve_feature_ui_main rev = new revolve_feature_ui_main("Rev_Dlg", "host_dlg", "Dlg Rev", prof, axis, 240.0, revolve_direction_ui_main.FORWARD, true);

                revolve_editor_dialog_ui_main dlg = new revolve_editor_dialog_ui_main(rev);
                check(dlg != null, "Revolve editor dialog constructed successfully");
                check("Edit Revolve".equals(dlg.getTitle()), "Edit dialog title is Edit Revolve");

                revolve_editor_dialog_ui_main newDlg = new revolve_editor_dialog_ui_main(null);
                check("Create Revolve".equals(newDlg.getTitle()), "Create dialog title is Create Revolve");
            } catch (Exception e) {
                check(false, "Dialog test failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
    }
}
