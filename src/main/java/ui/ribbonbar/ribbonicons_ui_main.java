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

    /** Hole feature icon with outer boundary and interior bore. */
    public static Node createHoleIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M 8 1 A 7 7 0 1 0 8 15 A 7 7 0 1 0 8 1 Z M 8 5 A 3 3 0 1 0 8 11 A 3 3 0 1 0 8 5 Z");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(1.4);
        scaleIcon(path, size);
        return path;
    }

    /** Linear pattern icon. */
    public static Node createLinearPatternIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M 3 8 A 2 2 0 1 0 3 8.01 M 8 8 A 2 2 0 1 0 8 8.01 M 13 8 A 2 2 0 1 0 13 8.01");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(1.4);
        scaleIcon(path, size);
        return path;
    }

    /** Circular pattern icon. */
    public static Node createCircularPatternIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M 8 3 A 1.5 1.5 0 1 0 8 3.01 M 13 8 A 1.5 1.5 0 1 0 13 8.01 M 8 13 A 1.5 1.5 0 1 0 8 13.01 M 3 8 A 1.5 1.5 0 1 0 3 8.01");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(1.4);
        scaleIcon(path, size);
        return path;
    }
    /** Machining tool / end mill cutter icon. */
    public static Node createMachiningIcon(double size, Color color) {
        SVGPath path = new SVGPath();
        path.setContent("M 6 1 L 10 1 L 10 4 L 11 6 L 11 11 L 8 15 L 5 11 L 5 6 L 6 4 Z M 7 6 L 9 7 M 7 9 L 9 10");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(1.4);
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
