package algorithm.sssp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bellman-Ford single-source shortest path algorithm.
 *
 * Unlike Dijkstra, this algorithm allows negative edges and explicitly checks for
 * negative cycles after the relaxation passes. The generated timeline breaks each
 * relaxation into its own visual step.
 */
public final class BellmanFordAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure BELLMAN-FORD(G, s)",
            "  initialize d[v] <- INF and d[s] <- 0",
            "  repeat |V|-1 times",
            "    for each edge (u, v): relax(u, v)",
            "  for each edge (u, v): if d[v] > d[u] + w(u, v) then report negative cycle",
            "end procedure"
    );

    private static final int INF = 1_000_000_000;

    @Override
    public String getDisplayName() {
        return "Bellman-Ford";
    }

    @Override
    public String getDescription() {
        return "Single-source shortest paths with support for negative weights.";
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SSSP;
    }

    @Override
    public List<String> getPseudocodeLines() {
        return PSEUDOCODE;
    }

    @Override
    public String validate(AlgorithmRequest request) {
        if (request == null || request.isEmpty()) {
            return "Select at least one vertex before running Bellman-Ford.";
        }
        if (request.getSourceVertex() == null) {
            return "Choose a source vertex for Bellman-Ford.";
        }
        return null;
    }

    /**
     * Runs Bellman-Ford and emits a step for every edge inspection/relaxation.
     */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        Map<Integer, Integer> dist = new LinkedHashMap<>();
        for (Vertex vertex : request.getScopeVertices()) {
            dist.put(vertex.getId(), INF);
        }
        Vertex source = request.getSourceVertex();
        dist.put(source.getId(), 0);
        SubroutineStepCollector relaxCollector = new SubroutineStepCollector();

        steps.add(AlgorithmSupport.distanceStep(
                request, dist, INF, 1,
                "Initialize distances",
                "All vertices start at INF except " + source.getName() + " which starts at 0.",
                null, source
        ));

        int n = request.getScopeVertices().size();
        for (int pass = 1; pass <= n - 1; pass++) {
            boolean changedInPass = false;
            steps.add(AlgorithmSupport.distanceStep(
                    request, dist, INF, 2,
                    "Pass " + pass + " begins", "Scanning all edges for relaxations.",
                    null, null
            ));

            for (Edge edge : request.getScopeEdges()) {
                Vertex uVertex = edge.getSource();
                Vertex vVertex = edge.getDestination();
                int u = uVertex.getId();
                int v = vVertex.getId();
                int du = dist.get(u);
                int dv = dist.get(v);
                int candidate = du >= INF ? INF : du + edge.getWeight();

                // Tag as RELAX call site
                steps.add(AlgorithmSupport.distanceStepWithSubroutine(
                        request, dist, INF, 3,
                        "RELAX " + uVertex.getName() + " -> " + vVertex.getName(),
                        "Calling RELAX(" + uVertex.getName() + ", " + vVertex.getName() + ", " + edge.getWeight() + "). "
                                + "d[" + vVertex.getName() + "]=" + AlgorithmSupport.formatDistance(dv, INF)
                                + "  candidate=" + AlgorithmSupport.formatDistance(candidate, INF) + ".",
                        edge, uVertex, SubroutineLibrary.RELAX
                ));

                // Subroutine body steps
                relaxCollector.add(AlgorithmSupport.distanceStep(request, dist, INF, 0,
                        "RELAX entry", "RELAX(" + uVertex.getName() + ", " + vVertex.getName() + ", " + edge.getWeight() + ") called.",
                        edge, uVertex));
                relaxCollector.add(AlgorithmSupport.distanceStep(request, dist, INF, 1,
                        "Test d[v] > d[u] + w",
                        "d[" + vVertex.getName() + "] = " + AlgorithmSupport.formatDistance(dv, INF)
                                + "   d[" + uVertex.getName() + "] + w = " + AlgorithmSupport.formatDistance(candidate, INF) + ".",
                        edge, vVertex));

                if (candidate < dv) {
                    dist.put(v, candidate);
                    changedInPass = true;
                    relaxCollector.add(AlgorithmSupport.distanceStep(request, dist, INF, 2,
                            "d[v] <- d[u] + w(u,v)",
                            "Condition TRUE. d[" + vVertex.getName() + "] <- " + candidate + ".",
                            edge, vVertex));
                    relaxCollector.add(AlgorithmSupport.distanceStep(request, dist, INF, 3,
                            "π[v] <- u", "π[" + vVertex.getName() + "] <- " + uVertex.getName() + ".",
                            edge, vVertex));
                    steps.add(AlgorithmSupport.distanceStep(request, dist, INF, 3,
                            "Relax edge", "Updated " + vVertex.getName() + " to " + candidate + ".",
                            edge, vVertex));
                } else {
                    relaxCollector.add(AlgorithmSupport.distanceStep(request, dist, INF, 1,
                            "No relaxation",
                            "Condition FALSE. d[" + vVertex.getName() + "] unchanged.",
                            edge, vVertex));
                    steps.add(AlgorithmSupport.distanceStep(request, dist, INF, 3,
                            "No relaxation", "No update for " + vVertex.getName() + ".",
                            edge, vVertex));
                }
            }

            steps.add(AlgorithmSupport.distanceStep(
                    request, dist, INF, 2,
                    "Pass " + pass + " ends",
                    changedInPass
                            ? "At least one distance changed in this pass."
                            : "No distance changed; remaining passes will only verify stability.",
                    null, null
            ));
        }

        boolean negativeCycle = false;
        for (Edge edge : request.getScopeEdges()) {
            Vertex uVertex = edge.getSource();
            Vertex vVertex = edge.getDestination();
            int u = uVertex.getId();
            int v = vVertex.getId();
            int du = dist.get(u);
            int dv = dist.get(v);
            int candidate = du >= INF ? INF : du + edge.getWeight();

            steps.add(AlgorithmSupport.distanceStep(
                    request, dist, INF, 4,
                    "Cycle check " + uVertex.getName() + " -> " + vVertex.getName(),
                    "Verify whether further relaxation is still possible.",
                    edge, uVertex
            ));

            if (candidate < dv) {
                negativeCycle = true;
                steps.add(AlgorithmSupport.distanceStep(
                        request, dist, INF, 4,
                        "Negative cycle detected",
                        "Further relaxation is possible after |V|-1 passes.",
                        edge, vVertex
                ));
                break;
            }
        }

        SubroutineTimeline relaxTimeline = relaxCollector.build(
                SubroutineLibrary.RELAX, "RELAX(u, v, w)", SubroutineLibrary.RELAX_PSEUDOCODE);
        SubroutineStepCollector.AlgorithmResultSubroutineData sub =
                SubroutineStepCollector.wrap(relaxTimeline, List.of(3));

        if (negativeCycle) {
            return new AlgorithmResult(
                    getDisplayName(), PSEUDOCODE, steps, true, "Infinite loop will occur",
                    sub.timelines, sub.callableByLine
            );
        }

        steps.add(AlgorithmSupport.distanceStep(
                request, dist, INF, 5,
                "Complete", "Bellman-Ford finished. Final distances are shown on the vertices.",
                null, null
        ));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps, false, "", sub.timelines, sub.callableByLine);
    }
}
