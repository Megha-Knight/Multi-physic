package ui.ribbonbar;

import javafx.scene.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * dynamic_panel_coordinator_ui_main.java
 * Centralized coordinator ensuring mutual exclusion across dynamic ribbon panels.
 * At most one dynamic panel can be open/visible at any given time.
 */
public class dynamic_panel_coordinator_ui_main {

    private final List<dynamic_panel_entry_ui_main> panels = new ArrayList<>();
    private dynamic_panel_entry_ui_main activePanel = null;

    public void register(dynamic_panel_entry_ui_main panel) {
        if (panel != null && !panels.contains(panel)) {
            panels.add(panel);
            syncNodeState(panel, panel.isPanelVisible());
        }
    }

    public void unregister(dynamic_panel_entry_ui_main panel) {
        if (panel != null) {
            if (activePanel == panel) activePanel = null;
            panels.remove(panel);
        }
    }

    public void openExclusive(dynamic_panel_entry_ui_main panel) {
        if (panel == null) {
            closeAll();
            return;
        }
        for (dynamic_panel_entry_ui_main p : panels) {
            if (p != panel && p.isPanelVisible()) {
                p.hide();
                syncNodeState(p, false);
            }
        }
        panel.show();
        syncNodeState(panel, true);
        activePanel = panel;
    }

    public void close(dynamic_panel_entry_ui_main panel) {
        if (panel != null && panel.isPanelVisible()) {
            panel.hide();
            syncNodeState(panel, false);
            if (activePanel == panel) activePanel = null;
        }
    }

    public void toggle(dynamic_panel_entry_ui_main panel) {
        if (panel == null) return;
        if (panel.isPanelVisible()) {
            close(panel);
        } else {
            openExclusive(panel);
        }
    }

    public void closeAll() {
        for (dynamic_panel_entry_ui_main p : panels) {
            if (p.isPanelVisible()) {
                p.hide();
                syncNodeState(p, false);
            }
        }
        activePanel = null;
    }

    public boolean isOpen(dynamic_panel_entry_ui_main panel) {
        return panel != null && panel.isPanelVisible();
    }

    public dynamic_panel_entry_ui_main getActivePanel() {
        return activePanel;
    }

    public List<dynamic_panel_entry_ui_main> getRegisteredPanels() {
        return Collections.unmodifiableList(panels);
    }

    private void syncNodeState(dynamic_panel_entry_ui_main p, boolean visible) {
        Node n = p.asNode();
        if (n != null) {
            n.setVisible(visible);
            n.setManaged(visible);
        }
    }
}
