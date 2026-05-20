# `algorithm.recursion` Package

<a id="pkg-algorithm-recursion"></a>

This package bridges recursive algorithm internals to the generic stack-frame view used by the UI.

It exists so recursive algorithms can keep their own internal frame representations while still emitting a clean, reusable snapshot format.

## Package responsibilities

- define a generic adapter contract for recursion state,
- provide immutable stack-frame models,
- and let algorithms publish stack snapshots to the UI.

## How this package connects to the rest of the system

- used by recursive algorithms in `algorithm.sssp` and `algorithm.general`,
- consumed by `ui.dialogs.AlgorithmStackFramesDialog`,
- and attached to `algorithm.AlgorithmStep` objects.

## `RecursiveStackAdapter`

<a id="cls-algorithm-recursion-recursivestackadapter"></a>

A generic conversion interface.

Implementations accept algorithm-specific recursion state and return a `StackFramesSnapshot`.

This is the key abstraction that keeps the UI decoupled from algorithm internals.

## `StackFrameView`

<a id="cls-algorithm-recursion-stackframeview"></a>

Immutable representation of one stack frame.

**Contains**

- a frame/method name,
- and a map of variable names to string values.

## `StackFramesSnapshot`

<a id="cls-algorithm-recursion-stackframessnapshot"></a>

Immutable ordered stack payload.

**Important behavior**

- frames are stored top-first,
- the UI can render them without knowing the original recursion type,
- and the snapshot is safe to share across the timeline.

## `DfsStackFrameAdapter`

<a id="cls-algorithm-recursion-dfsstackframeadapter"></a>

Maps DFS recursion state into generic stack frames.

**What it exposes**

- current vertex name,
- recursion depth,
- current phase,
- and time value.

## `SeidelStackFrameAdapter`

<a id="cls-algorithm-recursion-seidelstackframeadapter"></a>

Maps Seidel recursion state into generic stack frames.

**What it exposes**

- recursion depth,
- current problem size,
- and the current phase of the recursive call.

## Package-level design choices

- Adapter pattern keeps the UI generic.
- Frame data is immutable once it is captured.
- The stack viewer only needs a list of frames and labels; it does not need algorithm-specific classes.

