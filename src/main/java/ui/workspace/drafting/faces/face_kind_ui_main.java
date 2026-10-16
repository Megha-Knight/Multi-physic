package ui.workspace.drafting.faces;

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
    SPHERE_SURFACE("Sphere Surface", false),

    // Extrusion faces
    EXTRUDE_CAP("Extrude Cap", true),
    EXTRUDE_LATERAL("Extrude Lateral", false),
    EXTRUDE_SIDE_POS_U("Extrude Side +U", true),
    EXTRUDE_SIDE_NEG_U("Extrude Side -U", true),
    EXTRUDE_SIDE_POS_V("Extrude Side +V", true),
    EXTRUDE_SIDE_NEG_V("Extrude Side -V", true);

    private final String label;
    private final boolean planar;

    face_kind_ui_main(String label, boolean planar) {
        this.label = label;
        this.planar = planar;
    }

    public String getLabel() { return label; }
    public boolean isPlanar() { return planar; }
    public boolean isCurved() { return !planar; }
    public boolean isLateral() { return this == CYLINDER_LATERAL || this == CONE_LATERAL || this == EXTRUDE_LATERAL; }
    public boolean isCylinderCap() { return this == TOP_CAP || this == BOTTOM_CAP; }
    public boolean isExtrudeSide() { return this == EXTRUDE_SIDE_POS_U || this == EXTRUDE_SIDE_NEG_U || this == EXTRUDE_SIDE_POS_V || this == EXTRUDE_SIDE_NEG_V; }
    public boolean isExtrudeFace() { return this == EXTRUDE_CAP || this == EXTRUDE_LATERAL || isExtrudeSide(); }
}

