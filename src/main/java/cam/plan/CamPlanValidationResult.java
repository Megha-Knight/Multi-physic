package cam.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured diagnostic result from pre-execution validation of a CAM operation plan.
 */
public class CamPlanValidationResult {

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> resolvedOrder;

    public CamPlanValidationResult(boolean valid, List<String> errors, List<String> warnings, List<String> resolvedOrder) {
        this.valid = valid;
        this.errors = (errors != null) ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
        this.warnings = (warnings != null) ? Collections.unmodifiableList(new ArrayList<>(warnings)) : List.of();
        this.resolvedOrder = (resolvedOrder != null) ? Collections.unmodifiableList(new ArrayList<>(resolvedOrder)) : List.of();
    }

    public static CamPlanValidationResult valid(List<String> resolvedOrder, List<String> warnings) {
        return new CamPlanValidationResult(true, List.of(), warnings, resolvedOrder);
    }

    public static CamPlanValidationResult valid(List<String> resolvedOrder) {
        return valid(resolvedOrder, List.of());
    }

    public static CamPlanValidationResult invalid(List<String> errors, List<String> warnings) {
        return new CamPlanValidationResult(false, errors, warnings, List.of());
    }

    public static CamPlanValidationResult invalid(List<String> errors) {
        return invalid(errors, List.of());
    }

    public static CamPlanValidationResult invalid(String singleError) {
        return invalid(List.of(singleError), List.of());
    }

    public boolean isValid()                    { return valid; }
    public boolean hasErrors()                  { return !errors.isEmpty(); }
    public boolean hasWarnings()                { return !warnings.isEmpty(); }
    public List<String> getErrors()             { return errors; }
    public List<String> getWarnings()           { return warnings; }
    public List<String> getResolvedOrder()      { return resolvedOrder; }

    @Override
    public String toString() {
        return "CamPlanValidationResult{valid=" + valid + ", errors=" + errors.size()
            + ", warnings=" + warnings.size() + ", order=" + resolvedOrder + "}";
    }
}
