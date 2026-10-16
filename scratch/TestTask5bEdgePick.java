package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import ui.ribbonbar.machining_panel_ui_main;
import ui.workspace.drafting.faces.edge_picker_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.machining.machining_dialog_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.concurrent.CountDownLatch;

public class TestTask5bEdgePick {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String desc, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + desc);
            passed++;
        } else {
            System.err.println("  [FAIL] " + desc);
            failed++;
        }
    }

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            try {
                runTests();
            } catch (Throwable t) {
                t.printStackTrace();
                failed++;
            } finally {
                latch.countDown();
            }
        });
        latch.await();

        System.out.println("\nAll Task 5b tests finished: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
        System.exit(0);
    }

    private static void runTests() {
        System.out.println("=== Running TestTask5bEdgePick ===");

        // --- 1. Ribbon Bar Modify Category ---
        machining_panel_ui_main panel = new machining_panel_ui_main();
        check("machining_panel_ui_main instantiates", panel != null);

        // --- 2. Cube Linear Edge Raycast Pick ---
        shape_item_ui_main cube = new shape_item_ui_main("cube1", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-25, 0, -25), new Point3D(25, -50, 25), 0, 0, 0);
        cube.rebuild();
        topology_body_ui_main cubeTopo = cube.getTopology();
        check("Cube has 12 edges", cubeTopo.getEdges().size() == 12);

        // Raycast aiming at the top-front edge (Y = -50, Z = 25, X from -25 to 25)
        double[] rayTopFront = new double[]{0, -50, 50, 0, 0, -1};
        topology_edge_ui_main pickedLinear = edge_picker_ui_main.pickEdge(rayTopFront, cube);
        check("Raycast picks linear edge", pickedLinear != null);
        check("Picked edge is TOP_FRONT", pickedLinear != null && pickedLinear.getName().equals("TOP_FRONT"));

        // Raycast far away in empty space (X = 200, Y = 200)
        double[] rayMiss = new double[]{200, 200, 50, 0, 0, -1};
        topology_edge_ui_main pickedMiss = edge_picker_ui_main.pickEdge(rayMiss, cube);
        check("Miss ray returns null", pickedMiss == null);

        // --- 3. Cylinder Rim Edge Raycast Pick ---
        shape_item_ui_main cyl = new shape_item_ui_main("cyl1", "Cylinder", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(20, -40, 0), 0, 0, 0);
        cyl.rebuild();
        // Top rim is at Y = -40, circle of radius 20 centered at (0, -40, 0) in XZ plane
        // Ray origin (20, -60, 0) pointing up (0, 1, 0) directly hitting the top rim at (20, -40, 0)
        double[] rayCylTop = new double[]{20, -60, 0, 0, 1, 0};
        topology_edge_ui_main pickedCyl = edge_picker_ui_main.pickEdge(rayCylTop, cyl);
        check("Raycast picks cylinder rim", pickedCyl != null);
        check("Picked cylinder edge is TOP_RIM", pickedCyl != null && pickedCyl.getName().equals("TOP_RIM"));

        // Bottom rim is at Y = 0
        double[] rayCylBot = new double[]{0, 50, 20, 0, -1, 0};
        topology_edge_ui_main pickedCylBot = edge_picker_ui_main.pickEdge(rayCylBot, cyl);
        check("Raycast picks cylinder bottom rim", pickedCylBot != null && pickedCylBot.getName().equals("BOTTOM_RIM"));

        // --- 4. Cone Base Rim Raycast Pick ---
        shape_item_ui_main cone = new shape_item_ui_main("cone1", "Cone", basic_shapes_ui_main.CONE,
                new Point3D(0, 0, 0), new Point3D(25, -50, 0), 0, 0, 0);
        cone.rebuild();
        double[] rayCone = new double[]{25, 20, 0, 0, -1, 0};
        topology_edge_ui_main pickedCone = edge_picker_ui_main.pickEdge(rayCone, cone);
        check("Raycast picks cone BASE_RIM", pickedCone != null && pickedCone.getName().equals("BASE_RIM"));

        // --- 5. Blind Hole Rim Raycast Pick on Cube ---
        hole_feature_ui_main hBlind = new hole_feature_ui_main("h_blind", "cube1", "Blind Hole",
                HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 16.0, 10.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0);
        cube.addHole(hBlind);
        cube.rebuild();
        topology_body_ui_main cubeWithHoleTopo = cube.getTopology();
        check("Cube with hole has HOLE_RIM", cubeWithHoleTopo.getEdge("HOLE_RIM_h_blind") != null);

        // Raycast directly at hole rim: Hole is at U=0, V=0 on TOP face (X=0, Y=-50, Z=0), radius = 8 mm
        double[] rayHoleRim = new double[]{8, -20, 0, 0, -1, 0};
        topology_edge_ui_main pickedHole = edge_picker_ui_main.pickEdge(rayHoleRim, cube);
        check("Raycast picks hole rim edge", pickedHole != null);
        check("Picked edge is HOLE_RIM_h_blind", pickedHole != null && pickedHole.getName().equals("HOLE_RIM_h_blind"));

        // --- 6. Through Hole Exit Rim Raycast Pick ---
        shape_item_ui_main cubeThrough = new shape_item_ui_main("cube2", "Cube 2", basic_shapes_ui_main.CUBE,
                new Point3D(-25, 0, -25), new Point3D(25, -50, 25), 0, 0, 0);
        hole_feature_ui_main hThru = new hole_feature_ui_main("h_thru", "cube2", "Thru Hole",
                HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 12.0, 50.0, true, 0, 90, 0, 0, CutoutShape.CIRCLE, 0);
        cubeThrough.addHole(hThru);
        cubeThrough.rebuild();
        // Exit rim is on BOTTOM face (Y = 0) with radius 6 mm
        double[] rayExitRim = new double[]{6, 30, 0, 0, -1, 0};
        topology_edge_ui_main pickedExit = edge_picker_ui_main.pickEdge(rayExitRim, cubeThrough);
        check("Raycast picks through-hole exit rim", pickedExit != null && pickedExit.getName().equals("HOLE_EXIT_RIM_h_thru"));

        // --- 7. Extrusion Base Rim & Cap Rim Raycast Pick ---
        shape_item_ui_main cubeExt = new shape_item_ui_main("cube3", "Cube 3", basic_shapes_ui_main.CUBE,
                new Point3D(-30, 0, -30), new Point3D(30, -60, 30), 0, 0, 0);
        extrude_feature_ui_main ext = new extrude_feature_ui_main(
                "ext_boss", "cube3", "Boss", face_kind_ui_main.TOP,
                CutoutShape.CIRCLE, 0, 0, 20.0, 0.0, 15.0
        );
        cubeExt.addExtrude(ext);
        cubeExt.rebuild();
        // Cap rim is at Y = -75, radius = 10 mm
        double[] rayCapRim = new double[]{10, -90, 0, 0, 1, 0};
        topology_edge_ui_main pickedCap = edge_picker_ui_main.pickEdge(rayCapRim, cubeExt);
        check("Raycast picks extrusion cap rim", pickedCap != null && pickedCap.getName().equals("EXTRUDE_CAP_RIM_ext_boss"));

        // Base rim is at Y = -60, radius = 10 mm
        double[] rayBaseRim = new double[]{10, -50, 0, 0, -1, 0};
        topology_edge_ui_main pickedBase = edge_picker_ui_main.pickEdge(rayBaseRim, cubeExt);
        check("Raycast picks extrusion base rim", pickedBase != null && pickedBase.getName().equals("EXTRUDE_BASE_RIM_ext_boss"));

        // --- 8. Commit Fillet to Picked Hole Rim ---
        fillet_feature_ui_main fHole = new fillet_feature_ui_main("f_hole", "cube1", pickedHole.getName(), 2.0);
        check("fHole revalidates against cube", fHole.revalidate(cube));
        cube.addFillet(fHole);
        check("Cube has 1 fillet", cube.getFillets().size() == 1);

        // --- 9. Commit Chamfer to Picked Extrusion Cap Rim ---
        chamfer_feature_ui_main cExt = new chamfer_feature_ui_main("c_ext", "cube3", pickedCap.getName(), 2.5);
        check("cExt revalidates against cubeExt", cExt.revalidate(cubeExt));
        cubeExt.addChamfer(cExt);
        check("cubeExt has 1 chamfer", cubeExt.getChamfers().size() == 1);

        // --- 10. Commit Chamfer to Picked Cylinder Rim ---
        chamfer_feature_ui_main cCyl = new chamfer_feature_ui_main("c_cyl", "cyl1", pickedCyl.getName(), 3.0);
        check("cCyl revalidates against cyl", cCyl.revalidate(cyl));
        cyl.addChamfer(cCyl);
        check("cyl has 1 chamfer", cyl.getChamfers().size() == 1);

        // --- 11. Edge Pick Mode in shape_editor_ui_main ---
        javafx.scene.Group container = new javafx.scene.Group();
        javafx.scene.layout.Pane viewport = new javafx.scene.layout.Pane();
        javafx.scene.PerspectiveCamera camera = new javafx.scene.PerspectiveCamera();
        shape_editor_ui_main editor = new shape_editor_ui_main(
                container, viewport, null, null, e -> Point3D.ZERO, new javafx.scene.control.Label(), () -> false, camera
        );
        check("editor initially not in edge pick mode", !editor.isEdgePickMode());
        editor.enterEdgePickMode(machining_dialog_ui_main.Kind.FILLET);
        check("editor enters edge pick mode", editor.isEdgePickMode());
        check("edgePickKind is FILLET", editor.getEdgePickKind() == machining_dialog_ui_main.Kind.FILLET);
        editor.setHoveredEdge(pickedLinear);
        check("editor hovered edge is set", editor.getHoveredEdge() == pickedLinear);
        editor.exitEdgePickMode();
        check("editor exits edge pick mode", !editor.isEdgePickMode());
        check("editor hovered edge is cleared", editor.getHoveredEdge() == null);
    }
}
