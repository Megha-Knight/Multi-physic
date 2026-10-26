package ui.workspace.drafting.extrude;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;

public final class extrude_prism_mesh_helper_ui_main {

    private extrude_prism_mesh_helper_ui_main() {}

    public static Node buildPrismNode(hole_mesh_triangulator_ui_main.Frame f, extrude_feature_ui_main ext, PhongMaterial mat) {
        if (f == null || ext == null) return null;
        double w = ext.getDiameter(), len = ext.getHeight();
        double d = (ext.getWidth2() > 0.1) ? ext.getWidth2() : w;
        if (w <= 1e-4 || d <= 1e-4 || len <= 1e-4) return null;
        Point3D baseCenter = f.origin().add(f.u().multiply(ext.getU())).add(f.v().multiply(ext.getV()));

        Point2D[] pts2d = new Point2D[3];
        if (ext.getProfileShape() == CutoutShape.RIGHT_TRIANGLE) {
            pts2d[0] = new Point2D(-w * 0.5, -d * 0.5);
            pts2d[1] = new Point2D(w * 0.5, -d * 0.5);
            pts2d[2] = new Point2D(-w * 0.5, d * 0.5);
        } else {
            double ht = w * Math.sqrt(3.0) * 0.5;
            pts2d[0] = new Point2D(-w * 0.5, -ht / 3.0);
            pts2d[1] = new Point2D(w * 0.5, -ht / 3.0);
            pts2d[2] = new Point2D(0.0, 2.0 * ht / 3.0);
        }

        Point3D[] b = new Point3D[3], t = new Point3D[3];
        for (int i = 0; i < 3; i++) {
            b[i] = baseCenter.add(f.u().multiply(pts2d[i].getX())).add(f.v().multiply(pts2d[i].getY()));
            t[i] = b[i].add(f.n().multiply(len));
        }

        float[] points = new float[]{
            (float) b[0].getX(), (float) b[0].getY(), (float) b[0].getZ(),
            (float) b[1].getX(), (float) b[1].getY(), (float) b[1].getZ(),
            (float) b[2].getX(), (float) b[2].getY(), (float) b[2].getZ(),
            (float) t[0].getX(), (float) t[0].getY(), (float) t[0].getZ(),
            (float) t[1].getX(), (float) t[1].getY(), (float) t[1].getZ(),
            (float) t[2].getX(), (float) t[2].getY(), (float) t[2].getZ()
        };

        float[] texCoords = {0, 0, 1, 0, 0, 1, 1, 1};
        int[] faces = new int[]{
            // Bottom cap (outward normal)
            0, 0, 2, 2, 1, 1,
            // Top cap (outward normal)
            3, 0, 4, 1, 5, 2,
            // Side 0 (0-1-4-3)
            0, 0, 1, 1, 4, 3,  0, 0, 4, 3, 3, 2,
            // Side 1 (1-2-5-4)
            1, 0, 2, 1, 5, 3,  1, 0, 5, 3, 4, 2,
            // Side 2 (2-0-3-5)
            2, 0, 0, 1, 3, 3,  2, 0, 3, 3, 5, 2
        };

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(mat);
        return mv;
    }
}
