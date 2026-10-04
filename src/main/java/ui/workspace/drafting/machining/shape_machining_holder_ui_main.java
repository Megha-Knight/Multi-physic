package ui.workspace.drafting.machining;

import javafx.scene.Node;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * shape_machining_holder_ui_main.java
 * Container and lifecycle coordinator for Chamfer, Fillet, and Draft features on a CAD body.
 */
public class shape_machining_holder_ui_main {

    private final List<chamfer_feature_ui_main> chamfers = new ArrayList<>();
    private final List<fillet_feature_ui_main> fillets = new ArrayList<>();
    private final List<draft_feature_ui_main> drafts = new ArrayList<>();

    public List<chamfer_feature_ui_main> getChamfers() { return Collections.unmodifiableList(chamfers); }
    public void addChamfer(chamfer_feature_ui_main c) { if (c != null && !chamfers.contains(c)) chamfers.add(c); }
    public void removeChamfer(String id) { chamfers.removeIf(c -> c.getId().equals(id)); }
    public void clearChamfers() { chamfers.clear(); }
    public chamfer_feature_ui_main getChamfer(String id) { for (var c : chamfers) if (c.getId().equals(id)) return c; return null; }

    public List<fillet_feature_ui_main> getFillets() { return Collections.unmodifiableList(fillets); }
    public void addFillet(fillet_feature_ui_main f) { if (f != null && !fillets.contains(f)) fillets.add(f); }
    public void removeFillet(String id) { fillets.removeIf(f -> f.getId().equals(id)); }
    public void clearFillets() { fillets.clear(); }
    public fillet_feature_ui_main getFillet(String id) { for (var f : fillets) if (f.getId().equals(id)) return f; return null; }

    public List<draft_feature_ui_main> getDrafts() { return Collections.unmodifiableList(drafts); }
    public void addDraft(draft_feature_ui_main d) { if (d != null && !drafts.contains(d)) drafts.add(d); }
    public void removeDraft(String id) { drafts.removeIf(d -> d.getId().equals(id)); }
    public void clearDrafts() { drafts.clear(); }
    public draft_feature_ui_main getDraft(String id) { for (var d : drafts) if (d.getId().equals(id)) return d; return null; }

    public boolean isEmpty() { return chamfers.isEmpty() && fillets.isEmpty() && drafts.isEmpty(); }

    public void revalidate(shape_item_ui_main host) {
        for (var c : chamfers) c.revalidate(host);
        for (var f : fillets) f.revalidate(host);
        for (var d : drafts) d.revalidate(host);
    }

    public List<Node> buildVisuals(shape_item_ui_main host, boolean isSelected) {
        List<Node> list = new ArrayList<>();
        for (var c : chamfers) {
            if (c.isVisible() && c.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                Node n = chamfer_mesh_builder_ui_main.buildChamferNode(host, c, isSelected);
                if (n != null) list.add(n);
            }
        }
        for (var f : fillets) {
            if (f.isVisible() && f.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                Node n = fillet_mesh_builder_ui_main.buildFilletNode(host, f, isSelected);
                if (n != null) list.add(n);
            }
        }
        for (var d : drafts) {
            if (d.isVisible() && d.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                Node n = draft_mesh_builder_ui_main.buildDraftNode(host, d, isSelected);
                if (n != null) list.add(n);
            }
        }
        return list;
    }
}
