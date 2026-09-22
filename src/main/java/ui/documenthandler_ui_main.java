package ui;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import ui.breadcrumbbar.breadcrumb_ui_main;
import ui.footerbar.footer_ui_main;
import ui.workspace.filetab_ui_main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private final Supplier<Window> windowSupplier;
    private Runnable onFileChanged;

    public documenthandler_ui_main(filetab_ui_main tabBar, footer_ui_main footer,
                                   breadcrumb_ui_main breadcrumb, Supplier<Window> winSupplier) {
        this.documentTabBar = tabBar;
        this.footerBar = footer;
        this.breadcrumbBar = breadcrumb;
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

        documentTabBar.addTab(name, null, true);
        footerBar.setOpenedFilePath(new File(astraDir, name).getAbsolutePath() + " [Unsaved]");
        breadcrumbBar.setFolderPath(astraDir.getAbsolutePath());
        footerBar.setStatusText("New document: " + name + " (Unsaved)");
    }

    public void onSaveFile() {
        filetab_ui_main.TabItem active = documentTabBar.getActiveTab();
        if (active == null) return;
        if (active.getFile() == null) {
            onSaveAsFile();
            return;
        }
        if (writeNdFile(active.getFile())) {
            footerBar.setStatusText("Saved: " + active.getName());
            notifyChange();
        } else {
            footerBar.setStatusText("Save failed: " + active.getName());
        }
    }

    public void onSaveAsFile() {
        filetab_ui_main.TabItem active = documentTabBar.getActiveTab();
        if (active == null) return;
        FileChooser ch = new FileChooser();
        ch.setTitle("Save File As (.nd)");
        File initialDir = active.getFile() != null ? active.getFile().getParentFile() : framework_ui_main.astraDirectory();
        ch.setInitialDirectory((initialDir != null && initialDir.exists()) ? initialDir : framework_ui_main.astraDirectory());
        ch.setInitialFileName(active.getName());
        ch.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Neural Dynamics (*.nd)", "*.nd"),
            new FileChooser.ExtensionFilter("Legacy Astra (*.nc)", "*.nc")
        );
        File f = ch.showSaveDialog(windowSupplier.get());
        if (f != null) {
            String nameLower = f.getName().toLowerCase();
            if (!nameLower.endsWith(".nd") && !nameLower.endsWith(".nc")) {
                f = new File(f.getParentFile(), f.getName() + ".nd");
            }
            if (writeNdFile(f)) {
                documentTabBar.updateActiveTab(f.getName(), f);
                footerBar.setOpenedFilePath(f.getAbsolutePath());
                breadcrumbBar.setFolderPath(f.getAbsolutePath());
                footerBar.setStatusText("Saved: " + f.getName());
                notifyChange();
            } else {
                footerBar.setStatusText("Save failed: " + f.getName());
            }
        }
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
            documentTabBar.addTab(f.getName(), f, true);
            footerBar.setOpenedFilePath(f.getAbsolutePath());
            breadcrumbBar.setFolderPath(f.getAbsolutePath());
            footerBar.setStatusText("Opened: " + f.getName());
            notifyChange();
        }
    }

    private void notifyChange() {
        if (onFileChanged != null) onFileChanged.run();
    }

    private boolean writeNdFile(File f) {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("/* Astra Multi-Physics Model */\n");
            return true;
        } catch (IOException e) {
            System.err.println("[Astra] Error writing to " + f.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }
}
