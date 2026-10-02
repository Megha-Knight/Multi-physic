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

public class linear_pattern_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final shape_editor_ui_main editor;
    private final ComboBox<hole_feature_ui_main> seedCombo = new ComboBox<>();
    private final ComboBox<hole_pattern_ui_main.LinearDirection> dirCombo = new ComboBox<>();
    private final TextField countField = new TextField("4");
    private final TextField spacingField = new TextField("20.00");
    private final Label errorLabel = new Label();

    public static void open(shape_item_ui_main s, shape_editor_ui_main ed, Window owner) {
        if (s == null || !s.hasHoles()) return;
        new linear_pattern_dialog_ui_main(s, ed, owner).show();
    }

    public linear_pattern_dialog_ui_main(shape_item_ui_main shape, shape_editor_ui_main editor, Window owner) {
        this.shape = shape; this.editor = editor;
        if (owner != null) initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Linear Hole Pattern — " + shape.getName());

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
        if (!shape.getHoles().isEmpty()) {
            hole_feature_ui_main selHole = (editor != null) ? editor.getSelectedHole() : null;
            if (selHole != null && shape.getHoles().contains(selHole)) seedCombo.setValue(selHole);
            else seedCombo.setValue(shape.getHoles().getFirst());
        }

        dirCombo.getItems().addAll(hole_pattern_ui_main.LinearDirection.U_DIR, hole_pattern_ui_main.LinearDirection.V_DIR);
        dirCombo.setValue(hole_pattern_ui_main.LinearDirection.U_DIR);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(5);
        grid.add(new Label("Seed Hole:"), 0, 0);       grid.add(seedCombo, 1, 0);
        grid.add(new Label("Direction:"), 0, 1);       grid.add(dirCombo, 1, 1);
        grid.add(new Label("Count (>=2):"), 0, 2);     grid.add(countField, 1, 2);
        grid.add(new Label("Spacing (mm):"), 0, 3);    grid.add(spacingField, 1, 3);
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

        setScene(new Scene(root, 340, 220));
    }

    private void applyPattern() {
        hole_feature_ui_main seed = seedCombo.getValue();
        if (seed == null) { showError("Select a seed hole."); return; }
        int count; double spacing;
        try { count = Integer.parseInt(countField.getText().trim()); } catch (Exception e) { showError("Invalid instance count."); return; }
        try { spacing = Double.parseDouble(spacingField.getText().trim()); } catch (Exception e) { showError("Invalid spacing."); return; }
        if (count < 2) { showError("Count must be >= 2."); return; }
        if (spacing <= 0) { showError("Spacing must be > 0."); return; }

        hole_pattern_ui_main pat = hole_pattern_ui_main.createLinear(shape.getId(), seed.getId(), count, dirCombo.getValue(), spacing);
        StringBuilder err = new StringBuilder();
        if (!pattern_validation_helper_ui_main.validateAgainstHost(shape, pat, seed, err)) {
            showError(err.toString());
            return;
        }

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
