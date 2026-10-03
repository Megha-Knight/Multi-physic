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
import ui.workspace.drafting.faces.face_overlay_ui_main;
import ui.workspace.drafting.faces.face_reference_ui_main;
import ui.workspace.drafting.faces.face_picker_ui_main;
import ui.workspace.drafting.dimensions.dimension_editor_dialog_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_overlay_ui_main;
import ui.workspace.drafting.holes.hole_editor_dialog_ui_main;
import ui.workspace.drafting.holes.hole_pattern_ui_main;
import ui.workspace.drafting.holes.pattern_editor_dialog_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;
import ui.workspace.drafting.extrude.extrude_editor_dialog_ui_main;

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
    private ui.workspace.drafting.topology.topology_derived_face_ui_main selectedDerivedFace = null;

    private final Group container;
    private Consumer<String> statusCallback;
    private final axis_drag_controller_ui_main axisDrag;
    private final face_overlay_ui_main faceOverlay = new face_overlay_ui_main();
    private final hole_overlay_ui_main holeOverlay = new hole_overlay_ui_main();
    private face_reference_ui_main activeFace = null;
    private Runnable onShapesChanged;
    private Consumer<shape_item_ui_main> onSelectionChanged;
    private BiConsumer<shape_item_ui_main, hole_feature_ui_main> onHoleSelectionChanged;
    private BiConsumer<shape_item_ui_main, extrude_feature_ui_main> onExtrudeSelectionChanged;
    private BiConsumer<shape_item_ui_main, hole_pattern_ui_main> onPatternSelectionChanged;

    public shape_editor_ui_main(Group container, Pane viewport, SubScene subScene, camera_controller_ui_main camCtrl,
                                Function<MouseEvent, Point3D> raycaster, Label hudLabel, BooleanSupplier isDrawingActive, PerspectiveCamera camera) {
        this.container = container;
        axisDrag = new axis_drag_controller_ui_main(viewport, camera, camCtrl, container);
        axisDrag.setStatusCallback(s -> { if (statusCallback != null) statusCallback.accept(s); });
        container.getChildren().addAll(faceOverlay, holeOverlay);
        new shape_event_handler_ui_main(this, viewport, subScene, camCtrl, raycaster, hudLabel, isDrawingActive, axisDrag, camera, container);
    }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; } public void setOnShapesChanged(Runnable r) { this.onShapesChanged = r; }
    public void setOnSelectionChanged(Consumer<shape_item_ui_main> c) { this.onSelectionChanged = c; } public void setOnHoleSelectionChanged(BiConsumer<shape_item_ui_main, hole_feature_ui_main> c) { this.onHoleSelectionChanged = c; }
    public void setOnExtrudeSelectionChanged(BiConsumer<shape_item_ui_main, extrude_feature_ui_main> c) { this.onExtrudeSelectionChanged = c; } public void setOnPatternSelectionChanged(BiConsumer<shape_item_ui_main, hole_pattern_ui_main> c) { this.onPatternSelectionChanged = c; }

    public void notifyShapesChanged() {
        if (activeFace != null && selectedShape != null) updateActiveFace();
        if (selectedHole != null && selectedHoleShape != null) holeOverlay.highlightHole(selectedHoleShape, selectedHole);
        if (onShapesChanged != null) onShapesChanged.run();
    }

    public void recordSnapshot() { history.pushSnapshot(shapes); }

    public String generateNextName(basic_shapes_ui_main type) {
        String pfx = "untitled_" + type.name().toLowerCase(); int max = 0;
        for (shape_item_ui_main s : shapes) {
            String n = s.getName();
            if (n != null && n.startsWith(pfx)) try { max = Math.max(max, Integer.parseInt(n.substring(pfx.length()))); } catch (Exception ignored) {}
        }
        return pfx + String.format(Locale.US, "%02d", max + 1);
    }

    public void addShape(shape_item_ui_main item) {
        if (item.getName() == null || item.getName().isBlank()) item.setName(generateNextName(item.getType()));
        history.pushSnapshot(shapes); shapes.add(item); container.getChildren().add(item.getRootGroup());
        selectShape(item); notifyShapesChanged();
    }

    public void removeShape(shape_item_ui_main item) {
        if (item == null) return;
        if (activeFace != null && activeFace.getOwnerShapeId().equals(item.getId())) setActiveFace(null);
        if (selectedHoleShape == item) selectHole(null, null);
        if (selectedShape == item) selectShape(null);
        shapes.remove(item); container.getChildren().remove(item.getRootGroup()); notifyShapesChanged();
    }

    public void deleteSelected() { if (selectedShape != null) { history.pushSnapshot(shapes); String name = selectedShape.getName(); removeShape(selectedShape); if (statusCallback != null) statusCallback.accept("Deleted " + name); } }
    public void copySelected() { if (selectedShape != null) { history.copy(selectedShape); if (statusCallback != null) statusCallback.accept("Copied " + selectedShape.getName()); } } public void cutSelected() { if (selectedShape != null) { history.copy(selectedShape); deleteSelected(); } }
    public void paste() {
        if (!history.hasClipboard()) return;
        history.pushSnapshot(shapes); shape_item_ui_main item = history.paste(15.0);
        if (item != null) { item.setName(generateNextName(item.getType())); shapes.add(item); container.getChildren().add(item.getRootGroup()); selectShape(item); notifyShapesChanged(); if (statusCallback != null) statusCallback.accept("Pasted " + item.getName()); }
    }
    public void undo() { List<shape_item_ui_main> p = history.undo(shapes); if (p != null) { loadShapes(p); if (statusCallback != null) statusCallback.accept("Undo"); } } public void redo() { List<shape_item_ui_main> n = history.redo(shapes); if (n != null) { loadShapes(n); if (statusCallback != null) statusCallback.accept("Redo"); } }
    public boolean canUndo() { return history.canUndo(); } public boolean canRedo() { return history.canRedo(); }
    public void loadShapes(List<shape_item_ui_main> newShapes) {
        selectExtrude(null, null); selectHole(null, null); selectPattern(null, null); selectShape(null); setActiveFace(null); container.getChildren().clear(); shapes.clear();
        container.getChildren().addAll(axisDrag.getGizmo(), faceOverlay, holeOverlay);
        if (newShapes != null) {
            for (shape_item_ui_main s : newShapes) {
                if (s.getName() == null || s.getName().isBlank()) s.setName(generateNextName(s.getType()));
                shapes.add(s); container.getChildren().add(s.getRootGroup());
            }
        }
        notifyShapesChanged();
    }

    public void openDimensionEditor(shape_item_ui_main s) { if (s != null) dimension_editor_dialog_ui_main.open(s, this, container.getScene() != null ? container.getScene().getWindow() : null); }
    public void openHoleEditor(shape_item_ui_main s, hole_feature_ui_main h) { if (s != null && h != null) hole_editor_dialog_ui_main.open(s, h, this, container.getScene() != null ? container.getScene().getWindow() : null); }
    public void openExtrudeEditor(shape_item_ui_main s, extrude_feature_ui_main e) { if (s != null && e != null) extrude_editor_dialog_ui_main.open(s, e, this, container.getScene() != null ? container.getScene().getWindow() : null); } public void openPatternEditor(shape_item_ui_main s, hole_pattern_ui_main p) { if (s != null && p != null) pattern_editor_dialog_ui_main.open(s, p, this, container.getScene() != null ? container.getScene().getWindow() : null); }
    public void clearHistory() { history.clear(); } public List<shape_item_ui_main> getShapes() { return new ArrayList<>(shapes); }
    public shape_item_ui_main getSelectedShape()  { return selectedShape; } public axis_drag_controller_ui_main getAxisDrag() { return axisDrag; }
    public face_reference_ui_main getActiveFace() { return activeFace; } public hole_feature_ui_main getSelectedHole() { return selectedHole; }
    public shape_item_ui_main getSelectedHoleShape() { return selectedHoleShape; } public extrude_feature_ui_main getSelectedExtrude() { return selectedExtrude; }
    public hole_pattern_ui_main getSelectedPattern() { return selectedPattern; } public shape_item_ui_main getSelectedPatternShape() { return selectedPatternShape; }
    public ui.workspace.drafting.topology.topology_derived_face_ui_main getSelectedDerivedFace() { return selectedDerivedFace; }
    public void setSelectedDerivedFace(ui.workspace.drafting.topology.topology_derived_face_ui_main df) { this.selectedDerivedFace = df; }

    public void setActiveFace(face_reference_ui_main face) {
        this.activeFace = face; faceOverlay.highlightFace(face);
        if (statusCallback != null && face != null) statusCallback.accept(face.getFaceKind().getLabel() + " on " + (selectedShape != null ? selectedShape.getName() : "") + (face.isPlanar() ? " (Planar)" : " (Curved)"));
    }

    public void updateActiveFace() { if (activeFace != null && selectedShape != null) { activeFace = face_picker_ui_main.refreshFace(activeFace, selectedShape); faceOverlay.highlightFace(activeFace); } }

    public void selectShape(shape_item_ui_main item) {
        if (selectedShape == item) { if (activeFace != null) updateActiveFace(); return; }
        if (selectedHoleShape != item) selectHole(null, null);
        if (selectedPatternShape != item) selectPattern(null, null);
        setActiveFace(null);
        if (selectedShape != null) { selectedShape.setSelectedExtrudeId(null); selectedShape.setSelected(false); }
        selectedShape = item;
        if (selectedShape != null) {
            selectedShape.setSelected(true); axisDrag.attachTo(selectedShape);
            if (statusCallback != null) statusCallback.accept(selectedShape.getName() + " selected (" + selectedShape.getType().getLabel() + ")");
        } else axisDrag.detach();
        if (onSelectionChanged != null) onSelectionChanged.accept(selectedShape);
    }

    public void selectHole(shape_item_ui_main shape, hole_feature_ui_main hole) {
        this.selectedHoleShape = shape; this.selectedHole = hole;
        if (hole != null) { if (selectedExtrude != null) selectExtrude(shape, null); if (selectedPattern != null) selectPattern(shape, null); }
        if (shape != null && selectedShape != shape) selectShape(shape);
        holeOverlay.highlightHole(shape, hole);
        if (hole != null && statusCallback != null) statusCallback.accept(hole.getName() + " selected (" + hole.getFaceKind().getLabel() + ", Dia: " + hole.getDiameter() + " mm)");
        if (onHoleSelectionChanged != null) onHoleSelectionChanged.accept(shape, hole);
    }

    public void selectExtrude(shape_item_ui_main shape, extrude_feature_ui_main ext) {
        this.selectedExtrude = ext;
        if (shape != null && selectedShape != shape) selectShape(shape);
        if (shape != null) shape.setSelectedExtrudeId(ext != null ? ext.getId() : null);
        if (ext != null) { if (selectedHole != null) selectHole(shape, null); if (selectedPattern != null) selectPattern(shape, null); }
        if (ext != null && statusCallback != null) statusCallback.accept(ext.getName() + " selected (" + ext.getFaceKind().getLabel() + ")");
        if (onExtrudeSelectionChanged != null) onExtrudeSelectionChanged.accept(shape, ext);
        if (onShapesChanged != null) onShapesChanged.run();
    }

    public void selectPattern(shape_item_ui_main shape, hole_pattern_ui_main pat) {
        this.selectedPatternShape = shape; this.selectedPattern = pat;
        if (shape != null && selectedShape != shape) selectShape(shape);
        if (pat != null) { if (selectedHole != null) selectHole(shape, null); if (selectedExtrude != null) selectExtrude(shape, null); }
        if (pat != null && statusCallback != null) statusCallback.accept(pat.getPatternType().getLabel() + " selected (" + pat.getInstanceCount() + " instances)");
        if (onPatternSelectionChanged != null) onPatternSelectionChanged.accept(shape, pat);
    }

    public void checkHoleHit(face_reference_ui_main face, shape_item_ui_main shape) {
        if (shape != null && face != null) {
            var res = ui.workspace.drafting.faces.face_selection_resolver_ui_main.resolveFace(shape, face.getFaceKind(), face.getLocalHitU(), face.getLocalHitV());
            selectedDerivedFace = res.derivedFace();
        } else selectedDerivedFace = null;
        var h = hole_overlay_ui_main.findHoleAt(shape, face);
        if (h != null) { selectHole(shape, h); return; }
        var hit = hole_overlay_ui_main.findPatternInstanceAt(shape, face);
        if (hit != null) selectPattern(shape, hit.pattern()); else selectHole(shape, null);
    }

    public shape_item_ui_main findShapeNear(Point3D pt, double threshold) {
        if (selectedShape != null && selectedShape.isNear(pt, threshold)) return selectedShape;
        for (int i = shapes.size() - 1; i >= 0; i--) if (shapes.get(i).isNear(pt, threshold)) return shapes.get(i);
        return null;
    }

    public shape_item_ui_main findShapeByNode(javafx.scene.Node node) {
        if (node != null) for (int i = shapes.size() - 1; i >= 0; i--) if (shapes.get(i).containsNode(node)) return shapes.get(i);
        return null;
    }
}
