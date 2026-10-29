package ui.workspace.drafting.sketch;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.geometry_native_mesh_adapter_ui_main;
import backend.native_bridge.astra_geometry_native_bridge_ui_main;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;

public final class sketch_extrude_bridge_ui_main {
    private sketch_extrude_bridge_ui_main() {}

    public static Group buildExtrusion(sketch_feature_ui_main sketch, double height, boolean selected) {
        return buildExtrusion(null, sketch, height, selected);
    }

    public static Group buildExtrusion(shape_item_ui_main shape, sketch_feature_ui_main sketch, double height, boolean selected) {
        Group grp = new Group();
        if (sketch == null || !sketch.isValid() || height <= 1e-4) return grp;
        if (sketch.getProfileResult() == null) sketch.regenerate();
        var profRes = sketch.getProfileResult();
        if (profRes == null) return grp;
        sketch_profile_loop_ui_main outer = profRes.outerLoop();
        if (outer == null || outer.size() < 3 || outer.getAbsArea() < 1e-4) return grp;

        List<sketch_point_2d_ui_main> pts2d = outer.getVertices();
        if (outer.getSignedArea() < 0) java.util.Collections.reverse(pts2d);
        int n = pts2d.size();

        Point3D origin, normal, uAxis, vAxis;
        if (shape != null && sketch.getFaceKind() != null) {
            var f = ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main.getLocalFaceFrame(shape, sketch.getFaceKind());
            if (f != null) { origin = f.origin(); normal = f.n(); uAxis = f.u(); vAxis = f.v(); }
            else { sketch_coord_system_ui_main cs = sketch.getCoordSystem(); origin = cs.getOrigin(); normal = cs.getNormal(); uAxis = cs.getUAxis(); vAxis = cs.getVAxis(); }
        } else {
            sketch_coord_system_ui_main cs = sketch.getCoordSystem();
            origin = cs.getOrigin(); normal = cs.getNormal(); uAxis = cs.getUAxis(); vAxis = cs.getVAxis();
        }

        var bridge = astra_geometry_native_bridge_ui_main.getInstance();
        if (bridge.isAvailable()) {
            MeshView nativeMv = tryBuildNative(bridge, sketch, height, selected, origin, normal, uAxis, vAxis, pts2d);
            if (nativeMv != null) { grp.getChildren().add(nativeMv); return grp; }
        }

        float[] points = new float[n * 2 * 3];
        for (int i = 0; i < n; i++) {
            sketch_point_2d_ui_main p2 = pts2d.get(i);
            Point3D basePt = origin.add(uAxis.multiply(p2.x())).add(vAxis.multiply(p2.y()));
            Point3D topPt = basePt.add(normal.multiply(height));
            points[i * 3]     = (float) basePt.getX();
            points[i * 3 + 1] = (float) basePt.getY();
            points[i * 3 + 2] = (float) basePt.getZ();
            points[(n + i) * 3]     = (float) topPt.getX();
            points[(n + i) * 3 + 1] = (float) topPt.getY();
            points[(n + i) * 3 + 2] = (float) topPt.getZ();
        }

        float[] texCoords = {0, 0, 1, 0, 0, 1, 1, 1};
        int numFaces = (n - 2) * 2 + n * 2;
        int[] faces = new int[numFaces * 6];
        int fIdx = 0;
        for (int i = 1; i < n - 1; i++) {
            faces[fIdx++] = 0; faces[fIdx++] = 0; faces[fIdx++] = i + 1; faces[fIdx++] = 1; faces[fIdx++] = i; faces[fIdx++] = 2;
        }
        for (int i = 1; i < n - 1; i++) {
            faces[fIdx++] = n; faces[fIdx++] = 0; faces[fIdx++] = n + i; faces[fIdx++] = 1; faces[fIdx++] = n + i + 1; faces[fIdx++] = 2;
        }
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n, b1 = i, b2 = next, t1 = n + i, t2 = n + next;
            faces[fIdx++] = b1; faces[fIdx++] = 0; faces[fIdx++] = b2; faces[fIdx++] = 1; faces[fIdx++] = t2; faces[fIdx++] = 2;
            faces[fIdx++] = b1; faces[fIdx++] = 0; faces[fIdx++] = t2; faces[fIdx++] = 2; faces[fIdx++] = t1; faces[fIdx++] = 3;
        }

        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);

        MeshView mv = new MeshView(mesh);
        Color color = selected ? Color.web("#F59E0B") : Color.web("#3B82F6");
        PhongMaterial mat = new PhongMaterial(color);
        mat.setSpecularColor(Color.WHITE);
        mv.setMaterial(mat);
        grp.getChildren().add(mv);
        return grp;
    }

    private static MeshView tryBuildNative(astra_geometry_native_bridge_ui_main bridge, sketch_feature_ui_main sk,
                                           double h, boolean sel, Point3D org, Point3D nml, Point3D u, Point3D v,
                                           List<sketch_point_2d_ui_main> pts) {
        try (var model = bridge.createModel(); Arena arena = Arena.ofConfined()) {
            MemorySegment profReq = arena.allocate(184);
            profReq.set(ValueLayout.JAVA_INT, 0, 184); profReq.set(ValueLayout.JAVA_INT, 4, 1);
            long fId = sk.getId().hashCode();
            profReq.set(ValueLayout.JAVA_LONG, 8, fId); profReq.set(ValueLayout.JAVA_LONG, 16, fId);
            profReq.set(ValueLayout.JAVA_INT, 48, 120); profReq.set(ValueLayout.JAVA_INT, 52, 1); profReq.set(ValueLayout.JAVA_INT, 56, pts.size());
            profReq.set(ValueLayout.JAVA_DOUBLE, 64, org.getX()); profReq.set(ValueLayout.JAVA_DOUBLE, 72, org.getY()); profReq.set(ValueLayout.JAVA_DOUBLE, 80, org.getZ());
            profReq.set(ValueLayout.JAVA_DOUBLE, 88, u.getX()); profReq.set(ValueLayout.JAVA_DOUBLE, 96, u.getY()); profReq.set(ValueLayout.JAVA_DOUBLE, 104, u.getZ());
            profReq.set(ValueLayout.JAVA_DOUBLE, 112, v.getX()); profReq.set(ValueLayout.JAVA_DOUBLE, 120, v.getY()); profReq.set(ValueLayout.JAVA_DOUBLE, 128, v.getZ());
            MemorySegment coords = arena.allocate(pts.size() * 16L);
            for (int i = 0; i < pts.size(); i++) {
                coords.set(ValueLayout.JAVA_DOUBLE, i * 16L, pts.get(i).x());
                coords.set(ValueLayout.JAVA_DOUBLE, i * 16L + 8L, pts.get(i).y());
            }
            profReq.set(ValueLayout.ADDRESS, 136, coords);
            try (var profRes = bridge.evaluateFeature(model, profReq)) {
                MemorySegment extReq = arena.allocate(184);
                extReq.set(ValueLayout.JAVA_INT, 0, 184); extReq.set(ValueLayout.JAVA_INT, 4, 3);
                extReq.set(ValueLayout.JAVA_LONG, 8, fId + 1); extReq.set(ValueLayout.JAVA_LONG, 16, fId + 1);
                extReq.set(ValueLayout.JAVA_INT, 48, 56);
                extReq.set(ValueLayout.JAVA_LONG, 56, fId); extReq.set(ValueLayout.JAVA_LONG, 64, fId);
                extReq.set(ValueLayout.JAVA_DOUBLE, 72, nml.getX()); extReq.set(ValueLayout.JAVA_DOUBLE, 80, nml.getY()); extReq.set(ValueLayout.JAVA_DOUBLE, 88, nml.getZ());
                extReq.set(ValueLayout.JAVA_DOUBLE, 96, h);
                try (var extRes = bridge.evaluateFeature(model, extReq)) {
                    var snapshot = bridge.copyTessellationSnapshot(extRes);
                    return geometry_native_mesh_adapter_ui_main.createMeshView(snapshot, sel);
                }
            }
        } catch (Throwable ignored) { return null; }
    }
}
