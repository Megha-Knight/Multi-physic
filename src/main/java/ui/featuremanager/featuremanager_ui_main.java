package ui.featuremanager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

/**
 * featuremanager_ui_main.java
 * Bottom panel of the Model Navigator space: Feature Manager.
 * Features a clean header bar with block title and an empty content area.
 */
public class featuremanager_ui_main extends BorderPane {

    private final Pane contentArea;

    public featuremanager_ui_main() {
        getStyleClass().add("feature-manager-pane");
        setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-border-color: #C9D1D9;" +
            "-fx-border-width: 0 1 0 0;"
        );

        // Header Bar with Block Name
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 10, 4, 10));
        header.setPrefHeight(26);
        header.setMinHeight(24);
        header.setMaxHeight(28);
        header.setStyle(
            "-fx-background-color: #F1F5F9;" +
            "-fx-border-color: #CBD5E1;" +
            "-fx-border-width: 0 0 1 0;"
        );

        Label titleLabel = new Label("Feature Manager");
        titleLabel.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1F2933;"
        );
        header.getChildren().add(titleLabel);

        setTop(header);

        // Empty body content area ready for future feature/geometry tree
        contentArea = new Pane();
        contentArea.setStyle("-fx-background-color: #FFFFFF;");
        setCenter(contentArea);
    }

    public Pane getContentArea() {
        return contentArea;
    }
}
