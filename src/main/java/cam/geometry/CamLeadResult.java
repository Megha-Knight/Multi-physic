package cam.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable structured result holding lead-in and lead-out moves,
 * full assembled contour path, and geometric error diagnostics.
 */
public class CamLeadResult {

    private final boolean success;
    private final CamLeadMove leadIn;
    private final CamLeadMove leadOut;
    private final CamContour2D combinedPath;
    private final List<String> errors;
    private final List<String> warnings;

    public CamLeadResult(boolean success, CamLeadMove leadIn, CamLeadMove leadOut,
                         CamContour2D combinedPath, List<String> errors, List<String> warnings) {
        this.success = success;
        this.leadIn = leadIn;
        this.leadOut = leadOut;
        this.combinedPath = combinedPath;
        this.errors = (errors != null) ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
        this.warnings = (warnings != null) ? Collections.unmodifiableList(new ArrayList<>(warnings)) : List.of();
    }

    public static CamLeadResult success(CamLeadMove leadIn, CamLeadMove leadOut, CamContour2D combinedPath, List<String> warnings) {
        return new CamLeadResult(true, leadIn, leadOut, combinedPath, List.of(), warnings);
    }

    public static CamLeadResult success(CamLeadMove leadIn, CamLeadMove leadOut, CamContour2D combinedPath) {
        return success(leadIn, leadOut, combinedPath, List.of());
    }

    public static CamLeadResult failure(List<String> errors, List<String> warnings) {
        return new CamLeadResult(false, null, null, null, errors, warnings);
    }

    public static CamLeadResult failure(List<String> errors) {
        return failure(errors, List.of());
    }

    public static CamLeadResult failure(String singleError) {
        return failure(List.of(singleError), List.of());
    }

    public boolean isSuccess()                  { return success; }
    public CamLeadMove getLeadIn()              { return leadIn; }
    public CamLeadMove getLeadOut()             { return leadOut; }
    public CamContour2D getCombinedPath()       { return combinedPath; }
    public List<String> getErrors()             { return errors; }
    public List<String> getWarnings()           { return warnings; }
    public boolean hasWarnings()                { return !warnings.isEmpty(); }
}
