package algorithm.general;

import algorithm.*;
import algorithm.sssp.DagValidation;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Topological ordering demonstration algorithm.
 *
 * The algorithm emits steps compatible with the dedicated topological-order dialog,
 * which consumes the final exported order through {@link #getLastOrder()}.
 */
public final class TopologicalOrderAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure TOPOLOGICAL-SORT(G)",
            "  compute indegree of each vertex",
            "  enqueue all vertices with indegree 0",
            "  while queue is not empty:",
            "    remove u from queue and append u to output",
            "    for each edge (u, v): decrease indegree[v]; if 0 then enqueue v",
            "  if output contains fewer than |V| vertices then cycle exists",
            "end procedure"
    );

    private List<Vertex> lastOrder = List.of();
    private List<Edge> lastEdges = List.of();

    @Override
    public String getDisplayName() {
        return "Topological Order";
    }

    @Override
    public String getDescription() {
        return "Produces a linear ordering for DAGs and visualizes edge direction constraints.";
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SSSP;
    }

    @Override
    public boolean requiresSourceVertex() {
        return false;
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
        return DagValidation.buildDagWarning(request);
    }

    /** Runs Kahn's algorithm and emits a step for indegree updates and queue changes. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        Map<Integer, Integer> indegree = new LinkedHashMap<>();
        Map<Integer, Integer> color = new LinkedHashMap<>();
        Map<Integer, String> annotation = new LinkedHashMap<>();

        for (Vertex vertex : request.getScopeVertices()) {
            indegree.put(vertex.getId(), 0);
        }
        for (Edge edge : request.getScopeEdges()) {
            indegree.put(edge.getDestination().getId(), indegree.get(edge.getDestination().getId()) + 1);
            steps.add(step(request, indegree, color, annotation, 1,
                    "Count indegree",
                    "Increment indegree(" + edge.getDestination().getName() + ") due to "
                            + edge.getSource().getName() + " -> " + edge.getDestination().getName() + ".",
                    edge,
                    edge.getDestination()));
        }

        ArrayDeque<Vertex> queue = new ArrayDeque<>();
        for (Vertex vertex : request.getScopeVertices()) {
            if (indegree.get(vertex.getId()) == 0) {
                queue.add(vertex);
                steps.add(step(request, indegree, color, annotation, 2,
                        "Enqueue zero-indegree",
                        vertex.getName() + " enters queue (indegree 0).",
                        null,
                        vertex));
            }
        }

        List<Vertex> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            Vertex u = queue.removeFirst();
            order.add(u);
            color.put(u.getId(), AlgorithmPalette.FINISHED);
            steps.add(step(request, indegree, color, annotation, 4,
                    "Dequeue " + u.getName(),
                    u.getName() + " is appended to topological order.",
                    null,
                    u));

            for (Edge edge : request.getOutgoingEdges(u)) {
                Vertex v = edge.getDestination();
                int newIn = indegree.get(v.getId()) - 1;
                indegree.put(v.getId(), newIn);
                steps.add(step(request, indegree, color, annotation, 5,
                        "Decrease indegree",
                        "After processing " + u.getName() + " -> " + v.getName() + ", indegree(" + v.getName() + ") = " + newIn + ".",
                        edge,
                        v));
                if (newIn == 0) {
                    queue.add(v);
                    steps.add(step(request, indegree, color, annotation, 5,
                            "Enqueue " + v.getName(),
                            v.getName() + " now has indegree 0 and is enqueued.",
                            edge,
                            v));
                }
            }
        }

        lastOrder = order;
        lastEdges = request.getScopeEdges();

        if (order.size() < request.getScopeVertices().size()) {
            steps.add(step(request, indegree, color, annotation, 6,
                    "Cycle detected",
                    "Output has fewer vertices than |V|, so a cycle exists.",
                    null,
                    null));
        } else {
            steps.add(step(request, indegree, color, annotation, 7,
                    "Complete",
                    "Topological order: " + joinVertexNames(order),
                    null,
                    null));
        }

        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one visualization step using current indegree map and active edge/vertex context. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            Map<Integer, Integer> indegree,
            Map<Integer, Integer> color,
            Map<Integer, String> annotation,
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
            int id = vertex.getId();
            int c = color.getOrDefault(id, AlgorithmPalette.SCOPE_NEUTRAL);
            vertexColors.put(id, c);
            labels.put(id, "in=" + indegree.get(id));
        }
        if (activeVertex != null) {
            vertexColors.put(activeVertex.getId(), AlgorithmPalette.ACTIVE);
        }
        if (activeEdge != null) {
            edgeColors.put(EdgeKey.from(activeEdge), AlgorithmPalette.ACTIVE_EDGE);
        }
        return new AlgorithmStep(title, message, line, vertexColors, edgeColors, labels);
    }

    /** Formats the final order as a user-readable arrow-separated string. */
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

    /** @return last computed topological order, used by the companion dialog. */
    public List<Vertex> getLastOrder() {
        return lastOrder;
    }

    /** @return edges used by the visualization dialog for the last run. */
    public List<Edge> getLastEdges() {
        return lastEdges;
    }
}
