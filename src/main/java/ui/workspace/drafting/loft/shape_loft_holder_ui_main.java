package ui.workspace.drafting.loft;

import javafx.scene.Node;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * shape_loft_holder_ui_main.java
 * Container and lifecycle manager for Loft features on a CAD body.
 */
public class shape_loft_holder_ui_main {

    private final List<loft_feature_ui_main> lofts = new ArrayList<>();

    public List<loft_feature_ui_main> getLofts() {
        return Collections.unmodifiableList(lofts);
    }

    public boolean hasLofts() {
        return !lofts.isEmpty();
    }

    public boolean hasValidLoft() {
        for (loft_feature_ui_main l : lofts) {
            if (l.isVisible() && l.isValid()) return true;
        }
        return false;
    }

    public void addLoft(loft_feature_ui_main l) {
        if (l != null && !lofts.contains(l)) lofts.add(l);
    }

    public void removeLoft(String id) {
        if (id != null) lofts.removeIf(l -> id.equals(l.getId()));
    }

    public void clearLofts() {
        lofts.clear();
    }

    public loft_feature_ui_main getLoft(String id) {
        if (id == null) return null;
        for (loft_feature_ui_main l : lofts) if (id.equals(l.getId())) return l;
        return null;
    }

    public void revalidate(shape_item_ui_main host) {
        if (host == null) return;
        for (loft_feature_ui_main l : lofts) {
            l.revalidate(host);
        }
    }

    public List<Node> buildVisuals(shape_item_ui_main host, boolean selected) {
        List<Node> list = new ArrayList<>();
        if (host == null) return list;
        for (loft_feature_ui_main l : lofts) {
            if (!l.isVisible() || !l.isValid()) continue;
            Node n = loft_mesh_builder_ui_main.buildLoftNode(host, l, selected);
            if (n != null) list.add(n);
        }
        return list;
    }
}
