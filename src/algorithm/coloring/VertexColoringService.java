package algorithm.coloring;

import graph.Edge;
import graph.Vertex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes an exact minimum proper vertex coloring for a selected subgraph.
 */
public final class VertexColoringService {

    public Result compute(List<Vertex> vertices, List<Edge> edges) {
        if (vertices == null || vertices.isEmpty()) {
            return new Result(Map.of(), 0);
        }

        Map<Integer, Integer> indexByVertexId = new LinkedHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            indexByVertexId.put(vertices.get(i).getId(), i);
        }

        boolean[][] adjacency = new boolean[vertices.size()][vertices.size()];
        for (Edge edge : edges) {
            Integer sourceIndex = indexByVertexId.get(edge.getSource().getId());
            Integer destinationIndex = indexByVertexId.get(edge.getDestination().getId());
            if (sourceIndex == null || destinationIndex == null) {
                continue;
            }
            if (sourceIndex.equals(destinationIndex)) {
                throw new IllegalArgumentException("Vertex coloring is undefined for self-loops.");
            }
            adjacency[sourceIndex][destinationIndex] = true;
            adjacency[destinationIndex][sourceIndex] = true;
        }

        ExactColoringSolver.ColoringSolution solution = new ExactColoringSolver(adjacency).solve();
        int[] colors = solution.getColors();
        Map<Integer, Integer> colorByVertexId = new LinkedHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            colorByVertexId.put(vertices.get(i).getId(), colors[i]);
        }
        return new Result(colorByVertexId, solution.getColorCount());
    }

    /** Exact vertex-coloring assignment by vertex id. */
    public static final class Result {
        private final Map<Integer, Integer> colorByVertexId;
        private final int colorCount;

        private Result(Map<Integer, Integer> colorByVertexId, int colorCount) {
            this.colorByVertexId = Map.copyOf(colorByVertexId);
            this.colorCount = colorCount;
        }

        public Map<Integer, Integer> getColorByVertexId() {
            return colorByVertexId;
        }

        public int getColorCount() {
            return colorCount;
        }
    }
}

