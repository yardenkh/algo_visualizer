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
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Prim MST (interprets graph edges as weighted undirected links).
 *
 * The timeline emphasizes key values and selected parent links as the MST grows.
 */
public final class PrimAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure PRIM(G, r)",
            "  initialize all keys to INF, key[r] <- 0",
            "  while priority queue not empty:",
            "    extract vertex u with minimum key",
            "    add u to MST",
            "    for each adjacent edge (u, v): if w(u,v) improves key[v], update parent[v]",
            "end procedure"
    );

    private static final int INF = 1_000_000_000;

    @Override public String getDisplayName() { return "Prim"; }
    @Override public String getDescription() { return "Minimum spanning tree via greedy vertex expansion."; }
    @Override public AlgorithmType getType() { return AlgorithmType.MST; }
    @Override public GraphOrientationRequirement getOrientationRequirement() { return GraphOrientationRequirement.UNDIRECTED_ONLY; }
    @Override public List<String> getPseudocodeLines() { return PSEUDOCODE; }

    @Override
    public String validate(AlgorithmRequest request) {
        return request.getSourceVertex() == null ? "Choose a source vertex for Prim." : null;
    }

    /** Runs Prim from the chosen start vertex and emits key-update steps. */
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        Map<Integer, Integer> key = new LinkedHashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Map<Integer, EdgeKey> parentEdgeByVertex = new HashMap<>();
        Set<Integer> inTree = new HashSet<>();
        Set<EdgeKey> acceptedEdges = new HashSet<>();
        Set<EdgeKey> rejectedEdges = new HashSet<>();

        for (Vertex vertex : request.getScopeVertices()) {
            key.put(vertex.getId(), INF);
            parent.put(vertex.getId(), -1);
        }
        Vertex start = request.getSourceVertex();
        key.put(start.getId(), 0);

        PriorityQueue<Vertex> queue = new PriorityQueue<>(Comparator.comparingInt(v -> key.get(v.getId())));
        queue.addAll(request.getScopeVertices());

        steps.add(step(request, key, parent, inTree, acceptedEdges, rejectedEdges, 1,
                "Initialize",
                "Start at " + start.getName() + " with key 0.",
                null,
                start,
                true));

        while (!queue.isEmpty()) {
            Vertex u = queue.poll();
            if (inTree.contains(u.getId())) {
                continue;
            }

            steps.add(step(request, key, parent, inTree, acceptedEdges, rejectedEdges, 3,
                    "Extract min",
                    "Extracted " + u.getName() + " with key=" + AlgorithmSupport.formatDistance(key.get(u.getId()), INF) + ".",
                    null,
                    u,
                    true));

            inTree.add(u.getId());
            EdgeKey acceptedParentEdge = parentEdgeByVertex.get(u.getId());
            if (acceptedParentEdge != null) {
                acceptedEdges.add(acceptedParentEdge);
            }
            steps.add(step(request, key, parent, inTree, acceptedEdges, rejectedEdges, 4,
                    "Add to MST",
                    acceptedParentEdge == null
                            ? u.getName() + " is now fixed in MST."
                            : u.getName() + " is now fixed in MST via edge "
                            + edgeName(acceptedParentEdge) + ".",
                    null,
                    u,
                    false));

            for (Edge edge : request.getOutgoingEdges(u)) {
                Vertex v = edge.getDestination();
                if (inTree.contains(v.getId())) {
                    continue;
                }

                steps.add(step(request, key, parent, inTree, acceptedEdges, rejectedEdges, 5,
                        "Inspect edge",
                        "Inspect " + u.getName() + " -> " + v.getName() + " (w=" + edge.getWeight() + ").",
                        edge,
                        v,
                        true));

                if (edge.getWeight() < key.get(v.getId())) {
                    EdgeKey previousParentEdge = parentEdgeByVertex.get(v.getId());
                    if (previousParentEdge != null) {
                        rejectedEdges.add(previousParentEdge);
                    }
                    key.put(v.getId(), edge.getWeight());
                    parent.put(v.getId(), u.getId());
                    parentEdgeByVertex.put(v.getId(), EdgeKey.from(edge));
                    queue.remove(v);
                    queue.add(v);
                    steps.add(step(request, key, parent, inTree, acceptedEdges, rejectedEdges, 5,
                            "Improve key",
                            "Updated key(" + v.getName() + ")=" + edge.getWeight() + " and parent=" + u.getName() + ".",
                            edge,
                            v,
                            true));
                } else {
                    rejectedEdges.add(EdgeKey.from(edge));
                    steps.add(step(request, key, parent, inTree, acceptedEdges, rejectedEdges, 5,
                            "No key change",
                            "Edge " + u.getName() + " -> " + v.getName()
                                    + " is rejected; key(" + v.getName() + ") remains "
                                    + AlgorithmSupport.formatDistance(key.get(v.getId()), INF) + ".",
                            edge,
                            v,
                            false));
                }
            }
        }

        steps.add(step(request, key, parent, inTree, acceptedEdges, rejectedEdges, 6,
                "Complete",
                "Prim completed.",
                null,
                null,
                false));
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }

    /** Builds one Prim visualization snapshot from key/parent/in-tree state. */
    private static AlgorithmStep step(
            AlgorithmRequest request,
            Map<Integer, Integer> key,
            Map<Integer, Integer> parent,
            Set<Integer> inTree,
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
            int k = key.get(id);
            vertexColors.put(id, inTree.contains(id) ? AlgorithmPalette.ACCEPTED : (k >= INF ? AlgorithmPalette.SCOPE_NEUTRAL : AlgorithmPalette.DISCOVERED));
            String parentText = parent.get(id) < 0 ? "-" : "V" + parent.get(id);
            labels.put(id, "key=" + AlgorithmSupport.formatDistance(k, INF) + " p=" + parentText);
        }

        for (EdgeKey edgeKey : acceptedEdges) {
            edgeColors.put(edgeKey, AlgorithmPalette.ACCEPTED);
        }
        for (EdgeKey edgeKey : rejectedEdges) {
            edgeColors.put(edgeKey, AlgorithmPalette.REJECTED);
        }

        if (emphasizeActive && activeVertex != null && !inTree.contains(activeVertex.getId())) {
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

    private static String edgeName(EdgeKey edgeKey) {
        return "(" + (edgeKey.getSourceId() + 1) + ", " + (edgeKey.getDestinationId() + 1) + ")";
    }
}
