# Package Purpose

General-purpose algorithms that do not belong to a single specialized family. Includes reachability, ordering, APSP conceptual variants, and heuristic search demonstrations. Educational algorithms covering diverse graph problems.

# Package Mental Model

- **Diverse Problem Domain**: Topological sort, transitive closure, A* search, Seidel recursion—each solves different problem.
- **Topological Sort (DAG Ordering)**: Produces linear ordering of vertices where all edges go left-to-right. Foundation for DAG-based algorithms.
- **Transitive Closure (Reachability)**: Computes which vertices are reachable from which; matrix shows 1 if reachable, 0 otherwise. Educational alternative to Floyd-Warshall.
- **A* Search**: Heuristic-guided shortest path finding. Demonstrates that heuristics can guide search while maintaining correctness.
- **Seidel's Algorithm**: Recursive APSP approach for undirected unweighted graphs. Shows divide-and-conquer approach to shortest paths.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | `AlgorithmRequest` with scoped graph; some algorithms require DAG or undirected input |
| Outputs | `AlgorithmResult` with timeline; topological sort includes output vertex list; transitive closure/Seidel include matrix snapshots |
| Owns | Algorithm implementations (Topological Sort, Transitive Closure, A*, Seidel) |
| Depends On | `algorithm` (core interfaces, matrix snapshot), `algorithm.recursion` (Seidel uses stack frame adapters), `algorithm.sssp` (DAG validation) |
| Must NOT Depend On | `ui` |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `TopologicalOrderAlgorithm` | Kahn-style topological sort | `algorithm`, `DagValidation` | Registry | LOW | Produces linear ordering; requires DAG input |
| `TransitiveClosureAlgorithm` | Warshall-style reachability matrix | `algorithm` | Registry | LOW | Educational APSP variant; shows reachability explicitly |
| `AStarAlgorithm` | Heuristic-guided shortest path | `algorithm` | Registry | MEDIUM | Requires user-provided heuristic function; demonstrates A* principles |
| `SeidelAlgorithm` | Recursive APSP for undirected unweighted | `algorithm`, `recursion` adapters | Registry | MEDIUM | Complex recursive approach; emits stack frames |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| `GraphAlgorithm` | Base contract | All general algorithms | STABLE |

# Runtime Flows

## Topological Sort (Kahn's Algorithm) Execution
1. Compute in-degree of each vertex.
2. Initialize queue with all 0-in-degree vertices.
3. While queue not empty:
   - Dequeue vertex u, add to output order.
   - For each neighbor v: decrease in-degree, enqueue if in-degree becomes 0.
   - Emit step showing vertex removal.
4. If output size < V: cycle detected (error case).
5. Return timeline with final topological order.

## Transitive Closure (Warshall's Algorithm) Execution
1. Initialize: C[i][j] = 1 if edge exists, else 0.
2. For k = 0 to V-1:
   - For i = 0 to V-1:
     - For j = 0 to V-1:
       - If C[i][k] and C[k][j]: set C[i][j] = 1.
       - Emit step with matrix snapshot.
3. Return timeline with final reachability matrix.

## A* Search Execution
1. Initialize: g(source) = 0, f(source) = h(source), open set = {source}.
2. While open set not empty:
   - Select node n with minimum f value.
   - If n == target: found shortest path.
   - Expand n: for each neighbor m:
     - g_new = g(n) + edge_weight.
     - If g_new < g(m): update g(m), f(m) = g_new + h(m), add to open set.
     - Emit step showing expansion.
3. Return timeline with path.

## Seidel's Algorithm Execution (Undirected Unweighted)
1. Recursive approach: partition vertices into two sets based on BFS levels.
2. Recursively compute distances within each set.
3. Combine results: distance between sets computed via level analysis.
4. Emit steps and stack frames showing recursion depth.
5. Return timeline with final distance matrix.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| Topological order list | Linear ordering of DAG vertices | Ordered vertex list | Size == V; all edges go left-to-right in ordering |
| Reachability matrix | Transitive closure | C[i][j] = 1 if i can reach j | Reflexive: C[i][i] = 1; transitive: if C[i][j] and C[j][k] then C[i][k] |
| A* open/closed sets | Frontier and visited in search | Open set (priority queue), closed set | Open set ordered by f value; closed set contains expanded nodes |
| Recursion stack (Seidel) | Call stack for recursive calls | Frame snapshots | Stack depth shows recursion level |

# Hidden Assumptions

- **Topological Sort Requires DAG**: Algorithm produces meaningless output if graph has cycles. Validation should check.
- **Transitive Closure on Any Graph**: Works on cyclic graphs; reachability includes paths through cycles.
- **A* Heuristic is Admissible**: If heuristic never overestimates distance, A* finds optimal path. Non-admissible heuristics cause sub-optimal results.
- **Seidel Assumes Unweighted Undirected**: Distance = number of edges. Algorithm works only on unweighted undirected graphs.

# Concurrency Notes

Package is single-threaded. Algorithm execution is synchronous on EDT. No concurrency.

# Performance Notes

- **Topological Sort**: O(V + E) with queue-based approach. Very fast.
- **Transitive Closure**: O(V^3) with Warshall. Same as Floyd-Warshall.
- **A***: O(E log V) with binary heap; depends on heuristic quality. Excellent with good heuristics.
- **Seidel**: O(V^3 / log V) average case for undirected unweighted. Faster than Floyd-Warshall asymptotically but higher constant factors.

# Modification Guide For AI Assistants

## Safest Changes

- **Add new general algorithm**: Create new class implementing `GraphAlgorithm`, follow timeline pattern.
- **Enhance topological order visualization**: Show in-degree labels or layer coloring.
- **Optimize existing algorithms**: Improve performance without changing timeline output.
- **Add new A* variant**: Create new heuristic function, use existing A* as base.

## Dangerous Changes

- **Do NOT remove DAG validation from topological sort**: Validation ensures algorithm preconditions. Removing it silently produces garbage.
- **Do NOT modify Seidel's recursive structure**: Algorithm correctness depends on divide-and-conquer approach. Changes easily break it.
- **Do NOT assume unweighted for non-Seidel algorithms**: Only Seidel requires unweighted input. Others should handle arbitrary weights.

## Preferred Patterns

- **Use DagValidation for cycle checking**: Shared utility prevents duplication.
- **Emit steps frequently**: Intermediate states provide clear visualization.
- **Use stack frame adapters for recursive algorithms**: Seidel-style recursion should use adapter pattern for UI integration.

# Typical Change Recipes

### Add New General Algorithm
1. Create `algorithm.general.NewGeneralAlgorithm` implementing `GraphAlgorithm`.
2. Implement `run(AlgorithmRequest)`:
   - Validate input (orientation, acyclicity if needed, vertex requirements).
   - Main algorithm loop: compute solution, emit steps.
   - Return result.
3. Register in `GraphAlgorithmRegistry`.

### Extend A* with Custom Heuristic
1. Define heuristic function: `float heuristic(Vertex current, Vertex target)`.
2. Create new algorithm class or parameterize existing A*.
3. In A* run loop, use heuristic: `f = g + heuristic(n, target)`.
4. Timeline output shows search exploration with new heuristic.

### Add Matrix Snapshot to Algorithm
1. Compute matrix at key points in algorithm.
2. Create `MatrixSnapshot`:
   ```java
   MatrixSnapshot snapshot = new MatrixSnapshot(matrix, scopeVertices);
   ```
3. Attach to step:
   ```java
   AlgorithmStep step = new AlgorithmStep(..., colors, annotations, snapshot);
   ```

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm` | **Implements** | Concrete `GraphAlgorithm` implementations |
| `algorithm.recursion` | **Uses** | Seidel uses stack frame adapters |
| `algorithm.sssp` | **Uses** | DAG validation utility |
| `ui` | **Consumed By** | Topological order output shown in dialog; matrix snapshots shown by viewers |

# AI Quick Reference

## Entry Points
- `TopologicalOrderAlgorithm.run(AlgorithmRequest)`
- `TransitiveClosureAlgorithm.run(AlgorithmRequest)`
- `AStarAlgorithm.run(AlgorithmRequest)`
- `SeidelAlgorithm.run(AlgorithmRequest)`

## Important Utilities
- `DagValidation` — cycle detection for DAG-only algorithms
- Recursion adapters — for stack frame visualization


