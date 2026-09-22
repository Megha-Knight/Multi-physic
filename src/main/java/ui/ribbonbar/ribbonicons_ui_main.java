package ui.ribbonbar;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * ribbonicons_ui_main.java
 * Pure JavaFX vector icons for Ribbon buttons.
 */
public final class ribbonicons_ui_main {

    private ribbonicons_ui_main() {}

    /** Document icon with folded corner for 'New'. */
    public static Node createNewIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M 2 1 L 10 1 L 14 5 L 14 17 L 2 17 Z M 10 1 L 10 5 L 14 5");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(1.6);
        scaleIcon(path, size);
        return path;
    }

    /** Folder icon for 'Open'. */
    public static Node createOpenIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M 1 4 L 5 4 L 7 6 L 15 6 L 15 15 L 1 15 Z M 1 7 L 15 7");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(1.6);
        scaleIcon(path, size);
        return path;
    }

    /** Floppy diskette icon for 'Save'. */
    public static Node createSaveIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M 2 2 L 12 2 L 15 5 L 15 16 L 2 16 Z M 4 2 L 4 6 L 11 6 L 11 2 M 4 11 L 13 11 L 13 16 L 4 16 Z");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(1.5);
        scaleIcon(path, size);
        return path;
    }

    /** Dropdown down-arrow chevron. */
    public static Node createArrowDown(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M 1 2 L 5 6 L 9 2");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(1.6);
        scaleIcon(path, size);
        return path;
    }

    private static void scaleIcon(SVGPath path, double targetSize) {
        double currentSize = 16.0;
        double scale = targetSize / currentSize;
        path.setScaleX(scale);
        path.setScaleY(scale);
    }
}
