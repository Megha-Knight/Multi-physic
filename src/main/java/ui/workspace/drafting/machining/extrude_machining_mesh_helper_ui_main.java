package ui.workspace.drafting.machining;

import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.*;

public final class extrude_machining_mesh_helper_ui_main {

    private extrude_machining_mesh_helper_ui_main() {}

    public static Node buildMachinedExtrusionNode(shape_item_ui_main host, extrude_feature_ui_main ext, PhongMaterial mat) {
        if (host == null || ext == null) return null;

        List<fillet_feature_ui_main> fillets = new ArrayList<>();
        for (var f : host.getFillets()) {
            if (f.getEdgeId() != null && f.getEdgeId().contains(ext.getId())) fillets.add(f);
        }
        List<chamfer_feature_ui_main> chamfers = new ArrayList<>();
        for (var c : host.getChamfers()) {
            if (c.getEdgeId() != null && c.getEdgeId().contains(ext.getId())) chamfers.add(c);
        }

        if (fillets.isEmpty() && chamfers.isEmpty()) return null; // Not machined

        // Reject conflicting duplicate features targeting identical edge
        Set<String> targetEdges = new HashSet<>();
        for (var f : fillets) {
            if (!targetEdges.add(f.getEdgeId())) return null;
        }
        for (var c : chamfers) {
            if (!targetEdges.add(c.getEdgeId())) return null;
        }

        // Multi-constraint parameter validation
        double maxDim = Math.min(ext.getDiameter(), ext.getHeight()) * 0.49;
        for (var f : fillets) {
            if (f.getRadius() <= 0.0 || f.getRadius() > maxDim) return null;
        }
        for (var c : chamfers) {
            if (c.getDistance() <= 0.0 || c.getDistance() > maxDim) return null;
        }

        TriangleMesh mesh;
        CutoutShape ps = ext.getProfileShape();

        if (ps == CutoutShape.CIRCLE) {
            double fRad = fillets.isEmpty() ? 0.0 : fillets.get(0).getRadius();
            double cDist = chamfers.isEmpty() ? 0.0 : chamfers.get(0).getDistance();
            mesh = extrude_circular_machining_helper_ui_main.buildMachinedCircularMesh(ext.getRadius(), ext.getHeight(), cDist, fRad, 4);
        } else if (ps == CutoutShape.EQUILATERAL_TRIANGLE) {
            double blend = !chamfers.isEmpty() ? chamfers.get(0).getDistance() : fillets.get(0).getRadius();
            mesh = extrude_prism_machining_helper_ui_main.buildMachinedPrismMesh(ext.getDiameter(), ext.getHeight(), blend, false);
        } else if (ps == CutoutShape.RIGHT_TRIANGLE) {
            double blend = !chamfers.isEmpty() ? chamfers.get(0).getDistance() : fillets.get(0).getRadius();
            mesh = extrude_prism_machining_helper_ui_main.buildMachinedPrismMesh(ext.getDiameter(), ext.getHeight(), blend, true);
        } else {
            double w = ext.getDiameter();
            double d = ext.getWidth2() > 0.1 ? ext.getWidth2() : w;
            double h = ext.getHeight();
            double blend = !chamfers.isEmpty() ? chamfers.get(0).getDistance() : fillets.get(0).getRadius();
            mesh = extrude_box_machining_helper_ui_main.buildMachinedBoxMesh(w, h, d, blend);
        }

        MeshView mv = new MeshView(mesh);
        if (mat != null) mv.setMaterial(mat);
        mv.setUserData(ext);
        return mv;
    }
}
