# Source Map

<a id="doc-source-map"></a>

This page maps the source tree to the corresponding documentation pages.

## Root entry points

| Source | Doc page | Notes |
|---|---|---|
| `src/Main.java` | `packages/app-main.md` | Application start-up entry point |
| `src/ui/GraphEditorFrame.java` | `packages/ui.md` | Main window and control hub |
| `src/ui/GraphEditorController.java` | `packages/ui.md` | Editing state and graph mutations |
| `src/algorithm/GraphAlgorithmRegistry.java` | `packages/algorithm.md` | Central algorithm catalog |

## Full coverage indexes

- `reference/entity-reference.md` - class-by-class inventory and pointers.
- `reference/file-coverage.md` - complete 95-file source coverage matrix.
- `hover-integration.md` - IDE hover behavior and setup notes.

## Package-to-doc mapping

| Source package | Documentation page |
|---|---|
| `src/graph` | `packages/graph.md` |
| `src/algorithm` | `packages/algorithm.md` |
| `src/algorithm/sssp` | `packages/algorithm-sssp.md` |
| `src/algorithm/apsp` | `packages/algorithm-apsp.md` |
| `src/algorithm/mst` | `packages/algorithm-mst.md` |
| `src/algorithm/maxflow` | `packages/algorithm-maxflow.md` |
| `src/algorithm/general` | `packages/algorithm-general.md` |
| `src/algorithm/recursion` | `packages/algorithm-recursion.md` |
| `src/algorithm/coloring` | `packages/algorithm-coloring.md` |
| `src/ui` | `packages/ui.md` |
| `src/ui/dialogs` | `packages/ui-dialogs.md` |
| `src/ui/extras` | `packages/ui-extras.md` |
| `src/ui/templates` | `packages/ui-templates.md` |

## Useful source-file anchors

These are the highest-value classes to link directly from source:

- `ui.GraphEditorFrame` -> `packages/ui.md#cls-ui-grapheditorframe`
- `ui.GraphEditorController` -> `packages/ui.md#cls-ui-grapheditorcontroller`
- `ui.GraphCanvasPanel` -> `packages/ui.md#cls-ui-graphcanvaspanel`
- `ui.AlgorithmWorkspacePanel` -> `packages/ui.md#cls-ui-algorithmworkspacepanel`
- `algorithm.AlgorithmRequest` -> `packages/algorithm.md#cls-algorithm-algorithmrequest`
- `algorithm.AlgorithmResult` -> `packages/algorithm.md#cls-algorithm-algorithmresult`
- `algorithm.AlgorithmStep` -> `packages/algorithm.md#cls-algorithm-algorithmstep`
- `algorithm.AlgorithmPlaybackController` -> `packages/algorithm.md#cls-algorithm-algorithmplaybackcontroller`
- `algorithm.GraphAlgorithmRegistry` -> `packages/algorithm.md#cls-algorithm-graphalgorithmregistry`
- `algorithm.SubroutineLibrary` -> `packages/algorithm.md#cls-algorithm-subroutinelibrary`

## Recommended source-to-doc anchor convention

If you later add hyperlinks from source code into this documentation set, the most stable pattern is:

- package anchor: `#pkg-graph`, `#pkg-algorithm`, ...
- class anchor: `#cls-ui-grapheditorcontroller`, `#cls-algorithm-algorithmrequest`, ...
- method anchor: `#method-ui-grapheditorcontroller-copyselectiontoclipboard`, ...

This page is intentionally small: it exists so a reader can jump from a source file name to the correct documentation file quickly.




