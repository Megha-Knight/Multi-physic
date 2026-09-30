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

            if (!faceHoles.isEmpty()) {
                if (faceHoles.size() == 1) {
                    buildFaceWithSingleHole(center, s, face, faceHoles.get(0), pts, fcs);
                } else {
                    buildFaceWithTwoHoles(center, s, face, faceHoles.get(0), faceHoles.get(1), pts, fcs);
                }
            } else if (!oppHoles.isEmpty()) {
                if (oppHoles.size() == 1) {
                    buildExitFaceWithSingleHole(center, s, face, oppHoles.get(0), pts, fcs);
                } else {
                    buildExitFaceWithTwoHoles(center, s, face, oppHoles.get(0), oppHoles.get(1), pts, fcs);
                }
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

    private static void buildFaceWithSingleHole(Point3D c, double s, face_kind_ui_main kind,
                                                hole_feature_ui_main hole, List<Float> pts, List<Integer> fcs) {
        Frame f = hole_mesh_triangulator_ui_main.getCubeFaceFrame(c, s, kind);
        double hw = s * 0.5;
        buildHoleSubRegion(f, s, hole, -hw, hw, -hw, hw, pts, fcs);
    }

    private static void buildFaceWithTwoHoles(Point3D c, double s, face_kind_ui_main kind,
                                              hole_feature_ui_main h1, hole_feature_ui_main h2,
                                              List<Float> pts, List<Integer> fcs) {
        Frame f = hole_mesh_triangulator_ui_main.getCubeFaceFrame(c, s, kind);
        double hw = s * 0.5;
        boolean splitU = Math.abs(h1.getU() - h2.getU()) >= Math.abs(h1.getV() - h2.getV());
        hole_feature_ui_main hA = (splitU ? h1.getU() <= h2.getU() : h1.getV() <= h2.getV()) ? h1 : h2;
        hole_feature_ui_main hB = (hA == h1) ? h2 : h1;

        double uMaxA = splitU ? (hA.getU() + hB.getU()) * 0.5 : hw;
        double vMaxA = splitU ? hw : (hA.getV() + hB.getV()) * 0.5;
        buildHoleSubRegion(f, s, hA, -hw, uMaxA, -hw, vMaxA, pts, fcs);
        buildHoleSubRegion(f, s, hB, splitU ? uMaxA : -hw, hw, splitU ? -hw : vMaxA, hw, pts, fcs);
    }

    private static void buildHoleSubRegion(Frame f, double s, hole_feature_ui_main h,
                                           double uMin, double uMax, double vMin, double vMax,
                                           List<Float> pts, List<Integer> fcs) {
        double r = h.getRadius();
        Point3D hc = f.origin().add(f.u().multiply(h.getU())).add(f.v().multiply(h.getV()));
        double depth = h.isThroughAll() ? s : Math.min(s, h.getDepth());

        Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, uMin, uMax, vMin, vMax);
        Point3D[] entry = hole_mesh_triangulator_ui_main.getCirclePoints(hc, f.u(), f.v(), r, SEGS);
        Point3D[] boreEnd = new Point3D[SEGS];
        for (int i = 0; i < SEGS; i++) boreEnd[i] = entry[i].subtract(f.n().multiply(depth));

        hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, entry, pts, fcs, false);
        hole_mesh_triangulator_ui_main.buildCylindricalWall(entry, boreEnd, pts, fcs);
        if (!h.isThroughAll()) {
            hole_mesh_triangulator_ui_main.buildCircleCap(hc.subtract(f.n().multiply(depth)), boreEnd, pts, fcs, true);
        }
    }

    private static void buildExitFaceWithSingleHole(Point3D c, double s, face_kind_ui_main kind,
                                                    hole_feature_ui_main hole, List<Float> pts, List<Integer> fcs) {
        Frame f = hole_mesh_triangulator_ui_main.getCubeFaceFrame(c, s, kind);
        double hw = s * 0.5;
        buildExitSubRegion(f, hole, -hw, hw, -hw, hw, pts, fcs);
    }

    private static void buildExitFaceWithTwoHoles(Point3D c, double s, face_kind_ui_main kind,
                                                  hole_feature_ui_main h1, hole_feature_ui_main h2,
                                                  List<Float> pts, List<Integer> fcs) {
        Frame f = hole_mesh_triangulator_ui_main.getCubeFaceFrame(c, s, kind);
        double hw = s * 0.5;
        boolean splitU = Math.abs(h1.getU() - h2.getU()) >= Math.abs(h1.getV() - h2.getV());
        hole_feature_ui_main hA = (splitU ? h1.getU() <= h2.getU() : h1.getV() <= h2.getV()) ? h1 : h2;
        hole_feature_ui_main hB = (hA == h1) ? h2 : h1;

        double uMaxA = splitU ? (hA.getU() + hB.getU()) * 0.5 : hw;
        double vMaxA = splitU ? hw : (hA.getV() + hB.getV()) * 0.5;
        buildExitSubRegion(f, hA, -hw, uMaxA, -hw, vMaxA, pts, fcs);
        buildExitSubRegion(f, hB, splitU ? uMaxA : -hw, hw, splitU ? -hw : vMaxA, hw, pts, fcs);
    }

    private static void buildExitSubRegion(Frame f, hole_feature_ui_main h, double uMin, double uMax, double vMin, double vMax,
                                           List<Float> pts, List<Integer> fcs) {
        Point3D hc = f.origin().add(f.u().multiply(h.getU())).add(f.v().multiply(-h.getV()));
        Point3D[] outer = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, uMin, uMax, vMin, vMax);
        Point3D[] exit = hole_mesh_triangulator_ui_main.getCirclePoints(hc, f.u(), f.v(), h.getRadius(), SEGS);
        hole_mesh_triangulator_ui_main.triangulateAnnularFace(outer, exit, pts, fcs, true);
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
}
