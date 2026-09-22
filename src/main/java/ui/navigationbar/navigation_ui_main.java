package ui.navigationbar;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import ui.featuremanager.featuremanager_ui_main;
import ui.fileexplorer.fileexplorer_ui_main;
import ui.framework_ui_main;

/**
 * navigation_ui_main.java
 * Model Navigator space split into two sections:
 *   - Top half: File Explorer (fileexplorer_ui_main)
 *   - Bottom half: Feature Manager (featuremanager_ui_main)
 * Dimensions and styling are controlled centrally by framework_ui_main.
 */
public class navigation_ui_main extends BorderPane {

    public static final double COLUMN_WIDTH = framework_ui_main.NAVIGATOR_WIDTH;

    private final fileexplorer_ui_main fileExplorer;
    private final featuremanager_ui_main featureManager;
    private final SplitPane splitPane;

    public navigation_ui_main() {
        setPrefWidth(framework_ui_main.NAVIGATOR_WIDTH);
        setMinWidth(framework_ui_main.NAVIGATOR_MIN_WIDTH);
        setMaxWidth(framework_ui_main.NAVIGATOR_MAX_WIDTH);
        getStyleClass().add("model-navigator-pane");

        fileExplorer = new fileexplorer_ui_main();
        featureManager = new featuremanager_ui_main();

        // Vertical split pane dividing top and bottom halves
        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(fileExplorer, featureManager);
        splitPane.setDividerPositions(framework_ui_main.NAVIGATOR_SPLIT_RATIO);

        splitPane.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-box-border: transparent;" +
            "-fx-padding: 0;"
        );

        setCenter(splitPane);
    }

    public fileexplorer_ui_main getFileExplorer() {
        return fileExplorer;
    }

    public featuremanager_ui_main getFeatureManager() {
        return featureManager;
    }

    public SplitPane getSplitPane() {
        return splitPane;
    }
}
