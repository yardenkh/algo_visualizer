package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Container for a directed graph with vertices and edges.
 *
 * This class provides:
 * - Vertex storage by id for O(1) lookup
 * - Graph-wide edge list
 * - Structural operations: add/remove vertices and edges
 * - Query operations: find edges, check connectivity
 *
 * Vertices and edges are stored mutable; the graph does not enforce
 * immutability of the data returned to callers.
 *
 * All algorithm execution uses a {@link algorithm.AlgorithmRequest} to specify
 * a scope (subset of vertices/edges), allowing algorithms to run on
 * selected subgraphs without graph modification.
 *
 * Related classes: {@link graph.Vertex}, {@link graph.Edge}.
 * Roadmap: see {@code project roadmap.md}.
 */
public class DirectedGraph {
    /** Vertices stored by id for fast lookup and uniqueness. */
    private final Map<Integer, Vertex> vertices;
    /** Graph-wide list of directed edges. */
    private final List<Edge> edges;

    /** Creates an empty directed graph. */
    public DirectedGraph() {
        this.vertices = new LinkedHashMap<>();
        this.edges = new ArrayList<>();
    }

    /**
     * Returns all vertices currently stored in the graph.
     * The returned list is an unmodifiable snapshot of the current vertices.
     *
     * @return unmodifiable list of vertices
     */
    public List<Vertex> getVertices() {
        return Collections.unmodifiableList(new ArrayList<>(vertices.values()));
    }

    /**
     * Returns all edges currently stored in the graph.
     * The returned list is an unmodifiable snapshot of the current edges.
     *
     * @return unmodifiable list of edges
     */
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(new ArrayList<>(edges));
    }

    /** @return number of vertices in the graph */
    public int vertexCount() {
        return vertices.size();
    }

    /** @return number of edges in the graph */
    public int edgeCount() {
        return edges.size();
    }

    /** @return {@code true} if the graph currently has no vertices */
    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    /**
     * @param vertex vertex to test
     * @return {@code true} if a vertex with the same id exists in the graph
     */
    public boolean containsVertex(Vertex vertex) {
        return vertex != null && vertices.containsKey(vertex.getId());
    }

    /**
     * @param id vertex id to test
     * @return {@code true} if a vertex with that id exists in the graph
     */
    public boolean containsVertex(int id) {
        return vertices.containsKey(id);
    }

    /**
     * @param id vertex id to look up
     * @return the matching vertex, or {@code null} if it is not present
     */
    public Vertex getVertex(int id) {
        return vertices.get(id);
    }

    /**
     * Adds a vertex if its id is not already present.
     * If a vertex with the same id already exists, the existing stored vertex is returned.
     *
     * @param vertex vertex to add
     * @return the stored vertex instance for that id
     * @throws NullPointerException if {@code vertex} is {@code null}
     */
    public Vertex addVertex(Vertex vertex) {
        Objects.requireNonNull(vertex, "vertex cannot be null");
        vertices.putIfAbsent(vertex.getId(), vertex);
        return vertices.get(vertex.getId());
    }

    /**
     * Creates and adds a vertex with a generated name.
     *
     * @param id vertex id
     * @return the stored vertex instance for that id
     */
    public Vertex addVertex(int id) {
        return addVertex(new Vertex(id));
    }

    /**
     * Creates and adds a vertex with an explicit name.
     *
     * @param name vertex name
     * @param id vertex id
     * @return the stored vertex instance for that id
     */
    public Vertex addVertex(String name, int id) {
        return addVertex(new Vertex(name, id));
    }

    /**
     * Removes a vertex and every edge incident to it.
     *
     * @param vertex vertex to remove
     * @return {@code true} if the vertex existed and was removed
     */
    public boolean removeVertex(Vertex vertex) {
        if (vertex == null) {
            return false;
        }
        return removeVertexById(vertex.getId());
    }

    /**
     * Removes a vertex by id and also removes all incoming and outgoing edges.
     *
     * Removal is two-phase:
     * 1) collect incident edges from the global edge list
     * 2) remove each edge via {@link #removeEdge(Edge)} to keep endpoint structures in sync
     *
     * @param id id of the vertex to remove
     * @return {@code true} if the vertex existed and was removed
     */
    public boolean removeVertexById(int id) {
        Vertex vertex = vertices.get(id);
        if (vertex == null) {
            return false;
        }

        List<Edge> incidentEdges = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.contains(vertex)) {
                incidentEdges.add(edge);
            }
        }

        for (Edge edge : incidentEdges) {
            removeEdge(edge);
        }

        vertices.remove(id);
        vertex.clearConnections();
        vertex.reset();
        return true;
    }

    /**
     * Adds a directed edge with default weight {@code 1} and color {@code 0}.
     * Missing endpoint vertices are added automatically.
     *
     * @param source source vertex
     * @param destination destination vertex
     * @return the stored edge, or the existing edge if one already connects the same ordered pair
     */
    public Edge addEdge(Vertex source, Vertex destination) {
        return addEdge(source, destination, 1, 0);
    }

    /**
     * Adds a directed edge with a custom weight and default color {@code 0}.
     *
     * @param source source vertex
     * @param destination destination vertex
     * @param weight edge weight
     * @return the stored edge, or the existing edge if one already connects the same ordered pair
     */
    public Edge addEdge(Vertex source, Vertex destination, int weight) {
        return addEdge(source, destination, weight, 0);
    }

    /**
     * Adds a directed edge between two vertices.
     * Missing endpoint vertices are added automatically. This graph keeps at most one
     * edge per ordered pair of vertices.
     *
     * @param source source vertex
     * @param destination destination vertex
     * @param weight edge weight
     * @param color edge color/mark value
     * @return the stored edge, or the existing edge if one already connects the same ordered pair
     * @throws NullPointerException if {@code source} or {@code destination} is {@code null}
     */
    public Edge addEdge(Vertex source, Vertex destination, int weight, int color) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");

        Vertex actualSource = addVertex(source);
        Vertex actualDestination = addVertex(destination);

        Edge existingEdge = getEdge(actualSource, actualDestination);
        if (existingEdge != null) {
            return existingEdge;
        }

        Edge edge = new Edge(actualSource, actualDestination, weight, color);
        edges.add(edge);
        actualSource.addOutgoingEdge(edge);
        return edge;
    }

    /**
     * Adds a default edge between two vertex ids.
     * Missing vertices are created automatically.
     *
     * @param sourceId source vertex id
     * @param destinationId destination vertex id
     * @return the stored edge
     */
    public Edge addEdge(int sourceId, int destinationId) {
        return addEdge(addVertex(sourceId), addVertex(destinationId));
    }

    /**
     * Adds a weighted edge between two vertex ids.
     *
     * @param sourceId source vertex id
     * @param destinationId destination vertex id
     * @param weight edge weight
     * @return the stored edge
     */
    public Edge addEdge(int sourceId, int destinationId, int weight) {
        return addEdge(addVertex(sourceId), addVertex(destinationId), weight);
    }

    /**
     * Adds a fully specified edge between two vertex ids.
     *
     * @param sourceId source vertex id
     * @param destinationId destination vertex id
     * @param weight edge weight
     * @param color edge color/mark value
     * @return the stored edge
     */
    public Edge addEdge(int sourceId, int destinationId, int weight, int color) {
        return addEdge(addVertex(sourceId), addVertex(destinationId), weight, color);
    }

    /**
     * @param edge edge to test
     * @return {@code true} if the edge is currently stored in the graph
     */
    public boolean containsEdge(Edge edge) {
        return edge != null && edges.contains(edge);
    }

    /**
     * @param source source vertex
     * @param destination destination vertex
     * @return {@code true} if an edge exists from source to destination
     */
    public boolean containsEdge(Vertex source, Vertex destination) {
        return getEdge(source, destination) != null;
    }

    /**
     * @param sourceId source vertex id
     * @param destinationId destination vertex id
     * @return {@code true} if an edge exists from source to destination
     */
    public boolean containsEdge(int sourceId, int destinationId) {
        return containsEdge(getVertex(sourceId), getVertex(destinationId));
    }

    /**
     * Returns the edge from one vertex to another.
     *
     * @param source source vertex
     * @param destination destination vertex
     * @return the matching edge, or {@code null} if no such edge exists
     */
    public Edge getEdge(Vertex source, Vertex destination) {
        if (source == null || destination == null) {
            return null;
        }

        Vertex actualSource = vertices.get(source.getId());
        Vertex actualDestination = vertices.get(destination.getId());
        if (actualSource == null || actualDestination == null) {
            return null;
        }

        return actualSource.getEdgeTo(actualDestination);
    }

    /**
     * @param sourceId source vertex id
     * @param destinationId destination vertex id
     * @return the matching edge, or {@code null} if no such edge exists
     */
    public Edge getEdge(int sourceId, int destinationId) {
        return getEdge(getVertex(sourceId), getVertex(destinationId));
    }

    /**
     * Removes an edge from the graph and updates the source vertex.
     *
     * If this was the last edge from source to destination, the destination is also
     * removed from the source-neighbor list to keep adjacency consistent.
     *
     * @param edge edge to remove
     * @return {@code true} if the edge existed and was removed
     */
    public boolean removeEdge(Edge edge) {
        if (edge == null || !edges.remove(edge)) {
            return false;
        }

        Vertex source = vertices.get(edge.getSource().getId());
        if (source != null) {
            source.removeOutgoingEdge(edge);
            if (source.getEdgeTo(edge.getDestination()) == null) {
                source.removeNeighbor(edge.getDestination());
            }
        }

        return true;
    }

    /**
     * @param source source vertex
     * @param destination destination vertex
     * @return {@code true} if the matching edge existed and was removed
     */
    public boolean removeEdge(Vertex source, Vertex destination) {
        Edge edge = getEdge(source, destination);
        return edge != null && removeEdge(edge);
    }

    /**
     * @param sourceId source vertex id
     * @param destinationId destination vertex id
     * @return {@code true} if the matching edge existed and was removed
     */
    public boolean removeEdge(int sourceId, int destinationId) {
        return removeEdge(getVertex(sourceId), getVertex(destinationId));
    }

    /**
     * @param source source vertex
     * @param destination destination vertex
     * @return {@code true} if source has an outgoing edge to destination
     */
    public boolean isAdjacent(Vertex source, Vertex destination) {
        return containsEdge(source, destination);
    }

    /**
     * @param sourceId source vertex id
     * @param destinationId destination vertex id
     * @return {@code true} if source has an outgoing edge to destination
     */
    public boolean isAdjacent(int sourceId, int destinationId) {
        return containsEdge(sourceId, destinationId);
    }

    /**
     * Returns all direct successors of a vertex.
     * The returned list is an unmodifiable snapshot.
     *
     * @param vertex source vertex
     * @return unmodifiable list of successor vertices
     * @throws NullPointerException if {@code vertex} is {@code null}
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public List<Vertex> getSuccessors(Vertex vertex) {
        Vertex actualVertex = requireVertex(vertex);
        return Collections.unmodifiableList(new ArrayList<>(actualVertex.getNeighbors()));
    }

    /**
     * @param vertexId source vertex id
     * @return unmodifiable list of successor vertices
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public List<Vertex> getSuccessors(int vertexId) {
        return getSuccessors(requireVertex(vertexId));
    }

    /**
     * Returns all direct predecessors of a vertex.
     * The returned list is an unmodifiable snapshot.
     *
     * @param vertex target vertex
     * @return unmodifiable list of predecessor vertices
     * @throws NullPointerException if {@code vertex} is {@code null}
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public List<Vertex> getPredecessors(Vertex vertex) {
        Vertex actualVertex = requireVertex(vertex);
        List<Vertex> predecessors = new ArrayList<>();

        for (Edge edge : edges) {
            if (edge.getDestination().equals(actualVertex)) {
                predecessors.add(edge.getSource());
            }
        }

        return Collections.unmodifiableList(predecessors);
    }

    /**
     * @param vertexId target vertex id
     * @return unmodifiable list of predecessor vertices
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public List<Vertex> getPredecessors(int vertexId) {
        return getPredecessors(requireVertex(vertexId));
    }

    /**
     * Returns all outgoing edges of a vertex.
     * The returned list is an unmodifiable snapshot.
     *
     * @param vertex source vertex
     * @return unmodifiable list of outgoing edges
     * @throws NullPointerException if {@code vertex} is {@code null}
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public List<Edge> getOutgoingEdges(Vertex vertex) {
        Vertex actualVertex = requireVertex(vertex);
        return Collections.unmodifiableList(new ArrayList<>(actualVertex.getOutgoingEdges()));
    }

    /**
     * @param vertexId source vertex id
     * @return unmodifiable list of outgoing edges
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public List<Edge> getOutgoingEdges(int vertexId) {
        return getOutgoingEdges(requireVertex(vertexId));
    }

    /**
     * Returns all incoming edges of a vertex.
     * The returned list is an unmodifiable snapshot.
     *
     * @param vertex target vertex
     * @return unmodifiable list of incoming edges
     * @throws NullPointerException if {@code vertex} is {@code null}
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public List<Edge> getIncomingEdges(Vertex vertex) {
        Vertex actualVertex = requireVertex(vertex);
        List<Edge> incomingEdges = new ArrayList<>();

        for (Edge edge : edges) {
            if (edge.getDestination().equals(actualVertex)) {
                incomingEdges.add(edge);
            }
        }

        return Collections.unmodifiableList(incomingEdges);
    }

    /**
     * @param vertexId target vertex id
     * @return unmodifiable list of incoming edges
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public List<Edge> getIncomingEdges(int vertexId) {
        return getIncomingEdges(requireVertex(vertexId));
    }

    /**
     * @param vertex vertex to query
     * @return number of outgoing edges from that vertex
     * @throws NullPointerException if {@code vertex} is {@code null}
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public int getOutDegree(Vertex vertex) {
        return requireVertex(vertex).getOutDegree();
    }

    /**
     * @param vertexId vertex id to query
     * @return number of outgoing edges from that vertex
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public int getOutDegree(int vertexId) {
        return getOutDegree(requireVertex(vertexId));
    }

    /**
     * @param vertex vertex to query
     * @return number of incoming edges into that vertex
     * @throws NullPointerException if {@code vertex} is {@code null}
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public int getInDegree(Vertex vertex) {
        return getIncomingEdges(vertex).size();
    }

    /**
     * @param vertexId vertex id to query
     * @return number of incoming edges into that vertex
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    public int getInDegree(int vertexId) {
        return getInDegree(requireVertex(vertexId));
    }

    /** Removes all edges while keeping all vertices in the graph. */
    public void clearEdges() {
        edges.clear();
        for (Vertex vertex : vertices.values()) {
            vertex.clearConnections();
        }
    }

    /** Removes all vertices and edges from the graph. */
    public void clear() {
        clearEdges();
        vertices.clear();
    }

    /** Resets traversal-related metadata on every stored vertex. */
    public void resetTraversalState() {
        for (Vertex vertex : vertices.values()) {
            vertex.resetTraversalState();
        }
    }

    /** Resets shortest-path-related metadata on every stored vertex. */
    public void resetShortestPathState() {
        for (Vertex vertex : vertices.values()) {
            vertex.resetShortestPathState();
        }
    }

    /** Resets all temporary vertex state while keeping the graph structure. */
    public void resetVertexState() {
        for (Vertex vertex : vertices.values()) {
            vertex.reset();
        }
    }

    /**
     * Returns the stored graph vertex that matches the given vertex id.
     *
     * @param vertex vertex whose id should exist in the graph
     * @return stored vertex instance
     * @throws NullPointerException if {@code vertex} is {@code null}
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    private Vertex requireVertex(Vertex vertex) {
        Objects.requireNonNull(vertex, "vertex cannot be null");
        Vertex actualVertex = vertices.get(vertex.getId());
        if (actualVertex == null) {
            throw new IllegalArgumentException("vertex is not in the graph");
        }
        return actualVertex;
    }

    /**
     * Returns the stored graph vertex for a required id.
     *
     * @param vertexId required vertex id
     * @return stored vertex instance
     * @throws IllegalArgumentException if the vertex is not stored in the graph
     */
    private Vertex requireVertex(int vertexId) {
        Vertex vertex = vertices.get(vertexId);
        if (vertex == null) {
            throw new IllegalArgumentException("vertex with id " + vertexId + " is not in the graph");
        }
        return vertex;
    }

    /** @return readable string representation of this graph */
    @Override
    public String toString() {
        return "DirectedGraph{" +
                "vertexCount=" + vertexCount() +
                ", edgeCount=" + edgeCount() +
                ", vertices=" + vertices.values() +
                ", edges=" + edges +
                '}';
    }
}
