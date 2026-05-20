package algorithm;

import graph.Edge;

import java.util.Objects;

/**
 * Immutable identifier for an edge by endpoint ids.
 * Used as a stable map key inside {@link algorithm.AlgorithmStep} edge-color maps.
 */
public final class EdgeKey {
    private final int sourceId;
    private final int destinationId;

    /** Creates a key directly from source/destination ids. */
    public EdgeKey(int sourceId, int destinationId) {
        this.sourceId = sourceId;
        this.destinationId = destinationId;
    }

    /** Convenience factory that extracts endpoint ids from a live edge object. */
    public static EdgeKey from(Edge edge) {
        return new EdgeKey(edge.getSource().getId(), edge.getDestination().getId());
    }

    /** @return source vertex id. */
    public int getSourceId() {
        return sourceId;
    }

    /** @return destination vertex id. */
    public int getDestinationId() {
        return destinationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EdgeKey edgeKey)) {
            return false;
        }
        return sourceId == edgeKey.sourceId && destinationId == edgeKey.destinationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, destinationId);
    }
}
