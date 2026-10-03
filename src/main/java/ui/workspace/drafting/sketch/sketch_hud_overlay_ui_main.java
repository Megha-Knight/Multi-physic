package ui.workspace.drafting.sketch;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * sketch_hud_overlay_ui_main.java
 * Viewport HUD displaying active sketch identity, plane, status, and geometry count.
 */
public class sketch_hud_overlay_ui_main extends VBox {

    private final Label titleLabel = new Label("SKETCH: None");
    private final Label planeLabel = new Label("Plane: None");
    private final Label statusLabel = new Label("Status: IDLE");
    private final Label countsLabel = new Label("Geometry: 0 | Constraints: 0");
    private final Button closeButton = new Button("Finish Sketch");

    public sketch_hud_overlay_ui_main(Runnable onCloseAction) {
        setSpacing(3);
        setPadding(new Insets(8, 12, 8, 12));
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.82); -fx-background-radius: 6; " +
                 "-fx-border-color: rgba(56, 189, 248, 0.5); -fx-border-radius: 6; -fx-border-width: 1;");
        setMouseTransparent(false);

        titleLabel.setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold; -fx-font-size: 11px;");
        planeLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");
        statusLabel.setStyle("-fx-text-fill: #FCD34D; -fx-font-size: 10px; -fx-font-weight: bold;");
        countsLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 10px;");

        closeButton.setStyle("-fx-background-color: #0284C7; -fx-text-fill: white; -fx-font-size: 10px; " +
                            "-fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
        closeButton.setOnAction(e -> {
            if (onCloseAction != null) onCloseAction.run();
        });

        HBox bottomRow = new HBox(countsLabel);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        HBox actionRow = new HBox(closeButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(titleLabel, planeLabel, statusLabel, bottomRow, actionRow);
        setVisible(false);
    }

    public void update(sketch_feature_ui_main sketch) {
        if (sketch == null) {
            setVisible(false);
            return;
        }
        titleLabel.setText("SKETCH: " + sketch.getName());
        String planeStr = (sketch.getFaceKind() != null) ? sketch.getFaceKind().getLabel()
                : (sketch.getPlaneType() != null ? sketch.getPlaneType().name() : "Custom");
        planeLabel.setText("Plane: " + planeStr);

        sketch_constraint_status_ui_main st = (sketch.getSolveResult() != null)
                ? sketch.getSolveResult().status() : sketch_constraint_status_ui_main.UNDER_CONSTRAINED;
        statusLabel.setText("Status: " + st.name());
        statusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " +
                (st == sketch_constraint_status_ui_main.FULLY_CONSTRAINED ? "#22C55E" :
                 st == sketch_constraint_status_ui_main.INVALID ? "#EF4444" : "#F59E0B") + ";");

        countsLabel.setText(String.format("Geometry: %d | Constraints: %d",
                sketch.getEntities().size(), sketch.getConstraints().size()));
        setVisible(true);
    }
}
