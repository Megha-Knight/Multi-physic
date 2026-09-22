package ui.breadcrumbbar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import ui.framework_ui_main;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * breadcrumb_ui_main.java
 * Interactive Explorer & MATLAB-style Breadcrumb Bar.
 * Synchronizes with Documents/Astra workspace and active file selection.
 */
public class breadcrumb_ui_main extends HBox {

    private final HBox pathSegmentsBox;
    private String currentFolderPath = "";
    private final Deque<String> backStack = new ArrayDeque<>();
    private final Deque<String> forwardStack = new ArrayDeque<>();
    private Consumer<String> onPathChanged;

    public breadcrumb_ui_main() {
        setPadding(new Insets(2, 8, 2, 8));
        setPrefHeight(framework_ui_main.BREADCRUMB_BAR_HEIGHT);
        setMinHeight(framework_ui_main.BREADCRUMB_BAR_HEIGHT - 2);
        setMaxHeight(framework_ui_main.BREADCRUMB_BAR_HEIGHT + 4);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        getStyleClass().add("breadcrumb-bar");

        setStyle("-fx-background-color: " + framework_ui_main.BREADCRUMB_BAR_BG + "; " +
                 "-fx-border-color: " + framework_ui_main.BREADCRUMB_BORDER + "; -fx-border-width: 0 0 1 0;");

        Button backBtn    = createNavBtn("M 8 2 L 3 6.5 L 8 11", () -> navigateHistory(backStack, forwardStack));
        Button forwardBtn = createNavBtn("M 5 2 L 10 6.5 L 5 11", () -> navigateHistory(forwardStack, backStack));
        Button upBtn      = createNavBtn("M 2 8 L 6.5 3 L 11 8", this::navigateUp);

        HBox addressBar = new HBox(4);
        addressBar.setAlignment(Pos.CENTER_LEFT);
        addressBar.setPadding(new Insets(1, 6, 1, 6));
        addressBar.setPrefHeight(22);
        HBox.setHgrow(addressBar, Priority.ALWAYS);
        addressBar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 2; -fx-background-radius: 2;");

        pathSegmentsBox = new HBox(1);
        pathSegmentsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pathSegmentsBox, Priority.ALWAYS);

        addressBar.getChildren().addAll(createFolderIcon(), pathSegmentsBox);
        getChildren().addAll(new HBox(2, backBtn, forwardBtn, upBtn), addressBar);

        setFolderPath(framework_ui_main.astraDirectory().getAbsolutePath());
    }

    public void setOnPathChanged(Consumer<String> onPathChanged) {
        this.onPathChanged = onPathChanged;
    }

    public void setFolderPath(String path) {
        navigateTo(path, true);
    }

    public void navigateTo(String path, boolean recordHistory) {
        if (path == null || path.trim().isEmpty()) return;
        File f = new File(path);
        if (!f.exists()) return;

        String validPath = f.getAbsolutePath();
        if (recordHistory && !currentFolderPath.isEmpty() && !currentFolderPath.equalsIgnoreCase(validPath)) {
            backStack.push(currentFolderPath);
            forwardStack.clear();
        }
        currentFolderPath = validPath;
        buildSegments(f);
        if (onPathChanged != null) onPathChanged.accept(currentFolderPath);
    }

    private void buildSegments(File current) {
        pathSegmentsBox.getChildren().clear();
        Deque<File> chain = new ArrayDeque<>();
        File docs = new File(System.getProperty("user.home"), "Documents");
        boolean inDocs = isDescendantOrEqual(docs, current);

        File curr = current;
        while (curr != null) {
            chain.push(curr);
            if (inDocs && isSameFile(curr, docs)) break;
            curr = curr.getParentFile();
        }

        while (!chain.isEmpty()) {
            File item = chain.pop();
            String name = formatName(item);
            boolean isFile = item.isFile();

            Label segLabel = new Label(name);
            String baseStyle = "-fx-text-fill: #1F2933; -fx-font-size: 11px; -fx-padding: 1 3 1 3; -fx-cursor: hand;";
            segLabel.setStyle(baseStyle);
            segLabel.setOnMouseEntered(e -> segLabel.setStyle("-fx-text-fill: #005A85; -fx-font-size: 11px; -fx-padding: 1 3 1 3; -fx-background-color: #E2E8F0; -fx-cursor: hand;"));
            segLabel.setOnMouseExited(e -> segLabel.setStyle(baseStyle));
            segLabel.setOnMouseClicked(e -> navigateTo(item.getAbsolutePath(), true));
            pathSegmentsBox.getChildren().add(segLabel);

            if (!isFile) {
                Button dropBtn = new Button("›");
                dropBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-size: 11px; -fx-padding: 0 3 0 1; -fx-cursor: hand;");
                dropBtn.setOnMouseClicked(e -> breadcrumbmenu_ui_main.showSubfolderMenu(item, dropBtn, p -> navigateTo(p, true)));
                pathSegmentsBox.getChildren().add(dropBtn);
            }
        }
    }

    private String formatName(File f) {
        String name = f.getName();
        if (name == null || name.isEmpty()) name = f.getPath();
        if (name.endsWith(":\\") || name.endsWith(":/")) name = name.substring(0, name.length() - 2);
        else if (name.endsWith(":") || name.endsWith("\\") || name.endsWith("/")) name = name.substring(0, name.length() - 1);
        return name;
    }

    private void navigateUp() {
        if (!currentFolderPath.isEmpty()) {
            File parent = new File(currentFolderPath).getParentFile();
            if (parent != null && parent.exists()) navigateTo(parent.getAbsolutePath(), true);
        }
    }

    private void navigateHistory(Deque<String> from, Deque<String> to) {
        if (!from.isEmpty()) {
            to.push(currentFolderPath);
            navigateTo(from.pop(), false);
        }
    }

    private boolean isDescendantOrEqual(File ancestor, File child) {
        if (ancestor == null || child == null) return false;
        String a = ancestor.getAbsolutePath().toLowerCase();
        String c = child.getAbsolutePath().toLowerCase();
        return c.equals(a) || c.startsWith(a.endsWith(File.separator) ? a : a + File.separator);
    }

    private boolean isSameFile(File f1, File f2) {
        return f1 != null && f2 != null && f1.getAbsolutePath().equalsIgnoreCase(f2.getAbsolutePath());
    }

    public String getFolderPath() { return currentFolderPath; }

    private Button createNavBtn(String svg, Runnable act) {
        Button btn = new Button();
        SVGPath p = new SVGPath();
        p.setContent(svg);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#005A85"));
        p.setStrokeWidth(1.4);
        btn.setGraphic(p);
        btn.setPrefSize(20, 20);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 1; -fx-cursor: hand;");
        btn.setOnAction(e -> act.run());
        return btn;
    }

    private Node createFolderIcon() {
        SVGPath f = new SVGPath();
        f.setContent("M 1 2 L 4 2 L 6 3.5 L 12 3.5 L 12 10 L 1 10 Z");
        f.setFill(Color.web("#F59E0B"));
        f.setStroke(Color.web("#D97706"));
        f.setStrokeWidth(0.7);
        return f;
    }
}
