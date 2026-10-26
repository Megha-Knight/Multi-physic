package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.topology.topology_boundary_loop_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.util.ArrayList;
import java.util.List;

public final class face_overlay_mesh_helper_ui_main {

    private face_overlay_mesh_helper_ui_main() {}

    public static Node createRectSurface(Point3D center, Point3D u, Point3D v, Point3D n, double w, double h, PhongMaterial mat) {
        double hw = w * 0.5, hh = h * 0.5; Point3D offset = n.multiply(0.20);
        Point3D p0 = center.add(u.multiply(hw)).add(v.multiply(hh)).add(offset), p1 = center.add(u.multiply(-hw)).add(v.multiply(hh)).add(offset);
        Point3D p2 = center.add(u.multiply(-hw)).add(v.multiply(-hh)).add(offset), p3 = center.add(u.multiply(hw)).add(v.multiply(-hh)).add(offset);
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll((float) p0.getX(), (float) p0.getY(), (float) p0.getZ(), (float) p1.getX(), (float) p1.getY(), (float) p1.getZ(),
                                (float) p2.getX(), (float) p2.getY(), (float) p2.getZ(), (float) p3.getX(), (float) p3.getY(), (float) p3.getZ());
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        mesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 0, 0, 2, 2, 3, 3);
        return wrapMesh(mesh, mat);
    }

    public static Node createDiskSurface(Point3D center, Point3D u, Point3D v, Point3D n, double r, PhongMaterial mat) {
        int segs = 32; Point3D c = center.add(n.multiply(0.20)); TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll((float) c.getX(), (float) c.getY(), (float) c.getZ()); mesh.getTexCoords().addAll(0.5f, 0.5f);
        for (int i = 0; i < segs; i++) {
            double ang = i * 2.0 * Math.PI / segs; Point3D pt = c.add(u.multiply(r * Math.cos(ang))).add(v.multiply(r * Math.sin(ang)));
            mesh.getPoints().addAll((float) pt.getX(), (float) pt.getY(), (float) pt.getZ());
            mesh.getTexCoords().addAll((float) (0.5 + 0.5 * Math.cos(ang)), (float) (0.5 + 0.5 * Math.sin(ang)));
        }
        for (int i = 0; i < segs; i++) { int next = (i + 1) % segs; mesh.getFaces().addAll(0, 0, i + 1, i + 1, next + 1, next + 1); }
        return wrapMesh(mesh, mat);
    }

    public static Node createAnnularRectSurface(Point3D center, Point3D u, Point3D v, Point3D n, double w, double h,
                                                List<topology_boundary_loop_ui_main> innerLoops, PhongMaterial mat) {
        if (innerLoops == null || innerLoops.isEmpty()) return createRectSurface(center, u, v, n, w, h, mat);
        double hw = w * 0.5, hh = h * 0.5; Point3D c = center.add(n.multiply(0.20));
        List<Float> pts = new ArrayList<>(); List<Integer> fcs = new ArrayList<>();
        partitionRectLoops(new Frame(c, n, u, v), -hw, hw, -hh, hh, new ArrayList<>(innerLoops), pts, fcs);
        TriangleMesh mesh = new TriangleMesh();
        float[] pArr = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pArr[i] = pts.get(i);
        mesh.getPoints().setAll(pArr); mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        int[] fArr = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fArr[i] = fcs.get(i);
        mesh.getFaces().setAll(fArr);
        return wrapMesh(mesh, mat);
    }

    private static void partitionRectLoops(Frame f, double uMin, double uMax, double vMin, double vMax,
                                           List<topology_boundary_loop_ui_main> loops,
                                           List<Float> pts, List<Integer> fcs) {
        if (loops.isEmpty()) {
            Point3D[] oct = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, uMin, uMax, vMin, vMax);
            hole_mesh_triangulator_ui_main.addQuad(oct[1], oct[3], oct[5], oct[7], pts, fcs);
            return;
        }
        if (loops.size() == 1) {
            var loop = loops.get(0);
            Point3D[] oct = hole_mesh_triangulator_ui_main.getOctagonalPerimeter(f, uMin, uMax, vMin, vMax);
            Point3D hc = f.origin().add(f.u().multiply(loop.getCenter().getX())).add(f.v().multiply(loop.getCenter().getY()));
            Point3D[] circ = hole_mesh_triangulator_ui_main.getCirclePoints(hc, f.u(), f.v(), loop.getRadius(), 32);
            hole_mesh_triangulator_ui_main.triangulateAnnularFace(oct, circ, pts, fcs, false);
            return;
        }
        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (var l : loops) {
            double lu = l.getCenter().getX(), lv = l.getCenter().getY();
            minU = Math.min(minU, lu); maxU = Math.max(maxU, lu); minV = Math.min(minV, lv); maxV = Math.max(maxV, lv);
        }
        boolean splitU = (maxU - minU >= maxV - minV);
        if (splitU) loops.sort(java.util.Comparator.comparingDouble(l -> l.getCenter().getX()));
        else loops.sort(java.util.Comparator.comparingDouble(l -> l.getCenter().getY()));
        int bestK = 1; double maxGap = -1;
        for (int k = 1; k < loops.size(); k++) {
            double gap = splitU ? (loops.get(k).getCenter().getX() - loops.get(k - 1).getCenter().getX())
                                : (loops.get(k).getCenter().getY() - loops.get(k - 1).getCenter().getY());
            if (gap > maxGap) { maxGap = gap; bestK = k; }
        }
        double sVal = splitU ? (loops.get(bestK - 1).getCenter().getX() + loops.get(bestK).getCenter().getX()) * 0.5
                             : (loops.get(bestK - 1).getCenter().getY() + loops.get(bestK).getCenter().getY()) * 0.5;
        List<topology_boundary_loop_ui_main> left = new ArrayList<>(loops.subList(0, bestK));
        List<topology_boundary_loop_ui_main> right = new ArrayList<>(loops.subList(bestK, loops.size()));
        partitionRectLoops(f, uMin, splitU ? sVal : uMax, vMin, splitU ? vMax : sVal, left, pts, fcs);
        partitionRectLoops(f, splitU ? sVal : uMin, uMax, splitU ? vMin : sVal, vMax, right, pts, fcs);
    }

    public static Node createAnnularDiskSurface(Point3D center, Point3D u, Point3D v, Point3D n, double outerR,
                                                List<topology_boundary_loop_ui_main> innerLoops, PhongMaterial mat) {
        if (innerLoops == null || innerLoops.isEmpty()) return createDiskSurface(center, u, v, n, outerR, mat);
        Point3D c = center.add(n.multiply(0.20));
        if (innerLoops.size() > 1 && Math.abs(n.getY()) > 0.5) {
            List<ui.workspace.drafting.holes.hole_feature_ui_main> capHoles = new ArrayList<>();
            for (int i = 0; i < innerLoops.size(); i++) {
                var l = innerLoops.get(i);
                capHoles.add(new ui.workspace.drafting.holes.hole_feature_ui_main(
                    "HL-" + i, "owner", n.getY() < 0 ? face_kind_ui_main.TOP_CAP : face_kind_ui_main.BOTTOM_CAP,
                    l.getCenter().getX(), l.getCenter().getY(), l.getRadius() * 2.0, 10.0, false));
            }
            List<Float> pts = new ArrayList<>(); List<Integer> fcs = new ArrayList<>();
            ui.workspace.shapes.holes.hole_pattern_mesh_helper_ui_main.buildCylinderCapMultiHoles(
                c.getX(), c.getZ(), c.getY(), outerR, n.getY() < 0, capHoles, false, pts, fcs);
            TriangleMesh mesh = new TriangleMesh();
            float[] pArr = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pArr[i] = pts.get(i);
            mesh.getPoints().setAll(pArr); mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
            int[] fArr = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fArr[i] = fcs.get(i);
            mesh.getFaces().setAll(fArr);
            return wrapMesh(mesh, mat);
        }
        int segs = 32; var loop = innerLoops.get(0);
        Point3D hc = c.add(u.multiply(loop.getCenter().getX())).add(v.multiply(loop.getCenter().getY()));
        double inR = loop.getRadius();
        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        for (int i = 0; i < segs; i++) {
            double ang = i * 2.0 * Math.PI / segs;
            Point3D pOut = c.add(u.multiply(outerR * Math.cos(ang))).add(v.multiply(outerR * Math.sin(ang)));
            Point3D pIn  = hc.add(u.multiply(inR * Math.cos(ang))).add(v.multiply(inR * Math.sin(ang)));
            mesh.getPoints().addAll((float) pOut.getX(), (float) pOut.getY(), (float) pOut.getZ(),
                                    (float) pIn.getX(), (float) pIn.getY(), (float) pIn.getZ());
        }
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs, o1 = i * 2, i1 = i * 2 + 1, o2 = next * 2, i2 = next * 2 + 1;
            mesh.getFaces().addAll(o1, 0, o2, 1, i1, 2, o2, 1, i2, 3, i1, 2);
        }
        return wrapMesh(mesh, mat);
    }

    public static Node createCylinderLateralSurface(Point3D c, double r, double h, PhongMaterial mat) {
        int segs = 32; double cr = r + 0.20, yTop = c.getY() - h * 0.5, yBot = c.getY() + h * 0.5;
        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0, 1, 0, 0, 1, 1, 1);
        for (int i = 0; i < segs; i++) {
            double ang = i * 2.0 * Math.PI / segs, x = c.getX() + cr * Math.cos(ang), z = c.getZ() + cr * Math.sin(ang);
            mesh.getPoints().addAll((float) x, (float) yTop, (float) z, (float) x, (float) yBot, (float) z);
        }
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs, t1 = i * 2, b1 = i * 2 + 1, t2 = next * 2, b2 = next * 2 + 1;
            mesh.getFaces().addAll(t1, 0, t2, 1, b1, 2, t2, 1, b2, 3, b1, 2);
        }
        return wrapMesh(mesh, mat);
    }

    public static Node createConeLateralSurface(Point3D p1, double r, double h, PhongMaterial mat) {
        int segs = 32; double cr = r + 0.20, yBase = p1.getY(), yApex = p1.getY() - h - 0.20;
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll((float) p1.getX(), (float) yApex, (float) p1.getZ());
        mesh.getTexCoords().addAll(0.5f, 0.5f, 0, 0, 1, 0, 0, 1);
        for (int i = 0; i < segs; i++) {
            double ang = i * 2.0 * Math.PI / segs, x = p1.getX() + cr * Math.cos(ang), z = p1.getZ() + cr * Math.sin(ang);
            mesh.getPoints().addAll((float) x, (float) yBase, (float) z);
        }
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs;
            mesh.getFaces().addAll(0, 0, i + 1, 1, next + 1, 2);
        }
        return wrapMesh(mesh, mat);
    }

    public static Node createSphereSurface(Point3D c, double r, PhongMaterial mat) {
        int rings = 16, segs = 24; double sr = r + 0.20;
        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0, 1, 0, 0, 1, 1, 1);
        for (int ring = 0; ring <= rings; ring++) {
            double phi = Math.PI * ring / rings, y = c.getY() + sr * Math.cos(phi), rr = sr * Math.sin(phi);
            for (int seg = 0; seg < segs; seg++) {
                double theta = seg * 2.0 * Math.PI / segs;
                mesh.getPoints().addAll((float) (c.getX() + rr * Math.sin(theta)), (float) y, (float) (c.getZ() + rr * Math.cos(theta)));
            }
        }
        for (int ring = 0; ring < rings; ring++) {
            for (int seg = 0; seg < segs; seg++) {
                int nextSeg = (seg + 1) % segs, cur = ring * segs + seg, next = ring * segs + nextSeg;
                int bCur = (ring + 1) * segs + seg, bNext = (ring + 1) * segs + nextSeg;
                mesh.getFaces().addAll(cur, 0, next, 1, bCur, 2, next, 1, bNext, 3, bCur, 2);
            }
        }
        return wrapMesh(mesh, mat);
    }

    private static MeshView wrapMesh(TriangleMesh mesh, PhongMaterial mat) {
        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(mat);
        mv.setMouseTransparent(true);
        return mv;
    }
}
