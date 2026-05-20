package algorithm.coloring;

import graph.Edge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes an exact minimum proper edge coloring for a selected subgraph.
 *
 * Implemented as exact vertex coloring on the edge-conflict graph.
 */
public final class EdgeColoringService {

    public Result compute(List<Edge> edges) {
        if (edges == null || edges.isEmpty()) {
            return new Result(Map.of(), 0);
        }

        boolean[][] conflict = new boolean[edges.size()][edges.size()];
        for (int i = 0; i < edges.size(); i++) {
            Edge first = edges.get(i);
            if (first.getSource().getId() == first.getDestination().getId()) {
                throw new IllegalArgumentException("Edge coloring is undefined for self-loop edges.");
            }
            for (int j = i + 1; j < edges.size(); j++) {
                Edge second = edges.get(j);
                if (shareEndpoint(first, second)) {
                    conflict[i][j] = true;
                    conflict[j][i] = true;
                }
            }
        }

        ExactColoringSolver.ColoringSolution solution = new ExactColoringSolver(conflict).solve();
        int[] colors = solution.getColors();
        Map<Edge, Integer> colorByEdge = new LinkedHashMap<>();
        for (int i = 0; i < edges.size(); i++) {
            colorByEdge.put(edges.get(i), colors[i]);
        }
        return new Result(colorByEdge, solution.getColorCount());
    }

    private boolean shareEndpoint(Edge first, Edge second) {
        int firstSource = first.getSource().getId();
        int firstDestination = first.getDestination().getId();
        int secondSource = second.getSource().getId();
        int secondDestination = second.getDestination().getId();

        return firstSource == secondSource
                || firstSource == secondDestination
                || firstDestination == secondSource
                || firstDestination == secondDestination;
    }

    /** Exact edge-coloring assignment by edge object identity. */
    public static final class Result {
        private final Map<Edge, Integer> colorByEdge;
        private final int colorCount;

        private Result(Map<Edge, Integer> colorByEdge, int colorCount) {
            this.colorByEdge = Map.copyOf(colorByEdge);
            this.colorCount = colorCount;
        }

        public Map<Edge, Integer> getColorByEdge() {
            return colorByEdge;
        }

        public int getColorCount() {
            return colorCount;
        }
    }
}

