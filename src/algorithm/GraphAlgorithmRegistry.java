package algorithm;

import algorithm.apsp.FloydWarshallAlgorithm;
import algorithm.apsp.JohnsonAlgorithm;
import algorithm.apsp.MatrixMultiplicationApspAlgorithm;
import algorithm.apsp.RecursiveDoublingApspAlgorithm;
import algorithm.apsp.RepeatedDijkstraAlgorithm;
import algorithm.general.AStarAlgorithm;
import algorithm.general.SeidelAlgorithm;
import algorithm.general.TopologicalOrderAlgorithm;
import algorithm.general.TransitiveClosureAlgorithm;
import algorithm.mst.KruskalAlgorithm;
import algorithm.mst.PrimAlgorithm;
import algorithm.sssp.BellmanFordAlgorithm;
import algorithm.sssp.BreadthFirstSearchAlgorithm;
import algorithm.sssp.DagShortestPathAlgorithm;
import algorithm.sssp.DepthFirstSearchAlgorithm;
import algorithm.sssp.DijkstraShortestPathAlgorithm;
import algorithm.maxflow.DinicAlgorithm;
import algorithm.maxflow.EdmondsKarpAlgorithm;
import algorithm.maxflow.FordFulkersonAlgorithm;

import java.util.List;

/**
 * Central registry for algorithms available in the UI.
 *
 * If you add a new algorithm implementation, registering it here makes it appear in
 * {@link ui.dialogs.AlgorithmRunDialog} and therefore in the main application.
 *
 * Roadmap: see {@code project roadmap.md}.
 */
public final class GraphAlgorithmRegistry {
    private static final List<GraphAlgorithm> ALGORITHMS = List.of(
            // SSSP
            new BreadthFirstSearchAlgorithm(),
            new DepthFirstSearchAlgorithm(),
            new DijkstraShortestPathAlgorithm(),
            new BellmanFordAlgorithm(),
            new DagShortestPathAlgorithm(),
            new TopologicalOrderAlgorithm(),

            // APSP
            new MatrixMultiplicationApspAlgorithm(),
            new RecursiveDoublingApspAlgorithm(),
            new FloydWarshallAlgorithm(),
            new JohnsonAlgorithm(),
            new RepeatedDijkstraAlgorithm(),

            // MST
            new PrimAlgorithm(),
            new KruskalAlgorithm(),

            // Maximum flow
            new FordFulkersonAlgorithm(),
            new EdmondsKarpAlgorithm(),
            new DinicAlgorithm(),

            // General
            new TransitiveClosureAlgorithm(),
            new AStarAlgorithm(),
            new SeidelAlgorithm()
    );

    private GraphAlgorithmRegistry() {
    }

    /** @return immutable list of all algorithms exposed by the application. */
    public static List<GraphAlgorithm> getAlgorithms() {
        return ALGORITHMS;
    }
}
