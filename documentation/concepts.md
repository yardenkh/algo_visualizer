# Cross-Cutting Concepts

<a id="doc-concepts"></a>

This page explains the ideas that appear throughout the project, regardless of package.

## 1. Graph identity and scope

### Vertex identity

Vertices are identified by integer ids. The id is the canonical key used by the graph model, algorithm requests, snapshots, and the UI.

### Selection scope

The editor distinguishes between:

- the **full graph**, which contains every vertex and edge,
- the **algorithm scope**, which is the subset passed to algorithms,
- and the **selected subgraph**, which is the exact selection used by extras actions.

That distinction is important because some operations should use the whole graph, while others should operate only on the selected subgraph.

## 2. Timeline-based playback

Algorithms produce a sequence of `AlgorithmStep` objects. Each step is a frozen visual state, not an instruction to mutate the UI.

A step can carry:

- a pseudocode line number,
- vertex colors,
- edge colors,
- vertex labels,
- edge labels,
- and optional payloads such as matrix, stack, residual-graph, or level-graph snapshots.

The playback controller owns the current index and exposes simple stepping operations.

## 3. Sub-routines

Sub-routines are a first-class part of the playback model.

A sub-routine is represented by:

- a stable sub-routine id,
- a `SubroutineTimeline` containing its own pseudocode and step list,
- and a mapping from main pseudocode line indices to callable sub-routine ids.

This lets the pseudocode viewer show an "open sub-routine" affordance at the correct call sites.

## 4. Visualization payloads

Different algorithm families need different data views.

### Matrix payloads

Used by APSP and reachability algorithms. The payload contains row labels, column labels, cell text, and a highlighted row/column.

### Stack payloads

Used by recursive algorithms. The payload contains a stack of call frames rendered top-first.

### Residual-graph payloads

Used by maximum-flow algorithms. These payloads expose residual edges, highlighted augmenting edges, and graph positions.

### Level-graph payloads

Used by Dinic. These payloads expose BFS levels and the level-respecting residual edges.

## 5. Pseudocode as a UI contract

Algorithms return ordered pseudocode lines. The UI uses those lines for highlighting and for the pseudocode popup.

The pseudocode text is part of the public algorithm API because it is what the user reads while stepping through the run.

## 6. Coloring

There are two distinct coloring worlds:

### Algorithm playback colors

These colors are transient. They live in `AlgorithmStep` and are used to visualize a run.

### Editor/model colors

These colors are persistent graph attributes stored on vertices and edges. They remain after the user edits the graph or applies extras actions.

## 7. Undo, clipboard, and template loading

The editor controller owns the user-edit state:

- undo history,
- clipboard copy/paste,
- manual edge bends,
- selected vertex/edge state,
- and premade template loading.

That keeps the canvas mostly focused on interaction and drawing.

## 8. Recommended reading order for newcomers

1. `README.md`
2. `architecture.md`
3. `packages/graph.md`
4. `packages/algorithm.md`
5. `packages/ui.md`
6. the package page for the algorithm family you care about

