package ui.workspace.drafting;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import ui.workspace.camera.camera_controller_ui_main;
import ui.workspace.shapes.basic_shapes_ui_main;
import ui.workspace.drafting.gizmo.axis_drag_controller_ui_main;
import ui.workspace.drafting.faces.*;
import ui.workspace.drafting.dimensions.dimension_editor_dialog_ui_main;
import ui.workspace.drafting.holes.*;
import ui.workspace.drafting.extrude.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class shape_editor_ui_main {

    private final List<shape_item_ui_main> shapes = new ArrayList<>();
    private final shape_history_ui_main history = new shape_history_ui_main();
    private shape_item_ui_main selectedShape = null, selectedHoleShape = null, selectedPatternShape = null;
    private hole_feature_ui_main selectedHole = null;
    private extrude_feature_ui_main selectedExtrude = null;
    private hole_pattern_ui_main selectedPattern = null;
    private ui.workspace.drafting.topology.topology_derived_face_ui_main selectedDerivedFace = null, hoveredDerivedFace = null;
    private face_reference_ui_main hoveredFace = null, activeFace = null;

    private final Group container;
    private Consumer<String> statusCallback;
    private final axis_drag_controller_ui_main axisDrag;
    private ui.workspace.drafting.topology.topology_edge_ui_main hoveredEdge = null;
    private ui.workspace.drafting.machining.machining_dialog_ui_main.Kind edgePickKind = null;
    private Runnable onShapesChanged;
    private Consumer<shape_item_ui_main> onSelectionChanged;
    private BiConsumer<shape_item_ui_main, hole_feature_ui_main> onHoleSelectionChanged;
    private BiConsumer<shape_item_ui_main, extrude_feature_ui_main> onExtrudeSelectionChanged;
    private BiConsumer<shape_item_ui_main, hole_pattern_ui_main> onPatternSelectionChanged;
    private BiConsumer<shape_item_ui_main, ui.workspace.drafting.topology.topology_derived_face_ui_main> onDerivedFaceSelectionChanged;
    private final shape_editor_feature_selection_ui_main featureSel = new shape_editor_feature_selection_ui_main(this);

    public shape_editor_ui_main(Group container, Pane viewport, SubScene subScene, camera_controller_ui_main camCtrl,
                                Function<MouseEvent, Point3D> raycaster, Label hudLabel, BooleanSupplier isDrawingActive, PerspectiveCamera camera) {
        this.container = container;
        axisDrag = new axis_drag_controller_ui_main(viewport, camera, camCtrl, container);
        axisDrag.setStatusCallback(s -> { if (statusCallback != null) statusCallback.accept(s); });
        new shape_event_handler_ui_main(this, viewport, subScene, camCtrl, raycaster, hudLabel, isDrawingActive, axisDrag, camera, container);
        viewport_color_state_ui_main.addListener(() -> shape_editor_visual_helper_ui_main.refreshViewportColors(shapes));
    }

    public void postStatus(String s) { if (statusCallback != null && s != null) statusCallback.accept(s); }
    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; } public void setOnShapesChanged(Runnable r) { this.onShapesChanged = r; }
    public void setOnSelectionChanged(Consumer<shape_item_ui_main> c) { this.onSelectionChanged = c; } public void setOnHoleSelectionChanged(BiConsumer<shape_item_ui_main, hole_feature_ui_main> c) { this.onHoleSelectionChanged = c; }
    public void setOnExtrudeSelectionChanged(BiConsumer<shape_item_ui_main, extrude_feature_ui_main> c) { this.onExtrudeSelectionChanged = c; } public void setOnPatternSelectionChanged(BiConsumer<shape_item_ui_main, hole_pattern_ui_main> c) { this.onPatternSelectionChanged = c; }
    public void setOnDerivedFaceSelectionChanged(BiConsumer<shape_item_ui_main, ui.workspace.drafting.topology.topology_derived_face_ui_main> c) { this.onDerivedFaceSelectionChanged = c; }

    public void notifyShapesChanged() {
        if (activeFace != null && selectedShape != null) updateActiveFace();
        if (selectedHole != null && selectedHoleShape != null) selectedHoleShape.getHoleOverlay().highlightHole(selectedHoleShape, selectedHole);
        if (onShapesChanged != null) onShapesChanged.run();
    }

    public void recordSnapshot() { history.pushSnapshot(shapes); }

    public String generateNextName(basic_shapes_ui_main type) {
        String pfx = "untitled_" + type.name().toLowerCase(); int max = 0;
        for (shape_item_ui_main s : shapes) if (s.getName() != null && s.getName().startsWith(pfx)) try { max = Math.max(max, Integer.parseInt(s.getName().substring(pfx.length()))); } catch (Exception ignored) {}
        return pfx + String.format(Locale.US, "%02d", max + 1);
    }

    public void addShape(shape_item_ui_main item) {
        if (item.getName() == null || item.getName().isBlank()) item.setName(generateNextName(item.getType()));
        history.pushSnapshot(shapes); shapes.add(item); container.getChildren().add(item.getRootGroup()); selectShape(item); notifyShapesChanged();
    }
    public void removeShape(shape_item_ui_main item) {
        if (item == null) return;
        if (activeFace != null && activeFace.getOwnerShapeId().equals(item.getId())) setActiveFace(null);
        if (selectedHoleShape == item) selectHole(null, null); if (selectedShape == item) selectShape(null);
        shapes.remove(item); container.getChildren().remove(item.getRootGroup()); notifyShapesChanged();
    }
    public void deleteSelected() { if (selectedShape != null) { history.pushSnapshot(shapes); String name = selectedShape.getName(); removeShape(selectedShape); if (statusCallback != null) statusCallback.accept("Deleted " + name); } }
    public void copySelected() { if (selectedShape != null) { history.copy(selectedShape); if (statusCallback != null) statusCallback.accept("Copied " + selectedShape.getName()); } } public void cutSelected() { if (selectedShape != null) { history.copy(selectedShape); deleteSelected(); } }
    public void paste() { if (!history.hasClipboard()) return; history.pushSnapshot(shapes); shape_item_ui_main item = history.paste(15.0); if (item != null) { item.setName(generateNextName(item.getType())); shapes.add(item); container.getChildren().add(item.getRootGroup()); selectShape(item); notifyShapesChanged(); if (statusCallback != null) statusCallback.accept("Pasted " + item.getName()); } }
    public void undo() { List<shape_item_ui_main> p = history.undo(shapes); if (p != null) { loadShapes(p); if (statusCallback != null) statusCallback.accept("Undo"); } } public void redo() { List<shape_item_ui_main> n = history.redo(shapes); if (n != null) { loadShapes(n); if (statusCallback != null) statusCallback.accept("Redo"); } }
    public boolean canUndo() { return history.canUndo(); } public boolean canRedo() { return history.canRedo(); }
    public void loadShapes(List<shape_item_ui_main> newShapes) {
        clearHover(); clearEdgeHover(); selectExtrude(null, null); selectHole(null, null); selectPattern(null, null); selectShape(null); setActiveFace(null); selectDerivedFace(null, null); featureSel.clearAll();
        container.getChildren().clear(); shapes.clear(); container.getChildren().add(axisDrag.getGizmo());
        if (newShapes != null) for (shape_item_ui_main s : newShapes) { if (s.getName() == null || s.getName().isBlank()) s.setName(generateNextName(s.getType())); shapes.add(s); container.getChildren().add(s.getRootGroup()); }
        shape_editor_visual_helper_ui_main.refreshViewportColors(shapes); notifyShapesChanged();
    }

    public void openDimensionEditor(shape_item_ui_main s) { if (s != null) dimension_editor_dialog_ui_main.open(s, this, editorWindow()); } public void openHoleEditor(shape_item_ui_main s, hole_feature_ui_main h) { if (s != null && h != null) hole_editor_dialog_ui_main.open(s, h, this, editorWindow()); } public void openExtrudeEditor(shape_item_ui_main s, extrude_feature_ui_main e) { if (s != null && e != null) extrude_editor_dialog_ui_main.open(s, e, this, editorWindow()); } public void openPatternEditor(shape_item_ui_main s, hole_pattern_ui_main p) { if (s != null && p != null) pattern_editor_dialog_ui_main.open(s, p, this, editorWindow()); }
    private javafx.stage.Window editorWindow() { return container.getScene() != null ? container.getScene().getWindow() : null; }
    public void openFilletEditor(shape_item_ui_main s, ui.workspace.drafting.machining.fillet_feature_ui_main f) { if (s != null && f != null) ui.workspace.drafting.machining.machining_dialog_ui_main.openEdit(s, f, this, editorWindow()); } public void openChamferEditor(shape_item_ui_main s, ui.workspace.drafting.machining.chamfer_feature_ui_main c) { if (s != null && c != null) ui.workspace.drafting.machining.machining_dialog_ui_main.openEdit(s, c, this, editorWindow()); } public void openDraftEditor(shape_item_ui_main s, ui.workspace.drafting.machining.draft_feature_ui_main d) { if (s != null && d != null) ui.workspace.drafting.machining.machining_dialog_ui_main.openEdit(s, d, this, editorWindow()); }
    public ui.workspace.drafting.sketch.sketch_feature_ui_main getSelectedSketch() { return featureSel.getSelectedSketch(); } public ui.workspace.drafting.machining.fillet_feature_ui_main getSelectedFillet() { return featureSel.getSelectedFillet(); }
    public ui.workspace.drafting.machining.chamfer_feature_ui_main getSelectedChamfer() { return featureSel.getSelectedChamfer(); } public ui.workspace.drafting.machining.draft_feature_ui_main getSelectedDraft() { return featureSel.getSelectedDraft(); }
    public void selectSketch(shape_item_ui_main s, ui.workspace.drafting.sketch.sketch_feature_ui_main sk) { featureSel.selectSketch(s, sk); } public void selectFillet(shape_item_ui_main s, ui.workspace.drafting.machining.fillet_feature_ui_main f) { featureSel.selectFillet(s, f); }
    public void selectChamfer(shape_item_ui_main s, ui.workspace.drafting.machining.chamfer_feature_ui_main c) { featureSel.selectChamfer(s, c); } public void selectDraft(shape_item_ui_main s, ui.workspace.drafting.machining.draft_feature_ui_main d) { featureSel.selectDraft(s, d); }
    public void setOnSketchSelectionChanged(BiConsumer<shape_item_ui_main, ui.workspace.drafting.sketch.sketch_feature_ui_main> c) { featureSel.setOnSketchChanged(c); } public void setOnFilletSelectionChanged(BiConsumer<shape_item_ui_main, ui.workspace.drafting.machining.fillet_feature_ui_main> c) { featureSel.setOnFilletChanged(c); }
    public void setOnChamferSelectionChanged(BiConsumer<shape_item_ui_main, ui.workspace.drafting.machining.chamfer_feature_ui_main> c) { featureSel.setOnChamferChanged(c); } public void setOnDraftSelectionChanged(BiConsumer<shape_item_ui_main, ui.workspace.drafting.machining.draft_feature_ui_main> c) { featureSel.setOnDraftChanged(c); }
    public void clearHistory() { history.clear(); } public List<shape_item_ui_main> getShapes() { return new ArrayList<>(shapes); } public shape_item_ui_main getSelectedShape()  { return selectedShape; } public axis_drag_controller_ui_main getAxisDrag() { return axisDrag; }
    public face_reference_ui_main getActiveFace() { return activeFace; } public hole_feature_ui_main getSelectedHole() { return selectedHole; } public shape_item_ui_main getSelectedHoleShape() { return selectedHoleShape; } public extrude_feature_ui_main getSelectedExtrude() { return selectedExtrude; } public hole_pattern_ui_main getSelectedPattern() { return selectedPattern; } public shape_item_ui_main getSelectedPatternShape() { return selectedPatternShape; }
    public ui.workspace.drafting.topology.topology_derived_face_ui_main getSelectedDerivedFace() { return selectedDerivedFace; } public String getSelectedDerivedFaceId() { return selectedDerivedFace != null ? selectedDerivedFace.getId() : null; } public face_reference_ui_main getHoveredFace() { return hoveredFace; } public ui.workspace.drafting.topology.topology_derived_face_ui_main getHoveredDerivedFace() { return hoveredDerivedFace; }
    public void setHoveredFace(face_reference_ui_main face) {
        if (this.hoveredFace == face) return;
        if (this.hoveredFace != null) { shape_item_ui_main oldH = findShapeById(this.hoveredFace.getOwnerShapeId()); if (oldH != null) oldH.getFaceOverlay().clearHover(); }
        this.hoveredFace = face;
        if (face != null) { shape_item_ui_main s = findShapeById(face.getOwnerShapeId()); if (s != null) s.getFaceOverlay().setHoveredFace(s, face); }
    }
    public void setHoveredDerivedFace(shape_item_ui_main shape, ui.workspace.drafting.topology.topology_derived_face_ui_main df) {
        if (this.hoveredDerivedFace == df) return;
        if (this.hoveredDerivedFace != null) { this.hoveredDerivedFace.setHovered(false); shape_editor_visual_helper_ui_main.updateDerivedFaceVisual(this.selectedShape, this.hoveredDerivedFace); }
        this.hoveredDerivedFace = df;
        if (this.hoveredDerivedFace != null) { this.hoveredDerivedFace.setHovered(true); shape_editor_visual_helper_ui_main.updateDerivedFaceVisual(shape, this.hoveredDerivedFace); }
    }
    public void clearHover() { setHoveredFace(null); setHoveredDerivedFace(null, null); for (shape_item_ui_main s : shapes) if (s != null) { s.getFaceOverlay().clearHover(); s.getEdgeOverlay().clearHighlight(); } clearEdgeHover(); }
    public void enterEdgePickMode(ui.workspace.drafting.machining.machining_dialog_ui_main.Kind k) { this.edgePickKind = k; if (statusCallback != null) statusCallback.accept("Edge Pick: Click edge to " + (k != null ? k.name().toLowerCase() : "modify") + " (Esc to cancel)"); }
    public void exitEdgePickMode() { this.edgePickKind = null; clearEdgeHover(); if (statusCallback != null) statusCallback.accept("Exited edge pick mode."); }
    public boolean isEdgePickMode() { return edgePickKind != null; } public ui.workspace.drafting.machining.machining_dialog_ui_main.Kind getEdgePickKind() { return edgePickKind; } public ui.workspace.drafting.topology.topology_edge_ui_main getHoveredEdge() { return hoveredEdge; } public Group getContainer() { return container; }
    public void setHoveredEdge(ui.workspace.drafting.topology.topology_edge_ui_main edge) { if (this.hoveredEdge == edge) return; this.hoveredEdge = edge; if (selectedShape != null) selectedShape.getEdgeOverlay().highlightEdge(selectedShape, edge); if (edge != null && statusCallback != null) statusCallback.accept("Edge: " + edge.getName()); }
    public void clearEdgeHover() { setHoveredEdge(null); if (selectedShape != null) selectedShape.getEdgeOverlay().clearHighlight(); }

    public void selectDerivedFace(shape_item_ui_main shape, ui.workspace.drafting.topology.topology_derived_face_ui_main df) {
        if (this.selectedDerivedFace != null && this.selectedDerivedFace != df) { this.selectedDerivedFace.setSelected(false); shape_editor_visual_helper_ui_main.updateDerivedFaceVisual(this.selectedShape, this.selectedDerivedFace); }
        if (this.selectedShape != null) this.selectedShape.getFaceOverlay().highlightDerivedFace(null, null);
        this.selectedDerivedFace = df;
        if (df != null && shape != null) { this.selectedShape = shape; df.setSelected(true); shape_editor_visual_helper_ui_main.updateDerivedFaceVisual(shape, df); shape.getFaceOverlay().highlightDerivedFace(shape, df); }
        if (onDerivedFaceSelectionChanged != null) onDerivedFaceSelectionChanged.accept(shape, df);
    }

    public void setActiveFace(face_reference_ui_main face) {
        if (this.selectedDerivedFace != null) { this.selectedDerivedFace.setSelected(false); shape_editor_visual_helper_ui_main.updateDerivedFaceVisual(selectedShape, selectedDerivedFace); this.selectedDerivedFace = null; }
        if (this.activeFace != null && (face == null || !this.activeFace.getOwnerShapeId().equals(face.getOwnerShapeId()))) {
            shape_item_ui_main oldOwner = findShapeById(this.activeFace.getOwnerShapeId());
            if (oldOwner != null) oldOwner.getFaceOverlay().highlightFace(null, null);
        }
        this.activeFace = face;
        if (face != null) {
            shape_item_ui_main owner = (selectedShape != null && selectedShape.getId().equals(face.getOwnerShapeId())) ? selectedShape : findShapeById(face.getOwnerShapeId());
            if (owner != null) owner.getFaceOverlay().highlightFace(owner, face);
            if (statusCallback != null) statusCallback.accept(face.getFaceKind().getLabel() + " on " + (owner != null ? owner.getName() : "") + (face.isPlanar() ? " (Planar)" : " (Curved)"));
        } else if (selectedShape != null) selectedShape.getFaceOverlay().highlightFace(null, null);
    }

    public void updateActiveFace() { if (activeFace != null && selectedShape != null) { activeFace = face_picker_ui_main.refreshFace(activeFace, selectedShape); selectedShape.getFaceOverlay().highlightFace(selectedShape, activeFace); } }

    public void selectShape(shape_item_ui_main item) {
        if (selectedShape == item) { if (activeFace != null) updateActiveFace(); return; }
        featureSel.clearAll();
        if (selectedHoleShape != item) selectHole(null, null); if (selectedPatternShape != item) selectPattern(null, null);
        setActiveFace(null); selectDerivedFace(null, null);
        if (selectedShape != null) { selectedShape.setSelectedExtrudeId(null); selectedShape.setSelected(false); }
        selectedShape = item;
        if (selectedShape != null) { selectedShape.setSelected(true); axisDrag.attachTo(selectedShape); if (statusCallback != null) statusCallback.accept(selectedShape.getName() + " selected (" + selectedShape.getType().getLabel() + ")"); } else axisDrag.detach();
        if (onSelectionChanged != null) onSelectionChanged.accept(selectedShape);
    }

    public void selectHole(shape_item_ui_main shape, hole_feature_ui_main hole) {
        if (selectedHoleShape != null && selectedHoleShape != shape) selectedHoleShape.getHoleOverlay().highlightHole(null, null);
        this.selectedHoleShape = shape; this.selectedHole = hole;
        if (hole != null) { if (selectedExtrude != null) selectExtrude(shape, null); if (selectedPattern != null) selectPattern(shape, null); }
        if (shape != null && selectedShape != shape) selectShape(shape);
        if (shape != null) shape.getHoleOverlay().highlightHole(shape, hole);
        if (hole != null && statusCallback != null) statusCallback.accept(hole.getName() + " selected (" + hole.getFaceKind().getLabel() + ", Dia: " + hole.getDiameter() + " mm)");
        if (onHoleSelectionChanged != null) onHoleSelectionChanged.accept(shape, hole);
    }

    public void selectExtrude(shape_item_ui_main shape, extrude_feature_ui_main ext) {
        this.selectedExtrude = ext; if (shape != null && selectedShape != shape) selectShape(shape); if (shape != null) shape.setSelectedExtrudeId(ext != null ? ext.getId() : null);
        if (ext != null) { if (selectedHole != null) selectHole(shape, null); if (selectedPattern != null) selectPattern(shape, null); if (statusCallback != null) statusCallback.accept(ext.getName() + " selected (" + ext.getFaceKind().getLabel() + ")"); }
        if (onExtrudeSelectionChanged != null) onExtrudeSelectionChanged.accept(shape, ext); if (onShapesChanged != null) onShapesChanged.run();
    }

    public void selectPattern(shape_item_ui_main shape, hole_pattern_ui_main pat) {
        this.selectedPatternShape = shape; this.selectedPattern = pat; if (shape != null && selectedShape != shape) selectShape(shape);
        if (pat != null) { if (selectedHole != null) selectHole(shape, null); if (selectedExtrude != null) selectExtrude(shape, null); if (statusCallback != null) statusCallback.accept(pat.getPatternType().getLabel() + " selected (" + pat.getInstanceCount() + " instances)"); }
        if (onPatternSelectionChanged != null) onPatternSelectionChanged.accept(shape, pat);
    }

    public void checkHoleHit(face_reference_ui_main face, shape_item_ui_main shape) {
        if (shape != null && face != null) { var res = ui.workspace.drafting.faces.face_selection_resolver_ui_main.resolveFace(shape, face.getFaceKind(), face.getLocalHitU(), face.getLocalHitV()); selectDerivedFace(shape, res.derivedFace()); }
        else selectDerivedFace(shape, null);
        var h = hole_overlay_ui_main.findHoleAt(shape, face); if (h != null) { selectHole(shape, h); return; }
        var hit = hole_overlay_ui_main.findPatternInstanceAt(shape, face); if (hit != null) selectPattern(shape, hit.pattern()); else selectHole(shape, null);
    }
    public shape_item_ui_main findShapeById(String id) { if (id == null) return null; for (shape_item_ui_main s : shapes) if (id.equals(s.getId())) return s; return null; }
    public shape_item_ui_main findShapeNear(Point3D pt, double t) { if (selectedShape != null && selectedShape.isNear(pt, t)) return selectedShape; for (int i = shapes.size() - 1; i >= 0; i--) if (shapes.get(i).isNear(pt, t)) return shapes.get(i); return null; }
    public shape_item_ui_main findShapeByNode(javafx.scene.Node node) { if (node != null) for (int i = shapes.size() - 1; i >= 0; i--) if (shapes.get(i).containsNode(node)) return shapes.get(i); return null; }
}
