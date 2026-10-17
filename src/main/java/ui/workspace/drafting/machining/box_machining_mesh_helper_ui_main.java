package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;

import java.util.ArrayList;
import java.util.List;

public final class box_machining_mesh_helper_ui_main {

    private box_machining_mesh_helper_ui_main() {}

    public static Node buildMachinedBox(shape_item_ui_main shape, boolean isSelected) {
        if (shape == null) return null;
        Point3D p1 = shape.getP1(), p2 = shape.getP2();
        double w, h, d, cx, cz, y0, y1;
        if (shape.getType() == basic_shapes_ui_main.CUBE) {
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), sz = Math.max(Math.abs(dx), Math.abs(dz));
            w = sz; h = sz; d = sz;
            cx = p1.getX() + (dx >= 0 ? sz * 0.5 : -sz * 0.5); cz = p1.getZ() + (dz >= 0 ? sz * 0.5 : -sz * 0.5);
            y0 = 0.0; y1 = -sz;
        } else {
            w = Math.max(0.2, Math.abs(p2.getX() - p1.getX())); d = Math.max(0.2, Math.abs(p2.getZ() - p1.getZ()));
            h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            cx = (p1.getX() + p2.getX()) * 0.5; cz = (p1.getZ() + p2.getZ()) * 0.5;
            y0 = p1.getY(); y1 = p1.getY() - h;
        }
        double x0 = cx - w * 0.5, x1 = cx + w * 0.5, z0 = cz - d * 0.5, z1 = cz + d * 0.5;
        double maxM = Math.min(Math.min(w, h), d) * 0.45;

        double mTF = getM(shape, "TOP_FRONT", maxM), mTB = getM(shape, "TOP_BACK", maxM);
        double mTL = getM(shape, "TOP_LEFT", maxM),  mTR = getM(shape, "TOP_RIGHT", maxM);
        double mBF = getM(shape, "BOTTOM_FRONT", maxM), mBB = getM(shape, "BOTTOM_BACK", maxM);
        double mBL = getM(shape, "BOTTOM_LEFT", maxM),  mBR = getM(shape, "BOTTOM_RIGHT", maxM);
        double mFL = getM(shape, "FRONT_LEFT", maxM),   mFR = getM(shape, "FRONT_RIGHT", maxM);
        double mBLv = getM(shape, "BACK_LEFT", maxM),   mBRv = getM(shape, "BACK_RIGHT", maxM);

        double df = getDraftTaper(shape, face_kind_ui_main.FRONT, h);
        double db = getDraftTaper(shape, face_kind_ui_main.BACK, h);
        double dl = getDraftTaper(shape, face_kind_ui_main.LEFT, h);
        double dr = getDraftTaper(shape, face_kind_ui_main.RIGHT, h);

        List<Float> pts = new ArrayList<>(); List<Integer> fcs = new ArrayList<>();

        // Top face 4 corners
        int tfl = addPt(pts, x0 + mTL + dl, y1, z1 - mTF - df), tfr = addPt(pts, x1 - mTR - dr, y1, z1 - mTF - df);
        int tbr = addPt(pts, x1 - mTR - dr, y1, z0 + mTB + db), tbl = addPt(pts, x0 + mTL + dl, y1, z0 + mTB + db);
        addQuad(fcs, tfl, tfr, tbr, tbl);

        // Bottom face 4 corners
        int bfl = addPt(pts, x0 + mBL, y0, z1 - mBF), bfr = addPt(pts, x1 - mBR, y0, z1 - mBF);
        int bbr = addPt(pts, x1 - mBR, y0, z0 + mBB), bbl = addPt(pts, x0 + mBL, y0, z0 + mBB);
        addQuad(fcs, bfl, bbl, bbr, bfr);

        // Front face 4 corners
        int fftl = addPt(pts, x0 + mFL + dl, y1 + mTF, z1 - df), fftr = addPt(pts, x1 - mFR - dr, y1 + mTF, z1 - df);
        int ffbr = addPt(pts, x1 - mFR, y0 - mBF, z1), ffbl = addPt(pts, x0 + mFL, y0 - mBF, z1);
        addQuad(fcs, fftl, fftr, ffbr, ffbl);

        // Back face 4 corners
        int bbtl = addPt(pts, x0 + mBLv + dl, y1 + mTB, z0 + db), bbtr = addPt(pts, x1 - mBRv - dr, y1 + mTB, z0 + db);
        int bbbr = addPt(pts, x1 - mBRv, y0 - mBB, z0), bbbl = addPt(pts, x0 + mBLv, y0 - mBB, z0);
        addQuad(fcs, bbtr, bbtl, bbbl, bbbr);

        // Left face 4 corners
        int lftl = addPt(pts, x0 + dl, y1 + mTL, z1 - mFL - df), lbtl = addPt(pts, x0 + dl, y1 + mTL, z0 + mBLv + db);
        int lbbl = addPt(pts, x0, y0 - mBL, z0 + mBLv), lfbl = addPt(pts, x0, y0 - mBL, z1 - mFL);
        addQuad(fcs, lbtl, lftl, lfbl, lbbl);

        // Right face 4 corners
        int rftl = addPt(pts, x1 - dr, y1 + mTR, z1 - mFR - df), rbtl = addPt(pts, x1 - dr, y1 + mTR, z0 + mBRv + db);
        int rbbr = addPt(pts, x1, y0 - mBR, z0 + mBRv), rfbr = addPt(pts, x1, y0 - mBR, z1 - mFR);
        addQuad(fcs, rftl, rbtl, rbbr, rfbr);

        // 12 Bevel Quads / Rounded Blends
        addBevelOrBlend(shape, "TOP_FRONT", pts, fcs, tfl, tfr, fftr, fftl, 0, 1, 0, -1);
        addBevelOrBlend(shape, "TOP_BACK",  pts, fcs, bbtr, bbtl, tbl, tbr, 0, 1, 0, 1);
        addBevelOrBlend(shape, "TOP_LEFT",  pts, fcs, tbl, tfl, lftl, lbtl, 1, 0, 0, 1);
        addBevelOrBlend(shape, "TOP_RIGHT", pts, fcs, tfr, tbr, rbtl, rftl, -1, 0, 0, 1);

        addBevelOrBlend(shape, "BOTTOM_FRONT", pts, fcs, ffbl, ffbr, bfr, bfl, 0, -1, 0, -1);
        addBevelOrBlend(shape, "BOTTOM_BACK",  pts, fcs, bbr, bbl, bbbl, bbbr, 0, -1, 0, 1);
        addBevelOrBlend(shape, "BOTTOM_LEFT",  pts, fcs, bbl, bfl, lfbl, lbbl, 1, 0, 0, -1);
        addBevelOrBlend(shape, "BOTTOM_RIGHT", pts, fcs, bfr, bbr, rbbr, rfbr, -1, 0, 0, -1);

        addBevelOrBlend(shape, "FRONT_LEFT",  pts, fcs, lftl, lfbl, ffbl, fftl, 0, 0, 0, 0);
        addBevelOrBlend(shape, "FRONT_RIGHT", pts, fcs, fftr, ffbr, rfbr, rftl, 0, 0, 0, 0);
        addBevelOrBlend(shape, "BACK_LEFT",   pts, fcs, bbbl, bbtl, lbtl, lbbl, 0, 0, 0, 0);
        addBevelOrBlend(shape, "BACK_RIGHT",  pts, fcs, rbtl, rbbr, bbbr, bbtr, 0, 0, 0, 0);

        // 8 Corner Triangles
        addTri(fcs, tfl, fftl, lftl); addTri(fcs, tfr, rftl, fftr);
        addTri(fcs, tbl, lbtl, bbtl); addTri(fcs, tbr, bbtr, rbtl);
        addTri(fcs, bfl, lfbl, ffbl); addTri(fcs, bfr, ffbr, rfbr);
        addTri(fcs, bbl, bbbl, lbbl); addTri(fcs, bbr, rbbr, bbbr);

        float[] pa = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()];     for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(pa); mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1); mesh.getFaces().setAll(fa);
        MeshView mv = new MeshView(mesh); mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(false, isSelected));
        return mv;
    }

    private static double getM(shape_item_ui_main s, String edge, double maxM) {
        double ch = 0.0, fil = 0.0;
        for (var c : s.getChamfers()) if (c.isValid() && (edge.equalsIgnoreCase(c.getSimpleEdgeName()) || edge.equalsIgnoreCase(c.getEdgeId()))) ch = Math.max(ch, c.getDistance());
        for (var f : s.getFillets()) if (f.isValid() && (edge.equalsIgnoreCase(f.getSimpleEdgeName()) || edge.equalsIgnoreCase(f.getEdgeId()))) fil = Math.max(fil, f.getRadius());
        return Math.min(Math.max(ch, fil), maxM);
    }

    private static void addBevelOrBlend(shape_item_ui_main shape, String edge, List<Float> pts, List<Integer> fcs,
                                       int a, int b, int c, int d, double dx, double dy, double dz, double sign) {
        double fil = 0.0;
        for (var f : shape.getFillets()) if (f.isValid() && (edge.equalsIgnoreCase(f.getSimpleEdgeName()) || edge.equalsIgnoreCase(f.getEdgeId()))) fil = Math.max(fil, f.getRadius());
        if (fil > 0.0) {
            int segs = 4, prevA = a, prevB = b;
            for (int s = 1; s < segs; s++) {
                double frac = (double) s / segs;
                int nA = lerpPt(pts, a, d, frac), nB = lerpPt(pts, b, c, frac);
                addQuad(fcs, prevA, prevB, nB, nA);
                prevA = nA; prevB = nB;
            }
            addQuad(fcs, prevA, prevB, c, d);
        } else {
            addQuad(fcs, a, b, c, d);
        }
    }

    private static int lerpPt(List<Float> pts, int p1, int p2, double t) {
        double x = pts.get(p1 * 3) * (1.0 - t) + pts.get(p2 * 3) * t;
        double y = pts.get(p1 * 3 + 1) * (1.0 - t) + pts.get(p2 * 3 + 1) * t;
        double z = pts.get(p1 * 3 + 2) * (1.0 - t) + pts.get(p2 * 3 + 2) * t;
        return addPt(pts, x, y, z);
    }

    private static int addPt(List<Float> pts, double x, double y, double z) {
        int idx = pts.size() / 3; pts.add((float) x); pts.add((float) y); pts.add((float) z); return idx;
    }
    private static void addTri(List<Integer> fcs, int a, int b, int c) {
        fcs.add(a); fcs.add(0); fcs.add(b); fcs.add(1); fcs.add(c); fcs.add(2);
        fcs.add(a); fcs.add(0); fcs.add(c); fcs.add(2); fcs.add(b); fcs.add(1);
    }
    private static void addQuad(List<Integer> fcs, int a, int b, int c, int d) {
        addTri(fcs, a, b, c); addTri(fcs, a, c, d);
    }
    private static double getDraftTaper(shape_item_ui_main s, face_kind_ui_main kind, double h) {
        for (var d : s.getDrafts()) if (d.isValid() && d.getFaceKind() == kind) return h * Math.tan(Math.toRadians(d.getDraftAngle()));
        return 0.0;
    }
}
