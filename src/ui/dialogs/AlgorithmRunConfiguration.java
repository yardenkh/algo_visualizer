package ui.dialogs;

import algorithm.GraphAlgorithm;

/**
 * Selected algorithm and source vertex for a new playback session.
 * Returned by {@link ui.dialogs.AlgorithmRunDialog} when the user confirms the run.
 */
public final class AlgorithmRunConfiguration {
    private final GraphAlgorithm algorithm;
    private final Integer sourceVertexId;
    private final Integer sinkVertexId;

    /** Creates one immutable run configuration. */
    public AlgorithmRunConfiguration(GraphAlgorithm algorithm, Integer sourceVertexId, Integer sinkVertexId) {
        this.algorithm = algorithm;
        this.sourceVertexId = sourceVertexId;
        this.sinkVertexId = sinkVertexId;
    }

    /** @return chosen algorithm instance. */
    public GraphAlgorithm getAlgorithm() {
        return algorithm;
    }

    /** @return chosen source vertex id, or null if algorithm does not require one. */
    public Integer getSourceVertexId() {
        return sourceVertexId;
    }

    /** @return chosen sink vertex id, or null if algorithm does not require one. */
    public Integer getSinkVertexId() {
        return sinkVertexId;
    }
}
