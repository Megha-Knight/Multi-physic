package ui.workspace.drafting.machining;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.framework_ui_main;
import ui.workspace.drafting.faces.face_kind_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.topology.topology_body_ui_main;
import ui.workspace.drafting.topology.topology_edge_ui_main;

import java.util.Locale;

/**
 * Create / edit dialog for Fillet, Chamfer, and Draft machining features.
 */
public class machining_dialog_ui_main extends Stage {

    public enum Kind { FILLET, CHAMFER, DRAFT }

    private final shape_item_ui_main shape;
    private final shape_editor_ui_main editor;
    private final Kind kind;
    private final fillet_feature_ui_main editFillet;
    private final chamfer_feature_ui_main editChamfer;
    private final draft_feature_ui_main editDraft;

    private final ComboBox<String> targetCombo = new ComboBox<>();
    private final TextField valueField = new TextField();
    private final Label errorLabel = new Label();

    public static void openCreate(Kind k, shape_item_ui_main s, shape_editor_ui_main ed, Window w) {
        if (k != null && s != null) new machining_dialog_ui_main(k, s, ed, w, null, null, null).show();
    }
    public static void openCreateWithTarget(Kind k, shape_item_ui_main s, String target, shape_editor_ui_main ed, Window w) {
        if (k != null && s != null) {
            machining_dialog_ui_main dlg = new machining_dialog_ui_main(k, s, ed, w, null, null, null);
            if (target != null && !target.isBlank()) {
                if (!dlg.targetCombo.getItems().contains(target)) dlg.targetCombo.getItems().add(target);
                dlg.targetCombo.setValue(target);
            }
            dlg.show();
        }
    }
    public static void openEdit(shape_item_ui_main s, fillet_feature_ui_main f, shape_editor_ui_main ed, Window w) {
        if (s != null && f != null) new machining_dialog_ui_main(Kind.FILLET, s, ed, w, f, null, null).show();
    }
    public static void openEdit(shape_item_ui_main s, chamfer_feature_ui_main c, shape_editor_ui_main ed, Window w) {
        if (s != null && c != null) new machining_dialog_ui_main(Kind.CHAMFER, s, ed, w, null, c, null).show();
    }
    public static void openEdit(shape_item_ui_main s, draft_feature_ui_main d, shape_editor_ui_main ed, Window w) {
        if (s != null && d != null) new machining_dialog_ui_main(Kind.DRAFT, s, ed, w, null, null, d).show();
    }

    private machining_dialog_ui_main(Kind kind, shape_item_ui_main s, shape_editor_ui_main ed, Window w,
                                     fillet_feature_ui_main ef, chamfer_feature_ui_main ec, draft_feature_ui_main edr) {
        this.kind = kind; this.shape = s; this.editor = ed;
        this.editFillet = ef; this.editChamfer = ec; this.editDraft = edr;
        if (w != null) initOwner(w);
        initModality(Modality.APPLICATION_MODAL);
        boolean editing = ef != null || ec != null || edr != null;
        setTitle((editing ? "Edit " : "Create ") + kind.name().charAt(0) + kind.name().substring(1).toLowerCase()
            + " — " + s.getName());

        VBox root = new VBox(8); root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI', sans-serif;");
        Label hdr = new Label(kind == Kind.DRAFT
            ? "Draft tapers a planar side face. Select FRONT/BACK/LEFT/RIGHT."
            : (kind == Kind.FILLET ? "Fillet rounds a sharp edge." : "Chamfer bevels a sharp edge."));
        hdr.setStyle("-fx-font-size: 11px; -fx-text-fill: #005A85; -fx-wrap-text: true;");
        hdr.setMaxWidth(320);
        root.getChildren().add(hdr);

        populateTargets();
        String valueLabel = kind == Kind.DRAFT ? "Angle (deg):" : (kind == Kind.FILLET ? "Radius (mm):" : "Distance (mm):");
        if (ef != null) { targetCombo.setValue(ef.getSimpleEdgeName()); valueField.setText(fmt(ef.getRadius())); }
        else if (ec != null) { targetCombo.setValue(ec.getSimpleEdgeName()); valueField.setText(fmt(ec.getDistance())); }
        else if (edr != null) { targetCombo.setValue(edr.getFaceKind().name()); valueField.setText(fmt(edr.getDraftAngle())); }
        else valueField.setText(kind == Kind.DRAFT ? "5.00" : "3.00");

        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(6);
        grid.add(lbl(kind == Kind.DRAFT ? "Side face:" : "Edge:"), 0, 0); grid.add(targetCombo, 1, 0);
        grid.add(lbl(valueLabel), 0, 1); grid.add(valueField, 1, 1);
        targetCombo.setPrefWidth(180); valueField.setPrefWidth(180);
        root.getChildren().add(grid);

        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        errorLabel.setVisible(false); errorLabel.setWrapText(true); errorLabel.setMaxWidth(320);
        root.getChildren().add(errorLabel);

        Button cancel = new Button("Cancel"); cancel.setOnAction(e -> {
            shape.getFaceOverlay().highlightFace(null); shape.getEdgeOverlay().clearHighlight();
            if (editor != null) editor.setActiveFace(null); close();
        });
        Button apply = new Button(editing ? "Apply" : "Create"); apply.setDefaultButton(true);
        apply.setStyle("-fx-background-color: " + framework_ui_main.OBJECT_SELECTED_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold;");
        apply.setOnAction(e -> applyFeature());
        HBox btns = new HBox(8, cancel, apply); btns.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(btns);
        setScene(new Scene(root, 360, 210));
    }

    private void populateTargets() {
        topology_body_ui_main topo = shape.getTopology();
        if (kind == Kind.DRAFT) {
            for (face_kind_ui_main k : new face_kind_ui_main[]{face_kind_ui_main.FRONT, face_kind_ui_main.BACK, face_kind_ui_main.LEFT, face_kind_ui_main.RIGHT})
                if (topo != null && topo.getFace(k) != null) targetCombo.getItems().add(k.name());
        } else if (topo != null) {
            for (topology_edge_ui_main e : topo.getEdges())
                if (e.getLength() > 0.2) targetCombo.getItems().add(e.getName());
        }
        if (!targetCombo.getItems().isEmpty()) targetCombo.getSelectionModel().selectFirst();
    }

    private void applyFeature() {
        try {
            String tgt = targetCombo.getValue();
            if (tgt == null || tgt.isBlank()) { showError("Select a target first."); return; }
            double val = Double.parseDouble(valueField.getText().trim());
            if (editor != null) editor.recordSnapshot();
            boolean ok;
            if (kind == Kind.FILLET) {
                fillet_feature_ui_main f = editFillet != null ? editFillet
                    : new fillet_feature_ui_main(null, shape.getId(), tgt, val);
                f.setEdgeId(tgt); f.setRadius(val);
                ok = f.revalidate(shape);
                if (ok && editFillet == null) shape.addFillet(f); else if (ok) shape.rebuild();
                if (!ok) { showError(f.getDiagnosticMessage()); return; }
            } else if (kind == Kind.CHAMFER) {
                chamfer_feature_ui_main c = editChamfer != null ? editChamfer
                    : new chamfer_feature_ui_main(null, shape.getId(), tgt, val);
                c.setEdgeId(tgt); c.setDistance(val);
                ok = c.revalidate(shape);
                if (ok && editChamfer == null) shape.addChamfer(c); else if (ok) shape.rebuild();
                if (!ok) { showError(c.getDiagnosticMessage()); return; }
            } else {
                face_kind_ui_main fk = face_kind_ui_main.valueOf(tgt);
                draft_feature_ui_main d = editDraft != null ? editDraft
                    : new draft_feature_ui_main(null, shape.getId(), fk, val);
                d.setFaceKind(fk); d.setDraftAngle(val);
                ok = d.revalidate(shape);
                if (ok && editDraft == null) shape.addDraft(d); else if (ok) shape.rebuild();
                if (!ok) { showError(d.getDiagnosticMessage()); return; }
            }
            shape.getFaceOverlay().highlightFace(null);
            shape.getEdgeOverlay().clearHighlight();
            if (editor != null) { editor.setActiveFace(null); editor.notifyShapesChanged(); }
            close();
        } catch (NumberFormatException ex) {
            showError("Enter a valid number.");
        } catch (IllegalArgumentException ex) {
            showError("Invalid face selection.");
        }
    }

    private static Label lbl(String t) { Label l = new Label(t); l.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;"); return l; }
    private static String fmt(double v) { return String.format(Locale.US, "%.2f", v); }
    private void showError(String msg) { errorLabel.setText(msg != null ? msg : "Invalid."); errorLabel.setVisible(true); }
}
