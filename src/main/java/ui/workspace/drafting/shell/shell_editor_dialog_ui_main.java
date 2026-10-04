package ui.workspace.drafting.shell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ui.workspace.drafting.faces.face_kind_ui_main;

import java.util.*;
import java.util.function.Consumer;

/**
 * shell_editor_dialog_ui_main.java
 * Interactive JavaFX modal dialog for defining and editing parametric CAD shell features.
 */
public class shell_editor_dialog_ui_main {

    private final Stage stage = new Stage();
    private final TextField thicknessField = new TextField("2.0");
    private final ComboBox<shell_direction_ui_main> directionCombo = new ComboBox<>();
    private final Map<face_kind_ui_main, CheckBox> faceChecks = new LinkedHashMap<>();
    private final Label errorLabel = new Label();
    private Consumer<shell_feature_ui_main> onApply;

    public shell_editor_dialog_ui_main(Stage owner, String shapeId, shell_feature_ui_main existing) {
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existing != null ? "Edit Shell Feature" : "Create Shell Feature");
        stage.setResizable(false);

        directionCombo.getItems().addAll(shell_direction_ui_main.values());
        directionCombo.setValue(existing != null ? existing.getDirection() : shell_direction_ui_main.INWARD);

        if (existing != null) {
            thicknessField.setText(String.format(Locale.US, "%.2f", existing.getThickness()));
        }

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label tLbl = new Label("Thickness:");
        tLbl.setStyle("-fx-text-fill: #1E293B;");
        grid.add(tLbl, 0, 0);
        grid.add(thicknessField, 1, 0);

        Label dLbl = new Label("Direction:");
        dLbl.setStyle("-fx-text-fill: #1E293B;");
        grid.add(dLbl, 0, 1);
        grid.add(directionCombo, 1, 1);

        Label fLbl = new Label("Open Faces:");
        fLbl.setStyle("-fx-text-fill: #1E293B;");
        grid.add(fLbl, 0, 2);

        FlowPane facesPane = new FlowPane(8, 8);
        for (face_kind_ui_main k : List.of(face_kind_ui_main.TOP, face_kind_ui_main.BOTTOM, face_kind_ui_main.FRONT, face_kind_ui_main.BACK, face_kind_ui_main.LEFT, face_kind_ui_main.RIGHT)) {
            CheckBox cb = new CheckBox(k.getLabel());
            cb.setStyle("-fx-text-fill: #334155;");
            if (existing != null && existing.hasRemovedFace(k)) cb.setSelected(true);
            else if (existing == null && k == face_kind_ui_main.TOP) cb.setSelected(true);
            faceChecks.put(k, cb);
            facesPane.getChildren().add(cb);
        }
        grid.add(facesPane, 1, 2);

        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
        errorLabel.setWrapText(true);

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Cancel");
        Button btnApply = new Button("Apply");
        btnApply.setStyle("-fx-background-color: #0284C7; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        btnCancel.setOnAction(e -> stage.close());
        btnApply.setOnAction(e -> {
            try {
                double t = Double.parseDouble(thicknessField.getText().trim());
                if (t <= 0 || Double.isNaN(t) || Double.isInfinite(t)) {
                    errorLabel.setText("Thickness must be positive.");
                    return;
                }
                List<face_kind_ui_main> selected = new ArrayList<>();
                for (var entry : faceChecks.entrySet()) if (entry.getValue().isSelected()) selected.add(entry.getKey());
                if (selected.size() >= 6) {
                    errorLabel.setText("Cannot remove all 6 faces.");
                    return;
                }
                String id = existing != null ? existing.getId() : UUID.randomUUID().toString();
                String name = existing != null ? existing.getName() : "Shell";
                shell_feature_ui_main result = new shell_feature_ui_main(id, shapeId, name, t, directionCombo.getValue(), selected);
                if (onApply != null) onApply.accept(result);
                stage.close();
            } catch (Exception ex) {
                errorLabel.setText("Invalid numeric input.");
            }
        });
        btnBox.getChildren().addAll(btnCancel, btnApply);

        root.getChildren().addAll(grid, errorLabel, btnBox);
        Scene scene = new Scene(root, 360, 260);
        stage.setScene(scene);
    }

    public void setOnApply(Consumer<shell_feature_ui_main> onApply) {
        this.onApply = onApply;
    }

    public void show() {
        stage.show();
    }
}
