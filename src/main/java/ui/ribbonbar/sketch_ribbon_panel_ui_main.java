package ui.ribbonbar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ui.framework_ui_main;
import ui.workspace.drafting.sketch.sketch_controller_ui_main;
import ui.workspace.drafting.sketch.sketch_tool_type_ui_main;

/**
 * sketch_ribbon_panel_ui_main.java
 * Ribbon tool group for Interactive Sketch Mode tools (Select, Line, Circle, Rect, Arc, Close).
 */
public class sketch_ribbon_panel_ui_main extends HBox implements dynamic_panel_entry_ui_main {

    private final ToggleGroup toolGroup = new ToggleGroup();
    private final ToggleButton btnSelect = createToolBtn("Select", sketch_tool_type_ui_main.SELECT);
    private final ToggleButton btnLine   = createToolBtn("Line", sketch_tool_type_ui_main.LINE);
    private final ToggleButton btnCircle = createToolBtn("Circle", sketch_tool_type_ui_main.CIRCLE);
    private final ToggleButton btnRect       = createToolBtn("Rect", sketch_tool_type_ui_main.RECTANGLE);
    private final ToggleButton btnCenterRect = createToolBtn("Center Rect", sketch_tool_type_ui_main.CENTER_RECTANGLE);
    private final ToggleButton btnArc        = createToolBtn("Arc", sketch_tool_type_ui_main.ARC);
    private final Button btnFinish           = new Button("Finish Sketch");
    private final Button btnCancel           = new Button("Cancel");

    private sketch_controller_ui_main controller = null;
    private Runnable onFinishRequested = null;
    private Runnable onCancelRequested = null;

    public sketch_ribbon_panel_ui_main() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        setPadding(new Insets(1, 4, 1, 4));

        btnSelect.setSelected(true);
        btnSelect.setTooltip(new javafx.scene.control.Tooltip("Select / pick sketch geometry (Del/Backspace to delete)"));
        btnLine.setTooltip(new javafx.scene.control.Tooltip("Line: Click start point, then click end point"));
        btnCircle.setTooltip(new javafx.scene.control.Tooltip("Circle: Click center point, then click radius"));
        btnRect.setTooltip(new javafx.scene.control.Tooltip("Corner Rectangle: Click first corner, then opposite corner"));
        btnCenterRect.setTooltip(new javafx.scene.control.Tooltip("Center Rectangle: Click center, then corner (with diagonal construction lines)"));
        btnArc.setTooltip(new javafx.scene.control.Tooltip("Arc: Click center, start angle, and end angle"));

        btnFinish.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-size: 11px; " +
                           "-fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
        btnFinish.setTooltip(new javafx.scene.control.Tooltip("Validate and save the sketch for downstream features"));
        btnFinish.setOnAction(e -> { if (onFinishRequested != null) onFinishRequested.run(); });

        btnCancel.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-size: 11px; " +
                           "-fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
        btnCancel.setTooltip(new javafx.scene.control.Tooltip("Cancel sketch mode without saving"));
        btnCancel.setOnAction(e -> { if (onCancelRequested != null) onCancelRequested.run(); });

        HBox toolsRow = new HBox(3, btnSelect, btnLine, btnCircle, btnRect, btnCenterRect, btnArc, btnCancel, btnFinish);
        toolsRow.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("Sketch Tools");
        lbl.setStyle("-fx-font-size: " + framework_ui_main.RIBBON_GROUP_FONT_SIZE + "px; " +
                     "-fx-text-fill: " + framework_ui_main.TEXT_MUTED_COLOR + "; -fx-font-weight: bold;");
        VBox box = new VBox(1, toolsRow, lbl);
        box.setAlignment(Pos.CENTER);

        getChildren().add(box);
        setVisible(false);
        setManaged(false);
    }

    public void bind(sketch_controller_ui_main ctrl, Runnable onFinish, Runnable onCancel) {
        this.controller = ctrl;
        this.onFinishRequested = onFinish;
        this.onCancelRequested = onCancel;
    }

    public void bind(sketch_controller_ui_main ctrl, Runnable onClose) {
        bind(ctrl, onClose, onClose);
    }

    public void show() { setVisible(true); setManaged(true); }
    public void hide() { setVisible(false); setManaged(false); }
    @Override public boolean isPanelVisible() { return isVisible(); }
    @Override public javafx.scene.Node asNode() { return this; }

    public void setSketchModeActive(boolean active) {
        setVisible(active);
        setManaged(active);
        if (active) {
            btnSelect.setSelected(true);
            if (controller != null) controller.setTool(sketch_tool_type_ui_main.SELECT);
        }
    }

    private ToggleButton createToolBtn(String text, sketch_tool_type_ui_main toolType) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(toolGroup);
        b.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
        b.setOnAction(e -> {
            if (b.isSelected() && controller != null) {
                controller.setTool(toolType);
            }
        });
        return b;
    }
}
