package ui.workspace.drafting;

import javafx.geometry.Point3D;
import ui.workspace.document.document_serializer_ui_main;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * shape_history_ui_main.java
 * Manages Undo, Redo history stacks and shape clipboard for CAD operations.
 */
public class shape_history_ui_main {

    private static final int MAX_HISTORY = 40;

    private final Deque<List<shape_item_ui_main>> undoStack = new ArrayDeque<>();
    private final Deque<List<shape_item_ui_main>> redoStack = new ArrayDeque<>();
    private shape_item_ui_main clipboard = null;

    public void pushSnapshot(List<shape_item_ui_main> current) {
        if (current == null) return;
        if (undoStack.size() >= MAX_HISTORY) undoStack.removeLast();
        undoStack.push(document_serializer_ui_main.cloneShapes(current));
        redoStack.clear();
    }

    public List<shape_item_ui_main> undo(List<shape_item_ui_main> current) {
        if (undoStack.isEmpty()) return null;
        redoStack.push(document_serializer_ui_main.cloneShapes(current));
        return undoStack.pop();
    }

    public List<shape_item_ui_main> redo(List<shape_item_ui_main> current) {
        if (redoStack.isEmpty()) return null;
        undoStack.push(document_serializer_ui_main.cloneShapes(current));
        return redoStack.pop();
    }

    public void copy(shape_item_ui_main item) {
        if (item != null) {
            this.clipboard = new shape_item_ui_main(item.getType(), item.getP1(), item.getP2(),
                item.getWorldX(), item.getWorldY(), item.getWorldZ(), item.getRotationAngle());
        }
    }

    public shape_item_ui_main paste(double offsetDistance) {
        if (clipboard == null) return null;
        Point3D off = new Point3D(offsetDistance, 0, offsetDistance);
        Point3D np1 = clipboard.getP1().add(off);
        Point3D np2 = clipboard.getP2().add(off);
        clipboard = new shape_item_ui_main(clipboard.getType(), np1, np2,
            clipboard.getWorldX(), clipboard.getWorldY(), clipboard.getWorldZ(), clipboard.getRotationAngle());
        return new shape_item_ui_main(clipboard.getType(), np1, np2,
            clipboard.getWorldX(), clipboard.getWorldY(), clipboard.getWorldZ(), clipboard.getRotationAngle());
    }

    public boolean hasClipboard() { return clipboard != null; }
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
