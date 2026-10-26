package ui.workspace.drafting.machining;

import javafx.scene.shape.TriangleMesh;
import java.util.ArrayList;
import java.util.List;

public final class extrude_circular_machining_helper_ui_main {

    private extrude_circular_machining_helper_ui_main() {}

    public static TriangleMesh buildMachinedCircularMesh(double radius, double length, double chamferDist, double filletRad, int blendRows) {
        int N = 32;
        boolean isFillet = filletRad > 1e-4;
        boolean isChamfer = !isFillet && chamferDist > 1e-4;
        int K = isFillet ? Math.max(1, blendRows) : (isChamfer ? 1 : 0);
        double d = isChamfer ? chamferDist : (isFillet ? filletRad : 0.0);

        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        double yCap = -length, yBase = 0.0;

        // Ring 0: Top Cap Center (Index 0)
        pts.add(0.0f); pts.add((float) yCap); pts.add(0.0f);

        // Ring 1: Inner Cap Rim (Radius R - d, elevation yCap, Indices 1..N)
        double rInner = radius - d;
        for (int i = 0; i < N; i++) {
            double theta = 2.0 * Math.PI * i / N;
            pts.add((float) (rInner * Math.cos(theta)));
            pts.add((float) yCap);
            pts.add((float) (rInner * Math.sin(theta)));
        }

        // Blend Rings (Rings 1 + k for k = 1..K)
        for (int k = 1; k <= K; k++) {
            double phi = (Math.PI * 0.5 * k) / K;
            double rK = isFillet ? (radius - d + d * Math.sin(phi)) : radius;
            double yK = isFillet ? (yCap + d * (1.0 - Math.cos(phi))) : (yCap + d);
            for (int i = 0; i < N; i++) {
                double theta = 2.0 * Math.PI * i / N;
                pts.add((float) (rK * Math.cos(theta)));
                pts.add((float) yK);
                pts.add((float) (rK * Math.sin(theta)));
            }
        }

        // Base Rim: Ring (K + 2) at radius R, elevation yBase
        for (int i = 0; i < N; i++) {
            double theta = 2.0 * Math.PI * i / N;
            pts.add((float) (radius * Math.cos(theta)));
            pts.add((float) yBase);
            pts.add((float) (radius * Math.sin(theta)));
        }

        // Base Center: Ring (K + 3) at (0, yBase, 0)
        int baseCenterIdx = 1 + (K + 2) * N;
        pts.add(0.0f); pts.add((float) yBase); pts.add(0.0f);

        // 1. Cap Disc Triangles (Outward normal: -Y)
        for (int i = 0; i < N; i++) {
            int j = (i + 1) % N;
            fcs.add(0); fcs.add(0); fcs.add(1 + i); fcs.add(0); fcs.add(1 + j); fcs.add(0);
        }

        // 2. Blend Annular Strip Quads (Outward normal: facing outward from blend)
        for (int k = 0; k < K; k++) {
            int rTop = 1 + k * N, rBot = 1 + (k + 1) * N;
            for (int i = 0; i < N; i++) {
                int j = (i + 1) % N;
                int p1 = rTop + i, p2 = rBot + i, p3 = rBot + j, p4 = rTop + j;
                fcs.add(p1); fcs.add(0); fcs.add(p2); fcs.add(0); fcs.add(p3); fcs.add(0);
                fcs.add(p1); fcs.add(0); fcs.add(p3); fcs.add(0); fcs.add(p4); fcs.add(0);
            }
        }

        // 3. Cylinder Wall Quads (Outward normal: radially outward)
        int rWallTop = 1 + K * N, rWallBot = 1 + (K + 1) * N;
        for (int i = 0; i < N; i++) {
            int j = (i + 1) % N;
            int p1 = rWallTop + i, p2 = rWallBot + i, p3 = rWallBot + j, p4 = rWallTop + j;
            fcs.add(p1); fcs.add(0); fcs.add(p2); fcs.add(0); fcs.add(p3); fcs.add(0);
            fcs.add(p1); fcs.add(0); fcs.add(p3); fcs.add(0); fcs.add(p4); fcs.add(0);
        }

        // 4. Base Disc Triangles (Outward normal: +Y)
        for (int i = 0; i < N; i++) {
            int j = (i + 1) % N;
            fcs.add(baseCenterIdx); fcs.add(0); fcs.add(rWallBot + j); fcs.add(0); fcs.add(rWallBot + i); fcs.add(0);
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0);
        float[] pArr = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pArr[i] = pts.get(i);
        mesh.getPoints().setAll(pArr);
        int[] fArr = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fArr[i] = fcs.get(i);
        mesh.getFaces().setAll(fArr);
        return mesh;
    }
}
