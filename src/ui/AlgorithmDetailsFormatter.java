package ui;

import algorithm.GraphAlgorithm;

/**
 * Builds rich details for the algorithm selection panel.
 *
 * The formatter derives a friendlier input/output summary from the algorithm name/type
 * and appends the algorithm's own description text.
 *
 * Used by {@link ui.dialogs.AlgorithmRunDialog}.
 */
public final class AlgorithmDetailsFormatter {
    private AlgorithmDetailsFormatter() {
    }

    /**
     * Produces multi-section text for the run dialog description area.
     */
    public static String format(GraphAlgorithm algorithm) {
        if (algorithm == null) {
            return "No algorithm selected.";
        }

        String name = algorithm.getDisplayName();
        String type = algorithm.getType().getLabel();
        String orientation = switch (algorithm.getOrientationRequirement()) {
            case DIRECTED_ONLY -> "Directed only";
            case UNDIRECTED_ONLY -> "Undirected only";
            case EITHER -> "Directed or undirected";
        };
        String normalized = name.toLowerCase();

        String input;
        String output;

        if (normalized.contains("breadth-first") || normalized.contains("bfs")) {
            input = "Directed graph G=(V,E), selected scope, source vertex s.";
            output = "Traversal tree levels, reachable vertices, and dist(v) in edge count from s.";
        } else if (normalized.contains("depth-first") || normalized.contains("dfs")) {
            input = "Directed graph G=(V,E), selected scope, source vertex s.";
            output = "DFS forest, discovery/finish timestamps, edge classification behavior.";
        } else if (normalized.contains("dijkstra") && !normalized.contains("repeated")) {
            input = "Weighted directed graph with non-negative edges, source vertex s.";
            output = "Shortest-path estimates dist(v), predecessor structure, settled order.";
        } else if (normalized.contains("bellman-ford")) {
            input = "Weighted directed graph (negative edges allowed), source vertex s.";
            output = "Shortest-path distances or negative-cycle detection result.";
        } else if (normalized.contains("dag-shortest")) {
            input = "Directed acyclic graph (DAG), source vertex s.";
            output = "Shortest-path distances computed in topological order.";
        } else if (normalized.contains("topological")) {
            input = "Directed acyclic graph (DAG).";
            output = "Linear ordering of vertices where every edge goes forward.";
        } else if (normalized.contains("floyd-warshall")) {
            input = "Weighted directed graph adjacency/weight matrix.";
            output = "All-pairs shortest-path distance matrix D(i,j).";
        } else if (normalized.contains("matrix multiplication")) {
            input = "Weighted directed graph matrix W (min-plus semiring setting).";
            output = "APSP distance matrix after iterative EXTEND-SHORTEST-PATHS.";
        } else if (normalized.contains("recurssive doubling") || normalized.contains("recursive doubling")) {
            input = "Weighted directed graph matrix W.";
            output = "APSP matrix via repeated squaring / doubling of path-length coverage.";
        } else if (normalized.contains("johnson")) {
            input = "Sparse weighted directed graph, optionally with negative edges but no negative cycle.";
            output = "All-pairs shortest-path distances using Bellman-Ford + reweighted Dijkstra.";
        } else if (normalized.contains("repeated dijkastra") || normalized.contains("repeated dijkstra")) {
            input = "Weighted directed graph; runs Dijkstra from each source.";
            output = "APSP estimates per source, with explicit infinite-loop warning for negative-cycle behavior demo.";
        } else if (normalized.contains("prim")) {
            input = "Connected weighted graph interpretation (MST mode), root/source vertex r.";
            output = "Minimum spanning tree grown by cheapest frontier edge updates.";
        } else if (normalized.contains("kruskal")) {
            input = "Weighted graph interpretation (MST mode).";
            output = "Minimum spanning forest/tree via sorted edges + disjoint sets.";
        } else if (normalized.contains("dinic")) {
            input = "Directed capacitated network G=(V,E), source s, sink t.";
            output = "Maximum flow value and final flow assignment, with residual and level-graph states.";
        } else if (normalized.contains("edmonds-karp")) {
            input = "Directed capacitated network G=(V,E), source s, sink t.";
            output = "Maximum flow via shortest augmenting paths in residual graph.";
        } else if (normalized.contains("ford-fulkerson")) {
            input = "Directed capacitated network G=(V,E), source s, sink t.";
            output = "Maximum flow via augmenting paths and residual-capacity updates.";
        } else if (normalized.contains("transitive closure")) {
            input = "Directed graph adjacency relation.";
            output = "Reachability matrix T where T(i,j)=true iff j is reachable from i.";
        } else if (normalized.contains("a*")) {
            input = "Weighted graph, start/source node and implicit goal (demo target).";
            output = "Goal-directed shortest-path exploration states and g-score progression.";
        } else if (normalized.contains("seidel")) {
            input = "Unweighted undirected graph (APSP conceptual mode).";
            output = "All-pairs distance behavior through recursive matrix reduction/combination.";
        } else {
            input = algorithm.requiresSourceVertex()
                    ? "Selected graph scope and source vertex (when required)."
                    : "Selected graph scope.";
            output = "Algorithm-specific timeline states and computed result annotations.";
        }

        return "Name: " + name + System.lineSeparator()
                + "Type: " + type + System.lineSeparator()
                + "Orientation: " + orientation + System.lineSeparator()
                + System.lineSeparator()
                + "Input:" + System.lineSeparator()
                + "- " + input + System.lineSeparator()
                + System.lineSeparator()
                + "Output:" + System.lineSeparator()
                + "- " + output + System.lineSeparator()
                + System.lineSeparator()
                + "Description:" + System.lineSeparator()
                + "- " + algorithm.getDescription();
    }
}
