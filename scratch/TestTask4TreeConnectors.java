package scratch;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import ui.featuremanager.feature_tree_connector_ui_main;
import ui.featuremanager.feature_tree_cell_ui_main;
import ui.featuremanager.feature_tree_node_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestTask4TreeConnectors {

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

        System.out.println("\nAll Task 4 tests finished: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
        System.exit(0);
    }

    private static void runTests() {
        System.out.println("=== Running TestTask4TreeConnectors ===");

        // --- 1. Path Hierarchy & Depth ---
        TreeItem<String> root = new TreeItem<>("HiddenRoot");
        TreeItem<String> shapeItem = new TreeItem<>("Shape1");
        TreeItem<String> holeItem = new TreeItem<>("Hole1");
        TreeItem<String> sketchItem = new TreeItem<>("Sketch1");
        TreeItem<String> wallItem = new TreeItem<>("BoreWall");
        TreeItem<String> floorItem = new TreeItem<>("BoreFloor");

        holeItem.getChildren().addAll(wallItem, floorItem);
        shapeItem.getChildren().addAll(holeItem, sketchItem);
        root.getChildren().add(shapeItem);

        List<TreeItem<?>> shapePath = feature_tree_connector_ui_main.getPathFromRoot(shapeItem);
        check("1a. Top-level shape path size is 1", shapePath.size() == 1);
        check("1b. Top-level shape path contains shapeItem", shapePath.get(0) == shapeItem);

        List<TreeItem<?>> holePath = feature_tree_connector_ui_main.getPathFromRoot(holeItem);
        check("1c. Feature level path size is 2", holePath.size() == 2);
        check("1d. Feature level parent is shapeItem", holePath.get(0) == shapeItem);
        check("1e. Feature level child is holeItem", holePath.get(1) == holeItem);

        List<TreeItem<?>> wallPath = feature_tree_connector_ui_main.getPathFromRoot(wallItem);
        check("1f. Derived face level path size is 3", wallPath.size() == 3);
        check("1g. Derived face parent is holeItem", wallPath.get(1) == holeItem);
        check("1h. Derived face item is wallItem", wallPath.get(2) == wallItem);

        // --- 2. Guide Lines Canvas Geometry ---
        Canvas shapeLines = feature_tree_connector_ui_main.renderGuideLines(shapeItem, 22.0);
        check("2a. Shape lines node is null (root level needs no left indent guides)", shapeLines == null);

        Canvas holeLines = feature_tree_connector_ui_main.renderGuideLines(holeItem, 22.0);
        check("2b. Feature lines canvas non-null", holeLines != null);
        check("2c. Feature lines canvas width is COL_WIDTH (18px)", holeLines.getWidth() == feature_tree_connector_ui_main.COL_WIDTH);
        check("2d. Feature lines canvas height matches row height (22px)", holeLines.getHeight() == 22.0);

        Canvas wallLines = feature_tree_connector_ui_main.renderGuideLines(wallItem, 22.0);
        check("2e. Derived face lines canvas width is 2 * COL_WIDTH (36px)", wallLines.getWidth() == 2 * feature_tree_connector_ui_main.COL_WIDTH);

        Canvas floorLines = feature_tree_connector_ui_main.renderGuideLines(floorItem, 22.0);
        check("2f. Floor lines canvas width is 2 * COL_WIDTH (36px)", floorLines.getWidth() == 2 * feature_tree_connector_ui_main.COL_WIDTH);

        // --- 3. Sibling Position Logic Verification ---
        check("3a. Hole1 is NOT last child of Shape1 (has Sketch1 after)", shapeItem.getChildren().indexOf(holeItem) < shapeItem.getChildren().size() - 1);
        check("3b. Sketch1 IS last child of Shape1", shapeItem.getChildren().indexOf(sketchItem) == shapeItem.getChildren().size() - 1);
        check("3c. WallItem is NOT last child of Hole1 (has FloorItem after)", holeItem.getChildren().indexOf(wallItem) < holeItem.getChildren().size() - 1);
        check("3d. FloorItem IS last child of Hole1", holeItem.getChildren().indexOf(floorItem) == holeItem.getChildren().size() - 1);

        // --- 4. Disclosure Chevrons & Leaf Connectors ---
        shapeItem.setExpanded(true);
        Node shapeDisc = feature_tree_connector_ui_main.createDisclosureNode(shapeItem, 22.0);
        check("4a. Shape disclosure node is StackPane button", shapeDisc instanceof StackPane);
        StackPane shapePane = (StackPane) shapeDisc;
        check("4b. Shape disclosure contains Polygon chevron", !shapePane.getChildren().isEmpty() && shapePane.getChildren().get(0) instanceof Polygon);

        holeItem.setExpanded(false);
        Node holeDisc = feature_tree_connector_ui_main.createDisclosureNode(holeItem, 22.0);
        check("4c. Hole disclosure node is StackPane button", holeDisc instanceof StackPane);

        // Click toggles expanded state
        shapeItem.setExpanded(false);
        check("4d. Pre-toggle shape is collapsed", !shapeItem.isExpanded());
        shapeDisc.getOnMouseClicked().handle(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false,
                false, false, false, false, false, false, null));
        check("4e. Clicking disclosure button expanded shapeItem", shapeItem.isExpanded());

        // Leaf disclosure is connecting horizontal dash Canvas
        Node wallDisc = feature_tree_connector_ui_main.createDisclosureNode(wallItem, 22.0);
        check("4f. Leaf item (wall) disclosure is horizontal dash Canvas", wallDisc instanceof Canvas);
        Canvas wallDash = (Canvas) wallDisc;
        check("4g. Leaf dash width is 14px", wallDash.getWidth() == 14.0);

        // --- 5. TreeCell Integration ---
        feature_tree_cell_ui_main cell = new feature_tree_cell_ui_main(null, null);
        check("5a. feature_tree_cell_ui_main instantiates cleanly", cell != null);
    }
}
