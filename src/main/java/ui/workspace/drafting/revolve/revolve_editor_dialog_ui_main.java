package ui.workspace.drafting.revolve;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * revolve_editor_dialog_ui_main.java
 * Modal parameter editor dialog for configuring parametric Revolve features.
 */
public class revolve_editor_dialog_ui_main extends Dialog<revolve_feature_ui_main> {

    private final TextField nameField = new TextField();
    private final TextField angleField = new TextField("360.0");
    private final ComboBox<revolve_axis_type_ui_main> axisCombo = new ComboBox<>();
    private final ComboBox<revolve_direction_ui_main> dirCombo = new ComboBox<>();
    private final CheckBox solidCheck = new CheckBox("Solid Body");

    public revolve_editor_dialog_ui_main(revolve_feature_ui_main feature) {
        setTitle(feature == null ? "Create Revolve" : "Edit Revolve");
        setHeaderText("Configure Revolve Parameters");

        axisCombo.getItems().addAll(revolve_axis_type_ui_main.values());
        axisCombo.setValue(feature != null ? feature.getAxis().getType() : revolve_axis_type_ui_main.Y_AXIS);

        dirCombo.getItems().addAll(revolve_direction_ui_main.values());
        dirCombo.setValue(feature != null ? feature.getDirection() : revolve_direction_ui_main.FORWARD);

        if (feature != null) {
            nameField.setText(feature.getName());
            angleField.setText(String.valueOf(feature.getAngle()));
            solidCheck.setSelected(feature.isSolid());
        } else {
            nameField.setText("Revolve");
            solidCheck.setSelected(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Rotation Axis:"), 0, 1);
        grid.add(axisCombo, 1, 1);
        grid.add(new Label("Angle (deg):"), 0, 2);
        grid.add(angleField, 1, 2);
        grid.add(new Label("Direction:"), 0, 3);
        grid.add(dirCombo, 1, 3);
        grid.add(solidCheck, 1, 4);

        getDialogPane().setContent(new VBox(10, grid));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(bt -> {
            if (bt == ButtonType.OK && feature != null) {
                feature.setName(nameField.getText());
                try {
                    feature.setAngle(Double.parseDouble(angleField.getText()));
                } catch (Exception ignored) {}
                feature.setDirection(dirCombo.getValue());
                feature.setSolid(solidCheck.isSelected());
                return feature;
            }
            return null;
        });
    }
}
