package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.topology.topology_boundary_loop_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * face_overlay_mesh_helper_ui_main.java
 * Generates 3D highlight surface meshes for planar, circular, and hole-cutout CAD faces.
 */
public final class face_overlay_mesh_helper_ui_main {

    private face_overlay_mesh_helper_ui_main() {}

    public static Node createRectSurface(Point3D center, Point3D u, Point3D v, Point3D n, double w, double h, PhongMaterial mat) {
        double hw = w * 0.5, hh = h * 0.5;
        Point3D offset = n.multiply(0.20);
        Point3D p0 = center.add(u.multiply(hw)).add(v.multiply(hh)).add(offset);
        Point3D p1 = center.add(u.multiply(-hw)).add(v.multiply(hh)).add(offset);
        Point3D p2 = center.add(u.multiply(-hw)).add(v.multiply(-hh)).add(offset);
        Point3D p3 = center.add(u.multiply(hw)).add(v.multiply(-hh)).add(offset);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
            (float) p0.getX(), (float) p0.getY(), (float) p0.getZ(),
            (float) p1.getX(), (float) p1.getY(), (float) p1.getZ(),
            (float) p2.getX(), (float) p2.getY(), (float) p2.getZ(),
            (float) p3.getX(), (float) p3.getY(), (float) p3.getZ()
        );
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        mesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 0, 0, 2, 2, 3, 3);
        return wrapMesh(mesh, mat);
    }

    public static Node createDiskSurface(Point3D center, Point3D u, Point3D v, Point3D n, double r, PhongMaterial mat) {
        int segs = 32;
        Point3D c = center.add(n.multiply(0.20));
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll((float) c.getX(), (float) c.getY(), (float) c.getZ());
        mesh.getTexCoords().addAll(0.5f, 0.5f);
        for (int i = 0; i < segs; i++) {
            double ang = i * 2.0 * Math.PI / segs;
            Point3D pt = c.add(u.multiply(r * Math.cos(ang))).add(v.multiply(r * Math.sin(ang)));
            mesh.getPoints().addAll((float) pt.getX(), (float) pt.getY(), (float) pt.getZ());
            mesh.getTexCoords().addAll((float) (0.5 + 0.5 * Math.cos(ang)), (float) (0.5 + 0.5 * Math.sin(ang)));
        }
        for (int i = 0; i < segs; i++) {
            int next = (i + 1) % segs;
            mesh.getFaces().addAll(0, 0, i + 1, i + 1, next + 1, next + 1);
        }
        return wrapMesh(mesh, mat);
    }

    public static Node createAnnularRectSurface(Point3D center, Point3D u, Point3D v, Point3D n, double w, double h,
                                                List<topology_boundary_loop_ui_main> innerLoops, PhongMaterial mat) {
        if (innerLoops == null || innerLoops.isEmpty()) return createRectSurface(center, u, v, n, w, h, mat);
        double hw = w * 0.5, hh = h * 0.5;
        Point3D c = center.add(n.multiply(0.20));
        Point3D[] oct = new Point3D[] {
            c.add(u.multiply(hw)).add(v.multiply(hh)), c.add(u.multiply(0)).add(v.multiply(hh)),
            c.add(u.multiply(-hw)).add(v.multiply(hh)), c.add(u.multiply(-hw)).add(v.multiply(0)),
            c.add(u.multiply(-hw)).add(v.multiply(-hh)), c.add(u.multiply(0)).add(v.multiply(-hh)),
            c.add(u.multiply(hw)).add(v.multiply(-hh)), c.add(u.multiply(hw)).add(v.multiply(0))
        };
        var loop = innerLoops.get(0);
        Point3D hc = c.add(u.multiply(loop.getCenter().getX())).add(v.multiply(loop.getCenter().getY()));
        Point3D[] circ = hole_mesh_triangulator_ui_main.getCirclePoints(hc, u, v, loop.getRadius(), 32);
        List<Float> pts = new ArrayList<>(); List<Integer> fcs = new ArrayList<>();
        hole_mesh_triangulator_ui_main.triangulateAnnularFace(oct, circ, pts, fcs, false);
        TriangleMesh mesh = new TriangleMesh();
        float[] pArr = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pArr[i] = pts.get(i);
        mesh.getPoints().setAll(pArr);
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        int[] fArr = new int[fcs.size() * 2];
        for (int i = 0; i < fcs.size(); i++) { fArr[i * 2] = fcs.get(i); fArr[i * 2 + 1] = i % 4; }
        mesh.getFaces().setAll(fArr);
        return wrapMesh(mesh, mat);
    }

    public static Node createAnnularDiskSurface(Point3D center, Point3D u, Point3D v, Point3D n, double outerR,
                                                List<topology_boundary_loop_ui_main> innerLoops, PhongMaterial mat) {
        if (innerLoops == null || innerLoops.isEmpty()) return createDiskSurface(center, u, v, n, outerR, mat);
        int segs = 32;
        Point3D c = center.add(n.multiply(0.20));
        var loop = innerLoops.get(0);
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

    private static MeshView wrapMesh(TriangleMesh mesh, PhongMaterial mat) {
        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(mat);
        mv.setMouseTransparent(true);
        return mv;
    }
}
