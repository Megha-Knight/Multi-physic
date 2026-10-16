package ui.File_Types;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * stl_importer_ui_main.java
 * High-performance ASCII and Binary Stereolithography (STL) 3D mesh importer.
 * Reconstructs CAD bodies with proper bounding extents and TriangleMesh buffers.
 */
public final class stl_importer_ui_main {

    private stl_importer_ui_main() {}

    public static List<shape_item_ui_main> importFromStl(File file) {
        List<shape_item_ui_main> result = new ArrayList<>();
        if (file == null || !file.exists() || file.length() < 84) return result;

        List<Point3D> vertices = new ArrayList<>();
        boolean readSuccess = false;

        if (isBinaryStl(file)) {
            readSuccess = readBinaryStl(file, vertices);
        }
        if (!readSuccess || vertices.isEmpty()) {
            vertices.clear();
            readSuccess = readAsciiStl(file, vertices);
        }

        if (!readSuccess || vertices.size() < 3) return result;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (Point3D p : vertices) {
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
        }

        Point3D p1 = new Point3D(minX, minY, minZ);
        Point3D p2 = new Point3D(maxX, maxY, maxZ);

        TriangleMesh mesh = new TriangleMesh();
        float[] pts = new float[vertices.size() * 3];
        int[] fcs = new int[(vertices.size() / 3) * 6];

        for (int i = 0; i < vertices.size(); i++) {
            Point3D pt = vertices.get(i);
            pts[i * 3]     = (float) pt.getX();
            pts[i * 3 + 1] = (float) pt.getY();
            pts[i * 3 + 2] = (float) pt.getZ();
        }

        int triCount = vertices.size() / 3;
        for (int t = 0; t < triCount; t++) {
            int v0 = t * 3, v1 = t * 3 + 1, v2 = t * 3 + 2;
            fcs[t * 6]     = v0; fcs[t * 6 + 1] = 0;
            fcs[t * 6 + 2] = v1; fcs[t * 6 + 3] = 0;
            fcs[t * 6 + 4] = v2; fcs[t * 6 + 5] = 0;
        }

        mesh.getPoints().setAll(pts);
        mesh.getTexCoords().setAll(0.0f, 0.0f);
        mesh.getFaces().setAll(fcs);

        shape_item_ui_main shape = new shape_item_ui_main(basic_shapes_ui_main.CUBOID, p1, p2);
        String baseName = file.getName().replaceFirst("[.][^.]+$", "");
        shape.setName(baseName.isEmpty() ? "Imported_STL" : baseName);
        shape.getOverlays().setCustomMesh(mesh);
        shape.rebuild();

        result.add(shape);
        return result;
    }

    private static boolean isBinaryStl(File f) {
        long len = f.length();
        if (len < 84) return false;
        try (InputStream is = new FileInputStream(f)) {
            byte[] hdr = is.readNBytes(84);
            if (hdr.length < 84) return false;
            ByteBuffer bb = ByteBuffer.wrap(hdr, 80, 4).order(ByteOrder.LITTLE_ENDIAN);
            long numTriangles = Integer.toUnsignedLong(bb.getInt());
            return len == (84 + numTriangles * 50);
        } catch (Exception e) { return false; }
    }

    private static boolean readBinaryStl(File f, List<Point3D> out) {
        try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
            byte[] header = is.readNBytes(80);
            byte[] countBytes = is.readNBytes(4);
            if (header.length < 80 || countBytes.length < 4) return false;
            ByteBuffer bb = ByteBuffer.wrap(countBytes).order(ByteOrder.LITTLE_ENDIAN);
            int count = bb.getInt();
            byte[] triBuffer = new byte[50];
            ByteBuffer triBb = ByteBuffer.wrap(triBuffer).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < count; i++) {
                int read = is.readNBytes(triBuffer, 0, 50);
                if (read < 50) break;
                triBb.position(12); // Skip 12-byte normal vector
                float v1x = triBb.getFloat(), v1y = triBb.getFloat(), v1z = triBb.getFloat();
                float v2x = triBb.getFloat(), v2y = triBb.getFloat(), v2z = triBb.getFloat();
                float v3x = triBb.getFloat(), v3y = triBb.getFloat(), v3z = triBb.getFloat();
                out.add(new Point3D(v1x, v1y, v1z));
                out.add(new Point3D(v2x, v2y, v2z));
                out.add(new Point3D(v3x, v3y, v3z));
            }
            return !out.isEmpty();
        } catch (Exception e) { return false; }
    }

    private static boolean readAsciiStl(File f, List<Point3D> out) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("vertex")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        out.add(new Point3D(x, y, z));
                    }
                }
            }
            return !out.isEmpty();
        } catch (Exception e) { return false; }
    }
}
