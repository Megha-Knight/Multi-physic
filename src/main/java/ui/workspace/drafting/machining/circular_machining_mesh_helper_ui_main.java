package ui.workspace.drafting.machining;

import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_geometry_helper_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;
import ui.workspace.drafting.topology.topology_face_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * circular_machining_mesh_helper_ui_main.java
 * Generates 3D mesh geometry for conical chamfer frustums and toroidal fillets on circular edges.
 */
public final class circular_machining_mesh_helper_ui_main {

    private static final int CIRCLE_SEGS = 32;
    private static final int FILLET_ARC_SEGS = 8;

    private circular_machining_mesh_helper_ui_main() {}

    public record CircularEdgeFrame(Point3D center, double radius, Point3D normal, Point3D uAxis, Point3D vAxis, boolean isInternal) {}

    public static CircularEdgeFrame resolveCircularFrame(shape_item_ui_main shape, topology_edge_ui_main edge) {
        if (shape == null || edge == null) return null;
        Point3D c = edge.getLocalP1();
        double r = edge.getLength() * 0.5;
        if (r < 1e-4) return null;

        boolean isInternal = edge.getName().startsWith("HOLE_")
            || edge.getAdjacentFaceIds().stream().anyMatch(id -> id.contains("HOLE_WALL") || id.contains("BORE_WALL"));

        Point3D normal = new Point3D(0, -1, 0), u = new Point3D(1, 0, 0), v = new Point3D(0, 0, 1);
        topology_body_ui_main topo = shape.getTopology();
        for (String faceId : edge.getAdjacentFaceIds()) {
            topology_face_ui_main tf = (topo != null) ? topo.getFaceById(faceId) : null;
            if (tf != null && tf.getFaceKind() != null && tf.getFaceKind().isPlanar()) {
                var info = face_geometry_helper_ui_main.getFaceInfo(shape, tf.getFaceKind());
                normal = info.normal(); u = info.uAxis(); v = info.vAxis();
                break;
            }
        }
        if (edge.getCreatingFeatureId() != null && shape.getExtrusions() != null) {
            for (var ext : shape.getExtrusions()) {
                if (edge.getCreatingFeatureId().equals(ext.getId())) {
                    var info = face_geometry_helper_ui_main.getFaceInfo(shape, ext.getFaceKind());
                    normal = info.normal(); u = info.uAxis(); v = info.vAxis();
                    break;
                }
            }
        }
        return new CircularEdgeFrame(c, r, normal, u, v, isInternal);
    }

    public static TriangleMesh buildCircularChamferMesh(CircularEdgeFrame f, double d) {
        if (f == null || d <= 1e-4 || d >= f.radius()) return null;
        Point3D c = f.center(), n = f.normal(), u = f.uAxis(), v = f.vAxis();
        double r = f.radius();
        boolean internal = f.isInternal();

        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        for (int i = 0; i < CIRCLE_SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / CIRCLE_SEGS, a2 = (i + 1) * 2.0 * Math.PI / CIRCLE_SEGS;
            Point3D ur1 = u.multiply(Math.cos(a1)).add(v.multiply(Math.sin(a1)));
            Point3D ur2 = u.multiply(Math.cos(a2)).add(v.multiply(Math.sin(a2)));

            Point3D pFace1, pFace2, pWall1, pWall2;
            if (internal) {
                pFace1 = c.add(ur1.multiply(r + d)); pFace2 = c.add(ur2.multiply(r + d));
                pWall1 = c.add(ur1.multiply(r)).subtract(n.multiply(d)); pWall2 = c.add(ur2.multiply(r)).subtract(n.multiply(d));
                addQuad(pFace1, pWall1, pWall2, pFace2, pts, fcs);
            } else {
                pFace1 = c.add(ur1.multiply(r - d)); pFace2 = c.add(ur2.multiply(r - d));
                pWall1 = c.add(ur1.multiply(r)).subtract(n.multiply(d)); pWall2 = c.add(ur2.multiply(r)).subtract(n.multiply(d));
                addQuad(pFace1, pFace2, pWall2, pWall1, pts, fcs);
            }
        }
        return toMesh(pts, fcs);
    }

    public static TriangleMesh buildCircularFilletMesh(CircularEdgeFrame f, double radius) {
        if (f == null || radius <= 1e-4 || radius >= f.radius()) return null;
        Point3D c = f.center(), n = f.normal(), u = f.uAxis(), v = f.vAxis();
        double r = f.radius();
        boolean internal = f.isInternal();

        List<Float> pts = new ArrayList<>();
        List<Integer> fcs = new ArrayList<>();

        for (int i = 0; i < CIRCLE_SEGS; i++) {
            double a1 = i * 2.0 * Math.PI / CIRCLE_SEGS, a2 = (i + 1) * 2.0 * Math.PI / CIRCLE_SEGS;
            Point3D ur1 = u.multiply(Math.cos(a1)).add(v.multiply(Math.sin(a1)));
            Point3D ur2 = u.multiply(Math.cos(a2)).add(v.multiply(Math.sin(a2)));

            Point3D arcCenter1 = internal ? c.add(ur1.multiply(r + radius)).subtract(n.multiply(radius)) : c.add(ur1.multiply(r - radius)).subtract(n.multiply(radius));
            Point3D arcCenter2 = internal ? c.add(ur2.multiply(r + radius)).subtract(n.multiply(radius)) : c.add(ur2.multiply(r - radius)).subtract(n.multiply(radius));

            for (int j = 0; j < FILLET_ARC_SEGS; j++) {
                double phi1 = (j * Math.PI * 0.5) / FILLET_ARC_SEGS, phi2 = ((j + 1) * Math.PI * 0.5) / FILLET_ARC_SEGS;
                Point3D p1 = evalArcPt(arcCenter1, ur1, n, radius, phi1, internal);
                Point3D p2 = evalArcPt(arcCenter2, ur2, n, radius, phi1, internal);
                Point3D p3 = evalArcPt(arcCenter2, ur2, n, radius, phi2, internal);
                Point3D p4 = evalArcPt(arcCenter1, ur1, n, radius, phi2, internal);
                if (internal) addQuad(p1, p4, p3, p2, pts, fcs);
                else addQuad(p1, p2, p3, p4, pts, fcs);
            }
        }
        return toMesh(pts, fcs);
    }

    private static Point3D evalArcPt(Point3D arcCenter, Point3D ur, Point3D n, double r, double phi, boolean internal) {
        if (internal) return arcCenter.subtract(ur.multiply(r * Math.sin(phi))).add(n.multiply(r * Math.cos(phi)));
        return arcCenter.add(ur.multiply(r * Math.sin(phi))).add(n.multiply(r * Math.cos(phi)));
    }

    private static void addQuad(Point3D p1, Point3D p2, Point3D p3, Point3D p4, List<Float> pts, List<Integer> fcs) {
        int i1 = addVertex(p1, pts), i2 = addVertex(p2, pts), i3 = addVertex(p3, pts), i4 = addVertex(p4, pts);
        fcs.add(i1); fcs.add(0); fcs.add(i2); fcs.add(0); fcs.add(i3); fcs.add(0);
        fcs.add(i1); fcs.add(0); fcs.add(i3); fcs.add(0); fcs.add(i4); fcs.add(0);
    }

    private static int addVertex(Point3D p, List<Float> pts) {
        int idx = pts.size() / 3;
        pts.add((float) p.getX()); pts.add((float) p.getY()); pts.add((float) p.getZ());
        return idx;
    }

    private static TriangleMesh toMesh(List<Float> pts, List<Integer> fcs) {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        float[] pa = new float[pts.size()]; for (int i = 0; i < pts.size(); i++) pa[i] = pts.get(i);
        int[] fa = new int[fcs.size()]; for (int i = 0; i < fcs.size(); i++) fa[i] = fcs.get(i);
        mesh.getPoints().setAll(pa); mesh.getFaces().setAll(fa);
        int[] sga = new int[fa.length / 6]; java.util.Arrays.fill(sga, 1);
        mesh.getFaceSmoothingGroups().setAll(sga);
        return mesh;
    }
}
