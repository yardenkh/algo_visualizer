# Package Purpose

Root Swing UI package for graph editing and algorithm playback. Contains the main application shell and core editor components. Wires the graph model, algorithm framework, and all secondary views into one interactive desktop flow.

# Package Mental Model

- **Composition Root**: `GraphEditorFrame` is the main window that orchestrates all UI components and connects them via controllers.
- **Model-View Separation**: `GraphEditorController` holds all model mutation logic; painting/input handling is separate in `GraphCanvasPanel`.
- **Observer Pattern**: Components listen to controller changes via `addChangeListener()` callbacks. When graph/selection changes, all listeners are notified.
- **Playback Orchestration**: Frame manages algorithm execution lifecycle: run dialog → request building → algorithm execution → playback controller setup → updating all viewers.
- **Docked/Tabbed Workspace**: `AlgorithmWorkspacePanel` hosts secondary viewers (pseudocode, matrix, stack frames, etc.) in a tabbed or docked layout.
- **Rendering Pipeline**: `GraphCanvasPanel` handles all drawing (vertices, edges, labels, highlights) and receives input events (clicks, drags, keyboard).
- **Action Handlers**: Toolbar buttons and menu items trigger actions through controller or specialized handlers.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | User input (mouse/keyboard from canvas and dialogs), algorithm results from `algorithm` package |
| Outputs | Graph rendering, playback UI, status messages, dialogs for configuration/viewing |
| Owns | Main frame, canvas, workspace layout, primary editor state machine |
| Depends On | `graph` (model), `algorithm` (playback), `ui.dialogs`, `ui.extras`, `ui.templates` |
| Must NOT Depend On | Nothing problematic; dependencies are intentional |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `GraphEditorFrame` | Main application window and orchestrator | All UI components, algorithm | Application entry point | CRITICAL | Central composition root; changes here affect entire app flow |
| `GraphEditorController` | Model mutation logic (add/remove vertices, edges, selection) | `DirectedGraph` | Frame, canvas, dialogs | HIGH | All graph modifications go through here; undo/redo state machine lives here |
| `GraphCanvasPanel` | Rendering pipeline and direct manipulation input | `GraphEditorController` | Frame | HIGH | Handles all drawing and input events; responsible for responsiveness and correctness of canvas interaction |
| `AlgorithmWorkspacePanel` | Tabbed/docked container for algorithm viewers | Viewer dialogs | Frame | MEDIUM | Layout and tab management; content is provided by viewers |
| `EditorTool` | Enum of canvas tool modes (SELECT, PAN, ADD_VERTEX, CONNECT, etc.) | - | Frame, canvas | LOW | Stable enum; controls canvas interaction mode |
| `AlgorithmDetailsFormatter` | Utility to format algorithm info for display | - | Frame | LOW | Non-critical utility; formatting helper |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| - | UI package has no public interfaces | - | - |

# Runtime Flows

## Application Startup
1. `Main.main()` invokes `GraphEditorFrame()` constructor on EDT.
2. Frame creates `GraphEditorController`, `AlgorithmPlaybackController`, canvas, and workspace.
3. Frame builds toolbar with algorithm picker, tool buttons, playback controls.
4. Frame registers listeners on controller to update canvas/status on changes.
5. Frame calls `setVisible(true)`.
6. User interaction loop begins.

## User Editing a Vertex
1. User clicks canvas in ADD_VERTEX mode → `GraphCanvasPanel.mouseClicked()`.
2. Canvas translates click to graph coordinates.
3. Canvas calls `GraphEditorController.addVertex(x, y)`.
4. Controller creates new Vertex, stores position, adds to graph, snapshots for undo.
5. Controller notifies listeners → canvas repaints → vertex appears.

## User Running an Algorithm
1. User selects vertices/edges and chooses algorithm from "Run Algorithm" dialog.
2. Dialog calls `GraphEditorController.getSelectedVertices()` / `getSelectedEdges()`.
3. Frame builds `AlgorithmRequest` from selection and algorithm source/sink.
4. Frame calls `algorithm.run(request)` → gets `AlgorithmResult`.
5. Frame calls `playbackController.start(result)`.
6. Playback controller sets step index to 0.
7. Frame updates all viewers (pseudocode, matrix, canvas) with step 0.
8. User clicks "Step Forward" button → `playbackController.stepForward()` → listeners notified → viewers update.

## Canvas Rendering
1. `GraphCanvasPanel.paintComponent()` called by Swing repaint mechanism.
2. Canvas queries current playback step's coloring/annotations.
3. Canvas iterates vertices: draws vertex circle, applies color, draws name label.
4. Canvas iterates edges: draws line, applies color, draws weight/annotation label.
5. Canvas draws selection highlight (blue outline on selected vertices).
6. Canvas draws tool-specific overlays (e.g., preview edge being drawn, pan indicator).

## Tool Switching
1. User clicks toolbar button → frame calls `canvas.setActiveTool(newTool)`.
2. Canvas updates `activeTool` field and calls `repaint()`.
3. Next input event is interpreted according to new tool's rules.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| `GraphEditorFrame` | Main window | `controller`, `playbackController`, `canvas`, `workspace`, toolbar buttons | Single instance per app; never null after construction |
| `GraphEditorController` | Model state + mutation | `graph`, `positions`, `selectedVertexIds`, `undoStack`, `clipboard` | Graph is never null; positions map keys are vertex IDs; undo stack bounded at 200 items |
| `GraphCanvasPanel` | Rendering state + input | `controller`, `activeTool`, `canvasScale`, `panX`, `panY`, `mouseOverVertex`, `previewEdge` | Scale must be > 0; pan coordinates can be any value |

# Hidden Assumptions

- **Single Graph Instance**: There is exactly one `DirectedGraph` per frame. Algorithms work on snapshots of this graph, not separate copies.
- **Canvas Coordinates vs. Graph Coordinates**: Canvas uses pixel coordinates with pan/zoom applied. Graph uses abstract Cartesian coordinates stored in `positions` map. Conversion happens in canvas.
- **Undo/Redo is Linear**: Undo stack is a simple list. Branching undo (tree-based) is not supported. Performing a new action after undo discards the "future."
- **Selection Scope Determines Algorithm Scope**: When no vertices are selected, "all vertices" are considered selected for algorithm input. When vertices are selected, only those vertices and edges between them are included.
- **Playback Does Not Modify Graph**: Running algorithm and stepping through playback do not modify the live graph. Graph state remains unchanged until next edit.

# Concurrency Notes

Package is single-threaded; all access is from the Event Dispatch Thread (EDT). Algorithms run synchronously on EDT, blocking UI until completion. For very large graphs (1000+ vertices) or expensive algorithms, UI may appear frozen during execution.

# Performance Notes

- **Canvas Rendering**: Rendering 100 vertices and 200 edges takes ~10-20ms. Rendering 1000 vertices takes ~50-100ms. This is acceptable for interactive frame rates.
- **Algorithm Execution**: Most algorithms complete in <500ms for graphs with <100 vertices. Expensive algorithms (e.g., exact graph coloring) may take 1-5 seconds.
- **Undo Stack**: Storing 200 undo snapshots for a 100-vertex graph consumes ~10-20 MB. Large graphs will hit the limit quickly.
- **Memory**: The UI holds references to all vertices, edges, and positions. Large graphs (1000+ vertices) consume 10-50 MB depending on edge density.

# Modification Guide For AI Assistants

## Safest Changes

- **Add new toolbar buttons**: Create new `JButton`, add to toolbar, register action listener. No changes to core logic needed.
- **Add new viewer dialogs**: Create new dialog class in `ui.dialogs`, integrate into workspace or as standalone window. Dialogs consume playback controller state.
- **Add keyboard shortcuts**: Register in `GraphEditorShortcutSupport` or local key bindings. Shortcuts trigger existing actions.
- **Change colors/fonts**: Modify rendering code in `GraphCanvasPanel` or dialog classes. Non-structural changes.

## Dangerous Changes

- **Do NOT modify playback controller start/step logic**: This controls stepping through timelines. Off-by-one errors will break playback.
- **Do NOT change coordinate transformation between canvas and graph**: Pan/zoom logic is fragile. Changes can cause rendering artifacts or incorrect input handling.
- **Do NOT remove listener notification in controller**: UI updates depend on listeners firing. Removing notifications will cause UI to become out-of-sync with model.
- **Do NOT modify undo/redo stack structure**: Undo is a critical user expectation. Breaking it will frustrate users.

## Preferred Patterns

- **Use controller for all model mutations**: Never reach directly into graph to modify it. Always go through controller so listeners are notified and undo is recorded.
- **Register change listeners for UI updates**: When adding new UI components that need to update when model changes, add a listener to controller.
- **Build dialogs with owner frame**: Dialogs should be modal children of frame, not standalone. This ensures proper window focus and lifecycle.
- **Validate user input early**: Check for null, empty selection, invalid algorithm parameters before executing. Provide clear error messages.

## Anti-Patterns

- **Direct mutation of graph or positions without controller**: Changes won't be visible in UI or recorded in undo.
- **Storing direct references to selected vertices**: Selected set changes dynamically. Cache `selectedVertexIds` instead, then look up vertices as needed.
- **Assuming canvas pixel size equals graph size**: Canvas has pan/zoom applied. Always use coordinate transformation methods.

# Typical Change Recipes

### Add New Tool Mode
1. Add enum constant to `EditorTool` (e.g., `MY_NEW_TOOL`).
2. Add toolbar button in `GraphEditorFrame.buildToolbar()`.
3. Register button listener to call `canvas.setActiveTool(EditorTool.MY_NEW_TOOL)`.
4. In `GraphCanvasPanel.mouseClicked()`, add case for `MY_NEW_TOOL` handling.
5. In `paintComponent()`, add rendering logic if tool needs visual feedback.

### Add Algorithm Viewer to Workspace
1. Create new viewer panel class in `ui.dialogs` or directly in workspace.
2. Implement method to update viewer with current playback step: `update(AlgorithmStep step)`.
3. In `AlgorithmWorkspacePanel`, add tab with viewer.
4. Register viewer as listener on playback controller so it updates when stepping.

### Add Keyboard Shortcut
1. In `GraphEditorFrame`, call `canvas.getInputMap().put(KeyStroke.getKeyStroke("Ctrl+G"), "myAction")`.
2. Call `canvas.getActionMap().put("myAction", new AbstractAction() { ... })`.
3. Define action logic inside. Action can invoke controller methods or trigger dialog.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `graph` | **Owns** | Controller manages graph mutations on behalf of UI |
| `algorithm` | **Orchestrates** | Frame orchestrates algorithm runs and playback |
| `ui.dialogs` | **Uses** | Frame launches dialogs for configuration and viewing |
| `ui.extras` | **Uses** | Frame invokes extras handlers for graph transforms |
| `ui.templates` | **Uses** | Frame loads premade templates into editor |

# AI Quick Reference

## Entry Points
- `GraphEditorFrame()` — main window constructor
- `GraphEditorFrame.main(String[])` via `Main.java`
- `GraphEditorController.addVertex()`, `addEdge()`, `removeVertex()`, `removeEdge()` — model mutations
- `AlgorithmPlaybackController.start()`, `stepForward()`, `stepBackward()` — playback control

## Important APIs
- `GraphEditorController.getSelectedVertexIds()` → `Set<Integer>`
- `GraphEditorController.getSelectedEdges()` → `List<Edge>`
- `GraphCanvasPanel.setActiveTool(EditorTool)` — switch tool mode
- `GraphCanvasPanel.repaint()` — trigger canvas redraw
- `AlgorithmPlaybackController.getCurrentStep()` → `AlgorithmStep`
- `AlgorithmPlaybackController.addChangeListener(Runnable)` — register for playback updates

## Important Components
- `GraphEditorFrame.canvas` — main rendering and input surface
- `GraphEditorFrame.controller` — model mutation logic
- `GraphEditorFrame.playbackController` — playback state
- `GraphEditorFrame.workspace` — tabbed/docked viewer container
- `GraphEditorFrame.statusLabel` — status message display


