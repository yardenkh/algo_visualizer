# Package Purpose

Core algorithm framework and playback model. Defines the execution contract used by every algorithm and visualization surface. Algorithms receive an immutable request and return an immutable timeline of snapshots for step-by-step visualization.

# Package Mental Model

- **Immutable Timeline Paradigm**: Every algorithm produces an ordered list of `AlgorithmStep` snapshots, enabling stateless stepping forward/backward through visualization.
- **Deterministic Playback**: All state at each step is frozen, allowing independent rendering of any step without context.
- **Presentation-Oriented Payloads**: Steps include graph coloring, annotations, and optional data visualizations (matrices, stack frames, residual graphs) so UI rendering is simple and decoupled from algorithm logic.
- **Request-Response Architecture**: Algorithm input is encapsulated in immutable `AlgorithmRequest` snapshots. Algorithms never see live model state, ensuring isolation.
- **Subroutine Expansion**: Subroutines (RELAX, DFS-VISIT, etc.) are modeled as timeline expansion rather than nested execution contexts. When a subroutine is "called," the UI switches to a secondary timeline.
- **Registry-Based Discovery**: All concrete implementations are centrally registered so UI can discover and display available algorithms without hard dependencies.
- **Algorithm Metadata**: Each algorithm declares its display name, pseudocode, graph requirements (orientation, source/sink vertices), and validation rules.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | `graph.DirectedGraph`, `graph.Vertex`, `graph.Edge`, user selection scope (vertices/edges) |
| Outputs | `AlgorithmResult` (timeline + pseudocode), `AlgorithmStep` (snapshots with coloring/annotations), optional subroutine timelines |
| Owns | Timeline model, step payloads, playback state machine, subroutine registry, visualization helpers |
| Depends On | `graph` (model data), `algorithm.recursion` (stack frame adapters) |
| Must NOT Depend On | `ui`, `ui.dialogs`, Swing components |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `GraphAlgorithm` | Base interface for all algorithms | - | `GraphAlgorithmRegistry`, all algorithm subclasses | LOW | Contract is stable; extending requires no UI changes |
| `AlgorithmRequest` | Immutable input snapshot from selected scope | `graph.*` | All `GraphAlgorithm` implementations | MEDIUM | Critical boundary object; sort order must be deterministic |
| `AlgorithmResult` | Complete timeline + metadata from algorithm run | `AlgorithmStep`, `SubroutineTimeline` | `AlgorithmPlaybackController` | LOW | Immutable; used as-is by UI without modification |
| `AlgorithmStep` | Single snapshot in timeline (colors, annotations, data payloads) | `EdgeKey`, payloads | `AlgorithmPlaybackController`, UI dialogs | MEDIUM | Payload structure is extensible but requires UI updates for new types |
| `AlgorithmPlaybackController` | State machine for stepping through timeline (forward/backward) | `AlgorithmResult`, `SubroutineTimeline` | `GraphEditorFrame`, UI dialogs | HIGH | Controls all playback behavior; transitions between main/sub-timelines are sensitive |
| `AlgorithmType` | Enum of algorithm categories (SSSP, APSP, MST, etc.) | - | `GraphAlgorithm`, UI filters | LOW | Stable enum; adding categories requires UI updates |
| `SubroutineLibrary` | Static registry of CLRS-style subroutine definitions (RELAX, DFS-VISIT, etc.) | - | Algorithm implementations | MEDIUM | Subroutine IDs are string constants; adding new ones requires careful coordination with UI |
| `SubroutineTimeline` | Replayable timeline for a subroutine (ID, pseudocode, steps) | `AlgorithmStep` | `AlgorithmPlaybackController`, returned by algorithms | LOW | Container for subroutine data; immutable |
| `AlgorithmSupport` | Utility helpers (edge creation, color/annotation builders) | `graph.*` | Algorithm implementations | LOW | Non-critical; refactor-safe |
| `GraphAlgorithmRegistry` | Central discovery mechanism for all algorithms | `GraphAlgorithm` implementations | `GraphEditorFrame`, `AlgorithmRunDialog` | MEDIUM | Hard-coded list; adding algorithms requires explicit registration here |
| `AlgorithmPalette` | Style conventions (color codes, annotation formats) | - | Algorithm implementations | LOW | Constants; update only if UI rendering changes |
| `EdgeKey` | Stable edge identifier (source+dest id pair) used in step overlays | - | `AlgorithmStep`, algorithms | LOW | Immutable; canonical edge reference across packages |
| `MatrixSnapshot` | Immutable 2D distance/cost matrix used by APSP algorithms | - | `AlgorithmStep`, `AlgorithmMatrixDialog` | LOW | Transport object; immutable |
| `ResidualGraphSnapshot` | Flow residual capacities snapshot used by max-flow algorithms | - | `AlgorithmStep`, `ResidualGraphDialog` | LOW | Transport object; immutable |
| `LevelGraphSnapshot` | Level graph structure used by Dinic max-flow visualization | - | `AlgorithmStep`, `LevelGraphDialog` | LOW | Transport object; immutable |
| `GraphOrientationRequirement` | Enum: DIRECTED / UNDIRECTED / EITHER | - | `GraphAlgorithm` | LOW | Stable enum; used for validation before run |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| `GraphAlgorithm` | Base contract for all graph algorithms | 20+ concrete implementations (DFS, Dijkstra, Floyd-Warshall, etc.) | STABLE |
| - `getDisplayName()` | Human-readable algorithm label | - | STABLE |
| - `getDescription()` | Multi-line explanation shown in UI | - | STABLE |
| - `getType()` | Enum categorization (SSSP, APSP, MST, etc.) | - | STABLE |
| - `getPseudocodeLines()` | Ordered pseudocode lines for highlighting | - | STABLE |
| - `validate(AlgorithmRequest)` | Pre-run validation; returns error message or null | - | STABLE |
| - `run(AlgorithmRequest)` | Execute algorithm; return immutable `AlgorithmResult` | - | STABLE |
| - `requiresSourceVertex()` | Does algorithm need a source vertex? | - | STABLE |
| - `requiresSinkVertex()` | Does algorithm need a sink vertex? (max-flow) | - | STABLE |
| - `getOrientationRequirement()` | Declares graph orientation assumptions | - | STABLE |

# Runtime Flows

## Algorithm Execution Flow
1. User selects vertices/edges and chooses algorithm from dialog.
2. UI validates user input (source/sink vertex, orientation) via `algorithm.validate()`.
3. `AlgorithmRequest` is built from current selection and passed to `algorithm.run()`.
4. Algorithm generates deterministic timeline and returns `AlgorithmResult`.
5. `AlgorithmPlaybackController` loads the result and sets step index to 0.
6. UI renders current step: apply vertex colors, edge colors, annotations; highlight pseudocode line.

## Stepping Flow
- **Forward**: `currentStepIndex++`, render new step, notify listeners.
- **Backward**: `currentStepIndex--`, render new step, notify listeners.
- **UI Update**: All dialogs (matrix, stack frames, residual graph) listen to playback changes and update their views.

## Subroutine Handling Flow
1. Algorithm detects a subroutine call (e.g., RELAX function call in Dijkstra).
2. Algorithm attaches subroutine ID to the `AlgorithmStep` via `callableSubroutineId` field.
3. UI displays "Open Sub-Routine" button when subroutine ID is present.
4. User clicks button; playback controller switches `activeSubroutineTimeline` to the subroutine's `SubroutineTimeline`.
5. UI now displays subroutine pseudocode and steps.
6. When subroutine ends, controller returns to base timeline at saved `preSubroutineStepIndex`.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| `AlgorithmRequest` | Immutable snapshot of scope to run algorithm on | `graph`, `scopeVertices`, `scopeEdges`, `sourceVertex`, `sinkVertex` | Scope vertices/edges must be sorted; must exist in graph; source must be non-null for most algorithms |
| `AlgorithmResult` | Complete timeline produced by one algorithm run | `algorithmName`, `pseudocodeLines`, `steps`, `subroutineTimelines`, `callableSubroutineByLine` | Steps list must not be empty; subroutine IDs must match `SubroutineLibrary` constants |
| `AlgorithmStep` | One snapshot in timeline | `activePseudocodeLine`, `vertexColors`, `edgeColors`, `vertexAnnotations`, `edgeAnnotations`, payloads | Only pseudocode lines present in result's pseudocode list can be highlighted |
| `EdgeKey` | Immutable identifier for an edge | `sourceId`, `destinationId` | sourceId and destinationId must be >= 0; represents exactly one directed edge |
| `SubroutineTimeline` | Replayable subroutine definition + steps | `id`, `displayName`, `pseudocodeLines`, `steps` | ID must be non-blank; must match constant in `SubroutineLibrary` |

# Hidden Assumptions

- **Deterministic Sort Order**: `AlgorithmRequest` sorts vertices and edges deterministically so algorithm execution is reproducible. Algorithms must preserve this order.
- **Live Model Changes Not Reflected**: Once a request is built, changes to the live graph are ignored until the next run. Subroutines run in isolation without affecting main algorithm state.
- **Pseudocode Line Indexing**: Pseudocode lines are 0-indexed; algorithms must ensure `activePseudocodeLine` values are valid indices into the pseudocode list.
- **Subroutine Independence**: Subroutines are modeled as separate timelines, not nested execution contexts. Subroutine steps are pre-computed when algorithm runs, not generated on-demand.
- **No Global State**: Algorithms are stateless. The playback controller holds all step history; algorithms never retain execution state between runs.
- **Color/Annotation Immutability**: Once a step is created, its colors and annotations are never modified. UI can safely cache and diff steps.

# Concurrency Notes

Package is effectively single-threaded. Algorithm execution happens synchronously on the Event Dispatch Thread (EDT). `AlgorithmPlaybackController` is thread-unsafe and must only be accessed from EDT. No synchronization on result objects.

# Performance Notes

- **Timeline Generation**: Algorithms generate the entire timeline in one pass during `run()` call. Large graphs (100+ vertices) may take 100-500ms; this is acceptable for educational context but not production.
- **Step Rendering**: UI renders only the current step; avoiding rendering all steps saves significant time.
- **Subroutine Timelines**: Subroutine timelines are pre-computed during algorithm run, not on-demand, so switching between tabs is instant.
- **Memory**: Timeline snapshots are immutable and reference-heavy (many color maps, annotation maps). Large timelines (1000+ steps) may consume 10-50 MB; this is acceptable.

# Modification Guide For AI Assistants

## Safest Changes

- **Add a new algorithm**: Create a class in the appropriate subpackage (e.g., `algorithm.sssp`), implement `GraphAlgorithm`, register in `GraphAlgorithmRegistry`, and add pseudocode. No changes to core framework needed.
- **Add a new subroutine**: Add constant to `SubroutineLibrary`, add pseudocode list, update algorithm implementations to populate subroutine timeline via `SubroutineStepCollector`.
- **Extend step payloads**: Add new optional field to `AlgorithmStep` (e.g., new snapshot type). Update UI dialogs to display field if present.
- **Adjust visualization colors**: Edit `AlgorithmPalette` constants.

## Dangerous Changes

- **Do NOT modify `AlgorithmRequest` sorting logic**: Determinism depends on this; changing sort order will break reproducibility.
- **Do NOT add required fields to `AlgorithmStep`**: All new fields should be optional (with sensible defaults) to avoid breaking existing algorithm implementations.
- **Do NOT modify `AlgorithmPlaybackController` step indexing**: This is the core state machine; changes must be extremely careful to avoid off-by-one errors or lost step context.
- **Do NOT rename subroutine IDs in `SubroutineLibrary`**: Existing algorithm implementations reference these as string constants; renaming will break subroutine lookups.
- **Do NOT make algorithms stateful**: Each algorithm call must produce identical results given identical input. Never cache or retain execution state.

## Preferred Patterns

- **Use `AlgorithmSupport` helpers**: Utility functions for edge lookup, color/annotation building, and visualization state. Reuse rather than duplicating logic in algorithm implementations.
- **Emit steps frequently**: Many small steps are better than fewer large steps; they provide more granular visualization.
- **Use `SubroutineStepCollector`** to build subroutine timelines: This utility encapsulates the pattern of collecting steps for a subroutine scope.
- **Validate early**: Call `validate()` before heavy computation. Return descriptive error messages.
- **Use `EdgeKey` for edge identification**: Avoid using object references in step overlays; use `EdgeKey` instead for stable comparison.

## Anti-Patterns

- **Reaching back to live graph after building request**: Request is a snapshot. If you need updated graph state, build a new request.
- **Modifying step objects after creation**: Steps are immutable once created. Changing them will affect all playback history.
- **Assuming step indices match pseudocode lines**: Many algorithms skip certain lines or jump around. Never assume step index == pseudocode line index.
- **Using vertex/edge object identity**: Use IDs instead. Vertices and edges are mutable; IDs are stable.

# Typical Change Recipes

### Add a New Algorithm
1. Create `algorithm.NewAlgorithm` class implementing `GraphAlgorithm`.
2. Implement required methods: `getDisplayName()`, `getDescription()`, `getType()`, `getPseudocodeLines()`, `validate()`, `run()`.
3. In `run()`, build list of `AlgorithmStep` snapshots. For each significant action:
   - Create new color/annotation maps (copy from previous step if needed).
   - Record current vertex/edge state.
   - Create `new AlgorithmStep(title, message, pseudocodeLine, colors, annotations)`.
   - Add to list.
4. Return `new AlgorithmResult(algorithmName, pseudocodeLines, steps)`.
5. Register in `GraphAlgorithmRegistry` ALGORITHMS list.

### Add Subroutine Support to Existing Algorithm
1. Decide on subroutine ID (e.g., `SubroutineLibrary.RELAX`).
2. Add pseudocode lines to `SubroutineLibrary` (if not already present).
3. In algorithm's `run()` method, use `SubroutineStepCollector` to build subroutine timeline:
   ```java
   SubroutineStepCollector collector = new SubroutineStepCollector(SubroutineLibrary.RELAX, SubroutineLibrary.RELAX_PSEUDOCODE);
   // ... run subroutine logic, add steps to collector
   SubroutineTimeline subrTimeline = collector.build();
   ```
4. When subroutine is called in main algorithm, attach ID to main step:
   ```java
   AlgorithmStep mainStep = new AlgorithmStep(..., mainColors, mainAnnotations, SubroutineLibrary.RELAX);
   steps.add(mainStep);
   ```
5. Include subroutine timelines in `AlgorithmResult` constructor.

### Modify Step Payload Types
1. Add new optional field to `AlgorithmStep` (e.g., `new CustomSnapshot`).
2. Add constructor overload or use existing constructor and set field via setter (if mutable).
3. In algorithm, populate field when creating step:
   ```java
   AlgorithmStep step = new AlgorithmStep(..., mainColors, mainAnnotations);
   step.setCustomSnapshot(newCustomSnapshot);
   steps.add(step);
   ```
4. In UI dialog, check if field is present before rendering:
   ```java
   if (currentStep.getCustomSnapshot() != null) {
       // render custom snapshot
   }
   ```

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `graph` | **Consumes** | Algorithm input is read-only; requests are built from selected graph scope |
| `algorithm.sssp` | **Implements** | Concrete single-source shortest path algorithms (Dijkstra, BFS, DFS, etc.) |
| `algorithm.apsp` | **Implements** | Concrete all-pairs shortest path algorithms (Floyd-Warshall, Johnson, etc.) |
| `algorithm.mst` | **Implements** | Concrete minimum spanning tree algorithms (Prim, Kruskal) |
| `algorithm.maxflow` | **Implements** | Concrete maximum flow algorithms (Ford-Fulkerson, Edmonds-Karp, Dinic) |
| `algorithm.general` | **Implements** | General algorithms (topological sort, transitive closure, A*, Seidel) |
| `algorithm.coloring` | **Services** | Exact coloring algorithms (not playback timelines; used by extras) |
| `algorithm.recursion` | **Uses** | Adapters to convert recursive algorithm state to stack frame snapshots |
| `ui` | **Provides** | Entry point is playback controller; UI orchestrates algorithm runs and stepping |
| `ui.dialogs` | **Provides** | Playback dialog, pseudocode dialog, matrix/stack/residual graph viewers consume results |

# AI Quick Reference

## Entry Points
- `GraphAlgorithm.run(AlgorithmRequest)` → entry for algorithm implementation
- `GraphAlgorithmRegistry.getAlgorithms()` → entry for UI discovery
- `AlgorithmPlaybackController.start(AlgorithmResult)` → entry for playback start
- `AlgorithmPlaybackController.stepForward() / stepBackward()` → entry for playback navigation

## Important APIs
- `AlgorithmRequest` — immutable input; access via getters
- `AlgorithmResult` — immutable output; access via getters
- `AlgorithmStep` — timeline snapshot; immutable
- `AlgorithmPlaybackController` — playback state machine; supports listeners
- `SubroutineLibrary.RELAX`, `DFS_VISIT`, `BFS_LAYER`, `AUGMENT` — subroutine IDs

## Important Services
- `AlgorithmSupport` — edge lookup, color/annotation helpers, pseudocode building
- `SubroutineStepCollector` — collects steps for a subroutine scope
- `GraphAlgorithmRegistry` — algorithm discovery and registration

## Important Enums
- `AlgorithmType.SSSP`, `APSP`, `MST`, `MAXIMUM_FLOW`, `GENERAL` — algorithm category
- `GraphOrientationRequirement.DIRECTED`, `UNDIRECTED`, `EITHER` — graph orientation contract

## Important Constants
- `SubroutineLibrary.RELAX` — relax subroutine ID
- `SubroutineLibrary.DFS_VISIT` — DFS-VISIT subroutine ID
- `SubroutineLibrary.BFS_LAYER` — BFS blocking-flow subroutine ID
- `SubroutineLibrary.AUGMENT` — augmentation subroutine ID


