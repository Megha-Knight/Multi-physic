package ui.workspace.drafting.sweep;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ui.workspace.drafting.shape_item_ui_main;

/**
 * sweep_editor_dialog_ui_main.java
 * Modal dialog for inspecting and configuring Sweep feature parameters.
 */
public class sweep_editor_dialog_ui_main {

    public interface SweepDialogCallback {
        void onApply(sweep_feature_ui_main modified);
    }

    public static void show(Stage parentStage, shape_item_ui_main host, sweep_feature_ui_main sweep, SweepDialogCallback cb) {
        if (sweep == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        if (parentStage != null) dialog.initOwner(parentStage);
        dialog.setTitle("Edit Sweep Feature");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #FFFFFF;");

        Label title = new Label("Sweep Parameters: " + sweep.getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1E293B;");

        CheckBox solidCb = new CheckBox("Create Solid Body (Cap Start & End)");
        solidCb.setSelected(sweep.isSolid());

        ComboBox<sweep_orientation_ui_main> orientCombo = new ComboBox<>();
        orientCombo.getItems().addAll(sweep_orientation_ui_main.values());
        orientCombo.setValue(sweep.getOrientation());

        Label profInfo = new Label("Profile: " + (sweep.getProfile() != null ? sweep.getProfile().getName() : "None"));
        Label pathInfo = new Label("Path Length: " + String.format(java.util.Locale.US, "%.1f", sweep.getPath() != null ? sweep.getPath().getLength() : 0.0));

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle("-fx-background-color: #0284C7; -fx-text-fill: white; -fx-font-weight: bold;");
        Button cancelBtn = new Button("Cancel");

        applyBtn.setOnAction(e -> {
            sweep.setSolid(solidCb.isSelected());
            sweep.setOrientation(orientCombo.getValue());
            if (cb != null) cb.onApply(sweep);
            dialog.close();
        });
        cancelBtn.setOnAction(e -> dialog.close());

        btnBox.getChildren().addAll(cancelBtn, applyBtn);
        root.getChildren().addAll(title, profInfo, pathInfo, new Label("Orientation:"), orientCombo, solidCb, btnBox);

        dialog.setScene(new Scene(root, 360, 300));
        dialog.showAndWait();
    }
}
