package ui.workspace.shapes.holes;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * hole_mesh_builder_ui_main.java
 * High-precision CSG-grade 3D mesh subtraction engine for CAD solids with parametric holes.
 */
public final class hole_mesh_builder_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private hole_mesh_builder_ui_main() {}

    public static Node buildCylinderWithHoles(Point3D p1, Point3D p2, List<hole_feature_ui_main> holes,
                                             boolean isPreview, boolean isSelected) {
        return hole_cylinder_builder_ui_main.buildCylinderWithHoles(p1, p2, holes, isPreview, isSelected);
    }

    public static Node buildCuboidWithHoles(Point3D p1, Point3D p2, List<hole_feature_ui_main> holes,
                                            boolean isPreview, boolean isSelected) {
        return hole_cuboid_builder_ui_main.buildCuboidWithHoles(p1, p2, holes, isPreview, isSelected);
    }

    public static Node buildCubeWithHoles(Point3D p1, Point3D p2, List<hole_feature_ui_main> holes,
                                         boolean isPreview, boolean isSelected) {
        double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), s = Math.max(Math.abs(dx), Math.abs(dz));
        if (s < 0.2) return shape_geometry_3d_ui_main.createCube(p1, p2, isPreview, isSelected);

        double cx = p1.getX() + (dx >= 0 ? s * 0.5 : -s * 0.5), cz = p1.getZ() + (dz >= 0 ? s * 0.5 : -s * 0.5);
        Point3D center = new Point3D(cx, -s * 0.5, cz);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        for (face_kind_ui_main face : new face_kind_ui_main[]{
            face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM, face_kind_ui_main.FRONT,
            face_kind_ui_main.BACK, face_kind_ui_main.RIGHT, face_kind_ui_main.LEFT
        }) {
            List<hole_feature_ui_main> faceHoles = findHolesForFace(holes, face, s);
            List<hole_feature_ui_main> oppHoles = findThroughHolesForOppositeFace(holes, face, s);

            if (!faceHoles.isEmpty() || !oppHoles.isEmpty()) {
                Frame f = hole_mesh_triangulator_ui_main.getCubeFaceFrame(center, s, face);
                face_kind_ui_main opp = getOppositeFace(face);
                Frame fOpp = hole_mesh_triangulator_ui_main.getCubeFaceFrame(center, s, opp);
                hole_stepped_helper_ui_main.buildCubeFaceWithClusters(f, fOpp, s, faceHoles, oppHoles, holes, center, pts, fcs);
            } else {
                buildStandardQuadFace(center, s, face, pts, fcs);
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

    private static List<hole_feature_ui_main> findHolesForFace(List<hole_feature_ui_main> holes, face_kind_ui_main face, double s) {
        List<hole_feature_ui_main> res = new ArrayList<>();
        if (holes == null) return res;
        for (hole_feature_ui_main h : holes) {
            if (h != null && h.getFaceKind() == face && h.isValid() && h.fitsWithinFace(s, s)) res.add(h);
        }
        return res;
    }

    private static List<hole_feature_ui_main> findThroughHolesForOppositeFace(List<hole_feature_ui_main> holes, face_kind_ui_main face, double s) {
        List<hole_feature_ui_main> res = new ArrayList<>();
        if (holes == null) return res;
        for (hole_feature_ui_main h : holes) {
            if (h != null && h.isValid() && h.isThroughAll() && hole_mesh_triangulator_ui_main.isOpposite(h.getFaceKind(), face) && h.fitsWithinFace(s, s)) {
                res.add(h);
            }
        }
        return res;
    }

    private static void buildStandardQuadFace(Point3D c, double s, face_kind_ui_main kind, List<Float> pts, List<Integer> fcs) {
        Frame f = hole_mesh_triangulator_ui_main.getCubeFaceFrame(c, s, kind);
        double hw = s * 0.5;
        Point3D p0 = f.origin().subtract(f.u().multiply(hw)).subtract(f.v().multiply(hw));
        Point3D p1 = f.origin().add(f.u().multiply(hw)).subtract(f.v().multiply(hw));
        Point3D p2 = f.origin().add(f.u().multiply(hw)).add(f.v().multiply(hw));
        Point3D p3 = f.origin().subtract(f.u().multiply(hw)).add(f.v().multiply(hw));
        hole_mesh_triangulator_ui_main.addQuad(p0, p1, p2, p3, pts, fcs);
    }

    private static face_kind_ui_main getOppositeFace(face_kind_ui_main f) {
        return switch (f) {
            case TOP, TOP_CAP -> face_kind_ui_main.BOTTOM;
            case BOTTOM, BOTTOM_CAP -> face_kind_ui_main.TOP;
            case FRONT -> face_kind_ui_main.BACK;
            case BACK -> face_kind_ui_main.FRONT;
            case RIGHT -> face_kind_ui_main.LEFT;
            case LEFT -> face_kind_ui_main.RIGHT;
            default -> face_kind_ui_main.TOP;
        };
    }
}
