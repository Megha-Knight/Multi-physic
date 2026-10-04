package ui.workspace.drafting.booleans;

import javafx.scene.Node;
import ui.workspace.drafting.shape_item_ui_main;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * shape_boolean_holder_ui_main.java
 * Container and lifecycle manager for Boolean solid features on a CAD body.
 */
public class shape_boolean_holder_ui_main {

    private final List<boolean_feature_ui_main> booleans = new ArrayList<>();

    public List<boolean_feature_ui_main> getBooleans() {
        return Collections.unmodifiableList(booleans);
    }

    public boolean hasBooleans() {
        return !booleans.isEmpty();
    }

    public void addBoolean(boolean_feature_ui_main b) {
        if (b != null && !booleans.contains(b)) booleans.add(b);
    }

    public void removeBoolean(String id) {
        if (id != null) booleans.removeIf(b -> id.equals(b.getId()));
    }

    public void clearBooleans() {
        booleans.clear();
    }

    public boolean_feature_ui_main getBoolean(String id) {
        if (id == null) return null;
        for (boolean_feature_ui_main b : booleans) if (id.equals(b.getId())) return b;
        return null;
    }

    public void revalidate(shape_item_ui_main host, Collection<shape_item_ui_main> allShapes) {
        if (host == null) return;
        for (boolean_feature_ui_main b : booleans) {
            shape_item_ui_main tool = findShapeById(b.getToolBodyId(), allShapes);
            b.revalidate(host, tool);
        }
    }

    public List<Node> buildVisuals(shape_item_ui_main host, Collection<shape_item_ui_main> allShapes, boolean selected) {
        List<Node> list = new ArrayList<>();
        if (host == null) return list;
        for (boolean_feature_ui_main b : booleans) {
            if (!b.isVisible() || !b.isValid()) continue;
            shape_item_ui_main tool = findShapeById(b.getToolBodyId(), allShapes);
            if (tool != null) {
                Node n = boolean_mesh_builder_ui_main.buildBooleanNode(host, tool, b, selected);
                if (n != null) list.add(n);
            }
        }
        return list;
    }

    private static shape_item_ui_main findShapeById(String id, Collection<shape_item_ui_main> shapes) {
        if (id == null || shapes == null) return null;
        for (shape_item_ui_main s : shapes) if (s != null && id.equals(s.getId())) return s;
        return null;
    }
}
