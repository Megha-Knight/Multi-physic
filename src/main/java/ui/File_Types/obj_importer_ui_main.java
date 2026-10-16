package ui.File_Types;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * obj_importer_ui_main.java
 * High-performance Wavefront 3D Object (.obj) mesh importer.
 * Supports arbitrary polygon triangulation, vertex indexing, and CAD shape creation.
 */
public final class obj_importer_ui_main {

    private obj_importer_ui_main() {}

    public static List<shape_item_ui_main> importFromObj(File file) {
        List<shape_item_ui_main> result = new ArrayList<>();
        if (file == null || !file.exists() || file.length() == 0) return result;

        List<Point3D> rawVertices = new ArrayList<>();
        List<int[]> rawFaces = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("v ")) {
                    String[] parts = line.substring(2).trim().split("\\s+");
                    if (parts.length >= 3) {
                        double x = Double.parseDouble(parts[0]);
                        double y = Double.parseDouble(parts[1]);
                        double z = Double.parseDouble(parts[2]);
                        rawVertices.add(new Point3D(x, y, z));
                    }
                } else if (line.startsWith("f ")) {
                    String[] tokens = line.substring(2).trim().split("\\s+");
                    List<Integer> faceVerts = new ArrayList<>();
                    for (String tok : tokens) {
                        if (tok.isEmpty()) continue;
                        String vStr = tok.split("/")[0];
                        int idx = Integer.parseInt(vStr);
                        // OBJ 1-based indexing (negative is relative)
                        int resolved = (idx > 0) ? (idx - 1) : (rawVertices.size() + idx);
                        if (resolved >= 0 && resolved < rawVertices.size()) {
                            faceVerts.add(resolved);
                        }
                    }
                    // Fan triangulation for quads / n-gons
                    if (faceVerts.size() >= 3) {
                        int v0 = faceVerts.get(0);
                        for (int i = 1; i < faceVerts.size() - 1; i++) {
                            rawFaces.add(new int[]{v0, faceVerts.get(i), faceVerts.get(i + 1)});
                        }
                    }
                }
            }
        } catch (Exception e) { return result; }

        if (rawVertices.isEmpty() || rawFaces.isEmpty()) return result;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        float[] pts = new float[rawVertices.size() * 3];
        for (int i = 0; i < rawVertices.size(); i++) {
            Point3D pt = rawVertices.get(i);
            pts[i * 3]     = (float) pt.getX();
            pts[i * 3 + 1] = (float) pt.getY();
            pts[i * 3 + 2] = (float) pt.getZ();
            minX = Math.min(minX, pt.getX()); maxX = Math.max(maxX, pt.getX());
            minY = Math.min(minY, pt.getY()); maxY = Math.max(maxY, pt.getY());
            minZ = Math.min(minZ, pt.getZ()); maxZ = Math.max(maxZ, pt.getZ());
        }

        int[] fcs = new int[rawFaces.size() * 6];
        for (int i = 0; i < rawFaces.size(); i++) {
            int[] tri = rawFaces.get(i);
            fcs[i * 6]     = tri[0]; fcs[i * 6 + 1] = 0;
            fcs[i * 6 + 2] = tri[1]; fcs[i * 6 + 3] = 0;
            fcs[i * 6 + 4] = tri[2]; fcs[i * 6 + 5] = 0;
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(pts);
        mesh.getTexCoords().setAll(0.0f, 0.0f);
        mesh.getFaces().setAll(fcs);

        Point3D p1 = new Point3D(minX, minY, minZ);
        Point3D p2 = new Point3D(maxX, maxY, maxZ);

        shape_item_ui_main shape = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, p1, p2);
        String baseName = file.getName().replaceFirst("[.][^.]+$", "");
        shape.setName(baseName.isEmpty() ? "Imported_OBJ" : baseName);
        shape.getOverlays().setCustomMesh(mesh);
        shape.rebuild();

        result.add(shape);
        return result;
    }
}
