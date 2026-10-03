# Test Execution Safety & Non-Blocking Verification Standard

## 1. JavaFX Test Harness Termination Rule
Whenever writing or executing test runners, verification suites, or headless harnesses that initialize JavaFX (`Platform.startup`):
- **Unconditional System.exit()**: The test suite's `main()` method MUST wrap its entire logic in a `try-catch-finally` block where the `finally` block unconditionally calls `System.exit(exitCode)`.
- **Reason**: JavaFX creates non-daemon threads (Glass/Render threads). If an unhandled exception, failed assertion, or runtime error occurs, the JVM will NOT exit on its own. This causes background PowerShell runners and IDE subagents to hang indefinitely waiting for standard input/process exit.

Example Pattern:
```java
public static void main(String[] args) {
    int exitCode = 1;
    try {
        // Initialize JavaFX safely
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        // Execute all test suites
        runAllTests();
        if (allPassed()) {
            exitCode = 0;
        }
    } catch (Throwable t) {
        t.printStackTrace();
        exitCode = 1;
    } finally {
        System.exit(exitCode);
    }
}
```

## 2. Compilation-First Invariant
- Never run a test suite directly against `target/classes` without first compiling all project sources via `scratch/compile_sources.ps1`.
- IDE background compilation (e.g. Eclipse ECJ) can write class files with `Unresolved compilation problems` bytecode when files are in intermediate states. Always compile cleanly with Microsoft OpenJDK 25 `javac.exe` using low-memory options before executing verification suites.

## 3. Background Task Execution & Timeouts
- Test execution scripts (`.ps1`) should print explicit start, progress, and finish markers with `$LASTEXITCODE`.
- Background tasks must be verified promptly; if any task does not complete within expected bounds, immediately inspect the task log rather than waiting indefinitely.
