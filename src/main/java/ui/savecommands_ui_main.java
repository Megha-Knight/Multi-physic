package ui;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ui.breadcrumbbar.breadcrumb_ui_main;
import ui.footerbar.footer_ui_main;
import ui.workspace.document.document_serializer_ui_main;
import ui.workspace.document.file_tab_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.io.File;
import java.util.List;

/**
 * savecommands_ui_main.java
 * Standardized document save workflows: Save, Save As, Save Root, and Save In.
 * Guarantees safe format validation and prevents invalid STEP file generation.
 */
public final class savecommands_ui_main {

    private savecommands_ui_main() {}

    public static void executeSave(file_tab_ui_main.TabItem active, List<shape_item_ui_main> shapes,
                                   footer_ui_main footer, Runnable onSaveAs, Runnable notifyChange) {
        if (active == null) return;
        if (active.getFile() == null) {
            if (onSaveAs != null) onSaveAs.run();
            return;
        }
        if (document_serializer_ui_main.saveToFile(active.getFile(), shapes)) {
            active.setUserData(shapes);
            footer.setStatusText("Saved: " + active.getName());
            if (notifyChange != null) notifyChange.run();
        } else {
            footer.setStatusText("Save failed: " + active.getName());
        }
    }

    public static void executeSaveAs(file_tab_ui_main.TabItem active, List<shape_item_ui_main> shapes,
                                     Window window, file_tab_ui_main tabBar, footer_ui_main footer,
                                     breadcrumb_ui_main breadcrumb, Runnable notifyChange) {
        if (active == null) return;
        FileChooser ch = new FileChooser();
        ch.setTitle("Save File As");
        File initialDir = active.getFile() != null ? active.getFile().getParentFile() : framework_ui_main.astraDirectory();
        ch.setInitialDirectory((initialDir != null && initialDir.exists()) ? initialDir : framework_ui_main.astraDirectory());
        ch.setInitialFileName(active.getName());

        FileChooser.ExtensionFilter extNd = new FileChooser.ExtensionFilter("Multiphysics Document (*.nd)", "*.nd");
        FileChooser.ExtensionFilter extStep = new FileChooser.ExtensionFilter("STEP CAD File (*.step)", "*.step");
        FileChooser.ExtensionFilter extStp = new FileChooser.ExtensionFilter("STEP CAD File (*.stp)", "*.stp");
        FileChooser.ExtensionFilter extStl = new FileChooser.ExtensionFilter("Stereolithography Mesh (*.stl)", "*.stl");
        FileChooser.ExtensionFilter extObj = new FileChooser.ExtensionFilter("Wavefront 3D Object (*.obj)", "*.obj");
        FileChooser.ExtensionFilter extNc = new FileChooser.ExtensionFilter("CNC G-Code / Legacy (*.nc)", "*.nc");
        ch.getExtensionFilters().addAll(extNd, extStep, extStp, extStl, extObj, extNc);
        ch.setSelectedExtensionFilter(extNd);

        File f = ch.showSaveDialog(window);
        if (f == null) return;

        String lower = f.getName().toLowerCase();
        if (!hasKnownExtension(lower)) {
            f = new File(f.getParentFile(), f.getName() + getFilterExtension(ch.getSelectedExtensionFilter()));
        }

        if (document_serializer_ui_main.saveToFile(f, shapes)) {
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

    public static void executeSaveRoot(file_tab_ui_main.TabItem active, List<shape_item_ui_main> shapes,
                                       file_tab_ui_main tabBar, footer_ui_main footer,
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

        if (document_serializer_ui_main.saveToFile(target, shapes)) {
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

    public static void executeSaveIn(file_tab_ui_main.TabItem active, List<shape_item_ui_main> shapes,
                                     Window window, file_tab_ui_main tabBar, footer_ui_main footer,
                                     breadcrumb_ui_main breadcrumb, Runnable notifyChange) {
        if (active == null) return;
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Destination Folder (Save In)");
        File initialDir = active.getFile() != null ? active.getFile().getParentFile() : framework_ui_main.astraDirectory();
        dc.setInitialDirectory((initialDir != null && initialDir.exists()) ? initialDir : framework_ui_main.astraDirectory());

        File dir = dc.showDialog(window);
        if (dir == null || !dir.exists()) return;

        String name = active.getName();
        if (!hasKnownExtension(name.toLowerCase())) name += ".nd";
        File target = new File(dir, name);

        if (document_serializer_ui_main.saveToFile(target, shapes)) {
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

    private static boolean hasKnownExtension(String name) {
        if (name == null) return false;
        String l = name.toLowerCase();
        return l.endsWith(".nd") || l.endsWith(".step") || l.endsWith(".stp") || l.endsWith(".stl") || l.endsWith(".obj") || l.endsWith(".nc");
    }

    private static String getFilterExtension(FileChooser.ExtensionFilter filter) {
        if (filter != null && !filter.getExtensions().isEmpty()) {
            String ext = filter.getExtensions().get(0);
            if (ext.startsWith("*.")) return ext.substring(1);
        }
        return ".nd";
    }
}
