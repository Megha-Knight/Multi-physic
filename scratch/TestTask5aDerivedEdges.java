package scratch;

import javafx.application.Platform;
import javafx.geometry.Point3D;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.concurrent.CountDownLatch;

public class TestTask5aDerivedEdges {

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

        System.out.println("\nAll Task 5a tests finished: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
        System.exit(0);
    }

    private static void runTests() {
        System.out.println("=== Running TestTask5aDerivedEdges ===");

        // --- 1. Blind Hole Rim Registration on Cube ---
        shape_item_ui_main cube = new shape_item_ui_main("cube1", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-25, 25, -25), new Point3D(25, -25, 25), 0, 0, 0);
        hole_feature_ui_main hBlind = new hole_feature_ui_main("h_blind", "cube1", "Hole Blind",
                HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 10.0, 15.0, false, 0, 90, 0, 0, CutoutShape.CIRCLE, 0);
        cube.addHole(hBlind);

        topology_body_ui_main topo = cube.getTopology();
        check("1a. Cube topology built", topo != null);

        topology_edge_ui_main holeRim = topo.getEdge("HOLE_RIM_h_blind");
        check("1b. Hole rim edge exists by name", holeRim != null);
        check("1c. Hole rim edge exists by ID", topo.getEdgeById("cube1:E:HOLE_RIM:h_blind") != null);
        check("1d. Hole rim is non-linear (circular)", holeRim != null && !holeRim.isLinear());
        check("1e. Hole rim length is diameter (10.0)", holeRim != null && Math.abs(holeRim.getLength() - 10.0) < 1e-4);
        check("1f. Hole rim creating feature is h_blind", holeRim != null && "h_blind".equals(holeRim.getCreatingFeatureId()));
        check("1g. Hole rim has at least 2 adjacent faces", holeRim != null && holeRim.getAdjacentFaceIds().size() >= 2);
        check("1h. Adjacent face includes remaining face", holeRim != null && holeRim.getAdjacentFaceIds().contains("cube1:F:TOP:REMAINING"));
        check("1i. Adjacent face includes hole wall", holeRim != null && holeRim.getAdjacentFaceIds().contains("cube1:F:TOP:HOLE_WALL:h_blind"));

        // --- 2. Chamfer & Fillet on Hole Rim ---
        chamfer_feature_ui_main chValid = new chamfer_feature_ui_main("ch1", "cube1", "HOLE_RIM_h_blind", 2.0);
        check("2a. Chamfer 2.0mm on hole rim revalidates true", chValid.revalidate(cube));

        fillet_feature_ui_main filValid = new fillet_feature_ui_main("fil1", "cube1", "HOLE_RIM_h_blind", 2.0);
        check("2b. Fillet 2.0mm on hole rim revalidates true", filValid.revalidate(cube));

        chamfer_feature_ui_main chTooBig = new chamfer_feature_ui_main("ch2", "cube1", "HOLE_RIM_h_blind", 6.0);
        check("2c. Chamfer 6.0mm (>= radius 5.0) revalidates false", !chTooBig.revalidate(cube));

        // --- 3. Through-Hole Rims (Entry and Exit) ---
        shape_item_ui_main cube2 = new shape_item_ui_main("cube2", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-25, 25, -25), new Point3D(25, -25, 25), 0, 0, 0);
        hole_feature_ui_main hThru = new hole_feature_ui_main("h_thru", "cube2", "Through Bore",
                HoleType.SIMPLE, face_kind_ui_main.TOP, 0, 0, 12.0, 50.0, true, 0, 90, 0, 0, CutoutShape.CIRCLE, 0);
        cube2.addHole(hThru);

        topology_body_ui_main topo2 = cube2.getTopology();
        topology_edge_ui_main entryRim = topo2.getEdge("HOLE_RIM_h_thru");
        topology_edge_ui_main exitRim = topo2.getEdge("HOLE_EXIT_RIM_h_thru");
        check("3a. Through hole entry rim exists", entryRim != null);
        check("3b. Through hole exit rim exists", exitRim != null);
        check("3c. Entry rim length is 12.0", entryRim != null && Math.abs(entryRim.getLength() - 12.0) < 1e-4);
        check("3d. Exit rim length is 12.0", exitRim != null && Math.abs(exitRim.getLength() - 12.0) < 1e-4);
        check("3e. Exit rim adjacent to bottom remaining", exitRim != null && exitRim.getAdjacentFaceIds().contains("cube2:F:BOTTOM:REMAINING"));

        // --- 4. Extrusion Boundary Edges ---
        shape_item_ui_main cube3 = new shape_item_ui_main("cube3", "Cube", basic_shapes_ui_main.CUBE,
                new Point3D(-25, 25, -25), new Point3D(25, -25, 25), 0, 0, 0);
        extrude_feature_ui_main ext1 = new extrude_feature_ui_main("ext1", "cube3", "Boss 1",
                face_kind_ui_main.TOP, CutoutShape.CIRCLE, 0, 0, 16.0, 0, 10.0);
        cube3.addExtrude(ext1);

        topology_body_ui_main topo3 = cube3.getTopology();
        topology_edge_ui_main extBaseRim = topo3.getEdge("EXTRUDE_BASE_RIM_ext1");
        topology_edge_ui_main extCapRim = topo3.getEdge("EXTRUDE_CAP_RIM_ext1");

        check("4a. Extrude base rim exists", extBaseRim != null);
        check("4b. Extrude cap rim exists", extCapRim != null);
        check("4c. Extrude base rim length is diameter (16.0)", extBaseRim != null && Math.abs(extBaseRim.getLength() - 16.0) < 1e-4);
        check("4d. Extrude cap rim length is diameter (16.0)", extCapRim != null && Math.abs(extCapRim.getLength() - 16.0) < 1e-4);
        check("4e. Extrude base rim adjacent to host face", extBaseRim != null && extBaseRim.getAdjacentFaceIds().contains("cube3:F:TOP"));
        check("4f. Extrude cap rim adjacent to extrude cap face", extCapRim != null && extCapRim.getAdjacentFaceIds().contains("cube3:F:EXTRUDE_CAP:ext1"));

        chamfer_feature_ui_main chExt = new chamfer_feature_ui_main("chExt", "cube3", "EXTRUDE_CAP_RIM_ext1", 2.0);
        check("4g. Chamfer 2.0mm on extrude cap rim revalidates true", chExt.revalidate(cube3));

        fillet_feature_ui_main filExt = new fillet_feature_ui_main("filExt", "cube3", "EXTRUDE_BASE_RIM_ext1", 2.0);
        check("4h. Fillet 2.0mm on extrude base rim revalidates true", filExt.revalidate(cube3));

        // --- 5. Cylinder Cap Rim Non-zero Length ---
        shape_item_ui_main cyl = new shape_item_ui_main("cyl1", "Cylinder", basic_shapes_ui_main.CYLINDER,
                new Point3D(0, 0, 0), new Point3D(20, -40, 0), 0, 0, 0);
        topology_body_ui_main topoCyl = cyl.getTopology();
        topology_edge_ui_main topCylRim = topoCyl.getEdge("TOP_RIM");
        check("5a. Cylinder TOP_RIM length > 0 (equals 2r = 40.0)", topCylRim != null && Math.abs(topCylRim.getLength() - 40.0) < 1e-4);
        chamfer_feature_ui_main chCyl = new chamfer_feature_ui_main("chCyl", "cyl1", "TOP_RIM", 3.0);
        check("5b. Chamfer 3.0mm on cylinder TOP_RIM revalidates true", chCyl.revalidate(cyl));
    }
}
