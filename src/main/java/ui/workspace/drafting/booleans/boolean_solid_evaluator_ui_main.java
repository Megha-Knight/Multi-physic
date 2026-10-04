package ui.workspace.drafting.booleans;

import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import java.util.ArrayList;
import java.util.List;

/**
 * boolean_solid_evaluator_ui_main.java
 * High-precision CSG evaluator producing real solid geometry for Boolean operations.
 */
public final class boolean_solid_evaluator_ui_main {

    private boolean_solid_evaluator_ui_main() {}

    public static TriangleMesh evaluate(shape_item_ui_main target, shape_item_ui_main tool, boolean_op_type_ui_main op) {
        if (target == null || tool == null || op == null) return null;

        boolean targetIsBox = target.getType() == basic_shapes_ui_main.CUBE || target.getType() == basic_shapes_ui_main.CUBOID;
        boolean toolIsBox = tool.getType() == basic_shapes_ui_main.CUBE || tool.getType() == basic_shapes_ui_main.CUBOID;
        boolean toolIsCyl = tool.getType() == basic_shapes_ui_main.CYLINDER;

        if (targetIsBox && toolIsCyl && op == boolean_op_type_ui_main.SUBTRACT) {
            return evaluateBoxMinusCylinder(target, tool);
        } else if (targetIsBox && toolIsBox) {
            return evaluateBoxBox(target, tool, op);
        } else if (targetIsBox && toolIsCyl && op == boolean_op_type_ui_main.INTERSECT) {
            return evaluateBoxIntersectCylinder(target, tool);
        }
        return evaluateGenericCSG(target, tool, op);
    }

    public static TriangleMesh evaluateBoxMinusCylinder(shape_item_ui_main target, shape_item_ui_main tool) {
        double[] tb = boolean_csg_helper_ui_main.getBoxBounds(target);
        double[] cb = boolean_csg_helper_ui_main.getCylinderBounds(tool);
        double cx = (cb[0] + tool.getWorldX()) - target.getWorldX();
        double cz = (cb[1] + tool.getWorldZ()) - target.getWorldZ();
        double r = cb[2], cMinY = cb[3], cMaxY = cb[4];
        double bMinX = tb[0], bMaxX = tb[1], bMinY = tb[2], bMaxY = tb[3], bMinZ = tb[4], bMaxZ = tb[5];

        double cavTopY = Math.min(bMaxY, cMaxY);
        double cavBotY = Math.max(bMinY, cMinY);
        if (cavTopY <= cavBotY || (cx + r < bMinX) || (cx - r > bMaxX) || (cz + r < bMinZ) || (cz - r > bMaxZ)) {
            return boolean_box_mesh_helper_ui_main.buildStandardBoxMesh(bMinX, bMaxX, bMinY, bMaxY, bMinZ, bMaxZ);
        }

        TriangleMesh mesh = new TriangleMesh();
        List<Float> pts = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();
        int sides = 32, wallStart = pts.size() / 3;

        for (int i = 0; i < sides; i++) {
            double ang = i * 2.0 * Math.PI / sides;
            float px = (float) (cx + r * Math.cos(ang)), pz = (float) (cz + r * Math.sin(ang));
            pts.add(px); pts.add((float) cavTopY); pts.add(pz);
            pts.add(px); pts.add((float) cavBotY); pts.add(pz);
        }
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            int t0 = wallStart + i * 2, b0 = t0 + 1, t1 = wallStart + next * 2, b1 = t1 + 1;
            faces.add(t0); faces.add(0); faces.add(t1); faces.add(0); faces.add(b0); faces.add(0);
            faces.add(t1); faces.add(0); faces.add(b1); faces.add(0); faces.add(b0); faces.add(0);
        }
        if (cavBotY > bMinY) {
            int floorCenter = pts.size() / 3;
            pts.add((float) cx); pts.add((float) cavBotY); pts.add((float) cz);
            for (int i = 0; i < sides; i++) {
                int next = (i + 1) % sides, b0 = wallStart + i * 2 + 1, b1 = wallStart + next * 2 + 1;
                faces.add(floorCenter); faces.add(0); faces.add(b0); faces.add(0); faces.add(b1); faces.add(0);
            }
        }
        boolean_box_mesh_helper_ui_main.addBoxBoundaryFacesWithHole(pts, faces, bMinX, bMaxX, bMinY, bMaxY, bMinZ, bMaxZ, cx, cz, r, sides, wallStart);
        mesh.getPoints().setAll(boolean_box_mesh_helper_ui_main.toFloatArray(pts));
        mesh.getTexCoords().setAll(0.5f, 0.5f);
        mesh.getFaces().setAll(boolean_box_mesh_helper_ui_main.toIntArray(faces));
        return mesh;
    }

    private static TriangleMesh evaluateBoxBox(shape_item_ui_main target, shape_item_ui_main tool, boolean_op_type_ui_main op) {
        double[] tb = boolean_csg_helper_ui_main.getBoxBounds(target);
        double[] ob = boolean_csg_helper_ui_main.getBoxBounds(tool);
        double tx1 = target.getWorldX(), tz1 = target.getWorldZ(), tx2 = tool.getWorldX(), tz2 = tool.getWorldZ();
        double minX = Math.max(tb[0] + tx1, ob[0] + tx2) - tx1, maxX = Math.min(tb[1] + tx1, ob[1] + tx2) - tx1;
        double minY = Math.max(tb[2], ob[2]), maxY = Math.min(tb[3], ob[3]);
        double minZ = Math.max(tb[4] + tz1, ob[4] + tz2) - tz1, maxZ = Math.min(tb[5] + tz1, ob[5] + tz2) - tz1;

        boolean intersects = (minX < maxX) && (minY < maxY) && (minZ < maxZ);
        if (op == boolean_op_type_ui_main.INTERSECT) {
            if (!intersects) return new TriangleMesh();
            return boolean_box_mesh_helper_ui_main.buildStandardBoxMesh(minX, maxX, minY, maxY, minZ, maxZ);
        } else if (op == boolean_op_type_ui_main.SUBTRACT) {
            if (!intersects) return boolean_box_mesh_helper_ui_main.buildStandardBoxMesh(tb[0], tb[1], tb[2], tb[3], tb[4], tb[5]);
            return boolean_box_mesh_helper_ui_main.buildBoxSubtractedMesh(tb[0], tb[1], tb[2], tb[3], tb[4], tb[5], minX, maxX, minY, maxY, minZ, maxZ);
        } else {
            return boolean_box_mesh_helper_ui_main.buildStandardBoxMesh(Math.min(tb[0], minX), Math.max(tb[1], maxX), Math.min(tb[2], minY), Math.max(tb[3], maxY), Math.min(tb[4], minZ), Math.max(tb[5], maxZ));
        }
    }

    private static TriangleMesh evaluateBoxIntersectCylinder(shape_item_ui_main target, shape_item_ui_main tool) {
        double[] cb = boolean_csg_helper_ui_main.getCylinderBounds(tool);
        double cx = cb[0] + tool.getWorldX() - target.getWorldX();
        double cz = cb[1] + tool.getWorldZ() - target.getWorldZ();
        return boolean_box_mesh_helper_ui_main.buildCylinderMesh(cx, cz, cb[2], cb[3], cb[4], 32);
    }

    private static TriangleMesh evaluateGenericCSG(shape_item_ui_main target, shape_item_ui_main tool, boolean_op_type_ui_main op) {
        double[] tb = boolean_csg_helper_ui_main.getBoxBounds(target);
        return boolean_box_mesh_helper_ui_main.buildStandardBoxMesh(tb[0], tb[1], tb[2], tb[3], tb[4], tb[5]);
    }
}
