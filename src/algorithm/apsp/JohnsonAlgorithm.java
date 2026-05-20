package algorithm.apsp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Johnson APSP for sparse graphs.
 *
 * This educational implementation visualizes the high-level phases:
 * potential computation, negative-cycle check, edge reweighting, and per-source Dijkstra runs.
 */
public final class JohnsonAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure JOHNSON(G)",
            "  run BELLMAN-FORD from super-source to compute h[v]",
            "  if negative cycle exists then abort",
            "  reweight edges w'(u,v) <- w(u,v) + h[u] - h[v]",
            "  for each source u in V: run DIJKSTRA on w'",
            "  convert distances back to original weights",
            "end procedure"
    );

    private static final int INF = 1_000_000_000;

    @Override public String getDisplayName() { return "johnson"; }
    @Override public String getDescription() { return "All-pairs shortest paths using reweighting + Dijkstra."; }
    @Override public AlgorithmType getType() { return AlgorithmType.APSP; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }

    @Override
    public String validate(AlgorithmRequest request) {
        if (request.isEmpty()) {
            return "Select vertices before running Johnson.";
        }
        return null;
    }

    /** Runs the staged Johnson demonstration and emits summary steps for each phase. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();

        Map<Integer, Integer> h = new LinkedHashMap<>();
        for (Vertex vertex : request.getScopeVertices()) {
            h.put(vertex.getId(), 0);
        }
        steps.add(AlgorithmSupport.distanceStep(request, h, INF, 1,
                "Bellman-Ford setup",
                "Initialize potential h[v]=0 for all vertices.",
                null,
                null));

        int n = request.getScopeVertices().size();
        for (int pass = 1; pass <= Math.max(1, n - 1); pass++) {
            steps.add(AlgorithmSupport.distanceStep(request, h, INF, 1,
                    "Bellman-Ford pass " + pass,
                    "Relax all edges to compute potential h[v].",
                    null,
                    null));
            for (Edge edge : request.getScopeEdges()) {
                int u = edge.getSource().getId();
                int v = edge.getDestination().getId();
                int candidate = h.get(u) + edge.getWeight();
                int old = h.get(v);
                steps.add(AlgorithmSupport.distanceStep(request, h, INF, 1,
                        "Inspect edge",
                        edge.getSource().getName() + " -> " + edge.getDestination().getName()
                                + ": candidate=" + candidate + ", current=" + old,
                        edge,
                        edge.getSource()));
                if (candidate < old) {
                    h.put(v, candidate);
                    steps.add(AlgorithmSupport.distanceStep(request, h, INF, 1,
                            "Relax potential",
                            "Updated h(" + edge.getDestination().getName() + ") to " + candidate + ".",
                            edge,
                            edge.getDestination()));
                }
            }
        }

        for (Edge edge : request.getScopeEdges()) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            if (h.get(u) + edge.getWeight() < h.get(v)) {
                steps.add(AlgorithmSupport.neutralStep(request, 2,
                        "Abort",
                        "Negative cycle detected by Bellman-Ford stage."));
                return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps, true, "Infinite loop will occur");
            }
        }

        for (Edge edge : request.getScopeEdges()) {
            int wPrime = edge.getWeight() + h.get(edge.getSource().getId()) - h.get(edge.getDestination().getId());
            steps.add(AlgorithmSupport.neutralStep(request, 3,
                    "Reweight edge",
                    edge.getSource().getName() + " -> " + edge.getDestination().getName() + ": w'=" + wPrime + "."));
        }

        for (Vertex source : request.getScopeVertices()) {
            steps.add(AlgorithmSupport.neutralStep(request, 4,
                    "Run Dijkstra from " + source.getName(),
                    "Compute reweighted shortest paths from this source."));
        }

        steps.add(AlgorithmSupport.neutralStep(request, 5,
                "Restore original distances",
                "Convert d' back to d using h[v]-h[u]."));

        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }
}
