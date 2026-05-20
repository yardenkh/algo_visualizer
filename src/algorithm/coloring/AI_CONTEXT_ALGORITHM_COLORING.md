# Package Purpose

Exact coloring services for selected subgraphs. Provides reusable, UI-independent services for minimum proper coloring tasks. Used by Selection → Extras actions and intentionally separated from playback-oriented algorithm timelines.

# Package Mental Model

- **Not Timeline-Based**: Unlike playback algorithms, coloring services return immutable result objects directly without step-by-step timeline. They execute in background and return final coloring.
- **Exact Algorithms**: Uses branch-and-bound search with DSATUR heuristics. Guarantees optimal (minimum) coloring.
- **Two Problem Domains**: Vertex coloring (vertices get colors) and edge coloring (edges get colors).
- **Conflict Graph Approach**: Edge coloring is modeled as vertex coloring on the conflict graph (where each edge becomes a vertex, and edges are connected if they share endpoints).
- **Validation and Error Handling**: Services reject invalid inputs (self-loops, unsupported structures) with explicit error messages.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | List of vertices or edges; selected scope from UI |
| Outputs | Immutable result object with computed coloring and metadata |
| Owns | Coloring algorithms (vertex, edge), exact solver with branch-and-bound |
| Depends On | `graph` (Vertex, Edge classes) |
| Must NOT Depend On | `ui`, `algorithm`, playback system |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `VertexColoringService` | Main entry for vertex coloring | `ExactColoringSolver` | `ui.extras` handlers | MEDIUM | Computes chromatic number and exact coloring |
| `EdgeColoringService` | Main entry for edge coloring | `ExactColoringSolver` | `ui.extras` handlers | MEDIUM | Solves via conflict graph transformation |
| `ExactColoringSolver` | Branch-and-bound solver with DSATUR | - | Both services | HIGH | Core algorithm; expensive but exact |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| - | No public interfaces; service classes only | - | STABLE |

# Runtime Flows

## Vertex Coloring Execution
1. `VertexColoringService.compute(List<Vertex>)` called with scope vertices.
2. Validate input: reject self-loops, unsupported structures.
3. Call `ExactColoringSolver.solveVertexColoring(vertices)`.
4. Solver uses branch-and-bound with DSATUR:
   - Maintain current partial coloring.
   - Use DSATUR heuristic: select uncolored vertex with most colored neighbors.
   - Try coloring with minimum available color.
   - Backtrack if coloring cannot extend.
5. Return result with chromatic number and final coloring.

## Edge Coloring Execution
1. `EdgeColoringService.compute(List<Edge>)` called with scope edges.
2. Build conflict graph: each edge becomes vertex, edges connect if they share endpoint.
3. Validate: check for multi-edges or other invalid structures.
4. Call `ExactColoringSolver.solveVertexColoring(conflict graph vertices)`.
5. Map coloring back to original edges.
6. Return result with chromatic number and final coloring.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| Vertex coloring result | Assigned color for each vertex | Map<Vertex, Integer> | All vertices colored; no two adjacent vertices have same color |
| Edge coloring result | Assigned color for each edge | Map<Edge, Integer> | All edges colored; no two edges sharing vertex have same color |
| Conflict graph | Auxiliary structure for edge coloring | Vertices represent edges, vertices connected if share endpoint | Used only during edge coloring; not returned to caller |

# Hidden Assumptions

- **Simple Graphs Only**: No multi-edges, self-loops, or complex structures. Validation rejects these.
- **Color Classes as Integers**: Colors are non-negative integers (0, 1, 2, ...). UI maps integers to RGB via palette.
- **Exact Optimality Sought**: Services guarantee minimum coloring. This is expensive for large graphs but correct.
- **No Parallel Edges**: Edge coloring assumes at most one edge per ordered endpoint pair.

# Concurrency Notes

Package is single-threaded. Services execute synchronously. For large graphs (>100 vertices), coloring may take several seconds. Called from EDT but should ideally run in background thread (not currently implemented).

# Performance Notes

- **Vertex Coloring**: Exponential worst-case complexity. Polynomial for sparse graphs with bounded degeneracy.
- **NP-Hardness**: Graph coloring is NP-hard; no polynomial algorithm known. Branch-and-bound with DSATUR heuristic is practical for small/medium graphs.
- **Practical Limits**: Acceptable performance for <50 vertices with average degree <5. Larger instances may timeout.
- **DSATUR Heuristic**: Significantly prunes search tree by selecting high-degree vertices first.

# Modification Guide For AI Assistants

## Safest Changes

- **Optimize DSATUR heuristic**: Improve vertex selection or color assignment without changing final coloring.
- **Add new validation checks**: Reject more invalid input structures explicitly.
- **Add new result metadata**: Include additional info in result object (e.g., coloring quality metrics).

## Dangerous Changes

- **Do NOT modify branch-and-bound backtracking logic**: Correctness depends on exploring full search space correctly.
- **Do NOT remove validation**: Validation prevents crashes on invalid input.
- **Do NOT change conflict graph construction for edge coloring**: Construction directly affects correctness.

## Preferred Patterns

- **Validate input early**: Check vertices/edges for null, duplicates, unsupported structures before heavy computation.
- **Return immutable results**: Services should not maintain state between calls. Each call should be independent.
- **Use descriptive error messages**: Validation errors should tell user exactly what's wrong (e.g., "self-loop detected at vertex V3").

# Typical Change Recipes

### Add Heuristic Optimization to Coloring
1. Implement new vertex selection heuristic:
   ```java
   Vertex selectNextVertex(Set<Vertex> uncolored, Map<Vertex, Set<Integer>> usedColors) {
       // Custom heuristic: select vertex with most conflicts
       return uncolored.stream()
           .max(Comparator.comparingInt(v -> usedColors.get(v).size()))
           .orElse(null);
   }
   ```
2. Use in `ExactColoringSolver` instead of DSATUR.
3. Final coloring unchanged; algorithm may run faster/slower depending on heuristic quality.

### Add Validation for Specific Constraint
1. In `VertexColoringService.compute()`, add check:
   ```java
   if (vertices.stream().anyMatch(v -> v.getOutgoingEdges().isEmpty())) {
       throw new IllegalArgumentException("Isolated vertices not supported");
   }
   ```
2. Return descriptive error message to UI.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `graph` | **Consumes** | Services operate on Vertex/Edge lists |
| `ui.extras` | **Consumed By** | Handlers invoke services and display results |

# AI Quick Reference

## Entry Points
- `VertexColoringService.compute(List<Vertex>)` → vertex coloring result
- `EdgeColoringService.compute(List<Edge>)` → edge coloring result
- Both services are stateless; call with scope vertices/edges

## Important Result Object
- Chromatic number (minimum colors needed)
- Vertex-to-color or edge-to-color mapping
- Validation status (valid or error)

## Key Constraint
- All algorithms are exact (optimal) but exponential worst-case complexity
- Practical limits: ~50 vertices for vertex coloring


