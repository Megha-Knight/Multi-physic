package ui.workspace.drafting.shell;

import javafx.scene.Node;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * shape_shell_holder_ui_main.java
 * Container and lifecycle manager for Shell features on a CAD body.
 */
public class shape_shell_holder_ui_main {

    private final List<shell_feature_ui_main> shells = new ArrayList<>();

    public List<shell_feature_ui_main> getShells() {
        return Collections.unmodifiableList(shells);
    }

    public boolean hasShells() {
        return !shells.isEmpty();
    }

    public boolean hasValidShell() {
        for (shell_feature_ui_main s : shells) {
            if (s.isVisible() && s.isValid()) return true;
        }
        return false;
    }

    public void addShell(shell_feature_ui_main s) {
        if (s != null && !shells.contains(s)) shells.add(s);
    }

    public void removeShell(String id) {
        if (id != null) shells.removeIf(s -> id.equals(s.getId()));
    }

    public void clearShells() {
        shells.clear();
    }

    public shell_feature_ui_main getShell(String id) {
        if (id == null) return null;
        for (shell_feature_ui_main s : shells) if (id.equals(s.getId())) return s;
        return null;
    }

    public void revalidate(shape_item_ui_main host) {
        if (host == null) return;
        for (shell_feature_ui_main s : shells) {
            s.revalidate(host);
        }
    }

    public List<Node> buildVisuals(shape_item_ui_main host, boolean selected) {
        List<Node> list = new ArrayList<>();
        if (host == null) return list;
        for (shell_feature_ui_main s : shells) {
            if (!s.isVisible() || !s.isValid()) continue;
            Node n = shell_mesh_builder_ui_main.buildShellNode(host, s, selected);
            if (n != null) list.add(n);
        }
        return list;
    }
}
