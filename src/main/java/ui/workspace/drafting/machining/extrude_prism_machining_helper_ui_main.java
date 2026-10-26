package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import java.util.ArrayList;
import java.util.List;

public final class extrude_prism_machining_helper_ui_main {

    private extrude_prism_machining_helper_ui_main() {}

    public static TriangleMesh buildMachinedPrismMesh(double side, double len, double blendDist, boolean isRightTriangle) {
        double d = Math.min(blendDist, side * 0.2);
        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        Point3D P0, P1, P2;
        if (!isRightTriangle) {
            double hTri = side * Math.sqrt(3) * 0.5;
            P0 = new Point3D(-side * 0.5, 0, -hTri / 3.0);
            P1 = new Point3D(side * 0.5, 0, -hTri / 3.0);
            P2 = new Point3D(0, 0, 2.0 * hTri / 3.0);
        } else {
            P0 = new Point3D(-side * 0.5, 0, -side * 0.5);
            P1 = new Point3D(side * 0.5, 0, -side * 0.5);
            P2 = new Point3D(-side * 0.5, 0, side * 0.5);
        }

        Point3D centroid = P0.add(P1).add(P2).multiply(1.0 / 3.0);

        Point3D C0 = P0.add(centroid.subtract(P0).normalize().multiply(d * 1.414));
        Point3D C1 = P1.add(centroid.subtract(P1).normalize().multiply(d * 1.414));
        Point3D C2 = P2.add(centroid.subtract(P2).normalize().multiply(d * 1.414));

        pts.add((float) C0.getX()); pts.add((float) -len); pts.add((float) C0.getZ()); // 0
        pts.add((float) C1.getX()); pts.add((float) -len); pts.add((float) C1.getZ()); // 1
        pts.add((float) C2.getX()); pts.add((float) -len); pts.add((float) C2.getZ()); // 2

        Point3D d01 = P1.subtract(P0).normalize().multiply(d);
        Point3D d12 = P2.subtract(P1).normalize().multiply(d);
        Point3D d20 = P0.subtract(P2).normalize().multiply(d);

        Point3D W01 = P0.add(d01), W10 = P1.subtract(d01);
        Point3D W12 = P1.add(d12), W21 = P2.subtract(d12);
        Point3D W20 = P2.add(d20), W02 = P0.subtract(d20);

        double yRim = -len + d;
        pts.add((float) W01.getX()); pts.add((float) yRim); pts.add((float) W01.getZ()); // 3
        pts.add((float) W10.getX()); pts.add((float) yRim); pts.add((float) W10.getZ()); // 4
        pts.add((float) W12.getX()); pts.add((float) yRim); pts.add((float) W12.getZ()); // 5
        pts.add((float) W21.getX()); pts.add((float) yRim); pts.add((float) W21.getZ()); // 6
        pts.add((float) W20.getX()); pts.add((float) yRim); pts.add((float) W20.getZ()); // 7
        pts.add((float) W02.getX()); pts.add((float) yRim); pts.add((float) W02.getZ()); // 8

        pts.add((float) W01.getX()); pts.add(0.0f); pts.add((float) W01.getZ()); // 9
        pts.add((float) W10.getX()); pts.add(0.0f); pts.add((float) W10.getZ()); // 10
        pts.add((float) W12.getX()); pts.add(0.0f); pts.add((float) W12.getZ()); // 11
        pts.add((float) W21.getX()); pts.add(0.0f); pts.add((float) W21.getZ()); // 12
        pts.add((float) W20.getX()); pts.add(0.0f); pts.add((float) W20.getZ()); // 13
        pts.add((float) W02.getX()); pts.add(0.0f); pts.add((float) W02.getZ()); // 14

        pts.add((float) centroid.getX()); pts.add(0.0f); pts.add((float) centroid.getZ()); // 15

        // 1. Top Cap Triangle (normal in -Y)
        fcs.add(0); fcs.add(0); fcs.add(1); fcs.add(0); fcs.add(2); fcs.add(0);

        // 2. 3 Cap Bevel Quads
        fcs.add(0); fcs.add(0); fcs.add(4); fcs.add(0); fcs.add(1); fcs.add(0);
        fcs.add(0); fcs.add(0); fcs.add(3); fcs.add(0); fcs.add(4); fcs.add(0);

        fcs.add(1); fcs.add(0); fcs.add(6); fcs.add(0); fcs.add(2); fcs.add(0);
        fcs.add(1); fcs.add(0); fcs.add(5); fcs.add(0); fcs.add(6); fcs.add(0);

        fcs.add(2); fcs.add(0); fcs.add(8); fcs.add(0); fcs.add(0); fcs.add(0);
        fcs.add(2); fcs.add(0); fcs.add(7); fcs.add(0); fcs.add(8); fcs.add(0);

        // 3. 3 Corner Patches at top corners
        fcs.add(0); fcs.add(0); fcs.add(8); fcs.add(0); fcs.add(3); fcs.add(0);
        fcs.add(1); fcs.add(0); fcs.add(4); fcs.add(0); fcs.add(5); fcs.add(0);
        fcs.add(2); fcs.add(0); fcs.add(6); fcs.add(0); fcs.add(7); fcs.add(0);

        // 4. 3 Side Wall Quads down to base
        fcs.add(3); fcs.add(0); fcs.add(10); fcs.add(0); fcs.add(4); fcs.add(0);
        fcs.add(3); fcs.add(0); fcs.add(9); fcs.add(0); fcs.add(10); fcs.add(0);

        fcs.add(5); fcs.add(0); fcs.add(12); fcs.add(0); fcs.add(6); fcs.add(0);
        fcs.add(5); fcs.add(0); fcs.add(11); fcs.add(0); fcs.add(12); fcs.add(0);

        fcs.add(7); fcs.add(0); fcs.add(14); fcs.add(0); fcs.add(8); fcs.add(0);
        fcs.add(7); fcs.add(0); fcs.add(13); fcs.add(0); fcs.add(14); fcs.add(0);

        // 5. 3 Vertical Corner Bevel Quads
        fcs.add(8); fcs.add(0); fcs.add(9); fcs.add(0); fcs.add(3); fcs.add(0);
        fcs.add(8); fcs.add(0); fcs.add(14); fcs.add(0); fcs.add(9); fcs.add(0);

        fcs.add(4); fcs.add(0); fcs.add(11); fcs.add(0); fcs.add(5); fcs.add(0);
        fcs.add(4); fcs.add(0); fcs.add(10); fcs.add(0); fcs.add(11); fcs.add(0);

        fcs.add(6); fcs.add(0); fcs.add(13); fcs.add(0); fcs.add(7); fcs.add(0);
        fcs.add(6); fcs.add(0); fcs.add(12); fcs.add(0); fcs.add(13); fcs.add(0);

        // 6. Base Fan Triangles (normal in +Y)
        fcs.add(15); fcs.add(0); fcs.add(10); fcs.add(0); fcs.add(9); fcs.add(0);
        fcs.add(15); fcs.add(0); fcs.add(11); fcs.add(0); fcs.add(10); fcs.add(0);
        fcs.add(15); fcs.add(0); fcs.add(12); fcs.add(0); fcs.add(11); fcs.add(0);
        fcs.add(15); fcs.add(0); fcs.add(13); fcs.add(0); fcs.add(12); fcs.add(0);
        fcs.add(15); fcs.add(0); fcs.add(14); fcs.add(0); fcs.add(13); fcs.add(0);
        fcs.add(15); fcs.add(0); fcs.add(9); fcs.add(0); fcs.add(14); fcs.add(0);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0);
        float[] pArr = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pArr[i] = pts.get(i);
        mesh.getPoints().setAll(pArr);
        int[] fArr = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fArr[i] = fcs.get(i);
        mesh.getFaces().setAll(fArr);
        return mesh;
    }
}
