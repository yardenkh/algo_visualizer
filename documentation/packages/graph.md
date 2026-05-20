# `graph` Package

<a id="pkg-graph"></a>

The `graph` package contains the mutable in-memory graph model used by the editor and all algorithms.

It is intentionally small and independent from Swing. The rest of the project builds on top of these classes.

## Package responsibilities

- store vertices and directed edges,
- maintain adjacency information,
- expose structural mutation and query operations,
- and preserve the temporary metadata algorithms need during playback.

## How this package connects to the rest of the system

- `ui.GraphEditorController` mutates these objects while the user edits the graph.
- `algorithm.AlgorithmRequest` captures a stable snapshot of these objects for algorithm runs.
- `ui.templates.GraphTemplateLibrary` materializes templates into this model.

## `DirectedGraph`

<a id="cls-graph-directedgraph"></a>

The graph container.

**Responsibilities**

- store vertices by id,
- store graph-wide edge list,
- keep endpoint adjacency structures synchronized,
- and provide add/remove/query operations.

**Important behavior**

- Vertex ids are canonical identifiers.
- The graph stores at most one edge per ordered endpoint pair.
- Adding an edge automatically adds missing endpoint vertices.
- Removing a vertex removes all incident edges and clears its structural connections.

**Common methods**

- `addVertex(...)`
- `addEdge(...)`
- `removeVertex(...)` / `removeVertexById(...)`
- `getVertex(...)`
- `getEdge(...)`
- `containsVertex(...)`
- `containsEdge(...)`
- `getVertices()` / `getEdges()`

### Why the graph is mutable

The editor needs direct mutation for interactive editing. The algorithm layer protects itself by taking immutable request snapshots instead of working on the live graph directly.

## `Vertex`

<a id="cls-graph-vertex"></a>

A single vertex in the graph.

**What it stores**

- id and display name,
- a persistent color/mark value,
- adjacency lists and outgoing edges,
- and temporary fields such as visited, parent, distance, discovery time, finish time, and component id.

**Why it has extra fields**

Those fields make it easy for algorithms to reuse the same model object during playback demonstrations. Different algorithms can write the metadata they need without introducing separate vertex types.

**Important methods**

- `getId()` / `getName()` / `setName(...)`
- `getColor()` / `setColor(...)`
- `getNeighbors()`
- `getOutgoingEdges()`
- `addNeighbor(...)` / `removeNeighbor(...)`
- `addOutgoingEdge(...)` / `removeOutgoingEdge(...)`
- `disconnectFrom(...)`
- `clearConnections()`
- `reset()`

### Behavioral note

`Vertex` exposes its adjacency lists directly. That keeps the model lightweight, but it also means the graph and controller are responsible for keeping the overall structure consistent.

## `Edge`

<a id="cls-graph-edge"></a>

A directed edge between two vertices.

**What it stores**

- immutable source and destination endpoints,
- weight,
- and color/mark value.

**Important methods**

- `getSource()` / `getDestination()`
- `getWeight()` / `setWeight(...)`
- `getColor()` / `setColor(...)`
- `isLoop()`
- `connects(...)`
- `contains(...)`
- `getOpposite(...)`

### Behavior and design choices

- Endpoint references are immutable after construction.
- Metadata fields are mutable.
- Equality and hash code include endpoints and metadata, which matters when the UI or clipboard compares edges.

## Practical reading guide

If you are trying to understand a graph operation, ask these questions in order:

1. Is the operation mutating the live model or consuming a request snapshot?
2. Is it acting on vertices, edges, or both?
3. Does it need the graph-wide state or only the selected scope?
4. Does it need to preserve positions/colors/edge bends as well as structure?

## Extension notes

Add new behavior here only if it belongs to the core graph model itself. If the logic is user-interaction-specific, place it in `ui.GraphEditorController`. If it is algorithm-specific, use `algorithm`.

