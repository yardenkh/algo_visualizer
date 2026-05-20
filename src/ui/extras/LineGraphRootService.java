package ui.extras;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Recognizes line graphs and reconstructs a valid root graph when possible.
 *
 * Approach:
 * - uses an exact edge-clique partition search based on Krausz characterization
 * - reconstructs a root graph from the clique incidence structure
 * - verifies the reconstructed root by checking that its line graph equals input
 */
public final class LineGraphRootService {

    public ReconstructionResult reconstruct(boolean[][] adjacency) {
        Validation validation = validateUndirectedSimple(adjacency);
        if (!validation.valid) {
            return ReconstructionResult.failure(validation.message);
        }

        int n = adjacency.length;
        if (n == 0) {
            return ReconstructionResult.success(0, List.of());
        }
        if (n > 62) {
            return ReconstructionResult.failure("Root reconstruction currently supports up to 62 vertices.");
        }

        List<IntEdge> inputEdges = enumerateEdges(adjacency);
        if (inputEdges.isEmpty()) {
            List<IntEdge> matching = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                matching.add(new IntEdge(2 * i, 2 * i + 1));
            }
            return ReconstructionResult.success(2 * n, matching);
        }

        List<Long> maximalCliques = enumerateMaximalCliques(adjacency);
        if (maximalCliques.isEmpty()) {
            return ReconstructionResult.failure("Graph is not a line graph.");
        }

        CliquePartitionSolver solver = new CliquePartitionSolver(adjacency, inputEdges, maximalCliques);
        List<Long> partition = solver.solve();
        if (partition == null) {
            return ReconstructionResult.failure("Selected graph is not a line graph, so no root graph exists.");
        }

        ReconstructionResult reconstructed = rebuildRootAndVerify(adjacency, partition);
        if (!reconstructed.isSuccess()) {
            return ReconstructionResult.failure("Selected graph is not a line graph, so no root graph exists.");
        }
        return reconstructed;
    }

    private ReconstructionResult rebuildRootAndVerify(boolean[][] adjacency, List<Long> partition) {
        int n = adjacency.length;
        List<long[]> memberships = new ArrayList<>(n);
        for (int v = 0; v < n; v++) {
            memberships.add(new long[2]);
        }

        for (int i = 0; i < partition.size(); i++) {
            long clique = partition.get(i);
            for (int v = 0; v < n; v++) {
                if ((clique & (1L << v)) == 0L) {
                    continue;
                }
                long[] slots = memberships.get(v);
                if (slots[0] == 0L) {
                    slots[0] = i + 1L;
                } else if (slots[1] == 0L) {
                    slots[1] = i + 1L;
                } else {
                    return ReconstructionResult.failure("Selected graph is not a line graph, so no root graph exists.");
                }
            }
        }

        List<IntEdge> rootEdges = new ArrayList<>(n);
        int nextRootVertex = partition.size();
        long[] endpointA = new long[n];
        long[] endpointB = new long[n];

        for (int v = 0; v < n; v++) {
            long[] slots = memberships.get(v);
            if (slots[0] != 0L && slots[1] != 0L) {
                endpointA[v] = slots[0] - 1L;
                endpointB[v] = slots[1] - 1L;
            } else if (slots[0] != 0L) {
                endpointA[v] = slots[0] - 1L;
                endpointB[v] = nextRootVertex++;
            } else {
                endpointA[v] = nextRootVertex++;
                endpointB[v] = nextRootVertex++;
            }
            if (endpointA[v] == endpointB[v]) {
                return ReconstructionResult.failure("Selected graph is not a line graph, so no root graph exists.");
            }
            rootEdges.add(new IntEdge((int) endpointA[v], (int) endpointB[v]));
        }

        // Enforce simple root graph (no parallel edges).
        java.util.HashSet<Long> usedPairs = new java.util.HashSet<>();
        for (IntEdge edge : rootEdges) {
            int min = Math.min(edge.a, edge.b);
            int max = Math.max(edge.a, edge.b);
            long key = (((long) min) << 32) | (max & 0xffffffffL);
            if (!usedPairs.add(key)) {
                return ReconstructionResult.failure("Selected graph is not a line graph, so no root graph exists.");
            }
        }

        // Final correctness check: L(root) must match input exactly.
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                boolean expected = adjacency[i][j];
                boolean actual = shareEndpoint(rootEdges.get(i), rootEdges.get(j));
                if (expected != actual) {
                    return ReconstructionResult.failure("Selected graph is not a line graph, so no root graph exists.");
                }
            }
        }

        return ReconstructionResult.success(nextRootVertex, rootEdges);
    }

    private boolean shareEndpoint(IntEdge first, IntEdge second) {
        return first.a == second.a
                || first.a == second.b
                || first.b == second.a
                || first.b == second.b;
    }

    private List<IntEdge> enumerateEdges(boolean[][] adjacency) {
        int n = adjacency.length;
        List<IntEdge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (adjacency[i][j]) {
                    edges.add(new IntEdge(i, j));
                }
            }
        }
        return edges;
    }

    private List<Long> enumerateMaximalCliques(boolean[][] adjacency) {
        int n = adjacency.length;
        long[] neighbors = new long[n];
        long all = 0L;
        for (int i = 0; i < n; i++) {
            all |= (1L << i);
            long mask = 0L;
            for (int j = 0; j < n; j++) {
                if (adjacency[i][j]) {
                    mask |= (1L << j);
                }
            }
            neighbors[i] = mask;
        }

        List<Long> cliques = new ArrayList<>();
        bronKerbosch(0L, all, 0L, neighbors, cliques);
        List<Long> filtered = new ArrayList<>();
        for (Long clique : cliques) {
            if (Long.bitCount(clique) >= 2) {
                filtered.add(clique);
            }
        }
        return filtered;
    }

    private void bronKerbosch(long r, long p, long x, long[] neighbors, List<Long> output) {
        if (p == 0L && x == 0L) {
            output.add(r);
            return;
        }

        int pivot = choosePivot(p | x, neighbors);
        long candidates = p & ~neighbors[pivot];

        while (candidates != 0L) {
            int v = Long.numberOfTrailingZeros(candidates);
            long vBit = 1L << v;
            bronKerbosch(r | vBit, p & neighbors[v], x & neighbors[v], neighbors, output);
            p &= ~vBit;
            x |= vBit;
            candidates &= ~vBit;
        }
    }

    private int choosePivot(long setMask, long[] neighbors) {
        int bestVertex = Long.numberOfTrailingZeros(setMask);
        int bestDegree = -1;
        long tmp = setMask;
        while (tmp != 0L) {
            int v = Long.numberOfTrailingZeros(tmp);
            int degree = Long.bitCount(neighbors[v] & setMask);
            if (degree > bestDegree) {
                bestDegree = degree;
                bestVertex = v;
            }
            tmp &= ~(1L << v);
        }
        return bestVertex;
    }

    private Validation validateUndirectedSimple(boolean[][] adjacency) {
        if (adjacency == null) {
            return Validation.invalid("Selected graph is invalid.");
        }
        for (int i = 0; i < adjacency.length; i++) {
            if (adjacency[i] == null || adjacency[i].length != adjacency.length) {
                return Validation.invalid("Selected graph is invalid.");
            }
            if (adjacency[i][i]) {
                return Validation.invalid("Find Root Graph supports only simple graphs (no self-loops).");
            }
            for (int j = i + 1; j < adjacency.length; j++) {
                if (adjacency[i][j] != adjacency[j][i]) {
                    return Validation.invalid("Find Root Graph supports only undirected simple graphs.");
                }
            }
        }
        return Validation.valid();
    }

    private static final class CliquePartitionSolver {
        private final List<IntEdge> edges;
        private final List<long[]> cliqueVertices;
        private final List<int[]> cliqueEdgeIndices;
        private final List<int[]> edgeToCliques;

        private final boolean[] covered;
        private final boolean[] chosen;
        private final int[] membershipCount;

        private List<Long> solution;

        private CliquePartitionSolver(boolean[][] adjacency, List<IntEdge> edges, List<Long> maximalCliques) {
            this.edges = edges;
            this.cliqueVertices = new ArrayList<>();
            this.cliqueEdgeIndices = new ArrayList<>();
            this.covered = new boolean[edges.size()];
            this.membershipCount = new int[adjacency.length];

            // De-duplicate cliques by mask.
            java.util.LinkedHashSet<Long> unique = new java.util.LinkedHashSet<>(maximalCliques);
            List<Long> cliques = new ArrayList<>(unique);
            this.chosen = new boolean[cliques.size()];

            int[][] edgeIndex = new int[adjacency.length][adjacency.length];
            for (int[] row : edgeIndex) {
                Arrays.fill(row, -1);
            }
            for (int i = 0; i < edges.size(); i++) {
                IntEdge e = edges.get(i);
                edgeIndex[e.a][e.b] = i;
                edgeIndex[e.b][e.a] = i;
            }

            for (Long cliqueMask : cliques) {
                long[] vertices = toVertexArray(cliqueMask);
                cliqueVertices.add(vertices);
                java.util.ArrayList<Integer> edgeList = new java.util.ArrayList<>();
                for (int i = 0; i < vertices.length; i++) {
                    for (int j = i + 1; j < vertices.length; j++) {
                        int u = (int) vertices[i];
                        int v = (int) vertices[j];
                        int idx = edgeIndex[u][v];
                        if (idx >= 0) {
                            edgeList.add(idx);
                        }
                    }
                }
                int[] list = new int[edgeList.size()];
                for (int i = 0; i < edgeList.size(); i++) {
                    list[i] = edgeList.get(i);
                }
                cliqueEdgeIndices.add(list);
            }

            edgeToCliques = new ArrayList<>(edges.size());
            for (int i = 0; i < edges.size(); i++) {
                IntEdge e = edges.get(i);
                java.util.ArrayList<Integer> candidates = new java.util.ArrayList<>();
                for (int c = 0; c < cliques.size(); c++) {
                    long mask = cliques.get(c);
                    long uBit = 1L << e.a;
                    long vBit = 1L << e.b;
                    if ((mask & uBit) != 0L && (mask & vBit) != 0L) {
                        candidates.add(c);
                    }
                }
                int[] list = new int[candidates.size()];
                for (int j = 0; j < candidates.size(); j++) {
                    list[j] = candidates.get(j);
                }
                edgeToCliques.add(list);
            }
        }

        private List<Long> solve() {
            for (int edgeIndex = 0; edgeIndex < edges.size(); edgeIndex++) {
                if (edgeToCliques.get(edgeIndex).length == 0) {
                    return null;
                }
            }
            if (backtrack(0)) {
                return solution;
            }
            return null;
        }

        private boolean backtrack(int coveredCount) {
            if (coveredCount == edges.size()) {
                solution = new ArrayList<>();
                for (int i = 0; i < chosen.length; i++) {
                    if (chosen[i]) {
                        solution.add(toMask(cliqueVertices.get(i)));
                    }
                }
                return true;
            }

            int nextEdge = pickNextUncoveredEdge();
            if (nextEdge < 0) {
                return false;
            }

            int[] candidates = edgeToCliques.get(nextEdge);
            for (int cliqueIndex : candidates) {
                if (!canChooseClique(cliqueIndex)) {
                    continue;
                }
                int delta = chooseClique(cliqueIndex);
                if (backtrack(coveredCount + delta)) {
                    return true;
                }
                unchooseClique(cliqueIndex);
            }
            return false;
        }

        private int pickNextUncoveredEdge() {
            int bestEdge = -1;
            int bestOptions = Integer.MAX_VALUE;
            for (int e = 0; e < edges.size(); e++) {
                if (covered[e]) {
                    continue;
                }
                int options = 0;
                for (int cliqueIndex : edgeToCliques.get(e)) {
                    if (canChooseClique(cliqueIndex)) {
                        options++;
                    }
                }
                if (options == 0) {
                    return e;
                }
                if (options < bestOptions) {
                    bestOptions = options;
                    bestEdge = e;
                }
            }
            return bestEdge;
        }

        private boolean canChooseClique(int cliqueIndex) {
            if (chosen[cliqueIndex]) {
                return false;
            }
            for (int edgeIndex : cliqueEdgeIndices.get(cliqueIndex)) {
                if (covered[edgeIndex]) {
                    return false;
                }
            }
            for (long vertex : cliqueVertices.get(cliqueIndex)) {
                if (membershipCount[(int) vertex] >= 2) {
                    return false;
                }
            }
            return true;
        }

        private int chooseClique(int cliqueIndex) {
            chosen[cliqueIndex] = true;
            for (long vertex : cliqueVertices.get(cliqueIndex)) {
                membershipCount[(int) vertex]++;
            }
            int delta = 0;
            for (int edgeIndex : cliqueEdgeIndices.get(cliqueIndex)) {
                covered[edgeIndex] = true;
                delta++;
            }
            return delta;
        }

        private void unchooseClique(int cliqueIndex) {
            chosen[cliqueIndex] = false;
            for (long vertex : cliqueVertices.get(cliqueIndex)) {
                membershipCount[(int) vertex]--;
            }
            for (int edgeIndex : cliqueEdgeIndices.get(cliqueIndex)) {
                covered[edgeIndex] = false;
            }
        }

        private long[] toVertexArray(long mask) {
            int count = Long.bitCount(mask);
            long[] vertices = new long[count];
            int k = 0;
            long tmp = mask;
            while (tmp != 0L) {
                int v = Long.numberOfTrailingZeros(tmp);
                vertices[k++] = v;
                tmp &= ~(1L << v);
            }
            return vertices;
        }

        private long toMask(long[] vertices) {
            long mask = 0L;
            for (long vertex : vertices) {
                mask |= (1L << vertex);
            }
            return mask;
        }
    }

    /** Immutable reconstruction output. */
    public static final class ReconstructionResult {
        private final boolean success;
        private final String message;
        private final int rootVertexCount;
        private final List<IntEdge> rootEdges;

        private ReconstructionResult(boolean success, String message, int rootVertexCount, List<IntEdge> rootEdges) {
            this.success = success;
            this.message = message;
            this.rootVertexCount = rootVertexCount;
            this.rootEdges = rootEdges;
        }

        public static ReconstructionResult success(int rootVertexCount, List<IntEdge> rootEdges) {
            return new ReconstructionResult(true, "", rootVertexCount, List.copyOf(rootEdges));
        }

        public static ReconstructionResult failure(String message) {
            return new ReconstructionResult(false, message, 0, List.of());
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getRootVertexCount() {
            return rootVertexCount;
        }

        public List<IntEdge> getRootEdges() {
            return rootEdges;
        }
    }

    /** Pair of vertex ids representing one simple undirected edge. */
    public static final class IntEdge {
        private final int a;
        private final int b;

        public IntEdge(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }
    }

    private static final class Validation {
        private final boolean valid;
        private final String message;

        private Validation(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        private static Validation valid() {
            return new Validation(true, "");
        }

        private static Validation invalid(String message) {
            return new Validation(false, message);
        }
    }
}


