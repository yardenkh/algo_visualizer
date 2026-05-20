package algorithm.general;

import algorithm.*;
import algorithm.recursion.SeidelStackFrameAdapter;
import algorithm.recursion.StackFramesSnapshot;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seidel APSP (unweighted undirected); this implementation is a pedagogical timeline.
 *
 * The algorithm demonstrates recursive matrix reduction and exposes the conceptual
 * recursion through both matrix snapshots and stack-frame snapshots.
 */
public final class SeidelAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure SEIDEL-APSP(G)",
            "  if graph is complete return base distances",
            "  build A^2 and recursively solve reduced problem",
            "  combine recursive distances to recover exact shortest paths",
            "end procedure"
    );
    private static final SeidelStackFrameAdapter STACK_ADAPTER = new SeidelStackFrameAdapter();

    @Override public String getDisplayName() { return "seidel"; }
    @Override public String getDescription() { return "APSP for unweighted undirected graphs (educational timeline)."; }
    @Override public AlgorithmType getType() { return AlgorithmType.GENERAL; }
    @Override public GraphOrientationRequirement getOrientationRequirement() { return GraphOrientationRequirement.UNDIRECTED_ONLY; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }
    @Override public String validate(AlgorithmRequest request) { return request.isEmpty() ? "Select vertices before running Seidel." : null; }

    /** Runs the pedagogical Seidel timeline with recursive stack visualization. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        List<Vertex> vertices = request.getScopeVertices();
        List<SeidelStackFrameAdapter.SeidelFrameState> stackFrames = new ArrayList<>();
        int n = vertices.size();

        Map<Integer, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(vertices.get(i).getId(), i);
        }

        int[][] a = new int[n][n];
        for (int i = 0; i < n; i++) {
            a[i][i] = 0;
        }
        for (Edge edge : request.getScopeEdges()) {
            int i = idx.get(edge.getSource().getId());
            int j = idx.get(edge.getDestination().getId());
            a[i][j] = 1;
            a[j][i] = 1;
        }

        int depth = 0;
        int size = n;
        while (true) {
            stackFrames.add(new SeidelStackFrameAdapter.SeidelFrameState(depth, size, "enter"));
            steps.add(step(request, vertices, a, stackFrames, 1, -1, -1,
                    "Recursive call depth " + depth,
                    "Evaluate Seidel base condition at depth " + depth + "."));

            if (size <= 2 || depth >= 3) {
                replaceTop(stackFrames, new SeidelStackFrameAdapter.SeidelFrameState(depth, size, "base"));
                steps.add(step(request, vertices, a, stackFrames, 1, -1, -1,
                        "Base case",
                        "Graph is small enough for direct answer at depth " + depth + "."));
                break;
            }

            replaceTop(stackFrames, new SeidelStackFrameAdapter.SeidelFrameState(depth, size, "reduce"));
            steps.add(step(request, vertices, a, stackFrames, 2, -1, -1,
                    "Build reduced instance",
                    "Build matrix A^2 and reduced adjacency relation."));

            int[][] reduced = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int value = 0;
                    for (int k = 0; k < n; k++) {
                        if (a[i][k] == 1 && a[k][j] == 1) {
                            value = 1;
                            break;
                        }
                    }
                    reduced[i][j] = value;
                }
            }
            a = reduced;

            replaceTop(stackFrames, new SeidelStackFrameAdapter.SeidelFrameState(depth, size, "recurse"));
            steps.add(step(request, vertices, a, stackFrames, 2, -1, -1,
                    "Recurse",
                    "Call Seidel recursively on reduced matrix (depth " + (depth + 1) + ")."));
            depth++;
            size = Math.max(2, size / 2);
        }

        for (int d = depth; d >= 0; d--) {
            replaceTop(stackFrames, new SeidelStackFrameAdapter.SeidelFrameState(d, Math.max(2, size), "combine"));
            steps.add(step(request, vertices, a, stackFrames, 3, -1, -1,
                    "Combine level " + d,
                    "Combine recursive result into distance estimate at depth " + d + "."));
            if (!stackFrames.isEmpty()) {
                stackFrames.remove(stackFrames.size() - 1);
            }
        }

        steps.add(step(request, vertices, a, stackFrames, 4,
                -1,
                -1,
                "Complete",
                "Seidel demonstration finished."));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one Seidel step with both matrix and stack-frame payloads. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[][] matrix,
            List<SeidelStackFrameAdapter.SeidelFrameState> stackFrames,
            int line,
            int hiRow,
            int hiCol,
            String title,
            String message
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> labels = new LinkedHashMap<>();

        for (Vertex v : request.getScopeVertices()) {
            vertexColors.put(v.getId(), AlgorithmPalette.SCOPE_NEUTRAL);
            labels.put(v.getId(), "");
        }

        MatrixSnapshot snapshot = toSnapshot("A matrix", matrix, vertices, hiRow, hiCol);
        StackFramesSnapshot stackSnapshot = STACK_ADAPTER.toSnapshot(stackFrames, "Seidel Call Stack");
        return new AlgorithmStep(title, message, line, vertexColors, edgeColors, labels, snapshot, stackSnapshot);
    }

    /** Replaces the current top recursion frame without changing stack depth. */
    private static void replaceTop(List<SeidelStackFrameAdapter.SeidelFrameState> stackFrames, SeidelStackFrameAdapter.SeidelFrameState frame) {
        if (stackFrames.isEmpty()) {
            stackFrames.add(frame);
            return;
        }
        stackFrames.set(stackFrames.size() - 1, frame);
    }

    /** Converts the current Seidel matrix into a generic matrix snapshot. */
    private static MatrixSnapshot toSnapshot(String title, int[][] matrix, List<Vertex> vertices, int hiRow, int hiCol) {
        List<String> names = new ArrayList<>();
        for (Vertex v : vertices) {
            names.add(v.getName());
        }

        List<List<String>> cells = new ArrayList<>();
        for (int r = 0; r < matrix.length; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < matrix.length; c++) {
                row.add(String.valueOf(matrix[r][c]));
            }
            cells.add(row);
        }
        return new MatrixSnapshot(title, names, names, cells, hiRow, hiCol);
    }
}
