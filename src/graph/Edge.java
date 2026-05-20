package graph;

import java.util.Objects;

/**
 * Represents a directed edge between two vertices.
 *
 * An edge has:
 * - Source and destination vertices
 * - Weight (cost/distance)
 * - Optional color/mark value for algorithm visualization
 *
 * Endpoint references are immutable after construction; metadata fields
 * (weight/color) are mutable through setters.
 *
 * Related classes: {@link graph.Vertex}, {@link graph.DirectedGraph}.
 * Roadmap: see {@code project roadmap.md}.
 */
public final class Edge {
    /** Vertex where the edge starts. */
    private final Vertex source;
    /** Vertex where the edge ends. */
    private final Vertex destination;
    /** Numeric weight associated with the edge. */
    private int weight;
    /** Optional color/mark value associated with the edge. */
    private int color;

    /**
     * Creates an edge with default weight {@code 1} and color {@code 0}.
     *
     * @param source source vertex
     * @param destination destination vertex
     */
    public Edge(Vertex source, Vertex destination) {
        this(source, destination, 1, 0);
    }

    /**
     * Creates an edge with a custom weight and default color {@code 0}.
     *
     * @param source source vertex
     * @param destination destination vertex
     * @param weight weight value
     */
    public Edge(Vertex source, Vertex destination, int weight) {
        this(source, destination, weight, 0);
    }

    /**
     * Creates an edge with fully specified metadata.
     *
     * @param source source vertex
     * @param destination destination vertex
     * @param weight weight value
     * @param color color/mark value
     * @throws NullPointerException if {@code source} or {@code destination} is {@code null}
     */
    public Edge(Vertex source, Vertex destination, int weight, int color) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.destination = Objects.requireNonNull(destination, "destination cannot be null");
        this.weight = weight;
        this.color = color;
    }

    /** @return the source vertex of this edge */
    public Vertex getSource() {
        return source;
    }

    /** @return the destination vertex of this edge */
    public Vertex getDestination() {
        return destination;
    }

    /** @return the weight value of this edge */
    public int getWeight() {
        return weight;
    }

    /**
     * Updates the edge weight.
     *
     * @param weight new weight value
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /** @return the color/mark value of this edge */
    public int getColor() {
        return color;
    }

    /**
     * Updates the edge color/mark value.
     *
     * @param color new color value
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Checks whether the edge starts and ends at the same vertex.
     *
     * @return {@code true} if this edge is a self-loop
     */
    public boolean isLoop() {
        return source.equals(destination);
    }

    /**
     * Checks whether this edge goes from one specific vertex to another.
     *
     * @param source expected source vertex
     * @param destination expected destination vertex
     * @return {@code true} if both endpoints match
     */
    public boolean connects(Vertex source, Vertex destination) {
        return this.source.equals(source) && this.destination.equals(destination);
    }

    /**
     * Checks whether a vertex is one of the endpoints of this edge.
     *
     * @param vertex vertex to test
     * @return {@code true} if the vertex is either the source or destination
     */
    public boolean contains(Vertex vertex) {
        return source.equals(vertex) || destination.equals(vertex);
    }

    /**
     * Returns the opposite endpoint of a given endpoint.
     *
     * @param vertex one endpoint of this edge
     * @return the other endpoint, or {@code null} if the given vertex is not part of the edge
     */
    public Vertex getOpposite(Vertex vertex) {
        if (source.equals(vertex)) {
            return destination;
        }
        if (destination.equals(vertex)) {
            return source;
        }
        return null;
    }

    /**
     * Compares edges by endpoints and stored metadata.
     *
     * @param o object to compare with
     * @return {@code true} if both edges have the same endpoints, weight, and color
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Edge edge = (Edge) o;
        return weight == edge.weight
                && color == edge.color
                && source.equals(edge.source)
                && destination.equals(edge.destination);
    }

    /** @return hash code derived from endpoints and metadata */
    @Override
    public int hashCode() {
        return Objects.hash(source, destination, weight, color);
    }

    /** @return readable string representation of this edge */
    @Override
    public String toString() {
        return "Edge{" +
                "source=" + source +
                ", destination=" + destination +
                ", weight=" + weight +
                ", color=" + color +
                '}';
    }
}
