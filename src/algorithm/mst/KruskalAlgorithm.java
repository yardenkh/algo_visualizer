package algorithm.mst;

import algorithm.*;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kruskal minimum spanning tree demonstration.
 *
 * This implementation visualizes the sorted-edge sweep and tracks component ids
 * using a lightweight union-find parent map.
 */
public final class KruskalAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure KRUSKAL(G)",
            "  sort edges by nondecreasing weight",
            "  for each edge (u, v) in sorted order:",
            "    if FIND-SET(u) != FIND-SET(v): add edge and UNION(u, v)",
            "end procedure"
    );

    @Override public String getDisplayName() { return "Kruskal"; }
    @Override public String getDescription() { return "Minimum spanning tree via sorted edges and disjoint sets."; }
    @Override public AlgorithmType getType() { return AlgorithmType.MST; }
    @Override public GraphOrientationRequirement getOrientationRequirement() { return GraphOrientationRequirement.UNDIRECTED_ONLY; }
    @Override public boolean requiresSourceVertex() { return false; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }
    @Override public String validate(AlgorithmRequest request) { return request.isEmpty() ? "Select vertices before running Kruskal." : null; }

    /** Runs Kruskal and emits steps for sorting, acceptance, and cycle rejection decisions. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        List<Edge> edges = new ArrayList<>(request.getScopeEdges());
        edges.sort(Comparator.comparingInt(Edge::getWeight));

        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> acceptedVertices = new HashSet<>();
        Set<EdgeKey> acceptedEdges = new HashSet<>();
        Set<EdgeKey> rejectedEdges = new HashSet<>();
        for (Vertex vertex : request.getScopeVertices()) {
            parent.put(vertex.getId(), vertex.getId());
        }

        steps.add(step(request, parent, acceptedVertices, acceptedEdges, rejectedEdges,
                1, "Sort edges", "Edges sorted by weight.", null, null, false));

        for (Edge edge : edges) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            int ru = find(parent, u);
            int rv = find(parent, v);

            steps.add(step(request, parent, acceptedVertices, acceptedEdges, rejectedEdges, 2,
                    "Inspect edge",
                    edge.getSource().getName() + " -> " + edge.getDestination().getName()
                            + " (w=" + edge.getWeight() + ") with roots V" + ru + " and V" + rv + ".",
                    edge,
                    edge.getSource(),
                    true));

            if (ru != rv) {
                parent.put(ru, rv);
                acceptedEdges.add(EdgeKey.from(edge));
                acceptedVertices.add(u);
                acceptedVertices.add(v);
                steps.add(step(request, parent, acceptedVertices, acceptedEdges, rejectedEdges, 3,
                        "Accept edge",
                        "Different components, so edge is accepted into the MST and the components are merged.",
                        edge,
                        edge.getDestination(),
                        false));
            } else {
                rejectedEdges.add(EdgeKey.from(edge));
                steps.add(step(request, parent, acceptedVertices, acceptedEdges, rejectedEdges, 3,
                        "Reject edge",
                        "Same component, so edge is rejected because it would create a cycle.",
                        edge,
                        edge.getDestination(),
                        false));
            }
        }

        steps.add(step(request, parent, acceptedVertices, acceptedEdges, rejectedEdges,
                4, "Complete", "Kruskal completed.", null, null, false));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one Kruskal step showing the current component representative of every vertex. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            Map<Integer, Integer> parent,
            Set<Integer> acceptedVertices,
            Set<EdgeKey> acceptedEdges,
            Set<EdgeKey> rejectedEdges,
            int line,
            String title,
            String message,
            Edge activeEdge,
            Vertex activeVertex,
            boolean emphasizeActive
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> labels = new LinkedHashMap<>();

        for (Vertex vertex : request.getScopeVertices()) {
            int id = vertex.getId();
            int root = find(parent, id);
            vertexColors.put(id, acceptedVertices.contains(id) ? AlgorithmPalette.ACCEPTED : AlgorithmPalette.DISCOVERED);
            labels.put(id, "set=V" + root);
        }

        for (EdgeKey edgeKey : acceptedEdges) {
            edgeColors.put(edgeKey, AlgorithmPalette.ACCEPTED);
        }
        for (EdgeKey edgeKey : rejectedEdges) {
            edgeColors.put(edgeKey, AlgorithmPalette.REJECTED);
        }

        if (emphasizeActive && activeVertex != null && !acceptedVertices.contains(activeVertex.getId())) {
            vertexColors.put(activeVertex.getId(), AlgorithmPalette.ACTIVE);
        }
        if (emphasizeActive && activeEdge != null) {
            EdgeKey activeKey = EdgeKey.from(activeEdge);
            if (!acceptedEdges.contains(activeKey) && !rejectedEdges.contains(activeKey)) {
                edgeColors.put(activeKey, AlgorithmPalette.ACTIVE_EDGE);
            }
        }

        return new AlgorithmStep(title, message, line, vertexColors, edgeColors, labels);
    }

    /** Union-find find operation with path compression. */
    private static int find(Map<Integer, Integer> parent, int x) {
        if (parent.get(x) == x) {
            return x;
        }
        int root = find(parent, parent.get(x));
        parent.put(x, root);
        return root;
    }
}
