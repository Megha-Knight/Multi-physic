package ui.workspace.drafting.holes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.framework_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import java.util.Locale;

public class hole_editor_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final hole_feature_ui_main hole;
    private final shape_editor_ui_main editor;

    private final TextField nameField = new TextField();
    private final TextField diaField = new TextField();
    private final TextField w2Field = new TextField();
    private final TextField depthField = new TextField();
    private final CheckBox throughCheck = new CheckBox("Through-All");
    private final TextField uField = new TextField();
    private final TextField vField = new TextField();
    private final TextField csDiaField = new TextField("18.00");
    private final TextField csAngleField = new TextField("90.00");
    private final TextField cbDiaField = new TextField("16.00");
    private final TextField cbDepthField = new TextField("5.00");
    private final Label errorLabel = new Label();

    public static void open(shape_item_ui_main s, hole_feature_ui_main h, shape_editor_ui_main ed, Window w) {
        if (s != null && h != null) new hole_editor_dialog_ui_main(s, h, ed, w).show();
    }

    public hole_editor_dialog_ui_main(shape_item_ui_main s, hole_feature_ui_main h, shape_editor_ui_main ed, Window w) {
        this.shape = s; this.hole = h; this.editor = ed;
        if (w != null) initOwner(w);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Edit Hole / Cutout — " + h.getName());

        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        Label hdr = new Label(String.format("Shape: %s | Face: %s | %s", s.getName(), h.getFaceKind().getLabel(), h.getHoleType().getLabel()));
        hdr.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #1E293B;");
        root.getChildren().add(hdr);

        nameField.setText(h.getName());
        diaField.setText(String.format(Locale.US, "%.2f", h.getDiameter()));
        w2Field.setText(String.format(Locale.US, "%.2f", h.getWidth2() > 0.1 ? h.getWidth2() : h.getDiameter() * 0.6));
        depthField.setText(String.format(Locale.US, "%.2f", h.getDepth()));
        throughCheck.setSelected(h.isThroughAll());
        uField.setText(String.format(Locale.US, "%.2f", h.getU()));
        vField.setText(String.format(Locale.US, "%.2f", h.getV()));

        root.getChildren().add(row("Name:", nameField));
        root.getChildren().add(row("Size / Dia (mm):", diaField));
        if (h.getCutoutShape() == hole_feature_ui_main.CutoutShape.RECTANGLE || h.getCutoutShape() == hole_feature_ui_main.CutoutShape.RIGHT_TRIANGLE) {
            root.getChildren().add(row("Width 2 (mm):", w2Field));
        }

        if (h.getHoleType() == hole_feature_ui_main.HoleType.COUNTERSINK) {
            csDiaField.setText(String.format(Locale.US, "%.2f", h.getCsDiameter()));
            csAngleField.setText(String.format(Locale.US, "%.2f", h.getCsAngle()));
            root.getChildren().addAll(row("Sink Dia (Ds):", csDiaField), row("Angle (deg):", csAngleField));
        } else if (h.getHoleType() == hole_feature_ui_main.HoleType.COUNTERBORE) {
            cbDiaField.setText(String.format(Locale.US, "%.2f", h.getCbDiameter()));
            cbDepthField.setText(String.format(Locale.US, "%.2f", h.getCbDepth()));
            root.getChildren().addAll(row("Bore Dia (Db):", cbDiaField), row("Bore Depth:", cbDepthField));
        }

        HBox depthRow = row("Depth (mm):", depthField);
        depthField.disableProperty().bind(throughCheck.selectedProperty());
        root.getChildren().addAll(throughCheck, depthRow);
        root.getChildren().addAll(row("Pos U (mm):", uField), row("Pos V (mm):", vField));

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 10px;");
        errorLabel.setWrapText(true);
        root.getChildren().add(errorLabel);

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle(btnStyle(framework_ui_main.OBJECT_SELECTED_COLOR));
        applyBtn.setOnAction(e -> onApply());

        Button delBtn = new Button("Delete Hole");
        delBtn.setStyle(btnStyle("#DC2626"));
        delBtn.setOnAction(e -> onDelete());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(btnStyle("#64748B"));
        cancelBtn.setOnAction(e -> close());

        HBox actions = new HBox(8, delBtn, new Region(), cancelBtn, applyBtn);
        HBox.setHgrow(actions.getChildren().get(1), Priority.ALWAYS);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(actions);

        setScene(new Scene(root, 320, 390));
        setResizable(false);
    }

    private void onApply() {
        try {
            double dia = Double.parseDouble(diaField.getText().trim());
            double u = Double.parseDouble(uField.getText().trim()), v = Double.parseDouble(vField.getText().trim());
            boolean through = throughCheck.isSelected();
            double depth = through ? 1000.0 : Double.parseDouble(depthField.getText().trim());
            if (dia < 0.1 || (!through && depth < 0.1)) {
                errorLabel.setText("Invalid dimensions: Dia/depth must be >= 0.1 mm."); return;
            }
            hole.setName(nameField.getText().trim());
            hole.setDiameter(dia);
            hole.setThroughAll(through);
            hole.setDepth(depth);
            hole.setU(u);
            hole.setV(v);
            if (hole.getCutoutShape() == hole_feature_ui_main.CutoutShape.RECTANGLE || hole.getCutoutShape() == hole_feature_ui_main.CutoutShape.RIGHT_TRIANGLE) {
                hole.setWidth2(Double.parseDouble(w2Field.getText().trim()));
            }
            if (hole.getHoleType() == hole_feature_ui_main.HoleType.COUNTERSINK) {
                hole.setCsDiameter(Double.parseDouble(csDiaField.getText().trim()));
                hole.setCsAngle(Double.parseDouble(csAngleField.getText().trim()));
            } else if (hole.getHoleType() == hole_feature_ui_main.HoleType.COUNTERBORE) {
                hole.setCbDiameter(Double.parseDouble(cbDiaField.getText().trim()));
                hole.setCbDepth(Double.parseDouble(cbDepthField.getText().trim()));
            }
            shape.rebuild();
            if (editor != null) {
                editor.notifyShapesChanged();
                editor.selectHole(shape, hole);
            }
            close();
        } catch (Exception ex) {
            errorLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void onDelete() {
        shape.removeHole(hole.getId());
        if (editor != null) {
            editor.selectHole(shape, null);
            editor.notifyShapesChanged();
        }
        close();
    }

    private HBox row(String lbl, TextField tf) {
        Label l = new Label(lbl); l.setPrefWidth(95); l.setStyle("-fx-font-size: 11px;");
        tf.setPrefWidth(190);
        tf.setStyle("-fx-font-size: 11px; -fx-padding: 3; -fx-border-color: #CBD5E1; -fx-border-radius: 2;");
        HBox h = new HBox(6, l, tf); h.setAlignment(Pos.CENTER_LEFT); return h;
    }

    private String btnStyle(String bg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-weight: bold; "
             + "-fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-background-radius: 3; -fx-cursor: hand;";
    }
}
