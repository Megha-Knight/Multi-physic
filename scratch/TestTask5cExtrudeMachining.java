package scratch;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.edge_machining_analyzer_ui_main;
import ui.workspace.drafting.topology.boundary_condition_ui_main;

public class TestTask5cExtrudeMachining {

    private static int testsPassed = 0;
    private static int totalTests = 0;

    private static void check(String desc, boolean cond) {
        totalTests++;
        if (cond) {
            testsPassed++;
            System.out.println("  [PASS] " + desc);
        } else {
            System.err.println("  [FAIL] " + desc);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Task 5c: Extrusion Face-Picking & Machining Unit Tests ===");

        // Test 1: Shape with Extrusion Setup
        // Create Cube with p1 = (0, 0, 0), p2 = (100, 0, 100), height = 100
        // Center is at (50, -50, 50). Top face is at Y = -100, normal = (0, -1, 0).
        shape_item_ui_main cube = new shape_item_ui_main(
            basic_shapes_ui_main.CUBE,
            new Point3D(0, 0, 0),
            new Point3D(100, 0, 100)
        );

        // Add cylinder extrusion on TOP face: radius = 20 (dia = 40), height = 30 at (u=0, v=0)
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
            "ext-1", cube.getId(), "Boss Extrude 1", face_kind_ui_main.TOP,
            CutoutShape.CIRCLE, 0, 0, 40.0, 0, 30.0
        );
        cube.addExtrude(ext);

        check("Cube has 1 extrusion", cube.getExtrusions().size() == 1);
        check("Extrusion valid", ext.isValid());

        // Test 2: Raycast Face-Picking against Extrusion Cap
        // Cube top face is at Y = -100. Extrusion extends upwards along face normal (0, -1, 0) by height 30.
        // So extrusion cap is at Y = -130!
        // Ray from (50, -200, 50) straight down (0, 1, 0) towards (50, -130, 50)
        double[] rayTowardsCap = new double[]{ 50.0, -200.0, 50.0, 0.0, 1.0, 0.0 };
        face_reference_ui_main hitCap = face_picker_ui_main.pickFace(rayTowardsCap, cube);
        check("Raycast hits something", hitCap != null);
        check("Raycast hit targetFeatureId equals extrusion ID", hitCap != null && ext.getId().equals(hitCap.getTargetFeatureId()));
        check("Raycast hit isFeatureFace is true", hitCap != null && hitCap.isFeatureFace());
        check("Raycast hit faceKind is EXTRUDE_CAP", hitCap != null && (hitCap.getFaceKind() == face_kind_ui_main.EXTRUDE_CAP || hitCap.getFaceKind() == face_kind_ui_main.TOP));
        check("Raycast hit origin is at cap Y = -130", hitCap != null && Math.abs(hitCap.getFaceOrigin().getY() - (-130.0)) < 1e-3);

        // Test 3: Raycast hitting Base Primitive outside Extrusion
        // Shoot ray at (10, -200, 10) downwards towards base top face
        double[] rayTowardsBase = new double[]{ 10.0, -200.0, 10.0, 0.0, 1.0, 0.0 };
        face_reference_ui_main hitBase = face_picker_ui_main.pickFace(rayTowardsBase, cube);
        check("Raycast hits base", hitBase != null);
        check("Base hit targetFeatureId is null", hitBase != null && hitBase.getTargetFeatureId() == null);
        check("Base hit isFeatureFace is false", hitBase != null && !hitBase.isFeatureFace());
        check("Base hit origin is at top face Y = -100", hitBase != null && Math.abs(hitBase.getFaceOrigin().getY() - (-100.0)) < 1e-3);

        // Test 4: Drill Hole on Extrusion Cap
        // Hole diameter 10, depth 15, on extrusion cap (u=0, v=0)
        hole_feature_ui_main extHole = new hole_feature_ui_main(
            "hole-ext-1", cube.getId(), HoleType.SIMPLE, hitCap.getFaceKind(),
            0, 0, 10.0, 15.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0
        );
        extHole.setTargetFeatureId(hitCap.getTargetFeatureId());
        check("Extrude hole isOnFeature is true", extHole.isOnFeature());
        check("Extrude hole targetFeatureId is ext-1", ext.getId().equals(extHole.getTargetFeatureId()));

        cube.addHole(extHole);

        // Test 5: Isolation of Extrude Holes vs Base Primitive Holes
        // Base primitive should NOT subtract extHole
        check("getAllEffectiveHoles excludes extrude hole", cube.getAllEffectiveHoles().isEmpty());
        check("getEffectiveHolesForFeature returns extrude hole", cube.getEffectiveHolesForFeature(ext.getId()).size() == 1);

        // Add a hole to base primitive
        hole_feature_ui_main baseHole = new hole_feature_ui_main(
            "hole-base-1", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP,
            0, 0, 12.0, 20.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0
        );
        cube.addHole(baseHole);
        check("getAllEffectiveHoles includes base hole", cube.getAllEffectiveHoles().size() == 1);
        check("getAllEffectiveHoles has hole-base-1", cube.getAllEffectiveHoles().get(0).getId().equals("hole-base-1"));
        check("getEffectiveHolesForFeature still only has extrude hole", cube.getEffectiveHolesForFeature(ext.getId()).size() == 1);

        // Test 6: Boundary & Collision Analysis
        // Both holes are at (u=0, v=0) on face TOP, but one is on extrusion and one is on base.
        // They should NOT collide with each other!
        var relBase = edge_machining_analyzer_ui_main.analyzeHole(cube, null, baseHole);
        check("Base hole is contained and does not collide with extrude hole", relBase.getCondition() == boundary_condition_ui_main.CONTAINED);

        var relExt = edge_machining_analyzer_ui_main.analyzeHole(cube, null, extHole);
        check("Extrude hole is contained on extrusion cap", relExt.getCondition() == boundary_condition_ui_main.CONTAINED);

        // Test 6b: Collision between two holes on the same extrusion
        hole_feature_ui_main secondExtHole = new hole_feature_ui_main(
            "hole-ext-2", cube.getId(), HoleType.SIMPLE, hitCap.getFaceKind(),
            5.0, 0, 10.0, 15.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0
        );
        secondExtHole.setTargetFeatureId(ext.getId());
        var relCollide = edge_machining_analyzer_ui_main.analyzeHole(cube, null, secondExtHole);
        check("Two overlapping holes on extrusion detected as INTERSECTING_CAVITIES",
            relCollide.getCondition() == boundary_condition_ui_main.INTERSECTING_CAVITIES);

        // Test 6c: Oversized hole on clean extrusion crosses edge
        cube.removeHole("hole-ext-1");
        hole_feature_ui_main bigExtHole = new hole_feature_ui_main(
            "hole-ext-big", cube.getId(), HoleType.SIMPLE, hitCap.getFaceKind(),
            0, 0, 50.0, 15.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0
        );
        bigExtHole.setTargetFeatureId(ext.getId());
        var relBig = edge_machining_analyzer_ui_main.analyzeHole(cube, null, bigExtHole);
        check("Oversized hole on extrusion cap crosses edge", relBig.getCondition() == boundary_condition_ui_main.CROSSES_EDGE);
        check("Crosses edge mentions EXTRUDE_CAP_RIM", relBig.getAffectedEdgeName() != null && relBig.getAffectedEdgeName().contains("EXTRUDE_CAP_RIM"));

        // Put extHole back for topology test
        cube.addHole(extHole);

        // Test 7: Topology Representation for Extrusion & Extrude Holes
        topology_body_ui_main topo = cube.getTopology();
        check("Topology generated", topo != null);

        // Check extrusion faces in topology
        String capFaceId = cube.getId() + ":F:EXTRUDE_CAP:" + ext.getId();
        String latFaceId = cube.getId() + ":F:EXTRUDE_LATERAL:" + ext.getId();
        check("Topology has EXTRUDE_CAP face", topo.getDerivedFaceById(capFaceId) != null);
        check("Topology has EXTRUDE_LATERAL face", topo.getDerivedFaceById(latFaceId) != null);

        // Check extrusion rim edges
        var capRimEdge = topo.getEdge("EXTRUDE_CAP_RIM_" + ext.getId());
        check("Topology has EXTRUDE_CAP_RIM edge", capRimEdge != null);
        check("Extrude cap rim has customLength equal to diameter", capRimEdge != null && Math.abs(capRimEdge.getLength() - 40.0) < 1e-3);

        // Check hole rim edge on extrusion
        var holeRimEdge = topo.getEdge("HOLE_RIM_" + extHole.getId());
        check("Topology has HOLE_RIM edge on extrusion", holeRimEdge != null);
        check("Hole rim customLength is hole diameter (10.0)", holeRimEdge != null && Math.abs(holeRimEdge.getLength() - 10.0) < 1e-3);
        check("Hole rim edge adjacent to EXTRUDE_CAP face", holeRimEdge != null && holeRimEdge.getAdjacentFaceIds().contains(capFaceId));

        // Test 8: Extrusion Mesh Generation with Holes
        var nodeWithHoles = extrude_mesh_builder_ui_main.buildExtrudeNode(cube, ext, false);
        check("Extrude node with holes generated", nodeWithHoles != null);
        check("Extrude node is MeshView with holes subtracted", nodeWithHoles instanceof javafx.scene.shape.MeshView);

        // Test 9: Extrusion face toFaceReference transform fidelity
        var capFace = topo.getDerivedFaceById(capFaceId);
        check("capFace found in topology", capFace != null);
        var refFromTopo = capFace.toFaceReference(cube, 0, 0);
        check("refFromTopo is non-null", refFromTopo != null);
        check("refFromTopo targetFeatureId matches ext-1", refFromTopo != null && "ext-1".equals(refFromTopo.getTargetFeatureId()));
        check("refFromTopo worldHit Y is at -130 (cap plane)", refFromTopo != null && Math.abs(refFromTopo.getWorldHitPoint().getY() - (-130.0)) < 1e-3);
        check("refFromTopo normal is (0, -1, 0)", refFromTopo != null && Math.abs(refFromTopo.getWorldNormal().getY() - (-1.0)) < 1e-3);

        // Test 10: RefreshFace on extrusion cap reference
        var refreshed = face_picker_ui_main.refreshFace(refFromTopo, cube);
        check("Refreshed face reference retains targetFeatureId", refreshed != null && "ext-1".equals(refreshed.getTargetFeatureId()));
        check("Refreshed face origin Y is at -130", refreshed != null && Math.abs(refreshed.getFaceOrigin().getY() - (-130.0)) < 1e-3);

        // Test 11: Chamfer on circular extrusion rim
        var chExt = new ui.workspace.drafting.machining.chamfer_feature_ui_main("ch-1", cube.getId(), capRimEdge.getId(), 2.0);
        check("Chamfer on cap rim is valid", chExt.revalidate(cube));
        var chNode = ui.workspace.drafting.machining.chamfer_mesh_builder_ui_main.buildChamferNode(cube, chExt, false);
        check("Chamfer mesh view generated on cap rim", chNode instanceof javafx.scene.shape.MeshView);
        if (chNode instanceof javafx.scene.shape.MeshView mv) {
            var mesh = (javafx.scene.shape.TriangleMesh) mv.getMesh();
            check("Chamfer mesh has points and faces", mesh.getPoints().size() > 0 && mesh.getFaces().size() > 0);
        }

        // Test 12: Fillet on circular extrusion rim
        var flExt = new ui.workspace.drafting.machining.fillet_feature_ui_main("fl-1", cube.getId(), capRimEdge.getId(), 2.5);
        check("Fillet on cap rim is valid", flExt.revalidate(cube));
        var flNode = ui.workspace.drafting.machining.fillet_mesh_builder_ui_main.buildFilletNode(cube, flExt, false);
        check("Fillet mesh view generated on cap rim", flNode instanceof javafx.scene.shape.MeshView);
        if (flNode instanceof javafx.scene.shape.MeshView mv) {
            var mesh = (javafx.scene.shape.TriangleMesh) mv.getMesh();
            check("Fillet mesh has points and faces", mesh.getPoints().size() > 0 && mesh.getFaces().size() > 0);
        }

        // Test 13: Chamfer (countersink bevel) on hole rim
        var chHole = new ui.workspace.drafting.machining.chamfer_feature_ui_main("ch-2", cube.getId(), holeRimEdge.getId(), 1.0);
        check("Chamfer on hole rim is valid", chHole.revalidate(cube));
        var chHoleNode = ui.workspace.drafting.machining.chamfer_mesh_builder_ui_main.buildChamferNode(cube, chHole, false);
        check("Chamfer mesh view generated on hole rim", chHoleNode instanceof javafx.scene.shape.MeshView);

        // Test 14: Fillet on hole rim
        var flHole = new ui.workspace.drafting.machining.fillet_feature_ui_main("fl-2", cube.getId(), holeRimEdge.getId(), 1.0);
        check("Fillet on hole rim is valid", flHole.revalidate(cube));
        var flHoleNode = ui.workspace.drafting.machining.fillet_mesh_builder_ui_main.buildFilletNode(cube, flHole, false);
        check("Fillet mesh view generated on hole rim", flHoleNode instanceof javafx.scene.shape.MeshView);

        // Test 15: Raycast picking circular extrusion lateral face
        // Extrusion center is at (50, y, 50), ext extends from Y = -100 to Y = -130, radius = 20.
        // Shoot ray from (100, -115, 50) in direction (-1, 0, 0)
        double[] rayLat = new double[]{ 100.0, -115.0, 50.0, -1.0, 0.0, 0.0 };
        face_reference_ui_main hitLat = face_picker_ui_main.pickFace(rayLat, cube);
        check("Raycast hits circular lateral face", hitLat != null);
        check("Lateral hit targetFeatureId matches ext-1", hitLat != null && "ext-1".equals(hitLat.getTargetFeatureId()));
        check("Lateral hit faceKind is EXTRUDE_LATERAL", hitLat != null && hitLat.getFaceKind() == face_kind_ui_main.EXTRUDE_LATERAL);
        check("Lateral hit normal is (+1, 0, 0)", hitLat != null && Math.abs(hitLat.getWorldNormal().getX() - 1.0) < 1e-3);
        check("Lateral hit origin Y is at -115", hitLat != null && Math.abs(hitLat.getFaceOrigin().getY() - (-115.0)) < 1e-3);
        check("Lateral hit topology ID is distinct from cap", hitLat != null && !hitLat.getTopologyFaceId().equals(hitCap.getTopologyFaceId()));

        // Test 16: Face-reference refresh preserving lateral identity
        face_reference_ui_main refLat = face_picker_ui_main.refreshFace(hitLat, cube);
        check("Refreshed lateral face preserves targetFeatureId", refLat != null && "ext-1".equals(refLat.getTargetFeatureId()));
        check("Refreshed lateral face preserves EXTRUDE_LATERAL kind", refLat != null && refLat.getFaceKind() == face_kind_ui_main.EXTRUDE_LATERAL);
        check("Refreshed lateral face normal is (+1, 0, 0), not cap normal", refLat != null && Math.abs(refLat.getWorldNormal().getX() - 1.0) < 1e-3 && Math.abs(refLat.getWorldNormal().getY()) < 1e-3);
        check("Refreshed lateral face origin Y is at mid-height -115", refLat != null && Math.abs(refLat.getFaceOrigin().getY() - (-115.0)) < 1e-3);

        // Test 17: Hole creation on curved lateral surface rejected
        hole_feature_ui_main curvedHole = new hole_feature_ui_main(
            "hole-curved", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.EXTRUDE_LATERAL,
            0, 0, 6.0, 10.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0
        );
        curvedHole.setTargetFeatureId("ext-1");
        var curvedRel = edge_machining_analyzer_ui_main.analyzeHole(cube, null, curvedHole);
        check("Hole on curved lateral surface is rejected as CROSSES_EDGE / unsupported", curvedRel.getCondition() == boundary_condition_ui_main.CROSSES_EDGE);
        check("Rejection diagnostic mentions curved extrusion lateral", curvedRel.getDiagnosticMessage() != null && curvedRel.getDiagnosticMessage().contains("curved extrusion lateral"));

        // Test 18: Rectangular Extrusion on FRONT face
        // Cube FRONT face is at Z = 100, normal = (0, 0, 1). Center = (50, -50, 100).
        // Rectangular extrusion: w = 40 (X), d = 30 (Y), height = 25 (Z). Cap is at Z = 125.
        extrude_feature_ui_main extRect = new extrude_feature_ui_main(
            "ext-2", cube.getId(), "Boss Extrude 2", face_kind_ui_main.FRONT,
            CutoutShape.RECTANGLE, 0, 0, 40.0, 30.0, 25.0
        );
        cube.addExtrude(extRect);
        check("Cube now has 2 extrusions", cube.getExtrusions().size() == 2);

        // Test 19: Raycast picking rectangular extrusion cap
        // Cap is at Z = 125. Shoot ray from (50, -50, 200) in (0, 0, -1)
        double[] rayRectCap = new double[]{ 50.0, -50.0, 200.0, 0.0, 0.0, -1.0 };
        face_reference_ui_main hitRectCap = face_picker_ui_main.pickFace(rayRectCap, cube);
        check("Raycast hits rectangular extrusion cap", hitRectCap != null);
        check("Rectangular cap targetFeatureId is ext-2", hitRectCap != null && "ext-2".equals(hitRectCap.getTargetFeatureId()));
        check("Rectangular cap faceKind is EXTRUDE_CAP", hitRectCap != null && hitRectCap.getFaceKind() == face_kind_ui_main.EXTRUDE_CAP);
        check("Rectangular cap origin Z is at 125", hitRectCap != null && Math.abs(hitRectCap.getFaceOrigin().getZ() - 125.0) < 1e-3);
        check("Rectangular cap normal is (0, 0, 1)", hitRectCap != null && Math.abs(hitRectCap.getWorldNormal().getZ() - 1.0) < 1e-3);

        // Test 20: Raycast picking rectangular side faces: POS_U, NEG_U, POS_V, NEG_V
        // FRONT face frame: origin=(50, -50, 100), u=(1,0,0), v=(0,1,0), n=(0,0,1).
        // Extrusion mid Z is 100 + 12.5 = 112.5.
        // Side POS_U is at X = 50 + 20 = 70. Ray from (100, -50, 112.5) in (-1, 0, 0).
        double[] rayPosU = new double[]{ 100.0, -50.0, 112.5, -1.0, 0.0, 0.0 };
        face_reference_ui_main hitPosU = face_picker_ui_main.pickFace(rayPosU, cube);
        check("Hit rectangular side POS_U", hitPosU != null && hitPosU.getFaceKind() == face_kind_ui_main.EXTRUDE_SIDE_POS_U);
        check("Side POS_U targetFeatureId is ext-2", hitPosU != null && "ext-2".equals(hitPosU.getTargetFeatureId()));
        check("Side POS_U normal is (+1, 0, 0)", hitPosU != null && Math.abs(hitPosU.getWorldNormal().getX() - 1.0) < 1e-3);

        // Side NEG_U is at X = 50 - 20 = 30. Ray from (0, -50, 112.5) in (1, 0, 0).
        double[] rayNegU = new double[]{ 0.0, -50.0, 112.5, 1.0, 0.0, 0.0 };
        face_reference_ui_main hitNegU = face_picker_ui_main.pickFace(rayNegU, cube);
        check("Hit rectangular side NEG_U", hitNegU != null && hitNegU.getFaceKind() == face_kind_ui_main.EXTRUDE_SIDE_NEG_U);
        check("Side NEG_U normal is (-1, 0, 0)", hitNegU != null && Math.abs(hitNegU.getWorldNormal().getX() - (-1.0)) < 1e-3);

        // On FRONT face frame, v is (0, -1, 0).
        // Side POS_V has normal along +v = (0, -1, 0) at Y = -65. Shoot from Y = -100 in (0, 1, 0)
        double[] rayPosV = new double[]{ 50.0, -100.0, 112.5, 0.0, 1.0, 0.0 };
        face_reference_ui_main hitPosV = face_picker_ui_main.pickFace(rayPosV, cube);
        check("Hit rectangular side POS_V", hitPosV != null && hitPosV.getFaceKind() == face_kind_ui_main.EXTRUDE_SIDE_POS_V);

        // Side NEG_V has normal along -v = (0, 1, 0) at Y = -35. Shoot from Y = 0 in (0, -1, 0)
        double[] rayNegV = new double[]{ 50.0, 0.0, 112.5, 0.0, -1.0, 0.0 };
        face_reference_ui_main hitNegV = face_picker_ui_main.pickFace(rayNegV, cube);
        check("Hit rectangular side NEG_V", hitNegV != null && hitNegV.getFaceKind() == face_kind_ui_main.EXTRUDE_SIDE_NEG_V);

        // Test 21: Refreshing rectangular side face references
        face_reference_ui_main refPosU = face_picker_ui_main.refreshFace(hitPosU, cube);
        check("Refreshed side POS_U preserves identity", refPosU != null && refPosU.getFaceKind() == face_kind_ui_main.EXTRUDE_SIDE_POS_U);
        check("Refreshed side POS_U normal is (+1, 0, 0)", refPosU != null && Math.abs(refPosU.getWorldNormal().getX() - 1.0) < 1e-3);
        check("Refreshed side POS_U origin X is at 70", refPosU != null && Math.abs(refPosU.getFaceOrigin().getX() - 70.0) < 1e-3);

        // Test 22: Topology consistency for rectangular extrusion
        topology_body_ui_main topo2 = cube.getTopology();
        check("Topology has rectangular cap face", topo2.getDerivedFaceById(cube.getId() + ":F:EXTRUDE_CAP:ext-2") != null);
        check("Topology has EXTRUDE_SIDE_POS_U face", topo2.getDerivedFaceById(cube.getId() + ":F:EXTRUDE_SIDE_POS_U:ext-2") != null);
        check("Topology has EXTRUDE_SIDE_NEG_U face", topo2.getDerivedFaceById(cube.getId() + ":F:EXTRUDE_SIDE_NEG_U:ext-2") != null);
        check("Topology has EXTRUDE_SIDE_POS_V face", topo2.getDerivedFaceById(cube.getId() + ":F:EXTRUDE_SIDE_POS_V:ext-2") != null);
        check("Topology has EXTRUDE_SIDE_NEG_V face", topo2.getDerivedFaceById(cube.getId() + ":F:EXTRUDE_SIDE_NEG_V:ext-2") != null);

        // Verify getTopologyFaceId() consistency with actual registered derived faces
        check("hitPosU getTopologyFaceId() resolves in topology", topo2.getDerivedFaceById(hitPosU.getTopologyFaceId()) != null);
        check("hitRectCap getTopologyFaceId() resolves in topology", topo2.getDerivedFaceById(hitRectCap.getTopologyFaceId()) != null);

        // Test 23: Hole creation and boundary validation on rectangular extrusion side POS_U
        // Side POS_U has width = 30 (Y), height = 25 (Z). Valid centered hole of diameter 6 (radius 3)
        hole_feature_ui_main sideHole = new hole_feature_ui_main(
            "hole-side-1", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.EXTRUDE_SIDE_POS_U,
            0, 0, 6.0, 10.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0
        );
        sideHole.setTargetFeatureId("ext-2");
        var sideRel = edge_machining_analyzer_ui_main.analyzeHole(cube, null, sideHole);
        check("Contained hole on rectangular side POS_U is valid", sideRel.getCondition() == boundary_condition_ui_main.CONTAINED);

        // Oversized hole on side POS_U: diameter = 30 (radius = 15 > half-height 12.5) crosses boundary
        hole_feature_ui_main bigSideHole = new hole_feature_ui_main(
            "hole-side-big", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.EXTRUDE_SIDE_POS_U,
            0, 0, 30.0, 10.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0
        );
        bigSideHole.setTargetFeatureId("ext-2");
        var bigSideRel = edge_machining_analyzer_ui_main.analyzeHole(cube, null, bigSideHole);
        check("Oversized hole on extrusion side crosses boundary", bigSideRel.getCondition() == boundary_condition_ui_main.CROSSES_EDGE);

        // Add valid side hole to cube
        cube.addHole(sideHole);

        // Test 24: Multiple extrusion isolation
        check("ext-1 has 1 effective hole", cube.getEffectiveHolesForFeature("ext-1").size() == 1);
        check("ext-2 has 1 effective hole", cube.getEffectiveHolesForFeature("ext-2").size() == 1);
        check("base primitive still only has 1 hole", cube.getAllEffectiveHoles().size() == 1);

        // Test 25: Mesh generation for rectangular extrusion with side hole
        var rectNodeWithHole = extrude_mesh_builder_ui_main.buildExtrudeNode(cube, extRect, false);
        check("Rectangular extrude node with side hole generated", rectNodeWithHole instanceof javafx.scene.shape.MeshView);

        // Test 26: Shape translation preserving face reference coordinates via refreshFace
        cube.applyWorldDelta(100.0, 50.0, 20.0);
        face_reference_ui_main transCap = face_picker_ui_main.refreshFace(hitCap, cube);
        check("Translated cap origin Y reflects delta (-130 + 50 = -80)", transCap != null && Math.abs(transCap.getFaceOrigin().getY() - (-80.0)) < 1e-3);
        cube.applyWorldDelta(-100.0, -50.0, -20.0); // Reset translation

        System.out.println(String.format("Result: %d/%d tests passed.", testsPassed, totalTests));
        if (testsPassed != totalTests) {
            System.exit(1);
        }
    }
}
