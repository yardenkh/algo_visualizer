package algorithm;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Residual graph payload for max-flow visualization steps. */
public final class ResidualGraphSnapshot {
    private final List<Integer> vertexIds;
    private final Map<Integer, String> vertexLabels;
    private final Map<Integer, Point2D.Double> vertexPositions;
    private final List<ResidualEdge> edges;
    private final List<EdgeKey> highlightedEdges;

    public ResidualGraphSnapshot(
            List<Integer> vertexIds,
            Map<Integer, String> vertexLabels,
            Map<Integer, Point2D.Double> vertexPositions,
            List<ResidualEdge> edges,
            List<EdgeKey> highlightedEdges
    ) {
        this.vertexIds = Collections.unmodifiableList(new ArrayList<>(vertexIds));
        this.vertexLabels = Collections.unmodifiableMap(new LinkedHashMap<>(vertexLabels));

        Map<Integer, Point2D.Double> frozenPositions = new LinkedHashMap<>();
        for (Map.Entry<Integer, Point2D.Double> entry : vertexPositions.entrySet()) {
            Point2D.Double p = entry.getValue();
            frozenPositions.put(entry.getKey(), new Point2D.Double(p.x, p.y));
        }
        this.vertexPositions = Collections.unmodifiableMap(frozenPositions);

        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
        this.highlightedEdges = Collections.unmodifiableList(new ArrayList<>(highlightedEdges));
    }

    public List<Integer> getVertexIds() {
        return vertexIds;
    }

    public Map<Integer, String> getVertexLabels() {
        return vertexLabels;
    }

    public Map<Integer, Point2D.Double> getVertexPositions() {
        return vertexPositions;
    }

    public List<ResidualEdge> getEdges() {
        return edges;
    }

    public List<EdgeKey> getHighlightedEdges() {
        return highlightedEdges;
    }

    public static final class ResidualEdge {
        private final int sourceId;
        private final int destinationId;
        private final int capacity;

        public ResidualEdge(int sourceId, int destinationId, int capacity) {
            this.sourceId = sourceId;
            this.destinationId = destinationId;
            this.capacity = capacity;
        }

        public int getSourceId() {
            return sourceId;
        }

        public int getDestinationId() {
            return destinationId;
        }

        public int getCapacity() {
            return capacity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ResidualEdge edge)) {
                return false;
            }
            return sourceId == edge.sourceId && destinationId == edge.destinationId && capacity == edge.capacity;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(sourceId, destinationId, capacity);
        }
    }
}
