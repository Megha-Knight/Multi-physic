package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import ui.framework_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.shapes.shape_geometry_3d_ui_main;
import ui.workspace.shapes.shape_geometry_ui_main;

import java.util.List;
import java.util.UUID;

/**
 * shape_item_ui_main.java
 * Interactive 2D/3D CAD entity with stable identifier, 3D translation, and 360° rotation.
 */
public class shape_item_ui_main {

    private final String id;
    private String name;
    private final basic_shapes_ui_main type;
    private Point3D p1, p2;
    private double worldX = 0, worldY = 0, worldZ = 0, rotationAngle = 0;
    private final Translate worldTx = new Translate(0, 0, 0);
    private final Rotate worldRy = new Rotate(0, Rotate.Y_AXIS);

    private final Group rootGroup = new Group(), shapeGroup = new Group(), handlesGroup = new Group();
    private boolean selected = false;

    public shape_item_ui_main(basic_shapes_ui_main type, Point3D p1, Point3D p2) {
        this(UUID.randomUUID().toString(), null, type, p1, p2, 0, 0, 0, 0);
    }
    public shape_item_ui_main(basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z) {
        this(UUID.randomUUID().toString(), null, t, p1, p2, x, y, z, 0);
    }
    public shape_item_ui_main(basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z, double rot) {
        this(UUID.randomUUID().toString(), null, t, p1, p2, x, y, z, rot);
    }
    public shape_item_ui_main(String id, String name, basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z) {
        this(id, name, t, p1, p2, x, y, z, 0);
    }
    public shape_item_ui_main(String id, String name, basic_shapes_ui_main type, Point3D p1, Point3D p2,
                               double wx, double wy, double wz, double rot) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = name; this.type = type; this.p1 = p1; this.p2 = p2;
        rootGroup.getTransforms().addAll(worldTx, worldRy);
        rootGroup.getChildren().addAll(shapeGroup, handlesGroup);
        setWorldTranslation(wx, wy, wz);
        setRotationAngle(rot);
    }

    public void applyWorldDelta(double dx, double dy, double dz) {
        worldX += dx; worldY += dy; worldZ += dz;
        worldTx.setX(worldX); worldTx.setY(worldY); worldTx.setZ(worldZ);
        updateRotationPivot(); rebuild();
    }
    public void setWorldTranslation(double x, double y, double z) {
        worldX = x; worldY = y; worldZ = z;
        worldTx.setX(x); worldTx.setY(y); worldTx.setZ(z);
        updateRotationPivot(); rebuild();
    }
    public void setRotationAngle(double deg) {
        this.rotationAngle = shape_rotation_helper_ui_main.normalize360(deg);
        updateRotationPivot();
        worldRy.setAngle(this.rotationAngle);
    }
    private void updateRotationPivot() {
        Point3D c = getCenter();
        worldRy.setPivotX(c.getX()); worldRy.setPivotZ(c.getZ());
    }

    public double getWorldX() { return worldX; }
    public double getWorldY() { return worldY; }
    public double getWorldZ() { return worldZ; }
    public double getRotationAngle() { return rotationAngle; }
    public Point3D getCenter() { return shape_rotation_helper_ui_main.computeCenter(type, p1, p2); }
    public Point3D getWorldCenter() {
        Point3D c = getCenter();
        return type.is3D() ? new Point3D(worldX + c.getX(), worldY + c.getY(), worldZ + c.getZ()) : c;
    }

    public void translate(double dx, double dz) {
        if (type.is3D()) { applyWorldDelta(dx, 0, dz); }
        else {
            p1 = new Point3D(p1.getX() + dx, p1.getY(), p1.getZ() + dz);
            p2 = new Point3D(p2.getX() + dx, p2.getY(), p2.getZ() + dz);
            updateRotationPivot(); rebuild();
        }
    }

    public void rebuild() {
        shapeGroup.getChildren().clear(); handlesGroup.getChildren().clear();
        Node geo = switch (type) {
            case CIRCLE    -> shape_geometry_ui_main.createCircle(p1, p2, false, selected);
            case SQUARE    -> shape_geometry_ui_main.createSquare(p1, p2, false, selected);
            case RECTANGLE -> shape_geometry_ui_main.createRectangle(p1, p2, false, selected);
            case EQUILATERAL_TRIANGLE -> shape_geometry_ui_main.createEquilateralTriangle(p1, p2, false, selected);
            case RIGHT_TRIANGLE -> shape_geometry_ui_main.createRightTriangle(p1, p2, false, selected);
            case CUBE      -> shape_geometry_3d_ui_main.createCube(p1, p2, false, selected);
            case CYLINDER  -> shape_geometry_3d_ui_main.createCylinder(p1, p2, false, selected);
            case SPHERE    -> shape_geometry_3d_ui_main.createSphere(p1, p2, false, selected);
            case CONE      -> shape_geometry_3d_ui_main.createCone(p1, p2, false, selected);
            default -> null;
        };
        if (geo != null) shapeGroup.getChildren().add(geo);
        if (selected) buildResizeHandles();
    }

    private void buildResizeHandles() {
        PhongMaterial hMat = new PhongMaterial(Color.web(framework_ui_main.DRAFT_HANDLE_COLOR));
        List<Point3D> handles = getControlHandles();
        for (int i = 0; i < handles.size(); i++) {
            Point3D h = handles.get(i);
            Sphere s = new Sphere(i == 0 ? framework_ui_main.DRAFT_HANDLE_RADIUS * 1.3 : framework_ui_main.DRAFT_HANDLE_RADIUS);
            s.setMaterial(hMat); s.setTranslateX(h.getX()); s.setTranslateY(h.getY()); s.setTranslateZ(h.getZ());
            handlesGroup.getChildren().add(s);
        }
        Point3D rh = shape_rotation_helper_ui_main.getRotationHandlePos(this);
        Sphere rotSph = new Sphere(framework_ui_main.DRAFT_HANDLE_RADIUS * 1.5);
        rotSph.setMaterial(new PhongMaterial(Color.web(framework_ui_main.ROTATION_HANDLE_COLOR)));
        rotSph.setTranslateX(rh.getX()); rotSph.setTranslateY(rh.getY()); rotSph.setTranslateZ(rh.getZ());
        handlesGroup.getChildren().add(rotSph);
    }

    public List<Point3D> getControlHandles() { return shape_handles_ui_main.getControlHandles(type, p1, p2); }
    public boolean isRotationHandle(int idx) { return idx >= 0 && idx == getControlHandles().size(); }

    public void moveHandle(int index, Point3D newPos) {
        if (isRotationHandle(index)) {
            setRotationAngle(shape_rotation_helper_ui_main.calculateAngle(getWorldCenter(), newPos));
            return;
        }
        Point3D[] updated = shape_handles_ui_main.moveHandle(type, p1, p2, index, newPos);
        this.p1 = updated[0]; this.p2 = updated[1];
        updateRotationPivot(); rebuild();
    }

    public boolean containsNode(Node node) {
        for (Node c = node; c != null; c = c.getParent()) if (c == rootGroup) return true;
        return false;
    }
    public int findHandleByNode(Node node) { return (node == null) ? -1 : handlesGroup.getChildren().indexOf(node); }
    public int findHandleNear(Point3D groundPt, double threshold) {
        List<Point3D> handles = getControlHandles();
        for (int i = 0; i < handles.size(); i++) {
            Point3D h = handles.get(i);
            if (new Point3D(h.getX(), 0, h.getZ()).distance(groundPt) <= threshold) return i;
        }
        Point3D rh = shape_rotation_helper_ui_main.getRotationHandlePos(this);
        if (new Point3D(rh.getX(), 0, rh.getZ()).distance(groundPt) <= threshold * 1.6) return handles.size();
        return -1;
    }

    public boolean isNear(Point3D groundPt, double threshold) {
        double ox = type.is3D() ? worldX : 0, oz = type.is3D() ? worldZ : 0;
        if (type == basic_shapes_ui_main.CIRCLE || type == basic_shapes_ui_main.CYLINDER
                || type == basic_shapes_ui_main.SPHERE || type == basic_shapes_ui_main.CONE) {
            double r = p1.distance(p2), d = new Point3D(p1.getX() + ox, 0, p1.getZ() + oz).distance(groundPt);
            return Math.abs(d - r) <= threshold || d <= r;
        }
        double minX = Math.min(p1.getX(), p2.getX()) + ox - threshold, maxX = Math.max(p1.getX(), p2.getX()) + ox + threshold;
        double minZ = Math.min(p1.getZ(), p2.getZ()) + oz - threshold, maxZ = Math.max(p1.getZ(), p2.getZ()) + oz + threshold;
        return groundPt.getX() >= minX && groundPt.getX() <= maxX && groundPt.getZ() >= minZ && groundPt.getZ() <= maxZ;
    }

    public String formatDimensions() {
        double dist = p1.distance(p2), dx = Math.abs(p2.getX() - p1.getX()), dz = Math.abs(p2.getZ() - p1.getZ());
        String rotStr = String.format(" | Rot: %.0f°", rotationAngle);
        return switch (type) {
            case CIRCLE    -> String.format("%s | Radius: %.1f mm%s", getName(), dist, rotStr);
            case SQUARE    -> String.format("%s | Side: %.1f mm%s", getName(), Math.max(dx, dz), rotStr);
            case RECTANGLE -> String.format("%s | W: %.1f H: %.1f%s", getName(), dx, dz, rotStr);
            case EQUILATERAL_TRIANGLE -> String.format("%s | Side: %.1f mm%s", getName(), dist, rotStr);
            case RIGHT_TRIANGLE -> String.format("%s | Base: %.1f H: %.1f%s", getName(), dx, dz, rotStr);
            case CUBE      -> String.format("%s | Side: %.1f | X=%.0f Y=%.0f Z=%.0f%s", getName(), Math.max(dx, dz), worldX, worldZ, -worldY, rotStr);
            case CYLINDER  -> String.format("%s | R: %.1f H: %.1f | X=%.0f Y=%.0f Z=%.0f%s", getName(), dist, Math.max(6.0, dist*2), worldX, worldZ, -worldY, rotStr);
            case SPHERE    -> String.format("%s | Radius: %.1f | X=%.0f Y=%.0f Z=%.0f%s", getName(), dist, worldX, worldZ, -worldY, rotStr);
            case CONE      -> String.format("%s | R: %.1f H: %.1f | X=%.0f Y=%.0f Z=%.0f%s", getName(), dist, Math.max(6.0, dist*2), worldX, worldZ, -worldY, rotStr);
            default -> getName() + rotStr;
        };
    }

    public String getId() { return id; }
    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }
    public void setP1P2(Point3D np1, Point3D np2) { this.p1 = np1; this.p2 = np2; updateRotationPivot(); rebuild(); }
    public Group getRootGroup() { return rootGroup; }
    public basic_shapes_ui_main getType() { return type; }
    public Point3D getP1() { return p1; }
    public Point3D getP2() { return p2; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean sel) { this.selected = sel; rebuild(); }
}
