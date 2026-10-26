package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_face_ui_main;

/**
 * draft_mesh_builder_ui_main.java
 * Generates 3D tapered mesh geometry for parametric face draft features.
 */
public final class draft_mesh_builder_ui_main {

    private draft_mesh_builder_ui_main() {}

    public static Node buildDraftNode(shape_item_ui_main shape, draft_feature_ui_main dr, boolean isSelected) {
        if (shape == null || dr == null || !dr.isValid()) return null;
        topology_body_ui_main topo = shape.getTopology();
        if (topo == null) return null;
        topology_face_ui_main face = topo.getFace(dr.getFaceKind());
        if (face == null) return null;

        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        double w, h, d, cx, cz;
        if (shape.getType() == ui.workspace.shapes.basic_shapes_ui_main.CUBE) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), s = Math.max(Math.abs(dx), Math.abs(dz));
            w = s; h = s; d = s;
            cx = p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5); cz = p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5);
        } else {
            w = Math.max(0.1, Math.abs(p2.getX() - p1.getX())); d = Math.max(0.1, Math.abs(p2.getZ() - p1.getZ()));
            h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            cx = (p1.getX() + p2.getX()) * 0.5; cz = (p1.getZ() + p2.getZ()) * 0.5;
        }
        double y0 = p1.getY(), y1 = y0 - h;
        double hw = w * 0.5, hd = d * 0.5;

        Point3D nFace = draft_feature_ui_main.getFaceNormal(dr.getFaceKind());
        Point3D nNeutral = draft_feature_ui_main.getFaceNormal(dr.getNeutralKind());
        if (nFace == null || nNeutral == null) return null;

        Point3D pNeutral = getNeutralPoint(cx, cz, y0, y1, hw, hd, dr.getNeutralKind());
        Point3D dPull = dr.getPullDirection() != null ? dr.getPullDirection().normalize() : nNeutral.multiply(-1.0);
        double denom = dPull.dotProduct(nNeutral);
        if (Math.abs(denom) < 1e-6) return null;
        double tanTheta = Math.tan(Math.toRadians(dr.getDraftAngle()));

        Point3D[] baseQuad = getBaseFaceQuad(cx, cz, y0, y1, hw, hd, dr.getFaceKind());
        if (baseQuad == null) return null;

        Point3D[] drafted = new Point3D[4];
        for (int i = 0; i < 4; i++) {
            Point3D v = baseQuad[i];
            double num = v.subtract(pNeutral).dotProduct(nNeutral);
            double sDist = num / denom;
            double delta = sDist * tanTheta;
            drafted[i] = v.subtract(nFace.multiply(delta));
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        float[] pts = new float[]{
            (float) drafted[0].getX(), (float) drafted[0].getY(), (float) drafted[0].getZ(),
            (float) drafted[1].getX(), (float) drafted[1].getY(), (float) drafted[1].getZ(),
            (float) drafted[2].getX(), (float) drafted[2].getY(), (float) drafted[2].getZ(),
            (float) drafted[3].getX(), (float) drafted[3].getY(), (float) drafted[3].getZ()
        };
        mesh.getPoints().setAll(pts);
        int[] fcs = new int[]{ 0, 0, 3, 3, 2, 2,  0, 0, 2, 2, 1, 1 };
        mesh.getFaces().setAll(fcs);
        int[] sg = new int[]{1, 1};
        mesh.getFaceSmoothingGroups().setAll(sg);

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        Color c = isSelected ? Color.web("#38BDF8") : Color.web("#8B5CF6");
        PhongMaterial mat = new PhongMaterial(c);
        mat.setSpecularColor(Color.color(1, 1, 1, 0.5));
        mv.setMaterial(mat);
        mv.setId(dr.getId());
        return mv;
    }

    public static Point3D getNeutralPoint(double cx, double cz, double y0, double y1, double hw, double hd, face_kind_ui_main kind) {
        return switch (kind) {
            case TOP, TOP_CAP -> new Point3D(cx, y1, cz);
            case BOTTOM, BOTTOM_CAP, BASE_CAP -> new Point3D(cx, y0, cz);
            case FRONT -> new Point3D(cx, (y0 + y1) * 0.5, cz + hd);
            case BACK -> new Point3D(cx, (y0 + y1) * 0.5, cz - hd);
            case LEFT -> new Point3D(cx - hw, (y0 + y1) * 0.5, cz);
            case RIGHT -> new Point3D(cx + hw, (y0 + y1) * 0.5, cz);
            default -> new Point3D(cx, y0, cz);
        };
    }

    private static Point3D[] getBaseFaceQuad(double cx, double cz, double y0, double y1, double hw, double hd, face_kind_ui_main kind) {
        return switch (kind) {
            case FRONT -> new Point3D[]{
                new Point3D(cx - hw, y0, cz + hd), new Point3D(cx + hw, y0, cz + hd),
                new Point3D(cx + hw, y1, cz + hd), new Point3D(cx - hw, y1, cz + hd)
            };
            case BACK -> new Point3D[]{
                new Point3D(cx + hw, y0, cz - hd), new Point3D(cx - hw, y0, cz - hd),
                new Point3D(cx - hw, y1, cz - hd), new Point3D(cx + hw, y1, cz - hd)
            };
            case LEFT -> new Point3D[]{
                new Point3D(cx - hw, y0, cz - hd), new Point3D(cx - hw, y0, cz + hd),
                new Point3D(cx - hw, y1, cz + hd), new Point3D(cx - hw, y1, cz - hd)
            };
            case RIGHT -> new Point3D[]{
                new Point3D(cx + hw, y0, cz + hd), new Point3D(cx + hw, y0, cz - hd),
                new Point3D(cx + hw, y1, cz - hd), new Point3D(cx + hw, y1, cz + hd)
            };
            case TOP -> new Point3D[]{
                new Point3D(cx - hw, y1, cz - hd), new Point3D(cx + hw, y1, cz - hd),
                new Point3D(cx + hw, y1, cz + hd), new Point3D(cx - hw, y1, cz + hd)
            };
            case BOTTOM -> new Point3D[]{
                new Point3D(cx - hw, y0, cz + hd), new Point3D(cx + hw, y0, cz + hd),
                new Point3D(cx + hw, y0, cz - hd), new Point3D(cx - hw, y0, cz - hd)
            };
            default -> null;
        };
    }
}
