package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * face_overlay_ui_main.java
 * Real-time 3D CAD viewport highlight overlay for selected and hovered faces.
 * Displays translucent yellow (0.28 opacity) surface mesh, perimeter lines, and origin dot.
 */
public class face_overlay_ui_main extends Group {

    private final Group activeGroup = new Group();
    private final Group hoverGroup = new Group();
    private final PhongMaterial borderMat;
    private final PhongMaterial originMat;
    private face_reference_ui_main currentActiveFace = null;

    public face_overlay_ui_main() {
        borderMat = new PhongMaterial(Color.web("#F59E0B"));
        borderMat.setSpecularColor(Color.WHITE);
        originMat = new PhongMaterial(Color.web("#F59E0B"));
        originMat.setSpecularColor(Color.WHITE);
        getChildren().addAll(activeGroup, hoverGroup);
        setVisible(false);
        setMouseTransparent(true);
    }

    public void highlightFace(face_reference_ui_main face) {
        activeGroup.getChildren().clear();
        currentActiveFace = face;
        if (face == null) {
            if (hoverGroup.getChildren().isEmpty()) setVisible(false);
            return;
        }
        buildFaceVisuals(activeGroup, face, true);
        setVisible(true);
    }

    public void setHoveredFace(face_reference_ui_main face) {
        hoverGroup.getChildren().clear();
        if (face == null) {
            if (currentActiveFace == null && activeGroup.getChildren().isEmpty()) setVisible(false);
            return;
        }
        if (currentActiveFace != null && currentActiveFace.getFaceId().equals(face.getFaceId())) {
            return; // Already highlighted by active selection
        }
        buildFaceVisuals(hoverGroup, face, false);
        setVisible(true);
    }

    public void clearHover() {
        hoverGroup.getChildren().clear();
        if (currentActiveFace == null && activeGroup.getChildren().isEmpty()) setVisible(false);
    }

    private void buildFaceVisuals(Group target, face_reference_ui_main face, boolean showCenterDot) {
        Point3D n = face.getWorldNormal(), u = face.getUAxis(), v = face.getVAxis();
        Point3D origin = face.getFaceOrigin().add(n.multiply(0.15));
        double w = face.getFaceWidth(), h = face.getFaceHeight();
        PhongMaterial yMat = viewport_color_state_ui_main.getFaceHighlightMaterial();

        face_kind_ui_main kind = face.getFaceKind();
        if (kind == face_kind_ui_main.TOP_CAP || kind == face_kind_ui_main.BOTTOM_CAP || kind == face_kind_ui_main.BASE_CAP) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createDiskSurface(face.getFaceOrigin(), u, v, n, w * 0.5, yMat));
            buildCircularBoundary(target, origin, u, v, w * 0.5);
        } else if (face.isPlanar()) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createRectSurface(face.getFaceOrigin(), u, v, n, w, h, yMat));
            buildRectBoundary(target, origin, u, v, w, h);
        } else {
            buildCurvedMarker(target, face.getWorldHitPoint(), n);
        }

        if (showCenterDot) {
            Sphere centerDot = new Sphere(2.2);
            centerDot.setMaterial(originMat);
            centerDot.setTranslateX(origin.getX());
            centerDot.setTranslateY(origin.getY());
            centerDot.setTranslateZ(origin.getZ());
            target.getChildren().add(centerDot);
        }
    }

    public void highlightDerivedFace(ui.workspace.drafting.shape_item_ui_main shape, ui.workspace.drafting.topology.topology_derived_face_ui_main df) {
        activeGroup.getChildren().clear();
        currentActiveFace = null;
        if (shape == null || df == null) {
            if (hoverGroup.getChildren().isEmpty()) setVisible(false);
            return;
        }
        face_reference_ui_main ref = ui.workspace.drafting.topology.topology_geometry_helper_ui_main.reconstructFaceReference(
            shape, df.getSourceFaceKind(), df.getLocalCenter().getX(), df.getLocalCenter().getY());
        if (ref == null) { if (hoverGroup.getChildren().isEmpty()) setVisible(false); return; }
        Point3D n = ref.getWorldNormal(), u = ref.getUAxis(), v = ref.getVAxis();
        Point3D origin = ref.getFaceOrigin().add(n.multiply(0.15));
        double r = df.getRadius();

        switch (df.getRegionKind()) {
            case HOLE_WALL, BORE_WALL -> {
                Point3D topC = origin.add(u.multiply(df.getLocalCenter().getX())).add(v.multiply(df.getLocalCenter().getY()));
                buildCircularBoundary(activeGroup, topC, u, v, r);
                Point3D botC = topC.add(n.multiply(-df.getDepth()));
                buildCircularBoundary(activeGroup, botC, u, v, r);
            }
            case HOLE_FLOOR -> {
                Point3D floorC = origin.add(u.multiply(df.getLocalCenter().getX())).add(v.multiply(df.getLocalCenter().getY())).add(n.multiply(-df.getDepth()));
                buildCircularBoundary(activeGroup, floorC, u, v, r);
            }
            case REMAINING_FACE -> {
                if (ref.getFaceKind().isCylinderCap()) buildCircularBoundary(activeGroup, origin, u, v, ref.getFaceWidth() * 0.5);
                else buildRectBoundary(activeGroup, origin, u, v, ref.getFaceWidth(), ref.getFaceHeight());
                for (var loop : df.getInnerLoops()) {
                    Point3D holeC = origin.add(u.multiply(loop.getCenter().getX())).add(v.multiply(loop.getCenter().getY()));
                    buildCircularBoundary(activeGroup, holeC, u, v, loop.getRadius());
                }
            }
        }
        setVisible(true);
    }

    private void buildRectBoundary(Group target, Point3D center, Point3D u, Point3D v, double w, double h) {
        double hw = w * 0.5, hh = h * 0.5;
        Point3D c1 = center.add(u.multiply(hw)).add(v.multiply(hh));
        Point3D c2 = center.add(u.multiply(-hw)).add(v.multiply(hh));
        Point3D c3 = center.add(u.multiply(-hw)).add(v.multiply(-hh));
        Point3D c4 = center.add(u.multiply(hw)).add(v.multiply(-hh));
        target.getChildren().addAll(createLine(c1, c2), createLine(c2, c3), createLine(c3, c4), createLine(c4, c1));
    }

    private void buildCircularBoundary(Group target, Point3D center, Point3D u, Point3D v, double r) {
        int segs = 32;
        Point3D prev = null, first = null;
        for (int i = 0; i < segs; i++) {
            double ang = i * 2.0 * Math.PI / segs;
            Point3D pt = center.add(u.multiply(r * Math.cos(ang))).add(v.multiply(r * Math.sin(ang)));
            if (i == 0) first = pt;
            if (prev != null) target.getChildren().add(createLine(prev, pt));
            prev = pt;
        }
        if (prev != null && first != null) target.getChildren().add(createLine(prev, first));
    }

    private void buildCurvedMarker(Group target, Point3D hitPt, Point3D n) {
        Sphere s = new Sphere(3.0);
        s.setMaterial(borderMat);
        s.setTranslateX(hitPt.getX());
        s.setTranslateY(hitPt.getY());
        s.setTranslateZ(hitPt.getZ());
        target.getChildren().addAll(s, createLine(hitPt, hitPt.add(n.multiply(15.0))));
    }

    private Cylinder createLine(Point3D p1, Point3D p2) {
        Point3D diff = p2.subtract(p1);
        double len = diff.magnitude();
        Cylinder cyl = new Cylinder(0.8, Math.max(0.1, len));
        cyl.setMaterial(borderMat);
        Point3D mid = p1.midpoint(p2);
        cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axis = yAxis.crossProduct(diff);
        if (axis.magnitude() > 1e-4) {
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, yAxis.dotProduct(diff.normalize())))));
            cyl.getTransforms().add(new Rotate(angle, axis.normalize()));
        } else if (diff.getY() < 0) {
            cyl.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        }
        return cyl;
    }
}
