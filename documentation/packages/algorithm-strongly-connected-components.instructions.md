# Strongly Connected Components (SCC) - CLRS-Style Specification

## Purpose

This document specifies a CLRS-style two-pass strongly-connected-components algorithm tuned to this project's timeline model and visualization conventions.
It describes expected behavior, visualization payloads, implementation guidance tied to the existing API, and registration steps so the algorithm integrates with the playback UI.

## Summary

- Algorithm: CLRS two-pass SCC using DFS on G and G^T (transpose).
- Complexity: O(V + E).
- Scope: directed graphs only (algorithm should validate orientation requirement).
- Output: each depth-first tree discovered in the second pass is emitted as one SCC; steps should annotate vertices with finish times (`f`) and component ids (`comp`).

## Pseudocode (CLRS-inspired)

STRONGLY-CONNECTED-COMPONENTS(G)

1. call DFS(G) to compute finish times u.f for each vertex u
2. create G^T (transpose of G)
3. call DFS(G^T), but in the main loop of DFS consider the vertices
   in order of decreasing u.f computed on G
4. output the vertices of each depth-first tree in the second pass as one SCC

Note: implementers should reuse the project's `DepthFirstSearchAlgorithm` patterns (subroutine collection, stack-frame adapter) rather than reimplementing the low-level DFS visualization plumbing.

## Behavior and integration points

- First pass: run a DFS over the selected `AlgorithmRequest` scope to compute discovery/finish times.
  - Preferred approach: record discovery/finish times in local maps (e.g., `Map<Integer,Integer> discoveryTime`, `finishTime`) and emit those as per-step vertex annotations. This preserves the immutability of timeline snapshots and avoids accidental shared-model mutations during timeline generation.
  - Mutating the live `Vertex` (for example via `Vertex.setDiscoveryTime(...)`/`setFinishTime(...)`) is discouraged for transient timing annotations because `AlgorithmRequest` may reference live model objects; prefer maps for per-step annotations. If you intentionally want the final run to persist component ids for later UI inspection, record them in a component map during traversal and apply `Vertex.setComponent(<id>)` once after the second pass completes, outside the DFS loops.
- Transpose construction: build a temporary `DirectedGraph` containing the same scoped vertices and reversed scoped edges (for each scoped edge (u,v) include (v,u)). Use the `AlgorithmRequest.getScopeVertices()` and `getScopeEdges()` as sources for the transpose construction so the transpose respects the selected scope.
- Second pass: build a new `AlgorithmRequest` over the transpose graph (or iterate the transpose edges while using the original request's vertex list ordering). Process vertices in decreasing finish-time order from the first pass.
- Subroutine / stack visualization: attach the `SubroutineLibrary.DFS_VISIT` subroutine metadata for DFS-VISIT call sites and emit `StackFramesSnapshot` using `DfsStackFrameAdapter` (same pattern used by `DepthFirstSearchAlgorithm`).

## Visualization notes (concrete)

- Show finish-time ordering as an explicit list or annotation before the second pass begins (e.g., a short step titled "Finish-time order" with per-vertex `f=` annotations).
- Present the transpose graph explicitly (a step that shows reversed edges with a short message like "Transpose G^T created").
- During both passes highlight the active pseudocode line (use the same pseudocode indexing conventions used by `DepthFirstSearchAlgorithm`).
- Use the `SubroutineStepCollector` to collect DFS-VISIT subroutine steps and include them in the `AlgorithmResult` as the `subroutineTimelines` map with key `SubroutineLibrary.DFS_VISIT` and `callableByLine` entries mapping each DFS-VISIT call-site line index to `SubroutineLibrary.DFS_VISIT`.
- Keep vertex annotations short: set `f=<finish>` after the first pass, and replace it entirely with `comp=<id>` upon completion in the second pass; record component ids in a map during traversal and apply `Vertex.setComponent(<id>)` once after the second pass completes.

## Implementation notes (API-level guidance)

Implementations must follow the project's established patterns (see `DepthFirstSearchAlgorithm`):

- Package and class placement: implement as `algorithm.general.StronglyConnectedComponentsAlgorithm` (or a sensible package consistent with other general algorithms).
- Pseudocode: provide a `List<String>` pseudocode array for the algorithm; mark DFS-VISIT call sites using `SubroutineLibrary.DFS_VISIT` when emitting steps that represent calling the subroutine.
- Use `AlgorithmRequest` helpers: read vertices via `request.getScopeVertices()` and edges via `request.getScopeEdges()`; traverse neighbors via `request.getOutgoingEdges(vertex)` to respect the selected scope; validate input early in `validate(AlgorithmRequest)` and return descriptive messages (for example: "Select a directed graph; SCC requires directed orientation.").
- Reuse helpers and patterns:
  - Use `SubroutineStepCollector` to build the `DFS-VISIT` subroutine timeline exactly as `DepthFirstSearchAlgorithm` does.
  - Use `DfsStackFrameAdapter` to produce `StackFramesSnapshot` instances for stack visualization.
  - Use `AlgorithmSupport.neutralStep(...)` or equivalent step builders when emitting neutral initialization steps.

Implementation sketch (do not invent new framework APIs; follow these calls):

1. Validate scope: ensure `!request.isEmpty()` and that the scope represents a directed graph (the UI already checks orientation, but validate here too).
2. Split the implementation into a dedicated DFS helper and a separate visualization wrapper so the algorithmic logic and timeline assembly remain isolated.
3. The DFS helper should compute discovery and finish times and determine component membership in decreasing finish-time order.
4. The visualization wrapper should build the reversed scoped graph, handle the empty-edge and `OutOfMemoryError` cases, and assemble the `AlgorithmResult` with `SubroutineStepCollector` output.
5. Finalize `AlgorithmResult` with pseudocode lines, the assembled `steps`, and subroutine timelines keyed by `SubroutineLibrary.DFS_VISIT`.

## Cancellation and error handling

- Periodically check `Thread.currentThread().isInterrupted()` within any long loops / recursive explorations and, if set, return an `AlgorithmResult` that contains a final step with a clear cancellation message.
- Catch `StackOverflowError` thrown by deeply recursive DFS; produce a final step with message: `Error: Graph depth exceeds execution stack limits.`
- Catch `OutOfMemoryError` during transpose construction and emit a final step with message: `Error: Graph is too dense to construct transpose within memory limits.`

## Validation checklist

- `validate(request)` should return non-null error strings for:
  - empty scope: "Select at least one vertex before running SCC.";
  - non-directed or incompatible scope: "SCC requires a directed graph." (UI orientation checks will usually prevent this, but validate defensively).
  - extremely large scopes: use a concrete threshold and surface a warning via `executionNotice` on `AlgorithmResult` instead of rejecting. For example: if `request.getScopeVertices().size() > 5000 || request.getScopeEdges().size() > 20000` then set `executionNotice = "Warning: Scope is large, visualization may be slow."` in the `AlgorithmResult` constructor.

## Registration (where to add the implementation)

- Add the concrete algorithm class under `src/algorithm/general` (or the package used for other general algorithms).
- Register the instance in `src/algorithm/GraphAlgorithmRegistry.java` by importing your class and adding `new StronglyConnectedComponentsAlgorithm()` to the `ALGORITHMS` `List.of(...)` collection so it appears in `AlgorithmRunDialog`.

## Implementation examples and pointers

- Reference implementation patterns: `src/algorithm/sssp/DepthFirstSearchAlgorithm.java` shows the canonical use of `SubroutineStepCollector`, `DfsStackFrameAdapter`, `snapshot(...)` and `snapshotWithSubroutine(...)` patterns. Mirror those idioms for consistent subroutine and stack-frame visuals.
- Use `AlgorithmStep` constructors to attach `StackFramesSnapshot` where appropriate so the `AlgorithmStackFramesDialog` can render the call stack.
- Use `EdgeKey.from(edge)` when tracking or coloring edges in a step overlay so edge identities remain stable across timeline steps.

## Visualization messages — recommended phrases

- "Initialize SCC: computing finish times (first DFS)"
- "Recording finish-time order"
- "Transpose G^T created (edges reversed)"
- "Second DFS on G^T: discovering component <id>"
- "Component <id> completed: vertices = [...]"

## References

- `DepthFirstSearchAlgorithm` — example DFS implementation and stack-frame usage (`src/algorithm/sssp/DepthFirstSearchAlgorithm.java`).
- `SubroutineLibrary.DFS_VISIT` — canonical subroutine id and pseudocode (`src/algorithm/SubroutineLibrary.java`).
- `DfsStackFrameAdapter` — adapter for `StackFramesSnapshot` (`src/algorithm/recursion/DfsStackFrameAdapter.java`).
- `GraphAlgorithmRegistry` — where to register the new algorithm (`src/algorithm/GraphAlgorithmRegistry.java`).

Generated on: 2026-05-25
