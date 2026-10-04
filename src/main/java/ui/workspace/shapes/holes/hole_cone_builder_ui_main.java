package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.shapes.primitives.mesh_helper_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * hole_cone_builder_ui_main.java
 * High-precision 3D mesh subtraction for Cones with parametric holes and patterns.
 */
public final class hole_cone_builder_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private hole_cone_builder_ui_main() {}

    public static Node buildConeWithHoles(Point3D center, Point3D current, List<hole_feature_ui_main> holes,
                                          boolean isPreview, boolean isSelected) {
        double r = center.distance(new Point3D(current.getX(), 0, current.getZ()));
        if (r < 0.2 || holes == null || holes.isEmpty()) {
            return shape_geometry_3d_ui_main.createCone(center, current, isPreview, isSelected);
        }

        double height = Math.abs(current.getY()) > 0.1 ? Math.abs(current.getY()) : Math.max(6.0, r * 2.0);
        List<hole_feature_ui_main> baseHoles = new ArrayList<>();
        List<hole_feature_ui_main> lateralHoles = new ArrayList<>();

        for (hole_feature_ui_main hole : holes) {
            if (hole != null && hole.isValid()) {
                if (hole.getFaceKind() == face_kind_ui_main.BASE_CAP) baseHoles.add(hole);
                else if (hole.getFaceKind() == face_kind_ui_main.CONE_LATERAL) lateralHoles.add(hole);
            }
        }

        if (baseHoles.isEmpty() && lateralHoles.isEmpty()) {
            return shape_geometry_3d_ui_main.createCone(center, current, isPreview, isSelected);
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();
        double cx = center.getX(), cz = center.getZ();

        int apexIdx = hole_mesh_triangulator_ui_main.addVertex(new Point3D(cx, -height, cz), pts);
        int[] botIdx = new int[SEGS];

        for (int i = 0; i < SEGS; i++) {
            double a = i * 2.0 * Math.PI / SEGS;
            botIdx[i] = hole_mesh_triangulator_ui_main.addVertex(new Point3D(cx + r * Math.cos(a), 0, cz + r * Math.sin(a)), pts);
        }

        for (int i = 0; i < SEGS; i++) {
            int next = (i + 1) % SEGS;
            hole_mesh_triangulator_ui_main.addTriIdx(apexIdx, botIdx[next], botIdx[i], fcs, false);
        }

        if (baseHoles.isEmpty()) {
            int baseCenterIdx = hole_mesh_triangulator_ui_main.addVertex(new Point3D(cx, 0, cz), pts);
            for (int i = 0; i < SEGS; i++) {
                int next = (i + 1) % SEGS;
                hole_mesh_triangulator_ui_main.addTriIdx(baseCenterIdx, botIdx[i], botIdx[next], fcs, false);
            }
        } else {
            hole_pattern_mesh_helper_ui_main.buildCylinderCapMultiHoles(cx, cz, 0.0, r, false, baseHoles, false, pts, fcs);
            for (hole_feature_ui_main hole : baseHoles) {
                double hx = cx + hole.getU(), hz = cz - hole.getV();
                hole_advanced_mesh_helper_ui_main.buildCylinderHoleCavity(cx, cz, hx, hz, 0.0, height, false, hole, holes, r, pts, fcs);
            }
        }

        float[] pa = new float[pts.size()];
        for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()];
        for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);

        mesh.getPoints().setAll(pa);
        mesh.getFaces().setAll(fa);
        int[] sga = new int[fa.length / 6];
        java.util.Arrays.fill(sga, 1);
        mesh.getFaceSmoothingGroups().setAll(sga);

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(isPreview, isSelected));
        return mv;
    }
}
