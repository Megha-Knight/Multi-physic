package ui.workspace.drafting.faces;

import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.hole_region_kind_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_derived_face_ui_main;
import ui.workspace.drafting.topology.topology_face_ui_main;

/**
 * face_selection_resolver_ui_main.java
 * Resolves pick coordinates (u, v) on a CAD face into specific persistent topology entities
 * (Remaining Face, Hole Wall, Hole Floor, Bore Wall, or Base Face).
 */
public final class face_selection_resolver_ui_main {

    private face_selection_resolver_ui_main() {}

    public record FaceSelectionResult(
        topology_face_ui_main baseFace,
        topology_derived_face_ui_main derivedFace,
        hole_feature_ui_main targetHole
    ) {
        public boolean isDerived() { return derivedFace != null; }
        public String getResolvedId() { return derivedFace != null ? derivedFace.getId() : (baseFace != null ? baseFace.getId() : ""); }
        public String getResolvedName() { return derivedFace != null ? derivedFace.getName() : (baseFace != null ? baseFace.getName() : ""); }
    }

    public static FaceSelectionResult resolveFace(shape_item_ui_main shape, face_kind_ui_main faceKind, double u, double v) {
        if (shape == null || faceKind == null) return new FaceSelectionResult(null, null, null);
        topology_body_ui_main body = shape.getTopology();
        if (body == null) return new FaceSelectionResult(null, null, null);

        topology_face_ui_main baseFace = body.getFace(faceKind);

        // Check if pick hits any hole feature on this face
        for (hole_feature_ui_main h : shape.getAllEffectiveHoles()) {
            if (h.getFaceKind() != faceKind) continue;
            double du = u - h.getU(), dv = v - h.getV();
            double dist = Math.hypot(du, dv);
            if (dist <= h.getRadius() + 1e-3) {
                // Inside hole boundary -> Select Wall or Floor
                for (topology_derived_face_ui_main df : body.getDerivedFaces()) {
                    if (h.getId().equals(df.getCreatingFeatureId())) {
                        if (h.isThroughAll() && df.getRegionKind() == hole_region_kind_ui_main.BORE_WALL) {
                            return new FaceSelectionResult(baseFace, df, h);
                        } else if (!h.isThroughAll() && df.getRegionKind() == hole_region_kind_ui_main.HOLE_WALL) {
                            return new FaceSelectionResult(baseFace, df, h);
                        }
                    }
                }
            }
        }

        // Check for remaining face on this face
        String remId = body.getId() + ":F:" + faceKind.name() + ":REMAINING";
        topology_derived_face_ui_main rem = body.getDerivedFaceById(remId);
        if (rem != null) {
            return new FaceSelectionResult(baseFace, rem, null);
        }

        return new FaceSelectionResult(baseFace, null, null);
    }
}
