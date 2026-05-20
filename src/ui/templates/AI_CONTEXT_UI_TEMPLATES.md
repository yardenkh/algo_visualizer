# Package Purpose

Premade graph template model, catalog, and preview components. Defines the static graph template library used by the premade-graphs workflow. Templates are pure metadata (normalized positions + edge lists) that can be rendered in a small preview or loaded into the editor model.

# Package Mental Model

- **Template as Data**: Each template is immutable metadata describing a premade graph: vertices, edges, weights, and normalized positions.
- **Normalized Coordinates**: Positions are in a canonical rectangle (e.g., [0, 1] × [0, 1]) so previews and full-canvas loads render identically.
- **Static Library**: All templates are defined statically in `GraphTemplateLibrary`. No templates are loaded from files or database.
- **Taxonomy**: Templates are categorized by type (Complete, Cycle, BipartiteComplete, Random, etc.) for filtering in UI.
- **Preview Rendering**: `GraphTemplatePreviewPanel` renders small thumbnails of templates in the picker dialog.
- **Material-Ready**: When user selects template, it's materialized into the live graph in editor.

# Package Boundaries

| Type | Items |
|---|---|
| Inputs | Template type filter from UI picker |
| Outputs | Template metadata (for preview or materialization), rendered thumbnail |
| Owns | Template definitions, template types/taxonomy, preview panel |
| Depends On | `graph` (Vertex, Edge classes used for template data) |
| Must NOT Depend On | `ui` except `ui.dialogs` for preview usage |

# Important Classes

| Class | Role | Depends On | Used By | Risk Level | Notes |
|---|---|---|---|---|---|
| `GraphTemplate` | Immutable template data model | `Vertex`, `Edge`, `Point2D` | Library, dialog | LOW | Holds vertex/edge lists and positions; never modified |
| `GraphTemplateType` | Enum of template categories | - | Library, picker | LOW | Stable taxonomy; e.g., COMPLETE_GRAPH, CYCLE, RANDOM |
| `GraphTemplateLibrary` | Static catalog factory | `GraphTemplate` | Picker dialog | MEDIUM | Defines all builtin templates; single source of truth |
| `GraphTemplatePreviewPanel` | Lightweight thumbnail renderer | `GraphTemplate` | Picker dialog | LOW | Renders 200x200px preview; uses simplified drawing |

# Important Interfaces / Contracts

| Interface | Purpose | Implementations | Stability |
|---|---|---|---|
| - | No public interfaces; concrete classes only | - | STABLE |

# Runtime Flows

## Template Library Access
1. Picker dialog calls `GraphTemplateLibrary.allTemplates()`.
2. Library returns immutable list of all templates (cached in static list).
3. Dialog displays templates in grid or list.
4. User can filter by type: `GraphTemplateLibrary.byType(GraphTemplateType.COMPLETE_GRAPH)`.

## Template Preview Rendering
1. Picker displays thumbnail for each template.
2. For each template, creates `GraphTemplatePreviewPanel(template)`.
3. Panel is given 200x200px space.
4. Panel renders template: draws vertices as small circles, edges as lines.
5. Preview uses normalized coordinates directly (no pan/zoom needed).

## Template Materialization
1. User selects template and clicks OK.
2. Picker returns selected `GraphTemplate`.
3. Frame calls `controller.loadGraphTemplate(template)`.
4. Controller iterates vertices/edges in template:
   - Creates new `Vertex` objects.
   - Denormalizes positions (scales from [0,1] to canvas space).
   - Creates `Edge` objects.
   - Adds to live graph.
5. UI renders new graph.

# Important State / Data Structures

| Structure | Meaning | Important Fields | Invariants |
|---|---|---|---|
| `GraphTemplate` | Template definition | `name`, `description`, `vertices`, `edges`, `positions` | Positions are normalized [0, 1] × [0, 1]; vertices/edges are immutable copies |
| Normalized position | Coordinate in [0, 1] × [0, 1] | x, y values | 0 <= x <= 1, 0 <= y <= 1 |

# Hidden Assumptions

- **Normalized Coordinates**: All templates use normalized coordinates in [0, 1] × [0, 1]. Renderer handles denormalization to canvas space.
- **Static Library**: All templates are hardcoded in library. No dynamic loading or persistence.
- **Vertex IDs Start at 0**: When materializing, vertex IDs are assigned sequentially starting at 0 (or from next available ID in graph).
- **Unique Template Names**: Each template has a unique name for identification and caching.

# Concurrency Notes

Package is effectively single-threaded. `GraphTemplateLibrary` uses static initialization; no synchronization needed. Preview panels are created per template; no shared mutable state.

# Performance Notes

- **Library Initialization**: Static list of templates is built once on class load. Typically <50 templates. Load time negligible.
- **Preview Rendering**: Rendering 200x200px preview takes ~5-10ms. Multiple previews in grid may take 50-100ms total.
- **Materialization**: Creating vertices/edges from template takes <10ms. Denormalization and positioning is fast.

# Modification Guide For AI Assistants

## Safest Changes

- **Add new template to library**: Define new `GraphTemplate` in `GraphTemplateLibrary.build()`, add to list. No UI changes needed.
- **Add new template type**: Add constant to `GraphTemplateType` enum. Library can categorize new templates under this type.
- **Enhance preview rendering**: Add colors, labels, or decorations to preview panel. Doesn't affect template data.

## Dangerous Changes

- **Do NOT modify template objects after creation**: Templates are immutable. Modifying them corrupts the library.
- **Do NOT change normalized coordinate range**: All templates must use [0, 1] × [0, 1]. Changing this breaks preview/materialization consistency.
- **Do NOT reuse vertex objects across templates**: Each template should have its own copies. Sharing vertices causes issues when multiple templates are loaded.

## Preferred Patterns

- **Keep templates immutable**: Once created, never modify. Use builder pattern if needed during construction.
- **Use meaningful template names**: Names should be descriptive (e.g., "Complete Graph K5", "Petersen Graph") for UI display.
- **Include template descriptions**: Descriptions help users choose appropriate template for their needs.
- **Normalize carefully**: Ensure all positions are in [0, 1] × [0, 1] exactly.

# Typical Change Recipes

### Add New Template to Library
1. In `GraphTemplateLibrary.buildTemplates()`, add:
   ```java
   List<GraphTemplate> templates = new ArrayList<>();
   // ... existing templates ...
   
   // New template
   List<Vertex> newVertices = new ArrayList<>();
   newVertices.add(new Vertex(0)); // ID, name auto-generated
   newVertices.add(new Vertex(1));
   newVertices.add(new Vertex(2));
   
   List<Edge> newEdges = new ArrayList<>();
   newEdges.add(new Edge(newVertices.get(0), newVertices.get(1), 1));
   newEdges.add(new Edge(newVertices.get(1), newVertices.get(2), 1));
   
   Map<Integer, Point2D.Double> positions = new LinkedHashMap<>();
   positions.put(0, new Point2D.Double(0.5, 0.2));
   positions.put(1, new Point2D.Double(0.2, 0.8));
   positions.put(2, new Point2D.Double(0.8, 0.8));
   
   templates.add(new GraphTemplate(
       "My Triangle",
       "A simple three-vertex triangle",
       GraphTemplateType.GENERAL,
       newVertices,
       newEdges,
       positions
   ));
   ```
2. Template now appears in picker and can be loaded.

### Add New Template Type
1. Add constant to `GraphTemplateType` enum:
   ```java
   public enum GraphTemplateType {
       // ... existing types ...
       MY_NEW_TYPE("My New Category")
       
       // ... rest of enum ...
   }
   ```
2. Use type when creating templates:
   ```java
   new GraphTemplate(..., GraphTemplateType.MY_NEW_TYPE, ...)
   ```
3. Picker can filter by new type.

### Enhance Preview Rendering
1. Modify `GraphTemplatePreviewPanel.paintComponent()`:
   ```java
   // Draw vertices with custom color
   for (Vertex v : template.getVertices()) {
       Point2D.Double pos = template.getPositions().get(v.getId());
       int screenX = (int)(pos.x * getWidth());
       int screenY = (int)(pos.y * getHeight());
       g2d.setColor(Color.BLUE);
       g2d.fillOval(screenX - 3, screenY - 3, 6, 6);
   }
   // ... rest of rendering ...
   ```

# Cross-Package Relationships

| External Package | Relationship | Notes |
|---|---|---|
| `graph` | **Uses** | Template definitions use Vertex/Edge classes |
| `ui.dialogs` | **Consumed By** | `PremadeGraphsDialog` displays templates |
| `ui` | **Consumed By** | `GraphEditorController.loadGraphTemplate()` materializes |

# AI Quick Reference

## Entry Points
- `GraphTemplateLibrary.allTemplates()` → `List<GraphTemplate>`
- `GraphTemplateLibrary.byType(GraphTemplateType)` → `List<GraphTemplate>`
- `GraphTemplatePreviewPanel(GraphTemplate)` → renderable preview

## Important Classes
- `GraphTemplate` — immutable template data
- `GraphTemplateType` — enum of categories
- `GraphTemplatePreviewPanel` — thumbnail renderer

## Key Invariants
- Templates use normalized coordinates [0, 1] × [0, 1]
- Vertex IDs in template start at 0
- All templates immutable after construction


