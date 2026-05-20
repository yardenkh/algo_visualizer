package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Complete timeline returned by an algorithm run.
 *
 * Immutable container used by {@link AlgorithmPlaybackController}.
 * It combines pseudocode lines and generated {@link AlgorithmStep} snapshots.
 *
 * Roadmap: see {@code PROJECT_NAVIGATION.md} and {@code project roadmap.md}.
 */
public final class AlgorithmResult {
    private final String algorithmName;
    private final List<String> pseudocodeLines;
    private final List<AlgorithmStep> steps;
    private final boolean infiniteLoopLikely;
    private final String executionNotice;
    private final java.util.Map<String, SubroutineTimeline> subroutineTimelines;
    private final java.util.Map<Integer, String> callableSubroutineByLine;

    /**
     * Creates a standard result without execution warnings.
     */
    public AlgorithmResult(String algorithmName, List<String> pseudocodeLines, List<AlgorithmStep> steps) {
        this(algorithmName, pseudocodeLines, steps, false, "", java.util.Map.of(), java.util.Map.of());
    }

    /**
     * Creates a fully-specified result.
     *
     * @param algorithmName human-readable algorithm label shown in UI
     * @param pseudocodeLines ordered pseudocode lines for highlighting
     * @param steps immutable timeline snapshots
     * @param infiniteLoopLikely true when execution semantics indicate non-termination risk
     * @param executionNotice warning text shown near pseudocode panel
     */
    public AlgorithmResult(
            String algorithmName,
            List<String> pseudocodeLines,
            List<AlgorithmStep> steps,
            boolean infiniteLoopLikely,
            String executionNotice
    ) {
        this(algorithmName, pseudocodeLines, steps, infiniteLoopLikely, executionNotice, java.util.Map.of(), java.util.Map.of());
    }

    /**
     * Creates a fully-specified result including optional subroutine metadata.
     */
    public AlgorithmResult(
            String algorithmName,
            List<String> pseudocodeLines,
            List<AlgorithmStep> steps,
            boolean infiniteLoopLikely,
            String executionNotice,
            java.util.Map<String, SubroutineTimeline> subroutineTimelines,
            java.util.Map<Integer, String> callableSubroutineByLine
    ) {
        this.algorithmName = algorithmName;
        this.pseudocodeLines = Collections.unmodifiableList(new ArrayList<>(pseudocodeLines));
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.infiniteLoopLikely = infiniteLoopLikely;
        this.executionNotice = executionNotice == null ? "" : executionNotice;
        this.subroutineTimelines = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(
                subroutineTimelines == null ? java.util.Map.of() : subroutineTimelines
        ));
        this.callableSubroutineByLine = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(
                callableSubroutineByLine == null ? java.util.Map.of() : callableSubroutineByLine
        ));
    }

    /** @return displayed algorithm name. */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /** @return immutable pseudocode lines. */
    public List<String> getPseudocodeLines() {
        return pseudocodeLines;
    }

    /** @return immutable timeline steps. */
    public List<AlgorithmStep> getSteps() {
        return steps;
    }

    /** @return true when algorithm reported potential infinite-loop behavior. */
    public boolean isInfiniteLoopLikely() {
        return infiniteLoopLikely;
    }

    /** @return execution notice text for UI warning panel. */
    public String getExecutionNotice() {
        return executionNotice;
    }

    /** @return immutable subroutine timelines keyed by canonical subroutine id. */
    public java.util.Map<String, SubroutineTimeline> getSubroutineTimelines() {
        return subroutineTimelines;
    }

    /** @return optional map from main pseudocode line index to callable subroutine id. */
    public java.util.Map<Integer, String> getCallableSubroutineByLine() {
        return callableSubroutineByLine;
    }

    /**
     * Builds a copy with a different step list while preserving all metadata.
     *
     * This is used by playback overlays that temporarily expand the timeline.
     */
    public AlgorithmResult withSteps(List<AlgorithmStep> newSteps) {
        return new AlgorithmResult(
                algorithmName,
                pseudocodeLines,
                newSteps,
                infiniteLoopLikely,
                executionNotice,
                subroutineTimelines,
                callableSubroutineByLine
        );
    }
}
