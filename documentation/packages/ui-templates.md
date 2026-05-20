# `ui.templates` Package

<a id="pkg-ui-templates"></a>

This package contains the premade graph template model, catalog, and preview rendering logic.

It provides the built-in graph library shown by the premade graphs dialog.

## Package responsibilities

- define immutable graph templates,
- build the built-in catalog of templates,
- classify templates into human-friendly groups,
- and provide tiny preview renderers for the picker UI.

## How this package connects to the rest of the system

- consumed by `ui.dialogs.PremadeGraphsDialog`,
- materialized into the live graph model by `ui.GraphEditorController.loadGraphTemplate(...)`,
- and used for preview thumbnails in the template picker.

## `GraphTemplate`

<a id="cls-ui-templates-graphtemplate"></a>

Immutable template definition.

**Contains**

- template name,
- template type,
- normalized vertex positions,
- and a list of lightweight template edges.

### `GraphTemplate.TemplateEdge`

<a id="cls-ui-templates-graphtemplate-templateedge"></a>

A lightweight immutable edge record used inside templates.

It stores source id, destination id, weight, and color.

## `GraphTemplateLibrary`

<a id="cls-ui-templates-graphtemplatelibrary"></a>

The built-in catalog factory.

**Responsibilities**

- construct the static list of templates,
- provide `allTemplates()` access,
- and provide type-filtered access through `byType(...)`.

**Design note**

The library is deterministic and static so the premade graph picker always presents the same examples.

## `GraphTemplatePreviewPanel`

<a id="cls-ui-templates-graphtemplatepreviewpanel"></a>

A tiny thumbnail renderer for one graph template.

It draws a simplified miniature of the template so users can visually scan the catalog.

## `GraphTemplateType`

<a id="cls-ui-templates-graphtemplatetype"></a>

Classification enum used by the template picker.

Current values:

- `ALL_GRAPHS`
- `BASICS`
- `TREES`
- `DAGS`
- `WEIGHTED`
- `GRIDS`
- `BIPARTITE`
- `FLOW`
- `SPECIAL`

## Package-level design choices

- Templates are stored in normalized coordinates so previews and canvas loading can share the same data.
- The preview panel is lightweight enough to appear many times in a scrolling picker.
- The library uses immutable collections to keep the catalog stable.

