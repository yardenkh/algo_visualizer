/**
 * Core algorithm framework and playback model.
 *
 * <p>This package defines the execution contract used by every algorithm and every
 * visualization surface in the application. Implementations in subpackages are expected to be
 * deterministic timeline producers: they receive an immutable {@link algorithm.AlgorithmRequest}
 * and return an immutable {@link algorithm.AlgorithmResult} composed of ordered
 * {@link algorithm.AlgorithmStep} snapshots.
 *
 * <p><b>Primary responsibilities</b>
 * <ul>
 *   <li>Define algorithm interfaces and capability metadata ({@link algorithm.GraphAlgorithm},
 *       {@link algorithm.AlgorithmType}, {@link algorithm.GraphOrientationRequirement})</li>
 *   <li>Provide immutable run input/output containers ({@link algorithm.AlgorithmRequest},
 *       {@link algorithm.AlgorithmResult}, {@link algorithm.AlgorithmStep})</li>
 *   <li>Provide playback/session state management ({@link algorithm.AlgorithmPlaybackController})</li>
 *   <li>Provide common visualization payload types ({@link algorithm.MatrixSnapshot},
 *       {@link algorithm.ResidualGraphSnapshot}, {@link algorithm.LevelGraphSnapshot})</li>
 *   <li>Provide reusable algorithm helpers and style conventions ({@link algorithm.AlgorithmSupport},
 *       {@link algorithm.AlgorithmPalette})</li>
 * </ul>
 *
 * <p><b>Public APIs in this package</b>
 * <ul>
 *   <li>{@link algorithm.GraphAlgorithmRegistry} is the UI-facing algorithm catalog</li>
 *   <li>{@link algorithm.SubroutineLibrary}, {@link algorithm.SubroutineTimeline}, and
 *       {@link algorithm.SubroutineStepCollector} support expandable CLRS-style subroutines</li>
 *   <li>{@link algorithm.EdgeKey} is the stable edge identifier used in step overlays</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Consumes model data from {@code graph} via {@link algorithm.AlgorithmRequest}</li>
 *   <li>Is orchestrated by {@code ui} (especially {@code GraphEditorFrame})</li>
 *   <li>Delegates concrete implementations to subpackages: {@code sssp}, {@code apsp},
 *       {@code mst}, {@code maxflow}, {@code general}, {@code recursion}, and {@code coloring}</li>
 * </ul>
 *
 * <p><b>Design choices</b>
 * <ul>
 *   <li>Immutable snapshots over mutable live algorithm state for stable rewind/step behavior</li>
 *   <li>Presentation-oriented payloads embedded in steps to keep UI synchronization simple</li>
 *   <li>Subroutine overlays modeled as timeline expansion rather than nested execution contexts</li>
 * </ul>
 */
package algorithm;

