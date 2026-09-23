package ui.workspace;

/**
 * basicshapes_ui_main.java
 * Shape type enumeration and state descriptor for 2D profile drafting and 3D primitives in Astra.
 */
public enum basicshapes_ui_main {
    NONE("Select / Navigate", "Navigation mode (orbit & pan enabled)"),
    CIRCLE("Circle", "Click center point, drag for radius, click to place"),
    SQUARE("Square", "Click corner point, drag for side length, click to place"),
    RECTANGLE("Rectangle", "Click corner point, drag for width/height, click to place"),
    EQUILATERAL_TRIANGLE("Equilateral Triangle", "Click base point, drag for equal sides, click to place"),
    RIGHT_TRIANGLE("Right Triangle", "Click 90° corner point, drag base/height, click to place"),
    CUBE("Cube (3D)", "Click base point, drag for width & height, click to place"),
    CYLINDER("Cylinder (3D)", "Click center point, drag for radius & height, click to place"),
    SPHERE("Sphere (3D)", "Click center point, drag for radius, click to place"),
    CONE("Cone (3D)", "Click center point, drag for base radius & height, click to place");

    private final String label;
    private final String instruction;

    basicshapes_ui_main(String label, String instruction) {
        this.label = label;
        this.instruction = instruction;
    }

    public String getLabel() { return label; }
    public String getInstruction() { return instruction; }
    public boolean isDrawing() { return this != NONE; }
    public boolean is3D() { return this == CUBE || this == CYLINDER || this == SPHERE || this == CONE; }
}
