package ui.viewbar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

/**
 * view_ui_main.java
 * View Bar: Visually integrated viewport control strip.
 * Height: 40px. Kept clean and minimal per requirements.
 */
public class view_ui_main extends HBox {

    public view_ui_main() {
        setPadding(new Insets(4, 12, 4, 12));
        setPrefHeight(40);
        setMinHeight(36);
        setMaxHeight(44);
        setAlignment(Pos.CENTER_RIGHT);
        getStyleClass().add("view-bar");

        setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-border-color: #C9D1D9;" +
            "-fx-border-width: 0 0 1 0;"
        );
    }
}
