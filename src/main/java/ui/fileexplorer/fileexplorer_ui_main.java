package ui.fileexplorer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import ui.framework_ui_main;

import java.io.File;
import java.util.function.Consumer;

/**
 * fileexplorer_ui_main.java
 * Real system File Explorer rooted at Documents/Astra.
 * Features expandable tree nodes, CAD icons, dynamic refresh, and breadcrumb synchronization.
 */
public class fileexplorer_ui_main extends BorderPane {

    private final TreeView<File> treeView;
    private Consumer<String> onPathSelected;

    public fileexplorer_ui_main() {
        getStyleClass().add("file-explorer-pane");
        setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #C9D1D9; -fx-border-width: 0 1 1 0;");

        // Header Bar
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 10, 4, 10));
        header.setPrefHeight(26);
        header.setStyle("-fx-background-color: #F1F5F9; -fx-border-color: #CBD5E1; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("File Explorer");
        titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1F2933;");
        header.getChildren().add(titleLabel);
        setTop(header);

        // Dedicated Astra Workspace directory as sole tree root
        File astraDir = framework_ui_main.astraDirectory();
        filetreeitem_ui_main rootItem = new filetreeitem_ui_main(astraDir);
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.setStyle("-fx-background-color: #FFFFFF; -fx-font-size: 11.5px;");

        treeView.setCellFactory(tv -> new TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String name = item.getName();
                    if (name == null || name.isEmpty()) name = item.getPath();
                    if (name.endsWith(":\\") || name.endsWith(":/")) name = name.substring(0, name.length() - 2);
                    setText(name);
                    setGraphic(getTreeItem() != null ? getTreeItem().getGraphic() : null);
                }
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && n.getValue() != null && onPathSelected != null) {
                onPathSelected.accept(n.getValue().getAbsolutePath());
            }
        });

        setCenter(treeView);
    }

    public void setOnPathSelected(Consumer<String> onPathSelected) {
        this.onPathSelected = onPathSelected;
    }

    public void refresh() {
        TreeItem<File> root = treeView.getRoot();
        if (root instanceof filetreeitem_ui_main item) {
            item.refresh();
        }
    }

    public void refresh(String path) {
        refresh();
    }

    public void navigateTo(String path) {
        if (path == null) return;
        File target = new File(path);
        if (target.isFile()) target = target.getParentFile();
        if (target == null || !target.exists()) return;

        TreeItem<File> root = treeView.getRoot();
        String targetNorm = target.getAbsolutePath().toLowerCase();

        if (root == null || root.getValue() == null || !isAncestorOrSame(root.getValue(), target)) {
            filetreeitem_ui_main newRoot = new filetreeitem_ui_main(target);
            newRoot.setExpanded(true);
            treeView.setRoot(newRoot);
            treeView.getSelectionModel().select(newRoot);
            return;
        }

        TreeItem<File> curr = root;
        while (curr != null && !curr.getValue().equals(target)) {
            curr.setExpanded(true);
            TreeItem<File> next = null;
            for (TreeItem<File> child : curr.getChildren()) {
                if (child.getValue() == null) continue;
                String cNorm = child.getValue().getAbsolutePath().toLowerCase();
                if (targetNorm.equals(cNorm) || targetNorm.startsWith(cNorm + File.separator.toLowerCase())) {
                    next = child;
                    break;
                }
            }
            curr = next;
        }

        if (curr != null) {
            treeView.getSelectionModel().select(curr);
            treeView.scrollTo(treeView.getRow(curr));
        }
    }

    private boolean isAncestorOrSame(File root, File target) {
        String r = root.getAbsolutePath().toLowerCase();
        String t = target.getAbsolutePath().toLowerCase();
        return t.equals(r) || t.startsWith(r.endsWith(File.separator) ? r : r + File.separator);
    }
}
