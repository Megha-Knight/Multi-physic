package ui.featuremanager;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.List;

public final class feature_tree_connector_ui_main {

    public static final double COL_WIDTH = 18.0;
    public static final double ROW_HEIGHT = 22.0;
    public static final Color LINE_COLOR = Color.web("#94A3B8");
    public static final Color CHEVRON_COLOR = Color.web("#64748B");
    public static final Color CHEVRON_HOVER = Color.web("#0284C7");

    private feature_tree_connector_ui_main() {}

    public static List<TreeItem<?>> getPathFromRoot(TreeItem<?> item) {
        List<TreeItem<?>> path = new ArrayList<>();
        TreeItem<?> curr = item;
        while (curr != null && curr.getParent() != null) {
            path.add(0, curr);
            curr = curr.getParent();
        }
        return path;
    }

    public static Canvas renderGuideLines(TreeItem<?> item, double rowHeight) {
        if (item == null) return null;
        List<TreeItem<?>> path = getPathFromRoot(item);
        int depth = path.size() - 1;
        if (depth <= 0) return null;

        double h = rowHeight > 4.0 ? rowHeight : ROW_HEIGHT;
        double w = COL_WIDTH;
        double totalW = depth * w;
        Canvas canvas = new Canvas(totalW, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(LINE_COLOR);
        gc.setLineWidth(1.0);
        double midY = Math.floor(h * 0.5) + 0.5;

        for (int c = 0; c < depth; c++) {
            double colX = Math.floor(c * w + w * 0.5) + 0.5;
            if (c < depth - 1) {
                TreeItem<?> p = path.get(c);
                TreeItem<?> ch = path.get(c + 1);
                boolean hasMore = p.getChildren().indexOf(ch) < p.getChildren().size() - 1;
                if (hasMore) {
                    gc.strokeLine(colX, 0, colX, h);
                }
            } else {
                TreeItem<?> parent = path.get(depth - 1);
                boolean isLast = parent.getChildren().indexOf(item) == parent.getChildren().size() - 1;
                if (isLast) {
                    gc.strokeLine(colX, 0, colX, midY);
                } else {
                    gc.strokeLine(colX, 0, colX, h);
                }
                gc.strokeLine(colX, midY, colX + w * 0.5, midY);
            }
        }
        return canvas;
    }

    public static Node createDisclosureNode(TreeItem<?> item, double rowHeight) {
        if (item == null) return new Region();
        double h = rowHeight > 4.0 ? rowHeight : ROW_HEIGHT;
        if (item.getChildren().isEmpty()) {
            Canvas dash = new Canvas(14.0, h);
            GraphicsContext gc = dash.getGraphicsContext2D();
            gc.setStroke(LINE_COLOR);
            gc.setLineWidth(1.0);
            double midY = Math.floor(h * 0.5) + 0.5;
            gc.strokeLine(0, midY, 12.0, midY);
            return dash;
        }

        Polygon chevron = new Polygon();
        if (item.isExpanded()) {
            chevron.getPoints().addAll(1.0, 3.0, 9.0, 3.0, 5.0, 8.0);
        } else {
            chevron.getPoints().addAll(3.0, 1.0, 8.0, 5.0, 3.0, 9.0);
        }
        chevron.setFill(CHEVRON_COLOR);

        StackPane btn = new StackPane(chevron);
        btn.setAlignment(Pos.CENTER);
        btn.setPrefSize(14.0, h);
        btn.setMinSize(14.0, h);
        btn.setMaxSize(14.0, h);
        btn.setOnMouseEntered(e -> chevron.setFill(CHEVRON_HOVER));
        btn.setOnMouseExited(e -> chevron.setFill(CHEVRON_COLOR));
        btn.setOnMouseClicked(e -> {
            e.consume();
            item.setExpanded(!item.isExpanded());
        });
        return btn;
    }
}
