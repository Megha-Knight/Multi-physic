package ui.workspace.drafting.profiles;

import javafx.geometry.Point3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * profile_reference_ui_main.java
 * Reusable cross-section profile specification for Loft and Sweep operations.
 */
public class profile_reference_ui_main {

    public enum ProfileType {
        RECTANGLE("Rectangle"),
        CIRCLE("Circle"),
        POLYGON("Polygon"),
        SKETCH("Sketch");

        private final String label;
        ProfileType(String l) { this.label = l; }
        public String getLabel() { return label; }
    }

    private final String id;
    private String name;
    private final ProfileType type;
    private profile_loop_ui_main loop;
    private double width;
    private double height;
    private double radius;
    private double elevation;

    public profile_reference_ui_main(String id, String name, ProfileType type,
                                    profile_loop_ui_main loop, double w, double h, double r, double elev) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : (type != null ? type.getLabel() : "Profile");
        this.type = type != null ? type : ProfileType.RECTANGLE;
        this.loop = loop;
        this.width = w;
        this.height = h;
        this.radius = r;
        this.elevation = elev;
    }

    public static profile_reference_ui_main createRectangle(String id, String name, double w, double h, double elevation) {
        double hw = w * 0.5, hh = h * 0.5;
        List<Point3D> pts = List.of(
            new Point3D(-hw, elevation, -hh),
            new Point3D(hw, elevation, -hh),
            new Point3D(hw, elevation, hh),
            new Point3D(-hw, elevation, hh)
        );
        return new profile_reference_ui_main(id, name, ProfileType.RECTANGLE, new profile_loop_ui_main(pts, true), w, h, 0, elevation);
    }

    public static profile_reference_ui_main createCircle(String id, String name, double r, double elevation, int segments) {
        int segs = Math.max(8, segments);
        List<Point3D> pts = new ArrayList<>();
        for (int i = 0; i < segs; i++) {
            double ang = 2.0 * Math.PI * i / segs;
            pts.add(new Point3D(r * Math.cos(ang), elevation, r * Math.sin(ang)));
        }
        return new profile_reference_ui_main(id, name, ProfileType.CIRCLE, new profile_loop_ui_main(pts, true), r * 2.0, r * 2.0, r, elevation);
    }

    public static profile_reference_ui_main createPolygon(String id, String name, List<Point3D> pts, double elevation) {
        return new profile_reference_ui_main(id, name, ProfileType.POLYGON, new profile_loop_ui_main(pts, true), 0, 0, 0, elevation);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ProfileType getType() { return type; }
    public profile_loop_ui_main getLoop() { return loop; }
    public void setLoop(profile_loop_ui_main loop) { this.loop = loop; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getRadius() { return radius; }
    public double getElevation() { return elevation; }
    public void setElevation(double elevation) { this.elevation = elevation; }
}
