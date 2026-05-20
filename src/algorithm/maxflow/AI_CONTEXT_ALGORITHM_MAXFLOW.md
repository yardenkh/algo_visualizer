# Package Purpose

Maximum-flow algorithm implementations and shared support. Implement augmenting-path and blocking-flow algorithms for directed capacitated networks. Timelines include residual-graph and level-graph payloads so users can inspect intermediate flow states visually.

# Package Mental Model

- **Residual Graph Model**: Flow state is represented as residual capacities on edges. Algorithms iteratively find augmenting paths in residual graph and update flow.
- **Capacity and Flow**: Each edge has a capacity (maximum flow). Flow increases with augmentations but cannot exceed capacity.
- **Augmenting Paths**: Paths from source to sink in residual graph with available capacity. Each augmentation increases total flow.
- **Three Approaches**: Ford-Fulkerson (DFS), Edmonds-Karp (BFS), and Dinic (level graph + blocking flow).
- **Subroutine Support**: AUGMENT subroutine overlays show augmenting path finding and flow updating.
- **Visualization Payloads**: Steps include residual-graph and level-graph snapshots so UI can show network state evolution.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | `AlgorithmRequest` with source/sink vertices and weighted directed graph |
| Outputs | `AlgorithmResult` with timeline including residual/level graph snapshots and optional AUGMENT subroutines |
| Owns | Algorithm implementations (Ford-Fulkerson, Edmonds-Karp, Dinic), shared residual/level graph utilities |
| Depends On | `algorithm` (core interfaces, support), `algorithm.sssp` (BFS used by Edmonds-Karp, level-graph construction) |
| Must NOT Depend On | `ui` |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `FordFulkersonAlgorithm` | DFS augmenting paths | `algorithm`, `MaxFlowSupport` | Registry | MEDIUM | Simplest approach; can be slow or incorrect with irrational capacities |
| `EdmondsKarpAlgorithm` | BFS shortest augmenting paths | `algorithm`, `MaxFlowSupport`, `algorithm.sssp` | Registry | MEDIUM | O(VE^2); guaranteed to terminate and find max flow |
| `DinicAlgorithm` | Level graph + blocking flow | `algorithm`, `MaxFlowSupport` | Registry | MEDIUM | O(V^2 E); fastest practical algorithm; emits level-graph snapshots |
| `MaxFlowSupport` | Shared utilities for residual/level graph creation and flow updates | - | All maxflow algorithms | LOW | Non-critical utility; refactor-safe |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| `GraphAlgorithm` | Base contract | All max-flow algorithms | STABLE |

# Runtime Flows

## Ford-Fulkerson (DFS) Execution
1. Initialize: flow = 0 on all edges, residual capacity = edge capacity.
2. While augmenting path exists from source to sink in residual graph:
   - Find path using DFS.
   - Compute bottleneck capacity (min residual capacity on path).
   - Augment: increase flow on path edges, update residual capacities.
   - Emit step with residual-graph snapshot.
3. Return timeline with final max flow.

## Edmonds-Karp (BFS) Execution
1. Similar to Ford-Fulkerson, but find augmenting paths using BFS (shortest path).
2. This guarantees O(VE^2) complexity and correct termination even with irrational capacities.
3. Steps follow same structure with residual-graph snapshots.

## Dinic's Algorithm Execution
1. Build level graph: BFS from source to assign levels; level[s]=0, level[v]=level[u]+1 for edges in BFS tree.
2. While augmenting path exists (level[t] is defined):
   - Build blocking flow: DFS from source finding paths to sink using only level-increasing edges.
   - For each blocking path: augment flow.
   - Rebuild level graph.
   - Emit step with level-graph and residual-graph snapshots.
3. Return timeline with final max flow.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| Flow state | Current flow on each edge | Edge capacity, forward/backward flow | 0 <= flow <= capacity; flow conservation (except source/sink) |
| Residual graph | Capacities in residual network | Forward capacity, backward capacity | Residual capacities are non-negative; path exists iff augmenting path exists |
| Level graph | Vertex levels from BFS | Vertex level from source | Level increases along edges used by blocking flow |

# Hidden Assumptions

- **Non-Negative Capacities**: Algorithms assume capacities are non-negative integers. Negative or fractional capacities cause incorrect results.
- **Single Source/Sink**: Algorithms require exactly one source and one sink.
- **No Self-Loops**: Self-loops are ignored or cause issues.
- **Edge Capacities as Weights**: When building request, edge weights are interpreted as flow capacities.

# Concurrency Notes

Package is single-threaded. Algorithm execution is synchronous on EDT. No concurrency.

# Performance Notes

- **Ford-Fulkerson DFS**: Worst case O(E * max-flow) if flow is large. Practical performance acceptable for small flows.
- **Edmonds-Karp BFS**: O(VE^2) guaranteed; slower than Dinic but predictable.
- **Dinic**: O(V^2 E) time; fastest practical algorithm. Recommended for large networks.
- **Memory**: Storing residual/level graphs for each step can consume significant memory (10-50 MB for 1000 steps).

# Modification Guide For AI Assistants

## Safest Changes

- **Add new max-flow variant**: Create new class implementing `GraphAlgorithm`, follow same timeline pattern with residual/level snapshots.
- **Optimize ford-Fulkerson with heuristics**: Use Edmonds-Karp or Dinic preprocessing instead of pure DFS. Timeline remains similar.
- **Enhance residual-graph visualization**: Add flow/capacity labels or color coding in snapshots.

## Dangerous Changes

- **Do NOT modify residual capacity update logic**: Flow conservation depends on correct residual updates. Off-by-one errors cause incorrect max flow.
- **Do NOT remove source/sink validation**: Max flow requires exactly one source and sink. Removing checks will cause crashes.
- **Do NOT change level-graph construction in Dinic**: Level assignment must be consistent with BFS distance from source. Incorrect assignment breaks algorithm.

## Preferred Patterns

- **Validate non-negative capacities early**: Check edge weights before algorithm runs.
- **Emit residual-graph snapshot frequently**: Show network state evolution clearly.
- **Use AUGMENT subroutine overlay**: Show augmenting path finding and flow updates in subroutine.

# Typical Change Recipes

### Add New Max-Flow Algorithm
1. Create `algorithm.maxflow.NewMaxFlowAlgorithm` implementing `GraphAlgorithm`.
2. Implement `requiresSinkVertex()` returning `true`.
3. Implement `run(AlgorithmRequest)`:
   - Validate source/sink.
   - Initialize residual capacities from edge weights.
   - Main loop: find augmenting paths, augment, emit steps.
   - Emit residual-graph snapshot with each step.
   - Return result.
4. Register in `GraphAlgorithmRegistry`.

### Optimize Dinic with Edge Lists
1. Build level-graph edge lists instead of adjacency matrix.
2. Maintain current-edge pointers for blocking flow DFS (Shimon Even's optimization).
3. Timeline output remains identical; algorithm runs faster.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm` | **Implements** | Concrete `GraphAlgorithm` implementations |
| `algorithm.sssp` | **Uses** | BFS used by Edmonds-Karp and Dinic level-graph construction |
| `ui` | **Consumed By** | Residual/level graph snapshots displayed by `ResidualGraphDialog` and `LevelGraphDialog` |

# AI Quick Reference

## Entry Points
- `FordFulkersonAlgorithm.run(AlgorithmRequest)`
- `EdmondsKarpAlgorithm.run(AlgorithmRequest)`
- `DinicAlgorithm.run(AlgorithmRequest)`
- All require source and sink vertices

## Important Classes
- `MaxFlowSupport` — residual and level-graph utilities
- `ResidualGraphSnapshot` — immutable residual state payload
- `LevelGraphSnapshot` — immutable level-graph state payload

## Important Constants
- `SubroutineLibrary.AUGMENT` — augmenting path subroutine ID


