# Project Understanding Guide

This document is the single entry point for understanding, navigating, and extending the project.
It is intended to replace the practical role of `README.md`, `PROJECT_NAVIGATION.md`, and `project roadmap.md`.

## 1) What this system is

A desktop Java Swing graph editor and algorithm visualizer with three core capabilities:

1. Interactive graph authoring (add/move/delete/select/color vertices/edges).
2. Step-by-step playback of graph algorithms as immutable timeline snapshots.
3. Specialized side views (pseudocode, matrices, stack frames, residual/level graphs, topological order).

At runtime, users build or load a graph, choose an algorithm, and the algorithm produces an
`AlgorithmResult` containing ordered `AlgorithmStep` snapshots. The UI then plays those snapshots.

## 2) Architecture at a glance

- `src/Main.java`
  - Boots Swing and opens `GraphEditorFrame`.
- `src/graph`
  - Domain model (`DirectedGraph`, `Vertex`, `Edge`).
- `src/algorithm`
  - Framework contracts and timeline model (`GraphAlgorithm`, `AlgorithmRequest`, `AlgorithmResult`, `AlgorithmStep`, playback controller).
  - Algorithm families: `sssp`, `apsp`, `mst`, `maxflow`, `general`, plus `recursion` and `coloring` support.
- `src/ui`
  - Main window, canvas, controller, workspace orchestration.
- `src/ui/dialogs`
  - Run/config dialogs and algorithm visualization views.
- `src/ui/templates`
  - Premade graph model, categories, and static template catalog.
- `src/ui/extras`
  - Selection-based advanced actions (coloring, line graph, complement, crossing reduction, etc.).

## 3) Core concepts and abstractions

### 3.1 Timeline execution model (most important)

- Every runnable algorithm implements `GraphAlgorithm`.
- Input: immutable `AlgorithmRequest` built from selected graph scope.
- Output: immutable `AlgorithmResult`:
  - algorithm name
  - pseudocode lines
  - `List<AlgorithmStep>`
  - optional subroutine timelines and execution warnings
- Each `AlgorithmStep` stores visual state at one moment:
  - vertex/edge colors
  - vertex/edge annotations
  - active pseudocode line
  - optional payloads (`MatrixSnapshot`, `StackFramesSnapshot`, `ResidualGraphSnapshot`, `LevelGraphSnapshot`)

This is why stepping backward/forward is stable: UI never asks algorithms to "rewind".

### 3.2 Scope-first algorithm input

`AlgorithmRequest` always works on a chosen scope (selected vertices/induced edges, or full graph fallback).
Algorithms should use request helpers (for example `getOutgoingEdges`) so traversal cannot leak outside scope.

### 3.3 Controller-view separation

- `GraphEditorController` owns edits, selection state, graph import/export snapshots, and undo behavior.
- `GraphCanvasPanel` owns rendering + input dispatch.
- `GraphEditorFrame` wires actions, dialogs, and playback synchronization.

### 3.4 Orientation compatibility

Algorithms declare `GraphOrientationRequirement` (`DIRECTED_ONLY`, `UNDIRECTED_ONLY`, `EITHER`).
The UI uses this metadata before run so users get orientation-safe behavior.

## 4) High-value entry points

If you are new, read in this order:

1. `src/Main.java`
2. `src/ui/GraphEditorFrame.java`
3. `src/ui/GraphEditorController.java`
4. `src/algorithm/GraphAlgorithm.java`
5. `src/algorithm/AlgorithmRequest.java`
6. `src/algorithm/AlgorithmResult.java`
7. `src/algorithm/AlgorithmStep.java`
8. `src/algorithm/GraphAlgorithmRegistry.java`

Then pick one representative algorithm each:

- SSSP: `src/algorithm/sssp/DijkstraShortestPathAlgorithm.java`
- APSP: `src/algorithm/apsp/FloydWarshallAlgorithm.java`
- Max flow: `src/algorithm/maxflow/DinicAlgorithm.java`

## 5) Important APIs and how to use them

### 5.1 Registering runnable algorithms

`GraphAlgorithmRegistry` is the catalog consumed by run dialogs. New algorithms are not visible
until instantiated in its static list.

### 5.2 Playback control

`AlgorithmPlaybackController` drives current-step state and subroutine overlay expansion.
UI listeners subscribe with `addChangeListener(...)` and re-render on updates.

### 5.3 Subroutine overlays

The subroutine system (`SubroutineLibrary`, `SubroutineStepCollector`, `SubroutineTimeline`) allows
algorithms to expose expandable CLRS-style routines (for example RELAX, AUGMENT, DFS-VISIT).

## 6) Common flows with code snippets

### 6.1 Run algorithm flow (conceptual)

```java
GraphAlgorithm algorithm = config.getAlgorithm();
AlgorithmRequest request = controller.createAlgorithmRequest(sourceId, sinkId, collapsePairs);

String validation = algorithm.validate(request);
if (validation != null) {
    // show warning/error
    return;
}

AlgorithmResult result = algorithm.run(request);
playbackController.start(result);
```

### 6.2 Minimal algorithm skeleton

```java
public final class ExampleAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
        "procedure EXAMPLE(G)",
        "  ...",
        "end procedure"
    );

    @Override public String getDisplayName() { return "Example"; }
    @Override public String getDescription() { return "Demo algorithm."; }
    @Override public AlgorithmType getType() { return AlgorithmType.GENERAL; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }

    @Override
    public String validate(AlgorithmRequest request) {
        return request.isEmpty() ? "Select vertices first." : null;
    }

    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        steps.add(AlgorithmSupport.neutralStep(request, 0, "Start", "Initialize."));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }
}
```

### 6.3 Add a premade graph template

```java
// in GraphTemplateLibrary.buildTemplates()
items.add(customTemplate("Johnson Demo"));
```

Then define a helper returning `new GraphTemplate(name, type, positions, edges)`.

## 7) Extension playbooks

### 7.1 Add a new algorithm (end-to-end)

1. Implement `GraphAlgorithm` in an existing or new subpackage under `src/algorithm`.
2. Provide:
   - clear pseudocode list
   - validation logic
   - timeline steps (`AlgorithmStep`) with meaningful titles/messages
3. Register it in `GraphAlgorithmRegistry`.
4. If you need a new visualization payload:
   - extend `AlgorithmStep` data
   - add UI sync in `GraphEditorFrame` and `AlgorithmWorkspacePanel`
   - add or update dialog panel in `src/ui/dialogs`

### 7.2 Add a new extras action

1. Add handler in `src/ui/extras`.
2. Wire callback in `GraphEditorFrame` and `SelectionExtrasDialog`.
3. Keep business logic in controller/services; keep handler mostly orchestration + messages.

### 7.3 Add a new template category

1. Extend `GraphTemplateType`.
2. Add templates in `GraphTemplateLibrary`.
3. Confirm picker filtering in `PremadeGraphsDialog` still works as expected.

## 8) Design conventions used in this codebase

- Immutable algorithm output snapshots; mutable editor/model state.
- Stable vertex-id identity across packages.
- CLRS-inspired pseudocode naming to match educational intent.
- Step messages are instructional, not just debug text.
- Optional payloads are attached only when relevant (matrix/stack/residual/level).
- Dialogs focus on presentation; controller/services own mutation and computation.

## 9) Practical navigation index by task

- "Where do I change drawing behavior?"
  - `src/ui/GraphCanvasPanel.java`
- "Where do I change selection/edit mutations?"
  - `src/ui/GraphEditorController.java`
- "Where do I add algorithm metadata / list visibility?"
  - `src/algorithm/GraphAlgorithmRegistry.java`
- "Where do I tune run-dialog text?"
  - `src/ui/AlgorithmDetailsFormatter.java`
- "Where are max-flow residual/level snapshots created?"
  - `src/algorithm/maxflow/MaxFlowSupport.java`
- "Where are premade graphs defined?"
  - `src/ui/templates/GraphTemplateLibrary.java`

## 10) Notes for AI agents working in this repo

- Prefer understanding request scope first: many operations intentionally run on selected subgraphs.
- Do not bypass `AlgorithmRequest` helpers when implementing traversals.
- Keep timeline steps deterministic and comprehensible; avoid hidden state outside emitted steps.
- If adding new UI views, wire both floating dialog behavior and workspace embedding paths.
- Update relevant `package-info.java` when adding responsibilities to a package.

