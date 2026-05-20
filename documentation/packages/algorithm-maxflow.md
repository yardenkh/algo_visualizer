# `algorithm.maxflow` Package

<a id="pkg-algorithm-maxflow"></a>

This package contains the maximum-flow algorithms and the shared support code that builds residual and level graph snapshots.

It is one of the most UI-integrated parts of the algorithm layer because its steps feed specialized side panels.

## Package responsibilities

- implement augmenting-path and blocking-flow algorithms,
- create residual-network and level-graph visualization payloads,
- and expose callable sub-routines for augmentation and blocking-path logic.

## How this package connects to the rest of the system

- consumes `algorithm.AlgorithmRequest`,
- produces `algorithm.AlgorithmResult`,
- emits `algorithm.ResidualGraphSnapshot` and `algorithm.LevelGraphSnapshot`,
- and is rendered by `ui.dialogs.ResidualGraphDialog` and `ui.dialogs.LevelGraphDialog`.

## `MaxFlowSupport`

<a id="cls-algorithm-maxflow-maxflowsupport"></a>

Shared utility class for max-flow step generation.

**Responsibilities**

- build reusable `AlgorithmStep` objects,
- derive residual-graph snapshots from capacity/flow matrices,
- derive level-graph snapshots from BFS levels,
- and attach residual-edge highlights or callable sub-routine metadata.

This class keeps the three maximum-flow algorithms visually consistent.

## `FordFulkersonAlgorithm`

<a id="cls-algorithm-maxflow-fordfulkersonalgorithm"></a>

The DFS-based augmenting-path version.

**What it shows**

- residual path discovery,
- bottleneck calculation,
- augmentation along the path,
- and the updated flow values.

**Sub-routine integration**

Uses `AUGMENT` as a callable sub-routine so the pseudocode viewer can open the augmentation logic.

## `EdmondsKarpAlgorithm`

<a id="cls-algorithm-maxflow-edmondskarpalgorithm"></a>

The BFS-shortest-augmenting-path specialization of Ford-Fulkerson.

**What it shows**

- shortest augmenting path discovery in the residual graph,
- augmentation decisions,
- and the effect of each augmentation on the flow.

## `DinicAlgorithm`

<a id="cls-algorithm-maxflow-dinicalgorithm"></a>

Dinic’s algorithm adds a BFS-built level graph and DFS-based blocking flow search.

**What it shows**

- level graph construction,
- level-respecting path search,
- bottleneck evaluation,
- and blocking-flow augmentation.

**Sub-routine integration**

Uses `BFS_LAYER` as the callable sub-routine for the inner blocking-path search.

## Package-level design choices

- Capacity and flow are represented as matrices for predictable visualization.
- Every algorithm validates source, sink, orientation, and non-negative capacities.
- The UI can inspect the residual graph at each step without recomputing the network.

