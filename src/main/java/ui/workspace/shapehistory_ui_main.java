package ui.workspace;

import javafx.geometry.Point3D;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * shapehistory_ui_main.java
 * Manages Undo, Redo history stacks and shape clipboard for CAD operations.
 */
public class shapehistory_ui_main {

    private static final int MAX_HISTORY = 40;

    private final Deque<List<shapeitem_ui_main>> undoStack = new ArrayDeque<>();
    private final Deque<List<shapeitem_ui_main>> redoStack = new ArrayDeque<>();
    private shapeitem_ui_main clipboard = null;

    public void pushSnapshot(List<shapeitem_ui_main> current) {
        if (current == null) return;
        if (undoStack.size() >= MAX_HISTORY) undoStack.removeLast();
        undoStack.push(documentserializer_ui_main.cloneShapes(current));
        redoStack.clear();
    }

    public List<shapeitem_ui_main> undo(List<shapeitem_ui_main> current) {
        if (undoStack.isEmpty()) return null;
        redoStack.push(documentserializer_ui_main.cloneShapes(current));
        return undoStack.pop();
    }

    public List<shapeitem_ui_main> redo(List<shapeitem_ui_main> current) {
        if (redoStack.isEmpty()) return null;
        undoStack.push(documentserializer_ui_main.cloneShapes(current));
        return redoStack.pop();
    }

    public void copy(shapeitem_ui_main item) {
        if (item != null) {
            this.clipboard = new shapeitem_ui_main(item.getType(), item.getP1(), item.getP2());
        }
    }

    public shapeitem_ui_main paste(double offsetDistance) {
        if (clipboard == null) return null;
        Point3D off = new Point3D(offsetDistance, 0, offsetDistance);
        Point3D np1 = clipboard.getP1().add(off);
        Point3D np2 = clipboard.getP2().add(off);
        clipboard = new shapeitem_ui_main(clipboard.getType(), np1, np2);
        return new shapeitem_ui_main(clipboard.getType(), np1, np2);
    }

    public boolean hasClipboard() { return clipboard != null; }
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
