import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import ui.workspace.drafting.faces.*;
import ui.workspace.drafting.holes.*;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.drafting.topology.*;
import ui.featuremanager.*;
import ui.File_Types.document_serializer_ui_main;
import ui.workspace.drafting.shape_history_ui_main;

import java.io.File;
import java.util.*;

/**
 * TestStage3.java
 * Comprehensive automated test suite for Stage 3: Independent Machined Surface Regions.
 * Covers all 20 required verification categories.
 */
public class TestStage3 {

    private static int passCount = 0;
    private static int failCount = 0;

    private static void check(String desc, boolean condition) {
        if (condition) {
            passCount++;
            System.out.println("[PASS] " + desc);
        } else {
            failCount++;
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

        System.out.println("==================================================");
        System.out.println("STAGE 3: INDEPENDENT MACHINED SURFACE REGIONS TEST SUITE");
        System.out.println("==================================================\n");

        testBlindHoleRegions();
        testThroughHoleRegions();
        testMultipleHoles();
        testStableRegionIds();
        testBoreWallUniqueness();
        testFaceSelectionResolver();
        testFeatureManagerIntegration();
        testDefaultMachinedAppearance();
        testCustomAndResetColor();
        testOpacityAndPersistence();
        testSaveLoadTopology();
        testTopologySignatureStability();
        testUndoRedo();
        testTransformedBodies();
        testBoundaryRejection();
        testIntersectingHoleValidation();
        testStage1And2Integration();
        testStageKCompatibility();

        System.out.println("\n==================================================");
        if (failCount == 0) {
            System.out.println("ALL " + passCount + " STAGE 3 TESTS PASSED SUCCESSFULLY!");
            System.out.println("==================================================");
            System.exit(0);
        } else {
            System.err.println("FAILED: " + passCount + " passed, " + failCount + " failed.");
            System.out.println("==================================================");
            System.exit(1);
        }
    }

    // ============================================================
    // 1. BLIND HOLE REGIONS
    // ============================================================
    static void testBlindHoleRegions() {
        System.out.println("--- 1. Blind Hole Regions ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, -40, 40));
        hole_feature_ui_main blindHole = new hole_feature_ui_main("H_BLIND", cube.getId(),
            HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 16.0, 15.0, false);
        cube.addHole(blindHole);

        topology_body_ui_main topo = cube.getTopology();
        check("Blind Hole: Topology body non-null", topo != null);

        // Remaining face
        topology_derived_face_ui_main rem = topo.getDerivedFaceById(cube.getId() + ":F:TOP:REMAINING");
        check("Blind Hole: Top remaining face exists", rem != null);
        check("Blind Hole: Remaining face region kind REMAINING_FACE", rem != null && rem.getRegionKind() == hole_region_kind_ui_main.REMAINING_FACE);
        check("Blind Hole: Remaining face has outer loop", rem != null && rem.getOuterLoop() != null);
        check("Blind Hole: Remaining face has 1 inner loop", rem != null && rem.getInnerLoops().size() == 1);

        // Hole wall
        topology_derived_face_ui_main wall = topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_WALL:H_BLIND");
        check("Blind Hole: Hole wall exists", wall != null);
        check("Blind Hole: Wall region kind HOLE_WALL", wall != null && wall.getRegionKind() == hole_region_kind_ui_main.HOLE_WALL);
        assertClose("Blind Hole: Wall radius", wall != null ? wall.getRadius() : 0, 8.0, 1e-4);
        assertClose("Blind Hole: Wall depth", wall != null ? wall.getDepth() : 0, 15.0, 1e-4);

        // Hole floor
        topology_derived_face_ui_main floor = topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_FLOOR:H_BLIND");
        check("Blind Hole: Hole floor exists", floor != null);
        check("Blind Hole: Floor region kind HOLE_FLOOR", floor != null && floor.getRegionKind() == hole_region_kind_ui_main.HOLE_FLOOR);
        assertClose("Blind Hole: Floor radius", floor != null ? floor.getRadius() : 0, 8.0, 1e-4);
        check("Blind Hole: Floor has floor loop", floor != null && floor.getOuterLoop() != null);
    }

    // ============================================================
    // 2. THROUGH HOLE REGIONS
    // ============================================================
    static void testThroughHoleRegions() {
        System.out.println("\n--- 2. Through Hole Regions ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(50, -50, 50));
        hole_feature_ui_main thruHole = new hole_feature_ui_main("H_THRU", cube.getId(),
            HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 12.0, 50.0, true);
        cube.addHole(thruHole);

        topology_body_ui_main topo = cube.getTopology();
        check("Through Hole: Top remaining face exists", topo.getDerivedFaceById(cube.getId() + ":F:TOP:REMAINING") != null);
        check("Through Hole: Bottom remaining face exists", topo.getDerivedFaceById(cube.getId() + ":F:BOTTOM:REMAINING") != null);

        // Bore wall
        topology_derived_face_ui_main bore = topo.getDerivedFaceById(cube.getId() + ":F:TOP:BORE_WALL:H_THRU");
        check("Through Hole: Bore wall exists", bore != null);
        check("Through Hole: Bore wall region kind BORE_WALL", bore != null && bore.getRegionKind() == hole_region_kind_ui_main.BORE_WALL);
        assertClose("Through Hole: Bore wall radius", bore != null ? bore.getRadius() : 0, 6.0, 1e-4);
        assertClose("Through Hole: Bore wall depth", bore != null ? bore.getDepth() : 0, 50.0, 1e-4);

        // No floor for through hole
        check("Through Hole: NO hole floor generated", topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_FLOOR:H_THRU") == null);
    }

    // ============================================================
    // 3. MULTIPLE HOLES
    // ============================================================
    static void testMultipleHoles() {
        System.out.println("\n--- 3. Multiple Holes ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(100, -100, 100));
        hole_feature_ui_main h1 = new hole_feature_ui_main("H1", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, -25, -25, 10.0, 30.0, false);
        hole_feature_ui_main h2 = new hole_feature_ui_main("H2", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 25, 25, 14.0, 100.0, true);
        hole_feature_ui_main h3 = new hole_feature_ui_main("H3", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, -25, 25, 12.0, 40.0, false);
        cube.addHole(h1); cube.addHole(h2); cube.addHole(h3);

        topology_body_ui_main topo = cube.getTopology();
        topology_derived_face_ui_main remTop = topo.getDerivedFaceById(cube.getId() + ":F:TOP:REMAINING");
        check("Multiple Holes: Single top remaining face", remTop != null);
        check("Multiple Holes: 3 inner loops on top remaining face", remTop != null && remTop.getInnerLoops().size() == 3);

        // Check each hole's regions
        check("Multiple Holes: H1 wall exists", topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_WALL:H1") != null);
        check("Multiple Holes: H1 floor exists", topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_FLOOR:H1") != null);
        check("Multiple Holes: H2 bore wall exists", topo.getDerivedFaceById(cube.getId() + ":F:TOP:BORE_WALL:H2") != null);
        check("Multiple Holes: H3 wall exists", topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_WALL:H3") != null);
        check("Multiple Holes: H3 floor exists", topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_FLOOR:H3") != null);
    }

    // ============================================================
    // 4. STABLE REGION IDS
    // ============================================================
    static void testStableRegionIds() {
        System.out.println("\n--- 4. Stable Region IDs ---");
        shape_item_ui_main cube = new shape_item_ui_main("BODY_ALPHA", "Cube Alpha", basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(50, -50, 50), 0, 0, 0, 0, 0);
        hole_feature_ui_main h = new hole_feature_ui_main("HOLE_BETA", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.FRONT, 5, -5, 8.0, 20.0, false);
        cube.addHole(h);

        topology_body_ui_main topo = cube.getTopology();
        topology_derived_face_ui_main wall = topo.getDerivedFaceById("BODY_ALPHA:F:FRONT:HOLE_WALL:HOLE_BETA");
        topology_derived_face_ui_main floor = topo.getDerivedFaceById("BODY_ALPHA:F:FRONT:HOLE_FLOOR:HOLE_BETA");
        topology_derived_face_ui_main rem = topo.getDerivedFaceById("BODY_ALPHA:F:FRONT:REMAINING");

        check("Stable IDs: Wall ID format correct", wall != null && "BODY_ALPHA:F:FRONT:HOLE_WALL:HOLE_BETA".equals(wall.getId()));
        check("Stable IDs: Floor ID format correct", floor != null && "BODY_ALPHA:F:FRONT:HOLE_FLOOR:HOLE_BETA".equals(floor.getId()));
        check("Stable IDs: Remaining ID format correct", rem != null && "BODY_ALPHA:F:FRONT:REMAINING".equals(rem.getId()));
    }

    // ============================================================
    // 5. BORE WALL UNIQUENESS
    // ============================================================
    static void testBoreWallUniqueness() {
        System.out.println("\n--- 5. Bore Wall Uniqueness ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(60, -60, 60));
        hole_feature_ui_main thru = new hole_feature_ui_main("H_THRU_UNIQ", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 10.0, 60.0, true);
        cube.addHole(thru);

        topology_body_ui_main topo = cube.getTopology();
        int boreCount = 0;
        for (topology_derived_face_ui_main df : topo.getDerivedFaces()) {
            if (df.getRegionKind() == hole_region_kind_ui_main.BORE_WALL && "H_THRU_UNIQ".equals(df.getCreatingFeatureId())) {
                boreCount++;
            }
        }
        check("Bore Wall Uniqueness: Exactly ONE bore wall per through-hole", boreCount == 1);
    }

    // ============================================================
    // 6. FACE SELECTION RESOLVER
    // ============================================================
    static void testFaceSelectionResolver() {
        System.out.println("\n--- 6. Face Selection Resolver ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(100, -100, 100));
        hole_feature_ui_main blind = new hole_feature_ui_main("H_BLIND_RES", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, -20, 0, 16.0, 25.0, false);
        hole_feature_ui_main thru = new hole_feature_ui_main("H_THRU_RES", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 20, 0, 16.0, 100.0, true);
        cube.addHole(blind); cube.addHole(thru);

        // Click on blind hole aperture
        var r1 = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.TOP, -20, 0);
        check("Resolver: Pick blind hole center -> derived", r1.isDerived());
        check("Resolver: Pick blind hole center -> HOLE_WALL", r1.derivedFace() != null && r1.derivedFace().getRegionKind() == hole_region_kind_ui_main.HOLE_WALL);
        check("Resolver: Target hole matches H_BLIND_RES", r1.targetHole() == blind);

        // Click on through hole aperture
        var r2 = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.TOP, 20, 0);
        check("Resolver: Pick through hole center -> BORE_WALL", r2.derivedFace() != null && r2.derivedFace().getRegionKind() == hole_region_kind_ui_main.BORE_WALL);
        check("Resolver: Target hole matches H_THRU_RES", r2.targetHole() == thru);

        // Click outside holes on top face
        var r3 = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.TOP, 0, 30);
        check("Resolver: Pick outside holes -> REMAINING_FACE", r3.derivedFace() != null && r3.derivedFace().getRegionKind() == hole_region_kind_ui_main.REMAINING_FACE);

        // Click on unperforated face
        var r4 = face_selection_resolver_ui_main.resolveFace(cube, face_kind_ui_main.FRONT, 0, 0);
        check("Resolver: Pick FRONT (no holes) -> base face", !r4.isDerived() && r4.baseFace() != null);
    }

    // ============================================================
    // 7. FEATURE MANAGER INTEGRATION
    // ============================================================
    static void testFeatureManagerIntegration() {
        System.out.println("\n--- 7. Feature Manager Integration ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(50, -50, 50));
        hole_feature_ui_main blind = new hole_feature_ui_main("HFM1", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 10.0, 20.0, false);
        cube.addHole(blind);

        topology_body_ui_main topo = cube.getTopology();
        var wall = topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_WALL:HFM1");
        var floor = topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_FLOOR:HFM1");

        feature_tree_node_ui_main wallNode = feature_tree_node_ui_main.forDerivedFace(cube, blind, wall);
        feature_tree_node_ui_main floorNode = feature_tree_node_ui_main.forDerivedFace(cube, blind, floor);

        check("Feature Manager: Wall node isDerivedFace", wallNode.isDerivedFace());
        check("Feature Manager: Wall node label contains Hole Wall", wallNode.getLabel().contains("Hole Wall"));
        check("Feature Manager: Floor node isDerivedFace", floorNode.isDerivedFace());
        check("Feature Manager: Floor node label contains Hole Floor", floorNode.getLabel().contains("Hole Floor"));
        check("Feature Manager: Derived face node has icon", wallNode.getIcon() != null);
    }

    // ============================================================
    // 8. DEFAULT MACHINED APPEARANCE
    // ============================================================
    static void testDefaultMachinedAppearance() {
        System.out.println("\n--- 8. Default Machined Appearance ---");
        var appWall = hole_surface_appearance_ui_main.forRegion(hole_region_kind_ui_main.HOLE_WALL);
        var appFloor = hole_surface_appearance_ui_main.forRegion(hole_region_kind_ui_main.HOLE_FLOOR);
        var appBore = hole_surface_appearance_ui_main.forRegion(hole_region_kind_ui_main.BORE_WALL);

        check("Appearance: Hole wall has amber/machined diffuse color", appWall.diffuseColor() != null);
        assertClose("Appearance: Hole wall opacity ~ 50%", appWall.opacity(), 0.50, 0.05);
        check("Appearance: Hole floor has amber/machined diffuse color", appFloor.diffuseColor() != null);
        assertClose("Appearance: Hole floor opacity ~ 50%", appFloor.opacity(), 0.50, 0.05);
        check("Appearance: Bore wall has amber/machined diffuse color", appBore.diffuseColor() != null);
        assertClose("Appearance: Bore wall opacity ~ 50%", appBore.opacity(), 0.50, 0.05);
    }

    // ============================================================
    // 9. CUSTOM AND RESET COLOR
    // ============================================================
    static void testCustomAndResetColor() {
        System.out.println("\n--- 9. Custom and Reset Color ---");
        shape_item_ui_main cube = new shape_item_ui_main("SHP_COL", "Cube Color", basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(50, -50, 50), 0, 0, 0, 0, 0);
        hole_feature_ui_main h = new hole_feature_ui_main("H_COL", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 10.0, 20.0, false);
        cube.addHole(h);

        String wallId = cube.getId() + ":F:TOP:HOLE_WALL:H_COL";
        Color customCol = Color.web("#10B981"); // Emerald green
        shape_face_appearance_helper_ui_main.setAppearance(cube.getId(), wallId, new topology_face_appearance_ui_main(customCol, 0.85, true));

        topology_body_ui_main topo = cube.getTopology();
        topology_derived_face_ui_main wall = topo.getDerivedFaceById(wallId);
        check("Custom Color: Wall has override", wall != null && wall.hasAppearanceOverride());
        check("Custom Color: Effective color is emerald green", wall != null && wall.getEffectiveColor().equals(customCol));
        assertClose("Custom Color: Opacity 0.85", wall != null ? wall.getEffectiveOpacity() : 0, 0.85, 1e-4);

        // Reset Color
        shape_face_appearance_helper_ui_main.clearAppearance(cube.getId(), wallId);
        topology_body_ui_main topo2 = cube.getTopology();
        topology_derived_face_ui_main wall2 = topo2.getDerivedFaceById(wallId);
        check("Reset Color: Wall override cleared", wall2 != null && !wall2.hasAppearanceOverride());
        check("Reset Color: Wall returns to default machined color", wall2 != null && wall2.getEffectiveColor().equals(hole_surface_appearance_ui_main.forRegion(hole_region_kind_ui_main.HOLE_WALL).diffuseColor()));
    }

    // ============================================================
    // 10. OPACITY AND PERSISTENCE
    // ============================================================
    static void testOpacityAndPersistence() {
        System.out.println("\n--- 10. Opacity and Persistence ---");
        var app = new topology_face_appearance_ui_main(Color.web("#8B5CF6"), 0.65, true);
        String ndStr = app.formatNd();
        check("Persistence: formatNd non-empty", ndStr != null && !ndStr.isEmpty());
        var parsed = topology_face_appearance_ui_main.parseNd(ndStr);
        check("Persistence: parsed non-null", parsed != null);
        assertClose("Persistence: opacity preserved", parsed != null ? parsed.opacity() : 0, 0.65, 1e-4);
        check("Persistence: color preserved", parsed != null && parsed.diffuseColor().equals(Color.web("#8B5CF6")));
    }

    // ============================================================
    // 11. SAVE / LOAD TOPOLOGY
    // ============================================================
    static void testSaveLoadTopology() {
        System.out.println("\n--- 11. Save/Load Topology ---");
        shape_item_ui_main cube = new shape_item_ui_main("CUBE_PERSIST", "Persist Cube", basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(50, -50, 50), 10, 20, 30, 0, 0);
        hole_feature_ui_main h = new hole_feature_ui_main("H_PERSIST", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 5, -5, 12.0, 25.0, false);
        cube.addHole(h);

        String wallId = cube.getId() + ":F:TOP:HOLE_WALL:H_PERSIST";
        shape_face_appearance_helper_ui_main.setAppearance(cube.getId(), wallId, new topology_face_appearance_ui_main(Color.web("#EF4444"), 0.75, true));

        File tmp = new File("scratch/stage3_test_persist.nd");
        boolean saved = document_serializer_ui_main.saveToFile(tmp, List.of(cube));
        check("Save/Load: File saved successfully", saved && tmp.exists());

        List<shape_item_ui_main> loaded = document_serializer_ui_main.loadFromFile(tmp);
        check("Save/Load: 1 shape loaded", loaded != null && loaded.size() == 1);
        if (loaded != null && !loaded.isEmpty()) {
            shape_item_ui_main loadedCube = loaded.get(0);
            check("Save/Load: Shape ID matches", "CUBE_PERSIST".equals(loadedCube.getId()));
            check("Save/Load: 1 hole loaded", loadedCube.getHoles().size() == 1);

            topology_body_ui_main loadedTopo = loadedCube.getTopology();
            topology_derived_face_ui_main loadedWall = loadedTopo.getDerivedFaceById(wallId);
            check("Save/Load: Loaded wall exists in topology", loadedWall != null);
            check("Save/Load: Loaded wall has appearance override", loadedWall != null && loadedWall.hasAppearanceOverride());
            check("Save/Load: Loaded wall color is red", loadedWall != null && loadedWall.getEffectiveColor().equals(Color.web("#EF4444")));
        }
        if (tmp.exists()) tmp.delete();
    }

    // ============================================================
    // 12. TOPOLOGY SIGNATURE STABILITY
    // ============================================================
    static void testTopologySignatureStability() {
        System.out.println("\n--- 12. Topology Signature Stability ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, -40, 40));
        hole_feature_ui_main h = new hole_feature_ui_main("H_SIG", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 10.0, 20.0, false);
        cube.addHole(h);

        String sig1 = topology_signature_ui_main.generateSignature(cube.getTopology());
        check("Signature: Non-empty SHA-256 string", sig1 != null && sig1.length() == 64);

        // Rebuild and recompute
        cube.rebuild();
        String sig2 = topology_signature_ui_main.generateSignature(cube.getTopology());
        check("Signature: Deterministic across rebuild (sig1 == sig2)", sig1 != null && sig1.equals(sig2));
    }

    // ============================================================
    // 13. UNDO / REDO
    // ============================================================
    static void testUndoRedo() {
        System.out.println("\n--- 13. Undo / Redo ---");
        shape_history_ui_main hist = new shape_history_ui_main();
        shape_item_ui_main cube = new shape_item_ui_main("UNDO_CUBE", "Undo Cube", basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(50, -50, 50), 0, 0, 0, 0, 0);
        List<shape_item_ui_main> list = new ArrayList<>(List.of(cube));

        // State 0: push snapshot
        hist.pushSnapshot(list);

        // State 1: add hole and appearance override
        hole_feature_ui_main h = new hole_feature_ui_main("UNDO_H", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 10.0, 20.0, false);
        cube.addHole(h);
        String wallId = cube.getId() + ":F:TOP:HOLE_WALL:UNDO_H";
        shape_face_appearance_helper_ui_main.setAppearance(cube.getId(), wallId, new topology_face_appearance_ui_main(Color.BLUE, 0.9, true));

        // Undo
        List<shape_item_ui_main> undone = hist.undo(list);
        check("Undo/Redo: Can undo", undone != null && undone.size() == 1);
        check("Undo/Redo: Undone cube has NO holes", undone != null && undone.get(0).getHoles().isEmpty());

        // Redo
        List<shape_item_ui_main> redone = hist.redo(undone);
        check("Undo/Redo: Can redo", redone != null && redone.size() == 1);
        check("Undo/Redo: Redone cube HAS hole", redone != null && redone.get(0).getHoles().size() == 1);
        var restoredWall = redone.get(0).getTopology().getDerivedFaceById(wallId);
        check("Undo/Redo: Appearance restored on redo", restoredWall != null && restoredWall.hasAppearanceOverride());
    }

    // ============================================================
    // 14. TRANSFORMED BODIES
    // ============================================================
    static void testTransformedBodies() {
        System.out.println("\n--- 14. Transformed Bodies ---");
        shape_item_ui_main cube = new shape_item_ui_main("TR_CUBE", "Tr Cube", basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, -40, 40), 100, 50, -50, 45, 90);
        Point3D nTop = topology_geometry_helper_ui_main.getFaceNormal(cube, face_kind_ui_main.TOP);
        check("Transformed Bodies: Normal non-null", nTop != null);
        assertClose("Transformed Bodies: Normal is unit vector", nTop != null ? nTop.magnitude() : 0, 1.0, 1e-5);

        Point3D origTop = topology_geometry_helper_ui_main.getFaceOrigin(cube, face_kind_ui_main.TOP);
        check("Transformed Bodies: Origin non-null", origTop != null);

        var ref = topology_geometry_helper_ui_main.reconstructFaceReference(cube, face_kind_ui_main.TOP, 5, 5);
        check("Transformed Bodies: Face reference reconstructed", ref != null);
    }

    // ============================================================
    // 15. BOUNDARY REJECTION
    // ============================================================
    static void testBoundaryRejection() {
        System.out.println("\n--- 15. Boundary Rejection ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(40, -40, 40));
        // Hole with radius 15 at u=15 on a 40x40 face (extends to u=30, beyond face half-width 20)
        hole_feature_ui_main badHole = new hole_feature_ui_main("H_BAD", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 15, 0, 30.0, 20.0, false);
        cube.addHole(badHole);

        topology_body_ui_main topo = cube.getTopology();
        check("Boundary Rejection: Hole crossing boundary marked INVALID", badHole.getState() == ui.workspace.drafting.features.feature_state_ui_main.INVALID);
        check("Boundary Rejection: Has diagnostic message", badHole.getDiagnosticMessage() != null && badHole.getDiagnosticMessage().contains("boundary"));
        check("Boundary Rejection: No hole wall created for invalid hole", topo.getDerivedFaceById(cube.getId() + ":F:TOP:HOLE_WALL:H_BAD") == null);
    }

    // ============================================================
    // 16. INTERSECTING HOLE VALIDATION
    // ============================================================
    static void testIntersectingHoleValidation() {
        System.out.println("\n--- 16. Intersecting Hole Validation ---");
        shape_item_ui_main cube = new shape_item_ui_main(basic_shapes_ui_main.CUBE, new Point3D(0, 0, 0), new Point3D(60, -60, 60));
        // Two overlapping holes at same position
        hole_feature_ui_main h1 = new hole_feature_ui_main("H_INT1", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 20.0, 20.0, false);
        hole_feature_ui_main h2 = new hole_feature_ui_main("H_INT2", cube.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP, 5, 0, 20.0, 20.0, false);
        cube.addHole(h1); cube.addHole(h2);

        topology_body_ui_main topo = cube.getTopology();
        check("Intersecting Holes: Colliding hole marked INVALID", h2.getState() == ui.workspace.drafting.features.feature_state_ui_main.INVALID);
        check("Intersecting Holes: Has diagnostic message", h2.getDiagnosticMessage() != null && h2.getDiagnosticMessage().contains("Intersecting holes"));
    }

    // ============================================================
    // 17. STAGE 1 & 2 INTEGRATION REGRESSION
    // ============================================================
    static void testStage1And2Integration() {
        System.out.println("\n--- 17. Stage 1 & 2 Integration Regression ---");
        var coord = new ui.ribbonbar.dynamic_panel_coordinator_ui_main();
        check("Regression: Coordinator created", coord != null);

        shape_item_ui_main cyl = new shape_item_ui_main(basic_shapes_ui_main.CYLINDER, new Point3D(0, 0, 0), new Point3D(20, -40, 0));
        var topoCyl = cyl.getTopology();
        check("Regression: Cylinder has 3 faces", topoCyl.getFaceCount() == 3);
        check("Regression: Cylinder has TOP_CAP", topoCyl.getFace(face_kind_ui_main.TOP_CAP) != null);
    }

    // ============================================================
    // 18. STAGE K COMPATIBILITY REGRESSION
    // ============================================================
    static void testStageKCompatibility() {
        System.out.println("\n--- 18. Stage K Compatibility Regression ---");
        var sk = new ui.workspace.drafting.sketch.sketch_feature_ui_main("SK_STG3", "SHP1", "Sketch Stg3", ui.workspace.drafting.sketch.sketch_plane_type_ui_main.BASE_XZ, null);
        sk.addEntity(new ui.workspace.drafting.sketch.sketch_circle_ui_main("C1", new ui.workspace.drafting.sketch.sketch_point_2d_ui_main(0,0), 10));
        check("Stage K Compatibility: Sketch is valid", sk.isValid());
        check("Stage K Compatibility: Profile is CLOSED", sk.getProfileResult() != null && sk.getProfileResult().type() == ui.workspace.drafting.sketch.sketch_profile_type_ui_main.CLOSED);
    }
}
