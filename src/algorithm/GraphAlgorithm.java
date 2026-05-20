package algorithm;

import java.util.List;

/**
 * Base interface for all graph algorithms in this system.
 *
 * Every algorithm must:
 * 1. Declare its display name, description, and type (SSSP, APSP, MST, etc.)
 * 2. Implement validation to check if the graph meets algorithm requirements
 * 3. Produce an AlgorithmResult with step-by-step timeline for visualization
 *
 * The timeline paradigm allows real-time stepping (forward/backward) through
 * algorithm execution, with visualization payloads (matrix snapshots, stack frames, etc.)
 * attached to each step for rich algorithm understanding.
 *
 * @author Graph Editor
 */
public interface GraphAlgorithm {
    String getDisplayName();

    String getDescription();

    AlgorithmType getType();

    default boolean requiresSourceVertex() {
        return true;
    }

    /** Maximum-flow style algorithms can require a sink vertex in addition to source. */
    default boolean requiresSinkVertex() {
        return false;
    }

    /**
     * Declares whether this algorithm expects directed, undirected, or either graph orientation.
     */
    default GraphOrientationRequirement getOrientationRequirement() {
        return GraphOrientationRequirement.EITHER;
    }

    /** Ordered pseudocode lines displayed in the playback popup. */
    List<String> getPseudocodeLines();

    /** @return validation message if the request cannot run, otherwise {@code null}. */
    String validate(AlgorithmRequest request);

    AlgorithmResult run(AlgorithmRequest request);
}
