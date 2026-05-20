# `algorithm.mst` Package

<a id="pkg-algorithm-mst"></a>

This package contains the minimum spanning tree demonstrations.

The algorithms treat the selected graph as an undirected weighted structure and visualize how a spanning tree is constructed.

## Package responsibilities

- provide MST algorithms,
- show tree-building decisions step by step,
- and color accepted/rejected edges clearly in the playback view.

## How this package connects to the rest of the system

- consumes `algorithm.AlgorithmRequest`,
- produces `algorithm.AlgorithmResult`,
- uses shared algorithm colors from `algorithm.AlgorithmPalette`,
- and is rendered directly by the main playback UI.

## `PrimAlgorithm`

<a id="cls-algorithm-mst-primalgorithm"></a>

Prim’s algorithm grows one tree from a chosen start vertex.

**What it shows**

- frontier maintenance,
- cheapest-edge selection,
- accepted tree edges,
- and the evolving tree structure.

## `KruskalAlgorithm`

<a id="cls-algorithm-mst-kruskalalgorithm"></a>

Kruskal’s algorithm processes edges in sorted order and decides whether to accept or reject each one.

**What it shows**

- edge sorting,
- union/find-style decision making,
- accepted and rejected edges,
- and the formation of a spanning forest/tree.

## Package-level design choices

- The package is educational: it makes each accept/reject decision visible.
- It expects the selected scope to be compatible with an MST interpretation.
- It uses the shared timeline contract instead of a special MST-specific viewer.

