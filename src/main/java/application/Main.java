package application;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ui.UI_Main;

import java.io.InputStream;

/**
 * Main.java
 * -----------------------------------------------------------------------------
 * The real JavaFX Application class for the Multi-physics designer.
 * Referenced by pom.xml as ${app.main.class} = application.Main and used by
 * `mvn javafx:run`.
 *
 * Responsibilities:
 *   - Keep the native OS title bar (StageStyle.DECORATED = default).
 *   - Set the window title to "Multi-physics" and attach the app icon/logo,
 *     so the logo shows in the DEFAULT OS title bar (the in-window blue title
 *     strip has been removed).
 *   - Delegate the ENTIRE window content to {@link UI_Main}, which is the
 *     single governing UI class that assembles the remaining bars.
 */
public class Main extends Application {

    /** Initial window size (roughly matches the reference wireframe). */
    private static final double INITIAL_WIDTH  = 1200;
    private static final double INITIAL_HEIGHT = 680;

    @Override
    public void start(Stage stage) {
        // UI_Main is the top-level governing view that builds the bars.
        UI_Main root = new UI_Main();

        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);

        // Keep the native OS title bar and give it a name + logo/icon.
        stage.setTitle("Multi-physics");
        applyWindowIcon(stage);

        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Loads the generated application logo/icon from the classpath
     * (src/main/resources/icons/app_icon.png) and attaches it to the stage,
     * so it appears in the DEFAULT OS title bar and taskbar.
     * Fails silently if not found, so a missing icon never blocks the window.
     */
    private void applyWindowIcon(Stage stage) {
        try (InputStream in = getClass().getResourceAsStream("/icons/app_icon.png")) {
            if (in != null) {
                stage.getIcons().add(new Image(in));
            }
        } catch (Exception ignored) {
            // Icon is optional; ignore any loading problem.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
