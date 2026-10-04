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

        // 1 & 2: Check holes on this face (Floor -> Wall -> Bore Wall)
        for (hole_feature_ui_main h : shape.getAllEffectiveHoles()) {
            if (h.getFaceKind() == faceKind) {
                double dist = Math.hypot(u - h.getU(), v - h.getV());
                if (dist <= h.getRadius() + 1e-3) {
                    if (h.isThroughAll()) {
                        topology_derived_face_ui_main bore = findDerivedFace(body, h.getId(), hole_region_kind_ui_main.BORE_WALL);
                        if (bore != null) return new FaceSelectionResult(baseFace, bore, h);
                    } else {
                        // Priority 1: Hole Wall (cylindrical surface visible from face plane)
                        topology_derived_face_ui_main wall = findDerivedFace(body, h.getId(), hole_region_kind_ui_main.HOLE_WALL);
                        if (wall != null) return new FaceSelectionResult(baseFace, wall, h);
                        // Priority 2: Hole Floor fallback
                        topology_derived_face_ui_main floor = findDerivedFace(body, h.getId(), hole_region_kind_ui_main.HOLE_FLOOR);
                        if (floor != null) return new FaceSelectionResult(baseFace, floor, h);
                    }
                }
            } else if (h.isThroughAll() && ui.workspace.shapes.holes.hole_intersection_helper_ui_main.isOpposingFace(h.getFaceKind(), faceKind)) {
                // Exit face of through hole
                double dist = Math.hypot(u - h.getU(), v - h.getV());
                if (dist <= h.getRadius() + 1e-3) {
                    topology_derived_face_ui_main bore = findDerivedFace(body, h.getId(), hole_region_kind_ui_main.BORE_WALL);
                    if (bore != null) return new FaceSelectionResult(baseFace, bore, h);
                }
            }
        }

        // Priority 3: Remaining face
        String remId = body.getId() + ":F:" + faceKind.name() + ":REMAINING";
        topology_derived_face_ui_main rem = body.getDerivedFaceById(remId);
        if (rem != null) return new FaceSelectionResult(baseFace, rem, null);

        // Priority 4: Base face fallback
        return new FaceSelectionResult(baseFace, null, null);
    }

    private static topology_derived_face_ui_main findDerivedFace(topology_body_ui_main body, String fid, hole_region_kind_ui_main rk) {
        for (topology_derived_face_ui_main df : body.getDerivedFaces()) {
            if (fid.equals(df.getCreatingFeatureId()) && df.getRegionKind() == rk) return df;
        }
        return null;
    }
}
