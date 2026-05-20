package algorithm.apsp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * APSP by running Dijkstra from each source (works for non-negative weights).
 *
 * If the selected graph contains a negative cycle, this demo intentionally produces
 * a short repeating-warning sequence so users can see why the approach is invalid there.
 */
public final class RepeatedDijkstraAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure REPEATED-DIJKASTRA(G)",
            "  for each source s in V:",
            "    run DIJKSTRA(G, s)",
            "  if a negative cycle is encountered, process keeps improving forever",
            "  show INFINITE LOOP",
            "end procedure",
            "infinite loop"
    );

    private static final int INF = 1_000_000_000;

    @Override public String getDisplayName() { return "repeated dijkastra"; }
    @Override public String getDescription() { return "Runs Dijkstra from every source vertex to obtain APSP."; }
    @Override public AlgorithmType getType() { return AlgorithmType.APSP; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }
    @Override public String validate(AlgorithmRequest request) { return request.isEmpty() ? "Select vertices before running repeated dijkastra." : null; }

    /** Runs repeated Dijkstra over all scoped sources, or emits an infinite-loop warning demo. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();

        boolean hasNegativeEdge = request.hasNegativeWeightEdge();
        boolean hasNegativeCycle = hasNegativeEdge && AlgorithmSupport.hasNegativeCycle(request);

        if (hasNegativeCycle) {
            List<Edge> cycleEdges = request.getScopeEdges().isEmpty()
                    ? List.of()
                    : request.getScopeEdges().subList(0, Math.min(3, request.getScopeEdges().size()));
            for (int i = 1; i <= 4; i++) {
                for (Edge edge : cycleEdges) {
                    steps.add(AlgorithmSupport.neutralStep(
                            request,
                            3,
                            "Cycle replay " + i,
                            "Distance keeps decreasing on " + edge.getSource().getName() + " -> "
                                    + edge.getDestination().getName() + "."
                    ));
                }
            }
            steps.add(AlgorithmSupport.neutralStep(request, 6, "Infinite loop", "infinite loop"));
            return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps, true, "Infinite loop will occur");
        }

        for (Vertex source : request.getScopeVertices()) {
            Map<Integer, Integer> dist = new LinkedHashMap<>();
            for (Vertex v : request.getScopeVertices()) {
                dist.put(v.getId(), INF);
            }
            dist.put(source.getId(), 0);

            PriorityQueue<Vertex> queue = new PriorityQueue<>(Comparator.comparingInt(v -> dist.get(v.getId())));
            queue.add(source);

            steps.add(AlgorithmSupport.distanceStep(
                    request,
                    dist,
                    INF,
                    1,
                    "Source " + source.getName(),
                    "Run Dijkstra from " + source.getName() + ".",
                    null,
                    source
            ));

            while (!queue.isEmpty()) {
                Vertex current = queue.poll();
                int base = dist.get(current.getId());

                steps.add(AlgorithmSupport.distanceStep(
                        request,
                        dist,
                        INF,
                        2,
                        "Pop " + current.getName(),
                        "Current distance=" + AlgorithmSupport.formatDistance(base, INF),
                        null,
                        current
                ));

                for (Edge edge : request.getOutgoingEdges(current)) {
                    Vertex destination = edge.getDestination();
                    int candidate = base >= INF ? INF : base + edge.getWeight();
                    int old = dist.get(destination.getId());

                    steps.add(AlgorithmSupport.distanceStep(
                            request,
                            dist,
                            INF,
                            2,
                            "Inspect edge",
                            current.getName() + " -> " + destination.getName() + ": candidate="
                                    + AlgorithmSupport.formatDistance(candidate, INF)
                                    + ", current=" + AlgorithmSupport.formatDistance(old, INF),
                            edge,
                            current
                    ));

                    if (candidate < old) {
                        dist.put(destination.getId(), candidate);
                        queue.remove(destination);
                        queue.add(destination);
                        steps.add(AlgorithmSupport.distanceStep(
                                request,
                                dist,
                                INF,
                                2,
                                "Relax edge",
                                "Updated " + destination.getName() + " to " + candidate + ".",
                                edge,
                                destination
                        ));
                    } else {
                        steps.add(AlgorithmSupport.distanceStep(
                                request,
                                dist,
                                INF,
                                2,
                                "No relaxation",
                                "No update.",
                                edge,
                                destination
                        ));
                    }
                }
            }

            steps.add(AlgorithmSupport.distanceStep(
                    request,
                    dist,
                    INF,
                    2,
                    "Source done",
                    "Finished Dijkstra from " + source.getName() + ".",
                    null,
                    source
            ));
        }

        if (hasNegativeEdge) {
            steps.add(AlgorithmSupport.neutralStep(request, 3,
                    "Warning",
                    "Negative edges were detected; repeated Dijkstra may be incorrect."));
        }

        steps.add(AlgorithmSupport.neutralStep(request, 5, "Complete", "Repeated Dijkstra finished."));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }
}
