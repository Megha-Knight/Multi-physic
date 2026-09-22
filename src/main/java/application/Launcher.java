package application;

/**
 * Launcher.java
 * -----------------------------------------------------------------------------
 * Plain entry point for the Multi-physics application.
 *
 * This class does NOT extend javafx.application.Application on purpose.
 * It exists so the app can be started:
 *   - from the IDE "Run" button           -> application.Launcher
 *   - from the shaded / runnable JAR       -> Manifest Main-Class = application.Launcher
 *   - from jpackage native bundles
 *
 * It simply forwards control to {@link Main}, which is the real JavaFX
 * Application subclass (declared as ${app.main.class} = application.Main in pom.xml).
 *
 * Keeping the launcher separate avoids the classic
 * "JavaFX runtime components are missing" error when starting a fat JAR.
 */
public final class Launcher {

    private Launcher() {
        // no instances
    }

    public static void main(String[] args) {
        // Hand off to the JavaFX Application class.
        Main.main(args);
    }
}
