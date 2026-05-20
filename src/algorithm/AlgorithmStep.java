package algorithm;

import algorithm.recursion.StackFramesSnapshot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot of one step in an algorithm execution timeline.
 *
 * Each step captures:
 * - Pseudocode line being executed (for highlighting in the UI)
 * - Vertex colors and edge colors (for graph visualization)
 * - Vertex annotations/labels (e.g., "dist=5", "d=2 f=7")
 * - Optional matrix snapshot (for APSP algorithms)
 * - Optional stack frame snapshot (for recursive algorithms)
 *
 * Steps are created as an algorithm executes and then played back with
 * forward/backward stepping via AlgorithmPlaybackController.
 *
 * The timeline model ensures:
 * - All state at each step is immutable
 * - UI can render any step independently
 * - Stepping forward/backward is stateless and efficient
 *
 * @author Graph Editor
 */
public final class AlgorithmStep {
    private final String title;
    private final String message;
    private final int activePseudocodeLine;
    private final Map<Integer, Integer> vertexColors;
    private final Map<EdgeKey, Integer> edgeColors;
    private final Map<Integer, String> vertexAnnotations;
    private final Map<EdgeKey, String> edgeAnnotations;
    private final MatrixSnapshot matrixSnapshot;
    private final StackFramesSnapshot stackFramesSnapshot;
    private final ResidualGraphSnapshot residualGraphSnapshot;
    private final LevelGraphSnapshot levelGraphSnapshot;
    private final String callableSubroutineId;

    public AlgorithmStep(
            String title,
            String message,
            int activePseudocodeLine,
            Map<Integer, Integer> vertexColors,
            Map<EdgeKey, Integer> edgeColors,
            Map<Integer, String> vertexAnnotations
    ) {
        this(title, message, activePseudocodeLine, vertexColors, edgeColors, vertexAnnotations,
                Collections.emptyMap(), null, null, null, null, null);
    }

    public AlgorithmStep(
            String title,
            String message,
            int activePseudocodeLine,
            Map<Integer, Integer> vertexColors,
            Map<EdgeKey, Integer> edgeColors,
            Map<Integer, String> vertexAnnotations,
            MatrixSnapshot matrixSnapshot
    ) {
        this(title, message, activePseudocodeLine, vertexColors, edgeColors, vertexAnnotations,
                Collections.emptyMap(), matrixSnapshot, null, null, null, null);
    }

    public AlgorithmStep(
            String title,
            String message,
            int activePseudocodeLine,
            Map<Integer, Integer> vertexColors,
            Map<EdgeKey, Integer> edgeColors,
            Map<Integer, String> vertexAnnotations,
            MatrixSnapshot matrixSnapshot,
            StackFramesSnapshot stackFramesSnapshot
    ) {
        this(title, message, activePseudocodeLine, vertexColors, edgeColors, vertexAnnotations,
                Collections.emptyMap(), matrixSnapshot, stackFramesSnapshot, null, null, null);
    }

    /** Extended constructor for payload-rich steps (edge labels + matrix + stack + residual/level graphs). */
    public AlgorithmStep(
            String title,
            String message,
            int activePseudocodeLine,
            Map<Integer, Integer> vertexColors,
            Map<EdgeKey, Integer> edgeColors,
            Map<Integer, String> vertexAnnotations,
            Map<EdgeKey, String> edgeAnnotations,
            MatrixSnapshot matrixSnapshot,
            StackFramesSnapshot stackFramesSnapshot,
            ResidualGraphSnapshot residualGraphSnapshot,
            LevelGraphSnapshot levelGraphSnapshot
    ) {
        this(title, message, activePseudocodeLine, vertexColors, edgeColors, vertexAnnotations,
                edgeAnnotations, matrixSnapshot, stackFramesSnapshot, residualGraphSnapshot, levelGraphSnapshot, null);
    }

    /**
     * Extended constructor with optional callable subroutine metadata.
     *
     * If {@code callableSubroutineId} is non-blank, the UI can offer an "open sub-routine"
     * affordance for this step/line.
     */
    public AlgorithmStep(
            String title,
            String message,
            int activePseudocodeLine,
            Map<Integer, Integer> vertexColors,
            Map<EdgeKey, Integer> edgeColors,
            Map<Integer, String> vertexAnnotations,
            Map<EdgeKey, String> edgeAnnotations,
            MatrixSnapshot matrixSnapshot,
            StackFramesSnapshot stackFramesSnapshot,
            ResidualGraphSnapshot residualGraphSnapshot,
            LevelGraphSnapshot levelGraphSnapshot,
            String callableSubroutineId
    ) {
        this.title = title;
        this.message = message;
        this.activePseudocodeLine = activePseudocodeLine;
        this.vertexColors = Collections.unmodifiableMap(new LinkedHashMap<>(vertexColors));
        this.edgeColors = Collections.unmodifiableMap(new LinkedHashMap<>(edgeColors));
        this.vertexAnnotations = Collections.unmodifiableMap(new LinkedHashMap<>(vertexAnnotations));
        this.edgeAnnotations = Collections.unmodifiableMap(new LinkedHashMap<>(edgeAnnotations));
        this.matrixSnapshot = matrixSnapshot;
        this.stackFramesSnapshot = stackFramesSnapshot;
        this.residualGraphSnapshot = residualGraphSnapshot;
        this.levelGraphSnapshot = levelGraphSnapshot;
        this.callableSubroutineId = callableSubroutineId == null || callableSubroutineId.isBlank() ? null : callableSubroutineId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    /** Zero-based index of the currently active pseudocode line. */
    public int getActivePseudocodeLine() {
        return activePseudocodeLine;
    }

    public Map<Integer, Integer> getVertexColors() {
        return vertexColors;
    }

    public Map<EdgeKey, Integer> getEdgeColors() {
        return edgeColors;
    }

    public Map<Integer, String> getVertexAnnotations() {
        return vertexAnnotations;
    }

    public Map<EdgeKey, String> getEdgeAnnotations() {
        return edgeAnnotations;
    }

    public MatrixSnapshot getMatrixSnapshot() {
        return matrixSnapshot;
    }

    public StackFramesSnapshot getStackFramesSnapshot() {
        return stackFramesSnapshot;
    }

    public ResidualGraphSnapshot getResidualGraphSnapshot() {
        return residualGraphSnapshot;
    }

    public LevelGraphSnapshot getLevelGraphSnapshot() {
        return levelGraphSnapshot;
    }

    /** @return subroutine id that can be expanded from this step, or null when not available. */
    public String getCallableSubroutineId() {
        return callableSubroutineId;
    }
}
