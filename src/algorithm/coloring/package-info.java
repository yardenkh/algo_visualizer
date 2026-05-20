/**
 * Exact coloring services for selected subgraphs.
 *
 * <p>This package provides reusable, UI-independent services for minimum proper coloring tasks.
 * It is used by Selection -> Extras actions and is intentionally separated from playback-oriented
 * algorithm timelines.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link algorithm.coloring.VertexColoringService} computes exact chromatic number/coloring</li>
 *   <li>{@link algorithm.coloring.EdgeColoringService} computes exact edge coloring via conflict graph</li>
 *   <li>{@link algorithm.coloring.ExactColoringSolver} implements branch-and-bound DSATUR-style search</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Service {@code compute(...)} methods return immutable result objects for UI mapping</li>
 *   <li>Input is plain {@code List<Vertex>} / {@code List<Edge>} so callers can provide any scope</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Consumes graph model classes from {@code graph}</li>
 *   <li>Called by handlers in {@code ui.extras}</li>
 *   <li>Color class indexes are translated to RGB by UI palette helpers</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Exact algorithms favored over heuristics for deterministic teaching outputs</li>
 *   <li>Rejects unsupported structures (for example self-loops) with explicit validation errors</li>
 * </ul>
 */
package algorithm.coloring;

