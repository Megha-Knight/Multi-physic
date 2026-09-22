# Multi-physics — Primary Window (JavaFX)

A minimal, runnable JavaFX shell for the designer's primary window.
Built for **Java 25.0.4 LTS** and **Maven 3.9.16**, following your existing `pom.xml`.

> **Update:** The in-window blue **Titlebar** has been **removed**. The app now
> uses the **default OS title bar**, which carries the **Multi-physics** title,
> the **app logo/icon**, and the native minimize/maximize/close buttons.

## Structure

```
src/main/java/
├── application/
│   ├── Main.java            # JavaFX Application (pom: ${app.main.class} = application.Main)
│   └── Launcher.java        # Plain entry point (pom: ${app.launcher.class} = application.Launcher)
└── ui/
    ├── UI_Main.java         # Governing view — assembles the 6 remaining bars
    ├── titlebar/            # (intentionally empty — see README.txt)
    ├── ribbonbar/ribbon_ui_main.java        # now the topmost bar
    ├── toolbar/tools_ui_main.java
    ├── navigationbar/navigation_ui_main.java
    ├── breadcrumbbar/breadcrumb_ui_main.java
    ├── Directorybar/directory_ui_main.java
    └── footerbar/footer_ui_main.java

src/main/resources/
├── icons/app_icon.png       # Multi-physics logo (shown in the OS title bar) + 32/64/128/256 sizes
└── themes/{dark.css, light.css}
```

## Run

**Option A — JavaFX plugin (uses `application.Main`):**
```
mvn -o javafx:run
```
(`-o` = offline, honouring your `build/local-maven-repo` in `.mvn/maven.config`.)

**Option B — IDE Run button:** run `application.Launcher`
(the VS Code config **"Astra (Run button)"** already points to `application.Launcher`).

**Option C — Runnable JAR:**
```
mvn -o clean package
java -jar target/Astra.jar
```

## Notes
- The window closes via the **native OS close button** (top-right of the OS title bar).
- Every bar is an independent class named exactly as its file, ready for real controls.
