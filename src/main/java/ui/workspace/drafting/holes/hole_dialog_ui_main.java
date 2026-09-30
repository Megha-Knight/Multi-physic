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

import java.util.Locale;

/**
 * hole_dialog_ui_main.java
 * Modal dialog for specifying parametric Simple Hole dimensions and placement.
 */
public class hole_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final face_reference_ui_main face;
    private final shape_editor_ui_main editor;

    private final TextField diaField = new TextField("10.00");
    private final TextField depthField = new TextField("15.00");
    private final CheckBox throughCheck = new CheckBox("Through-All");
    private final TextField uField = new TextField("0.00");
    private final TextField vField = new TextField("0.00");
    private final Label errorLabel = new Label();

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
        setTitle("Simple Hole Feature — " + shape.getName());

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        Label hdr = new Label(String.format("Placement: %s on %s", face.getFaceKind().getLabel(), shape.getName()));
        hdr.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #005A85;");
        root.getChildren().add(hdr);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(6);

        throughCheck.setOnAction(e -> depthField.setDisable(throughCheck.isSelected()));

        grid.add(createLabel("Diameter (mm):"), 0, 0); grid.add(diaField, 1, 0);
        grid.add(createLabel("Depth (mm):"), 0, 1);    grid.add(depthField, 1, 1);
        grid.add(throughCheck, 1, 2);
        grid.add(createLabel("Position U (mm):"), 0, 3); grid.add(uField, 1, 3);
        grid.add(createLabel("Position V (mm):"), 0, 4); grid.add(vField, 1, 4);
        root.getChildren().add(grid);

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        root.getChildren().add(errorLabel);

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Cancel");
        btnCancel.setCancelButton(true);
        btnCancel.setOnAction(e -> close());

        Button btnApply = new Button("Create Hole");
        btnApply.setDefaultButton(true);
        btnApply.setStyle("-fx-background-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold;");
        btnApply.setOnAction(e -> applyHole());
        btnBox.getChildren().addAll(btnCancel, btnApply);
        root.getChildren().add(btnBox);

        setScene(new Scene(root, 300, 260));
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
            double u = Double.parseDouble(uField.getText().trim());
            double v = Double.parseDouble(vField.getText().trim());

            if (dia <= 0.1) { showError("Diameter must be greater than 0.1 mm."); return; }
            if (!throughCheck.isSelected() && depth <= 0.1) { showError("Depth must be greater than 0.1 mm."); return; }

            double maxDim = Math.max(face.getFaceWidth(), face.getFaceHeight());
            if (dia >= maxDim) {
                showError(String.format(Locale.US, "Diameter (%.1f) exceeds face size (%.1f mm).", dia, maxDim));
                return;
            }

            hole_feature_ui_main hole = new hole_feature_ui_main(
                java.util.UUID.randomUUID().toString(), shape.getId(), face.getFaceKind(),
                u, v, dia, depth, throughCheck.isSelected()
            );

            if (face.getFaceKind() == face_kind_ui_main.TOP_CAP || face.getFaceKind() == face_kind_ui_main.BOTTOM_CAP) {
                if (!hole.fitsWithinCylinderCap(face.getFaceWidth() * 0.5)) {
                    showError("Hole boundary extends outside the cylinder cap.");
                    return;
                }
            } else {
                if (!hole.fitsWithinFace(face.getFaceWidth(), face.getFaceHeight())) {
                    showError("Hole boundary extends outside the selected face bounds.");
                    return;
                }
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
