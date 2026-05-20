# AI Context Files - Package Index

This document provides a quick reference to all AI_CONTEXT files for the graph visualization and algorithm education project.

## Quick Navigation

All packages are documented in separate AI_CONTEXT files. Each file follows a standard format with package purpose, mental model, boundaries, important classes, runtime flows, and modification guides.

### Core Model Packages

| Package | File | Purpose |
|---------|------|---------|
| `graph` | `AI_CONTEXT_GRAPH.md` | Core domain model: vertices, edges, directed graphs |
| `algorithm` | `AI_CONTEXT_ALGORITHM.md` | Algorithm framework: timeline paradigm, playback control, subroutines |

### Algorithm Implementation Packages

| Package | File | Purpose |
|---------|------|---------|
| `algorithm.sssp` | `AI_CONTEXT_ALGORITHM_SSSP.md` | Single-source shortest path: BFS, DFS, Dijkstra, Bellman-Ford, DAG |
| `algorithm.apsp` | `AI_CONTEXT_ALGORITHM_APSP.md` | All-pairs shortest path: Floyd-Warshall, Johnson, Repeated Dijkstra |
| `algorithm.mst` | `AI_CONTEXT_ALGORITHM_MST.md` | Minimum spanning tree: Prim, Kruskal |
| `algorithm.maxflow` | `AI_CONTEXT_ALGORITHM_MAXFLOW.md` | Maximum flow: Ford-Fulkerson, Edmonds-Karp, Dinic |
| `algorithm.general` | `AI_CONTEXT_ALGORITHM_GENERAL.md` | General algorithms: topological sort, transitive closure, A*, Seidel |
| `algorithm.coloring` | `AI_CONTEXT_ALGORITHM_COLORING.md` | Graph coloring services: exact vertex/edge coloring |
| `algorithm.recursion` | `AI_CONTEXT_ALGORITHM_RECURSION.md` | Recursion visualization: stack frame adapters |

### UI Packages

| Package | File | Purpose |
|---------|------|---------|
| `ui` | `AI_CONTEXT_UI.md` | Main editor frame and core UI components |
| `ui.dialogs` | `AI_CONTEXT_UI_DIALOGS.md` | Modal dialogs and algorithm viewer panels |
| `ui.extras` | `AI_CONTEXT_UI_EXTRAS.md` | Selection extras action handlers and utilities |
| `ui.templates` | `AI_CONTEXT_UI_TEMPLATES.md` | Premade graph templates and preview rendering |

## Architecture Overview

### Layered Structure

```
┌─────────────────────────────────────────────┐
│             UI Layer                        │
│  ┌──────────────────────────────────────┐   │
│  │ ui.GraphEditorFrame (composition root)│   │
│  │ ui.GraphCanvasPanel (rendering)       │   │
│  │ ui.GraphEditorController (mutations)  │   │
│  └──────────────────────────────────────┘   │
│  ┌──────────────────────────────────────┐   │
│  │ ui.dialogs (viewers, config dialogs)  │   │
│  │ ui.extras (action handlers)           │   │
│  │ ui.templates (template library)       │   │
│  └──────────────────────────────────────┘   │
├─────────────────────────────────────────────┤
│          Algorithm Layer                    │
│  ┌──────────────────────────────────────┐   │
│  │ algorithm (framework, registry)       │   │
│  │ algorithm.sssp                        │   │
│  │ algorithm.apsp                        │   │
│  │ algorithm.mst                         │   │
│  │ algorithm.maxflow                     │   │
│  │ algorithm.general                     │   │
│  │ algorithm.coloring                    │   │
│  │ algorithm.recursion (adapters)        │   │
│  └──────────────────────────────────────┘   │
├─────────────────────────────────────────────┤
│           Model Layer                       │
│  ┌──────────────────────────────────────┐   │
│  │ graph (DirectedGraph, Vertex, Edge)   │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

### Data Flow

```
User Input (UI)
    ↓
GraphEditorController (mutation + undo)
    ↓
DirectedGraph (model)
    ↓
AlgorithmRequest (immutable snapshot)
    ↓
GraphAlgorithm.run() (timeline generation)
    ↓
AlgorithmResult (immutable output)
    ↓
AlgorithmPlaybackController (stepping)
    ↓
Viewers (matrix, pseudocode, stack frames, etc.)
```

## Key Concepts

### Timeline Paradigm

- All algorithms produce immutable timelines of steps.
- Each step contains: pseudocode line, vertex colors, edge colors, annotations, optional data payloads.
- Playback controller steps forward/backward through timeline.
- Viewers update reactively to step changes.

**See: `AI_CONTEXT_ALGORITHM.md`**

### Subroutine System

- Subroutines (RELAX, DFS-VISIT, AUGMENT, BFS-LAYER) are modeled as expandable overlays.
- When subroutine is called in main algorithm, UI shows "Open Sub-Routine" button.
- Clicking button switches playback to subroutine timeline.
- Subroutines are pre-computed during algorithm run, not on-demand.

**See: `AI_CONTEXT_ALGORITHM.md`, algorithm implementation files**

### Request-Response Isolation

- Algorithm never sees live graph directly.
- `AlgorithmRequest` is immutable snapshot of scope (vertices/edges to operate on).
- Algorithms cannot modify request or graph.
- Multiple algorithms can run on same graph without interference.

**See: `AI_CONTEXT_ALGORITHM.md`, `AI_CONTEXT_GRAPH.md`**

### Adapter Pattern

- Recursive algorithms maintain internal frame models.
- Adapters convert internal state to immutable `StackFramesSnapshot`.
- UI renders frames generically without knowing algorithm details.

**See: `AI_CONTEXT_ALGORITHM_RECURSION.md`**

### Color and Annotation System

- Steps attach vertex/edge colors and text annotations.
- Colors are class indices (0, 1, 2, ...) mapped to RGB by UI palette.
- Annotations are arbitrary strings (e.g., "distance=5", "visited").
- UI layers colors and annotations on graph rendering.

**See: `AI_CONTEXT_UI.md`, `AI_CONTEXT_UI_EXTRAS.md`**

## Common Workflows

### Running an Algorithm
1. User selects vertices/edges and chooses algorithm.
2. Frame builds `AlgorithmRequest` from selection.
3. Algorithm validates and runs: `algorithm.run(request)`.
4. Frame loads result into `AlgorithmPlaybackController`.
5. Playback controller starts at step 0.
6. Viewers listen and update.
7. User steps forward/backward.

**Files involved**: `AI_CONTEXT_UI.md`, `AI_CONTEXT_ALGORITHM.md`, algorithm implementation files

### Expanding a Subroutine
1. Playback is at step with callable subroutine ID.
2. UI shows "Open Sub-Routine" button.
3. User clicks button.
4. Playback controller switches to subroutine timeline.
5. Pseudocode viewer switches to subroutine pseudocode.
6. User steps through subroutine.
7. Subroutine ends; playback returns to main timeline at saved step index.

**Files involved**: `AI_CONTEXT_ALGORITHM.md`, `AI_CONTEXT_UI_DIALOGS.md`, algorithm implementation files

### Applying Graph Coloring
1. User selects vertices and clicks "Color Vertices" from extras menu.
2. Frame invokes `VertexColoringActionHandler.run()`.
3. Handler calls `VertexColoringService.compute()`.
4. Service runs exact coloring algorithm (expensive).
5. Handler applies result colors to vertices via controller.
6. Graph is updated; UI renders.

**Files involved**: `AI_CONTEXT_UI_EXTRAS.md`, `AI_CONTEXT_ALGORITHM_COLORING.md`, `AI_CONTEXT_UI.md`

### Loading a Premade Template
1. User clicks "Load Premade Graph".
2. Frame shows `PremadeGraphsDialog`.
3. Dialog displays template previews from `GraphTemplateLibrary`.
4. User selects template; dialog closes.
5. Frame calls `controller.loadGraphTemplate(template)`.
6. Controller materializes template: creates vertices, edges, positions.
7. Graph is updated; UI renders.

**Files involved**: `AI_CONTEXT_UI_TEMPLATES.md`, `AI_CONTEXT_UI_DIALOGS.md`, `AI_CONTEXT_UI.md`

## Important Conventions

### Pseudocode Line Indexing
- Pseudocode lines are 0-indexed.
- `activePseudocodeLine` in step refers to 0-based index.
- UI highlights line at this index in displayed pseudocode.

### Vertex ID Stability
- Vertex IDs are immutable and unique within a graph.
- IDs are canonical identity across all packages.
- Never use object references as keys; always use IDs.
- IDs are 0-indexed internally, 1-indexed in UI display.

### Color Indexing
- Colors are non-negative integers (0, 1, 2, ...).
- Integer maps to RGB via `ColorClassPalette`.
- Palette is shared globally; consistent indexing required.

### Edge Identification
- Edges are identified by `EdgeKey` (source ID + dest ID pair).
- Stable across graph mutations.
- Used in step overlays and subroutine references.

### Request Determinism
- `AlgorithmRequest` sorts vertices and edges deterministically.
- Algorithm outputs are reproducible given identical request.
- Never assume input order; sort if needed.

## Performance Notes

### Timeline Rendering
- Large timelines (1000+ steps) may consume 10-50 MB.
- Matrix rendering for 100x100 takes ~50-100ms.
- Most algorithms complete in <500ms for <100 vertices.

### Algorithm Complexity Rough Guides
- **BFS/DFS**: O(V+E), <10ms for 100 vertices
- **Dijkstra**: O((V+E) log V), ~50ms for 100 vertices
- **Bellman-Ford**: O(VE), ~100ms for 100 vertices
- **Floyd-Warshall**: O(V^3), ~500ms for 100 vertices
- **Exact Coloring**: Exponential, <1s for <30 vertices

## Modification Safety Guide

### Safe Changes
- Add new algorithm (implement `GraphAlgorithm`, register)
- Add new viewer dialog (listen to playback controller)
- Add new template (define in library)
- Add new handler for extras operation
- Enhance visualization (colors, labels, fonts)
- Add optional fields to classes

### Dangerous Changes
- Modify playback controller step logic
- Change `AlgorithmRequest` sorting order
- Remove DAG validation from DAG-only algorithms
- Modify frame stack ordering in `StackFramesSnapshot`
- Change timeline immutability contract
- Remove listener notifications in controller
- Modify residual capacity update logic in max-flow

## Where to Start

**For adding a new algorithm**:
1. Read `AI_CONTEXT_ALGORITHM.md` for framework understanding
2. Read relevant subpackage file (e.g., `AI_CONTEXT_ALGORITHM_SSSP.md`)
3. Implement `GraphAlgorithm` interface in appropriate subpackage
4. Register in `GraphAlgorithmRegistry`
5. See "Typical Change Recipes" in subpackage file

**For modifying UI**:
1. Read `AI_CONTEXT_UI.md` for overall architecture
2. Read `AI_CONTEXT_UI_DIALOGS.md` or `AI_CONTEXT_UI_EXTRAS.md` depending on change
3. Understand listener patterns and reactive updates
4. See "Typical Change Recipes" in relevant file

**For understanding existing system**:
1. Start with `AI_CONTEXT_ALGORITHM.md` and `AI_CONTEXT_GRAPH.md`
2. Read `AI_CONTEXT_UI.md` for UI orchestration
3. Dive into specific subpackage files as needed
4. Each file has "Cross-Package Relationships" section showing dependencies

## File Format

Each AI_CONTEXT file includes:

1. **Package Purpose** - One sentence summary
2. **Package Mental Model** - 5-20 bullet points explaining core concepts
3. **Package Boundaries** - Input/output/owns/depends/forbidden dependencies
4. **Important Classes** - Table of key classes with roles, dependencies, usage, risk levels
5. **Important Interfaces** - Table of contracts and implementations
6. **Runtime Flows** - Step-by-step algorithm execution or user interaction flows
7. **Important State / Data Structures** - Invariants and important fields
8. **Hidden Assumptions** - Implicit contracts that future developers must understand
9. **Concurrency Notes** - Threading, synchronization, async behavior
10. **Performance Notes** - Hot paths, expensive operations, caching
11. **Modification Guide** - Safe/dangerous changes, preferred patterns, anti-patterns
12. **Typical Change Recipes** - Compact step-by-step recipes for common changes
13. **Cross-Package Relationships** - Dependencies and usage relationships
14. **AI Quick Reference** - Important entry points, APIs, services, constants

## Future AI Agent Usage

These files are designed for AI agents and future developers:

- **Minimal context window**: Each file is self-contained and can fit in typical LLM context limits
- **Structural navigation**: Tables and bullet points enable efficient scanning
- **Explicit assumptions**: Hidden assumptions section surfaces non-obvious invariants
- **Safety guidance**: Modification guide sections highlight dangerous changes
- **Recipe format**: Typical change recipes are compact and actionable
- **Cross-references**: Each file links to related files for deeper understanding

When reading a file, focus on the **Mental Model** and **Modification Guide** sections for quick orientation, then dive into relevant **Runtime Flows** and **Important Classes** sections for details.


