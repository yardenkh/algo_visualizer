package algorithm.maxflow;

import algorithm.AlgorithmPalette;
import algorithm.AlgorithmRequest;
import algorithm.AlgorithmStep;
import algorithm.EdgeKey;
import algorithm.LevelGraphSnapshot;
import algorithm.ResidualGraphSnapshot;
import graph.Edge;
import graph.Vertex;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared helpers for maximum-flow timeline generation. */
final class MaxFlowSupport {
    private MaxFlowSupport() {
    }

    static AlgorithmStep step(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[][] capacity,
            int[][] flow,
            int activeLine,
            String title,
            String message,
            Vertex activeVertex,
            Edge activeEdge,
            Map<Integer, Integer> highlightVertices,
            LevelGraphSnapshot levelGraphSnapshot
    ) {
        return step(request, vertices, capacity, flow, activeLine, title, message,
                activeVertex, activeEdge, highlightVertices, levelGraphSnapshot, List.of());
    }

    static AlgorithmStep step(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[][] capacity,
            int[][] flow,
            int activeLine,
            String title,
            String message,
            Vertex activeVertex,
            Edge activeEdge,
            Map<Integer, Integer> highlightVertices,
            LevelGraphSnapshot levelGraphSnapshot,
            List<EdgeKey> residualHighlightedEdges
    ) {
        return buildStep(request, vertices, capacity, flow, activeLine, title, message,
                activeVertex, activeEdge, highlightVertices, levelGraphSnapshot, residualHighlightedEdges, null);
    }

    /** Same as {@link #step} but tags this step as a callable sub-routine anchor. */
    static AlgorithmStep stepWithSubroutine(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[][] capacity,
            int[][] flow,
            int activeLine,
            String title,
            String message,
            Vertex activeVertex,
            Edge activeEdge,
            Map<Integer, Integer> highlightVertices,
            LevelGraphSnapshot levelGraphSnapshot,
            List<EdgeKey> residualHighlightedEdges,
            String callableSubroutineId
    ) {
        return buildStep(request, vertices, capacity, flow, activeLine, title, message,
                activeVertex, activeEdge, highlightVertices, levelGraphSnapshot, residualHighlightedEdges, callableSubroutineId);
    }

    private static AlgorithmStep buildStep(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[][] capacity,
            int[][] flow,
            int activeLine,
            String title,
            String message,
            Vertex activeVertex,
            Edge activeEdge,
            Map<Integer, Integer> highlightVertices,
            LevelGraphSnapshot levelGraphSnapshot,
            List<EdgeKey> residualHighlightedEdges,
            String callableSubroutineId
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> vertexAnnotations = new LinkedHashMap<>();
        Map<EdgeKey, String> edgeAnnotations = new LinkedHashMap<>();

        int sourceId = request.getSourceVertex() == null ? -1 : request.getSourceVertex().getId();
        int sinkId = request.getSinkVertex() == null ? -1 : request.getSinkVertex().getId();

        for (Vertex vertex : vertices) {
            int id = vertex.getId();
            vertexColors.put(id, AlgorithmPalette.SCOPE_NEUTRAL);
            if (id == sourceId) {
                vertexAnnotations.put(id, "s");
            } else if (id == sinkId) {
                vertexAnnotations.put(id, "t");
            } else {
                vertexAnnotations.put(id, "");
            }
        }

        if (highlightVertices != null) {
            for (Map.Entry<Integer, Integer> e : highlightVertices.entrySet()) {
                vertexColors.put(e.getKey(), e.getValue());
            }
        }

        if (activeVertex != null) {
            vertexColors.put(activeVertex.getId(), AlgorithmPalette.ACTIVE);
        }

        for (Edge edge : request.getScopeEdges()) {
            int u = indexOf(vertices, edge.getSource().getId());
            int v = indexOf(vertices, edge.getDestination().getId());
            int f = flow[u][v];
            int c = capacity[u][v];
            EdgeKey key = EdgeKey.from(edge);
            edgeAnnotations.put(key, f + " / " + c);
        }

        if (activeEdge != null) {
            edgeColors.put(EdgeKey.from(activeEdge), AlgorithmPalette.ACTIVE_EDGE);
        }

        ResidualGraphSnapshot residual = residualSnapshot(
                request,
                vertices,
                capacity,
                flow,
                request.getSourceVertex(),
                request.getSinkVertex(),
                residualHighlightedEdges
        );

        return new AlgorithmStep(
                title,
                message,
                activeLine,
                vertexColors,
                edgeColors,
                vertexAnnotations,
                edgeAnnotations,
                null,
                null,
                residual,
                levelGraphSnapshot,
                callableSubroutineId
        );
    }

    static ResidualGraphSnapshot residualSnapshot(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[][] capacity,
            int[][] flow,
            Vertex source,
            Vertex sink,
            List<EdgeKey> highlightedEdges
    ) {
        List<Integer> ids = new ArrayList<>();
        Map<Integer, String> labels = new LinkedHashMap<>();
        Map<Integer, Point2D.Double> positions = new LinkedHashMap<>();
        for (Vertex v : vertices) {
            ids.add(v.getId());
            if (source != null && v.getId() == source.getId()) {
                labels.put(v.getId(), "s");
            } else if (sink != null && v.getId() == sink.getId()) {
                labels.put(v.getId(), "t");
            } else {
                labels.put(v.getId(), v.getName());
            }
            Point2D.Double p = request.getVertexPositions().get(v.getId());
            if (p != null) {
                positions.put(v.getId(), new Point2D.Double(p.x, p.y));
            }
        }

        List<ResidualGraphSnapshot.ResidualEdge> residualEdges = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            for (int j = 0; j < vertices.size(); j++) {
                int residual = capacity[i][j] - flow[i][j];
                if (residual > 0) {
                    residualEdges.add(new ResidualGraphSnapshot.ResidualEdge(vertices.get(i).getId(), vertices.get(j).getId(), residual));
                }
            }
        }
        return new ResidualGraphSnapshot(ids, labels, positions, residualEdges, highlightedEdges == null ? List.of() : highlightedEdges);
    }

    static LevelGraphSnapshot levelSnapshot(
            AlgorithmRequest request,
            List<Vertex> vertices,
            int[] level,
            int[][] capacity,
            int[][] flow,
            Vertex source,
            Vertex sink
    ) {
        List<Integer> ids = new ArrayList<>();
        Map<Integer, String> labels = new LinkedHashMap<>();
        Map<Integer, Point2D.Double> positions = new LinkedHashMap<>();
        Map<Integer, Integer> levels = new LinkedHashMap<>();
        List<EdgeKey> levelEdges = new ArrayList<>();

        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            ids.add(v.getId());
            if (source != null && v.getId() == source.getId()) {
                labels.put(v.getId(), "s");
            } else if (sink != null && v.getId() == sink.getId()) {
                labels.put(v.getId(), "t");
            } else {
                labels.put(v.getId(), v.getName());
            }
            Point2D.Double p = request.getVertexPositions().get(v.getId());
            if (p != null) {
                positions.put(v.getId(), new Point2D.Double(p.x, p.y));
            }
            if (level[i] >= 0) {
                levels.put(v.getId(), level[i]);
            }
        }

        for (int i = 0; i < vertices.size(); i++) {
            if (level[i] < 0) {
                continue;
            }
            for (int j = 0; j < vertices.size(); j++) {
                int residual = capacity[i][j] - flow[i][j];
                if (residual > 0 && level[j] == level[i] + 1) {
                    levelEdges.add(new EdgeKey(vertices.get(i).getId(), vertices.get(j).getId()));
                }
            }
        }

        return new LevelGraphSnapshot(ids, labels, positions, levels, levelEdges);
    }

    static int indexOf(List<Vertex> vertices, int vertexId) {
        for (int i = 0; i < vertices.size(); i++) {
            if (vertices.get(i).getId() == vertexId) {
                return i;
            }
        }
        return -1;
    }

    static int[][] capacities(AlgorithmRequest request, List<Vertex> vertices) {
        int n = vertices.size();
        int[][] capacity = new int[n][n];
        for (Edge edge : request.getScopeEdges()) {
            int u = indexOf(vertices, edge.getSource().getId());
            int v = indexOf(vertices, edge.getDestination().getId());
            if (u >= 0 && v >= 0) {
                capacity[u][v] = edge.getWeight();
            }
        }
        return capacity;
    }
}

