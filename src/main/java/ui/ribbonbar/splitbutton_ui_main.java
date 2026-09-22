package ui.ribbonbar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * splitbutton_ui_main.java
 * Compact CAD ribbon split button:
 *   - Top half: action icon button
 *   - Bottom half: text label + dropdown arrow
 * Dimensions: 44px width, 44px height.
 */
public class splitbutton_ui_main extends VBox {

    private final VBox topHalf;
    private final HBox bottomHalf;
    private final ContextMenu dropMenu;
    private Runnable primaryAction;

    private static final String STYLE_BASE =
        "-fx-background-color: transparent; -fx-background-radius: 3;";
    private static final String STYLE_TOP_HOVER =
        "-fx-background-color: rgba(41, 171, 226, 0.15); -fx-background-radius: 3 3 0 0;";
    private static final String STYLE_BOT_HOVER =
        "-fx-background-color: rgba(41, 171, 226, 0.22); -fx-background-radius: 0 0 3 3;";
    private static final String STYLE_TOP_PRESSED =
        "-fx-background-color: rgba(0, 90, 133, 0.25); -fx-background-radius: 3 3 0 0;";
    private static final String STYLE_BOT_PRESSED =
        "-fx-background-color: rgba(0, 90, 133, 0.35); -fx-background-radius: 0 0 3 3;";

    public splitbutton_ui_main(String text, Node icon, Runnable primaryAction) {
        this(text, icon, ui.framework_ui_main.RIBBON_BUTTON_WIDTH, primaryAction);
    }

    public splitbutton_ui_main(String text, Node icon, double width, Runnable primaryAction) {
        this.primaryAction = primaryAction;
        this.dropMenu = new ContextMenu();

        double btnH = ui.framework_ui_main.RIBBON_BUTTON_HEIGHT;

        setPrefWidth(width);
        setMinWidth(width - 4);
        setMaxWidth(width + 4);
        setPrefHeight(btnH);
        setMinHeight(btnH - 2);
        setMaxHeight(btnH + 2);
        setAlignment(Pos.CENTER);
        setStyle(STYLE_BASE);

        // TOP HALF: Action Icon + Text Label
        topHalf = new VBox(1);
        topHalf.setAlignment(Pos.CENTER);
        topHalf.setPrefHeight(btnH * 0.72);
        topHalf.setMinHeight(btnH * 0.68);
        topHalf.setPadding(new Insets(2, 1, 1, 1));

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: " + ui.framework_ui_main.RIBBON_LABEL_FONT_SIZE + "px; -fx-text-fill: " + ui.framework_ui_main.TEXT_MAIN_COLOR + ";");
        topHalf.getChildren().addAll(icon, textLabel);

        setupTopHalfInteractions();

        // BOTTOM HALF: Centered Dropdown Arrow Only
        bottomHalf = new HBox();
        bottomHalf.setAlignment(Pos.CENTER);
        bottomHalf.setPrefHeight(btnH * 0.28);
        bottomHalf.setMinHeight(btnH * 0.25);
        bottomHalf.setPadding(new Insets(0, 2, 2, 2));

        Node arrow = ribbonicons_ui_main.createArrowDown(5.5, Color.web("#52606D"));
        bottomHalf.getChildren().add(arrow);

        setupBottomHalfInteractions();

        getChildren().addAll(topHalf, bottomHalf);
    }

    public void addMenuItem(String title, Runnable action) {
        addMenuItem(title, null, action);
    }

    public void addMenuItem(String title, Node graphic, Runnable action) {
        MenuItem item = graphic != null ? new MenuItem(title, graphic) : new MenuItem(title);
        item.setOnAction(e -> {
            if (action != null) action.run();
        });
        dropMenu.getItems().add(item);
    }

    public void setPrimaryAction(Runnable primaryAction) {
        this.primaryAction = primaryAction;
    }

    public ContextMenu getDropMenu() {
        return dropMenu;
    }

    private void setupTopHalfInteractions() {
        topHalf.setOnMouseEntered(e -> topHalf.setStyle(STYLE_TOP_HOVER));
        topHalf.setOnMouseExited(e -> topHalf.setStyle(STYLE_BASE));
        topHalf.setOnMousePressed(e -> topHalf.setStyle(STYLE_TOP_PRESSED));
        topHalf.setOnMouseReleased(e -> {
            topHalf.setStyle(topHalf.isHover() ? STYLE_TOP_HOVER : STYLE_BASE);
            if (topHalf.isHover() && primaryAction != null) {
                primaryAction.run();
            }
        });
    }

    private void setupBottomHalfInteractions() {
        bottomHalf.setOnMouseEntered(e -> bottomHalf.setStyle(STYLE_BOT_HOVER));
        bottomHalf.setOnMouseExited(e -> bottomHalf.setStyle(STYLE_BASE));
        bottomHalf.setOnMousePressed(e -> {
            bottomHalf.setStyle(STYLE_BOT_PRESSED);
            if (!dropMenu.isShowing() && !dropMenu.getItems().isEmpty()) {
                dropMenu.show(bottomHalf, Side.BOTTOM, 0, 0);
            } else {
                dropMenu.hide();
            }
        });
        bottomHalf.setOnMouseReleased(e -> {
            bottomHalf.setStyle(bottomHalf.isHover() ? STYLE_BOT_HOVER : STYLE_BASE);
        });
    }
}
