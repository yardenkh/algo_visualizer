/**
 * Core graph domain model.
 *
 * <p>Deep reference: see {@code documentation/packages/graph.md} for full model invariants,
 * extension notes, and interactions with the UI and algorithm layers.
 *
 * <p>This package owns the mutable in-memory representation of vertices, directed edges, and
 * adjacency relations. It is intentionally lightweight and independent from Swing UI concerns.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link graph.DirectedGraph} - graph container, vertex index, and structural operations</li>
 *   <li>{@link graph.Vertex} - vertex identity, adjacency lists, and reusable algorithm metadata</li>
 *   <li>{@link graph.Edge} - directed endpoint pair plus weight/color attributes</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Graph mutation and query operations ({@code add/remove/get/contains})</li>
 *   <li>Vertex-level connection helpers ({@code addNeighbor}, {@code addOutgoingEdge}, etc.)</li>
 *   <li>Traversal-state reset helpers used by editor and algorithms</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Consumed by {@code ui} for editing and selection workflows</li>
 *   <li>Consumed by {@code algorithm} through {@link algorithm.AlgorithmRequest}</li>
 *   <li>Template loading in {@code ui.templates} ultimately materializes into these objects</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Vertex ids are the canonical identity key across all packages</li>
 *   <li>Graph enforces at most one stored edge per ordered endpoint pair</li>
 *   <li>Model mutability is intentional; playback snapshots provide immutable visualization state</li>
 * </ul>
 */
package graph;

