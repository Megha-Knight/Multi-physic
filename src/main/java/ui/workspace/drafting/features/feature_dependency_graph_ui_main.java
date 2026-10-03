package ui.workspace.drafting.features;

import ui.workspace.drafting.shape_item_ui_main;
import java.util.*;

public class feature_dependency_graph_ui_main {
    private final Map<String, feature_node_ui_main> nodes = new LinkedHashMap<>();

    public void registerFeature(feature_node_ui_main node) { if (node != null) nodes.put(node.getId(), node); }
    public void addNode(feature_node_ui_main node) { registerFeature(node); }
    public feature_node_ui_main getFeature(String id) { return nodes.get(id); }
    public feature_node_ui_main getNode(String id) { return nodes.get(id); }
    public Collection<feature_node_ui_main> getAllFeatures() { return nodes.values(); }
    public int getNodeCount() { return nodes.size(); }
    public void clear() { nodes.clear(); }

    public boolean canAddDependency(String featureId, String dependencyId) {
        if (featureId == null || dependencyId == null || featureId.equals(dependencyId)) return false;
        return !isReachable(dependencyId, featureId, new HashSet<>());
    }
    private boolean isReachable(String cur, String target, Set<String> visited) {
        if (cur.equals(target)) return true;
        if (!visited.add(cur)) return false;
        feature_node_ui_main node = nodes.get(cur);
        if (node != null) {
            for (feature_dependency_ui_main d : node.getDependencies()) {
                if (isReachable(d.dependencyId(), target, visited)) return true;
            }
        }
        return false;
    }

    public void addDependency(String featureId, String dependencyId, dependency_type_ui_main type) {
        if (featureId == null || dependencyId == null || type == null) return;
        feature_node_ui_main feat = nodes.get(featureId);
        if (feat != null) {
            feat.addDependency(dependencyId, type);
            feature_node_ui_main dep = nodes.get(dependencyId);
            if (dep != null) dep.addDependent(featureId);
        }
    }

    public boolean addDependencySafely(String featureId, String dependencyId, dependency_type_ui_main type) {
        if (!canAddDependency(featureId, dependencyId)) return false;
        addDependency(featureId, dependencyId, type);
        return true;
    }

    public void removeFeature(String featureId) {
        feature_node_ui_main removed = nodes.remove(featureId);
        if (removed == null) return;
        for (feature_node_ui_main n : nodes.values()) {
            n.removeDependency(featureId);
            n.removeDependent(featureId);
        }
    }
    public void removeNode(String id) { removeFeature(id); }

    public List<String> getParentIds(String id) {
        feature_node_ui_main n = nodes.get(id);
        return (n != null) ? n.getParentIds() : Collections.emptyList();
    }
    public Set<String> getChildIds(String id) {
        feature_node_ui_main n = nodes.get(id);
        return (n != null) ? new LinkedHashSet<>(n.getChildIds()) : Collections.emptySet();
    }

    public Set<String> getDeletionImpact(String featureId) {
        Set<String> impact = new LinkedHashSet<>();
        collectDownstream(featureId, impact);
        return impact;
    }
    private void collectDownstream(String id, Set<String> impact) {
        feature_node_ui_main n = nodes.get(id);
        if (n == null) return;
        for (String childId : n.getChildIds()) {
            if (impact.add(childId)) collectDownstream(childId, impact);
        }
    }

    public void markDirty(String featureId) { propagateDirty(featureId, new HashSet<>()); }
    private void propagateDirty(String id, Set<String> visited) {
        if (id == null || !visited.add(id)) return;
        feature_node_ui_main node = nodes.get(id);
        if (node != null) {
            if (node.getState() == feature_state_ui_main.CLEAN || node.getState() == feature_state_ui_main.VALID) {
                node.setState(feature_state_ui_main.DIRTY);
            }
            for (String depId : node.getDependents()) propagateDirty(depId, visited);
        }
    }

    public List<String> detectCycle() {
        Set<String> visited = new HashSet<>(), recStack = new HashSet<>();
        List<String> cyclePath = new ArrayList<>();
        for (String id : nodes.keySet()) {
            if (findCycleDfs(id, visited, recStack, cyclePath)) return cyclePath;
        }
        return Collections.emptyList();
    }
    public boolean hasCycle() { return !detectCycle().isEmpty(); }

    private boolean findCycleDfs(String cur, Set<String> visited, Set<String> recStack, List<String> path) {
        if (recStack.contains(cur)) { path.add(cur); return true; }
        if (visited.contains(cur)) return false;
        visited.add(cur); recStack.add(cur);
        feature_node_ui_main node = nodes.get(cur);
        if (node != null) {
            for (feature_dependency_ui_main dep : node.getDependencies()) {
                if (findCycleDfs(dep.dependencyId(), visited, recStack, path)) {
                    path.add(cur); return true;
                }
            }
        }
        recStack.remove(cur); return false;
    }

    public List<String> getRegenerationOrder() {
        List<String> cycle = detectCycle();
        if (!cycle.isEmpty()) {
            for (String cid : cycle) {
                feature_node_ui_main n = nodes.get(cid);
                if (n != null) n.setInvalid("Dependency cycle detected: " + cycle);
            }
            return Collections.emptyList();
        }
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String id : nodes.keySet()) {
            if (!visited.contains(id)) topoDfs(id, visited, new HashSet<>(), order);
        }
        return order;
    }
    private void topoDfs(String cur, Set<String> visited, Set<String> recStack, List<String> order) {
        if (visited.contains(cur) || recStack.contains(cur)) return;
        recStack.add(cur);
        feature_node_ui_main node = nodes.get(cur);
        if (node != null) {
            for (feature_dependency_ui_main dep : node.getDependencies()) {
                topoDfs(dep.dependencyId(), visited, recStack, order);
            }
        }
        recStack.remove(cur); visited.add(cur); order.add(cur);
    }

    public void validate(Collection<shape_item_ui_main> shapes) {
        List<String> cycle = detectCycle();
        if (!cycle.isEmpty()) for (String cid : cycle) {
            feature_node_ui_main n = nodes.get(cid); if (n != null) n.setInvalid("Dependency cycle: " + cycle);
        }
        for (feature_node_ui_main node : nodes.values()) {
            for (feature_dependency_ui_main dep : node.getDependencies()) {
                feature_node_ui_main depNode = nodes.get(dep.dependencyId());
                if (depNode == null) {
                    node.setInvalid("Missing dependency: " + dep.dependencyId()); break;
                } else if (depNode.getState() == feature_state_ui_main.INVALID) {
                    node.setInvalid("Upstream dependency " + depNode.getId() + " is invalid"); break;
                }
            }
        }
        feature_graph_builder_ui_main.syncValidationToShapes(this, shapes);
    }

    public List<String> validateDependencies(Collection<shape_item_ui_main> shapes) {
        validate(shapes);
        List<String> invalid = new ArrayList<>();
        for (feature_node_ui_main n : nodes.values()) if (n.getState() == feature_state_ui_main.INVALID) invalid.add(n.getId());
        return invalid;
    }

    public void buildFromShapes(Collection<shape_item_ui_main> shapes) {
        feature_graph_builder_ui_main.buildFromShapes(this, shapes);
    }
}
