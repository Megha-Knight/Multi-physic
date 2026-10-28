package cam.operation;

/**
 * Enumeration of supported CAM machining operation types in Astra.
 */
public enum CamOperationType {
    SPOT_DRILLING("Spot Drilling", true),
    DEEP_HOLE_DRILLING("Deep-Hole Peck Drilling", true),
    PRISMATIC_ROUGHING("Prismatic Roughing", false),
    CONTOUR_FINISHING("Contour Finishing", false),
    PARALLEL_PLANAR_FINISHING("Parallel Planar Finishing", false),
    WATERLINE_FINISHING("Waterline Finishing", false),
    FACING("Facing", false);

    private final String label;
    private final boolean axialHolemaking;

    CamOperationType(String label, boolean axialHolemaking) {
        this.label = label;
        this.axialHolemaking = axialHolemaking;
    }

    public String getLabel()               { return label; }
    public boolean isAxialHolemaking()     { return axialHolemaking; }
}
