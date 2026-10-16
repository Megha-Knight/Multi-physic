package ui.workspace.triad;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

/**
 * local_triad_3d_ui_main.java
 * Contextual in-scene 3D local coordinate triad anchored at the active sketch plane origin.
 * Renders local X (U, crimson), local Y (V, emerald), and normal Z (W, cyan) axes.
 */
public class local_triad_3d_ui_main extends Group {

    public static final double ARROW_LENGTH_UV = 24.0;
    public static final double ARROW_LENGTH_W = 16.0;
    public static final double SHAFT_RADIUS = 0.35;
    public static final double HEAD_RADIUS = 0.90;
    public static final double HEAD_LENGTH = 3.8;

    private final MeshView uArrow = new MeshView();
    private final MeshView vArrow = new MeshView();
    private final MeshView wArrow = new MeshView();
    private final Sphere originMarker = new Sphere(0.85);

    private coordinate_triad_math_ui_main.TriadFrame currentFrame = null;

    public local_triad_3d_ui_main() {
        setMouseTransparent(true);
        setVisible(false);

        PhongMaterial matU = new PhongMaterial(Color.web("#F43F5E")); // Crimson
        matU.setSpecularColor(Color.web("#FECDD3"));
        uArrow.setMaterial(matU);

        PhongMaterial matV = new PhongMaterial(Color.web("#10B981")); // Emerald
        matV.setSpecularColor(Color.web("#A7F3D0"));
        vArrow.setMaterial(matV);

        PhongMaterial matW = new PhongMaterial(Color.web("#06B6D4")); // Cyan
        matW.setSpecularColor(Color.web("#CFFAFE"));
        wArrow.setMaterial(matW);

        PhongMaterial matOrig = new PhongMaterial(Color.web("#1E293B"));
        originMarker.setMaterial(matOrig);

        getChildren().addAll(originMarker, uArrow, vArrow, wArrow);
    }

    public void updateFrame(coordinate_triad_math_ui_main.TriadFrame frame) {
        this.currentFrame = frame;
        if (frame == null || !frame.isValid()) {
            setVisible(false);
            return;
        }

        Point3D orig = frame.origin();
        originMarker.setTranslateX(orig.getX());
        originMarker.setTranslateY(orig.getY());
        originMarker.setTranslateZ(orig.getZ());

        Point3D tipU = orig.add(frame.uAxis().multiply(ARROW_LENGTH_UV));
        Point3D tipV = orig.add(frame.vAxis().multiply(ARROW_LENGTH_UV));
        Point3D tipW = orig.add(frame.wAxis().multiply(ARROW_LENGTH_W));

        uArrow.setMesh(buildArrowMesh(orig, tipU, SHAFT_RADIUS, HEAD_RADIUS, HEAD_LENGTH));
        vArrow.setMesh(buildArrowMesh(orig, tipV, SHAFT_RADIUS, HEAD_RADIUS, HEAD_LENGTH));
        wArrow.setMesh(buildArrowMesh(orig, tipW, SHAFT_RADIUS, HEAD_RADIUS, HEAD_LENGTH));

        setVisible(true);
    }

    public coordinate_triad_math_ui_main.TriadFrame getCurrentFrame() {
        return currentFrame;
    }

    private static TriangleMesh buildArrowMesh(Point3D p0, Point3D p1, double rShaft, double rHead, double lenHead) {
        TriangleMesh mesh = new TriangleMesh();
        Point3D diff = p1.subtract(p0);
        double len = diff.magnitude();
        if (len < 1e-3) return mesh;

        Point3D dir = diff.normalize();
        Point3D perp1 = Math.abs(dir.getY()) < 0.9 ? dir.crossProduct(new Point3D(0, 1, 0)).normalize()
                                                   : dir.crossProduct(new Point3D(1, 0, 0)).normalize();
        Point3D perp2 = dir.crossProduct(perp1).normalize();

        double shaftLen = Math.max(0.1, len - lenHead);
        Point3D headBaseCenter = p0.add(dir.multiply(shaftLen));

        int segs = 8;
        // Points: 0..segs-1 = base ring, segs..2*segs-1 = shaft top ring,
        // 2*segs..3*segs-1 = cone base ring, 3*segs = cone tip (p1)
        float[] points = new float[(3 * segs + 1) * 3];
        int idx = 0;

        for (int i = 0; i < segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            Point3D rad = perp1.multiply(Math.cos(a)).add(perp2.multiply(Math.sin(a)));
            Point3D ptBase = p0.add(rad.multiply(rShaft));
            points[idx++] = (float) ptBase.getX(); points[idx++] = (float) ptBase.getY(); points[idx++] = (float) ptBase.getZ();
        }
        for (int i = 0; i < segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            Point3D rad = perp1.multiply(Math.cos(a)).add(perp2.multiply(Math.sin(a)));
            Point3D ptTop = headBaseCenter.add(rad.multiply(rShaft));
            points[idx++] = (float) ptTop.getX(); points[idx++] = (float) ptTop.getY(); points[idx++] = (float) ptTop.getZ();
        }
        for (int i = 0; i < segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            Point3D rad = perp1.multiply(Math.cos(a)).add(perp2.multiply(Math.sin(a)));
            Point3D ptCone = headBaseCenter.add(rad.multiply(rHead));
            points[idx++] = (float) ptCone.getX(); points[idx++] = (float) ptCone.getY(); points[idx++] = (float) ptCone.getZ();
        }
        points[idx++] = (float) p1.getX(); points[idx++] = (float) p1.getY(); points[idx++] = (float) p1.getZ();

        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(0.0f, 0.0f);

        int tipIdx = 3 * segs;
        java.util.List<Integer> faces = new java.util.ArrayList<>();
        for (int i = 0; i < segs; i++) {
            int nxt = (i + 1) % segs;
            // Shaft quads (2 triangles)
            faces.add(i); faces.add(0); faces.add(i + segs); faces.add(0); faces.add(nxt); faces.add(0);
            faces.add(nxt); faces.add(0); faces.add(i + segs); faces.add(0); faces.add(nxt + segs); faces.add(0);
            // Cone triangles
            int c1 = 2 * segs + i, c2 = 2 * segs + nxt;
            faces.add(c1); faces.add(0); faces.add(tipIdx); faces.add(0); faces.add(c2); faces.add(0);
        }
        int[] fArray = new int[faces.size()];
        for (int i = 0; i < faces.size(); i++) fArray[i] = faces.get(i);
        mesh.getFaces().setAll(fArray);

        return mesh;
    }
}
