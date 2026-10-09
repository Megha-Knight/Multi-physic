package ui.workspace.drafting.features;

import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_pattern_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import java.util.*;

public final class feature_regeneration_engine_ui_main {
    private feature_regeneration_engine_ui_main() {}

    public record RegenerationReport(
            int totalFeatures,
            int regeneratedCount,
            int invalidCount,
            boolean cycleDetected,
            List<String> order
    ) {}

    public static RegenerationReport regenerateAll(Collection<shape_item_ui_main> shapes) {
        return regenerate(shapes, new feature_dependency_graph_ui_main());
    }

    /**
     * Marks the given feature and all transitive dependents as DIRTY on the
     * actual persistent model objects, so a subsequent regenerateAll() will
     * find and rebuild them. The graph is a one-shot computation tool here:
     * build → propagate → write back to persistent objects → discard graph.
     * Only the state written onto shape_item_ui_main/hole_feature_ui_main/etc.
     * survives across the graph.clear() inside the next regenerateAll() call.
     */
    public static void markDirtyAndCascade(String rootFeatureId, Collection<shape_item_ui_main> shapes) {
        if (rootFeatureId == null || shapes == null || shapes.isEmpty()) return;
        feature_dependency_graph_ui_main graph = new feature_dependency_graph_ui_main();
        graph.buildFromShapes(shapes);   // seeds states from live objects
        graph.markDirty(rootFeatureId);  // propagates DIRTY to all transitive dependents in graph
        Map<String, shape_item_ui_main> shapeMap = new HashMap<>();
        for (shape_item_ui_main s : shapes) shapeMap.put(s.getId(), s);
        for (feature_node_ui_main fn : graph.getAllFeatures()) {
            if (fn.getState() == feature_state_ui_main.DIRTY) {
                syncEntityState(fn, shapeMap); // writes DIRTY onto the real persistent object
            }
        }
        // graph is discarded here; DIRTY state now lives on the persistent objects
        // and will be re-read by buildFromShapes() inside the next regenerateAll() call
    }

    public static RegenerationReport regenerate(feature_dependency_graph_ui_main graph, Collection<shape_item_ui_main> shapes) {
        return regenerate(shapes, graph);
    }

    public static RegenerationReport regenerate(Collection<shape_item_ui_main> shapes, feature_dependency_graph_ui_main graph) {
        if (shapes == null || shapes.isEmpty()) return new RegenerationReport(0, 0, 0, false, Collections.emptyList());
        if (graph == null) graph = new feature_dependency_graph_ui_main();

        graph.buildFromShapes(shapes);
        graph.validate(shapes);

        boolean hasCycle = graph.hasCycle();
        List<String> order = graph.getRegenerationOrder();
        int invalidCount = 0, regenCount = 0;

        Map<String, shape_item_ui_main> shapeMap = new HashMap<>();
        for (shape_item_ui_main s : shapes) shapeMap.put(s.getId(), s);

        for (feature_node_ui_main fn : graph.getAllFeatures()) {
            if (fn.getState() == feature_state_ui_main.INVALID) invalidCount++;
            syncEntityState(fn, shapeMap);
        }

        Set<shape_item_ui_main> rebuiltShapes = new LinkedHashSet<>();
        for (String featId : order) {
            feature_node_ui_main node = graph.getFeature(featId);
            if (node == null) continue;

            if (node.getState() == feature_state_ui_main.CLEAN) {
                continue; // nothing changed, no reason to touch this shape
            }
            if (node.getState() == feature_state_ui_main.DIRTY) {
                shape_item_ui_main host = resolveHost(node, shapeMap);
                if (host != null && rebuiltShapes.add(host)) {
                    List<javafx.scene.Node> backup = new ArrayList<>(host.getShapeGroup().getChildren());
                    try {
                        host.revalidateFeatures();
                        host.rebuild();
                        regenCount++;
                        node.setState(feature_state_ui_main.CLEAN);
                    } catch (Throwable t) {
                        host.getShapeGroup().getChildren().setAll(backup);
                        node.setInvalid("Regeneration error: " + t.getMessage());
                        invalidCount++;
                    }
                } else {
                    node.setState(feature_state_ui_main.CLEAN);
                }
                syncEntityState(node, shapeMap);
            }
        }
        return new RegenerationReport(graph.getAllFeatures().size(), regenCount, invalidCount, hasCycle, order);
    }

    private static shape_item_ui_main resolveHost(feature_node_ui_main node, Map<String, shape_item_ui_main> map) {
        if (node.getType() == feature_type_ui_main.SOLID_BODY || node.getType() == feature_type_ui_main.BASE_SOLID ||
            node.getType() == feature_type_ui_main.SKETCH_PROFILE) {
            return map.get(node.getId());
        }
        if (node.getType() == feature_type_ui_main.SKETCH && map.containsKey(node.getId())) return map.get(node.getId());
        return (node.getOwnerId() != null) ? map.get(node.getOwnerId()) : null;
    }

    private static void syncEntityState(feature_node_ui_main fn, Map<String, shape_item_ui_main> map) {
        shape_item_ui_main host = resolveHost(fn, map);
        if (host == null) return;
        if (fn.getType() == feature_type_ui_main.SOLID_BODY || fn.getType() == feature_type_ui_main.BASE_SOLID ||
            fn.getType() == feature_type_ui_main.SKETCH_PROFILE) {
            host.setState(fn.getState());
        } else if (fn.getType() == feature_type_ui_main.SKETCH) {
            for (var sk : host.getSketches()) {
                if (sk.getId().equals(fn.getId())) {
                    sk.setState(fn.getState()); sk.setVisible(fn.isVisible());
                    if (fn.getState() == feature_state_ui_main.INVALID) sk.setDiagnosticMessage(fn.getDiagnosticMessage());
                    break;
                }
            }
        } else if (fn.getType() == feature_type_ui_main.HOLE_FEATURE || fn.getType() == feature_type_ui_main.HOLE) {
            for (hole_feature_ui_main h : host.getHoles()) {
                if (h.getId().equals(fn.getId())) {
                    h.setState(fn.getState()); h.setVisible(fn.isVisible()); break;
                }
            }
        } else if (fn.getType() == feature_type_ui_main.PATTERN_FEATURE || fn.getType() == feature_type_ui_main.PATTERN) {
            for (hole_pattern_ui_main p : host.getPatterns()) {
                if (p.getId().equals(fn.getId())) {
                    p.setState(fn.getState()); p.setVisible(fn.isVisible()); p.setDiagnosticMessage(fn.getDiagnosticMessage()); break;
                }
            }
        } else if (fn.getType() == feature_type_ui_main.EXTRUDE_FEATURE || fn.getType() == feature_type_ui_main.EXTRUDE) {
            for (extrude_feature_ui_main ext : host.getExtrusions()) {
                if (ext.getId().equals(fn.getId())) {
                    ext.setState(fn.getState()); ext.setVisible(fn.isVisible()); break;
                }
            }
        }
    }
}
