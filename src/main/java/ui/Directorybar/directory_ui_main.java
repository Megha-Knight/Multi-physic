package ui.Directorybar;

import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import ui.navigationbar.navigation_ui_main;

/**
 * directory_ui_main.java
 * Directory Bar: Left vertical sidebar panel below Navigation Bar.
 * Width: 260px, flexible vertical height.
 */
public class directory_ui_main extends StackPane {

    public directory_ui_main() {
        setPadding(new Insets(10));
        setPrefWidth(navigation_ui_main.COLUMN_WIDTH);
        setMinWidth(navigation_ui_main.COLUMN_WIDTH);
        setMaxWidth(navigation_ui_main.COLUMN_WIDTH);
        getStyleClass().add("directory-bar");

        setStyle(
            "-fx-background-color: linear-gradient(to bottom, #F8FAFC, #E4E9F0);" +
            "-fx-border-color: #C9D1D9;" +
            "-fx-border-width: 0 1 0 0;"
        );
    }
}
