package ui.fileexplorer;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;

/**
 * file_actions_helper_ui_main.java
 * Safe file deletion, explorer context menus, and system integration for Astra File Explorer.
 */
public final class file_actions_helper_ui_main {

    private static final Image ICON_TRASH = loadIcon("/icons/trash.png");
    private static final Image ICON_REFRESH = loadIcon("/icons/refresh.png");

    private file_actions_helper_ui_main() {}

    public static boolean deleteWithConfirmation(File file, Runnable onSuccess) {
        if (file == null || !file.exists()) return false;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete " + (file.isDirectory() ? "Folder" : "File"));
        alert.setHeaderText("Delete \"" + file.getName() + "\"?");
        alert.setContentText("Are you sure you want to delete this "
            + (file.isDirectory() ? "folder and all its contents? This cannot be undone." : "file? This cannot be undone."));

        ButtonType btnDelete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnDelete, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnDelete) {
            boolean deleted = deleteRecursively(file);
            if (deleted && onSuccess != null) onSuccess.run();
            return deleted;
        }
        return false;
    }

    public static boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) return false;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        return file.delete();
    }

    public static void revealInSystemExplorer(File file) {
        if (file == null || !file.exists()) return;
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
            } else if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file.isDirectory() ? file : file.getParentFile());
            }
        } catch (Exception ignored) {}
    }

    public static ContextMenu createContextMenu(File file, Runnable onOpen, Runnable onDelete, Runnable onRefresh) {
        ContextMenu menu = new ContextMenu();
        if (file == null) return menu;

        if (file.isFile() && onOpen != null) {
            MenuItem openItem = new MenuItem("Open");
            openItem.setOnAction(e -> onOpen.run());
            menu.getItems().add(openItem);
            menu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem deleteItem = new MenuItem("Delete");
        if (ICON_TRASH != null) deleteItem.setGraphic(new ImageView(ICON_TRASH));
        deleteItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        deleteItem.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        deleteItem.setOnAction(e -> {
            if (onDelete != null) onDelete.run();
        });
        menu.getItems().add(deleteItem);

        MenuItem revealItem = new MenuItem("Reveal in File Explorer");
        revealItem.setOnAction(e -> revealInSystemExplorer(file));
        menu.getItems().add(revealItem);

        if (onRefresh != null) {
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem refreshItem = new MenuItem("Refresh");
            if (ICON_REFRESH != null) refreshItem.setGraphic(new ImageView(ICON_REFRESH));
            refreshItem.setOnAction(e -> onRefresh.run());
            menu.getItems().add(refreshItem);
        }

        return menu;
    }

    private static Image loadIcon(String path) {
        try (InputStream is = file_actions_helper_ui_main.class.getResourceAsStream(path)) {
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}
        return null;
    }
}
