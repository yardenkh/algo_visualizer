package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single vertex in a graph.
 *
 * Each vertex has:
 * - A unique integer identifier (0-indexed internally, displayed as 1-indexed)
 * - A human-readable name (e.g., "V1", "V2")
 * - Optional color/mark value for algorithm state
 * - Adjacency list and outgoing edges
 * - Temporary state fields for algorithm execution (distance, discovery time, etc.)
 *
 * This vertex model is reusable across different algorithms without modification;
 * algorithms read/write to the temporary state fields as needed.
 *
 * Related classes: {@link graph.DirectedGraph}, {@link graph.Edge}.
 * Roadmap: see {@code project roadmap.md}.
 */
public class Vertex {
    /** Human-readable vertex name. */
    private String name;
    /** Optional color/mark value used by external code. */
    private int color;
    /** Immutable unique identifier of the vertex. */
    private final int id;
    /** Adjacent vertices reachable by outgoing edges. */
    private final List<Vertex> neighbors;
    /** Outgoing directed edges that start at this vertex. */
    private final List<Edge> outgoingEdges;
    /** Generic traversal flag. */
    private boolean visited;
    /** Optional per-vertex distance cache used by external algorithms. */
    private final Map<Vertex, Integer> distances;
    /** Parent or predecessor vertex used by external algorithms. */
    private Vertex parent;
    /** Single distance value used by shortest-path or BFS-style algorithms. */
    private int distance;
    /** Discovery time used by traversal-based algorithms. */
    private int discoveryTime;
    /** Finish time used by traversal-based algorithms. */
    private int finishTime;
    /** Connected-component or group marker. */
    private int component;

    /**
     * Creates a vertex with a generated name.
     * The generated name is in the form {@code V<id+1>} (1-indexed for pseudocode compatibility).
     *
     * @param id vertex identifier
     */
    public Vertex(int id) {
        this("V" + (id + 1), id);
    }

    /**
     * Creates a vertex and initializes its internal collections and state.
     *
     * @param name display name of the vertex
     * @param id unique identifier of the vertex
     */
    public Vertex(String name, int id) {
        this.name = name;
        this.id = id;
        this.neighbors = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.distances = new HashMap<>();
        reset();
    }

    /** @return the unique identifier of this vertex */
    public int getId() {
        return id;
    }

    /** @return the display name of this vertex */
    public String getName() {
        return name;
    }

    /**
     * @param name new display name for this vertex
     */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the current color/mark value */
    public int getColor() {
        return color;
    }

    /**
     * @param color new color/mark value
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Returns the adjacency list of this vertex.
     * The returned list is the internal mutable list used by the vertex.
     *
     * @return list of adjacent vertices
     */
    public List<Vertex> getNeighbors() {
        return neighbors;
    }

    /**
     * Adds a neighbor if it is not already present.
     *
     * @param neighbor vertex to add
     * @return {@code true} if the neighbor was added, otherwise {@code false}
     * @throws NullPointerException if {@code neighbor} is {@code null}
     */
    public boolean addNeighbor(Vertex neighbor) {
        Objects.requireNonNull(neighbor, "neighbor cannot be null");
        if (neighbors.contains(neighbor)) {
            return false;
        }
        return neighbors.add(neighbor);
    }

    /**
     * @param neighbor vertex to remove
     * @return {@code true} if the neighbor existed and was removed
     */
    public boolean removeNeighbor(Vertex neighbor) {
        return neighbors.remove(neighbor);
    }

    /**
     * @param neighbor vertex to test
     * @return {@code true} if the vertex is currently a neighbor
     */
    public boolean hasNeighbor(Vertex neighbor) {
        return neighbors.contains(neighbor);
    }

    /** @return number of outgoing edges from this vertex */
    public int getOutDegree() {
        return outgoingEdges.size();
    }

    /**
     * Returns the outgoing edges of this vertex.
     * The returned list is the internal mutable list used by the vertex.
     *
     * @return outgoing edge list
     */
    public List<Edge> getOutgoingEdges() {
        return outgoingEdges;
    }

    /**
     * Finds the outgoing edge from this vertex to the given destination.
     *
     * @param destination destination vertex to search for
     * @return the matching edge, or {@code null} if none exists
     */
    public Edge getEdgeTo(Vertex destination) {
        for (Edge edge : outgoingEdges) {
            if (edge.getDestination().equals(destination)) {
                return edge;
            }
        }
        return null;
    }

    /**
     * Adds an existing outgoing edge to this vertex.
     * Also keeps the neighbor list synchronized with the destination vertex.
     *
     * This method is used by {@link graph.DirectedGraph} when edges are created,
     * so adjacency and outgoing-edge views remain consistent.
     *
     * @param edge edge to attach
     * @throws NullPointerException if {@code edge} is {@code null}
     * @throws IllegalArgumentException if the edge source is not this vertex
     */
    public void addOutgoingEdge(Edge edge) {
        Objects.requireNonNull(edge, "edge cannot be null");
        if (!this.equals(edge.getSource())) {
            throw new IllegalArgumentException("edge source must be this vertex");
        }
        outgoingEdges.add(edge);
        addNeighbor(edge.getDestination());
    }

    /**
     * @param edge edge to remove
     * @return {@code true} if the edge existed and was removed
     */
    public boolean removeOutgoingEdge(Edge edge) {
        return outgoingEdges.remove(edge);
    }

    /**
     * Removes all outgoing edges from this vertex to one destination.
     *
     * @param destination destination vertex to disconnect from
     * @return {@code true} if at least one outgoing edge was removed
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public boolean disconnectFrom(Vertex destination) {
        Objects.requireNonNull(destination, "destination cannot be null");
        boolean removed = outgoingEdges.removeIf(edge -> edge.getDestination().equals(destination));
        neighbors.remove(destination);
        return removed;
    }

    /**
     * Clears structural connections of this vertex.
     * Does not reset traversal or shortest-path metadata.
     */
    public void clearConnections() {
        neighbors.clear();
        outgoingEdges.clear();
    }

    /**
     * Creates a new outgoing edge from this vertex to a destination.
     *
     * @param destination edge destination
     * @param weight edge weight value
     * @param color edge color/mark value
     * @return the created edge
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public Edge connectTo(Vertex destination, int weight, int color) {
        Objects.requireNonNull(destination, "destination cannot be null");
        Edge edge = new Edge(this, destination, weight, color);
        addOutgoingEdge(edge);
        return edge;
    }

    /** @return {@code true} if this vertex is marked as visited */
    public boolean isVisited() {
        return visited;
    }

    /**
     * @param visited new visited flag value
     */
    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    /** @return the current parent/predecessor vertex, or {@code null} if none is set */
    public Vertex getParent() {
        return parent;
    }

    /**
     * @param parent new parent/predecessor vertex
     */
    public void setParent(Vertex parent) {
        this.parent = parent;
    }

    /** @return the single distance value stored on this vertex */
    public int getDistance() {
        return distance;
    }

    /**
     * @param distance new single distance value
     */
    public void setDistance(int distance) {
        this.distance = distance;
    }

    /** @return discovery time metadata value */
    public int getDiscoveryTime() {
        return discoveryTime;
    }

    /**
     * @param discoveryTime new discovery time value
     */
    public void setDiscoveryTime(int discoveryTime) {
        this.discoveryTime = discoveryTime;
    }

    /** @return finish time metadata value */
    public int getFinishTime() {
        return finishTime;
    }

    /**
     * @param finishTime new finish time value
     */
    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }

    /** @return component/group marker value */
    public int getComponent() {
        return component;
    }

    /**
     * @param component new component/group marker value
     */
    public void setComponent(int component) {
        this.component = component;
    }

    /**
     * Returns the stored distance map.
     * The returned map is the internal mutable map used by the vertex.
     *
     * @return map of distances keyed by vertex
     */
    public Map<Vertex, Integer> getDistances() {
        return distances;
    }

    /**
     * Stores a distance value associated with another vertex.
     *
     * @param vertex target vertex key
     * @param distance distance value to store
     * @throws NullPointerException if {@code vertex} is {@code null}
     */
    public void setDistanceTo(Vertex vertex, int distance) {
        Objects.requireNonNull(vertex, "vertex cannot be null");
        distances.put(vertex, distance);
    }

    /**
     * @param vertex target vertex key
     * @return stored distance for that vertex, or {@code null} if no value exists
     */
    public Integer getDistanceTo(Vertex vertex) {
        return distances.get(vertex);
    }

    /**
     * @param vertex target vertex key
     * @return the removed distance value, or {@code null} if none existed
     */
    public Integer removeDistanceTo(Vertex vertex) {
        return distances.remove(vertex);
    }

    /** Removes all stored entries from the distance map. */
    public void clearDistances() {
        distances.clear();
    }

    /**
     * Resets traversal-related metadata.
     * Clears visited state, parent, discovery/finish times, and component marker.
     */
    public void resetTraversalState() {
        visited = false;
        parent = null;
        discoveryTime = -1;
        finishTime = -1;
        component = -1;
    }

    /**
     * Resets shortest-path-related metadata.
     * Sets the single distance to {@link Integer#MAX_VALUE}, clears the parent,
     * and clears the distance map.
     */
    public void resetShortestPathState() {
        distance = Integer.MAX_VALUE;
        parent = null;
        clearDistances();
    }

    /**
     * Resets all temporary algorithm state on this vertex.
     * Structural connections are kept intact so the graph topology is unchanged.
     */
    public void reset() {
        color = 0;
        resetTraversalState();
        resetShortestPathState();
    }

    /**
     * Compares vertices by their immutable identifier.
     *
     * @param o object to compare with
     * @return {@code true} if both vertices have the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Vertex vertex = (Vertex) o;
        return id == vertex.id;
    }

    /** @return hash code derived from the vertex id */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /** @return readable string representation of this vertex */
    @Override
    public String toString() {
        return "Vertex{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
