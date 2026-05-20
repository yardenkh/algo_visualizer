# `algorithm` Package

<a id="pkg-algorithm"></a>

The `algorithm` package is the **runtime and playback contract** for the whole application.

It defines how algorithms receive input, how they report results, how the UI steps through those results, and how shared visual payloads are attached to a timeline.

## Package responsibilities

- define the algorithm interface used by every concrete algorithm,
- capture immutable run input and output,
- store step-by-step visualization state,
- control playback and sub-routine overlays,
- provide shared helper methods and colors,
- and expose the central algorithm registry used by the UI.

## How this package connects to the rest of the system

- It **consumes** graph data from `graph` through `AlgorithmRequest`.
- It **is driven by** `ui.GraphEditorFrame` and `ui.AlgorithmWorkspacePanel`.
- It **feeds** the canvas and dialogs with immutable steps and visual payloads.
- Its subpackages (`sssp`, `apsp`, `mst`, `maxflow`, `general`, `recursion`, `coloring`) contain the concrete implementations and helper adapters.

## Core model classes

### `GraphAlgorithm`

<a id="cls-algorithm-graphalgorithm"></a>

The interface every algorithm must implement.

**Responsibilities**

- expose the display name, description, category, and orientation requirements,
- validate the current request before the run starts,
- return ordered pseudocode lines for the UI,
- and produce an `AlgorithmResult`.

**Important methods**

- `getDisplayName()` — label shown in the algorithm picker.
- `getDescription()` — used by the run dialog description area.
- `getType()` — drives the type filter in the run dialog.
- `getOrientationRequirement()` — lets the UI detect directed-only or undirected-only algorithms.
- `validate(...)` — returns a user-facing message, or `null` when the run is valid.
- `run(...)` — builds the immutable timeline.

### `AlgorithmRequest`

<a id="cls-algorithm-algorithmrequest"></a>

Immutable snapshot of the selected graph scope at the moment the user starts an algorithm.

**What it carries**

- the backing `DirectedGraph`,
- sorted scoped vertices and edges,
- the selected source vertex,
- the optional sink vertex,
- and the scoped vertex positions used by the UI.

**Important behavior**

- Copies and sorts vertices/edges so algorithm runs are deterministic.
- Rejects source/sink vertices that are outside the selected scope.
- Restricts outgoing-edge queries to the selected subgraph only.
- Provides scope helpers such as negative-weight detection and bidirectional edge checks.

**Common methods**

- `getScopeVertices()` / `getScopeEdges()` — immutable ordered scope data.
- `getOutgoingEdges(Vertex)` — scoped adjacency helper.
- `hasNegativeWeightEdge()` — validation aid for Dijkstra-style algorithms.
- `isSymmetricBidirectionalScope()` — helps detect undirected-style scopes stored as paired arcs.
- `hasAsymmetricScopedEdges()` — highlights directed asymmetry.

### `AlgorithmResult`

<a id="cls-algorithm-algorithmresult"></a>

The immutable output of one algorithm run.

**It contains**

- the algorithm name,
- the main pseudocode lines,
- the ordered list of `AlgorithmStep` objects,
- an infinite-loop warning flag and message,
- optional sub-routine timelines,
- and a mapping from main pseudocode line indices to callable sub-routine ids.

**Why this matters**

This class is the bridge between algorithm execution and playback. The UI never needs the algorithm’s internal variables after the run finishes.

**Important methods**

- `getSteps()` — the main timeline.
- `getSubroutineTimelines()` — reusable sub-routine overlays.
- `getCallableSubroutineByLine()` — call-site metadata for the pseudocode window.
- `withSteps(...)` — useful when the playback layer needs a derived step list.

### `AlgorithmStep`

<a id="cls-algorithm-algorithmstep"></a>

One immutable snapshot of algorithm state.

**A step can include**

- a title and message,
- the active pseudocode line,
- vertex colors,
- edge colors,
- vertex annotations,
- edge annotations,
- a matrix snapshot,
- a stack snapshot,
- a residual-graph snapshot,
- a level-graph snapshot,
- and an optional callable sub-routine id.

**Behavioral note**

The step object is intentionally presentation-oriented. Algorithms build these snapshots as they run so the UI can render them later without re-executing the algorithm.

### `AlgorithmPlaybackController`

<a id="cls-algorithm-algorithmplaybackcontroller"></a>

The playback state machine for the current run.

**Responsibilities**

- start and stop playback,
- step forward/backward through the current timeline,
- expose the current step, progress text, and info message,
- and manage the temporary sub-routine overlay state.

**Sub-routine behavior**

This controller is where the main pseudocode and sub-routine timelines are combined at runtime.

It tracks:

- the base result,
- the current result shown to the UI,
- the active sub-routine id,
- the injected step range,
- and the pre-overlay position so closing the overlay restores the correct main timeline location.

**Important methods**

#### `start(...)`

<a id="method-algorithm-algorithmplaybackcontroller-start"></a>

Begins playback for a fresh result and resets any active overlay state.

#### `stepForward()` / `stepBackward()`

<a id="method-algorithm-algorithmplaybackcontroller-stepforward"></a>
<a id="method-algorithm-algorithmplaybackcontroller-stepbackward"></a>

Move through the active timeline one step at a time.

#### `openSubroutineAtCurrentStep()`

<a id="method-algorithm-algorithmplaybackcontroller-opensubroutineatcurrentstep"></a>

Opens the call-site sub-routine associated with the current main-timeline step.

#### `openSubroutineForLine(...)`

<a id="method-algorithm-algorithmplaybackcontroller-opensubroutineforline"></a>

Opens a specific sub-routine based on a main pseudocode line index.

#### `closeActiveSubroutineOverlay()`

<a id="method-algorithm-algorithmplaybackcontroller-closeactivesubroutineoverlay"></a>

Restores the main timeline after a sub-routine overlay is closed.

#### `canOpenAnySubroutine()` / `canOpenSubroutineAtCurrentStep()`

<a id="method-algorithm-algorithmplaybackcontroller-canopenanysubroutine"></a>
<a id="method-algorithm-algorithmplaybackcontroller-canopensubroutineatcurrentstep"></a>

Used by the pseudocode UI to decide when the open-sub-routine affordance should appear.

### `GraphAlgorithmRegistry`

<a id="cls-algorithm-graphalgorithmregistry"></a>

The central catalog of algorithms exposed in the application.

**What it does**

- constructs the list of built-in algorithm instances,
- groups them by family in a fixed order,
- and exposes them to the run dialog.

If you add a new algorithm and do not register it here, the UI will not show it.

### `AlgorithmSupport`

<a id="cls-algorithm-algorithmsupport"></a>

Shared helper routines used by multiple algorithms.

**Key helpers**

- `neutralStep(...)` — create a default scope-colored step.
- `distanceStep(...)` / `distanceStepWithSubroutine(...)` — build shortest-path style steps.
- `topologicalOrder(...)` — helper used by DAG-oriented algorithms.
- `findCycles(...)` and `findBidirectionalEdgeWarnings(...)` — validation support for DAG checks.
- `hasNegativeCycle(...)` — used by algorithms that need a cycle warning.

### `AlgorithmPalette`

<a id="cls-algorithm-algorithmpalette"></a>

Shared RGB constants for playback visuals.

This class ensures that all algorithms use the same meaning for colors such as:

- neutral scope,
- discovered,
- frontier,
- active,
- finished,
- accepted,
- rejected,
- tree edge,
- and path edge.

### `AlgorithmType`

<a id="cls-algorithm-algorithmtype"></a>

The filter category used in the algorithm picker.

Current values:

- `APSP`
- `SSSP`
- `MST`
- `MAXIMUM_FLOW`
- `GENERAL`
- `ALL_ALGORITHMS`

### `GraphOrientationRequirement`

<a id="cls-algorithm-graphorientationrequirement"></a>

Declares whether an algorithm expects directed graphs, undirected graphs, or either.

This is used by the UI to warn the user before a run starts.

### `EdgeKey`

<a id="cls-algorithm-edgekey"></a>

A stable edge identifier built from source and destination ids.

It is used anywhere the UI needs to key edge coloring or edge annotations without depending on object identity.

## Visual payload classes

### `MatrixSnapshot`

<a id="cls-algorithm-matrixsnapshot"></a>

Immutable matrix payload used by matrix-based algorithms.

**Contains**

- row labels,
- column labels,
- cell text,
- and highlighted row/column indices.

The matrix dialog renders this payload directly.

### `ResidualGraphSnapshot`

<a id="cls-algorithm-residualgraphsnapshot"></a>

Immutable residual-network payload used by maximum-flow algorithms.

**Contains**

- vertex ids,
- vertex labels,
- vertex positions,
- residual edges with capacities,
- and a set of highlighted edges.

### `LevelGraphSnapshot`

<a id="cls-algorithm-levelgraphsnapshot"></a>

Immutable level-graph payload used by Dinic.

**Contains**

- vertex ids,
- vertex labels,
- vertex positions,
- BFS level values,
- and the residual edges that respect the level graph.

## Sub-routine support

### `SubroutineLibrary`

<a id="cls-algorithm-subroutinelibrary"></a>

A catalog of canonical CLRS-style sub-routines.

It provides stable ids and canonical pseudocode for routines such as:

- `RELAX`
- `DFS_VISIT`
- `BFS_LAYER`
- `AUGMENT`

Algorithms use these ids when they want the pseudocode window to offer an expandable sub-routine tab.

### `SubroutineTimeline`

<a id="cls-algorithm-subroutinetimeline"></a>

Immutable definition of one replayable sub-routine.

It stores:

- the sub-routine id,
- the display name,
- the pseudocode lines,
- and the step list to show when the sub-routine is opened.

### `SubroutineStepCollector`

<a id="cls-algorithm-subroutinestepcollector"></a>

Helper used by algorithms to collect sub-routine steps while they execute.

**Typical flow**

1. Create the collector before the run.
2. Add sub-routine steps while building the main timeline.
3. Build a `SubroutineTimeline` at the end of the run.
4. Wrap that timeline into the maps stored in `AlgorithmResult`.

**Why it exists**

It keeps call-site steps and sub-routine body steps aligned without duplicating the logic in every algorithm.

## Package-level extension guide

### When to add a new class here

Add a class to the core package when it is:

- shared by multiple algorithm families,
- part of the run/playback contract,
- or a visual payload that more than one dialog can consume.

### When to add a new subpackage instead

Add a subpackage when the code is specific to a family of algorithms, such as shortest paths, maximum flow, or recursion visualization.

