package application;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * application_error_handler_ui_main.java
 * Global uncaught exception handler and diagnostic error reporter for MultiPhysics.
 * Prevents silent thread failures, logs diagnostic traces, and provides user recovery dialogs.
 */
public class application_error_handler_ui_main implements Thread.UncaughtExceptionHandler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        System.err.printf("[%s] [UNCAUGHT EXCEPTION] Thread: %s - %s: %s%n",
            timestamp, thread.getName(), throwable.getClass().getName(), throwable.getMessage());
        throwable.printStackTrace(System.err);

        try {
            if (Platform.isFxApplicationThread()) {
                showErrorDialog(thread, throwable);
            } else {
                Platform.runLater(() -> showErrorDialog(thread, throwable));
            }
        } catch (Exception e) {
            // Toolkit not initialized or fatal teardown: standard stderr logging above suffices
        }
    }

    private void showErrorDialog(Thread t, Throwable ex) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("MultiPhysics Error Diagnostics");
            alert.setHeaderText("An unexpected error occurred in " + t.getName());
            String msg = ex.getMessage();
            alert.setContentText((msg != null && !msg.isBlank()) ? msg : ex.getClass().getSimpleName());

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            TextArea area = new TextArea(sw.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setMaxWidth(Double.MAX_VALUE);
            area.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(area, Priority.ALWAYS);
            GridPane.setHgrow(area, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(area, 0, 0);
            alert.getDialogPane().setExpandableContent(expContent);

            alert.show();
        } catch (Exception ignored) {
            // Prevent recursive error dialogs
        }
    }
}
