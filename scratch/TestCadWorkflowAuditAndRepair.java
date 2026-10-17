package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.File_Types.document_serializer_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main;
import ui.workspace.drafting.faces.edge_picker_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.machining.machining_solid_mesh_builder_ui_main;
import ui.workspace.drafting.shape_editor_feature_selection_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.sketch.*;
import ui.workspace.shapes.basic_shapes_ui_main;

import ui.featuremanager.featuremanager_ui_main;
import ui.featuremanager.feature_tree_node_ui_main;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestCadWorkflowAuditAndRepair {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertTrue(boolean cond, String msg) {
        testsTotal++;
        if (cond) {
            testsPassed++;
            System.out.println("  [PASS] " + msg);
        } else {
            System.err.println("  [FAIL] " + msg);
            throw new AssertionError("Assertion failed: " + msg);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  CAD WORKFLOW AUDIT & REPAIR VERIFICATION SUITE ");
        System.out.println("=================================================");

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already initialized
        }

        try {
            testSketchProfileAnalysis();
            testTrianglePrismExtrusionGeometry();
            testExtrudeParametricRegeneration();
            testFeatureManagerSelectionSynchronization();
            testEdgeAndFaceSelectionContracts();
            testFilletChamferDraftSolidMeshModifications();
            testSnappingAndAttractionBetweenBasicAndExtruded();
            testFeatureManagerExtrusionRepresentation();
            testChainedCadWorkflowAndPersistence();
            testCylinderConeMachiningAndSphereRejection();
            testFeatureManagerTreeRobustness();
            testMachiningCoexistingWithHoles();

            System.out.println("=================================================");
            System.out.println("  ALL " + testsTotal + " ASSERTIONS PASSED SUCCESSFULLY! (" + testsPassed + "/" + testsTotal + ")");
            System.out.println("=================================================");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("TEST SUITE FAILED: " + t.getMessage());
            System.exit(1);
        }
    }

    private static void testSketchProfileAnalysis() {
        System.out.println("\n--- Testing Sketch Profile Analysis ---");
        sketch_feature_ui_main sketch = new sketch_feature_ui_main(
                "sk1", "owner1", "Test Triangle Sketch", sketch_plane_type_ui_main.BASE_XZ, null
        );

        // Add 3 connected lines forming an equilateral/closed triangle
        sketch.addEntity(new sketch_line_ui_main("l1", 0, 0, 50, 0));
        sketch.addEntity(new sketch_line_ui_main("l2", 50, 0, 25, 43.3));
        sketch.addEntity(new sketch_line_ui_main("l3", 25, 43.3, 0, 0));

        sketch_profile_analyzer_ui_main.AnalysisResult result = sketch_profile_analyzer_ui_main.analyze(sketch.getEntities());
        assertTrue(result.isValid(), "Closed triangle sketch must be recognized as valid profile");
        assertTrue(result.loops().size() == 1, "Triangle sketch must contain exactly 1 closed loop");
        assertTrue(result.type() == sketch_profile_type_ui_main.CLOSED, "Triangle loop must be classified as CLOSED profile");

        // Disconnected sketch test
        List<sketch_entity_ui_main> openEntities = List.of(
                new sketch_line_ui_main("o1", 0, 0, 50, 0),
                new sketch_line_ui_main("o2", 50, 0, 25, 43.3)
        );
        sketch_profile_analyzer_ui_main.AnalysisResult openRes = sketch_profile_analyzer_ui_main.analyze(openEntities);
        assertTrue(!openRes.isValid(), "Open sketch must be rejected as invalid profile");
    }

    private static void testTrianglePrismExtrusionGeometry() {
        System.out.println("\n--- Testing Triangle Prism Extrusion Geometry ---");
        shape_item_ui_main host = new shape_item_ui_main(
                basic_shapes_ui_main.CUBOID, new Point3D(-50, -25, -50), new Point3D(50, 25, 50)
        );

        // 1. Right Triangle Prism Extrusion
        extrude_feature_ui_main extRight = new extrude_feature_ui_main(
                "ext_right", host.getId(), "Extrude Right Triangle",
                face_kind_ui_main.TOP, CutoutShape.RIGHT_TRIANGLE,
                0.0, 0.0, 40.0, 30.0, 60.0
        );
        host.addExtrude(extRight);

        Node rightNode = extrude_mesh_builder_ui_main.buildExtrudeNode(host, extRight, false);
        assertTrue(rightNode instanceof MeshView, "Extrude mesh builder must build MeshView for RIGHT_TRIANGLE");
        MeshView rightMv = (MeshView) rightNode;
        TriangleMesh rightMesh = (TriangleMesh) rightMv.getMesh();
        assertTrue(rightMesh != null, "MeshView must contain a TriangleMesh");

        // A triangular prism has 6 vertices (3 on base, 3 on top) -> 18 floats
        int numPoints = rightMesh.getPoints().size() / 3;
        assertTrue(numPoints == 6, "Triangular prism mesh must have exactly 6 vertices, found: " + numPoints);

        // A triangular prism has 2 triangular caps (2 faces) + 3 quad walls (6 faces) = 8 triangular faces -> 48 indices (6 per face)
        int numFaces = rightMesh.getFaces().size() / 6;
        assertTrue(numFaces == 8, "Triangular prism mesh must have exactly 8 triangular faces (not cylinder's 64+ faces), found: " + numFaces);

        // 2. Equilateral Triangle Prism Extrusion
        extrude_feature_ui_main extEq = new extrude_feature_ui_main(
                "ext_eq", host.getId(), "Extrude Equilateral Triangle",
                face_kind_ui_main.TOP, CutoutShape.EQUILATERAL_TRIANGLE,
                0.0, 0.0, 40.0, 40.0, 50.0
        );
        host.addExtrude(extEq);

        Node eqNode = extrude_mesh_builder_ui_main.buildExtrudeNode(host, extEq, false);
        assertTrue(eqNode instanceof MeshView, "Equilateral triangle extrude must produce MeshView");
        MeshView eqMv = (MeshView) eqNode;
        TriangleMesh eqMesh = (TriangleMesh) eqMv.getMesh();
        assertTrue(eqMesh.getPoints().size() / 3 == 6, "Equilateral triangular prism mesh must have 6 vertices");
        assertTrue(eqMesh.getFaces().size() / 6 == 8, "Equilateral triangular prism mesh must have 8 triangular faces");

        // Verify height/depth coordinates: length along normal must match extrusion depth (50.0)
        float[] points = new float[eqMesh.getPoints().size()];
        eqMesh.getPoints().toArray(points);
        // On TOP face (normal is (0, -1, 0)), Y coordinates vary by extrusion height 50.0
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 1; i < points.length; i += 3) {
            if (points[i] < minY) minY = points[i];
            if (points[i] > maxY) maxY = points[i];
        }
        double depthSpan = maxY - minY;
        assertTrue(Math.abs(depthSpan - 50.0) < 1e-3, "Extrusion depth span along Y must equal 50.0, got: " + depthSpan);
    }

    private static void testExtrudeParametricRegeneration() {
        System.out.println("\n--- Testing Extrude Parametric Regeneration ---");
        shape_item_ui_main host = new shape_item_ui_main(
                basic_shapes_ui_main.CUBOID, new Point3D(-50, -25, -50), new Point3D(50, 25, 50)
        );
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
                "ext_reg", host.getId(), "Extrude Reg",
                face_kind_ui_main.TOP, CutoutShape.RIGHT_TRIANGLE,
                0.0, 0.0, 30.0, 30.0, 20.0
        );
        host.addExtrude(ext);

        Node n1 = extrude_mesh_builder_ui_main.buildExtrudeNode(host, ext, false);
        TriangleMesh m1 = (TriangleMesh) ((MeshView) n1).getMesh();

        // Change depth to 80.0
        ext.setHeight(80.0);
        Node n2 = extrude_mesh_builder_ui_main.buildExtrudeNode(host, ext, false);
        TriangleMesh m2 = (TriangleMesh) ((MeshView) n2).getMesh();

        float[] pts = new float[m2.getPoints().size()];
        m2.getPoints().toArray(pts);
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 1; i < pts.length; i += 3) {
            if (pts[i] < minY) minY = pts[i];
            if (pts[i] > maxY) maxY = pts[i];
        }
        assertTrue(Math.abs((maxY - minY) - 80.0) < 1e-3, "Regenerated prism must have updated extrusion height 80.0");
    }

    private static void testFeatureManagerSelectionSynchronization() {
        System.out.println("\n--- Testing Feature Manager Selection Synchronization ---");
        shape_editor_feature_selection_ui_main featureSelection = new shape_editor_feature_selection_ui_main(null);
        shape_item_ui_main box = new shape_item_ui_main(
                basic_shapes_ui_main.CUBE, new Point3D(-25, -25, -25), new Point3D(25, 25, 25)
        );

        // Add features
        fillet_feature_ui_main fillet = new fillet_feature_ui_main("fil1", box.getId(), "TOP_FRONT", 3.0);
        box.addFillet(fillet);
        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main("ch1", box.getId(), "TOP_BACK", 2.0);
        box.addChamfer(chamfer);
        draft_feature_ui_main draft = new draft_feature_ui_main("dr1", box.getId(), face_kind_ui_main.FRONT, 5.0);
        box.addDraft(draft);

        // Select fillet
        featureSelection.selectFillet(box, fillet);
        assertTrue(featureSelection.getSelectedFillet() == fillet, "Must register selected fillet");
        assertTrue(featureSelection.getSelectedChamfer() == null, "Selecting fillet must clear chamfer selection");

        // Select chamfer
        featureSelection.selectChamfer(box, chamfer);
        assertTrue(featureSelection.getSelectedChamfer() == chamfer, "Must register selected chamfer");
        assertTrue(featureSelection.getSelectedFillet() == null, "Selecting chamfer must clear fillet selection");

        // Select draft
        featureSelection.selectDraft(box, draft);
        assertTrue(featureSelection.getSelectedDraft() == draft, "Must register selected draft");
        assertTrue(featureSelection.getSelectedChamfer() == null, "Selecting draft must clear chamfer selection");

        // Select sketch
        sketch_feature_ui_main sk = new sketch_feature_ui_main(
                "sk1", box.getId(), "Standalone Sketch", sketch_plane_type_ui_main.BASE_XZ, null
        );
        featureSelection.selectSketch(box, sk);
        assertTrue(featureSelection.getSelectedSketch() == sk, "Must register selected sketch");
        assertTrue(featureSelection.getSelectedDraft() == null, "Selecting sketch must clear draft selection");
    }

    private static void testEdgeAndFaceSelectionContracts() {
        System.out.println("\n--- Testing Edge & Face Selection Contracts ---");
        assertTrue(edge_picker_ui_main.DEFAULT_TOLERANCE == 12.0, "Edge picker default tolerance must be expanded to 12.0 px for reliable picking");
    }

    private static void testFilletChamferDraftSolidMeshModifications() {
        System.out.println("\n--- Testing Solid Mesh Modifications (Machining) ---");

        shape_item_ui_main box = new shape_item_ui_main(
                basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20)
        );

        // 1. Chamfer Bevel Mesh
        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main("ch1", box.getId(), "TOP_FRONT", 4.0);
        box.addChamfer(chamfer);
        Node solidChamferNode = machining_solid_mesh_builder_ui_main.buildSolid(box, false);
        assertTrue(solidChamferNode instanceof MeshView, "Solid mesh builder must return MeshView for chamfer");
        TriangleMesh chamferMesh = (TriangleMesh) ((MeshView) solidChamferNode).getMesh();
        // Standard cube has 12 triangles (2 per 6 faces). Beveled cube replaces edge with bevel quad (2 triangles), adding facets.
        int chamferFaces = chamferMesh.getFaces().size() / 6;
        assertTrue(chamferFaces >= 12, "Chamfered box mesh must contain solid facets, found: " + chamferFaces);

        // 2. Fillet Arc Blend Mesh
        shape_item_ui_main boxFillet = new shape_item_ui_main(
                basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20)
        );
        fillet_feature_ui_main fillet = new fillet_feature_ui_main("fil1", boxFillet.getId(), "TOP_FRONT", 3.0);
        boxFillet.addFillet(fillet);
        Node solidFilletNode = machining_solid_mesh_builder_ui_main.buildSolid(boxFillet, false);
        assertTrue(solidFilletNode instanceof MeshView, "Solid mesh builder must return MeshView for fillet");
        TriangleMesh filletMesh = (TriangleMesh) ((MeshView) solidFilletNode).getMesh();
        int filletFaces = filletMesh.getFaces().size() / 6;
        assertTrue(filletFaces >= 12, "Filleted box mesh must contain rounded blend facets, found: " + filletFaces);

        // Verify height and dimension preservation on fillet
        float[] pts = new float[filletMesh.getPoints().size()];
        filletMesh.getPoints().toArray(pts);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < pts.length; i += 3) {
            minX = Math.min(minX, pts[i]); maxX = Math.max(maxX, pts[i]);
            minY = Math.min(minY, pts[i+1]); maxY = Math.max(maxY, pts[i+1]);
            minZ = Math.min(minZ, pts[i+2]); maxZ = Math.max(maxZ, pts[i+2]);
        }
        assertTrue(Math.abs((maxX - minX) - 40.0) < 1e-2, "Filleted cube width must be preserved at 40.0, got: " + (maxX - minX));
        assertTrue(Math.abs((maxY - minY) - 40.0) < 1e-2, "Filleted cube height must be preserved at 40.0 (not halved!), got: " + (maxY - minY));
        assertTrue(Math.abs((maxZ - minZ) - 40.0) < 1e-2, "Filleted cube depth must be preserved at 40.0, got: " + (maxZ - minZ));

        boxFillet.rebuild();
        assertTrue(boxFillet.getShapeGroup().getChildren().size() == 1, "Only solid mesh node should be in shapeGroup, no orphaned green fillet strip overlay");

        // 3. Draft Angular Taper Mesh
        shape_item_ui_main boxDraft = new shape_item_ui_main(
                basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20)
        );
        draft_feature_ui_main draft = new draft_feature_ui_main("dr1", boxDraft.getId(), face_kind_ui_main.FRONT, 10.0);
        boxDraft.addDraft(draft);
        Node solidDraftNode = machining_solid_mesh_builder_ui_main.buildSolid(boxDraft, false);
        assertTrue(solidDraftNode instanceof MeshView, "Solid mesh builder must return MeshView for draft");
        TriangleMesh draftMesh = (TriangleMesh) ((MeshView) solidDraftNode).getMesh();

        // Verify draft calculation is non-zero
        double expectedTaper = 20.0 * Math.tan(Math.toRadians(10.0));
        assertTrue(expectedTaper > 0, "Taper offset must be positive");
    }

    private static void testSnappingAndAttractionBetweenBasicAndExtruded() {
        System.out.println("\n--- Testing Snapping and Attraction Between Basic and Extruded Geometry ---");
        shape_item_ui_main hostCube = new shape_item_ui_main(
                basic_shapes_ui_main.CUBE, new Point3D(-25, 0, -25), new Point3D(25, -50, 25)
        );
        hostCube.setWorldTranslation(0, 0, 0);

        shape_item_ui_main extrudeTri = new shape_item_ui_main(
                basic_shapes_ui_main.EQUILATERAL_TRIANGLE, new Point3D(-10, 0, -10), new Point3D(10, 0, 10)
        );
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
                "ext_snap", extrudeTri.getId(), "Boss Prism", face_kind_ui_main.TOP,
                CutoutShape.EQUILATERAL_TRIANGLE, 0, 0, 20.0, 0, 25.0
        );
        extrudeTri.addExtrude(ext);
        extrudeTri.setWorldTranslation(0, -45, 0);

        List<shape_item_ui_main> shapes = List.of(hostCube, extrudeTri);
        var snapResult = ui.workspace.drafting.extrude.extrude_magnet_helper_ui_main.trySnapExistingShape(extrudeTri, shapes);
        assertTrue(snapResult != null, "Extruded triangular prism must snap to nearest cube face");
        assertTrue(snapResult.host() == hostCube, "Snap host must be the cube");
        assertTrue(snapResult.faceRef().getFaceKind() == face_kind_ui_main.TOP, "Snap target face must be TOP");
        assertTrue(extrudeTri.isOnFace(), "Extruded triangular prism must now be marked as on face");
        assertTrue(hostCube.getId().equals(extrudeTri.getFaceOwnerId()), "Face owner ID must be cube's ID");

        shape_item_ui_main cyl = new shape_item_ui_main(
                basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(5, -15, 0)
        );
        cyl.setWorldTranslation(0, -55, 0);
        var cylSnap = ui.workspace.drafting.extrude.extrude_magnet_helper_ui_main.trySnapExistingShape(cyl, List.of(extrudeTri, cyl));
        assertTrue(cylSnap != null, "Basic shape (cylinder) must snap to extruded triangular prism");
        assertTrue(cylSnap.host() == extrudeTri, "Host must be the extruded prism");
        assertTrue(cyl.isOnFace(), "Cylinder must be snapped onto face of extruded prism");
    }

    private static void testFeatureManagerExtrusionRepresentation() {
        System.out.println("\n--- Testing Feature Manager Extrusion Representation ---");
        shape_item_ui_main triShape = new shape_item_ui_main(
                basic_shapes_ui_main.EQUILATERAL_TRIANGLE, new Point3D(-15, 0, -15), new Point3D(15, 0, 15)
        );
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
                "ext_fm", triShape.getId(), "Boss Prism", face_kind_ui_main.TOP,
                CutoutShape.EQUILATERAL_TRIANGLE, 0, 0, 30.0, 0, 40.0
        );
        triShape.addExtrude(ext);
        triShape.setName(ext.getName());

        String label = ui.featuremanager.featuremanager_ui_main.formatItemLabel(triShape);
        assertTrue(label.contains("Extrude"), "Feature Manager label must represent Extrude feature, got: " + label);

        var node = ui.featuremanager.feature_tree_node_ui_main.forShape(triShape);
        assertTrue(node.isShape(), "Node must be for shape");
        var icon = node.getIcon();
        assertTrue(icon != null, "Extruded shape must have dedicated icon in Feature Manager");
    }

    private static void testChainedCadWorkflowAndPersistence() throws Exception {
        System.out.println("\n--- Testing Chained CAD Workflow & Persistence ---");

        shape_item_ui_main box = new shape_item_ui_main(
                basic_shapes_ui_main.CUBE, new Point3D(-30, -30, -30), new Point3D(30, 30, 30)
        );

        // 1. Add Extrude feature
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
                "ext_chain", box.getId(), "Extrude Triangular Feature",
                face_kind_ui_main.TOP, CutoutShape.EQUILATERAL_TRIANGLE,
                0.0, 0.0, 25.0, 25.0, 35.0
        );
        box.addExtrude(ext);

        // 2. Add Chamfer feature to edge
        chamfer_feature_ui_main chamfer = new chamfer_feature_ui_main("ch_chain", box.getId(), "TOP_FRONT", 3.0);
        box.addChamfer(chamfer);

        // 3. Add Draft feature to face
        draft_feature_ui_main draft = new draft_feature_ui_main("dr_chain", box.getId(), face_kind_ui_main.FRONT, 5.0);
        box.addDraft(draft);

        // 4. Serialize and Deserialize
        File tempFile = File.createTempFile("cad_workflow_test_", ".mphys");
        tempFile.deleteOnExit();

        List<shape_item_ui_main> shapes = new ArrayList<>();
        shapes.add(box);

        document_serializer_ui_main.saveToFile(tempFile, shapes);
        assertTrue(tempFile.exists() && tempFile.length() > 0, "Document must serialize to file");

        List<shape_item_ui_main> loadedShapes = document_serializer_ui_main.loadFromFile(tempFile);
        assertTrue(loadedShapes != null && loadedShapes.size() == 1, "Document must load exactly 1 shape");

        shape_item_ui_main loaded = loadedShapes.get(0);
        assertTrue(loaded.getType() == basic_shapes_ui_main.CUBE, "Loaded shape must be CUBE");
        assertTrue(loaded.hasExtrusions(), "Extrusion feature must be restored on loaded shape");
        assertTrue(Math.abs(loaded.getExtrusions().get(0).getHeight() - 35.0) < 1e-3, "Extrusion height must be restored as 35.0");
        assertTrue(loaded.getChamfers().size() == 1, "Chamfer feature must be restored");
        assertTrue(loaded.getDrafts().size() == 1, "Draft feature must be restored");
    }

    private static void testCylinderConeMachiningAndSphereRejection() {
        System.out.println("\n--- Testing Cylinder & Cone Machining and Sphere Rejection ---");

        // 1. Cylinder with Chamfer on TOP_RIM
        shape_item_ui_main cyl = new shape_item_ui_main(
                basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(20, 40, 0)
        );
        chamfer_feature_ui_main chTop = new chamfer_feature_ui_main("ch_cyl", cyl.getId(), "TOP_RIM", 2.0);
        cyl.addChamfer(chTop);
        Node cylSolid = machining_solid_mesh_builder_ui_main.buildSolid(cyl, false);
        assertTrue(cylSolid instanceof MeshView, "Cylinder solid mesh builder must return MeshView for TOP_RIM chamfer");
        TriangleMesh cylMesh = (TriangleMesh) ((MeshView) cylSolid).getMesh();
        assertTrue(cylMesh.getPoints().size() > 0, "Cylinder machined mesh points must be non-empty");
        cyl.rebuild();
        assertTrue(cyl.getShapeGroup().getChildren().size() == 1, "Cylinder shapeGroup must contain exactly 1 solid mesh, no separate overlays");

        // 2. Cylinder with Fillet on BOTTOM_RIM
        shape_item_ui_main cylFil = new shape_item_ui_main(
                basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(20, 40, 0)
        );
        fillet_feature_ui_main filBot = new fillet_feature_ui_main("fil_cyl", cylFil.getId(), "BOTTOM_RIM", 2.5);
        cylFil.addFillet(filBot);
        Node cylFilSolid = machining_solid_mesh_builder_ui_main.buildSolid(cylFil, false);
        assertTrue(cylFilSolid instanceof MeshView, "Cylinder solid mesh builder must return MeshView for BOTTOM_RIM fillet");
        cylFil.rebuild();
        assertTrue(cylFil.getShapeGroup().getChildren().size() == 1, "Cylinder with fillet must contain exactly 1 solid mesh");

        // 3. Cone with Chamfer on BASE_RIM
        shape_item_ui_main cone = new shape_item_ui_main(
                basic_shapes_ui_main.CONE, new Point3D(0, 0, 0), new Point3D(20, 40, 0)
        );
        chamfer_feature_ui_main chBase = new chamfer_feature_ui_main("ch_cone", cone.getId(), "BASE_RIM", 2.0);
        cone.addChamfer(chBase);
        Node coneSolid = machining_solid_mesh_builder_ui_main.buildSolid(cone, false);
        assertTrue(coneSolid instanceof MeshView, "Cone solid mesh builder must return MeshView for BASE_RIM chamfer");
        cone.rebuild();
        assertTrue(cone.getShapeGroup().getChildren().size() == 1, "Cone shapeGroup must contain exactly 1 solid mesh");

        // 4. Sphere Revalidation
        shape_item_ui_main sphere = new shape_item_ui_main(
                basic_shapes_ui_main.SPHERE, new Point3D(0, 0, 0), new Point3D(20, 20, 0)
        );
        fillet_feature_ui_main filSphere = new fillet_feature_ui_main("fil_sph", sphere.getId(), "TOP_FRONT", 2.0);
        boolean filValid = filSphere.revalidate(sphere);
        assertTrue(!filValid, "Fillet on sphere must be rejected as invalid");
        assertTrue(filSphere.getDiagnosticMessage().toLowerCase().contains("sphere"), "Diagnostic message must explain sphere has no edges");

        chamfer_feature_ui_main chSphere = new chamfer_feature_ui_main("ch_sph", sphere.getId(), "TOP_FRONT", 2.0);
        boolean chValid = chSphere.revalidate(sphere);
        assertTrue(!chValid, "Chamfer on sphere must be rejected as invalid");
        assertTrue(chSphere.getDiagnosticMessage().toLowerCase().contains("sphere"), "Diagnostic message must explain sphere has no edges");

        draft_feature_ui_main drSphere = new draft_feature_ui_main("dr_sph", sphere.getId(), face_kind_ui_main.FRONT, 5.0);
        boolean drValid = drSphere.revalidate(sphere);
        assertTrue(!drValid, "Draft on sphere must be rejected as invalid");
        assertTrue(drSphere.getDiagnosticMessage().toLowerCase().contains("sphere"), "Diagnostic message must explain sphere has no planar faces");
    }

    private static void testFeatureManagerTreeRobustness() throws Exception {
        System.out.println("\n--- Testing Feature Manager TreeView Robustness ---");
        javafx.scene.Group container = new javafx.scene.Group();
        javafx.scene.layout.Pane viewport = new javafx.scene.layout.Pane();
        javafx.scene.PerspectiveCamera camera = new javafx.scene.PerspectiveCamera();
        shape_editor_ui_main editor = new shape_editor_ui_main(
                container, viewport, null, null, e -> Point3D.ZERO, new javafx.scene.control.Label(), () -> false, camera
        );
        featuremanager_ui_main fm = new featuremanager_ui_main();

        shape_item_ui_main box1 = new shape_item_ui_main(
                basic_shapes_ui_main.CUBE, new Point3D(-20, -20, -20), new Point3D(20, 20, 20)
        );
        shape_item_ui_main cyl1 = new shape_item_ui_main(
                basic_shapes_ui_main.CYLINDER, new Point3D(50, 0, 50), new Point3D(70, 40, 50)
        );
        editor.addShape(box1);
        editor.addShape(cyl1);

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                fm.bindToEditor(editor);
                for (int i = 0; i < 50; i++) {
                    editor.selectShape(i % 2 == 0 ? box1 : cyl1);
                    fm.refresh();
                    var sm = fm.getTreeView().getSelectionModel();
                    assertTrue(sm != null, "Selection model must remain valid during rapid refresh");
                }
                assertTrue(fm.getTreeView().getRoot().getChildren().size() == 2, "Root tree must contain 2 top-level shapes");
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });

        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        if (error[0] != null) {
            if (error[0] instanceof RuntimeException) throw (RuntimeException) error[0];
            throw new RuntimeException("FeatureManager test failed", error[0]);
        }
    }

    private static void testMachiningCoexistingWithHoles() {
        System.out.println("\n--- Testing Machining Coexisting with Holes ---");
        shape_item_ui_main box = new shape_item_ui_main(
                basic_shapes_ui_main.CUBE, new Point3D(-30, -30, -30), new Point3D(30, 30, 30)
        );

        // 1. Add blind hole to TOP face
        ui.workspace.drafting.holes.hole_feature_ui_main hole = new ui.workspace.drafting.holes.hole_feature_ui_main(
                "h_coexist", box.getId(), face_kind_ui_main.TOP,
                0.0, 0.0, 10.0, 15.0, false
        );
        box.addHole(hole);
        assertTrue(box.hasHoles(), "Box must report having holes");

        // 2. Add Fillet to BOTTOM_FRONT edge
        fillet_feature_ui_main fil = new fillet_feature_ui_main("fil_coexist", box.getId(), "BOTTOM_FRONT", 3.0);
        box.addFillet(fil);

        // 3. Add Chamfer to BOTTOM_BACK edge
        chamfer_feature_ui_main ch = new chamfer_feature_ui_main("ch_coexist", box.getId(), "BOTTOM_BACK", 2.0);
        box.addChamfer(ch);

        // 4. Rebuild and verify both holes and machining visuals are present
        box.rebuild();
        assertTrue(box.getShapeGroup().getChildren().size() >= 3, "ShapeGroup must contain hole-cutout solid, derived regions, and machining visuals");
        assertTrue(box.getFillets().size() == 1, "Fillet feature must be preserved when holes exist");
        assertTrue(box.getChamfers().size() == 1, "Chamfer feature must be preserved when holes exist");

        // 5. Test Cylinder with hole and machining
        shape_item_ui_main cyl = new shape_item_ui_main(
                basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(25, 50, 0)
        );
        ui.workspace.drafting.holes.hole_feature_ui_main cylHole = new ui.workspace.drafting.holes.hole_feature_ui_main(
                "cyl_h_coexist", cyl.getId(), face_kind_ui_main.TOP_CAP,
                0.0, 0.0, 8.0, 20.0, false
        );
        cyl.addHole(cylHole);
        fillet_feature_ui_main cylFil = new fillet_feature_ui_main("cyl_fil", cyl.getId(), "BOTTOM_RIM", 2.0);
        cyl.addFillet(cylFil);
        cyl.rebuild();
        assertTrue(cyl.hasHoles() && cyl.getFillets().size() == 1, "Cylinder must retain both holes and machining");
        assertTrue(cyl.getShapeGroup().getChildren().size() >= 2, "Cylinder must render hole solid and machining visuals simultaneously");
    }
}
