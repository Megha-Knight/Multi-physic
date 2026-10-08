package ui.workspace.drafting.loft;

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
 * loft_editor_dialog_ui_main.java
 * Modal dialog for inspecting and configuring Loft feature sections and parameters.
 */
public class loft_editor_dialog_ui_main {

    public interface LoftDialogCallback {
        void onApply(loft_feature_ui_main modified);
    }

    public static void show(Stage parentStage, shape_item_ui_main host, loft_feature_ui_main loft, LoftDialogCallback cb) {
        if (loft == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        if (parentStage != null) dialog.initOwner(parentStage);
        dialog.setTitle("Edit Loft Feature");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #FFFFFF;");

        Label title = new Label("Loft Parameters: " + loft.getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1E293B;");

        CheckBox solidCb = new CheckBox("Create Solid Body (Cap Start & End)");
        solidCb.setSelected(loft.isSolid());

        ListView<String> secList = new ListView<>();
        for (var s : loft.getSections()) {
            secList.getItems().add(s.getName() + " [Elev: " + s.getElevation() + "]");
        }
        secList.setPrefHeight(120);

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle("-fx-background-color: #0284C7; -fx-text-fill: white; -fx-font-weight: bold;");
        Button cancelBtn = new Button("Cancel");

        applyBtn.setOnAction(e -> {
            loft.setSolid(solidCb.isSelected());
            if (cb != null) cb.onApply(loft);
            dialog.close();
        });
        cancelBtn.setOnAction(e -> dialog.close());

        btnBox.getChildren().addAll(cancelBtn, applyBtn);
        root.getChildren().addAll(title, solidCb, new Label("Sections (" + loft.getSectionCount() + "):"), secList, btnBox);

        dialog.setScene(new Scene(root, 360, 320));
        dialog.showAndWait();
    }
}
