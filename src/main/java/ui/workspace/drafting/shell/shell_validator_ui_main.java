package ui.workspace.drafting.shell;

import javafx.geometry.Point3D;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.Collection;
import java.util.Set;

/**
 * shell_validator_ui_main.java
 * Validates parametric shell configurations against body geometry and topology.
 */
public final class shell_validator_ui_main {

    private shell_validator_ui_main() {}

    public static shell_validation_result_ui_main validate(shape_item_ui_main host,
                                                           double thickness,
                                                           shell_direction_ui_main direction,
                                                           Collection<face_kind_ui_main> removedFaces) {
        if (host == null) {
            return shell_validation_result_ui_main.failure("Host solid body cannot be null.");
        }
        if (host.getType() != basic_shapes_ui_main.CUBE && host.getType() != basic_shapes_ui_main.CUBOID) {
            return shell_validation_result_ui_main.failure("Shelling is currently only supported on planar solid bodies.");
        }
        if (Double.isNaN(thickness) || Double.isInfinite(thickness) || thickness <= 0.0) {
            return shell_validation_result_ui_main.failure("Thickness must be a finite positive value > 0.");
        }
        if (direction == null) {
            return shell_validation_result_ui_main.failure("Shell direction cannot be null.");
        }

        Point3D p1 = host.getP1(), p2 = host.getP2();
        if (p1 == null || p2 == null) {
            return shell_validation_result_ui_main.failure("Host shape bounds are undefined.");
        }

        double w, h, d;
        if (host.getType() == basic_shapes_ui_main.CUBE) {
            double sz = Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ()));
            w = sz; h = sz; d = sz;
        } else {
            w = Math.max(0.1, Math.abs(p2.getX() - p1.getX()));
            d = Math.max(0.1, Math.abs(p2.getZ() - p1.getZ()));
            h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
        }

        if (direction.isInward()) {
            double minDim = Math.min(w, Math.min(d, h));
            if (thickness * 2.0 >= minDim) {
                return shell_validation_result_ui_main.failure("Requested thickness (" + thickness + ") causes geometric collapse on body of dimension " + minDim + ".");
            }
        }

        if (removedFaces != null && removedFaces.size() >= 6) {
            return shell_validation_result_ui_main.failure("Cannot remove all faces of a solid body.");
        }

        if (removedFaces != null) {
            Set<face_kind_ui_main> validKinds = Set.of(
                face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM,
                face_kind_ui_main.FRONT, face_kind_ui_main.BACK,
                face_kind_ui_main.LEFT, face_kind_ui_main.RIGHT
            );
            for (face_kind_ui_main f : removedFaces) {
                if (f == null || !validKinds.contains(f)) {
                    return shell_validation_result_ui_main.failure("Invalid face kind for planar solid shell: " + f);
                }
            }
        }

        return shell_validation_result_ui_main.success();
    }
}
