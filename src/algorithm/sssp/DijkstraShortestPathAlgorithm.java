package algorithm.sssp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Dijkstra shortest-path playback for non-negative weighted directed graphs.
 *
 * The implementation uses a priority queue that may contain stale entries;
 * the timeline explicitly shows when such entries are skipped.
 */
public final class DijkstraShortestPathAlgorithm implements GraphAlgorithm {
    // CLRS-inspired style (not verbatim) to keep textbook-like notation in the UI.
    private static final List<String> PSEUDOCODE = List.of(
            "procedure DIJKSTRA(G, w, s)",
            "  for each v in V[G] do d[v] <- INF; pi[v] <- NIL",
            "  d[s] <- 0; insert s into priority queue Q",
            "  while Q is not empty do",
            "    u <- EXTRACT-MIN(Q)",
            "    if u is already settled then continue",
            "    for each edge (u, v) in Adj[u] do",
            "      if v is settled then continue",
            "      candidate <- d[u] + w(u, v)",
            "      RELAX(u, v, w)",
            "end procedure"
    );

    @Override
    public String getDisplayName() {
        return "Dijkstra Shortest Paths";
    }

    @Override
    public String getDescription() {
        return "Computes shortest-path distances from the chosen source using non-negative edge weights.";
    }

    @Override
    public List<String> getPseudocodeLines() {
        return PSEUDOCODE;
    }

    @Override
    public String validate(AlgorithmRequest request) {
        if (request == null || request.isEmpty()) {
            return "Select at least one vertex before running Dijkstra.";
        }
        if (request.getSourceVertex() == null) {
            return "Choose a source vertex for Dijkstra.";
        }
        if (request.hasNegativeWeightEdge()) {
            return "Dijkstra requires all selected edges to have non-negative weights.";
        }
        return null;
    }

    /** Runs Dijkstra from the chosen source and emits a step for each major queue/relaxation event. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        Map<Integer, Integer> distances = new LinkedHashMap<>();
        Map<Integer, Integer> predecessor = new LinkedHashMap<>();
        Set<Integer> settled = new LinkedHashSet<>();
        Set<Integer> frontier = new LinkedHashSet<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(node -> node.distance));
        Map<String, SubroutineTimeline> relaxTimelines = new LinkedHashMap<>();
        int relaxInvocationCounter = 0;

        for (Vertex vertex : request.getScopeVertices()) {
            distances.put(vertex.getId(), Integer.MAX_VALUE);
        }

        Vertex source = request.getSourceVertex();
        distances.put(source.getId(), 0);
        frontier.add(source.getId());
        queue.add(new Node(source, 0));

        steps.add(snapshot(request, distances, predecessor, settled, frontier, null, source,
                1,
                "Initialize Dijkstra",
                "Starting Dijkstra from " + source.getName() + ". The source distance is 0 and every other distance starts at INF."));

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            Vertex current = node.vertex;
            if (settled.contains(current.getId())) {
                steps.add(snapshot(request, distances, predecessor, settled, frontier, null, current,
                        4,
                        "Skip stale queue entry",
                        current.getName() + " already has a final settled distance, so this old queue entry is ignored."));
                continue;
            }

            frontier.remove(current.getId());
            settled.add(current.getId());
            steps.add(snapshot(request, distances, predecessor, settled, frontier, null, current,
                    5,
                    "Settle " + current.getName(),
                    current.getName() + " has the smallest unsettled tentative distance, so it becomes permanent now."));

            for (Edge edge : request.getOutgoingEdges(current)) {
                Vertex destination = edge.getDestination();
                steps.add(snapshot(request, distances, predecessor, settled, frontier, edge, current,
                        6,
                        "Inspect edge " + current.getName() + " -> " + destination.getName(),
                        "Checking whether edge " + current.getName() + " -> " + destination.getName() + " improves the best known distance."));

                if (settled.contains(destination.getId())) {
                    steps.add(snapshot(request, distances, predecessor, settled, frontier, edge, current,
                            7,
                            "Skip settled vertex",
                            destination.getName() + " is already settled, so its best distance cannot improve anymore."));
                    continue;
                }

                int currentDistance = distances.get(current.getId());
                if (currentDistance == Integer.MAX_VALUE) {
                    continue;
                }
                int candidate = currentDistance + edge.getWeight();
                steps.add(snapshot(request, distances, predecessor, settled, frontier, edge, current,
                        8,
                        "Compute candidate distance",
                        "Candidate distance to " + destination.getName() + " through " + current.getName() + " is " + candidate + "."));

                String relaxInvocationId = SubroutineLibrary.RELAX + "#" + relaxInvocationCounter++;
                List<AlgorithmStep> relaxInvocationSteps = new ArrayList<>();

                // --- RELAX sub-routine call site ---
                AlgorithmStep relaxInspect = snapshotWithSubroutine(
                        request, distances, predecessor, settled, frontier, edge, current,
                        9, "RELAX " + current.getName() + " -> " + destination.getName(),
                        "Calling RELAX(" + current.getName() + ", " + destination.getName() + ", " + edge.getWeight() + "). "
                                + "Testing if d[" + destination.getName() + "] > d[" + current.getName() + "] + " + edge.getWeight() + ".",
                        relaxInvocationId);
                steps.add(relaxInspect);

                // Subroutine steps: lines 1-4 of RELAX pseudocode
                AlgorithmStep sub0 = snapshot(request, distances, predecessor, settled, frontier, edge, current,
                        0, "RELAX entry", "RELAX(" + current.getName() + ", " + destination.getName() + ", " + edge.getWeight() + ") called.");
                AlgorithmStep sub1 = snapshot(request, distances, predecessor, settled, frontier, edge, destination,
                        1, "Test d[v] > d[u] + w",
                        "d[" + destination.getName() + "] = " + AlgorithmSupport.formatDistance(distances.get(destination.getId()), Integer.MAX_VALUE)
                                + "   d[" + current.getName() + "] + w = " + candidate + ".");

                if (candidate < distances.get(destination.getId())) {
                    distances.put(destination.getId(), candidate);
                    predecessor.put(destination.getId(), current.getId());
                    frontier.add(destination.getId());
                    queue.add(new Node(destination, candidate));

                    AlgorithmStep sub2 = snapshot(request, distances, predecessor, settled, frontier, edge, destination,
                            2, "d[v] <- d[u] + w(u,v)",
                            "Condition is TRUE. Setting d[" + destination.getName() + "] <- " + candidate + ".");
                    AlgorithmStep sub3 = snapshot(request, distances, predecessor, settled, frontier, edge, destination,
                            3, "π[v] <- u",
                            "Setting predecessor π[" + destination.getName() + "] <- " + current.getName() + ".");
                    relaxInvocationSteps.add(sub0);
                    relaxInvocationSteps.add(sub1);
                    relaxInvocationSteps.add(sub2);
                    relaxInvocationSteps.add(sub3);

                    steps.add(snapshotWithSubroutine(
                            request, distances, predecessor, settled, frontier, edge, current,
                            9,
                            "Relax edge",
                            "A shorter path to " + destination.getName() + " was found: new distance = " + candidate + ".",
                            relaxInvocationId
                    ));
                } else {
                    AlgorithmStep sub2NoUpdate = snapshot(request, distances, predecessor, settled, frontier, edge, destination,
                            1, "No relaxation needed",
                            "Condition is FALSE (candidate " + candidate + " >= current " + AlgorithmSupport.formatDistance(distances.get(destination.getId()), Integer.MAX_VALUE) + "). d[v] unchanged.");
                    relaxInvocationSteps.add(sub0);
                    relaxInvocationSteps.add(sub1);
                    relaxInvocationSteps.add(sub2NoUpdate);

                    steps.add(snapshotWithSubroutine(
                            request, distances, predecessor, settled, frontier, edge, current,
                            9,
                            "No relaxation",
                            "The candidate distance " + candidate + " does not improve the current best distance for "
                                    + destination.getName() + ".",
                            relaxInvocationId
                    ));
                }

                relaxTimelines.put(relaxInvocationId, new SubroutineTimeline(
                        relaxInvocationId,
                        "RELAX(u, v, w)",
                        SubroutineLibrary.RELAX_PSEUDOCODE,
                        relaxInvocationSteps
                ));
            }
        }

        steps.add(snapshot(request, distances, predecessor, settled, frontier, null, null,
                10,
                "Dijkstra complete",
                "Dijkstra finished. The green path edges form the current predecessor tree from the source."));

        return new AlgorithmResult(
                getDisplayName(),
                getPseudocodeLines(),
                steps,
                false,
                "",
                relaxTimelines,
                Map.of(9, SubroutineLibrary.RELAX)
        );
    }

    /** Builds one Dijkstra visualization snapshot from the current queue/settled state. */
    private AlgorithmStep snapshot(
            AlgorithmRequest request,
            Map<Integer, Integer> distances,
            Map<Integer, Integer> predecessor,
            Set<Integer> settled,
            Set<Integer> frontier,
            Edge activeEdge,
            Vertex current,
            int activeLine,
            String title,
            String message
    ) {
        return snapshotWithSubroutine(request, distances, predecessor, settled, frontier, activeEdge, current,
                activeLine, title, message, null);
    }

    /** Same as {@link #snapshot} but marks the step as a callable sub-routine anchor. */
    private AlgorithmStep snapshotWithSubroutine(
            AlgorithmRequest request,
            Map<Integer, Integer> distances,
            Map<Integer, Integer> predecessor,
            Set<Integer> settled,
            Set<Integer> frontier,
            Edge activeEdge,
            Vertex current,
            int activeLine,
            String title,
            String message,
            String callableSubroutineId
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> annotations = new LinkedHashMap<>();

        for (Vertex vertex : request.getScopeVertices()) {
            int id = vertex.getId();
            vertexColors.put(id, AlgorithmPalette.SCOPE_NEUTRAL);
            if (distances.get(id) != null && distances.get(id) != Integer.MAX_VALUE) {
                vertexColors.put(id, AlgorithmPalette.DISCOVERED);
            }
            if (frontier.contains(id)) {
                vertexColors.put(id, AlgorithmPalette.FRONTIER);
            }
            if (settled.contains(id)) {
                vertexColors.put(id, AlgorithmPalette.FINISHED);
            }
            if (current != null && current.getId() == id) {
                vertexColors.put(id, AlgorithmPalette.ACTIVE);
            }

            Integer distance = distances.get(id);
            annotations.put(id, distance == null || distance == Integer.MAX_VALUE ? "dist=INF" : "dist=" + distance);
        }

        for (Edge edge : request.getScopeEdges()) {
            Integer parentId = predecessor.get(edge.getDestination().getId());
            if (parentId != null && parentId == edge.getSource().getId()) {
                edgeColors.put(EdgeKey.from(edge), AlgorithmPalette.PATH_EDGE);
            }
        }
        if (activeEdge != null) {
            edgeColors.put(EdgeKey.from(activeEdge), AlgorithmPalette.ACTIVE_EDGE);
        }

        return new AlgorithmStep(title, message, activeLine, vertexColors, edgeColors, annotations,
                java.util.Collections.emptyMap(), null, null, null, null, callableSubroutineId);
    }

    /** Queue record storing a vertex together with the tentative distance used for ordering. */
    private static final class Node {
        private final Vertex vertex;
        private final int distance;

        private Node(Vertex vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SSSP;
    }
}
