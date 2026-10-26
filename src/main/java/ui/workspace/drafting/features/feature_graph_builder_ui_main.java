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
            if (s.isOnFace() && s.getFaceOwnerId() != null) {
                sNode.addDependency(s.getFaceOwnerId(), dependency_type_ui_main.HOST_BODY);
            }
            for (ui.workspace.drafting.sketch.sketch_feature_ui_main sk : s.getSketches()) {
                feature_node_ui_main skNode = new feature_node_ui_main(sk.getId(), sk.getName(), feature_type_ui_main.SKETCH, s.getId());
                skNode.setState(sk.getState());
                if (sk.getState() == feature_state_ui_main.INVALID) skNode.setInvalid(sk.getDiagnosticMessage());
                graph.registerFeature(skNode);
                skNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
            }
            for (extrude_feature_ui_main e : s.getExtrusions()) {
                feature_node_ui_main eNode = new feature_node_ui_main(e.getId(), e.getName(), feature_type_ui_main.EXTRUDE_FEATURE, s.getId());
                eNode.setState(e.getState());
                if (e.getState() == feature_state_ui_main.INVALID) eNode.setInvalid(e.getDiagnosticMessage());
                graph.registerFeature(eNode);
                eNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
                if (e.getSketchId() != null) eNode.addDependency(e.getSketchId(), dependency_type_ui_main.SKETCH_PROFILE);
            }
            for (hole_feature_ui_main h : s.getHoles()) {
                feature_node_ui_main hNode = new feature_node_ui_main(h.getId(), h.getName(), feature_type_ui_main.HOLE_FEATURE, s.getId());
                hNode.setState(h.getState());
                if (h.getState() == feature_state_ui_main.INVALID) hNode.setInvalid(h.getDiagnosticMessage());
                graph.registerFeature(hNode);
                hNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
            }
            for (hole_pattern_ui_main p : s.getPatterns()) {
                feature_node_ui_main pNode = new feature_node_ui_main(p.getId(), p.getPatternType().getLabel(), feature_type_ui_main.PATTERN_FEATURE, s.getId());
                pNode.setState(p.getState());
                if (p.getState() == feature_state_ui_main.INVALID) pNode.setInvalid(p.getDiagnosticMessage());
                graph.registerFeature(pNode);
                pNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
                if (p.getSeedHoleId() != null) pNode.addDependency(p.getSeedHoleId(), dependency_type_ui_main.SEED_FEATURE);
            }
            for (ui.workspace.drafting.machining.fillet_feature_ui_main f : s.getFillets()) {
                feature_node_ui_main fNode = new feature_node_ui_main(f.getId(), f.getName(), feature_type_ui_main.FILLET, s.getId());
                fNode.setState(f.getState());
                if (f.getState() == feature_state_ui_main.INVALID) fNode.setInvalid(f.getDiagnosticMessage());
                graph.registerFeature(fNode);
                fNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
            }
            for (ui.workspace.drafting.machining.chamfer_feature_ui_main c : s.getChamfers()) {
                feature_node_ui_main cNode = new feature_node_ui_main(c.getId(), c.getName(), feature_type_ui_main.CHAMFER, s.getId());
                cNode.setState(c.getState());
                if (c.getState() == feature_state_ui_main.INVALID) cNode.setInvalid(c.getDiagnosticMessage());
                graph.registerFeature(cNode);
                cNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
            }
            for (ui.workspace.drafting.machining.draft_feature_ui_main d : s.getDrafts()) {
                feature_node_ui_main dNode = new feature_node_ui_main(d.getId(), d.getName(), feature_type_ui_main.DRAFT, s.getId());
                dNode.setState(d.getState());
                if (d.getState() == feature_state_ui_main.INVALID) dNode.setInvalid(d.getDiagnosticMessage());
                graph.registerFeature(dNode);
                dNode.addDependency(s.getId(), dependency_type_ui_main.HOST_BODY);
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
            for (extrude_feature_ui_main e : s.getExtrusions()) {
                feature_node_ui_main en = graph.getFeature(e.getId());
                if (en != null && en.getState() == feature_state_ui_main.INVALID) {
                    e.setState(en.getState()); e.setDiagnosticMessage(en.getDiagnosticMessage());
                }
            }
            for (hole_feature_ui_main h : s.getHoles()) {
                feature_node_ui_main hn = graph.getFeature(h.getId());
                if (hn != null && hn.getState() == feature_state_ui_main.INVALID) {
                    h.setState(hn.getState()); h.setDiagnosticMessage(hn.getDiagnosticMessage());
                }
            }
            for (ui.workspace.drafting.sketch.sketch_feature_ui_main sk : s.getSketches()) {
                feature_node_ui_main skn = graph.getFeature(sk.getId());
                if (skn != null && skn.getState() == feature_state_ui_main.INVALID) {
                    sk.setState(skn.getState()); sk.setDiagnosticMessage(skn.getDiagnosticMessage());
                }
            }
            for (hole_pattern_ui_main p : s.getPatterns()) {
                feature_node_ui_main pn = graph.getFeature(p.getId());
                if (pn != null && pn.getState() == feature_state_ui_main.INVALID) {
                    p.setState(pn.getState()); p.setDiagnosticMessage(pn.getDiagnosticMessage());
                }
            }
            for (ui.workspace.drafting.machining.fillet_feature_ui_main f : s.getFillets()) {
                feature_node_ui_main fn = graph.getFeature(f.getId());
                if (fn != null && fn.getState() == feature_state_ui_main.INVALID) {
                    f.setState(fn.getState()); f.setDiagnosticMessage(fn.getDiagnosticMessage());
                }
            }
            for (ui.workspace.drafting.machining.chamfer_feature_ui_main c : s.getChamfers()) {
                feature_node_ui_main cn = graph.getFeature(c.getId());
                if (cn != null && cn.getState() == feature_state_ui_main.INVALID) {
                    c.setState(cn.getState()); c.setDiagnosticMessage(cn.getDiagnosticMessage());
                }
            }
            for (ui.workspace.drafting.machining.draft_feature_ui_main d : s.getDrafts()) {
                feature_node_ui_main dn = graph.getFeature(d.getId());
                if (dn != null && dn.getState() == feature_state_ui_main.INVALID) {
                    d.setState(dn.getState()); d.setDiagnosticMessage(dn.getDiagnosticMessage());
                }
            }
        }
    }
}
