package ui.fileexplorer;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

/**
 * filetreeitem_ui_main.java
 * Lazy-loading TreeItem for file system hierarchies with dynamic icon rendering and refresh.
 */
public class filetreeitem_ui_main extends TreeItem<File> {

    private static final Image ICON_FOLDER      = loadIcon("/icons/folder.png");
    private static final Image ICON_FOLDER_OPEN = loadIcon("/icons/folder_open.png");
    private static final Image ICON_FILE        = loadIcon("/icons/file.png");
    private static final Image ICON_FILE_ND     = loadIcon("/icons/file_nd.png");
    private static final Image ICON_FILE_STEP   = loadIcon("/icons/file_step.png");
    private static final Image ICON_FILE_STL    = loadIcon("/icons/file_stl.png");
    private static final Image ICON_FILE_OBJ    = loadIcon("/icons/file_obj.png");
    private static final Image ICON_FILE_NC     = loadIcon("/icons/file_nc.png");
    private static final Image ICON_FILE_CODE   = loadIcon("/icons/file_code.png");
    private static final Image ICON_FILE_PY     = loadIcon("/icons/file_py.png");
    private static final Image ICON_DRIVE       = loadIcon("/icons/drive.png");

    private boolean isFirstChildren = true;
    private boolean isLeaf = false;
    private boolean isFirstLeaf = true;

    public filetreeitem_ui_main(File file) {
        super(file);
        updateGraphic(false);
        expandedProperty().addListener((obs, o, isExp) -> updateGraphic(isExp));
    }

    public void refresh() {
        this.isFirstChildren = true;
        this.isFirstLeaf = true;
        super.getChildren().clear();
        if (isExpanded()) {
            getChildren();
        }
    }

    private void updateGraphic(boolean exp) {
        Image img = getIcon(getValue(), exp);
        if (img != null) setGraphic(new ImageView(img));
    }

    @Override
    public ObservableList<TreeItem<File>> getChildren() {
        if (isFirstChildren) {
            isFirstChildren = false;
            File f = getValue();
            if (f != null && f.isDirectory()) {
                File[] list = f.listFiles();
                if (list != null) {
                    Arrays.sort(list, (a, b) -> a.isDirectory() != b.isDirectory() ?
                        (a.isDirectory() ? -1 : 1) : a.getName().compareToIgnoreCase(b.getName()));
                    for (File child : list) {
                        if (!child.getName().startsWith(".")) {
                            super.getChildren().add(new filetreeitem_ui_main(child));
                        }
                    }
                }
            }
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        if (isFirstLeaf) {
            isFirstLeaf = false;
            File f = getValue();
            isLeaf = (f != null && f.isFile());
        }
        return isLeaf;
    }

    private static Image getIcon(File f, boolean exp) {
        if (f == null || f.getParent() == null) return ICON_DRIVE;
        if (f.isDirectory()) return exp ? ICON_FOLDER_OPEN : ICON_FOLDER;
        String n = f.getName().toLowerCase();
        if (n.endsWith(".nd")) return ICON_FILE_ND != null ? ICON_FILE_ND : ICON_FILE;
        if (n.endsWith(".step") || n.endsWith(".stp") || n.endsWith(".astra")) return ICON_FILE_STEP != null ? ICON_FILE_STEP : ICON_FILE;
        if (n.endsWith(".stl")) return ICON_FILE_STL != null ? ICON_FILE_STL : ICON_FILE;
        if (n.endsWith(".obj")) return ICON_FILE_OBJ != null ? ICON_FILE_OBJ : ICON_FILE;
        if (n.endsWith(".nc")) return ICON_FILE_NC != null ? ICON_FILE_NC : ICON_FILE;
        if (n.endsWith(".py")) return ICON_FILE_PY != null ? ICON_FILE_PY : ICON_FILE_CODE;
        if (n.endsWith(".java") || n.endsWith(".cpp") || n.endsWith(".c") || n.endsWith(".h") || n.endsWith(".json") || n.endsWith(".m")) {
            return ICON_FILE_CODE != null ? ICON_FILE_CODE : ICON_FILE;
        }
        return ICON_FILE;
    }

    private static Image loadIcon(String path) {
        try (InputStream is = filetreeitem_ui_main.class.getResourceAsStream(path)) {
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}
        return null;
    }
}
