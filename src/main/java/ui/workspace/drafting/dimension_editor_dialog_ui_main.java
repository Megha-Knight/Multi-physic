package ui.workspace.drafting;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.framework_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;

import java.util.*;

/**
 * dimension_editor_dialog_ui_main.java
 * Modal CAD Dimension & Position Properties editor dialog for Astra shapes.
 */
public class dimension_editor_dialog_ui_main extends Stage {

    private final shape_item_ui_main item;
    private final shape_editor_ui_main editor;
    private final Map<String, TextField> fieldMap = new LinkedHashMap<>();
    private final TextField nameField;
    private final Label errorLabel = new Label();
    private boolean syncLock = false;

    public static void open(shape_item_ui_main item, shape_editor_ui_main editor, Window owner) {
        if (item == null) return;
        new dimension_editor_dialog_ui_main(item, editor, owner).show();
    }

    public dimension_editor_dialog_ui_main(shape_item_ui_main item, shape_editor_ui_main editor, Window owner) {
        this.item = item;
        this.editor = editor;
        if (owner != null) initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Object Properties — " + item.getName());

        VBox root = new VBox(8);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        // Object Name
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label("Object Name:");
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        nameField = new TextField(item.getName());
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameRow.getChildren().addAll(nameLbl, nameField);
        root.getChildren().add(nameRow);

        // Sections
        Map<String, Double> dims = shape_dimension_helper_ui_main.getDimensions(item);
        root.getChildren().add(buildSection("Geometry", dims, false));
        root.getChildren().add(buildSection("Position", dims, true));

        setupSyncListeners();

        // Error message
        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        root.getChildren().add(errorLabel);

        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        Button btnApply = new Button("Apply");
        btnApply.setDefaultButton(true);
        btnApply.setStyle("-fx-background-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        Button btnCancel = new Button("Cancel");
        btnCancel.setCancelButton(true);
        btnCancel.setOnAction(e -> close());
        btnApply.setOnAction(e -> applyEdits());
        btnBox.getChildren().addAll(btnCancel, btnApply);
        root.getChildren().add(btnBox);

        Scene scene = new Scene(root, framework_ui_main.DIMENSION_DIALOG_WIDTH, framework_ui_main.DIMENSION_DIALOG_HEIGHT);
        setScene(scene);
    }

    private VBox buildSection(String title, Map<String, Double> allDims, boolean positionOnly) {
        VBox sec = new VBox(4);
        Label lbl = new Label(title.toUpperCase());
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(4);
        int r = 0;
        for (Map.Entry<String, Double> entry : allDims.entrySet()) {
            boolean isPos = entry.getKey().startsWith("Position") || entry.getKey().startsWith("Rotation");
            if (isPos != positionOnly) continue;
            Label l = new Label(entry.getKey() + ":");
            l.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;");
            TextField tf = new TextField(String.format(Locale.US, "%.2f", entry.getValue()));
            tf.setPrefWidth(120);
            fieldMap.put(entry.getKey(), tf);
            grid.add(l, 0, r);
            grid.add(tf, 1, r++);
        }
        sec.getChildren().addAll(lbl, grid);
        return sec;
    }

    private void setupSyncListeners() {
        TextField rField = fieldMap.get("Radius"), dField = fieldMap.get("Diameter");
        if (rField != null && dField != null) {
            rField.textProperty().addListener((o, ov, nv) -> syncDim(nv, dField, 2.0));
            dField.textProperty().addListener((o, ov, nv) -> syncDim(nv, rField, 0.5));
        }
        TextField brField = fieldMap.get("Base Radius"), bdField = fieldMap.get("Base Diameter");
        if (brField != null && bdField != null) {
            brField.textProperty().addListener((o, ov, nv) -> syncDim(nv, bdField, 2.0));
            bdField.textProperty().addListener((o, ov, nv) -> syncDim(nv, brField, 0.5));
        }
        if (item.getType() == basic_shapes_ui_main.EQUILATERAL_TRIANGLE) {
            TextField side = fieldMap.get("Side Length"), h = fieldMap.get("Height");
            if (h != null) h.setEditable(false);
            if (side != null && h != null) {
                side.textProperty().addListener((o, ov, nv) -> syncDim(nv, h, Math.sqrt(3.0) / 2.0));
            }
        }
    }

    private void syncDim(String valStr, TextField target, double factor) {
        if (syncLock) return;
        try {
            double v = Double.parseDouble(valStr.trim());
            syncLock = true;
            target.setText(String.format(Locale.US, "%.2f", v * factor));
        } catch (Exception ignored) {
        } finally { syncLock = false; }
    }

    private void applyEdits() {
        Map<String, Double> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, TextField> entry : fieldMap.entrySet()) {
            String key = entry.getKey();
            TextField tf = entry.getValue();
            try {
                double val = Double.parseDouble(tf.getText().trim());
                if (Double.isNaN(val) || Double.isInfinite(val)) throw new IllegalArgumentException();
                if (!key.startsWith("Position") && !key.startsWith("Rotation") && val <= 0.0) {
                    showError(key + " must be greater than zero.");
                    tf.setStyle("-fx-border-color: #DC2626;");
                    return;
                }
                tf.setStyle("");
                parsed.put(key, val);
            } catch (Exception ex) {
                showError("Invalid numeric value for " + key);
                tf.setStyle("-fx-border-color: #DC2626;");
                return;
            }
        }
        String newName = nameField.getText().trim();
        if (!newName.isEmpty()) item.setName(newName);

        if (editor != null) editor.recordSnapshot();
        shape_dimension_helper_ui_main.applyDimensions(item, parsed);
        if (editor != null) {
            editor.selectShape(item);
            editor.notifyShapesChanged();
        }
        close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
