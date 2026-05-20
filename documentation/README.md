# Graph Editor Documentation

<a id="doc-root"></a>

This folder is the project’s human-first documentation layer. It is designed to let a reader understand the system **without opening the source code first**, while still making it easy to jump from code symbols to the relevant explanation later.

## What this project is

This application is a Swing-based graph editor and algorithm visualizer. It combines:

- a mutable in-memory graph model,
- a graph editing canvas,
- a playback engine for algorithm timelines,
- multiple algorithm families,
- dialogs for matrices, recursion, residual graphs, topological order, and pseudocode,
- and a set of “extras” actions that transform or analyze the selected subgraph.

The codebase is intentionally split into a small number of stable layers so the flow is easy to reason about:

1. **`graph`** — the domain model.
2. **`algorithm`** — the execution/timeline framework plus algorithm families.
3. **`ui`** — the editor, canvas, playback workspace, and dialogs.
4. **`ui.extras` / `ui.templates`** — supporting actions and premade graph definitions.

## How to use these docs

<a id="doc-navigation"></a>

Start here and move in the same order the application itself does:

1. **`Main.java`** starts the Swing app.
2. **`ui.GraphEditorFrame`** builds the main window and wires together all controls.
3. **`ui.GraphEditorController`** owns the mutable graph state, clipboard, undo, and selection logic.
4. **`algorithm.GraphAlgorithmRegistry`** lists every algorithm exposed in the UI.
5. **`ui.AlgorithmWorkspacePanel`** and the dialogs show the playback surfaces.

If you only read a few pages, read these first:

- [`architecture.md`](architecture.md) — system overview and runtime flow.
- [`packages/graph.md`](packages/graph.md) — the graph domain model.
- [`packages/algorithm.md`](packages/algorithm.md) — the execution model and timeline payloads.
- [`packages/ui.md`](packages/ui.md) — the main UI shell and editing workflow.
- [`reference/file-coverage.md`](reference/file-coverage.md) — every Java file covered in one matrix.

## Documentation conventions

<a id="doc-conventions"></a>

### Anchor naming

Every page uses stable HTML anchors so future source-code hyperlinks can target them directly.

Recommended anchor scheme:

- `pkg-...` for package-level sections
- `cls-...` for class sections
- `method-...` for important methods or flows
- `concept-...` for cross-cutting ideas

Examples:

- `pkg-algorithm`
- `cls-ui-grapheditorframe`
- `method-ui-grapheditorcontroller-pasteclipboard`
- `concept-algorithm-timeline`

### Writing style

These docs are intentionally written for humans first:

- explain purpose before implementation,
- describe inputs and outputs explicitly,
- summarize behavior before listing details,
- connect every component to its neighbors,
- and use concrete examples when the code has a meaningful usage pattern.

## System overview

<a id="doc-overview"></a>

The application has three major runtime ideas:

### 1. The graph model is mutable

The `graph` package stores vertices, edges, names, colors, weights, and temporary traversal metadata. The editor mutates this model directly as the user edits the graph.

### 2. Algorithms do not mutate the live UI

Each algorithm receives an immutable `AlgorithmRequest`, then returns an immutable `AlgorithmResult` containing a timeline of `AlgorithmStep` snapshots.

### 3. Playback is step-based and UI-synchronized

The playback controller exposes one step at a time. The canvas and dialogs repaint themselves from the current step snapshot, not from the algorithm’s internal loop variables.

## Main runtime flow

<a id="doc-runtime-flow"></a>

```text
Main
  -> GraphEditorFrame
      -> GraphEditorController (graph state)
      -> GraphCanvasPanel (editing + rendering)
      -> AlgorithmPlaybackController (timeline stepping)
      -> AlgorithmWorkspacePanel (pseudocode, matrix, stack, flow views)
      -> dialogs and extras helpers
```

### Typical algorithm run

1. The user selects a graph scope and chooses an algorithm.
2. `GraphEditorFrame` creates an `AlgorithmRequest`.
3. The algorithm validates the request.
4. The algorithm runs and returns an `AlgorithmResult`.
5. `AlgorithmPlaybackController` starts the result.
6. The canvas and dialogs repaint from the current `AlgorithmStep`.
7. Optional sub-routines, matrices, stack frames, residual graphs, and level graphs appear when the current step carries those payloads.

## Core concepts

<a id="doc-concepts"></a>

### Algorithm timeline

A timeline is a sequence of immutable visual states. Each state can contain:

- active pseudocode line index,
- vertex and edge colors,
- vertex and edge labels,
- matrix payloads,
- stack payloads,
- residual graph payloads,
- level graph payloads,
- and an optional callable sub-routine id.

### Sub-routines

Sub-routines are modeled as reusable timeline overlays. An algorithm can mark a step as a call site and provide a canonical `SubroutineTimeline`. The pseudocode window can then open that sub-routine in its own tab while playback continues.

### Selection scope

The editor distinguishes between:

- the full graph,
- the algorithm scope,
- and the selected subgraph used by extras actions.

This separation matters because algorithms often run on the selected scope, while extras actions usually operate on the exact selection only.

### Visual payloads

Some algorithms need more than vertex/edge coloring:

- APSP algorithms use `MatrixSnapshot`.
- Recursive algorithms use `StackFramesSnapshot`.
- Max-flow algorithms use `ResidualGraphSnapshot` and `LevelGraphSnapshot`.

## Common examples

<a id="doc-examples"></a>

### Create an algorithm request

```java
AlgorithmRequest request = controller.createAlgorithmRequest(sourceId, sinkId);
GraphAlgorithm algorithm = GraphAlgorithmRegistry.getAlgorithms().get(0);
String validationError = algorithm.validate(request);
```

### Run an algorithm and start playback

```java
AlgorithmResult result = algorithm.run(request);
playbackController.start(result);
```

### Load a premade graph

```java
GraphTemplate template = GraphTemplateLibrary.byType(GraphTemplateType.BASICS).get(0);
controller.loadGraphTemplate(template);
```

### Copy and paste a selection

```java
controller.copySelectionToClipboard();
controller.pasteClipboard();
```

## Extending the system

<a id="doc-extension"></a>

### Adding a new algorithm

1. Implement `algorithm.GraphAlgorithm`.
2. Return a display name, description, type, pseudocode, validation logic, and a timeline result.
3. Register the new algorithm in `GraphAlgorithmRegistry`.
4. If the algorithm needs a new visual payload, add a new immutable snapshot class in `algorithm` and teach the relevant dialog/workspace view how to render it.

### Adding a new visual side panel

1. Create a dialog or embeddable panel in `ui.dialogs`.
2. Let it consume a snapshot or model object from `algorithm`.
3. Hook it into `AlgorithmWorkspacePanel.syncAll(...)`.
4. If it should also float as a separate window, expose both a dialog view and an embeddable panel view.

### Adding a new extras action

1. Add the domain computation in `ui.extras` or a supporting service.
2. Add a thin action handler that validates selection and shows messages.
3. Wire the handler into `SelectionExtrasDialog` and `GraphEditorFrame`.

## Quick index

<a id="doc-index"></a>

- [`architecture.md`](architecture.md)
- [`concepts.md`](concepts.md)
- [`source-map.md`](source-map.md)
- [`hover-integration.md`](hover-integration.md)
- [`packages/app-main.md`](packages/app-main.md)
- [`packages/graph.md`](packages/graph.md)
- [`packages/algorithm.md`](packages/algorithm.md)
- [`packages/algorithm-sssp.md`](packages/algorithm-sssp.md)
- [`packages/algorithm-apsp.md`](packages/algorithm-apsp.md)
- [`packages/algorithm-mst.md`](packages/algorithm-mst.md)
- [`packages/algorithm-maxflow.md`](packages/algorithm-maxflow.md)
- [`packages/algorithm-general.md`](packages/algorithm-general.md)
- [`packages/algorithm-recursion.md`](packages/algorithm-recursion.md)
- [`packages/algorithm-coloring.md`](packages/algorithm-coloring.md)
- [`packages/ui.md`](packages/ui.md)
- [`packages/ui-dialogs.md`](packages/ui-dialogs.md)
- [`packages/ui-extras.md`](packages/ui-extras.md)
- [`packages/ui-templates.md`](packages/ui-templates.md)
- [`reference/entity-reference.md`](reference/entity-reference.md)
- [`reference/file-coverage.md`](reference/file-coverage.md)

## Verification notes

The project includes `src/algorithm/AlgorithmPlaybackSmokeTest.java`, which exercises the main timeline and playback flows, including sub-routine overlays and the major visualization payload types.

If you are trying to understand a bug or add a feature, use the package page that matches the code’s package first. The pages are organized to reflect the source tree as closely as possible.
