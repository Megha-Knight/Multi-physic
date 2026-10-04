package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.framework_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_3d_ui_main;
import ui.workspace.shapes.primitives.shape_geometry_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_pattern_ui_main;
import ui.workspace.drafting.gizmo.shape_rotation_helper_ui_main;

import java.util.List;
import java.util.UUID;

public class shape_item_ui_main {

    private final String id;
    private String name;
    private final basic_shapes_ui_main type;
    private Point3D p1, p2;
    private double worldX = 0, worldY = 0, worldZ = 0, rotationX = 0, rotationY = 0;
    private final Translate worldTx = new Translate(0, 0, 0);
    private final Rotate worldRy = new Rotate(0, Rotate.Y_AXIS), worldRx = new Rotate(0, Rotate.X_AXIS);

    private final Group rootGroup = new Group(), shapeGroup = new Group(), handlesGroup = new Group();
    private boolean selected = false;
    private Point3D uAxis = new Point3D(1, 0, 0), vAxis = new Point3D(0, 0, 1), normal = new Point3D(0, -1, 0);
    private String faceOwnerId = null;
    private face_kind_ui_main faceKind = null;
    private final List<hole_feature_ui_main> holes = new java.util.ArrayList<>();
    private final List<hole_pattern_ui_main> patterns = new java.util.ArrayList<>();
    private final List<ui.workspace.drafting.extrude.extrude_feature_ui_main> extrusions = new java.util.ArrayList<>();
    private final List<ui.workspace.drafting.sketch.sketch_feature_ui_main> sketches = new java.util.ArrayList<>();
    private String selectedExtrudeId = null;

    private ui.workspace.drafting.features.feature_state_ui_main state = ui.workspace.drafting.features.feature_state_ui_main.CLEAN;
    public ui.workspace.drafting.features.feature_state_ui_main getState() { return state; }
    public void setState(ui.workspace.drafting.features.feature_state_ui_main s) { this.state = (s != null) ? s : ui.workspace.drafting.features.feature_state_ui_main.CLEAN; }

    public shape_item_ui_main(basic_shapes_ui_main type, Point3D p1, Point3D p2) { this(UUID.randomUUID().toString(), null, type, p1, p2, 0, 0, 0, 0, 0); }
    public shape_item_ui_main(basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z) { this(UUID.randomUUID().toString(), null, t, p1, p2, x, y, z, 0, 0); }
    public shape_item_ui_main(basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z, double rot) { this(UUID.randomUUID().toString(), null, t, p1, p2, x, y, z, 0, rot); }
    public shape_item_ui_main(String id, String name, basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z) { this(id, name, t, p1, p2, x, y, z, 0, 0); }
    public shape_item_ui_main(String id, String name, basic_shapes_ui_main type, Point3D p1, Point3D p2, double wx, double wy, double wz, double rot) { this(id, name, type, p1, p2, wx, wy, wz, 0, rot); }
    public shape_item_ui_main(String id, String name, basic_shapes_ui_main type, Point3D p1, Point3D p2, double wx, double wy, double wz, double rx, double ry) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString(); this.name = name; this.type = type; this.p1 = p1; this.p2 = p2;
        rootGroup.getTransforms().addAll(worldTx, worldRy, worldRx); rootGroup.getChildren().addAll(shapeGroup, handlesGroup);
        setWorldTranslation(wx, wy, wz); setRotation(rx, ry);
    }

    public void applyWorldDelta(double dx, double dy, double dz) { worldX += dx; worldY += dy; worldZ += dz; worldTx.setX(worldX); worldTx.setY(worldY); worldTx.setZ(worldZ); }
    public void setWorldTranslation(double x, double y, double z) { worldX = x; worldY = y; worldZ = z; worldTx.setX(x); worldTx.setY(y); worldTx.setZ(z); }
    public void setRotation(double rx, double ry) {
        this.rotationX = shape_rotation_helper_ui_main.normalize360(rx); this.rotationY = shape_rotation_helper_ui_main.normalize360(ry);
        updateRotationPivot(); worldRx.setAngle(this.rotationX); worldRy.setAngle(this.rotationY);
    }
    public void setRotationAngle(double deg) { setRotation(this.rotationX, deg); } public void setRotationX(double deg) { setRotation(deg, this.rotationY); } public void setRotationY(double deg) { setRotation(this.rotationX, deg); }
    private void updateRotationPivot() {
        Point3D c = getCenter();
        worldRy.setPivotX(c.getX()); worldRy.setPivotY(c.getY()); worldRy.setPivotZ(c.getZ());
        worldRx.setPivotX(c.getX()); worldRx.setPivotY(c.getY()); worldRx.setPivotZ(c.getZ());
    }

    public double getWorldX() { return worldX; } public double getWorldY() { return worldY; } public double getWorldZ() { return worldZ; }
    public double getRotationAngle() { return rotationY; } public double getRotationY() { return rotationY; } public double getRotationX() { return rotationX; }
    public Point3D getCenter() { return shape_rotation_helper_ui_main.computeCenter(type, p1, p2); }
    public Point3D getWorldCenter() { Point3D c = getCenter(); return new Point3D(worldX + c.getX(), worldY + c.getY(), worldZ + c.getZ()); }
    public Point3D getUAxis() { return uAxis; } public Point3D getVAxis() { return vAxis; } public Point3D getFaceNormal() { return normal; }
    public String getFaceOwnerId() { return faceOwnerId; } public face_kind_ui_main getFaceKind() { return faceKind; } public boolean isOnFace() { return faceOwnerId != null; }

    public List<hole_feature_ui_main> getHoles() { return holes; } public boolean hasHoles() { return !holes.isEmpty(); }
    public void addHole(hole_feature_ui_main h) { if (h != null) { holes.add(h); rebuild(); } }
    public void removeHole(String hId) { holes.removeIf(h -> h.getId().equals(hId)); patterns.removeIf(p -> hId.equals(p.getSeedHoleId())); rebuild(); }
    public void clearHoles() { holes.clear(); patterns.clear(); rebuild(); }
    public hole_feature_ui_main getHole(String id) { for (hole_feature_ui_main h : holes) if (h.getId().equals(id)) return h; return null; }
    public List<hole_pattern_ui_main> getPatterns() { return patterns; } public boolean hasPatterns() { return !patterns.isEmpty(); }
    public void addPattern(hole_pattern_ui_main p) { if (p != null) { patterns.add(p); rebuild(); } }
    public void removePattern(String pId) { patterns.removeIf(p -> p.getId().equals(pId)); rebuild(); }
    public void clearPatterns() { patterns.clear(); rebuild(); }
    public List<ui.workspace.drafting.extrude.extrude_feature_ui_main> getExtrusions() { return extrusions; } public boolean hasExtrusions() { return !extrusions.isEmpty(); }
    public void addExtrude(ui.workspace.drafting.extrude.extrude_feature_ui_main ext) { if (ext != null) { extrusions.add(ext); rebuild(); } } public void removeExtrude(String id) { extrusions.removeIf(e -> e.getId().equals(id)); if (id != null && id.equals(selectedExtrudeId)) selectedExtrudeId = null; rebuild(); }
    public void clearExtrusions() { extrusions.clear(); selectedExtrudeId = null; rebuild(); } public String getSelectedExtrudeId() { return selectedExtrudeId; } public void setSelectedExtrudeId(String id) { this.selectedExtrudeId = id; rebuild(); }
    public List<ui.workspace.drafting.sketch.sketch_feature_ui_main> getSketches() { return sketches; } public boolean hasSketches() { return !sketches.isEmpty(); }
    public void addSketch(ui.workspace.drafting.sketch.sketch_feature_ui_main s) { if (s != null) { sketches.add(s); rebuild(); } } public void removeSketch(String sId) { sketches.removeIf(s -> s.getId().equals(sId)); rebuild(); }
    public void clearSketches() { sketches.clear(); rebuild(); } public ui.workspace.drafting.sketch.sketch_feature_ui_main getSketch(String id) { for (var s : sketches) if (s.getId().equals(id)) return s; return null; }

    public List<hole_feature_ui_main> getAllEffectiveHoles() {
        List<hole_feature_ui_main> eff = new java.util.ArrayList<>();
        for (hole_feature_ui_main h : holes) {
            if (h.isVisible() && h.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) eff.add(h);
        }
        for (hole_pattern_ui_main pat : patterns) {
            if (!pat.isVisible() || pat.getState() == ui.workspace.drafting.features.feature_state_ui_main.INVALID) continue;
            for (hole_feature_ui_main h : holes) {
                if (h.getId().equals(pat.getSeedHoleId()) && h.isValid() && h.isVisible() && h.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                    eff.addAll(pat.generateDerivedHoles(h));
                    break;
                }
            }
        }
        return eff;
    }

    public boolean isFaceOwnerPresent(java.util.Collection<shape_item_ui_main> shapes) {
        if (faceOwnerId == null) return true;
        if (shapes == null) return false;
        for (shape_item_ui_main s : shapes) if (s != null && faceOwnerId.equals(s.getId())) return true;
        return false;
    }

    public void setFacePlane(Point3D u, Point3D v, Point3D n, String ownerId, face_kind_ui_main kind) {
        if (u != null) this.uAxis = u; if (v != null) this.vAxis = v; if (n != null) this.normal = n;
        this.faceOwnerId = ownerId; this.faceKind = kind;
        rebuild();
    }

    public void translate(double dx, double dz) { applyWorldDelta(dx, 0, dz); }

    public void rebuild() {
        shapeGroup.getChildren().clear(); handlesGroup.getChildren().clear();
        List<hole_feature_ui_main> eff = getAllEffectiveHoles();
        Node geo = switch (type) {
            case CIRCLE    -> shape_geometry_ui_main.createCircle(p1, p2, uAxis, vAxis, normal, false, selected);
            case SQUARE    -> shape_geometry_ui_main.createSquare(p1, p2, uAxis, vAxis, normal, false, selected);
            case RECTANGLE -> shape_geometry_ui_main.createRectangle(p1, p2, uAxis, vAxis, normal, false, selected);
            case EQUILATERAL_TRIANGLE -> shape_geometry_ui_main.createEquilateralTriangle(p1, p2, uAxis, vAxis, normal, false, selected);
            case RIGHT_TRIANGLE -> shape_geometry_ui_main.createRightTriangle(p1, p2, uAxis, vAxis, normal, false, selected);
            case CUBE      -> eff.isEmpty() ? shape_geometry_3d_ui_main.createCube(p1, p2, false, selected) : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCubeWithHoles(p1, p2, eff, false, selected);
            case CUBOID    -> eff.isEmpty() ? shape_geometry_3d_ui_main.createCuboid(p1, p2, false, selected) : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCuboidWithHoles(p1, p2, eff, false, selected);
            case CYLINDER  -> eff.isEmpty() ? shape_geometry_3d_ui_main.createCylinder(p1, p2, false, selected) : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildCylinderWithHoles(p1, p2, eff, false, selected);
            case SPHERE    -> shape_geometry_3d_ui_main.createSphere(p1, p2, false, selected);
            case CONE      -> eff.isEmpty() ? shape_geometry_3d_ui_main.createCone(p1, p2, false, selected) : ui.workspace.shapes.holes.hole_mesh_builder_ui_main.buildConeWithHoles(p1, p2, eff, false, selected);
            default -> null;
        };
        if (geo != null) shapeGroup.getChildren().add(geo);
        if (!eff.isEmpty()) shapeGroup.getChildren().add(ui.workspace.drafting.topology.derived_region_visual_builder_ui_main.buildDerivedRegionVisuals(this, getTopology()));
        for (var ext : extrusions) {
            if (ext.isVisible() && ext.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                shapeGroup.getChildren().add(ui.workspace.drafting.extrude.extrude_mesh_builder_ui_main.buildExtrudeNode(this, ext, selected, selectedExtrudeId != null && selectedExtrudeId.equals(ext.getId())));
            }
        }
        for (var sk : sketches) if (sk.isVisible()) shapeGroup.getChildren().add(ui.workspace.drafting.sketch.sketch_mesh_builder_ui_main.buildVisualGroup(sk, false));
        if (selected) buildResizeHandles();
    }

    private void buildResizeHandles() {
        PhongMaterial hMat = new PhongMaterial(Color.web(framework_ui_main.DRAFT_HANDLE_COLOR));
        List<Point3D> handles = getControlHandles();
        for (int i = 0; i < handles.size(); i++) {
            Point3D h = handles.get(i);
            Sphere s = new Sphere(i == 0 ? framework_ui_main.DRAFT_HANDLE_RADIUS * 1.3 : framework_ui_main.DRAFT_HANDLE_RADIUS);
            s.setMaterial(hMat); s.setTranslateX(h.getX()); s.setTranslateY(h.getY()); s.setTranslateZ(h.getZ()); handlesGroup.getChildren().add(s);
        }
    }

    public List<Point3D> getControlHandles() { return shape_handles_ui_main.getControlHandles(type, p1, p2); }
    public boolean isRotationHandle(int idx) { return false; }
    public void moveHandle(int index, Point3D newPos) {
        Point3D[] updated = shape_handles_ui_main.moveHandle(type, p1, p2, index, newPos);
        this.p1 = updated[0]; this.p2 = updated[1]; updateRotationPivot(); revalidateFeatures(); rebuild();
    }
    public void revalidateFeatures() {
        for (hole_feature_ui_main h : holes) h.revalidate(this);
        for (hole_pattern_ui_main p : patterns) p.revalidate(this);
        for (var e : extrusions) e.revalidate(this);
        for (var s : sketches) s.revalidate(this);
    }
    public ui.workspace.drafting.topology.topology_body_ui_main getTopology() { return ui.workspace.drafting.topology.topology_body_ui_main.buildTopology(this); }
    public boolean containsNode(Node node) { for (Node c = node; c != null; c = c.getParent()) if (c == rootGroup) return true; return false; }
    public int findHandleByNode(Node node) { return (node == null) ? -1 : handlesGroup.getChildren().indexOf(node); }
    public int findHandleNear(Point3D groundPt, double threshold) {
        List<Point3D> handles = getControlHandles();
        for (int i = 0; i < handles.size(); i++) {
            Point3D h = handles.get(i);
            if (new Point3D(h.getX() + worldX, 0, h.getZ() + worldZ).distance(groundPt) <= threshold) return i;
        }
        return -1;
    }
    public boolean isNear(Point3D groundPt, double threshold) { return shape_rotation_helper_ui_main.isPointNearShape(this, groundPt, threshold); }
    public String formatDimensions() { return shape_item_formatter_ui_main.formatDimensions(this); }
    public String getId() { return id; } public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }
    public void setP1P2(Point3D np1, Point3D np2) { this.p1 = np1; this.p2 = np2; updateRotationPivot(); revalidateFeatures(); rebuild(); }
    public Group getRootGroup() { return rootGroup; } public Group getShapeGroup() { return shapeGroup; } public basic_shapes_ui_main getType() { return type; }
    public Point3D getP1() { return p1; } public Point3D getP2() { return p2; }
    public boolean isSelected() { return selected; } public void setSelected(boolean sel) { this.selected = sel; rebuild(); }
}
