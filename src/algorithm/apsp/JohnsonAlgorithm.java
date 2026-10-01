package algorithm.apsp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Johnson APSP teaching implementation.
 *
 * The timeline follows the classic textbook pseudocode line-by-line,
 * including explicit construction of G' (with super-source), Bellman-Ford,
 * reweighting, per-source Dijkstra, and distance restoration.
 */
public final class JohnsonAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "JOHNSON(G, w)",
            "    compute G', where G'.V = G.V U {s}, G'.E = G.E U {(s, v) : v in G.V}, and w(s, v) = 0 for all v in G.V",
            "    if BELLMAN-FORD(G', w, s) == FALSE",
            "        print \"the input graph contains a negative-weight cycle\"",
            "    else for each vertex v in G'.V",
            "        set h(v) to the value of delta(s, v) computed by the Bellman-Ford algorithm",
            "        for each edge (u, v) in G'.E",
            "            w_hat(u, v) = w(u, v) + h(u) - h(v)",
            "        let D = (d_uv) be a new n x n matrix",
            "        for each vertex u in G.V",
            "           run DIJKSTRA(G, w_hat, u) to compute delta_hat(u, v) for all v in G.V",
            "           for each vertex v in G.V",
            "               d_uv = delta_hat(u, v) + h(v) - h(u)",
            "       return D"
    );

    private static final int INF = 1_000_000_000;
    private static final int SUPER_SOURCE_ID = -1;

    @Override public String getDisplayName() { return "johnson"; }
    @Override public String getDescription() { return "All-pairs shortest paths using reweighting + Dijkstra."; }
    @Override public AlgorithmType getType() { return AlgorithmType.APSP; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }

    @Override
    public String validate(AlgorithmRequest request) {
        if (request.isEmpty()) {
            return "Select vertices before running Johnson.";
        }
        return null;
    }

    /** Runs Johnson exactly as the displayed pseudocode and emits teachable intermediate states. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<Vertex> vertices = request.getScopeVertices();
        int n = vertices.size();
        List<AlgorithmStep> steps = new ArrayList<>();
        Map<Integer, Integer> indexById = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            indexById.put(vertices.get(i).getId(), i);
        }

        List<Arc> originalArcs = new ArrayList<>();
        for (Edge edge : request.getScopeEdges()) {
            originalArcs.add(new Arc(
                    indexById.get(edge.getSource().getId()),
                    indexById.get(edge.getDestination().getId()),
                    edge.getWeight(),
                    edge
            ));
        }

        int superIndex = n;
        List<Arc> augmentedArcs = new ArrayList<>(originalArcs);
        for (int v = 0; v < n; v++) {
            augmentedArcs.add(new Arc(superIndex, v, 0, null));
        }

        int[] hAll = new int[n + 1];
        for (int i = 0; i < hAll.length; i++) {
            hAll[i] = INF;
        }
        hAll[superIndex] = 0;

        steps.add(step(
                request,
                1,
                "Build G'",
                "Added super-source s and zero-weight edges (s, v) for all vertices. Open the Residual Graph tab to inspect G'.",
                labelsFromPotentials(vertices, hAll),
                Map.of(),
                null,
                buildSnapshot(request, vertices, originalArcs, augmentedArcs, null, true, false),
                null,
                -1,
                -1
        ));

        boolean relaxedOnNthPass = false;
        for (int pass = 1; pass <= n; pass++) {
            boolean changed = false;
            for (Arc arc : augmentedArcs) {
                if (hAll[arc.from] >= INF) {
                    continue;
                }
                int candidate = hAll[arc.from] + arc.weight;
                if (candidate < hAll[arc.to]) {
                    hAll[arc.to] = candidate;
                    changed = true;
                }
            }
            steps.add(step(
                    request,
                    2,
                    "Bellman-Ford pass " + pass,
                    "Relaxed all edges of G' to compute delta(s, v).",
                    labelsFromPotentials(vertices, hAll),
                    Map.of(),
                    null,
                    buildSnapshot(request, vertices, originalArcs, augmentedArcs, null, true, false),
                    hVectorSnapshot(vertices, hAll, true),
                    -1,
                    -1
            ));
            if (!changed) {
                break;
            }
            if (pass == n && changed) {
                relaxedOnNthPass = true;
            }
        }

        if (relaxedOnNthPass) {
            steps.add(step(
                    request,
                    3,
                    "Negative cycle detected",
                    "the input graph contains a negative-weight cycle",
                    labelsFromPotentials(vertices, hAll),
                    Map.of(),
                    null,
                    buildSnapshot(request, vertices, originalArcs, augmentedArcs, null, true, false),
                    hVectorSnapshot(vertices, hAll, true),
                    -1,
                    -1
            ));
            return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps, true,
                    "the input graph contains a negative-weight cycle");
        }

        steps.add(step(
                request,
                5,
                "Store potentials h(v)",
                "Bellman-Ford succeeded; set h(v)=delta(s,v) for each vertex.",
                labelsFromPotentials(vertices, hAll),
                Map.of(),
                null,
                buildSnapshot(request, vertices, originalArcs, augmentedArcs, null, true, false),
                hVectorSnapshot(vertices, hAll, true),
                -1,
                -1
        ));

        List<Arc> reweightedArcs = new ArrayList<>();
        Map<EdgeKey, String> reweightAnnotations = new LinkedHashMap<>();
        for (Arc arc : originalArcs) {
            int reweighted = arc.weight + hAll[arc.from] - hAll[arc.to];
            reweightedArcs.add(new Arc(arc.from, arc.to, reweighted, arc.sourceEdge));
            EdgeKey key = EdgeKey.from(arc.sourceEdge);
            reweightAnnotations.put(key, "w=" + arc.weight + " -> w_hat=" + reweighted);
            steps.add(step(
                    request,
                    7,
                    "Reweight edge",
                    vertices.get(arc.from).getName() + " -> " + vertices.get(arc.to).getName()
                            + ": w_hat = " + arc.weight + " + h(u) - h(v) = " + reweighted + ".",
                    labelsFromPotentials(vertices, hAll),
                    reweightAnnotations,
                    key,
                    buildSnapshot(request, vertices, originalArcs, augmentedArcs, reweightedArcs, true, true),
                    hVectorSnapshot(vertices, hAll, false),
                    arc.from,
                    arc.to
            ));
        }

        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = (i == j) ? 0 : INF;
            }
        }

        steps.add(step(
                request,
                8,
                "Initialize D",
                "Created D as an n x n matrix over vertices of G.",
                labelsFromPotentials(vertices, hAll),
                reweightAnnotations,
                null,
                buildSnapshot(request, vertices, originalArcs, augmentedArcs, reweightedArcs, false, true),
                dMatrixSnapshot("D matrix (current)", vertices, d, -1, -1),
                -1,
                -1
        ));

        for (int u = 0; u < n; u++) {
            Vertex source = vertices.get(u);
            steps.add(step(
                    request,
                    9,
                    "Choose source " + source.getName(),
                    "Run Dijkstra from " + source.getName() + " on reweighted graph.",
                    labelsFromPotentials(vertices, hAll),
                    reweightAnnotations,
                    null,
                    buildSnapshot(request, vertices, originalArcs, augmentedArcs, reweightedArcs, false, true),
                    dMatrixSnapshot("D matrix (before row " + source.getName() + ")", vertices, d, u, -1),
                    u,
                    -1
            ));

            int[] dHat = dijkstraOnReweighted(n, reweightedArcs, u);
            steps.add(step(
                    request,
                    10,
                    "Dijkstra done for " + source.getName(),
                    "Computed delta_hat(" + source.getName() + ", v) for all v.",
                    labelsFromPotentials(vertices, hAll),
                    reweightAnnotations,
                    null,
                    buildSnapshot(request, vertices, originalArcs, augmentedArcs, reweightedArcs, false, true),
                    dHatRowSnapshot(vertices, source.getName(), dHat, u),
                    u,
                    -1
            ));

            for (int v = 0; v < n; v++) {
                if (dHat[v] >= INF || hAll[v] >= INF || hAll[u] >= INF) {
                    d[u][v] = INF;
                } else {
                    d[u][v] = dHat[v] + hAll[v] - hAll[u];
                }
                steps.add(step(
                        request,
                        12,
                        "Restore d(" + source.getName() + ", " + vertices.get(v).getName() + ")",
                        "d_uv = delta_hat(u,v) + h(v) - h(u) = " + fmt(d[u][v]) + ".",
                        labelsFromPotentials(vertices, hAll),
                        reweightAnnotations,
                        null,
                        buildSnapshot(request, vertices, originalArcs, augmentedArcs, reweightedArcs, false, true),
                        dMatrixSnapshot("D matrix (current)", vertices, d, u, v),
                        u,
                        v
                ));
            }
        }

        steps.add(step(
                request,
                13,
                "Return D",
                "Johnson completed. G' and temporary reweighting were used only for this procedure.",
                labelsFromPotentials(vertices, hAll),
                reweightAnnotations,
                null,
                buildSnapshot(request, vertices, originalArcs, augmentedArcs, reweightedArcs, false, true),
                dMatrixSnapshot("Final D matrix", vertices, d, -1, -1),
                -1,
                -1
        ));

        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    private static int[] dijkstraOnReweighted(int n, List<Arc> arcs, int source) {
        List<List<Arc>> outgoing = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            outgoing.add(new ArrayList<>());
        }
        for (Arc arc : arcs) {
            outgoing.get(arc.from).add(arc);
        }
        for (List<Arc> bucket : outgoing) {
            bucket.sort(Comparator.comparingInt(a -> a.to));
        }

        int[] dist = new int[n];
        for (int i = 0; i < n; i++) {
            dist[i] = INF;
        }
        dist[source] = 0;

        PriorityQueue<NodeDist> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.distance));
        pq.add(new NodeDist(source, 0));

        while (!pq.isEmpty()) {
            NodeDist current = pq.poll();
            if (current.distance != dist[current.node]) {
                continue;
            }
            for (Arc arc : outgoing.get(current.node)) {
                if (dist[current.node] >= INF) {
                    continue;
                }
                int candidate = dist[current.node] + arc.weight;
                if (candidate < dist[arc.to]) {
                    dist[arc.to] = candidate;
                    pq.add(new NodeDist(arc.to, candidate));
                }
            }
        }
        return dist;
    }

    private static AlgorithmStep step(
            AlgorithmRequest request,
            int line,
            String title,
            String message,
            Map<Integer, String> vertexAnnotations,
            Map<EdgeKey, String> edgeAnnotations,
            EdgeKey activeEdge,
            ResidualGraphSnapshot snapshot,
            MatrixSnapshot matrix,
            int activeU,
            int activeV
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        for (Vertex vertex : request.getScopeVertices()) {
            vertexColors.put(vertex.getId(), AlgorithmPalette.SCOPE_NEUTRAL);
        }
        if (activeU >= 0 && activeU < request.getScopeVertices().size()) {
            vertexColors.put(request.getScopeVertices().get(activeU).getId(), AlgorithmPalette.ACTIVE);
        }
        if (activeV >= 0 && activeV < request.getScopeVertices().size()) {
            vertexColors.put(request.getScopeVertices().get(activeV).getId(), AlgorithmPalette.DISCOVERED);
        }
        if (activeEdge != null) {
            edgeColors.put(activeEdge, AlgorithmPalette.ACTIVE_EDGE);
        }
        return new AlgorithmStep(
                title,
                message,
                line,
                vertexColors,
                edgeColors,
                vertexAnnotations,
                edgeAnnotations,
                matrix,
                null,
                snapshot,
                null
        );
    }

    private static Map<Integer, String> labelsFromPotentials(List<Vertex> vertices, int[] hAll) {
        Map<Integer, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            labels.put(vertices.get(i).getId(), "h=" + fmt(hAll[i]));
        }
        return labels;
    }

    private static MatrixSnapshot hVectorSnapshot(List<Vertex> vertices, int[] hAll, boolean includeSuper) {
        List<String> rows = new ArrayList<>();
        List<List<String>> cells = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            rows.add(vertices.get(i).getName());
            cells.add(List.of(fmt(hAll[i])));
        }
        if (includeSuper) {
            rows.add("s");
            cells.add(List.of(fmt(hAll[vertices.size()])));
        }
        return new MatrixSnapshot("Potentials h(v)", rows, List.of("h"), cells, -1, 0);
    }

    private static MatrixSnapshot dHatRowSnapshot(List<Vertex> vertices, String sourceName, int[] dHat, int hiRow) {
        List<String> rows = new ArrayList<>();
        List<List<String>> cells = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            rows.add(vertices.get(i).getName());
            cells.add(List.of(fmt(dHat[i])));
        }
        return new MatrixSnapshot("delta_hat(" + sourceName + ", v)", rows, List.of("delta_hat"), cells, hiRow, 0);
    }

    private static MatrixSnapshot dMatrixSnapshot(String title, List<Vertex> vertices, int[][] matrix, int hiRow, int hiCol) {
        List<String> labels = new ArrayList<>();
        for (Vertex vertex : vertices) {
            labels.add(vertex.getName());
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

    private static ResidualGraphSnapshot buildSnapshot(
            AlgorithmRequest request,
            List<Vertex> vertices,
            List<Arc> originalArcs,
            List<Arc> augmentedArcs,
            List<Arc> reweightedArcs,
            boolean includeSuper,
            boolean showReweighted
    ) {
        List<Integer> ids = new ArrayList<>();
        Map<Integer, String> labels = new LinkedHashMap<>();
        Map<Integer, Point2D.Double> positions = new LinkedHashMap<>();

        for (Vertex vertex : vertices) {
            ids.add(vertex.getId());
            labels.put(vertex.getId(), vertex.getName());
        }
        if (includeSuper) {
            ids.add(SUPER_SOURCE_ID);
            labels.put(SUPER_SOURCE_ID, "s");
        }

        Map<Integer, Point2D.Double> known = request.getVertexPositions();
        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            Point2D.Double p = known.get(vertex.getId());
            if (p != null) {
                positions.put(vertex.getId(), new Point2D.Double(p.x, p.y));
            } else {
                double angle = (2.0 * Math.PI * i) / Math.max(1, vertices.size());
                positions.put(vertex.getId(), new Point2D.Double(0.6 + 0.35 * Math.cos(angle), 0.5 + 0.35 * Math.sin(angle)));
            }
        }
        if (includeSuper) {
            Point2D.Double sPos = superSourcePosition(positions);
            positions.put(SUPER_SOURCE_ID, sPos);
        }

        List<ResidualGraphSnapshot.ResidualEdge> edges = new ArrayList<>();
        List<EdgeKey> highlighted = new ArrayList<>();
        if (showReweighted && reweightedArcs != null) {
            for (Arc arc : reweightedArcs) {
                int sourceId = vertices.get(arc.from).getId();
                int destinationId = vertices.get(arc.to).getId();
                edges.add(new ResidualGraphSnapshot.ResidualEdge(sourceId, destinationId, arc.weight));
                highlighted.add(new EdgeKey(sourceId, destinationId));
            }
            if (includeSuper) {
                for (Arc arc : augmentedArcs) {
                    if (arc.from == vertices.size()) {
                        edges.add(new ResidualGraphSnapshot.ResidualEdge(SUPER_SOURCE_ID, vertices.get(arc.to).getId(), arc.weight));
                    }
                }
            }
        } else if (includeSuper) {
            for (Arc arc : augmentedArcs) {
                int sourceId = arc.from == vertices.size() ? SUPER_SOURCE_ID : vertices.get(arc.from).getId();
                int destinationId = vertices.get(arc.to).getId();
                edges.add(new ResidualGraphSnapshot.ResidualEdge(sourceId, destinationId, arc.weight));
                if (sourceId == SUPER_SOURCE_ID) {
                    highlighted.add(new EdgeKey(sourceId, destinationId));
                }
            }
        } else {
            for (Arc arc : originalArcs) {
                int sourceId = vertices.get(arc.from).getId();
                int destinationId = vertices.get(arc.to).getId();
                edges.add(new ResidualGraphSnapshot.ResidualEdge(sourceId, destinationId, arc.weight));
            }
        }

        return new ResidualGraphSnapshot(ids, labels, positions, edges, highlighted);
    }

    private static Point2D.Double superSourcePosition(Map<Integer, Point2D.Double> positions) {
        if (positions.isEmpty()) {
            return new Point2D.Double(0.08, 0.5);
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Point2D.Double point : positions.values()) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        double y = (minY + maxY) / 2.0;
        return new Point2D.Double(minX - 80.0, y);
    }

    private static String fmt(int value) {
        return value >= INF ? "INF" : String.valueOf(value);
    }

    private static final class Arc {
        private final int from;
        private final int to;
        private final int weight;
        private final Edge sourceEdge;

        private Arc(int from, int to, int weight, Edge sourceEdge) {
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.sourceEdge = sourceEdge;
        }
    }

    private static final class NodeDist {
        private final int node;
        private final int distance;

        private NodeDist(int node, int distance) {
            this.node = node;
            this.distance = distance;
        }
    }
}
