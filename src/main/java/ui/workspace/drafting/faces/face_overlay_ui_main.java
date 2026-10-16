package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.hole_region_kind_ui_main;
import ui.workspace.drafting.topology.topology_derived_face_ui_main;

public class face_overlay_ui_main extends Group {

    private final Group activeGroup = new Group(), hoverGroup = new Group();
    private final PhongMaterial borderMat, originMat;
    private face_reference_ui_main currentActiveFace = null;

    public face_overlay_ui_main() {
        borderMat = new PhongMaterial(Color.web("#F59E0B")); borderMat.setSpecularColor(Color.WHITE);
        originMat = new PhongMaterial(Color.web("#F59E0B")); originMat.setSpecularColor(Color.WHITE);
        getChildren().addAll(activeGroup, hoverGroup);
        setVisible(false);
        setMouseTransparent(true);
    }

    public void highlightFace(shape_item_ui_main shape, face_reference_ui_main face) {
        activeGroup.getChildren().clear();
        currentActiveFace = face;
        if (face == null) {
            if (hoverGroup.getChildren().isEmpty()) setVisible(false);
            return;
        }
        buildFaceVisuals(activeGroup, shape, face, true);
        setVisible(true);
    }

    public void highlightFace(face_reference_ui_main face) { highlightFace(null, face); }

    public void setHoveredFace(shape_item_ui_main shape, face_reference_ui_main face) {
        hoverGroup.getChildren().clear();
        if (face == null) {
            if (currentActiveFace == null && activeGroup.getChildren().isEmpty()) setVisible(false);
            return;
        }
        if (currentActiveFace != null && currentActiveFace.getFaceId().equals(face.getFaceId())) return;
        buildFaceVisuals(hoverGroup, shape, face, false);
        setVisible(true);
    }

    public void setHoveredFace(face_reference_ui_main face) { setHoveredFace(null, face); }

    public void clearHover() {
        hoverGroup.getChildren().clear();
        if (currentActiveFace == null && activeGroup.getChildren().isEmpty()) setVisible(false);
    }

    private void buildFaceVisuals(Group target, shape_item_ui_main shape, face_reference_ui_main face, boolean showCenterDot) {
        Point3D n, u, v, localOrigin;
        double w, h; boolean isCap = false;
        ui.workspace.drafting.extrude.extrude_feature_ui_main ext = null;
        if (face.isFeatureFace() && shape != null) {
            for (var e : shape.getExtrusions()) if (e.getId().equals(face.getTargetFeatureId())) { ext = e; break; }
        }
        if (ext != null) {
            var info = face_geometry_helper_ui_main.getExtrudeFaceInfo(shape, ext, face.getFaceKind());
            localOrigin = info.origin(); n = info.normal(); u = info.uAxis(); v = info.vAxis();
            w = info.width(); h = info.height(); isCap = info.isCap();
        } else if (shape != null) {
            var info = face_geometry_helper_ui_main.getFaceInfo(shape, face.getFaceKind());
            localOrigin = info.origin(); n = info.normal(); u = info.uAxis(); v = info.vAxis();
            w = face.getFaceWidth(); h = face.getFaceHeight(); isCap = info.isCylinderCap();
        } else {
            localOrigin = face.getFaceOrigin(); n = face.getWorldNormal(); u = face.getUAxis(); v = face.getVAxis();
            w = face.getFaceWidth(); h = face.getFaceHeight();
        }

        Point3D origin = localOrigin.add(n.multiply(0.15));
        PhongMaterial yMat = viewport_color_state_ui_main.getFaceHighlightMaterial();

        topology_derived_face_ui_main remFace = findRemainingFace(shape, face);
        if (remFace != null && !remFace.getInnerLoops().isEmpty()) {
            if (isCap) {
                target.getChildren().add(face_overlay_mesh_helper_ui_main.createAnnularDiskSurface(localOrigin, u, v, n, w * 0.5, remFace.getInnerLoops(), yMat));
            } else {
                target.getChildren().add(face_overlay_mesh_helper_ui_main.createAnnularRectSurface(localOrigin, u, v, n, w, h, remFace.getInnerLoops(), yMat));
            }
        } else if (isCap || face.getFaceKind() == face_kind_ui_main.TOP_CAP || face.getFaceKind() == face_kind_ui_main.BOTTOM_CAP || face.getFaceKind() == face_kind_ui_main.BASE_CAP) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createDiskSurface(localOrigin, u, v, n, w * 0.5, yMat));
        } else if (face.isPlanar()) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createRectSurface(localOrigin, u, v, n, w, h, yMat));
        } else {
            buildCurvedMarker(target, localOrigin, n);
        }

        if (showCenterDot) {
            Sphere centerDot = new Sphere(2.2);
            centerDot.setMaterial(originMat);
            centerDot.setTranslateX(origin.getX()); centerDot.setTranslateY(origin.getY()); centerDot.setTranslateZ(origin.getZ());
            target.getChildren().add(centerDot);
        }
    }

    private topology_derived_face_ui_main findRemainingFace(shape_item_ui_main shape, face_reference_ui_main face) {
        if (shape == null || face == null || shape.getTopology() == null) return null;
        for (var df : shape.getTopology().getDerivedFaces()) {
            if (df.getSourceFaceKind() == face.getFaceKind() && df.getRegionKind() == hole_region_kind_ui_main.REMAINING_FACE && !df.getInnerLoops().isEmpty()) {
                if (!face.isFeatureFace() || face.getTargetFeatureId().equals(df.getCreatingFeatureId())) return df;
            }
        }
        return null;
    }

    public void highlightDerivedFace(shape_item_ui_main shape, topology_derived_face_ui_main df) {
        activeGroup.getChildren().clear();
        currentActiveFace = null;
        if (shape == null || df == null) {
            if (hoverGroup.getChildren().isEmpty()) setVisible(false);
            return;
        }
        ui.workspace.drafting.extrude.extrude_feature_ui_main ext = (df.getCreatingFeatureId() != null && shape.getExtrusions() != null)
            ? shape.getExtrusions().stream().filter(e -> e.getId().equals(df.getCreatingFeatureId())).findFirst().orElse(null) : null;
        var info = (ext != null) ? face_geometry_helper_ui_main.getExtrudeFaceInfo(shape, ext, df.getSourceFaceKind())
                                 : face_geometry_helper_ui_main.getFaceInfo(shape, df.getSourceFaceKind());
        Point3D localOrigin = info.origin(), n = info.normal(), u = info.uAxis(), v = info.vAxis();
        Point3D origin = localOrigin.add(n.multiply(0.15));
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
                PhongMaterial yMat = viewport_color_state_ui_main.getFaceHighlightMaterial();
                if (info.isCylinderCap()) {
                    activeGroup.getChildren().add(face_overlay_mesh_helper_ui_main.createAnnularDiskSurface(localOrigin, u, v, n, info.width() * 0.5, df.getInnerLoops(), yMat));
                } else {
                    activeGroup.getChildren().add(face_overlay_mesh_helper_ui_main.createAnnularRectSurface(localOrigin, u, v, n, info.width(), info.height(), df.getInnerLoops(), yMat));
                }
            }
        }
        setVisible(true);
    }

    private void buildCircularBoundary(Group target, Point3D center, Point3D u, Point3D v, double r) {
        int segs = 32; Point3D prev = null, first = null;
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
        Sphere s = new Sphere(3.0); s.setMaterial(borderMat);
        s.setTranslateX(hitPt.getX()); s.setTranslateY(hitPt.getY()); s.setTranslateZ(hitPt.getZ());
        target.getChildren().addAll(s, createLine(hitPt, hitPt.add(n.multiply(15.0))));
    }

    private Cylinder createLine(Point3D p1, Point3D p2) {
        Point3D diff = p2.subtract(p1); double len = diff.magnitude();
        Cylinder cyl = new Cylinder(0.8, Math.max(0.1, len)); cyl.setMaterial(borderMat);
        Point3D mid = p1.midpoint(p2);
        cyl.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        Point3D yAxis = new Point3D(0, 1, 0), axis = yAxis.crossProduct(diff);
        if (axis.magnitude() > 1e-4) {
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, yAxis.dotProduct(diff.normalize())))));
            cyl.getTransforms().add(new Rotate(angle, axis.normalize()));
        } else if (diff.getY() < 0) cyl.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        return cyl;
    }
}
