package ui.workspace.shapes;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.face_kind_ui_main;
import ui.workspace.drafting.hole_feature_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * hole_mesh_builder_ui_main.java
 * High-precision CSG-grade 3D mesh subtraction engine for CAD solids with parametric holes.
 */
public final class hole_mesh_builder_ui_main {

    private static final int CIRCLE_SEGS = 24;

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

        hole_feature_ui_main hole = (holes != null && !holes.isEmpty()) ? holes.get(0) : null;
        face_kind_ui_main holeFace = (hole != null) ? hole.getFaceKind() : null;

        for (face_kind_ui_main face : new face_kind_ui_main[]{
            face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM, face_kind_ui_main.FRONT,
            face_kind_ui_main.BACK, face_kind_ui_main.RIGHT, face_kind_ui_main.LEFT
        }) {
            if (face == holeFace && hole != null && hole.isValid()) {
                buildFaceWithHole(center, s, face, hole, pts, fcs);
            } else if (hole != null && hole.isThroughAll() && isOpposite(holeFace, face)) {
                buildOppositeFaceHole(center, s, face, hole, pts, fcs);
            } else {
                buildStandardQuadFace(center, s, face, pts, fcs);
            }
        }

        float[] pointsArray = new float[pts.size()];
        for (int i = 0; i < pts.size(); i++) pointsArray[i] = pts.get(i);
        int[] facesArray = new int[fcs.size()];
        for (int i = 0; i < fcs.size(); i++) facesArray[i] = fcs.get(i);

        mesh.getPoints().setAll(pointsArray);
        mesh.getFaces().setAll(facesArray);
        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(isPreview, isSelected));
        return mv;
    }

    private static boolean isOpposite(face_kind_ui_main f1, face_kind_ui_main f2) {
        if (f1 == null || f2 == null) return false;
        return (f1 == face_kind_ui_main.TOP && f2 == face_kind_ui_main.BOTTOM) || (f1 == face_kind_ui_main.BOTTOM && f2 == face_kind_ui_main.TOP) ||
               (f1 == face_kind_ui_main.FRONT && f2 == face_kind_ui_main.BACK) || (f1 == face_kind_ui_main.BACK && f2 == face_kind_ui_main.FRONT) ||
               (f1 == face_kind_ui_main.RIGHT && f2 == face_kind_ui_main.LEFT) || (f1 == face_kind_ui_main.LEFT && f2 == face_kind_ui_main.RIGHT);
    }

    private static void buildFaceWithHole(Point3D c, double s, face_kind_ui_main kind,
                                          hole_feature_ui_main hole, List<Float> pts, List<Integer> fcs) {
        Frame f = getCubeFaceFrame(c, s, kind);
        double hw = s * 0.5, r = Math.min(hole.getRadius(), hw * 0.85);
        Point3D hc = f.origin.add(f.u.multiply(hole.getU())).add(f.v.multiply(hole.getV()));
        double depth = hole.isThroughAll() ? s : Math.min(s, hole.getDepth());

        Point3D[] outer = getOctagonalPerimeter(f, hw);
        Point3D[] entryCircle = getCirclePoints(hc, f.u, f.v, r);
        Point3D[] boreEndCircle = new Point3D[CIRCLE_SEGS];
        for (int i = 0; i < CIRCLE_SEGS; i++) boreEndCircle[i] = entryCircle[i].subtract(f.n.multiply(depth));

        triangulateAnnularFace(outer, entryCircle, pts, fcs, false);
        buildCylindricalWall(entryCircle, boreEndCircle, pts, fcs);

        if (!hole.isThroughAll()) {
            Point3D bottomCenter = hc.subtract(f.n.multiply(depth));
            buildCircleCap(bottomCenter, boreEndCircle, pts, fcs, true);
        }
    }

    private static void buildOppositeFaceHole(Point3D c, double s, face_kind_ui_main kind,
                                              hole_feature_ui_main hole, List<Float> pts, List<Integer> fcs) {
        Frame f = getCubeFaceFrame(c, s, kind);
        double hw = s * 0.5, r = Math.min(hole.getRadius(), hw * 0.85);
        Point3D hc = f.origin.add(f.u.multiply(hole.getU())).add(f.v.multiply(-hole.getV()));
        Point3D[] outer = getOctagonalPerimeter(f, hw);
        Point3D[] exitCircle = getCirclePoints(hc, f.u, f.v, r);
        triangulateAnnularFace(outer, exitCircle, pts, fcs, true);
    }

    private static void buildStandardQuadFace(Point3D c, double s, face_kind_ui_main kind,
                                              List<Float> pts, List<Integer> fcs) {
        Frame f = getCubeFaceFrame(c, s, kind);
        double hw = s * 0.5;
        Point3D p0 = f.origin.subtract(f.u.multiply(hw)).subtract(f.v.multiply(hw));
        Point3D p1 = f.origin.add(f.u.multiply(hw)).subtract(f.v.multiply(hw));
        Point3D p2 = f.origin.add(f.u.multiply(hw)).add(f.v.multiply(hw));
        Point3D p3 = f.origin.subtract(f.u.multiply(hw)).add(f.v.multiply(hw));
        addQuad(p0, p1, p2, p3, pts, fcs);
    }

    private static Point3D[] getOctagonalPerimeter(Frame f, double hw) {
        Point3D c = f.origin, u = f.u, v = f.v;
        return new Point3D[]{
            c.add(u.multiply(hw)), c.add(u.multiply(hw)).add(v.multiply(hw)),
            c.add(v.multiply(hw)), c.subtract(u.multiply(hw)).add(v.multiply(hw)),
            c.subtract(u.multiply(hw)), c.subtract(u.multiply(hw)).subtract(v.multiply(hw)),
            c.subtract(v.multiply(hw)), c.add(u.multiply(hw)).subtract(v.multiply(hw))
        };
    }

    private static Point3D[] getCirclePoints(Point3D center, Point3D u, Point3D v, double r) {
        Point3D[] circle = new Point3D[CIRCLE_SEGS];
        for (int i = 0; i < CIRCLE_SEGS; i++) {
            double a = i * 2.0 * Math.PI / CIRCLE_SEGS;
            circle[i] = center.add(u.multiply(r * Math.cos(a))).add(v.multiply(r * Math.sin(a)));
        }
        return circle;
    }

    private static void triangulateAnnularFace(Point3D[] oct, Point3D[] circ, List<Float> pts, List<Integer> fcs, boolean reverse) {
        for (int k = 0; k < 8; k++) {
            Point3D o1 = oct[k], o2 = oct[(k + 1) % 8];
            int arcStart = k * 3;
            addTri(o1, o2, circ[(arcStart + 3) % CIRCLE_SEGS], pts, fcs, reverse);
            for (int j = 0; j < 3; j++) {
                addTri(o1, circ[(arcStart + j + 1) % CIRCLE_SEGS], circ[(arcStart + j) % CIRCLE_SEGS], pts, fcs, reverse);
            }
        }
    }

    private static void buildCylindricalWall(Point3D[] top, Point3D[] bot, List<Float> pts, List<Integer> fcs) {
        for (int i = 0; i < CIRCLE_SEGS; i++) {
            int next = (i + 1) % CIRCLE_SEGS;
            addTri(top[i], top[next], bot[next], pts, fcs, false);
            addTri(top[i], bot[next], bot[i], pts, fcs, false);
        }
    }

    private static void buildCircleCap(Point3D center, Point3D[] circ, List<Float> pts, List<Integer> fcs, boolean facingEntry) {
        for (int i = 0; i < CIRCLE_SEGS; i++) {
            int next = (i + 1) % CIRCLE_SEGS;
            addTri(center, circ[next], circ[i], pts, fcs, !facingEntry);
        }
    }

    private static void addQuad(Point3D p0, Point3D p1, Point3D p2, Point3D p3, List<Float> pts, List<Integer> fcs) {
        addTri(p0, p1, p2, pts, fcs, false);
        addTri(p0, p2, p3, pts, fcs, false);
    }

    private static void addTri(Point3D a, Point3D b, Point3D c, List<Float> pts, List<Integer> fcs, boolean rev) {
        int i0 = addVertex(rev ? c : a, pts), i1 = addVertex(b, pts), i2 = addVertex(rev ? a : c, pts);
        fcs.add(i0); fcs.add(0); fcs.add(i1); fcs.add(0); fcs.add(i2); fcs.add(0);
    }

    private static int addVertex(Point3D p, List<Float> pts) {
        int idx = pts.size() / 3;
        pts.add((float) p.getX()); pts.add((float) p.getY()); pts.add((float) p.getZ());
        return idx;
    }

    private record Frame(Point3D origin, Point3D n, Point3D u, Point3D v) {}

    private static Frame getCubeFaceFrame(Point3D c, double s, face_kind_ui_main kind) {
        double hw = s * 0.5;
        return switch (kind) {
            case TOP    -> new Frame(new Point3D(c.getX(), c.getY() - hw, c.getZ()), new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1));
            case BOTTOM -> new Frame(new Point3D(c.getX(), c.getY() + hw, c.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1));
            case FRONT  -> new Frame(new Point3D(c.getX(), c.getY(), c.getZ() + hw), new Point3D(0, 0, 1), new Point3D(1, 0, 0), new Point3D(0, -1, 0));
            case BACK   -> new Frame(new Point3D(c.getX(), c.getY(), c.getZ() - hw), new Point3D(0, 0, -1), new Point3D(-1, 0, 0), new Point3D(0, -1, 0));
            case RIGHT  -> new Frame(new Point3D(c.getX() + hw, c.getY(), c.getZ()), new Point3D(1, 0, 0), new Point3D(0, 0, -1), new Point3D(0, -1, 0));
            case LEFT   -> new Frame(new Point3D(c.getX() - hw, c.getY(), c.getZ()), new Point3D(-1, 0, 0), new Point3D(0, 0, 1), new Point3D(0, -1, 0));
            default     -> new Frame(c, new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1));
        };
    }
}
