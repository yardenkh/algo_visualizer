# `algorithm.sssp` Package

<a id="pkg-algorithm-sssp"></a>

This package contains the single-source traversal and shortest-path algorithms.

It mixes two closely related ideas:

- **traversals** such as BFS and DFS, which explore a graph from a source,
- and **shortest-path algorithms**, which compute distances from that source.

All implementations use the shared algorithm timeline contract from the parent `algorithm` package.

## Package responsibilities

- define the canonical source-based graph-search algorithms,
- emit clear step-by-step timeline snapshots,
- expose CLRS-style pseudocode for synchronized playback,
- and register sub-routine call sites where the UI should allow expansion.

## How this package connects to the rest of the system

- consumes `algorithm.AlgorithmRequest`,
- produces `algorithm.AlgorithmResult`,
- uses `algorithm.AlgorithmSupport` for helper logic and shared step builders,
- and, for recursive search, uses `algorithm.recursion` adapters to display stack frames.

## `BreadthFirstSearchAlgorithm`

<a id="cls-algorithm-sssp-breadthfirstsearchalgorithm"></a>

Breadth-first search is the traversal baseline in this project.

**What it shows**

- queue/frontier behavior,
- discovery order,
- tree edges,
- and distance-from-source labels.

**Important behavior**

- Starts at the chosen source vertex.
- Continues into disconnected components when the queue becomes empty.
- Updates the visual state on every major queue or edge event.

**Why it matters**

BFS is the simplest example of the project’s playback pattern: a small state machine that turns algorithm events into visible steps.

## `DepthFirstSearchAlgorithm`

<a id="cls-algorithm-sssp-depthfirstsearchalgorithm"></a>

Depth-first search demonstrates recursion and stack visualization.

**What it shows**

- discovery and finish times,
- active recursion path,
- tree edges and back edges,
- and a recursion stack rendered through `algorithm.recursion.StackFramesSnapshot`.

**Sub-routine integration**

DFS is one of the best examples of the sub-routine system. It marks `DFS-VISIT(u)` call sites and builds a reusable sub-routine timeline for the pseudocode window.

**Important collaborators**

- `algorithm.recursion.DfsStackFrameAdapter`
- `algorithm.SubroutineStepCollector`
- `algorithm.SubroutineLibrary.DFS_VISIT`

## `DijkstraShortestPathAlgorithm`

<a id="cls-algorithm-sssp-dijkstrashortestpathalgorithm"></a>

Dijkstra computes shortest paths from a source on non-negative weighted graphs.

**What it shows**

- tentative distance labels (`g=` / `dist=` style annotations),
- settled vertices,
- frontier vertices,
- priority-queue relaxations,
- and the relaxed predecessor tree.

**Sub-routine integration**

This algorithm uses `RELAX` as a callable sub-routine. The main timeline marks the call site, and the sub-routine timeline expands the individual relaxation logic in the pseudocode viewer.

**Implementation notes**

- Stale priority-queue entries are explicitly skipped in the timeline.
- The algorithm refuses negative-weight scopes during validation.

## `BellmanFordAlgorithm`

<a id="cls-algorithm-sssp-bellmanfordalgorithm"></a>

Bellman-Ford supports negative weights and detects negative cycles.

**What it shows**

- repeated passes over all edges,
- each relaxation attempt,
- stability of distances,
- and the final negative-cycle verification pass.

**Sub-routine integration**

Bellman-Ford also uses `RELAX` as a reusable sub-routine timeline, which keeps it aligned with Dijkstra’s presentation.

**Important behavior**

- It tolerates negative edges.
- It returns a special warning when a negative cycle appears reachable from the source.

## `DagShortestPathAlgorithm`

<a id="cls-algorithm-sssp-dagshortestpathalgorithm"></a>

Computes shortest paths in a DAG by processing vertices in topological order.

**What it shows**

- the computed topological order,
- distance initialization,
- one relaxation pass per outgoing edge,
- and the final settled distances.

**Validation**

Uses `DagValidation` to reject scopes that are not DAG-like enough for the algorithm’s assumptions.

## `DagValidation`

<a id="cls-algorithm-sssp-dagvalidation"></a>

Shared validation helper for DAG-oriented algorithms.

**What it checks**

- cycle hints,
- bidirectional edge warnings,
- and user-facing guidance text.

This class exists so DAG-based algorithms do not duplicate the same validation formatting.

## Package-level design choices

- Textbook terminology is preserved so the pseudocode lines feel familiar.
- Distance labels are formatted consistently across algorithms.
- The UI sees immutable results, not live mutable algorithm variables.
- Sub-routines are reused rather than duplicated in every algorithm.

## Reading order for this package

1. `BreadthFirstSearchAlgorithm`
2. `DepthFirstSearchAlgorithm`
3. `DijkstraShortestPathAlgorithm`
4. `BellmanFordAlgorithm`
5. `DagShortestPathAlgorithm`
6. `DagValidation`



