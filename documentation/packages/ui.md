# `ui` Package

<a id="pkg-ui"></a>

The root `ui` package contains the main Swing application shell, the graph editor controller, the drawing canvas, and the workspace coordination layer.

This is the package that turns the model and algorithms into an interactive desktop application.

## Package responsibilities

- host the main application window,
- manage editing state and selection,
- render the graph canvas,
- coordinate algorithm playback and dialog synchronization,
- and provide utilities used by dialogs and extras handlers.

## How this package connects to the rest of the system

- consumes `graph` for live editing state,
- consumes `algorithm` for algorithm playback,
- consumes `ui.dialogs` for secondary windows,
- consumes `ui.extras` for selection-scoped actions,
- and consumes `ui.templates` for premade graphs.

## `GraphEditorFrame`

<a id="cls-ui-grapheditorframe"></a>

The main application window and composition root.

**Responsibilities**

- create and wire the controller, canvas, playback controller, workspace, and dialogs,
- expose toolbar actions and keyboard shortcuts,
- launch algorithm runs,
- synchronize playback-related views,
- and manage workspace layout changes.

**Important flow**

This class is where the pseudocode window gets its callbacks for opening and closing sub-routines.

**Subsystems inside this class**

- **Toolbar and tool mode controls** (`Select`, `Add Vertex`, `Connect`, `Delete`, `Pan`)
- **Selection actions panel** (coloring, extras, template loading)
- **Playback controls** (run, step back, stop, step forward)
- **Workspace mode coordination** (tabbed vs docked split)
- **Dialog synchronization** (pseudocode, matrix, stack, residual, level, topological)

**Lifecycle hooks to know**

- playback change listener drives `syncAllViews()` style updates,
- pseudocode dialog close requests map to playback stop behavior,
- sub-routine open/close requests route back into playback controller overlay APIs.

**Key collaborators**

- `GraphEditorController`
- `GraphCanvasPanel`
- `AlgorithmPlaybackController`
- `AlgorithmWorkspacePanel`
- `SelectionExtrasDialog`
- `AlgorithmPseudocodeDialog`

## `GraphEditorController`

<a id="cls-ui-grapheditorcontroller"></a>

The application-level controller for editing state.

**Responsibilities**

- own the mutable `DirectedGraph`, positions, and selection state,
- provide clipboard copy/paste,
- manage undo and manual edge bends,
- apply vertex/edge colors,
- load premade templates,
- and generate `AlgorithmRequest` objects.

### State ownership boundaries

The controller is the owner of these mutable states:

- graph structure (`DirectedGraph`),
- vertex positions,
- manual edge bends,
- selected vertices and selected edge,
- undo history snapshots,
- clipboard payload + paste offset counter,
- edge creation mode (directed vs undirected paired).

The canvas should **not** own canonical graph state; it only visualizes and forwards interaction intent.

### Selection and scope model

The controller exposes multiple scope APIs with different semantics:

- `getSelectedScopeVertices()` / `getSelectedScopeEdges()` for exact selected subgraph actions,
- `getAlgorithmScopeVertices()` and algorithm request builders for run scopes,
- mode-aware edge collapsing helpers for undirected paired behavior.

This distinction is central to preventing unintended whole-graph operations.

### Undo and snapshot model

Undo stores controller-level snapshots of graph, positions, and bends.

- Graph-modifying operations call `notifyModified()`.
- The controller captures stable snapshots and trims history depth.
- Restore paths rebuild graph/model state and then notify listeners.

### Copy/paste behavior details

- Copy can originate from selected vertices or a single selected edge.
- Clipboard stores relative coordinates around a centroid.
- Paste allocates new ids and offsets each paste so duplicates remain visible.
- Edge bends are preserved when copied edge geometry includes a manual control offset.

### Important methods and flows

#### `copySelectionToClipboard()`

<a id="method-ui-grapheditorcontroller-copyselectiontoclipboard"></a>

Copies the current selection or selected edge into the internal clipboard.

It stores:

- copied vertices,
- copied edges,
- relative positions,
- weights,
- colors,
- and bend offsets.

#### `pasteClipboard()`

<a id="method-ui-grapheditorcontroller-pasteclipboard"></a>

Creates new vertices and edges from the clipboard content.

The paste logic offsets successive pastes so repeated Ctrl+C / Ctrl+V actions do not overlap exactly.

#### `applyVertexColors(...)` and `applyEdgeColors(...)`

<a id="method-ui-grapheditorcontroller-applyvertexcolors"></a>
<a id="method-ui-grapheditorcontroller-applyedgecolors"></a>

Used both by extras actions and by any future feature that wants to recolor the current graph persistently.

#### `createAlgorithmRequest(...)`

<a id="method-ui-grapheditorcontroller-createalgorithmrequest"></a>

Builds the immutable request object passed into algorithm runs.

#### `loadGraphTemplate(...)`

<a id="method-ui-grapheditorcontroller-loadgraphtemplate"></a>

Clears the current graph and loads a premade template into the live editor model.

### Why this controller matters

It is the central place where user actions become graph mutations. If a feature changes the actual graph, it usually belongs here.

## `GraphCanvasPanel`

<a id="cls-ui-graphcanvaspanel"></a>

The interactive canvas for drawing and editing the graph.

**Responsibilities**

- paint the grid, edges, vertices, labels, and playback overlays,
- handle mouse and keyboard interaction,
- and forward graph edits to `GraphEditorController`.

**Important behavior**

- It reads the current `AlgorithmStep` from the playback controller and paints that state on top of the live graph.
- It supports selection, pan, zoom, vertex dragging, edge bending, and tool-based editing.

### Input model by tool

- **Select**: click/drag selection, move vertices, edit edge bend handles.
- **Add Vertex**: create vertices at click coordinates.
- **Connect**: choose source then destination and create edges using pending defaults.
- **Delete**: remove selected targets.
- **Pan**: move camera transform without mutating graph model.

### Rendering stack

1. draw background grid,
2. apply world transform (pan/zoom),
3. draw edges (base + playback overlays + labels + selection handles),
4. draw vertices (base + playback overlays + annotation badges),
5. draw screen-space selection rectangle.

### Overlay precedence

Playback colors and labels override base model colors for the active step. This is why algorithms can temporarily recolor edges/vertices without mutating the persistent graph colors.

## `AlgorithmWorkspacePanel`

<a id="cls-ui-algorithmworkspacepanel"></a>

The host for all secondary algorithm views.

**Responsibilities**

- show/hide the pseudocode, matrix, stack, residual, level, and topological-order panels,
- keep the panels synchronized with the playback controller,
- and support both tabbed and docked layouts.

**Why it exists**

It lets the same view components work in both floating and embedded modes without duplicating the rendering logic.

### Synchronization contract

`syncAll(playback)` is the central update entry point. It decides which tabs are visible based on the current step payload and playback state.

### Tab policy

- tabs appear only when their payload exists,
- tabs can be embedded or popped out,
- pseudocode tab remains the primary always-on playback companion.

## Extension checklist for this package

1. Decide whether the feature is model mutation (`GraphEditorController`) or view behavior (`GraphCanvasPanel` / dialogs).
2. Keep `GraphEditorFrame` as orchestration glue, not heavy computation.
3. Reuse playback payloads instead of introducing ad-hoc side-state in views.
4. Add method-level anchors in docs when introducing major new workflows.

## Package-level design choices

- The controller owns mutation; the canvas owns interaction and drawing.
- Playback state is separate from editing state.
- Views paint from state instead of storing their own copy of the graph.
- Clipboard and undo are managed centrally so shortcuts stay consistent.
