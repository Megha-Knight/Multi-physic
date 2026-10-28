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

    public static void markDirtyAndCascade(String rootFeatureId, Collection<shape_item_ui_main> shapes) {
        if (rootFeatureId == null || shapes == null || shapes.isEmpty()) return;
        feature_dependency_graph_ui_main graph = new feature_dependency_graph_ui_main();
        graph.buildFromShapes(shapes);
        graph.markDirty(rootFeatureId);
        Map<String, shape_item_ui_main> shapeMap = new HashMap<>();
        for (shape_item_ui_main s : shapes) shapeMap.put(s.getId(), s);
        for (feature_node_ui_main fn : graph.getAllFeatures()) {
            if (fn.getState() == feature_state_ui_main.DIRTY) syncEntityState(fn, shapeMap);
        }
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

        Set<shape_item_ui_main> hostsToRebuild = new LinkedHashSet<>();
        for (String featId : order) {
            feature_node_ui_main node = graph.getFeature(featId);
            if (node == null || node.getState() == feature_state_ui_main.CLEAN) continue;
            if (node.getState() == feature_state_ui_main.DIRTY) {
                shape_item_ui_main host = resolveHost(node, shapeMap);
                if (host != null) {
                    try {
                        evaluateNode(node, host);
                        node.setState(feature_state_ui_main.CLEAN);
                        hostsToRebuild.add(host);
                        regenCount++;
                    } catch (Throwable t) {
                        node.setInvalid("Regeneration error: " + t.getMessage());
                        invalidCount++;
                    }
                    syncEntityState(node, shapeMap);
                }
            }
        }
        for (shape_item_ui_main host : hostsToRebuild) {
            try { host.revalidateFeatures(); host.rebuild(); } catch (Throwable ignored) {}
        }
        return new RegenerationReport(graph.getAllFeatures().size(), regenCount, invalidCount, hasCycle, order);
    }

    private static void evaluateNode(feature_node_ui_main node, shape_item_ui_main host) {
        if (node.getType() == feature_type_ui_main.BOOLEAN) {
            node.setInvalid("Boolean solid evaluation requires native geometry kernel");
            throw new UnsupportedOperationException("Native B-rep boolean kernel required");
        }
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
        var st = fn.getState(); var msg = fn.getDiagnosticMessage();
        if (fn.getType() == feature_type_ui_main.SOLID_BODY || fn.getType() == feature_type_ui_main.BASE_SOLID || fn.getType() == feature_type_ui_main.SKETCH_PROFILE) host.setState(st);
        else if (fn.getType() == feature_type_ui_main.SKETCH) { for (var sk : host.getSketches()) if (sk.getId().equals(fn.getId())) { sk.setState(st); sk.setVisible(fn.isVisible()); if (st == feature_state_ui_main.INVALID) sk.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.HOLE_FEATURE || fn.getType() == feature_type_ui_main.HOLE) { for (var h : host.getHoles()) if (h.getId().equals(fn.getId())) { h.setState(st); h.setVisible(fn.isVisible()); break; } }
        else if (fn.getType() == feature_type_ui_main.PATTERN_FEATURE || fn.getType() == feature_type_ui_main.PATTERN) { for (var p : host.getPatterns()) if (p.getId().equals(fn.getId())) { p.setState(st); p.setVisible(fn.isVisible()); p.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.EXTRUDE_FEATURE || fn.getType() == feature_type_ui_main.EXTRUDE) { for (var ext : host.getExtrusions()) if (ext.getId().equals(fn.getId())) { ext.setState(st); ext.setVisible(fn.isVisible()); break; } }
        else if (fn.getType() == feature_type_ui_main.FILLET) { for (var f : host.getFillets()) if (f.getId().equals(fn.getId())) { f.setState(st); f.setVisible(fn.isVisible()); f.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.CHAMFER) { for (var c : host.getChamfers()) if (c.getId().equals(fn.getId())) { c.setState(st); c.setVisible(fn.isVisible()); c.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.DRAFT) { for (var d : host.getDrafts()) if (d.getId().equals(fn.getId())) { d.setState(st); d.setVisible(fn.isVisible()); d.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.BOOLEAN) { for (var b : host.getBooleans()) if (b.getId().equals(fn.getId())) { b.setState(st); b.setVisible(fn.isVisible()); b.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.SHELL) { for (var sh : host.getShells()) if (sh.getId().equals(fn.getId())) { sh.setState(st); sh.setVisible(fn.isVisible()); sh.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.LOFT) { for (var l : host.getLofts()) if (l.getId().equals(fn.getId())) { l.setState(st); l.setVisible(fn.isVisible()); l.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.SWEEP) { for (var sw : host.getSweeps()) if (sw.getId().equals(fn.getId())) { sw.setState(st); sw.setVisible(fn.isVisible()); sw.setDiagnosticMessage(msg); break; } }
        else if (fn.getType() == feature_type_ui_main.REVOLVE) { for (var r : host.getRevolves()) if (r.getId().equals(fn.getId())) { r.setState(st); r.setVisible(fn.isVisible()); r.setDiagnosticMessage(msg); break; } }
    }
}
