package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * derived_region_advanced_mesh_ui_main.java
 * Constructs TriangleMesh geometries for complex/advanced hole walls (Countersink, Counterbore).
 */
public final class derived_region_advanced_mesh_ui_main {
    private static final int SEGS = 32;

    private derived_region_advanced_mesh_ui_main() {}

    public static TriangleMesh createAdvancedWallMesh(Point3D topCenter, Point3D inDir, Point3D uAxis, Point3D vAxis,
                                                      topology_derived_face_ui_main df, shape_item_ui_main shape) {
        hole_feature_ui_main h = findHole(shape, df.getCreatingFeatureId());
        if (h == null || h.getHoleType() == hole_feature_ui_main.HoleType.SIMPLE) {
            return createCylinderWall(topCenter, inDir, uAxis, vAxis, df.getRadius(), df.getDepth());
        }

        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();
        double totalDepth = df.getDepth();
        double rBore = h.getRadius();

        if (h.getHoleType() == hole_feature_ui_main.HoleType.COUNTERSINK) {
            double cd = Math.min(totalDepth, h.getConeDepth());
            Point3D transCenter = topCenter.add(inDir.multiply(cd));
            Point3D endCenter = topCenter.add(inDir.multiply(totalDepth));
            buildFrustum(topCenter, transCenter, uAxis, vAxis, h.getOuterRadius(), rBore, pts, fcs);
            if (totalDepth > cd + 1e-4) {
                buildCylinderSection(transCenter, endCenter, uAxis, vAxis, rBore, pts, fcs);
            }
        } else if (h.getHoleType() == hole_feature_ui_main.HoleType.COUNTERBORE) {
            double cd = Math.min(totalDepth, h.getCbDepth());
            Point3D recessCenter = topCenter.add(inDir.multiply(cd));
            Point3D endCenter = topCenter.add(inDir.multiply(totalDepth));
            buildCylinderSection(topCenter, recessCenter, uAxis, vAxis, h.getOuterRadius(), pts, fcs);
            buildAnnulusSection(recessCenter, uAxis, vAxis, h.getOuterRadius(), rBore, pts, fcs);
            if (totalDepth > cd + 1e-4) {
                buildCylinderSection(recessCenter, endCenter, uAxis, vAxis, rBore, pts, fcs);
            }
        }

        return toMesh(pts, fcs);
    }

    private static hole_feature_ui_main findHole(shape_item_ui_main shape, String id) {
        if (shape == null || id == null) return null;
        for (hole_feature_ui_main h : shape.getAllEffectiveHoles()) {
            if (h != null && id.equals(h.getId())) return h;
        }
        return null;
    }

    public static TriangleMesh createCylinderWall(Point3D topCenter, Point3D inDir, Point3D uAxis, Point3D vAxis, double r, double depth) {
        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();
        Point3D botCenter = topCenter.add(inDir.multiply(depth));
        buildCylinderSection(topCenter, botCenter, uAxis, vAxis, r, pts, fcs);
        return toMesh(pts, fcs);
    }

    private static void buildFrustum(Point3D topC, Point3D botC, Point3D uAxis, Point3D vAxis, double r1, double r2, List<Float> pts, List<Integer> fcs) {
        int[] topIdx = new int[SEGS], botIdx = new int[SEGS];
        for (int i = 0; i < SEGS; i++) {
            double a = i * 2.0 * Math.PI / SEGS;
            Point3D rad1 = uAxis.multiply(r1 * Math.cos(a)).add(vAxis.multiply(r1 * Math.sin(a)));
            Point3D rad2 = uAxis.multiply(r2 * Math.cos(a)).add(vAxis.multiply(r2 * Math.sin(a)));
            topIdx[i] = addVertex(topC.add(rad1), pts);
            botIdx[i] = addVertex(botC.add(rad2), pts);
        }
        for (int i = 0; i < SEGS; i++) {
            int next = (i + 1) % SEGS;
            addQuad(topIdx[i], botIdx[i], botIdx[next], topIdx[next], fcs);
        }
    }

    private static void buildCylinderSection(Point3D topC, Point3D botC, Point3D uAxis, Point3D vAxis, double r, List<Float> pts, List<Integer> fcs) {
        buildFrustum(topC, botC, uAxis, vAxis, r, r, pts, fcs);
    }

    private static void buildAnnulusSection(Point3D center, Point3D uAxis, Point3D vAxis, double rOuter, double rInner, List<Float> pts, List<Integer> fcs) {
        int[] outIdx = new int[SEGS], inIdx = new int[SEGS];
        for (int i = 0; i < SEGS; i++) {
            double a = i * 2.0 * Math.PI / SEGS;
            Point3D radOut = uAxis.multiply(rOuter * Math.cos(a)).add(vAxis.multiply(rOuter * Math.sin(a)));
            Point3D radIn = uAxis.multiply(rInner * Math.cos(a)).add(vAxis.multiply(rInner * Math.sin(a)));
            outIdx[i] = addVertex(center.add(radOut), pts);
            inIdx[i] = addVertex(center.add(radIn), pts);
        }
        for (int i = 0; i < SEGS; i++) {
            int next = (i + 1) % SEGS;
            addQuad(outIdx[i], inIdx[i], inIdx[next], outIdx[next], fcs);
        }
    }

    private static int addVertex(Point3D p, List<Float> pts) {
        pts.add((float) p.getX()); pts.add((float) p.getY()); pts.add((float) p.getZ());
        return (pts.size() / 3) - 1;
    }

    private static void addQuad(int p0, int p1, int p2, int p3, List<Integer> fcs) {
        fcs.add(p0); fcs.add(0); fcs.add(p1); fcs.add(2); fcs.add(p2); fcs.add(3);
        fcs.add(p0); fcs.add(0); fcs.add(p2); fcs.add(3); fcs.add(p3); fcs.add(1);
    }

    private static TriangleMesh toMesh(List<Float> pts, List<Integer> fcs) {
        TriangleMesh m = new TriangleMesh();
        m.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        float[] pa = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);
        m.getPoints().setAll(pa); m.getFaces().setAll(fa);
        int[] sg = new int[fa.length / 6]; java.util.Arrays.fill(sg, 1);
        m.getFaceSmoothingGroups().setAll(sg);
        return m;
    }
}
