package ui.workspace;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import ui.framework_ui_main;

import java.util.ArrayList;
import java.util.List;

/**
 * shapeitem_ui_main.java
 * Interactive 2D/3D CAD entity supporting selection, translation, and vertex reshaping.
 */
public class shapeitem_ui_main {

    private final basicshapes_ui_main type;
    private Point3D p1;
    private Point3D p2;
    private final Group rootGroup = new Group();
    private final Group shapeGroup = new Group();
    private final Group handlesGroup = new Group();
    private boolean selected = false;

    public shapeitem_ui_main(basicshapes_ui_main type, Point3D p1, Point3D p2) {
        this.type = type;
        this.p1 = p1;
        this.p2 = p2;
        rootGroup.getChildren().addAll(shapeGroup, handlesGroup);
        rebuild();
    }

    public void rebuild() {
        shapeGroup.getChildren().clear();
        handlesGroup.getChildren().clear();

        javafx.scene.Node geo = switch (type) {
            case CIRCLE -> shapegeometry_ui_main.createCircle(p1, p2, false);
            case SQUARE -> shapegeometry_ui_main.createSquare(p1, p2, false);
            case RECTANGLE -> shapegeometry_ui_main.createRectangle(p1, p2, false);
            case EQUILATERAL_TRIANGLE -> shapegeometry_ui_main.createEquilateralTriangle(p1, p2, false);
            case RIGHT_TRIANGLE -> shapegeometry_ui_main.createRightTriangle(p1, p2, false);
            case CUBE -> shapegeometry3d_ui_main.createCube(p1, p2, false);
            case CYLINDER -> shapegeometry3d_ui_main.createCylinder(p1, p2, false);
            case SPHERE -> shapegeometry3d_ui_main.createSphere(p1, p2, false);
            case CONE -> shapegeometry3d_ui_main.createCone(p1, p2, false);
            default -> null;
        };
        if (geo != null) shapeGroup.getChildren().add(geo);

        if (selected) {
            PhongMaterial hMat = new PhongMaterial(Color.web(framework_ui_main.DRAFT_HANDLE_COLOR));
            List<Point3D> handles = getControlHandles();
            for (int i = 0; i < handles.size(); i++) {
                Point3D h = handles.get(i);
                double rad = (i == 0) ? framework_ui_main.DRAFT_HANDLE_RADIUS * 1.3 : framework_ui_main.DRAFT_HANDLE_RADIUS;
                Sphere sphere = new Sphere(rad);
                sphere.setMaterial(hMat);
                sphere.setTranslateX(h.getX());
                sphere.setTranslateZ(h.getZ());
                handlesGroup.getChildren().add(sphere);
            }
        }
    }

    public List<Point3D> getControlHandles() {
        List<Point3D> list = new ArrayList<>();
        if (type == basicshapes_ui_main.CIRCLE || type == basicshapes_ui_main.CYLINDER ||
            type == basicshapes_ui_main.SPHERE || type == basicshapes_ui_main.CONE) {
            list.add(p1); // Center translation handle
            list.add(new Point3D(p2.getX(), 0, p2.getZ())); // Radial sizing handle
        } else if (type == basicshapes_ui_main.SQUARE || type == basicshapes_ui_main.CUBE) {
            double s = Math.max(Math.abs(p2.getX() - p1.getX()), Math.abs(p2.getZ() - p1.getZ()));
            double x1 = p1.getX() + (p2.getX() >= p1.getX() ? s : -s), z1 = p1.getZ() + (p2.getZ() >= p1.getZ() ? s : -s);
            list.add(p1); list.add(new Point3D(x1, 0, p1.getZ())); list.add(new Point3D(x1, 0, z1)); list.add(new Point3D(p1.getX(), 0, z1));
        } else if (type == basicshapes_ui_main.RECTANGLE) {
            list.add(p1); list.add(new Point3D(p2.getX(), 0, p1.getZ())); list.add(p2); list.add(new Point3D(p1.getX(), 0, p2.getZ()));
        } else if (type == basicshapes_ui_main.EQUILATERAL_TRIANGLE) {
            list.add(p1); list.add(p2);
            double dx = p2.getX() - p1.getX(), dz = p2.getZ() - p1.getZ(), s = Math.sqrt(dx * dx + dz * dz), h = s * Math.sqrt(3.0) / 2.0;
            list.add(new Point3D(p1.getX() + dx * 0.5 - (dz / s) * h, 0, p1.getZ() + dz * 0.5 + (dx / s) * h));
        } else if (type == basicshapes_ui_main.RIGHT_TRIANGLE) {
            list.add(p1); list.add(new Point3D(p2.getX(), 0, p1.getZ())); list.add(new Point3D(p1.getX(), 0, p2.getZ()));
        }
        return list;
    }

    public void moveHandle(int index, Point3D newPos) {
        if (type == basicshapes_ui_main.CIRCLE || type == basicshapes_ui_main.CYLINDER ||
            type == basicshapes_ui_main.SPHERE || type == basicshapes_ui_main.CONE) {
            if (index == 0) translate(newPos.getX() - p1.getX(), newPos.getZ() - p1.getZ());
            else if (index == 1) p2 = new Point3D(newPos.getX(), 0, newPos.getZ());
        } else if (type == basicshapes_ui_main.SQUARE || type == basicshapes_ui_main.RECTANGLE || type == basicshapes_ui_main.CUBE) {
            if (index == 0) p1 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 2) p2 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 1) { p2 = new Point3D(newPos.getX(), 0, p2.getZ()); p1 = new Point3D(p1.getX(), 0, newPos.getZ()); }
            else if (index == 3) { p1 = new Point3D(newPos.getX(), 0, p1.getZ()); p2 = new Point3D(p2.getX(), 0, newPos.getZ()); }
        } else if (type == basicshapes_ui_main.RIGHT_TRIANGLE || type == basicshapes_ui_main.EQUILATERAL_TRIANGLE) {
            if (index == 0) p1 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 1) p2 = new Point3D(newPos.getX(), 0, newPos.getZ());
            else if (index == 2 && type == basicshapes_ui_main.RIGHT_TRIANGLE) p2 = new Point3D(p2.getX(), 0, newPos.getZ());
        }
        rebuild();
    }

    public void translate(double dx, double dz) {
        p1 = new Point3D(p1.getX() + dx, 0, p1.getZ() + dz);
        p2 = new Point3D(p2.getX() + dx, 0, p2.getZ() + dz);
        rebuild();
    }

    public int findHandleNear(Point3D groundPt, double threshold) {
        List<Point3D> handles = getControlHandles();
        for (int i = 0; i < handles.size(); i++) {
            if (handles.get(i).distance(groundPt) <= threshold) return i;
        }
        return -1;
    }

    public boolean isNear(Point3D groundPt, double threshold) {
        if (type == basicshapes_ui_main.CIRCLE || type == basicshapes_ui_main.CYLINDER ||
            type == basicshapes_ui_main.SPHERE || type == basicshapes_ui_main.CONE) {
            double r = p1.distance(p2), d = p1.distance(groundPt);
            return Math.abs(d - r) <= threshold || d <= r;
        }
        double minX = Math.min(p1.getX(), p2.getX()) - threshold, maxX = Math.max(p1.getX(), p2.getX()) + threshold;
        double minZ = Math.min(p1.getZ(), p2.getZ()) - threshold, maxZ = Math.max(p1.getZ(), p2.getZ()) + threshold;
        return groundPt.getX() >= minX && groundPt.getX() <= maxX && groundPt.getZ() >= minZ && groundPt.getZ() <= maxZ;
    }

    public String formatDimensions() {
        double dist = p1.distance(p2), dx = Math.abs(p2.getX() - p1.getX()), dz = Math.abs(p2.getZ() - p1.getZ());
        return switch (type) {
            case CIRCLE -> String.format("Circle  |  Radius: %.1f mm  |  Dia: %.1f mm", dist, dist * 2);
            case SQUARE -> String.format("Square  |  Side: %.1f mm", Math.max(dx, dz));
            case RECTANGLE -> String.format("Rectangle  |  W: %.1f mm  |  H: %.1f mm", dx, dz);
            case EQUILATERAL_TRIANGLE -> String.format("Equilateral Triangle  |  Side: %.1f mm", dist);
            case RIGHT_TRIANGLE -> String.format("Right Triangle  |  Base: %.1f mm  |  Height: %.1f mm", dx, dz);
            case CUBE -> String.format("Cube (3D)  |  Side: %.1f mm", Math.max(dx, dz));
            case CYLINDER -> String.format("Cylinder (3D)  |  Radius: %.1f mm  |  Height: %.1f mm", dist, dist * 2);
            case SPHERE -> String.format("Sphere (3D)  |  Radius: %.1f mm  |  Dia: %.1f mm", dist, dist * 2);
            case CONE -> String.format("Cone (3D)  |  Radius: %.1f mm  |  Height: %.1f mm", dist, dist * 2);
            default -> "";
        };
    }

    public Group getRootGroup() { return rootGroup; }
    public basicshapes_ui_main getType() { return type; }
    public Point3D getP1() { return p1; }
    public Point3D getP2() { return p2; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean sel) { this.selected = sel; rebuild(); }
}
