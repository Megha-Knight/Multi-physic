package ui.breadcrumbbar;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * breadcrumbmenu_ui_main.java
 * Context dropdown menu for breadcrumb folder segments and root drive browsing.
 */
public final class breadcrumbmenu_ui_main {

    private breadcrumbmenu_ui_main() {}

    public static void showSubfolderMenu(File folder, Node anchor, Consumer<String> onNavigate) {
        ContextMenu menu = new ContextMenu();
        if (folder.getParent() == null) {
            File[] roots = File.listRoots();
            if (roots != null) {
                for (File r : roots) {
                    String d = r.getPath();
                    if (d.endsWith(":\\") || d.endsWith(":/")) d = d.substring(0, d.length() - 2);
                    MenuItem mi = new MenuItem("🖴 " + d);
                    mi.setOnAction(e -> onNavigate.accept(r.getAbsolutePath()));
                    menu.getItems().add(mi);
                }
            }
        }
        File[] subs = folder.listFiles(File::isDirectory);
        if (subs != null && subs.length > 0) {
            Arrays.sort(subs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File sub : subs) {
                if (!sub.getName().startsWith(".")) {
                    MenuItem mi = new MenuItem("📁 " + sub.getName());
                    mi.setOnAction(e -> onNavigate.accept(sub.getAbsolutePath()));
                    menu.getItems().add(mi);
                }
            }
        }
        if (!menu.getItems().isEmpty()) {
            menu.show(anchor, Side.BOTTOM, 0, 0);
        }
    }
}
