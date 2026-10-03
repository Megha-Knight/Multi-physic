package ui.workspace.drafting.topology;

/**
 * topology_entity_ui_main.java
 * Base interface for analytical CAD topology entities (body, face, edge, vertex).
 * Guarantees persistent, stable identification independent of render mesh indices.
 */
public interface topology_entity_ui_main {
    String getId();
    String getName();
}
