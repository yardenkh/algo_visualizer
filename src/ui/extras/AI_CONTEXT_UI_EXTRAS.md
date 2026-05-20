# Package Purpose

Selection → Extras action handlers and related UI-side utilities. Translates user-triggered extras operations into controller/service calls, then reports outcomes through dialog messages. Intentionally thin orchestration layers around reusable domain services.

# Package Mental Model

- **Action Handler Pattern**: Each extras operation (color vertices, complement graph, find root, etc.) has a corresponding handler class. Handlers orchestrate the operation.
- **Service Delegation**: Handlers don't implement logic; they delegate to reusable services (coloring services, graph transforms, etc.).
- **Selection-Based Scope**: Handlers operate on user's current selection (selected vertices/edges). Empty selection may mean "all vertices/edges" depending on operation.
- **Error Handling**: Handlers catch exceptions and report errors to user via dialogs. Failures don't crash app.
- **Utility Classes**: Non-handler classes like `ColorClassPalette` and `EdgeCrossingReductionService` are reusable helpers.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | User selection (selected vertices/edges), user triggers (menu clicks), graph state |
| Outputs | Graph modifications, error messages, operation results displayed in dialogs |
| Owns | Handler classes, utility services, palette management |
| Depends On | `algorithm.coloring` (coloring services), `graph` (model), `ui` (controller access) |
| Must NOT Depend On | Nothing problematic; dependencies are intentional |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `VertexColoringActionHandler` | Computes and applies exact vertex coloring | `VertexColoringService` | Frame/dialog | MEDIUM | Calls coloring service, applies result to selection |
| `EdgeColoringActionHandler` | Computes and applies exact edge coloring | `EdgeColoringService` | Frame/dialog | MEDIUM | Similar to vertex coloring |
| `ComplementGraphActionHandler` | Creates complement graph | - | Frame/dialog | LOW | Adds missing edges, removes existing edges |
| `LineGraphActionHandler` | Creates line graph (vertices = edges) | - | Frame/dialog | LOW | Transforms graph structure |
| `FindRootGraphActionHandler` | Finds root vertex for tree/forest | - | Frame/dialog | LOW | Identifies potential roots |
| `ReduceEdgeCrossingsActionHandler` | Applies heuristic to reduce visual edge crossings | `EdgeCrossingReductionService` | Frame/dialog | MEDIUM | Reorders vertices for better layout |
| `ColorClassPalette` | Maps color class indices to RGB colors | - | UI rendering | LOW | Palette used by graph rendering |
| `LineGraphRootService` | Utility for finding roots in line graphs | - | `LineGraphActionHandler`, controller | LOW | Domain logic for root finding |
| `EdgeCrossingReductionService` | Heuristic service for layout optimization | - | `ReduceEdgeCrossingsActionHandler` | LOW | Non-critical layout utility |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| - | No public interfaces; handler classes only | - | STABLE |

# Runtime Flows

## Vertex Coloring Operation
1. User selects vertices and clicks "Color Vertices" from extras menu.
2. Frame creates `VertexColoringActionHandler` and calls `run()`.
3. Handler calls `VertexColoringService.compute(selectedVertices)`.
4. Service runs exact coloring algorithm, returns coloring result.
5. Handler checks result: if valid, applies colors to graph.
   ```java
   for (Vertex v : selectedVertices) {
       v.setColor(coloringResult.getVertexColor(v));
   }
   ```
6. Handler notifies controller (graph changed).
7. UI updates and displays success message.

## Complement Graph Operation
1. User selects vertices and clicks "Complement Graph" from extras menu.
2. Frame creates `ComplementGraphActionHandler` and calls `run()`.
3. Handler iterates all pairs of selected vertices:
   - If edge exists: remove.
   - If edge doesn't exist and not same vertex: add.
4. Handler notifies controller (graph changed).
5. UI renders new edges.

## Reduce Edge Crossings Operation
1. User clicks "Reduce Edge Crossings" from extras menu.
2. Frame creates `ReduceEdgeCrossingsActionHandler` and calls `run()`.
3. Handler calls `EdgeCrossingReductionService.optimize(controller.getGraph())`.
4. Service reorders vertices to reduce visual crossings (heuristic).
5. Handler updates controller's vertex positions.
6. UI re-renders with improved layout.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| Color class (index) | Integer 0-based color identifier | Mapped to RGB by palette | Palette must have enough colors for all classes |

# Hidden Assumptions

- **Selection Determines Scope**: Handlers operate on selected vertices/edges. If nothing selected, assume "all" (depends on handler).
- **Handlers are Transient**: Created, used, then discarded. No state persists between operations.
- **Graph is Mutable**: Handlers modify live graph via controller. Changes are immediately visible in UI.
- **Coloring Services are Expensive**: Exact coloring can take several seconds. Handlers don't show progress; app may appear frozen.
- **Color Palette is Global**: All handlers use same color palette. Palette must have consistent indexing.

# Concurrency Notes

Package is single-threaded. All access is from EDT. Handlers execute synchronously; expensive operations like coloring may block UI.

# Performance Notes

- **Vertex Coloring**: Expensive; can take 1-10 seconds for 50+ vertices depending on graph density.
- **Complement Graph**: O(V^2) to check all pairs. Acceptable for <100 vertices.
- **Line Graph**: O(E) to create new vertices; O(E^2) to determine edges. Slow for dense graphs.
- **Edge Crossing Reduction**: Heuristic is O(V^2 + E). Acceptable for <500 vertices.

# Modification Guide For AI Assistants

## Safest Changes

- **Add new handler for new extras operation**: Create handler class, implement domain logic or delegate to service, register in frame.
- **Enhance color palette**: Add more colors to palette or make palette customizable.
- **Add new utility service**: Create service for reusable operation, have handlers call it.

## Dangerous Changes

- **Do NOT modify existing handler logic without testing**: Handlers modify graph. Mistakes will corrupt graph state.
- **Do NOT assume graph state remains stable during operation**: User may modify graph while handler runs (unlikely but possible).

## Preferred Patterns

- **Use handler pattern**: Keep orchestration thin. Delegate logic to services.
- **Validate selection early**: Check that selection is non-empty and valid before heavy computation.
- **Provide clear user feedback**: Report success/failure via dialog. Long operations should show progress.
- **Use controller for mutations**: All graph changes go through controller so undo is recorded.

# Typical Change Recipes

### Add New Extras Operation Handler
1. Create `algorithm.extras.NewOperationActionHandler`.
2. Implement `run()` method:
   ```java
   public void run(GraphEditorController controller) {
       List<Vertex> selected = controller.getSelectedVertices();
       if (selected.isEmpty()) {
           JOptionPane.showMessageDialog(null, "Please select vertices");
           return;
       }
       // Perform operation
       try {
           Result result = performOperation(selected);
           controller.notifyChange(); // Update UI
           JOptionPane.showMessageDialog(null, "Success: " + result);
       } catch (Exception e) {
           JOptionPane.showErrorDialog(null, "Error: " + e.getMessage());
       }
   }
   ```
3. In frame, invoke handler:
   ```java
   new NewOperationActionHandler().run(controller);
   ```

### Optimize Coloring with Timeout
1. Wrap `VertexColoringService.compute()` with timeout.
2. If timeout exceeded, report to user and use greedy approximation instead.
3. Handler displays "Timeout; used approximation algorithm" in result message.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm.coloring` | **Uses** | Coloring services perform exact algorithms |
| `graph` | **Mutates** | Graph modifications via controller |
| `ui` | **Called By** | Handlers invoked from frame/dialog |

# AI Quick Reference

## Entry Points
- `VertexColoringActionHandler.run(GraphEditorController)`
- `EdgeColoringActionHandler.run(GraphEditorController)`
- `ComplementGraphActionHandler.run(GraphEditorController)`
- `LineGraphActionHandler.run(GraphEditorController)`
- All handlers are stateless; call `run()` directly

## Important Pattern
- Handlers are orchestration layers
- Actual logic delegated to services
- All graph changes go through controller

## Important Services
- `VertexColoringService` — exact vertex coloring
- `EdgeColoringService` — exact edge coloring
- `LineGraphRootService` — root finding for line graphs
- `EdgeCrossingReductionService` — layout heuristic


