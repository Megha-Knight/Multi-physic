package cam.toolpath;

import cam.model.CamPoint3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic sequence of toolpath motion segments with trajectory statistics.
 */
public class Toolpath {

    private final String operationId;
    private final List<ToolpathSegment> segments = new ArrayList<>();

    public Toolpath(String operationId) {
        this.operationId = operationId;
    }

    public void addSegment(ToolpathSegment segment) {
        if (segment != null) segments.add(segment);
    }

    public void addSegment(MotionType type, CamPoint3D start, CamPoint3D end, double feed, double rpm) {
        addSegment(new ToolpathSegment(type, start, end, feed, rpm));
    }

    public String getOperationId() { return operationId; }
    public List<ToolpathSegment> getSegments() { return Collections.unmodifiableList(segments); }
    public int getSegmentCount() { return segments.size(); }
    public boolean isEmpty() { return segments.isEmpty(); }

    public double getTotalCuttingDistance() {
        double d = 0;
        for (var s : segments) if (s.motionType().isCutting()) d += s.getLength();
        return d;
    }

    public double getTotalRapidDistance() {
        double d = 0;
        for (var s : segments) if (!s.motionType().isCutting()) d += s.getLength();
        return d;
    }

    public double getTotalDistance() {
        double d = 0;
        for (var s : segments) d += s.getLength();
        return d;
    }

    public double estimateCycleTimeSeconds(double machineRapidFeedMmPerMin) {
        double time = 0;
        for (var s : segments) time += s.getDurationSeconds(machineRapidFeedMmPerMin);
        return time;
    }

    public CamPoint3D getStartPoint() {
        if (segments.isEmpty()) return null;
        return segments.get(0).start();
    }

    public CamPoint3D getEndPoint() {
        if (segments.isEmpty()) return null;
        return segments.get(segments.size() - 1).end();
    }
}
