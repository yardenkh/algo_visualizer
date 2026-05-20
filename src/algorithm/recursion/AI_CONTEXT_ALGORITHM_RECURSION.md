# Package Purpose

Recursion visualization adapters and stack payload models. Bridges algorithm-specific recursion state to a generic UI representation. Recursive algorithms keep their own internal frame models and use adapters here to emit immutable stack frame snapshots.

# Package Mental Model

- **Adapter Pattern**: Recursive algorithms define their own frame models (e.g., DFS call stack, Seidel recursion frames). Adapters convert this internal state to generic `StackFramesSnapshot` for UI display.
- **Immutable Snapshots**: Snapshots are immutable and attached to algorithm steps, enabling stateless playback.
- **Frame Ordering**: Frames are ordered top-first (most recent call first) to match typical UI stack viewer conventions.
- **Frame Information**: Each frame shows call parameters (e.g., vertex ID, depth), local variables (e.g., discovery time), and return values.
- **Algorithm-Agnostic UI**: UI dialogs (`AlgorithmStackFramesDialog`) render frames generically without knowing algorithm-specific details.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | Algorithm-specific frame models (DFS call stack, Seidel frames, etc.) |
| Outputs | `StackFramesSnapshot` (immutable, generic) and `StackFrameView` (single frame) |
| Owns | Adapter implementations (DFS, Seidel, generic base), frame snapshot classes |
| Depends On | `algorithm` (AlgorithmStep integration) |
| Must NOT Depend On | `ui` |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `StackFrameView` | Single frame representation | - | `StackFramesSnapshot` | LOW | Immutable data object for one frame |
| `StackFramesSnapshot` | Immutable collection of frames | `StackFrameView` | `AlgorithmStep` | LOW | Transport object; immutable |
| `RecursiveStackAdapter` | Base adapter interface | - | Subclasses | LOW | Defines conversion contract |
| `DfsStackFrameAdapter` | Adapter for DFS recursion state | - | `DepthFirstSearchAlgorithm` | MEDIUM | Converts DFS call stack to snapshots |
| `SeidelStackFrameAdapter` | Adapter for Seidel recursion state | - | `SeidelAlgorithm` | MEDIUM | Converts Seidel call stack to snapshots |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| `RecursiveStackAdapter` | Adapter contract for converting frame state to snapshots | `DfsStackFrameAdapter`, `SeidelStackFrameAdapter` | SEMI-STABLE |

# Runtime Flows

## DFS Recursion Visualization
1. DFS algorithm maintains call stack of recursive calls (one frame per active DFS-VISIT call).
2. When emitting step, algorithm calls `DfsStackFrameAdapter.toSnapshot(stack)`.
3. Adapter converts each call on stack to `StackFrameView`:
   - Frame shows vertex ID, discovery time, current depth.
   - Top frame is most recent call.
4. Adapter returns `StackFramesSnapshot` with ordered frame list.
5. Step is created with snapshot attached:
   ```java
   AlgorithmStep step = new AlgorithmStep(..., colors, annotations, null, snapshot);
   ```
6. UI renders snapshot in stack frames viewer.

## Seidel Recursion Visualization
1. Seidel algorithm maintains recursion call stack for divide-and-conquer.
2. When emitting step, algorithm calls `SeidelStackFrameAdapter.toSnapshot(stack)`.
3. Adapter converts each call to `StackFrameView`:
   - Frame shows vertex set, recursion depth, current sub-problem.
4. Adapter returns `StackFramesSnapshot`.
5. Step includes snapshot.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| `StackFrameView` | Single call frame | `methodName`, `parameters`, `localVariables`, `returnValue` | Immutable; all fields are strings or immutable objects |
| `StackFramesSnapshot` | Collection of frames | `List<StackFrameView>` | Ordered top-first; never null |

# Hidden Assumptions

- **Adapter is Stateless**: Adapters should not maintain state between calls. Each call to `toSnapshot()` is independent.
- **Frame String Representation**: All frame information is converted to strings for UI display. Algorithms should format information clearly.
- **Top-First Ordering**: UI expects most recent call first. Adapters must reverse call stack order if needed.

# Concurrency Notes

Package is single-threaded. Adapters are called synchronously from algorithm execution on EDT. No concurrency.

# Performance Notes

- **Adapter Conversion**: O(depth of call stack) to convert stack to snapshots. Usually < 50 frames, so < 1ms.
- **Memory**: `StackFrameView` strings consume memory. For deep recursion (100+ frames), snapshot can consume 1-5 MB.

# Modification Guide For AI Assistants

## Safest Changes

- **Add new adapter for new recursive algorithm**: Create new class implementing `RecursiveStackAdapter`, follow same pattern as DFS/Seidel.
- **Enhance frame information**: Add more details to `StackFrameView` string representation (e.g., more local variables).
- **Optimize frame collection**: Cache snapshots or reuse frame objects where possible.

## Dangerous Changes

- **Do NOT modify frame ordering**: Top-first ordering is UI convention. Changing it will confuse users.
- **Do NOT remove immutability**: Snapshots are frozen at step time. Mutating snapshots will corrupt playback history.

## Preferred Patterns

- **Keep adapter stateless**: Adapters should be functions, not stateful objects. Each call should produce identical output for identical input.
- **Use descriptive frame names**: Frame names should match algorithm documentation (e.g., "DFS-VISIT", "SEIDEL-APSP-RECURSIVE").
- **Include both input and output info**: Show parameters at entry, local state during execution, and return values if meaningful.

# Typical Change Recipes

### Add Adapter for New Recursive Algorithm
1. Create `algorithm.recursion.NewRecursiveStackAdapter` implementing `RecursiveStackAdapter`.
2. Implement `toSnapshot(Object state)`:
   ```java
   public StackFramesSnapshot toSnapshot(List<CustomFrame> stack) {
       List<StackFrameView> frames = new ArrayList<>();
       for (int i = stack.size() - 1; i >= 0; i--) {
           CustomFrame frame = stack.get(i);
           String info = String.format("MYFUNC(%s), depth=%d", frame.getParam(), i);
           frames.add(new StackFrameView(info));
       }
       return new StackFramesSnapshot(frames);
   }
   ```
3. In algorithm, use adapter:
   ```java
   StackFramesSnapshot snapshot = adapter.toSnapshot(currentCallStack);
   AlgorithmStep step = new AlgorithmStep(..., colors, annotations, null, snapshot);
   ```

### Enhance DFS Frame Information
1. Modify `DfsStackFrameAdapter` to include more details:
   ```java
   String info = String.format("DFS-VISIT(%d), discovery=%d, current_neighbor=%s", 
       frame.vertexId, frame.discoveryTime, frame.currentNeighbor);
   ```
2. UI will display enhanced information in stack viewer.

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `algorithm` | **Consumed By** | Adapters produce snapshots attached to `AlgorithmStep` |
| `algorithm.sssp`, `algorithm.general` | **Used By** | Recursive algorithms (DFS, Seidel) use adapters |
| `ui.dialogs` | **Consumed By** | Snapshots displayed by `AlgorithmStackFramesDialog` |

# AI Quick Reference

## Entry Points
- `RecursiveStackAdapter.toSnapshot(Object state)` — generic conversion contract
- `DfsStackFrameAdapter.toSnapshot(List<DfsFrame>)` — DFS-specific
- `SeidelStackFrameAdapter.toSnapshot(List<SeidelFrame>)` — Seidel-specific

## Important Classes
- `StackFrameView` — immutable single frame
- `StackFramesSnapshot` — immutable collection of frames

## Pattern
- Adapters are stateless converters
- Frames are top-first in snapshot (most recent call first)


