package ui;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ui.breadcrumbbar.breadcrumb_ui_main;
import ui.footerbar.footer_ui_main;
import ui.navigationbar.navigation_ui_main;
import ui.ribbonbar.ribbon_ui_main;
import ui.shortcuts.shortcuts_ui_main;
import ui.shortcuts.space_bar_ui_main;
import ui.shortcuts.viewshortcuts_ui_main;
import ui.toolbar.tools_ui_main;
import ui.workspace.documentserializer_ui_main;
import ui.workspace.filetab_ui_main;
import ui.workspace.shapeeditor_ui_main;
import ui.workspace.shapeitem_ui_main;
import ui.workspace.workspace_ui_main;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * UI_Main.java
 * Governing view for Astra (Multi-Physics).
 * Coordinates Documents/Astra workspace, 3D drafting, breadcrumb, and navigation.
 */
public class UI_Main extends BorderPane {

    private final tools_ui_main tabToolbar;
    private final ribbon_ui_main ribbonBar;
    private final breadcrumb_ui_main breadcrumbBar;
    private final navigation_ui_main navigationBar;
    private final footer_ui_main footerBar;
    private final workspace_ui_main workspace3D;
    private final filetab_ui_main documentTabBar;
    private final documenthandler_ui_main docHandler;

    private final shortcuts_ui_main shortcuts;
    private final space_bar_ui_main spaceBarShortcut;
    private final viewshortcuts_ui_main viewShortcuts;

    public UI_Main() {
        try {
            URL css = getClass().getResource("/themes/light.css");
            if (css != null) getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        tabToolbar = new tools_ui_main();
        ribbonBar = new ribbon_ui_main();
        breadcrumbBar = new breadcrumb_ui_main();
        documentTabBar = new filetab_ui_main();
        footerBar = new footer_ui_main();
        workspace3D = new workspace_ui_main();

        docHandler = new documenthandler_ui_main(
            documentTabBar, footerBar, breadcrumbBar, workspace3D,
            () -> getScene() != null ? getScene().getWindow() : null
        );

        setupRibbonAndDrafting();
        setTop(new VBox(tabToolbar, ribbonBar, breadcrumbBar));

        navigationBar = new navigation_ui_main();
        setLeft(navigationBar);

        VBox centerArea = new VBox(documentTabBar, workspace3D);
        VBox.setVgrow(workspace3D, Priority.ALWAYS);
        setCenter(centerArea);
        setBottom(footerBar);

        shortcuts = new shortcuts_ui_main();
        spaceBarShortcut = new space_bar_ui_main(workspace3D.getCameraController());
        viewShortcuts = new viewshortcuts_ui_main(workspace3D.getCameraController());
        viewShortcuts.setOnToggleOrthographic(workspace3D::toggleProjection);

        setupDocumentSync();
        initShortcuts();

        sceneProperty().addListener((obs, o, sc) -> {
            if (sc != null) {
                shortcuts.attach(sc);
                spaceBarShortcut.attach(sc);
                viewShortcuts.attach(sc);
            }
        });

        docHandler.onNewFile();
    }

    private void setupRibbonAndDrafting() {
        ribbonBar.getNewButton().setPrimaryAction(docHandler::onNewFile);
        ribbonBar.getOpenButton().setPrimaryAction(docHandler::onOpenFile);
        ribbonBar.setOnSave(docHandler::onSaveFile);
        ribbonBar.setOnSaveAs(docHandler::onSaveAsFile);
        ribbonBar.setOnSaveRoot(docHandler::onSaveRootFile);
        ribbonBar.setOnSaveIn(docHandler::onSaveInFile);

        ribbonBar.setOnShapeSelected(shape -> {
            workspace3D.getDrafter().setShape(shape);
            footerBar.setStatusText(shape.getLabel() + " drafting active: Click & drag on XY ground plane. ESC to cancel.");
        });

        workspace3D.getDrafter().setStatusCallback(footerBar::setStatusText);
        workspace3D.getShapeEditor().setStatusCallback(footerBar::setStatusText);
    }

    private void setupDocumentSync() {
        boolean[] syncLock = {false};
        final filetab_ui_main.TabItem[] activeTabRef = {null};

        docHandler.setOnFileChanged(() -> navigationBar.getFileExplorer().refresh());
        documentTabBar.setOnNewRequested(docHandler::onNewFile);
        documentTabBar.setOnTabClosed(t -> {
            if (activeTabRef[0] == t) activeTabRef[0] = null;
        });

        documentTabBar.setOnTabSelected(t -> {
            workspace3D.getDrafter().cancel();
            if (activeTabRef[0] != null && activeTabRef[0] != t) {
                activeTabRef[0].setUserData(workspace3D.getShapeEditor().getShapes());
            }
            activeTabRef[0] = t;
            @SuppressWarnings("unchecked")
            List<shapeitem_ui_main> shapes = (List<shapeitem_ui_main>) t.getUserData();
            if (shapes == null && t.getFile() != null && t.getFile().exists()) {
                shapes = documentserializer_ui_main.loadFromFile(t.getFile());
                t.setUserData(shapes);
            }
            workspace3D.getShapeEditor().loadShapes(shapes != null ? shapes : Collections.emptyList());
            workspace3D.getShapeEditor().clearHistory();

            String p = t.getFile() != null ? t.getFile().getAbsolutePath() : t.getName();
            footerBar.setOpenedFilePath(p);
            footerBar.setStatusText("Active: " + t.getName());
            if (t.getFile() != null && !syncLock[0]) {
                syncLock[0] = true;
                try {
                    breadcrumbBar.setFolderPath(t.getFile().getAbsolutePath());
                    navigationBar.getFileExplorer().navigateTo(t.getFile().getParent());
                } finally { syncLock[0] = false; }
            }
        });

        breadcrumbBar.setOnPathChanged(path -> {
            footerBar.setOpenedFilePath(path);
            if (!syncLock[0]) {
                syncLock[0] = true;
                try { navigationBar.getFileExplorer().navigateTo(path); }
                finally { syncLock[0] = false; }
            }
        });

        navigationBar.getFileExplorer().setOnPathSelected(path -> {
            if (path == null) return;
            File f = new File(path);
            if (!syncLock[0]) {
                syncLock[0] = true;
                try {
                    breadcrumbBar.setFolderPath(path);
                    footerBar.setOpenedFilePath(path);
                    if (f.isFile() && (path.endsWith(".nd") || path.endsWith(".nc"))) {
                        docHandler.openFile(f);
                    }
                } finally { syncLock[0] = false; }
            }
        });
    }

    private void initShortcuts() {
        shapeeditor_ui_main editor = workspace3D.getShapeEditor();
        shortcuts.register(shortcuts_ui_main.NEW_FILE, docHandler::onNewFile);
        shortcuts.register(shortcuts_ui_main.OPEN_FILE, docHandler::onOpenFile);
        shortcuts.register(shortcuts_ui_main.SAVE_FILE, docHandler::onSaveFile);
        shortcuts.register(shortcuts_ui_main.SAVE_AS, docHandler::onSaveAsFile);
        shortcuts.register(shortcuts_ui_main.DELETE_ITEM, editor::deleteSelected);
        shortcuts.register(shortcuts_ui_main.BACK_SPACE, editor::deleteSelected);
        shortcuts.register(shortcuts_ui_main.COPY, editor::copySelected);
        shortcuts.register(shortcuts_ui_main.CUT, editor::cutSelected);
        shortcuts.register(shortcuts_ui_main.PASTE, editor::paste);
        shortcuts.register(shortcuts_ui_main.UNDO, editor::undo);
        shortcuts.register(shortcuts_ui_main.REDO, editor::redo);
        shortcuts.register(shortcuts_ui_main.REDO_ALT, editor::redo);
    }

    public tools_ui_main getTabToolbar()              { return tabToolbar; }
    public ribbon_ui_main getRibbonBar()               { return ribbonBar; }
    public breadcrumb_ui_main getBreadcrumbBar()       { return breadcrumbBar; }
    public navigation_ui_main getNavigationBar()       { return navigationBar; }
    public footer_ui_main getFooterBar()               { return footerBar; }
    public workspace_ui_main getWorkspace3D()          { return workspace3D; }
    public filetab_ui_main getDocumentTabBar()         { return documentTabBar; }
    public shortcuts_ui_main getShortcuts()            { return shortcuts; }
    public space_bar_ui_main getSpaceBarShortcut()     { return spaceBarShortcut; }
    public viewshortcuts_ui_main getViewShortcuts()    { return viewShortcuts; }
}
