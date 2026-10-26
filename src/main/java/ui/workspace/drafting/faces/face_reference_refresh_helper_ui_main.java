package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import java.util.ArrayList;
import java.util.List;
import ui.workspace.drafting.shape_item_ui_main;
import ui.ui_feature_action_helper_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;

public final class face_reference_refresh_helper_ui_main {
    private face_reference_refresh_helper_ui_main() {}

    public static face_reference_ui_main refresh(face_reference_ui_main prev, shape_item_ui_main shape) {
        if (prev == null || !ui_feature_action_helper_ui_main.isMachinableSolid(shape)) return null;
        var ref = ui.workspace.drafting.topology.topology_geometry_helper_ui_main.reconstructFaceReference(
            shape, prev.getFaceKind(), prev.getLocalHitU(), prev.getLocalHitV(), prev.getTargetFeatureId()
        );
        if (ref != null && (prev.getFaceKind() == face_kind_ui_main.EXTRUDE_LATERAL || prev.getFaceKind() == face_kind_ui_main.CYLINDER_LATERAL) && prev.getWorldNormal() != null) {
            List<Point3D> polygon = prev.getFacePolygon();
            if (prev.getTargetFacePart() != null) {
                var ext = shape.getExtrusions().stream().filter(e -> e.getId().equals(prev.getTargetFeatureId())).findFirst().orElse(null);
                List<Point3D> localPolygon = extrude_face_picker_ui_main.resolveLocalPolygon(shape, ext, prev.getTargetFacePart());
                if (!localPolygon.isEmpty()) {
                    polygon = new ArrayList<>();
                    for (Point3D point : localPolygon) polygon.add(shape_rotation_helper_ui_main.transformPoint(point,
                        shape.getCenter(), shape.getRotationX(), shape.getRotationY(), shape.getWorldX(), shape.getWorldY(), shape.getWorldZ()));
                }
            }
            return new face_reference_ui_main(
                ref.getOwnerShapeId(), ref.getTargetFeatureId(), ref.getFaceKind(),
                ref.getWorldHitPoint(), prev.getWorldNormal(), ref.getFaceOrigin(),
                ref.getUAxis(), ref.getVAxis(), ref.getLocalHitU(), ref.getLocalHitV(),
                ref.getFaceWidth(), ref.getFaceHeight(), prev.getTargetFacePart(), polygon
            );
        }
        if (ref != null && prev.getTargetFacePart() != null) {
            var ext = shape.getExtrusions().stream().filter(e -> e.getId().equals(prev.getTargetFeatureId())).findFirst().orElse(null);
            List<Point3D> localPolygon = extrude_face_picker_ui_main.resolveLocalPolygon(shape, ext, prev.getTargetFacePart());
            if (!localPolygon.isEmpty()) {
                List<Point3D> polygon = new ArrayList<>();
                for (Point3D point : localPolygon) polygon.add(shape_rotation_helper_ui_main.transformPoint(point,
                    shape.getCenter(), shape.getRotationX(), shape.getRotationY(), shape.getWorldX(), shape.getWorldY(), shape.getWorldZ()));
                return new face_reference_ui_main(ref.getOwnerShapeId(), ref.getTargetFeatureId(), ref.getFaceKind(),
                    ref.getWorldHitPoint(), ref.getWorldNormal(), ref.getFaceOrigin(), ref.getUAxis(), ref.getVAxis(),
                    ref.getLocalHitU(), ref.getLocalHitV(), ref.getFaceWidth(), ref.getFaceHeight(), prev.getTargetFacePart(), polygon);
            }
        }
        return ref;
    }

}
