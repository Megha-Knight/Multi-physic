package ui.workspace.document;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import ui.framework_ui_main;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * file_tab_ui_main.java
 * Document tabs bar situated below the breadcrumb bar in the workspace.
 * Displays active .nd files with close (✕) buttons and a (+) button for new files.
 */
public class file_tab_ui_main extends HBox {

    public static class TabItem {
        private String name;
        private File file;
        private HBox node;
        private Label nameLabel;
        private Object userData;

        public TabItem(String name, File file) {
            this.name = name;
            this.file = file;
        }

        public String getName() { return name; }
        public void setName(String name) {
            this.name = name;
            if (nameLabel != null) nameLabel.setText(name);
        }
        public File getFile() { return file; }
        public void setFile(File file) { this.file = file; }
        public Object getUserData() { return userData; }
        public void setUserData(Object userData) { this.userData = userData; }
    }

    private final HBox tabsContainer;
    private final List<TabItem> tabs = new ArrayList<>();
    private TabItem activeTab;
    private final Image fileIcon;

    private Consumer<TabItem> onTabSelected;
    private Consumer<TabItem> onTabClosed;
    private Runnable onNewRequested;

    public file_tab_ui_main() {
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(framework_ui_main.FILE_TAB_HEIGHT);
        setMinHeight(framework_ui_main.FILE_TAB_HEIGHT);
        setMaxHeight(framework_ui_main.FILE_TAB_HEIGHT);
        setPadding(new Insets(0, 8, 0, 8));
        setSpacing(4);
        setStyle("-fx-background-color: " + framework_ui_main.FILE_TAB_BG
            + "; -fx-border-color: " + framework_ui_main.FILE_TAB_BORDER
            + "; -fx-border-width: 0 0 1 0;");

        fileIcon = loadIcon("/icons/file_code.png");
        tabsContainer = new HBox(2);
        tabsContainer.setAlignment(Pos.BOTTOM_LEFT);

        Button newBtn = new Button("+");
        newBtn.setPrefSize(20, 20);
        newBtn.setMinSize(20, 20);
        newBtn.setMaxSize(20, 20);
        newBtn.setTooltip(new javafx.scene.control.Tooltip("New Tab (Ctrl+N)"));
        newBtn.setStyle("-fx-background-color: #E2E8F0; -fx-font-size: 13px; -fx-font-weight: bold; "
            + "-fx-text-fill: #334155; -fx-padding: 0; -fx-cursor: hand; -fx-background-radius: 3; "
            + "-fx-border-color: #CBD5E1; -fx-border-width: 1; -fx-border-radius: 3;");
        newBtn.setOnMouseEntered(e -> newBtn.setStyle("-fx-background-color: #CBD5E1; -fx-font-size: 13px; "
            + "-fx-font-weight: bold; -fx-text-fill: #005A85; -fx-padding: 0; -fx-cursor: hand; "
            + "-fx-background-radius: 3; -fx-border-color: #94A3B8; -fx-border-width: 1; -fx-border-radius: 3;"));
        newBtn.setOnMouseExited(e -> newBtn.setStyle("-fx-background-color: #E2E8F0; -fx-font-size: 13px; "
            + "-fx-font-weight: bold; -fx-text-fill: #334155; -fx-padding: 0; -fx-cursor: hand; "
            + "-fx-background-radius: 3; -fx-border-color: #CBD5E1; -fx-border-width: 1; -fx-border-radius: 3;"));
        newBtn.setOnAction(e -> { if (onNewRequested != null) onNewRequested.run(); });

        getChildren().addAll(tabsContainer, newBtn);
    }

    public TabItem addTab(String name, File file, boolean select) {
        TabItem item = new TabItem(name, file);
        HBox tabNode = new HBox(5);
        tabNode.setAlignment(Pos.CENTER);
        tabNode.setPadding(new Insets(3, 8, 3, 8));
        tabNode.setPrefHeight(framework_ui_main.FILE_TAB_HEIGHT - 2);

        if (fileIcon != null) {
            ImageView iv = new ImageView(fileIcon);
            iv.setFitWidth(13);
            iv.setFitHeight(13);
            tabNode.getChildren().add(iv);
        }

        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;");
        item.nameLabel = nameLbl;

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 9px; -fx-text-fill: #94A3B8; "
            + "-fx-padding: 0 2 0 2; -fx-cursor: hand; -fx-background-radius: 2;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #EF4444; -fx-font-size: 9px; "
            + "-fx-text-fill: #FFFFFF; -fx-padding: 0 2 0 2; -fx-cursor: hand; -fx-background-radius: 2;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 9px; "
            + "-fx-text-fill: #94A3B8; -fx-padding: 0 2 0 2; -fx-cursor: hand; -fx-background-radius: 2;"));
        closeBtn.setOnAction(e -> closeTab(item));

        tabNode.getChildren().addAll(nameLbl, closeBtn);
        tabNode.setOnMouseClicked(e -> selectTab(item));

        item.node = tabNode;
        tabs.add(item);
        tabsContainer.getChildren().add(tabNode);

        if (select || activeTab == null) selectTab(item);
        else renderTabStyles();
        return item;
    }

    public void selectTab(TabItem item) {
        if (item == null || !tabs.contains(item)) return;
        this.activeTab = item;
        renderTabStyles();
        if (onTabSelected != null) onTabSelected.accept(item);
    }

    public void closeTab(TabItem item) {
        if (item == null) return;
        int idx = tabs.indexOf(item);
        if (idx < 0) return;
        tabs.remove(item);
        tabsContainer.getChildren().remove(item.node);
        if (onTabClosed != null) onTabClosed.accept(item);
        if (activeTab == item) {
            if (!tabs.isEmpty()) {
                selectTab(tabs.get(Math.min(idx, tabs.size() - 1)));
            } else {
                activeTab = null;
                if (onNewRequested != null) onNewRequested.run();
            }
        }
    }

    public void updateActiveTab(String name, File file) {
        if (activeTab != null) {
            activeTab.setName(name);
            activeTab.setFile(file);
        }
    }

    private void renderTabStyles() {
        for (TabItem t : tabs) {
            boolean isActive = (t == activeTab);
            if (isActive) {
                t.node.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #005A85 #CBD5E1 transparent #CBD5E1; "
                    + "-fx-border-width: 2 1 0 1; -fx-border-radius: 3 3 0 0; -fx-background-radius: 3 3 0 0;");
                if (t.nameLabel != null)
                    t.nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #005A85;");
            } else {
                t.node.setStyle("-fx-background-color: #E2E8F0; -fx-border-color: #CBD5E1; "
                    + "-fx-border-width: 0 1 1 0; -fx-border-radius: 2 2 0 0; -fx-background-radius: 2 2 0 0; "
                    + "-fx-cursor: hand;");
                if (t.nameLabel != null)
                    t.nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
            }
        }
    }

    private Image loadIcon(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}
        return null;
    }

    public TabItem getActiveTab() { return activeTab; }
    public List<TabItem> getTabs() { return tabs; }

    public void setOnTabSelected(Consumer<TabItem> onTabSelected) { this.onTabSelected = onTabSelected; }
    public void setOnTabClosed(Consumer<TabItem> onTabClosed)     { this.onTabClosed = onTabClosed; }
    public void setOnNewRequested(Runnable onNewRequested)         { this.onNewRequested = onNewRequested; }
}
