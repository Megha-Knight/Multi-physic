package ui.workspace.drafting.faces;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
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
        boolean triangleExtrusion = ext != null && (ext.getProfileShape() == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.RIGHT_TRIANGLE
            || ext.getProfileShape() == ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape.EQUILATERAL_TRIANGLE);
        if (triangleExtrusion) {
            localOrigin = face.getFaceOrigin(); n = face.getWorldNormal(); u = face.getUAxis(); v = face.getVAxis();
            w = face.getFaceWidth(); h = face.getFaceHeight();
            isCap = face.getFaceKind() == face_kind_ui_main.EXTRUDE_CAP;
        } else if (ext != null) {
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
        if (triangleExtrusion && (face.getFaceKind() == face_kind_ui_main.EXTRUDE_CAP || face.getFaceKind() == face_kind_ui_main.EXTRUDE_LATERAL)) {
            if (!face.getFacePolygon().isEmpty()) extrude_face_overlay_helper_ui_main.addPolygonFace(
                target, face.getFacePolygon(), n, yMat, borderMat);
            else if (isCap) extrude_face_overlay_helper_ui_main.addTriangleCap(target, localOrigin, u, v, n,
                ext.getDiameter(), ext.getWidth2() > 0.1 ? ext.getWidth2() : ext.getDiameter(), ext.getProfileShape(), yMat, borderMat);
            else extrude_face_overlay_helper_ui_main.addRectFace(target, localOrigin, u, v, n, w, h, yMat, borderMat);
        } else if (remFace != null && !remFace.getInnerLoops().isEmpty()) {
            if (isCap) {
                target.getChildren().add(face_overlay_mesh_helper_ui_main.createAnnularDiskSurface(localOrigin, u, v, n, w * 0.5, remFace.getInnerLoops(), yMat));
            } else {
                target.getChildren().add(face_overlay_mesh_helper_ui_main.createAnnularRectSurface(localOrigin, u, v, n, w, h, remFace.getInnerLoops(), yMat));
            }
        } else if (isCap || face.getFaceKind() == face_kind_ui_main.TOP_CAP || face.getFaceKind() == face_kind_ui_main.BOTTOM_CAP || face.getFaceKind() == face_kind_ui_main.BASE_CAP) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createDiskSurface(localOrigin, u, v, n, w * 0.5, yMat));
        } else if (face.isPlanar()) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createRectSurface(localOrigin, u, v, n, w, h, yMat));
        } else if (shape != null && shape.getType() == ui.workspace.shapes.basic_shapes_ui_main.CYLINDER) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createCylinderLateralSurface(localOrigin, w / (2 * Math.PI), h, yMat));
        } else if (shape != null && shape.getType() == ui.workspace.shapes.basic_shapes_ui_main.CONE) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createConeLateralSurface(shape.getP1(), w / (2 * Math.PI), h, yMat));
        } else if (shape != null && shape.getType() == ui.workspace.shapes.basic_shapes_ui_main.SPHERE) {
            target.getChildren().add(face_overlay_mesh_helper_ui_main.createSphereSurface(shape.getP1(), Math.max(0.1, shape.getP1().distance(shape.getP2())), yMat));
        } else {
            extrude_face_overlay_helper_ui_main.buildCurvedMarker(target, localOrigin, n, borderMat);
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
                extrude_face_overlay_helper_ui_main.buildCircularBoundary(activeGroup, topC, u, v, r, borderMat);
                Point3D botC = topC.add(n.multiply(-df.getDepth()));
                extrude_face_overlay_helper_ui_main.buildCircularBoundary(activeGroup, botC, u, v, r, borderMat);
            }
            case HOLE_FLOOR -> {
                Point3D floorC = origin.add(u.multiply(df.getLocalCenter().getX())).add(v.multiply(df.getLocalCenter().getY())).add(n.multiply(-df.getDepth()));
                extrude_face_overlay_helper_ui_main.buildCircularBoundary(activeGroup, floorC, u, v, r, borderMat);
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


}
