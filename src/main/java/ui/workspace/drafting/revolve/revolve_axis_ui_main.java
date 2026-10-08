package ui.workspace.drafting.revolve;

import javafx.geometry.Point3D;
import java.util.Objects;

/**
 * revolve_axis_ui_main.java
 * Reusable 3D axis definition specifying origin point and direction vector for rotation.
 */
public class revolve_axis_ui_main {

    private final revolve_axis_type_ui_main type;
    private final Point3D origin;
    private final Point3D direction;

    public revolve_axis_ui_main(revolve_axis_type_ui_main type, Point3D origin, Point3D direction) {
        this.type = type != null ? type : revolve_axis_type_ui_main.Y_AXIS;
        this.origin = origin != null ? origin : new Point3D(0, 0, 0);
        if (direction != null && direction.magnitude() > 1e-9) {
            this.direction = direction.multiply(1.0 / direction.magnitude());
        } else {
            this.direction = direction != null ? direction : new Point3D(0, 0, 0);
        }
    }

    public static revolve_axis_ui_main xAxis() {
        return new revolve_axis_ui_main(revolve_axis_type_ui_main.X_AXIS, new Point3D(0, 0, 0), new Point3D(1, 0, 0));
    }

    public static revolve_axis_ui_main yAxis() {
        return new revolve_axis_ui_main(revolve_axis_type_ui_main.Y_AXIS, new Point3D(0, 0, 0), new Point3D(0, 1, 0));
    }

    public static revolve_axis_ui_main zAxis() {
        return new revolve_axis_ui_main(revolve_axis_type_ui_main.Z_AXIS, new Point3D(0, 0, 0), new Point3D(0, 0, 1));
    }

    public static revolve_axis_ui_main custom(Point3D origin, Point3D direction) {
        return new revolve_axis_ui_main(revolve_axis_type_ui_main.CUSTOM_AXIS, origin, direction);
    }

    public revolve_axis_type_ui_main getType() {
        return type;
    }

    public Point3D getOrigin() {
        return origin;
    }

    public Point3D getDirection() {
        return direction;
    }

    public boolean isValid() {
        return direction != null && direction.magnitude() > 1e-6
            && !Double.isNaN(direction.getX()) && !Double.isNaN(direction.getY()) && !Double.isNaN(direction.getZ())
            && !Double.isInfinite(direction.getX()) && !Double.isInfinite(direction.getY()) && !Double.isInfinite(direction.getZ());
    }

    public Point3D rotatePoint(Point3D pt, double angleDegrees) {
        if (pt == null || !isValid()) return pt;
        double rad = Math.toRadians(angleDegrees);
        double cosA = Math.cos(rad);
        double sinA = Math.sin(rad);

        Point3D p = pt.subtract(origin);
        double u = direction.getX(), v = direction.getY(), w = direction.getZ();
        double dot = p.getX() * u + p.getY() * v + p.getZ() * w;

        double rx = u * dot * (1.0 - cosA) + p.getX() * cosA + (-w * p.getY() + v * p.getZ()) * sinA;
        double ry = v * dot * (1.0 - cosA) + p.getY() * cosA + (w * p.getX() - u * p.getZ()) * sinA;
        double rz = w * dot * (1.0 - cosA) + p.getZ() * cosA + (-v * p.getX() + u * p.getY()) * sinA;

        return new Point3D(rx, ry, rz).add(origin);
    }
}
