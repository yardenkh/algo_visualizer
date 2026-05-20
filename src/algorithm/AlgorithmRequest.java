package algorithm;

import graph.DirectedGraph;
import graph.Edge;
import graph.Vertex;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable algorithm input built from the currently selected graph scope.
 *
 * This object is the contract boundary between editor state and algorithm execution.
 * It ensures algorithms see a stable, sorted view of selected vertices/edges.
 *
 * Roadmap: see {@code PROJECT_NAVIGATION.md} and {@code project roadmap.md}.
 */
public final class AlgorithmRequest {
    private final DirectedGraph graph;
    private final List<Vertex> scopeVertices;
    private final List<Edge> scopeEdges;
    private final Set<Integer> scopeVertexIds;
    private final Vertex sourceVertex;
    private final Vertex sinkVertex;
    private final Map<Integer, Point2D.Double> vertexPositions;

    /**
     * Creates a stable request snapshot from the current editor scope.
     *
     * Vertices and edges are copied and sorted so timeline generation is deterministic.
     */
    public AlgorithmRequest(DirectedGraph graph, Collection<Vertex> scopeVertices, Collection<Edge> scopeEdges, Vertex sourceVertex) {
        this(graph, scopeVertices, scopeEdges, sourceVertex, null, Map.of());
    }

    /**
     * Creates a stable request snapshot from the current editor scope.
     *
     * Vertices and edges are copied and sorted so timeline generation is deterministic.
     */
    public AlgorithmRequest(DirectedGraph graph, Collection<Vertex> scopeVertices, Collection<Edge> scopeEdges, Vertex sourceVertex, Vertex sinkVertex) {
        this(graph, scopeVertices, scopeEdges, sourceVertex, sinkVertex, Map.of());
    }

    /**
     * Creates a stable request snapshot from the current editor scope.
     *
     * Vertices and edges are copied and sorted so timeline generation is deterministic.
     */
    public AlgorithmRequest(
            DirectedGraph graph,
            Collection<Vertex> scopeVertices,
            Collection<Edge> scopeEdges,
            Vertex sourceVertex,
            Vertex sinkVertex,
            Map<Integer, Point2D.Double> vertexPositions
    ) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
        Objects.requireNonNull(scopeVertices, "scopeVertices cannot be null");
        Objects.requireNonNull(scopeEdges, "scopeEdges cannot be null");

        List<Vertex> sortedVertices = new ArrayList<>(scopeVertices);
        sortedVertices.sort(Comparator.comparingInt(Vertex::getId));
        this.scopeVertices = Collections.unmodifiableList(sortedVertices);

        List<Edge> sortedEdges = new ArrayList<>(scopeEdges);
        sortedEdges.sort(Comparator
                .comparingInt((Edge edge) -> edge.getSource().getId())
                .thenComparingInt(edge -> edge.getDestination().getId()));
        this.scopeEdges = Collections.unmodifiableList(sortedEdges);

        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        for (Vertex vertex : sortedVertices) {
            ids.add(vertex.getId());
        }
        this.scopeVertexIds = Collections.unmodifiableSet(ids);
        this.sourceVertex = sourceVertex;
        this.sinkVertex = sinkVertex;

        Map<Integer, Point2D.Double> frozen = new LinkedHashMap<>();
        for (Map.Entry<Integer, Point2D.Double> e : vertexPositions.entrySet()) {
            if (ids.contains(e.getKey()) && e.getValue() != null) {
                frozen.put(e.getKey(), new Point2D.Double(e.getValue().x, e.getValue().y));
            }
        }
        this.vertexPositions = Collections.unmodifiableMap(frozen);

        if (sourceVertex != null && !ids.contains(sourceVertex.getId())) {
            throw new IllegalArgumentException("source vertex must be inside the selected scope");
        }
        if (sinkVertex != null && !ids.contains(sinkVertex.getId())) {
            throw new IllegalArgumentException("sink vertex must be inside the selected scope");
        }
    }

    /** @return backing graph model (read-only usage expected by algorithms). */
    public DirectedGraph getGraph() {
        return graph;
    }

    /** @return sorted immutable vertices in scope. */
    public List<Vertex> getScopeVertices() {
        return scopeVertices;
    }

    /** @return sorted immutable edges in scope. */
    public List<Edge> getScopeEdges() {
        return scopeEdges;
    }

    /** @return selected source vertex, or null for source-less algorithms. */
    public Vertex getSourceVertex() {
        return sourceVertex;
    }

    /** @return selected sink vertex, or null when algorithm does not require one. */
    public Vertex getSinkVertex() {
        return sinkVertex;
    }

    /** @return true when scope has no vertices. */
    public boolean isEmpty() {
        return scopeVertices.isEmpty();
    }

    /**
     * Checks whether a vertex belongs to this request scope.
     */
    public boolean containsVertex(Vertex vertex) {
        return vertex != null && scopeVertexIds.contains(vertex.getId());
    }

    /**
     * @return true if any scoped edge has negative weight.
     *
     * Used by validation for algorithms like Dijkstra/repeated Dijkstra.
     */
    public boolean hasNegativeWeightEdge() {
        for (Edge edge : scopeEdges) {
            if (edge.getWeight() < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns outgoing edges from {@code vertex} restricted to this scope.
     *
     * This prevents algorithms from accidentally traversing edges that leave
     * the selected subgraph.
     */
    public List<Edge> getOutgoingEdges(Vertex vertex) {
        if (vertex == null) {
            return Collections.emptyList();
        }
        if (!containsVertex(vertex)) {
            return Collections.emptyList();
        }

        List<Edge> outgoing = new ArrayList<>();
        for (Edge edge : graph.getOutgoingEdges(vertex)) {
            if (containsVertex(edge.getDestination())) {
                outgoing.add(edge);
            }
        }
        outgoing.sort(Comparator.comparingInt(edge -> edge.getDestination().getId()));
        return Collections.unmodifiableList(outgoing);
    }

    /** @return immutable scoped vertex positions for visualization side-views. */
    public Map<Integer, Point2D.Double> getVertexPositions() {
        return vertexPositions;
    }

    /**
     * @return true when every non-loop scoped edge (u,v) has the reverse edge (v,u).
     *
     * This is used by UI compatibility checks to detect an undirected-style scope
     * represented as paired directed arcs.
     */
    public boolean isSymmetricBidirectionalScope() {
        if (scopeEdges.isEmpty()) {
            return false;
        }
        Set<Long> pairs = new LinkedHashSet<>();
        for (Edge edge : scopeEdges) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            if (u == v) {
                continue;
            }
            pairs.add(edgeKey(u, v));
        }
        if (pairs.isEmpty()) {
            return false;
        }
        for (Long key : pairs) {
            int u = sourceIdFromKey(key);
            int v = destinationIdFromKey(key);
            if (!pairs.contains(edgeKey(v, u))) {
                return false;
            }
        }
        return true;
    }

    /** @return true when at least one scoped non-loop edge is missing its reverse pair. */
    public boolean hasAsymmetricScopedEdges() {
        Set<Long> pairs = new LinkedHashSet<>();
        for (Edge edge : scopeEdges) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            if (u == v) {
                continue;
            }
            pairs.add(edgeKey(u, v));
        }
        for (Long key : pairs) {
            int u = sourceIdFromKey(key);
            int v = destinationIdFromKey(key);
            if (!pairs.contains(edgeKey(v, u))) {
                return true;
            }
        }
        return false;
    }

    private static long edgeKey(int sourceId, int destinationId) {
        return ((long) sourceId << 32) | (destinationId & 0xffffffffL);
    }

    private static int sourceIdFromKey(long key) {
        return (int) (key >> 32);
    }

    private static int destinationIdFromKey(long key) {
        return (int) key;
    }
}
