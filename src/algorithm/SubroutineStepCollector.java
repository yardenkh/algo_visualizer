package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds per-run {@link SubroutineTimeline} instances by collecting steps that
 * were tagged with a callable subroutine ID during algorithm execution.
 *
 * Usage pattern:
 *   1. Create a collector before running the algorithm.
 *   2. Pass it to snapshot helpers so steps can register themselves.
 *   3. Call {@link #build(String, String, List)} at the end of the run to
 *      produce the SubroutineTimeline to embed in {@link AlgorithmResult}.
 *
 * This class is not thread-safe; it is only used on the algorithm execution path.
 */
public final class SubroutineStepCollector {

    /** Collected steps, in encounter order. */
    private final List<AlgorithmStep> steps = new ArrayList<>();

    /** Adds a step to the collection. */
    public void add(AlgorithmStep step) {
        if (step != null) {
            steps.add(step);
        }
    }

    /** @return true if at least one step has been collected. */
    public boolean hasSteps() {
        return !steps.isEmpty();
    }

    /**
     * Builds a {@link SubroutineTimeline} from the collected steps.
     *
     * @param id          canonical id (e.g. {@link SubroutineLibrary#RELAX})
     * @param displayName label shown in the pseudocode tab title
     * @param pseudocodeLines canonical CLRS pseudocode lines for the subroutine
     * @return immutable SubroutineTimeline, or {@code null} when no steps were collected
     */
    public SubroutineTimeline build(String id, String displayName, List<String> pseudocodeLines) {
        if (steps.isEmpty()) {
            return null;
        }
        return new SubroutineTimeline(id, displayName, pseudocodeLines, new ArrayList<>(steps));
    }

    /**
     * Convenience builder: wraps a single SubroutineTimeline into the maps expected
     * by {@link AlgorithmResult}.
     *
     * @param timeline result from {@link #build}, may be null
     * @param callablePseudoLineIndices zero-based line indices in the main pseudocode that
     *                                  should display the "open sub-routine" affordance
     * @return pair of (subroutineTimelines, callableSubroutineByLine) maps ready for AlgorithmResult
     */
    public static AlgorithmResultSubroutineData wrap(
            SubroutineTimeline timeline,
            List<Integer> callablePseudoLineIndices
    ) {
        if (timeline == null) {
            return new AlgorithmResultSubroutineData(Map.of(), Map.of());
        }
        Map<String, SubroutineTimeline> timelines = Map.of(timeline.getId(), timeline);
        Map<Integer, String> byLine = new LinkedHashMap<>();
        for (int index : callablePseudoLineIndices) {
            byLine.put(index, timeline.getId());
        }
        return new AlgorithmResultSubroutineData(timelines, Collections.unmodifiableMap(byLine));
    }

    /**
     * Convenience holder returned by {@link #wrap}.
     */
    public static final class AlgorithmResultSubroutineData {
        public final Map<String, SubroutineTimeline> timelines;
        public final Map<Integer, String> callableByLine;

        private AlgorithmResultSubroutineData(
                Map<String, SubroutineTimeline> timelines,
                Map<Integer, String> callableByLine
        ) {
            this.timelines = timelines;
            this.callableByLine = callableByLine;
        }
    }
}

