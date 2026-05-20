# `ui.dialogs` Package

<a id="pkg-ui-dialogs"></a>

This package contains the secondary Swing windows and embeddable panels used by the editor.

These dialogs are mostly views. They render algorithm output, configuration choices, or helper content; they do not own the graph model.

## Package responsibilities

- present algorithm configuration and runtime views,
- show matrix, stack, residual, level, and topological order outputs,
- expose embeddable panels for workspace docking,
- and keep the presentation logic separated from the controller and algorithms.

## How this package connects to the rest of the system

- driven by `ui.GraphEditorFrame` and `ui.AlgorithmWorkspacePanel`,
- consumes timeline payloads from `algorithm`,
- and in some cases consumes template or selection data from `ui.templates` or `ui.extras`.

## `AlgorithmRunDialog`

<a id="cls-ui-dialogs-algorithmrundialog"></a>

Modal chooser for selecting an algorithm and optional source/sink vertices.

**What it does**

- filters algorithms by `AlgorithmType`,
- shows algorithm details using `AlgorithmDetailsFormatter`,
- and returns an `AlgorithmRunConfiguration`.

## `AlgorithmRunConfiguration`

<a id="cls-ui-dialogs-algorithmrunconfiguration"></a>

Immutable selection result from the run dialog.

It stores the chosen algorithm, source id, and optional sink id.

## `AlgorithmPseudocodeDialog`

<a id="cls-ui-dialogs-algorithmpseudocodedialog"></a>

The pseudocode viewer and sub-routine host.

**Important behavior**

- shows main pseudocode,
- optionally shows a reusable sub-routine tab,
- highlights the current main or sub-routine line,
- and exposes the "open sub-routine" affordance when the playback controller says a call site is available.

**Key callbacks**

- `setOnCloseRequest(...)`
- `setOnOpenSubroutineRequest(...)`
- `setOnCloseSubroutineRequest(...)`

**Why it matters**

This is the main visual surface for the sub-routine integration model.

## `AlgorithmMatrixDialog`

<a id="cls-ui-dialogs-algorithmmatrixdialog"></a>

Displays `algorithm.MatrixSnapshot` objects in a table.

## `AlgorithmStackFramesDialog`

<a id="cls-ui-dialogs-algorithmstackframesdialog"></a>

Displays `algorithm.recursion.StackFramesSnapshot` objects in a tree view.

## `ResidualGraphDialog`

<a id="cls-ui-dialogs-residualgraphdialog"></a>

Renders `algorithm.ResidualGraphSnapshot` objects for max-flow algorithms.

## `LevelGraphDialog`

<a id="cls-ui-dialogs-levelgraphdialog"></a>

Renders `algorithm.LevelGraphSnapshot` objects for Dinic.

## `TopologicalOrderDialog`

<a id="cls-ui-dialogs-topologicalorderdialog"></a>

Displays the currently revealed topological order and the forward-going arcs between visible vertices.

## `PremadeGraphsDialog`

<a id="cls-ui-dialogs-premadegraphsdialog"></a>

Graph template picker dialog.

It reads from `ui.templates.GraphTemplateLibrary`, groups templates by type, and returns the selected template to the editor.

## `ShortcutsDialog`

<a id="cls-ui-dialogs-shortcutsdialog"></a>

Simple read-only list of keyboard shortcuts supplied by the main frame.

## `SelectionExtrasDialog`

<a id="cls-ui-dialogs-selectionextrasdialog"></a>

Launcher for the selection-scoped extras actions.

It provides a human-friendly list of actions such as vertex coloring, edge coloring, complement graph, line graph, root graph reconstruction, and crossing reduction.

## `AlgorithmListCellRenderer`

<a id="cls-ui-dialogs-algorithmlistcellrenderer"></a>

Small renderer that displays algorithm names cleanly inside combo boxes.

## Package-level design choices

- The same visual component can often be shown as either a popup dialog or an embeddable panel.
- The dialogs do not mutate the graph directly; they show results and request actions from the controller.
- The pseudocode dialog is the coordination point for sub-routine opening and closing.

