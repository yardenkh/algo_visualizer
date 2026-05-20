package algorithm.apsp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Floyd-Warshall APSP algorithm.
 *
 * This class demonstrates dynamic programming over an all-pairs distance matrix.
 * Each timeline step highlights the currently active (k, i, j) context and updates
 * the matrix popup through {@link algorithm.MatrixSnapshot}.
 *
 * Related files:
 * - {@link ui.dialogs.AlgorithmMatrixDialog}
 * - {@link algorithm.AlgorithmStep}
 */
public final class FloydWarshallAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure FLOYD-WARSHALL(W)",
            "  D <- W",
            "  for k <- 1 to n:",
            "    for i <- 1 to n:",
            "      for j <- 1 to n:",
            "        D[i, j] <- min(D[i, j], D[i, k] + D[k, j])",
            "end procedure"
    );

    private static final int INF = 1_000_000_000;

    @Override public String getDisplayName() { return "Floyd-warshall"; }
    @Override public String getDescription() { return "Dynamic-programming all-pairs shortest paths."; }
    @Override public AlgorithmType getType() { return AlgorithmType.APSP; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }
    @Override public String validate(AlgorithmRequest request) { return request.isEmpty() ? "Select vertices before running Floyd-warshall." : null; }

    /**
     * Runs Floyd-Warshall and emits a fine-grained timeline for every major DP action.
     */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        List<Vertex> vertices = request.getScopeVertices();
        int n = vertices.size();

        Map<Integer, Integer> indexById = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            indexById.put(vertices.get(i).getId(), i);
        }

        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = (i == j) ? 0 : INF;
            }
        }
        for (Edge edge : request.getScopeEdges()) {
            int i = indexById.get(edge.getSource().getId());
            int j = indexById.get(edge.getDestination().getId());
            d[i][j] = Math.min(d[i][j], edge.getWeight());
        }

        steps.add(step(request, d, vertices, -1, -1, -1, 1,
                "Initialize matrix D",
                "D starts as direct-edge weights (and 0 on the diagonal)."));

        for (int k = 0; k < n; k++) {
            steps.add(step(request, d, vertices, k, -1, -1, 2,
                    "Choose intermediate k=" + vertices.get(k).getName(),
                    "Allow paths through " + vertices.get(k).getName() + "."));
            for (int i = 0; i < n; i++) {
                steps.add(step(request, d, vertices, k, i, -1, 3,
                        "Row i=" + vertices.get(i).getName(),
                        "Scan all destinations j for source " + vertices.get(i).getName() + "."));
                for (int j = 0; j < n; j++) {
                    int direct = d[i][j];
                    int via = (d[i][k] >= INF || d[k][j] >= INF) ? INF : d[i][k] + d[k][j];
                    steps.add(step(request, d, vertices, k, i, j, 4,
                            "Inspect triple (i,k,j)",
                            "Compare D[i,j]=" + fmt(direct) + " with D[i,k]+D[k,j]=" + fmt(via) + "."));

                    if (via < direct) {
                        d[i][j] = via;
                        steps.add(step(request, d, vertices, k, i, j, 5,
                                "Update D[i,j]",
                                "Updated distance " + vertices.get(i).getName() + " -> " + vertices.get(j).getName() + " to " + fmt(via) + "."));
                    } else {
                        steps.add(step(request, d, vertices, k, i, j, 5,
                                "No update",
                                "Current value already optimal for this k."));
                    }
                }
            }
        }

        steps.add(step(request, d, vertices, -1, -1, -1, 6,
                "Complete",
                "Floyd-warshall completed for all pairs."));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one timeline step, including matrix snapshot and highlighted role vertices. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            int[][] d,
            List<Vertex> vertices,
            int k,
            int i,
            int j,
            int line,
            String title,
            String message
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> labels = new LinkedHashMap<>();

        for (int idx = 0; idx < vertices.size(); idx++) {
            Vertex vertex = vertices.get(idx);
            int id = vertex.getId();
            vertexColors.put(id, AlgorithmPalette.SCOPE_NEUTRAL);
            labels.put(id, "self=" + fmt(d[idx][idx]));
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

        MatrixSnapshot snapshot = toSnapshot("D matrix", d, vertices, i, j);
        return new AlgorithmStep(title, message, line, vertexColors, edgeColors, labels, snapshot);
    }

    /** Converts the working distance matrix into a UI-friendly snapshot object. */
    private static MatrixSnapshot toSnapshot(String title, int[][] matrix, List<Vertex> vertices, int hiRow, int hiCol) {
        List<String> labels = new ArrayList<>();
        for (Vertex v : vertices) {
            labels.add(v.getName());
        }

        List<List<String>> cells = new ArrayList<>();
        for (int r = 0; r < matrix.length; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < matrix.length; c++) {
                row.add(fmt(matrix[r][c]));
            }
            cells.add(row);
        }

        return new MatrixSnapshot(title, labels, labels, cells, hiRow, hiCol);
    }

    /** Formats finite values and sentinel infinity for display in the matrix window. */
    private static String fmt(int v) {
        return v >= INF ? "INF" : String.valueOf(v);
    }
}
