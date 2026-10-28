package ui.workspace.drafting.features;

import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_pattern_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import java.util.Collection;

public final class feature_graph_builder_ui_main {
    private feature_graph_builder_ui_main() {}

    public static void buildFromShapes(feature_dependency_graph_ui_main graph, Collection<shape_item_ui_main> shapes) {
        if (graph == null) return;
        graph.clear();
        if (shapes == null) return;
        for (shape_item_ui_main s : shapes) {
            feature_type_ui_main sType = s.isOnFace() ? feature_type_ui_main.SKETCH_PROFILE : feature_type_ui_main.SOLID_BODY;
            feature_node_ui_main sNode = new feature_node_ui_main(s.getId(), s.getName(), sType, s.getFaceOwnerId());
            sNode.setState(s.getState());
            graph.registerFeature(sNode);
            if (s.isOnFace() && s.getFaceOwnerId() != null) sNode.addDependency(s.getFaceOwnerId(), dependency_type_ui_main.HOST_BODY);
            for (var sk : s.getSketches()) registerNode(graph, sk.getId(), sk.getName(), feature_type_ui_main.SKETCH, s.getId(), sk.getState(), sk.getDiagnosticMessage());
            for (extrude_feature_ui_main e : s.getExtrusions()) {
                feature_node_ui_main eNode = registerNode(graph, e.getId(), e.getName(), feature_type_ui_main.EXTRUDE_FEATURE, s.getId(), e.getState(), e.getDiagnosticMessage());
                if (e.getSketchId() != null) eNode.addDependency(e.getSketchId(), dependency_type_ui_main.SKETCH_PROFILE);
            }
            for (hole_feature_ui_main h : s.getHoles()) registerNode(graph, h.getId(), h.getName(), feature_type_ui_main.HOLE_FEATURE, s.getId(), h.getState(), h.getDiagnosticMessage());
            for (hole_pattern_ui_main p : s.getPatterns()) {
                feature_node_ui_main pNode = registerNode(graph, p.getId(), p.getPatternType().getLabel(), feature_type_ui_main.PATTERN_FEATURE, s.getId(), p.getState(), p.getDiagnosticMessage());
                if (p.getSeedHoleId() != null) pNode.addDependency(p.getSeedHoleId(), dependency_type_ui_main.SEED_FEATURE);
            }
            for (var f : s.getFillets()) registerNode(graph, f.getId(), f.getName(), feature_type_ui_main.FILLET, s.getId(), f.getState(), f.getDiagnosticMessage());
            for (var c : s.getChamfers()) registerNode(graph, c.getId(), c.getName(), feature_type_ui_main.CHAMFER, s.getId(), c.getState(), c.getDiagnosticMessage());
            for (var d : s.getDrafts()) registerNode(graph, d.getId(), d.getName(), feature_type_ui_main.DRAFT, s.getId(), d.getState(), d.getDiagnosticMessage());
            for (var b : s.getBooleans()) registerNode(graph, b.getId(), b.getName(), feature_type_ui_main.BOOLEAN, s.getId(), b.getState(), b.getDiagnosticMessage());
            for (var sh : s.getShells()) registerNode(graph, sh.getId(), sh.getName(), feature_type_ui_main.SHELL, s.getId(), sh.getState(), sh.getDiagnosticMessage());
            for (var l : s.getLofts()) registerNode(graph, l.getId(), l.getName(), feature_type_ui_main.LOFT, s.getId(), l.getState(), l.getDiagnosticMessage());
            for (var sw : s.getSweeps()) registerNode(graph, sw.getId(), sw.getName(), feature_type_ui_main.SWEEP, s.getId(), sw.getState(), sw.getDiagnosticMessage());
            for (var r : s.getRevolves()) registerNode(graph, r.getId(), r.getName(), feature_type_ui_main.REVOLVE, s.getId(), r.getState(), r.getDiagnosticMessage());
        }
        for (feature_node_ui_main n : graph.getAllFeatures()) {
            for (feature_dependency_ui_main dep : n.getDependencies()) {
                feature_node_ui_main target = graph.getFeature(dep.dependencyId());
                if (target != null) target.addDependent(n.getId());
            }
        }
    }

    private static feature_node_ui_main registerNode(feature_dependency_graph_ui_main graph, String id, String name, feature_type_ui_main type, String hostId, feature_state_ui_main state, String diag) {
        feature_node_ui_main node = new feature_node_ui_main(id, name, type, hostId);
        node.setState(state);
        if (state == feature_state_ui_main.INVALID) node.setInvalid(diag);
        graph.registerFeature(node);
        node.addDependency(hostId, dependency_type_ui_main.HOST_BODY);
        return node;
    }

    public static void syncValidationToShapes(feature_dependency_graph_ui_main graph, Collection<shape_item_ui_main> shapes) {
        if (graph == null || shapes == null) return;
        for (shape_item_ui_main s : shapes) {
            feature_node_ui_main sn = graph.getFeature(s.getId());
            if (sn != null && sn.getState() == feature_state_ui_main.INVALID) s.setState(sn.getState());
            for (extrude_feature_ui_main e : s.getExtrusions()) syncState(graph, e.getId(), e::setState, e::setDiagnosticMessage);
            for (hole_feature_ui_main h : s.getHoles()) syncState(graph, h.getId(), h::setState, h::setDiagnosticMessage);
            for (var sk : s.getSketches()) syncState(graph, sk.getId(), sk::setState, sk::setDiagnosticMessage);
            for (hole_pattern_ui_main p : s.getPatterns()) syncState(graph, p.getId(), p::setState, p::setDiagnosticMessage);
            for (var f : s.getFillets()) syncState(graph, f.getId(), f::setState, f::setDiagnosticMessage);
            for (var c : s.getChamfers()) syncState(graph, c.getId(), c::setState, c::setDiagnosticMessage);
            for (var d : s.getDrafts()) syncState(graph, d.getId(), d::setState, d::setDiagnosticMessage);
            for (var b : s.getBooleans()) syncState(graph, b.getId(), b::setState, b::setDiagnosticMessage);
            for (var sh : s.getShells()) syncState(graph, sh.getId(), sh::setState, sh::setDiagnosticMessage);
            for (var l : s.getLofts()) syncState(graph, l.getId(), l::setState, l::setDiagnosticMessage);
            for (var sw : s.getSweeps()) syncState(graph, sw.getId(), sw::setState, sw::setDiagnosticMessage);
            for (var r : s.getRevolves()) syncState(graph, r.getId(), r::setState, r::setDiagnosticMessage);
        }
    }

    private static void syncState(feature_dependency_graph_ui_main graph, String id, java.util.function.Consumer<feature_state_ui_main> setSt, java.util.function.Consumer<String> setDiag) {
        feature_node_ui_main n = graph.getFeature(id);
        if (n != null && n.getState() == feature_state_ui_main.INVALID) {
            setSt.accept(n.getState());
            setDiag.accept(n.getDiagnosticMessage());
        }
    }
}
