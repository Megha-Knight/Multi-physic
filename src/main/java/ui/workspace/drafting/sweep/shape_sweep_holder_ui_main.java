package ui.workspace.drafting.sweep;

import javafx.scene.Node;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * shape_sweep_holder_ui_main.java
 * Container and lifecycle manager for Sweep features on a CAD body.
 */
public class shape_sweep_holder_ui_main {

    private final List<sweep_feature_ui_main> sweeps = new ArrayList<>();

    public List<sweep_feature_ui_main> getSweeps() {
        return Collections.unmodifiableList(sweeps);
    }

    public boolean hasSweeps() {
        return !sweeps.isEmpty();
    }

    public boolean hasValidSweep() {
        for (sweep_feature_ui_main s : sweeps) {
            if (s.isVisible() && s.isValid()) return true;
        }
        return false;
    }

    public void addSweep(sweep_feature_ui_main s) {
        if (s != null && !sweeps.contains(s)) sweeps.add(s);
    }

    public void removeSweep(String id) {
        if (id != null) sweeps.removeIf(s -> id.equals(s.getId()));
    }

    public void clearSweeps() {
        sweeps.clear();
    }

    public sweep_feature_ui_main getSweep(String id) {
        if (id == null) return null;
        for (sweep_feature_ui_main s : sweeps) if (id.equals(s.getId())) return s;
        return null;
    }

    public void revalidate(shape_item_ui_main host) {
        if (host == null) return;
        for (sweep_feature_ui_main s : sweeps) {
            s.revalidate(host);
        }
    }

    public List<Node> buildVisuals(shape_item_ui_main host, boolean selected) {
        List<Node> list = new ArrayList<>();
        if (host == null) return list;
        for (sweep_feature_ui_main s : sweeps) {
            if (!s.isVisible() || !s.isValid()) continue;
            Node n = sweep_mesh_builder_ui_main.buildSweepNode(host, s, selected);
            if (n != null) list.add(n);
        }
        return list;
    }
}
