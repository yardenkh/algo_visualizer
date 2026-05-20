/**
 * Maximum-flow algorithm implementations and shared support.
 *
 * <p>This package implements augmenting-path and blocking-flow algorithms for directed
 * capacitated networks. Timelines include residual-graph and level-graph payloads so users can
 * inspect intermediate flow states visually.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link algorithm.maxflow.FordFulkersonAlgorithm} (DFS augmenting paths)</li>
 *   <li>{@link algorithm.maxflow.EdmondsKarpAlgorithm} (BFS shortest augmenting paths)</li>
 *   <li>{@link algorithm.maxflow.DinicAlgorithm} (BFS level graph + DFS blocking flow)</li>
 *   <li>{@link algorithm.maxflow.MaxFlowSupport} shared residual/level snapshot utilities</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Concrete {@link algorithm.GraphAlgorithm} implementations registered in
 *       {@link algorithm.GraphAlgorithmRegistry}</li>
 *   <li>Step payloads include {@link algorithm.ResidualGraphSnapshot} and
 *       {@link algorithm.LevelGraphSnapshot}</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Consumes source/sink-aware {@link algorithm.AlgorithmRequest}</li>
 *   <li>Uses subroutine metadata from {@link algorithm.SubroutineLibrary}</li>
 *   <li>Rendered by generic playback UI plus residual/level dialogs in {@code ui.dialogs}</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>All algorithms assume non-negative capacities and directed orientation</li>
 *   <li>Flow state is represented as matrices and converted to immutable snapshots per step</li>
 * </ul>
 */
package algorithm.maxflow;

