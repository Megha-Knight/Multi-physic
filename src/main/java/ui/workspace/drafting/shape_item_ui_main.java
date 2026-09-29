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
    private double worldX = 0, worldY = 0, worldZ = 0, rotationX = 0, rotationY = 0;
    private final Translate worldTx = new Translate(0, 0, 0);
    private final Rotate worldRy = new Rotate(0, Rotate.Y_AXIS), worldRx = new Rotate(0, Rotate.X_AXIS);

    private final Group rootGroup = new Group(), shapeGroup = new Group(), handlesGroup = new Group();
    private boolean selected = false;

    public shape_item_ui_main(basic_shapes_ui_main type, Point3D p1, Point3D p2) { this(UUID.randomUUID().toString(), null, type, p1, p2, 0, 0, 0, 0, 0); }
    public shape_item_ui_main(basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z) { this(UUID.randomUUID().toString(), null, t, p1, p2, x, y, z, 0, 0); }
    public shape_item_ui_main(basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z, double rot) { this(UUID.randomUUID().toString(), null, t, p1, p2, x, y, z, 0, rot); }
    public shape_item_ui_main(String id, String name, basic_shapes_ui_main t, Point3D p1, Point3D p2, double x, double y, double z) { this(id, name, t, p1, p2, x, y, z, 0, 0); }
    public shape_item_ui_main(String id, String name, basic_shapes_ui_main type, Point3D p1, Point3D p2,
                               double wx, double wy, double wz, double rot) { this(id, name, type, p1, p2, wx, wy, wz, 0, rot); }
    public shape_item_ui_main(String id, String name, basic_shapes_ui_main type, Point3D p1, Point3D p2,
                               double wx, double wy, double wz, double rx, double ry) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = name; this.type = type; this.p1 = p1; this.p2 = p2;
        rootGroup.getTransforms().addAll(worldTx, worldRy, worldRx);
        rootGroup.getChildren().addAll(shapeGroup, handlesGroup);
        setWorldTranslation(wx, wy, wz);
        setRotation(rx, ry);
    }

    public void applyWorldDelta(double dx, double dy, double dz) {
        worldX += dx; worldY += dy; worldZ += dz;
        worldTx.setX(worldX); worldTx.setY(worldY); worldTx.setZ(worldZ);
    }
    public void setWorldTranslation(double x, double y, double z) {
        worldX = x; worldY = y; worldZ = z;
        worldTx.setX(x); worldTx.setY(y); worldTx.setZ(z);
    }
    public void setRotation(double rx, double ry) {
        this.rotationX = shape_rotation_helper_ui_main.normalize360(rx);
        this.rotationY = shape_rotation_helper_ui_main.normalize360(ry);
        updateRotationPivot();
        worldRx.setAngle(this.rotationX);
        worldRy.setAngle(this.rotationY);
    }
    public void setRotationAngle(double deg) { setRotation(this.rotationX, deg); }
    public void setRotationX(double deg) { setRotation(deg, this.rotationY); }
    public void setRotationY(double deg) { setRotation(this.rotationX, deg); }
    private void updateRotationPivot() {
        Point3D c = getCenter();
        worldRy.setPivotX(c.getX()); worldRy.setPivotY(c.getY()); worldRy.setPivotZ(c.getZ());
        worldRx.setPivotX(c.getX()); worldRx.setPivotY(c.getY()); worldRx.setPivotZ(c.getZ());
    }

    public double getWorldX() { return worldX; } public double getWorldY() { return worldY; } public double getWorldZ() { return worldZ; }
    public double getRotationAngle() { return rotationY; } public double getRotationY() { return rotationY; } public double getRotationX() { return rotationX; }
    public Point3D getCenter() { return shape_rotation_helper_ui_main.computeCenter(type, p1, p2); }
    public Point3D getWorldCenter() { Point3D c = getCenter(); return new Point3D(worldX + c.getX(), worldY + c.getY(), worldZ + c.getZ()); }

    public void translate(double dx, double dz) { applyWorldDelta(dx, 0, dz); }

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
    }

    public List<Point3D> getControlHandles() { return shape_handles_ui_main.getControlHandles(type, p1, p2); }
    public boolean isRotationHandle(int idx) { return false; }

    public void moveHandle(int index, Point3D newPos) {
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
            if (new Point3D(h.getX() + worldX, 0, h.getZ() + worldZ).distance(groundPt) <= threshold) return i;
        }
        return -1;
    }

    public boolean isNear(Point3D groundPt, double threshold) { return shape_rotation_helper_ui_main.isPointNearShape(this, groundPt, threshold); }

    public String formatDimensions() {
        double dist = p1.distance(p2), dx = Math.abs(p2.getX() - p1.getX()), dz = Math.abs(p2.getZ() - p1.getZ());
        String rotStr = (rotationY != 0 || rotationX != 0) ? String.format(" | Rot Y: %.0f° | Rot X: %.0f°", rotationY, rotationX) : "";
        String posStr = String.format(" | Pos: X=%.1f Y=%.1f Z=%.1f", worldX, worldZ, -worldY);
        return switch (type) {
            case CIRCLE    -> String.format("%s | Radius: %.1f mm%s%s", getName(), dist, posStr, rotStr);
            case SQUARE    -> String.format("%s | Side: %.1f mm%s%s", getName(), Math.max(dx, dz), posStr, rotStr);
            case RECTANGLE -> String.format("%s | W: %.1f H: %.1f mm%s%s", getName(), dx, dz, posStr, rotStr);
            case EQUILATERAL_TRIANGLE -> String.format("%s | Side: %.1f mm%s%s", getName(), dist, posStr, rotStr);
            case RIGHT_TRIANGLE -> String.format("%s | Base: %.1f H: %.1f mm%s%s", getName(), dx, dz, posStr, rotStr);
            case CUBE      -> String.format("%s | Side: %.1f mm%s%s", getName(), Math.max(dx, dz), posStr, rotStr);
            case CYLINDER  -> String.format("%s | R: %.1f H: %.1f mm%s%s", getName(), dist, Math.max(6.0, dist*2), posStr, rotStr);
            case SPHERE    -> String.format("%s | Radius: %.1f mm%s%s", getName(), dist, posStr, rotStr);
            case CONE      -> String.format("%s | R: %.1f H: %.1f mm%s%s", getName(), dist, Math.max(6.0, dist*2), posStr, rotStr);
            default -> getName() + posStr + rotStr;
        };
    }

    public String getId() { return id; } public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }
    public void setP1P2(Point3D np1, Point3D np2) { this.p1 = np1; this.p2 = np2; updateRotationPivot(); rebuild(); }
    public Group getRootGroup() { return rootGroup; } public basic_shapes_ui_main getType() { return type; }
    public Point3D getP1() { return p1; } public Point3D getP2() { return p2; }
    public boolean isSelected() { return selected; } public void setSelected(boolean sel) { this.selected = sel; rebuild(); }
}
