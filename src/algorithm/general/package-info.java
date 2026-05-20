/**
 * General-purpose algorithms that do not belong to a single specialized family.
 *
 * <p>This package hosts algorithms used for reachability, ordering, conceptual APSP variants,
 * and heuristic search demonstrations.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link algorithm.general.TopologicalOrderAlgorithm} (Kahn-style ordering)</li>
 *   <li>{@link algorithm.general.TransitiveClosureAlgorithm} (Warshall-style reachability matrix)</li>
 *   <li>{@link algorithm.general.AStarAlgorithm} (heuristic-guided shortest-path demonstration)</li>
 *   <li>{@link algorithm.general.SeidelAlgorithm} (recursive APSP demonstration for undirected graphs)</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Each class implements {@link algorithm.GraphAlgorithm} and is registry-discoverable</li>
 *   <li>Algorithms may emit matrix and stack payloads via {@link algorithm.AlgorithmStep}</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Uses DAG checks from {@link algorithm.sssp.DagValidation} where acyclicity is required</li>
 *   <li>Topological order output is consumed by {@code ui.dialogs.TopologicalOrderDialog}</li>
 *   <li>Seidel recursion frames are adapted via {@code algorithm.recursion}</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Educational clarity is prioritized over production-performance tuning</li>
 *   <li>Algorithms keep to the shared timeline contract for consistent playback behavior</li>
 * </ul>
 */
package algorithm.general;

