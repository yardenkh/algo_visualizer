# `algorithm.apsp` Package

<a id="pkg-algorithm-apsp"></a>

This package contains the all-pairs shortest path algorithms.

These algorithms compute distances for every ordered pair of vertices in the selected scope and typically rely on matrix-based playback views.

## Package responsibilities

- provide APSP implementations with educational step-by-step timelines,
- expose matrix snapshots for visual inspection,
- and demonstrate different algorithmic strategies for the same problem.

## How this package connects to the rest of the system

- consumes `algorithm.AlgorithmRequest`,
- produces `algorithm.AlgorithmResult`,
- often emits `algorithm.MatrixSnapshot`,
- and is displayed by `ui.dialogs.AlgorithmMatrixDialog`.

## `FloydWarshallAlgorithm`

<a id="cls-algorithm-apsp-floydwarshallalgorithm"></a>

The classic dynamic-programming APSP algorithm.

**What it shows**

- intermediate matrix updates,
- k/i/j iteration structure,
- and explicit state changes in the distance matrix.

## `MatrixMultiplicationApspAlgorithm`

<a id="cls-algorithm-apsp-matrixmultiplicationapspalgorithm"></a>

A min-plus matrix-extension style APSP demonstration.

**What it shows**

- matrix growth over repeated extension,
- the relation between path length and matrix multiplication-like operations,
- and the resulting distance matrix after each iteration.

## `RecursiveDoublingApspAlgorithm`

<a id="cls-algorithm-apsp-recursivedoublingapspalgorithm"></a>

A recursive-doubling / repeated-squaring style APSP algorithm.

**What it shows**

- doubling the path-length coverage,
- recursive matrix refinement,
- and traceable matrix snapshots at each major stage.

## `JohnsonAlgorithm`

<a id="cls-algorithm-apsp-johnsonalgorithm"></a>

Sparse-graph APSP using Bellman-Ford potentials plus per-source Dijkstra runs.

**What it shows**

- potential computation,
- reweighting,
- per-source shortest-path passes,
- and the final APSP table.

## `RepeatedDijkstraAlgorithm`

<a id="cls-algorithm-apsp-repeateddijkstraalgorithm"></a>

A direct repeated-SSSP APSP demonstration.

**What it shows**

- a separate Dijkstra run for every source,
- accumulated distance tables,
- and warning behavior when the graph violates the assumptions of the demo.

## Package-level design choices

- All implementations keep textbook-style naming for the UI.
- Matrix snapshots are immutable so the matrix dialog can render them without special-case logic.
- The algorithms prioritize explanatory clarity over micro-optimizations.

