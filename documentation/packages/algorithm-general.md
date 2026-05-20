# `algorithm.general` Package

<a id="pkg-algorithm-general"></a>

This package collects algorithms that do not fit neatly into one of the specialized families.

It includes ordering, reachability, heuristic search, and a pedagogical recursive APSP demo.

## Package responsibilities

- host general-purpose graph algorithms,
- provide timelines that emphasize conceptual understanding,
- and support both matrix and recursion-oriented visual payloads.

## How this package connects to the rest of the system

- consumes `algorithm.AlgorithmRequest`,
- produces `algorithm.AlgorithmResult`,
- may emit matrix or stack snapshots,
- and is displayed by the same playback and dialog infrastructure as the family-specific algorithms.

## `TopologicalOrderAlgorithm`

<a id="cls-algorithm-general-topologicalorderalgorithm"></a>

Kahn-style topological ordering.

**What it shows**

- indegree counting,
- zero-indegree queueing,
- order construction,
- and cycle detection when the graph is not a DAG.

**Dialog integration**

The result is consumed by `ui.dialogs.TopologicalOrderDialog`, which visualizes the order as it is revealed.

## `TransitiveClosureAlgorithm`

<a id="cls-algorithm-general-transitiveclosurealgorithm"></a>

Warshall-style reachability closure.

**What it shows**

- boolean reachability matrix updates,
- the active `k/i/j` triple,
- and incremental reachability discoveries.

**Payload**

Produces `MatrixSnapshot` objects for the matrix dialog.

## `AStarAlgorithm`

<a id="cls-algorithm-general-astaralgorithm"></a>

A pedagogical A* search demonstration.

**What it shows**

- open-set behavior,
- g-score updates,
- goal detection,
- and heuristic-style exploration.

**Implementation note**

When no domain heuristic is available, this demo behaves like a Dijkstra-like search while preserving A* terminology.

## `SeidelAlgorithm`

<a id="cls-algorithm-general-seidelalgorithm"></a>

A recursive all-pairs shortest path demonstration for unweighted undirected graphs.

**What it shows**

- recursive depth,
- matrix reduction,
- combination of recursive results,
- and a conceptual call stack.

**Payloads**

Produces both `MatrixSnapshot` and `StackFramesSnapshot` so the matrix dialog and stack dialog can show the same step.

## Package-level design choices

- The algorithms favor explanation over mathematical brevity.
- Where recursion is central, the package uses the stack adapter layer to keep the UI independent of internal frame types.
- The package does not assume a single kind of output view; it uses whatever payload best explains the behavior.


