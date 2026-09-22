package ui.toolbar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import ui.framework_ui_main;

/**
 * tools_ui_main.java
 * Compact Top Tab/Toolbar Strip carrying the primary application tabs.
 * Dimensions and styling are controlled centrally by framework_ui_main.
 */
public class tools_ui_main extends HBox {

    private final HBox homeTab;

    public tools_ui_main() {
        setPadding(new Insets(0, 8, 0, 8));
        setPrefHeight(framework_ui_main.TAB_TOOLBAR_HEIGHT);
        setMinHeight(framework_ui_main.TAB_TOOLBAR_HEIGHT - 2);
        setMaxHeight(framework_ui_main.TAB_TOOLBAR_HEIGHT + 4);
        setAlignment(Pos.BOTTOM_LEFT);
        setSpacing(2);
        getStyleClass().add("tools-bar");

        setStyle(
            "-fx-background-color: " + framework_ui_main.TAB_TOOLBAR_BG + ";" +
            "-fx-border-color: " + framework_ui_main.TAB_BORDER_COLOR + ";" +
            "-fx-border-width: 0 0 1 0;"
        );

        homeTab = createTab("Home", true);
        getChildren().add(homeTab);
    }

    private HBox createTab(String title, boolean active) {
        HBox tab = new HBox();
        tab.setAlignment(Pos.CENTER);
        tab.setPadding(new Insets(3, 12, 3, 12));

        Label label = new Label(title);
        label.setStyle(
            "-fx-font-size: " + framework_ui_main.TAB_FONT_SIZE + "px; " +
            "-fx-font-weight: " + (active ? "bold" : "normal") + "; " +
            "-fx-text-fill: " + (active ? framework_ui_main.PRIMARY_BRAND_COLOR : "#E2EEF5") + ";"
        );

        tab.getChildren().add(label);

        if (active) {
            tab.setStyle(
                "-fx-background-color: " + framework_ui_main.RIBBON_BAR_BG + "; " +
                "-fx-border-color: " + framework_ui_main.RIBBON_BAR_BORDER + " " + framework_ui_main.RIBBON_BAR_BORDER + " transparent " + framework_ui_main.RIBBON_BAR_BORDER + "; " +
                "-fx-border-width: 1 1 0 1; " +
                "-fx-border-radius: 3 3 0 0; " +
                "-fx-background-radius: 3 3 0 0;"
            );
        } else {
            tab.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            tab.setOnMouseEntered(e ->
                tab.setStyle("-fx-background-color: rgba(255, 255, 255, 0.18); -fx-background-radius: 3 3 0 0;")
            );
            tab.setOnMouseExited(e -> tab.setStyle("-fx-background-color: transparent;"));
        }

        return tab;
    }

    public HBox getHomeTab() {
        return homeTab;
    }
}
