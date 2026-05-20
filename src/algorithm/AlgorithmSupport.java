package algorithm;

import graph.Edge;
import graph.Vertex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared helper methods for algorithm implementations. */
public final class AlgorithmSupport {
    private AlgorithmSupport() {
    }

    public static AlgorithmStep neutralStep(AlgorithmRequest request, int activeLine, String title, String message) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> annotations = new LinkedHashMap<>();
        for (Vertex vertex : request.getScopeVertices()) {
            vertexColors.put(vertex.getId(), AlgorithmPalette.SCOPE_NEUTRAL);
            annotations.put(vertex.getId(), "");
        }
        return new AlgorithmStep(title, message, activeLine, vertexColors, edgeColors, annotations);
    }

    public static boolean hasNegativeCycle(AlgorithmRequest request) {
        List<Vertex> vertices = request.getScopeVertices();
        if (vertices.isEmpty()) {
            return false;
        }
        Map<Integer, Integer> dist = new HashMap<>();
        for (Vertex vertex : vertices) {
            dist.put(vertex.getId(), 0);
        }
        int n = vertices.size();
        for (int i = 0; i < n - 1; i++) {
            for (Edge edge : request.getScopeEdges()) {
                int u = edge.getSource().getId();
                int v = edge.getDestination().getId();
                int nd = dist.get(u) + edge.getWeight();
                if (nd < dist.get(v)) {
                    dist.put(v, nd);
                }
            }
        }
        for (Edge edge : request.getScopeEdges()) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            if (dist.get(u) + edge.getWeight() < dist.get(v)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> findCycles(AlgorithmRequest request, int limit) {
        Map<Integer, List<Integer>> adj = new LinkedHashMap<>();
        for (Vertex v : request.getScopeVertices()) {
            adj.put(v.getId(), new ArrayList<>());
        }
        for (Edge edge : request.getScopeEdges()) {
            adj.get(edge.getSource().getId()).add(edge.getDestination().getId());
        }

        List<String> cycles = new ArrayList<>();
        Set<Integer> globallyVisited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> inStack = new HashSet<>();

        for (Vertex v : request.getScopeVertices()) {
            if (cycles.size() >= limit) {
                break;
            }
            dfsCycle(v.getId(), adj, globallyVisited, stack, inStack, cycles, limit);
        }
        return cycles;
    }

    private static void dfsCycle(
            int u,
            Map<Integer, List<Integer>> adj,
            Set<Integer> globallyVisited,
            Deque<Integer> stack,
            Set<Integer> inStack,
            List<String> cycles,
            int limit
    ) {
        globallyVisited.add(u);
        stack.push(u);
        inStack.add(u);

        for (int v : adj.getOrDefault(u, Collections.emptyList())) {
            if (cycles.size() >= limit) {
                break;
            }
            if (!globallyVisited.contains(v)) {
                dfsCycle(v, adj, globallyVisited, stack, inStack, cycles, limit);
            } else if (inStack.contains(v)) {
                List<Integer> cycle = new ArrayList<>();
                for (int x : stack) {
                    cycle.add(x);
                    if (x == v) {
                        break;
                    }
                }
                Collections.reverse(cycle);
                cycle.add(v);
                cycles.add(formatPath(cycle));
            }
        }

        stack.pop();
        inStack.remove(u);
    }

    private static String formatPath(List<Integer> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append("V").append(path.get(i));
        }
        return sb.toString();
    }

    public static List<String> findBidirectionalEdgeWarnings(AlgorithmRequest request) {
        Set<String> edges = new LinkedHashSet<>();
        for (Edge edge : request.getScopeEdges()) {
            edges.add(edge.getSource().getId() + ":" + edge.getDestination().getId());
        }
        List<String> warnings = new ArrayList<>();
        for (Edge edge : request.getScopeEdges()) {
            String reverse = edge.getDestination().getId() + ":" + edge.getSource().getId();
            if (edges.contains(reverse) && edge.getSource().getId() < edge.getDestination().getId()) {
                warnings.add("(" + edge.getSource().getName() + " <-> " + edge.getDestination().getName() + ")");
            }
        }
        return warnings;
    }

    public static List<Vertex> topologicalOrder(AlgorithmRequest request) {
        Map<Integer, Integer> indegree = new LinkedHashMap<>();
        Map<Integer, Vertex> byId = new LinkedHashMap<>();
        for (Vertex v : request.getScopeVertices()) {
            indegree.put(v.getId(), 0);
            byId.put(v.getId(), v);
        }
        for (Edge edge : request.getScopeEdges()) {
            indegree.put(edge.getDestination().getId(), indegree.get(edge.getDestination().getId()) + 1);
        }

        ArrayDeque<Vertex> q = new ArrayDeque<>();
        for (Vertex v : request.getScopeVertices()) {
            if (indegree.get(v.getId()) == 0) {
                q.add(v);
            }
        }

        List<Vertex> order = new ArrayList<>();
        while (!q.isEmpty()) {
            Vertex u = q.removeFirst();
            order.add(u);
            for (Edge edge : request.getOutgoingEdges(u)) {
                int vId = edge.getDestination().getId();
                indegree.put(vId, indegree.get(vId) - 1);
                if (indegree.get(vId) == 0) {
                    q.add(byId.get(vId));
                }
            }
        }
        return order;
    }

    public static AlgorithmStep distanceStep(
            AlgorithmRequest request,
            Map<Integer, Integer> dist,
            int infValue,
            int activeLine,
            String title,
            String message,
            Edge activeEdge,
            Vertex current
    ) {
        return buildDistanceStep(request, dist, infValue, activeLine, title, message, activeEdge, current, null);
    }

    /**
     * Same as {@link #distanceStep} but marks this step as a callable sub-routine anchor.
     *
     * @param callableSubroutineId the ID from {@link SubroutineLibrary} to associate with this step
     */
    public static AlgorithmStep distanceStepWithSubroutine(
            AlgorithmRequest request,
            Map<Integer, Integer> dist,
            int infValue,
            int activeLine,
            String title,
            String message,
            Edge activeEdge,
            Vertex current,
            String callableSubroutineId
    ) {
        return buildDistanceStep(request, dist, infValue, activeLine, title, message, activeEdge, current, callableSubroutineId);
    }

    private static AlgorithmStep buildDistanceStep(
            AlgorithmRequest request,
            Map<Integer, Integer> dist,
            int infValue,
            int activeLine,
            String title,
            String message,
            Edge activeEdge,
            Vertex current,
            String callableSubroutineId
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> annotations = new LinkedHashMap<>();

        for (Vertex vertex : request.getScopeVertices()) {
            int id = vertex.getId();
            int d = dist.getOrDefault(id, infValue);
            vertexColors.put(id, d >= infValue ? AlgorithmPalette.SCOPE_NEUTRAL : AlgorithmPalette.DISCOVERED);
            annotations.put(id, "dist=" + formatDistance(d, infValue));
        }

        if (current != null) {
            vertexColors.put(current.getId(), AlgorithmPalette.ACTIVE);
        }
        if (activeEdge != null) {
            edgeColors.put(EdgeKey.from(activeEdge), AlgorithmPalette.ACTIVE_EDGE);
        }

        return new AlgorithmStep(title, message, activeLine, vertexColors, edgeColors, annotations,
                java.util.Collections.emptyMap(), null, null, null, null, callableSubroutineId);
    }

    public static String formatDistance(int value, int infValue) {
        return value >= infValue ? "INF" : String.valueOf(value);
    }
}
