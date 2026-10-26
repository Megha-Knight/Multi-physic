package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.List;

/**
 * box_fillet_mesh_helper_ui_main.java
 * Generates 4-segment quarter-cylinder blend strips for box linear edge fillets.
 */
public final class box_fillet_mesh_helper_ui_main {

    public static final int SEGS = 4;

    public record EdgeBlend(int[] end0, int[] end1) {}

    private box_fillet_mesh_helper_ui_main() {}

    public static double getFil(shape_item_ui_main s, String edge, double maxM) {
        if (s == null || edge == null) return 0.0;
        double fil = 0.0;
        for (var f : s.getFillets()) {
            if (f.isValid() && (edge.equalsIgnoreCase(f.getSimpleEdgeName()) || edge.equalsIgnoreCase(f.getEdgeId()))) {
                fil = Math.max(fil, f.getRadius());
            }
        }
        return Math.min(fil, maxM);
    }

    public static double getChamfer(shape_item_ui_main s, String edge, double maxM) {
        if (s == null || edge == null) return 0.0;
        double ch = 0.0;
        for (var c : s.getChamfers()) {
            if (c.isValid() && (edge.equalsIgnoreCase(c.getSimpleEdgeName()) || edge.equalsIgnoreCase(c.getEdgeId()))) {
                ch = Math.max(ch, c.getDistance());
            }
        }
        return Math.min(ch, maxM);
    }

    public static Point3D[] getEdgeNormals(String edge) {
        Point3D top = new Point3D(0, -1, 0), bot = new Point3D(0, 1, 0);
        Point3D fr = new Point3D(0, 0, 1), bk = new Point3D(0, 0, -1);
        Point3D lf = new Point3D(-1, 0, 0), rt = new Point3D(1, 0, 0);
        return switch (edge.toUpperCase()) {
            case "TOP_FRONT"    -> new Point3D[]{top, fr};
            case "TOP_BACK"     -> new Point3D[]{bk, top};
            case "TOP_LEFT"     -> new Point3D[]{top, lf};
            case "TOP_RIGHT"    -> new Point3D[]{top, rt};
            case "BOTTOM_FRONT" -> new Point3D[]{fr, bot};
            case "BOTTOM_BACK"  -> new Point3D[]{bot, bk};
            case "BOTTOM_LEFT"  -> new Point3D[]{lf, bot};
            case "BOTTOM_RIGHT" -> new Point3D[]{rt, bot};
            case "FRONT_LEFT"   -> new Point3D[]{lf, fr};
            case "FRONT_RIGHT"  -> new Point3D[]{fr, rt};
            case "BACK_LEFT"    -> new Point3D[]{lf, bk};
            case "BACK_RIGHT"   -> new Point3D[]{bk, rt};
            default             -> new Point3D[]{top, fr};
        };
    }

    public static EdgeBlend addEdgePatch(shape_item_ui_main shape, String edge, List<Float> pts, List<Integer> fcs,
                                         int a, int b, int c, int d, double maxM) {
        int[] e0 = new int[]{a, d}, e1 = new int[]{b, c};
        if ((a == d && b == c) || (a == b && c == d)) return new EdgeBlend(e0, e1);

        double fil = getFil(shape, edge, maxM);
        if (fil > 1e-4) {
            Point3D[] ns = getEdgeNormals(edge);
            return addFilletStrip(pts, fcs, a, b, c, d, ns[0], ns[1], fil);
        }
        double ch = getChamfer(shape, edge, maxM);
        if (ch > 1e-4) {
            addQuad(fcs, a, b, c, d);
        }
        return new EdgeBlend(e0, e1);
    }

    public static EdgeBlend addFilletStrip(List<Float> pts, List<Integer> fcs,
                                           int a, int b, int c, int d,
                                           Point3D n1, Point3D n2, double r) {
        Point3D pa = getP(pts, a), pd = getP(pts, d);
        Point3D pb = getP(pts, b), pc = getP(pts, c);
        Point3D c0 = pa.subtract(n1.multiply(r)).add(pd.subtract(n2.multiply(r))).multiply(0.5);
        Point3D c1 = pb.subtract(n1.multiply(r)).add(pc.subtract(n2.multiply(r))).multiply(0.5);

        int[] p0 = new int[SEGS + 1], p1 = new int[SEGS + 1];
        p0[0] = a; p1[0] = b;
        p0[SEGS] = d; p1[SEGS] = c;

        for (int s = 1; s < SEGS; s++) {
            double ang = s * (Math.PI * 0.5 / SEGS);
            Point3D v = n1.multiply(r * Math.cos(ang)).add(n2.multiply(r * Math.sin(ang)));
            p0[s] = addPt(pts, c0.add(v));
            p1[s] = addPt(pts, c1.add(v));
        }
        for (int s = 0; s < SEGS; s++) {
            addQuad(fcs, p0[s], p1[s], p1[s + 1], p0[s + 1]);
        }
        return new EdgeBlend(p0, p1);
    }

    private static Point3D getP(List<Float> pts, int idx) {
        return new Point3D(pts.get(idx * 3), pts.get(idx * 3 + 1), pts.get(idx * 3 + 2));
    }

    private static int addPt(List<Float> pts, Point3D p) {
        int idx = pts.size() / 3;
        pts.add((float) p.getX()); pts.add((float) p.getY()); pts.add((float) p.getZ());
        return idx;
    }

    private static void addTri(List<Integer> fcs, int a, int b, int c) {
        if (a == b || b == c || a == c) return;
        fcs.add(a); fcs.add(0); fcs.add(b); fcs.add(1); fcs.add(c); fcs.add(2);
    }

    private static void addQuad(List<Integer> fcs, int a, int b, int c, int d) {
        if ((a == b && c == d) || (a == d && b == c)) return;
        addTri(fcs, a, b, c); addTri(fcs, a, c, d);
    }
}
