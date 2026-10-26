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
            y0 = p1.getY(); y1 = p1.getY() - sz;
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

        double hw = w * 0.5, hd = d * 0.5;
        Point3D dftl = dDelta(shape, face_kind_ui_main.FRONT, new Point3D(x0, y1, z1), cx, cz, y0, y1, hw, hd);
        Point3D dftr = dDelta(shape, face_kind_ui_main.FRONT, new Point3D(x1, y1, z1), cx, cz, y0, y1, hw, hd);
        Point3D dfbr = dDelta(shape, face_kind_ui_main.FRONT, new Point3D(x1, y0, z1), cx, cz, y0, y1, hw, hd);
        Point3D dfbl = dDelta(shape, face_kind_ui_main.FRONT, new Point3D(x0, y0, z1), cx, cz, y0, y1, hw, hd);
        Point3D dbtl = dDelta(shape, face_kind_ui_main.BACK,  new Point3D(x0, y1, z0), cx, cz, y0, y1, hw, hd);
        Point3D dbtr = dDelta(shape, face_kind_ui_main.BACK,  new Point3D(x1, y1, z0), cx, cz, y0, y1, hw, hd);
        Point3D dbbr = dDelta(shape, face_kind_ui_main.BACK,  new Point3D(x1, y0, z0), cx, cz, y0, y1, hw, hd);
        Point3D dbbl = dDelta(shape, face_kind_ui_main.BACK,  new Point3D(x0, y0, z0), cx, cz, y0, y1, hw, hd);
        Point3D dltf = dDelta(shape, face_kind_ui_main.LEFT,  new Point3D(x0, y1, z1), cx, cz, y0, y1, hw, hd);
        Point3D dltb = dDelta(shape, face_kind_ui_main.LEFT,  new Point3D(x0, y1, z0), cx, cz, y0, y1, hw, hd);
        Point3D dlbb = dDelta(shape, face_kind_ui_main.LEFT,  new Point3D(x0, y0, z0), cx, cz, y0, y1, hw, hd);
        Point3D dlbf = dDelta(shape, face_kind_ui_main.LEFT,  new Point3D(x0, y0, z1), cx, cz, y0, y1, hw, hd);
        Point3D drtf = dDelta(shape, face_kind_ui_main.RIGHT, new Point3D(x1, y1, z1), cx, cz, y0, y1, hw, hd);
        Point3D drtb = dDelta(shape, face_kind_ui_main.RIGHT, new Point3D(x1, y1, z0), cx, cz, y0, y1, hw, hd);
        Point3D drbb = dDelta(shape, face_kind_ui_main.RIGHT, new Point3D(x1, y0, z0), cx, cz, y0, y1, hw, hd);
        Point3D drbf = dDelta(shape, face_kind_ui_main.RIGHT, new Point3D(x1, y0, z1), cx, cz, y0, y1, hw, hd);

        Point3D top = new Point3D(0, -1, 0), bot = new Point3D(0, 1, 0);
        Point3D fr = new Point3D(0, 0, 1), bk = new Point3D(0, 0, -1);
        Point3D lf = new Point3D(-1, 0, 0), rt = new Point3D(1, 0, 0);

        List<Float> pts = new ArrayList<>(); List<Integer> fcs = new ArrayList<>();

        int tfl = addPt(pts, x0 + mTL + dltf.getX(), y1, z1 - Math.max(mTF, mFL) + dftl.getZ());
        int tfr = addPt(pts, x1 - mTR + drtf.getX(), y1, z1 - Math.max(mTF, mFR) + dftr.getZ());
        int tbr = addPt(pts, x1 - mTR + drtb.getX(), y1, z0 + Math.max(mTB, mBRv) + dbtr.getZ());
        int tbl = addPt(pts, x0 + mTL + dltb.getX(), y1, z0 + Math.max(mTB, mBLv) + dbtl.getZ());
        addFace(shape, face_kind_ui_main.TOP, pts, fcs, tbl, tbr, tfr, tfl);

        int bfl = addPt(pts, x0 + mBL + dlbf.getX(), y0, z1 - Math.max(mBF, mFL) + dfbl.getZ());
        int bfr = addPt(pts, x1 - mBR + drbf.getX(), y0, z1 - Math.max(mBF, mFR) + dfbr.getZ());
        int bbr = addPt(pts, x1 - mBR + drbb.getX(), y0, z0 + Math.max(mBB, mBRv) + dbbr.getZ());
        int bbl = addPt(pts, x0 + mBL + dlbb.getX(), y0, z0 + Math.max(mBB, mBLv) + dbbl.getZ());
        addFace(shape, face_kind_ui_main.BOTTOM, pts, fcs, bfl, bfr, bbr, bbl);

        int fftl = (mTF > 0 || mFL > 0 || mTL > 0) ? addPt(pts, x0 + mFL + dltf.getX(), y1 + Math.max(mTF, mTL), z1 + dftl.getZ()) : tfl;
        int fftr = (mTF > 0 || mFR > 0 || mTR > 0) ? addPt(pts, x1 - mFR + drtf.getX(), y1 + Math.max(mTF, mTR), z1 + dftr.getZ()) : tfr;
        int ffbr = (mBF > 0 || mFR > 0 || mBR > 0) ? addPt(pts, x1 - mFR + drbf.getX(), y0 - Math.max(mBF, mBR), z1 + dfbr.getZ()) : bfr;
        int ffbl = (mBF > 0 || mFL > 0 || mBL > 0) ? addPt(pts, x0 + mFL + dlbf.getX(), y0 - Math.max(mBF, mBL), z1 + dfbl.getZ()) : bfl;
        addFace(shape, face_kind_ui_main.FRONT, pts, fcs, fftl, fftr, ffbr, ffbl);

        int bbtl = (mTB > 0 || mBLv > 0 || mTL > 0) ? addPt(pts, x0 + mBLv + dltb.getX(), y1 + Math.max(mTB, mTL), z0 + dbtl.getZ()) : tbl;
        int bbtr = (mTB > 0 || mBRv > 0 || mTR > 0) ? addPt(pts, x1 - mBRv + drtb.getX(), y1 + Math.max(mTB, mTR), z0 + dbtr.getZ()) : tbr;
        int bbbr = (mBB > 0 || mBRv > 0 || mBR > 0) ? addPt(pts, x1 - mBRv + drbb.getX(), y0 - Math.max(mBB, mBR), z0 + dbbr.getZ()) : bbr;
        int bbbl = (mBB > 0 || mBLv > 0 || mBL > 0) ? addPt(pts, x0 + mBLv + dlbb.getX(), y0 - Math.max(mBB, mBL), z0 + dbbl.getZ()) : bbl;
        addFace(shape, face_kind_ui_main.BACK, pts, fcs, bbbl, bbbr, bbtr, bbtl);

        int l_tf = (mFL == 0 && (mTF > 0 || mTL > 0)) ? ((mTL > 0) ? fftl : tfl) : ((mTL > 0 || mFL > 0) ? addPt(pts, x0 + dltf.getX(), y1 + mTL, z1 - mFL + dftl.getZ()) : tfl);
        int l_tb = (mBLv == 0 && (mTB > 0 || mTL > 0)) ? ((mTL > 0) ? bbtl : tbl) : ((mTL > 0 || mBLv > 0) ? addPt(pts, x0 + dltb.getX(), y1 + mTL, z0 + mBLv + dbtl.getZ()) : tbl);
        int l_bb = (mBLv == 0 && (mBB > 0 || mBL > 0)) ? ((mBL > 0) ? bbbl : bbl) : ((mBL > 0 || mBLv > 0) ? addPt(pts, x0 + dlbb.getX(), y0 - mBL, z0 + mBLv + dbbl.getZ()) : bbl);
        int l_bf = (mFL == 0 && (mBF > 0 || mBL > 0)) ? ((mBL > 0) ? ffbl : bfl) : ((mBL > 0 || mFL > 0) ? addPt(pts, x0 + dlbf.getX(), y0 - mBL, z1 - mFL + dfbl.getZ()) : bfl);
        addFace(shape, face_kind_ui_main.LEFT, pts, fcs, l_tb, l_tf, l_bf, l_bb);

        int r_tf = (mFR == 0 && (mTF > 0 || mTR > 0)) ? ((mTR > 0) ? fftr : tfr) : ((mTR > 0 || mFR > 0) ? addPt(pts, x1 + drtf.getX(), y1 + mTR, z1 - mFR + dftr.getZ()) : tfr);
        int r_tb = (mBRv == 0 && (mTB > 0 || mTR > 0)) ? ((mTR > 0) ? bbtr : tbr) : ((mTR > 0 || mBRv > 0) ? addPt(pts, x1 + drtb.getX(), y1 + mTR, z0 + mBRv + dbtr.getZ()) : tbr);
        int r_bb = (mBRv == 0 && (mBB > 0 || mBR > 0)) ? ((mBR > 0) ? bbbr : bbr) : ((mBR > 0 || mBRv > 0) ? addPt(pts, x1 + drbb.getX(), y0 - mBR, z0 + mBRv + dbbr.getZ()) : bbr);
        int r_bf = (mFR == 0 && (mBF > 0 || mBR > 0)) ? ((mBR > 0) ? ffbr : bfr) : ((mBR > 0 || mFR > 0) ? addPt(pts, x1 + drbf.getX(), y0 - mBR, z1 - mFR + dfbr.getZ()) : bfr);
        addFace(shape, face_kind_ui_main.RIGHT, pts, fcs, r_tb, r_bb, r_bf, r_tf);

        var bTF = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "TOP_FRONT", pts, fcs, tfl, tfr, fftr, fftl, maxM);
        var bTB = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "TOP_BACK",  pts, fcs, bbtl, bbtr, tbr, tbl, maxM);
        var bTL = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "TOP_LEFT",  pts, fcs, tbl, tfl, l_tf, l_tb, maxM);
        var bTR = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "TOP_RIGHT", pts, fcs, tfr, tbr, r_tb, r_tf, maxM);

        var bBF = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "BOTTOM_FRONT", pts, fcs, ffbl, ffbr, bfr, bfl, maxM);
        var bBB = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "BOTTOM_BACK",  pts, fcs, bbl, bbr, bbbr, bbbl, maxM);
        var bBL = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "BOTTOM_LEFT",  pts, fcs, l_bb, l_bf, bfl, bbl, maxM);
        var bBR = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "BOTTOM_RIGHT", pts, fcs, r_bf, r_bb, bbr, bfr, maxM);

        var bFL = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "FRONT_LEFT",  pts, fcs, l_bf, l_tf, fftl, ffbl, maxM);
        var bFR = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "FRONT_RIGHT", pts, fcs, ffbr, fftr, r_tf, r_bf, maxM);
        var bBLv = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "BACK_LEFT",   pts, fcs, l_tb, l_bb, bbbl, bbtl, maxM);
        var bBRv = box_fillet_mesh_helper_ui_main.addEdgePatch(shape, "BACK_RIGHT",  pts, fcs, bbtr, bbbr, r_bb, r_tb, maxM);

        box_corner_mesh_helper_ui_main.closeCorner(pts, fcs, new Point3D(-1, -1, 1), lf, top, fr, bTF.end0(), false, bFL.end1(), true,  bTL.end1(), true,  mTF, mFL, mTL, l_bf, tfr, fftr);
        box_corner_mesh_helper_ui_main.closeCorner(pts, fcs, new Point3D(1, -1, 1),  rt, top, fr, bTF.end1(), false, bFR.end1(), false, bTR.end0(), true,  mTF, mFR, mTR, r_bf, tfl, fftl);
        box_corner_mesh_helper_ui_main.closeCorner(pts, fcs, new Point3D(-1, -1, -1), lf, bk, top, bTB.end0(), false, bTL.end0(), false, bBLv.end0(), false, mTB, mTL, mBLv, l_bb, bbtr, tbr);
        box_corner_mesh_helper_ui_main.closeCorner(pts, fcs, new Point3D(1, -1, -1),  rt, bk, top, bTB.end1(), false, bTR.end1(), false, bBRv.end0(), false, mTB, mTR, mBRv, r_bb, bbtl, tbl);

        box_corner_mesh_helper_ui_main.closeCorner(pts, fcs, new Point3D(-1, 1, 1),  lf, fr, bot, bBF.end0(), false, bBL.end1(), true,  bFL.end0(), false, mBF, mBL, mFL, l_tf, ffbr, bfr);
        box_corner_mesh_helper_ui_main.closeCorner(pts, fcs, new Point3D(1, 1, 1),   rt, fr, bot, bBF.end1(), false, bBR.end0(), true,  bFR.end0(), true,  mBF, mBR, mFR, r_tf, ffbl, bfl);
        box_corner_mesh_helper_ui_main.closeCorner(pts, fcs, new Point3D(-1, 1, -1), lf, bot, bk, bBB.end0(), false, bBLv.end1(), true, bBL.end0(), false, mBB, mBLv, mBL, l_tb, bbr, bbbr);
        box_corner_mesh_helper_ui_main.closeCorner(pts, fcs, new Point3D(1, 1, -1),  rt, bot, bk, bBB.end1(), false, bBRv.end1(), false, bBR.end1(), false, mBB, mBRv, mBR, r_tb, bbl, bbbl);

        float[] pa = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()];     for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(pa); mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1); mesh.getFaces().setAll(fa);
        MeshView mv = new MeshView(mesh); mv.setCullFace(CullFace.NONE);
        mv.setMaterial(shape_geometry_3d_ui_main.createMaterial(false, isSelected));
        return mv;
    }

    private static double getM(shape_item_ui_main s, String edge, double maxM) {
        double ch = box_fillet_mesh_helper_ui_main.getChamfer(s, edge, maxM);
        double fil = box_fillet_mesh_helper_ui_main.getFil(s, edge, maxM);
        return Math.min(Math.max(ch, fil), maxM);
    }

    private static void addFace(shape_item_ui_main s, face_kind_ui_main kind, List<Float> pts, List<Integer> fcs,
                                int p1, int p2, int p3, int p4) {
        List<ui.workspace.drafting.holes.hole_feature_ui_main> holes = new ArrayList<>();
        for (var h : s.getAllEffectiveHoles()) if (h.getFaceKind() == kind) holes.add(h);
        if (holes.isEmpty()) addQuad(fcs, p1, p2, p3, p4);
        else machined_face_hole_partitioner_ui_main.partitionAndBuildFace(
            s, kind, getP(pts, p1), getP(pts, p2), getP(pts, p3), getP(pts, p4), holes, pts, fcs);
    }

    private static Point3D getP(List<Float> pts, int idx) {
        return new Point3D(pts.get(idx * 3), pts.get(idx * 3 + 1), pts.get(idx * 3 + 2));
    }

    private static int addPt(List<Float> pts, double x, double y, double z) {
        int idx = pts.size() / 3; pts.add((float) x); pts.add((float) y); pts.add((float) z); return idx;
    }

    private static void addTri(List<Integer> fcs, int a, int b, int c) {
        if (a == b || b == c || a == c) return;
        fcs.add(a); fcs.add(0); fcs.add(b); fcs.add(1); fcs.add(c); fcs.add(2);
    }

    private static void addQuad(List<Integer> fcs, int a, int b, int c, int d) {
        if ((a == b && c == d) || (a == d && b == c)) return;
        addTri(fcs, a, b, c); addTri(fcs, a, c, d);
    }

    public static Point3D dDelta(shape_item_ui_main s, face_kind_ui_main fk, Point3D pt,
                                 double cx, double cz, double y0, double y1, double hw, double hd) {
        if (s == null) return Point3D.ZERO;
        Point3D tot = Point3D.ZERO;
        for (var d : s.getDrafts()) {
            if (!d.isValid() || d.getFaceKind() != fk) continue;
            Point3D nF = draft_feature_ui_main.getFaceNormal(d.getFaceKind());
            Point3D nN = draft_feature_ui_main.getFaceNormal(d.getNeutralKind());
            if (nF == null || nN == null) continue;
            Point3D pN = draft_mesh_builder_ui_main.getNeutralPoint(cx, cz, y0, y1, hw, hd, d.getNeutralKind());
            Point3D pull = d.getPullDirection() != null ? d.getPullDirection().normalize() : nN.multiply(-1.0);
            double denom = pull.dotProduct(nN);
            if (Math.abs(denom) < 1e-6) continue;
            double num = pt.subtract(pN).dotProduct(nN);
            double dist = num / denom;
            tot = tot.subtract(nF.multiply(dist * Math.tan(Math.toRadians(d.getDraftAngle()))));
        }
        return tot;
    }
}
