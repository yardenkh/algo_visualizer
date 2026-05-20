package algorithm.apsp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * APSP via min-plus matrix multiplication.
 *
 * This visualization shows how path-length coverage expands by repeatedly applying
 * EXTEND-SHORTEST-PATHS to the distance matrix.
 */
public final class MatrixMultiplicationApspAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure MATRIX-MULTIPLICATION-APSP(W)",
            "  L(1) <- W",
            "  for m <- 2 to n-1:",
            "    L(m) <- EXTEND-SHORTEST-PATHS(L(m-1), W)",
            "return L(n-1)",
            "end procedure"
    );

    private static final int INF = 1_000_000_000;

    @Override public String getDisplayName() { return "matrix multiplication"; }
    @Override public String getDescription() { return "All-pairs shortest paths by repeated min-plus matrix products."; }
    @Override public AlgorithmType getType() { return AlgorithmType.APSP; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }
    @Override public String validate(AlgorithmRequest request) { return request.isEmpty() ? "Select vertices before running matrix multiplication APSP." : null; }

    /** Runs the iterative min-plus matrix multiplication APSP demonstration. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<Vertex> vertices = request.getScopeVertices();
        int n = vertices.size();
        List<AlgorithmStep> steps = new ArrayList<>();

        Map<Integer, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(vertices.get(i).getId(), i);
        }

        int[][] w = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                w[i][j] = (i == j) ? 0 : INF;
            }
        }
        for (Edge edge : request.getScopeEdges()) {
            int i = idx.get(edge.getSource().getId());
            int j = idx.get(edge.getDestination().getId());
            w[i][j] = Math.min(w[i][j], edge.getWeight());
        }

        int[][] lPrev = copy(w);
        steps.add(step(request, vertices, lPrev, 1, -1, -1, "Initialize L(1)", "L(1) is set to direct weights matrix."));

        int maxM = Math.max(2, n - 1);
        for (int m = 2; m <= maxM; m++) {
            int[][] lCurr = fill(INF, n);
            for (int i = 0; i < n; i++) {
                lCurr[i][i] = 0;
            }
            steps.add(step(request, vertices, lPrev, 2, -1, -1,
                    "Iteration m=" + m,
                    "Compute L(" + m + ") using L(" + (m - 1) + ") * W in min-plus algebra."));

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    steps.add(step(request, vertices, lPrev, 3, i, j,
                            "Compute cell",
                            "Evaluate L(" + m + ")[" + vertices.get(i).getName() + ", " + vertices.get(j).getName() + "]."));
                    int best = INF;
                    for (int k = 0; k < n; k++) {
                        int candidate = (lPrev[i][k] >= INF || w[k][j] >= INF) ? INF : lPrev[i][k] + w[k][j];
                        if (candidate < best) {
                            best = candidate;
                        }
                    }
                    lCurr[i][j] = best;
                    steps.add(step(request, vertices, lCurr, 3, i, j,
                            "Cell finalized",
                            "L(" + m + ")[" + vertices.get(i).getName() + ", " + vertices.get(j).getName() + "] = " + fmt(best) + "."));
                }
            }
            lPrev = lCurr;
        }

        steps.add(step(request, vertices, lPrev, 4, -1, -1, "Return L(n-1)", "Matrix multiplication APSP completed."));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one matrix-focused visualization step for the current cell under evaluation. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[][] matrix,
            int line,
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
            vertexColors.put(vertex.getId(), AlgorithmPalette.SCOPE_NEUTRAL);
            labels.put(vertex.getId(), "");
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

    /** Converts the current L matrix into a reusable UI snapshot payload. */
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

    /** Creates an n x n matrix prefilled with a single value. */
    private static int[][] fill(int value, int n) {
        int[][] arr = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                arr[i][j] = value;
            }
        }
        return arr;
    }

    /** Deep-copies a square matrix used as the previous iteration state. */
    private static int[][] copy(int[][] src) {
        int[][] c = new int[src.length][src.length];
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, c[i], 0, src.length);
        }
        return c;
    }

    /** Formats a matrix cell, using INF for the sentinel value. */
    private static String fmt(int value) {
        return value >= INF ? "INF" : String.valueOf(value);
    }
}
