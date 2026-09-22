package ui.shortcuts;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * shortcuts_ui_main.java
 * Central shortcut manager for standard editing and file management operations.
 */
public class shortcuts_ui_main {

    private final Map<KeyCombination, Runnable> shortcutMap = new HashMap<>();

    // Standard Editing Shortcuts
    public static final KeyCombination COPY        = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination PASTE       = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination CUT         = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination UNDO        = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination REDO        = new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination REDO_ALT    = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination SELECT_ALL  = new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination DELETE_ITEM = new KeyCodeCombination(KeyCode.DELETE);
    public static final KeyCombination BACK_SPACE  = new KeyCodeCombination(KeyCode.BACK_SPACE);
    public static final KeyCombination FIND        = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);

    // File Management Shortcuts
    public static final KeyCombination NEW_FILE    = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination OPEN_FILE   = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination SAVE_FILE   = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination SAVE_AS     = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination REFRESH     = new KeyCodeCombination(KeyCode.F5);

    public shortcuts_ui_main() {
        registerDefaultShortcuts();
    }

    private void registerDefaultShortcuts() {
        register(COPY,        () -> System.out.println("[Shortcut] Ctrl+C: Copy"));
        register(PASTE,       () -> System.out.println("[Shortcut] Ctrl+V: Paste"));
        register(CUT,         () -> System.out.println("[Shortcut] Ctrl+X: Cut"));
        register(UNDO,        () -> System.out.println("[Shortcut] Ctrl+Z: Undo"));
        register(REDO,        () -> System.out.println("[Shortcut] Ctrl+Y: Redo"));
        register(REDO_ALT,    () -> System.out.println("[Shortcut] Ctrl+Shift+Z: Redo"));
        register(SELECT_ALL,  () -> System.out.println("[Shortcut] Ctrl+A: Select All"));
        register(DELETE_ITEM, () -> System.out.println("[Shortcut] Delete: Remove Item"));
        register(BACK_SPACE,  () -> System.out.println("[Shortcut] Backspace: Remove Item"));
        register(FIND,        () -> System.out.println("[Shortcut] Ctrl+F: Search / Find"));

        register(NEW_FILE,    () -> System.out.println("[Shortcut] Ctrl+N: New Project"));
        register(OPEN_FILE,   () -> System.out.println("[Shortcut] Ctrl+O: Open File"));
        register(SAVE_FILE,   () -> System.out.println("[Shortcut] Ctrl+S: Save Project"));
        register(SAVE_AS,     () -> System.out.println("[Shortcut] Ctrl+Shift+S: Save As"));
        register(REFRESH,     () -> System.out.println("[Shortcut] F5: Refresh Workspace"));
    }

    public void register(KeyCombination combination, Runnable action) {
        if (combination != null && action != null) {
            shortcutMap.put(combination, action);
        }
    }

    public void attach(Scene scene) {
        if (scene == null) return;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
    }

    private void handleKeyEvent(KeyEvent event) {
        if (event.getTarget() instanceof javafx.scene.control.TextInputControl) {
            return;
        }
        for (Map.Entry<KeyCombination, Runnable> entry : shortcutMap.entrySet()) {
            if (entry.getKey().match(event)) {
                entry.getValue().run();
                event.consume();
                return;
            }
        }
    }

    public Map<KeyCombination, Runnable> getShortcutMap() {
        return shortcutMap;
    }
}
