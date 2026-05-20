package algorithm.apsp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * APSP via repeated squaring (recursive doubling) in min-plus semiring.
 *
 * The timeline highlights how the matrix evolves as the maximum represented path
 * length doubles each iteration.
 */
public final class RecursiveDoublingApspAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure RECURSIVE-DOUBLING-APSP(W)",
            "  L <- W; m <- 1",
            "  while m < n-1:",
            "    L <- EXTEND-SHORTEST-PATHS(L, L)",
            "    m <- 2m",
            "return L",
            "end procedure"
    );

    private static final int INF = 1_000_000_000;

    @Override public String getDisplayName() { return "recurssive doubling"; }
    @Override public String getDescription() { return "All-pairs shortest paths by repeated squaring of distance matrices."; }
    @Override public AlgorithmType getType() { return AlgorithmType.APSP; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }
    @Override public String validate(AlgorithmRequest request) { return request.isEmpty() ? "Select vertices before running recursive doubling APSP." : null; }

    /** Runs the recursive-doubling APSP demonstration. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<Vertex> vertices = request.getScopeVertices();
        int n = vertices.size();
        List<AlgorithmStep> steps = new ArrayList<>();

        Map<Integer, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(vertices.get(i).getId(), i);
        }

        int[][] l = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                l[i][j] = (i == j) ? 0 : INF;
            }
        }
        for (Edge edge : request.getScopeEdges()) {
            int i = idx.get(edge.getSource().getId());
            int j = idx.get(edge.getDestination().getId());
            l[i][j] = Math.min(l[i][j], edge.getWeight());
        }

        int m = 1;
        int limit = Math.max(1, n - 1);
        steps.add(step(request, vertices, l, 1, m, -1, -1,
                "Initialize L and m",
                "L <- W and m <- 1 (direct-edge shortest paths)."));

        while (m < limit) {
            steps.add(step(request, vertices, l, 2, m, -1, -1,
                    "Check loop condition",
                    "m=" + m + " < " + limit + " so iterate."));

            int[][] next = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    next[i][j] = (i == j) ? 0 : INF;
                }
            }

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    steps.add(step(request, vertices, l, 3, m, i, j,
                            "Compute cell",
                            "Evaluate new L[" + vertices.get(i).getName() + ", " + vertices.get(j).getName() + "] with path bound " + (2 * m) + "."));

                    int best = INF;
                    for (int k = 0; k < n; k++) {
                        int candidate = (l[i][k] >= INF || l[k][j] >= INF) ? INF : l[i][k] + l[k][j];
                        if (candidate < best) {
                            best = candidate;
                        }
                    }
                    next[i][j] = best;

                    steps.add(step(request, vertices, next, 3, m, i, j,
                            "Cell finalized",
                            "new L[" + vertices.get(i).getName() + ", " + vertices.get(j).getName() + "] = " + fmt(best) + "."));
                }
            }

            l = next;
            int old = m;
            m *= 2;
            steps.add(step(request, vertices, l, 4, m, -1, -1,
                    "Double m",
                    "m <- 2m, " + old + " becomes " + m + "."));
        }

        steps.add(step(request, vertices, l, 2, m, -1, -1,
                "Check loop condition",
                "m=" + m + " is not less than " + limit + ", stop."));
        steps.add(step(request, vertices, l, 5, m, -1, -1,
                "Return L",
                "Recursive doubling APSP completed."));

        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one step describing the current matrix and doubling parameter m. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[][] matrix,
            int line,
            int m,
            int i,
            int j,
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
            labels.put(id, "diag=" + fmt(matrix[idx][idx]) + " m=" + m);
        }

        if (i >= 0) {
            vertexColors.put(vertices.get(i).getId(), AlgorithmPalette.ACTIVE);
            labels.put(vertices.get(i).getId(), "i");
        }
        if (j >= 0) {
            vertexColors.put(vertices.get(j).getId(), AlgorithmPalette.DISCOVERED);
            labels.put(vertices.get(j).getId(), "j");
        }

        MatrixSnapshot snapshot = toSnapshot("L matrix", matrix, vertices, i, j);
        return new AlgorithmStep(title, message, line, vertexColors, edgeColors, labels, snapshot);
    }

    /** Converts the current L matrix into a UI-ready matrix snapshot. */
    private static MatrixSnapshot toSnapshot(String title, int[][] matrix, List<Vertex> vertices, int hiRow, int hiCol) {
        List<String> names = new ArrayList<>();
        for (Vertex v : vertices) {
            names.add(v.getName());
        }

        List<List<String>> cells = new ArrayList<>();
        for (int r = 0; r < matrix.length; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < matrix.length; c++) {
                row.add(fmt(matrix[r][c]));
            }
            cells.add(row);
        }

        return new MatrixSnapshot(title, names, names, cells, hiRow, hiCol);
    }

    /** Formats finite values and the infinity sentinel for matrix display. */
    private static String fmt(int value) {
        return value >= INF ? "INF" : String.valueOf(value);
    }
}
