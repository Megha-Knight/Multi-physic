package ui.workspace.drafting;

public final class shape_item_formatter_ui_main {
    private shape_item_formatter_ui_main() {}

    public static String formatDimensions(shape_item_ui_main item) {
        javafx.geometry.Point3D p1 = item.getP1(), p2 = item.getP2();
        double dist = p1 != null && p2 != null ? p1.distance(p2) : 0;
        double dx = p1 != null && p2 != null ? Math.abs(p2.getX() - p1.getX()) : 0;
        double dz = p1 != null && p2 != null ? Math.abs(p2.getZ() - p1.getZ()) : 0;
        double rotY = item.getRotationY(), rotX = item.getRotationX();
        String rotStr = (rotY != 0 || rotX != 0) ? String.format(" | Rot Y: %.0f° | Rot X: %.0f°", rotY, rotX) : "";
        double px = item.getWorldX() + (p1 != null ? p1.getX() : 0.0);
        double py = item.getWorldZ() + (p1 != null ? p1.getZ() : 0.0);
        double pz = -(item.getWorldY() + (p1 != null ? p1.getY() : 0.0));
        String posStr = String.format(" | Pos: X=%.1f mm Y=%.1f mm Z=%.1f mm", px, py, pz);
        return switch (item.getType()) {
            case CIRCLE    -> String.format("%s | Radius: %.1f mm%s%s", item.getName(), dist, posStr, rotStr);
            case SQUARE    -> String.format("%s | Side: %.1f mm%s%s", item.getName(), Math.max(dx, dz), posStr, rotStr);
            case RECTANGLE -> String.format("%s | W: %.1f H: %.1f mm%s%s", item.getName(), dx, dz, posStr, rotStr);
            case EQUILATERAL_TRIANGLE -> String.format("%s | Side: %.1f mm%s%s", item.getName(), dist, posStr, rotStr);
            case RIGHT_TRIANGLE -> String.format("%s | Base: %.1f H: %.1f mm%s%s", item.getName(), dx, dz, posStr, rotStr);
            case CUBE      -> String.format("%s | Side: %.1f mm%s%s", item.getName(), Math.max(dx, dz), posStr, rotStr);
            case CUBOID    -> String.format("%s | W: %.1f D: %.1f H: %.1f mm%s%s", item.getName(), dx, dz, (p2 != null && Math.abs(p2.getY() - p1.getY()) > 0.1) ? Math.abs(p2.getY() - p1.getY()) : Math.max(6.0, Math.min(dx, dz) * 0.5), posStr, rotStr);
            case CYLINDER  -> String.format("%s | R: %.1f H: %.1f mm%s%s", item.getName(), dist, Math.max(6.0, dist*2), posStr, rotStr);
            case SPHERE    -> String.format("%s | Radius: %.1f mm%s%s", item.getName(), dist, posStr, rotStr);
            case CONE      -> String.format("%s | R: %.1f H: %.1f mm%s%s", item.getName(), dist, Math.max(6.0, dist*2), posStr, rotStr);
            default -> item.getName() + posStr + rotStr;
        };
    }
}
