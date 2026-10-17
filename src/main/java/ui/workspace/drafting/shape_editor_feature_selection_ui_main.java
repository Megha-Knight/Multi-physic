package ui.workspace.drafting;

import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.machining.chamfer_feature_ui_main;
import ui.workspace.drafting.machining.draft_feature_ui_main;
import ui.workspace.drafting.machining.fillet_feature_ui_main;
import ui.workspace.drafting.sketch.sketch_feature_ui_main;

import java.util.function.BiConsumer;

public class shape_editor_feature_selection_ui_main {

    private final shape_editor_ui_main editor;
    private sketch_feature_ui_main selectedSketch = null;
    private fillet_feature_ui_main selectedFillet = null;
    private chamfer_feature_ui_main selectedChamfer = null;
    private draft_feature_ui_main selectedDraft = null;

    private BiConsumer<shape_item_ui_main, sketch_feature_ui_main> onSketchChanged;
    private BiConsumer<shape_item_ui_main, fillet_feature_ui_main> onFilletChanged;
    private BiConsumer<shape_item_ui_main, chamfer_feature_ui_main> onChamferChanged;
    private BiConsumer<shape_item_ui_main, draft_feature_ui_main> onDraftChanged;

    public shape_editor_feature_selection_ui_main(shape_editor_ui_main editor) {
        this.editor = editor;
    }

    public sketch_feature_ui_main getSelectedSketch() { return selectedSketch; }
    public fillet_feature_ui_main getSelectedFillet() { return selectedFillet; }
    public chamfer_feature_ui_main getSelectedChamfer() { return selectedChamfer; }
    public draft_feature_ui_main getSelectedDraft() { return selectedDraft; }

    public void setOnSketchChanged(BiConsumer<shape_item_ui_main, sketch_feature_ui_main> c) { this.onSketchChanged = c; }
    public void setOnFilletChanged(BiConsumer<shape_item_ui_main, fillet_feature_ui_main> c) { this.onFilletChanged = c; }
    public void setOnChamferChanged(BiConsumer<shape_item_ui_main, chamfer_feature_ui_main> c) { this.onChamferChanged = c; }
    public void setOnDraftChanged(BiConsumer<shape_item_ui_main, draft_feature_ui_main> c) { this.onDraftChanged = c; }

    public void clearAll() {
        selectedSketch = null; selectedFillet = null; selectedChamfer = null; selectedDraft = null;
    }

    public void selectSketch(shape_item_ui_main shape, sketch_feature_ui_main sk) {
        this.selectedSketch = sk;
        if (sk != null) { selectedFillet = null; selectedChamfer = null; selectedDraft = null; }
        if (shape != null && editor != null && editor.getSelectedShape() != shape) editor.selectShape(shape);
        if (sk != null && editor != null) editor.postStatus(sk.getName() + " selected");
        if (onSketchChanged != null) onSketchChanged.accept(shape, sk);
    }

    public void selectFillet(shape_item_ui_main shape, fillet_feature_ui_main f) {
        this.selectedFillet = f;
        if (f != null) { selectedSketch = null; selectedChamfer = null; selectedDraft = null; }
        if (shape != null && editor != null && editor.getSelectedShape() != shape) editor.selectShape(shape);
        if (f != null && shape != null) {
            var topo = shape.getTopology();
            var edge = topo != null ? topo.getEdge(f.getSimpleEdgeName()) : null;
            if (edge != null) shape.getEdgeOverlay().highlightEdge(shape, edge);
            if (editor != null) editor.postStatus(f.getName() + " selected (R: " + f.getRadius() + " mm)");
        }
        if (onFilletChanged != null) onFilletChanged.accept(shape, f);
    }

    public void selectChamfer(shape_item_ui_main shape, chamfer_feature_ui_main c) {
        this.selectedChamfer = c;
        if (c != null) { selectedSketch = null; selectedFillet = null; selectedDraft = null; }
        if (shape != null && editor != null && editor.getSelectedShape() != shape) editor.selectShape(shape);
        if (c != null && shape != null) {
            var topo = shape.getTopology();
            var edge = topo != null ? topo.getEdge(c.getSimpleEdgeName()) : null;
            if (edge != null) shape.getEdgeOverlay().highlightEdge(shape, edge);
            if (editor != null) editor.postStatus(c.getName() + " selected (D: " + c.getDistance() + " mm)");
        }
        if (onChamferChanged != null) onChamferChanged.accept(shape, c);
    }

    public void selectDraft(shape_item_ui_main shape, draft_feature_ui_main d) {
        this.selectedDraft = d;
        if (d != null) { selectedSketch = null; selectedFillet = null; selectedChamfer = null; }
        if (shape != null && editor != null && editor.getSelectedShape() != shape) editor.selectShape(shape);
        if (d != null && shape != null) {
            var face = ui.workspace.drafting.topology.topology_geometry_helper_ui_main.reconstructFaceReference(shape, d.getFaceKind(), 0, 0);
            if (face != null) shape.getFaceOverlay().highlightFace(shape, face);
            if (editor != null) editor.postStatus(d.getName() + " selected (Angle: " + d.getDraftAngle() + "°)");
        }
        if (onDraftChanged != null) onDraftChanged.accept(shape, d);
    }
}
