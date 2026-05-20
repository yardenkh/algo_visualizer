package algorithm.maxflow;

import algorithm.AlgorithmPalette;
import algorithm.AlgorithmRequest;
import algorithm.AlgorithmResult;
import algorithm.AlgorithmStep;
import algorithm.AlgorithmType;
import algorithm.EdgeKey;
import algorithm.GraphAlgorithm;
import algorithm.GraphOrientationRequirement;
import algorithm.SubroutineLibrary;
import algorithm.SubroutineStepCollector;
import algorithm.SubroutineTimeline;
import graph.Vertex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Edmonds-Karp maximum flow using BFS shortest augmenting paths. */
public final class EdmondsKarpAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "procedure EDMONDS-KARP(G, s, t)",
            "  initialize f(u,v) <- 0",
            "  while BFS finds augmenting path p in residual graph:",
            "    c_f(p) <- bottleneck residual capacity on p",
            "    augment flow along p by c_f(p)",
            "  return f"
    );

    @Override
    public String getDisplayName() {
        return "Edmonds-Karp";
    }

    @Override
    public String getDescription() {
        return "Ford-Fulkerson specialization using BFS for shortest augmenting paths.";
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.MAXIMUM_FLOW;
    }

    @Override
    public boolean requiresSourceVertex() {
        return true;
    }

    @Override
    public boolean requiresSinkVertex() {
        return true;
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
        if (request == null || request.isEmpty()) {
            return "Select vertices before running Edmonds-Karp.";
        }
        if (request.getSourceVertex() == null || request.getSinkVertex() == null) {
            return "Choose both source (s) and sink (t).";
        }
        if (request.getSourceVertex().getId() == request.getSinkVertex().getId()) {
            return "Source and sink must be different vertices.";
        }
        for (var edge : request.getScopeEdges()) {
            if (edge.getWeight() < 0) {
                return "Capacities must be non-negative for maximum flow.";
            }
        }
        return null;
    }

    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<Vertex> vertices = request.getScopeVertices();
        List<AlgorithmStep> steps = new ArrayList<>();
        int[][] capacity = MaxFlowSupport.capacities(request, vertices);
        int[][] flow = new int[vertices.size()][vertices.size()];
        SubroutineStepCollector augCollector = new SubroutineStepCollector();

        int s = MaxFlowSupport.indexOf(vertices, request.getSourceVertex().getId());
        int t = MaxFlowSupport.indexOf(vertices, request.getSinkVertex().getId());

        steps.add(MaxFlowSupport.step(request, vertices, capacity, flow, 1,
                "Initialize flow", "All edge flows start at 0.",
                request.getSourceVertex(), null, null, null));

        while (true) {
            int[] parent = bfs(capacity, flow, s, t);
            if (parent[t] == -1) {
                break;
            }

            int bottleneck = Integer.MAX_VALUE;
            int v = t;
            while (v != s) {
                int u = parent[v];
                bottleneck = Math.min(bottleneck, capacity[u][v] - flow[u][v]);
                v = u;
            }

            List<EdgeKey> pathEdges = pathEdges(vertices, parent, s, t);
            steps.add(MaxFlowSupport.stepWithSubroutine(request, vertices, capacity, flow, 3,
                    "Augmenting path (residual)", "Highlight shortest augmenting path. Click 'open sub-routine' on line 4 to follow AUGMENT(f,p).",
                    vertices.get(t), null, highlightPathVertices(vertices, parent, s, t), null, pathEdges,
                    SubroutineLibrary.AUGMENT));

            int finalBottleneck = bottleneck;
            augCollector.add(MaxFlowSupport.step(request, vertices, capacity, flow, 0,
                    "AUGMENT entry", "AUGMENT(f, p) called with bottleneck = " + finalBottleneck + ".",
                    vertices.get(t), null, highlightPathVertices(vertices, parent, s, t), null, pathEdges));
            augCollector.add(MaxFlowSupport.step(request, vertices, capacity, flow, 1,
                    "c_f(p) <- min residual", "Bottleneck c_f(p) = " + finalBottleneck + ".",
                    vertices.get(t), null, highlightPathVertices(vertices, parent, s, t), null, pathEdges));

            v = t;
            while (v != s) {
                int u = parent[v];
                flow[u][v] += bottleneck;
                flow[v][u] -= bottleneck;
                augCollector.add(MaxFlowSupport.step(request, vertices, capacity, flow, 3,
                        "Augment edge", "f(" + vertices.get(u).getName() + "," + vertices.get(v).getName() + ") updated.",
                        vertices.get(v), null, null, null));
                v = u;
            }

            steps.add(MaxFlowSupport.step(request, vertices, capacity, flow, 4,
                    "Augment flow", "Flow increased by " + bottleneck + ".",
                    vertices.get(t), null, highlightPathVertices(vertices, parent, s, t), null));
        }

        int maxFlow = 0;
        for (int v = 0; v < vertices.size(); v++) {
            maxFlow += Math.max(0, flow[s][v]);
        }

        steps.add(MaxFlowSupport.step(request, vertices, capacity, flow, 5,
                "Complete", "Maximum flow value = " + maxFlow + ".",
                request.getSinkVertex(), null, null, null));

        SubroutineTimeline augTimeline = augCollector.build(
                SubroutineLibrary.AUGMENT, "AUGMENT(f, p)", SubroutineLibrary.AUGMENT_PSEUDOCODE);
        SubroutineStepCollector.AlgorithmResultSubroutineData sub =
                SubroutineStepCollector.wrap(augTimeline, List.of(3, 4));

        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps, false, "Maximum flow = " + maxFlow, sub.timelines, sub.callableByLine);
    }

    private static int[] bfs(int[][] capacity, int[][] flow, int s, int t) {
        int n = capacity.length;
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = -1;
        }
        parent[s] = s;

        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(s);
        while (!q.isEmpty()) {
            int u = q.removeFirst();
            for (int v = 0; v < n; v++) {
                int residual = capacity[u][v] - flow[u][v];
                if (residual > 0 && parent[v] == -1) {
                    parent[v] = u;
                    if (v == t) {
                        return parent;
                    }
                    q.add(v);
                }
            }
        }
        return parent;
    }

    private static List<EdgeKey> pathEdges(List<Vertex> vertices, int[] parent, int s, int t) {
        List<EdgeKey> edges = new ArrayList<>();
        int v = t;
        while (v != s && v >= 0) {
            int u = parent[v];
            if (u < 0) {
                break;
            }
            edges.add(new EdgeKey(vertices.get(u).getId(), vertices.get(v).getId()));
            v = u;
        }
        return edges;
    }

    private static Map<Integer, Integer> highlightPathVertices(List<Vertex> vertices, int[] parent, int s, int t) {
        Map<Integer, Integer> colors = new LinkedHashMap<>();
        int v = t;
        while (v != s && v >= 0) {
            colors.put(vertices.get(v).getId(), AlgorithmPalette.FRONTIER);
            v = parent[v];
        }
        colors.put(vertices.get(s).getId(), AlgorithmPalette.FRONTIER);
        return colors;
    }
}
