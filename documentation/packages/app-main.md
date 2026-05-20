# Application Entry (`Main`)

<a id="pkg-app-main"></a>

This page documents the root bootstrap path of the application.

## File

- `src/Main.java`

## `Main`

<a id="cls-main"></a>

`Main` is the Java entry point for the entire desktop app.

### Responsibilities

- start Swing on the Event Dispatch Thread (EDT),
- construct the main frame (`ui.GraphEditorFrame`),
- and show the UI.

### Runtime flow

1. JVM calls `Main.main(String[] args)`.
2. `SwingUtilities.invokeLater(...)` (or equivalent) schedules UI construction.
3. `GraphEditorFrame` initializes controller, canvas, dialogs, workspace, and playback wiring.
4. The application enters interactive mode.

### Why this matters

Even though this file is small, it defines the thread-correct UI startup boundary. Any global startup behavior (future config loading, logging initialization, command-line flags) should be added here or immediately around this boundary.

### Extension guidance

If you later add startup options, keep these rules:

- parse arguments before building Swing components,
- avoid long blocking work on the EDT,
- and if you must preload data, do it on a background thread and hand off only final UI updates to the EDT.

