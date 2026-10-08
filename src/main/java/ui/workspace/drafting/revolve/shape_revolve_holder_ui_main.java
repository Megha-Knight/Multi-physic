package ui.workspace.drafting.revolve;

import javafx.scene.Node;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * shape_revolve_holder_ui_main.java
 * Container and lifecycle manager for Revolve features associated with a CAD shape.
 */
public class shape_revolve_holder_ui_main {

    private final List<revolve_feature_ui_main> revolves = new ArrayList<>();

    public List<revolve_feature_ui_main> getRevolves() {
        return Collections.unmodifiableList(revolves);
    }

    public boolean hasRevolves() {
        return !revolves.isEmpty();
    }

    public boolean hasValidRevolve() {
        for (revolve_feature_ui_main r : revolves) {
            if (r.isValid() && r.isVisible()) return true;
        }
        return false;
    }

    public void addRevolve(revolve_feature_ui_main r) {
        if (r != null) revolves.add(r);
    }

    public void removeRevolve(String id) {
        if (id != null) revolves.removeIf(r -> r.getId().equals(id));
    }

    public void clearRevolves() {
        revolves.clear();
    }

    public revolve_feature_ui_main getRevolve(String id) {
        if (id == null) return null;
        for (revolve_feature_ui_main r : revolves) {
            if (r.getId().equals(id)) return r;
        }
        return null;
    }

    public void revalidate(shape_item_ui_main host) {
        for (revolve_feature_ui_main r : revolves) {
            r.revalidate(host);
        }
    }

    public List<Node> buildVisuals(shape_item_ui_main host, boolean isShapeSelected) {
        List<Node> list = new ArrayList<>();
        for (revolve_feature_ui_main r : revolves) {
            if (!r.isVisible() || !r.isValid()) continue;
            Node node = revolve_mesh_builder_ui_main.buildRevolveNode(host, r, isShapeSelected);
            if (node != null) list.add(node);
        }
        return list;
    }
}
