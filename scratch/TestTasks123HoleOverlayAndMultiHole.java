package scratch;

import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.derived_region_advanced_mesh_ui_main;
import ui.workspace.drafting.topology.derived_region_visual_builder_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_derived_face_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.holes.hole_cuboid_builder_ui_main;
import ui.workspace.shapes.holes.hole_profile_helper_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * TestTasks123HoleOverlayAndMultiHole.java
 *
 * Verifies Tasks 1, 2, and 3:
 * 1. Non-circular cutout wall and floor meshes (3 vertices for triangles, 4 for quads, 32 for circles).
 * 2. Cuboid overlay alignment and flushness (non-uniform cuboid face frames match physical body planes).
 * 3. Multi-hole triangulation on same face (both spaced apart and close together) leaves both holes open.
 */
public class TestTasks123HoleOverlayAndMultiHole {

    private static int passed = 0;
    private static int total = 0;

    private static void check(boolean condition, String message) {
        total++;
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Testing Task 1, 2, and 3: Hole Overlays, Cuboid Flushness & Multi-Hole Triangulation ===");

        testTask1NonCircularProfiles();
        testTask2CuboidOverlayFlushness();
        testTask3MultiHoleTriangulation();

        System.out.println(String.format("%nResult: %d/%d tests passed.", passed, total));
        if (passed != total) {
            System.exit(1);
        }
    }

    private static void testTask1NonCircularProfiles() {
        System.out.println("\n--- TASK 1: Non-Circular Wall & Floor Overlays ---");

        // 1. Circle hole
        hole_feature_ui_main hCircle = new hole_feature_ui_main(
            "h-circle", "owner-1", HoleType.SIMPLE, face_kind_ui_main.TOP,
            0, 0, 10.0, 20.0, false, 0, 90.0, 0, 0, CutoutShape.CIRCLE, 0
        );
        check(hole_profile_helper_ui_main.getProfileVertexCount(hCircle, 32) == 32,
            "Circle profile vertex count is 32");

        // 2. Square hole
        hole_feature_ui_main hSquare = new hole_feature_ui_main(
            "h-square", "owner-1", HoleType.SIMPLE, face_kind_ui_main.TOP,
            0, 0, 10.0, 20.0, false, 0, 90.0, 0, 0, CutoutShape.SQUARE, 0
        );
        check(hole_profile_helper_ui_main.getProfileVertexCount(hSquare, 32) == 4,
            "Square profile vertex count is 4");

        // 3. Rectangle hole
        hole_feature_ui_main hRect = new hole_feature_ui_main(
            "h-rect", "owner-1", HoleType.SIMPLE, face_kind_ui_main.TOP,
            0, 0, 10.0, 20.0, false, 0, 90.0, 0, 0, CutoutShape.RECTANGLE, 15.0
        );
        check(hole_profile_helper_ui_main.getProfileVertexCount(hRect, 32) == 4,
            "Rectangle profile vertex count is 4");

        // 4. Equilateral Triangle hole
        hole_feature_ui_main hEqTri = new hole_feature_ui_main(
            "h-eqtri", "owner-1", HoleType.SIMPLE, face_kind_ui_main.TOP,
            0, 0, 10.0, 20.0, false, 0, 90.0, 0, 0, CutoutShape.EQUILATERAL_TRIANGLE, 0
        );
        check(hole_profile_helper_ui_main.getProfileVertexCount(hEqTri, 32) == 3,
            "Equilateral triangle profile vertex count is 3");

        // 5. Right Triangle hole
        hole_feature_ui_main hRtTri = new hole_feature_ui_main(
            "h-rttri", "owner-1", HoleType.SIMPLE, face_kind_ui_main.TOP,
            0, 0, 10.0, 20.0, false, 0, 90.0, 0, 0, CutoutShape.RIGHT_TRIANGLE, 12.0
        );
        check(hole_profile_helper_ui_main.getProfileVertexCount(hRtTri, 32) == 3,
            "Right triangle profile vertex count is 3");

        // 6. Test mesh generation: Triangle wall mesh has exactly 6 vertices (3 top, 3 bot)
        Point3D center = new Point3D(0, -50, 0);
        Point3D inDir = new Point3D(0, 1, 0);
        Point3D u = new Point3D(1, 0, 0);
        Point3D v = new Point3D(0, 0, 1);
        TriangleMesh triWall = derived_region_advanced_mesh_ui_main.createProfileWall(center, inDir, u, v, 5.0, 20.0, hEqTri);
        check(triWall.getPoints().size() / 3 == 6, "Triangular wall mesh has exactly 6 vertices (3 top, 3 bot)");
        check(triWall.getFaces().size() / 6 == 6, "Triangular wall mesh has 6 triangles (3 rectangular side walls)");

        // 7. Test mesh generation: Square wall mesh has exactly 8 vertices (4 top, 4 bot)
        TriangleMesh sqWall = derived_region_advanced_mesh_ui_main.createProfileWall(center, inDir, u, v, 5.0, 20.0, hSquare);
        check(sqWall.getPoints().size() / 3 == 8, "Square wall mesh has exactly 8 vertices (4 top, 4 bot)");
        check(sqWall.getFaces().size() / 6 == 8, "Square wall mesh has 8 triangles (4 rectangular side walls)");
    }

    private static void testTask2CuboidOverlayFlushness() {
        System.out.println("\n--- TASK 2: Cuboid Overlay Flushness & Alignment ---");

        // Create a non-uniform rectangular cuboid:
        // p1 = (-50, 0, -30), p2 = (50, -40, 30) -> width = 100, height = 40, depth = 60
        shape_item_ui_main cuboid = new shape_item_ui_main(
            basic_shapes_ui_main.CUBOID,
            new Point3D(-50, 0, -30),
            new Point3D(50, -40, 30)
        );

        // Add a hole on TOP face (y = -40)
        hole_feature_ui_main topHole = new hole_feature_ui_main(
            "h-top", cuboid.getId(), HoleType.SIMPLE, face_kind_ui_main.TOP,
            0, 0, 8.0, 15.0, false, 0, 90.0, 0, 0, CutoutShape.CIRCLE, 0
        );
        cuboid.addHole(topHole);

        // Add a hole on FRONT face (z = +30)
        hole_feature_ui_main frontHole = new hole_feature_ui_main(
            "h-front", cuboid.getId(), HoleType.SIMPLE, face_kind_ui_main.FRONT,
            0, 0, 6.0, 10.0, false, 0, 90.0, 0, 0, CutoutShape.CIRCLE, 0
        );
        cuboid.addHole(frontHole);

        topology_body_ui_main body = topology_body_ui_main.buildTopology(cuboid);

        var visuals = derived_region_visual_builder_ui_main.buildDerivedRegionVisuals(cuboid, body);
        check(visuals.getChildren().size() >= 4, "Derived region visuals generated for cuboid (walls & floors)");

        // Verify top hole wall vertices start precisely at face plane Y = -40
        boolean topWallFlush = false;
        boolean frontWallFlush = false;

        for (var node : visuals.getChildren()) {
            if (node instanceof MeshView mv) {
                if (mv.getId().contains("TOP:HOLE_WALL:h-top")) {
                    TriangleMesh tm = (TriangleMesh) mv.getMesh();
                    float minY = Float.MAX_VALUE;
                    for (int i = 1; i < tm.getPoints().size(); i += 3) {
                        minY = Math.min(minY, tm.getPoints().get(i));
                    }
                    // In JavaFX, Y is negative upward, top face is at Y = -40
                    topWallFlush = Math.abs(minY - (-40.0f)) < 1e-3f;
                }
                if (mv.getId().contains("FRONT:HOLE_WALL:h-front")) {
                    TriangleMesh tm = (TriangleMesh) mv.getMesh();
                    float maxZ = -Float.MAX_VALUE;
                    for (int i = 2; i < tm.getPoints().size(); i += 3) {
                        maxZ = Math.max(maxZ, tm.getPoints().get(i));
                    }
                    // Front face is at Z = +30
                    frontWallFlush = Math.abs(maxZ - 30.0f) < 1e-3f;
                }
            }
        }

        check(topWallFlush, "Top hole overlay starts flush at cuboid TOP plane (Y = -40)");
        check(frontWallFlush, "Front hole overlay starts flush at cuboid FRONT plane (Z = +30)");
    }

    private static void testTask3MultiHoleTriangulation() {
        System.out.println("\n--- TASK 3: Multi-Hole Face Triangulation ---");

        Point3D p1 = new Point3D(-50, 0, -50);
        Point3D p2 = new Point3D(50, -100, 50);

        // Case A: Two holes spaced apart on TOP face
        List<hole_feature_ui_main> spacedHoles = new ArrayList<>();
        spacedHoles.add(new hole_feature_ui_main(
            "h1", "shape-test", HoleType.SIMPLE, face_kind_ui_main.TOP,
            -25.0, 0.0, 10.0, 30.0, false, 0, 90.0, 0, 0, CutoutShape.CIRCLE, 0
        ));
        spacedHoles.add(new hole_feature_ui_main(
            "h2", "shape-test", HoleType.SIMPLE, face_kind_ui_main.TOP,
            25.0, 0.0, 10.0, 30.0, false, 0, 90.0, 0, 0, CutoutShape.CIRCLE, 0
        ));

        MeshView spacedMesh = (MeshView) hole_cuboid_builder_ui_main.buildCuboidWithHoles(p1, p2, spacedHoles, false, false);
        check(spacedMesh != null, "Spaced multi-hole cuboid mesh built");
        TriangleMesh tmSpaced = (TriangleMesh) spacedMesh.getMesh();
        check(tmSpaced.getPoints().size() > 100, "Spaced multi-hole mesh has detailed vertex geometry");

        // Case B: Two holes close together on TOP face (proximity test)
        // Centers at u = -12 and u = +12, radius = 9. Distance between boundaries is only 6 units!
        List<hole_feature_ui_main> closeHoles = new ArrayList<>();
        closeHoles.add(new hole_feature_ui_main(
            "h-close-1", "shape-test", HoleType.SIMPLE, face_kind_ui_main.TOP,
            -12.0, 0.0, 9.0, 30.0, false, 0, 90.0, 0, 0, CutoutShape.CIRCLE, 0
        ));
        closeHoles.add(new hole_feature_ui_main(
            "h-close-2", "shape-test", HoleType.SIMPLE, face_kind_ui_main.TOP,
            12.0, 0.0, 9.0, 30.0, false, 0, 90.0, 0, 0, CutoutShape.CIRCLE, 0
        ));

        MeshView closeMesh = (MeshView) hole_cuboid_builder_ui_main.buildCuboidWithHoles(p1, p2, closeHoles, false, false);
        check(closeMesh != null, "Close multi-hole cuboid mesh built successfully");
        TriangleMesh tmClose = (TriangleMesh) closeMesh.getMesh();
        check(tmClose.getPoints().size() > 100, "Close multi-hole mesh has full vertex set");

        // Verify that vertices exist at cavity depths for BOTH holes (proves cavities are NOT omitted)
        int verticesAtDepth30 = 0;
        for (int i = 1; i < tmClose.getPoints().size(); i += 3) {
            float y = tmClose.getPoints().get(i);
            // Top face is at Y = -100, depth 30 goes into solid to Y = -70
            if (Math.abs(y - (-70.0f)) < 1e-2f) {
                verticesAtDepth30++;
            }
        }
        check(verticesAtDepth30 >= 64, "Both holes generated cavity floor vertices at Y = -70 (count=" + verticesAtDepth30 + ")");

        // Case C: Independent selection & topology for multiple holes on the same face
        shape_item_ui_main shape = new shape_item_ui_main(basic_shapes_ui_main.CUBE, p1, p2);
        shape.addHole(closeHoles.get(0));
        shape.addHole(closeHoles.get(1));

        topology_body_ui_main body = topology_body_ui_main.buildTopology(shape);

        List<topology_derived_face_ui_main> walls = new ArrayList<>();
        List<topology_derived_face_ui_main> floors = new ArrayList<>();
        for (var df : body.getDerivedFaces()) {
            if (df.getRegionKind() == ui.workspace.drafting.topology.hole_region_kind_ui_main.HOLE_WALL) walls.add(df);
            if (df.getRegionKind() == ui.workspace.drafting.topology.hole_region_kind_ui_main.HOLE_FLOOR) floors.add(df);
        }

        check(walls.size() == 2, "Topology registers exactly 2 independent HOLE_WALL faces");
        check(floors.size() == 2, "Topology registers exactly 2 independent HOLE_FLOOR faces");
        check(!walls.get(0).getId().equals(walls.get(1).getId()), "Each hole has distinct addressable topology ID");
    }
}
