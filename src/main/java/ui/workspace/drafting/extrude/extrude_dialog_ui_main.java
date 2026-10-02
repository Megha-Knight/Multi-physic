package ui.workspace.drafting.extrude;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.framework_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.Locale;

public class extrude_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final face_reference_ui_main face;
    private final shape_editor_ui_main editor;

    private final TextField nameField = new TextField("Boss 01");
    private final ComboBox<CutoutShape> shapeCombo = new ComboBox<>();
    private final TextField diaField = new TextField("20.00");
    private final TextField w2Field = new TextField("15.00");
    private final TextField heightField = new TextField("25.00");
    private final TextField uField = new TextField("0.00");
    private final TextField vField = new TextField("0.00");
    private final Label errorLabel = new Label();
    private final HBox w2Row;

    public static void open(shape_item_ui_main s, face_reference_ui_main f, shape_editor_ui_main ed, Window w) {
        if (s != null && f != null) new extrude_dialog_ui_main(s, f, ed, w).show();
    }

    public extrude_dialog_ui_main(shape_item_ui_main s, face_reference_ui_main f, shape_editor_ui_main ed, Window w) {
        this.shape = s; this.face = f; this.editor = ed;
        if (w != null) initOwner(w);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Extrude 3D Feature — " + s.getName());

        VBox root = new VBox(8); root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        Label hdr = new Label(String.format("Placement: %s Face on %s", f.getFaceKind().getLabel(), s.getName()));
        hdr.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #1E293B;");
        root.getChildren().add(hdr);

        shapeCombo.getItems().addAll(CutoutShape.CIRCLE, CutoutShape.SQUARE, CutoutShape.RECTANGLE);
        shapeCombo.setValue(CutoutShape.CIRCLE);
        shapeCombo.setPrefWidth(190);

        root.getChildren().add(row("Name:", nameField));
        root.getChildren().add(rowNode("Profile:", shapeCombo));
        root.getChildren().add(row("Size / Dia (mm):", diaField));
        w2Row = row("Width 2 (mm):", w2Field);
        w2Row.setVisible(false); w2Row.setManaged(false);
        root.getChildren().add(w2Row);

        shapeCombo.setOnAction(e -> {
            boolean isRect = (shapeCombo.getValue() == CutoutShape.RECTANGLE);
            w2Row.setVisible(isRect); w2Row.setManaged(isRect);
        });

        root.getChildren().add(row("Height (mm):", heightField));
        root.getChildren().add(row("Pos U (mm):", uField));
        root.getChildren().add(row("Pos V (mm):", vField));

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 10px;");
        errorLabel.setWrapText(true);
        root.getChildren().add(errorLabel);

        Button applyBtn = new Button("Extrude");
        applyBtn.setStyle("-fx-background-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 12; -fx-background-radius: 3; -fx-cursor: hand;");
        applyBtn.setDefaultButton(true);
        applyBtn.setOnAction(e -> onApply());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #64748B; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 3; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        HBox actions = new HBox(8, new Region(), cancelBtn, applyBtn);
        HBox.setHgrow(actions.getChildren().get(0), Priority.ALWAYS);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(actions);

        setScene(new Scene(root, 320, 360));
        setResizable(false);
    }

    private void onApply() {
        try {
            double dia = Double.parseDouble(diaField.getText().trim());
            double h = Double.parseDouble(heightField.getText().trim());
            double u = Double.parseDouble(uField.getText().trim()), v = Double.parseDouble(vField.getText().trim());
            double w2 = (shapeCombo.getValue() == CutoutShape.RECTANGLE) ? Double.parseDouble(w2Field.getText().trim()) : 0.0;
            if (dia < 0.1 || h < 0.1) { errorLabel.setText("Size and Height must be >= 0.1 mm."); return; }

            String name = nameField.getText().trim();
            if (name.isBlank()) name = "Boss " + (shape.getExtrusions().size() + 1);

            extrude_feature_ui_main ext = new extrude_feature_ui_main(
                null, shape.getId(), name, face.getFaceKind(), shapeCombo.getValue(), u, v, dia, w2, h
            );
            shape.addExtrude(ext);
            if (editor != null) {
                editor.notifyShapesChanged();
                editor.selectExtrude(shape, ext);
            }
            close();
        } catch (Exception ex) {
            errorLabel.setText("Error: " + ex.getMessage());
        }
    }

    private HBox row(String lbl, TextField tf) {
        Label l = new Label(lbl); l.setPrefWidth(95); l.setStyle("-fx-font-size: 11px;");
        tf.setPrefWidth(190); tf.setStyle("-fx-font-size: 11px; -fx-padding: 3; -fx-border-color: #CBD5E1; -fx-border-radius: 2;");
        HBox h = new HBox(6, l, tf); h.setAlignment(Pos.CENTER_LEFT); return h;
    }

    private HBox rowNode(String lbl, javafx.scene.Node n) {
        Label l = new Label(lbl); l.setPrefWidth(95); l.setStyle("-fx-font-size: 11px;");
        HBox h = new HBox(6, l, n); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
}
