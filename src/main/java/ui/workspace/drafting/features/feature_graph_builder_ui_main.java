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
            sNode.setState(s.getState()); graph.registerFeature(sNode);
            if (s.isOnFace() && s.getFaceOwnerId() != null) sNode.addDependency(s.getFaceOwnerId(), dependency_type_ui_main.HOST_BODY);
            for (hole_feature_ui_main h : s.getHoles()) {
                feature_node_ui_main hNode = new feature_node_ui_main(h.getId(), h.getName(), feature_type_ui_main.HOLE_FEATURE, s.getId());
                hNode.setState(h.getState()); graph.registerFeature(hNode);
                hNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
            }
            for (hole_pattern_ui_main p : s.getPatterns()) {
                feature_node_ui_main pNode = new feature_node_ui_main(p.getId(), p.getPatternType().getLabel(), feature_type_ui_main.PATTERN_FEATURE, s.getId());
                pNode.setState(p.getState()); graph.registerFeature(pNode);
                pNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
                if (p.getSeedHoleId() != null) pNode.addDependency(p.getSeedHoleId(), dependency_type_ui_main.SEED_FEATURE);
            }
            for (extrude_feature_ui_main e : s.getExtrusions()) {
                feature_node_ui_main eNode = new feature_node_ui_main(e.getId(), e.getName(), feature_type_ui_main.EXTRUDE_FEATURE, s.getId());
                eNode.setState(e.getState()); graph.registerFeature(eNode);
                eNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
            }
        }
        for (feature_node_ui_main n : graph.getAllFeatures()) {
            for (feature_dependency_ui_main dep : n.getDependencies()) {
                feature_node_ui_main target = graph.getFeature(dep.dependencyId());
                if (target != null) target.addDependent(n.getId());
            }
        }
    }

    public static void syncValidationToShapes(feature_dependency_graph_ui_main graph, Collection<shape_item_ui_main> shapes) {
        if (graph == null || shapes == null) return;
        for (shape_item_ui_main s : shapes) {
            feature_node_ui_main sn = graph.getFeature(s.getId());
            if (sn != null && sn.getState() == feature_state_ui_main.INVALID) s.setState(sn.getState());
            for (hole_feature_ui_main h : s.getHoles()) {
                feature_node_ui_main hn = graph.getFeature(h.getId());
                if (hn != null && hn.getState() == feature_state_ui_main.INVALID) h.setState(hn.getState());
            }
            for (hole_pattern_ui_main p : s.getPatterns()) {
                feature_node_ui_main pn = graph.getFeature(p.getId());
                if (pn != null && pn.getState() == feature_state_ui_main.INVALID) {
                    p.setState(pn.getState()); p.setDiagnosticMessage(pn.getDiagnosticMessage());
                }
            }
            for (extrude_feature_ui_main e : s.getExtrusions()) {
                feature_node_ui_main en = graph.getFeature(e.getId());
                if (en != null && en.getState() == feature_state_ui_main.INVALID) e.setState(en.getState());
            }
        }
    }
}
