package ui.workspace.drafting.topology;

import javafx.scene.paint.Color;
import java.util.Locale;

/**
 * topology_face_appearance_ui_main.java
 * Persistent appearance override for CAD faces and derived topology regions.
 */
public record topology_face_appearance_ui_main(
    Color diffuseColor,
    double opacity,
    boolean visible
) {
    public static final topology_face_appearance_ui_main DEFAULT =
        new topology_face_appearance_ui_main(Color.web("#94A3B8"), 1.0, true);

    public topology_face_appearance_ui_main withColor(Color c) {
        return new topology_face_appearance_ui_main(c, this.opacity, this.visible);
    }

    public topology_face_appearance_ui_main withOpacity(double op) {
        return new topology_face_appearance_ui_main(this.diffuseColor, Math.clamp(op, 0.0, 1.0), this.visible);
    }

    public topology_face_appearance_ui_main withVisible(boolean v) {
        return new topology_face_appearance_ui_main(this.diffuseColor, this.opacity, v);
    }

    public String formatNd() {
        String hex = (diffuseColor != null)
            ? String.format(Locale.US, "#%02X%02X%02X",
                (int)(diffuseColor.getRed() * 255),
                (int)(diffuseColor.getGreen() * 255),
                (int)(diffuseColor.getBlue() * 255))
            : "#94A3B8";
        return String.format(Locale.US, "%s:%.2f:%b", hex, opacity, visible);
    }

    public static topology_face_appearance_ui_main parseNd(String str) {
        if (str == null || str.isBlank()) return DEFAULT;
        String[] parts = str.split(":");
        try {
            Color c = (parts.length > 0 && parts[0].startsWith("#")) ? Color.web(parts[0]) : Color.web("#94A3B8");
            double op = (parts.length > 1) ? Double.parseDouble(parts[1]) : 1.0;
            boolean v = (parts.length <= 2) || Boolean.parseBoolean(parts[2]);
            return new topology_face_appearance_ui_main(c, Math.clamp(op, 0.0, 1.0), v);
        } catch (Exception e) {
            return DEFAULT;
        }
    }
}
