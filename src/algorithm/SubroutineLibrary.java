package algorithm;

import java.util.List;

/**
 * Canonical CLRS-style pseudocode definitions for all meaningful sub-routines.
 *
 * Each entry here corresponds to one reusable {@link SubroutineTimeline} that can be
 * opened as a second tab in the pseudocode dialog during playback.
 *
 * Rules enforced by design:
 * - Only pedagogically meaningful routines that appear in CLRS are included.
 * - IDs are stable string constants; algorithms reference them via these constants.
 * - Actual step lists are built per-run by the algorithm adapters, not stored here.
 *
 * Supported sub-routine IDs (referenced by algorithm adapters):
 *   RELAX       - RELAX(u, v, w)  used by Dijkstra and Bellman-Ford
 *   DFS_VISIT   - DFS-VISIT(u)    used by DFS
 *   BFS_LAYER   - BFS-LAYER       used by Dinic (blocking-flow DFS walk)
 *   AUGMENT     - AUGMENT(f, p)   used by Ford-Fulkerson and Edmonds-Karp
 */
public final class SubroutineLibrary {

    // -----------------------------------------------------------------------
    // Stable ID constants — algorithms store these as literals.
    // -----------------------------------------------------------------------

    /** ID for the RELAX(u, v, w) subroutine used in SSSP algorithms. */
    public static final String RELAX = "RELAX";

    /** ID for the DFS-VISIT(u) subroutine used in DFS. */
    public static final String DFS_VISIT = "DFS_VISIT";

    /** ID for the blocking-flow DFS walk subroutine used in Dinic. */
    public static final String BFS_LAYER = "BFS_LAYER";

    /** ID for the AUGMENT(f, p) subroutine used in augmenting-path max-flow algorithms. */
    public static final String AUGMENT = "AUGMENT";

    // -----------------------------------------------------------------------
    // Canonical CLRS pseudocode lines for each sub-routine.
    // -----------------------------------------------------------------------

    /**
     * RELAX(u, v, w) as described in CLRS Chapter 24.
     *
     * Used by Dijkstra (Chapter 24.3) and Bellman-Ford (Chapter 24.1).
     * The key idea: test whether going through u gives a shorter path to v.
     */
    public static final List<String> RELAX_PSEUDOCODE = List.of(
            "RELAX(u, v, w)",
            "  if d[v] > d[u] + w(u, v)",
            "      d[v] <- d[u] + w(u, v)",
            "      π[v] <- u"
    );

    /**
     * DFS-VISIT(u) as described in CLRS Chapter 22.3.
     *
     * Called recursively whenever a WHITE (undiscovered) vertex is first seen.
     * Colors each vertex GRAY on discovery and BLACK on finish.
     */
    public static final List<String> DFS_VISIT_PSEUDOCODE = List.of(
            "DFS-VISIT(u)",
            "  color[u] <- GRAY",
            "  time <- time + 1",
            "  d[u] <- time",
            "  for each v in Adj[u] do",
            "      if color[v] = WHITE then",
            "          π[v] <- u",
            "          DFS-VISIT(v)",
            "  color[u] <- BLACK",
            "  time <- time + 1",
            "  f[u] <- time"
    );

    /**
     * Blocking-flow DFS walk (level-respecting path) used in Dinic's algorithm.
     *
     * This is the inner DFS step of Dinic: finds one augmenting path in the level
     * graph built by BFS. Called repeatedly until no more level-respecting path exists.
     */
    public static final List<String> BFS_LAYER_PSEUDOCODE = List.of(
            "BLOCKING-FLOW-PATH(G_L, s, t, next)",
            "  path <- [s]",
            "  u <- s",
            "  while u != t do",
            "      v <- next level-graph neighbor of u",
            "      if no such v exists then return null  // dead end, dead end pruning",
            "      path.append(v); u <- v",
            "  bottleneck <- min residual capacity on path",
            "  for each edge (u, v) on path do",
            "      f(u, v) <- f(u, v) + bottleneck",
            "      f(v, u) <- f(v, u) - bottleneck",
            "  return bottleneck"
    );

    /**
     * AUGMENT(f, p) as used in Ford-Fulkerson and Edmonds-Karp (CLRS Chapter 26.2).
     *
     * Given an augmenting path p in the residual graph, this procedure computes
     * the bottleneck capacity and updates flow on every edge of p.
     */
    public static final List<String> AUGMENT_PSEUDOCODE = List.of(
            "AUGMENT(f, p)",
            "  c_f(p) <- min { c_f(u,v) : (u,v) in p }",
            "  for each edge (u, v) in p do",
            "      if (u, v) is a forward edge then",
            "          f(u, v) <- f(u, v) + c_f(p)",
            "      else",
            "          f(v, u) <- f(v, u) - c_f(p)"
    );

    private SubroutineLibrary() {
        // Static utility class — do not instantiate.
    }
}

