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
import ui.framework_ui_main;

/**
 * splitbutton_ui_main.java
 * Compact CAD ribbon split button supporting both bottom-split and side-split dropdown arrows.
 */
public class splitbutton_ui_main extends HBox {

    private final ContextMenu dropMenu = new ContextMenu();
    private Runnable primaryAction;
    private Runnable dropAction;

    private static final String STYLE_BASE = "-fx-background-color: transparent; -fx-background-radius: 3;";
    private static final String STYLE_ACT_HOVER = "-fx-background-color: rgba(41, 171, 226, 0.15); -fx-background-radius: 3;";
    private static final String STYLE_DRP_HOVER = "-fx-background-color: rgba(41, 171, 226, 0.22); -fx-background-radius: 3;";
    private static final String STYLE_ACT_PRESSED = "-fx-background-color: rgba(0, 90, 133, 0.25); -fx-background-radius: 3;";
    private static final String STYLE_DRP_PRESSED = "-fx-background-color: rgba(0, 90, 133, 0.35); -fx-background-radius: 3;";

    public splitbutton_ui_main(String text, Node icon, Runnable primaryAction) {
        this(text, icon, framework_ui_main.RIBBON_BUTTON_WIDTH, false, primaryAction);
    }

    public splitbutton_ui_main(String text, Node icon, double width, Runnable primaryAction) {
        this(text, icon, width, false, primaryAction);
    }

    public splitbutton_ui_main(String text, Node icon, double width, boolean arrowOnSide, Runnable primaryAction) {
        this.primaryAction = primaryAction;
        double btnH = framework_ui_main.RIBBON_BUTTON_HEIGHT;

        setPrefWidth(width);
        setMinWidth(width - 4);
        setMaxWidth(width + 4);
        setPrefHeight(btnH);
        setMinHeight(btnH - 2);
        setMaxHeight(btnH + 2);
        setAlignment(Pos.CENTER);
        setStyle(STYLE_BASE);

        Node arrow = ribbonicons_ui_main.createArrowDown(5.0, Color.web("#52606D"));

        if (!arrowOnSide) {
            // Classic vertical bottom split
            VBox topHalf = new VBox(1, icon, createLabel(text));
            topHalf.setAlignment(Pos.CENTER);
            topHalf.setPrefHeight(btnH * 0.72);
            topHalf.setPadding(new Insets(2, 1, 1, 1));
            bindAction(topHalf, () -> { if (this.primaryAction != null) this.primaryAction.run(); }, STYLE_ACT_HOVER, STYLE_ACT_PRESSED);

            HBox botHalf = new HBox(arrow);
            botHalf.setAlignment(Pos.CENTER);
            botHalf.setPrefHeight(btnH * 0.28);
            bindDrop(botHalf, STYLE_DRP_HOVER, STYLE_DRP_PRESSED);

            VBox column = new VBox(topHalf, botHalf);
            column.setAlignment(Pos.CENTER);
            column.setPrefWidth(width);
            getChildren().add(column);
        } else {
            // Modern horizontal side-arrow split
            VBox actionBox = new VBox(1, icon, createLabel(text));
            actionBox.setAlignment(Pos.CENTER);
            actionBox.setPrefWidth(width - 15);
            actionBox.setPrefHeight(btnH);
            actionBox.setPadding(new Insets(2, 2, 2, 2));
            bindAction(actionBox, () -> { if (this.primaryAction != null) this.primaryAction.run(); }, STYLE_ACT_HOVER, STYLE_ACT_PRESSED);

            VBox arrowBox = new VBox(arrow);
            arrowBox.setAlignment(Pos.CENTER);
            arrowBox.setPrefWidth(15);
            arrowBox.setPrefHeight(btnH);
            bindDrop(arrowBox, STYLE_DRP_HOVER, STYLE_DRP_PRESSED);

            getChildren().addAll(actionBox, arrowBox);
        }
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: " + framework_ui_main.RIBBON_LABEL_FONT_SIZE + "px; -fx-text-fill: " + framework_ui_main.TEXT_MAIN_COLOR + ";");
        return lbl;
    }

    private void bindAction(Node node, Runnable act, String hoverStyle, String pressedStyle) {
        node.setOnMouseEntered(e -> node.setStyle(hoverStyle));
        node.setOnMouseExited(e -> node.setStyle(STYLE_BASE));
        node.setOnMousePressed(e -> node.setStyle(pressedStyle));
        node.setOnMouseReleased(e -> {
            node.setStyle(node.isHover() ? hoverStyle : STYLE_BASE);
            if (node.isHover() && act != null) act.run();
        });
    }

    private void bindDrop(Node node, String hoverStyle, String pressedStyle) {
        node.setOnMouseEntered(e -> node.setStyle(hoverStyle));
        node.setOnMouseExited(e -> node.setStyle(STYLE_BASE));
        node.setOnMousePressed(e -> {
            node.setStyle(pressedStyle);
            if (dropAction != null) {
                dropAction.run();
            } else if (!dropMenu.isShowing() && !dropMenu.getItems().isEmpty()) {
                dropMenu.show(node, Side.BOTTOM, 0, 0);
            } else {
                dropMenu.hide();
            }
        });
        node.setOnMouseReleased(e -> node.setStyle(node.isHover() ? hoverStyle : STYLE_BASE));
    }

    public void addMenuItem(String title, Runnable action) { addMenuItem(title, null, action); }

    public void addMenuItem(String title, Node graphic, Runnable action) {
        MenuItem item = graphic != null ? new MenuItem(title, graphic) : new MenuItem(title);
        item.setOnAction(e -> { if (action != null) action.run(); });
        dropMenu.getItems().add(item);
    }

    public void setPrimaryAction(Runnable primaryAction) { this.primaryAction = primaryAction; }
    public void setDropAction(Runnable dropAction) { this.dropAction = dropAction; }
    public ContextMenu getDropMenu() { return dropMenu; }
}
