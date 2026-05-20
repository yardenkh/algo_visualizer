# Package Purpose

Single-source shortest path and traversal algorithms. Combines BFS/DFS traversal and shortest-path routines driven from a source vertex. Implementations follow the shared timeline model and expose CLRS-style pseudocode lines for synchronized playback.

# Package Mental Model

- **Source-Driven**: All algorithms start from a single source vertex and compute reachability, distances, or traversal order relative to that source.
- **Timeline Visualization**: Each algorithm emits steps highlighting edge inspection, vertex discovery, distance updates, and relaxation decisions.
- **Subroutine Support**: Algorithms like Dijkstra and Bellman-Ford use RELAX subroutine overlays. DFS uses DFS-VISIT. Each call site can expand to show subroutine execution.
- **Graph Orientation Flexibility**: BFS/DFS work on any orientation. Dijkstra and Bellman-Ford assume directed. Validation catches orientation mismatches.
- **DAG-Specific Variants**: DAG shortest path uses topological order for efficient single-pass relaxation. Validation checks for cycles in DAG algorithms.
- **Cycle Detection**: Bellman-Ford detects and reports negative cycles. Output indicates whether shortest paths are valid or cycle-poisoned.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | `AlgorithmRequest` with scoped graph, source vertex |
| Outputs | `AlgorithmResult` with timeline of steps; optional subroutine timelines (RELAX, DFS-VISIT) |
| Owns | Algorithm implementations (BFS, DFS, Dijkstra, Bellman-Ford, DAG, topological sort) |
| Depends On | `algorithm` (core interfaces, request/result), `algorithm.recursion` (stack frame adapters for DFS) |
| Must NOT Depend On | `ui`, `algorithm` subpackages other than recursion |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `BreadthFirstSearchAlgorithm` | BFS traversal | `algorithm` helpers | Registry | LOW | Standard BFS; emits steps for each level |
| `DepthFirstSearchAlgorithm` | DFS traversal with stack frames | `algorithm`, `recursion` adapters | Registry | MEDIUM | Emits recursive call stack visualization; complex timeline |
| `DijkstraShortestPathAlgorithm` | Dijkstra with RELAX subroutine | `algorithm`, `SubroutineLibrary` | Registry | MEDIUM | Non-negative weights; RELAX calls visible in timeline |
| `BellmanFordAlgorithm` | Bellman-Ford with negative cycle detection | `algorithm`, `SubroutineLibrary` | Registry | MEDIUM | Handles negative edges; detects negative cycles |
| `DagShortestPathAlgorithm` | Topological relaxation for DAGs | `algorithm`, `DagValidation` | Registry | LOW | Requires DAG input; validation enforces |
| `TopologicalOrderAlgorithm` | Kahn-style topological sort | `algorithm` | Registry | LOW | Produces ordered vertex list; no distances |
| `DagValidation` | Shared utility for cycle detection | - | Multiple DAG algorithms | LOW | Reusable DAG checking logic |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| `GraphAlgorithm` | Base contract | All SSSP algorithms | STABLE |

# Runtime Flows

## BFS Execution
1. Initialize: mark all unvisited, set source distance to 0, create queue with source.
2. While queue not empty:
   - Dequeue vertex u.
   - For each neighbor v:
     - If unvisited: mark visited, set distance to u.distance+1, enqueue.
     - Emit step highlighting edge (u,v) and distance update.
3. Return timeline.

## DFS Execution
1. Initialize: mark all unvisited, create stack with source.
2. Use recursive or iterative approach:
   - Visit vertex u: mark visited, emit step for discovery.
   - For each neighbor: if unvisited, visit recursively.
   - Emit step for finish time.
3. Stack frame adapter captures recursion depth for visualization.
4. Return timeline with stack frames.

## Dijkstra Execution
1. Initialize: all distances to ∞ except source 0, create min-heap with source.
2. While heap not empty:
   - Extract min-distance vertex u.
   - For each neighbor v:
     - If edge (u,v).weight + u.distance < v.distance:
       - Call RELAX(u, v, weight) subroutine.
       - Update v.distance, decrease v in heap.
       - Emit step with relaxation result.
3. Return timeline with subroutine timelines.

## Bellman-Ford Execution
1. Initialize: all distances to ∞ except source 0.
2. For i = 1 to |V|-1:
   - For each edge (u,v):
     - Call RELAX(u, v, weight).
     - Update if distance improves.
     - Emit step.
3. Check for negative cycle:
   - For each edge (u,v):
     - If u.distance + weight < v.distance: negative cycle found.
     - Mark distances as invalid.
4. Return timeline with cycle indicator.

## DAG Shortest Path Execution
1. Validate: confirm graph is acyclic (calls `DagValidation.validate()`).
2. Compute topological order.
3. For each vertex in topological order:
   - For each outgoing edge (u,v):
     - Call RELAX(u, v, weight).
     - Emit step.
4. Return timeline.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| Vertex state (during SSSP) | Distance, parent, visited flag | `distance`, `parent`, `visited` | distance >= 0; parent is predecessor in shortest path tree |

# Hidden Assumptions

- **Source Vertex Always Valid**: Validation ensures source vertex is in request scope before algorithm runs.
- **No Self-Loops in Traversal**: BFS/DFS may behave unexpectedly with self-loops. Algorithms assume simple graphs.
- **Dijkstra Assumes Non-Negative**: Dijkstra is only correct with non-negative edge weights. No validation enforces this; incorrect results silently if negative weights present.
- **DAG Assumption Validated**: DAG algorithms check for cycles explicitly; other algorithms don't. Assumption must be validated before run.
- **Negative Cycles Detected by Bellman-Ford Only**: Other algorithms do not detect negative cycles; Bellman-Ford does and marks distances as invalid.

# Concurrency Notes

Package is single-threaded. Algorithm execution is synchronous on EDT. No concurrency within algorithms.

# Performance Notes

- **BFS**: O(V + E) time, O(V) space. Very fast even for large graphs.
- **DFS**: O(V + E) time, O(V) space (stack depth). Slightly slower than BFS due to recursion.
- **Dijkstra**: O((V + E) log V) with binary heap. Acceptable for <1000 vertex graphs.
- **Bellman-Ford**: O(V * E) time. Slow for dense graphs; <100 vertices recommended for UI.
- **DAG Shortest Path**: O(V + E) time. Very fast; efficient for DAGs.

# Modification Guide For AI Assistants

## Safest Changes

- **Add new traversal variant**: Create new algorithm class, implement `GraphAlgorithm`, register in registry. No changes to framework needed.
- **Enhance step annotations**: Add more detail to vertex/edge annotations (e.g., "distance=5, updated").
- **Add new subroutine overlay**: If new subroutine is meaningful (e.g., new SEARCH helper), add to `SubroutineLibrary` and emit overlay steps.

## Dangerous Changes

- **Do NOT modify Dijkstra to use negative edges**: Algorithm correctness depends on non-negative assumption. Add validation instead.
- **Do NOT add required source vertex to BFS/DFS**: These can run on full graph with multiple sources if needed. Keep source optional in base implementation.
- **Do NOT remove DAG cycle check**: Topological sort and DAG-SP need acyclicity. Removing check silently breaks these algorithms.

## Preferred Patterns

- **Use `SubroutineStepCollector`** for RELAX/subroutine steps.
- **Emit steps frequently**: Many small steps provide more granular visualization than few large steps.
- **Use `AlgorithmSupport` helpers**: Edge lookup, color/annotation builders are reusable.
- **Validate early**: Check orientation, source vertex, graph acyclicity before heavy computation.

# Typical Change Recipes

### Add New SSSP Algorithm Variant
1. Create `algorithm.sssp.NewAlgorithmVariant` implementing `GraphAlgorithm`.
2. Implement `getPseudocodeLines()` returning CLRS-style pseudocode.
3. Implement `validate(AlgorithmRequest)` checking source vertex and graph requirements.
4. Implement `run(AlgorithmRequest)`:
   - Initialize distances, parent, visited flags.
   - Main loop: iterate edges/vertices, call RELAX, emit steps.
   - Return result with timeline.
5. Register in `GraphAlgorithmRegistry`.

### Add RELAX Subroutine to New Algorithm
1. In algorithm's `run()`, use `SubroutineStepCollector`:
   ```java
   SubroutineStepCollector collector = new SubroutineStepCollector(
       SubroutineLibrary.RELAX, SubroutineLibrary.RELAX_PSEUDOCODE
   );
   // ... relax logic, add steps to collector
   SubroutineTimeline relaxTimeline = collector.build();
   ```
2. When calling RELAX in main loop, attach ID to main step:
   ```java
   mainStep = new AlgorithmStep(..., colors, annotations, SubroutineLibrary.RELAX);
   ```
3. Include subroutine timelines in result.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm` | **Implements** | Concrete implementations of `GraphAlgorithm` |
| `algorithm.recursion` | **Uses** | DFS uses stack frame adapters for visualization |
| `ui` | **Consumed By** | Algorithm results are displayed in playback UI |

# AI Quick Reference

## Entry Points
- `BreadthFirstSearchAlgorithm.run(AlgorithmRequest)`
- `DijkstraShortestPathAlgorithm.run(AlgorithmRequest)`
- `BellmanFordAlgorithm.run(AlgorithmRequest)`
- All return `AlgorithmResult` with timeline

## Important Constants
- `SubroutineLibrary.RELAX` — relax subroutine ID used by Dijkstra/Bellman-Ford
- `SubroutineLibrary.DFS_VISIT` — DFS-VISIT subroutine ID used by DFS

## Important Classes
- `DagValidation` — utility for cycle checking


