package algorithm.sssp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Breadth-first search timeline generator.
 * The timeline emphasizes queue/frontier behavior, discovery distances, and tree edges.
 */
public final class BreadthFirstSearchAlgorithm implements GraphAlgorithm {
    // CLRS-inspired style (not verbatim) to keep textbook-like notation in the UI.
    private static final List<String> PSEUDOCODE = List.of(
            "procedure BFS(G, s)",
            "  for each u in V[G] do color[u] <- WHITE; d[u] <- INF",
            "  color[s] <- GRAY; d[s] <- 0; ENQUEUE(Q, s)",
            "  while Q is not empty do",
            "    u <- DEQUEUE(Q)",
            "    for each v in Adj[u] do",
            "      if color[v] = WHITE then color[v] <- GRAY; d[v] <- d[u] + 1; ENQUEUE(Q, v)",
            "    color[u] <- BLACK",
            "  if disconnected vertices remain, restart BFS from an unvisited vertex",
            "end procedure"
    );

    @Override
    public String getDisplayName() {
        return "Breadth-First Search (BFS)";
    }

    @Override
    public String getDescription() {
        return "Explores the selected graph level by level from the chosen source vertex.";
    }

    @Override
    public List<String> getPseudocodeLines() {
        return PSEUDOCODE;
    }

    @Override
    public String validate(AlgorithmRequest request) {
        if (request == null || request.isEmpty()) {
            return "Select at least one vertex before running BFS.";
        }
        if (request.getSourceVertex() == null) {
            return "Choose a source vertex for BFS.";
        }
        return null;
    }

    /** Runs BFS over the selected scope and emits one step per major queue/edge event. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        Map<Integer, Integer> distanceByVertex = new LinkedHashMap<>();
        Set<Integer> visited = new LinkedHashSet<>();
        Set<Integer> queued = new LinkedHashSet<>();
        Set<Integer> processed = new LinkedHashSet<>();
        Set<EdgeKey> treeEdges = new LinkedHashSet<>();
        Deque<Vertex> queue = new ArrayDeque<>();

        for (Vertex vertex : request.getScopeVertices()) {
            distanceByVertex.put(vertex.getId(), Integer.MAX_VALUE);
        }

        Vertex source = request.getSourceVertex();
        visited.add(source.getId());
        queued.add(source.getId());
        distanceByVertex.put(source.getId(), 0);
        queue.add(source);

        steps.add(snapshot(request, visited, queued, processed, treeEdges, null, source,
                distanceByVertex,
                1,
                "Initialize BFS",
                "Starting BFS from " + source.getName() + ". The source enters the queue with distance 0."));

        while (!queue.isEmpty()) {
            Vertex current = queue.removeFirst();
            queued.remove(current.getId());

            steps.add(snapshot(request, visited, queued, processed, treeEdges, null, current,
                    distanceByVertex,
                    3,
                    "Visit " + current.getName(),
                    current.getName() + " was removed from the queue. Now BFS explores its outgoing edges."));

            for (Edge edge : request.getOutgoingEdges(current)) {
                Vertex destination = edge.getDestination();

                steps.add(snapshot(request, visited, queued, processed, treeEdges, edge, current,
                        distanceByVertex,
                        4,
                        "Inspect edge " + current.getName() + " -> " + destination.getName(),
                        "Inspecting edge " + current.getName() + " -> " + destination.getName() + "."));

                if (!visited.contains(destination.getId())) {
                    visited.add(destination.getId());
                    queued.add(destination.getId());
                    treeEdges.add(EdgeKey.from(edge));
                    distanceByVertex.put(destination.getId(), distanceByVertex.get(current.getId()) + 1);
                    queue.addLast(destination);

                    steps.add(snapshot(request, visited, queued, processed, treeEdges, edge, current,
                            distanceByVertex,
                            6,
                            "Discover " + destination.getName(),
                            destination.getName() + " is discovered for the first time, gets distance "
                                    + distanceByVertex.get(destination.getId()) + ", and is appended to the queue."));
                } else {
                    steps.add(snapshot(request, visited, queued, processed, treeEdges, edge, current,
                            distanceByVertex,
                            5,
                            "Skip visited vertex",
                            destination.getName() + " was already discovered earlier, so BFS skips re-enqueuing it."));
                }
            }

            processed.add(current.getId());
            steps.add(snapshot(request, visited, queued, processed, treeEdges, null, current,
                    distanceByVertex,
                    7,
                    "Finish " + current.getName(),
                    current.getName() + " is fully processed. All of its outgoing edges in the selected graph were inspected."));

            if (queue.isEmpty()) {
                Vertex nextComponentStart = firstUnvisited(request, visited);
                if (nextComponentStart != null) {
                    visited.add(nextComponentStart.getId());
                    queued.add(nextComponentStart.getId());
                    distanceByVertex.put(nextComponentStart.getId(), 0);
                    queue.addLast(nextComponentStart);
                    steps.add(snapshot(request, visited, queued, processed, treeEdges, null, nextComponentStart,
                            distanceByVertex,
                            8,
                            "Start new component",
                            "The current BFS frontier is empty, so traversal continues from disconnected vertex "
                                    + nextComponentStart.getName() + "."));
                }
            }
        }

        steps.add(snapshot(request, visited, queued, processed, treeEdges, null, null,
                distanceByVertex,
                9,
                "BFS complete",
                "Breadth-first search finished for the selected graph scope."));

        return new AlgorithmResult(getDisplayName(), getPseudocodeLines(), steps);
    }

    /** Finds the next undiscovered vertex when BFS continues into a disconnected component. */
    private Vertex firstUnvisited(AlgorithmRequest request, Set<Integer> visited) {
        for (Vertex vertex : request.getScopeVertices()) {
            if (!visited.contains(vertex.getId())) {
                return vertex;
            }
        }
        return null;
    }

    /** Builds one BFS visualization snapshot from current discovered/queued/processed state. */
    private AlgorithmStep snapshot(
            AlgorithmRequest request,
            Set<Integer> visited,
            Set<Integer> queued,
            Set<Integer> processed,
            Set<EdgeKey> treeEdges,
            Edge activeEdge,
            Vertex current,
            Map<Integer, Integer> distanceByVertex,
            int activeLine,
            String title,
            String message
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> annotations = new LinkedHashMap<>();

        for (Vertex vertex : request.getScopeVertices()) {
            int id = vertex.getId();
            vertexColors.put(id, AlgorithmPalette.SCOPE_NEUTRAL);
            if (visited.contains(id)) {
                vertexColors.put(id, AlgorithmPalette.DISCOVERED);
            }
            if (queued.contains(id)) {
                vertexColors.put(id, AlgorithmPalette.FRONTIER);
            }
            if (processed.contains(id)) {
                vertexColors.put(id, AlgorithmPalette.FINISHED);
            }
            if (current != null && id == current.getId()) {
                vertexColors.put(id, AlgorithmPalette.ACTIVE);
            }

            int distance = distanceByVertex.getOrDefault(id, Integer.MAX_VALUE);
            annotations.put(id, distance == Integer.MAX_VALUE ? "dist=INF" : "dist=" + distance);
        }

        for (Edge edge : request.getScopeEdges()) {
            EdgeKey edgeKey = EdgeKey.from(edge);
            if (treeEdges.contains(edgeKey)) {
                edgeColors.put(edgeKey, AlgorithmPalette.TREE_EDGE);
            }
        }
        if (activeEdge != null) {
            edgeColors.put(EdgeKey.from(activeEdge), AlgorithmPalette.ACTIVE_EDGE);
        }

        return new AlgorithmStep(title, message, activeLine, vertexColors, edgeColors, annotations);
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SSSP;
    }
}
