/**
 * Minimum spanning tree (MST) demonstrations.
 *
 * <p>Algorithms in this package interpret selected edges as undirected weighted links and build a
 * spanning tree/forest view through timeline snapshots.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link algorithm.mst.PrimAlgorithm} grows a tree from a chosen start vertex</li>
 *   <li>{@link algorithm.mst.KruskalAlgorithm} accepts/rejects sorted edges via disjoint-set logic</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Concrete {@link algorithm.GraphAlgorithm} implementations discovered by the registry</li>
 *   <li>Orientation requirement is declared as UNDIRECTED-only for UI compatibility checks</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Reads scoped vertices/edges from {@link algorithm.AlgorithmRequest}</li>
 *   <li>Uses shared colors and helpers from {@code algorithm}</li>
 *   <li>Rendered by common playback UI without MST-specific dialogs</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Shows decision points explicitly (accept, reject, key update) for teaching</li>
 *   <li>Keeps parent/key/component labels on vertices for easy step inspection</li>
 * </ul>
 */
package algorithm.mst;

