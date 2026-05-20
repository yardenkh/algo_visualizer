/**
 * Single-source shortest path and traversal algorithms.
 *
 * <p>This package combines traversal (BFS/DFS) and shortest-path routines that are driven from
 * a source vertex. Implementations follow the shared timeline model and expose CLRS-style
 * pseudocode lines for synchronized playback.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link algorithm.sssp.BreadthFirstSearchAlgorithm} and
 *       {@link algorithm.sssp.DepthFirstSearchAlgorithm}</li>
 *   <li>{@link algorithm.sssp.DijkstraShortestPathAlgorithm} (non-negative weights)</li>
 *   <li>{@link algorithm.sssp.BellmanFordAlgorithm} (negative edges + cycle detection)</li>
 *   <li>{@link algorithm.sssp.DagShortestPathAlgorithm} (topological relaxation)</li>
 *   <li>{@link algorithm.sssp.DagValidation} shared DAG-warning message builder</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Concrete {@link algorithm.GraphAlgorithm} implementations registered centrally</li>
 *   <li>Subroutine-enabled algorithms expose RELAX and DFS-VISIT overlays through
 *       {@link algorithm.SubroutineTimeline}</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Consumes scoped graph views via {@link algorithm.AlgorithmRequest}</li>
 *   <li>Uses helpers from {@link algorithm.AlgorithmSupport} and stack adapters in
 *       {@code algorithm.recursion}</li>
 *   <li>Rendered by common UI playback and pseudocode dialogs</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Validation intentionally guards algorithm assumptions before run</li>
 *   <li>Timelines highlight edge inspection and relaxation decisions step-by-step</li>
 * </ul>
 */
package algorithm.sssp;

