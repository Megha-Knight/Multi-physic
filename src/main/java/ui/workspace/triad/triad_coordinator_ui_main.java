package ui.workspace.triad;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import ui.workspace.drafting.sketch.sketch_mode_ui_main;
import java.util.function.BiConsumer;

/**
 * triad_coordinator_ui_main.java
 * Orchestrates synchronization, lifecycle events, and interaction between
 * the persistent global world triad and the contextual in-scene local triad.
 */
public class triad_coordinator_ui_main {

    private final world_triad_ui_main worldTriad;
    private final local_triad_3d_ui_main localTriad;
    private final sketch_mode_ui_main sketchMode;

    public triad_coordinator_ui_main(StackPane viewport, Group root3D, Rotate rx, Rotate ry,
                                    sketch_mode_ui_main sketchMode,
                                    BiConsumer<Double, Double> snapAction) {
        this.sketchMode = sketchMode;
        this.worldTriad = new world_triad_ui_main(rx, ry);
        this.localTriad = new local_triad_3d_ui_main();

        if (snapAction != null) {
            this.worldTriad.setOnSnapView(snapAction);
        }

        root3D.getChildren().add(localTriad);

        StackPane.setAlignment(worldTriad, Pos.BOTTOM_LEFT);
        StackPane.setMargin(worldTriad, new Insets(0, 0, 14, 14));
        viewport.getChildren().add(worldTriad);

        if (sketchMode != null) {
            sketchMode.addOnModeChanged(this::syncState);
        }
        syncState();
    }

    public world_triad_ui_main getWorldTriad() {
        return worldTriad;
    }

    public local_triad_3d_ui_main getLocalTriad() {
        return localTriad;
    }

    public void syncState() {
        if (sketchMode != null && sketchMode.isActive()) {
            var cs = sketchMode.getActiveCoordSystem();
            var frame = coordinate_triad_math_ui_main.resolveLocalFrame(cs);
            localTriad.updateFrame(frame);
            worldTriad.setLocalActive(true, frame != null ? frame.name() : "Sketch");
        } else {
            localTriad.updateFrame(null);
            worldTriad.setLocalActive(false, null);
        }
    }

    public void setExplicitLocalFrame(coordinate_triad_math_ui_main.TriadFrame frame) {
        if (frame != null && frame.isValid()) {
            localTriad.updateFrame(frame);
            worldTriad.setLocalActive(true, frame.name());
        } else {
            syncState();
        }
    }
}
