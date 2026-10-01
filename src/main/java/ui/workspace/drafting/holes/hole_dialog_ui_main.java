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
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;

import java.util.Locale;

/**
 * hole_dialog_ui_main.java
 * Modal dialog for specifying parametric Holes (Simple, Countersink, Counterbore).
 */
public class hole_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final face_reference_ui_main face;
    private final shape_editor_ui_main editor;

    private final ComboBox<HoleType> typeCombo = new ComboBox<>();
    private final TextField diaField = new TextField("10.00");
    private final TextField csDiaField = new TextField("18.00");
    private final TextField csAngleField = new TextField("90.00");
    private final TextField cbDiaField = new TextField("16.00");
    private final TextField cbDepthField = new TextField("5.00");
    private final TextField depthField = new TextField("20.00");
    private final CheckBox throughCheck = new CheckBox("Through-All");
    private final TextField uField = new TextField("0.00");
    private final TextField vField = new TextField("0.00");
    private final Label errorLabel = new Label();

    private final HBox csDiaRow = new HBox(6), csAngleRow = new HBox(6);
    private final HBox cbDiaRow = new HBox(6), cbDepthRow = new HBox(6);

    public static void open(shape_item_ui_main shape, face_reference_ui_main face,
                            shape_editor_ui_main editor, Window owner) {
        if (shape == null || face == null) return;
        new hole_dialog_ui_main(shape, face, editor, owner).show();
    }

    public hole_dialog_ui_main(shape_item_ui_main shape, face_reference_ui_main face,
                               shape_editor_ui_main editor, Window owner) {
        this.shape = shape; this.face = face; this.editor = editor;
        if (owner != null) initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Parametric Hole Feature — " + shape.getName());

        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        Label hdr = new Label(String.format("Placement: %s on %s", face.getFaceKind().getLabel(), shape.getName()));
        hdr.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #005A85;");
        root.getChildren().add(hdr);

        typeCombo.getItems().addAll(HoleType.SIMPLE, HoleType.COUNTERSINK, HoleType.COUNTERBORE);
        typeCombo.setValue(HoleType.SIMPLE);
        typeCombo.setOnAction(e -> updateTypeVisibility());

        throughCheck.setOnAction(e -> depthField.setDisable(throughCheck.isSelected()));

        setupRow(csDiaRow, "Countersink Dia (mm):", csDiaField);
        setupRow(csAngleRow, "Cone Angle (deg):", csAngleField);
        setupRow(cbDiaRow, "Counterbore Dia (mm):", cbDiaField);
        setupRow(cbDepthRow, "Counterbore Depth (mm):", cbDepthField);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(5);
        grid.add(createLabel("Hole Type:"), 0, 0);       grid.add(typeCombo, 1, 0);
        grid.add(createLabel("Bore Dia (mm):"), 0, 1);    grid.add(diaField, 1, 1);
        grid.add(csDiaRow, 0, 2, 2, 1);
        grid.add(csAngleRow, 0, 3, 2, 1);
        grid.add(cbDiaRow, 0, 4, 2, 1);
        grid.add(cbDepthRow, 0, 5, 2, 1);
        grid.add(createLabel("Depth (mm):"), 0, 6);       grid.add(depthField, 1, 6);
        grid.add(throughCheck, 1, 7);
        grid.add(createLabel("Position U (mm):"), 0, 8);  grid.add(uField, 1, 8);
        grid.add(createLabel("Position V (mm):"), 0, 9);  grid.add(vField, 1, 9);
        root.getChildren().add(grid);

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        root.getChildren().add(errorLabel);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(e -> close());
        Button btnApply = new Button("Create Hole");
        btnApply.setDefaultButton(true);
        btnApply.setStyle("-fx-background-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold;");
        btnApply.setOnAction(e -> applyHole());

        HBox btnBox = new HBox(8, btnCancel, btnApply);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(btnBox);

        updateTypeVisibility();
        setScene(new Scene(root, 320, 380));
    }

    private void setupRow(HBox row, String labelText, TextField tf) {
        Label lbl = createLabel(labelText);
        lbl.setPrefWidth(140);
        tf.setPrefWidth(120);
        row.getChildren().addAll(lbl, tf);
        row.setAlignment(Pos.CENTER_LEFT);
    }

    private void updateTypeVisibility() {
        HoleType t = typeCombo.getValue();
        boolean isCs = (t == HoleType.COUNTERSINK), isCb = (t == HoleType.COUNTERBORE);
        csDiaRow.setVisible(isCs);   csDiaRow.setManaged(isCs);
        csAngleRow.setVisible(isCs); csAngleRow.setManaged(isCs);
        cbDiaRow.setVisible(isCb);   cbDiaRow.setManaged(isCb);
        cbDepthRow.setVisible(isCb); cbDepthRow.setManaged(isCb);
    }

    private Label createLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;");
        return l;
    }

    private void applyHole() {
        try {
            double dia = Double.parseDouble(diaField.getText().trim());
            double depth = throughCheck.isSelected() ? 50.0 : Double.parseDouble(depthField.getText().trim());
            double u = Double.parseDouble(uField.getText().trim()), v = Double.parseDouble(vField.getText().trim());
            HoleType type = typeCombo.getValue();
            double csDia = 0, csAngle = 90.0, cbDia = 0, cbDepth = 0;

            if (type == HoleType.COUNTERSINK) {
                csDia = Double.parseDouble(csDiaField.getText().trim());
                csAngle = Double.parseDouble(csAngleField.getText().trim());
            } else if (type == HoleType.COUNTERBORE) {
                cbDia = Double.parseDouble(cbDiaField.getText().trim());
                cbDepth = Double.parseDouble(cbDepthField.getText().trim());
            }

            hole_feature_ui_main hole = new hole_feature_ui_main(
                java.util.UUID.randomUUID().toString(), shape.getId(), type, face.getFaceKind(),
                u, v, dia, depth, throughCheck.isSelected(), csDia, csAngle, cbDia, cbDepth
            );

            if (!hole.isValid()) {
                showError("Invalid hole parameters. Verify diameter, angles, and depths.");
                return;
            }

            boolean isCyl = (face.getFaceKind() == face_kind_ui_main.TOP_CAP || face.getFaceKind() == face_kind_ui_main.BOTTOM_CAP);
            boolean fits = isCyl ? hole.fitsWithinCylinderCap(face.getFaceWidth() * 0.5) : hole.fitsWithinFace(face.getFaceWidth(), face.getFaceHeight());
            if (!fits) {
                showError("Hole boundary extends outside the selected face bounds.");
                return;
            }

            if (editor != null) editor.recordSnapshot();
            shape.addHole(hole);
            if (editor != null) editor.notifyShapesChanged();
            close();
        } catch (NumberFormatException ex) {
            showError("Please enter valid numeric values.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
