# Package Purpose

All-pairs shortest path (APSP) algorithms. Compute shortest-path distances for all ordered vertex pairs in the selected scope. Each algorithm emits timeline snapshots for matrix and graph overlays showing distance matrix evolution.

# Package Mental Model

- **Pair-Wise Distances**: All algorithms produce distance matrices where entry (i, j) is the shortest path from vertex i to vertex j.
- **Matrix Snapshots**: Steps include `MatrixSnapshot` payloads showing the distance matrix at each stage. UI displays matrix in modal viewer.
- **Educational Implementations**: Algorithms prioritize clarity and traceability over production performance. Multiple variants (Floyd-Warshall, Johnson, recursive doubling, repeated Dijkstra) show different approaches to the same problem.
- **Dynamic Programming vs. SSSP Repeated**: Floyd-Warshall uses DP iteration. Johnson uses SSSP repeated with potential reweighting. Recursive doubling uses matrix multiplication. Each approach is pedagogically interesting.
- **Infinity Representation**: Unreachable pairs show `∞` (Integer.MAX_VALUE - 1) in matrix. Algorithms handle infinity carefully to avoid overflow.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | `AlgorithmRequest` with scoped graph (no source/sink needed; all vertices are sources) |
| Outputs | `AlgorithmResult` with timeline including `MatrixSnapshot` payloads at key points |
| Owns | Algorithm implementations (Floyd-Warshall, Johnson, Recursive Doubling, Matrix Multiplication, Repeated Dijkstra) |
| Depends On | `algorithm` (core interfaces, matrix snapshot), `algorithm.sssp` (Dijkstra may be used by Johnson) |
| Must NOT Depend On | `ui` |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `FloydWarshallAlgorithm` | DP-based APSP | `algorithm` | Registry | LOW | O(V^3) time; classic approach; very clear pedagogically |
| `MatrixMultiplicationApspAlgorithm` | Min-plus matrix multiplication | `algorithm` | Registry | LOW | Shows algebraic approach; O(V^3 log V) with repeated squaring |
| `RecursiveDoublingApspAlgorithm` | Repeated squaring APSP | `algorithm` | Registry | LOW | Variant of matrix multiplication; shows recursion structure |
| `JohnsonAlgorithm` | Dijkstra repeated with reweighting | `algorithm`, `algorithm.sssp.DijkstraShortestPathAlgorithm` | Registry | MEDIUM | Most complex; handles negative edges via reweighting |
| `RepeatedDijkstraAlgorithm` | Runs Dijkstra once per source | `algorithm`, `algorithm.sssp.DijkstraShortestPathAlgorithm` | Registry | LOW | Simple approach; clearly shows repeated SSSP |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| `GraphAlgorithm` | Base contract | All APSP algorithms | STABLE |

# Runtime Flows

## Floyd-Warshall Execution
1. Initialize: D[i][j] = edge weight if edge exists, else ∞, D[i][i] = 0.
2. For k = 0 to V-1:
   - For i = 0 to V-1:
     - For j = 0 to V-1:
       - If D[i][k] + D[k][j] < D[i][j]: update D[i][j].
       - Emit step with matrix snapshot.
3. Return timeline with final matrix.

## Johnson's Algorithm Execution
1. Add artificial source vertex s with zero-weight edges to all vertices.
2. Run Bellman-Ford from s to find potentials h[v].
3. Reweight edges: w'(u,v) = w(u,v) + h[u] - h[v].
4. For each vertex s: run Dijkstra on reweighted graph.
5. Recover original distances: dist[u][v] = dist'[u][v] - h[u] + h[v].
6. Return timeline with all intermediate matrix snapshots.

## Repeated Dijkstra Execution
1. Initialize distance matrix D.
2. For each source vertex s:
   - Run Dijkstra from s to find distances to all targets.
   - Update row s of matrix.
   - Emit step with matrix snapshot.
3. Return timeline.

## Recursive Doubling Execution
1. Initialize: D^(1) = adjacency matrix.
2. For k = 1 to log(V)-1:
   - Compute D^(2k) = D^(k) ⊗ D^(k) (min-plus matrix product).
   - Emit step with matrix snapshot.
3. Return timeline.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| Distance Matrix | 2D array of shortest distances | `[i][j]` = distance from i to j | D[i][i] = 0; D[i][j] >= 0 or ∞; symmetric if undirected |
| `MatrixSnapshot` | Immutable matrix state attached to step | Row/column ordering, cell values | Must correspond to vertex order in request |

# Hidden Assumptions

- **Vertex Ordering**: Matrix rows/columns correspond to sorted vertex list in request. Algorithms assume this fixed order throughout.
- **No Self-Loops**: Algorithms assume no self-loops (d[i][i] = 0). Self-loops would be ignored or cause issues.
- **∞ Representation**: Infinity is `Integer.MAX_VALUE - 1` to avoid overflow. Operations assume this value.
- **Floyd-Warshall Initialization**: Initial distance = edge weight if present, else ∞. No edge = ∞.
- **Johnson's Reweighting**: Potential values (h) must be correctly computed by Bellman-Ford for reweighting to work. Negative cycles in original graph break this.

# Concurrency Notes

Package is single-threaded. Algorithm execution is synchronous on EDT. No concurrency.

# Performance Notes

- **Floyd-Warshall**: O(V^3) time; unavoidable for DP approach. Acceptable for <200 vertices.
- **Johnson**: O(V(V log V + E)) time with Dijkstra. More efficient for sparse graphs than Floyd-Warshall.
- **Repeated Dijkstra**: O(V(V log V + E)) time. Same complexity as Johnson; simpler implementation.
- **Matrix Multiplication**: O(V^3 log V) time with repeated squaring. Not faster than Floyd-Warshall in practice for APSP.
- **Recursive Doubling**: Similar to matrix multiplication; educational variant showing recursion pattern.

# Modification Guide For AI Assistants

## Safest Changes

- **Add new APSP algorithm**: Create new class implementing `GraphAlgorithm`, generate timeline with matrix snapshots, register. No changes to framework needed.
- **Enhance matrix visualization**: Add more annotations to matrix snapshots (e.g., highlight changed cells).
- **Optimize existing algorithms**: Improve performance without changing timeline output.

## Dangerous Changes

- **Do NOT modify matrix row/column ordering**: Ordering must match vertex sort order in request. Changing this will cause visualization mismatches.
- **Do NOT change infinity representation**: Some algorithms may overflow if infinity value changes. Update all places consistently.
- **Do NOT assume V <= some limit**: Code should work for any V in request scope, within performance bounds.

## Preferred Patterns

- **Emit matrix snapshot at key points**: Intermediate steps show algorithm progress clearly.
- **Use sorted vertex list from request**: Never assume vertices are in any particular order; sort if needed.
- **Validate early**: Check orientation (if needed), scope size before heavy computation.

# Typical Change Recipes

### Add New APSP Algorithm
1. Create `algorithm.apsp.NewApspAlgorithm` implementing `GraphAlgorithm`.
2. Implement `run(AlgorithmRequest)`:
   - Initialize distance matrix from adjacency data.
   - Main algorithm loop: update distances, emit steps with `MatrixSnapshot`.
   - Return result.
3. Create `MatrixSnapshot` at key points:
   ```java
   MatrixSnapshot snapshot = new MatrixSnapshot(distanceMatrix, scopeVertices);
   AlgorithmStep step = new AlgorithmStep(..., colors, annotations, snapshot);
   ```
4. Register in `GraphAlgorithmRegistry`.

### Optimize Floyd-Warshall Without Changing Output
1. Identify bottleneck (triple-nested loop).
2. Add optimizations:
   - Early termination if no updates in iteration.
   - Use primitive arrays instead of boxed types.
   - Cache frequently accessed values.
3. Timeline output and matrix snapshots remain identical.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm` | **Implements** | Concrete `GraphAlgorithm` implementations |
| `algorithm.sssp` | **Uses** | Johnson algorithm uses Dijkstra; Repeated Dijkstra runs SSSP |
| `ui` | **Consumed By** | Matrix snapshots displayed by `AlgorithmMatrixDialog` |

# AI Quick Reference

## Entry Points
- `FloydWarshallAlgorithm.run(AlgorithmRequest)`
- `JohnsonAlgorithm.run(AlgorithmRequest)`
- `RepeatedDijkstraAlgorithm.run(AlgorithmRequest)`
- All return `AlgorithmResult` with matrix snapshots

## Important Classes
- `MatrixSnapshot` — immutable distance matrix payload


