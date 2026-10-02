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
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.Locale;

public class extrude_editor_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final extrude_feature_ui_main extrude;
    private final shape_editor_ui_main editor;

    private final TextField nameField = new TextField();
    private final ComboBox<CutoutShape> shapeCombo = new ComboBox<>();
    private final TextField diaField = new TextField();
    private final TextField w2Field = new TextField();
    private final TextField heightField = new TextField();
    private final TextField uField = new TextField();
    private final TextField vField = new TextField();
    private final Label errorLabel = new Label();
    private final HBox w2Row;

    public static void open(shape_item_ui_main s, extrude_feature_ui_main ext, shape_editor_ui_main ed, Window w) {
        if (s != null && ext != null) new extrude_editor_dialog_ui_main(s, ext, ed, w).show();
    }

    public extrude_editor_dialog_ui_main(shape_item_ui_main s, extrude_feature_ui_main ext, shape_editor_ui_main ed, Window w) {
        this.shape = s; this.extrude = ext; this.editor = ed;
        if (w != null) initOwner(w);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Edit Extrude — " + ext.getName());

        VBox root = new VBox(8); root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        Label hdr = new Label(String.format("Shape: %s | Face: %s", s.getName(), ext.getFaceKind().getLabel()));
        hdr.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #1E293B;");
        root.getChildren().add(hdr);

        nameField.setText(ext.getName());
        shapeCombo.getItems().addAll(CutoutShape.CIRCLE, CutoutShape.SQUARE, CutoutShape.RECTANGLE);
        shapeCombo.setValue(ext.getProfileShape());
        shapeCombo.setPrefWidth(190);

        diaField.setText(String.format(Locale.US, "%.2f", ext.getDiameter()));
        w2Field.setText(String.format(Locale.US, "%.2f", ext.getWidth2() > 0.1 ? ext.getWidth2() : ext.getDiameter() * 0.6));
        heightField.setText(String.format(Locale.US, "%.2f", ext.getHeight()));
        uField.setText(String.format(Locale.US, "%.2f", ext.getU()));
        vField.setText(String.format(Locale.US, "%.2f", ext.getV()));

        root.getChildren().add(row("Name:", nameField));
        root.getChildren().add(rowNode("Profile:", shapeCombo));
        root.getChildren().add(row("Size / Dia (mm):", diaField));
        w2Row = row("Width 2 (mm):", w2Field);
        boolean isRect = (ext.getProfileShape() == CutoutShape.RECTANGLE);
        w2Row.setVisible(isRect); w2Row.setManaged(isRect);
        root.getChildren().add(w2Row);

        shapeCombo.setOnAction(e -> {
            boolean r = (shapeCombo.getValue() == CutoutShape.RECTANGLE);
            w2Row.setVisible(r); w2Row.setManaged(r);
        });

        root.getChildren().add(row("Height (mm):", heightField));
        root.getChildren().add(row("Pos U (mm):", uField));
        root.getChildren().add(row("Pos V (mm):", vField));

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 10px;");
        errorLabel.setWrapText(true);
        root.getChildren().add(errorLabel);

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle(btnStyle(framework_ui_main.OBJECT_SELECTED_COLOR));
        applyBtn.setDefaultButton(true);
        applyBtn.setOnAction(e -> onApply());

        Button delBtn = new Button("Delete Extrude");
        delBtn.setStyle(btnStyle("#DC2626"));
        delBtn.setOnAction(e -> onDelete());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(btnStyle("#64748B"));
        cancelBtn.setOnAction(e -> close());

        HBox actions = new HBox(8, delBtn, new Region(), cancelBtn, applyBtn);
        HBox.setHgrow(actions.getChildren().get(1), Priority.ALWAYS);
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

            extrude.setName(nameField.getText().trim());
            extrude.setProfileShape(shapeCombo.getValue());
            extrude.setDiameter(dia);
            extrude.setWidth2(w2);
            extrude.setHeight(h);
            extrude.setU(u);
            extrude.setV(v);

            shape.rebuild();
            if (editor != null) {
                editor.notifyShapesChanged();
                editor.selectExtrude(shape, extrude);
            }
            close();
        } catch (Exception ex) {
            errorLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void onDelete() {
        shape.removeExtrude(extrude.getId());
        if (editor != null) {
            editor.selectExtrude(shape, null);
            editor.notifyShapesChanged();
        }
        close();
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

    private String btnStyle(String bg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 3; -fx-cursor: hand;";
    }
}
