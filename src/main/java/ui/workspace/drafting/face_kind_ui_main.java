package ui.workspace.drafting;

/**
 * face_kind_ui_main.java
 * Enumeration of geometric face kinds for 3D solid primitives in Astra CAD.
 * Explicitly distinguishes planar faces from curved lateral/spherical surfaces.
 */
public enum face_kind_ui_main {
    // Box / Cube faces (all planar)
    TOP("Top Face", true),
    BOTTOM("Bottom Face", true),
    FRONT("Front Face", true),
    BACK("Back Face", true),
    LEFT("Left Face", true),
    RIGHT("Right Face", true),

    // Cylinder faces
    TOP_CAP("Top Cap", true),
    BOTTOM_CAP("Bottom Cap", true),
    CYLINDER_LATERAL("Cylinder Lateral", false),

    // Cone faces
    BASE_CAP("Base Cap", true),
    CONE_LATERAL("Cone Lateral", false),

    // Sphere
    SPHERE_SURFACE("Sphere Surface", false);

    private final String label;
    private final boolean planar;

    face_kind_ui_main(String label, boolean planar) {
        this.label = label;
        this.planar = planar;
    }

    public String getLabel() { return label; }
    public boolean isPlanar() { return planar; }
}
