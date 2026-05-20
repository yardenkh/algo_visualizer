# Architecture Overview

<a id="doc-architecture"></a>

This project follows a layered, model-driven Swing architecture.

## Layers at a glance

```text
Main.java
  -> ui.GraphEditorFrame
      -> ui.GraphEditorController
          -> graph.DirectedGraph / Vertex / Edge
      -> algorithm.AlgorithmPlaybackController
          -> algorithm.GraphAlgorithm implementations
      -> ui.AlgorithmWorkspacePanel
          -> ui.dialogs.* views
      -> ui.extras.* action handlers and services
      -> ui.templates.* premade graph catalog
```

## Why the project is structured this way

The project separates **state**, **execution**, and **presentation**:

- **State** lives in `graph` and `ui.GraphEditorController`.
- **Execution** lives in `algorithm` and its subpackages.
- **Presentation** lives in `ui`, `ui.dialogs`, `ui.extras`, and `ui.templates`.

That separation makes the system easier to extend because:

- algorithms can be added without rewriting the editor,
- dialogs can reuse the same timeline payloads in floating or docked form,
- and the graph model remains simple enough to inspect directly.

## Runtime responsibilities by layer

### `graph`

Owns the in-memory graph model. It does not know anything about Swing, algorithms, or playback.

### `ui`

Owns editing, selection, layout, clipboard, menu actions, and the main window shell. It translates user intent into graph mutations or algorithm requests.

### `algorithm`

Owns algorithm contracts, result objects, timeline snapshots, sub-routine support, and playback state.

### `ui.dialogs`

Owns secondary views that display algorithm outputs: pseudocode, matrices, stack frames, residual graphs, level graphs, topological order, and premade graph selection.

### `ui.extras`

Owns selection-scoped computations and transformations. These are not playback algorithms; they are graph utilities invoked from the Extras dialog.

### `ui.templates`

Owns the premade graph catalog used by the graph picker.

## Primary control flow

### 1. Startup

`Main.main(...)` launches `GraphEditorFrame` on the Swing event dispatch thread.

### 2. Editing

The frame delegates all graph mutations to `GraphEditorController`. The canvas paints the graph from controller state and forwards mouse/keyboard input back to the controller.

### 3. Algorithm selection and run

The frame opens `AlgorithmRunDialog`, which filters algorithms by `AlgorithmType` and returns an `AlgorithmRunConfiguration`.

### 4. Playback

The chosen `GraphAlgorithm` receives an `AlgorithmRequest`, returns an `AlgorithmResult`, and the `AlgorithmPlaybackController` steps through that result.

### 5. Side views

`AlgorithmWorkspacePanel.syncAll(...)` shows the correct side panels for the current step.

## Design patterns used throughout the project

### Model / controller / view split

- `DirectedGraph` is the model.
- `GraphEditorController` is the application controller.
- `GraphCanvasPanel` and the dialogs are views.

### Immutable timeline snapshots

Algorithms do not hand the UI live mutable state. They hand it immutable steps that can be replayed consistently.

### Adapter pattern for recursion

Recursive algorithms keep their own internal frame structures, then convert them into generic `StackFramesSnapshot` objects through adapters.

### Thin UI action handlers

The `ui.extras` package keeps action handlers thin. The heavy computation lives in services so it can be reused and tested independently.

### Reusable embeddable panels

Several dialogs can also provide embeddable panels. This allows the same data view to work both as a popup and as a docked workspace tab.

## What to read next

- [`concepts.md`](concepts.md)
- [`packages/graph.md`](packages/graph.md)
- [`packages/algorithm.md`](packages/algorithm.md)
- [`packages/ui.md`](packages/ui.md)

