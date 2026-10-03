package ui.workspace.drafting.features;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * feature_node_ui_main.java
 * Node in the CAD feature-history graph representing a parametric feature and its relations.
 */
public class feature_node_ui_main {

    private final String id;
    private String name;
    private final feature_type_ui_main type;
    private final String ownerId;

    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private String diagnosticMessage = null;
    private boolean visible = true;

    private final List<feature_dependency_ui_main> dependencies = new ArrayList<>();
    private final Set<String> dependents = new LinkedHashSet<>();

    public feature_node_ui_main(String id, String name, feature_type_ui_main type, String ownerId) {
        this.id = Objects.requireNonNull(id, "Feature id cannot be null");
        this.name = (name != null && !name.isBlank()) ? name : id;
        this.type = Objects.requireNonNull(type, "Feature type cannot be null");
        this.ownerId = ownerId;
    }

    public feature_node_ui_main(String id, feature_type_ui_main type, String name) {
        this(id, name, type, null);
    }

    public boolean hasUpstream(String depId) {
        for (feature_dependency_ui_main dep : dependencies) if (dep.dependencyId().equals(depId)) return true;
        return false;
    }
    public boolean hasDownstream(String depId) { return dependents.contains(depId); }
    public List<feature_dependency_ui_main> getUpstreamDependencies() { return dependencies; }
    public List<String> getParentIds() {
        List<String> list = new ArrayList<>();
        for (feature_dependency_ui_main d : dependencies) list.add(d.dependencyId());
        return list;
    }
    public Set<String> getChildIds() { return dependents; }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public feature_type_ui_main getType() { return type; }
    public String getOwnerId() { return ownerId; }

    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main state) {
        this.state = (state != null) ? state : feature_state_ui_main.VALID;
        if (this.state != feature_state_ui_main.INVALID) {
            this.diagnosticMessage = null;
        }
    }

    public boolean isSuppressed() { return state == feature_state_ui_main.SUPPRESSED; }
    public void setSuppressed(boolean s) {
        if (s) setState(feature_state_ui_main.SUPPRESSED);
        else if (state == feature_state_ui_main.SUPPRESSED) setState(feature_state_ui_main.DIRTY);
    }

    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setInvalid(String message) {
        this.state = feature_state_ui_main.INVALID;
        this.diagnosticMessage = message;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public List<feature_dependency_ui_main> getDependencies() { return dependencies; }
    public void addDependency(String dependencyId, dependency_type_ui_main depType) {
        if (dependencyId == null || depType == null) return;
        for (feature_dependency_ui_main existing : dependencies) {
            if (existing.dependencyId().equals(dependencyId) && existing.type() == depType) return;
        }
        dependencies.add(new feature_dependency_ui_main(this.id, dependencyId, depType));
    }
    public void removeDependency(String dependencyId) {
        dependencies.removeIf(d -> d.dependencyId().equals(dependencyId));
    }

    public Set<String> getDependents() { return dependents; }
    public void addDependent(String depId) { if (depId != null) dependents.add(depId); }
    public void removeDependent(String depId) { dependents.remove(depId); }

    public boolean isGenerative() {
        return visible && state != feature_state_ui_main.INVALID && state != feature_state_ui_main.SUPPRESSED;
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, deps=%d]", name, type, state, dependencies.size());
    }
}
