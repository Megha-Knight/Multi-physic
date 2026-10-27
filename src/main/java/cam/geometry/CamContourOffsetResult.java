package cam.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable structured result from 2D contour offset computation,
 * returning either valid offset geometry or specific geometric error diagnostics.
 */
public class CamContourOffsetResult {

    private final boolean success;
    private final CamContour2D offsetContour;
    private final List<String> errors;
    private final List<String> warnings;

    public CamContourOffsetResult(boolean success, CamContour2D offsetContour,
                                  List<String> errors, List<String> warnings) {
        this.success = success;
        this.offsetContour = offsetContour;
        this.errors = (errors != null) ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
        this.warnings = (warnings != null) ? Collections.unmodifiableList(new ArrayList<>(warnings)) : List.of();
    }

    public static CamContourOffsetResult success(CamContour2D contour, List<String> warnings) {
        if (contour == null) throw new IllegalArgumentException("Offset contour cannot be null on success.");
        return new CamContourOffsetResult(true, contour, List.of(), warnings);
    }

    public static CamContourOffsetResult success(CamContour2D contour) {
        return success(contour, List.of());
    }

    public static CamContourOffsetResult failure(List<String> errors, List<String> warnings) {
        return new CamContourOffsetResult(false, null, errors, warnings);
    }

    public static CamContourOffsetResult failure(List<String> errors) {
        return failure(errors, List.of());
    }

    public static CamContourOffsetResult failure(String singleError) {
        return failure(List.of(singleError), List.of());
    }

    public boolean isSuccess()                  { return success; }
    public CamContour2D getOffsetContour()      { return offsetContour; }
    public List<String> getErrors()             { return errors; }
    public List<String> getWarnings()           { return warnings; }
    public boolean hasWarnings()                { return !warnings.isEmpty(); }

    @Override
    public String toString() {
        return "CamContourOffsetResult{success=" + success
            + (success ? ", vertices=" + offsetContour.getVertexCount() : ", errors=" + errors) + "}";
    }
}
