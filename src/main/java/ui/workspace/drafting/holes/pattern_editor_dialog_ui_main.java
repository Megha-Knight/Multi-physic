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

public class pattern_editor_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final hole_pattern_ui_main pattern;
    private final shape_editor_ui_main editor;
    private final ComboBox<hole_feature_ui_main> seedCombo = new ComboBox<>();
    private final TextField countField = new TextField();
    private final Label errorLabel = new Label();

    // Linear controls
    private final ComboBox<hole_pattern_ui_main.LinearDirection> dirCombo = new ComboBox<>();
    private final TextField spacingField = new TextField();

    // Circular controls
    private final TextField centerUField = new TextField();
    private final TextField centerVField = new TextField();
    private final CheckBox fullCircleCheck = new CheckBox("Full 360°");
    private final TextField spanField = new TextField();
    private final ComboBox<String> circDirCombo = new ComboBox<>();

    public static void open(shape_item_ui_main s, hole_pattern_ui_main pat, shape_editor_ui_main ed, Window owner) {
        if (s == null || pat == null) return;
        new pattern_editor_dialog_ui_main(s, pat, ed, owner).show();
    }

    public pattern_editor_dialog_ui_main(shape_item_ui_main shape, hole_pattern_ui_main pattern,
                                         shape_editor_ui_main editor, Window owner) {
        this.shape = shape; this.pattern = pattern; this.editor = editor;
        if (owner != null) initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Edit " + pattern.getPatternType().getLabel() + " — " + shape.getName());

        VBox root = new VBox(8); root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        seedCombo.getItems().addAll(shape.getHoles());
        seedCombo.setCellFactory(lv -> new ListCell<>() {
            protected void updateItem(hole_feature_ui_main h, boolean empty) {
                super.updateItem(h, empty);
                setText((empty || h == null) ? null : h.getHoleType() + " at (" + h.getU() + ", " + h.getV() + ")");
            }
        });
        seedCombo.setButtonCell(seedCombo.getCellFactory().call(null));
        for (hole_feature_ui_main h : shape.getHoles()) {
            if (h.getId().equals(pattern.getSeedHoleId())) { seedCombo.setValue(h); break; }
        }

        countField.setText(String.valueOf(pattern.getCount()));
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(5);
        grid.add(new Label("Seed Hole:"), 0, 0); grid.add(seedCombo, 1, 0);

        if (pattern.getPatternType() == hole_pattern_ui_main.PatternType.LINEAR) {
            dirCombo.getItems().addAll(hole_pattern_ui_main.LinearDirection.U_DIR, hole_pattern_ui_main.LinearDirection.V_DIR);
            dirCombo.setValue(pattern.getDirection());
            spacingField.setText(String.format(java.util.Locale.US, "%.2f", pattern.getSpacing()));
            grid.add(new Label("Direction:"), 0, 1);    grid.add(dirCombo, 1, 1);
            grid.add(new Label("Count (>=2):"), 0, 2);  grid.add(countField, 1, 2);
            grid.add(new Label("Spacing (mm):"), 0, 3); grid.add(spacingField, 1, 3);
        } else {
            centerUField.setText(String.format(java.util.Locale.US, "%.2f", pattern.getCenterU()));
            centerVField.setText(String.format(java.util.Locale.US, "%.2f", pattern.getCenterV()));
            fullCircleCheck.setSelected(pattern.isFullCircle());
            spanField.setText(String.format(java.util.Locale.US, "%.2f", pattern.getAngularSpan()));
            spanField.setDisable(pattern.isFullCircle());
            fullCircleCheck.setOnAction(e -> spanField.setDisable(fullCircleCheck.isSelected()));
            circDirCombo.getItems().addAll("Counterclockwise", "Clockwise");
            circDirCombo.setValue(pattern.isClockwise() ? "Clockwise" : "Counterclockwise");
            grid.add(new Label("Center U (mm):"), 0, 1);   grid.add(centerUField, 1, 1);
            grid.add(new Label("Center V (mm):"), 0, 2);   grid.add(centerVField, 1, 2);
            grid.add(new Label("Count (>=2):"), 0, 3);     grid.add(countField, 1, 3);
            grid.add(fullCircleCheck, 1, 4);
            grid.add(new Label("Span Angle (°):"), 0, 5);  grid.add(spanField, 1, 5);
            grid.add(new Label("Direction:"), 0, 6);       grid.add(circDirCombo, 1, 6);
        }
        root.getChildren().add(grid);

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;"); errorLabel.setVisible(false);
        root.getChildren().add(errorLabel);

        Button btnCancel = new Button("Cancel"); btnCancel.setOnAction(e -> close());
        Button btnApply = new Button("Save Changes"); btnApply.setDefaultButton(true);
        btnApply.setStyle("-fx-background-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold;");
        btnApply.setOnAction(e -> applyEdit());
        HBox buttonBar = new HBox(8, btnCancel, btnApply); buttonBar.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(buttonBar);

        double h = pattern.getPatternType() == hole_pattern_ui_main.PatternType.LINEAR ? 220 : 290;
        setScene(new Scene(root, 360, h));
    }

    private void applyEdit() {
        hole_feature_ui_main seed = seedCombo.getValue();
        if (seed == null) { showError("Select a seed hole."); return; }
        int count;
        try { count = Integer.parseInt(countField.getText().trim()); } catch (Exception e) { showError("Invalid instance count."); return; }
        if (count < 2) { showError("Count must be >= 2."); return; }

        hole_pattern_ui_main temp;
        if (pattern.getPatternType() == hole_pattern_ui_main.PatternType.LINEAR) {
            double spacing;
            try { spacing = Double.parseDouble(spacingField.getText().trim()); } catch (Exception e) { showError("Invalid spacing."); return; }
            if (spacing <= 0) { showError("Spacing must be > 0."); return; }
            temp = new hole_pattern_ui_main(pattern.getId(), shape.getId(), seed.getId(), dirCombo.getValue(), count, spacing);
        } else {
            double cu, cv, span;
            try { cu = Double.parseDouble(centerUField.getText().trim()); } catch (Exception e) { showError("Invalid Center U."); return; }
            try { cv = Double.parseDouble(centerVField.getText().trim()); } catch (Exception e) { showError("Invalid Center V."); return; }
            try { span = Double.parseDouble(spanField.getText().trim()); } catch (Exception e) { showError("Invalid span angle."); return; }
            boolean full = fullCircleCheck.isSelected();
            if (!full && span <= 0) { showError("Span angle must be > 0."); return; }
            boolean cw = "Clockwise".equals(circDirCombo.getValue());
            temp = new hole_pattern_ui_main(pattern.getId(), shape.getId(), seed.getId(), cu, cv, count, span, cw, full);
        }

        StringBuilder err = new StringBuilder();
        if (!pattern_validation_helper_ui_main.validateAgainstHost(shape, temp, seed, err)) {
            showError(err.toString()); return;
        }

        if (editor != null) editor.recordSnapshot();
        if (pattern.getPatternType() == hole_pattern_ui_main.PatternType.LINEAR) {
            pattern.setDirection(temp.getDirection());
            pattern.setCount(temp.getCount());
            pattern.setSpacing(temp.getSpacing());
        } else {
            pattern.setCenterU(temp.getCenterU());
            pattern.setCenterV(temp.getCenterV());
            pattern.setCount(temp.getCount());
            pattern.setFullCircle(temp.isFullCircle());
            pattern.setAngularSpan(temp.getAngularSpan());
            pattern.setClockwise(temp.isClockwise());
        }
        shape.rebuild();
        if (editor != null) editor.notifyShapesChanged();
        close();
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
}
