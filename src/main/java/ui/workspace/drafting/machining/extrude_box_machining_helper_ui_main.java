package ui.workspace.drafting.machining;

import javafx.scene.shape.TriangleMesh;
import java.util.ArrayList;
import java.util.List;

public final class extrude_box_machining_helper_ui_main {

    private extrude_box_machining_helper_ui_main() {}

    public static TriangleMesh buildMachinedBoxMesh(double w, double h, double d, double blendDist) {
        double hw = w * 0.5, hd = d * 0.5;
        double ch = Math.min(blendDist, Math.min(w, Math.min(h, d)) * 0.45);

        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        // 4 Inner Cap Rim Vertices (Y = -h)
        pts.add((float) (-hw + ch)); pts.add((float) -h); pts.add((float) (-hd + ch)); // 0: Top-Back-Left inner
        pts.add((float) (hw - ch));  pts.add((float) -h); pts.add((float) (-hd + ch)); // 1: Top-Back-Right inner
        pts.add((float) (hw - ch));  pts.add((float) -h); pts.add((float) (hd - ch));  // 2: Top-Front-Right inner
        pts.add((float) (-hw + ch)); pts.add((float) -h); pts.add((float) (hd - ch));  // 3: Top-Front-Left inner

        // 8 Top Outer Wall Rim Vertices (Y = -h + ch)
        pts.add((float) (-hw + ch)); pts.add((float) (-h + ch)); pts.add((float) -hd); // 4
        pts.add((float) (hw - ch));  pts.add((float) (-h + ch)); pts.add((float) -hd); // 5
        pts.add((float) hw); pts.add((float) (-h + ch)); pts.add((float) (-hd + ch)); // 6
        pts.add((float) hw); pts.add((float) (-h + ch)); pts.add((float) (hd - ch));  // 7
        pts.add((float) (hw - ch));  pts.add((float) (-h + ch)); pts.add((float) hd);  // 8
        pts.add((float) (-hw + ch)); pts.add((float) (-h + ch)); pts.add((float) hd);  // 9
        pts.add((float) -hw); pts.add((float) (-h + ch)); pts.add((float) (hd - ch));  // 10
        pts.add((float) -hw); pts.add((float) (-h + ch)); pts.add((float) (-hd + ch)); // 11

        // 4 Base Vertices (Y = 0)
        pts.add((float) -hw); pts.add(0.0f); pts.add((float) -hd); // 12
        pts.add((float) hw);  pts.add(0.0f); pts.add((float) -hd); // 13
        pts.add((float) hw);  pts.add(0.0f); pts.add((float) hd);  // 14
        pts.add((float) -hw); pts.add(0.0f); pts.add((float) hd);  // 15

        // Cap Quad: outward normal points in -Y (towards exterior)
        fcs.add(0); fcs.add(0); fcs.add(1); fcs.add(0); fcs.add(2); fcs.add(0);
        fcs.add(0); fcs.add(0); fcs.add(2); fcs.add(0); fcs.add(3); fcs.add(0);

        // 4 Bevel Quads along cap edges
        fcs.add(0); fcs.add(0); fcs.add(5); fcs.add(0); fcs.add(1); fcs.add(0);
        fcs.add(0); fcs.add(0); fcs.add(4); fcs.add(0); fcs.add(5); fcs.add(0);

        fcs.add(1); fcs.add(0); fcs.add(7); fcs.add(0); fcs.add(2); fcs.add(0);
        fcs.add(1); fcs.add(0); fcs.add(6); fcs.add(0); fcs.add(7); fcs.add(0);

        fcs.add(2); fcs.add(0); fcs.add(9); fcs.add(0); fcs.add(3); fcs.add(0);
        fcs.add(2); fcs.add(0); fcs.add(8); fcs.add(0); fcs.add(9); fcs.add(0);

        fcs.add(3); fcs.add(0); fcs.add(11); fcs.add(0); fcs.add(0); fcs.add(0);
        fcs.add(3); fcs.add(0); fcs.add(10); fcs.add(0); fcs.add(11); fcs.add(0);

        // 4 Corner Patches connecting bevel terminations
        fcs.add(0); fcs.add(0); fcs.add(11); fcs.add(0); fcs.add(4); fcs.add(0);
        fcs.add(1); fcs.add(0); fcs.add(5); fcs.add(0); fcs.add(6); fcs.add(0);
        fcs.add(2); fcs.add(0); fcs.add(7); fcs.add(0); fcs.add(8); fcs.add(0);
        fcs.add(3); fcs.add(0); fcs.add(9); fcs.add(0); fcs.add(10); fcs.add(0);

        // 4 Side Quads down to Base
        fcs.add(4); fcs.add(0); fcs.add(13); fcs.add(0); fcs.add(5); fcs.add(0);
        fcs.add(4); fcs.add(0); fcs.add(12); fcs.add(0); fcs.add(13); fcs.add(0);

        fcs.add(6); fcs.add(0); fcs.add(14); fcs.add(0); fcs.add(7); fcs.add(0);
        fcs.add(6); fcs.add(0); fcs.add(13); fcs.add(0); fcs.add(14); fcs.add(0);

        fcs.add(8); fcs.add(0); fcs.add(15); fcs.add(0); fcs.add(9); fcs.add(0);
        fcs.add(8); fcs.add(0); fcs.add(14); fcs.add(0); fcs.add(15); fcs.add(0);

        fcs.add(10); fcs.add(0); fcs.add(12); fcs.add(0); fcs.add(11); fcs.add(0);
        fcs.add(10); fcs.add(0); fcs.add(15); fcs.add(0); fcs.add(12); fcs.add(0);

        // 4 Side Corner Bevels down to Base
        fcs.add(11); fcs.add(0); fcs.add(12); fcs.add(0); fcs.add(4); fcs.add(0);
        fcs.add(5); fcs.add(0); fcs.add(13); fcs.add(0); fcs.add(6); fcs.add(0);
        fcs.add(7); fcs.add(0); fcs.add(14); fcs.add(0); fcs.add(8); fcs.add(0);
        fcs.add(9); fcs.add(0); fcs.add(15); fcs.add(0); fcs.add(10); fcs.add(0);

        // Base Quad (Y = 0): outward normal points in +Y
        fcs.add(12); fcs.add(0); fcs.add(14); fcs.add(0); fcs.add(13); fcs.add(0);
        fcs.add(12); fcs.add(0); fcs.add(15); fcs.add(0); fcs.add(14); fcs.add(0);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0);
        float[] pArr = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pArr[i] = pts.get(i);
        mesh.getPoints().setAll(pArr);
        int[] fArr = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fArr[i] = fcs.get(i);
        mesh.getFaces().setAll(fArr);
        return mesh;
    }
}
