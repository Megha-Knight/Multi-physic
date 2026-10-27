package cam.plan;

import cam.operation.CamOperation;
import cam.operation.CamOperationType;
import cam.tool.CamTool;

import java.util.*;

/**
 * Encapsulates a planned machining operation within a CAM operation plan,
 * tracking its unique identity, type, tool, execution delegate, and dependencies.
 */
public class CamPlannedOperation {

    private final String id;
    private final String name;
    private final CamOperationType operationType;
    private final CamTool tool;
    private final CamOperation operation;
    private final Set<String> dependencies;

    public CamPlannedOperation(String id, String name, CamOperationType operationType,
                               CamTool tool, CamOperation operation, Collection<String> dependencies) {
        this.id = (id != null) ? id.trim() : "";
        this.name = (name != null && !name.isBlank()) ? name.trim() : this.id;
        this.operationType = operationType;
        this.tool = tool;
        this.operation = operation;
        Set<String> depSet = new LinkedHashSet<>();
        if (dependencies != null) {
            for (String dep : dependencies) {
                if (dep != null && !dep.isBlank()) depSet.add(dep.trim());
            }
        }
        this.dependencies = Collections.unmodifiableSet(depSet);
    }

    public static CamPlannedOperation of(CamOperation op, String... dependencies) {
        if (op == null) throw new IllegalArgumentException("CamOperation cannot be null.");
        List<String> deps = (dependencies != null) ? Arrays.asList(dependencies) : List.of();
        return new CamPlannedOperation(op.getId(), op.getName(), op.getOperationType(), op.getTool(), op, deps);
    }

    public static CamPlannedOperation of(CamOperation op, Collection<String> dependencies) {
        if (op == null) throw new IllegalArgumentException("CamOperation cannot be null.");
        return new CamPlannedOperation(op.getId(), op.getName(), op.getOperationType(), op.getTool(), op, dependencies);
    }

    public String getId()                           { return id; }
    public String getName()                         { return name; }
    public CamOperationType getOperationType()      { return operationType; }
    public CamTool getTool()                        { return tool; }
    public CamOperation getOperation()              { return operation; }
    public Set<String> getDependencies()            { return dependencies; }
    public boolean hasDependency(String depId)      { return dependencies.contains(depId); }

    @Override
    public String toString() {
        return "CamPlannedOperation{" + id + " ('" + name + "'), deps=" + dependencies + "}";
    }
}
