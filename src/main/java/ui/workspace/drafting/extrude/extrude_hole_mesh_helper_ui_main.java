package ui.workspace.drafting.extrude;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.util.ArrayList;
import java.util.List;

public final class extrude_hole_mesh_helper_ui_main {

    private static final int SEGS = hole_mesh_triangulator_ui_main.CIRCLE_SEGS;

    private extrude_hole_mesh_helper_ui_main() {}

    public static Node buildExtrudeWithHoles(Frame f, extrude_feature_ui_main ext,
                                            List<hole_feature_ui_main> holes, PhongMaterial mat) {
        if (f == null || ext == null || holes == null || holes.isEmpty()) return null;

        Point3D n = f.n(), u = f.u(), v = f.v();
        Point3D baseCenter = f.origin().add(u.multiply(ext.getU())).add(v.multiply(ext.getV()));
        double len = ext.getHeight();
        Point3D capCenter = baseCenter.add(n.multiply(len));

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        if (ext.getProfileShape() == CutoutShape.CIRCLE) {
            buildCircularExtrudeWithHoles(baseCenter, capCenter, n, u, v, ext.getRadius(), len, holes, pts, fcs);
        } else {
            double w = ext.getDiameter(), d = (ext.getWidth2() > 0.1) ? ext.getWidth2() : w;
            buildRectExtrudeWithHoles(baseCenter, capCenter, n, u, v, w, d, len, holes, pts, fcs);
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
        mv.setMaterial(mat);
        return mv;
    }

    private static void buildCircularExtrudeWithHoles(Point3D base, Point3D cap, Point3D n, Point3D u, Point3D v,
                                                     double r, double len, List<hole_feature_ui_main> holes,
                                                     List<Float> pts, List<Integer> fcs) {
        Point3D[] basePerim = hole_mesh_triangulator_ui_main.getCirclePoints(base, u, v, r, SEGS);
        Point3D[] capPerim = hole_mesh_triangulator_ui_main.getCirclePoints(cap, u, v, r, SEGS);

        hole_mesh_triangulator_ui_main.buildCircleCap(base, basePerim, pts, fcs, false);
        hole_mesh_triangulator_ui_main.buildCylindricalWall(capPerim, basePerim, pts, fcs);

        for (hole_feature_ui_main h : holes) {
            Point3D hCap = cap.add(u.multiply(h.getU())).add(v.multiply(h.getV()));
            double hr = h.getRadius();
            Point3D[] hPerim = ui.workspace.shapes.holes.hole_profile_helper_ui_main.getProfilePoints(hCap, u, v, h, hr, SEGS);
            for (int i = 0; i < SEGS; i++) {
                int next = (i + 1) % SEGS;
                hole_mesh_triangulator_ui_main.addQuad(capPerim[i], capPerim[next], hPerim[next], hPerim[i], pts, fcs);
            }
            double depth = h.isThroughAll() ? len : Math.min(len, h.getDepth());
            Point3D hFloor = hCap.subtract(n.multiply(depth));
            Point3D[] floorPerim = ui.workspace.shapes.holes.hole_profile_helper_ui_main.getProfilePoints(hFloor, u, v, h, hr, SEGS);
            hole_mesh_triangulator_ui_main.buildCylindricalWall(floorPerim, hPerim, pts, fcs);
            if (!h.isThroughAll() && depth < len) {
                hole_mesh_triangulator_ui_main.buildCircleCap(hFloor, floorPerim, pts, fcs, true);
            }
        }
    }

    private static void buildRectExtrudeWithHoles(Point3D base, Point3D cap, Point3D n, Point3D u, Point3D v,
                                                  double w, double d, double len, List<hole_feature_ui_main> holes,
                                                  List<Float> pts, List<Integer> fcs) {
        double hw = w * 0.5, hd = d * 0.5;
        Point3D[] b = new Point3D[]{
            base.add(u.multiply(-hw)).add(v.multiply(-hd)), base.add(u.multiply(hw)).add(v.multiply(-hd)),
            base.add(u.multiply(hw)).add(v.multiply(hd)), base.add(u.multiply(-hw)).add(v.multiply(hd))
        };
        Point3D[] c = new Point3D[]{
            cap.add(u.multiply(-hw)).add(v.multiply(-hd)), cap.add(u.multiply(hw)).add(v.multiply(-hd)),
            cap.add(u.multiply(hw)).add(v.multiply(hd)), cap.add(u.multiply(-hw)).add(v.multiply(hd))
        };
        hole_mesh_triangulator_ui_main.addQuad(b[0], b[3], b[2], b[1], pts, fcs);
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            hole_mesh_triangulator_ui_main.addQuad(c[i], c[next], b[next], b[i], pts, fcs);
        }
        Frame fCap = new Frame(cap, n, u, v);
        Point3D[] oct = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(fCap, -hw, hw, -hd, hd);
        for (hole_feature_ui_main h : holes) {
            Point3D hCap = cap.add(u.multiply(h.getU())).add(v.multiply(h.getV()));
            double hr = h.getRadius();
            Point3D[] hPerim = ui.workspace.shapes.holes.hole_profile_helper_ui_main.getProfilePoints(hCap, u, v, h, hr, SEGS);
            hole_mesh_triangulator_ui_main.triangulateAnnularFace(oct, hPerim, pts, fcs, false);
            double depth = h.isThroughAll() ? len : Math.min(len, h.getDepth());
            Point3D hFloor = hCap.subtract(n.multiply(depth));
            Point3D[] floorPerim = ui.workspace.shapes.holes.hole_profile_helper_ui_main.getProfilePoints(hFloor, u, v, h, hr, SEGS);
            hole_mesh_triangulator_ui_main.buildCylindricalWall(floorPerim, hPerim, pts, fcs);
            if (!h.isThroughAll() && depth < len) {
                hole_mesh_triangulator_ui_main.buildCircleCap(hFloor, floorPerim, pts, fcs, true);
            }
        }
    }
}
