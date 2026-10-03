package ui.workspace.drafting.features;

import java.util.Objects;

/**
 * feature_dependency_ui_main.java
 * Explicit dependency link pointing from a dependent feature to its required dependency.
 */
public record feature_dependency_ui_main(
        String featureId,
        String dependencyId,
        dependency_type_ui_main type
) {
    public feature_dependency_ui_main {
        Objects.requireNonNull(featureId, "featureId cannot be null");
        Objects.requireNonNull(dependencyId, "dependencyId cannot be null");
        Objects.requireNonNull(type, "dependency type cannot be null");
    }

    @Override
    public String toString() {
        return String.format("%s -> %s (%s)", featureId, dependencyId, type.getLabel());
    }
}
