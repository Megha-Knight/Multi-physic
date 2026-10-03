package ui.ribbonbar;

import javafx.scene.Node;

/**
 * dynamic_panel_entry_ui_main.java
 * Contract for collapsible dynamic ribbon panels participating in mutual exclusion.
 */
public interface dynamic_panel_entry_ui_main {
    void show();
    void hide();
    boolean isPanelVisible();
    Node asNode();
}
