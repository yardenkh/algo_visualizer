package ui.templates;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable premade graph template definition.
 *
 * A template contains normalized positions and lightweight edge records.
 * The live editor turns this into real vertices/edges via
 * {@link ui.GraphEditorController#loadGraphTemplate(ui.templates.GraphTemplate)}.
 */
public final class GraphTemplate {
    private final String name;
    private final GraphTemplateType type;
    private final Map<Integer, Point2D.Double> positions;
    private final List<TemplateEdge> edges;

    /** Creates one immutable template record. */
    public GraphTemplate(String name, GraphTemplateType type, Map<Integer, Point2D.Double> positions, List<TemplateEdge> edges) {
        this.name = name;
        this.type = type;
        this.positions = Collections.unmodifiableMap(new LinkedHashMap<>(positions));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
    }

    /** @return display name shown in the template library. */
    public String getName() {
        return name;
    }

    /** @return category used by the premade-graph filter UI. */
    public GraphTemplateType getType() {
        return type;
    }

    /** @return normalized vertex positions in [0,1]-style template space. */
    public Map<Integer, Point2D.Double> getPositions() {
        return positions;
    }

    /** @return immutable edge list for the template. */
    public List<TemplateEdge> getEdges() {
        return edges;
    }

    /** Lightweight edge record used inside template definitions. */
    public static final class TemplateEdge {
        private final int sourceId;
        private final int destinationId;
        private final int weight;
        private final int color;

        /** Creates one immutable template-edge record. */
        public TemplateEdge(int sourceId, int destinationId, int weight, int color) {
            this.sourceId = sourceId;
            this.destinationId = destinationId;
            this.weight = weight;
            this.color = color;
        }

        /** @return source vertex id inside template space. */
        public int getSourceId() {
            return sourceId;
        }

        /** @return destination vertex id inside template space. */
        public int getDestinationId() {
            return destinationId;
        }

        /** @return edge weight to apply when loading the template. */
        public int getWeight() {
            return weight;
        }

        /** @return stored edge color to apply when loading the template. */
        public int getColor() {
            return color;
        }
    }
}
