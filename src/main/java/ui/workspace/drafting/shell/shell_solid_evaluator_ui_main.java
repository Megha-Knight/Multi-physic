package ui.workspace.drafting.shell;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * shell_solid_evaluator_ui_main.java
 * Evaluates parametric thin-walled shell geometry for planar solids.
 */
public final class shell_solid_evaluator_ui_main {

    private shell_solid_evaluator_ui_main() {}

    public static TriangleMesh evaluate(shape_item_ui_main shape, shell_feature_ui_main shell) {
        if (shape == null || shell == null || !shell.isValid()) return null;
        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        if (p1 == null || p2 == null) return null;

        double w, h, d, cx, cz;
        if (shape.getType() == basic_shapes_ui_main.CUBE) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), sz = Math.max(Math.abs(dx), Math.abs(dz));
            w = sz; h = sz; d = sz;
            cx = p1.getX() + (dx >= 0 ? sz * 0.5 : -sz * 0.5); cz = p1.getZ() + (dz >= 0 ? sz * 0.5 : -sz * 0.5);
        } else {
            w = Math.max(0.1, Math.abs(p2.getX() - p1.getX())); d = Math.max(0.1, Math.abs(p2.getZ() - p1.getZ()));
            h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            cx = (p1.getX() + p2.getX()) * 0.5; cz = (p1.getZ() + p2.getZ()) * 0.5;
        }

        double x0 = cx - w * 0.5, x1 = cx + w * 0.5, y0 = 0.0, y1 = -h, z0 = cz - d * 0.5, z1 = cz + d * 0.5;
        double t = shell.getThickness();
        double bx0, bx1, by0, by1, bz0, bz1, ix0, ix1, iy0, iy1, iz0, iz1;

        if (shell.getDirection().isInward()) {
            bx0 = x0; bx1 = x1; by0 = y0; by1 = y1; bz0 = z0; bz1 = z1;
            ix0 = x0 + (shell.hasRemovedFace(face_kind_ui_main.LEFT) ? 0.0 : t);
            ix1 = x1 - (shell.hasRemovedFace(face_kind_ui_main.RIGHT) ? 0.0 : t);
            iy0 = y0 - (shell.hasRemovedFace(face_kind_ui_main.BOTTOM) ? 0.0 : t);
            iy1 = y1 + (shell.hasRemovedFace(face_kind_ui_main.TOP) ? 0.0 : t);
            iz0 = z0 + (shell.hasRemovedFace(face_kind_ui_main.BACK) ? 0.0 : t);
            iz1 = z1 - (shell.hasRemovedFace(face_kind_ui_main.FRONT) ? 0.0 : t);
        } else {
            bx0 = x0 - (shell.hasRemovedFace(face_kind_ui_main.LEFT) ? 0.0 : t);
            bx1 = x1 + (shell.hasRemovedFace(face_kind_ui_main.RIGHT) ? 0.0 : t);
            by0 = y0 + (shell.hasRemovedFace(face_kind_ui_main.BOTTOM) ? 0.0 : t);
            by1 = y1 - (shell.hasRemovedFace(face_kind_ui_main.TOP) ? 0.0 : t);
            bz0 = z0 - (shell.hasRemovedFace(face_kind_ui_main.BACK) ? 0.0 : t);
            bz1 = z1 + (shell.hasRemovedFace(face_kind_ui_main.FRONT) ? 0.0 : t);
            ix0 = x0; ix1 = x1; iy0 = y0; iy1 = y1; iz0 = z0; iz1 = z1;
        }

        List<Float> pts = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();

        // 1. Retained Outer Faces
        if (!shell.hasRemovedFace(face_kind_ui_main.BOTTOM)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, bz1, bx1, by0, bz1, bx1, by0, bz0, bx0, by0, bz0);
        if (!shell.hasRemovedFace(face_kind_ui_main.TOP)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by1, bz0, bx1, by1, bz0, bx1, by1, bz1, bx0, by1, bz1);
        if (!shell.hasRemovedFace(face_kind_ui_main.FRONT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, bz1, bx1, by0, bz1, bx1, by1, bz1, bx0, by1, bz1);
        if (!shell.hasRemovedFace(face_kind_ui_main.BACK)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx1, by0, bz0, bx0, by0, bz0, bx0, by1, bz0, bx1, by1, bz0);
        if (!shell.hasRemovedFace(face_kind_ui_main.LEFT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, bz0, bx0, by0, bz1, bx0, by1, bz1, bx0, by1, bz0);
        if (!shell.hasRemovedFace(face_kind_ui_main.RIGHT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx1, by0, bz1, bx1, by0, bz0, bx1, by1, bz0, bx1, by1, bz1);

        // 2. Retained Inner Faces
        if (!shell.hasRemovedFace(face_kind_ui_main.BOTTOM)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix0, iy0, iz0, ix1, iy0, iz0, ix1, iy0, iz1, ix0, iy0, iz1);
        if (!shell.hasRemovedFace(face_kind_ui_main.TOP)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix0, iy1, iz1, ix1, iy1, iz1, ix1, iy1, iz0, ix0, iy1, iz0);
        if (!shell.hasRemovedFace(face_kind_ui_main.FRONT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix0, iy1, iz1, ix1, iy1, iz1, ix1, iy0, iz1, ix0, iy0, iz1);
        if (!shell.hasRemovedFace(face_kind_ui_main.BACK)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix1, iy1, iz0, ix0, iy1, iz0, ix0, iy0, iz0, ix1, iy0, iz0);
        if (!shell.hasRemovedFace(face_kind_ui_main.LEFT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix0, iy1, iz0, ix0, iy1, iz1, ix0, iy0, iz1, ix0, iy0, iz0);
        if (!shell.hasRemovedFace(face_kind_ui_main.RIGHT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix1, iy1, iz1, ix1, iy1, iz0, ix1, iy0, iz0, ix1, iy0, iz1);

        // 3. Open Rim / Wall Connectors
        buildOpenRims(pts, faces, shell, bx0, bx1, by0, by1, bz0, bz1, ix0, ix1, iy0, iy1, iz0, iz1);

        return shell_mesh_quad_helper_ui_main.toMesh(pts, faces);
    }

    private static void buildOpenRims(List<Float> pts, List<Integer> faces, shell_feature_ui_main shell,
                                      double bx0, double bx1, double by0, double by1, double bz0, double bz1,
                                      double ix0, double ix1, double iy0, double iy1, double iz0, double iz1) {
        if (shell.hasRemovedFace(face_kind_ui_main.TOP)) {
            if (!shell.hasRemovedFace(face_kind_ui_main.BACK)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by1, bz0, bx1, by1, bz0, bx1, by1, iz0, bx0, by1, iz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.FRONT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by1, iz1, bx1, by1, iz1, bx1, by1, bz1, bx0, by1, bz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.LEFT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by1, iz0, ix0, by1, iz0, ix0, by1, iz1, bx0, by1, iz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.RIGHT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix1, by1, iz0, bx1, by1, iz0, bx1, by1, iz1, ix1, by1, iz1);
        }
        if (shell.hasRemovedFace(face_kind_ui_main.BOTTOM)) {
            if (!shell.hasRemovedFace(face_kind_ui_main.BACK)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, iz0, bx1, by0, iz0, bx1, by0, bz0, bx0, by0, bz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.FRONT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, bz1, bx1, by0, bz1, bx1, by0, iz1, bx0, by0, iz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.LEFT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, iz1, ix0, by0, iz1, ix0, by0, iz0, bx0, by0, iz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.RIGHT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix1, by0, iz1, bx1, by0, iz1, bx1, by0, iz0, ix1, by0, iz0);
        }
        if (shell.hasRemovedFace(face_kind_ui_main.FRONT)) {
            if (!shell.hasRemovedFace(face_kind_ui_main.LEFT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, bz1, ix0, by0, bz1, ix0, by1, bz1, bx0, by1, bz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.RIGHT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix1, by0, bz1, bx1, by0, bz1, bx1, by1, bz1, ix1, by1, bz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.BOTTOM)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix0, by0, bz1, ix1, by0, bz1, ix1, iy0, bz1, ix0, iy0, bz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.TOP)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix0, iy1, bz1, ix1, iy1, bz1, ix1, by1, bz1, ix0, by1, bz1);
        }
        if (shell.hasRemovedFace(face_kind_ui_main.BACK)) {
            if (!shell.hasRemovedFace(face_kind_ui_main.LEFT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix0, by0, bz0, bx0, by0, bz0, bx0, by1, bz0, ix0, by1, bz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.RIGHT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx1, by0, bz0, ix1, by0, bz0, ix1, by1, bz0, bx1, by1, bz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.BOTTOM)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix1, by0, bz0, ix0, by0, bz0, ix0, iy0, bz0, ix1, iy0, bz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.TOP)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, ix1, iy1, bz0, ix0, iy1, bz0, ix0, by1, bz0, ix1, by1, bz0);
        }
        if (shell.hasRemovedFace(face_kind_ui_main.LEFT)) {
            if (!shell.hasRemovedFace(face_kind_ui_main.FRONT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, bz1, bx0, by0, iz1, bx0, by1, iz1, bx0, by1, bz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.BACK)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, iz0, bx0, by0, bz0, bx0, by1, bz0, bx0, by1, iz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.BOTTOM)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, by0, iz1, bx0, by0, iz0, bx0, iy0, iz0, bx0, iy0, iz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.TOP)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx0, iy1, iz1, bx0, iy1, iz0, bx0, by1, iz0, bx0, by1, iz1);
        }
        if (shell.hasRemovedFace(face_kind_ui_main.RIGHT)) {
            if (!shell.hasRemovedFace(face_kind_ui_main.FRONT)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx1, by0, iz1, bx1, by0, bz1, bx1, by1, bz1, bx1, by1, iz1);
            if (!shell.hasRemovedFace(face_kind_ui_main.BACK)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx1, by0, bz0, bx1, by0, iz0, bx1, by1, iz0, bx1, by1, bz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.BOTTOM)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx1, by0, iz0, bx1, by0, iz1, bx1, iy0, iz1, bx1, iy0, iz0);
            if (!shell.hasRemovedFace(face_kind_ui_main.TOP)) shell_mesh_quad_helper_ui_main.addQuad(pts, faces, bx1, iy1, iz0, bx1, iy1, iz1, bx1, by1, iz1, bx1, by1, iz0);
        }
    }
}
