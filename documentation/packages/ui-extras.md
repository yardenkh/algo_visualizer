# `ui.extras` Package

<a id="pkg-ui-extras"></a>

This package contains the action handlers and support services for the Extras menu.

These are selection-scoped utilities: they operate on the currently selected subgraph and generally do not involve algorithm playback.

## Package responsibilities

- compute or transform graph properties for the selected scope,
- keep the action handlers thin and user-focused,
- and provide reusable support services for graph transformations.

## How this package connects to the rest of the system

- invoked from `ui.dialogs.SelectionExtrasDialog`,
- uses `ui.GraphEditorController` for selection, mutation, and visual updates,
- and, for coloring, relies on `algorithm.coloring` services.

## `VertexColoringActionHandler`

<a id="cls-ui-extras-vertexcoloringactionhandler"></a>

Validates the current selection, computes an exact vertex coloring, maps the color classes to RGB values, and applies those colors through the controller.

## `EdgeColoringActionHandler`

<a id="cls-ui-extras-edgecoloringactionhandler"></a>

Validates the current selection, computes an exact edge coloring, maps the color classes to RGB values, and applies those colors through the controller.

## `ComplementGraphActionHandler`

<a id="cls-ui-extras-complementgraphactionhandler"></a>

Replaces the selected induced subgraph with its complement.

## `LineGraphActionHandler`

<a id="cls-ui-extras-linegraphactionhandler"></a>

Transforms the selected graph into a line graph when possible.

## `FindRootGraphActionHandler`

<a id="cls-ui-extras-findrootgraphactionhandler"></a>

Attempts to reconstruct a root graph from a selected line graph.

## `ReduceEdgeCrossingsActionHandler`

<a id="cls-ui-extras-reduceedgecrossingsactionhandler"></a>

Runs the layout improvement service and applies new vertex positions when it finds a better embedding.

## Support services

### `EdgeCrossingReductionService`

<a id="cls-ui-extras-edgecrossingreductionservice"></a>

Heuristic layout improver that tries to reduce straight-line edge crossings.

It does not change the graph structure; it only improves positions.

### `LineGraphRootService`

<a id="cls-ui-extras-linegraphrootservice"></a>

Recognizes line graphs and attempts to reconstruct a valid root graph.

### `ColorClassPalette`

<a id="cls-ui-extras-colorclasspalette"></a>

Maps abstract color-class indices to stable UI-friendly RGB values.

## Package-level design choices

- Each handler validates early and reports understandable messages to the user.
- Computational services live outside the handler classes so they can be reused and tested.
- The Extras dialog stays simple by delegating real work to these handlers and services.

