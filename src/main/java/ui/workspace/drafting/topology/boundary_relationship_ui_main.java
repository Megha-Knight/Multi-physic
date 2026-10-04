package ui.workspace.drafting.topology;

import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.features.feature_state_ui_main;

/**
 * boundary_relationship_ui_main.java
 * High-fidelity report of a machining feature's spatial relationship with body boundaries.
 */
public class boundary_relationship_ui_main {

    private final boundary_condition_ui_main condition;
    private final face_kind_ui_main sourceFace;
    private final String sourceFaceId;
    private final String affectedEdgeId;
    private final String affectedEdgeName;
    private final face_kind_ui_main adjacentFaceKind;
    private final String adjacentFaceId;
    private final String involvedVertexId;
    private final String involvedVertexName;
    private final double marginDistance;
    private final double overlapDistance;
    private final feature_state_ui_main state;
    private final String diagnosticMessage;

    public boundary_relationship_ui_main(
            boundary_condition_ui_main condition,
            face_kind_ui_main sourceFace,
            String sourceFaceId,
            String affectedEdgeId,
            String affectedEdgeName,
            face_kind_ui_main adjacentFaceKind,
            String adjacentFaceId,
            String involvedVertexId,
            String involvedVertexName,
            double marginDistance,
            double overlapDistance,
            feature_state_ui_main state,
            String diagnosticMessage) {
        this.condition = condition != null ? condition : boundary_condition_ui_main.CONTAINED;
        this.sourceFace = sourceFace;
        this.sourceFaceId = sourceFaceId;
        this.affectedEdgeId = affectedEdgeId;
        this.affectedEdgeName = affectedEdgeName;
        this.adjacentFaceKind = adjacentFaceKind;
        this.adjacentFaceId = adjacentFaceId;
        this.involvedVertexId = involvedVertexId;
        this.involvedVertexName = involvedVertexName;
        this.marginDistance = marginDistance;
        this.overlapDistance = overlapDistance;
        this.state = state != null ? state : feature_state_ui_main.CLEAN;
        this.diagnosticMessage = diagnosticMessage;
    }

    public static boundary_relationship_ui_main contained(face_kind_ui_main src, String srcId, double margin) {
        return new boundary_relationship_ui_main(
            boundary_condition_ui_main.CONTAINED, src, srcId, null, null, null, null, null, null,
            margin, 0.0, feature_state_ui_main.CLEAN, "Feature is fully contained within host face."
        );
    }

    public static boundary_relationship_ui_main tangent(
            face_kind_ui_main src, String srcId, String edgeId, String edgeName,
            face_kind_ui_main adjKind, String adjId, double margin, String diag) {
        return new boundary_relationship_ui_main(
            boundary_condition_ui_main.TANGENT, src, srcId, edgeId, edgeName, adjKind, adjId, null, null,
            margin, 0.0, feature_state_ui_main.INVALID, diag
        );
    }

    public static boundary_relationship_ui_main crossesEdge(
            face_kind_ui_main src, String srcId, String edgeId, String edgeName,
            face_kind_ui_main adjKind, String adjId, double overlap, String diag) {
        return new boundary_relationship_ui_main(
            boundary_condition_ui_main.CROSSES_EDGE, src, srcId, edgeId, edgeName, adjKind, adjId, null, null,
            0.0, overlap, feature_state_ui_main.INVALID, diag
        );
    }

    public static boundary_relationship_ui_main crossesCorner(
            face_kind_ui_main src, String srcId, String vertId, String vertName,
            String edgeId, String edgeName, face_kind_ui_main adjKind, String adjId, double overlap, String diag) {
        return new boundary_relationship_ui_main(
            boundary_condition_ui_main.CROSSES_CORNER, src, srcId, edgeId, edgeName, adjKind, adjId,
            vertId, vertName, 0.0, overlap, feature_state_ui_main.INVALID, diag
        );
    }

    public static boundary_relationship_ui_main intersectingCavities(
            face_kind_ui_main src, String srcId, String otherFeatureId, double overlap, String diag) {
        return new boundary_relationship_ui_main(
            boundary_condition_ui_main.INTERSECTING_CAVITIES, src, srcId, null, null, null, otherFeatureId,
            null, null, 0.0, overlap, feature_state_ui_main.INVALID, diag
        );
    }

    public boundary_condition_ui_main getCondition() { return condition; }
    public face_kind_ui_main getSourceFace() { return sourceFace; }
    public String getSourceFaceId() { return sourceFaceId; }
    public String getAffectedEdgeId() { return affectedEdgeId; }
    public String getAffectedEdgeName() { return affectedEdgeName; }
    public face_kind_ui_main getAdjacentFaceKind() { return adjacentFaceKind; }
    public String getAdjacentFaceId() { return adjacentFaceId; }
    public String getInvolvedVertexId() { return involvedVertexId; }
    public String getInvolvedVertexName() { return involvedVertexName; }
    public double getMarginDistance() { return marginDistance; }
    public double getOverlapDistance() { return overlapDistance; }
    public feature_state_ui_main getState() { return state; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public boolean isValid() { return state != feature_state_ui_main.INVALID && condition == boundary_condition_ui_main.CONTAINED; }
}
