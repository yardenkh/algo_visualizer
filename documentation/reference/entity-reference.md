# Entity Reference (Class-Level)

<a id="doc-entity-reference"></a>

This page provides a class-by-class index with concise responsibility statements and the primary documentation location.

For full package context, use the `documentation/packages/` pages.

## `src` root

- `Main` - Swing startup entry point. See `documentation/packages/app-main.md#cls-main`.

## `algorithm` core

- `AlgorithmPalette` - shared timeline color constants. See `documentation/packages/algorithm.md#cls-algorithm-algorithmpalette`.
- `AlgorithmPlaybackController` - timeline stepping and sub-routine overlay state machine. See `documentation/packages/algorithm.md#cls-algorithm-algorithmplaybackcontroller`.
- `AlgorithmPlaybackSmokeTest` - no-dependency runtime smoke verification for playback and payloads. See `documentation/packages/algorithm.md`.
- `AlgorithmRequest` - immutable run input snapshot of scoped graph state. See `documentation/packages/algorithm.md#cls-algorithm-algorithmrequest`.
- `AlgorithmResult` - immutable algorithm output timeline + metadata + sub-routines. See `documentation/packages/algorithm.md#cls-algorithm-algorithmresult`.
- `AlgorithmStep` - immutable visual state at one timeline index. See `documentation/packages/algorithm.md#cls-algorithm-algorithmstep`.
- `AlgorithmSupport` - shared helpers for distance/cycle/topological and common step creation. See `documentation/packages/algorithm.md#cls-algorithm-algorithmsupport`.
- `AlgorithmType` - run-dialog filter taxonomy. See `documentation/packages/algorithm.md#cls-algorithm-algorithmtype`.
- `EdgeKey` - stable edge identity key for step overlays. See `documentation/packages/algorithm.md#cls-algorithm-edgekey`.
- `GraphAlgorithm` - interface contract for all algorithms. See `documentation/packages/algorithm.md#cls-algorithm-graphalgorithm`.
- `GraphAlgorithmRegistry` - central algorithm catalog exposed to UI. See `documentation/packages/algorithm.md#cls-algorithm-graphalgorithmregistry`.
- `GraphOrientationRequirement` - directed/undirected requirement enum. See `documentation/packages/algorithm.md#cls-algorithm-graphorientationrequirement`.
- `LevelGraphSnapshot` - Dinic level-graph visualization payload. See `documentation/packages/algorithm.md#cls-algorithm-levelgraphsnapshot`.
- `MatrixSnapshot` - matrix visualization payload. See `documentation/packages/algorithm.md#cls-algorithm-matrixsnapshot`.
- `ResidualGraphSnapshot` - residual network visualization payload. See `documentation/packages/algorithm.md#cls-algorithm-residualgraphsnapshot`.
- `SubroutineLibrary` - canonical sub-routine IDs and pseudocode definitions. See `documentation/packages/algorithm.md#cls-algorithm-subroutinelibrary`.
- `SubroutineStepCollector` - utility for collecting sub-routine steps and packaging result maps. See `documentation/packages/algorithm.md#cls-algorithm-subroutinestepcollector`.
- `SubroutineTimeline` - immutable sub-routine replay model. See `documentation/packages/algorithm.md#cls-algorithm-subroutinetimeline`.

## `algorithm.apsp`

- `FloydWarshallAlgorithm` - dynamic-programming APSP with matrix snapshots. See `documentation/packages/algorithm-apsp.md`.
- `JohnsonAlgorithm` - Bellman-Ford potentials plus repeated Dijkstra APSP. See `documentation/packages/algorithm-apsp.md`.
- `MatrixMultiplicationApspAlgorithm` - min-plus extension APSP variant. See `documentation/packages/algorithm-apsp.md`.
- `RecursiveDoublingApspAlgorithm` - repeated-squaring APSP variant. See `documentation/packages/algorithm-apsp.md`.
- `RepeatedDijkstraAlgorithm` - APSP via per-source Dijkstra demonstrations. See `documentation/packages/algorithm-apsp.md`.

## `algorithm.coloring`

- `EdgeColoringService` - exact proper edge coloring for selected scope. See `documentation/packages/algorithm-coloring.md`.
- `ExactColoringSolver` - DSATUR-style exact branch-and-bound solver. See `documentation/packages/algorithm-coloring.md`.
- `VertexColoringService` - exact proper vertex coloring for selected scope. See `documentation/packages/algorithm-coloring.md`.

## `algorithm.general`

- `AStarAlgorithm` - heuristic-search demonstration with timeline output. See `documentation/packages/algorithm-general.md`.
- `SeidelAlgorithm` - recursive APSP pedagogical timeline with matrix + stack payloads. See `documentation/packages/algorithm-general.md`.
- `TopologicalOrderAlgorithm` - Kahn-style ordering with indegree-focused steps. See `documentation/packages/algorithm-general.md`.
- `TransitiveClosureAlgorithm` - Warshall-style reachability closure with matrix steps. See `documentation/packages/algorithm-general.md`.

## `algorithm.maxflow`

- `DinicAlgorithm` - level graph + blocking flow max-flow algorithm. See `documentation/packages/algorithm-maxflow.md`.
- `EdmondsKarpAlgorithm` - BFS shortest augmenting path max-flow algorithm. See `documentation/packages/algorithm-maxflow.md`.
- `FordFulkersonAlgorithm` - DFS augmenting path max-flow algorithm. See `documentation/packages/algorithm-maxflow.md`.
- `MaxFlowSupport` - shared matrix/snapshot/annotation builder for max-flow steps. See `documentation/packages/algorithm-maxflow.md`.

## `algorithm.mst`

- `KruskalAlgorithm` - edge-sorted spanning forest construction. See `documentation/packages/algorithm-mst.md`.
- `PrimAlgorithm` - frontier-driven tree growth from a start vertex. See `documentation/packages/algorithm-mst.md`.

## `algorithm.recursion`

- `DfsStackFrameAdapter` - maps DFS internal recursion state to generic stack frames. See `documentation/packages/algorithm-recursion.md`.
- `RecursiveStackAdapter` - generic adapter contract for recursion snapshots. See `documentation/packages/algorithm-recursion.md`.
- `SeidelStackFrameAdapter` - maps Seidel internal recursion state to generic stack frames. See `documentation/packages/algorithm-recursion.md`.
- `StackFrameView` - immutable frame model (name + variables). See `documentation/packages/algorithm-recursion.md`.
- `StackFramesSnapshot` - immutable top-first frame list payload. See `documentation/packages/algorithm-recursion.md`.

## `algorithm.sssp`

- `BellmanFordAlgorithm` - negative-edge SSSP + cycle detection. See `documentation/packages/algorithm-sssp.md`.
- `BreadthFirstSearchAlgorithm` - queue-based traversal with component restarts. See `documentation/packages/algorithm-sssp.md`.
- `DagShortestPathAlgorithm` - topological-order-based DAG SSSP. See `documentation/packages/algorithm-sssp.md`.
- `DagValidation` - shared DAG warning formatter. See `documentation/packages/algorithm-sssp.md`.
- `DepthFirstSearchAlgorithm` - recursive traversal with stack payloads + DFS-VISIT sub-routine. See `documentation/packages/algorithm-sssp.md`.
- `DijkstraShortestPathAlgorithm` - non-negative weighted SSSP with RELAX sub-routine. See `documentation/packages/algorithm-sssp.md`.

## `graph`

- `DirectedGraph` - mutable directed graph container and structural operations. See `documentation/packages/graph.md`.
- `Edge` - directed weighted/color edge with immutable endpoints. See `documentation/packages/graph.md`.
- `Vertex` - vertex identity, adjacency, and reusable algorithm metadata fields. See `documentation/packages/graph.md`.

## `ui`

- `AlgorithmDetailsFormatter` - run-dialog explanation formatter. See `documentation/packages/ui.md`.
- `AlgorithmWorkspacePanel` - dock/tab workspace host for algorithm side views. See `documentation/packages/ui.md`.
- `EditorTool` - canvas interaction mode enum. See `documentation/packages/ui.md`.
- `GraphCanvasPanel` - interactive graph renderer + editing input layer. See `documentation/packages/ui.md`.
- `GraphEditorController` - graph mutation/selection/undo/clipboard core controller. See `documentation/packages/ui.md`.
- `GraphEditorFrame` - main app composition root and playback orchestration shell. See `documentation/packages/ui.md`.

## `ui.dialogs`

- `AlgorithmListCellRenderer` - display-friendly renderer for algorithm selection lists. See `documentation/packages/ui-dialogs.md`.
- `AlgorithmMatrixDialog` - matrix payload viewer. See `documentation/packages/ui-dialogs.md`.
- `AlgorithmPseudocodeDialog` - pseudocode + sub-routine tab viewer with callbacks. See `documentation/packages/ui-dialogs.md`.
- `AlgorithmRunConfiguration` - selected algorithm/source/sink tuple from run dialog. See `documentation/packages/ui-dialogs.md`.
- `AlgorithmRunDialog` - modal algorithm selection/configuration dialog. See `documentation/packages/ui-dialogs.md`.
- `AlgorithmStackFramesDialog` - recursion stack tree viewer. See `documentation/packages/ui-dialogs.md`.
- `LevelGraphDialog` - Dinic level-graph viewer. See `documentation/packages/ui-dialogs.md`.
- `PremadeGraphsDialog` - template picker with card previews and type filters. See `documentation/packages/ui-dialogs.md`.
- `ResidualGraphDialog` - residual-network viewer for max-flow steps. See `documentation/packages/ui-dialogs.md`.
- `SelectionExtrasDialog` - launcher dialog for selected-subgraph utility actions. See `documentation/packages/ui-dialogs.md`.
- `ShortcutsDialog` - read-only keyboard shortcut list viewer. See `documentation/packages/ui-dialogs.md`.
- `TopologicalOrderDialog` - ordered-vertex reveal viewer for topological runs. See `documentation/packages/ui-dialogs.md`.

## `ui.extras`

- `ColorClassPalette` - maps color classes to stable RGB colors. See `documentation/packages/ui-extras.md`.
- `ComplementGraphActionHandler` - runs complement transformation on selected subgraph. See `documentation/packages/ui-extras.md`.
- `EdgeColoringActionHandler` - invokes exact edge coloring and applies colors. See `documentation/packages/ui-extras.md`.
- `EdgeCrossingReductionService` - heuristic layout optimizer for crossing minimization. See `documentation/packages/ui-extras.md`.
- `FindRootGraphActionHandler` - reconstruct-root action wrapper for line-graph inputs. See `documentation/packages/ui-extras.md`.
- `LineGraphActionHandler` - line-graph transform action wrapper. See `documentation/packages/ui-extras.md`.
- `LineGraphRootService` - line graph recognition and root reconstruction engine. See `documentation/packages/ui-extras.md`.
- `ReduceEdgeCrossingsActionHandler` - action wrapper around layout improvement service. See `documentation/packages/ui-extras.md`.
- `VertexColoringActionHandler` - invokes exact vertex coloring and applies colors. See `documentation/packages/ui-extras.md`.

## `ui.templates`

- `GraphTemplate` - immutable premade graph model. See `documentation/packages/ui-templates.md`.
- `GraphTemplateLibrary` - static catalog of premade templates. See `documentation/packages/ui-templates.md`.
- `GraphTemplatePreviewPanel` - thumbnail renderer for template cards. See `documentation/packages/ui-templates.md`.
- `GraphTemplateType` - template category taxonomy for filtering. See `documentation/packages/ui-templates.md`.

