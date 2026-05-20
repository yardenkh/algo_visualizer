# `algorithm.coloring` Package

<a id="pkg-algorithm-coloring"></a>

This package contains the exact coloring services used by the Extras actions.

Unlike the playback-oriented algorithm families, these services are direct computations on the selected subgraph. They do not produce a step-by-step timeline.

## Package responsibilities

- compute exact proper vertex and edge colorings,
- validate the input graph for coloring assumptions,
- and return immutable result objects that the UI can map to RGB values.

## How this package connects to the rest of the system

- consumes `graph.Vertex` and `graph.Edge`,
- is invoked from `ui.extras.VertexColoringActionHandler` and `ui.extras.EdgeColoringActionHandler`,
- and feeds back into `ui.GraphEditorController.applyVertexColors(...)` and `applyEdgeColors(...)`.

## `ExactColoringSolver`

<a id="cls-algorithm-coloring-exactcoloringsolver"></a>

An exact branch-and-bound solver with DSATUR-style branching order.

**Responsibilities**

- validate that the graph is simple and suitable for coloring,
- find an optimal coloring,
- and return the color assignment plus the number of colors used.

## `VertexColoringService`

<a id="cls-algorithm-coloring-vertexcoloringservice"></a>

Computes a minimum proper vertex coloring for the selected subgraph.

**Important behavior**

- converts selected edges into an undirected adjacency relation,
- rejects self-loops,
- and returns a mapping from vertex id to color class index.

## `EdgeColoringService`

<a id="cls-algorithm-coloring-edgecoloringservice"></a>

Computes a minimum proper edge coloring for the selected subgraph.

**Important behavior**

- converts edges into a conflict graph,
- rejects self-loop edges,
- and returns a mapping from edge object to color class index.

## Package-level design choices

- These services favor exact solutions over heuristics so the UI output is deterministic.
- The result objects are immutable and easy to transform into palette colors.
- Coloring is separated from playback so extras actions stay fast and straightforward.

