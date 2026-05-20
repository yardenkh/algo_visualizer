# Package Purpose

Core graph domain model. Owns the mutable in-memory representation of vertices, directed edges, and adjacency relations. Intentionally lightweight and independent from Swing UI concerns; data model is pure.

# Package Mental Model

- **Mutable Graph Container**: Vertices and edges are stored in memory and can be added/removed dynamically by UI and templates.
- **Vertex Identity via ID**: Each vertex has an immutable unique ID (0-indexed internally, displayed 1-indexed). IDs are the canonical identity across all packages.
- **Adjacency Lists**: Vertices maintain lists of neighbors and outgoing edges for efficient graph traversal.
- **Ephemeral State Fields**: Vertices have temporary fields (distance, visited, discovery time, etc.) used by algorithms. Algorithms read/write these fields as needed; UI clears them after each run.
- **Weighted Directed Edges**: Each edge has a source vertex, destination vertex, weight (cost), and optional color.
- **Scope-Based Algorithm Execution**: Algorithms never modify the live graph; instead, `AlgorithmRequest` specifies a scope (subset of vertices/edges) for the algorithm to operate on.
- **No Swing Dependencies**: Graph package is pure model; no UI dependencies.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | User edits from UI (add/remove vertices, add/remove edges, set weights/colors) |
| Outputs | Graph structure queries (get vertices, get edges, find edge, get neighbors) |
| Owns | Vertex storage, edge storage, adjacency lists, mutable algorithm metadata |
| Depends On | Nothing (pure model; no external dependencies) |
| Must NOT Depend On | `ui`, `algorithm`, Swing components |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `DirectedGraph` | Container for vertices and edges | `Vertex`, `Edge` | `GraphEditorController`, `AlgorithmRequest` | HIGH | Central model object; all graph mutations go through this class |
| `Vertex` | Single graph vertex with adjacency info | - | `DirectedGraph`, algorithms, UI | HIGH | Mutable; ID is immutable. Algorithms use temporary fields heavily. |
| `Edge` | Single directed edge between vertices | `Vertex` | `DirectedGraph`, algorithms, UI | MEDIUM | Immutable endpoints; weight/color are mutable. Used in step overlays. |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| - | Graph package has no interfaces; concrete classes only | - | STABLE |

# Runtime Flows

## Graph Construction (User Editing)
1. User creates new vertex via UI → `GraphEditorController.addVertex()` → `DirectedGraph.addVertex()`.
2. Vertex is stored in `vertices` map by ID.
3. UI records vertex position in controller's `positions` map.

## Edge Creation
1. User draws edge from source to destination → `GraphEditorController.addEdge()`.
2. `DirectedGraph.addEdge()` creates new `Edge` object, adds to `edges` list.
3. Both vertices update their adjacency lists: source adds destination to `neighbors` and adds edge to `outgoingEdges`.
4. Edge is stored at most once per ordered endpoint pair (enforced by `findEdge()` check).

## Algorithm Execution Preparation
1. User selects subset of vertices/edges and chooses algorithm.
2. `AlgorithmRequest` is built from current graph, selected vertices, selected edges, and source/sink vertices.
3. Request is **copied**; algorithm never sees live graph mutations.
4. Algorithm operates only on request; live graph remains unchanged.

## Vertex State Usage by Algorithms
1. Before algorithm runs, vertex fields are cleared (visited=false, distance=∞, etc.).
2. During algorithm execution, temporary fields are read/written (e.g., distance updates, coloring).
3. After algorithm completes, UI does NOT clear fields. If user runs another algorithm, fields are overwritten.
4. User can see final state of temporary fields in UI (e.g., final distances after Dijkstra).

## Undo/Snapshot
1. After each user action, `GraphEditorController` snapshots current graph state.
2. Snapshot includes vertices, edges, and positions.
3. Undo stack holds up to 200 snapshots.
4. User presses Ctrl+Z → controller restores graph from snapshot.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| `DirectedGraph` | Container for graph data | `vertices` (Map<Integer, Vertex>), `edges` (List<Edge>) | No duplicate vertices by ID; at most one edge per ordered endpoint pair |
| `Vertex` | Single vertex | `id` (immutable), `name`, `color`, `neighbors`, `outgoingEdges`, `visited`, `distance`, `parent`, `discoveryTime`, `finishTime`, `component` | ID is unique within graph; neighbors and outgoingEdges must be consistent |
| `Edge` | Single edge | `source`, `destination` (immutable), `weight`, `color` | Endpoints must not be null; weight should be non-negative (though not enforced) |

# Hidden Assumptions

- **At Most One Edge Per Ordered Pair**: `DirectedGraph.findEdge(source, dest)` returns at most one edge. If you try to add an edge that already exists, behavior is undefined (implementation may overwrite or ignore).
- **Vertex ID Uniqueness**: Vertex IDs are globally unique within a graph. No two vertices can have the same ID.
- **Temporary Fields Reset by User**: Algorithms leave temporary fields in `Vertex` (distance, visited, etc.) populated. UI does not automatically clear these. If user wants clean state, they must reload graph or select "Clear Selections."
- **Edge Weight Sign Assumptions**: Graph does not enforce non-negative weights, but some algorithms (Dijkstra) assume non-negative weights and will produce wrong results with negative edges. Validation happens in algorithm, not here.
- **Graph Mutability**: Live graph can change at any time. Code that caches graph state without copying should be careful; changes are visible immediately.
- **Mutable vs. Immutable**: Vertex and Edge objects themselves are mutable (weight, color, temporary fields can change). However, the endpoint references in an Edge are immutable.

# Concurrency Notes

Package is effectively single-threaded. All access is from the Event Dispatch Thread (EDT). No synchronization. Graph mutations during algorithm execution will cause undefined behavior; algorithms must use immutable `AlgorithmRequest` snapshot instead.

# Performance Notes

- **Vertex Lookup**: O(1) via ID in `vertices` map.
- **Edge Lookup**: O(degree) linear search in outgoing edges list. Consider using adjacency map if algorithms need frequent edge lookups.
- **Graph Mutations**: O(1) for add vertex, O(degree) for add/remove edge.
- **Memory**: Small graphs (<100 vertices) consume <1 MB. Large graphs (1000 vertices) consume ~5-10 MB (depends on edge density).

# Modification Guide For AI Assistants

## Safest Changes

- **Add new query methods**: New methods like `getVertexsByProperty()`, `getEdgesByWeight()` are safe. No existing code is affected.
- **Add optional vertex/edge metadata fields**: New fields like `userData`, `userColor`, etc. are safe if they start as null/default and don't affect core logic.
- **Extend traversal helpers**: Methods like `getReachableVertices()`, `getConnectedComponents()` are safe educational utilities.

## Dangerous Changes

- **Do NOT modify vertex ID assignment**: IDs are immutable and referenced everywhere. Changing ID assignment will break reproducibility and algorithm correctness.
- **Do NOT add required fields to Vertex or Edge**: All new fields should be optional. Existing code that instantiates vertices/edges will break.
- **Do NOT change adjacency list representation**: Algorithms may iterate over neighbors or outgoingEdges. Changing data structure without updating all uses will break.
- **Do NOT remove or rename existing public methods**: Code depends on stable API. If you need to deprecate, add new method and keep old one.
- **Do NOT change the at-most-one-edge-per-pair invariant**: Many algorithms assume this; breaking it causes undefined behavior.

## Preferred Patterns

- **Use vertex ID for identity**: Never use object references as keys across packages. Always use vertex ID.
- **Copy graph state if needed**: If code needs to modify graph for temporary purposes, build a new DirectedGraph instead of mutating live one.
- **Query via getters**: Use public `getVertices()`, `getEdges()`, etc. rather than accessing internal maps directly.
- **Validate input early**: Check for null vertices/edges, invalid IDs, etc. before processing.

## Anti-Patterns

- **Storing direct references to vertices across algorithm runs**: Vertices are mutable; references will show updated state from subsequent algorithms.
- **Assuming edge indices**: Edge list order is not guaranteed. Always search by endpoints, not index.
- **Modifying vertex temporary fields without clearing them**: Leave the graph in a clean state so next algorithm doesn't inherit stale data.

# Typical Change Recipes

### Add a New Vertex Property
1. Add new field to `Vertex` class (e.g., `private int customValue;`).
2. Add getter and setter (e.g., `public int getCustomValue()`, `public void setCustomValue(int v)`).
3. Update `Vertex` constructor or add initialization in getter if lazy-init is desired.
4. Algorithms and UI can now read/write this field as needed.

### Query Vertices by Property
1. Add method to `DirectedGraph`:
   ```java
   public List<Vertex> getVerticesByProperty(Predicate<Vertex> predicate) {
       return vertices.values().stream().filter(predicate).toList();
   }
   ```
2. Callers use: `graph.getVerticesByProperty(v -> v.getDistance() < 10)`.

### Deep Copy Graph
1. Create new `DirectedGraph` in caller.
2. For each vertex in original, create new vertex with same ID and properties.
3. For each edge in original, create new edge between corresponding new vertices.
4. This is safe for temporary graph manipulation (e.g., graph transforms in extras).

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm` | **Consumed By** | Algorithms read graph structure via immutable `AlgorithmRequest` snapshots. Never direct access to live graph. |
| `ui` | **Consumed By** | `GraphEditorController` manages graph on behalf of UI. Mutations originate from user input. |
| `ui.templates` | **Consumed By** | Templates materialize as vertices/edges added to live graph. |
| `ui.extras` | **Consumed By** | Graph transforms (complement graph, line graph) create new graphs. |

# AI Quick Reference

## Entry Points
- `DirectedGraph()` — constructor for new empty graph
- `DirectedGraph.addVertex(Vertex)` — add vertex
- `DirectedGraph.addEdge(Edge)` — add edge
- `Vertex(int id)` — constructor with auto-generated name "V<id+1>"

## Important APIs
- `DirectedGraph.getVertices()` → `List<Vertex>`
- `DirectedGraph.getEdges()` → `List<Edge>`
- `DirectedGraph.getVertex(int id)` → `Vertex` or null
- `DirectedGraph.findEdge(Vertex source, Vertex dest)` → `Edge` or null
- `Vertex.getId()` → immutable ID
- `Vertex.getName()` → human-readable name
- `Vertex.getColor()` → optional color value
- `Vertex.setColor(int)` → set color
- `Vertex.addNeighbor(Vertex)` → add to adjacency list
- `Vertex.getOutgoingEdges()` → `List<Edge>`
- `Edge.getSource()`, `getDestination()`, `getWeight()`, `getColor()`

## Important Temporary Fields (for Algorithms)
- `Vertex.distance` — used by shortest-path algorithms
- `Vertex.visited` — used by traversal algorithms
- `Vertex.parent` — used by path reconstruction
- `Vertex.discoveryTime`, `finishTime` — used by DFS
- `Vertex.component` — used by connected-component algorithms


