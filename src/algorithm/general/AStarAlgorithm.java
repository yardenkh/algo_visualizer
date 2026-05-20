package algorithm.general;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A* search demonstration.
 *
 * This educational version uses a zero heuristic when no domain-specific heuristic is available,
 * which effectively makes the behavior Dijkstra-like while preserving A* terminology in the UI.
 */
public final class AStarAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure A*(G, start, goal)",
            "  OPEN <- priority queue ordered by f(v)=g(v)+h(v)",
            "  g[start] <- 0; push start",
            "  while OPEN not empty:",
            "    u <- pop minimum f",
            "    if u = goal then reconstruct path and return",
            "    for each edge (u, v): relax tentative g[v]",
            "end procedure"
    );

    private static final int INF = 1_000_000_000;

    @Override public String getDisplayName() { return "A*"; }
    @Override public String getDescription() { return "Heuristic shortest-path search; this demo uses h(v)=0 when no heuristic is provided."; }
    @Override public AlgorithmType getType() { return AlgorithmType.GENERAL; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }

    @Override
    public String validate(AlgorithmRequest request) {
        if (request.getSourceVertex() == null) {
            return "Choose a source vertex for A*.";
        }
        return null;
    }

    /** Runs the demonstration search and emits steps for OPEN processing and relaxations. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        List<Vertex> vertices = request.getScopeVertices();
        Vertex start = request.getSourceVertex();
        Vertex goal = vertices.get(vertices.size() - 1);
        if (goal.getId() == start.getId() && vertices.size() > 1) {
            goal = vertices.get(0);
        }

        Map<Integer, Integer> gScore = new LinkedHashMap<>();
        for (Vertex vertex : vertices) {
            gScore.put(vertex.getId(), INF);
        }
        gScore.put(start.getId(), 0);

        PriorityQueue<Vertex> open = new PriorityQueue<>(Comparator.comparingInt(v -> gScore.get(v.getId())));
        open.add(start);

        steps.add(step(request, gScore, 2,
                "Initialize OPEN",
                "Start=" + start.getName() + ", Goal=" + goal.getName() + ".",
                null,
                start));

        while (!open.isEmpty()) {
            Vertex current = open.poll();
            steps.add(step(request, gScore, 4,
                    "Pop minimum",
                    "Expand " + current.getName() + " with g=" + AlgorithmSupport.formatDistance(gScore.get(current.getId()), INF) + ".",
                    null,
                    current));

            if (current.getId() == goal.getId()) {
                steps.add(step(request, gScore, 5,
                        "Goal reached",
                        "Reached goal " + goal.getName() + ".",
                        null,
                        current));
                return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
            }

            for (Edge edge : request.getOutgoingEdges(current)) {
                Vertex next = edge.getDestination();
                int candidate = gScore.get(current.getId()) >= INF ? INF : gScore.get(current.getId()) + edge.getWeight();
                int old = gScore.get(next.getId());

                steps.add(step(request, gScore, 6,
                        "Inspect edge",
                        current.getName() + " -> " + next.getName() + " candidate="
                                + AlgorithmSupport.formatDistance(candidate, INF)
                                + ", current=" + AlgorithmSupport.formatDistance(old, INF),
                        edge,
                        next));

                if (candidate < old) {
                    gScore.put(next.getId(), candidate);
                    open.remove(next);
                    open.add(next);
                    steps.add(step(request, gScore, 6,
                            "Relax",
                            "Updated g(" + next.getName() + ") to " + candidate + " and reinserted in OPEN.",
                            edge,
                            next));
                } else {
                    steps.add(step(request, gScore, 6,
                            "No relaxation",
                            "No update for " + next.getName() + ".",
                            edge,
                            next));
                }
            }
        }

        steps.add(step(request, gScore, 7,
                "No path",
                "OPEN is empty; goal is unreachable.",
                null,
                null));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one visualization step from the current g-score map and active search focus. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            Map<Integer, Integer> gScore,
            int line,
            String title,
            String message,
            Edge activeEdge,
            Vertex activeVertex
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> labels = new LinkedHashMap<>();
        for (Vertex vertex : request.getScopeVertices()) {
            int g = gScore.get(vertex.getId());
            vertexColors.put(vertex.getId(), g >= INF ? AlgorithmPalette.SCOPE_NEUTRAL : AlgorithmPalette.DISCOVERED);
            labels.put(vertex.getId(), "g=" + AlgorithmSupport.formatDistance(g, INF));
        }

        if (activeVertex != null) {
            vertexColors.put(activeVertex.getId(), AlgorithmPalette.ACTIVE);
        }
        if (activeEdge != null) {
            edgeColors.put(EdgeKey.from(activeEdge), AlgorithmPalette.ACTIVE_EDGE);
        }

        return new AlgorithmStep(title, message, line, vertexColors, edgeColors, labels);
    }
}
