package algorithm.sssp;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DagShortestPathAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure DAG-SHORTEST-PATHS(G, s)",
            "  topologically sort vertices",
            "  initialize d[v] <- INF and d[s] <- 0",
            "  for each u in topological order:",
            "    for each edge (u, v): relax(u, v)",
            "end procedure"
    );

    private static final int INF = 1_000_000_000;

    @Override
    public String getDisplayName() {
        return "DAG-shortest-path";
    }

    @Override
    public String getDescription() {
        return "Computes shortest paths on a DAG in topological order.";
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SSSP;
    }

    @Override
    public GraphOrientationRequirement getOrientationRequirement() {
        return GraphOrientationRequirement.DIRECTED_ONLY;
    }

    @Override
    public List<String> getPseudocodeLines() {
        return PSEUDOCODE;
    }

    @Override
    public String validate(AlgorithmRequest request) {
        if (request.getSourceVertex() == null) {
            return "Choose a source vertex for DAG shortest path.";
        }
        return DagValidation.buildDagWarning(request);
    }

    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<Vertex> order = AlgorithmSupport.topologicalOrder(request);
        List<AlgorithmStep> steps = new ArrayList<>();
        Map<Integer, Integer> dist = new LinkedHashMap<>();
        for (Vertex vertex : request.getScopeVertices()) {
            dist.put(vertex.getId(), INF);
        }
        Vertex source = request.getSourceVertex();
        dist.put(source.getId(), 0);

        steps.add(AlgorithmSupport.distanceStep(
                request,
                dist,
                INF,
                1,
                "Topological order",
                "Order: " + joinVertexNames(order),
                null,
                null
        ));
        steps.add(AlgorithmSupport.distanceStep(
                request,
                dist,
                INF,
                2,
                "Initialize distances",
                "Set source " + source.getName() + " to 0 and all others to INF.",
                null,
                source
        ));

        for (Vertex u : order) {
            steps.add(AlgorithmSupport.distanceStep(
                    request,
                    dist,
                    INF,
                    3,
                    "Process " + u.getName(),
                    "Relax all outgoing edges from " + u.getName() + ".",
                    null,
                    u
            ));

            for (Edge edge : request.getOutgoingEdges(u)) {
                Vertex v = edge.getDestination();
                int du = dist.get(u.getId());
                int dv = dist.get(v.getId());
                int candidate = du >= INF ? INF : du + edge.getWeight();

                steps.add(AlgorithmSupport.distanceStep(
                        request,
                        dist,
                        INF,
                        4,
                        "Inspect " + u.getName() + " -> " + v.getName(),
                        "Candidate=" + AlgorithmSupport.formatDistance(candidate, INF)
                                + ", current=" + AlgorithmSupport.formatDistance(dv, INF),
                        edge,
                        u
                ));

                if (candidate < dv) {
                    dist.put(v.getId(), candidate);
                    steps.add(AlgorithmSupport.distanceStep(
                            request,
                            dist,
                            INF,
                            4,
                            "Relax edge",
                            "Updated " + v.getName() + " to " + candidate + ".",
                            edge,
                            v
                    ));
                } else {
                    steps.add(AlgorithmSupport.distanceStep(
                            request,
                            dist,
                            INF,
                            4,
                            "No relaxation",
                            "No update for " + v.getName() + ".",
                            edge,
                            v
                    ));
                }
            }
        }

        steps.add(AlgorithmSupport.distanceStep(
                request,
                dist,
                INF,
                5,
                "Complete",
                "DAG shortest path finished. Distances are final.",
                null,
                null
        ));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    private static String joinVertexNames(List<Vertex> order) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(order.get(i).getName());
        }
        return sb.toString();
    }
}
