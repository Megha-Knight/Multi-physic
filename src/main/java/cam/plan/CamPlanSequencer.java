package cam.plan;

import java.util.*;

/**
 * Deterministic, dependency-aware topological sequencer for CAM operation plans.
 * Validates dependencies, detects cycles, and resolves stable execution orders.
 */
public final class CamPlanSequencer {

    private CamPlanSequencer() {}

    public static CamPlanValidationResult sequence(CamOperationPlan plan) {
        if (plan == null) return CamPlanValidationResult.invalid("Operation plan cannot be null.");
        List<CamPlannedOperation> ops = plan.getOperations();
        if (ops.isEmpty()) return CamPlanValidationResult.invalid("Operation plan contains no operations.");

        List<String> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Map<String, CamPlannedOperation> opMap = new LinkedHashMap<>();

        for (CamPlannedOperation op : ops) {
            if (op == null || op.getId() == null || op.getId().isBlank()) {
                errors.add("Operation contains missing or blank identity ID.");
                continue;
            }
            if (!seenIds.add(op.getId())) {
                errors.add("Duplicate operation ID '" + op.getId() + "' encountered in plan.");
            }
            opMap.put(op.getId(), op);
        }

        for (CamPlannedOperation op : ops) {
            if (op == null || op.getId().isBlank()) continue;
            for (String depId : op.getDependencies()) {
                if (depId.equals(op.getId())) {
                    errors.add("Self-dependency detected: Operation '" + op.getId() + "' cannot depend on itself.");
                } else if (!opMap.containsKey(depId)) {
                    errors.add("Missing dependency: Operation '" + op.getId() + "' depends on unknown operation '" + depId + "'.");
                }
            }
        }

        if (!errors.isEmpty()) {
            return CamPlanValidationResult.invalid(errors);
        }

        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> dependents = new LinkedHashMap<>();

        for (String id : opMap.keySet()) {
            inDegree.put(id, opMap.get(id).getDependencies().size());
            dependents.put(id, new ArrayList<>());
        }

        for (CamPlannedOperation op : ops) {
            for (String depId : op.getDependencies()) {
                dependents.get(depId).add(op.getId());
            }
        }

        List<String> readyQueue = new ArrayList<>();
        for (String id : opMap.keySet()) {
            if (inDegree.get(id) == 0) readyQueue.add(id);
        }

        List<String> resolvedOrder = new ArrayList<>();

        while (!readyQueue.isEmpty()) {
            String current = readyQueue.remove(0);
            resolvedOrder.add(current);

            for (String nextId : dependents.get(current)) {
                int updated = inDegree.get(nextId) - 1;
                inDegree.put(nextId, updated);
                if (updated == 0) {
                    readyQueue.add(nextId);
                }
            }
        }

        if (resolvedOrder.size() < opMap.size()) {
            List<String> cycleOps = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() > 0) cycleOps.add(entry.getKey());
            }
            errors.add("Dependency cycle detected involving operations: " + cycleOps);
            return CamPlanValidationResult.invalid(errors);
        }

        return CamPlanValidationResult.valid(resolvedOrder);
    }
}
