package ui.fileexplorer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import ui.framework_ui_main;

import java.io.File;
import java.util.function.Consumer;

/**
 * fileexplorer_ui_main.java
 * Real system File Explorer rooted at Documents/Astra with VS Code styling, deletion, and context menus.
 */
public class fileexplorer_ui_main extends BorderPane {

    private final TreeView<File> treeView;
    private Consumer<String> onPathSelected;
    private Consumer<File> onFileDeleted;

    public fileexplorer_ui_main() {
        getStyleClass().add("file-explorer-pane");
        setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #C9D1D9; -fx-border-width: 0 1 1 0;");

        File astraDir = framework_ui_main.astraDirectory();
        filetreeitem_ui_main rootItem = new filetreeitem_ui_main(astraDir);
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.setStyle("-fx-background-color: #FFFFFF; -fx-font-size: 11.5px;");

        // Header Bar with VS Code style action buttons
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(3, 8, 3, 10));
        header.setPrefHeight(26);
        header.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("EXPLORER");
        titleLabel.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-letter-spacing: 0.5px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = createToolBtn("↻", "Refresh (F5)", this::refresh);
        Button btnDelete  = createToolBtn("🗑", "Delete Selected (Del)", this::deleteSelected);
        Button btnFolder  = createToolBtn("📂", "Open in Explorer", () -> {
            TreeItem<File> s = treeView.getSelectionModel().getSelectedItem();
            file_actions_helper_ui_main.revealInSystemExplorer((s != null && s.getValue() != null) ? s.getValue() : framework_ui_main.astraDirectory());
        });
        HBox actions = new HBox(1, btnRefresh, btnDelete, btnFolder);
        actions.setAlignment(Pos.CENTER_RIGHT);
        header.getChildren().addAll(titleLabel, spacer, actions);
        setTop(header);

        treeView.setCellFactory(tv -> {
            TreeCell<File> cell = new TreeCell<>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null); setGraphic(null); setContextMenu(null);
                    } else {
                        String name = item.getName();
                        if (name == null || name.isEmpty()) name = item.getPath();
                        if (name.endsWith(":\\") || name.endsWith(":/")) name = name.substring(0, name.length() - 2);
                        setText(name);
                        setGraphic(getTreeItem() != null ? getTreeItem().getGraphic() : null);
                        setContextMenu(file_actions_helper_ui_main.createContextMenu(
                            item,
                            () -> { if (onPathSelected != null) onPathSelected.accept(item.getAbsolutePath()); },
                            () -> deleteFile(item),
                            fileexplorer_ui_main.this::refresh
                        ));
                    }
                }
            };
            cell.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !cell.isEmpty() && cell.getItem() != null) {
                    File f = cell.getItem();
                    if (f.isFile() && onPathSelected != null) onPathSelected.accept(f.getAbsolutePath());
                }
            });
            return cell;
        });

        treeView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                deleteSelected(); e.consume();
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && n.getValue() != null && onPathSelected != null) {
                onPathSelected.accept(n.getValue().getAbsolutePath());
            }
        });

        setCenter(treeView);
    }

    public void setOnPathSelected(Consumer<String> onPathSelected) { this.onPathSelected = onPathSelected; }
    public void setOnFileDeleted(Consumer<File> onFileDeleted) { this.onFileDeleted = onFileDeleted; }

    public void deleteSelected() {
        TreeItem<File> s = treeView.getSelectionModel().getSelectedItem();
        if (s != null && s.getValue() != null) deleteFile(s.getValue());
    }

    public void deleteFile(File file) {
        if (file == null) return;
        file_actions_helper_ui_main.deleteWithConfirmation(file, () -> {
            refresh();
            if (onFileDeleted != null) onFileDeleted.accept(file);
        });
    }

    private Button createToolBtn(String text, String tip, Runnable act) {
        Button b = new Button(text);
        b.setTooltip(new Tooltip(tip));
        b.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 1 5 1 5; -fx-font-size: 11px;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #E2E8F0; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 1 5 1 5; -fx-font-size: 11px;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 1 5 1 5; -fx-font-size: 11px;"));
        b.setOnAction(e -> act.run());
        return b;
    }

    public void refresh() {
        TreeItem<File> root = treeView.getRoot();
        if (root instanceof filetreeitem_ui_main item) item.refresh();
    }
    public void refresh(String path) { refresh(); }

    public void navigateTo(String path) {
        if (path == null) return;
        File target = new File(path);
        if (target.isFile()) target = target.getParentFile();
        if (target == null || !target.exists()) return;

        TreeItem<File> root = treeView.getRoot();
        String targetNorm = target.getAbsolutePath().toLowerCase();
        if (root == null || root.getValue() == null || !isAncestorOrSame(root.getValue(), target)) {
            filetreeitem_ui_main newRoot = new filetreeitem_ui_main(target);
            newRoot.setExpanded(true); treeView.setRoot(newRoot);
            treeView.getSelectionModel().select(newRoot); return;
        }

        TreeItem<File> curr = root;
        while (curr != null && !curr.getValue().equals(target)) {
            curr.setExpanded(true);
            TreeItem<File> next = null;
            for (TreeItem<File> child : curr.getChildren()) {
                if (child.getValue() == null) continue;
                String cNorm = child.getValue().getAbsolutePath().toLowerCase();
                if (targetNorm.equals(cNorm) || targetNorm.startsWith(cNorm + File.separator.toLowerCase())) {
                    next = child; break;
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
        String r = root.getAbsolutePath().toLowerCase(), t = target.getAbsolutePath().toLowerCase();
        return t.equals(r) || t.startsWith(r.endsWith(File.separator) ? r : r + File.separator);
    }
}
