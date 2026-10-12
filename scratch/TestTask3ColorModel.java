import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import ui.framework_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.faces.viewport_color_state_ui_main;
import ui.workspace.drafting.faces.face_overlay_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.hole_region_kind_ui_main;
import ui.workspace.drafting.topology.hole_surface_appearance_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_derived_face_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

public class TestTask3ColorModel {

    private static int passed = 0;
    private static int total = 0;

    private static void check(String name, boolean condition) {
        total++;
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            System.err.println("  [FAIL] " + name);
            throw new AssertionError("Assertion failed: " + name);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Running TestTask3ColorModel ===");

        // Test 1: Viewport Focus State Transitions (Blue <-> Gray)
        viewport_color_state_ui_main.setViewportFocused(true);
        check("1a. Focused state base color is brand blue (#0284C7)",
            viewport_color_state_ui_main.getBaseColor().equals(Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)));

        viewport_color_state_ui_main.setViewportFocused(false);
        check("1b. Unfocused state base color is unselected gray (#8C9DAE)",
            viewport_color_state_ui_main.getBaseColor().equals(Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR)));

        viewport_color_state_ui_main.setViewportFocused(true);
        check("1c. Returning to focus restores brand blue",
            viewport_color_state_ui_main.getBaseColor().equals(Color.web(framework_ui_main.OBJECT_SELECTED_COLOR)));

        // Test 2: Hole Base Color Shading (Programmatic baseColor.darker(), not amber)
        viewport_color_state_ui_main.setViewportFocused(true);
        Color expectedBlueHole = Color.web(framework_ui_main.OBJECT_SELECTED_COLOR).darker();
        check("2a. Focused hole base color is blue.darker()",
            viewport_color_state_ui_main.getHoleBaseColor().equals(expectedBlueHole));
        check("2b. Focused hole color is NOT old hardcoded amber",
            !viewport_color_state_ui_main.getHoleBaseColor().equals(Color.web("#F59E0B")));

        viewport_color_state_ui_main.setViewportFocused(false);
        Color expectedGrayHole = Color.web(framework_ui_main.OBJECT_UNSELECTED_COLOR).darker();
        check("2c. Unfocused hole base color is gray.darker()",
            viewport_color_state_ui_main.getHoleBaseColor().equals(expectedGrayHole));
        viewport_color_state_ui_main.setViewportFocused(true);

        // Test 3: Reduced Opacity Yellow Highlight (0.28 opacity, #F59E0B)
        Color hlColor = viewport_color_state_ui_main.getFaceHighlightColor();
        double hlOpacity = viewport_color_state_ui_main.getFaceHighlightOpacity();
        check("3a. Face highlight color is yellow #F59E0B", hlColor.equals(Color.web("#F59E0B")));
        check("3b. Face highlight opacity is exactly 0.28 (between 0.25 and 0.30)",
            Math.abs(hlOpacity - 0.28) < 1e-4);

        // Test 4: Derived Face Hover & Selection State Machine
        shape_item_ui_main box = new shape_item_ui_main("B1", "TestBox", basic_shapes_ui_main.CUBOID,
            new Point3D(0, 0, 0), new Point3D(100, -50, 80), 0, 0, 0);
        hole_feature_ui_main h1 = new hole_feature_ui_main("H1", box.getId(), hole_feature_ui_main.HoleType.SIMPLE,
            face_kind_ui_main.TOP, 0, 0, 10.0, 20.0, false);
        box.addHole(h1);

        topology_body_ui_main topo = box.getTopology();
        topology_derived_face_ui_main wall = topo.getDerivedFaces().stream()
            .filter(df -> df.getRegionKind() == hole_region_kind_ui_main.HOLE_WALL)
            .findFirst().orElse(null);
        check("4a. Derived wall face exists", wall != null);

        // Default state: unselected and unhovered -> darker blue
        check("4b. Unselected wall has darker blue base color",
            wall.getEffectiveColor().equals(expectedBlueHole));

        // Hovered state -> yellow at 0.28 opacity
        wall.setHovered(true);
        check("4c. Hovered wall turns yellow #F59E0B",
            wall.getEffectiveColor().equals(Color.web("#F59E0B")));
        check("4d. Hovered wall has 0.28 opacity",
            Math.abs(wall.getEffectiveOpacity() - 0.28) < 1e-4);

        // Revert hover -> returns to darker blue
        wall.setHovered(false);
        check("4e. Un-hovered wall reverts to darker blue",
            wall.getEffectiveColor().equals(expectedBlueHole));

        // Selected state -> stays yellow at 0.28 opacity
        wall.setSelected(true);
        check("4f. Selected wall stays yellow #F59E0B",
            wall.getEffectiveColor().equals(Color.web("#F59E0B")));
        check("4g. Selected wall has 0.28 opacity",
            Math.abs(wall.getEffectiveOpacity() - 0.28) < 1e-4);
        wall.setSelected(false);

        // Test 5: Multi-Hole Independent Selection (2+ holes)
        hole_feature_ui_main h2 = new hole_feature_ui_main("H2", box.getId(), hole_feature_ui_main.HoleType.SIMPLE,
            face_kind_ui_main.TOP, 25.0, 0, 8.0, 15.0, false);
        box.addHole(h2);

        topology_body_ui_main topo2 = box.getTopology();
        topology_derived_face_ui_main wallH1 = topo2.getDerivedFaces().stream()
            .filter(df -> "H1".equals(df.getCreatingFeatureId()) && df.getRegionKind() == hole_region_kind_ui_main.HOLE_WALL)
            .findFirst().orElse(null);
        topology_derived_face_ui_main floorH1 = topo2.getDerivedFaces().stream()
            .filter(df -> "H1".equals(df.getCreatingFeatureId()) && df.getRegionKind() == hole_region_kind_ui_main.HOLE_FLOOR)
            .findFirst().orElse(null);
        topology_derived_face_ui_main wallH2 = topo2.getDerivedFaces().stream()
            .filter(df -> "H2".equals(df.getCreatingFeatureId()) && df.getRegionKind() == hole_region_kind_ui_main.HOLE_WALL)
            .findFirst().orElse(null);
        topology_derived_face_ui_main floorH2 = topo2.getDerivedFaces().stream()
            .filter(df -> "H2".equals(df.getCreatingFeatureId()) && df.getRegionKind() == hole_region_kind_ui_main.HOLE_FLOOR)
            .findFirst().orElse(null);

        check("5a. All 4 derived faces exist for 2 holes",
            wallH1 != null && floorH1 != null && wallH2 != null && floorH2 != null);

        // Select wallH1 only
        wallH1.setSelected(true);
        wallH2.setSelected(false);
        floorH1.setSelected(false);
        floorH2.setSelected(false);

        check("5b. Hole 1 wall is selected yellow",
            wallH1.getEffectiveColor().equals(Color.web("#F59E0B")));
        check("5c. Hole 1 floor remains unselected darker blue",
            floorH1.getEffectiveColor().equals(expectedBlueHole));
        check("5d. Hole 2 wall remains unselected darker blue",
            wallH2.getEffectiveColor().equals(expectedBlueHole));
        check("5e. Hole 2 floor remains unselected darker blue",
            floorH2.getEffectiveColor().equals(expectedBlueHole));

        // Switch selection to floorH2
        wallH1.setSelected(false);
        floorH2.setSelected(true);

        check("5f. Hole 1 wall reverts to darker blue",
            wallH1.getEffectiveColor().equals(expectedBlueHole));
        check("5g. Hole 2 floor is selected yellow",
            floorH2.getEffectiveColor().equals(Color.web("#F59E0B")));

        // Test 6: Base Primitive Face Hover Resolution
        shape_item_ui_main cube = new shape_item_ui_main("C1", "Cube", basic_shapes_ui_main.CUBE,
            new Point3D(0, 0, 0), new Point3D(40, -40, 40), 0, 0, 0);
        // Ray pointing down towards top face (0, -40, 0)
        double[] rayTop = new double[]{ 20.0, -100.0, 20.0,  0.0, 1.0, 0.0 };
        face_reference_ui_main pickedTop = face_picker_ui_main.pickFace(rayTop, cube);
        check("6a. Ray from above picks TOP face", pickedTop != null && pickedTop.getFaceKind() == face_kind_ui_main.TOP);

        // Ray pointing from front towards front face (20, -20, 40)
        double[] rayFront = new double[]{ 20.0, -20.0, 100.0,  0.0, 0.0, -1.0 };
        face_reference_ui_main pickedFront = face_picker_ui_main.pickFace(rayFront, cube);
        check("6b. Ray from front picks FRONT face", pickedFront != null && pickedFront.getFaceKind() == face_kind_ui_main.FRONT);

        // Test 7: Face Overlay Highlight & Hover Groups
        face_overlay_ui_main overlay = new face_overlay_ui_main();
        overlay.highlightFace(pickedTop);
        check("7a. Active face highlight makes overlay visible", overlay.isVisible());

        overlay.setHoveredFace(pickedFront);
        check("7b. Hovering a different face updates hover group", overlay.isVisible());

        overlay.clearHover();
        check("7c. Clearing hover preserves active face visibility", overlay.isVisible());

        overlay.highlightFace(null);
        check("7d. Clearing active face hides overlay when no hover", !overlay.isVisible());

        System.out.println("\nAll Task 3 tests PASSED: " + passed + "/" + total);
    }
}
