# Package Purpose

Dialog and embeddable-panel components for secondary UI workflows. Contains modal configuration dialogs and modeless algorithm viewers. Most classes provide embeddable panels so same UI content works in floating windows and workspace tabs.

# Package Mental Model

- **Modal Dialogs**: Configuration dialogs (algorithm run dialog, premade graph picker) block user until dismissed. Results are read on close.
- **Modeless Viewers**: Algorithm viewers (pseudocode, matrix, stack frames, residual graph) are non-blocking and update continuously as user steps through playback.
- **Embeddable Panels**: Viewers expose `JPanel` content that can be embedded in dialogs or workspace tabs. Workspace reuses same viewer for tabbed layout.
- **Playback Synchronization**: Viewers listen to `AlgorithmPlaybackController` and update whenever step changes.
- **Data-Driven Rendering**: Each viewer reads current step data and renders accordingly. No state maintained in viewer.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | User input (dialog selections), playback step changes |
| Outputs | Dialog results (selected parameters, chosen graph), rendered algorithm views |
| Owns | All dialog and viewer classes |
| Depends On | `algorithm` (playback state, step data), `graph` (for template selection), `ui` (for templates) |
| Must NOT Depend On | Any external packages beyond listed |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `AlgorithmRunDialog` | Modal for selecting algorithm and parameters | `GraphAlgorithmRegistry`, `AlgorithmRunConfiguration` | Frame | MEDIUM | User input for algorithm choice, source/sink, parameters |
| `AlgorithmRunConfiguration` | Data object capturing user's algorithm choice | - | Dialog, Frame | LOW | Immutable container for user selections |
| `AlgorithmPseudocodeDialog` | Modeless pseudocode + subroutine viewer | `AlgorithmPlaybackController` | Frame, workspace | MEDIUM | Highlights active pseudocode line; expandable subroutine tabs |
| `AlgorithmMatrixDialog` | Modeless matrix snapshot viewer | `AlgorithmPlaybackController` | Frame, workspace | LOW | Renders distance/flow matrix from step |
| `AlgorithmStackFramesDialog` | Modeless stack frame visualization | `AlgorithmPlaybackController` | Frame, workspace | LOW | Displays recursive call stack for algorithms like DFS |
| `ResidualGraphDialog` | Modeless residual graph viewer (max-flow) | `AlgorithmPlaybackController` | Frame, workspace | LOW | Renders residual capacities for flow algorithms |
| `LevelGraphDialog` | Modeless level graph viewer (Dinic) | `AlgorithmPlaybackController` | Frame, workspace | LOW | Renders level structure for Dinic algorithm |
| `PremadeGraphsDialog` | Modal for selecting and loading templates | `GraphTemplateLibrary` | Frame | LOW | User picks premade graph; loads into editor |
| `SelectionExtrasDialog` | Modal for extras operations (coloring, transforms) | Action handlers | Frame | LOW | Displays menu of extras operations |
| `ShortcutsDialog` | Modal showing keyboard shortcuts | - | Frame | LOW | Reference; no user input |
| `TopologicalOrderDialog` | Modeless viewer for topological sort output | `AlgorithmPlaybackController` | Frame, workspace | LOW | Shows vertex ordering result |
| `AlgorithmListCellRenderer` | Custom renderer for algorithm picker | - | `AlgorithmRunDialog` | LOW | Displays algorithm metadata nicely |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| - | Dialogs have no shared interfaces | - | STABLE |

# Runtime Flows

## Launching Algorithm Run
1. User clicks "Run Algorithm" button.
2. Frame creates and shows `AlgorithmRunDialog`.
3. Dialog displays list of algorithms in picker.
4. User selects algorithm, source/sink vertices, any parameters.
5. User clicks OK.
6. Dialog returns `AlgorithmRunConfiguration` with selections.
7. Frame uses configuration to build request and run algorithm.

## Viewing Pseudocode During Playback
1. Frame creates `AlgorithmPseudocodeDialog`.
2. Dialog registers as listener on playback controller.
3. Algorithm starts; playback controller notifies listeners.
4. Dialog updates: highlights active pseudocode line, displays message.
5. User steps forward/backward.
6. Playback controller notifies; dialog updates current line.
7. If step has subroutine ID, dialog displays "Open Sub-Routine" button.
8. User clicks button; playback switches to subroutine.
9. Dialog updates to show subroutine pseudocode.

## Viewing Matrix Snapshot
1. Algorithm with matrix payloads runs (e.g., Floyd-Warshall).
2. Frame creates `AlgorithmMatrixDialog`.
3. Dialog registers as listener on playback controller.
4. User steps; playback controller notifies.
5. Dialog checks if current step has `MatrixSnapshot`.
6. If yes: render matrix as table. If no: show empty message.
7. Continue stepping: matrix updates with each new step's data.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| `AlgorithmRunConfiguration` | User's algorithm choice | `algorithm`, `sourceVertex`, `sinkVertex`, `customParameters` | Algorithm never null; source always present; sink present if required |
| Dialog listeners | Callbacks from playback controller | Runnable listeners on controller | One listener per dialog; listeners are stateless |

# Hidden Assumptions

- **Dialogs are Transient**: Dialogs are created fresh for each operation. No state persists between operations.
- **Viewers Update Passively**: Viewers listen to controller and update when notified. They don't poll or maintain independent state.
- **Single Algorithm Result Loaded**: At any time, exactly one algorithm result is loaded in playback controller. Viewers display data from that result.
- **Pseudocode Line Indexing**: Pseudocode lines are 0-indexed in algorithm; dialogs assume this.
- **Subroutine Expansion is Optional**: Not all algorithms have subroutines. Dialogs check `callableSubroutineId` before showing button.

# Concurrency Notes

Package is single-threaded. All access is from EDT. Dialogs are created, used, and discarded synchronously. No background threads.

# Performance Notes

- **Dialog Creation**: Creating dialogs is fast (<10ms). Initial rendering varies by content (matrix dialogs render slowly for large matrices).
- **Matrix Rendering**: Rendering 100x100 matrix takes ~50-100ms. Larger matrices may be slow.
- **List Rendering**: Algorithm picker with 20 algorithms renders quickly (<5ms).

# Modification Guide For AI Assistants

## Safest Changes

- **Add new non-critical viewer**: Create new dialog class listening to playback controller. No changes to existing dialogs needed.
- **Enhance existing viewer rendering**: Add colors, fonts, labels to matrix/frame/residual graph renderers.
- **Add new dialog**: Create new modal for new workflow (e.g., algorithm parameter configuration). Hook into frame.

## Dangerous Changes

- **Do NOT modify playback controller listener registration**: If listeners aren't notified, dialogs won't update.
- **Do NOT assume step data types**: Check if step has payload before accessing (e.g., `if (step.getMatrixSnapshot() != null)`).
- **Do NOT maintain state across dialog instances**: Each dialog should be stateless. Multiple dialogs of same type should all render identically from same playback controller state.

## Preferred Patterns

- **Use factory methods in frame**: Frame should create and manage dialogs, not viewers themselves.
- **Register listeners immediately after creation**: Dialogs register with controller before showing.
- **Render defensive**: Always check if data is present before rendering. Gracefully handle missing data.
- **Use consistent layout**: All viewers should follow similar layout patterns (title, content, status).

# Typical Change Recipes

### Add New Viewer Dialog
1. Create `algorithm.dialogs.NewViewerDialog` extending `JDialog`.
2. In constructor, create content panel and register listener:
   ```java
   playbackController.addChangeListener(() -> updateView());
   ```
3. Implement `updateView()`:
   ```java
   AlgorithmStep step = playbackController.getCurrentStep();
   if (step == null) {
       contentPanel.setText("No algorithm running");
   } else {
       // Render step data
   }
   ```
4. In `GraphEditorFrame`, create and show dialog:
   ```java
   new NewViewerDialog(this, playbackController).setVisible(true);
   ```

### Add Algorithm Parameter Input
1. Add UI component to `AlgorithmRunDialog` (e.g., spinner for parameter).
2. In dialog's OK handler, read component value:
   ```java
   int paramValue = (Integer) spinner.getValue();
   AlgorithmRunConfiguration config = new AlgorithmRunConfiguration(algo, source, sink, paramValue);
   ```
3. Return config to frame.
4. Frame uses config to build request with parameter.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm` | **Consumes** | Reads playback controller state and step data |
| `ui` | **Used By** | Frame creates and manages dialogs |
| `ui.templates` | **Consumes** | Premade graph picker reads template library |
| `ui.extras` | **Consumes** | Selection extras dialog launches action handlers |

# AI Quick Reference

## Entry Points
- `AlgorithmRunDialog(Frame, GraphAlgorithmRegistry)` — algorithm picker
- `AlgorithmPseudocodeDialog(Frame, AlgorithmPlaybackController)` — pseudocode viewer
- `PremadeGraphsDialog(Frame, GraphEditorController)` — template picker

## Important Pattern
- Dialogs register listeners on controller in constructor
- Listeners are called whenever playback controller state changes
- Dialogs should gracefully handle missing data

## Important Classes
- `AlgorithmRunConfiguration` — immutable result from run dialog
- Viewer dialogs all follow similar update pattern


