package ui.workspace.drafting.booleans;

/**
 * boolean_op_type_ui_main.java
 * Defines solid Boolean operation types for multi-body CAD modeling.
 */
public enum boolean_op_type_ui_main {
    UNION("Boolean Union", "∪"),
    SUBTRACT("Boolean Subtract", "-"),
    INTERSECT("Boolean Intersect", "∩");

    private final String label;
    private final String symbol;

    boolean_op_type_ui_main(String label, String symbol) {
        this.label = label;
        this.symbol = symbol;
    }

    public String getLabel() { return label; }
    public String getSymbol() { return symbol; }
}
