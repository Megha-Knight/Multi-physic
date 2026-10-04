package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main;
import ui.workspace.shapes.holes.hole_mesh_triangulator_ui_main.Frame;

import java.util.List;

/**
 * derived_region_visual_builder_ui_main.java
 * Generates independent, addressable 3D MeshViews for derived machined surfaces
 * (hole walls, hole floors, bore walls) with centralized appearance and picking.
 */
public final class derived_region_visual_builder_ui_main {

    private static final int SEGS = 32;

    private derived_region_visual_builder_ui_main() {}

    public static Group buildDerivedRegionVisuals(shape_item_ui_main shape, topology_body_ui_main body) {
        Group group = new Group();
        group.setId("DERIVED_REGIONS_GROUP");
        if (shape == null || body == null) return group;

        for (topology_derived_face_ui_main df : body.getDerivedFaces()) {
            if (!df.isValid()) continue;
            MeshView mv = buildMeshViewForRegion(shape, df);
            if (mv != null) {
                mv.setId(df.getId());
                mv.setUserData(df);
                group.getChildren().add(mv);
            }
        }
        return group;
    }

    private static MeshView buildMeshViewForRegion(shape_item_ui_main shape, topology_derived_face_ui_main df) {
        face_kind_ui_main kind = df.getSourceFaceKind();
        if (kind == null) return null;
        Frame f;
        Point3D center;
        if (kind.isLateral()) {
            f = getCurvedHoleFrame(shape, kind, df.getLocalCenter().getX(), df.getLocalCenter().getY());
            if (f == null) return null;
            center = f.origin();
        } else {
            f = getFaceFrame(shape, kind);
            if (f == null) return null;
            center = f.origin().add(f.u().multiply(df.getLocalCenter().getX())).add(f.v().multiply(df.getLocalCenter().getY()));
        }

        Point3D inDir = f.n().multiply(-1.0); // inwards into the solid

        TriangleMesh mesh = switch (df.getRegionKind()) {
            case HOLE_WALL, BORE_WALL -> derived_region_advanced_mesh_ui_main.createAdvancedWallMesh(center, inDir, f.u(), f.v(), df, shape);
            case HOLE_FLOOR -> createDiskMesh(center.add(inDir.multiply(df.getDepth())), f.n(), f.u(), f.v(), df.getRadius());
            default -> null;
        };

        if (mesh == null) return null;

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);
        mv.setMaterial(createMaterialForDerivedFace(df));
        return mv;
    }

    public static PhongMaterial createMaterialForDerivedFace(topology_derived_face_ui_main df) {
        Color baseCol = df.getEffectiveColor();
        double op = df.getEffectiveOpacity();
        Color diffuse = Color.color(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), op);
        PhongMaterial mat = new PhongMaterial(diffuse);
        mat.setSpecularColor(Color.color(1, 1, 1, 0.4));
        return mat;
    }

    private static TriangleMesh createDiskMesh(Point3D center, Point3D norm, Point3D uAxis, Point3D vAxis, double r) {
        TriangleMesh m = new TriangleMesh();
        m.getTexCoords().setAll(0, 0, 1, 0, 0, 1, 1, 1);
        float[] pts = new float[(SEGS + 1) * 3];
        pts[0] = (float) center.getX(); pts[1] = (float) center.getY(); pts[2] = (float) center.getZ();

        for (int i = 0; i < SEGS; i++) {
            double a = i * 2.0 * Math.PI / SEGS;
            Point3D pt = center.add(uAxis.multiply(r * Math.cos(a))).add(vAxis.multiply(r * Math.sin(a)));
            int idx = (i + 1) * 3;
            pts[idx] = (float) pt.getX(); pts[idx + 1] = (float) pt.getY(); pts[idx + 2] = (float) pt.getZ();
        }
        m.getPoints().setAll(pts);

        int[] fcs = new int[SEGS * 6];
        for (int i = 0; i < SEGS; i++) {
            int next = (i + 1) % SEGS;
            int fIdx = i * 6;
            fcs[fIdx] = 0; fcs[fIdx + 1] = 0;
            fcs[fIdx + 2] = i + 1; fcs[fIdx + 3] = 1;
            fcs[fIdx + 4] = next + 1; fcs[fIdx + 5] = 2;
        }
        m.getFaces().setAll(fcs);
        int[] sg = new int[fcs.length / 6];
        java.util.Arrays.fill(sg, 1);
        m.getFaceSmoothingGroups().setAll(sg);
        return m;
    }

    private static Frame getCurvedHoleFrame(shape_item_ui_main s, face_kind_ui_main kind, double u, double v) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ()));
        double h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
        if (kind == face_kind_ui_main.CYLINDER_LATERAL) {
            double theta = (r > 1e-4) ? (u / r) : 0;
            Point3D center = new Point3D(p1.getX() + r * Math.cos(theta), -h * 0.5 + v, p1.getZ() + r * Math.sin(theta));
            Point3D norm = new Point3D(Math.cos(theta), 0, Math.sin(theta)).normalize();
            Point3D uAx = new Point3D(-Math.sin(theta), 0, Math.cos(theta)).normalize(), vAx = new Point3D(0, -1, 0);
            return new Frame(center, norm, uAx, vAx);
        }
        if (kind == face_kind_ui_main.CONE_LATERAL) {
            double y = Math.max(-h, Math.min(0, -h * 0.5 + v));
            double ry = Math.max(0.1, r * (-y / h));
            double theta = (ry > 1e-4) ? (u / ry) : 0;
            Point3D center = new Point3D(p1.getX() + ry * Math.cos(theta), y, p1.getZ() + ry * Math.sin(theta));
            Point3D norm = new Point3D(Math.cos(theta) * h, r, Math.sin(theta) * h).normalize();
            Point3D uAx = new Point3D(-Math.sin(theta), 0, Math.cos(theta)).normalize(), vAx = uAx.crossProduct(norm).normalize();
            return new Frame(center, norm, uAx, vAx);
        }
        return null;
    }

    private static Frame getFaceFrame(shape_item_ui_main s, face_kind_ui_main kind) {
        Point3D p1 = s.getP1(), p2 = s.getP2();
        if (s.getType().isBox()) {
            double w = Math.abs(p2.getX() - p1.getX()), d = Math.abs(p2.getZ() - p1.getZ());
            double h = Math.abs(p2.getY() - p1.getY()) > 0.1 ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(w, d) * 0.5);
            Point3D c = new Point3D((p1.getX() + p2.getX()) * 0.5, p1.getY() - h * 0.5, (p1.getZ() + p2.getZ()) * 0.5);
            return hole_mesh_triangulator_ui_main.getCubeFaceFrame(c, Math.max(w, Math.max(h, d)), kind);
        } else if (s.getType() == ui.workspace.shapes.basic_shapes_ui_main.CYLINDER) {
            double r = p1.distance(new Point3D(p2.getX(), 0, p2.getZ())), h = Math.abs(p2.getY()) > 0.1 ? Math.abs(p2.getY()) : Math.max(6.0, r * 2.0);
            if (kind == face_kind_ui_main.TOP_CAP) return new Frame(new Point3D(p1.getX(), -h, p1.getZ()), new Point3D(0, -1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, 1));
            if (kind == face_kind_ui_main.BOTTOM_CAP) return new Frame(new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1));
        } else if (s.getType() == ui.workspace.shapes.basic_shapes_ui_main.CONE) {
            if (kind == face_kind_ui_main.BASE_CAP) return new Frame(new Point3D(p1.getX(), 0, p1.getZ()), new Point3D(0, 1, 0), new Point3D(1, 0, 0), new Point3D(0, 0, -1));
        }
        return null;
    }
}
