package algorithm.general;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Warshall-style transitive closure.
 *
 * This algorithm visualizes reachability as a boolean matrix and highlights
 * the currently active k/i/j indices.
 */
public final class TransitiveClosureAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure TRANSITIVE-CLOSURE(G)",
            "  T <- adjacency matrix of G",
            "  for k <- 1 to n:",
            "    for i <- 1 to n:",
            "      for j <- 1 to n: T[i,j] <- T[i,j] OR (T[i,k] AND T[k,j])",
            "end procedure"
    );

    @Override public String getDisplayName() { return "transitive closure"; }
    @Override public String getDescription() { return "Computes reachability between every ordered pair of vertices."; }
    @Override public AlgorithmType getType() { return AlgorithmType.GENERAL; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }
    @Override public String validate(AlgorithmRequest request) { return request.isEmpty() ? "Select vertices before running transitive closure." : null; }

    /** Runs transitive closure and emits matrix-focused reachability steps. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<Vertex> vertices = request.getScopeVertices();
        int n = vertices.size();
        List<AlgorithmStep> steps = new ArrayList<>();

        Map<Integer, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(vertices.get(i).getId(), i);
        }

        boolean[][] t = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            t[i][i] = true;
        }
        for (Edge edge : request.getScopeEdges()) {
            t[idx.get(edge.getSource().getId())][idx.get(edge.getDestination().getId())] = true;
        }

        steps.add(step(request, vertices, t, 1, -1, -1, -1,
                "Initialize T",
                "Reachability starts from adjacency matrix (and self reachability)."));

        for (int k = 0; k < n; k++) {
            steps.add(step(request, vertices, t, 2, k, -1, -1,
                    "Intermediate k=" + vertices.get(k).getName(),
                    "Allow reachability via " + vertices.get(k).getName() + "."));
            for (int i = 0; i < n; i++) {
                steps.add(step(request, vertices, t, 3, k, i, -1,
                        "Row i=" + vertices.get(i).getName(),
                        "Evaluate all destinations j for source " + vertices.get(i).getName() + "."));
                for (int j = 0; j < n; j++) {
                    boolean old = t[i][j];
                    boolean via = t[i][k] && t[k][j];
                    steps.add(step(request, vertices, t, 4, k, i, j,
                            "Inspect triple",
                            "T[i,j]=" + old + ", via k=" + via + "."));
                    t[i][j] = old || via;
                    if (t[i][j] != old) {
                        steps.add(step(request, vertices, t, 4, k, i, j,
                                "Update reachability",
                                "Now " + vertices.get(i).getName() + " reaches " + vertices.get(j).getName() + "."));
                    }
                }
            }
        }

        steps.add(step(request, vertices, t, 5, -1, -1, -1,
                "Complete",
                "Transitive closure computed."));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one boolean-matrix visualization step for the current triple context. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            List<Vertex> vertices,
            boolean[][] matrix,
            int line,
            int k,
            int i,
            int j,
            String title,
            String message
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> labels = new LinkedHashMap<>();
        for (Vertex vertex : vertices) {
            vertexColors.put(vertex.getId(), AlgorithmPalette.SCOPE_NEUTRAL);
            labels.put(vertex.getId(), "");
        }
        if (k >= 0) {
            vertexColors.put(vertices.get(k).getId(), AlgorithmPalette.FRONTIER);
            labels.put(vertices.get(k).getId(), "k");
        }
        if (i >= 0) {
            vertexColors.put(vertices.get(i).getId(), AlgorithmPalette.ACTIVE);
            labels.put(vertices.get(i).getId(), "i");
        }
        if (j >= 0) {
            vertexColors.put(vertices.get(j).getId(), AlgorithmPalette.DISCOVERED);
            labels.put(vertices.get(j).getId(), "j");
        }
        MatrixSnapshot snapshot = toSnapshot("T matrix", matrix, vertices, i, j);
        return new AlgorithmStep(title, message, line, vertexColors, edgeColors, labels, snapshot);
    }

    /** Converts a boolean reachability matrix into the generic matrix snapshot payload. */
    private static MatrixSnapshot toSnapshot(String title, boolean[][] matrix, List<Vertex> vertices, int hiRow, int hiCol) {
        List<String> names = new ArrayList<>();
        for (Vertex v : vertices) {
            names.add(v.getName());
        }

        List<List<String>> cells = new ArrayList<>();
        for (int r = 0; r < matrix.length; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < matrix.length; c++) {
                row.add(matrix[r][c] ? "1" : "0");
            }
            cells.add(row);
        }
        return new MatrixSnapshot(title, names, names, cells, hiRow, hiCol);
    }
}
