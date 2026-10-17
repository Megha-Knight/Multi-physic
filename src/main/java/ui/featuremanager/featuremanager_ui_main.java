package ui.featuremanager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import ui.framework_ui_main;
import ui.workspace.drafting.shape_editor_ui_main;
import ui.workspace.drafting.shape_item_ui_main;
import ui.workspace.drafting.holes.hole_feature_ui_main;
import ui.workspace.drafting.holes.hole_pattern_ui_main;
import ui.workspace.drafting.extrude.extrude_feature_ui_main;

public class featuremanager_ui_main extends BorderPane {

    private final TreeView<feature_tree_node_ui_main> treeView = new TreeView<>();
    private final TreeItem<feature_tree_node_ui_main> rootItem = new TreeItem<>(null);
    private final ListView<shape_item_ui_main> legacyListView = new ListView<>();
    private shape_editor_ui_main editor; private boolean syncLock = false;
    private java.util.function.Consumer<ui.workspace.drafting.sketch.sketch_feature_ui_main> onSketchDoubleClicked;
    public void setOnSketchDoubleClicked(java.util.function.Consumer<ui.workspace.drafting.sketch.sketch_feature_ui_main> c) { this.onSketchDoubleClicked = c; }

    public featuremanager_ui_main() {
        getStyleClass().add("feature-manager-pane"); setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #C9D1D9; -fx-border-width: 0 1 0 0;");
        Label lbl = new Label("Feature Manager"); lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1F2933;");
        HBox header = new HBox(lbl); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(4, 10, 4, 10)); header.setPrefHeight(26);
        header.setStyle("-fx-background-color: #F1F5F9; -fx-border-color: #CBD5E1; -fx-border-width: 0 0 1 0;"); setTop(header);
        treeView.setRoot(rootItem); treeView.setShowRoot(false);
        treeView.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0; -fx-indent: 0;");
        treeView.setCellFactory(tv -> new feature_tree_cell_ui_main(() -> editor, () -> onSketchDoubleClicked));
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (syncLock || editor == null || newVal == null || newVal.getValue() == null) return;
            syncLock = true;
            try {
                feature_tree_node_ui_main val = newVal.getValue();
                if (val.isShape()) { editor.selectShape(val.getShape()); }
                else if (val.isHole()) editor.selectHole(val.getParentShape(), val.getHole());
                else if (val.isPattern()) editor.selectPattern(val.getParentShape(), val.getPattern());
                else if (val.isExtrude()) editor.selectExtrude(val.getParentShape(), val.getExtrude());
                else if (val.isDerivedFace()) { editor.selectDerivedFace(val.getParentShape(), val.getDerivedFace()); }
                else if (val.isSketch()) editor.selectSketch(val.getParentShape(), val.getSketch());
                else if (val.isFillet()) editor.selectFillet(val.getParentShape(), val.getFillet());
                else if (val.isChamfer()) editor.selectChamfer(val.getParentShape(), val.getChamfer());
                else if (val.isDraft()) editor.selectDraft(val.getParentShape(), val.getDraft());
            } finally { syncLock = false; }
        });

        treeView.setOnMouseClicked(e -> {
            if (e.getTarget() == treeView && editor != null) { editor.selectPattern(null, null); editor.selectExtrude(null, null); editor.selectHole(null, null); editor.selectDerivedFace(null, null); editor.selectShape(null); }
        });
        legacyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!syncLock && editor != null) { syncLock = true; try { editor.selectShape(newVal); if (newVal != null) { editor.selectHole(newVal, null); editor.selectExtrude(newVal, null); editor.selectDerivedFace(newVal, null); } } finally { syncLock = false; } }
        });
        setCenter(treeView);
    }

    public void bindToEditor(shape_editor_ui_main ed) {
        this.editor = ed; if (editor == null) return;
        editor.setOnShapesChanged(this::refresh);
        editor.setOnSelectionChanged(s -> syncFromCanvas()); editor.setOnHoleSelectionChanged((s, h) -> syncFromCanvas());
        editor.setOnPatternSelectionChanged((s, p) -> syncFromCanvas()); editor.setOnExtrudeSelectionChanged((s, ext) -> syncFromCanvas());
        editor.setOnDerivedFaceSelectionChanged((s, df) -> syncFromCanvas());
        editor.setOnSketchSelectionChanged((s, sk) -> syncFromCanvas());
        editor.setOnFilletSelectionChanged((s, f) -> syncFromCanvas());
        editor.setOnChamferSelectionChanged((s, c) -> syncFromCanvas());
        editor.setOnDraftSelectionChanged((s, d) -> syncFromCanvas());
        refresh();
    }

    private boolean refreshing = false;

    public void refresh() {
        if (editor == null) return;
        if (!javafx.application.Platform.isFxApplicationThread()) {
            try { javafx.application.Platform.runLater(this::refresh); return; } catch (IllegalStateException ignored) {}
        }
        if (refreshing) return;
        refreshing = true;
        try {
            java.util.List<TreeItem<feature_tree_node_ui_main>> newItems = new java.util.ArrayList<>();
            for (shape_item_ui_main s : editor.getShapes()) {
                TreeItem<feature_tree_node_ui_main> sNode = new TreeItem<>(feature_tree_node_ui_main.forShape(s));
                sNode.setExpanded(true); newItems.add(sNode);
                for (var sk : s.getSketches()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forSketch(s, sk)));
                for (hole_feature_ui_main h : s.getHoles()) {
                    TreeItem<feature_tree_node_ui_main> hNode = new TreeItem<>(feature_tree_node_ui_main.forHole(s, h));
                    var body = s.getTopology();
                    if (body != null && h.getState() != ui.workspace.drafting.features.feature_state_ui_main.INVALID) {
                        if (h.isThroughAll()) {
                            var remEntry = body.getDerivedFaceById(s.getId() + ":F:" + h.getFaceKind().name() + ":REMAINING");
                            if (remEntry != null) hNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forDerivedFace(s, h, remEntry)));
                            for (var df : body.getDerivedFacesForFeature(h.getId())) hNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forDerivedFace(s, h, df)));
                            var oppK = ui.workspace.shapes.holes.hole_mesh_builder_ui_main.getOppositeFace(h.getFaceKind());
                            if (oppK != null) {
                                var remExit = body.getDerivedFaceById(s.getId() + ":F:" + oppK.name() + ":REMAINING");
                                if (remExit != null) hNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forDerivedFace(s, h, remExit)));
                            }
                        } else {
                            for (var df : body.getDerivedFacesForFeature(h.getId())) hNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forDerivedFace(s, h, df)));
                        }
                    }
                    hNode.setExpanded(true); sNode.getChildren().add(hNode);
                }
                for (hole_pattern_ui_main p : s.getPatterns()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forPattern(s, p)));
                for (extrude_feature_ui_main ext : s.getExtrusions()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forExtrude(s, ext)));
                for (var c : s.getChamfers()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forChamfer(s, c)));
                for (var f : s.getFillets()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forFillet(s, f)));
                for (var d : s.getDrafts()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forDraft(s, d)));
                for (var b : s.getBooleans()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forBoolean(s, b)));
                for (var sh : s.getShells()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forShell(s, sh)));
                for (var lf : s.getLofts()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forLoft(s, lf)));
                for (var sw : s.getSweeps()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forSweep(s, sw)));
                for (var rev : s.getRevolves()) sNode.getChildren().add(new TreeItem<>(feature_tree_node_ui_main.forRevolve(s, rev)));
            }
            syncLock = true;
            try { treeView.getSelectionModel().clearSelection(); rootItem.getChildren().setAll(newItems); legacyListView.getItems().setAll(editor.getShapes()); } finally { syncLock = false; }
            syncFromCanvas();
        } finally { refreshing = false; }
    }

    public TreeView<feature_tree_node_ui_main> getTreeView() { return treeView; }
    public ListView<shape_item_ui_main> getListView() { return legacyListView; }

    private void syncFromCanvas() {
        if (editor == null) return;
        if (!javafx.application.Platform.isFxApplicationThread()) {
            try { javafx.application.Platform.runLater(this::syncFromCanvas); return; } catch (IllegalStateException ignored) {}
        }
        if (syncLock) return;
        syncLock = true;
        try {
            shape_item_ui_main selShape = editor.getSelectedShape();
            hole_feature_ui_main selHole = editor.getSelectedHole(); hole_pattern_ui_main selPat = editor.getSelectedPattern();
            extrude_feature_ui_main selExt = editor.getSelectedExtrude(); String selDfId = editor.getSelectedDerivedFaceId();
            if (selShape == null) { treeView.getSelectionModel().clearSelection(); legacyListView.getSelectionModel().clearSelection(); return; }
            if (legacyListView.getSelectionModel().getSelectedItem() != selShape) legacyListView.getSelectionModel().select(selShape);

            for (TreeItem<feature_tree_node_ui_main> sItem : rootItem.getChildren()) {
                if (sItem.getValue() != null && sItem.getValue().getShape() == selShape) {
                    TreeItem<feature_tree_node_ui_main> target = sItem;
                    if (selDfId != null) {
                        for (var hItem : sItem.getChildren()) for (var dfItem : hItem.getChildren()) {
                            if (dfItem.getValue() != null && dfItem.getValue().isDerivedFace() && selDfId.equals(dfItem.getValue().getDerivedFace().getId())) {
                                sItem.setExpanded(true); hItem.setExpanded(true); target = dfItem; break;
                            }
                        }
                    } else if (selHole != null) {
                        for (var hItem : sItem.getChildren()) if (hItem.getValue() != null && hItem.getValue().getHole() == selHole) { sItem.setExpanded(true); target = hItem; break; }
                    } else if (selPat != null) {
                        for (var pItem : sItem.getChildren()) if (pItem.getValue() != null && pItem.getValue().getPattern() == selPat) { sItem.setExpanded(true); target = pItem; break; }
                    } else if (selExt != null) {
                        for (var eItem : sItem.getChildren()) if (eItem.getValue() != null && eItem.getValue().getExtrude() == selExt) { sItem.setExpanded(true); target = eItem; break; }
                    } else if (editor.getSelectedSketch() != null) {
                        for (var skItem : sItem.getChildren()) if (skItem.getValue() != null && skItem.getValue().getSketch() == editor.getSelectedSketch()) { sItem.setExpanded(true); target = skItem; break; }
                    } else if (editor.getSelectedFillet() != null) {
                        for (var fItem : sItem.getChildren()) if (fItem.getValue() != null && fItem.getValue().getFillet() == editor.getSelectedFillet()) { sItem.setExpanded(true); target = fItem; break; }
                    } else if (editor.getSelectedChamfer() != null) {
                        for (var cItem : sItem.getChildren()) if (cItem.getValue() != null && cItem.getValue().getChamfer() == editor.getSelectedChamfer()) { sItem.setExpanded(true); target = cItem; break; }
                    } else if (editor.getSelectedDraft() != null) {
                        for (var dItem : sItem.getChildren()) if (dItem.getValue() != null && dItem.getValue().getDraft() == editor.getSelectedDraft()) { sItem.setExpanded(true); target = dItem; break; }
                    }
                    if (treeView.getSelectionModel().getSelectedItem() != target) {
                        treeView.getSelectionModel().select(target);
                        int row = treeView.getRow(target); if (row >= 0) treeView.scrollTo(row);
                    }
                    return;
                }
            }
        } finally { syncLock = false; }
    }

    public static String formatItemLabel(shape_item_ui_main item) {
        if (item == null) return "";
        String lbl = item.getName();
        int h = item.getHoles().size(), ext = item.getExtrusions().size(), pat = item.getPatterns().size(), ch = item.getChamfers().size(), fil = item.getFillets().size(), dr = item.getDrafts().size(), bl = item.getBooleans().size(), sh = item.getShells().size(), lf = item.getLofts().size(), sw = item.getSweeps().size(), rv = item.getRevolves().size();
        if (h > 0 || ext > 0 || pat > 0 || ch > 0 || fil > 0 || dr > 0 || bl > 0 || sh > 0 || lf > 0 || sw > 0 || rv > 0) {
            java.util.List<String> p = new java.util.ArrayList<>();
            if (h > 0) p.add(h + (h == 1 ? " Hole" : " Holes"));
            if (pat > 0) p.add(pat + (pat == 1 ? " Pattern" : " Patterns"));
            if (ext > 0) p.add(ext + (ext == 1 ? " Extrude" : " Extrudes"));
            if (ch > 0) p.add(ch + (ch == 1 ? " Chamfer" : " Chamfers"));
            if (fil > 0) p.add(fil + (fil == 1 ? " Fillet" : " Fillets"));
            if (dr > 0) p.add(dr + (dr == 1 ? " Draft" : " Drafts"));
            if (bl > 0) p.add(bl + (bl == 1 ? " Boolean" : " Booleans"));
            if (sh > 0) p.add(sh + (sh == 1 ? " Shell" : " Shells"));
            if (lf > 0) p.add(lf + (lf == 1 ? " Loft" : " Lofts"));
            if (sw > 0) p.add(sw + (sw == 1 ? " Sweep" : " Sweeps"));
            if (rv > 0) p.add(rv + (rv == 1 ? " Revolve" : " Revolves"));
            lbl += " [" + String.join(", ", p) + "]";
        }
        return lbl;
    }
}
