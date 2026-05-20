package algorithm;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dinic level-graph payload for max-flow visualization steps. */
public final class LevelGraphSnapshot {
    private final List<Integer> vertexIds;
    private final Map<Integer, String> vertexLabels;
    private final Map<Integer, Point2D.Double> vertexPositions;
    private final Map<Integer, Integer> levels;
    private final List<EdgeKey> levelEdges;

    public LevelGraphSnapshot(
            List<Integer> vertexIds,
            Map<Integer, String> vertexLabels,
            Map<Integer, Point2D.Double> vertexPositions,
            Map<Integer, Integer> levels,
            List<EdgeKey> levelEdges
    ) {
        this.vertexIds = Collections.unmodifiableList(new ArrayList<>(vertexIds));
        this.vertexLabels = Collections.unmodifiableMap(new LinkedHashMap<>(vertexLabels));

        Map<Integer, Point2D.Double> frozenPositions = new LinkedHashMap<>();
        for (Map.Entry<Integer, Point2D.Double> entry : vertexPositions.entrySet()) {
            Point2D.Double p = entry.getValue();
            frozenPositions.put(entry.getKey(), new Point2D.Double(p.x, p.y));
        }
        this.vertexPositions = Collections.unmodifiableMap(frozenPositions);

        this.levels = Collections.unmodifiableMap(new LinkedHashMap<>(levels));
        this.levelEdges = Collections.unmodifiableList(new ArrayList<>(levelEdges));
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

    public Map<Integer, Integer> getLevels() {
        return levels;
    }

    public List<EdgeKey> getLevelEdges() {
        return levelEdges;
    }
}
