package ui.workspace.drafting.topology;

import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import ui.workspace.drafting.faces.face_kind_ui_main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * topology_derived_face_ui_main.java
 * Topology face region derived from features (e.g. remaining face, hole wall, hole floor, through-bore).
 */
public class topology_derived_face_ui_main extends topology_face_ui_main {

    private final String sourceFaceId;
    private final face_kind_ui_main sourceFaceKind;
    private final String creatingFeatureId;
    private final String customId;
    private final String customName;
    private final hole_region_kind_ui_main regionKind;
    private final List<topology_boundary_loop_ui_main> loops = new ArrayList<>();
    private final double depth;
    private final double radius;
    private final Point3D localCenter;
    private topology_face_appearance_ui_main appearanceOverride = null;
    private boolean valid = true;
    private String diagnosticMessage = null;

    public topology_derived_face_ui_main(String bodyId, String customId, String name,
                                         face_kind_ui_main sourceFaceKind, String creatingFeatureId,
                                         hole_region_kind_ui_main regionKind, double width, double height,
                                         double depth, double radius, Point3D localCenter) {
        super(bodyId, sourceFaceKind != null ? sourceFaceKind : face_kind_ui_main.TOP, width, height);
        this.customId = (customId != null && !customId.isBlank()) ? customId : (bodyId + ":F:" + (sourceFaceKind != null ? sourceFaceKind.name() : "DERIVED"));
        this.customName = (name != null && !name.isBlank()) ? name : regionKind.getLabel();
        this.sourceFaceKind = sourceFaceKind;
        this.sourceFaceId = (bodyId != null && sourceFaceKind != null) ? (bodyId + ":F:" + sourceFaceKind.name()) : "";
        this.creatingFeatureId = creatingFeatureId;
        this.regionKind = Objects.requireNonNull(regionKind, "regionKind cannot be null");
        this.depth = depth;
        this.radius = radius;
        this.localCenter = localCenter != null ? localCenter : new Point3D(0, 0, 0);
    }

    @Override public String getId() { return customId; }
    @Override public String getName() { return customName; }

    public void addLoop(topology_boundary_loop_ui_main loop) {
        if (loop != null && !loops.contains(loop)) loops.add(loop);
    }

    public List<topology_boundary_loop_ui_main> getLoops() { return Collections.unmodifiableList(loops); }

    public topology_boundary_loop_ui_main getOuterLoop() {
        for (topology_boundary_loop_ui_main l : loops) if (l.isOuter()) return l;
        return null;
    }

    public List<topology_boundary_loop_ui_main> getInnerLoops() {
        List<topology_boundary_loop_ui_main> inners = new ArrayList<>();
        for (topology_boundary_loop_ui_main l : loops) if (!l.isOuter()) inners.add(l);
        return Collections.unmodifiableList(inners);
    }

    public String getSourceFaceId() { return sourceFaceId; }
    public face_kind_ui_main getSourceFaceKind() { return sourceFaceKind; }
    public String getCreatingFeatureId() { return creatingFeatureId; }
    public hole_region_kind_ui_main getRegionKind() { return regionKind; }
    public double getDepth() { return depth; }
    public double getRadius() { return radius; }
    public Point3D getLocalCenter() { return localCenter; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    private boolean selected = false;
    public boolean isSelected() { return selected; }
    public void setSelected(boolean sel) { this.selected = sel; }

    public topology_face_appearance_ui_main getAppearanceOverride() { return appearanceOverride; }
    public void setAppearanceOverride(topology_face_appearance_ui_main app) { this.appearanceOverride = app; }
    public boolean hasAppearanceOverride() { return appearanceOverride != null; }

    public Color getEffectiveColor() {
        if (appearanceOverride != null && appearanceOverride.diffuseColor() != null) {
            return appearanceOverride.diffuseColor();
        }
        return hole_surface_appearance_ui_main.forRegion(regionKind).diffuseColor();
    }

    public double getEffectiveOpacity() {
        if (appearanceOverride != null) return appearanceOverride.opacity();
        return hole_surface_appearance_ui_main.forRegion(regionKind).opacity();
    }

    @Override
    public String toString() {
        return String.format("DerivedFace[%s (%s on %s, loops=%d, r=%.1f, d=%.1f)]",
            getId(), regionKind.name(), sourceFaceKind != null ? sourceFaceKind.name() : "NONE",
            loops.size(), radius, depth);
    }
}
