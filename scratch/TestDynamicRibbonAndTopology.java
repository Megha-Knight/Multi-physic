import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import ui.File_Types.document_serializer_ui_main;
import ui.featuremanager.feature_tree_node_ui_main;
import ui.ribbonbar.*;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_selection_resolver_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.*;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestDynamicRibbonAndTopology {

    private static int testsPassed = 0;
    private static int totalTests = 0;

    private static void check(String desc, boolean condition) {
        totalTests++;
        if (condition) {
            testsPassed++;
            System.out.println("[PASS] " + desc);
        } else {
            System.err.println("[FAIL] " + desc);
            throw new AssertionError("Test failed: " + desc);
        }
    }

    public static void main(String[] args) {
        int exitCode = 1;
        try {
            CountDownLatch fxLatch = new CountDownLatch(1);
            try {
                Platform.startup(fxLatch::countDown);
            } catch (IllegalStateException e) {
                fxLatch.countDown();
            }
            if (!fxLatch.await(5, TimeUnit.SECONDS)) {
                System.err.println("JavaFX startup timed out");
                System.exit(1);
            }

            CountDownLatch runLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    runAllTests();
                    runLatch.countDown();
                } catch (Throwable t) {
                    t.printStackTrace();
                    runLatch.countDown();
                }
            });

            if (runLatch.await(30, TimeUnit.SECONDS) && testsPassed == totalTests && totalTests >= 40) {
                System.out.println("\n==========================================");
                System.out.printf("ALL %d TESTS PASSED SUCCESSFULLY!%n", totalTests);
                System.out.println("==========================================");
                exitCode = 0;
            } else {
                System.err.printf("FAILED: %d/%d passed.%n", testsPassed, totalTests);
                exitCode = 1;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        } finally {
            System.exit(exitCode);
        }
    }

    private static void runAllTests() {
        System.out.println("Running Stage 1 & Stage 2 Test Suite...\n");

        // ==========================================
        // STAGE 1: DYNAMIC RIBBON PANEL TESTS
        // ==========================================
        System.out.println("--- STAGE 1: Dynamic Ribbon Panels ---");
        dynamic_panel_coordinator_ui_main coord = new dynamic_panel_coordinator_ui_main();
        basicshapespanel_ui_main pShapes = new basicshapespanel_ui_main();
        machining_panel_ui_main pMachining = new machining_panel_ui_main();
        extrude_ribbon_panel_ui_main pExtrude = new extrude_ribbon_panel_ui_main();

        coord.register(pShapes);
        coord.register(pMachining);
        coord.register(pExtrude);

        check("Coordinator: Registered 3 dynamic panels", coord.getRegisteredPanels().size() == 3);
        check("Coordinator: Initially no panel is active", coord.getActivePanel() == null);
        check("Coordinator: All panels initially hidden", !pShapes.isPanelVisible() && !pMachining.isPanelVisible() && !pExtrude.isPanelVisible());

        // 1. Open Basic Shapes
        coord.openExclusive(pShapes);
        check("Basic Shapes open: isPanelVisible true", pShapes.isPanelVisible());
        check("Basic Shapes open: Node visible and managed", pShapes.asNode().isVisible() && pShapes.asNode().isManaged());
        check("Basic Shapes open: Extrude and Machining are closed", !pExtrude.isPanelVisible() && !pMachining.isPanelVisible());
        check("Coordinator: Active panel is Basic Shapes", coord.getActivePanel() == pShapes);

        // 2. Open Extrude -> closes Basic Shapes
        coord.openExclusive(pExtrude);
        check("Extrude open: Basic Shapes closed", !pShapes.isPanelVisible());
        check("Extrude open: Extrude panel visible", pExtrude.isPanelVisible());
        check("Extrude open: Node visible and managed", pExtrude.asNode().isVisible() && pExtrude.asNode().isManaged());
        check("Extrude open: Machining closed", !pMachining.isPanelVisible());
        check("Coordinator: Active panel is Extrude", coord.getActivePanel() == pExtrude);

        // 3. Open Machining -> closes Extrude
        coord.openExclusive(pMachining);
        check("Machining open: Extrude closed", !pExtrude.isPanelVisible());
        check("Machining open: Machining visible", pMachining.isPanelVisible());
        check("Machining open: Basic Shapes closed", !pShapes.isPanelVisible());

        // 4. Toggle active panel closes it
        coord.toggle(pMachining);
        check("Toggle active panel: Machining now closed", !pMachining.isPanelVisible());
        check("Toggle active panel: No active panel", coord.getActivePanel() == null);

        // 5. Toggle inactive panel opens it
        coord.toggle(pExtrude);
        check("Toggle inactive panel: Extrude now open", pExtrude.isPanelVisible());

        // 6. Close panel
        coord.close(pExtrude);
        check("Close panel: Extrude closed", !pExtrude.isPanelVisible());

        // 7. Close all
        coord.openExclusive(pShapes);
        coord.closeAll();
        check("closeAll: All panels closed", !pShapes.isPanelVisible() && !pMachining.isPanelVisible() && !pExtrude.isPanelVisible());

        // 8. Extrude panel controls
        pExtrude.setProfileShape(CutoutShape.RECTANGLE);
        pExtrude.setDiameter(35.0);
        pExtrude.setWidth2(25.0);
        pExtrude.setExtrudeHeight(45.0);
        check("Extrude panel: profile is RECTANGLE", pExtrude.getProfileShape() == CutoutShape.RECTANGLE);
        check("Extrude panel: diameter is 35.0", Math.abs(pExtrude.getDiameter() - 35.0) < 1e-4);
        check("Extrude panel: width2 is 25.0", Math.abs(pExtrude.getWidth2() - 25.0) < 1e-4);
        check("Extrude panel: height is 45.0", Math.abs(pExtrude.getExtrudeHeight() - 45.0) < 1e-4);

        boolean[] applied = {false};
        pExtrude.setOnApply(cfg -> {
            if (cfg.profile() == CutoutShape.RECTANGLE && cfg.diameter() == 35.0) applied[0] = true;
        });
        pExtrude.show();
        // Invoke apply
        try {
            var m = extrude_ribbon_panel_ui_main.class.getDeclaredMethod("handleApply");
            m.setAccessible(true);
            m.invoke(pExtrude);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        check("Extrude panel: apply callback executed with correct parameters", applied[0]);
        pExtrude.hide();

        // Ribbon coordinator integration
        ribbon_ui_main ribbon = new ribbon_ui_main();
        check("Ribbon: Has panel coordinator", ribbon.getPanelCoordinator() != null);
        check("Ribbon: Has extrude panel", ribbon.getExtrudePanel() != null);
        check("Ribbon: Coordinator has registered panels", ribbon.getPanelCoordinator().getRegisteredPanels().size() >= 3);

        // ==========================================
        // STAGE 2: TOPOLOGY-AWARE DERIVED FACE TESTS
        // ==========================================
        System.out.println("\n--- STAGE 2: Topology-Aware Derived Faces ---");

        // 1. Blind hole on Cube
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-25, 0, -25), new Point3D(25, 50, 25));
        hole_feature_ui_main blindHole = new hole_feature_ui_main("H1", cube.getId(), "Hole 1", HoleType.SIMPLE,
            face_kind_ui_main.TOP, 0.0, 0.0, 16.0, 20.0, false, 0, 0, 0, 0, CutoutShape.CIRCLE, 0);
        cube.addHole(blindHole);

        topology_body_ui_main cubeBody = cube.getTopology();
        check("Topology: Cube has 6 base faces", cubeBody.getFaceCount() == 6);
        check("Topology: Cube has derived faces", cubeBody.getDerivedFaceCount() > 0);

        topology_derived_face_ui_main remTop = cubeBody.getDerivedFaceById(cube.getId() + ":F:TOP:REMAINING");
        check("Blind Hole: Remaining TOP face exists", remTop != null);
        check("Blind Hole: Remaining face has outer loop", remTop != null && remTop.getOuterLoop() != null && remTop.getOuterLoop().isOuter());
        check("Blind Hole: Remaining face has exactly 1 inner loop", remTop != null && remTop.getInnerLoops().size() == 1);
        check("Blind Hole: Inner loop is circular", remTop != null && remTop.getInnerLoops().get(0).isCircular());
        check("Blind Hole: Inner loop radius is 8.0", remTop != null && Math.abs(remTop.getInnerLoops().get(0).getRadius() - 8.0) < 1e-4);

        topology_derived_face_ui_main holeWall = cubeBody.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_WALL:H1");
        check("Blind Hole: Hole Wall derived face exists", holeWall != null);
        check("Blind Hole: Hole Wall is interior wall", holeWall != null && holeWall.getRegionKind().isWall() && holeWall.getRegionKind().isInterior());
        check("Blind Hole: Hole Wall radius is 8.0", holeWall != null && Math.abs(holeWall.getRadius() - 8.0) < 1e-4);
        check("Blind Hole: Hole Wall depth is 20.0", holeWall != null && Math.abs(holeWall.getDepth() - 20.0) < 1e-4);

        topology_derived_face_ui_main holeFloor = cubeBody.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_FLOOR:H1");
        check("Blind Hole: Hole Floor derived face exists", holeFloor != null);
        check("Blind Hole: Hole Floor is floor", holeFloor != null && holeFloor.getRegionKind().isFloor());
        check("Blind Hole: Hole Floor radius is 8.0", holeFloor != null && Math.abs(holeFloor.getRadius() - 8.0) < 1e-4);

        // 2. Through hole on Cuboid (TOP -> BOTTOM)
        shape_item_ui_main block = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-30, 0, -20), new Point3D(30, 40, 20));
        hole_feature_ui_main throughHole = new hole_feature_ui_main("TH1", block.getId(), "Through 1", HoleType.SIMPLE,
            face_kind_ui_main.TOP, 5.0, -5.0, 12.0, 40.0, true, 0, 0, 0, 0, CutoutShape.CIRCLE, 0);
        block.addHole(throughHole);

        topology_body_ui_main blockBody = block.getTopology();
        topology_derived_face_ui_main boreWall = blockBody.getDerivedFaceById(block.getId() + ":F:TOP:BORE_WALL:TH1");
        check("Through Hole: Continuous Bore Wall exists", boreWall != null);
        check("Through Hole: Bore Wall is through-bore", boreWall != null && boreWall.getRegionKind().isThroughBore());
        check("Through Hole: Bore Wall radius is 6.0", boreWall != null && Math.abs(boreWall.getRadius() - 6.0) < 1e-4);
        check("Through Hole: Bore Wall depth matches thickness (40.0)", boreWall != null && Math.abs(boreWall.getDepth() - 40.0) < 1e-4);

        topology_derived_face_ui_main noFloor = blockBody.getDerivedFaceById(block.getId() + ":F:TOP:HOLE_FLOOR:TH1");
        check("Through Hole: NO hole floor generated", noFloor == null);

        topology_derived_face_ui_main remBottom = blockBody.getDerivedFaceById(block.getId() + ":F:BOTTOM:REMAINING");
        check("Through Hole: Exit BOTTOM face has remaining face region", remBottom != null);
        check("Through Hole: Exit face has inner opening loop", remBottom != null && remBottom.getInnerLoops().size() == 1);

        // 3. Multiple holes on single face
        shape_item_ui_main multiShape = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, new Point3D(-50, 0, -50), new Point3D(50, 30, 50));
        hole_feature_ui_main mh1 = new hole_feature_ui_main("MH1", multiShape.getId(), "Hole A", HoleType.SIMPLE, face_kind_ui_main.TOP, -20.0, 0.0, 10.0, 15.0, false, 0,0,0,0, CutoutShape.CIRCLE, 0);
        hole_feature_ui_main mh2 = new hole_feature_ui_main("MH2", multiShape.getId(), "Hole B", HoleType.SIMPLE, face_kind_ui_main.TOP, 0.0, 0.0, 12.0, 15.0, false, 0,0,0,0, CutoutShape.CIRCLE, 0);
        hole_feature_ui_main mh3 = new hole_feature_ui_main("MH3", multiShape.getId(), "Hole C", HoleType.SIMPLE, face_kind_ui_main.TOP, 20.0, 0.0, 14.0, 15.0, false, 0,0,0,0, CutoutShape.CIRCLE, 0);
        multiShape.addHole(mh1);
        multiShape.addHole(mh2);
        multiShape.addHole(mh3);

        topology_body_ui_main multiBody = multiShape.getTopology();
        topology_derived_face_ui_main multiRemTop = multiBody.getDerivedFaceById(multiShape.getId() + ":F:TOP:REMAINING");
        check("Multi-Hole: Exactly ONE remaining face on TOP", multiRemTop != null);
        check("Multi-Hole: Remaining face has exactly 3 inner loops", multiRemTop != null && multiRemTop.getInnerLoops().size() == 3);
        check("Multi-Hole: 3 distinct hole walls exist", multiBody.getDerivedFacesForFeature("MH1").size() >= 2 &&
                                                          multiBody.getDerivedFacesForFeature("MH2").size() >= 2 &&
                                                          multiBody.getDerivedFacesForFeature("MH3").size() >= 2);

        // 4. Boundary crossing rejection
        shape_item_ui_main smallCube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(-10, 0, -10), new Point3D(10, 20, 10));
        hole_feature_ui_main edgeHole = new hole_feature_ui_main("EH1", smallCube.getId(), "Crossing Hole", HoleType.SIMPLE,
            face_kind_ui_main.TOP, 9.0, 0.0, 8.0, 10.0, false, 0,0,0,0, CutoutShape.CIRCLE, 0);
        smallCube.addHole(edgeHole);
        topology_body_ui_main edgeBody = smallCube.getTopology();
        check("Boundary Check: Hole crossing edge marked INVALID", edgeHole.getState() == ui.workspace.drafting.features.feature_state_ui_main.INVALID);
        check("Boundary Check: Has diagnostic message", edgeHole.getDiagnosticMessage() != null && edgeHole.getDiagnosticMessage().contains("multi-face"));

        // 5. Deterministic Topology Signature
        String sig1 = topology_signature_ui_main.generateSignature(multiBody);
        multiShape.rebuild();
        topology_body_ui_main multiBodyRebuilt = multiShape.getTopology();
        String sig2 = topology_signature_ui_main.generateSignature(multiBodyRebuilt);
        check("Topology Signature: Non-empty SHA-256 string", sig1 != null && sig1.length() == 64);
        check("Topology Signature: Deterministic across rebuild (sig1 == sig2)", sig1.equals(sig2));

        // 6. Face Selection Resolver
        var resFloor = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.TOP, 0.0, 0.0);
        check("Face Resolver: Center of hole resolves to HOLE_WALL", resFloor.isDerived() && resFloor.derivedFace().getRegionKind().isWall());
        check("Face Resolver: Target hole is H1", resFloor.targetHole() == blindHole);

        var resRem = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.TOP, 20.0, 20.0);
        check("Face Resolver: Outside hole resolves to REMAINING_FACE", resRem.isDerived() && resRem.derivedFace().getRegionKind().isRemainingFace());

        var resSide = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.FRONT, 0.0, 0.0);
        check("Face Resolver: Unmodified face resolves to Base Face", !resSide.isDerived() && resSide.baseFace() != null);

        // 7. Appearance Overrides & Persistence
        String wallId = cube.getId() + ":F:TOP:HOLE_WALL:H1";
        topology_face_appearance_ui_main customApp = new topology_face_appearance_ui_main(Color.web("#EC4899"), 0.65, true);
        shape_face_appearance_helper_ui_main.setAppearance(cube.getId(), wallId, customApp);
        cube.rebuild();

        topology_derived_face_ui_main wallAfterRebuild = cube.getTopology().getDerivedFaceById(wallId);
        check("Appearance Override: Survives rebuild", wallAfterRebuild != null && wallAfterRebuild.hasAppearanceOverride());
        check("Appearance Override: Correct color applied", wallAfterRebuild != null && wallAfterRebuild.getEffectiveColor().equals(Color.web("#EC4899")));

        // Test File Persistence of Appearance Overrides
        File tmpFile = new File("scratch/test_model_appearance.nd");
        boolean saved = document_serializer_ui_main.saveToFile(tmpFile, List.of(cube));
        check("Serialization: Saved .nd file successfully", saved && tmpFile.exists());

        shape_face_appearance_helper_ui_main.clearAll();
        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tmpFile);
        check("Serialization: Loaded 1 shape from .nd", loaded.size() == 1);
        if (!loaded.isEmpty()) {
            shape_item_ui_main loadedCube = loaded.get(0);
            topology_derived_face_ui_main loadedWall = loadedCube.getTopology().getDerivedFaceById(wallId);
            check("Serialization: Loaded derived face exists", loadedWall != null);
            check("Serialization: Restored appearance override", loadedWall != null && loadedWall.hasAppearanceOverride());
            check("Serialization: Restored color", loadedWall != null && loadedWall.getEffectiveColor().equals(Color.web("#EC4899")));
        }
        if (tmpFile.exists()) tmpFile.delete();

        // 8. Feature Manager Tree integration
        feature_tree_node_ui_main holeNode = feature_tree_node_ui_main.forHole(cube, blindHole);
        check("Feature Manager: Node is hole", holeNode.isHole());

        feature_tree_node_ui_main dfNode = feature_tree_node_ui_main.forDerivedFace(cube, blindHole, holeWall);
        check("Feature Manager: Derived face node created", dfNode.isDerivedFace());
        check("Feature Manager: Derived face label matches", dfNode.getLabel().contains("Hole Wall"));
        check("Feature Manager: Derived face icon loaded", dfNode.getIcon() != null);
    }
}
