package ui.footerbar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import ui.framework_ui_main;

/**
 * footer_ui_main.java
 * Compact Blue Footer/Status Bar displaying the opened file location.
 * Dimensions and styling are controlled centrally by framework_ui_main.
 */
public class footer_ui_main extends HBox {

    private final Label filePathLabel;
    private final Label statusLabel;
    private String openedFilePath = "";

    public footer_ui_main() {
        setPadding(new Insets(1, 10, 1, 10));
        setPrefHeight(framework_ui_main.FOOTER_BAR_HEIGHT);
        setMinHeight(framework_ui_main.FOOTER_BAR_HEIGHT - 2);
        setMaxHeight(framework_ui_main.FOOTER_BAR_HEIGHT + 4);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("footer-bar");

        setStyle(
            "-fx-background-color: " + framework_ui_main.FOOTER_BAR_BG + ";" +
            "-fx-border-color: " + framework_ui_main.FOOTER_BORDER + ";" +
            "-fx-border-width: 1 0 0 0;"
        );

        filePathLabel = new Label();
        filePathLabel.setStyle(
            "-fx-text-fill: " + framework_ui_main.FOOTER_TEXT_COLOR + "; " +
            "-fx-font-size: " + framework_ui_main.FOOTER_FONT_SIZE + "px; " +
            "-fx-font-family: 'Segoe UI', Consolas, monospace;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("Ready");
        statusLabel.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.75); -fx-font-size: 10.5px;"
        );

        getChildren().addAll(filePathLabel, spacer, statusLabel);

        setOpenedFilePath(framework_ui_main.astraDirectory().getAbsolutePath());
    }

    public void setOpenedFilePath(String path) {
        this.openedFilePath = (path != null) ? path : "";
        filePathLabel.setText(this.openedFilePath);
    }

    public String getOpenedFilePath() {
        return openedFilePath;
    }

    public void setStatusText(String status) {
        statusLabel.setText(status != null ? status : "");
    }
}
