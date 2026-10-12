package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * face_overlay_mesh_helper_ui_main.java
 * Generates 3D highlight surface meshes for planar and circular CAD faces.
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
        mesh.getFaces().addAll(
            0, 0, 1, 1, 2, 2,
            0, 0, 2, 2, 3, 3
        );

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(mat);
        mv.setMouseTransparent(true);
        return mv;
    }

    public static Node createDiskSurface(Point3D center, Point3D u, Point3D v, Point3D n, double r, PhongMaterial mat) {
        int segs = 32;
        Point3D offset = n.multiply(0.20);
        Point3D c = center.add(offset);

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

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(mat);
        mv.setMouseTransparent(true);
        return mv;
    }
}
