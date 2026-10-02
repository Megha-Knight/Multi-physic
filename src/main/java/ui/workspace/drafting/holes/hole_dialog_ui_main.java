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
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main.HoleType;
import ui.workspace.drafting.holes.hole_feature_ui_main.CutoutShape;

/**
 * hole_dialog_ui_main.java
 * Modal dialog for specifying parametric Cutouts (Circle, Square, Rectangle, Triangles).
 */
public class hole_dialog_ui_main extends Stage {

    private final shape_item_ui_main shape;
    private final face_reference_ui_main face;
    private final shape_editor_ui_main editor;
    private final CutoutShape cutoutShape;

    private final ComboBox<HoleType> typeCombo = new ComboBox<>();
    private final TextField diaField = new TextField("10.00"), width2Field = new TextField("10.00");
    private final TextField csDiaField = new TextField("18.00"), csAngleField = new TextField("90.00");
    private final TextField cbDiaField = new TextField("16.00"), cbDepthField = new TextField("5.00");
    private final TextField depthField = new TextField("20.00"), uField = new TextField("0.00"), vField = new TextField("0.00");
    private final CheckBox throughCheck = new CheckBox("Through-All");
    private final Label errorLabel = new Label();
    private final HBox csDiaRow = new HBox(6), csAngleRow = new HBox(6), cbDiaRow = new HBox(6), cbDepthRow = new HBox(6), w2Row = new HBox(6);

    public static void open(shape_item_ui_main s, face_reference_ui_main f, shape_editor_ui_main ed, Window w) {
        open(s, f, ed, w, CutoutShape.CIRCLE);
    }

    public static void open(shape_item_ui_main s, face_reference_ui_main f, shape_editor_ui_main ed, Window w, CutoutShape cs) {
        if (s != null && f != null) new hole_dialog_ui_main(s, f, ed, w, cs).show();
    }

    public hole_dialog_ui_main(shape_item_ui_main s, face_reference_ui_main f, shape_editor_ui_main ed, Window w) {
        this(s, f, ed, w, CutoutShape.CIRCLE);
    }

    public hole_dialog_ui_main(shape_item_ui_main s, face_reference_ui_main f, shape_editor_ui_main ed, Window w, CutoutShape cs) {
        this.shape = s; this.face = f; this.editor = ed; this.cutoutShape = (cs != null) ? cs : CutoutShape.CIRCLE;
        if (w != null) initOwner(w);
        initModality(Modality.APPLICATION_MODAL);
        setTitle((cutoutShape == CutoutShape.CIRCLE ? "Parametric Hole" : cutoutShape.getLabel() + " Cutout") + " — " + s.getName());

        VBox root = new VBox(8); root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");

        Label hdr = new Label(String.format("Placement: %s on %s", f.getFaceKind().getLabel(), s.getName()));
        hdr.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #005A85;");
        root.getChildren().add(hdr);

        typeCombo.getItems().addAll(HoleType.SIMPLE, HoleType.COUNTERSINK, HoleType.COUNTERBORE);
        typeCombo.setValue(HoleType.SIMPLE);
        typeCombo.setOnAction(e -> updateTypeVisibility());
        throughCheck.setOnAction(e -> depthField.setDisable(throughCheck.isSelected()));

        setupRow(csDiaRow, "Countersink Dia (mm):", csDiaField); setupRow(csAngleRow, "Cone Angle (deg):", csAngleField);
        setupRow(cbDiaRow, "Counterbore Dia (mm):", cbDiaField); setupRow(cbDepthRow, "Counterbore Depth (mm):", cbDepthField);
        setupRow(w2Row, "Height (mm):", width2Field);

        String dimLbl = switch (cutoutShape) {
            case SQUARE, EQUILATERAL_TRIANGLE -> "Side Length (mm):";
            case RECTANGLE -> "Width (mm):";
            case RIGHT_TRIANGLE -> "Base (mm):";
            default -> "Bore Dia (mm):";
        };

        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(5);
        int r = 0;
        if (cutoutShape == CutoutShape.CIRCLE) {
            grid.add(createLabel("Hole Type:"), 0, r); grid.add(typeCombo, 1, r++);
        }
        grid.add(createLabel(dimLbl), 0, r); grid.add(diaField, 1, r++);
        if (cutoutShape == CutoutShape.RECTANGLE || cutoutShape == CutoutShape.RIGHT_TRIANGLE) grid.add(w2Row, 0, r++, 2, 1);
        if (cutoutShape == CutoutShape.CIRCLE) {
            grid.add(csDiaRow, 0, r++, 2, 1); grid.add(csAngleRow, 0, r++, 2, 1);
            grid.add(cbDiaRow, 0, r++, 2, 1); grid.add(cbDepthRow, 0, r++, 2, 1);
        }
        grid.add(createLabel("Depth (mm):"), 0, r); grid.add(depthField, 1, r++);
        grid.add(throughCheck, 1, r++);
        grid.add(createLabel("Position U (mm):"), 0, r); grid.add(uField, 1, r++);
        grid.add(createLabel("Position V (mm):"), 0, r); grid.add(vField, 1, r++);
        root.getChildren().add(grid);

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        errorLabel.setVisible(false); root.getChildren().add(errorLabel);

        Button btnCancel = new Button("Cancel"); btnCancel.setOnAction(e -> close());
        Button btnApply = new Button(cutoutShape == CutoutShape.CIRCLE ? "Create Hole" : "Create Cutout");
        btnApply.setDefaultButton(true);
        btnApply.setStyle("-fx-background-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold;");
        btnApply.setOnAction(e -> applyHole());

        HBox btnBox = new HBox(8, btnCancel, btnApply); btnBox.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(btnBox);
        updateTypeVisibility();
        setScene(new Scene(root, 330, 390));
    }

    private void setupRow(HBox row, String labelText, TextField tf) {
        Label lbl = createLabel(labelText); lbl.setPrefWidth(140); tf.setPrefWidth(120);
        row.getChildren().addAll(lbl, tf); row.setAlignment(Pos.CENTER_LEFT);
    }

    private void updateTypeVisibility() {
        if (cutoutShape != CutoutShape.CIRCLE) return;
        HoleType t = typeCombo.getValue();
        boolean isCs = (t == HoleType.COUNTERSINK), isCb = (t == HoleType.COUNTERBORE);
        csDiaRow.setVisible(isCs); csDiaRow.setManaged(isCs);
        csAngleRow.setVisible(isCs); csAngleRow.setManaged(isCs);
        cbDiaRow.setVisible(isCb); cbDiaRow.setManaged(isCb);
        cbDepthRow.setVisible(isCb); cbDepthRow.setManaged(isCb);
    }

    private Label createLabel(String t) {
        Label l = new Label(t); l.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;"); return l;
    }

    private void applyHole() {
        try {
            double dia = Double.parseDouble(diaField.getText().trim());
            double depth = throughCheck.isSelected() ? 1000.0 : Double.parseDouble(depthField.getText().trim());
            double u = Double.parseDouble(uField.getText().trim()), v = Double.parseDouble(vField.getText().trim());
            HoleType type = (cutoutShape == CutoutShape.CIRCLE) ? typeCombo.getValue() : HoleType.SIMPLE;
            double csDia = 0, csAngle = 90.0, cbDia = 0, cbDepth = 0, w2 = 0;

            if (type == HoleType.COUNTERSINK) {
                csDia = Double.parseDouble(csDiaField.getText().trim()); csAngle = Double.parseDouble(csAngleField.getText().trim());
            } else if (type == HoleType.COUNTERBORE) {
                cbDia = Double.parseDouble(cbDiaField.getText().trim()); cbDepth = Double.parseDouble(cbDepthField.getText().trim());
            }
            if (cutoutShape == CutoutShape.RECTANGLE || cutoutShape == CutoutShape.RIGHT_TRIANGLE) {
                w2 = Double.parseDouble(width2Field.getText().trim());
            }

            hole_feature_ui_main hole = new hole_feature_ui_main(
                java.util.UUID.randomUUID().toString(), shape.getId(), type, face.getFaceKind(),
                u, v, dia, depth, throughCheck.isSelected(), csDia, csAngle, cbDia, cbDepth, cutoutShape, w2
            );

            if (!hole.isValid()) { showError("Invalid parameters. Verify diameter, angles, and depths."); return; }
            boolean isCyl = (face.getFaceKind() == face_kind_ui_main.TOP_CAP || face.getFaceKind() == face_kind_ui_main.BOTTOM_CAP);
            boolean fits = isCyl ? hole.fitsWithinCylinderCap(face.getFaceWidth() * 0.5) : hole.fitsWithinFace(face.getFaceWidth(), face.getFaceHeight());
            if (!fits) { showError("Cutout boundary extends outside the selected face bounds."); return; }

            for (hole_feature_ui_main existing : shape.getHoles()) {
                if (existing.getFaceKind() == face.getFaceKind()) {
                    double dist = Math.hypot(u - existing.getU(), v - existing.getV());
                    if (dist < 0.5) {
                        if (Math.abs(dia - existing.getDiameter()) < 0.1 && Math.abs(depth - existing.getDepth()) < 0.1) {
                            showError("A feature with the same dimensions already exists here."); return;
                        }
                    } else if (dist < (hole.getOuterRadius() + existing.getOuterRadius()) - 0.5) {
                        showError("Feature overlaps with an existing off-center feature on this face."); return;
                    }
                }
            }

            if (editor != null) editor.recordSnapshot();
            shape.addHole(hole);
            if (editor != null) editor.notifyShapesChanged();
            close();
        } catch (NumberFormatException ex) {
            showError("Please enter valid numeric values.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg); errorLabel.setVisible(true);
    }
}
