package algorithm.sssp;

import algorithm.*;

import java.util.List;

/**
 * Shared DAG validation message builder for DAG shortest path and topological order.
 * Centralizes pre-run checks for algorithms that require acyclic directed structure.
 */
public final class DagValidation {
    private DagValidation() {
    }

    /**
     * Builds the user-facing warning text for cycle and bidirectional-edge problems.
     *
     * @return warning text, or null if the selected scope looks DAG-compatible
     */
    public static String buildDagWarning(AlgorithmRequest request) {
        List<String> cycles = AlgorithmSupport.findCycles(request, 5);
        List<String> bidirectional = AlgorithmSupport.findBidirectionalEdgeWarnings(request);
        if (cycles.isEmpty() && bidirectional.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("the graph contain the cycles: ");
        if (cycles.isEmpty()) {
            sb.append("none detected");
        } else {
            sb.append(System.lineSeparator());
            for (String cycle : cycles) {
                sb.append(" - ").append(cycle).append(System.lineSeparator());
            }
        }

        sb.append(System.lineSeparator());
        sb.append("the edges ");
        if (bidirectional.isEmpty()) {
            sb.append("(none)");
        } else {
            sb.append(System.lineSeparator());
            for (String edge : bidirectional) {
                sb.append(" - ").append(edge).append(System.lineSeparator());
            }
        }
        sb.append(" are directed. please take care of this before running the algorithm");
        return sb.toString();
    }
}
