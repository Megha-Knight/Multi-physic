package ui;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import ui.breadcrumbbar.breadcrumb_ui_main;
import ui.footerbar.footer_ui_main;
import ui.workspace.documentserializer_ui_main;
import ui.workspace.filetab_ui_main;
import ui.workspace.shapeitem_ui_main;
import ui.workspace.workspace_ui_main;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * documenthandler_ui_main.java
 * Handles document creation, opening, saving (.nd), and path synchronization.
 * Roots new and saved documents inside Documents/Astra.
 */
public class documenthandler_ui_main {

    private final filetab_ui_main documentTabBar;
    private final footer_ui_main footerBar;
    private final breadcrumb_ui_main breadcrumbBar;
    private final workspace_ui_main workspace;
    private final Supplier<Window> windowSupplier;
    private Runnable onFileChanged;

    public documenthandler_ui_main(filetab_ui_main tabBar, footer_ui_main footer,
                                   breadcrumb_ui_main breadcrumb, workspace_ui_main workspace,
                                   Supplier<Window> winSupplier) {
        this.documentTabBar = tabBar;
        this.footerBar = footer;
        this.breadcrumbBar = breadcrumb;
        this.workspace = workspace;
        this.windowSupplier = winSupplier;
    }

    public void setOnFileChanged(Runnable onFileChanged) {
        this.onFileChanged = onFileChanged;
    }

    public void onNewFile() {
        File astraDir = framework_ui_main.astraDirectory();
        int count = 1;
        String name;
        while (true) {
            name = "untitled_" + count + ".nd";
            File candidate = new File(astraDir, name);
            boolean open = false;
            for (filetab_ui_main.TabItem t : documentTabBar.getTabs()) {
                if (name.equalsIgnoreCase(t.getName())) { open = true; break; }
            }
            if (!candidate.exists() && !open) break;
            count++;
        }

        filetab_ui_main.TabItem tab = documentTabBar.addTab(name, null, true);
        tab.setUserData(Collections.emptyList());
        workspace.getShapeEditor().loadShapes(Collections.emptyList());
        workspace.getShapeEditor().clearHistory();
        footerBar.setOpenedFilePath(new File(astraDir, name).getAbsolutePath() + " [Unsaved]");
        breadcrumbBar.setFolderPath(astraDir.getAbsolutePath());
        footerBar.setStatusText("New document: " + name + " (Unsaved)");
    }

    public void onSaveFile() {
        savecommands_ui_main.executeSave(
            documentTabBar.getActiveTab(), workspace.getShapeEditor().getShapes(),
            footerBar, this::onSaveAsFile, this::notifyChange
        );
    }

    public void onSaveAsFile() {
        savecommands_ui_main.executeSaveAs(
            documentTabBar.getActiveTab(), workspace.getShapeEditor().getShapes(),
            windowSupplier.get(), documentTabBar, footerBar, breadcrumbBar, this::notifyChange
        );
    }

    public void onSaveRootFile() {
        savecommands_ui_main.executeSaveRoot(
            documentTabBar.getActiveTab(), workspace.getShapeEditor().getShapes(),
            documentTabBar, footerBar, breadcrumbBar, this::notifyChange
        );
    }

    public void onSaveInFile() {
        savecommands_ui_main.executeSaveIn(
            documentTabBar.getActiveTab(), workspace.getShapeEditor().getShapes(),
            windowSupplier.get(), documentTabBar, footerBar, breadcrumbBar, this::notifyChange
        );
    }

    public void openFile(File f) {
        if (f == null || !f.exists()) return;
        for (filetab_ui_main.TabItem t : documentTabBar.getTabs()) {
            if (t.getFile() != null && t.getFile().getAbsolutePath().equalsIgnoreCase(f.getAbsolutePath())) {
                documentTabBar.selectTab(t);
                return;
            }
        }
        List<shapeitem_ui_main> shapes = documentserializer_ui_main.loadFromFile(f);
        filetab_ui_main.TabItem tab = documentTabBar.addTab(f.getName(), f, true);
        tab.setUserData(shapes);
        workspace.getShapeEditor().loadShapes(shapes);
        workspace.getShapeEditor().clearHistory();
        footerBar.setOpenedFilePath(f.getAbsolutePath());
        breadcrumbBar.setFolderPath(f.getAbsolutePath());
        footerBar.setStatusText("Opened: " + f.getName());
        notifyChange();
    }

    public void onOpenFile() {
        FileChooser ch = new FileChooser();
        ch.setTitle("Open Project or Script");
        File astraDir = framework_ui_main.astraDirectory();
        ch.setInitialDirectory(astraDir.exists() ? astraDir : new File(System.getProperty("user.home")));
        ch.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Supported Files", "*.*", "*.nd", "*.astra", "*.nc", "*.step", "*.stl"),
            new FileChooser.ExtensionFilter("Neural Dynamics (*.nd)", "*.nd"),
            new FileChooser.ExtensionFilter("Legacy Astra (*.nc)", "*.nc")
        );
        File f = ch.showOpenDialog(windowSupplier.get());
        if (f != null) {
            openFile(f);
        }
    }

    private void notifyChange() {
        if (onFileChanged != null) onFileChanged.run();
    }
}
