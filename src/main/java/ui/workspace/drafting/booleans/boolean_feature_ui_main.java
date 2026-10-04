package ui.workspace.drafting.booleans;

import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import java.util.Objects;
import java.util.UUID;

/**
 * boolean_feature_ui_main.java
 * Parametric Boolean solid feature referencing target and tool solid bodies.
 */
public class boolean_feature_ui_main {

    private final String id;
    private final String targetBodyId;
    private String toolBodyId;
    private String name;
    private boolean_op_type_ui_main opType;
    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private boolean visible = true;
    private String diagnosticMessage = null;

    public boolean_feature_ui_main(String id, String targetBodyId, String toolBodyId,
                                  boolean_op_type_ui_main opType, String name) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.targetBodyId = targetBodyId;
        this.toolBodyId = toolBodyId;
        this.opType = (opType != null) ? opType : boolean_op_type_ui_main.SUBTRACT;
        this.name = (name != null && !name.isBlank()) ? name : (this.opType.getLabel() + " (" + toolBodyId + ")");
    }

    public boolean_feature_ui_main(String targetBodyId, String toolBodyId, boolean_op_type_ui_main opType) {
        this(UUID.randomUUID().toString(), targetBodyId, toolBodyId, opType, null);
    }

    public boolean isValid() {
        return state != feature_state_ui_main.INVALID && targetBodyId != null && toolBodyId != null
                && !targetBodyId.equals(toolBodyId);
    }

    public boolean revalidate(shape_item_ui_main target, shape_item_ui_main tool) {
        if (target == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Target body does not exist";
            return false;
        }
        if (tool == null) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Tool body does not exist";
            return false;
        }
        if (target.getId().equals(tool.getId())) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Target body cannot be identical to tool body";
            return false;
        }
        if (target.getState() == feature_state_ui_main.INVALID || tool.getState() == feature_state_ui_main.INVALID) {
            state = feature_state_ui_main.INVALID;
            diagnosticMessage = "Referenced body has invalid geometry";
            return false;
        }
        state = feature_state_ui_main.CLEAN;
        diagnosticMessage = null;
        return true;
    }

    public String getId() { return id; }
    public String getTargetBodyId() { return targetBodyId; }
    public String getToolBodyId() { return toolBodyId; }
    public void setToolBodyId(String toolId) { this.toolBodyId = toolId; }
    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }
    public boolean_op_type_ui_main getOpType() { return opType; }
    public void setOpType(boolean_op_type_ui_main opType) { this.opType = (opType != null) ? opType : boolean_op_type_ui_main.SUBTRACT; }
    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main s) { this.state = (s != null) ? s : feature_state_ui_main.CLEAN; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    public boolean_feature_ui_main copy() {
        boolean_feature_ui_main cp = new boolean_feature_ui_main(id, targetBodyId, toolBodyId, opType, name);
        cp.setState(state);
        cp.setVisible(visible);
        cp.setDiagnosticMessage(diagnosticMessage);
        return cp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof boolean_feature_ui_main that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
