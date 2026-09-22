package ui;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ui.breadcrumbbar.breadcrumb_ui_main;
import ui.footerbar.footer_ui_main;
import ui.workspace.documentserializer_ui_main;
import ui.workspace.filetab_ui_main;
import ui.workspace.shapeitem_ui_main;

import java.io.File;
import java.util.List;

/**
 * savecommands_ui_main.java
 * Standardized document save workflows: Save, Save As, Save Root, and Save In.
 * Guarantees safe format validation and prevents invalid STEP file generation.
 */
public final class savecommands_ui_main {

    private savecommands_ui_main() {}

    public static void executeSave(filetab_ui_main.TabItem active, List<shapeitem_ui_main> shapes,
                                   footer_ui_main footer, Runnable onSaveAs, Runnable notifyChange) {
        if (active == null) return;
        if (active.getFile() == null) {
            if (onSaveAs != null) onSaveAs.run();
            return;
        }
        if (documentserializer_ui_main.saveToFile(active.getFile(), shapes)) {
            active.setUserData(shapes);
            footer.setStatusText("Saved: " + active.getName());
            if (notifyChange != null) notifyChange.run();
        } else {
            footer.setStatusText("Save failed: " + active.getName());
        }
    }

    public static void executeSaveAs(filetab_ui_main.TabItem active, List<shapeitem_ui_main> shapes,
                                     Window window, filetab_ui_main tabBar, footer_ui_main footer,
                                     breadcrumb_ui_main breadcrumb, Runnable notifyChange) {
        if (active == null) return;
        FileChooser ch = new FileChooser();
        ch.setTitle("Save File As");
        File initialDir = active.getFile() != null ? active.getFile().getParentFile() : framework_ui_main.astraDirectory();
        ch.setInitialDirectory((initialDir != null && initialDir.exists()) ? initialDir : framework_ui_main.astraDirectory());
        ch.setInitialFileName(active.getName());

        FileChooser.ExtensionFilter extNd = new FileChooser.ExtensionFilter("Astra Document (*.nd)", "*.nd");
        FileChooser.ExtensionFilter extNc = new FileChooser.ExtensionFilter("Legacy Astra (*.nc)", "*.nc");
        FileChooser.ExtensionFilter extStep = new FileChooser.ExtensionFilter("STEP File (*.step)", "*.step");
        FileChooser.ExtensionFilter extStp = new FileChooser.ExtensionFilter("STEP File (*.stp)", "*.stp");
        ch.getExtensionFilters().addAll(extNd, extNc, extStep, extStp);
        ch.setSelectedExtensionFilter(extNd);

        File f = ch.showSaveDialog(window);
        if (f == null) return;

        String lower = f.getName().toLowerCase();
        FileChooser.ExtensionFilter chosenExt = ch.getSelectedExtensionFilter();
        boolean isStep = lower.endsWith(".step") || lower.endsWith(".stp") ||
                         (chosenExt != null && (chosenExt.getExtensions().contains("*.step") || chosenExt.getExtensions().contains("*.stp")));

        if (isStep) {
            footer.setStatusText("STEP export is not available yet.");
            return;
        }

        if (!lower.endsWith(".nd") && !lower.endsWith(".nc")) {
            f = new File(f.getParentFile(), f.getName() + ".nd");
        }

        if (documentserializer_ui_main.saveToFile(f, shapes)) {
            active.setUserData(shapes);
            tabBar.updateActiveTab(f.getName(), f);
            footer.setOpenedFilePath(f.getAbsolutePath());
            breadcrumb.setFolderPath(f.getAbsolutePath());
            footer.setStatusText("Saved: " + f.getName());
            if (notifyChange != null) notifyChange.run();
        } else {
            footer.setStatusText("Save failed: " + f.getName());
        }
    }

    public static void executeSaveRoot(filetab_ui_main.TabItem active, List<shapeitem_ui_main> shapes,
                                       filetab_ui_main tabBar, footer_ui_main footer,
                                       breadcrumb_ui_main breadcrumb, Runnable notifyChange) {
        if (active == null) return;
        File astraDir = framework_ui_main.astraDirectory();
        File target;

        if (active.getFile() != null && active.getFile().getParentFile() != null &&
            active.getFile().getParentFile().getAbsolutePath().equalsIgnoreCase(astraDir.getAbsolutePath())) {
            target = active.getFile();
        } else {
            int count = 1;
            String name;
            while (true) {
                name = "untitled_" + count + ".nd";
                File candidate = new File(astraDir, name);
                if (!candidate.exists()) {
                    target = candidate;
                    break;
                }
                count++;
            }
        }

        if (documentserializer_ui_main.saveToFile(target, shapes)) {
            active.setUserData(shapes);
            tabBar.updateActiveTab(target.getName(), target);
            footer.setOpenedFilePath(target.getAbsolutePath());
            breadcrumb.setFolderPath(target.getAbsolutePath());
            footer.setStatusText("Saved to root: " + target.getName());
            if (notifyChange != null) notifyChange.run();
        } else {
            footer.setStatusText("Save Root failed: " + target.getName());
        }
    }

    public static void executeSaveIn(filetab_ui_main.TabItem active, List<shapeitem_ui_main> shapes,
                                     Window window, filetab_ui_main tabBar, footer_ui_main footer,
                                     breadcrumb_ui_main breadcrumb, Runnable notifyChange) {
        if (active == null) return;
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Destination Folder (Save In)");
        File initialDir = active.getFile() != null ? active.getFile().getParentFile() : framework_ui_main.astraDirectory();
        dc.setInitialDirectory((initialDir != null && initialDir.exists()) ? initialDir : framework_ui_main.astraDirectory());

        File dir = dc.showDialog(window);
        if (dir == null || !dir.exists()) return;

        String name = active.getName();
        if (!name.toLowerCase().endsWith(".nd") && !name.toLowerCase().endsWith(".nc")) {
            name += ".nd";
        }
        File target = new File(dir, name);

        if (documentserializer_ui_main.saveToFile(target, shapes)) {
            active.setUserData(shapes);
            tabBar.updateActiveTab(target.getName(), target);
            footer.setOpenedFilePath(target.getAbsolutePath());
            breadcrumb.setFolderPath(target.getAbsolutePath());
            footer.setStatusText("Saved in: " + target.getAbsolutePath());
            if (notifyChange != null) notifyChange.run();
        } else {
            footer.setStatusText("Save In failed: " + target.getName());
        }
    }
}
