# Package Purpose

Minimum spanning tree (MST) demonstrations. Interpret selected edges as undirected weighted links and build a spanning tree/forest view through timeline snapshots. Two main approaches: Kruskal (edge-based greedy) and Prim (vertex-based greedy).

# Package Mental Model

- **Undirected Interpretation**: Graph edges are treated as undirected; weight is symmetric. Algorithms work on any vertex subset.
- **Greedy Approaches**: Both Prim and Kruskal are greedy; they make locally optimal decisions that lead to globally optimal solution (provable by matroid theory).
- **Vertex vs. Edge**: Prim grows tree from a source vertex. Kruskal sorts edges and accepts/rejects based on cycle detection.
- **Tree/Forest Output**: If graph is disconnected, algorithm produces forest (multiple trees). Each tree is part of final result.
- **Disjoint Set (Union-Find)**: Kruskal uses disjoint-set data structure to efficiently detect cycles when adding edges.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | `AlgorithmRequest` with scoped graph (orientation requirement is UNDIRECTED) |
| Outputs | `AlgorithmResult` with timeline showing edges added/rejected and tree/forest state |
| Owns | Algorithm implementations (Prim, Kruskal) |
| Depends On | `algorithm` (core interfaces, support) |
| Must NOT Depend On | `ui` |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `PrimAlgorithm` | Vertex-based greedy MST | `algorithm` | Registry | LOW | O(V^2) with simple implementation; works well for small/dense graphs |
| `KruskalAlgorithm` | Edge-based greedy MST with union-find | `algorithm` | Registry | LOW | O(E log E) sorting + O(E α(V)) union-find; efficient for sparse graphs |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| `GraphAlgorithm` | Base contract | Prim, Kruskal | STABLE |

# Runtime Flows

## Prim's Algorithm Execution
1. Pick arbitrary start vertex (usually first in list).
2. Mark start as in-tree, initialize min-heap with edges from start.
3. While not all vertices in tree:
   - Extract min-weight edge (u, v) where u ∈ tree and v ∉ tree.
   - Add v to tree, add edge (u, v) to MST.
   - Add edges from v to vertices not yet in tree to heap.
   - Emit step highlighting edge and tree growth.
4. Return timeline with final tree.

## Kruskal's Algorithm Execution
1. Sort edges by weight (ascending).
2. Initialize disjoint-set with each vertex as singleton.
3. For each edge (u, v) in sorted order:
   - If find(u) != find(v): (u, v) connects two components)
     - Accept edge: add to MST, union(u, v).
     - Emit step.
   - Else: reject (u, v) would create cycle).
     - Emit step showing rejection.
4. Return timeline with final tree/forest.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| Tree state (Prim) | Vertices in tree, edges in MST | Set of tree vertices, MST edges | All tree vertices connected; no cycles; exactly |tree|-1 edges |
| Component state (Kruskal) | Disjoint-set structure | Parent pointers, rank | Union-find invariants maintained |

# Hidden Assumptions

- **Undirected Graph**: Treated as undirected even if input graph is directed. Algorithms assume bidirectional connectivity.
- **No Self-Loops**: Self-loops are ignored or cause issues. Algorithms assume simple graphs.
- **Unique Source for Prim**: Prim starts from a chosen source. For disconnected graphs, forest is built by finding unvisited components and re-running from them.
- **Equal Weight Edges**: If multiple edges have same weight, tie-breaking order is deterministic (sorted by endpoints). This ensures reproducibility.

# Concurrency Notes

Package is single-threaded. Algorithm execution is synchronous on EDT. No concurrency.

# Performance Notes

- **Prim**: O(V^2) simple implementation; O(E log V) with binary heap. Acceptable for <500 vertices.
- **Kruskal**: O(E log E) sorting + O(E α(V)) union-find operations. Efficient even for large sparse graphs.

# Modification Guide For AI Assistants

## Safest Changes

- **Optimize Prim with priority queue**: Use binary heap instead of linear scan for min-weight edge. Timeline remains identical.
- **Add detailed annotations**: Show key/parent values or component information in vertex labels.
- **Add new MST variant**: Create new class, implement `GraphAlgorithm`, follow same timeline pattern.

## Dangerous Changes

- **Do NOT assume directed edges**: MST algorithms treat edges as undirected. Changing this breaks correctness.
- **Do NOT remove cycle detection in Kruskal**: Union-find check is essential; removing it creates incorrect forests.

## Preferred Patterns

- **Use union-find efficiently**: Implement path compression and union by rank for near-linear performance.
- **Emit steps frequently**: Show each edge accept/reject decision for clear visualization.
- **Mark tree edges distinctly**: Use different colors for tree edges vs. non-tree edges.

# Typical Change Recipes

### Optimize Prim with Binary Heap
1. Replace linear scan for min-weight edge with binary heap.
2. When adding vertex v to tree:
   - For each edge from v: add to heap with weight.
3. Extract min from heap; skip if both endpoints in tree.
4. Timeline output remains the same; algorithm just runs faster.

### Add Kruskal Variant with Different Tie-Breaking
1. Modify edge sorting comparator in Kruskal.
2. Sort by weight first; use different tie-breaker (e.g., edge ID, alphabetical vertex name).
3. Timeline structure remains identical; edge order may differ if tie-breaking differs.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm` | **Implements** | Concrete `GraphAlgorithm` implementations |
| `ui` | **Consumed By** | Timeline rendered in playback UI |

# AI Quick Reference

## Entry Points
- `PrimAlgorithm.run(AlgorithmRequest)`
- `KruskalAlgorithm.run(AlgorithmRequest)`
- Both return `AlgorithmResult` with timeline

## Algorithm Characteristics
- **Prim**: Source-based, grows tree from starting vertex
- **Kruskal**: Edge-based, sorts and greedily adds edges


