package cam.plan;

import cam.model.ManufacturingSetup;
import cam.operation.CamOperation;
import cam.post.ControllerProfile;

import java.util.*;

/**
 * Top-level CAM Operation Plan aggregating a manufacturing setup, controller profile,
 * and dependency-tracked machining operations.
 */
public class CamOperationPlan {

    private final String id;
    private String name;
    private final ManufacturingSetup setup;
    private ControllerProfile profile;
    private final List<CamPlannedOperation> operations = new ArrayList<>();

    public CamOperationPlan(String id, String name, ManufacturingSetup setup, ControllerProfile profile) {
        this.id = (id != null && !id.isBlank()) ? id.trim() : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name.trim() : "Operation Plan";
        this.setup = setup;
        this.profile = (profile != null) ? profile : ControllerProfile.createIso6983Standard();
    }

    public CamOperationPlan(String name, ManufacturingSetup setup, ControllerProfile profile) {
        this(null, name, setup, profile);
    }

    public CamOperationPlan(String name, ManufacturingSetup setup) {
        this(null, name, setup, ControllerProfile.createIso6983Standard());
    }

    public CamOperationPlan addOperation(CamPlannedOperation op) {
        if (op == null) throw new IllegalArgumentException("Planned operation cannot be null.");
        this.operations.add(op);
        return this;
    }

    public CamOperationPlan addOperation(CamOperation op, String... dependencies) {
        return addOperation(CamPlannedOperation.of(op, dependencies));
    }

    public String getId()                                    { return id; }
    public String getName()                                  { return name; }
    public void setName(String name)                         { this.name = name; }
    public ManufacturingSetup getSetup()                     { return setup; }
    public ControllerProfile getProfile()                    { return profile; }
    public void setProfile(ControllerProfile profile)        { this.profile = profile; }
    public List<CamPlannedOperation> getOperations()         { return Collections.unmodifiableList(operations); }
    public int getOperationCount()                           { return operations.size(); }

    public CamPlannedOperation getOperation(String opId) {
        if (opId == null) return null;
        for (CamPlannedOperation op : operations) {
            if (opId.equals(op.getId())) return op;
        }
        return null;
    }

    public boolean hasOperation(String opId) {
        return getOperation(opId) != null;
    }
}
