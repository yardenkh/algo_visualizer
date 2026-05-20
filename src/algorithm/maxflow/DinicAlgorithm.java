package algorithm.maxflow;

import algorithm.AlgorithmPalette;
import algorithm.AlgorithmRequest;
import algorithm.AlgorithmResult;
import algorithm.AlgorithmStep;
import algorithm.AlgorithmType;
import algorithm.GraphAlgorithm;
import algorithm.GraphOrientationRequirement;
import algorithm.LevelGraphSnapshot;
import algorithm.SubroutineLibrary;
import algorithm.SubroutineStepCollector;
import algorithm.SubroutineTimeline;
import graph.Vertex;
import algorithm.EdgeKey;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/** Dinic maximum flow algorithm with explicit level-graph visualization. */
public final class DinicAlgorithm implements GraphAlgorithm {
    private static final List<String> PSEUDOCODE = List.of(
            "DINIC(G, s, t)",
            "  for each edge (u, v) in G",
            "      f(u, v) <- 0",
            "  maxFlow <- 0",
            "  repeat",
            "      run BFS in the residual graph from s",
            "      use BFS distances to build the level graph",
            "      if t is unreachable in the level graph, return maxFlow",
            "      while DFS finds an s->t path P that always goes to the next level",
            "          bottleneck <- minimum residual capacity on P",
            "          highlight P so we can inspect the chosen augmenting path",
            "          augment every edge of P by bottleneck",
            "          maxFlow <- maxFlow + bottleneck"
    );

    @Override
    public String getDisplayName() {
        return "Dinic";
    }

    @Override
    public String getDescription() {
        return "Maximum flow by building a layered residual graph with BFS and then using DFS to push blocking flow through that level graph.";
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
            return "Select vertices before running Dinic.";
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
        SubroutineStepCollector blockingCollector = new SubroutineStepCollector();

        int s = MaxFlowSupport.indexOf(vertices, request.getSourceVertex().getId());
        int t = MaxFlowSupport.indexOf(vertices, request.getSinkVertex().getId());

        steps.add(MaxFlowSupport.step(request, vertices, capacity, flow, 2,
                "Initialize flow", "All edge flows start at 0.",
                request.getSourceVertex(), null, null, null));

        int maxFlow = 0;
        int[] level = new int[vertices.size()];
        while (bfsLevel(capacity, flow, s, t, level)) {
            LevelGraphSnapshot levelSnapshot = MaxFlowSupport.levelSnapshot(
                    request, vertices, level, capacity, flow,
                    request.getSourceVertex(), request.getSinkVertex());
            steps.add(MaxFlowSupport.step(request, vertices, capacity, flow, 6,
                    "Build level graph", "BFS built a new level graph.",
                    request.getSourceVertex(), null, null, levelSnapshot));

            int[] next = new int[vertices.size()];
            while (true) {
                List<Integer> path = new ArrayList<>();
                path.add(s);
                int pushed = findBlockingPath(capacity, flow, level, next, s, t, Integer.MAX_VALUE, path);
                if (pushed == 0) {
                    break;
                }

                List<EdgeKey> pathEdges = pathEdges(vertices, path);
                // Tag as BFS_LAYER callable
                steps.add(MaxFlowSupport.stepWithSubroutine(request, vertices, capacity, flow, 10,
                        "Augmenting path (residual)", "Level-respecting path found. Click 'open sub-routine' on line 8 to follow BLOCKING-FLOW-PATH.",
                        vertices.get(t), null, highlightPathVertices(vertices, path), levelSnapshot, pathEdges,
                        SubroutineLibrary.BFS_LAYER));

                // Collect blocking-path subroutine body steps (before augmentation)
                int finalPushed = pushed;
                blockingCollector.add(MaxFlowSupport.step(request, vertices, capacity, flow, 0,
                        "BLOCKING-FLOW-PATH entry", "Finding level-graph path from s to t.",
                        request.getSourceVertex(), null, null, levelSnapshot));
                for (int i = 0; i < path.size(); i++) {
                    blockingCollector.add(MaxFlowSupport.step(request, vertices, capacity, flow, i == 0 ? 1 : 3,
                            "Path vertex " + vertices.get(path.get(i)).getName(),
                            "Adding " + vertices.get(path.get(i)).getName() + " to path.",
                            vertices.get(path.get(i)), null, highlightPathVertices(vertices, path.subList(0, i + 1)), levelSnapshot));
                }
                blockingCollector.add(MaxFlowSupport.step(request, vertices, capacity, flow, 7,
                        "Bottleneck = " + finalPushed, "Minimum residual capacity on path is " + finalPushed + ".",
                        vertices.get(t), null, highlightPathVertices(vertices, path), levelSnapshot, pathEdges));

                applyAugment(flow, path, pushed);
                maxFlow += pushed;

                for (int i = 0; i + 1 < path.size(); i++) {
                    int u = path.get(i), v = path.get(i + 1);
                    blockingCollector.add(MaxFlowSupport.step(request, vertices, capacity, flow, 9,
                            "Update edge " + vertices.get(u).getName() + "->" + vertices.get(v).getName(),
                            "f(" + vertices.get(u).getName() + "," + vertices.get(v).getName() + ") updated by " + finalPushed + ".",
                            vertices.get(v), null, null, levelSnapshot));
                }

                steps.add(MaxFlowSupport.step(request, vertices, capacity, flow, 11,
                        "Blocking flow augment", "Augmented " + pushed + " units on the highlighted path.",
                        vertices.get(t), null, highlightPathVertices(vertices, path), levelSnapshot));
            }
        }

        steps.add(MaxFlowSupport.step(request, vertices, capacity, flow, 7,
                "Complete", "No more s->t path exists in the residual level graph. Maximum flow = " + maxFlow + ".",
                request.getSinkVertex(), null, null, null));

        SubroutineTimeline blockingTimeline = blockingCollector.build(
                SubroutineLibrary.BFS_LAYER, "BLOCKING-FLOW-PATH", SubroutineLibrary.BFS_LAYER_PSEUDOCODE);
        SubroutineStepCollector.AlgorithmResultSubroutineData sub =
                SubroutineStepCollector.wrap(blockingTimeline, List.of(8, 10, 11));

        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps, false, "Maximum flow = " + maxFlow, sub.timelines, sub.callableByLine);
    }

    private static boolean bfsLevel(int[][] capacity, int[][] flow, int s, int t, int[] level) {
        Arrays.fill(level, -1);
        level[s] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(s);

        while (!q.isEmpty()) {
            int u = q.removeFirst();
            for (int v = 0; v < capacity.length; v++) {
                int residual = capacity[u][v] - flow[u][v];
                if (residual > 0 && level[v] < 0) {
                    level[v] = level[u] + 1;
                    q.add(v);
                }
            }
        }
        return level[t] >= 0;
    }

    private static int findBlockingPath(
            int[][] capacity,
            int[][] flow,
            int[] level,
            int[] next,
            int u,
            int t,
            int pushed,
            List<Integer> path
    ) {
        if (u == t) {
            return pushed;
        }

        for (int v = next[u]; v < capacity.length; v++) {
            next[u] = v + 1;
            int residual = capacity[u][v] - flow[u][v];
            if (residual <= 0 || level[v] != level[u] + 1) {
                continue;
            }

            path.add(v);
            int tr = findBlockingPath(capacity, flow, level, next, v, t, Math.min(pushed, residual), path);
            if (tr > 0) {
                return tr;
            }
            path.remove(path.size() - 1);
        }
        return 0;
    }

    private static void applyAugment(int[][] flow, List<Integer> path, int pushed) {
        for (int i = 0; i + 1 < path.size(); i++) {
            int u = path.get(i);
            int v = path.get(i + 1);
            flow[u][v] += pushed;
            flow[v][u] -= pushed;
        }
    }

    private static List<EdgeKey> pathEdges(List<Vertex> vertices, List<Integer> path) {
        List<EdgeKey> edges = new ArrayList<>();
        for (int i = 0; i + 1 < path.size(); i++) {
            int u = path.get(i);
            int v = path.get(i + 1);
            edges.add(new EdgeKey(vertices.get(u).getId(), vertices.get(v).getId()));
        }
        return edges;
    }

    private static Map<Integer, Integer> highlightPathVertices(List<Vertex> vertices, List<Integer> path) {
        Map<Integer, Integer> colors = new LinkedHashMap<>();
        for (Integer idx : path) {
            colors.put(vertices.get(idx).getId(), AlgorithmPalette.FRONTIER);
        }
        return colors;
    }
}
