# File Coverage Matrix (Complete Source Inventory)

<a id="doc-file-coverage"></a>

This matrix documents **every Java source file** currently in `src/`.

Columns:

- **File** - source path
- **Primary responsibility** - what the file contributes
- **Deep documentation** - main page to read next

## Root

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/Main.java` | App bootstrap entry point | `documentation/packages/app-main.md` |

## `src/algorithm`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/algorithm/AlgorithmPalette.java` | Shared playback color constants | `documentation/packages/algorithm.md` |
| `src/algorithm/AlgorithmPlaybackController.java` | Timeline stepping + sub-routine overlay lifecycle | `documentation/packages/algorithm.md` |
| `src/algorithm/AlgorithmPlaybackSmokeTest.java` | Playback/timeline smoke test harness | `documentation/packages/algorithm.md` |
| `src/algorithm/AlgorithmRequest.java` | Immutable scoped input snapshot for algorithms | `documentation/packages/algorithm.md` |
| `src/algorithm/AlgorithmResult.java` | Immutable run output and metadata carrier | `documentation/packages/algorithm.md` |
| `src/algorithm/AlgorithmStep.java` | Immutable step payload model | `documentation/packages/algorithm.md` |
| `src/algorithm/AlgorithmSupport.java` | Cross-algorithm helper utilities | `documentation/packages/algorithm.md` |
| `src/algorithm/AlgorithmType.java` | UI-facing algorithm category enum | `documentation/packages/algorithm.md` |
| `src/algorithm/EdgeKey.java` | Stable edge key for overlay maps | `documentation/packages/algorithm.md` |
| `src/algorithm/GraphAlgorithm.java` | Core algorithm contract interface | `documentation/packages/algorithm.md` |
| `src/algorithm/GraphAlgorithmRegistry.java` | Central algorithm catalog | `documentation/packages/algorithm.md` |
| `src/algorithm/GraphOrientationRequirement.java` | Directed/undirected requirement enum | `documentation/packages/algorithm.md` |
| `src/algorithm/LevelGraphSnapshot.java` | Dinic level graph visual payload | `documentation/packages/algorithm.md` |
| `src/algorithm/MatrixSnapshot.java` | Matrix visual payload | `documentation/packages/algorithm.md` |
| `src/algorithm/package-info.java` | Package-level framework overview | `documentation/packages/algorithm.md` |
| `src/algorithm/ResidualGraphSnapshot.java` | Residual network visual payload | `documentation/packages/algorithm.md` |
| `src/algorithm/SubroutineLibrary.java` | Canonical sub-routine ids and pseudocode | `documentation/packages/algorithm.md` |
| `src/algorithm/SubroutineStepCollector.java` | Utility to collect/build sub-routine timelines | `documentation/packages/algorithm.md` |
| `src/algorithm/SubroutineTimeline.java` | Sub-routine timeline model | `documentation/packages/algorithm.md` |

## `src/algorithm/apsp`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/algorithm/apsp/FloydWarshallAlgorithm.java` | Dynamic-programming APSP timeline | `documentation/packages/algorithm-apsp.md` |
| `src/algorithm/apsp/JohnsonAlgorithm.java` | Johnson APSP timeline | `documentation/packages/algorithm-apsp.md` |
| `src/algorithm/apsp/MatrixMultiplicationApspAlgorithm.java` | Min-plus matrix APSP variant | `documentation/packages/algorithm-apsp.md` |
| `src/algorithm/apsp/package-info.java` | APSP family package overview | `documentation/packages/algorithm-apsp.md` |
| `src/algorithm/apsp/RecursiveDoublingApspAlgorithm.java` | Recursive doubling APSP variant | `documentation/packages/algorithm-apsp.md` |
| `src/algorithm/apsp/RepeatedDijkstraAlgorithm.java` | Repeated Dijkstra APSP variant | `documentation/packages/algorithm-apsp.md` |

## `src/algorithm/coloring`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/algorithm/coloring/EdgeColoringService.java` | Exact edge coloring service | `documentation/packages/algorithm-coloring.md` |
| `src/algorithm/coloring/ExactColoringSolver.java` | Exact DSATUR-like branch-and-bound engine | `documentation/packages/algorithm-coloring.md` |
| `src/algorithm/coloring/package-info.java` | Coloring package overview | `documentation/packages/algorithm-coloring.md` |
| `src/algorithm/coloring/VertexColoringService.java` | Exact vertex coloring service | `documentation/packages/algorithm-coloring.md` |

## `src/algorithm/general`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/algorithm/general/AStarAlgorithm.java` | A* demonstration timeline | `documentation/packages/algorithm-general.md` |
| `src/algorithm/general/package-info.java` | General algorithms package overview | `documentation/packages/algorithm-general.md` |
| `src/algorithm/general/SeidelAlgorithm.java` | Recursive Seidel APSP demonstration | `documentation/packages/algorithm-general.md` |
| `src/algorithm/general/TopologicalOrderAlgorithm.java` | Kahn topological ordering timeline | `documentation/packages/algorithm-general.md` |
| `src/algorithm/general/TransitiveClosureAlgorithm.java` | Warshall transitive closure timeline | `documentation/packages/algorithm-general.md` |

## `src/algorithm/maxflow`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/algorithm/maxflow/DinicAlgorithm.java` | Dinic max-flow timeline | `documentation/packages/algorithm-maxflow.md` |
| `src/algorithm/maxflow/EdmondsKarpAlgorithm.java` | Edmonds-Karp max-flow timeline | `documentation/packages/algorithm-maxflow.md` |
| `src/algorithm/maxflow/FordFulkersonAlgorithm.java` | Ford-Fulkerson max-flow timeline | `documentation/packages/algorithm-maxflow.md` |
| `src/algorithm/maxflow/MaxFlowSupport.java` | Shared max-flow step/snapshot utilities | `documentation/packages/algorithm-maxflow.md` |
| `src/algorithm/maxflow/package-info.java` | Max-flow package overview | `documentation/packages/algorithm-maxflow.md` |

## `src/algorithm/mst`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/algorithm/mst/KruskalAlgorithm.java` | Kruskal MST timeline | `documentation/packages/algorithm-mst.md` |
| `src/algorithm/mst/package-info.java` | MST package overview | `documentation/packages/algorithm-mst.md` |
| `src/algorithm/mst/PrimAlgorithm.java` | Prim MST timeline | `documentation/packages/algorithm-mst.md` |

## `src/algorithm/recursion`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/algorithm/recursion/DfsStackFrameAdapter.java` | DFS state-to-stack adapter | `documentation/packages/algorithm-recursion.md` |
| `src/algorithm/recursion/package-info.java` | Recursion payload/adapter package overview | `documentation/packages/algorithm-recursion.md` |
| `src/algorithm/recursion/RecursiveStackAdapter.java` | Generic recursion adapter contract | `documentation/packages/algorithm-recursion.md` |
| `src/algorithm/recursion/SeidelStackFrameAdapter.java` | Seidel state-to-stack adapter | `documentation/packages/algorithm-recursion.md` |
| `src/algorithm/recursion/StackFramesSnapshot.java` | Immutable stack payload | `documentation/packages/algorithm-recursion.md` |
| `src/algorithm/recursion/StackFrameView.java` | Immutable stack frame model | `documentation/packages/algorithm-recursion.md` |

## `src/algorithm/sssp`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/algorithm/sssp/BellmanFordAlgorithm.java` | Bellman-Ford timeline | `documentation/packages/algorithm-sssp.md` |
| `src/algorithm/sssp/BreadthFirstSearchAlgorithm.java` | BFS timeline | `documentation/packages/algorithm-sssp.md` |
| `src/algorithm/sssp/DagShortestPathAlgorithm.java` | DAG shortest-path timeline | `documentation/packages/algorithm-sssp.md` |
| `src/algorithm/sssp/DagValidation.java` | DAG warning/validation utility | `documentation/packages/algorithm-sssp.md` |
| `src/algorithm/sssp/DepthFirstSearchAlgorithm.java` | DFS recursive timeline + stack payload | `documentation/packages/algorithm-sssp.md` |
| `src/algorithm/sssp/DijkstraShortestPathAlgorithm.java` | Dijkstra timeline + RELAX sub-routine | `documentation/packages/algorithm-sssp.md` |
| `src/algorithm/sssp/package-info.java` | SSSP package overview | `documentation/packages/algorithm-sssp.md` |

## `src/graph`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/graph/DirectedGraph.java` | Mutable directed graph container | `documentation/packages/graph.md` |
| `src/graph/Edge.java` | Directed edge model | `documentation/packages/graph.md` |
| `src/graph/package-info.java` | Graph package overview | `documentation/packages/graph.md` |
| `src/graph/Vertex.java` | Vertex model and reusable metadata fields | `documentation/packages/graph.md` |

## `src/ui`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/ui/AlgorithmDetailsFormatter.java` | Algorithm explanation formatter for run dialog | `documentation/packages/ui.md` |
| `src/ui/AlgorithmWorkspacePanel.java` | Unified workspace host for side views | `documentation/packages/ui.md` |
| `src/ui/EditorTool.java` | Canvas interaction mode enum | `documentation/packages/ui.md` |
| `src/ui/GraphCanvasPanel.java` | Interactive graph canvas renderer | `documentation/packages/ui.md` |
| `src/ui/GraphEditorController.java` | Main graph/editing state controller | `documentation/packages/ui.md` |
| `src/ui/GraphEditorFrame.java` | Main application frame and orchestration shell | `documentation/packages/ui.md` |
| `src/ui/package-info.java` | UI package overview | `documentation/packages/ui.md` |

## `src/ui/dialogs`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/ui/dialogs/AlgorithmListCellRenderer.java` | Human-readable algorithm list renderer | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/AlgorithmMatrixDialog.java` | Matrix viewer dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/AlgorithmPseudocodeDialog.java` | Pseudocode/sub-routine dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/AlgorithmRunConfiguration.java` | Immutable run selection result | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/AlgorithmRunDialog.java` | Algorithm chooser/configuration dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/AlgorithmStackFramesDialog.java` | Recursion stack viewer dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/LevelGraphDialog.java` | Dinic level graph viewer dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/package-info.java` | Dialogs package overview | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/PremadeGraphsDialog.java` | Premade graph picker dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/ResidualGraphDialog.java` | Residual network viewer dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/SelectionExtrasDialog.java` | Extras launcher dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/ShortcutsDialog.java` | Keyboard shortcuts viewer dialog | `documentation/packages/ui-dialogs.md` |
| `src/ui/dialogs/TopologicalOrderDialog.java` | Topological order visualization dialog | `documentation/packages/ui-dialogs.md` |

## `src/ui/extras`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/ui/extras/ColorClassPalette.java` | Color-class to RGB mapper | `documentation/packages/ui-extras.md` |
| `src/ui/extras/ComplementGraphActionHandler.java` | Complement transform action wrapper | `documentation/packages/ui-extras.md` |
| `src/ui/extras/EdgeColoringActionHandler.java` | Edge coloring action wrapper | `documentation/packages/ui-extras.md` |
| `src/ui/extras/EdgeCrossingReductionService.java` | Layout crossing reduction service | `documentation/packages/ui-extras.md` |
| `src/ui/extras/FindRootGraphActionHandler.java` | Root-graph reconstruction action wrapper | `documentation/packages/ui-extras.md` |
| `src/ui/extras/LineGraphActionHandler.java` | Line-graph conversion action wrapper | `documentation/packages/ui-extras.md` |
| `src/ui/extras/LineGraphRootService.java` | Line graph recognition/root reconstruction engine | `documentation/packages/ui-extras.md` |
| `src/ui/extras/package-info.java` | Extras package overview | `documentation/packages/ui-extras.md` |
| `src/ui/extras/ReduceEdgeCrossingsActionHandler.java` | Crossing reduction action wrapper | `documentation/packages/ui-extras.md` |
| `src/ui/extras/VertexColoringActionHandler.java` | Vertex coloring action wrapper | `documentation/packages/ui-extras.md` |

## `src/ui/templates`

| File | Primary responsibility | Deep documentation |
|---|---|---|
| `src/ui/templates/GraphTemplate.java` | Immutable template model | `documentation/packages/ui-templates.md` |
| `src/ui/templates/GraphTemplateLibrary.java` | Static catalog of premade templates | `documentation/packages/ui-templates.md` |
| `src/ui/templates/GraphTemplatePreviewPanel.java` | Template thumbnail renderer | `documentation/packages/ui-templates.md` |
| `src/ui/templates/GraphTemplateType.java` | Template type enum taxonomy | `documentation/packages/ui-templates.md` |
| `src/ui/templates/package-info.java` | Templates package overview | `documentation/packages/ui-templates.md` |

## Coverage summary

- Total Java files inventoried: **95**
- Package-info files included: **13**
- Inventory status: **full coverage of current `src/` tree**

