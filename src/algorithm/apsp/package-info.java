/**
 * All-pairs shortest path (APSP) algorithms.
 *
 * <p>This package contains educational implementations that compute shortest-path distances for
 * all ordered vertex pairs in the selected scope. Each algorithm implements
 * {@link algorithm.GraphAlgorithm} and emits timeline snapshots for matrix and graph overlays.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link algorithm.apsp.FloydWarshallAlgorithm} - dynamic programming over D matrix</li>
 *   <li>{@link algorithm.apsp.MatrixMultiplicationApspAlgorithm} - min-plus matrix extension</li>
 *   <li>{@link algorithm.apsp.RecursiveDoublingApspAlgorithm} - repeated squaring style APSP</li>
 *   <li>{@link algorithm.apsp.JohnsonAlgorithm} - Bellman-Ford potentials + per-source Dijkstra</li>
 *   <li>{@link algorithm.apsp.RepeatedDijkstraAlgorithm} - repeated SSSP demonstration</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Concrete algorithm classes are instantiated by {@link algorithm.GraphAlgorithmRegistry}</li>
 *   <li>APSP steps frequently attach {@link algorithm.MatrixSnapshot} payloads consumed by UI</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Reads scoped graph data from {@link algorithm.AlgorithmRequest}</li>
 *   <li>Shares helpers with core framework via {@link algorithm.AlgorithmSupport}</li>
 *   <li>Renders in UI through algorithm playback and {@code ui.dialogs.AlgorithmMatrixDialog}</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Names and pseudocode intentionally mirror textbook terminology for teaching</li>
 *   <li>Implementations prioritize traceability and explanation over asymptotic micro-optimizations</li>
 * </ul>
 */
package algorithm.apsp;

