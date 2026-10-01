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
import ui.workspace.drafting.faces.face_kind_ui_main;

public class circular_pattern_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final shape_editor_ui_main editor;
    private final ComboBox<hole_feature_ui_main> seedCombo = new ComboBox<>();
    private final TextField centerUField = new TextField("0.00");
    private final TextField centerVField = new TextField("0.00");
    private final TextField countField = new TextField("6");
    private final CheckBox fullCircleCheck = new CheckBox("Full 360°");
    private final TextField spanField = new TextField("360.00");
    private final ComboBox<String> dirCombo = new ComboBox<>();
    private final Label errorLabel = new Label();

    public static void open(shape_item_ui_main s, shape_editor_ui_main ed, Window owner) {
        if (s == null || !s.hasHoles()) return;
        new circular_pattern_dialog_ui_main(s, ed, owner).show();
    }

    public circular_pattern_dialog_ui_main(shape_item_ui_main shape, shape_editor_ui_main editor, Window owner) {
        this.shape = shape; this.editor = editor;
        if (owner != null) initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Circular Hole Pattern — " + shape.getName());

        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        seedCombo.getItems().addAll(shape.getHoles());
        seedCombo.setCellFactory(lv -> new ListCell<>() {
            protected void updateItem(hole_feature_ui_main h, boolean empty) {
                super.updateItem(h, empty);
                setText((empty || h == null) ? null : h.getHoleType() + " at (" + h.getU() + ", " + h.getV() + ")");
            }
        });
        seedCombo.setButtonCell(seedCombo.getCellFactory().call(null));
        if (!shape.getHoles().isEmpty()) seedCombo.setValue(shape.getHoles().getFirst());

        fullCircleCheck.setSelected(true);
        spanField.setDisable(true);
        fullCircleCheck.setOnAction(e -> spanField.setDisable(fullCircleCheck.isSelected()));

        dirCombo.getItems().addAll("Counterclockwise", "Clockwise");
        dirCombo.setValue("Counterclockwise");

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(5);
        grid.add(new Label("Seed Hole:"), 0, 0);          grid.add(seedCombo, 1, 0);
        grid.add(new Label("Center U (mm):"), 0, 1);      grid.add(centerUField, 1, 1);
        grid.add(new Label("Center V (mm):"), 0, 2);      grid.add(centerVField, 1, 2);
        grid.add(new Label("Count (>=2):"), 0, 3);        grid.add(countField, 1, 3);
        grid.add(fullCircleCheck, 1, 4);
        grid.add(new Label("Span Angle (°):"), 0, 5);     grid.add(spanField, 1, 5);
        grid.add(new Label("Direction:"), 0, 6);          grid.add(dirCombo, 1, 6);
        root.getChildren().add(grid);

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        root.getChildren().add(errorLabel);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(e -> close());
        Button btnApply = new Button("Create Pattern");
        btnApply.setDefaultButton(true);
        btnApply.setStyle("-fx-background-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold;");
        btnApply.setOnAction(e -> applyPattern());

        HBox buttonBar = new HBox(8, btnCancel, btnApply);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(buttonBar);

        setScene(new Scene(root, 360, 290));
    }

    private void applyPattern() {
        hole_feature_ui_main seed = seedCombo.getValue();
        if (seed == null) { showError("Select a seed hole."); return; }
        int count; double cu, cv, span;
        try { count = Integer.parseInt(countField.getText().trim()); } catch (Exception e) { showError("Invalid instance count."); return; }
        try { cu = Double.parseDouble(centerUField.getText().trim()); } catch (Exception e) { showError("Invalid Center U."); return; }
        try { cv = Double.parseDouble(centerVField.getText().trim()); } catch (Exception e) { showError("Invalid Center V."); return; }
        try { span = Double.parseDouble(spanField.getText().trim()); } catch (Exception e) { showError("Invalid span angle."); return; }
        if (count < 2) { showError("Count must be >= 2."); return; }
        boolean full = fullCircleCheck.isSelected();
        if (!full && span <= 0) { showError("Span angle must be > 0."); return; }

        boolean cw = "Clockwise".equals(dirCombo.getValue());
        hole_pattern_ui_main pat = hole_pattern_ui_main.createCircular(shape.getId(), seed.getId(), count, cu, cv, span, cw, full);
        if (!pat.isValid(seed)) { showError("Pattern definition is invalid (radius must be > 0)."); return; }

        // Face boundary check
        face_kind_ui_main kind = seed.getFaceKind();
        if (kind != null && kind.isCylinderCap()) {
            double r = Math.max(1.0, shape.getP1().distance(shape.getP2()));
            if (!pat.fitsWithinCylinderCap(seed, r)) { showError("Pattern instances exceed cylinder cap bounds."); return; }
        } else {
            double w = Math.abs(shape.getP2().getX() - shape.getP1().getX());
            double h = Math.abs(shape.getP2().getZ() - shape.getP1().getZ());
            if (!pat.fitsWithinFace(seed, Math.max(1.0, w), Math.max(1.0, h))) { showError("Pattern instances exceed face bounds."); return; }
        }

        if (pat.hasOverlappingInstances(seed)) { showError("Pattern instances overlap each other."); return; }

        if (editor != null) editor.recordSnapshot();
        shape.addPattern(pat);
        if (editor != null) editor.notifyShapesChanged();
        close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
